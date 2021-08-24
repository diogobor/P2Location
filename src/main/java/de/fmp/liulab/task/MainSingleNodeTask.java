package de.fmp.liulab.task;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.DefaultFormatter;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTableUtil;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.view.model.VisualLexicon;
import org.cytoscape.view.model.VisualProperty;
import org.cytoscape.view.presentation.customgraphics.CyCustomGraphics2;
import org.cytoscape.view.presentation.customgraphics.CyCustomGraphics2Factory;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.presentation.property.values.BendFactory;
import org.cytoscape.view.presentation.property.values.HandleFactory;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.TaskMonitor.Level;

import de.fmp.liulab.core.ProteinStructureManager;
import de.fmp.liulab.internal.UpdateViewListener;
import de.fmp.liulab.internal.view.JFrameWithoutMaxAndMinButton;
import de.fmp.liulab.model.CrossLink;
import de.fmp.liulab.model.PDB;
import de.fmp.liulab.model.PTM;
import de.fmp.liulab.model.Protein;
import de.fmp.liulab.model.ProteinDomain;
import de.fmp.liulab.utils.Util;

/**
 * Class responsible for applying layout to a node
 * 
 * @author diogobor
 *
 */
public class MainSingleNodeTask extends AbstractTask implements ActionListener {

	public static boolean isPlotDone = false;

	private CyApplicationManager cyApplicationManager;
	private CyNetwork myNetwork;
	private CyNetworkView netView;
	private CyCustomGraphics2Factory vgFactory;
	private HandleFactory handleFactory;
	private BendFactory bendFactory;

	private boolean forcedWindowOpen;
	public static VisualStyle style;
	public static VisualLexicon lexicon;

	private static View<CyNode> nodeView;
	public CyRow myCurrentRow;
	private List<CyNode> nodes;
	private boolean isCurrentNode_modified = false;

	private Protein myProtein;
	private ArrayList<PTM> myPTMs;
	private ArrayList<CrossLink> myMonolinks;

	// Window
	private JFrameWithoutMaxAndMinButton mainFrame;
	private JPanel mainPanel;
	private JPanel protein_panel;
	private JPanel domain_panel;
	private JPanel ptm_panel;
	private JLabel textLabel_status_result;
	private String[] columnNamesDomainTable = { "Domain(*)", "Start Residue(*)", "End Residue(*)", "e-value", "Color" };
	private final Class[] columnClassDomainTable = new Class[] { String.class, Integer.class, Integer.class,
			String.class, String.class };
	private DefaultTableModel domainTableDataModel;
	private static JTable mainProteinDomainTable;
	private static JList rowHeaderDomainTable;
	private static JScrollPane proteinDomainTableScrollPanel;

	private String[] columnNamesPTMTable = { "PTM", "Residue", "Position" };
	private final Class[] columnClassPTMTable = new Class[] { String.class, String.class, Integer.class };
	private DefaultTableModel ptmTableDataModel;
	private static JTable mainProteinPTMTable;
	private static JList rowHeaderPTMTable;
	private static JScrollPane proteinPTMTableScrollPanel;

	public static CyNode node;

	private Thread pfamThread;
	private Thread pyMOLThread;
	private Thread uniprotThread;
	private JButton proteinDomainServerButton;
	private JButton proteinPTMServerButton;
	private static Thread applyLayoutThread;
	private static JButton okButton;

	private boolean IsCommandLine;

	private JButton pyMOLButton;

	/**
	 * Constructor
	 * 
	 * @param cyApplicationManager main app manager
	 * @param vmmServiceRef        visual mapping manager
	 * @param vgFactory            graphic factory
	 * @param bendFactory          bend factory
	 * @param handleFactory        handle factory
	 * @param forcedWindowOpen     forced window open
	 */
	public MainSingleNodeTask(CyApplicationManager cyApplicationManager, final VisualMappingManager vmmServiceRef,
			CyCustomGraphics2Factory<?> vgFactory, BendFactory bendFactory, HandleFactory handleFactory,
			boolean forcedWindowOpen, boolean isCommandLine) {

		this.cyApplicationManager = cyApplicationManager;
		this.netView = cyApplicationManager.getCurrentNetworkView();
		this.myNetwork = cyApplicationManager.getCurrentNetwork();
		this.vgFactory = vgFactory;
		style = vmmServiceRef.getCurrentVisualStyle();
		// Get the current Visual Lexicon
		lexicon = cyApplicationManager.getCurrentRenderingEngine().getVisualLexicon();
		this.bendFactory = bendFactory;
		this.handleFactory = handleFactory;
		this.forcedWindowOpen = forcedWindowOpen;
		this.IsCommandLine = isCommandLine;

		// Initialize protein domain colors map if LoadProteinDomainTask has not been
		// initialized
		Util.init_availableProteinDomainColorsMap();

		if (mainFrame == null)
			mainFrame = new JFrameWithoutMaxAndMinButton(new JFrame(), "Predict Location Protein - Single Node", 0);

		mainFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		Dimension appSize = null;

		if (Util.isWindows()) {
			appSize = new Dimension(540, 615);
		} else if (Util.isMac()) {
			appSize = new Dimension(520, 585);
		} else {
			appSize = new Dimension(520, 610);
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

	}

	/**
	 * Method responsible for initializing the task
	 */
	@Override
	public void run(final TaskMonitor taskMonitor) throws Exception {

		taskMonitor.setTitle("P2Location - Layout task");
		// Write your own function here.
		if (cyApplicationManager.getCurrentNetwork() == null) {
			throw new Exception("ERROR: No network has been loaded.");
		}

		checkSingleOrMultipleSelectedNodes(taskMonitor);

		if (IsCommandLine) {
			this.deselectNodes();
		}
	}

	/**
	 * Method responsible for deselecting all nodes.
	 */
	private void deselectNodes() {
		List<CyNode> selectedNodes = CyTableUtil.getNodesInState(myNetwork, CyNetwork.SELECTED, true);
		for (CyNode cyNode : selectedNodes) {
			myNetwork.getRow(cyNode).set(CyNetwork.SELECTED, false);
		}
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

			taskMonitor.showMessage(TaskMonitor.Level.INFO, "No node has been selected.");

		} else if (nodes.size() > 1) {

			executeMultipleNodes(taskMonitor);

		} else {
			node = nodes.get(0);
			executeSingleNode(taskMonitor);
		}

	}

	/**
	 * Method responsible for executing layout to a single node
	 * 
	 * @param taskMonitor
	 * @throws Exception
	 */
	private void executeSingleNode(final TaskMonitor taskMonitor) throws Exception {

		getNodeInformation(taskMonitor);

		if (myProtein.intraLinks.size() == 0 && myProtein.interLinks.size() == 0) {// It's a intralink_single_node

			String nodeName = (String) myCurrentRow.getRaw(CyNetwork.NAME);
			if (!nodeName.contains("- Source") && !nodeName.contains("- Target") && !nodeName.contains("PTM - ")
					&& !nodeName.contains("MONOLINK")) {
				throw new Exception("There is neither intralink nor interlink for the protein: " + nodeName
						+ ".\nCheck the columns name.");
			}
			// Intralink (Source or Target) and PTM node
			return;
		}

		if (forcedWindowOpen && !IsCommandLine) {// Action comes from Context Menu item

			this.init_xl_layout(taskMonitor);

		} else {// Action comes from Shortcut

			if (isCurrentNode_modified) {
				if (isPlotDone)
					this.restoreDefaultStyle(taskMonitor);

			} else {

				this.setCurrentLayout(taskMonitor);

			}
		}
	}

	/**
	 * Method responsible for executing layout to multiple nodes
	 * 
	 * @param taskMonitor
	 * @throws Exception
	 */
	private void executeMultipleNodes(final TaskMonitor taskMonitor) throws Exception {

		forcedWindowOpen = false;
		for (CyNode current_node : nodes) {
			node = current_node;
			executeSingleNode(taskMonitor);
		}
	}

	@Override
	public void cancel() {

		this.cancel();
		super.cancel();
	}

	@Override
	public void actionPerformed(ActionEvent e) {

	}

	/**
	 * Method responsible for applying to node the current layout/style
	 * 
	 * @param taskMonitor
	 */
	private void setCurrentLayout(final TaskMonitor taskMonitor) {

		isPlotDone = false;
		ProcessProteinLocationTask.isPlotDone = false;
		Util.stopUpdateViewer = false;

		if (netView == null) {
			netView = cyApplicationManager.getCurrentNetworkView();
		}

		taskMonitor.showMessage(TaskMonitor.Level.INFO, "Getting the selected node...");
		nodeView = netView.getNodeView(node);

		double tmp_scaling_factor = Util.node_label_factor_size;
		Util.node_label_factor_size = 1.0;
		taskMonitor.showMessage(TaskMonitor.Level.INFO, "Setting node styles...");

		Util.setNodeStyles(myNetwork, node, netView, style, Util.getProteinSequenceFromUniprot(myCurrentRow));
		taskMonitor.setProgress(0.2);

		taskMonitor.showMessage(TaskMonitor.Level.INFO, "Getting protein domains...");
		getProteinDomains(node);

		taskMonitor.showMessage(TaskMonitor.Level.INFO, "Setting protein domains to node...");
		setNodeDomainColors(taskMonitor);
		taskMonitor.setProgress(0.75);

		if (Util.showPTMs) {
			taskMonitor.showMessage(TaskMonitor.Level.INFO, "Getting post-translational modifications...");
			getPTMs(node);

			taskMonitor.showMessage(TaskMonitor.Level.INFO, "Setting post-translational modifications to node...");
			Util.setNodePTMs(taskMonitor, myNetwork, netView, node, style, handleFactory, bendFactory, lexicon, myPTMs,
					false);
			taskMonitor.setProgress(0.85);
		}

		if (Util.showMonolinkedPeptides) {
			taskMonitor.showMessage(TaskMonitor.Level.INFO, "Getting monolinks...");
			getMonolinks(node);

			taskMonitor.showMessage(TaskMonitor.Level.INFO, "Setting monolinks to node...");
			Util.setMonolinksToNode(taskMonitor, myNetwork, netView, node, style, handleFactory, bendFactory, lexicon,
					myMonolinks, getPtnSequenceOfMonolinks(node));
			taskMonitor.setProgress(0.85);
		}

		taskMonitor.showMessage(TaskMonitor.Level.INFO, "Defining styles for cross-links...");
		isPlotDone = Util.addOrUpdateEdgesToNetwork(myNetwork, node, style, netView, nodeView, handleFactory,
				bendFactory, lexicon, Util.getProteinLengthScalingFactor(), myProtein, taskMonitor, null);
		taskMonitor.setProgress(0.95);
		Util.node_label_factor_size = tmp_scaling_factor;

		if (Util.node_label_factor_size != 1)
			resizeProtein(taskMonitor);

		Util.updateMapNodesPosition(node, nodeView);

		// Apply the change to the view
		style.apply(netView);
		netView.updateView();
		taskMonitor.setProgress(1.0);
	}

