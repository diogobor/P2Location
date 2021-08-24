package de.fmp.liulab.task.command_lines;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

public class ExportMonolinksCommandTaskFactory extends AbstractTaskFactory {

	public static final String DESCRIPTION = "Export monolinked peptides";
	public static final String LONG_DESCRIPTION = "Command responsible for exporting monolinked peptides.";

	private CyApplicationManager cyApplicationManager;

	/**
	 * Constructor
	 */
	public ExportMonolinksCommandTaskFactory(CyApplicationManager cyApplicationManager) {
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

		return new TaskIterator(new ExportMonolinksCommandTask(cyApplicationManager));
	}

}
