package de.fmp.liulab.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.events.SetCurrentNetworkEvent;
import org.cytoscape.application.events.SetCurrentNetworkListener;
import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTableUtil;
import org.cytoscape.model.events.AboutToRemoveEdgesEvent;
import org.cytoscape.model.events.AboutToRemoveEdgesListener;
import org.cytoscape.model.events.NetworkAddedEvent;
import org.cytoscape.model.events.NetworkAddedListener;
import org.cytoscape.model.events.NetworkDestroyedEvent;
import org.cytoscape.model.events.NetworkDestroyedListener;
import org.cytoscape.model.events.RemovedEdgesEvent;
import org.cytoscape.model.events.RemovedEdgesListener;
import org.cytoscape.model.events.RowSetRecord;
import org.cytoscape.model.events.RowsSetEvent;
import org.cytoscape.model.events.RowsSetListener;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.model.View;
import org.cytoscape.view.model.VisualLexicon;
import org.cytoscape.view.model.events.ViewChangeRecord;
import org.cytoscape.view.model.events.ViewChangedEvent;
import org.cytoscape.view.model.events.ViewChangedListener;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.presentation.property.values.BendFactory;
import org.cytoscape.view.presentation.property.values.HandleFactory;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.work.FinishStatus;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskObserver;
import org.cytoscape.work.swing.DialogTaskManager;

import de.fmp.liulab.model.CrossLink;
import de.fmp.liulab.model.PredictedTransmem;
import de.fmp.liulab.model.Protein;
import de.fmp.liulab.model.ProteinDomain;
import de.fmp.liulab.model.Residue;
import de.fmp.liulab.task.MainSingleNodeTask;
import de.fmp.liulab.task.ProcessProteinLocationTask;
import de.fmp.liulab.task.ProteinScalingFactorHorizontalExpansionTableTask;
import de.fmp.liulab.task.ProteinScalingFactorHorizontalExpansionTableTaskFactory;
import de.fmp.liulab.task.UpdateViewerTaskFactory;
import de.fmp.liulab.utils.Util;

/**
 * Class responsible for updating layout
 * 
 * @author borges.diogo
 *
 */
