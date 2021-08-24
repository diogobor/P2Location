package de.fmp.liulab.task;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.table.DefaultTableModel;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.view.model.VisualLexicon;
import org.cytoscape.view.presentation.customgraphics.CyCustomGraphics2Factory;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;

import de.fmp.liulab.internal.UpdateViewListener;
import de.fmp.liulab.internal.view.JFrameWithoutMaxAndMinButton;
import de.fmp.liulab.internal.view.MenuBar;
import de.fmp.liulab.model.PTM;
import de.fmp.liulab.utils.Util;

/**
 * Class responsible for loading PTM(s) task
 * @author borges.diogo
 *
 */
public class LoadPTMsTask extends AbstractTask implements ActionListener {

	private CyApplicationManager cyApplicationManager;
	private CyNetwork myNetwork;
	private CyCustomGraphics2Factory vgFactory;

	public static VisualLexicon lexicon;
	public static VisualStyle style;

	// Window
	private JFrameWithoutMaxAndMinButton mainFrame;
	private JPanel mainPanel;
	private static JLabel textLabel_status_result;
	private MenuBar menuBar = new MenuBar();
	private JPanel information_panel;

	// Table
	private static JTable mainProteinPTMTable;
	public static DefaultTableModel ptmTableDataModel;
	private String[] columnNamesPTMTable = { "Node Name", "PTM(s)" };
	private final Class[] columnClassPTMTable = new Class[] { String.class, String.class };
	private String rowstring, value;
	private Clipboard clipboard;
	private StringSelection stsel;
	private static JList rowHeaderPTMTable;
	private static JScrollPane proteinPTMTableScrollPanel;

	private static boolean isPTMLoaded = true;
	private static boolean ptmDoStop = false;
	private static Thread ptmThread;
	private JButton proteinPTMServerButton;

	// Map<Protein - Node SUID, List<PTM>
	public static Map<Long, List<PTM>> ptmsMap = new HashMap<Long, List<PTM>>();

	private static JButton okButton;
	private static Thread storePTMsThread;

	private static boolean isStoredPTMs = false;
	public static boolean isPlotDone = false;
	public static Thread disposeMainJFrameThread;

