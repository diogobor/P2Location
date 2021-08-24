package de.fmp.liulab.task.command_lines;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

public class ExportPTMsCommandTaskFactory extends AbstractTaskFactory {

	public static final String DESCRIPTION = "Export post-translational modifications";
	public static final String LONG_DESCRIPTION = "Command responsible for exporting post-translational modifications.";

	private CyApplicationManager cyApplicationManager;

	/**
	 * Constructor
	 */
	public ExportPTMsCommandTaskFactory(CyApplicationManager cyApplicationManager) {
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

		return new TaskIterator(new ExportProteinDomainsCommandTask(cyApplicationManager));
	}


}
