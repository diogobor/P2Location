package de.fmp.liulab.task;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.table.DefaultTableModel;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.view.model.VisualLexicon;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;

import de.fmp.liulab.internal.UpdateViewListener;
import de.fmp.liulab.internal.view.JFrameWithoutMaxAndMinButton;
import de.fmp.liulab.internal.view.MenuBar;
import de.fmp.liulab.model.CrossLink;
import de.fmp.liulab.model.Protein;
import de.fmp.liulab.model.ProteinDomain;
import de.fmp.liulab.model.Residue;
import de.fmp.liulab.utils.Util;

/**
 * Class responsible for loading domains of a set of proteins
 * 
 * @author borges.diogo
 *
 */
public class ProcessProteinLocationTask extends AbstractTask implements ActionListener {

	private CyApplicationManager cyApplicationManager;
	private static CyNetwork myNetwork;
	public static VisualLexicon lexicon;
	public static VisualStyle style;
	private CyNetworkFactory myNetFactory;
	public static boolean hasMoreResidueToBePredicted;

	// Window
	private JFrameWithoutMaxAndMinButton mainFrame;
	private JPanel mainPanel;
	private static JLabel textLabel_status_result;
	private MenuBar menuBar = new MenuBar();
	private JPanel information_panel;

	// Table
	private static JTable mainProteinDomainTable;
	public static DefaultTableModel tableDataModel;
	private static String[] columnNames = { "Node Name", "Description", "Sequence", "Topological Domain(s)",
			"Subcellular location" };
	@SuppressWarnings("rawtypes")
	private final Class[] columnClass = new Class[] { String.class, String.class, String.class, String.class,
			String.class };
	private String rowstring, value;
	private Clipboard clipboard;
	private StringSelection stsel;
	@SuppressWarnings("rawtypes")
	private static JList rowHeader;
	private static JScrollPane proteinDomainTableScrollPanel;

	private static boolean isPfamLoaded = true;
	private static boolean pfamDoStop = false;
	private static boolean isPtnSequenceLoaded = true;
	private static boolean ptnSequenceDoStop = false;
	private static Thread pfamThread;
	private static JButton proteinDomainServerButton;
	private static JButton proteinSequenceServerButton;
	private static Thread ptnSequenceThread;

	private static JButton okButton;
	private static Thread storeDomainThread;
	private static boolean isStoredDomains = false;

	public static boolean isPlotDone = false;

	public static Thread disposeMainJFrameThread;

	private final static String TRANSMEMBRANE = "transmem";
	private final static String UNKNOWN_DOMAIN = "###";
	private final static String UNKNOWN_PREDICTED_DOMAIN = "Unknown";
	private final static String INNER_MEMBRANE = "IMM";
	private final static String OUTER_MEMBRANE = "OMM";
	private final static String INTERMEMBRANE = "intermembrane";
	private final static String CYTOSOL = "cytoplasmic";
	private final static String MATRIX = "matrix";
	public final static String UNKNOWN_RESIDUE = "UK";
	private boolean predictLocation = false;
	private boolean updateAnnotationDomain = false;

	// Map<domain name, residues>
	public static HashMap<String, List<Residue>> compartments = new HashMap<String, List<Residue>>();
	private static List<Residue> all_unknownResidues;
	private static List<Residue> all_knownResidues;
	public static int epochs = 1;
	public static HashMap<Integer, Integer> number_unknown_residues = new HashMap<Integer, Integer>();

	// Map<Protein - Node SUID, Protein
	private static Map<Long, Protein> proteinsMap = new HashMap<Long, Protein>();

	// OMM / IMM prediction
	private static String predicted_protein_domain_name;

	// It is a new prediction (true) or the method was called by pressing 'continue'
	// button (false)
	private boolean isNewPrediction;

	/**
	 * /** Constructor
	 * 
	 * @param cyApplicationManager main app manager
	 * @param vmmServiceRef        visual mapping manager
	 * @param vgFactory            graphic factory
	 */
	@SuppressWarnings("static-access")
	public ProcessProteinLocationTask(CyApplicationManager cyApplicationManager, CyNetworkFactory netFactory,
			final VisualMappingManager vmmServiceRef, boolean predictLocation, boolean updateAnnotationDomain,
			boolean isNewPrediction) {

		this.predictLocation = predictLocation;
		this.updateAnnotationDomain = updateAnnotationDomain;
		this.menuBar.domain_ptm_or_monolink = 0;
		this.cyApplicationManager = cyApplicationManager;
		this.myNetwork = cyApplicationManager.getCurrentNetwork();
		this.style = vmmServiceRef.getCurrentVisualStyle();
		// Get the current Visual Lexicon
		this.lexicon = cyApplicationManager.getCurrentRenderingEngine().getVisualLexicon();
		this.isNewPrediction = isNewPrediction;
		this.myNetFactory = netFactory;

		proteinsMap = new HashMap<Long, Protein>();

		if (!predictLocation) {
			createFrame();
		}
	}

