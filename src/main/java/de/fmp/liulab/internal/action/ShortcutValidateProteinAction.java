package de.fmp.liulab.internal.action;

import static java.awt.event.InputEvent.SHIFT_DOWN_MASK;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;

import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.swing.DialogTaskManager;

import de.fmp.liulab.task.UpdateProteinInformationTaskFactory;

public class ShortcutValidateProteinAction extends AbstractCyAction {

	private static final String MENU_NAME = "Validate protein(s)";
	private static final String MENU_CATEGORY = "Apps.P2Location";
	private static final long serialVersionUID = 1L;
	private DialogTaskManager dialogTaskManager;
	private UpdateProteinInformationTaskFactory myFactory;

	/**
	 * Constructor
	 * 
	 * @param dialogTaskManager task manager
	 * @param myFactory         main factory
	 */
	public ShortcutValidateProteinAction(DialogTaskManager dialogTaskManager,
			UpdateProteinInformationTaskFactory myFactory) {
		super(MENU_NAME);
		setPreferredMenu(MENU_CATEGORY);
		setMenuGravity(1.0f);
//		insertSeparatorAfter = true;
		setAcceleratorKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_V, SHIFT_DOWN_MASK));
		this.dialogTaskManager = dialogTaskManager;
		this.myFactory = myFactory;
	}

	/**
	 * Method responsible for activating action.
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		// Get the task iterator
		TaskIterator ti = myFactory.createTaskIterator(true);

		// Execute the task through the TaskManager
		dialogTaskManager.execute(ti);
	}
}
