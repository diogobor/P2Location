package de.fmp.liulab.task.command_lines;

import java.awt.Color;
import java.lang.reflect.Field;

import org.cytoscape.work.ProvidesTitle;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;

import de.fmp.liulab.internal.MainControlPanel;
import de.fmp.liulab.utils.Util;

/**
 * Class responsible for setting P2Location parameters via command line
 * 
 * @author diogobor
 *
 */
public class SetParametersCommandTask extends CyRESTAbstractTask {

	@ProvidesTitle
	public String getTitle() {
		return "Set P2Location parameters";
	}

	@Tunable(description = "Display intralinks", longDescription = "Display or hide all identified intralinks", exampleStringValue = "true")
	public boolean displayIntralinks = Util.showIntraLinks;

	@Tunable(description = "Intralinks color", longDescription = "Set a color to all identified intralinks", exampleStringValue = "#FF0000 or FF0000 or red")
	public String intralinksColor = null;

	@Tunable(description = "Display interlinks", longDescription = "Display or hide all identified interlinks", exampleStringValue = "true")
	public boolean displayInterlinks = Util.showInterLinks;

	@Tunable(description = "Interlinks color", longDescription = "Set a color to all identified interlinks", exampleStringValue = "#FF0000 or FF0000 or red")
	public String interlinksColor = null;

	@Tunable(description = "Display peptides", longDescription = "Display or hide all identified intralinked peptides", exampleStringValue = "true")
	public boolean displayMonolinks = Util.showMonolinkedPeptides;

	@Tunable(description = "Peptides color", longDescription = "Set a color to all identified intralinked peptides", exampleStringValue = "#FF0000 or FF0000 or red")
	public String MonoLinksPeptideColor = null;

	@Tunable(description = "Display post-translational modifications", longDescription = "Display or hide all PTM(s)", exampleStringValue = "true")
	public boolean displayPTM = Util.showPTMs;

	@Tunable(description = "PTM color", longDescription = "Set a color to all post-translational modifications", exampleStringValue = "#FF0000 or FF0000 or red")
	public String PTMColor = null;

	@Tunable(description = "Set opacity of cross-links", longDescription = "Set the opacity of all identified cross-links (range between 0 - transparent and 255 - opaque)", exampleStringValue = "120")
	public Integer opacityLinks = Util.edge_link_opacity;

	@Tunable(description = "Set width of cross-links", longDescription = "Set the width of all identified cross-links (range between 1 and 10)", exampleStringValue = "3")
	public double widthLinks = Util.edge_link_width;

	@Tunable(description = "Display cross-links legend", longDescription = "Display or hide the legends of all identified cross-links", exampleStringValue = "true")
	public boolean displayLinksLegend = false;

	@Tunable(description = "Set font size of cross-links legend", longDescription = "Set the font size of the legend of all identified cross-links", exampleStringValue = "12")
	public Integer fontSizeLinksLegend = Util.edge_label_font_size;

	@Tunable(description = "Set opacity of cross-links legend", longDescription = "Set the opacity of the legend of all identified cross-links (range between 0 - transparent and 255 - opaque)", exampleStringValue = "120")
	public Integer opacityLinksLegend = Util.edge_label_opacity;

	@Tunable(description = "Set the threshold -log(score) to intralinks.", longDescription = "Set the threshold score to intralinks. All intralinks that have a -log(score) above the threshold will be displayed.", exampleStringValue = "20")
	public double scoreIntralink = 0.0;

	@Tunable(description = "Set the threshold -log(score) to interlinks.", longDescription = "Set the threshold score to interlinks. All interlinks that have a -log(score) above the threshold will be displayed.", exampleStringValue = "20")
	public double scoreInterlink = 0.0;

	@Tunable(description = "Set the threshold -log(score) to PPI links.", longDescription = "Set the threshold score to PPI links. All PPI links that have a -log(score) above the threshold will be displayed.", exampleStringValue = "20")
	public double scorePPIlink = Integer.MIN_VALUE;

	@Tunable(description = "Set font size of nodes name", longDescription = "Set the font size of the name of all nodes", exampleStringValue = "PDE12")
	public Integer fontSizeNodesName = Util.node_label_font_size;

	@Tunable(description = "Node border color", longDescription = "Set a color to all nodes borders", exampleStringValue = "#FF0000 or FF0000 or red")
	public String nodeBorderColor = null;

	@Tunable(description = "Set opacity of border nodes", longDescription = "Set the opacity of the border of all nodes (range between 0 - transparent and 255 - opaque)", exampleStringValue = "120")
	public Integer opacityBorderNodes = Util.node_border_opacity;

	@Tunable(description = "Set width of cross-links", longDescription = "Set the width of the border of all nodes (range between 1 and 10)", exampleStringValue = "3")
	public double widthBorderNodes = Util.node_border_width;

	/**
	 * Constructor
	 */
	public SetParametersCommandTask() {

	}

	/**
	 * Add "#" in the beginning of the string
	 * 
	 * @param value current string
	 * @return new string with "#"
	 */
	private String addCharp(String value) {
		boolean isValid = true;
		byte[] tmp = value.getBytes();
		for (int i = 0; i < 6; i++) {
			if (tmp[i] < 48 || tmp[i] > 70) {
				isValid = false;
				break;
			}
		}
		if (isValid) {
			value = "#" + value;
		}
		return value;
	}

