package de.fmp.liulab.task;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTableUtil;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.view.model.VisualLexicon;
import org.cytoscape.view.presentation.customgraphics.CyCustomGraphics2Factory;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.presentation.property.values.BendFactory;
import org.cytoscape.view.presentation.property.values.HandleFactory;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;

import de.fmp.liulab.internal.view.JFrameWithoutMaxAndMinButton;
import de.fmp.liulab.model.Protein;
import de.fmp.liulab.model.ProteinDomain;
import de.fmp.liulab.utils.Util;

public class UpdateProteinInformationTask extends AbstractTask implements ActionListener {

	private CyApplicationManager cyApplicationManager;
	private CyNetwork myNetwork;
	private CyNetworkView netView;
	private HandleFactory handleFactory;
	private BendFactory bendFactory;
	public VisualStyle style;
	public VisualLexicon lexicon;

	private boolean validateProtein;
	private Protein myProtein;
	private List<ProteinDomain> myProteinDomains;

	private List<CyNode> nodes;

	public static CyNode node;

	// Window
	private JFrameWithoutMaxAndMinButton mainFrame;
	private JPanel mainPanel;
	private JPanel protein_panel;
	private JPanel domain_panel;
	private static JLabel textLabel_status_result;

	// Table
	private String[] columnNamesDomainTable = { "Domain(*)", "Start Residue(*)", "End Residue(*)", "Score", "Valid" };
	@SuppressWarnings("rawtypes")
	private final Class[] columnClassDomainTable = new Class[] { String.class, Integer.class, Integer.class,
			String.class, Boolean.class };
	private static DefaultTableModel domainTableDataModel;
	private static JTable mainProteinDomainTable;
	@SuppressWarnings("rawtypes")
	private static JList rowHeaderDomainTable;
	private static JScrollPane proteinDomainTableScrollPanel;

	private static JButton okButton;
	private static Thread storeDomainThread;
	private static boolean isStoredProteinDomains = false;

	public static Thread disposeMainJFrameThread;

	// Map<Protein - Node SUID, Protein
	private static Map<Long, Protein> proteinsMap = new HashMap<Long, Protein>();

	public UpdateProteinInformationTask(CyApplicationManager cyApplicationManager,
			final VisualMappingManager vmmServiceRef, CyCustomGraphics2Factory<?> vgFactory, BendFactory bendFactory,
			HandleFactory handleFactory, boolean validateProtein) {
		this.cyApplicationManager = cyApplicationManager;
		this.netView = cyApplicationManager.getCurrentNetworkView();
		this.myNetwork = cyApplicationManager.getCurrentNetwork();
		style = vmmServiceRef.getCurrentVisualStyle();
		// Get the current Visual Lexicon
		lexicon = cyApplicationManager.getCurrentRenderingEngine().getVisualLexicon();
		this.bendFactory = bendFactory;
		this.handleFactory = handleFactory;
		this.validateProtein = validateProtein;

	}

