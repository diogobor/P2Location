package de.fmp.liulab.task.command_lines;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

/**
 * Factory class responsible for calling load protein domains task
 * 
 * @author diogobor
 *
 */
public class LoadProteinDomainsCommandTaskFactory extends AbstractTaskFactory {

	public static final String DESCRIPTION = "Load protein domains";
	public static final String LONG_DESCRIPTION = "Command responsible for loading proteins domains.";

	private CyApplicationManager cyApplicationManager;

	/**
	 * Constructor
	 */
	public LoadProteinDomainsCommandTaskFactory(CyApplicationManager cyApplicationManager) {
		this.cyApplicationManager = cyApplicationManager;
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

		return new TaskIterator(new LoadProteinDomainsCommandTask(cyApplicationManager));
	}

}
