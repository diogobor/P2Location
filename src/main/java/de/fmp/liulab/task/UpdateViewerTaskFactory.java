package de.fmp.liulab.task;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.presentation.property.values.BendFactory;
import org.cytoscape.view.presentation.property.values.HandleFactory;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

/**
 * Class responsible for calling the task for updating edges
 * @author diogobor
 *
 */
public class UpdateViewerTaskFactory extends AbstractTaskFactory {

	/**
	 * Constructor
	 */
	public UpdateViewerTaskFactory() {

	}

	/**
	 * Create iterator
	 * 
	 * @param cyApplicationManager main app manager
	 * @param handleFactory handle factory
	 * @param bendFactory bend factory
	 * @param myNetwork current network
	 * @param netView current network view
	 * @param node current node
	 * @return task iterator
	 */
	public TaskIterator createTaskIterator(CyApplicationManager cyApplicationManager, HandleFactory handleFactory,
			BendFactory bendFactory, CyNetwork myNetwork, CyNetworkView netView, CyNode node) {

		return new TaskIterator(new UpdateViewerTask(cyApplicationManager, handleFactory, bendFactory, myNetwork,
				netView, node));
	}

	@Override
	public TaskIterator createTaskIterator() {
		// TODO Auto-generated method stub
		return null;
	}

}
