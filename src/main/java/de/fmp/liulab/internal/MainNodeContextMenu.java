package de.fmp.liulab.internal;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenuItem;

import org.cytoscape.application.swing.CyMenuItem;
import org.cytoscape.application.swing.CyNodeViewContextMenuFactory;
import org.cytoscape.model.CyNode;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.swing.DialogTaskManager;

/**
 * Class responsible for creating main node context menu
 * @author diogobor
 *
 */
public class MainNodeContextMenu implements CyNodeViewContextMenuFactory, ActionListener {
	private TaskFactory myFactory;
	private DialogTaskManager dialogTaskManager;

	/**
	 * Constructor
	 * @param myFactory graphic instance
	 * @param dialogTaskManager task manager
	 */
	public MainNodeContextMenu(TaskFactory myFactory, DialogTaskManager dialogTaskManager) {
		this.myFactory = myFactory;
		this.dialogTaskManager = dialogTaskManager;
	}

	/**
	 * Method responsible for creating main MenuItem
	 */
	public CyMenuItem createMenuItem(CyNetworkView netView, View<CyNode> nodeView) {
		JMenuItem menuItem = new JMenuItem("P2Location");
		menuItem.addActionListener(this);

		CyMenuItem cyMenuItem = new CyMenuItem(menuItem, 0);
		return cyMenuItem;
	}

	/**
	 * The task class will be instantiated here.
	 */
	public void actionPerformed(ActionEvent e) {

		// Get the task iterator
		TaskIterator ti = myFactory.createTaskIterator();

		// Execute the task through the TaskManager
		dialogTaskManager.execute(ti);
	}
}