	/**
	 * Update domain annotations in the cytoscape node table
	 * 
	 * @param taskMonitor
	 * @param proteinDomainList
	 */
	private void update_protein_domain_table(TaskMonitor taskMonitor, List<ProteinDomain> proteinDomainList) {

		if (proteinDomainList.size() > 0) {
			String node_name = myNetwork.getDefaultNodeTable().getRow(node.getSUID()).getRaw(CyNetwork.NAME).toString();

			// Update proteinsMap
			List<Protein> proteinList = Util.proteinsMap.get(myNetwork.toString());
			Optional<Protein> isPtnPresent = proteinList.stream().filter(value -> value.gene.equals(node_name))
					.findFirst();

			if (isPtnPresent.isPresent()) {
				Protein _myProtein = isPtnPresent.get();
				_myProtein.domains = proteinDomainList;
			}

			Util.updateProteins(taskMonitor, myNetwork, null);
		}
	}

	/**
	 * Update ptms in the cytoscape node table
	 * 
	 * @param taskMonitor
	 * @param proteinDomainList
	 */
	private void update_ptms_table(TaskMonitor taskMonitor, List<PTM> ptmsList) {

		if (ptmsList.size() > 0) {
			Util.update_PTMColumn(taskMonitor, myNetwork, ptmsList, node);
		}
	}

	/**
	 * Resize protein node
	 * 
	 * @param taskMonitor task monitor
	 */
	private void resizeProtein(final TaskMonitor taskMonitor) {
		isPlotDone = false;
		taskMonitor.showMessage(TaskMonitor.Level.INFO, "Resizing node length...");
		Util.setNodeStyles(myNetwork, node, netView, style, Util.getProteinSequenceFromUniprot(myCurrentRow));
		taskMonitor.setProgress(0.2);

		if (Util.showPTMs) {
			Util.setNodePTMs(taskMonitor, myNetwork, netView, node, style, handleFactory, bendFactory, lexicon, myPTMs,
					true);
		}

		if (Util.showMonolinkedPeptides) {
			Util.setMonolinksToNode(taskMonitor, myNetwork, netView, node, style, handleFactory, bendFactory, lexicon,
					myMonolinks, getPtnSequenceOfMonolinks(node));
		}

		taskMonitor.showMessage(TaskMonitor.Level.INFO, "Resizing edges...");
		isPlotDone = Util.addOrUpdateEdgesToNetwork(myNetwork, node, style, netView, nodeView, handleFactory,
				bendFactory, lexicon, Util.getProteinLength(), myProtein, taskMonitor, null);
	}

	/**
	 * Method responsible for getting all ptms of the selected node from the main
	 * map (Util.ptmsMap)
	 * 
	 * @param node
	 */
	private void getPTMs(CyNode node) {

		boolean hasPTMs = false;

		String network_name = myNetwork.toString();
		if (Util.ptmsMap.containsKey(network_name)) {

			Map<Long, List<PTM>> all_ptms = Util.ptmsMap.get(network_name);

			if (all_ptms.containsKey(node.getSUID())) {
				hasPTMs = true;
				myPTMs = (ArrayList<PTM>) all_ptms.get(node.getSUID());
			}
		}

		if (!hasPTMs) {
			myPTMs = new ArrayList<PTM>();
		}
	}

	/**
	 * Method responsible for getting the protein sequence of a respective node with
	 * monolinks
	 * 
	 * @param node
	 * @return
	 */
	private String getPtnSequenceOfMonolinks(CyNode node) {
		boolean hasMonolinks = false;
		String ptnSequence = "";

		String network_name = myNetwork.toString();
		if (Util.monolinksMap.containsKey(network_name)) {

			Map<Long, Protein> all_monolinks = Util.monolinksMap.get(network_name);

			if (all_monolinks.containsKey(node.getSUID())) {
				hasMonolinks = true;

				ptnSequence = (String) ((Protein) all_monolinks.get(node.getSUID())).sequence;
			}
		}

		if (!hasMonolinks) {
			ptnSequence = "";
		}

		return ptnSequence;
	}

	/**
	 * Method responsible for getting all monolinks of the selected node from the
	 * main map (Util.ptmsMap)
	 * 
	 * @param node
	 */
	private void getMonolinks(CyNode node) {

		boolean hasMonolinks = false;

		String network_name = myNetwork.toString();
		if (Util.monolinksMap.containsKey(network_name)) {

			Map<Long, Protein> all_monolinks = Util.monolinksMap.get(network_name);

			if (all_monolinks.containsKey(node.getSUID())) {
				hasMonolinks = true;

//				myMonolinks = (ArrayList<CrossLink>) ((Protein) all_monolinks.get(node.getSUID())).monolinks;
			}
		}

		if (!hasMonolinks) {
			myMonolinks = new ArrayList<CrossLink>();
		}
	}

