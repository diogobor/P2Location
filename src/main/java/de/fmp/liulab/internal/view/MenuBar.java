package de.fmp.liulab.internal.view;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import de.fmp.liulab.parser.Parser;

/**
 * Class responsible for creating menu on Set of Nodes window
 * 
 * @author borges.diogo
 *
 */
public class MenuBar implements ActionListener {

	public int domain_ptm_or_monolink = 0;
	private JMenuBar menuBar = new JMenuBar();
	public static JFileChooser chooseNetwork = null;
	private Parser parserFile;

	// File menu
	private JMenu fileMenu = new JMenu();
	private JMenuItem importNetwork = new JMenuItem();

	/**
	 * Method responsible for activating action.
	 */
	@Override
	public void actionPerformed(ActionEvent evt) {
		Object source = evt.getSource();

		if (source == importNetwork) {
			chooseNetwork = new JFileChooser();
			if (domain_ptm_or_monolink == 0)
				chooseNetwork.setFileFilter(new ExtensionFileFilter("Uniprot file (*.tab)", "tab"));
			chooseNetwork.setFileFilter(new ExtensionFileFilter("CSV file (*.csv)", "csv"));
			chooseNetwork.setFileSelectionMode(JFileChooser.FILES_ONLY);
			chooseNetwork.setDialogTitle("Import file");

			if (chooseNetwork.showOpenDialog(chooseNetwork) != JFileChooser.APPROVE_OPTION)
				return;

			parserFile = new Parser(chooseNetwork.getSelectedFile().toString());
			try {
				parserFile.updateDataModel(domain_ptm_or_monolink);
			} catch (Exception e) {
				JOptionPane.showMessageDialog(null, e.getMessage(), "P2Location - Error", JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	/**
	 * Get current menu bar
	 * 
	 * @return menu bar
	 */
	public JMenuBar getMenuBar() {
		menuBar.setVisible(true);
		menuBar.add(getFileMenu());
		return menuBar;
	}

	public void setEnabled(boolean b) {
		fileMenu.setEnabled(b);
	}

	/**
	 * Creates main menu
	 * 
	 * @return current menu
	 */
	protected JMenu getFileMenu() {
		fileMenu.setMnemonic('F');
		fileMenu.setText("File");
		fileMenu.add(getSubMenuImportNetwork());

		return fileMenu;
	}

	/**
	 * Get current subMenu item: 'Import file'
	 * 
	 * @return current subMenu item
	 */
	protected JMenuItem getSubMenuImportNetwork() {
		importNetwork.setMnemonic('I');
		importNetwork.setText("Import file");
		importNetwork.addActionListener(this);

		return importNetwork;
	}
}