	private void createFrame() {

		if (mainFrame == null)
			mainFrame = new JFrameWithoutMaxAndMinButton(new JFrame(), "P2Location - Set predicted protein domains", 4);

		mainFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		Dimension appSize = null;
		if (Util.isWindows()) {
			appSize = new Dimension(545, 375);
		} else if (Util.isMac()) {
			appSize = new Dimension(530, 355);
		} else {
			appSize = new Dimension(530, 365);
		}
		mainFrame.setSize(appSize);
		mainFrame.setResizable(false);

		if (mainPanel == null)
			mainPanel = new JPanel();
		mainPanel.setBounds(10, 10, 495, 365);
		mainPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), ""));
		mainPanel.setLayout(null);

		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		mainFrame.setLocation((screenSize.width - appSize.width) / 2, (screenSize.height - appSize.height) / 2);
		mainFrame.setVisible(true);

		initThreads();
	}

	private void initThreads() {
		disposeMainJFrameThread = new Thread() {
			public synchronized void run() {
				disposeMainFrame();
			}
		};
	}

	private void disposeMainFrame() {
		mainFrame.dispose();

		JOptionPane.showMessageDialog(null, "Protein domains have been loaded successfully!",
				"P2Location - Protein domains", JOptionPane.INFORMATION_MESSAGE,
				new ImageIcon(getClass().getResource("/images/logo.png")));
	}

	/**
	 * Method responsible for canceling the loading process
	 * 
	 * @return true if process is canceled, otherwise, returns false.
	 */
	public static boolean cancelProcess() {

		boolean concluedProcess = true;

		if (!isStoredProteinDomains) {
			int input = JOptionPane.showConfirmDialog(null,
					"Protein domains has not been stored yet. Do you want to close this window?",
					"P2Location - Protein domains", JOptionPane.INFORMATION_MESSAGE);
			// 0=yes, 1=no, 2=cancel
			if (input == 0) {
				concluedProcess = true;
				if (storeDomainThread != null) {
					storeDomainThread.interrupt();
				}
			} else {
				concluedProcess = false;
			}
		}

		if (concluedProcess) {
			return true;
		} else {
			return false;
		}

	}

	/**
	 * Set properties to the Node domain table
	 * 
	 * @param number_lines total number of lines
	 */
	public static void setTableProperties(int number_lines) {
		if (mainProteinDomainTable != null) {
			mainProteinDomainTable.setPreferredScrollableViewportSize(new Dimension(490, 90));
			mainProteinDomainTable.getColumnModel().getColumn(0).setPreferredWidth(150);
			mainProteinDomainTable.getColumnModel().getColumn(1).setPreferredWidth(150);
			mainProteinDomainTable.getColumnModel().getColumn(2).setPreferredWidth(150);
			mainProteinDomainTable.getColumnModel().getColumn(3).setPreferredWidth(80);
			mainProteinDomainTable.getColumnModel().getColumn(4).setPreferredWidth(100);
			mainProteinDomainTable.setFillsViewportHeight(true);
			mainProteinDomainTable.setAutoCreateRowSorter(true);

			Util.updateRowHeader(number_lines, mainProteinDomainTable, rowHeaderDomainTable,
					proteinDomainTableScrollPanel);
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void run(TaskMonitor taskMonitor) throws Exception {

		taskMonitor.setTitle("P2Location - Update Protein Information task");

		if (cyApplicationManager.getCurrentNetwork() == null) {
			throw new Exception("ERROR: No network has been loaded.");
		}

		checkSingleOrMultipleSelectedNodes(taskMonitor);

	}

	/**
	 * Method responsible for checking how many nodes have been selected
	 * 
	 * @param taskMonitor
	 * @throws Exception
	 */
	private void checkSingleOrMultipleSelectedNodes(final TaskMonitor taskMonitor) throws Exception {

		nodes = CyTableUtil.getNodesInState(myNetwork, CyNetwork.SELECTED, true);

		if (nodes.size() == 0) {

			taskMonitor.showMessage(TaskMonitor.Level.WARN, "No node has been selected.");
			throw new Exception("WARNING: No node has been selected!");

		} else if (nodes.size() > 1) {

			if (validateProtein)
				executeMultipleNodes(taskMonitor);
			else {
				taskMonitor.showMessage(TaskMonitor.Level.WARN,
						"More than one protein has been selected.\nPlease select only one!");
				throw new Exception("WARNING: More than one protein has been selected\nPlease select only one!");
			}

		} else {
			node = nodes.get(0);
			executeSingleNode(taskMonitor);
		}

	}

	/**
	 * Method responsible for executing layout to multiple nodes
	 * 
	 * @param taskMonitor
	 * @throws Exception
	 */
	private void executeMultipleNodes(final TaskMonitor taskMonitor) throws Exception {

		int old_progress = 0;
		int summary_processed = 0;
		int total_rows = nodes.size();

		for (CyNode current_node : nodes) {
			node = current_node;
			executeSingleNode(taskMonitor);

			summary_processed++;
			Util.progressBar(summary_processed, old_progress, total_rows, "Updating protein information: ", taskMonitor,
					null);
		}
	}

	/**
	 * Method responsible for executing layout to a single node
	 * 
	 * @param taskMonitor
	 * @throws Exception
	 */
	private void executeSingleNode(final TaskMonitor taskMonitor) throws Exception {

		// ##### GET THE SELECTED NODE - ONLY ONE IS POSSIBLE TO APPLY CHANGES ######

		if (myNetwork == null || node == null || netView == null)
			return;

		CyRow myCurrentRow = myNetwork.getRow(node);
		if (myCurrentRow == null)
			return;

		String nodeName = (String) myCurrentRow.getRaw(CyNetwork.NAME);
		myProtein = Util.getProtein(myNetwork, nodeName);
		if (myProtein == null) {
			throw new Exception(
					"There is no information in column 'sequence' or 'domain_annotation' for the protein: " + nodeName);
		}

		if (validateProtein) {

			myProtein.isValid = !myProtein.isValid;

			View<CyNode> new_node_source_view = netView.getNodeView(node);
			if (myProtein.isValid)
				new_node_source_view.setLockedValue(BasicVisualLexicon.NODE_VISIBLE, true);
			else
				new_node_source_view.setLockedValue(BasicVisualLexicon.NODE_VISIBLE, false);

			Util.restoreDefaultStyle(null, style, netView, cyApplicationManager, new_node_source_view, handleFactory,
					bendFactory, node, myNetwork, myCurrentRow, lexicon);
			Util.updateValidProteinInformationInCytoScapeNodeTable(myNetwork, node, myProtein,
					Util.VALID_PROTEINS_COLUMN);
		} else {

			this.init_predictedDomain_window(taskMonitor);

		}

	}

	/**
	 * Method responsible for opening the Single Node Layout window
	 * 
	 * @param taskMonitor
	 * @throws Exception
	 */
	private void init_predictedDomain_window(final TaskMonitor taskMonitor) throws Exception {

		getSelectedProtein(node); // Fill in myProteinDomains collection based on the main Map
		// (Util.proteinDomainsMap)

		boolean hasPredictedDomains = true;

		if (myProtein != null && myProtein.domains != null) {

			if (myProtein.domains.stream().filter(value -> value.isPredicted).collect(Collectors.toList()).size() > 0) {

				createFrame();
				setFrameObjects(taskMonitor);

				// Display the window
				mainFrame.add(mainPanel, BorderLayout.CENTER);
				mainFrame.setLocationRelativeTo(null);
				mainFrame.setVisible(true);

			} else
				hasPredictedDomains = false;
		} else
			hasPredictedDomains = false;

		if (!hasPredictedDomains)
			throw new Exception("WARNING: There is no predicted domain for the selected protein.");
	}

	/**
	 * Set all objects to main Frame
	 * 
	 * @param taskMonitor
	 */
	private void setFrameObjects(final TaskMonitor taskMonitor) {

		initFrameLabels();

		initTableScreen();

		initButtons(taskMonitor);
	}

	/**
	 * Set all labels in P2Location window / frame
	 */
	private void initFrameLabels() {

		int offset_y = -20;

		protein_panel = new JPanel();
		protein_panel.setBorder(BorderFactory.createTitledBorder("Protein"));
		if (Util.isWindows())
			protein_panel.setBounds(10, 10, 250, 90);
		else if (Util.isMac())
			protein_panel.setBounds(10, 10, 275, 90);
		else
			protein_panel.setBounds(10, 10, 277, 90);
		protein_panel.setLayout(null);
		mainPanel.add(protein_panel);

		JLabel textLabel_Protein_lbl = new JLabel("Name:");
		textLabel_Protein_lbl.setFont(new java.awt.Font("Tahoma", Font.PLAIN, 12));
		textLabel_Protein_lbl.setBounds(10, offset_y, 50, 100);
		protein_panel.add(textLabel_Protein_lbl);

		JLabel textLabel_Protein_result = new JLabel();
		textLabel_Protein_result.setText(myProtein.gene);
		textLabel_Protein_result.setFont(new java.awt.Font("Tahoma", Font.BOLD, 12));
		if (Util.isUnix())
			textLabel_Protein_result.setBounds(95, offset_y, 100, 100);
		else
			textLabel_Protein_result.setBounds(85, offset_y, 100, 100);
		protein_panel.add(textLabel_Protein_result);
		offset_y += 30;

		JLabel textLabel_Protein_size_lbl = new JLabel("Size:");
		textLabel_Protein_size_lbl.setFont(new java.awt.Font("Tahoma", Font.PLAIN, 12));
		textLabel_Protein_size_lbl.setBounds(10, offset_y, 70, 100);
		protein_panel.add(textLabel_Protein_size_lbl);

		JLabel textLabel_Protein_size_result = new JLabel();
		textLabel_Protein_size_result.setText((int) Util.getProteinLength() + " residues");
		textLabel_Protein_size_result.setFont(new java.awt.Font("Tahoma", Font.BOLD, 12));
		if (Util.isUnix())
			textLabel_Protein_size_result.setBounds(95, offset_y, 100, 100);
		else
			textLabel_Protein_size_result.setBounds(85, offset_y, 100, 100);
		protein_panel.add(textLabel_Protein_size_result);

		JPanel logo_panel = new JPanel();
		logo_panel.setBorder(BorderFactory.createTitledBorder(""));
		if (Util.isWindows())
			logo_panel.setBounds(265, 16, 245, 82);
		else if (Util.isMac())
			logo_panel.setBounds(290, 16, 220, 82);
		else
			logo_panel.setBounds(290, 25, 220, 72);
		logo_panel.setLayout(null);
		mainPanel.add(logo_panel);

		ImageIcon imageIcon = new javax.swing.ImageIcon(getClass().getResource("/images/logo.png")); // load the image
																										// to a
																										// imageIcon
		Image image = imageIcon.getImage(); // transform it
		Image newimg = image.getScaledInstance(80, 80, java.awt.Image.SCALE_SMOOTH); // scale it the smooth way
		imageIcon = new ImageIcon(newimg); // transform it back

		JLabel jLabelIcon = new JLabel();
		if (Util.isWindows())
			jLabelIcon.setBounds(80, -60, 200, 200);
		else
			jLabelIcon.setBounds(65, -60, 200, 200);
		jLabelIcon.setIcon(imageIcon);
		logo_panel.add(jLabelIcon);

		if (Util.isWindows())
			offset_y = 75;
		else
			offset_y = 65;

		if (myProtein != null && !myProtein.isConflictedDomain)
			textLabel_status_result = new JLabel("There is no domain conflict!");
		else
			textLabel_status_result = new JLabel("???");
		textLabel_status_result.setFont(new java.awt.Font("Tahoma", Font.PLAIN, 12));
		textLabel_status_result.setForeground(new Color(159, 17, 17));
		textLabel_status_result.setBounds(55, offset_y, 450, 100);

		JLabel textLabel_status = new JLabel("Status:");
		textLabel_status.setFont(new java.awt.Font("Tahoma", Font.PLAIN, 12));
		textLabel_status.setBounds(10, offset_y, 50, 100);
		mainPanel.add(textLabel_status);
		mainPanel.add(textLabel_status_result);

		if (Util.isWindows())
			offset_y = 240;
		else
			offset_y = 230;
		JLabel textLabel_required_fields = new JLabel("(*) Required fields");
		textLabel_required_fields.setFont(new java.awt.Font("Tahoma", Font.PLAIN, 10));
		textLabel_required_fields.setBounds(20, offset_y, 150, 50);
		mainPanel.add(textLabel_required_fields);

	}

	/**
	 * Method responsible for initializing the table in the Frame
	 */
	private void initTableScreen() {

		// Get indexes of non-predicted domains
		List<Integer> nonPredictedDomains = new ArrayList<Integer>();
		if (myProtein != null && myProtein.domains != null && myProtein.domains.size() > 0) {
			for (int i = 0; i < myProtein.domains.size(); i++) {
				ProteinDomain ptnDomain = myProtein.domains.get(i);
				if (!ptnDomain.isPredicted)
					nonPredictedDomains.add(i);

			}
		}

		Object[][] domainDataObj = new Object[1][5];
		// create table model with data
		domainTableDataModel = new DefaultTableModel(domainDataObj, columnNamesDomainTable) {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isCellEditable(int row, int column) {
				if (column == 0 || column == 3)
					return false;

				if (nonPredictedDomains.contains(row))
					return false;
				return true;
			}

			@Override
			public Class<?> getColumnClass(int columnIndex) {
				return columnClassDomainTable[columnIndex];
			}
		};

		mainProteinDomainTable = new JTable(domainTableDataModel);
		// Create the scroll pane and add the table to it.
		proteinDomainTableScrollPanel = new JScrollPane();

		if (Util.isWindows())
			proteinDomainTableScrollPanel.setBounds(8, 20, 485, 90);
		else
			proteinDomainTableScrollPanel.setBounds(8, 20, 485, 90);
		proteinDomainTableScrollPanel.setViewportView(mainProteinDomainTable);
		proteinDomainTableScrollPanel.setRowHeaderView(rowHeaderDomainTable);

		domain_panel = new JPanel();
		domain_panel.setBorder(BorderFactory.createTitledBorder("Predicted domains"));
		if (Util.isWindows())
			domain_panel.setBounds(10, 145, 503, 140);
		else
			domain_panel.setBounds(10, 135, 503, 140);
		domain_panel.setLayout(null);
		mainPanel.add(domain_panel);

		domain_panel.add(proteinDomainTableScrollPanel);

		if (myProtein != null && myProtein.domains != null && myProtein.domains.size() > 0) {

			domainDataObj = new Object[myProtein.domains.size()][5];
			domainTableDataModel.setDataVector(domainDataObj, columnNamesDomainTable);

			Collections.sort(myProtein.domains, new Comparator<ProteinDomain>() {
				@Override
				public int compare(ProteinDomain lhs, ProteinDomain rhs) {
					return lhs.startId > rhs.startId ? 1 : (lhs.startId < rhs.startId) ? -1 : 0;
				}
			});

			int countPtnDomain = 0;
			for (ProteinDomain domain : myProtein.domains) {
				domainTableDataModel.setValueAt(domain.name, countPtnDomain, 0);
				domainTableDataModel.setValueAt(domain.startId, countPtnDomain, 1);
				domainTableDataModel.setValueAt(domain.endId, countPtnDomain, 2);
				domainTableDataModel.setValueAt(domain.eValue, countPtnDomain, 3);
				domainTableDataModel.setValueAt(domain.isValid, countPtnDomain, 4);
				countPtnDomain++;
			}
			setTableProperties(myProtein.domains.size());
		} else {
			setTableProperties(1);
		}

	}

	/**
	 * Method responsible for getting the protein domains of the selected node from
	 * the main map (Util.proteinDomainsMap)
	 * 
	 * @param node
	 */
	private void getSelectedProtein(CyNode node) {

		String network_name = myNetwork.toString();
		if (Util.proteinsMap.containsKey(network_name)) {

			List<Protein> all_proteins = Util.proteinsMap.get(network_name);
			String node_name = myNetwork.getRow(node).get(CyNetwork.NAME, String.class);

			Optional<Protein> isPtnPresent = all_proteins.stream().filter(value -> value.gene.equals(node_name))
					.findFirst();

			if (isPtnPresent.isPresent()) {
				myProtein = isPtnPresent.get();
			}

		}
	}

	/**
	 * Method responsible for initializing all button in the Frame
	 * 
	 * @param taskMonitor
	 */
	private void initButtons(final TaskMonitor taskMonitor) {

		Icon iconBtnOk = new ImageIcon(getClass().getResource("/images/okBtn.png"));
		okButton = new JButton(iconBtnOk);
		okButton.setText("OK");
		if (Util.isWindows())
			okButton.setBounds(30, 300, 220, 25);
		else
			okButton.setBounds(30, 290, 220, 25);

		okButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent ae) {

				try {

					isStoredProteinDomains = false;
					okButton.setEnabled(false);
					storeProteinDomains(taskMonitor, myNetwork);

				} catch (Exception e1) {
					textLabel_status_result.setText("ERROR: Check Task History.");
					taskMonitor.showMessage(TaskMonitor.Level.ERROR, "ERROR: " + e1.getMessage());

					if (storeDomainThread != null)
						storeDomainThread.interrupt();
				}
			}
		});
		mainPanel.add(okButton);

		Icon iconBtnCancel = new ImageIcon(getClass().getResource("/images/cancelBtn.png"));
		JButton cancelButton = new JButton(iconBtnCancel);
		cancelButton.setText("Cancel");
		if (Util.isWindows())
			cancelButton.setBounds(265, 300, 220, 25);
		else
			cancelButton.setBounds(265, 290, 220, 25);

		cancelButton.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if (cancelProcess())
					mainFrame.dispose();
			}
		});
		mainPanel.add(cancelButton);
	}

	/**
	 * Method responsible for storing protein domains
	 * 
	 * @param taskMonitor
	 * @throws Exception
	 */
	private void storeProteinDomains(TaskMonitor taskMonitor, final CyNetwork myNetwork) throws Exception {

		if (myNetwork == null) {
			throw new Exception("ERROR: No network has been found.");
		}

		storeDomainThread = new Thread() {

			public synchronized void run() {

				taskMonitor.setProgress(0.2);

				try {
					textLabel_status_result.setText("Getting protein domains from table...");
					getProteinDomainsFromTable();
					taskMonitor.setProgress(0.45);

					textLabel_status_result.setText("Checking domain conflicts...");
					checkProteinDomains();
					taskMonitor.setProgress(0.65);

					storeProteinDomains();
					taskMonitor.setProgress(0.85);
					// It's finished!
					isStoredProteinDomains = true;
					textLabel_status_result.setText("Done!");
					okButton.setEnabled(true);
					disposeMainJFrameThread.start();
					taskMonitor.setProgress(1.0);

				} catch (Exception e2) {
					textLabel_status_result.setText(e2.getMessage());
					taskMonitor.showMessage(TaskMonitor.Level.WARN, e2.getMessage());
					isStoredProteinDomains = false;
					okButton.setEnabled(true);
					return;
				}

			}
		};

		storeDomainThread.start();
	}

	/**
	 * Get all domains assigned on the Table
	 * 
	 * @throws Exception
	 */
	private void getProteinDomainsFromTable() throws Exception {

		myProteinDomains = new ArrayList<ProteinDomain>();

		for (int row = 0; row < domainTableDataModel.getRowCount(); row++) {

			String domain = domainTableDataModel.getValueAt(row, 0) != null
					? domainTableDataModel.getValueAt(row, 0).toString()
					: "";
			int startId = domainTableDataModel.getValueAt(row, 1) != null
					? (int) domainTableDataModel.getValueAt(row, 1)
					: 0;
			int endId = domainTableDataModel.getValueAt(row, 2) != null ? (int) domainTableDataModel.getValueAt(row, 2)
					: 0;

			String eValue = domainTableDataModel.getValueAt(row, 3) != null
					? domainTableDataModel.getValueAt(row, 3).toString()
					: "";

			boolean isValid = domainTableDataModel.getValueAt(row, 4) != null
					? (boolean) domainTableDataModel.getValueAt(row, 4)
					: true;

			boolean isPredicted = domainTableDataModel.isCellEditable(row, 4);

			myProteinDomains.add(new ProteinDomain(domain, startId, endId, isPredicted, eValue, isValid));

		}

	}

	/**
	 * Method responsible for saving new protein domains
	 */
	private void storeProteinDomains() {

		if (myProtein == null)
			return;

		myProtein.domains = myProteinDomains;
		updateProteinMap(myNetwork, node, myProtein);
		Util.updateProteinDomains(myNetwork, node);

	}

	/**
	 * Check whether exists domains in the same region
	 * 
	 * @throws Exception
	 */
	private void checkProteinDomains() throws Exception {

		if (myProteinDomains == null)
			return;

		List<ProteinDomain> validDomains = myProteinDomains.stream().filter(value -> value.isValid)
				.collect(Collectors.toList());

		for (ProteinDomain proteinDomain : validDomains) {
			if (ProcessProteinLocationTask.hasSimilarProteinDomain(validDomains, proteinDomain.startId,
					proteinDomain.endId)) {
				throw new Exception("There is a domain conflict. Please select only one domain for each region.");
			}
		}

		Protein current_protein = new Protein();
		current_protein.domains = myProteinDomains;

		ProcessProteinLocationTask.checkConflictProteinDomains(current_protein);

		if (current_protein.isConflictedDomain)
			throw new Exception("There is a domain conflict. Please select only one domain for each region.");

	}

	/**
	 * Method responsible for setting information to all nodes
	 * 
	 * @throws Exception
	 */
	private static String setNodesInformation(final TaskMonitor taskMonitor, final CyNetwork myNetwork)
			throws Exception {

		if (myNetwork == null)
			return "ERROR: Network has not been found.";

		// Initialize protein domain colors map if MainSingleNodeTask has not been
		// initialized
		Util.init_availableProteinDomainColorsMap();

		int old_progress = 0;
		int summary_processed = 0;
		int total_genes = proteinsMap.size();

		StringBuilder sb_error = new StringBuilder();

		for (Map.Entry<Long, Protein> entry : proteinsMap.entrySet()) {

			CyNode currentNode = null;

			Long key = entry.getKey();
			String node_name = myNetwork.getDefaultNodeTable().getRow(key).getRaw(CyNetwork.NAME).toString();

			CyNode node = Util.getNode(myNetwork, key);
			if (node != null) {

				Protein protein = entry.getValue();
				if (protein != null) {
					protein.predicted_domain_epoch = 0;
					updateProteinMap(myNetwork, currentNode, protein);
				}

			} else {
				sb_error.append("WARNING: Node " + node_name + " has not been found.\n");
			}

			summary_processed++;
			int new_progress = (int) ((double) summary_processed / (total_genes) * 100);
			if (new_progress > old_progress) {
				old_progress = new_progress;

				if (textLabel_status_result != null)
					textLabel_status_result.setText("Storing protein domains: " + old_progress + "%");
				taskMonitor.showMessage(TaskMonitor.Level.INFO, "Storing protein domains: " + old_progress + "%");
			}
		}

		taskMonitor.setProgress(0.98);
		return sb_error.toString();
	}

	/**
	 * Method responsible for update Protein domains map
	 * 
	 * @param node
	 * @param myProtein
	 */
	public static void updateProteinMap(CyNetwork myNetwork, CyNode node, Protein myProtein) {
		String network_name = myNetwork.toString();

		if (Util.proteinsMap.containsKey(network_name)) {

			List<Protein> all_proteins = Util.proteinsMap.get(network_name);
			Optional<Protein> isPtnPresent = all_proteins.stream().filter(value -> value.gene.equals(myProtein.gene))
					.findFirst();
			if (isPtnPresent.isPresent()) {
				Protein ptn = isPtnPresent.get();
				ptn.checksum = myProtein.checksum;
				ptn.domains = myProtein.domains;
				ptn.fullName = myProtein.fullName;
				ptn.gene = myProtein.gene;
				ptn.pdbIds = myProtein.pdbIds;
				ptn.proteinID = myProtein.proteinID;
				ptn.sequence = myProtein.sequence;
				ptn.location = myProtein.location;
				ptn.predicted_domain_epoch = myProtein.predicted_domain_epoch;
				ptn.isValid = myProtein.isValid;
			} else {
				all_proteins.add(myProtein);
			}

		} else {// Network does not exists

			List<Protein> proteins = new ArrayList<Protein>();
			proteins.add(myProtein);
			Util.proteinsMap.put(network_name, proteins);
		}

		Util.updateProteinDomainsColorMap(
				myProtein.domains.stream().filter(value -> value.isValid).collect(Collectors.toList()));

	}

}
