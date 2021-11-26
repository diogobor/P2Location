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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
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

	// Window
	private JFrameWithoutMaxAndMinButton mainFrame;
	private JPanel mainPanel;
	private static JLabel textLabel_status_result;
	private MenuBar menuBar = new MenuBar();
	private JPanel information_panel;

	// Table
	private static JTable mainProteinDomainTable;
	public static DefaultTableModel tableDataModel;
	private static String[] columnNames = { "Node Name", "Sequence", "Topological Domain(s)", "Subcellular location" };
	@SuppressWarnings("rawtypes")
	private final Class[] columnClass = new Class[] { String.class, String.class, String.class, String.class };
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
	private JButton proteinDomainServerButton;
	private JButton proteinSequenceServerButton;
	private static Thread ptnSequenceThread;

	private static JButton okButton;
	private static Thread storeDomainThread;
	private static boolean isStoredDomains = false;

	public static boolean isPlotDone = false;

	public static Thread disposeMainJFrameThread;

	private final static String TRANSMEMBRANE = "transmem";
	private final static String UNKNOWN_DOMAIN = "###";
	private final static String INNER_MEMBRANE = "IMM";
	private final static String OUTER_MEMBRANE = "OMM";
	private final static String INTERMEMBRANE = "intermembrane";
	private final static String CYTOSOL = "cytoplasmic";
	private final static String MATRIX = "matrix";
	private final static String UNKNOWN_RESIDUE = "UK";
	private boolean predictLocation = false;
	private boolean updateAnnotationDomain = false;

	// Map<domain name, residues>
	private static HashMap<String, List<Residue>> compartments = new HashMap<String, List<Residue>>();
	private List<Residue> all_unknownResidues;
	private static List<Residue> all_knownResidues;
	public static int epochs = 1;
	public static HashMap<Integer, Integer> number_unknown_residues = new HashMap<Integer, Integer>();

	// Map<Protein - Node SUID, Protein
	private static Map<Long, Protein> proteinsMap = new HashMap<Long, Protein>();

	// OMM / IMM prediction
	private String predicted_protein_domain_name;

	/**
	 * /** Constructor
	 * 
	 * @param cyApplicationManager main app manager
	 * @param vmmServiceRef        visual mapping manager
	 * @param vgFactory            graphic factory
	 */
	@SuppressWarnings("static-access")
	public ProcessProteinLocationTask(CyApplicationManager cyApplicationManager,
			final VisualMappingManager vmmServiceRef, boolean predictLocation, boolean updateAnnotationDomain) {

		this.predictLocation = predictLocation;
		this.updateAnnotationDomain = updateAnnotationDomain;
		this.menuBar.domain_ptm_or_monolink = 0;
		this.cyApplicationManager = cyApplicationManager;
		this.myNetwork = cyApplicationManager.getCurrentNetwork();
		this.style = vmmServiceRef.getCurrentVisualStyle();
		// Get the current Visual Lexicon
		this.lexicon = cyApplicationManager.getCurrentRenderingEngine().getVisualLexicon();

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
			appSize = new Dimension(540, 375);
		} else if (Util.isMac()) {
			appSize = new Dimension(525, 355);
		} else {
			appSize = new Dimension(525, 365);
		}
		mainFrame.setSize(appSize);
		mainFrame.setResizable(false);

		if (mainPanel == null)
			mainPanel = new JPanel();
		mainPanel.setBounds(10, 10, 490, 365);
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
			taskMonitor.setTitle("P2Location - Update epoch");
			annotatePredictedLocation(taskMonitor, epochs);
		}
	}

	/**
	 * Set all labels in P2Location window / frame
	 */
	private void initFrameLabels() {

		int offset_y = 0;

		information_panel = new JPanel();
		information_panel.setBorder(BorderFactory.createTitledBorder(""));
		information_panel.setBounds(10, 8, 355, 146);
		information_panel.setLayout(null);
		mainPanel.add(information_panel);

		JLabel textLabel_Protein_lbl_1 = null;
		if (Util.isUnix())
			textLabel_Protein_lbl_1 = new JLabel("Fill in the table below to indicate what proteins will");
		else
			textLabel_Protein_lbl_1 = new JLabel("Fill in the table below to indicate what proteins will have their");
		textLabel_Protein_lbl_1.setFont(new java.awt.Font("Tahoma", Font.PLAIN, 12));
		textLabel_Protein_lbl_1.setBounds(10, offset_y, 450, 40);
		information_panel.add(textLabel_Protein_lbl_1);
		offset_y += 20;

		JLabel textLabel_Protein_lbl_2 = null;
		if (Util.isUnix()) {
			textLabel_Protein_lbl_2 = new JLabel("have their domains loaded.");
			textLabel_Protein_lbl_2.setBounds(10, offset_y, 250, 40);
		} else {
			textLabel_Protein_lbl_2 = new JLabel("domains loaded.");
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
		logo_panel.setBounds(370, 8, 140, 146);
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
		proteinDomainTableScrollPanel.setBounds(10, 160, 500, 105);
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
				taskMonitor.setTitle("Monolinks...");

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
			okButton.setBounds(30, 280, 220, 25);
		else
			okButton.setBounds(30, 270, 220, 25);

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
			cancelButton.setBounds(265, 280, 220, 25);
		else
			cancelButton.setBounds(265, 270, 220, 25);

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
						tableDataModel.setValueAt(protein.sequence, countPtnDomain, 1);
						tableDataModel.setValueAt(ToStringProteinDomains(protein.domains), countPtnDomain, 2);
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
				Protein ptn = entry.getValue();
				ptn.sequence = sequence;
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
								OrganizeResidueCompartment(taskMonitor);
								all_knownResidues = getAllKnownResidues();
								ComputeNewResidues(taskMonitor, all_knownResidues, true);
							}
							Util.updateProteins(taskMonitor, myNetwork, textLabel_status_result, false, false);
						}

						taskMonitor.setProgress(1.0);
						taskMonitor.showMessage(TaskMonitor.Level.INFO,
								"Protein domains have been loaded successfully!");

						if (isFromScreen) {
							textLabel_status_result.setText("Done!");
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
				if (protein != null)
					updateProteinMap(myNetwork, currentNode, protein);

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
				addReactionSites(ptn);
			} else {
				addReactionSites(myProtein);
				all_proteins.add(myProtein);
			}

		} else {// Network does not exists

			List<Protein> proteins = new ArrayList<Protein>();
			addReactionSites(myProtein);
			proteins.add(myProtein);
			Util.proteinsMap.put(network_name, proteins);
		}

		Util.updateProteinDomainsColorMap(myProtein.domains);

	}

	/**
	 * Method responsible for updating reaction sites into protein
	 * 
	 * @param protein
	 */
	public static void addReactionSites(Protein protein) {
		// Find out all lysines
		List<Residue> residues = new ArrayList<Residue>();

		for (int index = protein.sequence.indexOf(Util.REACTION_RESIDUE); index >= 0; index = protein.sequence
				.indexOf(Util.REACTION_RESIDUE, index + 1)) {
			residues.add(new Residue(Util.REACTION_RESIDUE, "UK", (index + 1), protein));
		}

		if (protein.reactionSites != null && protein.reactionSites.size() > 0) {
			residues.addAll(protein.reactionSites);
			residues = residues.stream().distinct().collect(Collectors.toList());
		}

		if (protein.domains != null && protein.domains.size() > 0) {
			for (ProteinDomain domain : protein.domains) {

				residues.stream().filter(value -> value.position >= domain.startId && value.position <= domain.endId)
						.forEach(

								res -> {

									res.location = domain.name;
									res.predictedLocation = domain.name;

								});

			}
			Collections.sort(residues, new Comparator<Residue>() {
				@Override
				public int compare(Residue lhs, Residue rhs) {
					return lhs.position > rhs.position ? 1 : (lhs.position < rhs.position) ? -1 : 0;
				}
			});
			residues = residues.stream().distinct().collect(Collectors.toList());
		}
		protein.reactionSites = residues;

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
			String domainsStr = tableDataModel.getValueAt(row, 2) != null ? tableDataModel.getValueAt(row, 2).toString()
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
						proteinDomains.add(new ProteinDomain(domainName, startId, endId, ""));
					}
				} catch (Exception e) {
					sbError.append("ERROR: Row: " + (row + 1)
							+ " - Protein domains don't match with the pattern 'name[start_index-end_index]'\n");
				}
			}

			String sequence = tableDataModel.getValueAt(row, 1) != null ? tableDataModel.getValueAt(row, 1).toString()
					: "";

			String location = tableDataModel.getValueAt(row, 3) != null ? tableDataModel.getValueAt(row, 3).toString()
					: "";

			if (gene.isEmpty() || gene.isBlank()) {
				sbError.append("ERROR: Row: " + (row + 1) + " - Gene is empty.");
			} else {
				CyNode current_node = Util.getNode(myNetwork, gene);
				if (current_node != null) {

					Protein ptn = proteinsMap.get(current_node.getSUID());
					if (ptn == null) {
						ptn = new Protein(gene, gene, sequence, location, proteinDomains);
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
		String[] columnNames = { "Node Name", "Sequence", "Topological Domain(s)", "Subcellular location" };
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
			mainProteinDomainTable.setPreferredScrollableViewportSize(new Dimension(490, 90));
			mainProteinDomainTable.getColumnModel().getColumn(0).setPreferredWidth(90);
			mainProteinDomainTable.getColumnModel().getColumn(1).setPreferredWidth(100);
			mainProteinDomainTable.getColumnModel().getColumn(2).setPreferredWidth(150);
			mainProteinDomainTable.getColumnModel().getColumn(3).setPreferredWidth(150);
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

			if (compartment.getKey().equals(UNKNOWN_RESIDUE))
				continue;

			all_knownResidues.addAll(compartment.getValue());
		}

		return all_knownResidues;
	}

	/**
	 * Method responsible for restoring parameters of residues that contain original
	 * domain
	 * 
	 * @param taskMonitor task monitor
	 */
	private void restoreParamsOriginalResidues(TaskMonitor taskMonitor) {

		List<Protein> allProteins = Util.proteinsMap.get(myNetwork.toString());

		int old_progress = 0;
		int summary_processed = 0;
		int total_ptns = allProteins.size();

		for (final Protein protein : allProteins) {
			List<Residue> residues = protein.reactionSites;

			if (residues == null)
				continue;

			for (Residue residue : residues) {
				if (residue.predictedLocation.equals("UK"))
					continue;

				residue.history_residues = null;
				residue.predicted_epoch = -1;
				residue.previous_residue = null;
				residue.score = 0.0;
			}
			
			summary_processed++;

			Util.progressBar(summary_processed, old_progress, total_ptns, "Restoring residues parameters: ",
					taskMonitor, null);
		}
	}

	/**
	 * Method responsible for starting the prediction location process
	 * 
	 * @param taskMonitor current task monitor
	 */
	private void processLocation(TaskMonitor taskMonitor) {

		restoreParamsOriginalResidues(taskMonitor);

		epochs = 1;
		int old_number_uk_residues = 0;
		number_unknown_residues = new HashMap<Integer, Integer>();

		processRoundLocation(taskMonitor, old_number_uk_residues);
		annotatePredictedLocation(taskMonitor, epochs);

		if (Util.consider_domain_whole_ptn) {
			do {
				UnifyProteinDomains(taskMonitor);

				// Call again the process to predict domains just to check if there is an
				// unknown domain
				processRoundLocation(taskMonitor, old_number_uk_residues);
			} while (checkConflictProteinDomains());
		}
	}

	/**
	 * Method responsible for checking the conflict protein domains
	 * 
	 * @return true if there is conflict
	 */
	private boolean checkConflictProteinDomains() {
		boolean isThereConflictProteinDomains = false;

		List<Protein> allProteins = Util.proteinsMap.get(myNetwork.toString());

		for (Protein protein : allProteins) {

			if (!protein.isConflictedDomain && protein.domains != null && protein.domains.size() > 1) {

				for (int i = 0; i < protein.domains.size() - 1; i++) {

					if (!protein.domains.get(i).name.toLowerCase().contains(TRANSMEMBRANE)) {
						if (!protein.domains.get(i).name.toLowerCase()
								.equals(protein.domains.get(i + 1).name.toLowerCase())
								&& !protein.domains.get(i + 1).name.toLowerCase().contains(TRANSMEMBRANE)) {
							isThereConflictProteinDomains = true;
							break;
						}
					}
				}
			}
		}

		return isThereConflictProteinDomains;
	}

	/**
	 * Method responsible for unifying different locations
	 * 
	 * @param taskMonitor current task monitor
	 */
	private void UnifyProteinDomains(TaskMonitor taskMonitor) {

		List<Protein> allProteins = Util.proteinsMap.get(myNetwork.toString());

		List<String> unique_domain = new ArrayList<String>();

		boolean isThereTransmem = false;
		for (Protein protein : allProteins) {

			isThereTransmem = false;
			protein.isConflictedDomain = false;
			if (protein.domains == null)
				continue;

			Collections.sort(protein.domains, new Comparator<ProteinDomain>() {
				@Override
				public int compare(ProteinDomain lhs, ProteinDomain rhs) {
					return lhs.startId > rhs.startId ? 1 : (lhs.startId < rhs.startId) ? -1 : 0;
				}
			});

			for (ProteinDomain domain : protein.domains) {

				if (!domain.name.toLowerCase().contains(TRANSMEMBRANE))
					unique_domain.add(domain.name);
				else
					isThereTransmem = true;
			}

			unique_domain = unique_domain.stream().distinct().collect(Collectors.toList());
			if (!isThereTransmem && unique_domain.size() == 1) {// There is no Transmembrane domain and there is more
																// than one domain

				for (Residue residue : protein.reactionSites) {
					residue.location = protein.domains.get(0).name;
					residue.predictedLocation = protein.domains.get(0).name;
				}

				boolean isPredicted = true;
				if (protein.domains.get(0).startId == 1 && protein.domains.get(0).endId == protein.sequence.length())
					isPredicted = false;
				ProteinDomain unifiedDomain = new ProteinDomain(protein.domains.get(0).name, 1,
						protein.sequence.length(), isPredicted, protein.domains.get(0).eValue);
				protein.domains.clear();
				protein.domains.add(unifiedDomain);

			} else if (isThereTransmem) {

				unique_domain.clear();

				// There is only Transmembrane domain
				if (protein.domains.size() == 1)
					continue;

				for (int i = 0; i < protein.domains.size() - 1; i++) {

					if (!protein.domains.get(i).name.toLowerCase().contains(TRANSMEMBRANE)) {
						if (!protein.domains.get(i).name.toLowerCase()
								.equals(protein.domains.get(i + 1).name.toLowerCase())
								&& !protein.domains.get(i + 1).name.toLowerCase().contains(TRANSMEMBRANE)) {
							protein.isConflictedDomain = true;
							break;
						} else {
							int offset = protein.domains.get(i + 1).startId - protein.domains.get(i).endId + 1;
							UnifyResiduesDomains(protein, offset, true);
						}
					}
				}

			} else if (unique_domain.size() > 0)
				protein.isConflictedDomain = true;

			unique_domain.clear();
		}

		predictDomainsBasedOnTransmemInfo();

		Util.updateXLStatus(taskMonitor, myNetwork, textLabel_status_result);
		Util.updateProteins(taskMonitor, myNetwork, null, false, true);
	}

	/**
	 * Method responsible for predicting missed domains based on the 'Transmembrane'
	 * information
	 */
	private void predictDomainsBasedOnTransmemInfo() {

		List<Protein> allProteins = Util.proteinsMap.get(myNetwork.toString());

		int count_transmem = 0;
		List<String> unique_domain = new ArrayList<String>();

		int count_protein = 0;
		try {

			for (Protein protein : allProteins.stream().filter(value -> !value.isPredictedTransmem)
					.collect(Collectors.toList())) {

				if (protein.domains == null)
					continue;

				Collections.sort(protein.domains, new Comparator<ProteinDomain>() {
					@Override
					public int compare(ProteinDomain lhs, ProteinDomain rhs) {
						return lhs.startId > rhs.startId ? 1 : (lhs.startId < rhs.startId) ? -1 : 0;
					}
				});

				unique_domain.clear();
				count_transmem = 0;
				List<ProteinDomain> new_domain_list = new ArrayList<ProteinDomain>();

				for (ProteinDomain domain : protein.domains) {

					if (domain.name.toLowerCase().contains(TRANSMEMBRANE))
						count_transmem++;
					else
						unique_domain.add(domain.name);
				}

				unique_domain = unique_domain.stream().distinct().collect(Collectors.toList());

				if (count_transmem > 0 && unique_domain.size() > 0) {

					List<ProteinDomain> transmemDomains = protein.domains.stream()
							.filter(value -> value.name.toLowerCase().contains(TRANSMEMBRANE))
							.collect(Collectors.toList());
					Collections.sort(transmemDomains, new Comparator<ProteinDomain>() {
						@Override
						public int compare(ProteinDomain lhs, ProteinDomain rhs) {
							return lhs.startId > rhs.startId ? 1 : (lhs.startId < rhs.startId) ? -1 : 0;
						}
					});

					int indexOfLastDomain = protein.domains.indexOf(Collections.max(protein.domains));
					int startDomain = 1;
					predicted_protein_domain_name = "";

					for (int i = 0; i < protein.domains.size(); i++) {

						ProteinDomain current_domain = protein.domains.get(i);

						ProteinDomain last_new_domain = null;
						if (new_domain_list.size() > 0)
							last_new_domain = new_domain_list.get(new_domain_list.size() - 1);

						if (current_domain.name.toLowerCase().contains(TRANSMEMBRANE)) {

							if (new_domain_list.size() == 0
									|| last_new_domain.name.toLowerCase().contains(TRANSMEMBRANE)) {
								if (current_domain.startId != 1) {
									ProteinDomain new_domain = new ProteinDomain(UNKNOWN_DOMAIN, startDomain,
											current_domain.startId - 1, true, "predicted");
									new_domain_list.add(new_domain);
									predicted_protein_domain_name = "";
								}
							}
							new_domain_list.add(current_domain);
							startDomain = current_domain.endId + 1;
							transmemDomains.remove(current_domain);
							continue;

						}

						ProteinDomain next_transmem = transmemDomains.size() > 0
								? transmemDomains.get(transmemDomains.indexOf(Collections.min(transmemDomains)))
								: null;

						if (next_transmem == null) {
							// update range
							current_domain.startId = last_new_domain.endId + 1;
							new_domain_list.add(current_domain);
							startDomain = current_domain.endId + 1;
							continue;
						}

						// e.g [IMS | Transmem | ??? ]
						if (current_domain.startId < next_transmem.startId) {
							predicted_protein_domain_name = current_domain.name;
							i = protein.domains.indexOf(next_transmem) - 1;

							current_domain.startId = startDomain;
							current_domain.endId = next_transmem.startId - 1;

							startDomain = next_transmem.endId + 1;
							transmemDomains.remove(next_transmem);

						} else {

							// e.g [??? | Transmem | IMS ]
							predicted_protein_domain_name = current_domain.name;

							if (current_domain.equals(protein.domains.get(indexOfLastDomain))) {

								// It is the last domain
								current_domain.endId = protein.sequence.length();
								current_domain.startId = next_transmem.endId + 1;
							}

							startDomain = 1;
							addPredictedDomainBasedOnOMMorIMMDomain(new_domain_list, protein, next_transmem,
									startDomain);
							startDomain = next_transmem.endId + 1;
						}
						current_domain.isPredicted = true;
						new_domain_list.add(current_domain);
						protein.isPredictedTransmem = true;
					}

					Collections.sort(new_domain_list, new Comparator<ProteinDomain>() {
						@Override
						public int compare(ProteinDomain lhs, ProteinDomain rhs) {
							return lhs.startId > rhs.startId ? 1 : (lhs.startId < rhs.startId) ? -1 : 0;
						}
					});

					ProteinDomain last_domain = new_domain_list
							.get(new_domain_list.indexOf(Collections.max(new_domain_list)));
					if (last_domain.endId < protein.sequence.length()) {

						// It is necessary to add the last domain
						if (last_domain.name.toLowerCase().contains(TRANSMEMBRANE)) {

							if (predicted_protein_domain_name.isEmpty() || predicted_protein_domain_name.isBlank()) {
								ProteinDomain new_domain = new ProteinDomain(UNKNOWN_DOMAIN, startDomain,
										protein.sequence.length(), true, "predicted");
								new_domain_list.add(new_domain);
							} else {
								addPredictedDomainBasedOnOMMorIMMDomain(new_domain_list, protein,
										new ProteinDomain(TRANSMEMBRANE.toUpperCase(), protein.sequence.length() + 1,
												-1, true, "predicted"),
										startDomain);
							}
						} else// Update range of the last domain
							last_domain.endId = protein.sequence.length();

					}

					new_domain_list = new_domain_list.stream().distinct().collect(Collectors.toList());

					// Check if exists unknown domains (name == '###')
					if (new_domain_list.stream().filter(value -> value.name.equals(UNKNOWN_DOMAIN))
							.collect(Collectors.toList()).size() > 0) {

						for (int i = 0; i < new_domain_list.size(); i++) {
							ProteinDomain domain = new_domain_list.get(i);

							boolean isForward = false;
							if (domain.name.equals(UNKNOWN_DOMAIN)) {

								predicted_protein_domain_name = "";
								int j = i + 1;
								for (; j < new_domain_list.size(); j++) {
									ProteinDomain next_domain = new_domain_list.get(j);

									if (!next_domain.name.toLowerCase().contains(TRANSMEMBRANE)
											&& !next_domain.name.equals(UNKNOWN_DOMAIN)) {
										predicted_protein_domain_name = next_domain.name;
										isForward = true;
										break;
									}
								}

								if (!isForward && i > 0) {

									j = i - 1;
									for (; j >= 0; j--) {
										ProteinDomain previous_domain = new_domain_list.get(j);

										if (!previous_domain.name.toLowerCase().contains(TRANSMEMBRANE)
												&& !previous_domain.name.equals(UNKNOWN_DOMAIN)) {
											predicted_protein_domain_name = previous_domain.name;
											break;
										}
									}
								}

								// Set 'predicted_protein_domain_name' based on 'Transmem' info
								predictDomainBasedOnOMMorIMM(count_protein, j, new_domain_list, protein, isForward);
								domain.name = predicted_protein_domain_name;

							}
						}
					}

					protein.domains = new_domain_list;

					// Update protein domain status
					if (protein.domains.size() > 1) {

						protein.isConflictedDomain = false;

						for (int i = 0; i < protein.domains.size() - 1; i++) {

							if (!protein.domains.get(i).name.toLowerCase().contains(TRANSMEMBRANE)) {
								if (!protein.domains.get(i).name.toLowerCase()
										.equals(protein.domains.get(i + 1).name.toLowerCase())
										&& !protein.domains.get(i + 1).name.toLowerCase().contains(TRANSMEMBRANE)) {
									protein.isConflictedDomain = true;
									break;
								}
							}
						}
					}

					updateResiduesLocation(protein);
				}

				count_protein++;
			}
		} catch (Exception e) {
			System.out.println(count_protein);
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
	private void predictDomainBasedOnOMMorIMM(int i, int j, List<ProteinDomain> new_domain_list, Protein protein,
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

				if (protein.location.equals(OUTER_MEMBRANE)) {
					if (predicted_protein_domain_name.toLowerCase().contains(INTERMEMBRANE)) {

						predicted_protein_domain_name = "TOPO_DOM - Cytoplasmic";

					} else if (predicted_protein_domain_name.toLowerCase().contains(CYTOSOL)) {

						predicted_protein_domain_name = "TOPO_DOM - Mitochondrial intermembrane";

					}
				} else if (protein.location.equals(INNER_MEMBRANE)) {
					if (predicted_protein_domain_name.toLowerCase().contains(MATRIX)) {

						predicted_protein_domain_name = "TOPO_DOM - Mitochondrial intermembrane";

					} else if (predicted_protein_domain_name.toLowerCase().contains(INTERMEMBRANE)) {

						predicted_protein_domain_name = "TOPO_DOM - Mitochondrial matrix";

					}
				} else {

					if (predicted_protein_domain_name.toLowerCase().contains(CYTOSOL))
						predicted_protein_domain_name = "TOPO_DOM - Mitochondrial intermembrane";
					else
						predicted_protein_domain_name = "";
				}
			}
		}
	}

	/**
	 * Method responsible for updating the residue location of a specific protein
	 * 
	 * @param protein current proteinF
	 */
	private void updateResiduesLocation(Protein protein) {

		for (ProteinDomain domain : protein.domains) {

			List<Residue> current_residues = protein.reactionSites.stream()
					.filter(value -> value.position >= domain.startId && value.position <= domain.endId)
					.collect(Collectors.toList());

			for (Residue residue : current_residues) {
				if (!residue.isConflicted) {
					residue.location = domain.name;
					residue.predictedLocation = domain.name;
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
	private void addPredictedDomainBasedOnOMMorIMMDomain(List<ProteinDomain> new_domain_list, Protein current_protein,
			ProteinDomain next_transmem, int startDomain) {

		ProteinDomain new_predicted_domain = null;
		if (current_protein.location.equals(OUTER_MEMBRANE)) {
			if (predicted_protein_domain_name.toLowerCase().contains(INTERMEMBRANE)) {

				new_predicted_domain = new ProteinDomain("TOPO_DOM - Cytoplasmic", startDomain,
						next_transmem.startId - 1, true, "predicted");
				predicted_protein_domain_name = CYTOSOL;

			} else if (predicted_protein_domain_name.toLowerCase().contains(CYTOSOL)) {

				new_predicted_domain = new ProteinDomain("TOPO_DOM - Mitochondrial intermembrane", startDomain,
						next_transmem.startId - 1, true, "predicted");
				predicted_protein_domain_name = INTERMEMBRANE;

			}
		} else if (current_protein.location.equals(INNER_MEMBRANE)) {
			if (predicted_protein_domain_name.toLowerCase().contains(MATRIX)) {

				new_predicted_domain = new ProteinDomain("TOPO_DOM - Mitochondrial intermembrane", startDomain,
						next_transmem.startId - 1, true, "predicted");
				predicted_protein_domain_name = INTERMEMBRANE;

			} else if (predicted_protein_domain_name.toLowerCase().contains(INTERMEMBRANE)) {

				new_predicted_domain = new ProteinDomain("TOPO_DOM - Mitochondrial matrix", startDomain,
						next_transmem.startId - 1, true, "predicted");
				predicted_protein_domain_name = MATRIX;

			}
		} else {
			if (predicted_protein_domain_name.toLowerCase().contains(CYTOSOL)) {

				new_predicted_domain = new ProteinDomain("TOPO_DOM - Mitochondrial intermembrane", startDomain,
						next_transmem.startId - 1, true, "predicted");
				predicted_protein_domain_name = INTERMEMBRANE;
			} else
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
		do {

			OrganizeResidueCompartment(taskMonitor);

			if (epochs == 1) {
				all_unknownResidues = compartments.get(UNKNOWN_RESIDUE);
				if (all_unknownResidues == null)
					break;
			}

			if (compartments.get(UNKNOWN_RESIDUE) != null)
				number_unknown_residues.put(epochs, compartments.get(UNKNOWN_RESIDUE).size());
			else
				number_unknown_residues.put(epochs, 0);

			int uk_res = number_unknown_residues.get(epochs);
			// It means there is no possibility to predict more residues location
			if (uk_res == old_number_uk_residues)
				break;

			ComputeNewResidues(taskMonitor, all_unknownResidues, false);
			if (compartments.get(UNKNOWN_RESIDUE) != null)
				old_number_uk_residues = compartments.get(UNKNOWN_RESIDUE).size();
			else
				old_number_uk_residues = 0;

			if (Util.getEpochs && (Util.epochs + 1) <= epochs)
				break;

			epochs++;
		} while (compartments.containsKey(UNKNOWN_RESIDUE));
	}

	/**
	 * Method responsible for getting protein domain
	 * 
	 * @param protein      protein
	 * @param start_domain start domain
	 * @param end_domain   end domain
	 * @return domain
	 */
	private ProteinDomain getProteinDomain(Protein protein, final int start_domain, final int end_domain) {

		if (protein.domains != null && protein.domains.size() > 0) {
			Optional<ProteinDomain> isProteinDomainPresent = protein.domains.stream()
					.filter(value -> value.startId == start_domain && value.endId <= end_domain).findFirst();

			if (isProteinDomainPresent.isPresent())
				return isProteinDomainPresent.get();
			else
				return null;
		} else
			return null;
	}

	/**
	 * Method responsible for organizing residues according to residue location
	 */
	private static void OrganizeResidueCompartment(TaskMonitor taskMonitor) {

		List<Protein> allProteins = Util.proteinsMap.get(myNetwork.toString());
		compartments = new HashMap<String, List<Residue>>();

		int old_progress = 0;
		int summary_processed = 0;
		int total_ptns = allProteins.size();

		for (final Protein protein : allProteins) {
			List<Residue> residues = protein.reactionSites;

			if (residues == null)
				continue;

			for (Residue residue : residues) {
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
					"Epoch: " + epochs + "\nOrganizing residue compartments: ", taskMonitor, null);

		}
	}

	/**
	 * Method responsible for computing new residue location
	 * 
	 * @param epochs
	 * @param taskMonitor
	 */
	private static void ComputeNewResidues(TaskMonitor taskMonitor, List<Residue> residue_collection,
			boolean isKnownResidues) {

		int old_progress = 0;
		int summary_processed = 0;
		int qtd_residues = compartments.values().stream().mapToInt(d -> d.size()).sum();
		int total_compartments = compartments.size() * qtd_residues;

		for (Map.Entry<String, List<Residue>> compartment : compartments.entrySet()) {

			if (compartment.getKey().equals(UNKNOWN_RESIDUE))
				continue;

			for (Residue residue : compartment.getValue()) {

				List<Residue> current_uk_residues = new ArrayList<Residue>();

				Protein protein = residue.protein;

				int pos = residue.position;

				List<CrossLink> links;

				// Check if the current residue contains CSM

				if (protein.intraLinks != null && protein.intraLinks.size() > 0) {

					links = (List<CrossLink>) protein.intraLinks.stream()
							.filter(value -> value.pos_site_a == pos || value.pos_site_b == pos)
							.collect(Collectors.toList());

					for (CrossLink crossLink : links) {

						// It means the current residue is A
						if (crossLink.pos_site_a == pos) {
							Optional<Residue> isResiduePresent = residue_collection.stream()
									.filter(value -> value.protein.proteinID.equals(crossLink.protein_a)
											&& value.position == crossLink.pos_site_b && !value.isConflicted
											&& value.predicted_epoch == -1)
									.findFirst();
							if (isResiduePresent.isPresent()) {
								Residue res = isResiduePresent.get();
								double score = crossLink.score / epochs;// -Math.log10(crossLink.score) * 1 / epochs;

								boolean saveConflict = true;
								if (score > res.score) {

									if (Util.considerConflict && ((!isKnownResidues
											&& !res.predictedLocation.equals(residue.predictedLocation))
											|| (isKnownResidues && !res.location.equals(residue.location)))) {

										if (!(isKnownResidues && res.score == 0))
											saveConflict = false;

										if (res.predictedLocation.toLowerCase().contains(TRANSMEMBRANE)) {
											Protein current_ptn_rs = res.protein;
											List<Residue> neighbors = current_ptn_rs.reactionSites.stream().filter(
													value -> value.position >= (res.position - Util.transmemNeighborAA)
															&& value.position <= (res.position
																	+ Util.transmemNeighborAA))
													.collect(Collectors.toList());

											if (neighbors.size() > 0) {
												for (Residue neighbor : neighbors) {
													if (!neighbor.predictedLocation.toLowerCase()
															.contains(TRANSMEMBRANE)) {
														res.score = score;
														current_uk_residues.add(res);
														crossLink.location = res.location;
														saveConflict = false;
														break;
													}
												}
											}
										} else if (!isKnownResidues
												&& !res.predictedLocation.equals(residue.predictedLocation)) {
											// It will save the new prediction if the current process is not 'known
											// residues'
											res.score = score;
											current_uk_residues.add(res);
											crossLink.location = res.location;
										}
									} else {
										saveConflict = false;

										if (residue.predictedLocation.toLowerCase().contains(TRANSMEMBRANE)) {
											Protein current_ptn_rs = residue.protein;
											List<ProteinDomain> neighbors = current_ptn_rs.domains.stream().filter(
													value -> value.startId <= (res.position + Util.transmemNeighborAA)
															&& value.startId >= (res.position
																	- Util.transmemNeighborAA))
													.collect(Collectors.toList());

											if (neighbors.size() > 0) {
												for (ProteinDomain neighbor : neighbors) {
													if (!neighbor.name.toLowerCase().contains(TRANSMEMBRANE)) {
														res.score = score;
														crossLink.location = res.location;
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
												&& !res.predictedLocation.equals(residue.predictedLocation)) {
											// It will save the new prediction if the current process is not 'known
											// residues'
											res.score = score;
											current_uk_residues.add(res);
											crossLink.location = res.location;
										}
									}

									if (saveConflict) {

										boolean isValid = false;

										if (Util.getThreshold_score) {
											if (Util.threshold_score >= -Math.log10(crossLink.score)) {
												isValid = true;
												crossLink.location = "";
											}
										} else {
											isValid = false;
										}

										if (!isValid) {
											res.isConflicted = true;
											res.conflicted_residue = residue;
											res.predictedLocation = "UK";
											res.conflicted_score = score;
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
								Residue res = isResiduePresent.get();
								double score = crossLink.score / epochs;// -Math.log10(crossLink.score) * 1 / epochs;

								boolean saveConflict = true;
								if (score > res.score) {

									if (Util.considerConflict && ((!isKnownResidues
											&& !res.predictedLocation.equals(residue.predictedLocation))
											|| (isKnownResidues && !res.location.equals(residue.location)))) {

										if (!(isKnownResidues && res.score == 0))
											saveConflict = false;

										if (res.predictedLocation.toLowerCase().contains(TRANSMEMBRANE)) {
											Protein current_ptn_rs = res.protein;
											List<Residue> neighbors = current_ptn_rs.reactionSites.stream().filter(
													value -> value.position >= (res.position - Util.transmemNeighborAA)
															&& value.position <= (res.position
																	+ Util.transmemNeighborAA))
													.collect(Collectors.toList());

											if (neighbors.size() > 0) {
												for (Residue neighbor : neighbors) {
													if (!neighbor.predictedLocation.toLowerCase()
															.contains(TRANSMEMBRANE)) {
														res.score = score;
														current_uk_residues.add(res);
														crossLink.location = res.location;
														saveConflict = false;
														break;
													}
												}
											}
										} else if (!isKnownResidues
												&& !res.predictedLocation.equals(residue.predictedLocation)) {
											// It will save the new prediction if the current process is not 'known
											// residues'
											res.score = score;
											current_uk_residues.add(res);
											crossLink.location = res.location;
										}
									} else {
										saveConflict = false;

										if (residue.predictedLocation.toLowerCase().contains(TRANSMEMBRANE)) {
											Protein current_ptn_rs = residue.protein;
											List<ProteinDomain> neighbors = current_ptn_rs.domains.stream().filter(
													value -> value.startId >= (res.position - Util.transmemNeighborAA)
															|| value.endId <= (res.position + Util.transmemNeighborAA))
													.collect(Collectors.toList());

											if (neighbors.size() > 0) {
												for (ProteinDomain neighbor : neighbors) {
													if (!neighbor.name.toLowerCase().contains(TRANSMEMBRANE)) {
														res.score = score;
														crossLink.location = res.location;
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
												&& !res.predictedLocation.equals(residue.predictedLocation)) {
											// It will save the new prediction if the current process is not 'known
											// residues'
											res.score = score;
											current_uk_residues.add(res);
											crossLink.location = res.location;
										}
									}

									if (saveConflict) {

										boolean isValid = false;

										if (Util.getThreshold_score) {
											if (Util.threshold_score >= -Math.log10(crossLink.score)) {
												isValid = true;
												crossLink.location = "";
											}
										} else {
											isValid = false;
										}

										if (!isValid) {
											res.isConflicted = true;
											res.conflicted_residue = residue;
											res.predictedLocation = "UK";
											res.conflicted_score = score;
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
								Residue res = isResiduePresent.get();
								double score = crossLink.score / epochs;// -Math.log10(crossLink.score) * 1 / epochs;

								boolean saveConflict = true;
								if (score > res.score) {

									if (Util.considerConflict && ((!isKnownResidues
											&& !res.predictedLocation.equals(residue.predictedLocation))
											|| (isKnownResidues && !res.location.equals(residue.location)))) {

										if (!(isKnownResidues && res.score == 0))
											saveConflict = false;

										if (res.predictedLocation.toLowerCase().contains(TRANSMEMBRANE)) {
											Protein current_ptn_rs = res.protein;
											List<Residue> neighbors = current_ptn_rs.reactionSites.stream().filter(
													value -> value.position >= (res.position - Util.transmemNeighborAA)
															&& value.position <= (res.position
																	+ Util.transmemNeighborAA))
													.collect(Collectors.toList());

											if (neighbors.size() > 0) {
												for (Residue neighbor : neighbors) {
													if (!neighbor.predictedLocation.toLowerCase()
															.contains(TRANSMEMBRANE)) {
														res.score = score;
														current_uk_residues.add(res);
														crossLink.location = res.location;
														saveConflict = false;
														break;
													}
												}
											}
										} else if (!isKnownResidues
												&& !res.predictedLocation.equals(residue.predictedLocation)) {
											// It will save the new prediction if the current process is not 'known
											// residues'
											res.score = score;
											current_uk_residues.add(res);
											crossLink.location = res.location;
										}
									} else {
										saveConflict = false;

										if (residue.predictedLocation.toLowerCase().contains(TRANSMEMBRANE)) {
											Protein current_ptn_rs = residue.protein;
											List<ProteinDomain> neighbors = current_ptn_rs.domains.stream().filter(
													value -> value.startId <= (res.position + Util.transmemNeighborAA)
															&& value.startId >= (res.position
																	- Util.transmemNeighborAA))
													.collect(Collectors.toList());

											if (neighbors.size() > 0) {
												for (ProteinDomain neighbor : neighbors) {
													if (!neighbor.name.toLowerCase().contains(TRANSMEMBRANE)) {
														res.score = score;
														crossLink.location = res.location;
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
												&& !res.predictedLocation.equals(residue.predictedLocation)) {
											// It will save the new prediction if the current process is not 'known
											// residues'
											res.score = score;
											current_uk_residues.add(res);
											crossLink.location = res.location;
										}
									}

									if (saveConflict) {

										boolean isValid = false;

										if (Util.getThreshold_score) {
											if (Util.threshold_score >= -Math.log10(crossLink.score)) {
												isValid = true;
												crossLink.location = "";
											}
										} else {
											isValid = false;
										}

										if (!isValid) {
											res.isConflicted = true;
											res.conflicted_residue = residue;
											res.predictedLocation = "UK";
											res.conflicted_score = score;
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
								Residue res = isResiduePresent.get();
								double score = crossLink.score / epochs;// -Math.log10(crossLink.score) * 1 / epochs;

								boolean saveConflict = true;
								if (score > res.score) {

									if (Util.considerConflict && ((!isKnownResidues
											&& !res.predictedLocation.equals(residue.predictedLocation))
											|| (isKnownResidues && !res.location.equals(residue.location)))) {

										if (!(isKnownResidues && res.score == 0))
											saveConflict = false;

										if (res.predictedLocation.toLowerCase().contains(TRANSMEMBRANE)) {
											Protein current_ptn_rs = res.protein;
											List<Residue> neighbors = current_ptn_rs.reactionSites.stream().filter(
													value -> value.position >= (res.position - Util.transmemNeighborAA)
															&& value.position <= (res.position
																	+ Util.transmemNeighborAA))
													.collect(Collectors.toList());

											if (neighbors.size() > 0) {
												for (Residue neighbor : neighbors) {
													if (!neighbor.predictedLocation.toLowerCase()
															.contains(TRANSMEMBRANE)) {
														res.score = score;
														current_uk_residues.add(res);
														crossLink.location = res.location;
														saveConflict = false;
														break;
													}
												}
											}
										} else if (!isKnownResidues
												&& !res.predictedLocation.equals(residue.predictedLocation)) {
											// It will save the new prediction if the current process is not 'known
											// residues'
											res.score = score;
											current_uk_residues.add(res);
											crossLink.location = res.location;
										}
									} else {
										saveConflict = false;

										if (residue.predictedLocation.toLowerCase().contains(TRANSMEMBRANE)) {
											Protein current_ptn_rs = residue.protein;
											List<ProteinDomain> neighbors = current_ptn_rs.domains.stream().filter(
													value -> value.startId <= (res.position + Util.transmemNeighborAA)
															&& value.startId >= (res.position
																	- Util.transmemNeighborAA))
													.collect(Collectors.toList());

											if (neighbors.size() > 0) {
												for (ProteinDomain neighbor : neighbors) {
													if (!neighbor.name.toLowerCase().contains(TRANSMEMBRANE)) {
														res.score = score;
														crossLink.location = res.location;
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
												&& !res.predictedLocation.equals(residue.predictedLocation)) {
											// It will save the new prediction if the current process is not 'known
											// residues'
											res.score = score;
											current_uk_residues.add(res);
											crossLink.location = res.location;
										}
									}

									if (saveConflict) {

										boolean isValid = false;

										if (Util.getThreshold_score) {
											if (Util.threshold_score >= -Math.log10(crossLink.score)) {
												isValid = true;
												crossLink.location = "";
											}
										} else {
											isValid = false;
										}

										if (!isValid) {
											res.isConflicted = true;
											res.conflicted_residue = residue;
											res.predictedLocation = "UK";
											res.conflicted_score = score;
										}
									}
								}
							}
						}
					}
				}

				for (Residue uk_residue : current_uk_residues) {

					boolean isValid = false;

					if (Util.getThreshold_score) {
						if (Util.threshold_score >= uk_residue.score)
							isValid = true;
					} else {
						isValid = true;
					}

					if (isValid) {

						if (uk_residue.predicted_epoch != epochs) {
							Residue current_res = new Residue(residue.aminoacid, residue.location, residue.position,
									residue.protein);
							current_res.history_residues = residue.history_residues;
							current_res.predicted_epoch = residue.predicted_epoch;
							current_res.predictedLocation = residue.predictedLocation;
							current_res.previous_residue = residue.previous_residue;
							current_res.score = residue.score;
							current_res.isConflicted = residue.isConflicted;
							current_res.conflicted_residue = residue.conflicted_residue;
							uk_residue.addHistoryResidue(current_res);
						}

						uk_residue.predictedLocation = residue.predictedLocation;
						uk_residue.predicted_epoch = epochs;
						uk_residue.previous_residue = residue;
					}
				}

				summary_processed++;
				Util.progressBar(summary_processed, old_progress, total_compartments,
						"Epoch: " + epochs + "\nPredicting residue location: ", taskMonitor, null);

			}
		}
		taskMonitor.showMessage(TaskMonitor.Level.INFO, "Epoch: " + epochs + "\nPredicting residue location: 100%");
	}

	/**
	 * Method responsible for removing unknown protein domains
	 */
	private void removeUnknownProteinDomains() {
		List<Protein> allProteins = Util.proteinsMap.get(myNetwork.toString());

		for (Protein protein : allProteins) {
			if (protein.domains != null && protein.domains.size() > 0)
				protein.domains.removeIf(value -> value.isPredicted);
		}
	}

	/**
	 * Method responsible for updating the annotation domain of all proteins
	 * according to the new predicted annotations
	 */
	private void annotatePredictedLocation(TaskMonitor taskMonitor, int epoch) {

		removeUnknownProteinDomains();

		List<Protein> allProteins = Util.proteinsMap.get(myNetwork.toString());

		for (Protein protein : allProteins) {

			int start_domain = -1;
			int end_domain = -1;
			String domain = "";

			for (int i = 0; i < protein.reactionSites.size(); i++) {

				Residue first_residue = protein.reactionSites.get(i);
				if (first_residue.predicted_epoch > epoch || first_residue.predicted_epoch == -1)
					continue;

				// Get the residue that was predicted in a specific epoch
				// -1 means that the residue was not predicted
				if (first_residue.predicted_epoch > -1 && first_residue.predicted_epoch != epoch) {

					if (!first_residue.history_residues.contains(first_residue))
						first_residue.history_residues.add(first_residue);
					List<Residue> res = first_residue.history_residues.stream()
							.filter(value -> value.predicted_epoch <= epoch).collect(Collectors.toList());
					if (res.size() > 0)
						first_residue = res.get(res.size() - 1);

				}

				if (first_residue.predictedLocation.equals(UNKNOWN_RESIDUE))
					continue;

				domain = first_residue.predictedLocation;
				start_domain = first_residue.position;
				end_domain = first_residue.position;

				int j;
				for (j = i + 1; j < protein.reactionSites.size(); j++) {

					Residue second_residue = protein.reactionSites.get(j);

					if (second_residue.predicted_epoch > epoch)
						continue;

					// Get the residue that was predicted in a specific epoch
					// -1 means that the residue was not predicted
					if (second_residue.predicted_epoch > -1 && second_residue.predicted_epoch != epoch) {

						if (!second_residue.history_residues.contains(second_residue))
							second_residue.history_residues.add(second_residue);

						List<Residue> res = second_residue.history_residues.stream()
								.filter(value -> value.predicted_epoch <= epoch).collect(Collectors.toList());
						if (res.size() > 0)
							second_residue = res.get(res.size() - 1);

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

					if (protein_domain != null) {
						if (protein.domains != null)
							protein.domains.remove(protein_domain);
					}

					protein_domain = new ProteinDomain(domain, start_domain, end_domain, "predicted");
					protein_domain.isPredicted = true;
					if (protein.domains != null)
						protein.domains.add(protein_domain);
					else {
						List<ProteinDomain> new_domains = new ArrayList<ProteinDomain>();
						new_domains.add(protein_domain);
						protein.domains = new_domains;
					}
				}
			}

			// Reset all XLs location. They will be updated in the updateProteins method
			resetCrossLinksAnnotation(protein);
		}

		// Annotate neighbor amino acids based on predicted location
		AnnotateNeighborAminoAcids();

		Util.updateProteins(taskMonitor, myNetwork, null, false, true);
	}

	/**
	 * Method responsible for annotating neighbor amino acids based on predicted
	 * location
	 */
	private void AnnotateNeighborAminoAcids() {

		List<Protein> allProteins = Util.proteinsMap.get(myNetwork.toString());

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

			}
		}

		UnifyResiduesDomainsFromAllProteins();// E.g. Domain[216-722] and Domain[217-723] => Domain[216-723]
	}

	/**
	 * Method responsible for merging similar protein domains with different ranges
	 * 
	 * @param protein  current protein
	 * @param delta_aa delta number of amino acids
	 */
	private void UnifyResiduesDomains(Protein protein, int delta_aa, boolean forceUnification) {

		for (ProteinDomain domain : protein.domains) {

			if (domain.name.toLowerCase().contains(TRANSMEMBRANE))
				continue;

			// e.g. Domain[716-722] and Domain[717-723] => Domain[716-723] -> delta == 0
			// e.g. Domain[136-142] and Domain[144-150] => Domain[136-150] -> delta == 2
			List<ProteinDomain> candidates_domains = protein.domains.stream()
					.filter(value -> value.startId >= domain.startId - delta_aa
							&& value.startId <= domain.endId + delta_aa && value.endId >= domain.endId
							&& value.name.equals(domain.name))
					.collect(Collectors.toList());

			if (candidates_domains.size() > 0) {
				for (ProteinDomain expandDomain : candidates_domains) {
					if (domain.equals(expandDomain))
						continue;

					if (forceUnification || domain.isPredicted) {
						domain.endId = expandDomain.endId;
						expandDomain.startId = domain.startId;
						expandDomain.isPredicted = true;
						expandDomain.eValue = "predicted";
					}
				}
			}
		}

		protein.domains = protein.domains.stream().distinct().collect(Collectors.toList());
	}

	/**
	 * Method responsible for merging similar protein domains with different ranges
	 */
	private void UnifyResiduesDomainsFromAllProteins() {

		List<Protein> allProteins = Util.proteinsMap.get(myNetwork.toString());

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
