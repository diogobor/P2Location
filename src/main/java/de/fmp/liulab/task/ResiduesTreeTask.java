package de.fmp.liulab.task;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.CyTableUtil;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.view.model.VisualLexicon;
import org.cytoscape.view.presentation.customgraphics.CyCustomGraphics2Factory;
import org.cytoscape.view.presentation.property.values.BendFactory;
import org.cytoscape.view.presentation.property.values.HandleFactory;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;

import de.fmp.liulab.model.Protein;
import de.fmp.liulab.model.Residue;
import de.fmp.liulab.utils.Util;

public class ResiduesTreeTask extends AbstractTask implements ActionListener {

	private CyApplicationManager cyApplicationManager;
	private CyNetworkManager networkManager;
	private CyNetwork myNetwork;
	private CyNetworkFactory myNetFactory;
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
	private boolean IsCommandLine;
	public static CyNode node;

	private Residue myResidue;

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
	public ResiduesTreeTask(CyApplicationManager cyApplicationManager, CyNetworkFactory netFactory,
			CyNetworkManager networkManager, final VisualMappingManager vmmServiceRef,
			CyCustomGraphics2Factory<?> vgFactory, BendFactory bendFactory, HandleFactory handleFactory,
			boolean forcedWindowOpen, boolean isCommandLine) {

		this.cyApplicationManager = cyApplicationManager;
		this.netView = cyApplicationManager.getCurrentNetworkView();
		this.myNetwork = cyApplicationManager.getCurrentNetwork();
		this.networkManager = networkManager;
		this.myNetFactory = netFactory;
		this.vgFactory = vgFactory;
		style = vmmServiceRef.getCurrentVisualStyle();
		// Get the current Visual Lexicon
		lexicon = cyApplicationManager.getCurrentRenderingEngine().getVisualLexicon();
		this.bendFactory = bendFactory;
		this.handleFactory = handleFactory;
		this.forcedWindowOpen = forcedWindowOpen;
		this.IsCommandLine = isCommandLine;

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

			taskMonitor.showMessage(TaskMonitor.Level.ERROR,
					"More than one node has been selected.\nPlease select only one.");
			throw new Exception("More than one node has been selected.\nPlease select only one.");

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
		createNetwork();
	}

	private void createNetwork() {
		CyNetwork myNetwork = myNetFactory.createNetwork();
		CyNode node1 = myNetwork.addNode();
		CyNode node2 = myNetwork.addNode();
		CyEdge edge1 = myNetwork.addEdge(node1, node2, false);

		myNetwork.getRow(node1).set(CyNetwork.NAME, "Node1");
		myNetwork.getRow(node2).set(CyNetwork.NAME, "Node2");
		myNetwork.getRow(edge1).set(CyNetwork.NAME, "Edge2");
		myNetwork.getRow(myNetwork).set(CyNetwork.NAME, "NetworkName");

		// Create our new columns
		CyTable nodeTable = myNetwork.getDefaultNodeTable();
		// CyTable nodeTable = myNetwork.getTable(CyNode.class,
		// CyNetwork.DEFAULT_ATTRS);
		if (nodeTable.getColumn("Hello") == null) {
			nodeTable.createListColumn("Hello", String.class, false);
		}
		if (nodeTable.getColumn("World") == null) {
			nodeTable.createColumn("World", Double.class, false);
		}

		List<String> list1 = new ArrayList<String>();
		list1.add("One1");
		list1.add("One2");
		list1.add("One3");
		// Now, add the data
//		myNetwork.getRow(node1).set("Hello", Arrays.asList("One", "Two", "Three"));
		myNetwork.getRow(node1).set("Hello", Arrays.asList(list1.toArray()));
		// nodeTable.getRow(node1.getSUID()).set("Hello",Arrays.asList("One", "Two",
		// "Three"));
		myNetwork.getRow(node2).set("Hello", Arrays.asList("Four", "Five", "Six"));
		// nodeTable.getRow(node2.getSUID()).set("Hello",Arrays.asList("One", "Two",
		// "Three"));

		myNetwork.getRow(node1).set("World", 1.0);
		myNetwork.getRow(node2).set("World", 4.0);
		networkManager.addNetwork(myNetwork);

	}

	/**
	 * Get all information of a node
	 * 
	 * @throws Exception
	 */
	public void getNodeInformation(TaskMonitor taskMonitor) throws Exception {

		// ##### GET THE SELECTED NODE - ONLY ONE IS POSSIBLE TO APPLY CHANGES ######

		myCurrentRow = myNetwork.getRow(node);

		String pattern = "(RESIDUE)(\\d+)( \\[Source:)( \\w+)( \\()(\\d+)(\\)(\\]))";
		String nodeName = (String) myCurrentRow.getRaw(CyNetwork.NAME);
		if (!nodeName.matches(pattern))
			return;

		String protein_name = nodeName.replaceAll(pattern, "$4").trim();
		String res_pos = nodeName.replaceAll(pattern, "$6").trim();

		Protein protein = Util.getProtein(myNetwork, protein_name);
		myResidue = protein.reactionSites.stream().filter(value -> value.position == Integer.parseInt(res_pos))
				.findFirst().get();

		nodeView = netView.getNodeView(node);

		/**
		 * Modify node style
		 */
		Util.setProteinLength(protein.sequence.length());

		isCurrentNode_modified = Util.IsNodeModified(myNetwork, netView, node);
		Util.node_label_factor_size = 1.0;
		Util.isProtein_expansion_horizontal = true;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void run(TaskMonitor taskMonitor) throws Exception {

		checkSingleOrMultipleSelectedNodes(taskMonitor);

	}

}
