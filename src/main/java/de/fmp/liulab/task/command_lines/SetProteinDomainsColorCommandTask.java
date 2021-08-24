package de.fmp.liulab.task.command_lines;

import java.awt.Color;
import java.lang.reflect.Field;
import java.util.Map.Entry;

import org.cytoscape.work.ProvidesTitle;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;

import de.fmp.liulab.utils.Util;

/**
 * Class responsible for setting protein domains color via command line
 * 
 * @author diogobor
 *
 */
public class SetProteinDomainsColorCommandTask extends CyRESTAbstractTask {

	@ProvidesTitle
	public String getTitle() {
		return "Set protein domains color";
	}

	@Tunable(description = "Protein domain name", longDescription = "Name of the protein domain to change the color.", exampleStringValue = "PDE12")
	public String domainName = "";

	@Tunable(description = "Protein domain color", longDescription = "Set a color to a protein domain", exampleStringValue = "#AAAAAA")
	public String proteinDomainColor = null;

	/**
	 * Constructor
	 */
	public SetProteinDomainsColorCommandTask() {
	}

	/**
	 * Covert string to color
	 * 
	 * @param colorStr color string
	 * @return color
	 */
	private Color getColor(String colorStr) {
		Color _linksColor;
		try {
			_linksColor = Color.decode(colorStr);

		} catch (Exception e) {
			try {
				Field field = Class.forName("java.awt.Color").getField(colorStr);
				_linksColor = (Color) field.get(null);
			} catch (Exception e2) {
				_linksColor = null; // Not defined
			}
		}
		return _linksColor;
	}

	/**
	 * Run
	 */
	public void run(TaskMonitor taskMonitor) throws Exception {

		if (!(domainName.isBlank() || domainName.isEmpty())) {

			if (this.proteinDomainColor != null) {
				Color _linksColor = getColor(this.proteinDomainColor);

				if (_linksColor != null) {

					if (Util.proteinDomainsColorMap.containsKey(domainName)) {
						Color color = new Color(_linksColor.getRed(), _linksColor.getGreen(), _linksColor.getBlue(),
								100);

						Util.proteinDomainsColorMap.put(domainName, color);
					} else {
						taskMonitor.showMessage(TaskMonitor.Level.WARN, "Protein domain has not been found.");
					}

				}
			} else {
				taskMonitor.showMessage(TaskMonitor.Level.WARN, "No color has been defined.");
			}

		} else {

			listAllProteinDomains(taskMonitor);
		}
	}

	/**
	 * Get maximum length of all protein domains name
	 * 
	 * @return number
	 */
	private int getMaxLength() {

		int maxName_width = 0;
		if (Util.proteinDomainsColorMap != null && Util.proteinDomainsColorMap.size() > 0) {
			for (Entry<String, Color> domain : Util.proteinDomainsColorMap.entrySet()) {
				if (maxName_width < domain.getKey().length())
					maxName_width = domain.getKey().length();
			}
		}
		return maxName_width;
	}

	/**
	 * Get space for a specific domain to be printed in task monitor
	 * 
	 * @param maxName_width max protein domain length
	 * @param length        current length to be subtracted.
	 * @return number
	 */
	private String getSpace(int maxName_width, int length) {
		int currentLength = (maxName_width - length) / 2;
		StringBuilder sbTab = new StringBuilder();
		for (int i = 0; i < currentLength + 2; i++)
			sbTab.append("_ ");
		return sbTab.toString();
	}

	/**
	 * List all protein domains
	 * 
	 * @param taskMonitor task monitor
	 */
	private void listAllProteinDomains(TaskMonitor taskMonitor) {

		if (Util.proteinDomainsColorMap.size() > 0) {

			taskMonitor.showMessage(TaskMonitor.Level.INFO, "Protein domains:");

			int maxName_width = getMaxLength();
			taskMonitor.showMessage(TaskMonitor.Level.INFO,
					"Name " + getSpace(maxName_width, "Name".length()) + " Color (RGB alpha)");

			taskMonitor.showMessage(TaskMonitor.Level.INFO, "");

			for (Entry<String, Color> domain : Util.proteinDomainsColorMap.entrySet()) {

				Color current_color = domain.getValue();
				if (current_color != null) {
					String colorStr = current_color.getRed() + "#" + current_color.getGreen() + "#"
							+ current_color.getBlue() + "#" + current_color.getAlpha();
					taskMonitor.showMessage(TaskMonitor.Level.INFO,
							domain.getKey() + " " + getSpace(maxName_width, domain.getKey().length()) + " " + colorStr);

				} else {
					taskMonitor.showMessage(TaskMonitor.Level.INFO, domain.getKey() + " No color");
				}

			}
		} else {
			taskMonitor.showMessage(TaskMonitor.Level.WARN, "No protein domain has been found.");
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <R> R getResults(Class<? extends R> type) {
		return (R) "Done!";
	}

}
