package de.fmp.liulab.task.command_lines;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.swing.DialogTaskManager;

import de.fmp.liulab.task.MainSingleNodeTaskFactory;

/**
 * Factory class responsible for calling apply/restore style task
 * @author diogobor
 *
 */
public class ApplyRestoreStyleCommandTaskFactory extends AbstractTaskFactory {

	public static final String DESCRIPTION = "Apply/Restore style to a node";
	public static final String LONG_DESCRIPTION = "Command responsible for expanding or restoring a node to display or hide all identified link as well as protein domains.";

	private CyApplicationManager cyApplicationManager;
	private MainSingleNodeTaskFactory myFactory;
	private DialogTaskManager dialogTaskManager;

	/**
	 * Constructor
	 * 
	 * @param cyApplicationManager main app manager
	 * @param myFactory            main factory
	 * @param dialogTaskManager    task manager
	 */
	public ApplyRestoreStyleCommandTaskFactory(CyApplicationManager cyApplicationManager,
			MainSingleNodeTaskFactory myFactory, DialogTaskManager dialogTaskManager) {
		this.cyApplicationManager = cyApplicationManager;
		this.myFactory = myFactory;
		this.dialogTaskManager = dialogTaskManager;
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

		return new TaskIterator(new ApplyRestoreStyleCommandTask(cyApplicationManager, myFactory, dialogTaskManager));
	}

}
