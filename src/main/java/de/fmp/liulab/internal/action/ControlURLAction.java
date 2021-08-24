package de.fmp.liulab.internal.action;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.util.swing.OpenBrowser;

/**
 * Class responsible for the Browser action
 * 
 * @author diogobor
 *
 */
public class ControlURLAction extends AbstractCyAction {

	private static final String MENU_NAME = "About P2Location";
	private static final String MENU_CATEGORY = "Apps.P2Location";
	private static final long serialVersionUID = 1L;
	private OpenBrowser openBrowser;
	private String version;

	/**
	 * Constructor
	 * @param openBrowser open browser reference
	 * @param version version of the app
	 */
	public ControlURLAction(OpenBrowser openBrowser, String version) {
		super(MENU_NAME);
		setPreferredMenu(MENU_CATEGORY);
		setAcceleratorKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0));
		setMenuGravity(4.0f);
		this.openBrowser = openBrowser;
		this.version = version;
	}

	/**
	 * Method responsible for activating action.
	 */
	public void actionPerformed(ActionEvent e) {

		String msg = "<html><p><b>Developed by The Liu Lab:</b></p><p><b>Diogo Borges</b> (diogobor@gmail.com)</p><p><b>Ying Zhu</b> (zhu@fmp-berlin.de)</p><p><b>Fan Liu</b> (fliu@fmp-berlin.de)</p><br/><p><b>Version:</b> "
				+ version + "</p></html>";

		JOptionPane.showMessageDialog(null, msg, "P2Location - About", JOptionPane.INFORMATION_MESSAGE,
				new ImageIcon(getClass().getResource("/images/logo.png")));

		openBrowser.openURL("https://www.theliulab.com/");
	}
}
