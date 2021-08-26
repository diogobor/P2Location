package de.fmp.liulab.task;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.view.model.VisualLexicon;
import org.cytoscape.view.presentation.property.values.BendFactory;
import org.cytoscape.view.presentation.property.values.HandleFactory;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;

import de.fmp.liulab.model.CrossLink;
import de.fmp.liulab.model.PTM;
import de.fmp.liulab.model.Protein;
import de.fmp.liulab.utils.Util;

/**
 * Class responsible for updating edges
 * 
 * @author diogobor
 *
 */
public class UpdateViewerTask extends AbstractTask {

	private CyApplicationManager cyApplicationManager;

	private HandleFactory handleFactory;
	private BendFactory bendFactory;

	private CyNetwork myNetwork;
	private CyNetworkView netView;

	private CyNode node;

	private boolean IsIntraLink = false;

	/**
	 * Constructor
	 * 
	 * @param cyApplicationManager main app manager
	 * @param handleFactory        handle factory
	 * @param bendFactory          bend factory
	 * @param myNetwork            current network
	 * @param netView              current network view
	 * @param node                 current node
	 */
	public UpdateViewerTask(CyApplicationManager cyApplicationManager, HandleFactory handleFactory,
			BendFactory bendFactory, CyNetwork myNetwork, CyNetworkView netView, CyNode node) {

		this.cyApplicationManager = cyApplicationManager;

		this.handleFactory = handleFactory;
		this.bendFactory = bendFactory;

		this.myNetwork = myNetwork;
		this.netView = netView;
		this.node = node;

	}

	@Override
	public void run(TaskMonitor taskMonitor) throws Exception {

		updateNodesAndEdges(this.node, taskMonitor);
	}

	/**
	 * Method responsible for updating Nodes and Edges
	 * 
	 * @param current_node current node
	 * @param taskMonitor  task monitor
	 */
	private void updateNodesAndEdges(final CyNode current_node, TaskMonitor taskMonitor) {

		MainSingleNodeTask.isPlotDone = false;

		double current_scaling_factor = myNetwork.getRow(current_node).get(Util.PROTEIN_SCALING_FACTOR_COLUMN_NAME,
				Double.class) != null
						? myNetwork.getRow(current_node).get(Util.PROTEIN_SCALING_FACTOR_COLUMN_NAME, Double.class)
						: 1.0;
		View<CyNode> nodeView = netView.getNodeView(current_node);

		if (current_scaling_factor != 1)
			Util.updateMapNodesPosition(current_node, nodeView);

		CyRow proteinA_node_row = myNetwork.getRow(current_node);
		Object length_other_protein_a = proteinA_node_row.getRaw(Util.PROTEIN_LENGTH_A);
		Object length_other_protein_b = proteinA_node_row.getRaw(Util.PROTEIN_LENGTH_B);

		if (length_other_protein_a == null) {
			if (length_other_protein_b == null)
				length_other_protein_a = 10;
			else
				length_other_protein_a = length_other_protein_b;
		}

		Util.setProteinLength((float) ((Number) length_other_protein_a).doubleValue());

		VisualStyle style = MainSingleNodeTask.style;
		if (style == null)
			style = ProcessProteinLocationTask.style;
		if (style == null)
			return;

		VisualLexicon lexicon = MainSingleNodeTask.lexicon;
		if (lexicon == null)
			lexicon = ProcessProteinLocationTask.lexicon;
		if (lexicon == null)
			return;

		Util.stopUpdateViewer = false;

		String node_name = myNetwork.getDefaultNodeTable().getRow(current_node.getSUID()).getRaw(CyNetwork.NAME)
				.toString();
		Protein protein = Util.getProtein(myNetwork, node_name);

		if (protein == null) {
			MainSingleNodeTask.isPlotDone = true;
			return;
		}

		if (protein.interLinks != null && protein.interLinks.size() > 0) { // The selectedNode has interlinks
			IsIntraLink = false;
		} else {
			IsIntraLink = true;
		}

		if (Util.IsNodeModified(myNetwork, netView, current_node)) {

			Util.node_label_factor_size = 1.0;
			Util.setNodeStyles(myNetwork, current_node, netView, style);

			MainSingleNodeTask.isPlotDone = Util.addOrUpdateEdgesToNetwork(myNetwork, current_node, style, netView,
					nodeView, handleFactory, bendFactory, lexicon, ((Number) length_other_protein_a).floatValue(),
					protein, taskMonitor, null);

			Util.node_label_factor_size = current_scaling_factor;

			if (Util.node_label_factor_size != 1) {
				MainSingleNodeTask.isPlotDone = false;
				this.resizeProtein(current_node, nodeView, lexicon, length_other_protein_a, style);
			}

			if (!Util.showInterLinks) {// hide all interlinks

				Util.hideAllInterLinks(myNetwork, current_node, netView);
			}

			if (Util.showPTMs) {

				ArrayList<PTM> myPTMs = this.getPTMs(node);
				Util.setNodePTMs(taskMonitor, myNetwork, netView, node, style, handleFactory, bendFactory, lexicon,
						myPTMs, true);

			}

			if (Util.showMonolinkedPeptides) {
				ArrayList<CrossLink> myMonolinks = this.getMonolinks(current_node);
				Util.setMonolinksToNode(taskMonitor, myNetwork, netView, current_node, style, handleFactory,
						bendFactory, lexicon, myMonolinks, "");
			}

		} else if (!IsIntraLink) {
			Util.updateAllAssiciatedInterlinkNodes(myNetwork, cyApplicationManager, netView, handleFactory, bendFactory,
					current_node);// Check if all associated nodes are
									// unmodified
			MainSingleNodeTask.isPlotDone = true;
		}
//		else {// Nodes are not modified -> check comb_score to display edges or not
//
//			Util.checkUnmodifiedEdgeToDisplay(myNetwork, cyApplicationManager, netView, handleFactory, bendFactory,
//					current_node);
//			MainSingleNodeTask.isPlotDone = true;
//		}
	}

