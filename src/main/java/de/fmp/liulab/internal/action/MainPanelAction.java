package de.fmp.liulab.internal.action;

import java.awt.event.ActionEvent;

import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.application.swing.CytoPanelState;

import de.fmp.liulab.internal.MainControlPanel;

/**
 * Class responsible for creating main panel
 * @author diogobor
 *
 */
public class MainPanelAction extends AbstractCyAction {

	private static final String MENU_NAME = "Settings";
	private static final String MENU_CATEGORY = "Apps.P2Location";
	private static final long serialVersionUID = 1L;
	private CySwingApplication desktopApp;
	private final CytoPanel cytoPanelWest;
	private MainControlPanel myControlPanel;

	/**
	 * Constructor
	 * @param desktopApp swing application
	 * @param myCytoPanel main control panel
	 */
	public MainPanelAction(CySwingApplication desktopApp, MainControlPanel myCytoPanel) {
		super(MENU_NAME);
		setPreferredMenu(MENU_CATEGORY);
		setMenuGravity(3.0f);
		this.desktopApp = desktopApp;

		// Note: myControlPanel is bean we defined and registered as a service
		this.cytoPanelWest = this.desktopApp.getCytoPanel(CytoPanelName.WEST);
		this.myControlPanel = myCytoPanel;
	}

	/**
	 * Method responsible for activating action.
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		// If the state of the cytoPanelWest is HIDE, show it
		if (cytoPanelWest.getState() == CytoPanelState.HIDE) {
			cytoPanelWest.setState(CytoPanelState.DOCK);
		}

		// Select my panel
		int index = cytoPanelWest.indexOfComponent(myControlPanel);
		if (index == -1) {
			return;
		}
		cytoPanelWest.setSelectedIndex(index);
	}
}