	/**
	 * Run
	 */
	public void run(TaskMonitor taskMonitor) throws Exception {

		Util.showIntraLinks = this.displayIntralinks;
		Util.showInterLinks = this.displayInterlinks;
		Util.showMonolinkedPeptides = this.displayMonolinks;
		Util.showPTMs = this.displayPTM;

		Color _linksColor;
		if (this.intralinksColor != null) {

			if (this.intralinksColor.length() == 6 && !this.intralinksColor.startsWith("#")) {
				this.intralinksColor = addCharp(this.intralinksColor);
			}

			try {
				_linksColor = Color.decode(this.intralinksColor);

			} catch (Exception e) {
				try {
					Field field = Class.forName("java.awt.Color").getField(this.intralinksColor);
					_linksColor = (Color) field.get(null);
				} catch (Exception e2) {
					_linksColor = null; // Not defined
				}
			}
			if (_linksColor != null)
				Util.IntraLinksColor = _linksColor;
		}

		if (this.interlinksColor != null) {

			if (this.interlinksColor.length() == 6 && !this.interlinksColor.startsWith("#")) {
				this.interlinksColor = addCharp(this.interlinksColor);
			}

			try {
				_linksColor = Color.decode(this.interlinksColor);
			} catch (Exception e) {
				try {
					Field field = Class.forName("java.awt.Color").getField(this.interlinksColor);
					_linksColor = (Color) field.get(null);
				} catch (Exception e2) {
					_linksColor = null; // Not defined
				}
			}

			if (_linksColor != null)
				Util.InterLinksColor = _linksColor;
		}
		
		if (this.MonoLinksPeptideColor != null) {

			if (this.MonoLinksPeptideColor.length() == 6 && !this.MonoLinksPeptideColor.startsWith("#")) {
				this.MonoLinksPeptideColor = addCharp(this.MonoLinksPeptideColor);
			}

			try {
				_linksColor = Color.decode(this.MonoLinksPeptideColor);
			} catch (Exception e) {
				try {
					Field field = Class.forName("java.awt.Color").getField(this.MonoLinksPeptideColor);
					_linksColor = (Color) field.get(null);
				} catch (Exception e2) {
					_linksColor = null; // Not defined
				}
			}

			if (_linksColor != null)
				Util.MonoLinksPeptideColor = _linksColor;
		}
		
		if (this.PTMColor != null) {

			if (this.PTMColor.length() == 6 && !this.PTMColor.startsWith("#")) {
				this.PTMColor = addCharp(this.PTMColor);
			}

			try {
				_linksColor = Color.decode(this.PTMColor);
			} catch (Exception e) {
				try {
					Field field = Class.forName("java.awt.Color").getField(this.PTMColor);
					_linksColor = (Color) field.get(null);
				} catch (Exception e2) {
					_linksColor = null; // Not defined
				}
			}

			if (_linksColor != null)
				Util.PTMColor = _linksColor;
		}

		if (this.opacityLinks > 255)
			Util.edge_link_opacity = 255;
		else if (this.opacityLinks < 0)
			Util.edge_link_opacity = 0;
		else
			Util.edge_link_opacity = this.opacityLinks;

		if (this.widthLinks > 10)
			Util.edge_link_width = 10;
		else if (this.widthLinks < 0)
			Util.edge_link_width = 0;
		else
			Util.edge_link_width = this.widthLinks;
		Util.showLinksLegend = this.displayLinksLegend;
		Util.edge_label_font_size = this.fontSizeLinksLegend;

		if (this.opacityLinksLegend > 255)
			Util.edge_label_opacity = 255;
		else if (this.opacityLinksLegend < 0)
			Util.edge_label_opacity = 0;
		else
			Util.edge_label_opacity = this.opacityLinksLegend;
		Util.intralink_threshold_score = this.scoreIntralink;
		Util.interlink_threshold_score = this.scoreInterlink;
		Util.combinedlink_threshold_score = this.scorePPIlink;
		Util.node_label_font_size = this.fontSizeNodesName;

		if (this.nodeBorderColor != null) {

			if (this.nodeBorderColor.length() == 6 && !this.nodeBorderColor.startsWith("#")) {
				this.nodeBorderColor = addCharp(this.nodeBorderColor);
			}

			try {
				_linksColor = Color.decode(this.nodeBorderColor);
			} catch (Exception e) {
				try {
					Field field = Class.forName("java.awt.Color").getField(this.nodeBorderColor);
					_linksColor = (Color) field.get(null);
				} catch (Exception e2) {
					_linksColor = null; // Not defined
				}
			}

			if (_linksColor != null)
				Util.NodeBorderColor = _linksColor;
		}

		if (this.opacityBorderNodes > 255)
			Util.node_border_opacity = 255;
		else if (this.opacityBorderNodes < 0)
			Util.node_border_opacity = 0;
		else
			Util.node_border_opacity = this.opacityBorderNodes;

		if (this.widthBorderNodes > 10)
			Util.node_border_width = 10;
		else if (this.widthBorderNodes < 0)
			Util.node_border_width = 0;
		else
			Util.node_border_width = this.widthBorderNodes;

		MainControlPanel.updateParamsValue();

	}

	@SuppressWarnings("unchecked")
	@Override
	public <R> R getResults(Class<? extends R> type) {
		return (R) "Done!";
	}
}