	/**
	 * Method responsible for getting the protein domains of the selected node from
	 * the main map (Util.proteinDomainsMap)
	 * 
	 * @param node
	 */
	private void getProteinDomains(CyNode node) {

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
	 * Method responsible for opening the Single Node Layout window
	 * 
	 * @param taskMonitor
	 */
	private void init_xl_layout(final TaskMonitor taskMonitor) {

		isPlotDone = false;
		setFrameObjects(taskMonitor);
		// Display the window
		mainFrame.add(mainPanel, BorderLayout.CENTER);
		mainFrame.setLocationRelativeTo(null);
		mainFrame.setVisible(true);
	}

	/**
	 * Get all information of a node
	 * 
	 * @throws Exception
	 */
	public void getNodeInformation(TaskMonitor taskMonitor) throws Exception {

		// ##### GET THE SELECTED NODE - ONLY ONE IS POSSIBLE TO APPLY CHANGES ######

		myCurrentRow = myNetwork.getRow(node);

		String node_name = myNetwork.getDefaultNodeTable().getRow(node.getSUID()).getRaw(CyNetwork.NAME).toString();
		myProtein = Util.getProtein(myNetwork, node_name);
		if (myProtein == null) {
			throw new Exception("There is no information in column 'sequence' or 'domain_annotation' for the protein: "
					+ node_name);
		}

		String nodeName = (String) myCurrentRow.getRaw(CyNetwork.NAME);
		if (nodeName.contains("PTM - "))
			return;

		nodeView = netView.getNodeView(node);
		Object length_protein_a = myCurrentRow.getRaw(Util.PROTEIN_LENGTH_A);
		Object length_protein_b = myCurrentRow.getRaw(Util.PROTEIN_LENGTH_B);

		double currentNodeWidth = nodeView.getVisualProperty(BasicVisualLexicon.NODE_WIDTH);
		if (currentNodeWidth == 0)
			currentNodeWidth = 1;

		if (length_protein_a == null) {
			if (length_protein_b == null) {

				if (!nodeName.contains("- Source") && !nodeName.contains("- Target")
						&& !nodeName.contains("MONOLINK")) {
					throw new Exception(
							"There is no information in column 'length_protein_a' or 'length_protein_b' for the protein: "
									+ nodeName);
				}
				length_protein_a = 10;// Intralink (Source or Target) node
			} else
				length_protein_a = length_protein_b;
		}

		/**
		 * Modify node style
		 */
		currentNodeWidth = ((Number) length_protein_a).doubleValue();
		Util.setProteinLength((float) currentNodeWidth);

		isCurrentNode_modified = Util.IsNodeModified(myNetwork, netView, node);
		Util.node_label_factor_size = myNetwork.getRow(node).get(Util.PROTEIN_SCALING_FACTOR_COLUMN_NAME,
				Double.class) == null ? 1.0
						: myNetwork.getRow(node).get(Util.PROTEIN_SCALING_FACTOR_COLUMN_NAME, Double.class);
		Util.isProtein_expansion_horizontal = myNetwork.getRow(node).get(Util.HORIZONTAL_EXPANSION_COLUMN_NAME,
				Boolean.class) == null ? true
						: myNetwork.getRow(node).get(Util.HORIZONTAL_EXPANSION_COLUMN_NAME, Boolean.class);

	}

	/**
	 * Set properties to the Node domain table
	 */
	private void setTableProperties(int number_lines, int table) {

		if (table == 0) {
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
		} else {
			if (mainProteinPTMTable != null) {
				mainProteinPTMTable.setPreferredScrollableViewportSize(new Dimension(490, 90));
				mainProteinPTMTable.getColumnModel().getColumn(0).setPreferredWidth(150);
				mainProteinPTMTable.getColumnModel().getColumn(1).setPreferredWidth(150);
				mainProteinPTMTable.setFillsViewportHeight(true);
				mainProteinPTMTable.setAutoCreateRowSorter(true);

				Util.updateRowHeader(number_lines, mainProteinPTMTable, rowHeaderPTMTable, proteinPTMTableScrollPanel);
			}
		}
	}

	/**
	 * Set all labels in P2Location window / frame
	 */
	private void setFrameLabels() {

		int offset_y = -20;

		protein_panel = new JPanel();
		protein_panel.setBorder(BorderFactory.createTitledBorder("Protein"));
		if (Util.isWindows())
			protein_panel.setBounds(10, 10, 250, 150);
		else if (Util.isMac())
			protein_panel.setBounds(10, 10, 275, 150);
		else
			protein_panel.setBounds(10, 10, 277, 150);
		protein_panel.setLayout(null);
		mainPanel.add(protein_panel);

		JLabel textLabel_Protein_lbl = new JLabel("Name:");
		textLabel_Protein_lbl.setFont(new java.awt.Font("Tahoma", Font.PLAIN, 12));
		textLabel_Protein_lbl.setBounds(10, offset_y, 50, 100);
		protein_panel.add(textLabel_Protein_lbl);

		JLabel textLabel_Protein_result = new JLabel();
		textLabel_Protein_result.setText((String) myCurrentRow.getRaw(CyNetwork.NAME));
		textLabel_Protein_result.setFont(new java.awt.Font("Tahoma", Font.BOLD, 12));
		if (Util.isUnix())
			textLabel_Protein_result.setBounds(105, offset_y, 100, 100);
		else
			textLabel_Protein_result.setBounds(95, offset_y, 100, 100);
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
			textLabel_Protein_size_result.setBounds(105, offset_y, 100, 100);
		else
			textLabel_Protein_size_result.setBounds(95, offset_y, 100, 100);
		protein_panel.add(textLabel_Protein_size_result);
		offset_y += 30;

		JLabel textLabel_Protein_expansion_lbl = new JLabel("Expansion:");
		textLabel_Protein_expansion_lbl.setFont(new java.awt.Font("Tahoma", Font.PLAIN, 12));
		textLabel_Protein_expansion_lbl.setBounds(10, offset_y, 70, 100);
		protein_panel.add(textLabel_Protein_expansion_lbl);
		offset_y += 40;

		boolean isHorizontalExpansion = myNetwork.getRow(node).get(Util.HORIZONTAL_EXPANSION_COLUMN_NAME,
				Boolean.class) == null ? true
						: myNetwork.getRow(node).get(Util.HORIZONTAL_EXPANSION_COLUMN_NAME, Boolean.class);
		JRadioButton protein_expansion_horizontal = new JRadioButton("Horizontal");
		protein_expansion_horizontal.setSelected(isHorizontalExpansion);
		if (Util.isWindows()) {
			protein_expansion_horizontal.setBounds(89, offset_y, 90, 20);
		} else if (Util.isMac()) {
			protein_expansion_horizontal.setBounds(89, offset_y, 105, 20);
		} else {
			protein_expansion_horizontal.setBounds(105, offset_y, 105, 20);
		}
		protein_expansion_horizontal.addItemListener(new ItemListener() {

			@Override
			public void itemStateChanged(ItemEvent event) {
				int state = event.getStateChange();
				if (state == ItemEvent.SELECTED) {

					Util.isProtein_expansion_horizontal = true;
				} else if (state == ItemEvent.DESELECTED) {

					Util.isProtein_expansion_horizontal = false;
				}
			}
		});
		protein_panel.add(protein_expansion_horizontal);

		JRadioButton protein_expansion_vertical = new JRadioButton("Vertical");
		protein_expansion_vertical.setSelected(!isHorizontalExpansion);
		if (Util.isWindows()) {
			protein_expansion_vertical.setBounds(179, offset_y, 63, 20);
		} else if (Util.isMac()) {
			protein_expansion_vertical.setBounds(185, offset_y, 90, 20);
		} else {
			protein_expansion_vertical.setBounds(195, offset_y, 90, 20);
		}
		protein_expansion_vertical.addItemListener(new ItemListener() {

			@Override
			public void itemStateChanged(ItemEvent event) {
				int state = event.getStateChange();
				if (state == ItemEvent.SELECTED) {

					Util.isProtein_expansion_horizontal = false;
				} else if (state == ItemEvent.DESELECTED) {

					Util.isProtein_expansion_horizontal = true;
				}
			}
		});
		protein_panel.add(protein_expansion_vertical);
		ButtonGroup bg_expansion = new ButtonGroup();
		bg_expansion.add(protein_expansion_horizontal);
		bg_expansion.add(protein_expansion_vertical);
		offset_y -= 10;

		JLabel factor_size_node = new JLabel("Scaling factor:");
		factor_size_node.setFont(new java.awt.Font("Tahoma", Font.PLAIN, 12));
		factor_size_node.setBounds(10, offset_y, 90, 100);
		protein_panel.add(factor_size_node);

		offset_y = 165;

		JRadioButton protein_domain_pfam = new JRadioButton("Pfam");
		protein_domain_pfam.setSelected(Util.isProteinDomainPfam);
		if (Util.isWindows()) {
			protein_domain_pfam.setBounds(179, offset_y, 50, 20);
		} else if (Util.isMac()) {
			protein_domain_pfam.setBounds(193, offset_y, 65, 20);
		} else {
			protein_domain_pfam.setBounds(198, offset_y, 65, 20);
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
		mainPanel.add(protein_domain_pfam);

		JRadioButton protein_domain_supfam = new JRadioButton("Supfam");
		protein_domain_supfam.setSelected(!Util.isProteinDomainPfam);
		if (Util.isWindows()) {
			protein_domain_supfam.setBounds(119, offset_y, 64, 20);
		} else if (Util.isMac()) {
			protein_domain_supfam.setBounds(119, offset_y, 79, 20);
		} else {
			protein_domain_supfam.setBounds(124, offset_y, 79, 20);
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
		mainPanel.add(protein_domain_supfam);

		ButtonGroup bg_database = new ButtonGroup();
		bg_database.add(protein_domain_pfam);
		bg_database.add(protein_domain_supfam);

		offset_y -= 40;

		JLabel textLabel_Pfam = new JLabel("Search for domains:");
		textLabel_Pfam.setFont(new java.awt.Font("Tahoma", Font.PLAIN, 12));
		textLabel_Pfam.setBounds(10, offset_y, 150, 100);
		mainPanel.add(textLabel_Pfam);

		if (Util.isWindows())
			offset_y += 65;
		else
			offset_y += 55;

		textLabel_status_result = new JLabel("???");
		textLabel_status_result.setFont(new java.awt.Font("Tahoma", Font.PLAIN, 12));
		textLabel_status_result.setForeground(new Color(159, 17, 17));
		textLabel_status_result.setBounds(65, offset_y, 350, 40);

		JPanel logo_panel = new JPanel();
		logo_panel.setBorder(BorderFactory.createTitledBorder(""));
		if (Util.isWindows())
			logo_panel.setBounds(265, 16, 245, 142);
		else if (Util.isMac())
			logo_panel.setBounds(290, 16, 220, 142);
		else
			logo_panel.setBounds(290, 25, 220, 132);
		logo_panel.setLayout(null);
		mainPanel.add(logo_panel);

		JLabel jLabelIcon = new JLabel();
		if (Util.isWindows())
			jLabelIcon.setBounds(70, -75, 300, 300);
		else
			jLabelIcon.setBounds(55, -75, 300, 300);
		jLabelIcon.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/logo.png")));
		logo_panel.add(jLabelIcon);

		if (Util.isWindows())
			offset_y -= 35;
		else
			offset_y -= 25;

		JLabel textLabel_PyMOL = new JLabel("Visualize structure (PyMOL):");
		textLabel_PyMOL.setFont(new java.awt.Font("Tahoma", Font.PLAIN, 12));
		if (Util.isWindows())
			textLabel_PyMOL.setBounds(265, offset_y, 180, 40);
		else if (Util.isMac())
			textLabel_PyMOL.setBounds(290, offset_y, 180, 40);
		else
			textLabel_PyMOL.setBounds(292, offset_y, 180, 40);
		mainPanel.add(textLabel_PyMOL);

		if (Util.isWindows())
			offset_y += 5;
		else
			offset_y -= 5;
		JLabel textLabel_status = new JLabel("Status:");
		textLabel_status.setFont(new java.awt.Font("Tahoma", Font.PLAIN, 12));
		textLabel_status.setBounds(10, offset_y, 50, 100);
		mainPanel.add(textLabel_status);
		mainPanel.add(textLabel_status_result);

		if (Util.isWindows())
			offset_y = 320;
		else
			offset_y = 310;
		JLabel textLabel_required_fields = new JLabel("(*) Required fields");
		textLabel_required_fields.setFont(new java.awt.Font("Tahoma", Font.PLAIN, 10));
		textLabel_required_fields.setBounds(20, offset_y, 150, 50);
		mainPanel.add(textLabel_required_fields);

		if (Util.isWindows())
			offset_y += 43;
		else
			offset_y += 40;

		JLabel textLabel_search_for_ptms = new JLabel("Search for PTMs:");
		textLabel_search_for_ptms.setFont(new java.awt.Font("Tahoma", Font.PLAIN, 12));
		textLabel_search_for_ptms.setBounds(10, offset_y, 100, 40);
		mainPanel.add(textLabel_search_for_ptms);

	}

	/**
	 * Set all objects to the main Frame
	 * 
	 * @param taskMonitor
	 */
	private void setFrameObjects(final TaskMonitor taskMonitor) {

		setFrameLabels();

		double current_scaling_factor = myNetwork.getRow(node).get(Util.PROTEIN_SCALING_FACTOR_COLUMN_NAME,
				Double.class);

		SpinnerModel factor_size_node = new SpinnerNumberModel(current_scaling_factor, // initial value
				0.1, // min
				1, // max
				0.1); // step
		final JSpinner spinner_factor_size_node = new JSpinner(factor_size_node);
		if (Util.isUnix())
			spinner_factor_size_node.setBounds(105, 110, 60, 20);
		else
			spinner_factor_size_node.setBounds(95, 110, 60, 20);
		JComponent comp_factor_size_node = spinner_factor_size_node.getEditor();
		JFormattedTextField field_factor_size_node = (JFormattedTextField) comp_factor_size_node.getComponent(0);
		DefaultFormatter formatter_factor_size_node = (DefaultFormatter) field_factor_size_node.getFormatter();
		formatter_factor_size_node.setCommitsOnValidEdit(true);
		spinner_factor_size_node.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				Util.node_label_factor_size = (double) spinner_factor_size_node.getValue();
			}
		});
		spinner_factor_size_node.setToolTipText(
				"Scaling factor to the protein length. It ranges between 0 (small) and 1 (original length).");
		protein_panel.add(spinner_factor_size_node);

		Icon iconBtn = new ImageIcon(getClass().getResource("/images/browse_Icon.png"));
		proteinDomainServerButton = new JButton(iconBtn);
		if (Util.isWindows())
			proteinDomainServerButton.setBounds(228, 160, 30, 30);
		else
			proteinDomainServerButton.setBounds(253, 160, 30, 30);
		proteinDomainServerButton.setEnabled(true);
		proteinDomainServerButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

		proteinDomainServerButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				taskMonitor.setTitle("XL interactions");
				proteinDomainServerButton.setEnabled(false);
				try {

					textLabel_status_result.setText("Accessing Pfam database...");
					taskMonitor.showMessage(TaskMonitor.Level.INFO, "Accessing Pfam database...");
					pfamThread = new Thread() {
						public void run() {
							taskMonitor.setTitle("XL interactions");

							textLabel_status_result.setText("Getting protein domains...");
							taskMonitor.showMessage(TaskMonitor.Level.INFO, "Getting protein domains...");
							myProtein.domains = Util.getProteinDomainsFromServer(myCurrentRow, taskMonitor);
							taskMonitor.setProgress(0.4);
							if (myProtein.domains.size() > 0)
								textLabel_status_result.setText("Done!");
							else {
								textLabel_status_result.setText("WARNING: Check Task History.");
								taskMonitor.showMessage(TaskMonitor.Level.WARN, "No protein domain has been found for '"
										+ myNetwork.getDefaultNodeTable().getRow(node.getSUID()).getRaw(CyNetwork.NAME)
										+ "'.");
							}

							Object[][] data = null;
							if (myProtein.domains.size() > 0)
								data = new Object[myProtein.domains.size()][5];
							else
								data = new Object[1][5];
							domainTableDataModel.setDataVector(data, columnNamesDomainTable);

							int countPtnDomain = 0;
							for (ProteinDomain domain : myProtein.domains) {
								domainTableDataModel.setValueAt(domain.name, countPtnDomain, 0);
								domainTableDataModel.setValueAt(domain.startId, countPtnDomain, 1);
								domainTableDataModel.setValueAt(domain.endId, countPtnDomain, 2);
								domainTableDataModel.setValueAt(domain.eValue, countPtnDomain, 3);
								countPtnDomain++;
							}

							if (myProtein.domains.size() > 0)
								setTableProperties(myProtein.domains.size(), 0);
							else
								setTableProperties(1, 0);
							proteinDomainServerButton.setEnabled(true);
						}
					};

					pfamThread.start();

				} catch (Exception exception) {
				}
			}
		});
		mainPanel.add(proteinDomainServerButton);

		Icon iconPyMOLBtn = new ImageIcon(getClass().getResource("/images/pyMOL_logo.png"));
		pyMOLButton = new JButton(iconPyMOLBtn);
		if (Util.isWindows())
			pyMOLButton.setBounds(455, 160, 30, 30);
		else
			pyMOLButton.setBounds(480, 160, 30, 30);

		pyMOLButton.setEnabled(true);
		pyMOLButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
		pyMOLButton.setToolTipText("Open PyMOL");

		pyMOLButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				taskMonitor.setTitle("Visualize protein structure");
				pyMOLButton.setEnabled(false);

				try {

					pyMOLThread = new Thread() {
						public void run() {

							String msgINFO = "";

							if (myProtein.intraLinks.size() == 0) {
								msgINFO = "There is no myProtein.intraLinks";
								textLabel_status_result.setText(msgINFO + ".");
								taskMonitor.showMessage(TaskMonitor.Level.WARN,
										msgINFO + " to protein: " + (String) myCurrentRow.getRaw(CyNetwork.NAME));

								if (pyMOLButton != null)
									pyMOLButton.setEnabled(true);
								return;
							}

							if (textLabel_status_result != null)
								textLabel_status_result.setText("Getting PDB information from Uniprot...");
							taskMonitor.showMessage(TaskMonitor.Level.INFO, "Getting PDB information from Uniprot...");

							Protein ptn = Util.getPDBidFromUniprot(myCurrentRow, taskMonitor);
							List<PDB> pdbIds = ptn.pdbIds;
							if (pdbIds.size() > 0) {
								PDB pdbID = pdbIds.get(0);

								if (pdbIds.size() > 1) {

									// Open a window to select only one PDB
									getPDBInformation(pdbIds, msgINFO, taskMonitor, ptn, null, true, "", false, false,
											(String) myCurrentRow.getRaw(CyNetwork.NAME), false);

									try {
										pyMOLThread.join();
									} catch (InterruptedException e) {
										e.printStackTrace();
									}
								}

								try {
									processPDBFile(msgINFO, taskMonitor, pdbID, ptn);
								} catch (Exception e) {
									taskMonitor.showMessage(TaskMonitor.Level.ERROR, e.getMessage());
								}

							} else {

								textLabel_status_result.setText("ERROR: Check Task History.");
								taskMonitor.showMessage(TaskMonitor.Level.ERROR,
										"There is no PDB for the protein: " + ptn.proteinID);

								if (pyMOLButton != null)
									pyMOLButton.setEnabled(true);
								return;
							}

						}
					};

					pyMOLThread.start();

				} catch (Exception exception) {
				}
			}
		});
		mainPanel.add(pyMOLButton);

		// ######## DOMAIN TABLE #########

		Object[][] domainDataObj = new Object[1][5];
		// create table model with data
		domainTableDataModel = new DefaultTableModel(domainDataObj, columnNamesDomainTable) {
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
				return columnClassDomainTable[columnIndex];
			}
		};

		getProteinDomains(node); // Fill in myProteinDomains collection based on the main Map
									// (Util.proteinDomainsMap)

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
		domain_panel.setBorder(BorderFactory.createTitledBorder("Domains"));
		if (Util.isWindows())
			domain_panel.setBounds(10, 225, 503, 140);
		else
			domain_panel.setBounds(10, 215, 503, 140);
		domain_panel.setLayout(null);
		mainPanel.add(domain_panel);

		domain_panel.add(proteinDomainTableScrollPanel);

		if (myProtein != null && myProtein.domains != null && myProtein.domains.size() > 0) {
			domainDataObj = new Object[myProtein.domains.size()][5];
			domainTableDataModel.setDataVector(domainDataObj, columnNamesDomainTable);

			int countPtnDomain = 0;
			for (ProteinDomain domain : myProtein.domains) {
				domainTableDataModel.setValueAt(domain.name, countPtnDomain, 0);
				domainTableDataModel.setValueAt(domain.startId, countPtnDomain, 1);
				domainTableDataModel.setValueAt(domain.endId, countPtnDomain, 2);
				domainTableDataModel.setValueAt(domain.eValue, countPtnDomain, 3);
				Color color = domain.color;
				if (color != null) {

					String colorStr = color.getRed() + "#" + color.getGreen() + "#" + color.getBlue() + "#"
							+ color.getAlpha();
					domainTableDataModel.setValueAt(colorStr, countPtnDomain, 4);
				}
				countPtnDomain++;
			}
			setTableProperties(myProtein.domains.size(), 0);
		} else {
			setTableProperties(1, 0);
		}

		mainProteinDomainTable.addMouseListener(new java.awt.event.MouseAdapter() {
			@Override
			public void mouseClicked(java.awt.event.MouseEvent evt) {
				int viewRow = mainProteinDomainTable.rowAtPoint(evt.getPoint());
				int viewColumn = mainProteinDomainTable.columnAtPoint(evt.getPoint());
				if (viewColumn == 4) {
					String currentColor = (String) domainTableDataModel.getValueAt(viewRow, viewColumn);
					Color initialcolor = Color.RED;
					if (currentColor != null && !currentColor.equals("")) {
						String[] cols = currentColor.split("#");
						initialcolor = new Color(Integer.parseInt(cols[0]), Integer.parseInt(cols[1]),
								Integer.parseInt(cols[2]), Integer.parseInt(cols[3]));
					}

					Color color = JColorChooser.showDialog(null, "Select a color", initialcolor);
					String colorStr = color.getRed() + "#" + color.getGreen() + "#" + color.getBlue() + "#"
							+ color.getAlpha();
					domainTableDataModel.setValueAt(colorStr, viewRow, viewColumn);
				}

			}
		});

		Action insertLineToTableActionDomainTable = new AbstractAction("insertLineToTable") {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent evt) {
				domainTableDataModel.addRow(new Object[] { "" });
				Util.updateRowHeader(domainTableDataModel.getRowCount(), mainProteinDomainTable, rowHeaderDomainTable,
						proteinDomainTableScrollPanel);
				textLabel_status_result.setText("Domains table: row has been inserted.");
			}
		};

		KeyStroke keyStrokeInsertLineDomainTable = KeyStroke.getKeyStroke(KeyEvent.VK_I, InputEvent.CTRL_DOWN_MASK);
		mainProteinDomainTable.getActionMap().put("insertLineToTable", insertLineToTableActionDomainTable);
		mainProteinDomainTable.getInputMap(JTable.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
				.put(keyStrokeInsertLineDomainTable, "insertLineToTable");

		Action deleteLineToDomainTableAction = new AbstractAction("deleteLineToTable") {
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
						domainTableDataModel.removeRow(mainProteinDomainTable.getSelectedRow());
						Util.updateRowHeader(domainTableDataModel.getRowCount(), mainProteinDomainTable,
								rowHeaderDomainTable, proteinDomainTableScrollPanel);
						textLabel_status_result.setText("Domains table: row has been deleted.");
					}
				}

			}
		};

		KeyStroke keyStrokeDeleteLine = KeyStroke.getKeyStroke(KeyEvent.VK_D, InputEvent.CTRL_DOWN_MASK);
		mainProteinDomainTable.getActionMap().put("deleteLineToTable", deleteLineToDomainTableAction);
		mainProteinDomainTable.getInputMap(JTable.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(keyStrokeDeleteLine,
				"deleteLineToTable");

		// ######## END DOMAIN TABLE #########

		proteinPTMServerButton = new JButton(iconBtn);
		if (Util.isWindows())
			proteinPTMServerButton.setBounds(108, 368, 30, 30);
		else
			proteinPTMServerButton.setBounds(113, 355, 30, 30);
		proteinPTMServerButton.setEnabled(true);
		proteinPTMServerButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
		proteinPTMServerButton.setToolTipText("Connect to Uniprot server");

		proteinPTMServerButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				taskMonitor.setTitle("Looking for PTMs");
				proteinPTMServerButton.setEnabled(false);
				try {

					textLabel_status_result.setText("Accessing Uniprot database...");
					taskMonitor.showMessage(TaskMonitor.Level.INFO, "Accessing Uniprot database...");
					uniprotThread = new Thread() {
						public void run() {
							taskMonitor.setTitle("Looking for PTMs");

							textLabel_status_result.setText("Getting protein information...");
							taskMonitor.showMessage(TaskMonitor.Level.INFO, "Getting protein information...");
							myPTMs = Util.getPTMs(myCurrentRow, taskMonitor);

							taskMonitor.setProgress(0.4);
							if (myPTMs.size() > 0)
								textLabel_status_result.setText("Done!");
							else {
								textLabel_status_result.setText("WARNING: Check Task History.");
								taskMonitor.showMessage(TaskMonitor.Level.WARN, "No ptm has been found for '"
										+ myNetwork.getDefaultNodeTable().getRow(node.getSUID()).getRaw(CyNetwork.NAME)
										+ "'.");
							}

							Object[][] data = null;
							if (myPTMs.size() > 0)
								data = new Object[myPTMs.size()][3];
							else
								data = new Object[1][3];
							ptmTableDataModel.setDataVector(data, columnNamesPTMTable);

							int countPtnDomain = 0;
							for (PTM domain : myPTMs) {
								ptmTableDataModel.setValueAt(domain.name, countPtnDomain, 0);
								ptmTableDataModel.setValueAt(domain.residue, countPtnDomain, 1);
								ptmTableDataModel.setValueAt(domain.position, countPtnDomain, 2);
								countPtnDomain++;
							}

							if (myPTMs.size() > 0)
								setTableProperties(myPTMs.size(), 1);
							else
								setTableProperties(1, 1);
							proteinPTMServerButton.setEnabled(true);
						}
					};

					uniprotThread.start();

				} catch (Exception exception) {
				}
			}
		});
		mainPanel.add(proteinPTMServerButton);

		// ######## PTM TABLE ################

		Object[][] ptmDataObj = new Object[1][3];
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

			@Override
			public void setValueAt(Object data, int row, int column) {
				if (column == 1)
					super.setValueAt(data.toString().toUpperCase().charAt(0), row, column);
				else
					super.setValueAt(data, row, column);
			}

		};

		getPTMs(node); // Fill in myPTMs collection based on the main Map
						// (Util.ptmsMap)

		mainProteinPTMTable = new JTable(ptmTableDataModel);
		// Create the scroll pane and add the table to it.
		proteinPTMTableScrollPanel = new JScrollPane();

		if (Util.isWindows())
			proteinPTMTableScrollPanel.setBounds(8, 20, 485, 90);
		else
			proteinPTMTableScrollPanel.setBounds(8, 20, 485, 90);
		proteinPTMTableScrollPanel.setViewportView(mainProteinPTMTable);
		proteinPTMTableScrollPanel.setRowHeaderView(rowHeaderPTMTable);

		ptm_panel = new JPanel();
		ptm_panel.setBorder(BorderFactory.createTitledBorder("Post-translational modifications"));
		if (Util.isWindows())
			ptm_panel.setBounds(10, 405, 503, 120);
		else
			ptm_panel.setBounds(10, 395, 503, 120);
		ptm_panel.setLayout(null);
		mainPanel.add(ptm_panel);

		ptm_panel.add(proteinPTMTableScrollPanel);

		if (myPTMs != null && myPTMs.size() > 0) {
			ptmDataObj = new Object[myPTMs.size()][5];
			ptmTableDataModel.setDataVector(ptmDataObj, columnNamesPTMTable);

			int countPTM = 0;
			for (PTM ptm : myPTMs) {
				ptmTableDataModel.setValueAt(ptm.name, countPTM, 0);
				ptmTableDataModel.setValueAt(ptm.residue, countPTM, 1);
				ptmTableDataModel.setValueAt(ptm.position, countPTM, 2);
				countPTM++;
			}
			setTableProperties(myPTMs.size(), 1);
		} else {
			setTableProperties(1, 1);
		}

		Action insertLineToTableActionPTMTable = new AbstractAction("insertLineToPTMTable") {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent evt) {
				ptmTableDataModel.addRow(new Object[] { "" });
				Util.updateRowHeader(ptmTableDataModel.getRowCount(), mainProteinPTMTable, rowHeaderPTMTable,
						proteinPTMTableScrollPanel);
				textLabel_status_result.setText("PTM table: row has been inserted.");
			}
		};

		KeyStroke keyStrokeInsertLinePTMTable = KeyStroke.getKeyStroke(KeyEvent.VK_I, InputEvent.CTRL_DOWN_MASK);
		mainProteinPTMTable.getActionMap().put("insertLineToPTMTable", insertLineToTableActionPTMTable);
		mainProteinPTMTable.getInputMap(JTable.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(keyStrokeInsertLinePTMTable,
				"insertLineToPTMTable");

		Action deleteLineToPTMTableAction = new AbstractAction("deleteLineToPTMTable") {
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
						textLabel_status_result.setText("PTM table: row has been deleted.");
					}
				}
			}
		};

		KeyStroke keyStrokeDeleteLinePTM = KeyStroke.getKeyStroke(KeyEvent.VK_D, InputEvent.CTRL_DOWN_MASK);
		mainProteinPTMTable.getActionMap().put("deleteLineToPTMTable", deleteLineToPTMTableAction);
		mainProteinPTMTable.getInputMap(JTable.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(keyStrokeDeleteLinePTM,
				"deleteLineToPTMTable");

		// ######## END PTM TABLE ############

		Icon iconBtnOk = new ImageIcon(getClass().getResource("/images/okBtn.png"));
		okButton = new JButton(iconBtnOk);
		okButton.setText("OK");

		if (Util.isWindows()) {
			okButton.setBounds(30, 535, 220, 25);
		} else if (Util.isMac()) {
			okButton.setBounds(30, 520, 220, 25);
		} else {
			okButton.setBounds(30, 525, 220, 25);
		}

		okButton.setEnabled(true);

		okButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent ae) {

				okButton.setEnabled(false);
				proteinDomainServerButton.setEnabled(false);

				isPlotDone = false;
				ProcessProteinLocationTask.isPlotDone = false;

				if (netView == null) {
					netView = cyApplicationManager.getCurrentNetworkView();
				}

				nodeView = netView.getNodeView(node);

				applyLayoutThread = new Thread() {

					public void run() {

						textLabel_status_result.setText("Setting node styles...");
						taskMonitor.showMessage(TaskMonitor.Level.INFO, "Setting node styles...");

						Util.node_label_factor_size = 1;
						Util.setNodeStyles(myNetwork, node, netView, style,
								Util.getProteinSequenceFromUniprot(myCurrentRow));
						taskMonitor.setProgress(0.2);

						textLabel_status_result.setText("Getting protein domains from table...");
						try {
							getNodeDomainsFromTable();
							taskMonitor.setProgress(0.4);
						} catch (Exception e2) {
							textLabel_status_result.setText(e2.getMessage());
							taskMonitor.showMessage(TaskMonitor.Level.WARN, e2.getMessage());
						}

						if (Util.showPTMs) {
							try {
								getNodePTMsFromTable();
								taskMonitor.setProgress(0.4);
							} catch (Exception e2) {
								textLabel_status_result.setText(e2.getMessage());
								taskMonitor.showMessage(TaskMonitor.Level.WARN, e2.getMessage());
							}
						}

						textLabel_status_result.setText("Setting protein domains to node...");
						taskMonitor.showMessage(TaskMonitor.Level.INFO, "Setting protein domains to node...");
						setNodeDomainColors(taskMonitor);
						taskMonitor.setProgress(0.75);

						update_protein_domain_table(taskMonitor, myProtein.domains);
						taskMonitor.setProgress(0.85);

						textLabel_status_result.setText("Setting ptms to node...");
						taskMonitor.showMessage(TaskMonitor.Level.INFO, "Setting ptms to node...");

						if (Util.showPTMs) {
							Util.setNodePTMs(taskMonitor, myNetwork, netView, node, style, handleFactory, bendFactory,
									lexicon, myPTMs, false);
							taskMonitor.setProgress(0.90);

							update_ptms_table(taskMonitor, myPTMs);
							taskMonitor.setProgress(0.92);
						}

						if (Util.showMonolinkedPeptides) {
							taskMonitor.showMessage(TaskMonitor.Level.INFO, "Getting monolinks...");
							getMonolinks(node);

							taskMonitor.showMessage(TaskMonitor.Level.INFO, "Setting monolinks to node...");
							Util.setMonolinksToNode(taskMonitor, myNetwork, netView, node, style, handleFactory,
									bendFactory, lexicon, myMonolinks, getPtnSequenceOfMonolinks(node));
							taskMonitor.setProgress(0.93);
						}

						textLabel_status_result.setText("Setting styles to the edges...");
						taskMonitor.showMessage(TaskMonitor.Level.INFO, "Setting styles to the edges...");
						isPlotDone = false;
						isPlotDone = Util.addOrUpdateEdgesToNetwork(myNetwork, node, style, netView, nodeView,
								handleFactory, bendFactory, lexicon, Util.getProteinLengthScalingFactor(), myProtein,
								taskMonitor, textLabel_status_result);
						taskMonitor.setProgress(0.95);

						Util.node_label_factor_size = (double) spinner_factor_size_node.getValue();
						resizeProtein(taskMonitor);

						// Apply the change to the view
						style.apply(netView);
						netView.updateView();
						taskMonitor.setProgress(1.0);
						textLabel_status_result.setText("Done!");

						okButton.setEnabled(true);
						proteinDomainServerButton.setEnabled(true);
						Util.updateMapNodesPosition(node, nodeView);

						mainFrame.dispose();
					}
				};

				applyLayoutThread.start();

			}
		});
		mainPanel.add(okButton);

		Icon iconBtnCancel = new ImageIcon(getClass().getResource("/images/cancelBtn.png"));
		JButton cancelButton = new JButton(iconBtnCancel);
		cancelButton.setText("Cancel");

		if (Util.isWindows()) {
			cancelButton.setBounds(265, 535, 220, 25);
		} else if (Util.isMac()) {
			cancelButton.setBounds(265, 520, 220, 25);
		} else {
			cancelButton.setBounds(265, 525, 220, 25);
		}

		cancelButton.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if (cancelProcess()) {
					mainFrame.dispose();
				}
			}
		});
		mainPanel.add(cancelButton);

		Icon iconBtnRestoreStyle = new ImageIcon(getClass().getResource("/images/restore.png"));
		JButton restoreStyleButton = new JButton(iconBtnRestoreStyle);
		restoreStyleButton.setText("Restore style");

		if (Util.isWindows()) {
			restoreStyleButton.setBounds(390, 195, 120, 25);
		} else {
			restoreStyleButton.setBounds(390, 190, 120, 25);
		}

		restoreStyleButton.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				taskMonitor.setProgress(0.6);
				restoreDefaultStyle(taskMonitor);
				taskMonitor.setProgress(1.0);
				mainFrame.dispose();
			}
		});
		mainPanel.add(restoreStyleButton);
	}

	/**
	 * Method responsible for creating a window to provide to the user a list with
	 * all PBDs. Only one of them needs to be selected.
	 * 
	 * @param pdbIds              pdb IDs
	 * @param msgINFO             output info
	 * @param taskMonitor         task monitor
	 * @param ptnSource           protein source
	 * @param ptnTarget           protein target
	 * @param processPDBfile      if true, process the pdbfile with an unknown
	 *                            chain, otherwise process pdbfile with a specific
	 *                            chain
	 * @param pdbFile             pdb file name
	 * @param HasMoreThanOneChain indicates if there is more than one chain
	 * @param isFromEdgeAction    indicates if the method is called from edge action
	 * @param nodeName            node name
	 */
	public void getPDBInformation(List<PDB> pdbIds, String msgINFO, TaskMonitor taskMonitor, Protein ptnSource,
			Protein ptnTarget, boolean processPDBfile, String pdbFile, boolean HasMoreThanOneChain,
			boolean isFromEdgeAction, String nodeName, boolean processTarget) {

		JFrameWithoutMaxAndMinButton pdbFrame = new JFrameWithoutMaxAndMinButton(new JFrame(), "P2Location", -1);

		pdbFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		Dimension appSize = null;
		if (Util.isWindows()) {
			appSize = new Dimension(300, 345);
		} else if (Util.isMac()) {
			appSize = new Dimension(280, 325);
		} else {
			appSize = new Dimension(280, 345);
		}
		pdbFrame.setSize(appSize);
		pdbFrame.setResizable(false);

		JPanel pdbPanel = new JPanel();

		boolean isPDBInformation = false;

		if (!isFromEdgeAction) {
			if (processPDBfile)
				isPDBInformation = true;

			else
				isPDBInformation = false;
		} else {
			if (processTarget) {
				if (pdbIds.get(0).entry.isBlank() || pdbIds.get(0).entry.isEmpty()) {
					isPDBInformation = false;
				} else {
					if (processPDBfile)
						isPDBInformation = true;
					else
						isPDBInformation = false;
				}
			} else {
				if (processPDBfile)
					isPDBInformation = true;
				else
					isPDBInformation = false;

			}
		}

		if (isPDBInformation)
			pdbPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "PDB Information"));
		else
			pdbPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Protein Chain"));

		pdbPanel.setLayout(null);
		pdbPanel.setBounds(20, 20, 280, 325);

		int offset_y = 10;

		JLabel textLabel_protein_title = new JLabel("Node name:");
		textLabel_protein_title.setFont(new java.awt.Font("Tahoma", Font.PLAIN, 12));
		textLabel_protein_title.setBounds(10, offset_y, 80, 40);
		pdbPanel.add(textLabel_protein_title);

		JLabel textLabel_protein_name = new JLabel();
		textLabel_protein_name.setFont(new java.awt.Font("Tahoma", Font.BOLD, 12));
		if (Util.isUnix())
			textLabel_protein_name.setBounds(95, offset_y, 80, 40);
		else
			textLabel_protein_name.setBounds(85, offset_y, 80, 40);

		String[] cols_nodeName = null;
		if (nodeName.contains("#")) {
			cols_nodeName = nodeName.split("#");
			textLabel_protein_name.setText(cols_nodeName[0]);
		} else {
			textLabel_protein_name.setText(nodeName);
		}
		pdbPanel.add(textLabel_protein_name);

		if (nodeName.contains("#")) {

			offset_y += 15;
			JLabel textLabel_protein_second_name = new JLabel();
			textLabel_protein_second_name.setText(cols_nodeName[1]);
			textLabel_protein_second_name.setFont(new java.awt.Font("Tahoma", Font.BOLD, 12));
			if (Util.isUnix()) {
				textLabel_protein_second_name.setBounds(95, offset_y, 80, 40);
				offset_y += 30;
			} else {
				textLabel_protein_second_name.setBounds(85, offset_y, 80, 40);
				offset_y += 20;
			}
			pdbPanel.add(textLabel_protein_second_name);

		} else {
			offset_y += 30;
		}

		JLabel textLabel_title = null;
		if (isPDBInformation) {
			textLabel_title = new JLabel("Select one item:");
			textLabel_title.setBounds(10, offset_y, 150, 40);
			textLabel_title.setFont(new java.awt.Font("Tahoma", Font.PLAIN, 12));
			pdbPanel.add(textLabel_title);
		} else {
			textLabel_title = new JLabel("No chain matched with the protein description.");
			textLabel_title.setBounds(10, offset_y, 400, 40);
			textLabel_title.setFont(new java.awt.Font("Tahoma", Font.ITALIC, 12));
			pdbPanel.add(textLabel_title);
			offset_y += 15;
			JLabel textLabel2_title = new JLabel("Please select one item:");
			textLabel2_title.setBounds(10, offset_y, 400, 40);
			textLabel2_title.setFont(new java.awt.Font("Tahoma", Font.PLAIN, 12));
			pdbPanel.add(textLabel2_title);
		}

		// create table model with data

		String[] columnPDBNames = null;
		final Class[] columnPDBClass;
		int numberClasses = 4;

		if (isPDBInformation) {
			columnPDBNames = new String[] { "PDB Entry", "Resolution", "Chain", "Positions" };
			columnPDBClass = new Class[] { String.class, String.class, String.class, String.class };
		} else {
			columnPDBNames = new String[] { "Chain" };
			columnPDBClass = new Class[] { String.class };
			numberClasses = 1;
		}

		Object[][] data = null;
		if (pdbIds.size() > 0)
			data = new Object[pdbIds.size()][numberClasses];
		else
			data = new Object[1][numberClasses];

		DefaultTableModel tableDataModel = new DefaultTableModel(data, columnPDBNames) {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
			}

			@Override
			public Class<?> getColumnClass(int columnIndex) {
				return columnPDBClass[columnIndex];
			}
		};

		tableDataModel.setDataVector(data, columnPDBNames);

		int countPtnDomain = 0;
		for (PDB pdb : pdbIds) {
			if (isPDBInformation) {
				tableDataModel.setValueAt(pdb.entry, countPtnDomain, 0);
				tableDataModel.setValueAt(pdb.resolution + " \u212B", countPtnDomain, 1);
				tableDataModel.setValueAt(pdb.chain, countPtnDomain, 2);
				tableDataModel.setValueAt(pdb.positions, countPtnDomain, 3);
			} else {
				tableDataModel.setValueAt(pdb.chain, countPtnDomain, 0);
			}
			countPtnDomain++;
		}

		if (nodeName.contains("#")) {
			offset_y += 35;
		} else {
			offset_y += 40;
		}

		JTable mainPdbTable = new JTable(tableDataModel);

		// Create the scroll pane and add the table to it.
		JScrollPane pdbTableScrollPanel = new JScrollPane();

		if (isPDBInformation) {
			if (Util.isWindows())
				pdbTableScrollPanel.setBounds(10, offset_y, 260, 180);
			else
				pdbTableScrollPanel.setBounds(10, offset_y, 260, 180);
		} else {
			if (Util.isWindows())
				pdbTableScrollPanel.setBounds(10, offset_y, 260, 165);
			else
				pdbTableScrollPanel.setBounds(10, offset_y, 260, 165);
		}
		pdbTableScrollPanel.setViewportView(mainPdbTable);
		pdbTableScrollPanel.setRowHeaderView(rowHeaderDomainTable);
		pdbPanel.add(pdbTableScrollPanel);

		if (mainPdbTable != null && isPDBInformation) {
			mainPdbTable.getColumnModel().getColumn(0).setPreferredWidth(80);
			mainPdbTable.getColumnModel().getColumn(1).setPreferredWidth(80);
			mainPdbTable.getColumnModel().getColumn(2).setPreferredWidth(50);
			mainPdbTable.getColumnModel().getColumn(3).setPreferredWidth(65);
			mainPdbTable.setFillsViewportHeight(true);
			mainPdbTable.setAutoCreateRowSorter(true);
		}
		mainPdbTable.setRowSelectionInterval(0, 0);

		Icon iconBtnOk = new ImageIcon(getClass().getResource("/images/okBtn.png"));
		JButton okButton = new JButton(iconBtnOk);
		okButton.setText("OK");

		okButton.setBounds(70, 270, 140, 25);
		okButton.setEnabled(true);
		okButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

		okButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent ae) {

				pdbFrame.dispose();

				int row = mainPdbTable.getSelectedRow();
				String value = tableDataModel.getValueAt(row, 0) != null ? tableDataModel.getValueAt(row, 0).toString()
						: "";

				try {
					if (!isFromEdgeAction) {

						if (processPDBfile) {
							PDB pdb = new PDB(value, "", "", "");
							processPDBFile(msgINFO, taskMonitor, pdb, ptnSource);
						} else
							processPDBorCiFfileWithSpecificChain(taskMonitor, pdbFile, ptnSource, HasMoreThanOneChain,
									value);

					} else {

						if (processPDBfile)
							MainSingleEdgeTask.processPDBFile(taskMonitor, value, ptnSource, ptnTarget, nodeName,
									processTarget, "");
						else
							MainSingleEdgeTask.processPDBorCIFfileWithSpecificChain(taskMonitor, ptnSource, ptnTarget,
									value);

					}
				} catch (Exception e) {
					taskMonitor.showMessage(Level.ERROR, e.getMessage());
					JOptionPane.showMessageDialog(null, e.getMessage(), "P2Location - Alert",
							JOptionPane.ERROR_MESSAGE);
				}

			}
		});
		pdbPanel.add(okButton);

		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		pdbFrame.setLocation((screenSize.width - appSize.width) / 2, (screenSize.height - appSize.height) / 2);

		// Display the window
		pdbFrame.add(pdbPanel);
		pdbFrame.setLocationRelativeTo(null);
		pdbFrame.setVisible(true);

	}

	/**
	 * Method responsible for creating pymol script (*.pml) when the user selected a
	 * specific protein chain
	 * 
	 * @param taskMonitor         task monitor
	 * @param pdbFile             pdb file
	 * @param ptn                 protein
	 * @param HasMoreThanOneChain has more than one protein chain
	 * @param proteinChain        protein chain
	 * @throws Exception
	 */
	private void processPDBorCiFfileWithSpecificChain(TaskMonitor taskMonitor, String pdbFile, Protein ptn,
			boolean HasMoreThanOneChain, String proteinChain) throws Exception {

		String msgINFO = "Creating tmp PyMOL script file...";
		if (textLabel_status_result != null)
			textLabel_status_result.setText(msgINFO);

		taskMonitor.showMessage(TaskMonitor.Level.INFO, msgINFO);

		String proteinSequenceFromPDBFile = "";

		if (pdbFile.endsWith("pdb"))
			proteinSequenceFromPDBFile = ProteinStructureManager.getProteinSequenceFromPDBFileWithSpecificChain(pdbFile,
					ptn, taskMonitor, proteinChain, false);
		else
			proteinSequenceFromPDBFile = ProteinStructureManager.getProteinSequenceFromCIFFileWithSpecificChain(pdbFile,
					ptn, taskMonitor, proteinChain, false);

		String tmpPyMOLScriptFile = ProteinStructureManager.createPyMOLScriptFile(ptn, myProtein.intraLinks,
				taskMonitor, pdbFile, proteinSequenceFromPDBFile, HasMoreThanOneChain, proteinChain);

		if (tmpPyMOLScriptFile.equals("ERROR")) {

			taskMonitor.showMessage(TaskMonitor.Level.ERROR, "Error creating PyMOL script file.");
			if (textLabel_status_result != null) {

				textLabel_status_result.setText("ERROR: Check Task History.");
				pyMOLButton.setEnabled(true);
				return;

			} else {

				throw new Exception("Error creating PyMOL script file.");

			}

		}

		ProteinStructureManager.executePyMOL(taskMonitor, tmpPyMOLScriptFile, textLabel_status_result);

		if (textLabel_status_result != null) {

			textLabel_status_result.setText("Done!");
			pyMOLButton.setEnabled(true);
		}

	}

	/**
	 * Method responsible for creating and processing PDB file
	 * 
	 * @param msgINFO     output info
	 * @param taskMonitor taskmonitor
	 * @param pdbID       pdb ID
	 * @param ptn         protein
	 * @throws Exception
	 */
	public void processPDBFile(String msgINFO, TaskMonitor taskMonitor, PDB pdbID, Protein ptn) throws Exception {

		msgINFO = "Creating tmp PDB file...";

		if (textLabel_status_result != null)
			textLabel_status_result.setText(msgINFO);
		taskMonitor.showMessage(TaskMonitor.Level.INFO, msgINFO);

		if (pdbID.resolution.equals("SMR")) {// It means that there is no PDB ID stored on
			// Uniprot, but there is a SWISS-MODEL

			String new_pdbID = Util.getPDBidOrURLFromSwissModel(pdbID.entry, ptn.checksum, taskMonitor);
			pdbID.entry = new_pdbID;

			if (new_pdbID.isBlank() || new_pdbID.isEmpty()) {

				if (textLabel_status_result != null) {

					textLabel_status_result.setText("ERROR: Check Task History.");
					pyMOLButton.setEnabled(true);
				}

				taskMonitor.showMessage(TaskMonitor.Level.ERROR, "There is no PDB for the protein: " + ptn.proteinID);
				return;
			}

		}

		String pdbFile = ProteinStructureManager.createPDBFile(pdbID.entry, taskMonitor);
		if (pdbFile.equals("ERROR")) {

			if (textLabel_status_result != null) {

				textLabel_status_result.setText("ERROR: Check Task History.");
				pyMOLButton.setEnabled(true);
			} else {
				JOptionPane.showMessageDialog(null, "Error creating PDB file.", "P2Location - Alert",
						JOptionPane.ERROR_MESSAGE);
			}

			taskMonitor.showMessage(TaskMonitor.Level.ERROR, "Error creating PDB file.");
			return;
		}

		msgINFO = "Creating tmp PyMOL script file...";
		if (textLabel_status_result != null)
			textLabel_status_result.setText(msgINFO);
		taskMonitor.showMessage(TaskMonitor.Level.INFO, msgINFO);

		// tmpPyMOLScriptFile[0-> PyMOL script file name]
		String[] tmpPyMOLScriptFile = ProteinStructureManager.createPyMOLScriptFileUnknowChain(ptn,
				myProtein.intraLinks, taskMonitor, pdbFile, pdbID.entry);

		if (tmpPyMOLScriptFile[0].equals("CHAINS")) {

			// tmpPyMOLScriptFile[0-> 'CHAINS'; 1-> HasMoreThanOneChain; 2-> chains:
			// separated by
			// '#']

			boolean HasMoreThanOneChain = tmpPyMOLScriptFile[1].equals("true");
			String[] protein_chains = tmpPyMOLScriptFile[2].replace("CHAINS:", "").split("#");

			List<String> protein_chainsList = Arrays.asList(protein_chains);
			List<PDB> PDBchains = new ArrayList<PDB>();
			for (String chainStr : protein_chainsList) {
				PDBchains.add(new PDB("", "", chainStr, ""));
			}
			if (PDBchains.size() > 1) {

				taskMonitor.showMessage(TaskMonitor.Level.INFO, "Select one chain...");
				// Open a window to select only one protein chain
				getPDBInformation(PDBchains, msgINFO, taskMonitor, ptn, null, false, pdbFile, HasMoreThanOneChain,
						false, (String) myCurrentRow.getRaw(CyNetwork.NAME), false);
			} else {
				// There is only one protein chain
				taskMonitor.showMessage(TaskMonitor.Level.INFO, "Processing PDB file...");
				processPDBorCiFfileWithSpecificChain(taskMonitor, pdbFile, ptn, HasMoreThanOneChain,
						protein_chainsList.get(0));
			}

		} else if (tmpPyMOLScriptFile[0].equals("ERROR")) {

			if (textLabel_status_result != null) {

				textLabel_status_result.setText("ERROR: Check Task History.");
				pyMOLButton.setEnabled(true);
			} else {
				JOptionPane.showMessageDialog(null, "Error creating PyMOL script file.", "P2Location - Alert",
						JOptionPane.ERROR_MESSAGE);
			}

			taskMonitor.showMessage(TaskMonitor.Level.ERROR, "Error creating PyMOL script file.");
			return;

		} else {

			ProteinStructureManager.executePyMOL(taskMonitor, tmpPyMOLScriptFile[0], textLabel_status_result);

			if (textLabel_status_result != null) {

				textLabel_status_result.setText("Done!");
				pyMOLButton.setEnabled(true);
			}

		}
	}

	public static boolean cancelProcess() {
		boolean concluedProcess = true;

		if (!okButton.isEnabled()) {
			int input = JOptionPane.showConfirmDialog(null,
					"Style has not been finished yet. Do you want to close this window?", "P2Location - Single Node",
					JOptionPane.INFORMATION_MESSAGE);
			// 0=yes, 1=no, 2=cancel
			if (input == 0) {
				isPlotDone = true;
				concluedProcess = true;
				Util.stopUpdateViewer = true;
				if (applyLayoutThread != null) {
					applyLayoutThread.interrupt();
				}
			} else {
				isPlotDone = false;
				concluedProcess = false;
				Util.stopUpdateViewer = false;
			}
		} else {
			concluedProcess = true;
			isPlotDone = true;
			Util.stopUpdateViewer = false;
		}

		if (concluedProcess) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Method responsible for restoring basic node styles
	 * 
	 * @param taskMonitor
	 */
	private void clearNodeStyle(final TaskMonitor taskMonitor) {

		taskMonitor.showMessage(TaskMonitor.Level.INFO, "Restoring node layout...");
		nodeView.clearValueLock(BasicVisualLexicon.NODE_WIDTH);
		nodeView.clearValueLock(BasicVisualLexicon.NODE_TRANSPARENCY);
		nodeView.clearValueLock(BasicVisualLexicon.NODE_PAINT);
		nodeView.clearValueLock(BasicVisualLexicon.NODE_FILL_COLOR);
		nodeView.clearValueLock(BasicVisualLexicon.NODE_LABEL_COLOR);
		nodeView.clearValueLock(BasicVisualLexicon.NODE_SELECTED_PAINT);
		nodeView.clearValueLock(BasicVisualLexicon.NODE_BORDER_WIDTH);
		nodeView.clearValueLock(BasicVisualLexicon.NODE_BORDER_PAINT);
		nodeView.clearValueLock(BasicVisualLexicon.NODE_HEIGHT);
		nodeView.clearValueLock(BasicVisualLexicon.NODE_SHAPE);
		nodeView.clearValueLock(BasicVisualLexicon.NODE_TOOLTIP);
		nodeView.clearValueLock(BasicVisualLexicon.NODE_LABEL_FONT_SIZE);

		// ######################### NODE_LABEL_POSITION ######################

		taskMonitor.showMessage(TaskMonitor.Level.INFO, "Restoring node label position...");

		// Try to get the label visual property by its ID
		VisualProperty<?> vp_label_position = lexicon.lookup(CyNode.class, Util.NODE_LABEL_POSITION);
		if (vp_label_position != null) {
			nodeView.clearValueLock(vp_label_position);
		}
		// ######################### NODE_LABEL_POSITION ######################

		// ######################### NODE_COLOR_LINEAR_GRADIENT ######################
		taskMonitor.showMessage(TaskMonitor.Level.INFO, "Clearing all colors of node domains...");

		VisualProperty<CyCustomGraphics2> vp_node_linear_gradient = (VisualProperty<CyCustomGraphics2>) lexicon
				.lookup(CyNode.class, "NODE_CUSTOMGRAPHICS_1");
		if (vp_node_linear_gradient != null) {
			nodeView.clearValueLock(vp_node_linear_gradient);
		}

		// ############################################################################

		this.hideNodeResidues();
	}

	/**
	 * Method responsible for restoring the layout of the selected node.
	 * 
	 * @param taskMonitor
	 */
	private void restoreDefaultStyle(final TaskMonitor taskMonitor) {

		isPlotDone = false;

		if (style == null) {
			return;
		}
		if (netView == null) {
			netView = cyApplicationManager.getCurrentNetworkView();
		}

		taskMonitor.showMessage(TaskMonitor.Level.INFO, "Getting protein...");
		nodeView = netView.getNodeView(node);

		this.clearNodeStyle(taskMonitor);
		UpdateViewListener.isNodeModified = false;
		Util.restoreEdgesStyle(taskMonitor, myNetwork, cyApplicationManager, netView, handleFactory, bendFactory, node);

		taskMonitor.showMessage(TaskMonitor.Level.INFO, "Removing monolinks...");
		this.hideMonolinks();

		// Apply the change to the view
		style.apply(netView);
		netView.updateView();

		taskMonitor.showMessage(TaskMonitor.Level.INFO, "Done!");

		isPlotDone = true;
	}

	/**
	 * Method responsible for hiding monolink nodes
	 */
	private void hideMonolinks() {

		if (myNetwork == null)
			return;

		String nodeName = (String) myCurrentRow.getRaw(CyNetwork.NAME);

		// Check if the node exists in the network
		Stream<CyRow> monolinksRows = myNetwork.getDefaultNodeTable().getAllRows().stream()
				.filter(new Predicate<CyRow>() {
					public boolean test(CyRow o) {
						return o.get(CyNetwork.NAME, String.class).contains("MONOLINK")
								&& o.get(CyNetwork.NAME, String.class).contains(nodeName);
					}
				});

		for (Iterator<CyRow> i = monolinksRows.iterator(); i.hasNext();) {

			CyRow _node_row = i.next();

			CyNode _node = myNetwork.getNode(Long.parseLong(_node_row.getRaw(CyIdentifiable.SUID).toString()));

			View<CyNode> monolinkNodeView = netView.getNodeView(_node);
			monolinkNodeView.setLockedValue(BasicVisualLexicon.NODE_VISIBLE, false);
		}

	}

	/**
	 * Method responsible for hiding nodes residues
	 */
	private void hideNodeResidues() {

		if (myNetwork == null)
			return;

		String nodeName = (String) myCurrentRow.getRaw(CyNetwork.NAME);

		// Check if the node exists in the network
		Stream<CyRow> residuesRows = myNetwork.getDefaultNodeTable().getAllRows().stream()
				.filter(new Predicate<CyRow>() {
					public boolean test(CyRow o) {
						return o.get(CyNetwork.NAME, String.class).contains("RESIDUE")
								&& o.get(CyNetwork.NAME, String.class).contains(nodeName);
					}
				});

		for (Iterator<CyRow> i = residuesRows.iterator(); i.hasNext();) {

			CyRow _node_row = i.next();

			CyNode _node = myNetwork.getNode(Long.parseLong(_node_row.getRaw(CyIdentifiable.SUID).toString()));

			View<CyNode> residueNodeView = netView.getNodeView(_node);
			residueNodeView.setLockedValue(BasicVisualLexicon.NODE_VISIBLE, false);
		}

	}

	/**
	 * Get ptms assigned on the Table
	 * 
	 * @throws Exception
	 */
	private void getNodePTMsFromTable() throws Exception {

		myPTMs = new ArrayList<PTM>();

		for (int row = 0; row < ptmTableDataModel.getRowCount(); row++) {

			String description = ptmTableDataModel.getValueAt(row, 0) != null
					? ptmTableDataModel.getValueAt(row, 0).toString()
					: "";

			char residue = ptmTableDataModel.getValueAt(row, 1) != null
					? ptmTableDataModel.getValueAt(row, 1).toString().charAt(0)
					: "".charAt(0);
			int position = ptmTableDataModel.getValueAt(row, 2) != null ? (int) ptmTableDataModel.getValueAt(row, 2)
					: 0;

			myPTMs.add(new PTM(description, residue, position));
		}

		if (myPTMs.size() == 0) {
			throw new Exception("WARNING: No ptm has been found.");
		}

	}

	/**
	 * Get all domains assigned on the Table
	 * 
	 * @throws Exception
	 */
	private void getNodeDomainsFromTable() throws Exception {

		if (myProtein.domains == null)
			myProtein.domains = new ArrayList<ProteinDomain>();

		for (int row = 0; row < domainTableDataModel.getRowCount(); row++) {

			String domain = domainTableDataModel.getValueAt(row, 0) != null
					? domainTableDataModel.getValueAt(row, 0).toString()
					: "";
			int startId = domainTableDataModel.getValueAt(row, 1) != null
					? (int) domainTableDataModel.getValueAt(row, 1)
					: 0;
			int endId = domainTableDataModel.getValueAt(row, 2) != null ? (int) domainTableDataModel.getValueAt(row, 2)
					: 0;

			Optional<ProteinDomain> isPtnDomainPresent = myProtein.domains.stream()
					.filter(value -> value.name.equals(domain) && value.startId == startId && value.endId == endId)
					.findFirst();

			if (domainTableDataModel.getValueAt(row, 4) != null
					&& !domainTableDataModel.getValueAt(row, 4).toString().equals("")) {
				String[] colorStr = domainTableDataModel.getValueAt(row, 4).toString().split("#");
				Color color = new Color(Integer.parseInt(colorStr[0]), Integer.parseInt(colorStr[1]),
						Integer.parseInt(colorStr[2]), 100);

				if (isPtnDomainPresent.isPresent()) {
					ProteinDomain _myProteinDomain = isPtnDomainPresent.get();
					_myProteinDomain.color = color;
				} else {
					myProtein.domains.add(new ProteinDomain(domain, startId, endId, color));
				}
			} else if (domain.equals("") && startId == 0 && endId == 0) {
				continue;
			} else {

				if (isPtnDomainPresent.isPresent()) {
					ProteinDomain _myProteinDomain = isPtnDomainPresent.get();
					_myProteinDomain.startId = startId;
					_myProteinDomain.endId = endId;
					_myProteinDomain.eValue = "";
				} else {
					myProtein.domains.add(new ProteinDomain(domain, startId, endId, ""));
				}

			}
		}

		if (myProtein.domains.size() == 0) {
			throw new Exception("WARNING: No domain has been found.");
		}
		Util.updateProteinDomainsColorMap(myProtein.domains);
	}

	/**
	 * Set all domains to a node
	 */
	private void setNodeDomainColors(final TaskMonitor taskMonitor) {
		// ######################### NODE_COLOR_LINEAR_GRADIENT ######################
		boolean hasDomain = false;
		StringBuilder sb_domains = new StringBuilder();
		VisualProperty<CyCustomGraphics2<?>> vp_node_linear_gradient = (VisualProperty<CyCustomGraphics2<?>>) lexicon
				.lookup(CyNode.class, "NODE_CUSTOMGRAPHICS_1");
		if (vp_node_linear_gradient != null) {

			Map<String, Object> chartProps = new HashMap<String, Object>();
			List<java.awt.Color> colors = new ArrayList<java.awt.Color>();
			List<Float> values = new ArrayList<Float>();
			values.add(0.0f);
			colors.add(new Color(255, 255, 255, 100));

			if (myProtein == null)
				return;

			Collections.sort(myProtein.domains);

			int countDomain = 1;
			for (ProteinDomain domain : myProtein.domains) {

				int startId = domain.startId;
				int endId = domain.endId;

				if (startId > Util.getProteinLength())
					continue;
				if (endId > Util.getProteinLength()) {
					taskMonitor.showMessage(TaskMonitor.Level.ERROR, "ERROR Domain: " + domain.name
							+ " - The position of the final residue is greater than the length of the protein.");
					endId = (int) Util.getProteinLength();
				}

				float initial_range = ((float) startId / Util.getProteinLength());
				float initial_range_white = initial_range - 0.0001f >= 0.0 ? initial_range - 0.0001f : initial_range;

				if (initial_range_white == 0) {
					values.add(initial_range_white);
					values.add(initial_range + 0.0001f);

				} else {
					values.add(initial_range_white);
					values.add(initial_range);
				}
				colors.add(new Color(255, 255, 255, 100));
				if (domain.color == null) {
					colors.add(Util.proteinDomainsColorMap.get(domain.name));
				} else {
					colors.add(domain.color);
				}

				float end_range = ((float) endId / Util.getProteinLength());
				float end_range_white = end_range + 0.0001f <= 1.0 ? end_range + 0.0001f : end_range;

				if (end_range_white == 1.0) {
					values.add(end_range - 0.0001f);

				} else {
					values.add(end_range);
				}

				if (domain.color == null) {
					colors.add(Util.proteinDomainsColorMap.get(domain.name));
				} else {
					colors.add(domain.color);
				}
				values.add(end_range_white);
				colors.add(new Color(255, 255, 255, 100));

				sb_domains.append(
						"<p>" + countDomain + ". <i>" + domain.name + " </i>[" + startId + " - " + endId + "]</p>");
				hasDomain = true;
				countDomain++;
			}
			values.add(1.0f);
			colors.add(new Color(255, 255, 255, 100));
			chartProps.put("cy_gradientFractions", values);
			chartProps.put("cy_gradientColors", colors);

			if (Util.isProtein_expansion_horizontal)
				chartProps.put("cy_angle", 0.0);
			else
				chartProps.put("cy_angle", 270.0);

			CyCustomGraphics2<?> customGraphics = vgFactory.getInstance(chartProps);
			if (vp_node_linear_gradient != null)
				nodeView.setLockedValue(vp_node_linear_gradient, customGraphics);
		}

		String protein_name = myNetwork.getDefaultNodeTable().getRow(node.getSUID()).getRaw(CyNetwork.NAME).toString();

		if (hasDomain) {
			if (myProtein.domains.size() > 1)
				nodeView.setLockedValue(BasicVisualLexicon.NODE_TOOLTIP,
						"<html><p><b>Protein:</b></p><p>" + protein_name + " [1 - " + (int) Util.getProteinLength()
								+ "]</p><br/><p><b>Domains:</i></p>" + sb_domains.toString() + "</html>");
			else
				nodeView.setLockedValue(BasicVisualLexicon.NODE_TOOLTIP,
						"<html><p><b>Protein:</b></p><p>" + protein_name + " [1 - " + (int) Util.getProteinLength()
								+ "]</p><br/><p><b>Domain:</i></p>" + sb_domains.toString() + "</html>");

			String network_name = myNetwork.toString();
			if (Util.proteinsMap.containsKey(network_name)) {

				List<Protein> all_proteins = Util.proteinsMap.get(network_name);
				Optional<Protein> isPtnPresent = all_proteins.stream()
						.filter(value -> value.gene.equals(myProtein.gene)).findFirst();
				if (isPtnPresent.isPresent()) {
					Protein ptn = isPtnPresent.get();
					ptn.checksum = myProtein.checksum;
					ptn.domains = myProtein.domains;
					ptn.fullName = myProtein.fullName;
					ptn.gene = myProtein.gene;
					ptn.interLinks = myProtein.interLinks;
					ptn.intraLinks = myProtein.intraLinks;
					ptn.pdbIds = myProtein.pdbIds;
					ptn.proteinID = myProtein.proteinID;
					ptn.reactionSites = myProtein.reactionSites;
					ptn.sequence = myProtein.sequence;
				}

			} else {// Network does not exists

				List<Protein> proteins = new ArrayList<Protein>();
				proteins.add(myProtein);
				Util.proteinsMap.put(network_name, proteins);
			}

		} else
			nodeView.setLockedValue(BasicVisualLexicon.NODE_TOOLTIP, "<html><p><b>Protein:</b></p><p>" + protein_name
					+ " [1 - " + (int) Util.getProteinLength() + "]</p></html>");
		// ############################### END ################################
	}

}