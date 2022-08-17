package de.fmp.liulab.internal.action;

import static java.awt.event.InputEvent.CTRL_DOWN_MASK;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;

import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.swing.DialogTaskManager;

import de.fmp.liulab.task.ProcessProteinLocationTaskFactory;

/**
 * Class responsible for loading monolinks
 * 
 * @author borges.diogo
 *
 */
public class LoadProteinLocationAction extends AbstractCyAction {

	private static final String MENU_NAME = "Load";
	private static final String MENU_CATEGORY = "Apps.P2Location.Protein location";
	private static final long serialVersionUID = 1L;
	private DialogTaskManager dialogTaskManager;
	private ProcessProteinLocationTaskFactory myFactory;

	/**
	 * Constructor
	 * 
	 * @param dialogTaskManager task manager
	 * @param myFactory         main factory
	 */
	public LoadProteinLocationAction(DialogTaskManager dialogTaskManager, ProcessProteinLocationTaskFactory myFactory) {
		super(MENU_NAME);
		setPreferredMenu(MENU_CATEGORY);
		setAcceleratorKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_1, CTRL_DOWN_MASK));
		this.dialogTaskManager = dialogTaskManager;
		this.myFactory = myFactory;
	}

	/**
	 * Method responsible for activating action.
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		// Get the task iterator
		TaskIterator ti = myFactory.createTaskIterator(false, false, true);

		// Execute the task through the TaskManager
		dialogTaskManager.execute(ti);
	}

}
