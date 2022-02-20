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
import de.fmp.liulab.model.CrossLink;
import de.fmp.liulab.model.Protein;
import de.fmp.liulab.model.ProteinDomain;
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
			msg = "<html><p>There are no protein domain(s) for the following network: <b>" + myNetwork.toString()
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

				FileWriter myWriterSQL = new FileWriter(
						fileName.toString().substring(0, fileName.toString().length() - 3) + "sql");
				FileWriter myWriter = new FileWriter(fileName);
				myWriter.write("Accession Number, Sequence, Description, Domains, Crosslinks, Subcellular Location\n");

//				myWriter.write("Protein,#intralinks,#interlinks,#total\n");
				List<Protein> all_proteinDomains = Util.proteinsMap.get(myNetwork.toString());
//				for (Protein protein : all_proteinDomains) {
//					myWriter.write(protein.gene + "," + protein.intraLinks.size() + "," + protein.interLinks.size()
//							+ "," + (int)(protein.intraLinks.size() + protein.interLinks.size()) + "\n");
//				}
				for (Protein protein : all_proteinDomains) {

					StringBuilder sb_original_domains = new StringBuilder();

					if (protein.domains != null && protein.domains.size() > 0) {
						for (ProteinDomain domain : protein.domains) {
							String domain_clean = domain.name.replaceAll("'", "").replaceAll(",", "_");
							sb_original_domains.append(domain_clean + "[" + domain.startId + "-" + domain.endId + "],");
						}
					} else {
						sb_original_domains.append(",");
					}

					StringBuilder sb_links = new StringBuilder();

					if (protein.intraLinks != null) {
						for (CrossLink link : protein.intraLinks) {
							sb_links.append(link.protein_a + "-" + link.pos_site_a + "-" + link.protein_a + "-"
									+ link.pos_site_b + "#");
						}
					}

					if (protein.interLinks != null) {
						for (CrossLink link : protein.interLinks) {
							sb_links.append(link.protein_a + "-" + link.pos_site_a + "-" + link.protein_b + "-"
									+ link.pos_site_b + "#");
						}
					}

					String domainsStr = sb_original_domains.toString().substring(0,
							sb_original_domains.toString().length() - 1);
					String linksStr = sb_links.toString().length() > 0
							? sb_links.toString().substring(0, sb_links.toString().length() - 1)
							: "";
					
					String description_clean = protein.fullName.replaceAll("'", "").replaceAll(",", "_");
					myWriter.write(protein.proteinID + "," + protein.sequence + "," + protein.fullName + ","
							+ domainsStr + "," + linksStr + "," + protein.location + "\n");

					myWriterSQL.write(
							"INSERT INTO protein (accession_number, sequence, description, domains, crosslinks, subcellular_location) VALUES ('"
									+ protein.proteinID + "', '" + protein.sequence + "', '" + description_clean + "', '"
									+ domainsStr + "', '" + linksStr + "', '" + protein.location + "');\n");
				}

				myWriter.close();
				myWriterSQL.close();
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
				String errorMsg = "<html><p>ERROR: It is not possible to save the file.</p><p>" + e.getMessage()
						+ "</p></html>";
				JOptionPane.showMessageDialog(null, errorMsg, "P2Location - Export protein domains",
						JOptionPane.ERROR_MESSAGE);
			} else {
				taskMonitor.showMessage(TaskMonitor.Level.ERROR, "ERROR: It is not possible to save the file.");
			}

		} catch (Exception e2) {
			if (taskMonitor == null) {
				String errorMsg = "<html><p>ERROR: It is not possible to save the file.</p><p>" + e2.getMessage()
						+ "</p></html>";
				JOptionPane.showMessageDialog(null, errorMsg, "P2Location - Export protein domains",
						JOptionPane.ERROR_MESSAGE);
			} else {
				taskMonitor.showMessage(TaskMonitor.Level.ERROR, "ERROR: It is not possible to save the file.");
			}
		}
	}
}