	private void createFrame() {

		if (mainFrame == null)
			mainFrame = new JFrameWithoutMaxAndMinButton(new JFrame(), "P2Location - Load protein domains", 1);

		mainFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		Dimension appSize = null;
		if (Util.isWindows()) {
			appSize = new Dimension(640, 375);
		} else if (Util.isMac()) {
			appSize = new Dimension(625, 355);
		} else {
			appSize = new Dimension(625, 365);
		}
		mainFrame.setSize(appSize);
		mainFrame.setResizable(false);

		if (mainPanel == null)
			mainPanel = new JPanel();
		mainPanel.setBounds(10, 10, 590, 365);
		mainPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), ""));
		mainPanel.setLayout(null);

		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		mainFrame.setLocation((screenSize.width - appSize.width) / 2, (screenSize.height - appSize.height) / 2);
		mainFrame.setJMenuBar(menuBar.getMenuBar());
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
	 * Method responsible for running task
	 */
	@Override
	public void run(TaskMonitor taskMonitor) throws Exception {

		if (!predictLocation) {
			taskMonitor.setTitle("P2Location - Load protein domains task");

			if (cyApplicationManager.getCurrentNetwork() == null) {
				throw new Exception("ERROR: No networks has been loaded.");
			}

			setFrameObjects(taskMonitor);

			// Display the window
			mainFrame.add(mainPanel, BorderLayout.CENTER);
			mainFrame.setLocationRelativeTo(null);
			mainFrame.setVisible(true);

		} else if (!updateAnnotationDomain) {
			taskMonitor.setTitle("P2Location - Predict residue location");
			processLocation(taskMonitor);
		} else {

			if (myNetwork == null)
				return;

			taskMonitor.setTitle("P2Location - Update epoch");

			if (Util.mapNodeTable.containsKey(myNetwork.toString())) {
				Map<Integer, CyTable> epoch_nodeTable = Util.mapNodeTable.get(myNetwork.toString());

				if (epoch_nodeTable.containsKey(epochs)) {

					CyTable nodeTable = epoch_nodeTable.get(epochs);

					restoreProteinInformation(taskMonitor, myNetwork);
					ProteinScalingFactorHorizontalExpansionTableTask.updateNodeTable(taskMonitor, nodeTable,
							myNetwork.toString());
					checkTransmemValidity();
					Util.updateProteins(taskMonitor, myNetwork, null, false, false);

					// It's necessary to put epochs++ because OrganizeResidueCompartment has already
					// done for epochs++
					epochs++;

					// Remove all unknown residues from other epochs

					int total = number_unknown_residues.size();
					for (int i = epochs; i <= total; i++) {

						if (number_unknown_residues.containsKey(i)) {
							number_unknown_residues.remove(i);
						}
					}
				}
			}
		}
	}

	/**
	 * Set all labels in P2Location window / frame
	 */
	private void initFrameLabels() {

		int offset_y = 0;

		information_panel = new JPanel();
		information_panel.setBorder(BorderFactory.createTitledBorder(""));
		information_panel.setBounds(10, 8, 455, 146);
		information_panel.setLayout(null);
		mainPanel.add(information_panel);

		JLabel textLabel_Protein_lbl_1 = null;
		if (Util.isUnix())
			textLabel_Protein_lbl_1 = new JLabel("Fill in the table below to indicate what proteins will");
		else
			textLabel_Protein_lbl_1 = new JLabel(
					"Fill in the table below to indicate what proteins will have their domains");
		textLabel_Protein_lbl_1.setFont(new java.awt.Font("Tahoma", Font.PLAIN, 12));
		textLabel_Protein_lbl_1.setBounds(10, offset_y, 450, 40);
		information_panel.add(textLabel_Protein_lbl_1);
		offset_y += 20;

		JLabel textLabel_Protein_lbl_2 = null;
		if (Util.isUnix()) {
			textLabel_Protein_lbl_2 = new JLabel("have their domains loaded.");
			textLabel_Protein_lbl_2.setBounds(10, offset_y, 250, 40);
		} else {
			textLabel_Protein_lbl_2 = new JLabel("loaded.");
			textLabel_Protein_lbl_2.setBounds(10, offset_y, 100, 40);
		}
		textLabel_Protein_lbl_2.setFont(new java.awt.Font("Tahoma", Font.PLAIN, 12));
		information_panel.add(textLabel_Protein_lbl_2);
		offset_y += 30;

		JLabel textLabel_Pfam = new JLabel("Search for domains:");
		textLabel_Pfam.setFont(new java.awt.Font("Tahoma", Font.PLAIN, 12));
		textLabel_Pfam.setBounds(10, offset_y, 150, 40);
		information_panel.add(textLabel_Pfam);
		offset_y += 30;

		JLabel textLabel_ptn_sequence = new JLabel("Retrieve protein sequence(s):");
		textLabel_ptn_sequence.setFont(new java.awt.Font("Tahoma", Font.PLAIN, 12));
		if (Util.isWindows())
			textLabel_ptn_sequence.setBounds(10, offset_y, 170, 40);
		else
			textLabel_ptn_sequence.setBounds(10, offset_y, 160, 40);
		information_panel.add(textLabel_ptn_sequence);

		offset_y -= 20;
		JRadioButton protein_domain_pfam = new JRadioButton("Pfam");
		protein_domain_pfam.setSelected(Util.isProteinDomainPfam);
		if (Util.isWindows()) {
			protein_domain_pfam.setBounds(179, offset_y, 50, 20);
		} else if (Util.isMac()) {
			protein_domain_pfam.setBounds(203, offset_y, 65, 20);
		} else {
			protein_domain_pfam.setBounds(228, offset_y, 65, 20);
		}
		protein_domain_pfam.addItemListener(new ItemListener() {

			@Override
			public void itemStateChanged(ItemEvent event) {
				int state = event.getStateChange();
				if (state == ItemEvent.SELECTED) {

					Util.isProteinDomainPfam = true;
				} else if (state == ItemEvent.DESELECTED) {

					Util.isProteinDomainPfam = false;
				}
			}
		});
		information_panel.add(protein_domain_pfam);

		JRadioButton protein_domain_supfam = new JRadioButton("Supfam");
		protein_domain_supfam.setSelected(!Util.isProteinDomainPfam);
		if (Util.isWindows()) {
			protein_domain_supfam.setBounds(119, offset_y, 64, 20);
		} else if (Util.isMac()) {
			protein_domain_supfam.setBounds(119, offset_y, 79, 20);
		} else {
			protein_domain_supfam.setBounds(149, offset_y, 79, 20);
		}
		protein_domain_supfam.addItemListener(new ItemListener() {

			@Override
			public void itemStateChanged(ItemEvent event) {
				int state = event.getStateChange();
				if (state == ItemEvent.SELECTED) {

					Util.isProteinDomainPfam = false;
				} else if (state == ItemEvent.DESELECTED) {

					Util.isProteinDomainPfam = true;
				}
			}
		});
		information_panel.add(protein_domain_supfam);

		ButtonGroup bg_database = new ButtonGroup();
		bg_database.add(protein_domain_pfam);
		bg_database.add(protein_domain_supfam);

		offset_y = 110;

		textLabel_status_result = new JLabel("");
		textLabel_status_result.setFont(new java.awt.Font("Tahoma", Font.PLAIN, 12));
		textLabel_status_result.setForeground(new Color(159, 17, 17));
		if (Util.isUnix())
			textLabel_status_result.setBounds(65, offset_y, 350, 40);
		else
			textLabel_status_result.setBounds(55, offset_y, 350, 40);

		JPanel logo_panel = new JPanel();
		logo_panel.setBorder(BorderFactory.createTitledBorder(""));
		logo_panel.setBounds(470, 8, 140, 146);
		logo_panel.setLayout(null);
		mainPanel.add(logo_panel);

		ImageIcon imageIcon = new javax.swing.ImageIcon(getClass().getResource("/images/logo.png")); // load the image
																										// to a
																										// imageIcon
		Image image = imageIcon.getImage(); // transform it
		Image newimg = image.getScaledInstance(130, 130, java.awt.Image.SCALE_SMOOTH); // scale it the smooth way
		imageIcon = new ImageIcon(newimg); // transform it back

		JLabel jLabelIcon = new JLabel();
		jLabelIcon.setBounds(5, -25, 200, 200);
		jLabelIcon.setIcon(imageIcon);
		logo_panel.add(jLabelIcon);

		JLabel textLabel_status = new JLabel("Status:");
		textLabel_status.setFont(new java.awt.Font("Tahoma", Font.PLAIN, 12));
		textLabel_status.setBounds(10, offset_y, 50, 40);
		information_panel.add(textLabel_status);
		information_panel.add(textLabel_status_result);
	}

	/**
	 * Method responsible for initializing the table in the Frame
	 */
	private void initTableScreen() {

		Object[][] data = new Object[1][columnNames.length];
		// create table model with data
		tableDataModel = new DefaultTableModel(data, columnNames) {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isCellEditable(int row, int column) {
				return true;
			}

			@Override
			public Class<?> getColumnClass(int columnIndex) {
				return columnClass[columnIndex];
			}
		};

		mainProteinDomainTable = new JTable(tableDataModel);
		Action insertLineToTableAction = new AbstractAction("insertLineToTable") {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent evt) {
				tableDataModel.addRow(new Object[] { "" });

				Util.updateRowHeader(tableDataModel.getRowCount(), mainProteinDomainTable, rowHeader,
						proteinDomainTableScrollPanel);
				textLabel_status_result.setText("Row has been inserted.");
			}
		};

		KeyStroke keyStrokeInsertLine = KeyStroke.getKeyStroke(KeyEvent.VK_I, InputEvent.CTRL_DOWN_MASK);
		mainProteinDomainTable.getActionMap().put("insertLineToTable", insertLineToTableAction);
		mainProteinDomainTable.getInputMap(JTable.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(keyStrokeInsertLine,
				"insertLineToTable");

		Action deleteLineToTableAction = new AbstractAction("deleteLineToTable") {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent evt) {

				if (mainProteinDomainTable.getSelectedRow() != -1) {

					int input = JOptionPane.showConfirmDialog(null, "Do you confirm the removal of the line "
							+ (mainProteinDomainTable.getSelectedRow() + 1) + "?");
					// 0=yes, 1=no, 2=cancel
					if (input == 0) {
						// remove selected row from the model
						tableDataModel.removeRow(mainProteinDomainTable.getSelectedRow());
						Util.updateRowHeader(tableDataModel.getRowCount(), mainProteinDomainTable, rowHeader,
								proteinDomainTableScrollPanel);
					}
				}

				textLabel_status_result.setText("Row has been deleted.");
			}
		};

		KeyStroke keyStrokeDeleteLine = KeyStroke.getKeyStroke(KeyEvent.VK_D, InputEvent.CTRL_DOWN_MASK);
		mainProteinDomainTable.getActionMap().put("deleteLineToTable", deleteLineToTableAction);
		mainProteinDomainTable.getInputMap(JTable.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(keyStrokeDeleteLine,
				"deleteLineToTable");

		final KeyStroke copy = KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK, false);
		// Identifying the copy KeyStroke user can modify this
		// to copy on some other Key combination.
		final KeyStroke paste = KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_DOWN_MASK, false);
		// Identifying the Paste KeyStroke user can modify this
		// to copy on some other Key combination.
		mainProteinDomainTable.registerKeyboardAction(this, "Copy", copy, JComponent.WHEN_FOCUSED);
		mainProteinDomainTable.registerKeyboardAction(this, "Paste", paste, JComponent.WHEN_FOCUSED);
		clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();

		// Create the scroll pane and add the table to it.
		proteinDomainTableScrollPanel = new JScrollPane();
		proteinDomainTableScrollPanel.setBounds(10, 160, 600, 105);
		proteinDomainTableScrollPanel.setViewportView(mainProteinDomainTable);
		proteinDomainTableScrollPanel.setRowHeaderView(rowHeader);
		setTableProperties(1);
		mainPanel.add(proteinDomainTableScrollPanel);
	}

	/**
	 * Method responsible for getting protein domains from Pfam or Supfam database
	 * for each node
	 * 
	 * @param taskMonitor
	 */
	private static String getPfamOrSupfamProteinDomainsForeachNode(CyNetwork myNetwork, final TaskMonitor taskMonitor) {

		StringBuilder sb_error = new StringBuilder();
		if (myNetwork == null)
			sb_error.append("ERROR: No network has been found.");

		int old_progress = 0;
		int summary_processed = 0;
		int total_genes = proteinsMap.size();

		CyRow myCurrentRow = null;
		for (Map.Entry<Long, Protein> entry : proteinsMap.entrySet()) {

			if (pfamDoStop)
				break;

			Long key = entry.getKey();
			String node_name = myNetwork.getDefaultNodeTable().getRow(key).getRaw(CyNetwork.NAME).toString();

			CyNode node = Util.getNode(myNetwork, key);
			if (node != null) {

				myCurrentRow = myNetwork.getRow(node);

				List<ProteinDomain> new_proteinDomains = Util.getProteinDomainsFromServer(myCurrentRow, taskMonitor);

				if (new_proteinDomains.size() > 0) {
					Protein ptn = entry.getValue();
					ptn.domains = new_proteinDomains;
					entry.setValue(ptn);
				}
			} else {
				sb_error.append("ERROR: Node " + node_name + " has not been found.\n");
			}

			summary_processed++;
			int new_progress = (int) ((double) summary_processed / (total_genes) * 100);
			if (new_progress > old_progress) {
				old_progress = new_progress;

				if (textLabel_status_result != null) {
					textLabel_status_result.setText("Getting protein domains: " + old_progress + "%");
				}
				taskMonitor.showMessage(TaskMonitor.Level.INFO, "Getting protein domains: " + old_progress + "%");
			}
		}
		if (sb_error.toString().isBlank() || sb_error.toString().isEmpty()) {

			taskMonitor.showMessage(TaskMonitor.Level.INFO, "Done!");
			if (textLabel_status_result != null) {
				textLabel_status_result.setText("Done!");
			}
		}

		return sb_error.toString();
	}

	/**
	 * Method responsible for initializing all button in the Frame
	 * 
	 * @param taskMonitor
	 */
	private void initButtons(final TaskMonitor taskMonitor) {

		Icon iconBtn = new ImageIcon(getClass().getResource("/images/browse_Icon.png"));
		proteinDomainServerButton = new JButton(iconBtn);
		if (Util.isWindows())
			proteinDomainServerButton.setBounds(250, 55, 30, 30);
		else if (Util.isMac())
			proteinDomainServerButton.setBounds(280, 55, 30, 30);
		else
			proteinDomainServerButton.setBounds(295, 55, 30, 30);

		proteinDomainServerButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
		proteinDomainServerButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				taskMonitor.setTitle("Load protein domains...");

				if (isPfamLoaded) {
					try {

						isPlotDone = false;
						textLabel_status_result.setText("Getting protein domains...");
						taskMonitor.showMessage(TaskMonitor.Level.INFO, "Getting protein domains...");
						String msgError = getNodesFromTable(myNetwork, true, false);
						if (!msgError.isBlank() && !msgError.isEmpty()) {
							textLabel_status_result.setText("ERROR: Check Task History.");
							taskMonitor.showMessage(TaskMonitor.Level.ERROR, msgError);
						} else {

							if (Util.isProteinDomainPfam) {
								textLabel_status_result.setText("Accessing Pfam database...");
								taskMonitor.showMessage(TaskMonitor.Level.INFO, "Accessing Pfam database...");
							} else {
								textLabel_status_result.setText("Accessing Supfam database...");
								taskMonitor.showMessage(TaskMonitor.Level.INFO, "Accessing Supfam database...");
							}
							getProteinDomainsFromServer(taskMonitor, myNetwork, true);

						}
					} catch (Exception exception) {
					}
				} else {
					JOptionPane.showMessageDialog(null, "Wait! There is another process in progress!",
							"P2Location - Protein domains", JOptionPane.WARNING_MESSAGE);
				}
			}
		});
		information_panel.add(proteinDomainServerButton);

		proteinSequenceServerButton = new JButton(iconBtn);
		if (Util.isWindows())
			proteinSequenceServerButton.setBounds(180, 85, 30, 30);
		else if (Util.isMac())
			proteinSequenceServerButton.setBounds(179, 85, 30, 30);
		else
			proteinSequenceServerButton.setBounds(204, 85, 30, 30);

		proteinSequenceServerButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
		proteinSequenceServerButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				taskMonitor.setTitle("Protein domains");

				if (isPtnSequenceLoaded) {
					try {

						isPlotDone = false;
						textLabel_status_result.setText("Getting protein sequences...");
						taskMonitor.showMessage(TaskMonitor.Level.INFO, "Getting protein sequences...");
						String msgError = getNodesFromTable(myNetwork, true, false);
						if (!msgError.isBlank() && !msgError.isEmpty()) {
							textLabel_status_result.setText("ERROR: Check Task History.");
							taskMonitor.showMessage(TaskMonitor.Level.ERROR, msgError);
						} else {

							textLabel_status_result.setText("Accessing Uniprot database...");
							taskMonitor.showMessage(TaskMonitor.Level.INFO, "Accessing Uniprot database...");
							getProteinSequencesFromServer(taskMonitor, myNetwork, true);

						}

					} catch (Exception exception) {
					}
				} else {
					JOptionPane.showMessageDialog(null, "Wait! There is another process in progress!",
							"P2Location - Protein location", JOptionPane.WARNING_MESSAGE);
				}
			}
		});
		information_panel.add(proteinSequenceServerButton);

		Icon iconBtnOk = new ImageIcon(getClass().getResource("/images/okBtn.png"));
		okButton = new JButton(iconBtnOk);
		okButton.setText("OK");
		if (Util.isWindows())
			okButton.setBounds(80, 280, 220, 25);
		else
			okButton.setBounds(80, 270, 220, 25);

		okButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent ae) {

				boolean concluedProcess = true;
				try {

					if (!isPfamLoaded) {
						int input = JOptionPane.showConfirmDialog(null,
								"Pfam process has not been finished yet. Do you want to close this window?",
								"P2Location - Protein domains", JOptionPane.INFORMATION_MESSAGE);
						// 0=yes, 1=no, 2=cancel
						if (input == 0) {
							concluedProcess = true;
							if (pfamThread != null) {
								pfamDoStop = true;
								pfamThread.interrupt();
							}
						} else {
							concluedProcess = false;
							pfamDoStop = false;
						}
					}

					if (concluedProcess) {

						okButton.setEnabled(false);
						proteinDomainServerButton.setEnabled(false);
						proteinSequenceServerButton.setEnabled(false);

						storeProteins(taskMonitor, myNetwork, true);

					}

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
			cancelButton.setBounds(315, 280, 220, 25);
		else
			cancelButton.setBounds(315, 270, 220, 25);

		cancelButton.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if (cancelProcess())
					mainFrame.dispose();
			}
		});
		mainPanel.add(cancelButton);
	}

	/**
	 * Get protein domains from server
	 * 
	 * @param taskMonitor task monitor
	 */
	public void getProteinSequencesFromServer(TaskMonitor taskMonitor, CyNetwork myNetwork, boolean fromScreen) {

		ptnSequenceThread = new Thread() {
			public synchronized void run() {
				ptnSequenceDoStop = false;
				isPtnSequenceLoaded = false;

				String msgError = getProteinSequenceForeachNode(myNetwork, taskMonitor);
				if (!msgError.isBlank() && !msgError.isEmpty()) {
					taskMonitor.showMessage(TaskMonitor.Level.ERROR, msgError);
					if (textLabel_status_result != null)
						textLabel_status_result.setText("ERROR: Check Task History.");
					isPtnSequenceLoaded = true;
				} else {
					Object[][] data = null;
					if (proteinsMap.size() > 0)
						data = new Object[proteinsMap.size()][columnNames.length];
					else
						data = new Object[1][columnNames.length];

					tableDataModel.setDataVector(data, columnNames);
					int countPtnDomain = 0;

					for (Map.Entry<Long, Protein> entry : proteinsMap.entrySet()) {
						Long nodeKey = entry.getKey();
						if (nodeKey == null)
							continue;

						Protein protein = entry.getValue();

						tableDataModel.setValueAt(protein.gene, countPtnDomain, 0);
						tableDataModel.setValueAt(protein.fullName, countPtnDomain, 1);
						tableDataModel.setValueAt(protein.sequence, countPtnDomain, 2);
						tableDataModel.setValueAt(ToStringProteinDomains(protein.domains), countPtnDomain, 3);
						countPtnDomain++;
					}

					if (proteinsMap.size() > 0)
						setTableProperties(proteinsMap.size());
					else
						setTableProperties(1);
					isPtnSequenceLoaded = true;

					// It's called via command line
//					if (!fromScreen) {
//						try {
//							storeMonolinks(taskMonitor, myNetwork, false);
//						} catch (Exception e) {
//							taskMonitor.showMessage(TaskMonitor.Level.ERROR, "ERROR: " + e.getMessage());
//						}
//					}
				}
			}
		};

		ptnSequenceThread.start();
	}

	/**
	 * Method responsible for getting protein sequences from Uniprot database for
	 * each node
	 * 
	 * @param taskMonitor
	 */
	private static String getProteinSequenceForeachNode(CyNetwork myNetwork, final TaskMonitor taskMonitor) {

		StringBuilder sb_error = new StringBuilder();
		if (myNetwork == null)
			sb_error.append("ERROR: No network has been found.");

		int old_progress = 0;
		int summary_processed = 0;
		int total_genes = proteinsMap.size();

		CyRow myCurrentRow = null;

		for (Map.Entry<Long, Protein> entry : proteinsMap.entrySet()) {

			if (ptnSequenceDoStop)
				break;

			Long key = entry.getKey();
			String node_name = myNetwork.getDefaultNodeTable().getRow(key).getRaw(CyNetwork.NAME).toString();

			CyNode node = Util.getNode(myNetwork, key);
			if (node != null) {

				myCurrentRow = myNetwork.getRow(node);

				String sequence = Util.getProteinSequenceFromUniprot(myCurrentRow);
				String fullName = Util.getProteinDescriptionFromUniprot(myCurrentRow);
				Protein ptn = entry.getValue();
				ptn.sequence = sequence;
				ptn.fullName = fullName;
				entry.setValue(ptn);
			} else {
				sb_error.append("ERROR: Node " + node_name + " has not been found.\n");
			}
			summary_processed++;
			int new_progress = (int) ((double) summary_processed / (total_genes) * 100);
			if (new_progress > old_progress) {
				old_progress = new_progress;

				if (textLabel_status_result != null) {
					textLabel_status_result.setText("Getting protein sequence(s): " + old_progress + "%");
				}
				taskMonitor.showMessage(TaskMonitor.Level.INFO, "Getting protein sequence(s): " + old_progress + "%");
			}

		}

		if (sb_error.toString().isBlank() || sb_error.toString().isEmpty()) {

			taskMonitor.showMessage(TaskMonitor.Level.INFO, "Done!");
			if (textLabel_status_result != null) {
				textLabel_status_result.setText("Done!");
			}
		}

		return sb_error.toString();
	}

	/**
	 * Get protein domains from server
	 * 
	 * @param taskMonitor task monitor
	 */
	public static void getProteinDomainsFromServer(TaskMonitor taskMonitor, CyNetwork myNetwork, boolean fromScreen) {

		pfamThread = new Thread() {
			public synchronized void run() {
				pfamDoStop = false;
				isPfamLoaded = false;

				String msgError = getPfamOrSupfamProteinDomainsForeachNode(myNetwork, taskMonitor);
				if (!msgError.isBlank() && !msgError.isEmpty()) {
					taskMonitor.showMessage(TaskMonitor.Level.ERROR, msgError);
					if (textLabel_status_result != null)
						textLabel_status_result.setText("ERROR: Check Task History.");
					isPfamLoaded = true;
				} else {
					Object[][] data = null;
					if (proteinsMap.size() > 0)
						data = new Object[proteinsMap.size()][columnNames.length];
					else
						data = new Object[1][columnNames.length];

					tableDataModel.setDataVector(data, columnNames);
					int countPtnDomain = 0;

					for (Map.Entry<Long, Protein> entry : proteinsMap.entrySet()) {
						Long nodeKey = entry.getKey();
						if (nodeKey == null)
							continue;

						Protein protein = entry.getValue();

						tableDataModel.setValueAt(protein.gene, countPtnDomain, 0);
						tableDataModel.setValueAt(protein.sequence, countPtnDomain, 1);
						tableDataModel.setValueAt(ToStringProteinDomains(protein.domains), countPtnDomain, 2);
						countPtnDomain++;
					}

					if (proteinsMap.size() > 0)
						setTableProperties(proteinsMap.size());
					else
						setTableProperties(1);
					isPfamLoaded = true;

					// It's called via command line
					if (!fromScreen) {
						try {
							storeProteins(taskMonitor, myNetwork, false);
						} catch (Exception e) {
							taskMonitor.showMessage(TaskMonitor.Level.ERROR, "ERROR: " + e.getMessage());
						}
					}
				}
			}
		};

		pfamThread.start();
	}

	/**
	 * Method responsible for storing protein domains
	 * 
	 * @param taskMonitor
	 * @throws Exception
	 */
	public static void storeProteins(TaskMonitor taskMonitor, final CyNetwork myNetwork, boolean isFromScreen)
			throws Exception {

		if (myNetwork == null) {
			throw new Exception("ERROR: No network has been found.");
		}

		storeDomainThread = new Thread() {

			public synchronized void run() {

				isPlotDone = false;
				if (isFromScreen)
					textLabel_status_result.setText("Checking nodes ...");
				taskMonitor.showMessage(TaskMonitor.Level.INFO, "Checking nodes...");
				String msgError = getNodesFromTable(myNetwork, !isFromScreen, true);
				if (!msgError.isBlank() && !msgError.isEmpty() && msgError.contains("ERROR:")) {
					textLabel_status_result.setText("ERROR: Check Task History.");
					taskMonitor.showMessage(TaskMonitor.Level.ERROR, msgError);

					isPlotDone = true;
					UpdateViewListener.isNodeModified = true;
					isStoredDomains = true;

				} else {

					// Show msg with all ptns that have not been found
					if (!msgError.isBlank() && !msgError.isEmpty())
						taskMonitor.showMessage(TaskMonitor.Level.WARN, msgError);

					isStoredDomains = false;
					if (isFromScreen)
						textLabel_status_result.setText("Setting nodes information...");
					taskMonitor.showMessage(TaskMonitor.Level.INFO, "Setting nodes information...");
					try {

						msgError = setNodesInformation(taskMonitor, myNetwork);
						if (!msgError.isBlank() && !msgError.isEmpty()) {
							textLabel_status_result.setText("WARNING: Check Task History.");
							taskMonitor.showMessage(TaskMonitor.Level.WARN, msgError);
						}

						if (Util.proteinsMap.size() > 0) {

							// Get cross-links
							Util.getXLsAllProteins(taskMonitor, myNetwork, textLabel_status_result);
							Util.updateAllXLLocationBasedOnProteinDomains(taskMonitor, myNetwork,
									textLabel_status_result);

							// Check conflict among stored residues
							if (Util.considerConflict) {
								epochs = 1;
								OrganizeResidueCompartment(taskMonitor);
								all_knownResidues = getAllKnownResidues();
								computeResiduesScoreForAllProteins(taskMonitor);
								computeNewResidues(taskMonitor, all_knownResidues, true);
								epochs = 1;
								// TODO: TEMP
//								printResidueScore();
								// TODO: TEMP
							}
							Util.updateProteins(taskMonitor, myNetwork, textLabel_status_result, false, false);
						}

						taskMonitor.setProgress(1.0);
						taskMonitor.showMessage(TaskMonitor.Level.INFO,
								"Protein domains have been loaded successfully!");

						if (isFromScreen) {
							textLabel_status_result.setText("Done!");

							okButton.setEnabled(true);
							proteinDomainServerButton.setEnabled(true);
							proteinSequenceServerButton.setEnabled(true);

							disposeMainJFrameThread.start();
						}

					} catch (Exception e) {
						if (isFromScreen)
							textLabel_status_result.setText("ERROR: Check Task History.");
						taskMonitor.showMessage(TaskMonitor.Level.ERROR, "ERROR: " + e.getMessage());
					}

					isPlotDone = true;
					UpdateViewListener.isNodeModified = true;
					isStoredDomains = true;
				}
			}
		};

		storeDomainThread.start();
	}

	private static void printResidueScore() {

		List<Protein> allProteins = Util.proteinsMap.get(myNetwork.toString());

		if (allProteins == null || allProteins.size() == 0)
			return;

		int count = 0;
		try {
			for (Protein protein : allProteins) {

				if (protein.domains == null || protein.domains.size() == 0)
					continue;

				for (Residue res : protein.reactionSites) {

					if (res.location == null || res.location.toLowerCase().equals(TRANSMEMBRANE) || res.score == 150.0
							|| res.conflicted_score == 150.00 || res.score == -1 || res.conflicted_score == -1)
						continue;
					if (res.isConflicted)
						System.out.println("###" + res.conflicted_score);
					else {
						try {
							if (-Math.log(res.score) < 6.8)
								System.out.println(res.score);
						} catch (Exception e) {
							System.out.println();
						}

					}
					count++;
				}

			}
		} catch (Exception e) {
			System.out.println(count);
		}

	}

	/**
	 * Method responsible for canceling the loading process
	 * 
	 * @return true if process is canceled, otherwise, returns false.
	 */
	public static boolean cancelProcess() {

		boolean concluedProcess = true;

		if (!isPtnSequenceLoaded) {
			int input = JOptionPane.showConfirmDialog(null,
					"Sequence process has not been finished yet. Do you want to close this window?",
					"P2Location - Protein domains", JOptionPane.INFORMATION_MESSAGE);
			// 0=yes, 1=no, 2=cancel
			if (input == 0) {
				concluedProcess = true;
				if (ptnSequenceThread != null) {
					ptnSequenceDoStop = true;
					ptnSequenceThread.interrupt();
				}
			} else {
				concluedProcess = false;
				ptnSequenceDoStop = false;
			}
		}

		if (!isPfamLoaded) {
			int input = JOptionPane.showConfirmDialog(null,
					"Supfam/Pfam process has not been finished yet. Do you want to close this window?",
					"P2Location - Protein domains", JOptionPane.INFORMATION_MESSAGE);
			// 0=yes, 1=no, 2=cancel
			if (input == 0) {
				concluedProcess = true;
				if (pfamThread != null) {
					pfamDoStop = true;
					pfamThread.interrupt();
				}
			} else {
				concluedProcess = false;
				pfamDoStop = false;
			}
		}

		if (!okButton.isEnabled() && !isStoredDomains) {
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
	 * Method responsible for converting ProteinDomain collection into string
	 * 
	 * @param proteinDomains
	 * @return
	 */
	private static String ToStringProteinDomains(List<ProteinDomain> proteinDomains) {

		if (proteinDomains == null || proteinDomains.size() == 0) {
			return "";
		}

		StringBuilder sb_proteinDomains = new StringBuilder();
		for (ProteinDomain ptn_domain : proteinDomains) {
			sb_proteinDomains.append(ptn_domain.name + "[" + ptn_domain.startId + "-" + ptn_domain.endId + "],");
		}
		return sb_proteinDomains.toString().substring(0, sb_proteinDomains.toString().length() - 1);
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
				addReactionSites(ptn, true);
			} else {
				addReactionSites(myProtein, true);
				all_proteins.add(myProtein);
			}

		} else {// Network does not exists

			List<Protein> proteins = new ArrayList<Protein>();
			addReactionSites(myProtein, true);
			proteins.add(myProtein);
			Util.proteinsMap.put(network_name, proteins);
		}

		Util.updateProteinDomainsColorMap(myProtein.domains);

	}

	/**
	 * Method responsible for updating reaction sites into protein
	 * 
	 * @param protein current protein
	 */
	public static void addReactionSites(Protein protein, boolean isNewDomainSet) {
		// Find out all lysines
		List<Residue> residues = new ArrayList<Residue>();

		// Add N-term residue
		residues.add(new Residue(protein.sequence.charAt(0), "UK", 1, protein));

		for (int index = protein.sequence.indexOf(Util.REACTION_RESIDUE); index >= 0; index = protein.sequence
				.indexOf(Util.REACTION_RESIDUE, index + 1)) {
			residues.add(new Residue(Util.REACTION_RESIDUE, "UK", (index + 1), protein));
		}

		if (!isNewDomainSet && protein.reactionSites != null && protein.reactionSites.size() > 0) {
			residues.addAll(protein.reactionSites);
		}

		residues = residues.stream().distinct().collect(Collectors.toList());
		protein.reactionSites = residues;

		if (isNewDomainSet)
			updateResiduesLocationAndScore(protein);

	}

	/**
	 * Method responsible for restoring reaction sites information
	 * 
	 * @param taskMonitor task monitor
	 * @param myNetwork   current network
	 */
	public void restoreReactionSites(TaskMonitor taskMonitor, CyNetwork myNetwork) {
		if (taskMonitor != null)
			taskMonitor.setTitle("Restoring reaction sites information");

		if (myNetwork == null || Util.proteinsMap == null || Util.proteinsMap.size() == 0)
			return;

		List<Protein> proteinList = Util.getProteins(myNetwork, false);
		if (proteinList == null || proteinList.size() == 0)
			return;

		int old_progress = 0;
		int summary_processed = 0;
		int total_rows = proteinList.size();

		for (final Protein protein : proteinList) {

			// Reset reaction sites
			addReactionSites(protein, true);

			summary_processed++;
			Util.progressBar(summary_processed, old_progress, total_rows, "Restoring reaction sites information: ",
					taskMonitor, null);
		}
	}

	/**
	 * Method responsible for restoring protein information
	 * 
	 * @param taskMonitor task monitor
	 * @param myNetwork   current network
	 */
	public static void restoreProteinInformation(TaskMonitor taskMonitor, CyNetwork myNetwork) {

		if (taskMonitor != null)
			taskMonitor.setTitle("Restoring protein information");

		if (myNetwork == null || Util.proteinsMap == null || Util.proteinsMap.size() == 0)
			return;

		List<Protein> proteinList = Util.getProteins(myNetwork, false);
		if (proteinList == null || proteinList.size() == 0)
			return;

		int old_progress = 0;
		int summary_processed = 0;
		int total_rows = proteinList.size();

		for (final Protein protein : proteinList) {

			if (protein.domains != null) {
				protein.domains = protein.domains.stream().filter(value -> !value.isPredicted)
						.collect(Collectors.toList());
				protein.domains.forEach(value -> value.isValid = true);
			}
			protein.domainScores = null;
			protein.isConflictedDomain = false;
			protein.isPredictedBasedOnTransmemInfo = false;
			protein.isValid = true;
			protein.predicted_domain_epoch = -1;
			// Reset reaction sites
			addReactionSites(protein, true);

			summary_processed++;
			Util.progressBar(summary_processed, old_progress, total_rows, "Updating proteins information: ",
					taskMonitor, null);
		}
	}

	/**
	 * Get all nodes filled out in JTable
	 */
	public static String getNodesFromTable(CyNetwork myNetwork, boolean retrieveAllNodes, boolean isFinalStorage) {

		StringBuilder sbError = new StringBuilder();

		boolean containsData = false;
		int old_progress = 0;
		int summary_processed = 0;
		int total_rows = tableDataModel.getRowCount();

		for (int row = 0; row < tableDataModel.getRowCount(); row++) {
			String gene = tableDataModel.getValueAt(row, 0) != null ? tableDataModel.getValueAt(row, 0).toString() : "";

			List<ProteinDomain> proteinDomains = new ArrayList<ProteinDomain>();
			String domainsStr = tableDataModel.getValueAt(row, 3) != null ? tableDataModel.getValueAt(row, 3).toString()
					: "";
			if (!domainsStr.isBlank() && !domainsStr.isEmpty()) {

				try {
					String[] cols = domainsStr.split(",");
					for (String col : cols) {
						String[] domainsArray = col.split("\\[|\\]");
						String domainName = domainsArray[0].trim();
						String[] colRange = domainsArray[1].split("-");
						int startId = Integer.parseInt(colRange[0]);
						int endId = Integer.parseInt(colRange[1]);

						if (domainName.toLowerCase().equals(TRANSMEMBRANE))
							proteinDomains.add(new ProteinDomain(domainName, startId, endId,
									String.valueOf(Util.initialTransmembraneScore)));
						else
							proteinDomains.add(new ProteinDomain(domainName, startId, endId,
									String.valueOf(Util.initialResidueScore)));
					}
				} catch (Exception e) {
					sbError.append("ERROR: Row: " + (row + 1)
							+ " - Protein domains don't match with the pattern 'name[start_index-end_index]'\n");
				}

				Collections.sort(proteinDomains, new Comparator<ProteinDomain>() {
					@Override
					public int compare(ProteinDomain lhs, ProteinDomain rhs) {
						return lhs.startId > rhs.startId ? 1 : (lhs.startId < rhs.startId) ? -1 : 0;
					}
				});
			}

			String description = tableDataModel.getValueAt(row, 1) != null
					? tableDataModel.getValueAt(row, 1).toString()
					: "";

			String sequence = tableDataModel.getValueAt(row, 2) != null ? tableDataModel.getValueAt(row, 2).toString()
					: "";

			String location = tableDataModel.getValueAt(row, 4) != null ? tableDataModel.getValueAt(row, 4).toString()
					: "";

			if (gene.isEmpty() || gene.isBlank()) {
				sbError.append("ERROR: Row: " + (row + 1) + " - Gene is empty.");
			} else {
				CyNode current_node = Util.getNode(myNetwork, gene);
				if (current_node != null) {

					Protein ptn = proteinsMap.get(current_node.getSUID());
					if (ptn == null) {
						ptn = new Protein(gene, gene, description, sequence, location, proteinDomains);
						proteinsMap.put(current_node.getSUID(), ptn);

					} else {
						if (!(sequence.isBlank() || sequence.isEmpty()))
							ptn.sequence = sequence;
						if (!(location.isBlank() || location.isEmpty()))
							ptn.location = location;
						if (proteinDomains.size() > 0)
							ptn.domains = proteinDomains;
					}
					containsData = true;

				} else {
					sbError.append("WARNING: Row: " + (row + 1) + " - Protein '" + gene + "' has not been found.\n");
				}
			}

			if (isFinalStorage && (sequence.isEmpty() || sequence.isBlank())) {
				sbError.append("ERROR: Row: " + (row + 1) + " - Sequence is empty.");
			}

			summary_processed++;
			int new_progress = (int) ((double) summary_processed / (total_rows) * 100);
			if (new_progress > old_progress) {
				old_progress = new_progress;

				if (textLabel_status_result != null)
					textLabel_status_result.setText("Checking nodes: " + old_progress + "%");
			}
		}

		if (retrieveAllNodes && !containsData && sbError.toString().equals("ERROR: Row: 1 - Gene is empty.")) {
			// No protein is filled in the table. Then, get all proteins

			fillAllNodesInTheTable(myNetwork);
			getNodesFromTable(myNetwork, retrieveAllNodes, isFinalStorage);
			return "";
		}

		if (proteinsMap.size() == 0)
			return "";
		else
			return sbError.toString();
	}

	/**
	 * Get all nodes name and add in the table
	 * 
	 * @param myNetwork current network
	 */
	private static void fillAllNodesInTheTable(CyNetwork myNetwork) {

		StringBuilder sb_data_to_be_stored = new StringBuilder();

		List<CyNode> allNodes = myNetwork.getNodeList();
		for (CyNode cyNode : allNodes) {

			String nodeName = myNetwork.getRow(cyNode).get(CyNetwork.NAME, String.class);

			if (nodeName.contains("Target") || nodeName.contains("Source") || nodeName.contains("PTM"))
				continue;

			sb_data_to_be_stored.append(nodeName).append("\n");

		}

		updateDataModel(sb_data_to_be_stored);
	}

	/**
	 * Update table data model
	 * 
	 * @param sb_data_to_be_stored data
	 */
	public static void updateDataModel(StringBuilder sb_data_to_be_stored) {

		int countPtnDomain = 0;
		String[] data_to_be_stored = sb_data_to_be_stored.toString().split("\n");

		Object[][] data = new Object[data_to_be_stored.length][2];
		String[] columnNames = { "Node Name", "Description", "Sequence", "Topological Domain(s)",
				"Subcellular location" };
		tableDataModel.setDataVector(data, columnNames);

		for (String line : data_to_be_stored) {
			String[] cols_line = line.split("\t");
			tableDataModel.setValueAt(cols_line[0], countPtnDomain, 0);
			countPtnDomain++;
		}

		setTableProperties(countPtnDomain);
	}

	/**
	 * Set properties to the Node domain table
	 * 
	 * @param number_lines total number of lines
	 */
	public static void setTableProperties(int number_lines) {
		if (mainProteinDomainTable != null) {
			mainProteinDomainTable.setPreferredScrollableViewportSize(new Dimension(590, 90));
			mainProteinDomainTable.getColumnModel().getColumn(0).setPreferredWidth(90);
			mainProteinDomainTable.getColumnModel().getColumn(1).setPreferredWidth(100);
			mainProteinDomainTable.getColumnModel().getColumn(2).setPreferredWidth(100);
			mainProteinDomainTable.getColumnModel().getColumn(3).setPreferredWidth(150);
			mainProteinDomainTable.getColumnModel().getColumn(4).setPreferredWidth(150);
			mainProteinDomainTable.setFillsViewportHeight(true);
			mainProteinDomainTable.setAutoCreateRowSorter(true);

			Util.updateRowHeader(number_lines, mainProteinDomainTable, rowHeader, proteinDomainTableScrollPanel);
		}
	}

	/**
	 * Method responsible for activating action.
	 */
	@Override
	public void actionPerformed(ActionEvent e) {

		final String actionCommand = e.getActionCommand();

		if (actionCommand.equals("Copy")) {
			StringBuilder sbf = new StringBuilder();
			// Check to ensure we have selected only a contiguous block of cells.
			final int numcols = mainProteinDomainTable.getSelectedColumnCount();
			final int numrows = mainProteinDomainTable.getSelectedRowCount();
			final int[] rowsselected = mainProteinDomainTable.getSelectedRows();
			final int[] colsselected = mainProteinDomainTable.getSelectedColumns();

			if (!((numrows - 1 == rowsselected[rowsselected.length - 1] - rowsselected[0]
					&& numrows == rowsselected.length)
					&& (numcols - 1 == colsselected[colsselected.length - 1] - colsselected[0]
							&& numcols == colsselected.length))) {
				JOptionPane.showMessageDialog(null, "Invalid Copy Selection", "Invalid Copy Selection",
						JOptionPane.ERROR_MESSAGE);
				return;
			}
			for (int i = 0; i < numrows; i++) {
				for (int j = 0; j < numcols; j++) {
					sbf.append(mainProteinDomainTable.getValueAt(rowsselected[i], colsselected[j]));
					if (j < numcols - 1) {
						sbf.append('\t');
					}
				}
				sbf.append('\n');
			}
			stsel = new StringSelection(sbf.toString());
			clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			clipboard.setContents(stsel, stsel);
		} else if (actionCommand.equals("Paste")) {

			final int startRow = (mainProteinDomainTable.getSelectedRows())[0];
			final int startCol = (mainProteinDomainTable.getSelectedColumns())[0];
			try {
				final String trString = (String) (clipboard.getContents(this).getTransferData(DataFlavor.stringFlavor));
				final StringTokenizer st1 = new StringTokenizer(trString, "\n");

				Object[][] data = new Object[st1.countTokens()][columnNames.length];
				tableDataModel.setDataVector(data, columnNames);

				int i = 0;
				for (i = 0; st1.hasMoreTokens(); i++) {
					rowstring = st1.nextToken();
					StringTokenizer st2 = new StringTokenizer(rowstring, "\t");
					for (int j = 0; st2.hasMoreTokens(); j++) {
						value = (String) st2.nextToken();
						value = value.replaceAll("\r", "").replaceAll("\n", "");
						if (startRow + i < mainProteinDomainTable.getRowCount()
								&& startCol + j < mainProteinDomainTable.getColumnCount()) {
							mainProteinDomainTable.setValueAt(value, startRow + i, startCol + j);
						}
					}
				}

				setTableProperties(i);

			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	/**
	 * Method responsible for getting all know residues
	 * 
	 * @return
	 */
	private static List<Residue> getAllKnownResidues() {

		List<Residue> all_knownResidues = new ArrayList<Residue>();
		for (Map.Entry<String, List<Residue>> compartment : compartments.entrySet()) {

			if (compartment.getKey().equals(UNKNOWN_RESIDUE)
					|| compartment.getKey().toLowerCase().equals(TRANSMEMBRANE))
				continue;

			all_knownResidues.addAll(compartment.getValue());
		}

		return all_knownResidues;
	}

	/**
	 * Method responsible for restoring parameters of residues that contain original
	 * domain
	 * 
	 * @param protein current protein
	 */
	public static void resetParamsOriginalResidues(Protein protein) {
		List<Residue> residues = protein.reactionSites;

		if (residues == null)
			return;

		for (Residue residue : residues) {

			residue.history_residues = null;
			residue.predicted_epoch = -1;
			residue.previous_residue = null;

			residue.score = -1;
			residue.predictedLocation = "UK";
			residue.isConflicted = false;
			residue.conflicted_residue = null;
			residue.conflicted_score = 0.0;
			residue.location = "UK";
		}
	}

	/**
	 * Method responsible for restoring parameters of residues that contain original
	 * domain
	 * 
	 * @param protein current protein
	 */
	public static void restoreParamsOriginalResidues(Protein protein) {
		List<Residue> residues = protein.reactionSites;

		if (residues == null)
			return;

		for (Residue residue : residues) {
			if (residue.predictedLocation.equals("UK"))
				continue;

			residue.history_residues = null;
			residue.predicted_epoch = -1;
			residue.previous_residue = null;

		}
	}

	/**
	 * Method responsible for restoring parameters of residues that contain original
	 * domain
	 * 
	 * @param taskMonitor task monitor
	 */
	private void restoreParamsOriginalResiduesForAllProteins(TaskMonitor taskMonitor) {

		List<Protein> allProteins = Util.getProteins(myNetwork, true);

		if (allProteins == null || allProteins.size() == 0)
			return;

		int old_progress = 0;
		int summary_processed = 0;
		int total_ptns = allProteins.size();

		for (final Protein protein : allProteins) {

			if (protein.predicted_domain_epoch != -1)
				continue;

			restoreParamsOriginalResidues(protein);

			summary_processed++;

			Util.progressBar(summary_processed, old_progress, total_ptns, "Restoring residues parameters: ",
					taskMonitor, null);
		}
	}

	private int getOldNumberUnknownResidues() {
		int old_number_uk_residues = 0;

		if (number_unknown_residues.containsKey(epochs - 1))
			old_number_uk_residues = number_unknown_residues.get(epochs - 1);

		return old_number_uk_residues;
	}

	/**
	 * Method responsible for starting the prediction location process
	 * 
	 * @param taskMonitor current task monitor
	 */
	private void processLocation(TaskMonitor taskMonitor) {

		restoreParamsOriginalResiduesForAllProteins(taskMonitor);

		// Turn valid all transm regions (even those with score < cutoff) because no
		// residues can be predicted in these transm regions
		turnValidAllTransmembraneDomains();

		if (isNewPrediction) {
			epochs = 1;
			number_unknown_residues = new HashMap<Integer, Integer>();
		}
		int old_number_uk_residues = getOldNumberUnknownResidues();
		processRoundLocation(taskMonitor, old_number_uk_residues);

		epochs--;
		Util.updateProteins(taskMonitor, myNetwork, null, false, true);
		CyTable nodeTable = prepareNodeTable(taskMonitor);
		epochs++;

		Util.saveNodeTable(taskMonitor, myNetwork.toString(), nodeTable, epochs - 1);

		// Remove all unknown residues from other epochs

		int total = number_unknown_residues.size();
		for (int i = epochs; i <= total; i++) {

			if (number_unknown_residues.containsKey(i)) {
				number_unknown_residues.remove(i);
			}
		}
	}

	private CyTable prepareNodeTable(TaskMonitor taskMonitor) {

		CyNetwork myNewNetwork = myNetFactory.createNetwork();
		myNewNetwork.getRow(myNewNetwork).set(CyNetwork.NAME, myNetwork.toString());

		CyTable nodeTable = myNewNetwork.getDefaultNodeTable();

		// Create columns in new nodeTable
		ProteinScalingFactorHorizontalExpansionTableTask.updateNodeTable(taskMonitor, nodeTable, myNetwork.toString());

		for (CyRow row : myNetwork.getDefaultNodeTable().getAllRows()) {

			CyNode new_node = myNewNetwork.addNode();

			myNewNetwork.getRow(new_node).set(CyNetwork.NAME, row.getRaw(CyNetwork.NAME));
			myNewNetwork.getRow(new_node).set(CyNetwork.SUID, row.getRaw(CyNetwork.SUID));

			myNewNetwork.getRow(new_node).set(Util.PROTEIN_SCALING_FACTOR_COLUMN_NAME,
					row.getRaw(Util.PROTEIN_SCALING_FACTOR_COLUMN_NAME));

			myNewNetwork.getRow(new_node).set(Util.HORIZONTAL_EXPANSION_COLUMN_NAME,
					row.getRaw(Util.HORIZONTAL_EXPANSION_COLUMN_NAME));

			myNewNetwork.getRow(new_node).set(Util.PROTEIN_DOMAIN_COLUMN, row.getRaw(Util.PROTEIN_DOMAIN_COLUMN));

			myNewNetwork.getRow(new_node).set(Util.PREDICTED_PROTEIN_DOMAIN_COLUMN,
					row.getRaw(Util.PREDICTED_PROTEIN_DOMAIN_COLUMN));

			myNewNetwork.getRow(new_node).set(Util.CONFLICTED_PREDICTED_RESIDUES_COLUMN,
					row.getRaw(Util.CONFLICTED_PREDICTED_RESIDUES_COLUMN));

			myNewNetwork.getRow(new_node).set(Util.CONFLICTED_PROTEIN_DOMAINS_COLUMN,
					row.getRaw(Util.CONFLICTED_PROTEIN_DOMAINS_COLUMN));

			myNewNetwork.getRow(new_node).set(Util.VALID_PROTEINS_COLUMN, row.getRaw(Util.VALID_PROTEINS_COLUMN));

			myNewNetwork.getRow(new_node).set(Util.VALID_DOMAINS_COLUMN, row.getRaw(Util.VALID_DOMAINS_COLUMN));

			myNewNetwork.getRow(new_node).set(Util.PROTEIN_SEQUENCE_COLUMN, row.getRaw(Util.PROTEIN_SEQUENCE_COLUMN));

			myNewNetwork.getRow(new_node).set(Util.PROTEIN_NAME_COLUMN, row.getRaw(Util.PROTEIN_NAME_COLUMN));

			myNewNetwork.getRow(new_node).set(Util.SUBCELLULAR_LOCATION_COLUMN,
					row.getRaw(Util.SUBCELLULAR_LOCATION_COLUMN));

			myNewNetwork.getRow(new_node).set(Util.PROTEIN_DOMAINS_SCORES_COLUMN,
					row.getRaw(Util.PROTEIN_DOMAINS_SCORES_COLUMN));

			myNewNetwork.getRow(new_node).set(Util.PROTEIN_LENGTH_A, row.getRaw(Util.PROTEIN_LENGTH_A));

			myNewNetwork.getRow(new_node).set(Util.PROTEIN_LENGTH_B, row.getRaw(Util.PROTEIN_LENGTH_B));

			myNewNetwork.getRow(new_node).set(Util.PROTEIN_A, row.getRaw(Util.PROTEIN_A));

			myNewNetwork.getRow(new_node).set(Util.PROTEIN_B, row.getRaw(Util.PROTEIN_B));

		}

		Util.updateProteins(taskMonitor, myNewNetwork, null, false, false);
		Util.cleanUnusableNodes(nodeTable);
		return nodeTable;
	}

	private boolean checkUnknownResidues(TaskMonitor taskMonitor, int old_number_uk_residues) {

		int uk_res = number_unknown_residues.get(epochs);
		// It means there is no possibility to predict more residues location
		if (uk_res == old_number_uk_residues)
			return false;

		if (Util.getEpochs && (Util.epochs + 1) <= epochs)
			return false;

		return true;

	}

	/**
	 * Method responsible for checking the conflict domains of a specific protein
	 * 
	 * @param taskMonitor task monitor
	 * @param protein     current protein
	 */
	public static void checkConflictProteinDomains(Protein protein) {

		List<String> unique_domain = new ArrayList<String>();

		boolean isThereTransmem = false;

		isThereTransmem = false;
		protein.isConflictedDomain = false;
		if (protein.domains == null)
			return;

		List<ProteinDomain> onlyValidDomains = protein.domains.stream().filter(value -> value.isValid)
				.collect(Collectors.toList());

		Collections.sort(onlyValidDomains, new Comparator<ProteinDomain>() {
			@Override
			public int compare(ProteinDomain lhs, ProteinDomain rhs) {
				return lhs.startId > rhs.startId ? 1 : (lhs.startId < rhs.startId) ? -1 : 0;
			}
		});

		if (onlyValidDomains.stream().filter(value -> value.name.toLowerCase().equals(TRANSMEMBRANE))
				.collect(Collectors.toList()).size() > 0)
			isThereTransmem = true;

		for (ProteinDomain domain : onlyValidDomains.stream()
				.filter(value -> !value.name.toLowerCase().equals(TRANSMEMBRANE)).collect(Collectors.toList())) {
			unique_domain.add(domain.name);
		}

		unique_domain = unique_domain.stream().distinct().collect(Collectors.toList());
		if (isThereTransmem) {

			unique_domain.clear();

			// There are only Transmembrane domains
			if (onlyValidDomains.size() == 1)
				return;

			// Copy protein domains collection because it will be changed on
			// UnifyResiduesDomains method
			List<ProteinDomain> current_ptn_domain_list = onlyValidDomains.stream().collect(Collectors.toList());

			for (int i = 0; i < current_ptn_domain_list.size() - 1; i++) {

				if (!current_ptn_domain_list.get(i).name.toLowerCase().contains(TRANSMEMBRANE)) {
					if (!current_ptn_domain_list.get(i).name.toLowerCase()
							.equals(current_ptn_domain_list.get(i + 1).name.toLowerCase())
							&& !current_ptn_domain_list.get(i + 1).name.toLowerCase().contains(TRANSMEMBRANE)) {
						if (Util.fixDomainManually) {
							protein.isConflictedDomain = true;
							break;
						}
					} else if (current_ptn_domain_list.get(i).name.toLowerCase()
							.equals(current_ptn_domain_list.get(i + 1).name.toLowerCase())) {

						// [TRANSM | Matrix [epoch 1] | Matrix [epoch 2] ] => it's not a conflict
						if (current_ptn_domain_list.get(i).epoch == current_ptn_domain_list.get(i + 1).epoch) {

							int end_first_domain = current_ptn_domain_list.get(i).endId;
							int start_next_domain = current_ptn_domain_list.get(i + 1).startId;

							// [IMS | T <<< score | IMS] => it's valid
							Optional<ProteinDomain> possible_transm_low_score = protein.domains.stream()
									.filter(value -> value.name.toLowerCase().equals("transmem") && !value.isValid
											&& value.startId > end_first_domain && value.endId < start_next_domain)
									.findFirst();

							if (possible_transm_low_score.isEmpty()) {

								if (Util.fixDomainManually) {
									protein.isConflictedDomain = true;
									break;
								}
							}
						}
					}

					if (current_ptn_domain_list.size() - 2 > i) {
						if (current_ptn_domain_list.get(i).name.toLowerCase()
								.equals(current_ptn_domain_list.get(i + 2).name.toLowerCase())
								&& current_ptn_domain_list.get(i + 1).name.toLowerCase().equals(TRANSMEMBRANE)
								&& !current_ptn_domain_list.get(i).name.equals(UNKNOWN_PREDICTED_DOMAIN)) {
							if (Util.fixDomainManually) {
								protein.isConflictedDomain = true;
								break;
							}
						}
					}
				}
			}
		} else if (unique_domain.size() > 1) {
			if (!Util.dualLocalization_conflict) {
				if (Util.fixDomainManually) {
					protein.isConflictedDomain = true;
					return;
				}
			}
		}

		unique_domain.clear();

	}

	/**
	 * Method responsible for checking the conflict domains of all valid proteins
	 * 
	 * @param taskMonitor current task monitor
	 */
	private void checkConflictProteinsDomains(TaskMonitor taskMonitor) {

		List<Protein> allProteins = Util.getProteins(myNetwork, true);
		if (allProteins == null || allProteins.size() == 0)
			return;

		for (Protein protein : allProteins) {
			checkConflictProteinDomains(protein);
		}
	}

	/**
	 * Method responsible for checking whether there are conflict residues in a
	 * specific protein
	 * 
	 * @param protein current protein
	 * @return true if contains conflict residue
	 */
	public static boolean containsConflictResidue(Protein protein) {

		if (protein != null && protein.reactionSites.stream().filter(value -> value.isConflicted)
				.collect(Collectors.toList()).size() > 0)
			return true;

		return false;

	}

	/**
	 * Method responsible for updating the residue location of a specific protein
	 * 
	 * @param protein current proteinF
	 */
	public static void updateResiduesLocationAndScore(Protein protein) {

		if (protein == null || !protein.isValid)
			return;

		if (protein.domains == null)
			return;

		// Transmembrane regions
		for (ProteinDomain domain : protein.domains.stream()
				.filter(value -> value.name.toLowerCase().equals(TRANSMEMBRANE)).collect(Collectors.toList())) {
			// Get all residues of a specific domain
			List<Residue> residues = protein.reactionSites.stream()
					.filter(value -> value.position >= domain.startId && value.position <= domain.endId)
					.collect(Collectors.toList());

			for (Residue residue : residues) {
				residue.score = -1;
				residue.location = domain.name;
				residue.predictedLocation = domain.name;
			}
		}

		// Other domains
		if (!(protein.isConflictedDomain || containsConflictResidue(protein))) {
			for (ProteinDomain domain : protein.domains.stream().filter(value -> value.isValid)
					.collect(Collectors.toList())) {

				// Get all residues of a specific domain
				List<Residue> residues = protein.reactionSites.stream()
						.filter(value -> value.position >= domain.startId && value.position <= domain.endId)
						.collect(Collectors.toList());

				if (domain.isPredicted) {
					if (domain.eValue.isBlank() || domain.eValue.isEmpty() || domain.eValue.equals("predicted")
							|| domain.name.isBlank() || domain.name.isEmpty())
						continue;

					for (Residue residue : residues) {

						if (!domain.name.toLowerCase().equals(TRANSMEMBRANE)) {
							residue.score = Double.parseDouble(domain.eValue);
							residue.location = domain.name;
							residue.predictedLocation = domain.name;
							if (residue.score != Util.initialResidueScore)
								residue.predicted_epoch = epochs;
						} else
							residue.score = -1;
					}
				} else {
					for (Residue residue : residues) {
						residue.score = Util.initialResidueScore;
						residue.location = domain.name;
						residue.predictedLocation = domain.name;
					}
				}
			}
		} else {

			// Group domains based on the range position
			Map<String, List<ProteinDomain>> groupedDomains = protein.domains.stream()
					.filter(value -> value.isValid && !value.name.isBlank() && !value.name.isEmpty()
							&& ((value.isPredicted && !value.eValue.isBlank() && !value.eValue.isEmpty()
									&& !value.eValue.equals("predicted")) || !value.isPredicted))
					.collect(Collectors.groupingBy(w -> w.startId + "_" + w.endId));

			for (Entry<String, List<ProteinDomain>> proteinDomain : groupedDomains.entrySet()) {

				String[] split_index = proteinDomain.getKey().split("_");

				int startId = Integer.parseInt(split_index[0]);
				int endId = Integer.parseInt(split_index[1]);

				// Get all residues of a specific domain
				List<Residue> residues = protein.reactionSites.stream()
						.filter(value -> value.position >= startId && value.position <= endId)
						.collect(Collectors.toList());

				List<ProteinDomain> current_domains = proteinDomain.getValue();
				ProteinDomain domain = null;
				boolean isConflict = false;
				if (current_domains.size() > 1)
					isConflict = true;
				else
					domain = current_domains.get(0);

				for (Residue residue : residues) {

					if (isConflict) {
						if (!residue.isConflicted) {
							residue.isConflicted = true;
							residue.conflicted_residue = new Residue('_', "", 0, null);
						}
					} else {
						if (!domain.name.toLowerCase().equals(TRANSMEMBRANE)) {
							if (domain.isPredicted)
								residue.score = Double.parseDouble(domain.eValue);
							else
								residue.score = Util.initialResidueScore;
						} else
							residue.score = -1;
						residue.location = domain.name;
						residue.predictedLocation = domain.name;
						if (residue.score != Util.initialResidueScore)
							residue.predicted_epoch = epochs;
					}
				}
			}
		}
	}

	/**
	 * Method responsible for updating residue score based on domain score
	 * 
	 * @param taskMonitor
	 */
	private void updateResidueScoresBasedOnDomainScore(TaskMonitor taskMonitor) {

		List<Protein> allProteins = Util.getProteins(myNetwork, true);
		if (allProteins == null || allProteins.size() == 0)
			return;

		taskMonitor.showMessage(TaskMonitor.Level.INFO, "Updating residue score: 0%");

		// Progress bar
		int old_progress = 0;
		int summary_processed = 0;
		int total_compartments = allProteins.size();
		for (Protein protein : allProteins) {

			updateResiduesLocationAndScore(protein);

			summary_processed++;
			Util.progressBar(summary_processed, old_progress, total_compartments, "Updating residue score: ",
					taskMonitor, null);
		}

		Util.updateXLStatus(taskMonitor, myNetwork, textLabel_status_result);
	}

	/**
	 * Method responsible for sorting domains based on their scores
	 * 
	 * @param map
	 */
	private void sortDomainScores(Protein protein) {

		List<Entry<String, Double>> capitalList = new LinkedList<>(protein.domainScores.entrySet());

		// call the sort() method of Collections
		Collections.sort(capitalList, (l1, l2) -> l2.getValue().compareTo(l1.getValue()));

		// create a new map
		LinkedHashMap<String, Double> result = new LinkedHashMap<String, Double>();

		// get entry from list to the map
		for (Map.Entry<String, Double> entry : capitalList) {
			result.put(entry.getKey(), entry.getValue());
		}

		protein.domainScores = result;

	}

	/**
	 * Method responsible for computing the domain score for proteins and checking
	 * the conflicts taking into account dual localization feature
	 * 
	 * @param score
	 */
	private void computeFinalDomainScore(TaskMonitor taskMonitor, boolean addDomainScore) {

		List<Protein> allProteins = Util.getProteins(myNetwork, true);

		if (allProteins == null || allProteins.size() == 0)
			return;

		int transmCount = 0;

		// Progress bar
		int old_progress = 0;
		int summary_processed = 0;
		int total_compartments = allProteins.size();

		for (Protein protein : allProteins) {
			try {

				summary_processed++;
				Util.progressBar(summary_processed, old_progress, total_compartments, "Computing domains score: ",
						taskMonitor, null);

				protein.isConflictedDomain = false;
				List<ProteinDomain> newDomains = new ArrayList<ProteinDomain>();
				transmCount = 0;

				List<ProteinDomain> validDomains = null;
				if (protein.domains != null) {
					Collections.sort(protein.domains, new Comparator<ProteinDomain>() {
						@Override
						public int compare(ProteinDomain lhs, ProteinDomain rhs) {
							return lhs.startId > rhs.startId ? 1 : (lhs.startId < rhs.startId) ? -1 : 0;
						}
					});

					transmCount = protein.domains.stream()
							.filter(value -> value.name.toLowerCase().equals(TRANSMEMBRANE))
							.collect(Collectors.toList()).size();

					validDomains = protein.domains.stream().filter(value -> value.isValid).collect(Collectors.toList());
				}

				if (protein.domainScores == null)
					protein.domainScores = new HashMap<String, Double>();

				if (transmCount == 0) {
					// There is no Transmembrane domain

					// There is only one non-predicted domain
					if (validDomains != null && validDomains.size() == 1) {

						if (!validDomains.get(0).isPredicted)
							continue;

						if (!Util.dualLocalization_conflict)
							validDomains = ComputeDomainsScore(protein.reactionSites, 1, protein.sequence.length(),
									true);
						else if (protein.domains == null)
							validDomains = new ArrayList<ProteinDomain>();

						if (addDomainScore) {
							protein.domainScores.put(protein.domains.get(0).name,
									Double.valueOf(protein.domains.get(0).eValue));
						}

						List<ProteinDomain> invalidDomains = protein.domains.stream().filter(value -> !value.isValid)
								.collect(Collectors.toList());
						validDomains.addAll(invalidDomains);
						validDomains = validDomains.stream().distinct().collect(Collectors.toList());
						protein.domains = validDomains;
						continue;
					}

					// Unified proteins -> e.g Matrix[11-30], Matrix [55-70] => Matrix [1-120]
					// e.g. Matrix [11-30], IMS [55-70] => Matrix [1-120] and IMS [1-120]
					if (!Util.dualLocalization_conflict) {
						newDomains = ComputeDomainsScore(protein.reactionSites, 1, protein.sequence.length(), true);
						if (validDomains != null) {
							newDomains.addAll(validDomains.stream().filter(value -> !value.isPredicted)
									.collect(Collectors.toList()));

							newDomains.addAll(validDomains.stream()
									.filter(value -> value.isValid && value.isPredicted && value.epoch < epochs)
									.collect(Collectors.toList()));
						}
						validDomains = newDomains;
					} else if (validDomains == null)
						validDomains = new ArrayList<ProteinDomain>();
					if (validDomains.size() == 0)
						continue;

					// remove domains created from null conflict residues
					validDomains.removeIf(value -> ((value.name.isBlank() || value.name.isEmpty())
							&& !value.eValue.isBlank() && !value.eValue.isEmpty()) || value.eValue.equals("0E0"));

					if (addDomainScore) {
						for (ProteinDomain proteinDomain : validDomains) {
							if (proteinDomain.isPredicted)
								protein.domainScores.put(proteinDomain.name, Double.valueOf(proteinDomain.eValue));
							else {
								if (proteinDomain.eValue.isBlank() || proteinDomain.eValue.isEmpty())
									protein.domainScores.put(proteinDomain.name, Util.initialResidueScore);
								else
									protein.domainScores.put(proteinDomain.name, Double.valueOf(proteinDomain.eValue));
							}
						}
					}

					validDomains.stream().forEach(value -> {
						value.startId = 1;
						value.endId = protein.sequence.length();
					});
//					mergeSimilarDomains(validDomains);

					// It's necessary to check for valid domains because in the line 2207 and 2228
					// some
					// domains can be invalid
					if (validDomains.stream().filter(value -> value.isValid).collect(Collectors.toList()).size() == 1) {
						protein.isConflictedDomain = false;
					} else {
						if (!Util.dualLocalization_conflict) {
							sortDomainScores(protein);
							protein.isConflictedDomain = true;
						} else {
							protein.isConflictedDomain = false;
						}
					}

					List<ProteinDomain> invalidDomains = protein.domains.stream().filter(value -> !value.isValid)
							.collect(Collectors.toList());
					validDomains.addAll(invalidDomains);
					protein.domains = validDomains;

				} else {

					if (transmCount == 1) {

						// There is only one transmembrane
						if (validDomains.size() == 1) {

							// Remove low confidence transmem regions

							validDomains.stream()
									.filter(value -> Double
											.parseDouble(value.eValue) < Util.transmemPredictionRegionsUpperScore)
									.collect(Collectors.toList()).forEach(candidate -> candidate.isValid = false);

							continue;
						}

						ProteinDomain transmem = protein.domains.stream()
								.filter(value -> value.name.toLowerCase().contains(TRANSMEMBRANE))
								.collect(Collectors.toList()).get(0);

						double transm_score = Util.initialTransmembraneScore;
						if (!(transmem.eValue.isBlank() || transmem.eValue.isEmpty()))
							transm_score = Double.parseDouble(transmem.eValue);

						if (transm_score < Util.transmemPredictionRegionsUpperScore) {

							transmem.isValid = false;
							// Remove all 'predicted' domains
							validDomains = validDomains.stream().filter(value -> !value.eValue.equals("predicted"))
									.collect(Collectors.toList());

							if (validDomains.size() > 0 && !Util.dualLocalization_conflict)
								protein.isConflictedDomain = true;
							else
								protein.isConflictedDomain = false;

						} else {// it's transmembrane

							newDomains.clear();

							// Group domains based on the range position
							Map<String, List<ProteinDomain>> groupedDomains = validDomains.stream()
									.filter(value -> !value.name.toLowerCase().contains(TRANSMEMBRANE)
											&& !value.eValue.equals("predicted"))
									.collect(Collectors.groupingBy(w -> w.startId + "_" + w.endId));

							// Select the domain with highest score (before and after transm domain)
							for (Entry<String, List<ProteinDomain>> proteinDomain : groupedDomains.entrySet()) {

								List<ProteinDomain> current_domains = proteinDomain.getValue();
								// Sort descending
								Collections.sort(current_domains, new Comparator<ProteinDomain>() {

									@Override
									public int compare(ProteinDomain lhs, ProteinDomain rhs) {

										if (!(lhs.eValue.isBlank() || lhs.eValue.isEmpty() || rhs.eValue.isBlank()
												|| rhs.eValue.isEmpty())) {
											return Double.parseDouble(lhs.eValue) > Double.parseDouble(rhs.eValue) ? -1
													: Double.parseDouble(lhs.eValue) < Double.parseDouble(rhs.eValue)
															? 1
															: 0;
										} else
											return 0;

									}
								});

								boolean isThereOneHighestScore = false;
								ProteinDomain domain_with_highest_score = current_domains.get(0);

								if (!Util.fixDomainManually) {

									if (!(domain_with_highest_score.eValue.isBlank()
											|| domain_with_highest_score.eValue.isEmpty())) {

										for (ProteinDomain evalue : current_domains) {
											if (!(evalue.eValue.isBlank() || evalue.eValue.isEmpty())) {
												double score = Double.parseDouble(evalue.eValue);
												if (Double.parseDouble(domain_with_highest_score.eValue)
														- score > Util.deltaScore) {
													isThereOneHighestScore = true;
												}
											}
										}
									}
								}

								if (isThereOneHighestScore)
									newDomains.add(domain_with_highest_score);
								else
									newDomains.addAll(proteinDomain.getValue());
							}

							newDomains.addAll(validDomains.stream().filter(value -> value.eValue.equals("predicted"))
									.collect(Collectors.toList()));
							newDomains.add(transmem);
//							protein.domains = newDomains;
							validDomains = newDomains;

							// remove domains created from null conflict residues
							validDomains.removeIf(
									value -> ((value.name.isBlank() || value.name.isEmpty()) && !value.eValue.isBlank()
											&& !value.eValue.isEmpty()) || value.eValue.equals("0E0"));

							Collections.sort(validDomains, new Comparator<ProteinDomain>() {
								@Override
								public int compare(ProteinDomain lhs, ProteinDomain rhs) {
									return lhs.startId > rhs.startId ? 1 : (lhs.startId < rhs.startId) ? -1 : 0;
								}
							});

							for (ProteinDomain proteinDomain : validDomains.stream()
									.filter(value -> !value.name.toLowerCase().contains(TRANSMEMBRANE)
											&& !value.eValue.equals("predicted"))
									.collect(Collectors.toList())) {
								if (proteinDomain.eValue.isBlank() || proteinDomain.eValue.isEmpty())
									continue;

								if (addDomainScore) {
									protein.domainScores.put(proteinDomain.name, Double.valueOf(proteinDomain.eValue));
								}
							}

							List<ProteinDomain> invalidDomains = protein.domains.stream()
									.filter(value -> !value.isValid).collect(Collectors.toList());
							validDomains.addAll(invalidDomains);
							validDomains = validDomains.stream().distinct().collect(Collectors.toList());
							protein.domains = validDomains;

							checkConflictProteinDomains(protein);

						}
					} else {// There are more than one transmem

						List<ProteinDomain> all_transmem = protein.domains.stream()
								.filter(value -> value.name.toLowerCase().contains(TRANSMEMBRANE))
								.collect(Collectors.toList());

						newDomains.clear();

						for (int i = 0; i < all_transmem.size(); i++) {

							ProteinDomain transmem = all_transmem.get(i);
							if (transmem == null)
								continue;

							int transm_index = protein.domains.indexOf(transmem);
							double transm_score = Double.parseDouble(transmem.eValue);

							if ((i + 1) >= all_transmem.size()) {

								if (transm_score < Util.transmemPredictionRegionsUpperScore) {

									// ... |Transm (score 0.5) | Last Domain
									// Add Last Domain
									newDomains
											.addAll(validDomains.stream()
													.filter(value -> !value.eValue.equals("predicted")
															&& (value.startId > transmem.endId))
													.collect(Collectors.toList()));

								} else {

									// Group domains based on the range position
									Map<String, List<ProteinDomain>> groupedDomains = validDomains.stream()
											.filter(value -> !value.name.toLowerCase().contains(TRANSMEMBRANE)
													&& !value.eValue.equals("predicted")
													&& value.startId > transmem.endId)
											.collect(Collectors.groupingBy(w -> w.startId + "_" + w.endId));

									// Select the domain with highest score
									for (Entry<String, List<ProteinDomain>> proteinDomain : groupedDomains.entrySet()) {

										ProteinDomain domain_with_highest_score = proteinDomain.getValue().get(0);
										boolean isThereOneHighestScore = false;

										if (!Util.fixDomainManually) {

											if (!(domain_with_highest_score.eValue.isBlank()
													|| domain_with_highest_score.eValue.isEmpty())) {

												for (ProteinDomain evalue : proteinDomain.getValue()) {
													if (!(evalue.eValue.isBlank() || evalue.eValue.isEmpty())) {
														double score = Double.parseDouble(evalue.eValue);
														if (Double.parseDouble(domain_with_highest_score.eValue)
																- score > Util.deltaScore) {
															isThereOneHighestScore = true;
														}
													}
												}
											}
										}

										if (isThereOneHighestScore)
											newDomains.add(domain_with_highest_score);
										else
											newDomains.addAll(proteinDomain.getValue());

									}

									// Add Last Domain (predicted)
									newDomains.addAll(validDomains.stream().filter(
											value -> value.eValue.equals("predicted") && value.startId > transmem.endId)
											.collect(Collectors.toList()));

									newDomains.add(transmem);
								}

								break;
							}

							ProteinDomain next_transmem = all_transmem.get(i + 1);
							if (next_transmem == null) {
								continue;
							}

							if (transm_score < Util.transmemPredictionRegionsUpperScore) {

								// [Domain 1 | Transm (score 0.5) | Domain 2 (predicted) | Transm (score 0.7) |
								// Domain 3]
								// => Remove Domain 1 (predicted)
								if (transm_index == 1) {
									newDomains
											.addAll(validDomains.stream()
													.filter(value -> !(value.eValue.equals("predicted")
															&& value.endId < transmem.startId))
													.collect(Collectors.toList()));

									// => Remove Domain 2
									newDomains.removeAll(validDomains.stream().filter(value -> (value.eValue
											.equals("predicted")
											&& (value.startId > transmem.endId && value.endId < next_transmem.startId)))
											.collect(Collectors.toList()));
								} else {

									// => Remove Domain 2
									newDomains.addAll(validDomains.stream().filter(value -> !(value.eValue
											.equals("predicted")
											&& (value.startId > transmem.endId && value.endId < next_transmem.startId)))
											.collect(Collectors.toList()));
								}

								newDomains.remove(transmem);

							} else {

								// [Domain 1 and Domain 5 | Transm (score 0.75) | Domain 2 and Domain 4 | Transm
								// (score 0.7)
								// | Domain 3]

								// Group domains based on the range position
								// Domain 2 and Domain 4
								Map<String, List<ProteinDomain>> groupedDomains = validDomains.stream()
										.filter(value -> !value.name.toLowerCase().contains(TRANSMEMBRANE)
												&& !value.eValue.equals("predicted") && value.startId > transmem.endId
												&& value.endId < next_transmem.startId)
										.collect(Collectors.groupingBy(w -> w.startId + "_" + w.endId));

								// Select the domain with highest score
								for (Entry<String, List<ProteinDomain>> proteinDomain : groupedDomains.entrySet()) {

									ProteinDomain domain_with_highest_score = proteinDomain.getValue().get(0);
									boolean isThereOneHighestScore = false;

									if (!Util.fixDomainManually) {

										if (!(domain_with_highest_score.eValue.isBlank()
												|| domain_with_highest_score.eValue.isEmpty())) {

											for (ProteinDomain evalue : proteinDomain.getValue()) {
												if (!(evalue.eValue.isBlank() || evalue.eValue.isEmpty())) {
													double score = Double.parseDouble(evalue.eValue);
													if (Double.parseDouble(domain_with_highest_score.eValue)
															- score > Util.deltaScore) {
														isThereOneHighestScore = true;
													}
												}
											}
										}
									}

									if (isThereOneHighestScore)
										newDomains.add(domain_with_highest_score);
									else
										newDomains.addAll(proteinDomain.getValue());

								}

								// Add Domain 2 (predicted)
								newDomains.addAll(validDomains.stream()
										.filter(value -> value.eValue.equals("predicted")
												&& value.startId > transmem.endId
												&& value.endId < next_transmem.startId)
										.collect(Collectors.toList()));

								newDomains.add(transmem);

								// => Domain 1 and Domain 5 | Transmem
								if (transm_index == 1) {

									// Group domains based on the range position
									// Domain 1 and Domain 5
									groupedDomains = validDomains.stream()
											.filter(value -> !value.name.toLowerCase().contains(TRANSMEMBRANE)
													&& !value.eValue.equals("predicted")
													&& value.endId < transmem.startId)
											.collect(Collectors.groupingBy(w -> w.startId + "_" + w.endId));

									// Select the domain with highest score
									for (Entry<String, List<ProteinDomain>> proteinDomain : groupedDomains.entrySet()) {

										ProteinDomain domain_with_highest_score = proteinDomain.getValue().get(0);

										if (!(domain_with_highest_score.eValue.isBlank()
												|| domain_with_highest_score.eValue.isEmpty())) {
											for (ProteinDomain evalue : proteinDomain.getValue()) {
												if (!(evalue.eValue.isBlank() || evalue.eValue.isEmpty())) {
													double score = Double.parseDouble(evalue.eValue);
													if (Double.parseDouble(domain_with_highest_score.eValue) < score)
														domain_with_highest_score = evalue;
												}
											}
										}
										newDomains.add(domain_with_highest_score);
									}

									// Add Domain 1 (predicted)
									newDomains.addAll(validDomains.stream().filter(
											value -> value.eValue.equals("predicted") && value.endId < transmem.startId)
											.collect(Collectors.toList()));

								}

							}

						}

						validDomains = newDomains;
						List<ProteinDomain> invalidDomains = protein.domains.stream().filter(value -> !value.isValid)
								.collect(Collectors.toList());
						validDomains.addAll(invalidDomains);
						validDomains = validDomains.stream().distinct().collect(Collectors.toList());
						protein.domains = validDomains;

						// remove domains created from null conflict residues
						protein.domains.removeIf(value -> ((value.name.isBlank() || value.name.isEmpty())
								&& !value.eValue.isBlank() && !value.eValue.isEmpty()) || value.eValue.equals("0E0"));

						Collections.sort(protein.domains, new Comparator<ProteinDomain>() {

							@Override
							public int compare(ProteinDomain lhs, ProteinDomain rhs) {
								return lhs.startId > rhs.startId ? 1 : (lhs.startId < rhs.startId) ? -1 : 0;
							}
						});

						if (addDomainScore) {
							for (

							ProteinDomain proteinDomain : protein.domains.stream()
									.filter(value -> !value.name.toLowerCase().contains(TRANSMEMBRANE)
											&& !value.eValue.equals("predicted"))
									.collect(Collectors.toList())) {
								if (proteinDomain.eValue.isBlank() || proteinDomain.eValue.isEmpty())
									continue;
								protein.domainScores.put(proteinDomain.name, Double.valueOf(proteinDomain.eValue));
							}
						}
						checkConflictProteinDomains(protein);
					}
				}
			} catch (Exception e) {
				System.out.println("ERROR: computeFinalDomainScore -> index:" + summary_processed);
			}
		}

	}

	/**
	 * Method responsible for predicting missed domains based on the 'Transmembrane'
	 * information
	 * 
	 * @param protein current protein
	 */
	public static void predictDomainsBasedOnTransmemInfo(Protein protein) {

		if (protein.domains == null)
			return;

		int count_transmem = 0;
		List<String> unique_domain = new ArrayList<String>();

		List<ProteinDomain> validDomains = protein.domains.stream().filter(value -> value.isValid)
				.collect(Collectors.toList());

		Collections.sort(validDomains, new Comparator<ProteinDomain>() {
			@Override
			public int compare(ProteinDomain lhs, ProteinDomain rhs) {
				return lhs.startId > rhs.startId ? 1 : (lhs.startId < rhs.startId) ? -1 : 0;
			}
		});

		count_transmem = 0;
		List<ProteinDomain> new_domain_list = new ArrayList<ProteinDomain>();

		for (ProteinDomain domain : validDomains) {

			if (domain.name.toLowerCase().contains(TRANSMEMBRANE))
				count_transmem++;
			else
				unique_domain.add(domain.name);
		}

		unique_domain = unique_domain.stream().distinct().collect(Collectors.toList());

		if (count_transmem > 0 && unique_domain.size() > 0) {

			List<ProteinDomain> transmemDomains = validDomains.stream()
					.filter(value -> value.name.toLowerCase().contains(TRANSMEMBRANE)).collect(Collectors.toList());
			Collections.sort(transmemDomains, new Comparator<ProteinDomain>() {
				@Override
				public int compare(ProteinDomain lhs, ProteinDomain rhs) {
					return lhs.startId > rhs.startId ? 1 : (lhs.startId < rhs.startId) ? -1 : 0;
				}
			});

			// Group domains based on the range position
			Map<String, List<ProteinDomain>> groupedDomains = validDomains.stream()
					.filter(value -> !value.eValue.equals("predicted"))
					.collect(Collectors.groupingBy(w -> w.startId + "_" + w.endId));

			List<String> domain_range_keys = new ArrayList<String>(groupedDomains.keySet());

			Collections.sort(domain_range_keys, new Comparator<String>() {
				@Override
				public int compare(String lhs, String rhs) {

					String[] split_lhs = lhs.split("_");
					String[] split_rhs = rhs.split("_");

					int lhs_startId = Integer.parseInt(split_lhs[0]);
					int rhs_startId = Integer.parseInt(split_rhs[0]);

					return lhs_startId > rhs_startId ? 1 : (lhs_startId < rhs_startId) ? -1 : 0;
				}
			});

			int indexOfLastDomain = domain_range_keys.size() - 1;
			int startDomain = 1;
			predicted_protein_domain_name = "Unknown";

			int conflict_domain = -1;
			List<ProteinDomain> conflict_domains = null;

			for (int i = 0; i < domain_range_keys.size(); i++) {

				List<ProteinDomain> current_domains = groupedDomains.get(domain_range_keys.get(i));
				ProteinDomain current_domain = current_domains.get(0);

				ProteinDomain last_new_domain = null;
				if (new_domain_list.size() > 0)
					last_new_domain = new_domain_list.get(new_domain_list.size() - 1);

				if (current_domain.name.toLowerCase().equals(TRANSMEMBRANE)) {

					if (new_domain_list.size() == 0 || last_new_domain.name.toLowerCase().contains(TRANSMEMBRANE)) {

						if (Double.parseDouble(current_domain.eValue) > Util.transmemPredictionRegionsUpperScore
								&& last_new_domain != null && !last_new_domain.eValue.isBlank()
								&& !last_new_domain.eValue.isEmpty()
								&& Double.parseDouble(last_new_domain.eValue) > Util.transmemPredictionRegionsUpperScore
								&& current_domain.startId != 1 && startDomain < current_domain.startId) {
							ProteinDomain new_domain = new ProteinDomain(UNKNOWN_DOMAIN, startDomain,
									current_domain.startId - 1, true, "predicted", epochs);
							new_domain_list.add(new_domain);
							predicted_protein_domain_name = "Unknown";
						}
					}
					new_domain_list.add(current_domain);
					startDomain = current_domain.endId + 1;

					if (Double.parseDouble(current_domain.eValue) < Util.transmemPredictionRegionsUpperScore)
						current_domain.isValid = false;

					transmemDomains.remove(current_domain);
					continue;

				}

				ProteinDomain next_domain = null;
				List<ProteinDomain> next_domains = null;

				if (domain_range_keys.size() > (i + 1)) {

					// Get next group position

					next_domains = groupedDomains.get(domain_range_keys.get(i + 1));
					next_domain = next_domains.get(0);

				}

				ProteinDomain next_transmem = transmemDomains.size() > 0
						? transmemDomains.get(transmemDomains.indexOf(Collections.min(transmemDomains)))
						: null;

				if (next_transmem == null) {
					// There is no further transmem

					if (current_domain.startId != last_new_domain.startId
							&& current_domain.endId != last_new_domain.endId) {

						// update range in all domains of the current range
						for (ProteinDomain domain : current_domains) {
							domain.startId = startDomain;
							if (domain.endId < protein.sequence.length())
								domain.endId = protein.sequence.length();
						}

					}
					new_domain_list.addAll(current_domains);

					// e.g. [Matrix (current) | IMS (next)]
					if (next_domain != null) {
						conflict_domain = domain_range_keys.indexOf(next_domain.startId + "_" + next_domain.endId);

						if (conflict_domains == null)
							conflict_domains = groupedDomains.get(domain_range_keys.get(conflict_domain));
						// update range in all domains of the current range
						for (ProteinDomain domain : conflict_domains) {
							domain.startId = startDomain;
							domain.isPredicted = true;
							if (domain.endId < protein.sequence.length())
								domain.endId = protein.sequence.length();
						}

						new_domain_list.addAll(conflict_domains);
						conflict_domain = -1;
					}

					continue;
				}

				if (current_domain.startId < next_transmem.startId) {

					// e.g [IMS | Transmem | ??? ]
					if (next_domain != null && next_domain.name.toLowerCase().equals(TRANSMEMBRANE)) {

						if (current_domains.size() == 1
								&& Double.parseDouble(next_domain.eValue) > Util.transmemPredictionRegionsUpperScore
								&& conflict_domain == -1)
							predicted_protein_domain_name = current_domain.name;
						else // There is conflict domains
							predicted_protein_domain_name = "Unknown";

						i = domain_range_keys.indexOf(next_transmem.startId + "_" + next_transmem.endId) - 1;

						if (!(current_domain.eValue.isBlank() || current_domain.eValue.isEmpty())
								&& current_domain.startId != startDomain
								&& Double.parseDouble(current_domain.eValue) != Util.initialResidueScore) {

							// update range in all domains of the current range
							for (ProteinDomain domain : current_domains) {
								domain.startId = startDomain;
								domain.isPredicted = true;
							}

							if (conflict_domain != -1) {

								if (conflict_domains == null)
									conflict_domains = groupedDomains.get(domain_range_keys.get(conflict_domain));
								// update range in all domains of the current range
								for (ProteinDomain domain : conflict_domains) {
									domain.startId = startDomain;
									domain.isPredicted = true;
								}
							}

							protein.isPredictedBasedOnTransmemInfo = true;
						}
						if (!(current_domain.eValue.isBlank() || current_domain.eValue.isEmpty())

								&& Double.parseDouble(current_domain.eValue) != Util.initialResidueScore) {

							// e.g. [IMS | TRANSMEM]
							// e.g. [ 150 - 190 | 200 - 300 ]
							if (current_domain.endId <= next_transmem.startId - 1) {

								// update range in all domains of the current range
								for (ProteinDomain domain : current_domains) {
									domain.endId = next_transmem.startId - 1;
									domain.isPredicted = true;
								}

							} else {
								// e.g. [IMS | TRANSMEM]
								// e.g. [ 150 - 305 | 200 - 300 ] -> It's necessary to split IMS

								List<ProteinDomain> new_splitted_domains = new ArrayList<ProteinDomain>();
								// update range in all domains of the current range
								for (ProteinDomain domain : current_domains) {
									int current_endId = domain.endId;

									// [150 - 199]
									domain.endId = next_transmem.startId - 1;
									domain.isPredicted = true;
									domain.eValue = Util
											.RoundScore(Util.ComputeDomainScore(protein, domain.startId, domain.endId));

									// Create a new domain
									// [301 - 305]
									ProteinDomain new_splitted_domain = new ProteinDomain(domain.name,
											next_transmem.endId + 1, current_endId, true, domain.eValue, epochs);
									new_splitted_domain.eValue = Util.RoundScore(Util.ComputeDomainScore(protein,
											new_splitted_domain.startId, new_splitted_domain.endId));
									new_splitted_domains.add(new_splitted_domain);

								}

								current_domains.addAll(new_splitted_domains);

							}

							if (conflict_domain != -1) {

								if (conflict_domains == null)
									conflict_domains = groupedDomains.get(domain_range_keys.get(conflict_domain));

								List<ProteinDomain> new_splitted_domains = new ArrayList<ProteinDomain>();
								// update range in all domains of the current range
								for (ProteinDomain domain : conflict_domains) {

									// e.g. [IMS | TRANSMEM]
									// e.g. [ 150 - 190 | 200 - 300 ]
									if (current_domain.endId <= next_transmem.startId - 1) {
										domain.endId = next_transmem.startId - 1;
										domain.isPredicted = true;

									} else {
										// e.g. [IMS | TRANSMEM]
										// e.g. [ 150 - 305 | 200 - 300 ] -> It's necessary to split IMS

										// update range in all domains of the current range

										int current_endId = domain.endId;

										// [150 - 199]
										domain.endId = next_transmem.startId - 1;
										domain.isPredicted = true;
										domain.eValue = Util.RoundScore(
												Util.ComputeDomainScore(protein, domain.startId, domain.endId));

										// Create a new domain
										// [301 - 305]
										ProteinDomain new_splitted_domain = new ProteinDomain(domain.name,
												next_transmem.endId + 1, current_endId, true, domain.eValue, epochs);
										new_splitted_domain.eValue = Util.RoundScore(Util.ComputeDomainScore(protein,
												new_splitted_domain.startId, new_splitted_domain.endId));
										new_splitted_domains.add(new_splitted_domain);

									}

								}
								conflict_domains.addAll(new_splitted_domains);

							}

							protein.isPredictedBasedOnTransmemInfo = true;
						}
						conflict_domain = -1;

						startDomain = next_transmem.endId + 1;
						transmemDomains.remove(next_transmem);
					} else if (next_domain == null || !(next_domain.startId < next_transmem.startId)) {
						startDomain = current_domain.endId + 1;
					} else
						conflict_domain = domain_range_keys
								.indexOf(current_domain.startId + "_" + current_domain.endId);

				} else {

					// e.g [??? | Transmem | IMS ]

					if (Double.parseDouble(next_transmem.eValue) > Util.transmemPredictionRegionsUpperScore) {

						if (current_domains.size() == 1)
							predicted_protein_domain_name = current_domain.name;
						else // There is conflict domains
							predicted_protein_domain_name = "Unknown";

						String current_domain_index = current_domain.startId + "_" + current_domain.endId;
						if (current_domain_index.equals(domain_range_keys.get(indexOfLastDomain))) {

							// It is the last domain
							// update range in all domains of the current range
							for (ProteinDomain domain : current_domains) {
								domain.endId = protein.sequence.length();
								domain.startId = next_transmem.endId + 1;
							}

						}

						startDomain = 1;

						addPredictedDomainBasedOnOMMorIMMDomain(new_domain_list, protein, next_transmem, startDomain);
						startDomain = next_transmem.endId + 1;

						// update range in all domains of the current range
						for (ProteinDomain domain : current_domains) {
							domain.isPredicted = true;
						}

						protein.isPredictedBasedOnTransmemInfo = true;
					}

				}
				if (conflict_domain == -1) {// If conflict_domain != -1 means conflict domains indexes need to
											// be
											// update before adding to new_domain_list
					if (conflict_domains != null) {
						new_domain_list.addAll(conflict_domains);
						conflict_domains = null;
					}
					new_domain_list.addAll(current_domains);
				}

			}

			// remove domains created from null conflict residues
			new_domain_list.removeIf(value -> ((value.name.isBlank() || value.name.isEmpty()) && !value.eValue.isBlank()
					&& !value.eValue.isEmpty()) || value.eValue.equals("0E0"));

			groupedDomains = new_domain_list.stream().collect(Collectors.groupingBy(w -> w.startId + "_" + w.endId));

			domain_range_keys = new ArrayList<String>(groupedDomains.keySet());
			Collections.sort(domain_range_keys, new Comparator<String>() {
				@Override
				public int compare(String lhs, String rhs) {

					String[] split_lhs = lhs.split("_");
					String[] split_rhs = rhs.split("_");

					int lhs_startId = Integer.parseInt(split_lhs[0]);
					int rhs_startId = Integer.parseInt(split_rhs[0]);

					return lhs_startId > rhs_startId ? 1 : (lhs_startId < rhs_startId) ? -1 : 0;
				}
			});

			indexOfLastDomain = domain_range_keys.size() - 1;
			List<ProteinDomain> last_domains = groupedDomains.get(domain_range_keys.get(indexOfLastDomain));
			ProteinDomain last_domain = last_domains.get(0);

			if (last_domain.endId < protein.sequence.length()) {

				// It is necessary to add the last domain
				if (last_domain.name.toLowerCase().equals(TRANSMEMBRANE)
						&& Double.parseDouble(last_domain.eValue) > Util.transmemPredictionRegionsUpperScore) {

					if (predicted_protein_domain_name.isEmpty() || predicted_protein_domain_name.isBlank()
							|| predicted_protein_domain_name.equals(UNKNOWN_PREDICTED_DOMAIN)) {
						ProteinDomain new_domain = new ProteinDomain(UNKNOWN_DOMAIN, startDomain,
								protein.sequence.length(), true, "predicted", epochs);
						new_domain_list.add(new_domain);
					} else {
						addPredictedDomainBasedOnOMMorIMMDomain(new_domain_list, protein,
								new ProteinDomain(TRANSMEMBRANE.toUpperCase(), protein.sequence.length() + 1, -1, true,
										"predicted", epochs),
								startDomain);
					}
				} else// Update range of the last domain
				{
					// update range in all domains of the current range
					for (ProteinDomain domain : last_domains) {
						if (!domain.name.toLowerCase().equals(TRANSMEMBRANE))
							domain.endId = protein.sequence.length();
					}
				}

			}

			new_domain_list = new_domain_list.stream().distinct().collect(Collectors.toList());

			// Check if exists unknown domains (name == '###')
			if (new_domain_list.stream().filter(value -> value.name.equals(UNKNOWN_DOMAIN)).collect(Collectors.toList())
					.size() > 0) {

				groupedDomains = new_domain_list.stream().filter(value -> value.isValid)
						.collect(Collectors.groupingBy(w -> w.startId + "_" + w.endId));

				domain_range_keys = new ArrayList<String>(groupedDomains.keySet());
				Collections.sort(domain_range_keys, new Comparator<String>() {
					@Override
					public int compare(String lhs, String rhs) {

						String[] split_lhs = lhs.split("_");
						String[] split_rhs = rhs.split("_");

						int lhs_startId = Integer.parseInt(split_lhs[0]);
						int rhs_startId = Integer.parseInt(split_rhs[0]);

						return lhs_startId > rhs_startId ? 1 : (lhs_startId < rhs_startId) ? -1 : 0;
					}
				});

				List<ProteinDomain> domainsUsedToPredictBasedOnTransmInfo = new ArrayList<ProteinDomain>();
				for (int i = 0; i < domain_range_keys.size(); i++) {
					List<ProteinDomain> domains = groupedDomains.get(domain_range_keys.get(i));
					ProteinDomain domain = domains.get(0);
					domainsUsedToPredictBasedOnTransmInfo.add(domain);
				}

				for (int i = 0; i < domain_range_keys.size(); i++) {

					List<ProteinDomain> domains = groupedDomains.get(domain_range_keys.get(i));
					ProteinDomain domain = domains.get(0);

					boolean isForward = false;
					if (domain.name.equals(UNKNOWN_DOMAIN)) {

						predicted_protein_domain_name = "Unknown";
						int j = i + 1;
						for (; j < domain_range_keys.size(); j++) {

							List<ProteinDomain> next_domains = groupedDomains.get(domain_range_keys.get(j));
							ProteinDomain next_domain = next_domains.get(0);

							if (!next_domain.name.toLowerCase().contains(TRANSMEMBRANE)
									&& !next_domain.name.equals(UNKNOWN_DOMAIN)) {
								isForward = true;

								if (next_domains.size() > 1) {
									List<String> uniqueName = new ArrayList<String>();
									for (ProteinDomain current_domain : next_domains) {
										uniqueName.add(current_domain.name);
									}
									uniqueName = uniqueName.stream().distinct().collect(Collectors.toList());

									// It means that all domains in this range are equal and have been predicted in
									// different epochs
									if (uniqueName.size() == 1)
										predicted_protein_domain_name = next_domain.name;
									else
										predicted_protein_domain_name = "Unknown";
								} else
									predicted_protein_domain_name = next_domain.name;
								break;
							}
						}

						if (!isForward && i > 0) {

							j = i - 1;
							for (; j >= 0; j--) {

								List<ProteinDomain> previous_domains = groupedDomains.get(domain_range_keys.get(j));
								ProteinDomain previous_domain = previous_domains.get(0);

								if (!previous_domain.name.toLowerCase().contains(TRANSMEMBRANE)
										&& !previous_domain.name.equals(UNKNOWN_DOMAIN)) {

									if (previous_domains.size() > 1) {

										List<String> uniqueName = new ArrayList<String>();
										for (ProteinDomain current_domain : previous_domains) {
											uniqueName.add(current_domain.name);
										}
										uniqueName = uniqueName.stream().distinct().collect(Collectors.toList());

										// It means that all domains in this range are equal and have been predicted in
										// different epochs
										if (uniqueName.size() == 1)
											predicted_protein_domain_name = previous_domain.name;
										else
											predicted_protein_domain_name = "Unknown";
									} else
										predicted_protein_domain_name = previous_domain.name;
									break;
								}
							}
						}

						// Set 'predicted_protein_domain_name' based on 'Transmem' info
						if (!isForward)
							predictDomainBasedOnOMMorIMM(i, j, domainsUsedToPredictBasedOnTransmInfo, protein,
									isForward);
						else
							predictDomainBasedOnOMMorIMM(i, (j - 1), domainsUsedToPredictBasedOnTransmInfo, protein,
									isForward);
						domain.name = predicted_protein_domain_name;

					}
				}
				domainsUsedToPredictBasedOnTransmInfo.clear();
			}

			// Check Transmem scores. If score < transmemPredictionRegionsUpperScore,
			// domains is invalid
			new_domain_list.stream()
					.filter(value -> value.isValid && value.name.toLowerCase().equals(TRANSMEMBRANE)
							&& Double.parseDouble(value.eValue) < Util.transmemPredictionRegionsUpperScore)
					.forEach(value -> value.isValid = false);

			List<ProteinDomain> invalidDomains = protein.domains.stream().filter(value -> !value.isValid)
					.collect(Collectors.toList());

			// Check if exists similar invalid domains and valid domains. If so, remove the
			// one predicted in this round (valid domain)
			for (ProteinDomain invalid_domain : invalidDomains) {

				Optional<ProteinDomain> similar_valid = new_domain_list
						.stream().filter(value -> value.startId == invalid_domain.startId
								&& value.endId == invalid_domain.endId && value.name.equals(invalid_domain.name))
						.findFirst();
				if (similar_valid.isPresent()) {
					new_domain_list.remove(similar_valid.get());
				}

			}

			new_domain_list.addAll(invalidDomains);
			new_domain_list = new_domain_list.stream().distinct().collect(Collectors.toList());
			protein.domains = new_domain_list;
			Collections.sort(protein.domains, new Comparator<ProteinDomain>() {
				@Override
				public int compare(ProteinDomain lhs, ProteinDomain rhs) {
					return lhs.startId > rhs.startId ? 1 : (lhs.startId < rhs.startId) ? -1 : 0;
				}
			});

//			// Merge similar domains in different epochs
//			mergeSimilarDomains(protein.domains);

			validDomains = protein.domains.stream().filter(value -> value.isValid).collect(Collectors.toList());

			// Update protein domain status
			if (validDomains.size() > 1) {

				checkConflictProteinDomains(protein);
			}

			updateResiduesLocationAndScore(protein);
		}

	}

	/**
	 * Method responsible for predicting missed domains from all proteins based on
	 * the 'Transmembrane' information
	 */
	private void predictDomainsFromAllProteinsBasedOnTransmemInfo() {

		List<Protein> allProteins = Util.getProteins(myNetwork, true);
		if (allProteins == null || allProteins.size() == 0)
			return;

		int count_protein = 0;

		for (Protein protein : allProteins.stream().filter(value -> !value.isPredictedBasedOnTransmemInfo)
				.collect(Collectors.toList())) {
			try {

				predictDomainsBasedOnTransmemInfo(protein);

			} catch (Exception e) {
				System.out.println("ERROR: predictDomainsBasedOnTransmemInfo -> index:" + count_protein);
			}
			count_protein++;
		}

	}

	/**
	 * Method responsible for merging similar domains predicted in different epochs
	 * 
	 * @param domains current domains
	 */
	private void mergeSimilarDomains(List<ProteinDomain> domains) {

		for (ProteinDomain proteinDomain : domains.stream()
				.filter(value -> !value.name.toLowerCase().equals(TRANSMEMBRANE)).collect(Collectors.toList())) {

			// eg: Matrix[10,20] => start_d = 12, end_d = 18
			List<ProteinDomain> isProteinDomainPresent = domains.stream()
					.filter(value -> value.isValid && value.name.equals(proteinDomain.name)
							&& value.startId <= proteinDomain.startId && value.endId >= proteinDomain.endId)
					.collect(Collectors.toList());

			if (isProteinDomainPresent != null && isProteinDomainPresent.size() > 1) {

				int max_epoch = Collections.max(isProteinDomainPresent, Comparator.comparing(s -> s.getEpoch())).epoch;

				Optional<ProteinDomain> max_epoch_domain = isProteinDomainPresent.stream()
						.filter(value -> value.epoch == max_epoch).findFirst();

				String common_score = "";
				if (max_epoch_domain.isPresent()) {
					common_score = max_epoch_domain.get().eValue;
				}

				for (ProteinDomain domain : isProteinDomainPresent) {
					domain.eValue = common_score;
				}
				isProteinDomainPresent.stream().filter(value -> value.epoch < max_epoch).forEach(value -> {
					value.isValid = false;
				});
			}
		}
	}

	/**
	 * Method responsible for setting 'predicted_protein_domain_name' based on
	 * Transmem info
	 * 
	 * @param i               current i
	 * @param j               current j
	 * @param new_domain_list new domain list
	 * @param protein         current protein
	 * @param isForward       search forward or backward
	 */
	private static void predictDomainBasedOnOMMorIMM(int i, int j, List<ProteinDomain> new_domain_list, Protein protein,
			boolean isForward) {

		int count_reverse = 0;
		int limit = 0;
		if (isForward) {
			count_reverse = j;
			limit = i;
		} else {
			count_reverse = i - 1;
			limit = j;
		}

		for (; count_reverse >= limit; count_reverse--) {

			ProteinDomain previous_domain = new_domain_list.get(count_reverse);
			if (previous_domain.name.toLowerCase().contains(TRANSMEMBRANE)) {

				if (protein.location != null && protein.location.equals(OUTER_MEMBRANE)) {
					if (predicted_protein_domain_name.toLowerCase().contains(INTERMEMBRANE)) {

						predicted_protein_domain_name = "TOPO_DOM - Cytoplasmic";

					} else if (predicted_protein_domain_name.toLowerCase().contains(CYTOSOL)) {

						predicted_protein_domain_name = "TOPO_DOM - Mitochondrial intermembrane";

					}
				} else if (protein.location != null && protein.location.equals(INNER_MEMBRANE)) {
					if (predicted_protein_domain_name.toLowerCase().contains(MATRIX)) {

						predicted_protein_domain_name = "TOPO_DOM - Mitochondrial intermembrane";

					} else if (predicted_protein_domain_name.toLowerCase().contains(INTERMEMBRANE)) {

						predicted_protein_domain_name = "TOPO_DOM - Mitochondrial matrix";

					}
				} else {

					if (predicted_protein_domain_name.toLowerCase().contains(CYTOSOL)) {
						predicted_protein_domain_name = "TOPO_DOM - Mitochondrial intermembrane";
						protein.location = OUTER_MEMBRANE;
					} else if (predicted_protein_domain_name.toLowerCase().contains(MATRIX)) {
						predicted_protein_domain_name = "TOPO_DOM - Mitochondrial intermembrane";
						protein.location = INNER_MEMBRANE;
					} else
						predicted_protein_domain_name = "Unknown";
				}
			}
		}
	}

	/**
	 * Method responsible for adding new protein domain based on 'transmem'
	 * information: IMM or OMM
	 * 
	 * @param new_domain_list current new domain list
	 * @param current_protein current protein
	 * @param next_transmem   next transmembrane domain
	 * @param startDomain     start of new protein domain
	 */
	private static void addPredictedDomainBasedOnOMMorIMMDomain(List<ProteinDomain> new_domain_list,
			Protein current_protein, ProteinDomain next_transmem, int startDomain) {

		ProteinDomain new_predicted_domain = null;
		if (current_protein.location != null && current_protein.location.equals(OUTER_MEMBRANE)) {
			if (predicted_protein_domain_name.toLowerCase().contains(INTERMEMBRANE)) {

				new_predicted_domain = new ProteinDomain("TOPO_DOM - Cytoplasmic", startDomain,
						next_transmem.startId - 1, true, "predicted", epochs);
				predicted_protein_domain_name = CYTOSOL;

			} else if (predicted_protein_domain_name.toLowerCase().contains(CYTOSOL)) {

				new_predicted_domain = new ProteinDomain("TOPO_DOM - Mitochondrial intermembrane", startDomain,
						next_transmem.startId - 1, true, "predicted", epochs);
				predicted_protein_domain_name = INTERMEMBRANE;

			}
		} else if (current_protein.location != null && current_protein.location.equals(INNER_MEMBRANE)) {
			if (predicted_protein_domain_name.toLowerCase().contains(MATRIX)) {

				new_predicted_domain = new ProteinDomain("TOPO_DOM - Mitochondrial intermembrane", startDomain,
						next_transmem.startId - 1, true, "predicted", epochs);
				predicted_protein_domain_name = INTERMEMBRANE;

			} else if (predicted_protein_domain_name.toLowerCase().contains(INTERMEMBRANE)) {

				new_predicted_domain = new ProteinDomain("TOPO_DOM - Mitochondrial matrix", startDomain,
						next_transmem.startId - 1, true, "predicted", epochs);
				predicted_protein_domain_name = MATRIX;

			}
		} else {
			if (predicted_protein_domain_name.toLowerCase().contains(CYTOSOL)) {

				new_predicted_domain = new ProteinDomain("TOPO_DOM - Mitochondrial intermembrane", startDomain,
						next_transmem.startId - 1, true, "predicted", epochs);
				predicted_protein_domain_name = INTERMEMBRANE;
				current_protein.location = OUTER_MEMBRANE;

			} else if (predicted_protein_domain_name.toLowerCase().contains(MATRIX)) {
				new_predicted_domain = new ProteinDomain("TOPO_DOM - Mitochondrial intermembrane", startDomain,
						next_transmem.startId - 1, true, "predicted", epochs);
				predicted_protein_domain_name = INTERMEMBRANE;
				current_protein.location = INNER_MEMBRANE;
			}

			else
				predicted_protein_domain_name = "";
		}

		if (new_predicted_domain != null)
			new_domain_list.add(new_predicted_domain);
	}

	/**
	 * Method responsible for processing and predicting all locations
	 * 
	 * @param taskMonitor            current task monitor
	 * @param old_number_uk_residues old number of unknown residue
	 */
	private void processRoundLocation(TaskMonitor taskMonitor, int old_number_uk_residues) {
//		do {

		// It is necessary to run two times to check if there are possible residues to
		// be predicted
		for (int times = 0; times < 2; times++) {

			if (!number_unknown_residues.containsKey(epochs)) {
				OrganizeResidueCompartment(taskMonitor);

				if (epochs == 1) {
					all_unknownResidues = compartments.get(UNKNOWN_RESIDUE);
					if (all_unknownResidues == null)
						return;// break;
				}

				if (compartments.get(UNKNOWN_RESIDUE) != null)
					number_unknown_residues.put(epochs, compartments.get(UNKNOWN_RESIDUE).size());
				else
					number_unknown_residues.put(epochs, 0);
			}

			all_knownResidues = getAllKnownResidues();

			int uk_res = number_unknown_residues.get(epochs);

			// It means there are no unknown residues to be predicted
			if (uk_res == 0) {
				hasMoreResidueToBePredicted = false;
				return;// break;
			}

			// It means there is no possibility to predict more residues location
			if (epochs > 1 && uk_res == old_number_uk_residues) {
				hasMoreResidueToBePredicted = false;
				return;// break;
			}

			if (Util.getEpochs && (Util.epochs + 1) <= epochs)
				return;// break;

			if (times == 1) {
				hasMoreResidueToBePredicted = true;
				break;
			}

			if (epochs == 1) {

				computeResiduesScoreForAllProteins(taskMonitor);
			}

			computeNewResidues(taskMonitor, all_unknownResidues, false);

			annotatePredictedLocation(taskMonitor, epochs);

			predictDomainsFromAllProteinsBasedOnTransmemInfo();
			UnifyProteinsDomains();

			computeFinalDomainScore(taskMonitor, false);
			restoreReactionSites(taskMonitor, myNetwork);
//			updateResidueScoresBasedOnDomainScore(taskMonitor);
			checkConflictProteinsDomains(taskMonitor);

			if (compartments.get(UNKNOWN_RESIDUE) != null)
				old_number_uk_residues = compartments.get(UNKNOWN_RESIDUE).size();
			else
				old_number_uk_residues = 0;

			epochs++;
		}
//		} while (compartments.containsKey(UNKNOWN_RESIDUE));
	}

	/**
	 * Method responsible for getting protein domain
	 * 
	 * @param protein      protein
	 * @param start_domain start domain
	 * @param end_domain   end domain
	 * @return domain
	 */
	public static ProteinDomain getProteinDomain(Protein protein, final int start_domain, final int end_domain) {

		if (protein.domains != null && protein.domains.size() > 0) {

			// eg: Matrix[10,20] => start_d = 12, end_d = 18
			Optional<ProteinDomain> isProteinDomainPresent = protein.domains.stream()
					.filter(value -> value.isValid && value.epoch == epochs && value.isValid
							&& value.startId <= start_domain && value.endId >= end_domain)
					.findFirst();

			if (isProteinDomainPresent.isPresent())
				return isProteinDomainPresent.get();
			else
				return null;
		} else
			return null;
	}

	/**
	 * Method responsible for checking if there is a specific protein domain
	 * 
	 * @param proteinDomains list of protein domains
	 * @param start_domain   start domain
	 * @param end_domain     end domain
	 * @return true if exists a protein domain
	 */
	public static boolean hasSimilarProteinDomain(List<ProteinDomain> proteinDomains, final int start_domain,
			final int end_domain) {

		if (proteinDomains != null && proteinDomains.size() > 0) {

			// eg: Matrix[10,20] => start_d = 12, end_d = 18
			List<ProteinDomain> candidates_domains = proteinDomains.stream()
					.filter(value -> value.isValid && value.startId <= start_domain && value.endId >= end_domain)
					.collect(Collectors.toList());

			if (candidates_domains.size() > 1)
				return true;
			else
				return false;
		} else
			return false;
	}

	/**
	 * Method responsible for organizing residues according to residue location
	 */
	public static void OrganizeResidueCompartment(TaskMonitor taskMonitor) {

		// Retrieve only candidate proteins for the current epoch (valid proteins)
		List<Protein> allProteins = Util.getProteins(myNetwork, true);

		if (allProteins == null || allProteins.size() == 0)
			return;

		compartments = new HashMap<String, List<Residue>>();

		int old_progress = 0;
		int summary_processed = 0;
		int total_ptns = allProteins.size();

		// Select only residues from proteins that don't contain conflict domains
		for (final Protein protein : allProteins.stream().filter(value -> !value.isConflictedDomain)
				.collect(Collectors.toList())) {

			List<Residue> residues = protein.reactionSites;

			if (residues == null)
				continue;

			for (Residue residue : residues) {

				if (residue.isConflicted)
					continue;

				int pos = residue.position;
				// Check if the current residue contains CSM
				long amount = 0;

				if (protein.intraLinks != null)
					amount = protein.intraLinks.stream()
							.filter(value -> value.pos_site_a == pos || value.pos_site_b == pos).count();
				if (protein.interLinks != null)
					amount += protein.interLinks.stream()
							.filter(value -> (value.protein_a.equals(protein.proteinID) && value.pos_site_a == pos)
									|| (value.protein_b.equals(protein.proteinID) && value.pos_site_b == pos))
							.count();

				boolean isValid = false;
				if (amount > 0) {

					if (Util.getSpecCount) {
						if (Util.specCount >= amount)
							isValid = true;
					} else {
						isValid = true;
					}
				}

				if (isValid) {
					if (!compartments.containsKey(residue.predictedLocation)) {
						List<Residue> list = new ArrayList<Residue>();
						list.add(residue);

						compartments.put(residue.predictedLocation, list);
					} else {
						compartments.get(residue.predictedLocation).add(residue);
					}
				}
			}

			summary_processed++;

			Util.progressBar(summary_processed, old_progress, total_ptns,
					"Epoch: " + epochs + "\nOrganizing residue compartments: ", taskMonitor, textLabel_status_result);

		}
	}

	/**
	 * Method responsible for computing initial residues scoring
	 * 
	 * @param protein current protein
	 */
	public static void computeResiduesScore(Protein protein) {

		if (protein == null || !protein.isValid)
			return;

		List<Residue> residues = protein.reactionSites;

		if (residues == null)
			return;

		// Residues that contain initialResidue or belong to Transmem are localization
		// markers
		for (Residue residue : residues.stream().filter(value -> !(value.score == Util.initialResidueScore
				|| value.location.toLowerCase().equals(TRANSMEMBRANE))).collect(Collectors.toList())) {

			// It means the score has already been computed
			if (-Math.log10(residue.score) < 0)
				continue;

			int pos = residue.position;

			List<CrossLink> all_links = new ArrayList<CrossLink>();
			if (protein.intraLinks != null) {
				all_links.addAll(
						protein.intraLinks.stream().filter(value -> value.pos_site_a == pos || value.pos_site_b == pos)
								.collect(Collectors.toList()));
			}
			if (protein.interLinks != null)
				all_links.addAll(protein.interLinks.stream()
						.filter(value -> (value.protein_a.equals(protein.proteinID) && value.pos_site_a == pos)
								|| (value.protein_b.equals(protein.proteinID) && value.pos_site_b == pos))
						.collect(Collectors.toList()));

			double residue_score = 0;
			int countValidLinks = 0;
			for (CrossLink crossLink : all_links) {
				Protein proteinA = Util.getProtein(myNetwork, crossLink.protein_a);
				if (proteinA != null && proteinA.isValid) {

					Protein proteinB = Util.getProtein(myNetwork, crossLink.protein_b);
					if (proteinB != null && proteinB.isValid) {
						residue_score += Math.pow(crossLink.score, 2);
						countValidLinks++;
					}
				}
			}
			if (residue_score > 0)
				residue.score = Math.sqrt(residue_score / countValidLinks);
		}
	}

	/**
	 * Method responsible for computing initial residues scoring
	 * 
	 * @param taskMonitor current task monitor
	 */
	private static void computeResiduesScoreForAllProteins(TaskMonitor taskMonitor) {

		// Sqrt(Sum[xl_score^2] / # xl)

		List<Protein> allProteins = Util.getProteins(myNetwork, true);

		if (allProteins == null || allProteins.size() == 0)
			return;

		int old_progress = 0;
		int summary_processed = 0;
		int total_ptns = allProteins.size();

		for (final Protein protein : allProteins) {

			summary_processed++;
			Util.progressBar(summary_processed, old_progress, total_ptns, "Computing residues score: ", taskMonitor,
					textLabel_status_result);

			computeResiduesScore(protein);
		}
	}

	/**
	 * Method responsible for computing new residue location
	 * 
	 * @param epochs
	 * @param taskMonitor
	 */
	private static void computeNewResidues(TaskMonitor taskMonitor, List<Residue> residue_collection,
			boolean isKnownResidues) {

		int old_progress = 0;
		int summary_processed = 0;
		int qtd_residues = compartments.values().stream().mapToInt(d -> d.size()).sum();
		int total_compartments = compartments.size() * qtd_residues;

		for (Map.Entry<String, List<Residue>> compartment : compartments.entrySet()) {
			try {

				if (compartment.getKey().equals(UNKNOWN_RESIDUE)) {
					summary_processed += compartment.getValue().size();
					continue;
				}

				for (Residue residue : compartment.getValue()) {

					summary_processed++;
					Util.progressBar(summary_processed, old_progress, total_compartments,
							"Epoch: " + epochs + "\nPredicting residue location: ", taskMonitor, null);

					if (residue.isConflicted)
						continue;

					Protein protein = residue.protein;

					if (protein.isConflictedDomain || !protein.isValid)
						continue;

					List<Residue> current_uk_residues = new ArrayList<Residue>();
					int pos = residue.position;

					List<CrossLink> links;

					// Check if the current residue contains CSM

					if (protein.intraLinks != null && protein.intraLinks.size() > 0) {

						links = (List<CrossLink>) protein.intraLinks.stream()
								.filter(value -> value.pos_site_a == pos || value.pos_site_b == pos)
								.collect(Collectors.toList());

						for (CrossLink crossLink : links) {

							// It means that the current residue is A
							if (crossLink.pos_site_a == pos) {
								Optional<Residue> isResiduePresent = residue_collection.stream()
										.filter(value -> value.protein.proteinID.equals(crossLink.protein_a)
												&& value.position == crossLink.pos_site_b && !value.isConflicted
												&& value.predicted_epoch == -1)
										.findFirst();
								if (isResiduePresent.isPresent()) {

									// current residue
									Residue res = isResiduePresent.get();
									if (!res.protein.isValid)
										continue;

									// Res.score contains the score that takes into account the crosslink.score

									double score = 0;

									if (-Math.log10(res.score) < 0)
										// if it's negative, it means that -Log10 has already been applied in previous
										// epochs
										score = Math.sqrt(res.score * residue.score / epochs);
									else
										// e.g. sqrt(((-log10 res_score) * previous_residue_score) / epochs)
										score = Math.sqrt(-Math.log10(res.score) * residue.score / epochs);

									boolean saveConflict = true;
									if (score >= res.score) {

										if (Util.considerConflict && ((!isKnownResidues
												&& !res.predictedLocation.equals(residue.predictedLocation))
												|| (isKnownResidues && !res.location.equals(residue.location)))) {

											if (!(isKnownResidues && res.score == Util.initialResidueScore))
												saveConflict = false;

											if (saveConflict && residue.predictedLocation.toLowerCase()
													.contains(TRANSMEMBRANE)) {
												Protein current_ptn_rs = residue.protein;
												List<Residue> neighbors = current_ptn_rs.reactionSites.stream()
														.filter(value -> value.position >= (residue.position
																- Util.transmemNeighborAA)
																&& value.position <= (residue.position
																		+ Util.transmemNeighborAA))
														.collect(Collectors.toList());

												if (neighbors.size() > 0) {

													// If there are only transmembrane regions, then there is no
													// conflict
													if (neighbors.stream()
															.filter(value -> value.predictedLocation.toLowerCase()
																	.equals(TRANSMEMBRANE))
															.collect(Collectors.toList()).size() == neighbors.size()) {
														saveConflict = false;
													} else {
														for (Residue neighbor : neighbors) {
															if (!neighbor.predictedLocation.toLowerCase()
																	.equals(TRANSMEMBRANE)) {
																res.score = score;
																res.predictedLocation = neighbor.location;
																current_uk_residues.add(res);
																crossLink.location = neighbor.location;
																saveConflict = false;
																break;
															}
														}
													}
												}
											} else if (saveConflict && residue.predictedLocation.toLowerCase()
													.contains(TRANSMEMBRANE)) {
												saveConflict = false;

											} else if (!isKnownResidues
													&& !res.predictedLocation.equals(residue.predictedLocation)
													&& !residue.predictedLocation.toLowerCase().equals(TRANSMEMBRANE)) {
												// It will save the new prediction if the current process is not 'known
												// residues'
												res.score = score;
												current_uk_residues.add(res);
												crossLink.location = residue.predictedLocation;
											}
										} else {
											saveConflict = false;

											if (residue.predictedLocation.toLowerCase().contains(TRANSMEMBRANE)) {
												Protein current_ptn_rs = residue.protein;
												List<ProteinDomain> neighbors = current_ptn_rs.domains.stream()
														.filter(value -> value.isValid
																&& value.startId <= (residue.position
																		+ Util.transmemNeighborAA)
																&& value.startId >= (residue.position
																		- Util.transmemNeighborAA))
														.collect(Collectors.toList());

												if (neighbors.size() > 0) {
													for (ProteinDomain neighbor : neighbors) {
														if (!neighbor.name.toLowerCase().contains(TRANSMEMBRANE)) {
															res.score = score;
															crossLink.location = neighbor.name;
															saveConflict = false;

															if (res.predicted_epoch != epochs) {
																Residue current_res = new Residue(residue.aminoacid,
																		residue.location, residue.position,
																		residue.protein);
																current_res.history_residues = residue.history_residues;
																current_res.predicted_epoch = residue.predicted_epoch;
																current_res.predictedLocation = residue.predictedLocation;
																current_res.previous_residue = residue.previous_residue;
																current_res.score = residue.score;
																current_res.isConflicted = residue.isConflicted;
																current_res.conflicted_residue = residue.conflicted_residue;
																res.addHistoryResidue(current_res);
															}

															res.predictedLocation = neighbor.name;
															res.predicted_epoch = epochs;
															res.previous_residue = residue;

															break;
														}
													}
												}
											} else if (!isKnownResidues
													&& !res.predictedLocation.equals(residue.predictedLocation)
													&& !residue.predictedLocation.toLowerCase().equals(TRANSMEMBRANE)) {
												// It will save the new prediction if the current process is not 'known
												// residues'
												res.score = score;
												current_uk_residues.add(res);
												crossLink.location = residue.predictedLocation;
											}
										}

										if (saveConflict) {

											boolean isValid = false;

											if (Util.getThreshold_score) {
												if (Util.threshold_score >= -Math.log10(res.score)) {
													isValid = true;
													crossLink.location = "";
												}
											} else if (!isKnownResidues
													&& Math.abs(-Math.log10(res.score) - score) > Util.deltaScore) {
												isValid = true;
											} else {
												isValid = false;
											}

											if (!isValid) {
												// target residue
												res.isConflicted = true;
												res.conflicted_residue = residue;
												res.conflicted_score = score;

												// source residue
												residue.isConflicted = true;
												residue.conflicted_residue = res;
												residue.conflicted_score = score;
											}
										}
									}
								} else {// It means the target residue has been already predicted -> It's a conflict
									Optional<Residue> current_residue_isPresent = all_knownResidues.stream()
											.filter(value -> !value.isConflicted
													&& value.position == crossLink.pos_site_b
													&& value.protein.proteinID.equals(crossLink.protein_a))
											.findFirst();
									if (current_residue_isPresent.isPresent()) {
										Residue current_residue = current_residue_isPresent.get();
										if (!current_residue.protein.isValid)
											continue;

										if (!residue.location.equals(current_residue.location) || (residue.location
												.equals(UNKNOWN_RESIDUE)
												&& !residue.predictedLocation.equals(current_residue.location))) {

											boolean isConflict = true;

											if (residue.location.toLowerCase().contains(TRANSMEMBRANE)) {
												Protein current_ptn_rs = residue.protein;
												List<ProteinDomain> neighbors = current_ptn_rs.domains.stream()
														.filter(value -> value.isValid && !value.isPredicted
																&& value.startId <= (residue.position
																		+ Util.transmemNeighborAA)
																&& value.startId >= (residue.position
																		- Util.transmemNeighborAA))
														.collect(Collectors.toList());

												if (neighbors.size() > 0) {

													boolean thereIsOnlyTransmem = true;

													for (ProteinDomain neighbor : neighbors) {
														if (!neighbor.name.toLowerCase().contains(TRANSMEMBRANE)) {

															thereIsOnlyTransmem = false;
															if (neighbor.name.equals(current_residue.location)) {
																isConflict = false;

																if (residue.score != Util.initialResidueScore) {
																	residue.location = neighbor.name;
																	residue.predictedLocation = neighbor.name;
																	residue.predicted_epoch = epochs;

																	double score = Math
																			.sqrt((-Math.log10(crossLink.score)
																					* current_residue.score) / epochs);
																	residue.score = score;
																}

																break;
															}
														}
													}
													if (thereIsOnlyTransmem)
														isConflict = false;
												} else
													// It means there is no conflicted neighbor
													isConflict = false;
											}

											if (isConflict) {

												double score = 0;
												double residue_score = residue.score > 0 ? residue.score : 1;

												if (-Math.log10(current_residue.score) < 0)
													// if it's negative, it means that -Log10 has already been
													// applied
													// in
													// previous
													// epochs
													score = Math.sqrt(current_residue.score * residue_score / epochs);
												else
													// e.g. sqrt(((-log10 res_score) * previous_residue_score) /
													// epochs)
													score = Math.sqrt(-Math.log10(current_residue.score) * residue_score
															/ epochs);

												// source residue
												residue.isConflicted = true;
												residue.conflicted_residue = current_residue;
												residue.conflicted_score = score;
												residue.predicted_epoch = epochs;

												// target residue
												residue.conflicted_residue.isConflicted = true;
												residue.conflicted_residue.conflicted_residue = residue;
												residue.conflicted_residue.conflicted_score = score;
												residue.conflicted_residue.predicted_epoch = epochs;
											}
										}
									}
								}
							} else {// It means the current residue is B

								Optional<Residue> isResiduePresent = residue_collection.stream()
										.filter(value -> value.protein.proteinID.equals(crossLink.protein_a)
												&& value.position == crossLink.pos_site_a && !value.isConflicted
												&& value.predicted_epoch == -1)
										.findFirst();
								if (isResiduePresent.isPresent()) {
									// current residue
									Residue res = isResiduePresent.get();
									if (!res.protein.isValid)
										continue;

									// Res.score contains the score that takes into account the crosslink.score

									double score = 0;

									if (-Math.log10(res.score) < 0)
										// if it's negative, it means that -Log10 has already been applied in previous
										// epochs
										score = Math.sqrt(res.score * residue.score / epochs);
									else
										// e.g. sqrt(((-log10 res_score) * previous_residue_score) / epochs)
										score = Math.sqrt(-Math.log10(res.score) * residue.score / epochs);

									boolean saveConflict = true;
									if (score >= res.score) {

										if (Util.considerConflict && ((!isKnownResidues
												&& !res.predictedLocation.equals(residue.predictedLocation))
												|| (isKnownResidues && !res.location.equals(residue.location)))) {

											if (!(isKnownResidues && res.score == Util.initialResidueScore))
												saveConflict = false;

											if (saveConflict && residue.predictedLocation.toLowerCase()
													.contains(TRANSMEMBRANE)) {
												Protein current_ptn_rs = residue.protein;
												List<Residue> neighbors = current_ptn_rs.reactionSites.stream()
														.filter(value -> value.position >= (residue.position
																- Util.transmemNeighborAA)
																&& value.position <= (residue.position
																		+ Util.transmemNeighborAA))
														.collect(Collectors.toList());

												if (neighbors.size() > 0) {

													// If there are only transmembrane regions, then there is no
													// conflict
													if (neighbors.stream()
															.filter(value -> value.predictedLocation.toLowerCase()
																	.equals(TRANSMEMBRANE))
															.collect(Collectors.toList()).size() == neighbors.size()) {
														saveConflict = false;
													} else {
														for (Residue neighbor : neighbors) {
															if (!neighbor.predictedLocation.toLowerCase()
																	.equals(TRANSMEMBRANE)) {
																res.score = score;
																res.predictedLocation = neighbor.location;
																current_uk_residues.add(res);
																crossLink.location = neighbor.location;
																saveConflict = false;
																break;
															}
														}
													}
												}
											} else if (saveConflict
													&& res.predictedLocation.toLowerCase().contains(TRANSMEMBRANE)) {
												saveConflict = false;

											} else if (!isKnownResidues
													&& !res.predictedLocation.equals(residue.predictedLocation)
													&& !residue.predictedLocation.toLowerCase().equals(TRANSMEMBRANE)) {
												// It will save the new prediction if the current process is not 'known
												// residues'
												res.score = score;
												current_uk_residues.add(res);
												crossLink.location = residue.predictedLocation;
											}
										} else {
											saveConflict = false;

											if (residue.predictedLocation.toLowerCase().contains(TRANSMEMBRANE)) {
												Protein current_ptn_rs = residue.protein;
												List<ProteinDomain> neighbors = current_ptn_rs.domains.stream()
														.filter(value -> value.startId >= (residue.position
																- Util.transmemNeighborAA)
																|| value.endId <= (residue.position
																		+ Util.transmemNeighborAA))
														.collect(Collectors.toList());

												if (neighbors.size() > 0) {
													for (ProteinDomain neighbor : neighbors) {
														if (!neighbor.name.toLowerCase().contains(TRANSMEMBRANE)) {
															res.score = score;
															crossLink.location = neighbor.name;
															saveConflict = false;

															if (res.predicted_epoch != epochs) {
																Residue current_res = new Residue(residue.aminoacid,
																		residue.location, residue.position,
																		residue.protein);
																current_res.history_residues = residue.history_residues;
																current_res.predicted_epoch = residue.predicted_epoch;
																current_res.predictedLocation = residue.predictedLocation;
																current_res.previous_residue = residue.previous_residue;
																current_res.score = residue.score;
																current_res.isConflicted = residue.isConflicted;
																current_res.conflicted_residue = residue.conflicted_residue;
																res.addHistoryResidue(current_res);
															}

															res.predictedLocation = neighbor.name;
															res.predicted_epoch = epochs;
															res.previous_residue = residue;

															break;
														}
													}
												}
											} else if (!isKnownResidues
													&& !res.predictedLocation.equals(residue.predictedLocation)
													&& !residue.predictedLocation.toLowerCase().equals(TRANSMEMBRANE)) {
												// It will save the new prediction if the current process is not 'known
												// residues'
												res.score = score;
												current_uk_residues.add(res);
												crossLink.location = residue.predictedLocation;
											}
										}

										if (saveConflict) {

											boolean isValid = false;

											if (Util.getThreshold_score) {
												if (Util.threshold_score >= -Math.log10(res.score)) {
													isValid = true;
													crossLink.location = "";
												}
											} else if (!isKnownResidues
													&& Math.abs(-Math.log10(res.score) - score) > Util.deltaScore) {
												isValid = true;
											} else {
												isValid = false;
											}

											if (!isValid) {
												// target residue
												res.isConflicted = true;
												res.conflicted_residue = residue;
												res.conflicted_score = score;

												// source residue
												residue.isConflicted = true;
												residue.conflicted_residue = res;
												residue.conflicted_score = score;
											}
										}
									}
								} else {// It means the target residue has been already predicted -> It's a conflict
									Optional<Residue> current_residue_isPresent = all_knownResidues.stream()
											.filter(value -> !value.isConflicted
													&& value.position == crossLink.pos_site_a
													&& value.protein.proteinID.equals(crossLink.protein_a))
											.findFirst();
									if (current_residue_isPresent.isPresent()) {
										Residue current_residue = current_residue_isPresent.get();
										if (!current_residue.protein.isValid)
											continue;

										if (!residue.location.equals(current_residue.location) || (residue.location
												.equals(UNKNOWN_RESIDUE)
												&& !residue.predictedLocation.equals(current_residue.location))) {

											boolean isConflict = true;

											if (residue.location.toLowerCase().contains(TRANSMEMBRANE)) {
												Protein current_ptn_rs = residue.protein;
												List<ProteinDomain> neighbors = current_ptn_rs.domains.stream()
														.filter(value -> value.isValid && !value.isPredicted
																&& value.startId <= (residue.position
																		+ Util.transmemNeighborAA)
																&& value.startId >= (residue.position
																		- Util.transmemNeighborAA))
														.collect(Collectors.toList());

												if (neighbors.size() > 0) {

													boolean thereIsOnlyTransmem = true;

													for (ProteinDomain neighbor : neighbors) {
														if (!neighbor.name.toLowerCase().contains(TRANSMEMBRANE)) {

															thereIsOnlyTransmem = false;
															if (neighbor.name.equals(current_residue.location)) {
																isConflict = false;

																if (residue.score != Util.initialResidueScore) {
																	residue.location = neighbor.name;
																	residue.predictedLocation = neighbor.name;
																	residue.predicted_epoch = epochs;

																	double score = Math
																			.sqrt((-Math.log10(crossLink.score)
																					* current_residue.score) / epochs);
																	residue.score = score;
																}

																break;
															}
														}
													}
													if (thereIsOnlyTransmem)
														isConflict = false;
												} else
													// It means there is no conflicted neighbor
													isConflict = false;
											}

											if (isConflict) {

												double score = 0;
												double residue_score = residue.score > 0 ? residue.score : 1;

												if (-Math.log10(current_residue.score) < 0)
													// if it's negative, it means that -Log10 has already been
													// applied
													// in
													// previous
													// epochs
													score = Math.sqrt(current_residue.score * residue_score / epochs);
												else
													// e.g. sqrt(((-log10 res_score) * previous_residue_score) /
													// epochs)
													score = Math.sqrt(-Math.log10(current_residue.score) * residue_score
															/ epochs);

												// source residue
												residue.isConflicted = true;
												residue.conflicted_residue = current_residue;
												residue.conflicted_score = score;
												residue.predicted_epoch = epochs;

												// target residue
												residue.conflicted_residue.isConflicted = true;
												residue.conflicted_residue.conflicted_residue = residue;
												residue.conflicted_residue.conflicted_score = score;
												residue.conflicted_residue.predicted_epoch = epochs;
											}
										}
									}
								}
							}
						}
					}

					if (protein.interLinks != null && protein.interLinks.size() > 0) {

						links = (List<CrossLink>) protein.interLinks.stream()
								.filter(value -> (value.protein_a.equals(protein.proteinID) && value.pos_site_a == pos)
										|| (value.protein_b.equals(protein.proteinID) && value.pos_site_b == pos))
								.collect(Collectors.toList());

						for (CrossLink crossLink : links) {

							// It means the current residue is A
							if (crossLink.protein_a.equals(protein.proteinID) && crossLink.pos_site_a == pos) {

								Optional<Residue> isResiduePresent = residue_collection.stream()
										.filter(value -> value.protein.proteinID.equals(crossLink.protein_b)
												&& value.position == crossLink.pos_site_b && !value.isConflicted
												&& value.predicted_epoch == -1)
										.findFirst();

								if (isResiduePresent.isPresent()) {
									// current residue
									Residue res = isResiduePresent.get();
									if (!res.protein.isValid)
										continue;

									// Res.score contains the score that takes into account the crosslink.score

									double score = 0;

									if (-Math.log10(res.score) < 0)
										// if it's negative, it means that -Log10 has already been applied in previous
										// epochs
										score = Math.sqrt(res.score * residue.score / epochs);
									else
										// e.g. sqrt(((-log10 res_score) * previous_residue_score) / epochs)
										score = Math.sqrt(-Math.log10(res.score) * residue.score / epochs);

									boolean saveConflict = true;
									if (score >= res.score) {

										if (Util.considerConflict && ((!isKnownResidues
												&& !res.predictedLocation.equals(residue.predictedLocation))
												|| (isKnownResidues && !res.location.equals(residue.location)))) {

											if (!(isKnownResidues && res.score == Util.initialResidueScore))
												saveConflict = false;

											if (saveConflict
													&& res.predictedLocation.toLowerCase().contains(TRANSMEMBRANE)) {
												Protein current_ptn_rs = res.protein;
												List<Residue> neighbors = current_ptn_rs.reactionSites.stream()
														.filter(value -> value.position >= (res.position
																- Util.transmemNeighborAA)
																&& value.position <= (res.position
																		+ Util.transmemNeighborAA))
														.collect(Collectors.toList());

												if (neighbors.size() > 0) {

													// If there are only transmembrane regions, then there is no
													// conflict
													if (neighbors.stream()
															.filter(value -> value.predictedLocation.toLowerCase()
																	.equals(TRANSMEMBRANE))
															.collect(Collectors.toList()).size() == neighbors.size()) {
														saveConflict = false;
													} else {
														for (Residue neighbor : neighbors) {
															if (!neighbor.predictedLocation.toLowerCase()
																	.equals(TRANSMEMBRANE)) {
																res.score = score;
																res.predictedLocation = neighbor.location;
																current_uk_residues.add(res);
																crossLink.location = neighbor.location;
																saveConflict = false;
																break;
															}
														}
													}
												}
											} else if (saveConflict && residue.predictedLocation.toLowerCase()
													.contains(TRANSMEMBRANE)) {
												saveConflict = false;

											} else if (!isKnownResidues
													&& !res.predictedLocation.equals(residue.predictedLocation)
													&& !residue.predictedLocation.toLowerCase().equals(TRANSMEMBRANE)) {
												// It will save the new prediction if the current process is not 'known
												// residues'
												res.score = score;
												current_uk_residues.add(res);
												crossLink.location = residue.predictedLocation;
											}
										} else {
											saveConflict = false;

											if (residue.predictedLocation.toLowerCase().contains(TRANSMEMBRANE)) {
												Protein current_ptn_rs = residue.protein;
												List<ProteinDomain> neighbors = current_ptn_rs.domains.stream()
														.filter(value -> value.isValid
																&& value.startId <= (res.position
																		+ Util.transmemNeighborAA)
																&& value.startId >= (res.position
																		- Util.transmemNeighborAA))
														.collect(Collectors.toList());

												if (neighbors.size() > 0) {
													for (ProteinDomain neighbor : neighbors) {
														if (!neighbor.name.toLowerCase().contains(TRANSMEMBRANE)) {
															res.score = score;
															crossLink.location = neighbor.name;
															saveConflict = false;

															if (res.predicted_epoch != epochs) {
																Residue current_res = new Residue(residue.aminoacid,
																		residue.location, residue.position,
																		residue.protein);
																current_res.history_residues = residue.history_residues;
																current_res.predicted_epoch = residue.predicted_epoch;
																current_res.predictedLocation = residue.predictedLocation;
																current_res.previous_residue = residue.previous_residue;
																current_res.score = residue.score;
																current_res.isConflicted = residue.isConflicted;
																current_res.conflicted_residue = residue.conflicted_residue;
																res.addHistoryResidue(current_res);
															}

															res.predictedLocation = neighbor.name;
															res.predicted_epoch = epochs;
															res.previous_residue = residue;

															break;
														}
													}
												}
											} else if (!isKnownResidues
													&& !res.predictedLocation.equals(residue.predictedLocation)
													&& !residue.predictedLocation.toLowerCase().equals(TRANSMEMBRANE)) {
												// It will save the new prediction if the current process is not 'known
												// residues'
												res.score = score;
												current_uk_residues.add(res);
												crossLink.location = residue.predictedLocation;
											}
										}

										if (saveConflict) {

											boolean isValid = false;

											if (Util.getThreshold_score) {
												if (Util.threshold_score >= -Math.log10(res.score)) {
													isValid = true;
													crossLink.location = "";
												}
											} else if (!isKnownResidues
													&& Math.abs(-Math.log10(res.score) - score) > Util.deltaScore) {
												isValid = true;
											} else {
												isValid = false;
											}

											if (!isValid) {
												// target residue
												res.isConflicted = true;
												res.conflicted_residue = residue;
												res.conflicted_score = score;

												// source residue
												residue.isConflicted = true;
												residue.conflicted_residue = res;
												residue.conflicted_score = score;
											}
										}
									}
								} else {// It means the target residue has been already predicted -> It's a conflict
									Optional<Residue> current_residue_isPresent = all_knownResidues.stream()
											.filter(value -> !value.isConflicted
													&& value.position == crossLink.pos_site_b
													&& value.protein.proteinID.equals(crossLink.protein_b))
											.findFirst();
									if (current_residue_isPresent.isPresent()) {
										Residue current_residue = current_residue_isPresent.get();
										if (!current_residue.protein.isValid)
											continue;

										if (!residue.location.equals(current_residue.location) || (residue.location
												.equals(UNKNOWN_RESIDUE)
												&& !residue.predictedLocation.equals(current_residue.location))) {

											boolean isConflict = true;

											if (residue.location.toLowerCase().contains(TRANSMEMBRANE)) {
												Protein current_ptn_rs = residue.protein;
												List<ProteinDomain> neighbors = current_ptn_rs.domains.stream()
														.filter(value -> value.isValid && !value.isPredicted
																&& value.startId <= (residue.position
																		+ Util.transmemNeighborAA)
																&& value.startId >= (residue.position
																		- Util.transmemNeighborAA))
														.collect(Collectors.toList());

												if (neighbors.size() > 0) {

													boolean thereIsOnlyTransmem = true;

													for (ProteinDomain neighbor : neighbors) {
														if (!neighbor.name.toLowerCase().contains(TRANSMEMBRANE)) {

															thereIsOnlyTransmem = false;
															if (neighbor.name.equals(current_residue.location)) {
																isConflict = false;

																if (residue.score != Util.initialResidueScore) {
																	residue.location = neighbor.name;
																	residue.predictedLocation = neighbor.name;
																	residue.predicted_epoch = epochs;

																	double score = Math
																			.sqrt((-Math.log10(crossLink.score)
																					* current_residue.score) / epochs);
																	residue.score = score;
																}

																break;
															}
														}
													}
													if (thereIsOnlyTransmem)
														isConflict = false;
												} else
													// It means there is no conflicted neighbor
													isConflict = false;
											}

											if (isConflict) {

												double score = 0;
												double residue_score = residue.score > 0 ? residue.score : 1;

												if (-Math.log10(current_residue.score) < 0)
													// if it's negative, it means that -Log10 has already been
													// applied
													// in
													// previous
													// epochs
													score = Math.sqrt(current_residue.score * residue_score / epochs);
												else
													// e.g. sqrt(((-log10 res_score) * previous_residue_score) /
													// epochs)
													score = Math.sqrt(-Math.log10(current_residue.score) * residue_score
															/ epochs);

												// source residue
												residue.isConflicted = true;
												residue.conflicted_residue = current_residue;
												residue.conflicted_score = score;
												residue.predicted_epoch = epochs;

												// target residue
												residue.conflicted_residue.isConflicted = true;
												residue.conflicted_residue.conflicted_residue = residue;
												residue.conflicted_residue.conflicted_score = score;
												residue.conflicted_residue.predicted_epoch = epochs;
											}
										}
									}
								}
							} else {// It means the current residue is B

								Optional<Residue> isResiduePresent = residue_collection.stream()
										.filter(value -> value.protein.proteinID.equals(crossLink.protein_a)
												&& value.position == crossLink.pos_site_a && !value.isConflicted
												&& value.predicted_epoch == -1)
										.findFirst();
								if (isResiduePresent.isPresent()) {
									// current residue
									Residue res = isResiduePresent.get();
									if (!res.protein.isValid)
										continue;

									// Res.score contains the score that takes into account the crosslink.score

									double score = 0;

									if (-Math.log10(res.score) < 0)
										// if it's negative, it means that -Log10 has already been applied in previous
										// epochs
										score = Math.sqrt(res.score * residue.score / epochs);
									else
										// e.g. sqrt(((-log10 res_score) * previous_residue_score) / epochs)
										score = Math.sqrt(-Math.log10(res.score) * residue.score / epochs);

									boolean saveConflict = true;
									if (score >= res.score) {

										if (Util.considerConflict && ((!isKnownResidues
												&& !res.predictedLocation.equals(residue.predictedLocation))
												|| (isKnownResidues && !res.location.equals(residue.location)))) {

											if (!(isKnownResidues && res.score == Util.initialResidueScore))
												saveConflict = false;

											if (saveConflict && residue.predictedLocation.toLowerCase()
													.contains(TRANSMEMBRANE)) {
												Protein current_ptn_rs = residue.protein;
												List<Residue> neighbors = current_ptn_rs.reactionSites.stream()
														.filter(value -> value.position >= (residue.position
																- Util.transmemNeighborAA)
																&& value.position <= (residue.position
																		+ Util.transmemNeighborAA))
														.collect(Collectors.toList());

												if (neighbors.size() > 0) {

													// If there are only transmembrane regions, then there is no
													// conflict
													if (neighbors.stream()
															.filter(value -> value.predictedLocation.toLowerCase()
																	.equals(TRANSMEMBRANE))
															.collect(Collectors.toList()).size() == neighbors.size()) {
														saveConflict = false;
													} else {
														for (Residue neighbor : neighbors) {
															if (!neighbor.predictedLocation.toLowerCase()
																	.equals(TRANSMEMBRANE)) {
																res.score = score;
																res.predictedLocation = neighbor.location;
																current_uk_residues.add(res);
																crossLink.location = neighbor.location;
																saveConflict = false;
																break;
															}
														}
													}
												}
											} else if (saveConflict
													&& res.predictedLocation.toLowerCase().contains(TRANSMEMBRANE)) {
												saveConflict = false;

											} else if (!isKnownResidues
													&& !res.predictedLocation.equals(residue.predictedLocation)
													&& !residue.predictedLocation.toLowerCase().equals(TRANSMEMBRANE)) {
												// It will save the new prediction if the current process is not 'known
												// residues'
												res.score = score;
												current_uk_residues.add(res);
												crossLink.location = residue.predictedLocation;
											}
										} else {
											saveConflict = false;

											if (residue.predictedLocation.toLowerCase().contains(TRANSMEMBRANE)) {
												Protein current_ptn_rs = residue.protein;
												List<ProteinDomain> neighbors = current_ptn_rs.domains.stream()
														.filter(value -> value.startId <= (residue.position
																+ Util.transmemNeighborAA)
																&& value.startId >= (residue.position
																		- Util.transmemNeighborAA))
														.collect(Collectors.toList());

												if (neighbors.size() > 0) {
													for (ProteinDomain neighbor : neighbors) {
														if (!neighbor.name.toLowerCase().contains(TRANSMEMBRANE)) {
															res.score = score;
															crossLink.location = neighbor.name;
															saveConflict = false;

															if (res.predicted_epoch != epochs) {
																Residue current_res = new Residue(residue.aminoacid,
																		residue.location, residue.position,
																		residue.protein);
																current_res.history_residues = residue.history_residues;
																current_res.predicted_epoch = residue.predicted_epoch;
																current_res.predictedLocation = residue.predictedLocation;
																current_res.previous_residue = residue.previous_residue;
																current_res.score = residue.score;
																current_res.isConflicted = residue.isConflicted;
																current_res.conflicted_residue = residue.conflicted_residue;
																res.addHistoryResidue(current_res);
															}

															res.predictedLocation = neighbor.name;
															res.predicted_epoch = epochs;
															res.previous_residue = residue;

															break;
														}
													}
												}
											} else if (!isKnownResidues
													&& !res.predictedLocation.equals(residue.predictedLocation)
													&& !residue.predictedLocation.toLowerCase().equals(TRANSMEMBRANE)) {
												// It will save the new prediction if the current process is not 'known
												// residues'
												res.score = score;
												current_uk_residues.add(res);
												crossLink.location = residue.predictedLocation;
											}
										}

										if (saveConflict) {

											boolean isValid = false;

											if (Util.getThreshold_score) {
												if (Util.threshold_score >= -Math.log10(res.score)) {
													isValid = true;
													crossLink.location = "";
												}
											} else if (!isKnownResidues
													&& Math.abs(-Math.log10(res.score) - score) > Util.deltaScore) {
												isValid = true;
											} else {
												isValid = false;
											}

											if (!isValid) {
												// target residue
												res.isConflicted = true;
												res.conflicted_residue = residue;
												res.conflicted_score = score;

												// source residue
												residue.isConflicted = true;
												residue.conflicted_residue = res;
												residue.conflicted_score = score;
											}
										}
									}
								} else {// It means the target residue has been already predicted -> It's a conflict
									Optional<Residue> current_residue_isPresent = all_knownResidues.stream()
											.filter(value -> !value.isConflicted
													&& value.position == crossLink.pos_site_a
													&& value.protein.proteinID.equals(crossLink.protein_a))
											.findFirst();
									if (current_residue_isPresent.isPresent()) {
										Residue current_residue = current_residue_isPresent.get();
										if (!current_residue.protein.isValid)
											continue;

										if (!residue.location.equals(current_residue.location) || (residue.location
												.equals(UNKNOWN_RESIDUE)
												&& !residue.predictedLocation.equals(current_residue.location))) {

											boolean isConflict = true;

											if (residue.location.toLowerCase().contains(TRANSMEMBRANE)) {
												Protein current_ptn_rs = residue.protein;
												List<ProteinDomain> neighbors = current_ptn_rs.domains.stream()
														.filter(value -> value.isValid && !value.isPredicted
																&& value.startId <= (residue.position
																		+ Util.transmemNeighborAA)
																&& value.startId >= (residue.position
																		- Util.transmemNeighborAA))
														.collect(Collectors.toList());

												if (neighbors.size() > 0) {

													boolean thereIsOnlyTransmem = true;

													for (ProteinDomain neighbor : neighbors) {
														if (!neighbor.name.toLowerCase().contains(TRANSMEMBRANE)) {

															thereIsOnlyTransmem = false;
															if (neighbor.name.equals(current_residue.location)) {
																isConflict = false;

																if (residue.score != Util.initialResidueScore) {
																	residue.location = neighbor.name;
																	residue.predictedLocation = neighbor.name;
																	residue.predicted_epoch = epochs;

																	double score = Math
																			.sqrt((-Math.log10(crossLink.score)
																					* current_residue.score) / epochs);
																	residue.score = score;
																}

																break;
															}
														}
													}
													if (thereIsOnlyTransmem)
														isConflict = false;
												} else
													// It means there is no conflicted neighbor
													isConflict = false;
											}

											if (isConflict) {

												double score = 0;
												double residue_score = residue.score > 0 ? residue.score : 1;

												if (-Math.log10(current_residue.score) < 0)
													// if it's negative, it means that -Log10 has already been
													// applied
													// in
													// previous
													// epochs
													score = Math.sqrt(current_residue.score * residue_score / epochs);
												else
													// e.g. sqrt(((-log10 res_score) * previous_residue_score) /
													// epochs)
													score = Math.sqrt(-Math.log10(current_residue.score) * residue_score
															/ epochs);

												// source residue
												residue.isConflicted = true;
												residue.conflicted_residue = current_residue;
												residue.conflicted_score = score;
												residue.predicted_epoch = epochs;

												// target residue
												residue.conflicted_residue.isConflicted = true;
												residue.conflicted_residue.conflicted_residue = residue;
												residue.conflicted_residue.conflicted_score = score;
												residue.conflicted_residue.predicted_epoch = epochs;
											}
										}
									}
								}
							}
						}
					}

					if (!residue.isConflicted) {
						for (Residue uk_residue : current_uk_residues) {

							boolean isValid = false;

							if (Util.getThreshold_score) {
								if (Util.threshold_score >= -Math.log10(uk_residue.score))
									isValid = true;
							} else {
								isValid = true;
							}

							if (isValid) {

								if (uk_residue.predicted_epoch != epochs) {
									Residue current_res = new Residue(residue.aminoacid, residue.location,
											residue.position, residue.protein);
									current_res.history_residues = residue.history_residues;
									current_res.predicted_epoch = residue.predicted_epoch;
									current_res.predictedLocation = residue.predictedLocation;
									current_res.previous_residue = residue.previous_residue;
									current_res.score = residue.score;
									current_res.isConflicted = residue.isConflicted;
									current_res.conflicted_residue = residue.conflicted_residue;
									uk_residue.addHistoryResidue(current_res);
								}

								// If predicted location is transmem, it means that uk_residue was assessed as
								// the neighbor location
								if (!residue.predictedLocation.toLowerCase().equals(TRANSMEMBRANE))
									uk_residue.predictedLocation = residue.predictedLocation;
								uk_residue.predicted_epoch = epochs;
								uk_residue.previous_residue = residue;
							}
						}
					}
				}
			} catch (Exception e) {
				System.out.println("ERROR: computeNewResidues -> index:" + summary_processed);
			}
		}

		taskMonitor.showMessage(TaskMonitor.Level.INFO, "Epoch: " + epochs + "\nPredicting residue location: 100%");
	}

	/**
	 * Method responsible for removing unknown protein domains
	 */
	private void removeUnknownProteinDomains() {

		List<Protein> allProteins = Util.getProteins(myNetwork, true);
		if (allProteins == null || allProteins.size() == 0)
			return;

		for (Protein protein : allProteins) {

			if (protein.domains != null && protein.domains.size() > 0) {
				if (protein.domains.stream().filter(value -> value.isPredicted).collect(Collectors.toList()).size() > 0)
					protein.isPredictedBasedOnTransmemInfo = false;

				protein.domains.removeIf(value -> value.isValid && !value.name.toLowerCase().equals(TRANSMEMBRANE)
						&& value.isPredicted && value.epoch >= epochs);
			}
		}
	}

	/**
	 * Method responsible for checking transmem score
	 */
	private void checkTransmemValidity() {
		List<Protein> allProteins = Util.getProteins(myNetwork, true);

		if (allProteins == null || allProteins.size() == 0)
			return;

		for (Protein protein : allProteins) {

			if (protein.domains == null)
				continue;

			// Iterate only over transmembrane domains
			for (ProteinDomain domain : protein.domains) {

				if (domain.name.toLowerCase().contains(TRANSMEMBRANE)
						&& !(domain.eValue.isBlank() || domain.eValue.isEmpty())
						&& Double.parseDouble(domain.eValue) < Util.transmemPredictionRegionsUpperScore)
					domain.isValid = false;

			}
		}
	}

	/**
	 * Method responsible for validating all transm regions
	 */
	private void turnValidAllTransmembraneDomains() {

		List<Protein> allProteins = Util.getProteins(myNetwork, true);

		if (allProteins == null || allProteins.size() == 0)
			return;

		for (Protein protein : allProteins) {

			if (protein.domains == null)
				continue;

			// Iterate only over transmembrane domains
			for (ProteinDomain domain : protein.domains) {

				if (!(domain.name.toLowerCase().contains(TRANSMEMBRANE)))
					continue;

				domain.isValid = true;
			}
		}
	}

	/**
	 * Method responsible for getting the closest transmem region based on a
	 * specific position
	 * 
	 * @param domains protein domains
	 * @param start   position
	 * @return transmem region
	 */
	private ProteinDomain getClosestTransmemRegion(List<ProteinDomain> domains, int start) {
		if (domains == null)
			return null;

		Optional<ProteinDomain> isPresent = domains.stream()
				.filter(value -> value.name.toLowerCase().equals(TRANSMEMBRANE) && value.startId > start).findFirst();

		if (isPresent.isPresent())
			return isPresent.get();
		return null;

	}

	/**
	 * 
	 * Method responsible for updating the annotation domain of all proteins
	 * according to the new predicted annotations
	 *
	 * @param taskMonitor
	 * @param epoch
	 */
	private void annotatePredictedLocation(TaskMonitor taskMonitor, int epoch) {

		removeUnknownProteinDomains();

		List<Protein> allProteins = Util.getProteins(myNetwork, true);

		if (allProteins == null || allProteins.size() == 0)
			return;

		int countProtein = 0;
		for (Protein protein : allProteins) {

			try {

				int start_domain = -1;
				int end_domain = -1;
				String domain = "";

				ProteinDomain transmem = null;

				for (int i = 0; i < protein.reactionSites.size(); i++) {

					Residue first_residue = protein.reactionSites.get(i);
					if (first_residue.predicted_epoch > epoch || first_residue.predicted_epoch == -1)
						continue;

					// Get the residue that has been predicted in a specific epoch
					// -1 means that the residue has not been predicted
					if (first_residue.predicted_epoch > -1 && first_residue.predicted_epoch != epoch
							&& first_residue.history_residues != null && first_residue.history_residues.size() > 0) {

						if (!first_residue.history_residues.contains(first_residue))
							first_residue.history_residues.add(first_residue);
						List<Residue> res = first_residue.history_residues.stream()
								.filter(value -> value.predicted_epoch <= epoch).collect(Collectors.toList());
						if (res.size() > 0)
							first_residue = res.get(res.size() - 1);

					}

					if (first_residue.predictedLocation.equals(UNKNOWN_RESIDUE)
							|| first_residue.predictedLocation.toLowerCase().equals(TRANSMEMBRANE))
						continue;

					domain = first_residue.predictedLocation;
					start_domain = first_residue.position;
					end_domain = first_residue.position;
					transmem = getClosestTransmemRegion(protein.domains, start_domain);

					int j;
					for (j = i + 1; j < protein.reactionSites.size(); j++) {

						Residue second_residue = protein.reactionSites.get(j);

						if (second_residue.predicted_epoch > epoch)
							continue;

						// Get the residue that was predicted in a specific epoch
						// -1 means that the residue has not been predicted
						if (second_residue.predicted_epoch > -1 && second_residue.predicted_epoch != epoch
								&& second_residue.history_residues != null
								&& second_residue.history_residues.size() > 0) {

							if (!second_residue.history_residues.contains(second_residue))
								second_residue.history_residues.add(second_residue);

							List<Residue> res = second_residue.history_residues.stream()
									.filter(value -> value.predicted_epoch <= epoch).collect(Collectors.toList());
							if (res.size() > 0)
								second_residue = res.get(res.size() - 1);

						}

						if (transmem != null && second_residue.position >= transmem.startId)
							break;

						if (second_residue.predictedLocation.equals(UNKNOWN_RESIDUE))
							continue;

						else if (second_residue.predictedLocation.toLowerCase().equals(TRANSMEMBRANE)) {
							break;
						}

						if (second_residue.predictedLocation.equals(domain)) {
							end_domain = second_residue.position;
						} else {
							break;
						}
					}
					i = j - 1;

					if (start_domain <= end_domain) {

						ProteinDomain protein_domain = getProteinDomain(protein, start_domain, end_domain);

						// Remove all domains that contains residues in the rang [start_domain,
						// end_domain]
						while (protein_domain != null) {
							if (protein.domains != null)
								protein.domains.remove(protein_domain);

							protein_domain = getProteinDomain(protein, start_domain, end_domain);
						}

						if (protein.domains == null)
							protein.domains = new ArrayList<ProteinDomain>();

						boolean isComputed = true;
						protein.domains.addAll(
								ComputeDomainsScore(protein.reactionSites, start_domain, end_domain, isComputed));

						if (isComputed && protein.predicted_domain_epoch == -1)
							protein.predicted_domain_epoch = epochs;

					}
				}

				if (protein.domains != null) {

					// remove domains created from null conflict residues
					protein.domains.removeIf(value -> ((value.name.isBlank() || value.name.isEmpty())
							&& !value.eValue.isBlank() && !value.eValue.isEmpty()) || value.eValue.equals("0E0"));

					protein.domains = protein.domains.stream().distinct().collect(Collectors.toList());

					// Sort protein domains list
					Collections.sort(protein.domains, new Comparator<ProteinDomain>() {
						@Override
						public int compare(ProteinDomain lhs, ProteinDomain rhs) {
							return lhs.startId > rhs.startId ? 1 : (lhs.startId < rhs.startId) ? -1 : 0;
						}
					});

					// Reset all XLs location. They will be updated in the updateProteins method
					resetCrossLinksAnnotation(protein);

				}

				countProtein++;
			} catch (Exception e) {
				System.out.println("ERROR: annotatePredictedLocation -> index:" + countProtein);
			}
		}

		if (!Util.consider_domain_whole_ptn) {
			// Annotate neighbor amino acids based on predicted location
			AnnotateNeighborAminoAcids();
		} else {

			UnifyResiduesDomainsFromAllProteins();
		}

//		Util.updateProteins(taskMonitor, myNetwork, null, false, true);
	}

	/**
	 * Method responsible for computing domain score based on residues scores
	 * 
	 * @param residues  list of residues
	 * @param start_pos domain start position
	 * @param end_pos   domain end position
	 * @return domain list
	 */
	private List<ProteinDomain> ComputeDomainsScore(List<Residue> residues, int start_pos, int end_pos,
			boolean notComputed) {

		if (residues == null || residues.size() == 0) {
			notComputed = false;
			return new ArrayList<ProteinDomain>();
		}

		// Algorithm cannot backward.
		Protein current_protein = residues.get(0).protein;
		if (current_protein.predicted_domain_epoch != -1 && current_protein.domains != null
				&& getProteinDomain(residues.get(0).protein, start_pos, end_pos) != null &&

				// Check whether domains
				// contain only valid
				// transmem
				current_protein.domains.stream()
						.filter(value -> value.isValid
								&& Double.parseDouble(value.eValue) > Util.transmemPredictionRegionsUpperScore
								&& value.name.toLowerCase().equals(TRANSMEMBRANE))
						.collect(Collectors.toList()).size() != current_protein.domains.size())
			return current_protein.domains;

		List<ProteinDomain> newDomains = new ArrayList<ProteinDomain>();

		// Compute the scores based on residues score (non-conflict residues)
		Map<String, List<Residue>> groupedResidues = residues.stream()
				.filter(value -> value.position >= start_pos && value.position <= end_pos
						&& value.score != Util.initialResidueScore)
				.collect(Collectors.groupingBy(w -> w.predictedLocation));

		// Update residues when there is conflict
		for (Residue residue : residues.stream().filter(value -> value.position >= start_pos
				&& value.position <= end_pos && value.isConflicted && value.conflicted_residue != null)
				.collect(Collectors.toList())) {

			if (residue.conflicted_residue.predictedLocation.isBlank()
					|| residue.conflicted_residue.predictedLocation.isEmpty())
				continue;

			// Transmem does not predict and conflict other residues
			if (residue.conflicted_residue.predictedLocation.toLowerCase().equals(TRANSMEMBRANE))
				continue;

			if (groupedResidues.containsKey(residue.conflicted_residue.predictedLocation)) {

				List<Residue> current_residues = groupedResidues.get(residue.conflicted_residue.predictedLocation);
				if (!current_residues.contains(residue)) {
					current_residues.add(residue);
				}
			} else {
				List<Residue> current_residues = new ArrayList<Residue>();
				current_residues.add(residue);
				groupedResidues.put(residue.conflicted_residue.predictedLocation, current_residues);
			}
		}

		for (Entry<String, List<Residue>> entry : groupedResidues.entrySet()) {

			if (entry.getKey().equals(UNKNOWN_RESIDUE))
				continue;

			double score = 0;

			for (Residue residue : entry.getValue()) {
				if (residue.isConflicted && residue.conflicted_score > 0) {

					// Target residue
					if (residue.predictedLocation.equals(entry.getKey()))
						score += Math.pow(residue.score, 2);
					else// Conflicted residue
						score += Math.pow(residue.conflicted_residue.conflicted_score, 2);
				} else
					score += Math.pow(residue.score, 2);
			}

			score = Math.sqrt(score / entry.getValue().size());

			// Transmem regions that contain score lower then UpperScore will not be
			// considered
			if (entry.getKey().toLowerCase().contains(TRANSMEMBRANE)
					&& score < Util.transmemPredictionRegionsUpperScore)
				continue;

			ProteinDomain new_domain = new ProteinDomain(entry.getKey(), start_pos, end_pos, true,
					Util.RoundScore(score), true, epochs);
			newDomains.add(new_domain);
		}

		return newDomains;

	}

	/**
	 * Method responsible for annotating neighbor amino acids based on predicted
	 * location
	 */
	private void AnnotateNeighborAminoAcids() {

		List<Protein> allProteins = Util.getProteins(myNetwork, true);
		if (allProteins == null || allProteins.size() == 0)
			return;

		int limit_range = Util.neighborAA * 2;

		for (Protein protein : allProteins) {

			if (protein.domains == null)
				continue;

			for (ProteinDomain domain : protein.domains) {

				if (!domain.isPredicted)
					continue;

				int range = domain.endId - domain.startId;

				if (range >= (limit_range))
					continue;

				int new_range = (limit_range - range) / 2;

				int new_start = domain.startId - new_range;
				if (new_start < 0)
					new_start = 0;
				int new_end = domain.endId + new_range;
				if (new_end > protein.sequence.length())
					new_end = protein.sequence.length();
				domain.startId = new_start;
				domain.endId = new_end;
				domain.eValue = Util.RoundScore(Util.ComputeDomainScore(protein, domain.startId, domain.endId));

			}
		}

		UnifyResiduesDomainsFromAllProteins();// E.g. Domain[216-722] and Domain[217-723] => Domain[216-723]
	}

	/**
	 * Method responsible for merging similar protein domains with different ranges
	 * (ONLY one source domain)
	 * 
	 * @param protein
	 * @param startDomainID
	 * @param delta_aa
	 * @param forceUnification
	 */
	private void UnifyResiduesDomain(Protein protein, int startDomainID, int delta_aa, boolean forceUnification) {

		ProteinDomain domain = protein.domains.get(startDomainID);
		if (domain.name.toLowerCase().contains(TRANSMEMBRANE))
			return;

		// e.g. Domain[716-722] and Domain[717-723] => Domain[716-723] -> delta == 0
		// e.g. Domain[136-142] and Domain[144-150] => Domain[136-150] -> delta == 2
		List<ProteinDomain> candidates_domains = protein.domains.stream()
				.filter(value -> value.startId >= domain.startId - delta_aa && value.startId <= domain.endId + delta_aa
						&& value.endId >= domain.endId && value.name.equals(domain.name)
						&& !value.eValue.equals("invalid"))
				.collect(Collectors.toList());

		if (candidates_domains.size() > 0) {
			for (ProteinDomain expandDomain : candidates_domains) {
				if (domain.equals(expandDomain)) {

					if (candidates_domains.size() > 1)
						domain.eValue = "invalid";
					continue;
				}

				if (forceUnification || domain.isPredicted) {
					domain.endId = expandDomain.endId;
					expandDomain.startId = domain.startId;
					expandDomain.isPredicted = true;
					expandDomain.eValue = Util
							.RoundScore(Util.ComputeDomainScore(protein, expandDomain.startId, expandDomain.endId));
				}
			}
		}
	}

	/**
	 * Method responsible for unifying domains of all proteins
	 */
	private void UnifyProteinsDomains() {

		List<Protein> allProteins = Util.getProteins(myNetwork, true);
		if (allProteins == null || allProteins.size() == 0)
			return;

		for (Protein protein : allProteins) {

			if (protein.domains == null)
				continue;

			UnifyProteinDomains(protein, 1);
		}
	}

	/**
	 * Method responsible for unifying domains of a specific protein
	 * 
	 * @param protein  current protein
	 * @param delta_aa delta
	 */
	private void UnifyProteinDomains(Protein protein, int delta_aa) {

		// Select all domains that are far from delta_aa

		List<ProteinDomain> current_ptn_domain_list = new ArrayList<ProteinDomain>();
		List<ProteinDomain> final_domain_list = new ArrayList<ProteinDomain>();

		// Group domains by name
		Map<String, List<ProteinDomain>> groupedDomains = protein.domains.stream().filter(value -> value.isValid)
				.collect(Collectors.groupingBy(w -> w.name));

		for (Entry<String, List<ProteinDomain>> proteinDomain : groupedDomains.entrySet()) {

			if (proteinDomain.getKey().toLowerCase().equals(TRANSMEMBRANE)) {

				final_domain_list.addAll(proteinDomain.getValue());
				continue;
			}

			for (int i = 0; i < proteinDomain.getValue().size(); i++) {

				current_ptn_domain_list.clear();
				ProteinDomain ptnDomain = proteinDomain.getValue().get(i);
				try {

					current_ptn_domain_list.add((ProteinDomain) ptnDomain.clone());
				} catch (Exception e) {
				}

				if (proteinDomain.getValue().size() == 1) {
					try {

						final_domain_list.add((ProteinDomain) ptnDomain.clone());
					} catch (Exception e) {
					}
					continue;
				}

				boolean isProcessed = false;
				int j;
				for (j = i + 1; j < proteinDomain.getValue().size(); j++) {
					ProteinDomain ptnDomain2 = proteinDomain.getValue().get(j);

					if (ptnDomain2 != null && ptnDomain.endId + delta_aa == ptnDomain2.startId) {

						isProcessed = false;
						try {

							current_ptn_domain_list.add((ProteinDomain) ptnDomain2.clone());
						} catch (Exception e) {
						}

						i++;
						ptnDomain = proteinDomain.getValue().get(i);

					} else {

						isProcessed = true;
						current_ptn_domain_list = current_ptn_domain_list.stream().distinct()
								.collect(Collectors.toList());
						if (current_ptn_domain_list.size() == 0)
							break;

						Collections.sort(current_ptn_domain_list, new Comparator<ProteinDomain>() {
							@Override
							public int compare(ProteinDomain lhs, ProteinDomain rhs) {
								return lhs.startId > rhs.startId ? 1 : (lhs.startId < rhs.startId) ? -1 : 0;
							}
						});

						ProteinDomain initial_domain = current_ptn_domain_list.get(0);
						ProteinDomain final_domain = current_ptn_domain_list.get(current_ptn_domain_list.size() - 1);
						if (initial_domain != final_domain && !initial_domain.name.equals("Unknown")) {

							initial_domain.endId = final_domain.endId;
							initial_domain.eValue = Util.RoundScore(
									Util.ComputeDomainScore(protein, initial_domain.startId, initial_domain.endId));

							if (initial_domain.eValue.equals("0E0"))
								initial_domain.eValue = "predicted";
							else if (initial_domain.eValue.equals("1.5E2")) {
								initial_domain.eValue = "";
								initial_domain.isPredicted = false;
							}
						}
						final_domain_list.add(initial_domain);
						break;

					}
				}

				if (!isProcessed) {
					current_ptn_domain_list = current_ptn_domain_list.stream().distinct().collect(Collectors.toList());
					if (current_ptn_domain_list.size() == 0)
						break;

					Collections.sort(current_ptn_domain_list, new Comparator<ProteinDomain>() {

						@Override
						public int compare(ProteinDomain lhs, ProteinDomain rhs) {
							return lhs.startId > rhs.startId ? 1 : (lhs.startId < rhs.startId) ? -1 : 0;
						}
					});

					ProteinDomain initial_domain = current_ptn_domain_list.get(0);
					ProteinDomain final_domain = current_ptn_domain_list.get(current_ptn_domain_list.size() - 1);
					if (initial_domain != final_domain && !initial_domain.name.equals("Unknown")) {
						initial_domain.endId = final_domain.endId;
						initial_domain.eValue = Util.RoundScore(
								Util.ComputeDomainScore(protein, initial_domain.startId, initial_domain.endId));

						if (initial_domain.eValue.equals("0E0"))
							initial_domain.eValue = "predicted";
						else if (initial_domain.eValue.equals("1.5E2")) {
							initial_domain.eValue = "";
							initial_domain.isPredicted = false;
						}
					}
					final_domain_list.add(initial_domain);

				} else if (j == proteinDomain.getValue().size()) {
					try {

						final_domain_list.add((ProteinDomain) ptnDomain.clone());
					} catch (Exception e) {
					}
				}

				i = j - 1;

			}
		}

		if (final_domain_list.size() > 0) {

			List<ProteinDomain> invalidDomains = protein.domains.stream().filter(value -> !value.isValid)
					.collect(Collectors.toList());
			final_domain_list.addAll(invalidDomains);
			final_domain_list = final_domain_list.stream().distinct().collect(Collectors.toList());
			protein.domains = final_domain_list;

			Collections.sort(protein.domains, new Comparator<ProteinDomain>() {
				@Override
				public int compare(ProteinDomain lhs, ProteinDomain rhs) {
					return lhs.startId > rhs.startId ? 1 : (lhs.startId < rhs.startId) ? -1 : 0;
				}
			});

		}
	}

	/**
	 * Method responsible for merging similar protein domains with different ranges
	 * 
	 * @param protein  current protein
	 * @param delta_aa delta number of amino acids
	 */
	private void UnifyResiduesDomains(Protein protein, int delta_aa, boolean forceUnification) {

		List<ProteinDomain> current_ptn_domain_list = new ArrayList<ProteinDomain>();
		for (ProteinDomain proteinDomain : protein.domains) {
			try {

				current_ptn_domain_list.add((ProteinDomain) proteinDomain.clone());
			} catch (Exception e) {
			}
		}

		for (ProteinDomain domain : current_ptn_domain_list) {

			if (domain.name.toLowerCase().contains(TRANSMEMBRANE))
				continue;

			// e.g. Domain[716-722] and Domain[717-723] => Domain[716-723] -> delta == 0
			// e.g. Domain[136-142] and Domain[144-150] => Domain[136-150] -> delta == 2
			List<ProteinDomain> candidates_domains = protein.domains.stream()
					.filter(value -> value.isValid && value.startId >= domain.startId - delta_aa
							&& value.startId <= domain.endId + delta_aa && value.endId >= domain.endId
							&& value.name.equals(domain.name) && value.epoch == domain.epoch)
					.collect(Collectors.toList());

			if (candidates_domains.size() > 0) {
				for (ProteinDomain expandDomain : candidates_domains) {
					if (domain.equals(expandDomain)) {

						if (candidates_domains.size() > 1)
							domain.eValue = "invalid";
						continue;
					}

					if (forceUnification || domain.isPredicted) {
						domain.endId = expandDomain.endId;
						expandDomain.startId = domain.startId;
						expandDomain.isPredicted = true;
						expandDomain.eValue = Util
								.RoundScore(Util.ComputeDomainScore(protein, expandDomain.startId, expandDomain.endId));
					}
				}
			}
		}

		protein.domains = protein.domains.stream().filter(value -> !value.eValue.equals("invalid"))
				.collect(Collectors.toList());

		// E.g. IMS [50-250], IMS [50-255], IMS [50-300]
		checkSubsetProteinDomains(protein);

		protein.domains = protein.domains.stream().distinct().collect(Collectors.toList());
	}

	/**
	 * Method responsible for merging subset protein domains with different ranges
	 * 
	 * @param protein
	 */
	private void checkSubsetProteinDomains(Protein protein) {

		for (ProteinDomain domain : protein.domains) {

			if (domain.name.toLowerCase().contains(TRANSMEMBRANE))
				continue;

			// E.g. IMS [50-250], IMS [50-255], IMS [50-300]
			List<ProteinDomain> candidates_domains = protein.domains.stream()
					.filter(value -> value.epoch == domain.epoch && value.startId == domain.startId
							&& value.name.equals(domain.name))
					.collect(Collectors.toList());

			int max_value = Collections.max(candidates_domains, Comparator.comparing(s -> s.getEndId())).endId;
			if (candidates_domains.size() > 0) {
				for (ProteinDomain expandDomain : candidates_domains) {

					if (domain.equals(expandDomain)) {
						continue;
					}
					if (domain.isPredicted) {
						expandDomain.startId = domain.startId;
						expandDomain.endId = max_value;
						expandDomain.isPredicted = true;
						expandDomain.eValue = Util
								.RoundScore(Util.ComputeDomainScore(protein, expandDomain.startId, expandDomain.endId));
					}
				}
			}

			// E.g. IMS [50-250], IMS [150-250], IMS [230-250]
			candidates_domains = protein.domains.stream().filter(value -> value.epoch == domain.epoch
					&& value.endId == domain.endId && value.name.equals(domain.name)).collect(Collectors.toList());

			int min_value = Collections.min(candidates_domains, Comparator.comparing(s -> s.getStartId())).startId;
			if (candidates_domains.size() > 0) {
				for (ProteinDomain expandDomain : candidates_domains) {

					if (domain.equals(expandDomain)) {
						continue;
					}
					if (domain.isPredicted) {
						expandDomain.startId = min_value;
						expandDomain.endId = domain.endId;
						expandDomain.isPredicted = true;
						expandDomain.eValue = Util
								.RoundScore(Util.ComputeDomainScore(protein, expandDomain.startId, expandDomain.endId));
					}
				}
			}
		}

	}

	/**
	 * Method responsible for merging similar protein domains with different ranges
	 */
	private void UnifyResiduesDomainsFromAllProteins() {

		List<Protein> allProteins = Util.getProteins(myNetwork, true);
		if (allProteins == null || allProteins.size() == 0)
			return;

		for (Protein protein : allProteins) {

			if (protein.domains == null)
				continue;

			UnifyResiduesDomains(protein, 0, false);
		}
	}

	/**
	 * Method responsible for resetting all xl location
	 * 
	 * @param protein current protein
	 */
	private void resetCrossLinksAnnotation(Protein protein) {

		for (CrossLink xl : protein.intraLinks) {
			xl.location = null;
		}
		for (CrossLink xl : protein.interLinks) {
			xl.location = null;
		}

	}

}
