package de.fmp.liulab.internal.action;

import static java.awt.event.InputEvent.CTRL_DOWN_MASK;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

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
import de.fmp.liulab.model.Protein;
import de.fmp.liulab.model.ProteinDomain;
import de.fmp.liulab.model.Residue;
import de.fmp.liulab.utils.Util;

/**
 * Class responsible for exporting protein domains
 * 
 * @author diogobor
 *
 */
public class ExportProteinDomainsAction extends AbstractCyAction {

	private static final String MENU_NAME = "Export";
	private static final String MENU_CATEGORY = "Apps.P2Location.Protein location";
	private static final long serialVersionUID = 1L;
	private CyApplicationManager cyApplicationManager;
	private CyNetwork myNetwork;

	/**
	 * Constructor
	 * 
	 * @param cyApplicationManager main app manager
	 */
	public ExportProteinDomainsAction(CyApplicationManager cyApplicationManager) {
		super(MENU_NAME);
		setPreferredMenu(MENU_CATEGORY);
		setAcceleratorKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_D, CTRL_DOWN_MASK));
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
		if (!Util.proteinsMap.containsKey(myNetwork.toString())
				|| Util.proteinsMap.get(myNetwork.toString()).size() == 0) {
			msg = "<html><p>There is no protein domain(s) for the following network: <b>" + myNetwork.toString()
					+ "</b></p></html>";
			isEmpty = true;
		} else {
			msg = "<html><p><b>Selected network:</b></p><p>" + myNetwork.toString() + "</p></html>";
		}

		JOptionPane.showMessageDialog(null, msg, "P2Location - Export protein domains", JOptionPane.INFORMATION_MESSAGE,
				new ImageIcon(getClass().getResource("/images/logo.png")));

		if (isEmpty)
			return;

		JFrame parentFrame = new JFrame();
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setFileFilter(new ExtensionFileFilter("CSV file", "csv"));
		fileChooser.setDialogTitle("Save protein domains");

		int userSelection = fileChooser.showSaveDialog(parentFrame);

		if (userSelection == JFileChooser.APPROVE_OPTION) {
			File fileToSave = getSelectedFileWithExtension(fileChooser);
			String full_fileName = fileToSave.getAbsolutePath();
			if (!full_fileName.toLowerCase().endsWith(".csv")) {
				full_fileName += ".csv";
			}
			createProteinDomainsFile(full_fileName, myNetwork, null);
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
	 * Method responsible for creating the output file with all domains for the
	 * selected network
	 * 
	 * @param fileName  file name
	 * @param myNetwork current network
	 */
	public static void createProteinDomainsFile(String fileName, CyNetwork myNetwork, TaskMonitor taskMonitor) {
		try {

			if (Util.proteinsMap.containsKey(myNetwork.toString())) {

				FileWriter myWriter = new FileWriter(fileName);

				List<Protein> all_proteinDomains = Util.proteinsMap.get(myNetwork.toString());
				for (Protein protein : all_proteinDomains) {

					StringBuilder sb_conflicted_residues = new StringBuilder();
					StringBuilder sb_original_domains = new StringBuilder();
					StringBuilder sb_predicted_domains = new StringBuilder();

					if (protein.reactionSites != null) {
						for (Residue residue : protein.reactionSites) {
							sb_conflicted_residues.append(residue.aminoacid + "[" + residue.position + "],");
						}
					}

					if (protein.domains != null) {
						for (ProteinDomain domain : protein.domains) {
							if (!domain.isPredicted)
								sb_original_domains
										.append(domain.name + "[" + domain.startId + "-" + domain.endId + "],");
							else
								sb_predicted_domains
										.append(domain.name + "[" + domain.startId + "-" + domain.endId + "],");
						}
					}
					if (sb_original_domains.length() > 0 && sb_predicted_domains.length() > 0
							&& sb_conflicted_residues.length() > 0)// 3 columns
						myWriter.write(protein.gene + "," + "\""
								+ sb_original_domains
										.toString().substring(0, sb_original_domains.toString().length() - 1)
								+ "\"" + "," + "\""
								+ sb_predicted_domains.toString().substring(0,
										sb_predicted_domains.toString().length() - 1)
								+ "\"" + "," + "\"" + sb_conflicted_residues.toString().substring(0,
										sb_conflicted_residues.toString().length() - 1)
								+ "\"\n");
					else if (sb_original_domains.length() > 0 && sb_predicted_domains.length() > 0)// 1st and 2nd
						myWriter.write(protein.gene + "," + "\""
								+ sb_original_domains.toString().substring(0,
										sb_original_domains.toString().length() - 1)
								+ "\"" + "," + "\"" + sb_predicted_domains.toString().substring(0,
										sb_predicted_domains.toString().length() - 1)
								+ "\"\n");
					else if (sb_original_domains.length() > 0 && sb_conflicted_residues.length() > 0)// 1st and 3rd
						myWriter.write(protein.gene + "," + "\""
								+ sb_original_domains.toString().substring(0,
										sb_original_domains.toString().length() - 1)
								+ "\"" + ",," + "\"" + sb_conflicted_residues.toString().substring(0,
										sb_conflicted_residues.toString().length() - 1)
								+ "\"\n");
					else if (sb_predicted_domains.length() > 0 && sb_conflicted_residues.length() > 0)// 2nd and 3rd
						myWriter.write(protein.gene + ",," + "\""
								+ sb_predicted_domains.toString().substring(0,
										sb_predicted_domains.toString().length() - 1)
								+ "\"" + "," + "\"" + sb_conflicted_residues.toString().substring(0,
										sb_conflicted_residues.toString().length() - 1)
								+ "\"\n");
					else if (sb_original_domains.length() > 0)// 1st
						myWriter.write(protein.gene + "," + "\"" + sb_original_domains.toString().substring(0,
								sb_original_domains.toString().length() - 1) + "\"" + "\n");
					else if (sb_predicted_domains.length() > 0)// 2nd
						myWriter.write(protein.gene + ",," + "\"" + sb_predicted_domains.toString().substring(0,
								sb_predicted_domains.toString().length() - 1) + "\"" + "\n");
					else if (sb_conflicted_residues.length() > 0)// 3rd
						myWriter.write(protein.gene + ",,," + "\"" + sb_conflicted_residues.toString().substring(0,
								sb_conflicted_residues.toString().length() - 1) + "\"" + "\n");
				}

				myWriter.close();
				if (taskMonitor == null) {
					JOptionPane.showMessageDialog(null, "File has been saved successfully!",
							"P2Location - Export protein domains", JOptionPane.INFORMATION_MESSAGE);
				} else {
					taskMonitor.showMessage(TaskMonitor.Level.INFO, "File has been saved successfully!");
				}

			} else {// Network does not exists

				if (taskMonitor == null) {
					JOptionPane.showMessageDialog(null, "Network has not been found!",
							"P2Location - Export protein domains", JOptionPane.WARNING_MESSAGE);
				} else {
					taskMonitor.showMessage(TaskMonitor.Level.WARN, "Network has not been found!");
				}
				return;
			}

		} catch (IOException e) {

			if (taskMonitor == null) {
				String errorMsg = "<htmml><p>ERROR: It is not possible to save the file.</p><p>" + e.getMessage()
						+ "</p></html>";
				JOptionPane.showMessageDialog(null, errorMsg, "P2Location - Export protein domains",
						JOptionPane.ERROR_MESSAGE);
			} else {
				taskMonitor.showMessage(TaskMonitor.Level.ERROR, "ERROR: It is not possible to save the file.");
			}

		} catch (Exception e2) {
			if (taskMonitor == null) {
				String errorMsg = "<htmml><p>ERROR: It is not possible to save the file.</p><p>" + e2.getMessage()
						+ "</p></html>";
				JOptionPane.showMessageDialog(null, errorMsg, "P2Location - Export protein domains",
						JOptionPane.ERROR_MESSAGE);
			} else {
				taskMonitor.showMessage(TaskMonitor.Level.ERROR, "ERROR: It is not possible to save the file.");
			}
		}
	}
}