	public LoadPTMsTask(CyApplicationManager cyApplicationManager, final VisualMappingManager vmmServiceRef,
			CyCustomGraphics2Factory vgFactory) {

		this.menuBar.domain_ptm_or_monolink = 1;
		this.cyApplicationManager = cyApplicationManager;
		this.myNetwork = cyApplicationManager.getCurrentNetwork();
		this.vgFactory = vgFactory;

		this.style = vmmServiceRef.getCurrentVisualStyle();
		// Get the current Visual Lexicon
		this.lexicon = cyApplicationManager.getCurrentRenderingEngine().getVisualLexicon();

		if (mainFrame == null)
			mainFrame = new JFrameWithoutMaxAndMinButton(new JFrame(),
					"P2Location - Load post-translational modifications", 2);

		mainFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		Dimension appSize = null;
		if (Util.isWindows()) {
			appSize = new Dimension(540, 345);
		} else if (Util.isMac()) {
			appSize = new Dimension(525, 325);
		} else {
			appSize = new Dimension(525, 335);
		}
		mainFrame.setSize(appSize);
		mainFrame.setResizable(false);

		if (mainPanel == null)
			mainPanel = new JPanel();
		mainPanel.setBounds(10, 10, 490, 335);
		mainPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), ""));
		mainPanel.setLayout(null);

		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		mainFrame.setLocation((screenSize.width - appSize.width) / 2, (screenSize.height - appSize.height) / 2);
		mainFrame.setJMenuBar(menuBar.getMenuBar());
		mainFrame.setVisible(true);

		ptmsMap = new HashMap<Long, List<PTM>>();

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

		JOptionPane.showMessageDialog(null, "PTM(s) have been loaded successfully!",
				"P2Location - Post-translational modifications", JOptionPane.INFORMATION_MESSAGE,
				new ImageIcon(getClass().getResource("/images/logo.png")));
	}

	/**
	 * Method responsible for running task
	 */
	@Override
	public void run(TaskMonitor taskMonitor) throws Exception {

		taskMonitor.setTitle("P2Location - Load post-translational modifications task");

		if (cyApplicationManager.getCurrentNetwork() == null) {
			throw new Exception("ERROR: No networks has been loaded.");
		}

		setFrameObjects(taskMonitor);

		// Display the window
		mainFrame.add(mainPanel, BorderLayout.CENTER);
		mainFrame.setLocationRelativeTo(null);
		mainFrame.setVisible(true);
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

		int offset_y = 0;

		information_panel = new JPanel();
		information_panel.setBorder(BorderFactory.createTitledBorder(""));
		information_panel.setBounds(10, 8, 355, 116);
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
			textLabel_Protein_lbl_2 = new JLabel("have their PTM(s) loaded.");
			textLabel_Protein_lbl_2.setBounds(10, offset_y, 250, 40);
		} else {
			textLabel_Protein_lbl_2 = new JLabel("PTM(s) loaded.");
			textLabel_Protein_lbl_2.setBounds(10, offset_y, 100, 40);
		}
		textLabel_Protein_lbl_2.setFont(new java.awt.Font("Tahoma", Font.PLAIN, 12));
		information_panel.add(textLabel_Protein_lbl_2);
		offset_y += 30;

		JLabel textLabel_PTM = new JLabel("Search for PTMs:");
		textLabel_PTM.setFont(new java.awt.Font("Tahoma", Font.PLAIN, 12));
		textLabel_PTM.setBounds(10, offset_y, 100, 40);
		information_panel.add(textLabel_PTM);

		offset_y = 80;

		textLabel_status_result = new JLabel("");
		textLabel_status_result.setFont(new java.awt.Font("Tahoma", Font.PLAIN, 12));
		textLabel_status_result.setForeground(new Color(159, 17, 17));
		if (Util.isUnix())
			textLabel_status_result.setBounds(65, offset_y, 350, 40);
		else
			textLabel_status_result.setBounds(55, offset_y, 350, 40);

		JPanel logo_panel = new JPanel();
		logo_panel.setBorder(BorderFactory.createTitledBorder(""));
		logo_panel.setBounds(370, 8, 140, 116);
		logo_panel.setLayout(null);
		mainPanel.add(logo_panel);

		JLabel jLabelIcon = new JLabel();
		jLabelIcon.setBounds(13, -95, 300, 300);
		jLabelIcon.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/logo.png")));
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

		Object[][] ptmDataObj = new Object[1][2];
		// create table model with data
		ptmTableDataModel = new DefaultTableModel(ptmDataObj, columnNamesPTMTable) {
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
				return columnClassPTMTable[columnIndex];
			}
		};

		mainProteinPTMTable = new JTable(ptmTableDataModel);
		Action insertLineToTableAction = new AbstractAction("insertLineToTable") {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent evt) {
				ptmTableDataModel.addRow(new Object[] { "" });

				Util.updateRowHeader(ptmTableDataModel.getRowCount(), mainProteinPTMTable, rowHeaderPTMTable,
						proteinPTMTableScrollPanel);
				textLabel_status_result.setText("Row has been inserted.");
			}
		};

		KeyStroke keyStrokeInsertLine = KeyStroke.getKeyStroke(KeyEvent.VK_I, InputEvent.CTRL_DOWN_MASK);
		mainProteinPTMTable.getActionMap().put("insertLineToTable", insertLineToTableAction);
		mainProteinPTMTable.getInputMap(JTable.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(keyStrokeInsertLine,
				"insertLineToTable");

		Action deleteLineToTableAction = new AbstractAction("deleteLineToTable") {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent evt) {

				if (mainProteinPTMTable.getSelectedRow() != -1) {

					int input = JOptionPane.showConfirmDialog(null, "Do you confirm the removal of the line "
							+ (mainProteinPTMTable.getSelectedRow() + 1) + "?");
					// 0=yes, 1=no, 2=cancel
					if (input == 0) {
						// remove selected row from the model
						ptmTableDataModel.removeRow(mainProteinPTMTable.getSelectedRow());
						Util.updateRowHeader(ptmTableDataModel.getRowCount(), mainProteinPTMTable, rowHeaderPTMTable,
								proteinPTMTableScrollPanel);
					}
				}

				textLabel_status_result.setText("Row has been deleted.");
			}
		};

		KeyStroke keyStrokeDeleteLine = KeyStroke.getKeyStroke(KeyEvent.VK_D, InputEvent.CTRL_DOWN_MASK);
		mainProteinPTMTable.getActionMap().put("deleteLineToTable", deleteLineToTableAction);
		mainProteinPTMTable.getInputMap(JTable.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(keyStrokeDeleteLine,
				"deleteLineToTable");

		final KeyStroke copy = KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK, false);
		// Identifying the copy KeyStroke user can modify this
		// to copy on some other Key combination.
		final KeyStroke paste = KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_DOWN_MASK, false);
		// Identifying the Paste KeyStroke user can modify this
		// to copy on some other Key combination.
		mainProteinPTMTable.registerKeyboardAction(this, "Copy", copy, JComponent.WHEN_FOCUSED);
		mainProteinPTMTable.registerKeyboardAction(this, "Paste", paste, JComponent.WHEN_FOCUSED);
		clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();

		// Create the scroll pane and add the table to it.
		proteinPTMTableScrollPanel = new JScrollPane();
		proteinPTMTableScrollPanel.setBounds(10, 130, 500, 105);
		proteinPTMTableScrollPanel.setViewportView(mainProteinPTMTable);
		proteinPTMTableScrollPanel.setRowHeaderView(rowHeaderPTMTable);
		setTableProperties(1);
		mainPanel.add(proteinPTMTableScrollPanel);
	}

	/**
	 * Set properties to the Node domain table
	 * 
	 * @param number_lines total number of lines
	 */
	public static void setTableProperties(int number_lines) {
		if (mainProteinPTMTable != null) {
			mainProteinPTMTable.setPreferredScrollableViewportSize(new Dimension(490, 90));
			mainProteinPTMTable.getColumnModel().getColumn(0).setPreferredWidth(150);
			mainProteinPTMTable.getColumnModel().getColumn(1).setPreferredWidth(150);
			mainProteinPTMTable.setFillsViewportHeight(true);
			mainProteinPTMTable.setAutoCreateRowSorter(true);

			Util.updateRowHeader(number_lines, mainProteinPTMTable, rowHeaderPTMTable, proteinPTMTableScrollPanel);
		}
	}

	/**
	 * Method responsible for initializing all button in the Frame
	 * 
	 * @param taskMonitor
	 */
	private void initButtons(final TaskMonitor taskMonitor) {

		Icon iconBtn = new ImageIcon(getClass().getResource("/images/browse_Icon.png"));
		proteinPTMServerButton = new JButton(iconBtn);
		if (Util.isWindows())
			proteinPTMServerButton.setBounds(119, 55, 30, 30);
		else if (Util.isMac())
			proteinPTMServerButton.setBounds(123, 55, 30, 30);
		else
			proteinPTMServerButton.setBounds(148, 55, 30, 30);

		proteinPTMServerButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
		proteinPTMServerButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				taskMonitor.setTitle("Post-translational modifications...");

				if (isPTMLoaded) {
					try {

						isPlotDone = false;
						textLabel_status_result.setText("Getting protein domains...");
						taskMonitor.showMessage(TaskMonitor.Level.INFO, "Getting protein domains...");
						String msgError = getNodesFromTable(myNetwork, true);
						if (!msgError.isBlank() && !msgError.isEmpty()) {
							textLabel_status_result.setText("ERROR: Check Task History.");
							taskMonitor.showMessage(TaskMonitor.Level.ERROR, msgError);
						} else {

							textLabel_status_result.setText("Accessing Uniprot database...");
							taskMonitor.showMessage(TaskMonitor.Level.INFO, "Accessing Uniprot database...");
							getPTMsFromServer(taskMonitor, myNetwork, true);

						}
					} catch (Exception exception) {
					}
				} else {
					JOptionPane.showMessageDialog(null, "Wait! There is another process in progress!",
							"P2Location - Post-translational modifications", JOptionPane.WARNING_MESSAGE);
				}
			}
		});
		information_panel.add(proteinPTMServerButton);

		Icon iconBtnOk = new ImageIcon(getClass().getResource("/images/okBtn.png"));
		okButton = new JButton(iconBtnOk);
		okButton.setText("OK");
		if (Util.isWindows())
			okButton.setBounds(30, 250, 220, 25);
		else
			okButton.setBounds(30, 240, 220, 25);

		okButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent ae) {

				boolean concluedProcess = true;
				try {

					if (!isPTMLoaded) {
						int input = JOptionPane.showConfirmDialog(null,
								"PTM process has not been finished yet. Do you want to close this window?",
								"P2Location - Post-translational modifications", JOptionPane.INFORMATION_MESSAGE);
						// 0=yes, 1=no, 2=cancel
						if (input == 0) {
							concluedProcess = true;
							if (ptmThread != null) {
								ptmDoStop = true;
								ptmThread.interrupt();
							}
						} else {
							concluedProcess = false;
							ptmDoStop = false;
						}
					}

					if (concluedProcess) {
						storePTMs(taskMonitor, myNetwork, true);

					}

				} catch (Exception e1) {
					textLabel_status_result.setText("ERROR: Check Task History.");
					taskMonitor.showMessage(TaskMonitor.Level.ERROR, "ERROR: " + e1.getMessage());

					if (storePTMsThread != null)
						storePTMsThread.interrupt();
				}
			}
		});
		mainPanel.add(okButton);

		Icon iconBtnCancel = new ImageIcon(getClass().getResource("/images/cancelBtn.png"));
		JButton cancelButton = new JButton(iconBtnCancel);
		cancelButton.setText("Cancel");
		if (Util.isWindows())
			cancelButton.setBounds(265, 250, 220, 25);
		else
			cancelButton.setBounds(265, 240, 220, 25);

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
	public static void getPTMsFromServer(TaskMonitor taskMonitor, CyNetwork myNetwork, boolean fromScreen) {

		String[] columnNamesPTMTable = { "Node Name", "PTM(s)" };
		ptmThread = new Thread() {
			public synchronized void run() {
				ptmDoStop = false;
				isPTMLoaded = false;

				String msgError = getPTMsForeachNode(myNetwork, taskMonitor);
				if (!msgError.isBlank() && !msgError.isEmpty()) {
					taskMonitor.showMessage(TaskMonitor.Level.ERROR, msgError);
					if (textLabel_status_result != null)
						textLabel_status_result.setText("ERROR: Check Task History.");
					isPTMLoaded = true;
				} else {
					Object[][] data = null;
					if (ptmsMap.size() > 0)
						data = new Object[ptmsMap.size()][2];
					else
						data = new Object[1][2];

					ptmTableDataModel.setDataVector(data, columnNamesPTMTable);
					int countPtnDomain = 0;

					for (Map.Entry<Long, List<PTM>> entry : ptmsMap.entrySet()) {
						Long nodeKey = entry.getKey();
						if (nodeKey == null)
							continue;

						final String node_name = myNetwork.getDefaultNodeTable().getRow(nodeKey).getRaw(CyNetwork.NAME)
								.toString();

						List<PTM> ptmList = entry.getValue();

						ptmTableDataModel.setValueAt(node_name, countPtnDomain, 0);
						ptmTableDataModel.setValueAt(ToStringPTMs(ptmList), countPtnDomain, 1);
						countPtnDomain++;
					}

					if (ptmsMap.size() > 0)
						setTableProperties(ptmsMap.size());
					else
						setTableProperties(1);
					isPTMLoaded = true;

					// It's called via command line
					if (!fromScreen) {
						try {
							storePTMs(taskMonitor, myNetwork, false);
						} catch (Exception e) {
							taskMonitor.showMessage(TaskMonitor.Level.ERROR, "ERROR: " + e.getMessage());
						}
					}
				}
			}
		};

		ptmThread.start();
	}

	/**
	 * Method responsible for getting protein domains from Pfam or Supfam database
	 * for each node
	 * 
	 * @param taskMonitor
	 */
	private static String getPTMsForeachNode(CyNetwork myNetwork, final TaskMonitor taskMonitor) {

		int old_progress = 0;
		int summary_processed = 0;
		int total_genes = ptmsMap.size();

		StringBuilder sb_error = new StringBuilder();

		CyRow myCurrentRow = null;

		for (Map.Entry<Long, List<PTM>> entry : ptmsMap.entrySet()) {

			if (ptmDoStop)
				break;

			Long key = entry.getKey();
			String node_name = myNetwork.getDefaultNodeTable().getRow(key).getRaw(CyNetwork.NAME).toString();

			CyNode node = Util.getNode(myNetwork, key);
			if (node != null) {

				myCurrentRow = myNetwork.getRow(node);

				List<PTM> new_ptms = Util.getPTMs(myCurrentRow, taskMonitor);
				if (new_ptms.size() > 0) {
					entry.setValue(new_ptms);
				}

			} else {
				sb_error.append("ERROR: Node " + node_name + " has not been found.\n");
			}
			summary_processed++;
			int new_progress = (int) ((double) summary_processed / (total_genes) * 100);
			if (new_progress > old_progress) {
				old_progress = new_progress;

				if (textLabel_status_result != null) {
					textLabel_status_result.setText("Getting PTM(s): " + old_progress + "%");
				}
				taskMonitor.showMessage(TaskMonitor.Level.INFO, "Getting PTM(s): " + old_progress + "%");
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
	 * Method responsible for converting PTMs collection into string
	 * 
	 * @param ptmsList
	 * @return
	 */
	private static String ToStringPTMs(List<PTM> ptmsList) {

		if (ptmsList == null || ptmsList.size() == 0) {
			return "";
		}

		StringBuilder sb_ptms = new StringBuilder();
		for (PTM ptm : ptmsList) {
			sb_ptms.append(ptm.name + "[" + ptm.residue + "-" + ptm.position + "],");
		}
		return sb_ptms.toString().substring(0, sb_ptms.toString().length() - 1);
	}

	/**
	 * Method responsible for storing protein domains
	 * 
	 * @param taskMonitor
	 * @throws Exception
	 */
	public static void storePTMs(TaskMonitor taskMonitor, final CyNetwork myNetwork, boolean isFromScreen)
			throws Exception {

		if (myNetwork == null) {
			throw new Exception("ERROR: No network has been found.");
		}

		storePTMsThread = new Thread() {

			public synchronized void run() {

				isPlotDone = false;
				if (isFromScreen)
					textLabel_status_result.setText("Checking nodes ...");
				taskMonitor.showMessage(TaskMonitor.Level.INFO, "Checking nodes...");
				String msgError = getNodesFromTable(myNetwork, !isFromScreen);
				if (!msgError.isBlank() && !msgError.isEmpty()) {
					textLabel_status_result.setText("ERROR: Check Task History.");
					taskMonitor.showMessage(TaskMonitor.Level.ERROR, msgError);

					isPlotDone = true;
					UpdateViewListener.isNodeModified = true;
					isStoredPTMs = true;

				} else {
					isStoredPTMs = false;
					if (isFromScreen)
						textLabel_status_result.setText("Setting nodes information...");
					taskMonitor.showMessage(TaskMonitor.Level.INFO, "Setting nodes information...");
					try {

						msgError = setNodesInformation(taskMonitor, myNetwork);
						if (!msgError.isBlank() && !msgError.isEmpty()) {
							textLabel_status_result.setText("WARNING: Check Task History.");
							taskMonitor.showMessage(TaskMonitor.Level.WARN, msgError);
						}

						if (ptmsMap.size() > 0) {
							Util.update_PTMColumn(taskMonitor, myNetwork, ptmsMap);
						}

						taskMonitor.setProgress(1.0);
						taskMonitor.showMessage(TaskMonitor.Level.INFO,
								"Post-translational modifications have been loaded successfully!");

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
					isStoredPTMs = true;
				}
			}
		};

		storePTMsThread.start();
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

		int old_progress = 0;
		int summary_processed = 0;
		int total_genes = ptmsMap.size();

		StringBuilder sb_error = new StringBuilder();

		for (Map.Entry<Long, List<PTM>> entry : ptmsMap.entrySet()) {
			Long key = entry.getKey();
			CyNode node = Util.getNode(myNetwork, key);
			if (node == null)
				continue;
			List<PTM> ptmList = entry.getValue();
			if (ptmList.size() > 0) {
				updatePTMsMap(myNetwork, node, ptmList);

			}

			summary_processed++;
			int new_progress = (int) ((double) summary_processed / (total_genes) * 100);
			if (new_progress > old_progress) {
				old_progress = new_progress;

				if (textLabel_status_result != null)
					textLabel_status_result.setText("Storing ptms: " + old_progress + "%");
				taskMonitor.showMessage(TaskMonitor.Level.INFO, "Storing ptms: " + old_progress + "%");
			}
		}

		taskMonitor.setProgress(0.98);
		return sb_error.toString();
	}

	/**
	 * Method responsible for canceling the loading process
	 * 
	 * @return true if process is canceled, otherwise, returns false.
	 */
	public static boolean cancelProcess() {

		boolean concluedProcess = true;

		if (!isPTMLoaded) {
			int input = JOptionPane.showConfirmDialog(null,
					"PTMs process has not been finished yet. Do you want to close this window?",
					"P2Location - Post-translational modifications", JOptionPane.INFORMATION_MESSAGE);
			// 0=yes, 1=no, 2=cancel
			if (input == 0) {
				concluedProcess = true;
				if (ptmThread != null) {
					ptmDoStop = true;
					ptmThread.interrupt();
				}
			} else {
				concluedProcess = false;
				ptmDoStop = false;
			}
		}

		if (!okButton.isEnabled() && !isStoredPTMs) {
			int input = JOptionPane.showConfirmDialog(null,
					"PTMs have not been stored yet. Do you want to close this window?",
					"P2Location - Load post-translational modifications", JOptionPane.INFORMATION_MESSAGE);
			// 0=yes, 1=no, 2=cancel
			if (input == 0) {
				concluedProcess = true;
				if (storePTMsThread != null) {
					storePTMsThread.interrupt();
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
	 * Get all nodes filled out in JTable
	 */
	public static String getNodesFromTable(CyNetwork myNetwork, boolean retrieveAllNodes) {

		StringBuilder sbError = new StringBuilder();

		int old_progress = 0;
		int summary_processed = 0;
		int total_rows = ptmTableDataModel.getRowCount();

		for (int row = 0; row < ptmTableDataModel.getRowCount(); row++) {
			String protein = ptmTableDataModel.getValueAt(row, 0) != null
					? ptmTableDataModel.getValueAt(row, 0).toString()
					: "";

			List<PTM> ptms = new ArrayList<PTM>();
			String ptmsStr = ptmTableDataModel.getValueAt(row, 1) != null
					? ptmTableDataModel.getValueAt(row, 1).toString()
					: "";
			if (!ptmsStr.isBlank() && !ptmsStr.isEmpty()) {

				try {
					String[] cols = ptmsStr.split(",");
					for (String col : cols) {
						String[] ptmsArray = col.split("\\[|\\]");
						String ptmName = ptmsArray[0].trim();
						String[] colRange = ptmsArray[1].split("-");
						char residue = colRange[0].charAt(0);
						int position = Integer.parseInt(colRange[1]);
						ptms.add(new PTM(ptmName, residue, position));
					}
				} catch (Exception e) {
					sbError.append("ERROR: Row: " + (row + 1)
							+ " - PTMs don't match with the pattern 'name[residue-position]'\n");
				}
			}
			if (protein.isEmpty() || protein.isBlank()) {
				sbError.append("ERROR: Row: " + (row + 1) + " - Protein is empty.");
			} else {
				CyNode current_node = Util.getNode(myNetwork, protein);
				ptmsMap.put(current_node.getSUID(), ptms);
			}

			summary_processed++;
			int new_progress = (int) ((double) summary_processed / (total_rows) * 100);
			if (new_progress > old_progress) {
				old_progress = new_progress;

				if (textLabel_status_result != null)
					textLabel_status_result.setText("Checking nodes: " + old_progress + "%");
			}

		}

		if (retrieveAllNodes && ptmsMap.size() == 0 && sbError.toString().equals("ERROR: Row: 1 - Protein is empty.")) {
			// No protein is filled in the table. Then, get all proteins

			fillAllNodesInTheTable(myNetwork);
			getNodesFromTable(myNetwork, retrieveAllNodes);
			return "";
		}

		if (ptmsMap.size() == 0)
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
		String[] columnNamesPTMTable = { "Node Name", "PTM(s)" };
		ptmTableDataModel.setDataVector(data, columnNamesPTMTable);

		for (String line : data_to_be_stored) {
			String[] cols_line = line.split("\t");
			ptmTableDataModel.setValueAt(cols_line[0], countPtnDomain, 0);
			countPtnDomain++;
		}

		setTableProperties(countPtnDomain);
	}

	/**
	 * Method responsible for update PTMs map
	 * 
	 * @param node
	 * @param myPTMs
	 */
	public static void updatePTMsMap(CyNetwork myNetwork, CyNode node, List<PTM> myPTMs) {
		String network_name = myNetwork.toString();
		if (Util.ptmsMap.containsKey(network_name)) {

			Map<Long, List<PTM>> all_ptms = Util.ptmsMap.get(network_name);
			all_ptms.put(node.getSUID(), myPTMs);

		} else {// Network does not exists

			Map<Long, List<PTM>> PTMs = new HashMap<Long, List<PTM>>();
			PTMs.put(node.getSUID(), myPTMs);
			Util.ptmsMap.put(network_name, PTMs);
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {

		final String actionCommand = e.getActionCommand();

		if (actionCommand.equals("Copy")) {
			StringBuilder sbf = new StringBuilder();
			// Check to ensure we have selected only a contiguous block of cells.
			final int numcols = mainProteinPTMTable.getSelectedColumnCount();
			final int numrows = mainProteinPTMTable.getSelectedRowCount();
			final int[] rowsselected = mainProteinPTMTable.getSelectedRows();
			final int[] colsselected = mainProteinPTMTable.getSelectedColumns();

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
					sbf.append(mainProteinPTMTable.getValueAt(rowsselected[i], colsselected[j]));
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

			final int startRow = (mainProteinPTMTable.getSelectedRows())[0];
			final int startCol = (mainProteinPTMTable.getSelectedColumns())[0];
			try {
				final String trString = (String) (clipboard.getContents(this).getTransferData(DataFlavor.stringFlavor));
				final StringTokenizer st1 = new StringTokenizer(trString, "\n");

				Object[][] data = new Object[st1.countTokens()][2];
				ptmTableDataModel.setDataVector(data, columnNamesPTMTable);

				int i = 0;
				for (i = 0; st1.hasMoreTokens(); i++) {
					rowstring = st1.nextToken();
					StringTokenizer st2 = new StringTokenizer(rowstring, "\t");
					for (int j = 0; st2.hasMoreTokens(); j++) {
						value = (String) st2.nextToken();
						if (startRow + i < mainProteinPTMTable.getRowCount()
								&& startCol + j < mainProteinPTMTable.getColumnCount()) {
							mainProteinPTMTable.setValueAt(value, startRow + i, startCol + j);
						}
					}
				}

				setTableProperties(i);

			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}
}
