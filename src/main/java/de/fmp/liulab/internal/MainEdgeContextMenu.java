package de.fmp.liulab.internal;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenuItem;

import org.cytoscape.application.swing.CyEdgeViewContextMenuFactory;
import org.cytoscape.application.swing.CyMenuItem;
import org.cytoscape.model.CyEdge;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.swing.DialogTaskManager;

/**
 * Class responsible for creating main edge context menu
 * 
 * @author diogobor
 *
 */
public class MainEdgeContextMenu implements CyEdgeViewContextMenuFactory, ActionListener {
	private TaskFactory myFactory;
	private DialogTaskManager dialogTaskManager;

	/**
	 * Constructor
	 * 
	 * @param myFactory         graphic instance
	 * @param dialogTaskManager task manager
	 */
	public MainEdgeContextMenu(TaskFactory myFactory, DialogTaskManager dialogTaskManager) {
		this.myFactory = myFactory;
		this.dialogTaskManager = dialogTaskManager;
	}

	/**
	 * Method responsible for creating main MenuItem
	 */
	@Override
	public CyMenuItem createMenuItem(CyNetworkView netView, View<CyEdge> edgeView) {
		JMenuItem menuItem = new JMenuItem("Visualize interactions in PyMOL");
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