public class UpdateViewListener implements ViewChangedListener, RowsSetListener, SetCurrentNetworkListener,
		NetworkAddedListener, NetworkDestroyedListener, RemovedEdgesListener, AboutToRemoveEdgesListener {

	private CyApplicationManager cyApplicationManager;
	private CyNetwork myNetwork;
	private CyNetworkView netView;
	private HandleFactory handleFactory;
	private BendFactory bendFactory;
	private VisualStyle style;
	private CyNetworkViewManager networkViewManager;

	private DialogTaskManager dialogTaskManager;
	private ProteinScalingFactorHorizontalExpansionTableTaskFactory proteinScalingFactorHorizontalExpansionTableTaskFactory;
	private UpdateViewerTaskFactory updateViewerTaskFactory;

	private CyNode selectedNode;
	private CyNode current_node;
	private String current_network_name;

	public static boolean isNodeModified = false;

	private List<Long> nodes_suids = new ArrayList<Long>();

	/**
	 * Constructor
	 * 
	 * @param cyApplicationManager                                    main app
	 *                                                                manager
	 * @param handleFactory                                           handle factory
	 * @param bendFactory                                             bend factory
	 * @param vmmServiceRef                                           visual mapping
	 *                                                                manager
	 * @param dialogTaskManager                                       task manager
	 * @param proteinScalingFactorHorizontalExpansionTableTaskFactory protein length
	 *                                                                scaling factor
	 *                                                                factory
	 * @param updateViewerTaskFactory                                 update viewer
	 *                                                                task factory
	 * @param networkViewManager                                      networkview
	 *                                                                manager
	 */
	public UpdateViewListener(CyApplicationManager cyApplicationManager, HandleFactory handleFactory,
			BendFactory bendFactory, VisualMappingManager vmmServiceRef, DialogTaskManager dialogTaskManager,
			ProteinScalingFactorHorizontalExpansionTableTaskFactory proteinScalingFactorHorizontalExpansionTableTaskFactory,
			UpdateViewerTaskFactory updateViewerTaskFactory, CyNetworkViewManager networkViewManager) {
		this.cyApplicationManager = cyApplicationManager;
		this.handleFactory = handleFactory;
		this.bendFactory = bendFactory;
		this.style = vmmServiceRef.getCurrentVisualStyle();
		this.dialogTaskManager = dialogTaskManager;
		this.proteinScalingFactorHorizontalExpansionTableTaskFactory = proteinScalingFactorHorizontalExpansionTableTaskFactory;
		this.updateViewerTaskFactory = updateViewerTaskFactory;
		this.networkViewManager = networkViewManager;
	}

	/**
	 * Method responsible for displaying or not valid proteins
	 * 
	 * @param e event
	 */
	private void hiddenNotValidProteins(RowsSetEvent e) {

		if (myNetwork == null || netView == null)
			return;

		if (e.getColumnRecords(Util.VALID_PROTEINS_COLUMN).size() == 1) {
			for (RowSetRecord record : e.getColumnRecords(Util.VALID_PROTEINS_COLUMN)) {
				Long suid = record.getRow().get(CyIdentifiable.SUID, Long.class);
				Boolean value = (Boolean) record.getValue();
				CyNode selectedNode = myNetwork.getNode(suid);
				View<CyNode> new_node_source_view = netView.getNodeView(selectedNode);
				if (value.equals(Boolean.TRUE))
					new_node_source_view.setLockedValue(BasicVisualLexicon.NODE_VISIBLE, true);
				else
					new_node_source_view.setLockedValue(BasicVisualLexicon.NODE_VISIBLE, false);

				CyRow myCurrentRow = myNetwork.getRow(selectedNode);
				VisualLexicon lexicon = cyApplicationManager.getCurrentRenderingEngine().getVisualLexicon();
				Util.restoreDefaultStyle(null, style, netView, cyApplicationManager, new_node_source_view,
						handleFactory, bendFactory, selectedNode, myNetwork, myCurrentRow, lexicon);
				Util.updateValidProteinInfo(myNetwork, selectedNode, value);
			}
		}
	}

	/**
	 * Method responsible for capturing the selected node.
	 */
	@Override
	public void handleEvent(RowsSetEvent e) {

		myNetwork = cyApplicationManager.getCurrentNetwork();
		Collection<CyNetworkView> views = networkViewManager.getNetworkViews(myNetwork);
		if (views.size() != 0)
			netView = views.iterator().next();
		else
			return;
		nodes_suids.clear();

		try {
			// First check whether 'valid_proteins' column has been changed
			if (e.containsColumn(Util.VALID_PROTEINS_COLUMN)) {
				hiddenNotValidProteins(e);
			}

			// Check whether this event has anything to do with selections
			if (!e.containsColumn(CyNetwork.SELECTED)) {
				// Nope, we're done
				return;
			}

			// For each selected node, get the view in the current network
			// and change the shape
			if (e.getSource() != myNetwork.getDefaultNodeTable())
				return;

			if (e.getColumnRecords(CyNetwork.SELECTED).size() == 1) {
				this.selectedNode = null;
				for (RowSetRecord record : e.getColumnRecords(CyNetwork.SELECTED)) {
					Long suid = record.getRow().get(CyIdentifiable.SUID, Long.class);
					Boolean value = (Boolean) record.getValue();
					if (value.equals(Boolean.TRUE)) {
						this.selectedNode = myNetwork.getNode(suid);

						updateNodesAndEdges(new ArrayList<CyNode>(Arrays.asList(this.selectedNode)));
					}
				}
			}
		} catch (Exception exception) {
		}
	}

	/**
	 * Method responsible for update all nodes according to the movement of the
	 * mouse.
	 */
	@Override
	public void handleEvent(ViewChangedEvent<?> e) {

		try {

			if (!MainSingleNodeTask.isPlotDone)
				return;
			if (!ProcessProteinLocationTask.isPlotDone)
				return;

			myNetwork = cyApplicationManager.getCurrentNetwork();
			Collection<CyNetworkView> views = networkViewManager.getNetworkViews(myNetwork);
			if (views.size() != 0)
				netView = views.iterator().next();
			else
				return;

			List<CyNode> nodes = CyTableUtil.getNodesInState(myNetwork, CyNetwork.SELECTED, true);

			Set<CyNode> nodeSuidList = new HashSet<CyNode>();

			for (ViewChangeRecord<?> record : e.getPayloadCollection()) {
				View<?> record_view = record.getView();
				if (record_view.getModel() instanceof CyNode) {
					nodeSuidList.add(((CyNode) record_view.getModel()));
				}
			}

			if (nodeSuidList.size() == 0)// It means no CyNode has been selected
				return;

			List<CyNode> nodes_to_be_updated = new ArrayList<CyNode>();
			// Check if all selected nodes have been modified
			for (final CyNode _node : nodes) {

				String node_name = myNetwork.getRow(_node).get(CyNetwork.NAME, String.class);
				// Check if the node exists in the network

				Optional<CyNode> isNodePresent = nodeSuidList.stream().filter(new Predicate<CyNode>() {
					public boolean test(CyNode o) {
						return o.getSUID() == _node.getSUID();
					}
				}).findFirst();
				if (!isNodePresent.isPresent()) {
					if (!(node_name.contains("Source") || node_name.contains("Target") || node_name.contains("PTM")))
						return;
				}

				if (node_name.contains("Source") || node_name.contains("Target") || node_name.contains("PTM")) {
					nodes_to_be_updated.add(_node);
				} else {
					Optional<Long> isNodePresent_SUID = nodes_suids.stream().filter(new Predicate<Long>() {
						public boolean test(Long o) {
							return o == _node.getSUID();
						}
					}).findFirst();
					if (!isNodePresent_SUID.isPresent()) {
						nodes_to_be_updated.add(_node);
					}
				}
			}

			updateNodesAndEdges(nodes_to_be_updated);

		} catch (Exception exception) {
		}
	}

	/**
	 * Update nodes and edges
	 * 
	 * @param current_node current node
	 */
	private void updateNodesAndEdges(List<CyNode> nodes) {
		final Iterator<CyNode> _iterator_CyNode = nodes.iterator();

		if (!_iterator_CyNode.hasNext())
			return;

		current_node = _iterator_CyNode.next();

		String node_name = myNetwork.getRow(current_node).get(CyNetwork.NAME, String.class);
		while (node_name.contains("Source") || node_name.contains("Target") || node_name.contains("PTM")) {
			if (_iterator_CyNode.hasNext()) {
				current_node = _iterator_CyNode.next();
				node_name = myNetwork.getRow(current_node).get(CyNetwork.NAME, String.class);
			} else
				return;
		}

		View<CyNode> nodeView = netView.getNodeView(current_node);
		double current_posX = nodeView.getVisualProperty(BasicVisualLexicon.NODE_X_LOCATION);
		double current_posY = nodeView.getVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION);
		if (Util.mapLastNodesPosition.containsKey(current_node)) {
			double last_posX = (double) Util.mapLastNodesPosition.get(current_node).getFirst();
			double last_posY = (double) Util.mapLastNodesPosition.get(current_node).getSecond();
			if (current_posX == last_posX && current_posY == last_posY)
				return;
		}

		if (!Util.stopUpdateViewer) {
			if (this.dialogTaskManager != null && this.updateViewerTaskFactory != null) {

				nodes_suids.add(current_node.getSUID());

				TaskIterator ti = this.updateViewerTaskFactory.createTaskIterator(cyApplicationManager, handleFactory,
						bendFactory, myNetwork, netView, current_node);

				TaskObserver observer = new TaskObserver() {

					@Override
					public void taskFinished(ObservableTask task) {

					}

					@Override
					public void allFinished(FinishStatus finishStatus) {
						if (!_iterator_CyNode.hasNext()) {
							nodes_suids.remove(current_node.getSUID());
							return;
						}

						final List<CyNode> remainingList = new ArrayList<CyNode>();
						_iterator_CyNode.forEachRemaining(new Consumer<CyNode>() {
							@Override
							public void accept(CyNode key) {
								remainingList.add(key);
							}
						});
						nodes_suids.remove(current_node.getSUID());
						updateNodesAndEdges(remainingList);
					}
				};

				this.dialogTaskManager.execute(ti, observer);

			}
		}

	}

	/**
	 * Method responsible for update all nodes according to the current selected
	 * network
	 */

	public void handleEvent(SetCurrentNetworkEvent e) {

		Util.mapLastNodesPosition.clear();
		nodes_suids.clear();

		try {

			// Update variables
			if (cyApplicationManager == null)
				return;

			// Check if the current network is null
			if (e.getNetwork() == null)
				return;

			// Get the real network
			myNetwork = cyApplicationManager.getCurrentNetwork();
			if (myNetwork == null)
				return;

			current_network_name = myNetwork.toString();

			int network_index = Util.myCyNetworkList.indexOf(myNetwork);

			if (network_index == 0 && ProcessProteinLocationTask.number_unknown_residues != null
					&& ProcessProteinLocationTask.number_unknown_residues.size() > 0) {
				MainControlPanel.setEpochCombobox();
			}

			Collection<CyNetworkView> views = networkViewManager.getNetworkViews(myNetwork);
			if (views.size() != 0)
				netView = views.iterator().next();
			else
				return;

			if (netView == null)
				return;
			// Load network and netview to P2Location setting to update the edges plot
			MainControlPanel.myNetwork = myNetwork;
			MainControlPanel.netView = netView;
			MainControlPanel.style = this.style;
			MainControlPanel.handleFactory = this.handleFactory;
			MainControlPanel.bendFactory = this.bendFactory;

			if (cyApplicationManager.getCurrentRenderingEngine() == null)
				return;
			MainControlPanel.lexicon = cyApplicationManager.getCurrentRenderingEngine().getVisualLexicon();
			MainSingleNodeTask.lexicon = cyApplicationManager.getCurrentRenderingEngine().getVisualLexicon();
			MainSingleNodeTask.style = this.style;

			List<CyNode> nodes = CyTableUtil.getNodesInState(myNetwork, CyNetwork.SELECTED, true);
			if (nodes.size() == 1) {

				CyRow proteinA_node_row = myNetwork.getRow(nodes.get(0));
				Object length_other_protein_a = proteinA_node_row.getRaw(Util.PROTEIN_LENGTH_A);
				Object length_other_protein_b = proteinA_node_row.getRaw(Util.PROTEIN_LENGTH_B);

				if (length_other_protein_a == null) {
					if (length_other_protein_b == null)
						length_other_protein_a = 10;
					else
						length_other_protein_a = length_other_protein_b;
				}

				Util.setProteinLength(((Number) length_other_protein_a).floatValue());

				try {
					if (this.style == null)
						isNodeModified = false;
					else
						isNodeModified = Util.IsNodeModified(myNetwork, netView, nodes.get(0));

				} catch (Exception e2) {
					isNodeModified = false;
				}
			}

			Util.filterUnmodifiedEdges(myNetwork, netView);

		} catch (Exception exception) {
		} finally {

			MainSingleNodeTask.isPlotDone = true;
			ProcessProteinLocationTask.isPlotDone = true;
		}
	}

	/**
	 * Method responsible for updating proteinScalingFactor after creating a network
	 */
	@Override
	public void handleEvent(NetworkAddedEvent event) {

		CyNetwork cyNetwork = event.getNetwork();
		if (cyNetwork != null) {
			Util.myCyNetworkList.add(cyNetwork);
			myNetwork = cyNetwork;

			// Initialize proteinMap
			if (!Util.proteinsMap.containsKey(cyNetwork.toString()))
				Util.proteinsMap.put(cyNetwork.toString(), new ArrayList<Protein>());

			// Initialize Transmem dictionary
			if (!Util.proteinsWithPredTransmDict.containsKey(cyNetwork.toString())) {
				Util.proteinsWithPredTransmDict.put(cyNetwork.toString(),
						new HashMap<Protein, List<PredictedTransmem>>());
			}

			createAuxiliarColumnsTable();

			MainControlPanel.enable_disableDisplayBox(false, true);
			MainControlPanel.enable_disable_spinners(true);
			MainControlPanel.unselectCheckboxes();
		}
	}

	/**
	 * Create auxiliar columns in the tables
	 */
	private void createAuxiliarColumnsTable() {

		// Check if the node exists in the network
		Optional<CyNetwork> isNetworkPresent = Util.myCyNetworkList.stream().filter(new Predicate<CyNetwork>() {
			public boolean test(CyNetwork o) {
				return o.getSUID() == myNetwork.getSUID();
			}
		}).findFirst();

		if (isNetworkPresent.isPresent()) {// Get node if exists
			CyNetwork current_network = isNetworkPresent.get();
			if (this.dialogTaskManager != null && this.proteinScalingFactorHorizontalExpansionTableTaskFactory != null
					&& !ProteinScalingFactorHorizontalExpansionTableTask.isProcessing) {
				TaskIterator ti = this.proteinScalingFactorHorizontalExpansionTableTaskFactory
						.createTaskIterator(current_network, false);
				this.dialogTaskManager.execute(ti);

			}
		}
	}

	@Override
	public void handleEvent(NetworkDestroyedEvent event) {

		if (!(current_network_name.isBlank() || current_network_name.isEmpty())) {
			if (Util.proteinsMap.containsKey(current_network_name))
				Util.proteinsMap.remove(current_network_name);

			if (Util.proteinsWithPredTransmDict.containsKey(current_network_name)) {
				Util.proteinsWithPredTransmDict.remove(current_network_name);
			}

			// Check if the network exists in the network list
			Optional<CyNetwork> isNetworkPresent = Util.myCyNetworkList.stream().filter(new Predicate<CyNetwork>() {
				public boolean test(CyNetwork o) {
					return o.getSUID() == myNetwork.getSUID();
				}
			}).findFirst();

			if (isNetworkPresent.isPresent()) {// Get node if exists
				CyNetwork current_network = isNetworkPresent.get();
				Util.myCyNetworkList.remove(current_network);
			}

		}

		MainControlPanel.enable_disableDisplayBox(false, true);
		MainControlPanel.enable_disable_spinners(false);
		MainControlPanel.unselectCheckboxes();
		MainControlPanel.myNetwork = null;
	}

	@Override
	public void handleEvent(RemovedEdgesEvent e) {

		System.out.println("Edge has been removed sucessfully");

	}

	@Override
	public void handleEvent(AboutToRemoveEdgesEvent e) {

		String pattern = "(\\[Source: )(\\w+)(\\s+)(\\()(\\d+)(\\))(\\])(\\s+)(\\[Target: )(\\w+)(\\s+)(\\()(\\d+)(\\))(\\])";
		String edgeName = e.getSource().getDefaultEdgeTable().getRow(e.getEdges().iterator().next().getSUID())
				.get(CyNetwork.NAME, String.class);
		if (!edgeName.matches(pattern))
			return;

		String source_protein_name = edgeName.replaceAll(pattern, "$2").trim();
		int source_res_pos = Integer.parseInt(edgeName.replaceAll(pattern, "$5").trim());

		String target_protein_name = edgeName.replaceAll(pattern, "$10").trim();
		int target_res_pos = Integer.parseInt(edgeName.replaceAll(pattern, "$13").trim());

		String[] networkFullName = this.myNetwork.toString().split("#");

		String networkMainName = "";
		if (networkFullName.length > 0)
			networkMainName = networkFullName[0];

		List<Protein> proteinList = Util.proteinsMap.get(networkMainName);
		if (proteinList == null || proteinList.size() == 0)
			return;

		boolean isIntralink = source_protein_name.equals(target_protein_name);

		Protein source_protein = null;
		Protein target_protein = null;
		try {
			source_protein = proteinList.stream().filter(value -> value.gene.equals(source_protein_name)).findFirst()
					.get();

			target_protein = proteinList.stream().filter(value -> value.gene.equals(target_protein_name)).findFirst()
					.get();
		} catch (Exception e2) {
			// TODO: handle exception
		}

		if (source_protein == null || target_protein == null)
			return;

		if (!isIntralink) {

			// Remove XL from source protein
			List<CrossLink> allXLs = source_protein.interLinks;
			CrossLink to_be_removed = allXLs.stream()
					.filter(value -> (value.protein_a.equals(source_protein_name) && value.pos_site_a == source_res_pos
							&& value.protein_b.equals(target_protein_name) && value.pos_site_b == target_res_pos)
							|| (value.protein_a.equals(target_protein_name) && value.pos_site_a == target_res_pos
									&& value.protein_b.equals(source_protein_name)
									&& value.pos_site_b == source_res_pos))
					.findFirst().get();

			if (to_be_removed != null)
				allXLs.remove(to_be_removed);

			// Remove XL from target protein
			allXLs = target_protein.interLinks;
			to_be_removed = allXLs.stream()
					.filter(value -> (value.protein_a.equals(source_protein_name) && value.pos_site_a == source_res_pos
							&& value.protein_b.equals(target_protein_name) && value.pos_site_b == target_res_pos)
							|| (value.protein_a.equals(target_protein_name) && value.pos_site_a == target_res_pos
									&& value.protein_b.equals(source_protein_name)
									&& value.pos_site_b == source_res_pos))
					.findFirst().get();

			if (to_be_removed != null)
				allXLs.remove(to_be_removed);

		}

		// Update conflicted residues
		List<Residue> all_residues = source_protein.reactionSites;
		Residue conflicted_residue = null;
		try {
			conflicted_residue = all_residues.stream().filter(value -> value.position == source_res_pos).findFirst()
					.get();

			conflicted_residue.conflicted_residue = null;
			conflicted_residue.conflicted_score = 0.0;
			conflicted_residue.isConflicted = false;

			all_residues = target_protein.reactionSites;
			conflicted_residue = all_residues.stream().filter(value -> value.position == target_res_pos).findFirst()
					.get();
			conflicted_residue.conflicted_residue = null;
			conflicted_residue.conflicted_score = 0.0;
			conflicted_residue.isConflicted = false;

			if (myNetwork.toString().contains("#")) {

				String parent_network_str = myNetwork.toString().split("#")[0];

				Optional<CyNetwork> parent_network_optional = Util.myCyNetworkList.stream()
						.filter(value -> value.toString().equals(parent_network_str)).findFirst();

				if (parent_network_optional.isPresent()) {
					CyNetwork parent_network = parent_network_optional.get();

					CyNode source_node = Util.getNode(parent_network, source_protein.gene);
					if (source_node == null)
						return;

					Util.fillConflictedResiduesColumn(parent_network, source_protein, source_node);

				}
			}

		} catch (Exception e2) {
			// TODO: handle exception
		}

	}
}
