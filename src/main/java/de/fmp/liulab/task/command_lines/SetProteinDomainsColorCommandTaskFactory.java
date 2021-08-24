package de.fmp.liulab.task.command_lines;

import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

/**
 * Factory class responsible for calling set protein domains color task
 * 
 * @author diogobor
 *
 */
public class SetProteinDomainsColorCommandTaskFactory extends AbstractTaskFactory {

	public static final String DESCRIPTION = "Set protein domains color";
	public static final String LONG_DESCRIPTION = "Command responsible for setting proteins domains color.";

	/**
	 * Constructor
	 */
	public SetProteinDomainsColorCommandTaskFactory() {
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

		return new TaskIterator(new SetProteinDomainsColorCommandTask());
	}

}
