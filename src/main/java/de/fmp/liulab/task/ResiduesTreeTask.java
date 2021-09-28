package de.fmp.liulab.task;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.model.View;
import org.cytoscape.view.model.VisualLexicon;
import org.cytoscape.view.model.VisualProperty;
import org.cytoscape.view.presentation.customgraphics.CyCustomGraphics2Factory;
import org.cytoscape.view.presentation.property.ArrowShapeVisualProperty;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.presentation.property.LineTypeVisualProperty;
import org.cytoscape.view.presentation.property.values.Bend;
import org.cytoscape.view.presentation.property.values.BendFactory;
import org.cytoscape.view.presentation.property.values.Handle;
import org.cytoscape.view.presentation.property.values.HandleFactory;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;

import de.fmp.liulab.model.CrossLink;
import de.fmp.liulab.model.Protein;
import de.fmp.liulab.model.Residue;
import de.fmp.liulab.utils.Util;

public class ResiduesTreeTask extends AbstractTask implements ActionListener {

	private CyApplicationManager cyApplicationManager;
	private CyNetworkManager networkManager;
	private CyNetwork myNetwork;
	private CyNetworkFactory myNetFactory;
	private CyNetworkViewManager viewManager;
	private CyNetworkViewFactory viewFactory;
	private CyNetworkView netView;
	private CyCustomGraphics2Factory vgFactory;
	private HandleFactory handleFactory;
	private BendFactory bendFactory;

	private boolean forcedWindowOpen;
	public static VisualStyle style;
	public static VisualLexicon lexicon;

	private View<CyNode> nodeView;
	public CyRow myCurrentRow;
	private List<CyNode> nodes;
	private boolean isCurrentNode_modified = false;
	private boolean IsCommandLine;
	public static CyNode node;

