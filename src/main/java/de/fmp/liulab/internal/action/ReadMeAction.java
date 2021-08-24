package de.fmp.liulab.internal.action;

import java.awt.event.ActionEvent;

import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.util.swing.OpenBrowser;

/**
 * Class responsible for open Protocol Exchange in the Browser
 * @author diogobor
 *
 */
public class ReadMeAction extends AbstractCyAction {

	private static final String MENU_NAME = "Read Me";
	private static final String MENU_CATEGORY = "Apps.P2Location";
	private static final long serialVersionUID = 1L;
	private OpenBrowser openBrowser;

	/**
	 * Constructor
	 * 
	 * @param openBrowser open browser reference
	 */
	public ReadMeAction(OpenBrowser openBrowser) {
		super(MENU_NAME);
		setPreferredMenu(MENU_CATEGORY);
		setMenuGravity(4.0f);
		this.openBrowser = openBrowser;
	}

	/**
	 * Method responsible for activating action.
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		openBrowser.openURL("http://dx.doi.org/10.21203/rs.3.pex-1172/v1");

	}

}
