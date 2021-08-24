package de.fmp.liulab.task.command_lines;

import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

/**
 * Factory class responsible for calling set parameters task
 * 
 * @author diogobor
 *
 */
public class SetParametersCommandTaskFactory extends AbstractTaskFactory {

	public static final String DESCRIPTION = "Set P2Location parameters";
	public static final String LONG_DESCRIPTION = "Command responsible for setting all parameters to display and hide nodes/edges.";

	/**
	 * Constructor
	 */
	public SetParametersCommandTaskFactory() {
	}

	/**
	 * Check if the task is ready
	 */
	public boolean isReady() {
		return true;
	}

	/**
	 * Create task iterator
	 */
	public TaskIterator createTaskIterator() {

		return new TaskIterator(new SetParametersCommandTask());
	}
}
