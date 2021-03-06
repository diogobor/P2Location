package de.fmp.liulab.internal.action;

import static java.awt.event.InputEvent.CTRL_DOWN_MASK;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.work.TaskMonitor;

import de.fmp.liulab.internal.view.ExtensionFileFilter;
import de.fmp.liulab.model.CrossLink;
import de.fmp.liulab.model.Protein;
import de.fmp.liulab.utils.Util;

/**
 * Class responsible for exporting monolinked peptides
 * 
 * @author borges.diogo
 *
 */
public class ExportMonolinksAction extends AbstractCyAction {

	private static final String MENU_NAME = "Export";
	private static final String MENU_CATEGORY = "Apps.P2Location.Monolinked peptides";
	private static final long serialVersionUID = 1L;
	private CyApplicationManager cyApplicationManager;
	private CyNetwork myNetwork;

	/**
	 * Constructor
	 * 
	 * @param cyApplicationManager main app manager
	 */
	public ExportMonolinksAction(CyApplicationManager cyApplicationManager) {
		super(MENU_NAME);
		setPreferredMenu(MENU_CATEGORY);
		setAcceleratorKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_SLASH, CTRL_DOWN_MASK));
		this.cyApplicationManager = cyApplicationManager;
	}

	/**
	 * Method responsible for activating action.
	 */
	@Override
	public void actionPerformed(ActionEvent e) {

		this.myNetwork = cyApplicationManager.getCurrentNetwork();

		if (myNetwork == null) {
			return;
		}

		boolean isEmpty = false;
		String msg = "";
		if (!Util.monolinksMap.containsKey(myNetwork.toString())
				|| Util.monolinksMap.get(myNetwork.toString()).size() == 0) {
			msg = "<html><p>There is no monolinked peptides for the following network: <b>" + myNetwork.toString()
					+ "</b></p></html>";
			isEmpty = true;
		} else {
			msg = "<html><p><b>Selected network:</b></p><p>" + myNetwork.toString() + "</p></html>";
		}

		JOptionPane.showMessageDialog(null, msg, "P2Location - Export Monolinked Peptide(s)",
				JOptionPane.INFORMATION_MESSAGE, new ImageIcon(getClass().getResource("/images/logo.png")));

		if (isEmpty)
			return;

		JFrame parentFrame = new JFrame();
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setFileFilter(new ExtensionFileFilter("CSV file", "csv"));
		fileChooser.setDialogTitle("Save Monolinked Peptide(s)");

		int userSelection = fileChooser.showSaveDialog(parentFrame);

		if (userSelection == JFileChooser.APPROVE_OPTION) {
			File fileToSave = getSelectedFileWithExtension(fileChooser);
			String full_fileName = fileToSave.getAbsolutePath();
			if (!full_fileName.toLowerCase().endsWith(".csv")) {
				full_fileName += ".csv";
			}
			createMonolinksFile(full_fileName, myNetwork, null);
		}
	}

	/**
	 * Returns the selected file from a JFileChooser, including the extension from
	 * the file filter.
	 * 
	 * @param c file chooser reference
	 * @return return the file
	 */
	public static File getSelectedFileWithExtension(JFileChooser c) {
		File file = c.getSelectedFile();
		if (c.getFileFilter() instanceof FileNameExtensionFilter) {
			String[] exts = ((FileNameExtensionFilter) c.getFileFilter()).getExtensions();
			String nameLower = file.getName().toLowerCase();
			for (String ext : exts) { // check if it already has a valid extension
				if (nameLower.endsWith('.' + ext.toLowerCase())) {
					return file; // if yes, return as-is
				}
			}
			// if not, append the first extension from the selected filter
			file = new File(file.toString() + '.' + exts[0]);
		}
		return file;
	}

	/**
	 * Method responsible for creating the output file with all monolinks for the
	 * selected network
	 * 
	 * @param fileName  file name
	 * @param myNetwork current network
	 */
	public static void createMonolinksFile(String fileName, CyNetwork myNetwork, TaskMonitor taskMonitor) {
		try {

			if (Util.monolinksMap.containsKey(myNetwork.toString())) {

				FileWriter myWriter = new FileWriter(fileName);

				Map<Long, Protein> all_monolinks = Util.monolinksMap.get(myNetwork.toString());
				for (Map.Entry<Long, Protein> entry : all_monolinks.entrySet()) {

					String node_name = myNetwork.getDefaultNodeTable().getRow(entry.getKey()).getRaw(CyNetwork.NAME)
							.toString();

					Protein current_proteinWithMonolinks = entry.getValue();
					StringBuilder sb_monolinks = new StringBuilder();
//					for (CrossLink xl : current_proteinWithMonolinks.monolinks) {
//						sb_monolinks.append(xl.sequence + "[" + xl.pos_site_a + "-" + xl.pos_site_b + "],");
//					}
					myWriter.write(node_name + "," + current_proteinWithMonolinks.sequence + "," + "\""
							+ sb_monolinks.toString().substring(0, sb_monolinks.toString().length() - 1) + "\"\n");
				}

				myWriter.close();
				if (taskMonitor == null) {
					JOptionPane.showMessageDialog(null, "File has been saved successfully!",
							"P2Location - Export Monolinked Peptide(s)", JOptionPane.INFORMATION_MESSAGE);
				} else {
					taskMonitor.showMessage(TaskMonitor.Level.INFO, "File has been saved successfully!");
				}

			} else {// Network does not exists

				if (taskMonitor == null) {
					JOptionPane.showMessageDialog(null, "Network has not been found!",
							"P2Location - Export Monolinked Peptide(s)", JOptionPane.WARNING_MESSAGE);
				} else {
					taskMonitor.showMessage(TaskMonitor.Level.WARN, "Network has not been found!");
				}
				return;
			}

		} catch (IOException e) {

			if (taskMonitor == null) {
				String errorMsg = "<htmml><p>ERROR: It is not possible to save the file.</p><p>" + e.getMessage()
						+ "</p></html>";
				JOptionPane.showMessageDialog(null, errorMsg, "P2Location - Export Monolinked Peptide(s)",
						JOptionPane.ERROR_MESSAGE);
			} else {
				taskMonitor.showMessage(TaskMonitor.Level.ERROR, "ERROR: It is not possible to save the file.");
			}

		}
	}

}
