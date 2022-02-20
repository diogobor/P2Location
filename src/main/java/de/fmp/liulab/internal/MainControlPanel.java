package de.fmp.liulab.internal;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.DefaultFormatter;

import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.VisualLexicon;
import org.cytoscape.view.presentation.property.values.BendFactory;
import org.cytoscape.view.presentation.property.values.HandleFactory;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.work.FinishStatus;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskObserver;
import org.cytoscape.work.swing.DialogTaskManager;

import de.fmp.liulab.core.ConfigurationManager;
import de.fmp.liulab.task.ProcessProteinLocationTask;
import de.fmp.liulab.task.ProcessProteinLocationTaskFactory;
import de.fmp.liulab.task.ProcessTransmemRegionsTaskFactory;
import de.fmp.liulab.utils.Util;

/**
 * Class responsible for controlling all layout parameters
 * 
 * @author diogobor
 *
 */
public class MainControlPanel extends JPanel implements CytoPanelComponent {

	private ProcessProteinLocationTaskFactory processProteinLocationTaskFactory;
	private ProcessTransmemRegionsTaskFactory processTransmemRegionsTaskFactory;
	private TaskFactory singleNodeTaskFactory;
	private DialogTaskManager dialogTaskManager;

	private static final int WIDTH_PANEL = 320;
	private static final long serialVersionUID = 8292806967891823933L;
	private JPanel link_panel;
	private JPanel filter_panel;
	private JPanel option_panel;
	private JPanel prediction_panel;
	private JPanel protein_domains_prediction_panel;
	private JPanel transmem_regions_prediction_panel;
	private JPanel display_prediction_panel;
	private JPanel fix_conflict_panel;
	private JPanel node_panel;
	private JPanel node_border_panel;
	private JPanel pymol_panel;

	private static JButton processButton;
	private static JButton processTransmemButton;
	private static JButton interLinkColorButton;
	private static JButton ptmColorButton;
	private static JButton monolinkColorButton;
	private static JButton borderNodeColorButton;
	private static JCheckBox enable_epochs;
	private static JCheckBox consider_domain_whole_ptn;
	private static JCheckBox show_intra_link;
	private static JCheckBox enable_score;
	private static JCheckBox enable_specCount;
	private static JCheckBox enable_conflict;
	private static JCheckBox show_monolinks;

	private static JSpinner spinner_font_size_link_legend;
	private static JSpinner spinner_opacity_edge_label;
	private static JSpinner spinner_opacity_edge_link;
	private static JSpinner spinner_epochs;
	private static JSpinner spinner_score;
	private static JSpinner spinner_specCount;
	private static JSpinner spinner_neighborAA;
	private static JSpinner spinner_transmem_neighborAA;
	private static JSpinner spinner_width_edge_link;
	private static JSpinner spinner_score_intralink;
	private static JSpinner spinner_score_interlink;
	private static JSpinner spinner_score_combinedlink;
	private static JSpinner spinner_font_size_node;
	private static JSpinner spinner_opacity_node_border;
	private static JSpinner spinner_width_node_border;
	private static JSpinner spinner_deltaScore;
	private static JSpinner spinner_transmemUpperCutoffScore;
	private static JSpinner spinner_transmemLowerCutoffScore;

	private static JComboBox epochCombobox;
	private static Integer current_epoch;
	private static JButton display_epochButton;

	private static JLabel pymolPathStr;
	private static JCheckBox show_links_legend;

	private Properties P2LocationProps;

	// Update nodes and edges
	public static CyNetwork myNetwork;
	public static CyNetworkView netView;
	public static VisualStyle style;
	public static HandleFactory handleFactory;
	public static BendFactory bendFactory;
	public static VisualLexicon lexicon;

	/**
	 * Constructor
	 * 
	 * @param P2LocationProps setting properties
	 * @param cm              configuration manager
	 */
	public MainControlPanel(Properties P2LocationProps, ConfigurationManager cm,
			ProcessProteinLocationTaskFactory processProteinLocationTaskFactory,
			ProcessTransmemRegionsTaskFactory processTransmemRegionsTaskFactory, TaskFactory mainSingleNodeTaskFactory,
			DialogTaskManager dialogTaskManager) {

		this.processProteinLocationTaskFactory = processProteinLocationTaskFactory;
		this.processTransmemRegionsTaskFactory = processTransmemRegionsTaskFactory;
		this.singleNodeTaskFactory = mainSingleNodeTaskFactory;
		this.dialogTaskManager = dialogTaskManager;

		this.P2LocationProps = P2LocationProps;
		this.load_default_parameters(cm);
		this.setFrameObjects();
		this.setVisible(true);
	}