	private Residue myResidue;
	private CyNode highlight_node;

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
			CyNetworkManager networkManager, CyNetworkViewManager viewManager, CyNetworkViewFactory viewFactory,
			final VisualMappingManager vmmServiceRef, CyCustomGraphics2Factory<?> vgFactory, BendFactory bendFactory,
			HandleFactory handleFactory, boolean forcedWindowOpen, boolean isCommandLine) {

		this.cyApplicationManager = cyApplicationManager;
		this.netView = cyApplicationManager.getCurrentNetworkView();
		this.myNetwork = cyApplicationManager.getCurrentNetwork();
		this.networkManager = networkManager;
		this.myNetFactory = netFactory;
		this.viewManager = viewManager;
		this.viewFactory = viewFactory;
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

	/**
	 * Method responsible for creating network
	 */
	private void createNetwork() {

		if (myNetFactory == null || myResidue == null)
			return;

		CyNetwork myNetwork = myNetFactory.createNetwork();

		createColumnInToTheTable(myNetwork, Util.PROTEIN_LENGTH_A);
		createColumnInToTheTable(myNetwork, Util.PROTEIN_LENGTH_B);
		createColumnInToTheTable(myNetwork, Util.PROTEIN_A);
		createColumnInToTheTable(myNetwork, Util.PROTEIN_B);

		// The last residue will be the target residue
		List<Residue> dependent_residues = myResidue.history_residues;
		if (dependent_residues == null) {
			dependent_residues = new ArrayList<Residue>();
			dependent_residues.add(myResidue);
		}

		if (dependent_residues.size() > 0 && dependent_residues.get(0).conflicted_residue == null)
			// Now the target residue is the first element of the list
			Collections.reverse(dependent_residues);

		int count_residue = 1;
		for (Residue residue : dependent_residues) {
			CyNode new_node = myNetwork.addNode();

			// RESIDUE1 [Source: PTN (POSITION)]
			String node_name = "Residue" + count_residue + " [Source: " + residue.protein.gene + " (" + residue.position
					+ ")]";
			myNetwork.getRow(new_node).set(CyNetwork.NAME, node_name);
			if (count_residue == 1)
				highlight_node = new_node;

			myNetwork.getRow(new_node).set(Util.PROTEIN_LENGTH_A, Integer.toString(residue.protein.sequence.length()));
			myNetwork.getRow(new_node).set(Util.PROTEIN_LENGTH_B, Integer.toString(residue.protein.sequence.length()));
			myNetwork.getRow(new_node).set(Util.PROTEIN_A, "sp|" + residue.protein.proteinID + "|NO");
			myNetwork.getRow(new_node).set(Util.PROTEIN_B, "sp|" + residue.protein.proteinID + "|NO");
			count_residue++;
		}

		// Add conflicted_residue
		CyNode conflicted_node = myNetwork.addNode();

		// RESIDUE1 [Source: PTN (POSITION)]
		String conflicted_node_name = "Residue" + count_residue + " [Source: "
				+ myResidue.conflicted_residue.protein.gene + " (" + myResidue.conflicted_residue.position + ")]";
		myNetwork.getRow(conflicted_node).set(CyNetwork.NAME, conflicted_node_name);
		myNetwork.getRow(conflicted_node).set(Util.PROTEIN_LENGTH_A,
				Integer.toString(myResidue.conflicted_residue.protein.sequence.length()));
		myNetwork.getRow(conflicted_node).set(Util.PROTEIN_LENGTH_B,
				Integer.toString(myResidue.conflicted_residue.protein.sequence.length()));
		myNetwork.getRow(conflicted_node).set(Util.PROTEIN_A,
				"sp|" + myResidue.conflicted_residue.protein.proteinID + "|NO");
		myNetwork.getRow(conflicted_node).set(Util.PROTEIN_B,
				"sp|" + myResidue.conflicted_residue.protein.proteinID + "|NO");

		// Add conflicted_edge
		String conflicted_edge_name = "RESIDUE" + count_residue + " - RESIDUE1";

		String first_node_name = "Residue1 [Source: " + dependent_residues.get(0).protein.gene + " ("
				+ dependent_residues.get(0).position + ")]";
		CyNode first_node = Util.getNode(myNetwork, first_node_name);

		CyEdge conflicted_edge = myNetwork.addEdge(conflicted_node, first_node, false);
		myNetwork.getRow(conflicted_edge).set(CyNetwork.NAME, conflicted_edge_name);

		// Add edges
		if (dependent_residues.size() > 1) {
			for (int i = 0; i < dependent_residues.size(); i += 2) {
				// RESIDUE1 [Source: PTN (POSITION)]
				String node_name = "Residue" + (i + 1) + " [Source: " + dependent_residues.get(i).protein.gene + " ("
						+ dependent_residues.get(i).position + ")]";
				CyNode node1 = Util.getNode(myNetwork, node_name);

				String node_name_2 = "Residue" + (i + 2) + " [Source: " + dependent_residues.get(i + 1).protein.gene
						+ " (" + dependent_residues.get(i + 1).position + ")]";
				CyNode node2 = Util.getNode(myNetwork, node_name_2);
				CyEdge edge = myNetwork.addEdge(node1, node2, false);

				String edge_name = "RESIDUE" + (i + 1) + " - RESIDUE" + (i + 2);
				myNetwork.getRow(edge).set(CyNetwork.NAME, edge_name);
			}
		}

		myNetwork.getRow(myNetwork).set(CyNetwork.NAME, "RESIDUE_TREE - Protein: " + myResidue.protein.gene + " - "
				+ myResidue.aminoacid + "[" + myResidue.position + "]");

		networkManager.addNetwork(myNetwork);

		createNetworkView(myNetwork);

	}

	/**
	 * Method responsible for getting intralinks
	 * 
	 * @param residue current residue
	 * @return list of XLs
	 */
	private List<CrossLink> getInterestedIntraLinks(Residue residue) {

		List<CrossLink> intralinks = residue.protein.intraLinks.stream()
				.filter(value -> value.pos_site_a == residue.position || value.pos_site_b == residue.position)
				.collect(Collectors.toList());
		return intralinks;
	}

	/**
	 * Method responsible for getting interlinks
	 * 
	 * @param residue current residue
	 * @return list of XLs
	 */
	private List<CrossLink> getInterestedInterLinks(Residue residue) {

		List<CrossLink> interlinks = (List<CrossLink>) residue.protein.interLinks.stream().filter(
				value -> (value.protein_a.equals(residue.protein.proteinID) && value.pos_site_a == residue.position)
						|| (value.protein_b.equals(residue.protein.proteinID) && value.pos_site_b == residue.position))
				.collect(Collectors.toList());
		return interlinks;
	}

	/**
	 * Method responsible for checking if source protein comes firstly in XLs
	 * (source - target)
	 * 
	 * @param protein    current protein
	 * @param sourceNode source node
	 * @return list of XLs
	 */
	private List<CrossLink> checkTargetProteins(Protein protein, CyNode sourceNode) {

		List<CrossLink> new_XL = new ArrayList<CrossLink>();
		for (CrossLink crossLink : protein.interLinks) {

			if (crossLink.protein_a.equals(protein.proteinID))
				new_XL.add(crossLink);
			else {
				CrossLink xl = new CrossLink();
				xl.end_pos_protein = crossLink.end_pos_protein;
				xl.location = crossLink.location;
				xl.pos_site_a = crossLink.pos_site_b;
				xl.pos_site_b = crossLink.pos_site_a;
				xl.protein_a = crossLink.protein_b;
				xl.protein_b = crossLink.protein_a;
				xl.score = crossLink.score;
				xl.sequence = crossLink.sequence;
				xl.start_pos_protein = crossLink.start_pos_protein;
				new_XL.add(xl);
			}

		}

		return new_XL;
	}

	/**
	 * Method responsible for plotting all cross-links
	 * 
	 * @param myNetwork current network
	 * @param netView   current network view
	 * @param node      current node
	 * @param protein   current protein
	 */
	private void plotInterLink(CyNetwork myNetwork, CyNetworkView netView, CyNode node, Protein protein) {

		double x_or_y_Pos_source = 0;
		double xl_pos_source = 0;

		double x_or_y_Pos_target = 0;
		double xl_pos_target = 0;

		double center_position_source_node = 0;
		double center_position_target_node = 0;

		float other_node_width_or_height = 0;

		double initial_position_source_node = 0;
		double initial_position_target_node = 0;

		float proteinLength = Util.proteinLength;
		String pattern = "(Residue)(\\d+)( \\[Source:)( \\w+)( \\()(\\d+)(\\)(\\]))";
		boolean IsIntraLink = false;

		List<CrossLink> current_crosslinks;

		for (CyEdge edge : myNetwork.getAdjacentEdgeIterable(node, CyEdge.Type.ANY)) {

			CyNode sourceNode = myNetwork.getEdge(edge.getSUID()).getSource();
			CyNode targetNode = myNetwork.getEdge(edge.getSUID()).getTarget();

			if (sourceNode.getSUID() == targetNode.getSUID()) {// It's intralink
				IsIntraLink = true;
				current_crosslinks = protein.intraLinks;
			} else {// It's interlink
				IsIntraLink = false;
				current_crosslinks = checkTargetProteins(protein, sourceNode);
			}

			View<CyEdge> currentEdgeView = netView.getEdgeView(edge);
			while (currentEdgeView == null) {
				netView.updateView();
				try {
					Thread.sleep(200);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
				currentEdgeView = netView.getEdgeView(edge);
			}

			View<CyNode> sourceNodeView = null;
			View<CyNode> targetNodeView = null;

			// Target node will 'always' be the non-focused node (opposite of node.getSUID)
			if (sourceNode.getSUID() == highlight_node.getSUID()) {

				sourceNodeView = netView.getNodeView(sourceNode);
				targetNodeView = netView.getNodeView(targetNode);

			} else {

				sourceNodeView = netView.getNodeView(targetNode);
				targetNodeView = netView.getNodeView(sourceNode);
			}

			other_node_width_or_height = ((Number) targetNodeView.getVisualProperty(BasicVisualLexicon.NODE_WIDTH))
					.floatValue();

			if ((Math.round(other_node_width_or_height * 100.0)
					/ 100.0) == (Math.round((proteinLength * Util.node_label_factor_size) * 100.0) / 100.0))
				other_node_width_or_height = ((Number) sourceNodeView.getVisualProperty(BasicVisualLexicon.NODE_WIDTH))
						.floatValue();

			initial_position_source_node = Util.getXPositionOf(sourceNodeView);
			initial_position_target_node = Util.getXPositionOf(targetNodeView);

			// Target node will 'always' be the non-focused node (opposite of node.getSUID)
			if (sourceNode.getSUID() == highlight_node.getSUID()) {
				center_position_source_node = (proteinLength * Util.node_label_factor_size) / 2.0;
				center_position_target_node = (other_node_width_or_height) / 2.0;
			} else {
				center_position_source_node = (other_node_width_or_height) / 2.0;
				center_position_target_node = (proteinLength * Util.node_label_factor_size) / 2.0;
			}

			String source_node_name = myNetwork.getDefaultNodeTable().getRow(sourceNode.getSUID())
					.getRaw(CyNetwork.NAME).toString();
			String protein_source_name = source_node_name.replaceAll(pattern, "$4").trim();
			String res_source_pos = source_node_name.replaceAll(pattern, "$6").trim();

			String target_node_name = myNetwork.getDefaultNodeTable().getRow(targetNode.getSUID())
					.getRaw(CyNetwork.NAME).toString();
			String protein_target_name = target_node_name.replaceAll(pattern, "$4").trim();
			String res_target_pos = target_node_name.replaceAll(pattern, "$6").trim();

			for (int countEdge = 0; countEdge < current_crosslinks.size(); countEdge++) {

				if (!IsIntraLink && (!(current_crosslinks.get(countEdge).protein_a.equals(protein_source_name)
						&& current_crosslinks.get(countEdge).pos_site_a == Integer.parseInt(res_source_pos)
						&& current_crosslinks.get(countEdge).protein_b.equals(protein_target_name)
						&& current_crosslinks.get(countEdge).pos_site_b == Integer.parseInt(res_target_pos))))
					continue;
				else if (IsIntraLink
						&& !(current_crosslinks.get(countEdge).pos_site_a == Integer.parseInt(res_source_pos)
								&& current_crosslinks.get(countEdge).pos_site_b == Integer.parseInt(res_target_pos)))
					continue;

				// ###### STYLE #######
				Color linkColor = Util.InterLinksColor;
				CrossLink link = current_crosslinks.get(countEdge);

				if (link != null && link.location != null && !link.location.equals("UK")) {
					linkColor = Util.proteinDomainsColorMap.get(link.location);
				}

				currentEdgeView.setLockedValue(BasicVisualLexicon.EDGE_PAINT, linkColor);
				currentEdgeView.setLockedValue(BasicVisualLexicon.EDGE_SELECTED_PAINT, Color.RED);
				currentEdgeView.setLockedValue(BasicVisualLexicon.EDGE_STROKE_SELECTED_PAINT, Color.RED);
				currentEdgeView.setLockedValue(BasicVisualLexicon.EDGE_TRANSPARENCY, Util.edge_link_opacity);
				currentEdgeView.setLockedValue(BasicVisualLexicon.EDGE_WIDTH, Util.edge_link_width);
				currentEdgeView.setLockedValue(BasicVisualLexicon.EDGE_TARGET_ARROW_SHAPE,
						ArrowShapeVisualProperty.NONE);
				currentEdgeView.setLockedValue(BasicVisualLexicon.EDGE_SOURCE_ARROW_SHAPE,
						ArrowShapeVisualProperty.NONE);
				currentEdgeView.setLockedValue(BasicVisualLexicon.EDGE_LINE_TYPE, LineTypeVisualProperty.SOLID);

				// ##### POSITION #######

				if (sourceNode.getSUID() == highlight_node.getSUID()) {
					xl_pos_source = current_crosslinks.get(countEdge).pos_site_a * Util.node_label_factor_size;
					xl_pos_target = current_crosslinks.get(countEdge).pos_site_b;
				} else {
					xl_pos_source = current_crosslinks.get(countEdge).pos_site_b * Util.node_label_factor_size;
					xl_pos_target = current_crosslinks.get(countEdge).pos_site_a;
				}

				if (xl_pos_source <= center_position_source_node) { // [-protein_length/2, 0]
					x_or_y_Pos_source = (-center_position_source_node) + xl_pos_source;
				} else { // [0, protein_length/2]
					x_or_y_Pos_source = xl_pos_source - center_position_source_node;
				}
				x_or_y_Pos_source += initial_position_source_node;
				if (xl_pos_target <= center_position_target_node) { // [-protein_length/2, 0]
					x_or_y_Pos_target = (-center_position_target_node) + xl_pos_target;
				} else { // [0, protein_length/2]
					x_or_y_Pos_target = xl_pos_target - center_position_target_node;
				}
				x_or_y_Pos_target += initial_position_target_node;

				// ########## GET EDGE_BEND STYLE TO MODIFY #########

				Bend bend = bendFactory.createBend();

				Handle h = null;
				Handle h2 = null;

				h = handleFactory.createHandle(netView, currentEdgeView, (x_or_y_Pos_source - Util.OFFSET_BEND),
						Util.getYPositionOf(sourceNodeView));

				h2 = handleFactory.createHandle(netView, currentEdgeView, (x_or_y_Pos_target - Util.OFFSET_BEND),
						Util.getYPositionOf(targetNodeView));

				if (sourceNode.getSUID() == highlight_node.getSUID()) {
					bend.insertHandleAt(0, h);
					bend.insertHandleAt(1, h2);
				} else {// If target node is the selected node then first insert h2
					bend.insertHandleAt(0, h2);
					bend.insertHandleAt(1, h);
				}

				currentEdgeView.setLockedValue(BasicVisualLexicon.EDGE_BEND, bend);

				VisualProperty<?> vp_edge_curved = lexicon.lookup(CyEdge.class, "EDGE_CURVED");
				if (vp_edge_curved != null) {
					Object edge_curved_obj = vp_edge_curved.parseSerializableString("false");
					if (edge_curved_obj != null) {
						currentEdgeView.setLockedValue(vp_edge_curved, edge_curved_obj);
					}
				}
				break;
			}

			break;// there is only one edge
		}

	}

	/**
	 * Method responsible for creating network view
	 * 
	 * @param myNetwork current network
	 */
	private void createNetworkView(CyNetwork myNetwork) {
		// Create our view
		CyNetworkView view = viewFactory.createNetworkView(myNetwork);
		viewManager.addNetworkView(view);

		// The first residue will be the target residue
		List<Residue> dependent_residues = myResidue.history_residues;
		if (dependent_residues == null) {
			dependent_residues = new ArrayList<Residue>();
			dependent_residues.add(myResidue);
		}

		double y_location = 100;
		for (int i = 0; i < dependent_residues.size(); i++) {
			// RESIDUE1 [Source: PTN (POSITION)]
			String node_name = "Residue" + (i + 1) + " [Source: " + dependent_residues.get(i).protein.gene + " ("
					+ dependent_residues.get(i).position + ")]";
			CyNode new_node = Util.getNode(myNetwork, node_name);

			if (new_node == null)
				continue;

			Util.updateProteinDomainsColorMap(dependent_residues.get(i).protein.domains);

			// First, just set the appropriate locked values
			View<CyNode> new_nodeView = view.getNodeView(new_node);
			new_nodeView.setLockedValue(BasicVisualLexicon.NODE_LABEL, dependent_residues.get(i).protein.gene);

			Util.setProteinLength(dependent_residues.get(i).protein.sequence.length());
			Util.setNodeStyles(myNetwork, new_node, view, style);

			if (i == 0) {// target protein
				new_nodeView.setLockedValue(BasicVisualLexicon.NODE_BORDER_PAINT, new Color(153, 0, 0, 70));
				new_nodeView.setLockedValue(BasicVisualLexicon.NODE_SELECTED_PAINT, new Color(153, 0, 0, 70));
			}

			Util.setNodeDomainColors(null, dependent_residues.get(i).protein, new_nodeView, myNetwork, new_node,
					vgFactory, lexicon);

			new_nodeView.setLockedValue(BasicVisualLexicon.NODE_X_LOCATION, 1.0);
			new_nodeView.setLockedValue(BasicVisualLexicon.NODE_Y_LOCATION, y_location);
			this.plotResidue(dependent_residues.get(i), new_nodeView, myNetwork, new_node, view);

			y_location += 100;

		}

		// Conflicted residue
		String conflicted_node_name = "Residue" + (dependent_residues.size() + 1) + " [Source: "
				+ myResidue.conflicted_residue.protein.gene + " (" + myResidue.conflicted_residue.position + ")]";
		CyNode conflicted_node = Util.getNode(myNetwork, conflicted_node_name);

		if (conflicted_node != null) {

			Util.updateProteinDomainsColorMap(myResidue.conflicted_residue.protein.domains);

			View<CyNode> conflicted_nodeView = view.getNodeView(conflicted_node);
			conflicted_nodeView.setLockedValue(BasicVisualLexicon.NODE_LABEL,
					myResidue.conflicted_residue.protein.gene);

			Util.setProteinLength(myResidue.conflicted_residue.protein.sequence.length());
			Util.setNodeStyles(myNetwork, conflicted_node, view, style);
			Util.setNodeDomainColors(null, myResidue.conflicted_residue.protein, conflicted_nodeView, myNetwork,
					conflicted_node, vgFactory, lexicon);

			conflicted_nodeView.setLockedValue(BasicVisualLexicon.NODE_X_LOCATION, 1.0);
			conflicted_nodeView.setLockedValue(BasicVisualLexicon.NODE_Y_LOCATION, 0.0);
			this.plotResidue(myResidue.conflicted_residue, conflicted_nodeView, myNetwork, conflicted_node, view);

			Protein new_protein_with_only_target_xls = new Protein(myResidue.conflicted_residue.protein.proteinID,
					myResidue.conflicted_residue.protein.gene, myResidue.conflicted_residue.protein.sequence);

			new_protein_with_only_target_xls.intraLinks = this.getInterestedIntraLinks(myResidue.conflicted_residue);
			new_protein_with_only_target_xls.interLinks = this.getInterestedInterLinks(myResidue.conflicted_residue);

			plotInterLink(myNetwork, view, conflicted_node, new_protein_with_only_target_xls);

		}

		// Add edge labels with the score
		if (dependent_residues.size() > 1) {
			for (int i = 0; i < dependent_residues.size(); i += 2) {
				String edge_name = "RESIDUE" + (i + 1) + " - RESIDUE" + (i + 2);
				CyEdge new_edge = Util.getEdge(myNetwork, edge_name, false);

				if (new_edge == null)
					continue;

				View<CyEdge> new_edgeView = view.getEdgeView(new_edge);
				new_edgeView.setLockedValue(BasicVisualLexicon.EDGE_LABEL,
						"Score: " + String.format("%.4f", dependent_residues.get(i).score));

				String tooltip = "<html><p>" + dependent_residues.get(i).protein.gene + " ["
						+ dependent_residues.get(i).position + "] - " + dependent_residues.get(i + 1).protein.gene
						+ " [" + dependent_residues.get(i + 1).position + "]</p></html>";
				new_edgeView.setLockedValue(BasicVisualLexicon.EDGE_TOOLTIP, tooltip);

				Protein new_protein_with_only_target_xls = new Protein(dependent_residues.get(i).protein.proteinID,
						dependent_residues.get(i).protein.gene, dependent_residues.get(i).protein.sequence);

				new_protein_with_only_target_xls.intraLinks = this.getInterestedIntraLinks(dependent_residues.get(i));
				new_protein_with_only_target_xls.interLinks = this.getInterestedInterLinks(dependent_residues.get(i));

				String node_name = "Residue" + (i + 1) + " [Source: " + dependent_residues.get(i).protein.gene + " ("
						+ dependent_residues.get(i).position + ")]";
				CyNode new_node = Util.getNode(myNetwork, node_name);

				if (new_node == null)
					continue;

				Util.setProteinLength(new_protein_with_only_target_xls.sequence.length());
				plotInterLink(myNetwork, view, new_node, new_protein_with_only_target_xls);
			}
		}

		// Conflicted_edge
		String conflicted_edge_name = "RESIDUE" + (dependent_residues.size() + 1) + " - RESIDUE1";
		CyEdge conflicted_edge = Util.getEdge(myNetwork, conflicted_edge_name, false);

		if (conflicted_edge != null) {
			View<CyEdge> conflicted_edgeView = view.getEdgeView(conflicted_edge);
			conflicted_edgeView.setLockedValue(BasicVisualLexicon.EDGE_LABEL,
					"Score: " + String.format("%.4f", myResidue.conflicted_score));
			
			String tooltip = "<html><p>" + dependent_residues.get(0).protein.gene + " ["
					+ dependent_residues.get(0).position + "] - " + myResidue.conflicted_residue.protein.gene
					+ " [" + myResidue.conflicted_residue.position + "]</p></html>";
			conflicted_edgeView.setLockedValue(BasicVisualLexicon.EDGE_TOOLTIP, tooltip);
		}
	}

	/**
	 * Method responsible for plotting all residue
	 * 
	 * @param current_residue current residue
	 * @param nodeView        current node view
	 * @param myNetwork       current network
	 * @param node            current node
	 * @param view            current network view
	 */
	private void plotResidue(Residue current_residue, View<CyNode> nodeView, CyNetwork myNetwork, CyNode node,
			CyNetworkView view) {

		if (current_residue == null || myNetwork == null || node == null || view == null) {
			return;
		}

		Protein protein = current_residue.protein;
		if (protein == null)
			return;

		double x_or_y_Pos_source = 0;
		double xl_pos_source = 0;
		double center_position_source_node = (Util.proteinLength * Util.node_label_factor_size) / 2.0;

		double initial_position_source_node = 0;
		if (Util.isProtein_expansion_horizontal) {
			initial_position_source_node = Util.getXPositionOf(nodeView);
		} else {
			initial_position_source_node = Util.getYPositionOf(nodeView);
		}

		final String node_name_added_by_app = "RESIDUE_BAR [Source: " + protein.gene + " (" + current_residue.position
				+ ")]";

		CyNode _node = Util.getNode(myNetwork, node_name_added_by_app);
		if (_node == null) {// Add a new node if does not exist

			_node = myNetwork.addNode();
			myNetwork.getRow(_node).set(CyNetwork.NAME, node_name_added_by_app);
			Util.setResidueStyle(view, _node, nodeView, current_residue, xl_pos_source, center_position_source_node,
					x_or_y_Pos_source, initial_position_source_node);
		}
	}

	/**
	 * Method responsible for creating columns into the table
	 * 
	 * @param myNetwork   current network
	 * @param column_name column name
	 */
	private void createColumnInToTheTable(CyNetwork myNetwork, String column_name) {
		CyTable nodeTable = myNetwork.getDefaultNodeTable();
		if (nodeTable.getColumn(column_name) == null) {
			try {
				nodeTable.createColumn(column_name, String.class, false);

				for (CyRow row : myNetwork.getDefaultNodeTable().getAllRows()) {
					row.set(column_name, "");
				}

			} catch (Exception e) {
			}
		}
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