	/**
	 * Method responsible for getting all ptms of the selected node from the main
	 * map (Util.ptmsMap)
	 * 
	 * @param node
	 * @return ptms list
	 */
	private ArrayList<PTM> getPTMs(CyNode node) {

		String network_name = myNetwork.toString();
		if (Util.ptmsMap.containsKey(network_name)) {

			Map<Long, List<PTM>> all_ptms = Util.ptmsMap.get(network_name);

			if (all_ptms.containsKey(node.getSUID())) {
				return (ArrayList<PTM>) all_ptms.get(node.getSUID());
			}
		}

		return new ArrayList<PTM>();
	}

	/**
	 * Method responsible for getting all monolinks of the selected node from the
	 * main map (Util.ptmsMap)
	 * 
	 * @param node
	 * @return ptms list
	 */
	private ArrayList<CrossLink> getMonolinks(CyNode node) {

//		String network_name = myNetwork.toString();
//		if (Util.monolinksMap.containsKey(network_name)) {
//
//			Map<Long, Protein> all_monolinks = Util.monolinksMap.get(network_name);
//
//			if (all_monolinks.containsKey(node.getSUID())) {
//				return (ArrayList<CrossLink>) ((Protein) all_monolinks.get(node.getSUID())).monolinks;
//			}
//		}

		return new ArrayList<CrossLink>();

	}

	/**
	 * Resize protein node
	 * 
	 * @param current_node           current node
	 * @param nodeView               current node view
	 * @param lexicon                lexicon
	 * @param length_other_protein_a length of the current protein
	 */
	private void resizeProtein(CyNode current_node, View<CyNode> nodeView, VisualLexicon lexicon,
			Object length_other_protein_a, VisualStyle style) {
		Util.setNodeStyles(myNetwork, current_node, netView, style);

		String node_name = myNetwork.getDefaultNodeTable().getRow(current_node.getSUID()).getRaw(CyNetwork.NAME)
				.toString();
		Protein protein = Util.getProtein(myNetwork, node_name);
		MainSingleNodeTask.isPlotDone = Util.addOrUpdateEdgesToNetwork(myNetwork, current_node, style, netView,
				nodeView, handleFactory, bendFactory, lexicon, ((Number) length_other_protein_a).floatValue(), protein,
				null, null);
	}
}