	/**
	 * Load default parameters to main panel
	 * 
	 * @param bc main context
	 * @param cm configuration manager
	 */
	private void load_default_parameters(ConfigurationManager cm) {

		String propertyValue = "";
		if (cm == null)
			propertyValue = ((Properties) P2LocationProps).getProperty("p2location.epochs");
		else
			propertyValue = cm.getProperties().getProperty("p2location.epochs");
		Util.epochs = Integer.parseInt(propertyValue);

		if (cm == null)
			propertyValue = ((Properties) P2LocationProps).getProperty("p2location.getEpochs");
		else
			propertyValue = cm.getProperties().getProperty("p2location.getEpochs");
		Util.getEpochs = Boolean.parseBoolean(propertyValue);

		if (cm == null)
			propertyValue = ((Properties) P2LocationProps).getProperty("p2location.threshold_score");
		else
			propertyValue = cm.getProperties().getProperty("p2location.threshold_score");
		Util.threshold_score = Integer.parseInt(propertyValue);

		if (cm == null)
			propertyValue = ((Properties) P2LocationProps).getProperty("p2location.getThreshold_score");
		else
			propertyValue = cm.getProperties().getProperty("p2location.getThreshold_score");
		Util.getThreshold_score = Boolean.parseBoolean(propertyValue);

		if (cm == null)
			propertyValue = ((Properties) P2LocationProps).getProperty("p2location.specCount");
		else
			propertyValue = cm.getProperties().getProperty("p2location.specCount");
		Util.specCount = Integer.parseInt(propertyValue);

		if (cm == null)
			propertyValue = ((Properties) P2LocationProps).getProperty("p2location.getSpecCount");
		else
			propertyValue = cm.getProperties().getProperty("p2location.getSpecCount");
		Util.getSpecCount = Boolean.parseBoolean(propertyValue);

		if (cm == null)
			propertyValue = ((Properties) P2LocationProps).getProperty("p2location.considerConflict");
		else
			propertyValue = cm.getProperties().getProperty("p2location.considerConflict");
		Util.considerConflict = Boolean.parseBoolean(propertyValue);

		if (cm == null)
			propertyValue = ((Properties) P2LocationProps).getProperty("p2location.showLinksLegend");
		else
			propertyValue = cm.getProperties().getProperty("p2location.showLinksLegend");
		Util.showLinksLegend = Boolean.parseBoolean(propertyValue);

		if (cm == null)
			propertyValue = ((Properties) P2LocationProps).getProperty("p2location.showIntraLinks");
		else
			propertyValue = cm.getProperties().getProperty("p2location.showIntraLinks");
		Util.showIntraLinks = Boolean.parseBoolean(propertyValue);

		if (cm == null)
			propertyValue = ((Properties) P2LocationProps).getProperty("p2location.showInterLinks");
		else
			propertyValue = cm.getProperties().getProperty("p2location.showInterLinks");
		Util.showInterLinks = Boolean.parseBoolean(propertyValue);

		if (cm == null)
			propertyValue = ((Properties) P2LocationProps).getProperty("p2location.edge_label_font_size");
		else
			propertyValue = cm.getProperties().getProperty("p2location.edge_label_font_size");
		Util.edge_label_font_size = Integer.parseInt(propertyValue);

		if (cm == null)
			propertyValue = ((Properties) P2LocationProps).getProperty("p2location.node_label_font_size");
		else
			propertyValue = cm.getProperties().getProperty("p2location.node_label_font_size");
		Util.node_label_font_size = Integer.parseInt(propertyValue);

		if (cm == null)
			propertyValue = ((Properties) P2LocationProps).getProperty("p2location.edge_label_opacity");
		else
			propertyValue = cm.getProperties().getProperty("p2location.edge_label_opacity");
		Util.edge_label_opacity = Integer.parseInt(propertyValue);

		if (cm == null)
			propertyValue = ((Properties) P2LocationProps).getProperty("p2location.edge_link_opacity");
		else
			propertyValue = cm.getProperties().getProperty("p2location.edge_link_opacity");
		Util.edge_link_opacity = Integer.parseInt(propertyValue);

		if (cm == null)
			propertyValue = ((Properties) P2LocationProps).getProperty("p2location.node_border_opacity");
		else
			propertyValue = cm.getProperties().getProperty("p2location.node_border_opacity");
		Util.node_border_opacity = Integer.parseInt(propertyValue);

		if (cm == null)
			propertyValue = ((Properties) P2LocationProps).getProperty("p2location.edge_link_width");
		else
			propertyValue = cm.getProperties().getProperty("p2location.edge_link_width");
		Util.edge_link_width = Double.parseDouble(propertyValue);

		if (cm == null)
			propertyValue = ((Properties) P2LocationProps).getProperty("p2location.node_border_width");
		else
			propertyValue = cm.getProperties().getProperty("p2location.node_border_width");
		Util.node_border_width = Double.parseDouble(propertyValue);

		if (cm == null)
			propertyValue = ((Properties) P2LocationProps).getProperty("p2location.IntraLinksColor");
		else
			propertyValue = cm.getProperties().getProperty("p2location.IntraLinksColor");
		Util.IntraLinksColor = stringToColor(propertyValue);

		if (cm == null)
			propertyValue = ((Properties) P2LocationProps).getProperty("p2location.InterLinksColor");
		else
			propertyValue = cm.getProperties().getProperty("p2location.InterLinksColor");
		Util.InterLinksColor = stringToColor(propertyValue);

		if (cm == null)
			propertyValue = ((Properties) P2LocationProps).getProperty("p2location.NodeBorderColor");
		else
			propertyValue = cm.getProperties().getProperty("p2location.NodeBorderColor");
		Util.NodeBorderColor = stringToColor(propertyValue);

		if (cm == null)
			propertyValue = ((Properties) P2LocationProps).getProperty("p2location.intralink_threshold_score");
		else
			propertyValue = cm.getProperties().getProperty("p2location.intralink_threshold_score");
		Util.intralink_threshold_score = Double.parseDouble(propertyValue);

		if (cm == null)
			propertyValue = ((Properties) P2LocationProps).getProperty("p2location.interlink_threshold_score");
		else
			propertyValue = cm.getProperties().getProperty("p2location.interlink_threshold_score");
		Util.interlink_threshold_score = Double.parseDouble(propertyValue);

		if (cm == null)
			propertyValue = ((Properties) P2LocationProps).getProperty("p2location.combinedlink_threshold_score");
		else
			propertyValue = cm.getProperties().getProperty("p2location.combinedlink_threshold_score");
//		Util.combinedlink_threshold_score = Double.parseDouble(propertyValue);

		if (cm == null)
			propertyValue = ((Properties) P2LocationProps).getProperty("p2location.pymol_path");
		else
			propertyValue = cm.getProperties().getProperty("p2location.pymol_path");
		Util.PYMOL_PATH = propertyValue;

		if (cm == null)
			propertyValue = ((Properties) P2LocationProps).getProperty("p2location.showPTMs");
		else
			propertyValue = cm.getProperties().getProperty("p2location.showPTMs");
		Util.showPTMs = Boolean.parseBoolean(propertyValue);

		if (cm == null)
			propertyValue = ((Properties) P2LocationProps).getProperty("p2location.PTMColor");
		else
			propertyValue = cm.getProperties().getProperty("p2location.PTMColor");
		Util.PTMColor = stringToColor(propertyValue);

		if (cm == null)
			propertyValue = ((Properties) P2LocationProps).getProperty("p2location.showMonolinkedPeptides");
		else
			propertyValue = cm.getProperties().getProperty("p2location.showMonolinkedPeptides");
		Util.showMonolinkedPeptides = Boolean.parseBoolean(propertyValue);

		if (cm == null)
			propertyValue = ((Properties) P2LocationProps).getProperty("p2location.MonoLinksPeptideColor");
		else
			propertyValue = cm.getProperties().getProperty("p2location.MonoLinksPeptideColor");
		Util.MonoLinksPeptideColor = stringToColor(propertyValue);

	}

	/**
	 * Method responsible for checking if the PyMOL is correct for a specific OS.
	 */
	private void checkPyMOLname() {

		if (Util.isWindows()) {
			if (Util.PYMOL_PATH.endsWith(".exe")) {
				pymolPathStr.setText(Util.PYMOL_PATH);
			} else {
				pymolPathStr.setText("pymol");
				Util.PYMOL_PATH = "pymol";
			}
		} else if (Util.isMac()) {
			if (Util.PYMOL_PATH.endsWith(".app")) {
				pymolPathStr.setText(Util.PYMOL_PATH);
			} else {
				pymolPathStr.setText("/Applications/PyMOL.app");
				Util.PYMOL_PATH = "/Applications/PyMOL.app";
			}
		} else if (Util.isUnix()) {
			if (Util.PYMOL_PATH.endsWith("pymol")) {
				pymolPathStr.setText(Util.PYMOL_PATH);
			} else {
				pymolPathStr.setText("pymol");
				Util.PYMOL_PATH = "pymol";
			}
		}
	}

	/**
	 * Converter string to color
	 * 
	 * @param color_string string color
	 * @return color object
	 */
	private Color stringToColor(String color_string) {

		Color color = null;
		String[] cols = color_string.split("#");
		color = new Color(Integer.parseInt(cols[0]), Integer.parseInt(cols[1]), Integer.parseInt(cols[2]), 255);

		return color;

	}

	private void init_params() {

		link_panel = new JPanel();
		link_panel.setBackground(Color.WHITE);
		link_panel.setBorder(BorderFactory.createTitledBorder("Parameters"));
		link_panel.setLayout(null);
	}

