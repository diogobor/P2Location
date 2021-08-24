package de.fmp.liulab.task;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

/**
 * Class responsible for calling the class to expand the protein bar
 * 
 * @author diogobor
 *
 */
public class ProteinScalingFactorHorizontalExpansionTableTaskFactory extends AbstractTaskFactory {

	/**
	 * Empty constructor
	 */
	public ProteinScalingFactorHorizontalExpansionTableTaskFactory() {
	}

	/**
	 * Method responsible for calling the task
	 * 
	 * @param myNetwork                 current network
	 * @param forcedHorizontalExpansion Force horizontal expansion param as true
	 * @return return current task iterator
	 */
	public TaskIterator createTaskIterator(CyNetwork myNetwork, boolean forcedHorizontalExpansion) {
		return new TaskIterator(
				new ProteinScalingFactorHorizontalExpansionTableTask(myNetwork, forcedHorizontalExpansion));
	}

	@Override
	public TaskIterator createTaskIterator() {
		// TODO Auto-generated method stub
		return null;
	}
}
