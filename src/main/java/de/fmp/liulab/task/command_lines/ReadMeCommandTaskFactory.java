package de.fmp.liulab.task.command_lines;

import org.cytoscape.util.swing.OpenBrowser;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

/**
 * Factory class responsible for calling read me task
 * 
 * @author diogobor
 *
 */
public class ReadMeCommandTaskFactory extends AbstractTaskFactory {

	public static final String DESCRIPTION = "Read Me";
	public static final String LONG_DESCRIPTION = "Command responsible for opening the Protocol Exchange that explains step-by-step how P2Location works.";

	private OpenBrowser openBrowser;

	/**
	 * Constructor
	 * 
	 * @param openBrowser browser
	 */
	public ReadMeCommandTaskFactory(OpenBrowser openBrowser) {
		this.openBrowser = openBrowser;
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

		return new TaskIterator(new ReadMeCommandTask(openBrowser));
	}
}