	private void init_filter(int offset_x) {

		int offset_y = 25;

		filter_panel = new JPanel();
		filter_panel.setBackground(Color.WHITE);
		filter_panel.setBorder(BorderFactory.createTitledBorder("Filter"));
		filter_panel.setBounds(10, offset_y, WIDTH_PANEL, 120);
		filter_panel.setLayout(null);
		link_panel.add(filter_panel);

		SpinnerModel model_epochs = new SpinnerNumberModel(Util.epochs.intValue(), // initial
				// value
				1, // min
				255, // max
				1); // step
		spinner_epochs = new JSpinner(model_epochs);
		spinner_epochs.setBounds(offset_x, offset_y, 60, 20);
		JComponent comp_epochs = spinner_epochs.getEditor();
		JFormattedTextField field_epochs = (JFormattedTextField) comp_epochs.getComponent(0);
		DefaultFormatter formatter_epochs = (DefaultFormatter) field_epochs.getFormatter();
		formatter_epochs.setCommitsOnValidEdit(true);
		spinner_epochs.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				Util.epochs = (Integer) spinner_epochs.getValue();
				P2LocationProps.setProperty("p2location.epochs", Util.epochs.toString());
			}
		});
		spinner_epochs.setToolTipText("Set a value between 0 and 100.");
		filter_panel.add(spinner_epochs);
		offset_y += 30;

		SpinnerModel model_score = new SpinnerNumberModel(Util.threshold_score, // initial
				// value
				0, // min
				255, // max
				1); // step
		spinner_score = new JSpinner(model_score);
		spinner_score.setBounds(offset_x, offset_y, 60, 20);
		JComponent comp_score = spinner_score.getEditor();
		JFormattedTextField field_score = (JFormattedTextField) comp_score.getComponent(0);
		DefaultFormatter formatter_score = (DefaultFormatter) field_score.getFormatter();
		formatter_score.setCommitsOnValidEdit(true);
		spinner_score.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				Util.threshold_score = (double) spinner_score.getValue();
				P2LocationProps.setProperty("p2location.threshold_score", Double.toString(Util.threshold_score));
			}
		});
		spinner_score.setToolTipText("Set a value between 0 and 100.");
		filter_panel.add(spinner_score);
		offset_y += 30;

		SpinnerModel model_specCount = new SpinnerNumberModel(Util.specCount.intValue(), // initial
				// value
				1, // min
				255, // max
				1); // step
		spinner_specCount = new JSpinner(model_specCount);
		spinner_specCount.setBounds(offset_x, offset_y, 60, 20);
		JComponent comp_specCount = spinner_specCount.getEditor();
		JFormattedTextField field_specCount = (JFormattedTextField) comp_specCount.getComponent(0);
		DefaultFormatter formatter_specCount = (DefaultFormatter) field_specCount.getFormatter();
		formatter_specCount.setCommitsOnValidEdit(true);
		spinner_specCount.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				Util.specCount = (Integer) spinner_specCount.getValue();
				P2LocationProps.setProperty("p2location.specCount", Util.specCount.toString());

			}
		});
		spinner_specCount.setToolTipText("Set a value between 0 and 100.");
		filter_panel.add(spinner_specCount);

		this.init_filter_checkboxes();
	}

	private void init_filter_checkboxes() {

		int offset_y = 20;
		enable_epochs = new JCheckBox("Epochs:");
		enable_epochs.setBackground(Color.WHITE);
		enable_epochs.setSelected(Util.getEpochs);
		enable_epochs.setFont(new java.awt.Font("Tahoma", Font.PLAIN, 12));

		if (Util.isWindows())
			enable_epochs.setBounds(5, offset_y, 115, 20);
		else
			enable_epochs.setBounds(5, offset_y, 130, 20);

		enable_epochs.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {// checkbox has been selected
					spinner_epochs.setEnabled(true);
					Util.getEpochs = true;
					P2LocationProps.setProperty("p2location.getEpochs", "true");

				} else {
					spinner_epochs.setEnabled(false);
					Util.getEpochs = false;
					P2LocationProps.setProperty("p2location.getEpochs", "false");
				}
			}
		});
		filter_panel.add(enable_epochs);
		offset_y += 30;

		enable_score = new JCheckBox("-Log(Score):");
		enable_score.setBackground(Color.WHITE);
		enable_score.setSelected(Util.getThreshold_score);
		enable_score.setFont(new java.awt.Font("Tahoma", Font.PLAIN, 12));
		if (Util.isWindows())
			enable_score.setBounds(5, offset_y, 115, 20);
		else
			enable_score.setBounds(5, offset_y, 130, 20);

		enable_score.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {// checkbox has been selected
					spinner_score.setEnabled(true);
					Util.getThreshold_score = true;
					P2LocationProps.setProperty("p2location.getThreshold_score", "true");

				} else {
					spinner_score.setEnabled(false);
					Util.getThreshold_score = false;
					P2LocationProps.setProperty("p2location.getThreshold_score", "false");
				}
			}
		});
		filter_panel.add(enable_score);
		offset_y += 30;

		enable_specCount = new JCheckBox("CSM(s):");
		enable_specCount.setBackground(Color.WHITE);
		enable_specCount.setSelected(Util.getSpecCount);
		enable_specCount.setFont(new java.awt.Font("Tahoma", Font.PLAIN, 12));
		if (Util.isWindows())
			enable_specCount.setBounds(5, offset_y, 115, 20);
		else
			enable_specCount.setBounds(5, offset_y, 130, 20);

		enable_specCount.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {// checkbox has been selected
					spinner_specCount.setEnabled(true);
					Util.getSpecCount = true;
					P2LocationProps.setProperty("p2location.getSpecCount", "true");

				} else {
					spinner_specCount.setEnabled(false);
					Util.getSpecCount = false;
					P2LocationProps.setProperty("p2location.getSpecCount", "false");
				}
			}
		});
		filter_panel.add(enable_specCount);
	}

	private void init_options(int offset_x) {

		int offset_y = 25;

		option_panel = new JPanel();
		option_panel.setBackground(Color.WHITE);
		option_panel.setBorder(BorderFactory.createTitledBorder("Option"));
		option_panel.setBounds(10, offset_y + 130, WIDTH_PANEL, 125);
		option_panel.setLayout(null);
		link_panel.add(option_panel);

		JLabel neighbor_aa = new JLabel("# Neighbor AA - Residue:");
		neighbor_aa.setFont(new java.awt.Font("Tahoma", Font.PLAIN, 12));
		neighbor_aa.setBounds(10, offset_y - 10, 150, 40);
		option_panel.add(neighbor_aa);

		SpinnerModel model_neighborAA = new SpinnerNumberModel(Util.neighborAA.intValue(), // initial
				// value
				1, // min
				255, // max
				1); // step
		spinner_neighborAA = new JSpinner(model_neighborAA);
		spinner_neighborAA.setEnabled(false);
		spinner_neighborAA.setBounds(offset_x, offset_y, 60, 20);
		JComponent comp_neighborAA = spinner_neighborAA.getEditor();
		JFormattedTextField field_neighborAA = (JFormattedTextField) comp_neighborAA.getComponent(0);
		DefaultFormatter formatter_neighborAA = (DefaultFormatter) field_neighborAA.getFormatter();
		formatter_neighborAA.setCommitsOnValidEdit(true);
		spinner_neighborAA.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				Util.neighborAA = (Integer) spinner_neighborAA.getValue();
				P2LocationProps.setProperty("p2location.neighborAA", Util.neighborAA.toString());

			}
		});
		spinner_neighborAA.setToolTipText("Set a value between 0 and 100.");
		option_panel.add(spinner_neighborAA);
		offset_y += 30;

		JLabel transmem_neighbor_aa = new JLabel("# Neighbor AA - Transmembrane:");
		transmem_neighbor_aa.setFont(new java.awt.Font("Tahoma", Font.PLAIN, 12));
		transmem_neighbor_aa.setBounds(10, offset_y - 10, 190, 40);
		option_panel.add(transmem_neighbor_aa);

		SpinnerModel model_transmem_neighborAA = new SpinnerNumberModel(Util.neighborAA.intValue(), // initial
				// value
				1, // min
				255, // max
				1); // step
		spinner_transmem_neighborAA = new JSpinner(model_transmem_neighborAA);
		spinner_transmem_neighborAA.setEnabled(false);
		spinner_transmem_neighborAA.setBounds(offset_x, offset_y, 60, 20);
		JComponent comp_transmem_neighborAA = spinner_transmem_neighborAA.getEditor();
		JFormattedTextField field_transmem_neighborAA = (JFormattedTextField) comp_transmem_neighborAA.getComponent(0);
		DefaultFormatter formatter_transmem_neighborAA = (DefaultFormatter) field_transmem_neighborAA.getFormatter();
		formatter_transmem_neighborAA.setCommitsOnValidEdit(true);
		spinner_transmem_neighborAA.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				Util.transmemNeighborAA = (Integer) spinner_transmem_neighborAA.getValue();
				P2LocationProps.setProperty("p2location.transmem_neighborAA", Util.transmemNeighborAA.toString());

			}
		});
		spinner_transmem_neighborAA.setToolTipText("Set a value between 0 and 100.");
		option_panel.add(spinner_transmem_neighborAA);
		offset_y += 30;

		consider_domain_whole_ptn = new JCheckBox("Consider domain for entire protein");
		consider_domain_whole_ptn.setBackground(Color.WHITE);
		consider_domain_whole_ptn.setSelected(Util.consider_domain_whole_ptn);
		consider_domain_whole_ptn.setFont(new java.awt.Font("Tahoma", Font.PLAIN, 12));
		consider_domain_whole_ptn.setBounds(5, offset_y, 220, 20);

		consider_domain_whole_ptn.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {// checkbox has been selected
					Util.consider_domain_whole_ptn = true;
					P2LocationProps.setProperty("p2location.consider_domain_whole_ptn", "true");
					spinner_transmem_neighborAA.setEnabled(false);
					spinner_neighborAA.setEnabled(false);

				} else {
					Util.consider_domain_whole_ptn = false;
					P2LocationProps.setProperty("p2location.consider_domain_whole_ptn", "false");
					spinner_transmem_neighborAA.setEnabled(true);
					spinner_neighborAA.setEnabled(true);
				}
			}
		});
		option_panel.add(consider_domain_whole_ptn);
	}

	/**
	 * Method responsible for initializing buttons of link panel
	 * 
	 * @param offset_x     offset x
	 * @param button_width button width
	 */
	private void init_prediction(int offset_x) {

		int offset_y = 20;
		int button_width = 38;
		int button_height = 15;

		if (Util.isWindows()) {
			button_width = 65;
			button_height = 20;
		}

		prediction_panel = new JPanel();
		prediction_panel.setBackground(Color.WHITE);
		prediction_panel.setBorder(BorderFactory.createTitledBorder("Prediction"));
		prediction_panel.setBounds(10, offset_y + 275, WIDTH_PANEL, 390);
		prediction_panel.setLayout(null);
		link_panel.add(prediction_panel);

		this.init_transmem_regions_prediction(offset_x, button_width + 20, button_height);
		this.init_protein_domains_prediction(offset_x, button_width + 20, button_height);

	}

	/**
	 * Method responsible for initializing transmem region
	 * 
	 * @param offset_x      offset x
	 * @param button_width  button width
	 * @param button_height botton height
	 */
	private void init_transmem_regions_prediction(int offset_x, int button_width, int button_height) {

		offset_x -= 10;
		int offset_y = 25;
		transmem_regions_prediction_panel = new JPanel();
		transmem_regions_prediction_panel.setBackground(Color.WHITE);
		transmem_regions_prediction_panel.setBorder(BorderFactory.createTitledBorder("Transmembrane regions"));
		transmem_regions_prediction_panel.setBounds(10, offset_y, WIDTH_PANEL - 20, 105);
		transmem_regions_prediction_panel.setLayout(null);
		prediction_panel.add(transmem_regions_prediction_panel);

		offset_y -= 15;
		JLabel upperScoreLabel = new JLabel("Upper Score:");
		upperScoreLabel.setFont(new java.awt.Font("Tahoma", Font.PLAIN, 12));
		upperScoreLabel.setBounds(10, offset_y, 150, 40);
		transmem_regions_prediction_panel.add(upperScoreLabel);

		offset_y += 8;

		SpinnerModel model_transmemUpperCutoffScore = new SpinnerNumberModel(Util.transmemPredictionRegionsUpperScore, // initial
				// value
				0, // min
				1, // max
				0.1); // step
		spinner_transmemUpperCutoffScore = new JSpinner(model_transmemUpperCutoffScore);
		spinner_transmemUpperCutoffScore.setBounds(offset_x, offset_y, 60, 20);
		JComponent comp_transmemUpperCutoffScore = spinner_transmemUpperCutoffScore.getEditor();
		JFormattedTextField field_transmemUpperCutoffScore = (JFormattedTextField) comp_transmemUpperCutoffScore.getComponent(0);
		DefaultFormatter formatter_transmemUpperCutoffScore = (DefaultFormatter) field_transmemUpperCutoffScore.getFormatter();
		formatter_transmemUpperCutoffScore.setCommitsOnValidEdit(true);
		spinner_transmemUpperCutoffScore.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				Util.transmemPredictionRegionsUpperScore = (Double) spinner_transmemUpperCutoffScore.getValue();
				P2LocationProps.setProperty("p2location.transmemPredictionRegionsScore",
						String.valueOf(Util.transmemPredictionRegionsUpperScore));

			}
		});
		spinner_transmemUpperCutoffScore.setToolTipText("Set a value between 0 and 1.");
		transmem_regions_prediction_panel.add(spinner_transmemUpperCutoffScore);
		offset_y += 20;
		
		JLabel lowerScoreLabel = new JLabel("Lower Score:");
		lowerScoreLabel.setFont(new java.awt.Font("Tahoma", Font.PLAIN, 12));
		lowerScoreLabel.setBounds(10, offset_y, 150, 40);
		transmem_regions_prediction_panel.add(lowerScoreLabel);

		offset_y += 8;

		SpinnerModel model_transmemLowerCutoffScore = new SpinnerNumberModel(Util.transmemPredictionRegionsLowerScore, // initial
				// value
				0, // min
				1, // max
				0.1); // step
		spinner_transmemLowerCutoffScore = new JSpinner(model_transmemLowerCutoffScore);
		spinner_transmemLowerCutoffScore.setBounds(offset_x, offset_y, 60, 20);
		JComponent comp_transmemLowerCutoffScore = spinner_transmemLowerCutoffScore.getEditor();
		JFormattedTextField field_transmemLowerCutoffScore = (JFormattedTextField) comp_transmemLowerCutoffScore.getComponent(0);
		DefaultFormatter formatter_transmemLowerCutoffScore = (DefaultFormatter) field_transmemLowerCutoffScore.getFormatter();
		formatter_transmemLowerCutoffScore.setCommitsOnValidEdit(true);
		spinner_transmemLowerCutoffScore.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				Util.transmemPredictionRegionsLowerScore = (Double) spinner_transmemLowerCutoffScore.getValue();
				P2LocationProps.setProperty("p2location.transmemPredictionRegionsScore",
						String.valueOf(Util.transmemPredictionRegionsLowerScore));

			}
		});
		spinner_transmemLowerCutoffScore.setToolTipText("Set a value between 0 and 1.");
		transmem_regions_prediction_panel.add(spinner_transmemLowerCutoffScore);
		offset_y += 30;

		processTransmemButton = new JButton("Run");
		processTransmemButton.setBounds(offset_x + 5, offset_y, button_width, button_height);
		processTransmemButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
		processTransmemButton.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if (processTransmemButton.isEnabled()) {

					if (myNetwork != null && processTransmemRegionsTaskFactory != null && dialogTaskManager != null) {

						TaskIterator ti = processTransmemRegionsTaskFactory.createTaskIterator(true, false);

						TaskObserver observer = new TaskObserver() {

							@Override
							public void taskFinished(ObservableTask task) {

							}

							@Override
							public void allFinished(FinishStatus finishStatus) {

								JOptionPane.showMessageDialog(null,
										"Transmembrane prediction has been done successfully!",
										"P2Location - Information", JOptionPane.INFORMATION_MESSAGE,
										new ImageIcon(getClass().getResource("/images/logo.png")));

								enable_disableDisplayBox(true, false);
								enable_disable_predictBox(true);
								enable_disable_transmemBox(true);
								processButton.setEnabled(true);
							}
						};

						dialogTaskManager.execute(ti, observer);
						processButton.setEnabled(false);

						enable_disable_predictBox(false);
						enable_disable_transmemBox(false);

					} else {
						JOptionPane.showMessageDialog(null, "No network has been loaded!", "P2Location - Alert",
								JOptionPane.WARNING_MESSAGE, new ImageIcon(getClass().getResource("/images/logo.png")));
					}
				}
			}
		});
		transmem_regions_prediction_panel.add(processTransmemButton);

	}

	/**
	 * Method responsible for creating protein domains panel
	 * 
	 * @param offset_x      current offset x
	 * @param button_width  current button width
	 * @param button_height current button height
	 */
	private void init_protein_domains_prediction(int offset_x, int button_width, int button_height) {

		offset_x -= 10;
		int offset_y = 25;
		protein_domains_prediction_panel = new JPanel();
		protein_domains_prediction_panel.setBackground(Color.WHITE);
		protein_domains_prediction_panel.setBorder(BorderFactory.createTitledBorder("Protein domains"));
		protein_domains_prediction_panel.setBounds(10, offset_y + 115, WIDTH_PANEL - 20, 240);
		protein_domains_prediction_panel.setLayout(null);
		prediction_panel.add(protein_domains_prediction_panel);

		enable_conflict = new JCheckBox("Fix conflicts manually");
		enable_conflict.setBackground(Color.WHITE);
		enable_conflict.setSelected(Util.fixDomainManually);
		enable_conflict.setFont(new java.awt.Font("Tahoma", Font.PLAIN, 12));
		if (Util.isWindows())
			enable_conflict.setBounds(5, offset_y - 5, 155, 20);
		else
			enable_conflict.setBounds(5, offset_y - 5, 170, 20);

		protein_domains_prediction_panel.add(enable_conflict);
		offset_y += 30;
		enable_conflict.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {// checkbox has been selected
					spinner_deltaScore.setEnabled(false);
					Util.fixDomainManually = true;
					P2LocationProps.setProperty("p2location.fixDomainManually", "true");

				} else {
					spinner_deltaScore.setEnabled(true);
					Util.fixDomainManually = false;
					P2LocationProps.setProperty("p2location.fixDomainManually", "false");
				}
			}
		});

		this.init_fix_conflict(offset_x, button_width + 20, button_height);
		offset_y += 65;

		processButton = new JButton("Run");
		processButton.setBounds(offset_x + 5, offset_y, button_width, button_height);
		processButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
		processButton.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if (processButton.isEnabled()) {

					if (myNetwork != null && processProteinLocationTaskFactory != null && dialogTaskManager != null) {

						TaskIterator ti = processProteinLocationTaskFactory.createTaskIterator(true, false);

						TaskObserver observer = new TaskObserver() {

							@Override
							public void taskFinished(ObservableTask task) {

							}

							@SuppressWarnings({ "unchecked", "rawtypes" })
							@Override
							public void allFinished(FinishStatus finishStatus) {

								// The last epoch is equal to the before last
								int real_epochs = 1;
								if (ProcessProteinLocationTask.epochs > 1)
									real_epochs = ProcessProteinLocationTask.epochs - 1;

								int uk_residue_location = 0;
								if (Util.isKeyPresent(ProcessProteinLocationTask.number_unknown_residues, real_epochs))
									uk_residue_location = ProcessProteinLocationTask.number_unknown_residues
											.get(real_epochs);
								JOptionPane.showMessageDialog(null,
										"Prediction has been done successfully!\nEpoch(s): " + real_epochs
												+ "\n# Unknown Residue location: " + uk_residue_location,
										"P2Location - Information", JOptionPane.INFORMATION_MESSAGE,
										new ImageIcon(getClass().getResource("/images/logo.png")));

								List<String> epochsList = new ArrayList<String>();
								for (int i = 1; i <= ProcessProteinLocationTask.epochs - 1; i++) {
									epochsList.add(Integer.toString(i));
								}

								epochCombobox.setModel(new DefaultComboBoxModel(epochsList.toArray()));
								epochCombobox.setSelectedIndex(epochsList.size() - 1);
								current_epoch = Integer.parseInt(epochsList.get(epochsList.size() - 1));
								enable_disableDisplayBox(true, false);
								enable_disable_predictBox(true);
								enable_disable_transmemBox(true);
								processButton.setEnabled(true);
							}
						};

						processButton.setEnabled(false);
						dialogTaskManager.execute(ti, observer);

						enable_disable_predictBox(false);
						enable_disable_transmemBox(false);

					} else {
						JOptionPane.showMessageDialog(null, "No network has been loaded!", "P2Location - Alert",
								JOptionPane.WARNING_MESSAGE, new ImageIcon(getClass().getResource("/images/logo.png")));
					}
				}
			}
		});
		protein_domains_prediction_panel.add(processButton);
		enable_disable_spinners(false);

		this.init_display_prediction(offset_x, button_width + 20, button_height);

	}

	/**
	 * Method responsible for initializing display panel
	 * 
	 * @param offset_x     offset x
	 * @param button_width button width
	 */
	private void init_fix_conflict(int offset_x, int button_width, int button_height) {

		int offset_y = 15;
		fix_conflict_panel = new JPanel();
		fix_conflict_panel.setBackground(Color.WHITE);
		fix_conflict_panel.setBorder(BorderFactory.createTitledBorder("Fix conflicts automatically"));
		fix_conflict_panel.setBounds(10, offset_y + 30, WIDTH_PANEL - 40, 65);
		fix_conflict_panel.setLayout(null);
		protein_domains_prediction_panel.add(fix_conflict_panel);

		JLabel epochLabel = new JLabel("\u0394 score:");
		epochLabel.setFont(new java.awt.Font("Tahoma", Font.PLAIN, 12));
		epochLabel.setBounds(10, offset_y, 150, 40);
		fix_conflict_panel.add(epochLabel);

		offset_y += 8;
		offset_x -= 10;

		SpinnerModel model_deltaScore = new SpinnerNumberModel(Util.deltaScore, // initial
				// value
				0, // min
				100, // max
				0.1); // step
		spinner_deltaScore = new JSpinner(model_deltaScore);
		spinner_deltaScore.setEnabled(false);
		spinner_deltaScore.setBounds(offset_x, offset_y, 60, 20);
		JComponent comp_deltaScore = spinner_deltaScore.getEditor();
		JFormattedTextField field_deltaScore = (JFormattedTextField) comp_deltaScore.getComponent(0);
		DefaultFormatter formatter_deltaScore = (DefaultFormatter) field_deltaScore.getFormatter();
		formatter_deltaScore.setCommitsOnValidEdit(true);
		spinner_deltaScore.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				Util.deltaScore = (Double) spinner_deltaScore.getValue();
				P2LocationProps.setProperty("p2location.deltaScore", String.valueOf(Util.deltaScore));

			}
		});
		spinner_deltaScore.setToolTipText("Set a value between 0 and 100.");
		fix_conflict_panel.add(spinner_deltaScore);

	}

	/**
	 * Method responsible for initializing display panel
	 * 
	 * @param offset_x     offset x
	 * @param button_width button width
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void init_display_prediction(int offset_x, int button_width, int button_height) {

		int offset_y = 25;
		display_prediction_panel = new JPanel();
		display_prediction_panel.setBackground(Color.WHITE);
		display_prediction_panel.setBorder(BorderFactory.createTitledBorder("Display"));
		display_prediction_panel.setBounds(10, offset_y + 115, WIDTH_PANEL - 40, 85);
		display_prediction_panel.setLayout(null);
		protein_domains_prediction_panel.add(display_prediction_panel);

		offset_y -= 10;
		JLabel epochLabel = new JLabel("Epoch:");
		epochLabel.setFont(new java.awt.Font("Tahoma", Font.PLAIN, 12));
		epochLabel.setBounds(10, offset_y, 50, 40);
		display_prediction_panel.add(epochLabel);

		List<String> epochsList = new ArrayList<String>();
		int real_epoch = 1;
		if (ProcessProteinLocationTask.epochs > 1)
			real_epoch = ProcessProteinLocationTask.epochs - 1;
		for (int i = 1; i <= real_epoch; i++) {
			epochsList.add(Integer.toString(i));
		}

		offset_y += 2;
		offset_x -= 10;
		// Create the combo box, select item at index 4.
		// Indices start at 0, so 4 specifies the pig.
		epochCombobox = new JComboBox(epochsList.toArray());
		int combobox_height = 40;
		if (Util.isWindows())
			combobox_height = 15;

		epochCombobox.setBounds(offset_x, offset_y, 70, combobox_height);
		epochCombobox.setSelectedIndex(epochsList.size() - 1);
		epochCombobox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				current_epoch = Integer.parseInt(epochCombobox.getItemAt(epochCombobox.getSelectedIndex()).toString());
			}
		});
		current_epoch = Integer.parseInt(epochsList.get(epochsList.size() - 1));
		display_prediction_panel.add(epochCombobox);
		offset_y += 40;
		offset_x += 5;
		if (Util.isWindows())
			offset_x -= 20;

		display_epochButton = new JButton("Update");
		display_epochButton.setBounds(offset_x - 10, offset_y, button_width, button_height);
		display_epochButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
		display_epochButton.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if (display_epochButton.isEnabled()) {

					ProcessProteinLocationTask.epochs = current_epoch;

					if (myNetwork != null && processProteinLocationTaskFactory != null && dialogTaskManager != null) {

						// Update protein domains according to the epochs
						TaskIterator updateProteinDomainsTaskIterator = processProteinLocationTaskFactory
								.createTaskIterator(true, true);

						TaskObserver updateProteinDomainsObserver = new TaskObserver() {

							@Override
							public void taskFinished(ObservableTask task) {

							}

							@Override
							public void allFinished(FinishStatus finishStatus) {

								selectModifiedNodes();

								// Update plotting (restore layout)
								TaskIterator updatePlottingTaskRestoreLayoutIterator = singleNodeTaskFactory
										.createTaskIterator();

								TaskObserver updatePlottingRestoreLayoutObserver = new TaskObserver() {

									@Override
									public void taskFinished(ObservableTask task) {

									}

									@Override
									public void allFinished(FinishStatus finishStatus) {

										// Update plotting (apply layout)
										TaskIterator updatePlottingTaskApplyLayoutIterator = singleNodeTaskFactory
												.createTaskIterator();

										TaskObserver updatePlottingApplyLayoutObserver = new TaskObserver() {

											@Override
											public void taskFinished(ObservableTask task) {

											}

											@Override
											public void allFinished(FinishStatus finishStatus) {

												unselectAllNodes();

												JOptionPane.showMessageDialog(null,
														"Protein annotations have been updated successfully!\n# Unknown Residue location: "
																+ ProcessProteinLocationTask.number_unknown_residues
																		.get(ProcessProteinLocationTask.epochs),
														"P2Location - Predict protein location",
														JOptionPane.INFORMATION_MESSAGE,
														new ImageIcon(getClass().getResource("/images/logo.png")));

											}
										};

										// Execute the task through the TaskManager
										dialogTaskManager.execute(updatePlottingTaskApplyLayoutIterator,
												updatePlottingApplyLayoutObserver);

									}
								};

								// Execute the task through the TaskManager
								dialogTaskManager.execute(updatePlottingTaskRestoreLayoutIterator,
										updatePlottingRestoreLayoutObserver);

							}
						};

						dialogTaskManager.execute(updateProteinDomainsTaskIterator, updateProteinDomainsObserver);

					} else {
						JOptionPane.showMessageDialog(null, "No network has been loaded!",
								"P2Location - Predict protein location", JOptionPane.WARNING_MESSAGE,
								new ImageIcon(getClass().getResource("/images/logo.png")));
					}
				}
			}
		});
		display_prediction_panel.add(display_epochButton);
		enable_disableDisplayBox(false, false);
	}

	public static void enable_disable_predictBox(boolean enable) {

		if (enable_epochs != null)
			enable_epochs.setEnabled(enable);
		if (enable_score != null)
			enable_score.setEnabled(enable);
		if (enable_specCount != null)
			enable_specCount.setEnabled(enable);
		if (enable_conflict != null)
			enable_conflict.setEnabled(enable);
	}

	public static void enable_disable_spinners(boolean enable) {
		if (spinner_epochs != null)
			spinner_epochs.setEnabled(enable);
		if (spinner_score != null)
			spinner_score.setEnabled(enable);
		if (spinner_specCount != null)
			spinner_specCount.setEnabled(enable);
	}

	/**
	 * Method responsible for enabling or disabling all fields in Display Box
	 * 
	 * @param enable
	 * @param isInitial
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void enable_disableDisplayBox(boolean enable, boolean isInitial) {

		if (epochCombobox != null)
			epochCombobox.setEnabled(enable);
		if (display_epochButton != null)
			display_epochButton.setEnabled(enable);

		if (isInitial) {
			List<String> epochsList = new ArrayList<String>();
			epochsList.add("1");

			if (epochCombobox != null) {
				epochCombobox.setModel(new DefaultComboBoxModel(epochsList.toArray()));
				epochCombobox.setSelectedIndex(epochsList.size() - 1);
			}
		}
	}

	public static void enable_disable_transmemBox(boolean enable) {

		if (processTransmemButton != null)
			processTransmemButton.setEnabled(enable);

		if (spinner_transmemUpperCutoffScore != null)
			spinner_transmemUpperCutoffScore.setEnabled(enable);
		
		if (spinner_transmemLowerCutoffScore != null)
			spinner_transmemLowerCutoffScore.setEnabled(enable);

	}

	/**
	 * Method responsible for unchecking checkboxes
	 */
	public static void unselectCheckboxes() {
		enable_epochs.setSelected(false);
		enable_score.setSelected(false);
		enable_specCount.setSelected(false);

		// Except this checkbox
		enable_conflict.setSelected(true);
	}

	/**
	 * Method responsible for selecting modified nodes
	 */
	private void selectModifiedNodes() {

		List<CyNode> allNodes = myNetwork.getNodeList();
		for (CyNode cyNode : allNodes) {

			String node_name = myNetwork.getDefaultNodeTable().getRow(cyNode.getSUID()).get(CyNetwork.NAME,
					String.class);
			if (node_name.contains("RESIDUE"))
				continue;

			if (Util.IsNodeModified(myNetwork, netView, cyNode))
				myNetwork.getRow(cyNode).set(CyNetwork.SELECTED, true);
			else
				myNetwork.getRow(cyNode).set(CyNetwork.SELECTED, false);
		}
	}

	private void unselectAllNodes() {
		List<CyNode> allNodes = myNetwork.getNodeList();
		for (CyNode cyNode : allNodes) {
			myNetwork.getRow(cyNode).set(CyNetwork.SELECTED, false);
		}
	}

	/**
	 * Method responsible for initializing node style features
	 * 
	 * @param offset_x     offset x
	 * @param button_width button width
	 */
	private void init_node_style_features(int offset_x, int button_width) {

		node_panel = new JPanel();
		node_panel.setBackground(Color.WHITE);
		node_panel.setBorder(BorderFactory.createTitledBorder("Node"));
		node_panel.setLayout(null);

		int offset_y = 10;
		JLabel font_size_node = new JLabel("Font size:");
		font_size_node.setFont(new java.awt.Font("Tahoma", Font.PLAIN, 12));
		font_size_node.setBounds(10, offset_y, 100, 40);
		node_panel.add(font_size_node);

		offset_y = 20;
		if (!Util.isWindows())// MacOS or Unix
			offset_x -= 5;
		SpinnerModel model_node = new SpinnerNumberModel(Util.node_label_font_size.intValue(), // initial value
				0, // min
				100, // max
				1); // step
		spinner_font_size_node = new JSpinner(model_node);
		spinner_font_size_node.setBounds(offset_x, offset_y, 60, 20);
		JComponent comp_node = spinner_font_size_node.getEditor();
		JFormattedTextField field_node = (JFormattedTextField) comp_node.getComponent(0);
		DefaultFormatter formatter_node = (DefaultFormatter) field_node.getFormatter();
		formatter_node.setCommitsOnValidEdit(true);
		spinner_font_size_node.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				Util.node_label_font_size = (Integer) spinner_font_size_node.getValue();
				P2LocationProps.setProperty("p2location.node_label_font_size", Util.node_label_font_size.toString());

				if (myNetwork != null && netView != null) {
					Util.updateNodesStyles(myNetwork, netView);
				}
			}
		});
		node_panel.add(spinner_font_size_node);

	}

	/**
	 * Method responsible for initializing node border panel
	 * 
	 * @param offset_x     offset x
	 * @param button_width button width
	 */
	private void init_node_border_features(int offset_x, int button_width) {

		node_border_panel = new JPanel();
		node_border_panel.setBackground(Color.WHITE);
		node_border_panel.setBorder(BorderFactory.createTitledBorder("Border"));
		node_border_panel.setBounds(10, 50, 230, 120);
		node_border_panel.setLayout(null);
		node_panel.add(node_border_panel);

		int offset_y = 10;
		JLabel textLabel_border_node_color = new JLabel("Color:");
		textLabel_border_node_color.setFont(new java.awt.Font("Tahoma", Font.PLAIN, 12));
		textLabel_border_node_color.setBounds(10, offset_y, 50, 40);
		node_border_panel.add(textLabel_border_node_color);
		offset_y += 30;

		JLabel opacity_node_border = new JLabel("Opacity:");
		opacity_node_border.setFont(new java.awt.Font("Tahoma", Font.PLAIN, 12));
		opacity_node_border.setBounds(10, offset_y, 100, 40);
		node_border_panel.add(opacity_node_border);
		offset_y += 30;

		JLabel width_node_border = new JLabel("Width:");
		width_node_border.setFont(new java.awt.Font("Tahoma", Font.PLAIN, 12));
		width_node_border.setBounds(10, offset_y, 100, 40);
		node_border_panel.add(width_node_border);

		offset_y = 20;
		offset_x -= 10;
		borderNodeColorButton = new JButton();
		borderNodeColorButton.setBounds(offset_x, offset_y, button_width, 15);
		borderNodeColorButton.setBackground(Util.NodeBorderColor);
		borderNodeColorButton.setForeground(Util.NodeBorderColor);
		borderNodeColorButton.setOpaque(true);
		borderNodeColorButton.setBorderPainted(false);
		borderNodeColorButton.setToolTipText("Value: R:" + Util.NodeBorderColor.getRed() + " G:"
				+ Util.NodeBorderColor.getGreen() + " B:" + Util.NodeBorderColor.getBlue() + " - "
				+ String.format("#%02X%02X%02X", Util.NodeBorderColor.getRed(), Util.NodeBorderColor.getGreen(),
						Util.NodeBorderColor.getBlue()));

		borderNodeColorButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
		borderNodeColorButton.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {

				Color initialcolor = borderNodeColorButton.getBackground();
				Color color = JColorChooser.showDialog(null, "Select a color", initialcolor);
				if (color == null)
					color = initialcolor;
				borderNodeColorButton.setBackground(color);
				borderNodeColorButton.setForeground(color);
				borderNodeColorButton.setOpaque(true);
				borderNodeColorButton.setBorderPainted(false);
				Util.NodeBorderColor = color;
				P2LocationProps.setProperty("p2location.NodeBorderColor",
						color.getRed() + "#" + color.getGreen() + "#" + color.getBlue());

				borderNodeColorButton.setToolTipText("Value: R:" + Util.NodeBorderColor.getRed() + " G:"
						+ Util.NodeBorderColor.getGreen() + " B:" + Util.NodeBorderColor.getBlue() + " - "
						+ String.format("#%02X%02X%02X", Util.NodeBorderColor.getRed(), Util.NodeBorderColor.getGreen(),
								Util.NodeBorderColor.getBlue()));

				if (myNetwork != null && netView != null) {
					Util.updateNodesStyles(myNetwork, netView);
				}
			}
		});

		node_border_panel.add(borderNodeColorButton);

		offset_y = 50;
		if (!Util.isWindows())// MacOS or Unix
			offset_x -= 5;
		SpinnerModel model_opacity_node_border = new SpinnerNumberModel(Util.node_border_opacity.intValue(), // initial
				// value
				0, // min
				255, // max
				1); // step
		spinner_opacity_node_border = new JSpinner(model_opacity_node_border);
		spinner_opacity_node_border.setBounds(offset_x, offset_y, 60, 20);
		JComponent comp_opacitiy_node_border = spinner_opacity_node_border.getEditor();
		JFormattedTextField field_opacity_node_border = (JFormattedTextField) comp_opacitiy_node_border.getComponent(0);
		DefaultFormatter formatter_opacity_node_border = (DefaultFormatter) field_opacity_node_border.getFormatter();
		formatter_opacity_node_border.setCommitsOnValidEdit(true);
		spinner_opacity_node_border.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				Util.node_border_opacity = (Integer) spinner_opacity_node_border.getValue();
				P2LocationProps.setProperty("p2location.node_border_opacity", Util.node_border_opacity.toString());

				if (myNetwork != null && netView != null) {
					Util.updateNodesStyles(myNetwork, netView);
				}
			}
		});
		spinner_opacity_node_border.setToolTipText("Set a value between 0 (transparent) and 255 (opaque).");
		node_border_panel.add(spinner_opacity_node_border);
		offset_y += 30;

		SpinnerModel width_node_border_spinner = new SpinnerNumberModel(Util.node_border_width, // initial
				// value
				1, // min
				10, // max
				0.1); // step
		spinner_width_node_border = new JSpinner(width_node_border_spinner);
		spinner_width_node_border.setBounds(offset_x, offset_y, 60, 20);
		JComponent comp_width_node_border = spinner_width_node_border.getEditor();
		JFormattedTextField field_width_node_border = (JFormattedTextField) comp_width_node_border.getComponent(0);
		DefaultFormatter formatter_width_node_border = (DefaultFormatter) field_width_node_border.getFormatter();
		formatter_width_node_border.setCommitsOnValidEdit(true);
		spinner_width_node_border.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				Util.node_border_width = (double) spinner_width_node_border.getValue();
				P2LocationProps.setProperty("p2location.node_border_width", String.valueOf(Util.node_border_width));

				if (myNetwork != null && netView != null) {
					Util.updateNodesStyles(myNetwork, netView);
				}
			}
		});
		spinner_width_node_border.setToolTipText("Set a value between 1 and 10.");
		node_border_panel.add(spinner_width_node_border);
	}

	/**
	 * Method responsible for initializing pymol setting panel
	 * 
	 * @param offset_x offset x
	 */
	private void init_pymol_panel(int offset_x) {

		pymol_panel = new JPanel();
		pymol_panel.setBackground(Color.WHITE);
		pymol_panel.setBorder(BorderFactory.createTitledBorder("PyMOL"));
		pymol_panel.setLayout(null);

		int offset_y = 10;
		JLabel pymolPath_label = new JLabel("Application path:");
		pymolPath_label.setFont(new java.awt.Font("Tahoma", Font.PLAIN, 12));
		if (Util.isUnix())
			pymolPath_label.setBounds(10, offset_y, 130, 40);
		else
			pymolPath_label.setBounds(10, offset_y, 100, 40);
		pymol_panel.add(pymolPath_label);
		offset_y += 30;

		pymolPathStr = new JLabel("???");
		pymolPathStr.setFont(new java.awt.Font("Tahoma", Font.ITALIC, 12));
		pymolPathStr.setBounds(10, offset_y, 350, 40);
		pymol_panel.add(pymolPathStr);

		offset_y = 20;
		if (!Util.isWindows())// MacOS or Unix
			offset_x -= 5;

		Icon iconPyMOLBtn = new ImageIcon(getClass().getResource("/images/pyMOL_logo.png"));
		JButton pyMOL_pathButton = new JButton(iconPyMOLBtn);
		if (Util.isWindows())
			offset_x -= 5;

		offset_y -= 5;
		pyMOL_pathButton.setBounds(offset_x, offset_y, 30, 30);

		pyMOL_pathButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
		pyMOL_pathButton.setToolTipText("Select the PyMOL software");

		pyMOL_pathButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				pymolPathStr.setText(getPyMOLPath());
				Util.PYMOL_PATH = pymolPathStr.getText();
				P2LocationProps.setProperty("p2location.pymol_path", pymolPathStr.getText());
			}
		});

		pymol_panel.add(pyMOL_pathButton);
	}

	/**
	 * Select PyMOL file
	 * 
	 * @return path
	 */
	private String getPyMOLPath() {
		JFileChooser choosePyMOL = new JFileChooser();
		choosePyMOL.setFileSelectionMode(JFileChooser.FILES_ONLY);
		choosePyMOL.setDialogTitle("Select PyMOL file");

		if (choosePyMOL.showOpenDialog(choosePyMOL) != JFileChooser.APPROVE_OPTION)
			return Util.PYMOL_PATH;

		return choosePyMOL.getSelectedFile().toString();
	}

	/**
	 * Method responsible for putting objects to Panel
	 */
	private void setFrameObjects() {

		// use the border layout for this CytoPanel
		setLayout(new GridBagLayout());
		this.setBackground(Color.WHITE);

		int offset_x = 205;// MacOS and Unix
		int ipdax = 300;// MacOS and Unix
		if (Util.isWindows()) {
			offset_x = 200;
			ipdax = 250;
		}

		this.init_params();
		this.init_filter(offset_x);
		this.init_options(offset_x);
		this.init_prediction(offset_x);

//		this.init_link_style_features(offset_x, button_width);
//		this.init_link_edge_labels_features(offset_x, button_width);

		this.add(link_panel, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.NORTH,
				GridBagConstraints.BOTH, new Insets(5, 0, 0, 0), ipdax, 440));

