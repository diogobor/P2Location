package de.fmp.liulab.task.command_lines;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

public class LoadMonolinksCommandTaskFactory extends AbstractTaskFactory {

	public static final String DESCRIPTION = "Load Monolinked peptides(s)";
	public static final String LONG_DESCRIPTION = "Command responsible for loading monolinked peptide(s).";

	private CyApplicationManager cyApplicationManager;

	/**
	 * Constructor
	 */
	public LoadMonolinksCommandTaskFactory(CyApplicationManager cyApplicationManager) {
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

		return new TaskIterator(new LoadMonolinksCommandTask(cyApplicationManager));
	}

}
