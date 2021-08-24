package de.fmp.liulab.task.command_lines;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

/**
 * Factory class responsible for calling export protein domains task
 * 
 * @author diogobor
 *
 */
public class ExportProteinDomainsCommandTaskFactory extends AbstractTaskFactory {

	public static final String DESCRIPTION = "Export protein domains";
	public static final String LONG_DESCRIPTION = "Command responsible for exporting proteins domains.";

	private CyApplicationManager cyApplicationManager;

	/**
	 * Constructor
	 */
	public ExportProteinDomainsCommandTaskFactory(CyApplicationManager cyApplicationManager) {
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