//		this.init_node_style_features(offset_x, button_width);
//		this.init_node_border_features(offset_x, button_width);

//		this.add(node_panel, new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0, GridBagConstraints.NORTH,
//				GridBagConstraints.BOTH, new Insets(0, 0, 5, 0), ipdax, 170));
//
//		this.init_pymol_panel(offset_x);
//		this.checkPyMOLname();
//
//		this.add(pymol_panel, new GridBagConstraints(0, 2, 1, 1, 1.0, 1.0, GridBagConstraints.NORTH,
//				GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), ipdax, 75));

	}

	/**
	 * Method responsible for updating parameters
	 */
	public static void updateParamsValue() {

		show_intra_link.setSelected(Util.showIntraLinks);
		enable_epochs.setSelected(Util.showInterLinks);
		enable_score.setSelected(Util.showPTMs);
		show_monolinks.setSelected(Util.showMonolinkedPeptides);

		if (Util.showIntraLinks) {
			processButton.setEnabled(true);
			spinner_score_intralink.setEnabled(true);
		} else {

			processButton.setEnabled(false);
			spinner_score_intralink.setEnabled(false);
		}

		if (Util.showInterLinks) {
			interLinkColorButton.setEnabled(true);
			spinner_score_interlink.setEnabled(true);
		} else {
			interLinkColorButton.setEnabled(false);
			spinner_score_interlink.setEnabled(false);
		}

		if (Util.showPTMs) {
			ptmColorButton.setEnabled(true);
		} else {
			ptmColorButton.setEnabled(false);
		}

		if (Util.showMonolinkedPeptides) {
			monolinkColorButton.setEnabled(true);
		} else {
			monolinkColorButton.setEnabled(false);
		}

		processButton.setBackground(Util.IntraLinksColor);
		processButton.setForeground(Util.IntraLinksColor);

		interLinkColorButton.setBackground(Util.InterLinksColor);
		interLinkColorButton.setForeground(Util.InterLinksColor);

		spinner_opacity_edge_link.setValue(Util.edge_link_opacity);
		spinner_width_edge_link.setValue(Util.edge_link_width);

		spinner_score_intralink.setValue(Util.intralink_threshold_score);
		spinner_score_interlink.setValue(Util.interlink_threshold_score);
		spinner_score_combinedlink.setValue(Util.combinedlink_threshold_score);

		if (!Util.showIntraLinks && !Util.showInterLinks) {
			show_links_legend.setEnabled(false);
		} else {
			show_links_legend.setEnabled(true);
		}

		if (Util.showLinksLegend) {
			spinner_font_size_link_legend.setEnabled(true);
			spinner_opacity_edge_label.setEnabled(true);
		} else {
			spinner_font_size_link_legend.setEnabled(false);
			spinner_opacity_edge_label.setEnabled(false);
		}

		spinner_font_size_link_legend.setValue(Util.edge_label_font_size);
		spinner_opacity_edge_label.setValue(Util.edge_label_opacity);

		spinner_font_size_node.setValue(Util.node_label_font_size);
		borderNodeColorButton.setBackground(Util.NodeBorderColor);
		borderNodeColorButton.setForeground(Util.NodeBorderColor);
		spinner_opacity_node_border.setValue(Util.node_border_opacity);
		spinner_width_node_border.setValue(Util.node_border_width);

	}

	/**
	 * Get current component
	 */
	public Component getComponent() {
		return this;
	}

	/**
	 * Returns Cytoscape panel location
	 */
	public CytoPanelName getCytoPanelName() {
		return CytoPanelName.WEST;
	}

	/**
	 * Returns panel title
	 */
	public String getTitle() {
		return "P2Location Settings";
	}

	/**
	 * Return the logo
	 */
	public Icon getIcon() {
		ImageIcon imgIcon = new ImageIcon(getClass().getResource("/images/logo.png"));
		return imgIcon;
	}

}
