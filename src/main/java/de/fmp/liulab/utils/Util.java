package de.fmp.liulab.utils;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.swing.AbstractListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListModel;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.view.model.VisualLexicon;
import org.cytoscape.view.model.VisualProperty;
import org.cytoscape.view.presentation.customgraphics.CyCustomGraphics2;
import org.cytoscape.view.presentation.customgraphics.CyCustomGraphics2Factory;
import org.cytoscape.view.presentation.property.ArrowShapeVisualProperty;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.presentation.property.LineTypeVisualProperty;
import org.cytoscape.view.presentation.property.NodeShapeVisualProperty;
import org.cytoscape.view.presentation.property.values.Bend;
import org.cytoscape.view.presentation.property.values.BendFactory;
import org.cytoscape.view.presentation.property.values.Handle;
import org.cytoscape.view.presentation.property.values.HandleFactory;
import org.cytoscape.view.presentation.property.values.ObjectPosition;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.work.TaskMonitor;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import de.fmp.liulab.internal.UpdateViewListener;
import de.fmp.liulab.internal.view.JTableRowRenderer;
import de.fmp.liulab.model.CrossLink;
import de.fmp.liulab.model.Fasta;
import de.fmp.liulab.model.PDB;
import de.fmp.liulab.model.PTM;
import de.fmp.liulab.model.Protein;
import de.fmp.liulab.model.ProteinDomain;
import de.fmp.liulab.model.Residue;
import de.fmp.liulab.task.MainSingleNodeTask;
import de.fmp.liulab.task.ProcessProteinLocationTask;

/**
 * Class responsible for getting / setting common methods
 * 
 * @author borges.diogo
 *
 */
public class Util {

	public static String PROTEIN_SCALING_FACTOR_COLUMN_NAME = "scaling_factor";
	public static String HORIZONTAL_EXPANSION_COLUMN_NAME = "is_horizontal_expansion";
	public static String PROTEIN_DOMAIN_COLUMN = "domain_annotation";
	public static String PREDICTED_PROTEIN_DOMAIN_COLUMN = "predicted_domains";
	public static String CONFLICTED_PREDICTED_RESIDUES_COLUMN = "conflicted_residues";
	public static String PROTEIN_SEQUENCE_COLUMN = "sequence";
	public static String PTM_COLUMN = "ptms";
	public static String MONOLINK_COLUMN = "monolinks";
	public static String PROTEIN_LENGTH_A = "length_protein_a";
	public static String PROTEIN_LENGTH_B = "length_protein_b";
	public static String NODE_LABEL_POSITION = "NODE_LABEL_POSITION";
	public static String XL_PROTEIN_A_B = "crosslinks_ab";
	public static String XL_PROTEIN_B_A = "crosslinks_ba";
	public static String PROTEIN_A = "protein_a";
	public static String PROTEIN_B = "protein_b";
	private static String XL_SCORE_AB = "score_ab";
	private static String XL_SCORE_BA = "score_ba";
	public static String XL_COMB_SCORE = "ppi_score";
	public static String PYMOL_PATH = "";
	public static String PDB_PATH = "\"/Applications/\"";
	public static List<CyNetwork> myCyNetworkList = new ArrayList<CyNetwork>();
	public static char REACTION_RESIDUE = 'K';

	private static String OS = System.getProperty("os.name").toLowerCase();

	public final static float OFFSET_BEND = 2;
	private final static float OFFSET_MONOLINK = 1;
	private final static float PTM_LENGTH = 45;
	private static String edge_label_blank_spaces = "\n\n";

	public static Color PTMColor = new Color(153, 153, 153);
	public static Color IntraLinksColor = new Color(102, 102, 102);
	public static Color InterLinksColor = new Color(102, 102, 102);
	public static Color MonoLinksPeptideColor = new Color(0, 153, 255);
	public static Color NodeBorderColor = new Color(315041);// Dark green
	public static boolean showLinksLegend = false;
	public static boolean showIntraLinks = true;
	public static boolean showInterLinks = true;
	public static boolean showPTMs = false;
	public static boolean showMonolinkedPeptides = false;
	public static boolean showResidues = true;
	public static Integer edge_label_font_size = 12;
	public static Integer node_label_font_size = 12;
	public static double node_label_factor_size = 1;
	public static Integer edge_label_opacity = 120;
	public static Integer edge_link_opacity = 120;
	public static Integer node_border_opacity = 200;
	public static double edge_link_width = 2;
	public static double node_border_width = 1.5;
	public static boolean isProtein_expansion_horizontal = true;
	public static boolean isProteinDomainPfam = false;
	public static boolean stopUpdateViewer = false;
	public static double intralink_threshold_score = 0;
	public static double interlink_threshold_score = 0;
	public static double combinedlink_threshold_score = 0;
	public static Integer epochs = 100;
	public static boolean getEpochs = false;
	public static Integer specCount = 5;
	public static boolean getSpecCount = false;
	public static double threshold_score = 10;
	public static boolean getThreshold_score = false;
	public static Integer neighborAA = 5;
	public static Integer transmemNeighborAA = 5;
	public static boolean considerConflict = true;

	// Map<Network name,List<Protein>
	public static Map<String, List<Protein>> proteinsMap = new HashMap<String, List<Protein>>();
	public static Map<String, Color> proteinDomainsColorMap = new HashMap<String, Color>();
	public static List<java.awt.Color> available_domain_colors = new ArrayList<Color>();

	// Map<Network name, Map<Protein - Node SUID, List<PTM>>
	public static Map<String, Map<Long, List<PTM>>> ptmsMap = new HashMap<String, Map<Long, List<PTM>>>();

	// Map<Network name, Map<Protein - Node SUID, Protein>
	public static Map<String, Map<Long, Protein>> monolinksMap = new HashMap<String, Map<Long, Protein>>();

	public static Map<CyNode, Tuple2> mapLastNodesPosition = new HashMap<CyNode, Tuple2>();

	public static float proteinLength;

	public static void setProteinLength(float value) {
		proteinLength = value;
	}

	public static float getProteinLengthScalingFactor() {
		return (float) (proteinLength * node_label_factor_size);
	}

	public static float getProteinLength() {
		return (float) (proteinLength);
	}

	/**
	 * Method responsible for updating protein domains color Map
	 * 
	 * @param proteinDomains list of protein domains
	 */
	public static void updateProteinDomainsColorMap(List<ProteinDomain> proteinDomains) {

		for (ProteinDomain ptnDomain : proteinDomains) {
			if (!proteinDomainsColorMap.containsKey(ptnDomain.name)) {

				proteinDomainsColorMap.put(ptnDomain.name, available_domain_colors
						.get(proteinDomainsColorMap.size() % Util.available_domain_colors.size()));
			}
		}
	}

	/**
	 * Method responsible for updating Map Nodes Position
	 * 
	 * @param current_node current node
	 * @param nodeView     current node view
	 */
	public static void updateMapNodesPosition(CyNode current_node, View<CyNode> nodeView) {

		double current_posX = nodeView.getVisualProperty(BasicVisualLexicon.NODE_X_LOCATION);
		double current_posY = nodeView.getVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION);
		if (Util.mapLastNodesPosition.containsKey(current_node)) {
			double last_posX = (double) Util.mapLastNodesPosition.get(current_node).getFirst();
			double last_posY = (double) Util.mapLastNodesPosition.get(current_node).getSecond();
			if (current_posX == last_posX && current_posY == last_posY)
				return;
			else
				Util.mapLastNodesPosition.put(current_node, new Tuple2(current_posX, current_posY));
		} else {
			Util.mapLastNodesPosition.put(current_node, new Tuple2(current_posX, current_posY));
		}
	}

	/**
	 * Initialize protein domain colors map
	 */
	public static void init_availableProteinDomainColorsMap() {
		if (Util.available_domain_colors.size() == 0) {
			Util.available_domain_colors.add(new Color(0, 64, 128, 100));
			Util.available_domain_colors.add(new Color(0, 128, 64, 100));
			Util.available_domain_colors.add(new Color(255, 128, 0, 100));
			Util.available_domain_colors.add(new Color(128, 128, 0, 100));
			Util.available_domain_colors.add(new Color(128, 128, 128, 100));
			Util.available_domain_colors.add(new Color(128, 64, 64, 100));
			Util.available_domain_colors.add(new Color(0, 128, 192, 100));
			Util.available_domain_colors.add(new Color(174, 0, 0, 100));
			Util.available_domain_colors.add(new Color(255, 255, 0, 100));
			Util.available_domain_colors.add(new Color(0, 64, 0, 100));
			Util.available_domain_colors.add(new Color(204, 0, 0, 100));
			Util.available_domain_colors.add(new Color(255, 198, 0, 100));
			Util.available_domain_colors.add(new Color(10, 60, 128, 100));
			Util.available_domain_colors.add(new Color(20, 118, 60, 100));
			Util.available_domain_colors.add(new Color(155, 158, 0, 100));
			Util.available_domain_colors.add(new Color(28, 148, 0, 100));
			Util.available_domain_colors.add(new Color(155, 100, 128, 100));
			Util.available_domain_colors.add(new Color(100, 64, 64, 100));
			Util.available_domain_colors.add(new Color(100, 128, 192, 100));
			Util.available_domain_colors.add(new Color(254, 20, 0, 100));
			Util.available_domain_colors.add(new Color(255, 255, 100, 100));
			Util.available_domain_colors.add(new Color(0, 64, 100, 100));
			Util.available_domain_colors.add(new Color(204, 100, 250, 100));
			Util.available_domain_colors.add(new Color(255, 208, 100, 100));
			Util.available_domain_colors.add(Color.BLACK);
			Util.available_domain_colors.add(Color.BLUE);
			Util.available_domain_colors.add(Color.CYAN);
			Util.available_domain_colors.add(Color.DARK_GRAY);
			Util.available_domain_colors.add(Color.GRAY);
			Util.available_domain_colors.add(Color.GREEN);
			Util.available_domain_colors.add(Color.LIGHT_GRAY);
			Util.available_domain_colors.add(Color.MAGENTA);
			Util.available_domain_colors.add(Color.ORANGE);
			Util.available_domain_colors.add(Color.PINK);
			Util.available_domain_colors.add(Color.RED);
			Util.available_domain_colors.add(Color.YELLOW);
		}
	}

	/**
	 * Create dictionary with residues
	 * 
	 * @return dictionary
	 */
	public static Map<ByteBuffer, Integer> createResiduesDict() {

		Map<ByteBuffer, Integer> ResiduesDict = new HashMap<ByteBuffer, Integer>();// e.g <GLU, E>
		ResiduesDict.put(ByteBuffer.wrap(new byte[] { 71, 76, 89 }), 71);// Glycine (G)
		ResiduesDict.put(ByteBuffer.wrap(new byte[] { 65, 76, 65 }), 65);// Alanine (A)
		ResiduesDict.put(ByteBuffer.wrap(new byte[] { 83, 69, 82 }), 83);// Serine (S)
		ResiduesDict.put(ByteBuffer.wrap(new byte[] { 80, 82, 79 }), 80);// Proline (P)
		ResiduesDict.put(ByteBuffer.wrap(new byte[] { 86, 65, 76 }), 86);// Valine (V)
		ResiduesDict.put(ByteBuffer.wrap(new byte[] { 84, 72, 82 }), 84);// Threonine (T)
		ResiduesDict.put(ByteBuffer.wrap(new byte[] { 67, 89, 83 }), 67);// Cystein (C)
		ResiduesDict.put(ByteBuffer.wrap(new byte[] { 73, 76, 69 }), 73);// Isoleucine (I)
		ResiduesDict.put(ByteBuffer.wrap(new byte[] { 76, 69, 85 }), 76);// Leucine (L)
		ResiduesDict.put(ByteBuffer.wrap(new byte[] { 65, 83, 78 }), 78);// Asparagine (N)
		ResiduesDict.put(ByteBuffer.wrap(new byte[] { 65, 83, 80 }), 68);// Aspartic Acid (D)
		ResiduesDict.put(ByteBuffer.wrap(new byte[] { 71, 76, 78 }), 81);// Glutamine (Q)
		ResiduesDict.put(ByteBuffer.wrap(new byte[] { 76, 89, 83 }), 75);// Lysine (K)
		ResiduesDict.put(ByteBuffer.wrap(new byte[] { 71, 76, 88 }), 90);// Glutamic Acid or Glutamine (Z)
		ResiduesDict.put(ByteBuffer.wrap(new byte[] { 71, 76, 85 }), 69);// Glutamic Acid (E)
		ResiduesDict.put(ByteBuffer.wrap(new byte[] { 77, 69, 84 }), 77);// Methionine (M)
		ResiduesDict.put(ByteBuffer.wrap(new byte[] { 72, 73, 83 }), 72);// Histidine (H)
		ResiduesDict.put(ByteBuffer.wrap(new byte[] { 80, 72, 69 }), 70);// Phenilanyne (F)
		ResiduesDict.put(ByteBuffer.wrap(new byte[] { 83, 68, 67 }), 85);// Selenocysteine (U)
		ResiduesDict.put(ByteBuffer.wrap(new byte[] { 65, 82, 71 }), 82);// Arginine (R)
		ResiduesDict.put(ByteBuffer.wrap(new byte[] { 84, 89, 82 }), 89);// Tyrosine (Y)
		ResiduesDict.put(ByteBuffer.wrap(new byte[] { 84, 82, 80 }), 87);// Tyrosine (Y)

		return ResiduesDict;
	}

	/**
	 * Check if a specific edge has been modified
	 * 
	 * @param myNetwork current network
	 * @param netView   current network view
	 * @param edge      current edge
	 * @return true if the edge is modified, otherwise, returns false.
	 */
	public static boolean isEdgeModified(CyNetwork myNetwork, CyNetworkView netView, CyEdge edge) {

		if (myNetwork == null || netView == null || edge == null)
			return false;

		VisualStyle style = MainSingleNodeTask.style;
		if (style == null)
			style = ProcessProteinLocationTask.style;

		View<CyEdge> edgeView = netView.getEdgeView(edge);
		if (!edgeView.getVisualProperty(BasicVisualLexicon.EDGE_TOOLTIP)
				.equals(style.getDefaultValue(BasicVisualLexicon.EDGE_TOOLTIP))
				&& !edgeView.getVisualProperty(BasicVisualLexicon.EDGE_LABEL)
						.equals(style.getDefaultValue(BasicVisualLexicon.EDGE_LABEL))
				&& !edgeView.getVisualProperty(BasicVisualLexicon.EDGE_TRANSPARENCY)
						.equals(style.getDefaultValue(BasicVisualLexicon.EDGE_TRANSPARENCY))) {
			return true;
		} else
			return false;
	}

	/**
	 * Check if a specific node has been modified
	 * 
	 * @param myNetwork current network
	 * @param netView   current network view
	 * @param node      current node
	 * @return true if node is modified otherwise returns false
	 */
	public static boolean IsNodeModified(CyNetwork myNetwork, CyNetworkView netView, CyNode node) {

		if (myNetwork == null || netView == null || node == null)
			return false;

		Object length_other_protein_a;
		Object length_other_protein_b;

		CyRow proteinA_node_row = myNetwork.getRow(node);

		length_other_protein_a = proteinA_node_row.getRaw(PROTEIN_LENGTH_A);
		length_other_protein_b = proteinA_node_row.getRaw(PROTEIN_LENGTH_B);

		if (length_other_protein_a == null) {
			if (length_other_protein_b == null)
				length_other_protein_a = 10;
			else
				length_other_protein_a = length_other_protein_b;
		}

		// Check if 'length_other_protein_a' is a number
		try {
			String _length_other_protein_a = length_other_protein_a.toString();
			Integer.parseInt(_length_other_protein_a);
		} catch (NumberFormatException e) {
			return true;
		}

		View<CyNode> proteinA_nodeView = netView.getNodeView(node);
		float proteinA_node_width = ((Number) proteinA_nodeView.getVisualProperty(BasicVisualLexicon.NODE_WIDTH))
				.floatValue();

		double current_factor_scaling_length_protein = myNetwork.getRow(node).get(PROTEIN_SCALING_FACTOR_COLUMN_NAME,
				Double.class) == null ? 1
						: myNetwork.getRow(node).get(PROTEIN_SCALING_FACTOR_COLUMN_NAME, Double.class);

		float scaling_protein_size = (float) (((Number) length_other_protein_a).floatValue()
				* current_factor_scaling_length_protein);
		if (scaling_protein_size == proteinA_node_width) {// Expansion - horizontal
			return checkModifiedNode(myNetwork, netView, node);

		} else {// Expansion - vertical

			float proteinA_node_height = ((Number) proteinA_nodeView.getVisualProperty(BasicVisualLexicon.NODE_HEIGHT))
					.floatValue();

			if (scaling_protein_size == proteinA_node_height) {
				return checkModifiedNode(myNetwork, netView, node);
			}
			return false;
		}
	}

	/**
	 * Method responsible for checking if a node is modified
	 * 
	 * @param myNetwork
	 * @param netView
	 * @param style
	 * @param node
	 * @return true if node is modified otherwise returns false
	 */
	private static boolean checkModifiedNode(CyNetwork myNetwork, CyNetworkView netView, CyNode node) {
		View<CyNode> nodeView = netView.getNodeView(node);

		VisualLexicon lexicon = MainSingleNodeTask.lexicon;
		if (lexicon == null)
			lexicon = ProcessProteinLocationTask.lexicon;

		if (lexicon == null)
			return false;

		VisualStyle style = MainSingleNodeTask.style;
		if (style == null)
			style = ProcessProteinLocationTask.style;

		if (style == null)
			return false;

		// Try to get the label visual property by its ID
		VisualProperty<?> vp_label_position = lexicon.lookup(CyNode.class, Util.NODE_LABEL_POSITION);
		if (vp_label_position != null) {

			// If the property is supported by this rendering engine,
			// use the serialization string value to create the actual property value

			ObjectPosition position = (ObjectPosition) vp_label_position.parseSerializableString("W,E,r,-10.00,0.00");

			// If the parsed value is ok, apply it to the visual style
			// as default value or a visual mapping

			if (position != null) {
				ObjectPosition current_position = (ObjectPosition) nodeView.getVisualProperty(vp_label_position);
				if (current_position.getJustify() == position.getJustify()
						&& current_position.getOffsetX() == position.getOffsetX()
						&& current_position.getOffsetY() == position.getOffsetY()
						&& (!nodeView.getVisualProperty(BasicVisualLexicon.NODE_TOOLTIP)
								.equals(style.getDefaultValue(BasicVisualLexicon.NODE_TOOLTIP)))) {// Expansion
																									// horizontal
					isProtein_expansion_horizontal = true;
					if (myNetwork.getRow(node).get(HORIZONTAL_EXPANSION_COLUMN_NAME, Boolean.class) != null)
						myNetwork.getRow(node).set(HORIZONTAL_EXPANSION_COLUMN_NAME, isProtein_expansion_horizontal);
					return true;
				} else {
					isProtein_expansion_horizontal = false;
					if (myNetwork.getRow(node).get(HORIZONTAL_EXPANSION_COLUMN_NAME, Boolean.class) != null)
						myNetwork.getRow(node).set(HORIZONTAL_EXPANSION_COLUMN_NAME, isProtein_expansion_horizontal);
					position = (ObjectPosition) vp_label_position.parseSerializableString("N,S,c,0.00,0.00");
					if (current_position.getJustify() == position.getJustify()
							&& current_position.getOffsetX() == position.getOffsetX()
							&& current_position.getOffsetY() == position.getOffsetY()
							&& (!nodeView.getVisualProperty(BasicVisualLexicon.NODE_TOOLTIP)
									.equals(style.getDefaultValue(BasicVisualLexicon.NODE_TOOLTIP))))// Expansion
																										// vertical
						return true;
					else
						return false;
				}
			} else
				return false;
		} else
			return false;
	}

	/**
	 * Add all edges to the network
	 * 
	 * @param myNetwork               current network
	 * @param node                    current node
	 * @param style                   current style
	 * @param netView                 current network view
	 * @param nodeView                current node view
	 * @param handleFactory           handle factory
	 * @param bendFactory             bend factory
	 * @param lexicon                 lexicon
	 * @param proteinLength           current protein length
	 * @param intraLinks              all intra-links
	 * @param interLinks              all interlinks
	 * @param taskMonitor             task monitor
	 * @param textLabel_status_result display current status
	 * @return true if all edges have been added or updated
	 */
	public static boolean addOrUpdateEdgesToNetwork(CyNetwork myNetwork, CyNode node, VisualStyle style,
			CyNetworkView netView, View<CyNode> nodeView, HandleFactory handleFactory, BendFactory bendFactory,
			VisualLexicon lexicon, float proteinLength, Protein protein, final TaskMonitor taskMonitor,
			JLabel textLabel_status_result) {
		boolean HasAdjacentEdges = false;
		boolean IsIntraLink = false;
		boolean ContainsInterLink = false;
		boolean ContainsIntraLink = false;
		boolean IsMixedNode = false;
		List<CrossLink> intraLinks = protein.intraLinks;
		List<CrossLink> interLinks = protein.interLinks;

		if (myNetwork == null || node == null || style == null || netView == null) {
			return false;
		} else {
			nodeView = netView.getNodeView(node);
		}

		int total_edges = 0;
		int old_progress = 0;
		int summary_processed = 0;
		if (taskMonitor != null)
			total_edges = myNetwork.getAdjacentEdgeList(node, CyEdge.Type.ANY).size();

		for (CyEdge edge : myNetwork.getAdjacentEdgeIterable(node, CyEdge.Type.ANY)) {

			try {

				if (stopUpdateViewer)// This variable is set true when applyLayoutThread is interrupted
										// (MainSingleNodeTask)
					break;

				// Check if the edge was inserted by this app
				String edge_name = myNetwork.getDefaultEdgeTable().getRow(edge.getSUID()).get(CyNetwork.NAME,
						String.class);

				CyNode sourceNode = myNetwork.getEdge(edge.getSUID()).getSource();
				CyNode targetNode = myNetwork.getEdge(edge.getSUID()).getTarget();

				if (sourceNode.getSUID() == targetNode.getSUID()) {
					IsIntraLink = true;
				} else {
					IsIntraLink = false;
				}

				View<CyEdge> currentEdgeView = netView.getEdgeView(edge);
				while (currentEdgeView == null) {
					netView.updateView();
					try {
						Thread.sleep(200);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
					currentEdgeView = netView.getEdgeView(edge);
				}

				if (!edge_name.startsWith("[Source:") && !edge_name.contains("PTM")) {// New edges
					HasAdjacentEdges = true;

					if (IsIntraLink) {
						ContainsIntraLink = true;
						currentEdgeView.setLockedValue(BasicVisualLexicon.EDGE_VISIBLE, false);

						plotIntraLinks(myNetwork, nodeView, node, netView, handleFactory, bendFactory, style,
								proteinLength, intraLinks);// Add or update intralinks

					} else {
						ContainsInterLink = true;

						if (isEdgeModified(myNetwork, netView, edge)) {
							restoreEdgeStyle(myNetwork, node, netView, handleFactory, bendFactory, style, lexicon, edge,
									sourceNode, targetNode, edge_name, proteinLength, IsIntraLink);
						} else {// keep the original edge
							currentEdgeView.setLockedValue(BasicVisualLexicon.EDGE_VISIBLE, true);
						}

						if (showInterLinks) {
							plotInterLinks(myNetwork, nodeView, netView, handleFactory, bendFactory, style, node,
									sourceNode, targetNode, lexicon, proteinLength, interLinks);
							currentEdgeView.setLockedValue(BasicVisualLexicon.EDGE_VISIBLE, false);
						}
					}

				} else { // Update all sites of the current selected node
					HasAdjacentEdges = true;

					if (!edge_name.contains("PTM")) {

						if (isEdgeModified(myNetwork, netView, edge)) {
							restoreEdgeStyle(myNetwork, node, netView, handleFactory, bendFactory, style, lexicon, edge,
									sourceNode, targetNode, edge_name, proteinLength, IsIntraLink);
						} else {// Hide the modified edge (interlink)
							currentEdgeView.setLockedValue(BasicVisualLexicon.EDGE_VISIBLE, false);
						}

						if (showInterLinks) {

							String[] edgeNameArr = edge_name.split("\\[|\\]");
							String[] position1 = edgeNameArr[1].split("\\(|\\)");
							String[] position2 = edgeNameArr[3].split("\\(|\\)");

							String protein_a = position1[0].split("Source: ")[1].trim();
							String protein_b = position2[0].split("Target: ")[1].trim();

							CrossLink link = interLinks.stream()
									.filter(value -> value.pos_site_a == Integer.parseInt(position1[1])
											&& value.pos_site_b == Integer.parseInt(position2[1])
											&& value.protein_a.equals(protein_a) && value.protein_b.equals(protein_b))
									.findFirst().get();
							String location = "";
							if (link != null && link.location != null && !link.location.equals("UK")) {
								location = link.location;
							}
							updateInterLinkEdgesPosition(myNetwork, node, netView, handleFactory, bendFactory, style,
									lexicon, edge, sourceNode, targetNode, edge_name, proteinLength, location);
						}
					}
				}
			} catch (Exception e) {
				System.out.println(e.getMessage());
			} finally {

				summary_processed++;
				progressBar(summary_processed, old_progress, total_edges, "Defining styles for cross-links: ",
						taskMonitor, textLabel_status_result);

			}

		}

		if (ContainsInterLink && !ContainsIntraLink && intraLinks.size() > 0)
			IsMixedNode = true;

		if (!HasAdjacentEdges || IsMixedNode) { // Node is alone and it does not have adjacent
			if (taskMonitor != null) {
				taskMonitor.showMessage(TaskMonitor.Level.INFO, "Defining styles for intra links: 98%");
			}

			plotIntraLinks(myNetwork, nodeView, node, netView, handleFactory, bendFactory, style, proteinLength,
					intraLinks);
		}

		UpdateViewListener.isNodeModified = true;
		ProcessProteinLocationTask.isPlotDone = true;
		return true;
	}

	/**
	 * Plot all intralink
	 * 
	 * @param myNetwork     current network
	 * @param nodeView      current node view
	 * @param original_node original node
	 * @param netView       current network view
	 * @param handleFactory handle factory
	 * @param bendFactory   bend factory
	 * @param style         current style
	 * @param proteinLength length of the current protein
	 * @param intraLinks    all intralinks
	 */
	private static void plotIntraLinks(CyNetwork myNetwork, View<CyNode> nodeView, CyNode original_node,
			CyNetworkView netView, HandleFactory handleFactory, BendFactory bendFactory, VisualStyle style,
			float proteinLength, List<CrossLink> intraLinks) {

		double initial_positionX_node = getXPositionOf(nodeView);
		double initial_positionY_node = getYPositionOf(nodeView);
		double center_position_node = (proteinLength * Util.node_label_factor_size) / 2.0;

		double x_or_y_Pos_source = 0;
		double xl_pos_source = 0;

		double x_or_y_Pos_target = 0;
		double xl_pos_target = 0;

		if (showIntraLinks) {
			for (int countEdge = 0; countEdge < intraLinks.size(); countEdge++) {

				final String egde_name_added_by_app = "Edge" + countEdge + " [Source: "
						+ intraLinks.get(countEdge).protein_a + " (" + intraLinks.get(countEdge).pos_site_a
						+ ")] [Target: " + intraLinks.get(countEdge).protein_b + " ("
						+ intraLinks.get(countEdge).pos_site_b + ")] - Score: " + intraLinks.get(countEdge).score;

				CyEdge current_edge = getEdge(myNetwork, egde_name_added_by_app, false);
				if (current_edge == null) {// Add a new edge if does not exist

					String node_name_source = intraLinks.get(countEdge).protein_a + " ["
							+ intraLinks.get(countEdge).pos_site_a + " - " + intraLinks.get(countEdge).pos_site_b
							+ "] - Source";

					CyNode new_node_source = myNetwork.addNode();
					myNetwork.getRow(new_node_source).set(CyNetwork.NAME, node_name_source);

					String node_name_target = intraLinks.get(countEdge).protein_a + " ["
							+ intraLinks.get(countEdge).pos_site_a + " - " + intraLinks.get(countEdge).pos_site_b
							+ "] - Target";

					CyNode new_node_target = myNetwork.addNode();
					myNetwork.getRow(new_node_target).set(CyNetwork.NAME, node_name_target);

					CyEdge newEdge = myNetwork.addEdge(new_node_source, new_node_target, true);
					myNetwork.getRow(newEdge).set(CyNetwork.NAME, egde_name_added_by_app);

					View<CyEdge> newEdgeView = netView.getEdgeView(newEdge);
					while (newEdgeView == null) {
						netView.updateView();
						try {
							Thread.sleep(200);
						} catch (InterruptedException e1) {
							e1.printStackTrace();
						}
						newEdgeView = netView.getEdgeView(newEdge);
					}

					Color linkColor = IntraLinksColor;
					CrossLink link = intraLinks.get(countEdge);

					if (link != null && link.location != null && !link.location.equals("UK")) {
						linkColor = Util.proteinDomainsColorMap.get(link.location);
					}

					newEdgeView.setLockedValue(BasicVisualLexicon.EDGE_PAINT, linkColor);
					newEdgeView.setLockedValue(BasicVisualLexicon.EDGE_SELECTED_PAINT, Color.RED);
					newEdgeView.setLockedValue(BasicVisualLexicon.EDGE_STROKE_SELECTED_PAINT, Color.RED);
					newEdgeView.setLockedValue(BasicVisualLexicon.EDGE_TRANSPARENCY, edge_link_opacity);

					newEdgeView.setLockedValue(BasicVisualLexicon.EDGE_LABEL, "[" + intraLinks.get(countEdge).pos_site_a
							+ "] - [" + intraLinks.get(countEdge).pos_site_b + "]");

					double score = intraLinks.get(countEdge).score;
					double log_score = round(-Math.log10(intraLinks.get(countEdge).score), 2);
					String tooltip = "";

					if (Double.isNaN(score)) {
						tooltip = "<html><p>[" + intraLinks.get(countEdge).pos_site_a + "] - ["
								+ intraLinks.get(countEdge).pos_site_b + "]</p></html>";
					} else {
						tooltip = "<html><p>[" + intraLinks.get(countEdge).pos_site_a + "] - ["
								+ intraLinks.get(countEdge).pos_site_b + "]</p><p><i>Score: " + score
								+ "</p><p><i>-Log(score): " + log_score + "</i></p></html>";
					}
					newEdgeView.setLockedValue(BasicVisualLexicon.EDGE_TOOLTIP, tooltip);
					newEdgeView.setLockedValue(BasicVisualLexicon.EDGE_LABEL_FONT_SIZE, edge_label_font_size);

					if (showLinksLegend) {
						newEdgeView.setLockedValue(BasicVisualLexicon.EDGE_LABEL_TRANSPARENCY, edge_label_opacity);
						newEdgeView.setLockedValue(BasicVisualLexicon.EDGE_LABEL_FONT_SIZE, edge_label_font_size);
					} else {
						newEdgeView.setLockedValue(BasicVisualLexicon.EDGE_LABEL_TRANSPARENCY, 0);
					}
					newEdgeView.setLockedValue(BasicVisualLexicon.EDGE_WIDTH, edge_link_width);
					newEdgeView.setLockedValue(BasicVisualLexicon.EDGE_TARGET_ARROW_SHAPE,
							ArrowShapeVisualProperty.NONE);
					newEdgeView.setLockedValue(BasicVisualLexicon.EDGE_SOURCE_ARROW_SHAPE,
							ArrowShapeVisualProperty.NONE);
					newEdgeView.setLockedValue(BasicVisualLexicon.EDGE_LINE_TYPE, LineTypeVisualProperty.SOLID);

					xl_pos_source = intraLinks.get(countEdge).pos_site_a;
					if (xl_pos_source <= center_position_node) { // [-protein_length/2, 0]
						x_or_y_Pos_source = (-center_position_node) + xl_pos_source;
					} else { // [0, protein_length/2]
						x_or_y_Pos_source = xl_pos_source - center_position_node;
					}
					if (isProtein_expansion_horizontal) {
						x_or_y_Pos_source += initial_positionX_node;
					} else {
						x_or_y_Pos_source += initial_positionY_node;
					}

					xl_pos_target = intraLinks.get(countEdge).pos_site_b;
					if (xl_pos_target <= center_position_node) { // [-protein_length/2, 0]
						x_or_y_Pos_target = (-center_position_node) + xl_pos_target;
					} else { // [0, protein_length/2]
						x_or_y_Pos_target = xl_pos_target - center_position_node;
					}
					if (isProtein_expansion_horizontal) {
						x_or_y_Pos_target += initial_positionX_node;
					} else {
						x_or_y_Pos_target += initial_positionY_node;
					}

					View<CyNode> new_node_source_view = netView.getNodeView(new_node_source);

					if (isProtein_expansion_horizontal) {
						new_node_source_view.setLockedValue(BasicVisualLexicon.NODE_X_LOCATION,
								(x_or_y_Pos_source) * Util.node_label_factor_size);
						new_node_source_view.setLockedValue(BasicVisualLexicon.NODE_Y_LOCATION, initial_positionY_node);
					} else {
						new_node_source_view.setLockedValue(BasicVisualLexicon.NODE_X_LOCATION, initial_positionX_node);
						new_node_source_view.setLockedValue(BasicVisualLexicon.NODE_Y_LOCATION,
								(x_or_y_Pos_source) * Util.node_label_factor_size);
					}

					new_node_source_view.setLockedValue(BasicVisualLexicon.NODE_WIDTH, 0.01);
					new_node_source_view.setLockedValue(BasicVisualLexicon.NODE_LABEL_TRANSPARENCY, 0);
					new_node_source_view.setLockedValue(BasicVisualLexicon.NODE_LABEL, "");
					new_node_source_view.setLockedValue(BasicVisualLexicon.NODE_VISIBLE, true);
					new_node_source_view.setLockedValue(BasicVisualLexicon.NODE_BORDER_WIDTH, 0.0);
					new_node_source_view.setLockedValue(BasicVisualLexicon.NODE_BORDER_PAINT, Color.WHITE);

					View<CyNode> new_node_target_view = netView.getNodeView(new_node_target);
					if (isProtein_expansion_horizontal) {
						new_node_target_view.setLockedValue(BasicVisualLexicon.NODE_X_LOCATION,
								(x_or_y_Pos_target) * Util.node_label_factor_size);
						new_node_target_view.setLockedValue(BasicVisualLexicon.NODE_Y_LOCATION, initial_positionY_node);
					} else {
						new_node_target_view.setLockedValue(BasicVisualLexicon.NODE_X_LOCATION, initial_positionX_node);
						new_node_target_view.setLockedValue(BasicVisualLexicon.NODE_Y_LOCATION,
								(x_or_y_Pos_target) * Util.node_label_factor_size);
					}

					new_node_target_view.setLockedValue(BasicVisualLexicon.NODE_WIDTH, 0.01);
					new_node_target_view.setLockedValue(BasicVisualLexicon.NODE_LABEL_TRANSPARENCY, 0);
					new_node_target_view.setLockedValue(BasicVisualLexicon.NODE_LABEL, "");
					new_node_target_view.setLockedValue(BasicVisualLexicon.NODE_VISIBLE, true);
					new_node_target_view.setLockedValue(BasicVisualLexicon.NODE_BORDER_WIDTH, 0.0);
					new_node_target_view.setLockedValue(BasicVisualLexicon.NODE_BORDER_PAINT, Color.WHITE);

					Bend bend = bendFactory.createBend();

					double x_or_y_Pos = (x_or_y_Pos_source + x_or_y_Pos_target) / 2;

					if (Math.abs(x_or_y_Pos_source) > Math.abs(x_or_y_Pos_target)) {
						x_or_y_Pos = Math.abs(x_or_y_Pos_source) - Math.abs(x_or_y_Pos);
					} else {
						x_or_y_Pos = Math.abs(x_or_y_Pos_target) - Math.abs(x_or_y_Pos);
					}
					x_or_y_Pos += 50;
					if (isProtein_expansion_horizontal) {
						x_or_y_Pos += initial_positionY_node;
					} else {
						x_or_y_Pos += initial_positionX_node;
					}

					Handle h = null;
					if (isProtein_expansion_horizontal) {
						h = handleFactory.createHandle(netView, newEdgeView,
								((x_or_y_Pos_source + x_or_y_Pos_target) * Util.node_label_factor_size) / 2,
								(x_or_y_Pos) * Util.node_label_factor_size);
					} else {
						h = handleFactory.createHandle(netView, newEdgeView, (x_or_y_Pos) * Util.node_label_factor_size,
								((x_or_y_Pos_source + x_or_y_Pos_target) * Util.node_label_factor_size) / 2);
					}

					bend.insertHandleAt(0, h);
					newEdgeView.setLockedValue(BasicVisualLexicon.EDGE_BEND, bend);

					if (intraLinks.get(countEdge).score != Double.NaN
							&& -Math.log10(intraLinks.get(countEdge).score) < intralink_threshold_score) {

						new_node_source_view.setLockedValue(BasicVisualLexicon.NODE_VISIBLE, false);
						new_node_target_view.setLockedValue(BasicVisualLexicon.NODE_VISIBLE, false);
					}

				} else { // Update edge position

					View<CyEdge> edgeView = netView.getEdgeView(current_edge);
					edgeView.setLockedValue(BasicVisualLexicon.EDGE_VISIBLE, true);
					updateIntraLinkEdgesPosition(myNetwork, netView, original_node, edgeView, intraLinks, countEdge,
							center_position_node, initial_positionX_node, initial_positionY_node);

				}
			}
		} else {// restore intralinks

			String node_name = myNetwork.getDefaultNodeTable().getRow(original_node.getSUID()).getRaw(CyNetwork.NAME)
					.toString();
			Protein protein = Util.getProtein(myNetwork, node_name);
			hideAllIntraLinks(myNetwork, netView, protein);
		}
	}

	/**
	 * Method responsible for getting edge from a name
	 * 
	 * @param myNetwork current network
	 * @param edge_name current edge name
	 * @param isSimilar check if the name is similar or equals with edge_name
	 * @return edge
	 */
	public static CyEdge getEdge(CyNetwork myNetwork, final String edge_name, boolean isSimilar) {

		CyEdge _edge = null;

		if (myNetwork == null)
			return _edge;

		// Check if the node exists in the network
		Optional<CyRow> isEdgePresent = null;

		if (isSimilar) {
			isEdgePresent = myNetwork.getDefaultEdgeTable().getAllRows().stream().filter(new Predicate<CyRow>() {
				public boolean test(CyRow o) {
					return o.get(CyNetwork.NAME, String.class).contains(edge_name);
				}
			}).findFirst();

		} else {
			isEdgePresent = myNetwork.getDefaultEdgeTable().getAllRows().stream().filter(new Predicate<CyRow>() {

				public boolean test(CyRow o) {
					return o.get(CyNetwork.NAME, String.class).equals(edge_name);
				}
			}).findFirst();

		}
		if (isEdgePresent.isPresent()) {// Get node if exists

			CyRow _node_row = isEdgePresent.get();
			_edge = myNetwork.getEdge(Long.parseLong(_node_row.getRaw(CyIdentifiable.SUID).toString()));
		}

		return _edge;
	}

	/**
	 * Update Protein domain column
	 * 
	 * @param taskMonitor task monitor
	 * @param myNetwork   current network
	 */
	public static void updateProteins(TaskMonitor taskMonitor, CyNetwork myNetwork, JLabel textLabel_status_result) {

		if (taskMonitor != null)
			taskMonitor.setTitle("Updating proteins");

		if (myNetwork == null || Util.proteinsMap == null || Util.proteinsMap.size() == 0)
			return;

		List<Protein> proteinList = Util.proteinsMap.get(myNetwork.toString());
		if (proteinList == null || proteinList.size() == 0)
			return;

		int old_progress = 0;
		int summary_processed = 0;
		int total_rows = proteinList.size();

		for (final Protein protein : proteinList) {

			CyNode node = getNode(myNetwork, protein.gene);
			if (node == null)
				continue;

			fillProteinDomainColumns(myNetwork, protein, node);
			fillProteinSequenceColumn(myNetwork, protein, node);
			fillConflictedResiduesColumn(myNetwork, protein, node);

			/**
			 * Get intra and interlinks
			 */
			if (protein.interLinks == null && protein.intraLinks == null) {
				Tuple2 inter_and_intralinks = Util.getAllLinksFromNode(node, myNetwork);
				protein.interLinks = (ArrayList<CrossLink>) inter_and_intralinks.getFirst();
				protein.intraLinks = (ArrayList<CrossLink>) inter_and_intralinks.getSecond();
			}

			summary_processed++;
			progressBar(summary_processed, old_progress, total_rows, "Updating proteins information: ", taskMonitor,
					textLabel_status_result);
		}

		/**
		 * Update CrossLinks location based on protein domain name
		 */
		old_progress = 0;
		summary_processed = 0;
		total_rows = proteinList.size();
		for (final Protein protein : proteinList) {
			updateCrosslinksLocationBasedOnProteinDomains(myNetwork, protein);

			summary_processed++;
			progressBar(summary_processed, old_progress, total_rows, "Updating cross-link information: ", taskMonitor,
					textLabel_status_result);
		}
	}

	private static void fillConflictedResiduesColumn(CyNetwork myNetwork, final Protein protein, CyNode node) {

		List<String> list_residues = new ArrayList<String>();
		if (protein.reactionSites == null)
			return;

		for (Residue residue : protein.reactionSites) {
			if (residue.isConflicted) {
				if (residue.predicted_epoch <= ProcessProteinLocationTask.epochs) {
					list_residues.add(
							Character.toString(residue.aminoacid) + "[" + Integer.toString(residue.position) + "]");
				}
			}
		}

		addDomainsOrConflictedResiduesIntoTheTable(myNetwork, node, list_residues,
				CONFLICTED_PREDICTED_RESIDUES_COLUMN);
	}

	/**
	 * Method responsible for filling protein sequence column
	 * 
	 * @param myNetwork current network
	 * @param protein   current protein
	 * @param node      current node
	 */
	private static void fillProteinSequenceColumn(CyNetwork myNetwork, final Protein protein, CyNode node) {

		if (myNetwork.getRow(node).get(PROTEIN_SEQUENCE_COLUMN, String.class) != null)
			myNetwork.getRow(node).set(PROTEIN_SEQUENCE_COLUMN, protein.sequence);
		else {
			// Create Scaling factor protein column
			CyTable nodeTable = myNetwork.getDefaultNodeTable();
			if (nodeTable.getColumn(PROTEIN_SEQUENCE_COLUMN) == null) {
				try {
					nodeTable.createColumn(PROTEIN_SEQUENCE_COLUMN, String.class, false);

					CyRow row = myNetwork.getRow(node);
					row.set(PROTEIN_SEQUENCE_COLUMN, protein.sequence);

				} catch (IllegalArgumentException e) {
					try {
						CyRow row = myNetwork.getRow(node);
						row.set(PROTEIN_SEQUENCE_COLUMN, protein.sequence);

					} catch (Exception e2) {
					}
				} catch (Exception e) {
				}
			} else {
				CyRow row = myNetwork.getRow(node);
				row.set(PROTEIN_SEQUENCE_COLUMN, protein.sequence);
			}
		}
	}

	/**
	 * Method responsible for filling protein domains columns: Original and
	 * predicted columns
	 * 
	 * @param myNetwork  current network
	 * @param sb_domains accumulative protein domains
	 * @param protein    current protein
	 * @param node       current node
	 */
	private static void fillProteinDomainColumns(CyNetwork myNetwork, final Protein protein, CyNode node) {

		if (protein.domains != null && protein.domains.size() > 0) {

			List<String> list_original_domains = new ArrayList<>();
			List<String> list_predicted_domains = new ArrayList<>();
			for (ProteinDomain domain : protein.domains) {
				if (domain.isPredicted) {
					list_predicted_domains.add(domain.name + "[" + Integer.toString(domain.startId) + "-"
							+ Integer.toString(domain.endId) + "]");
				} else {
					list_original_domains.add(domain.name + "[" + Integer.toString(domain.startId) + "-"
							+ Integer.toString(domain.endId) + "]");
				}

			}

			addDomainsOrConflictedResiduesIntoTheTable(myNetwork, node, list_predicted_domains,
					PREDICTED_PROTEIN_DOMAIN_COLUMN);

			addDomainsOrConflictedResiduesIntoTheTable(myNetwork, node, list_original_domains, PROTEIN_DOMAIN_COLUMN);
		}

	}

	/**
	 * Method responsible for adding domain or conflicted residue information into
	 * the table
	 * 
	 * @param myNetwork                           current network
	 * @param node                                current node
	 * @param list_domains_or_conflicted_residues current domains or conflicted
	 *                                            residues
	 * @param columnName                          column name: predicted or original
	 */
	private static void addDomainsOrConflictedResiduesIntoTheTable(CyNetwork myNetwork, CyNode node,
			List<String> list_domains_or_conflicted_residues, String columnName) {

		if (myNetwork.getRow(node).get(columnName, List.class) != null) {
			if (list_domains_or_conflicted_residues.size() > 0)
				myNetwork.getRow(node).set(columnName, Arrays.asList(list_domains_or_conflicted_residues.toArray()));
			else
				myNetwork.getRow(node).set(columnName, new ArrayList<String>());
		} else {
			// Create protein domain or conflicted residue column
			CyTable nodeTable = myNetwork.getDefaultNodeTable();
			if (nodeTable.getColumn(columnName) == null) {
				try {
					nodeTable.createListColumn(columnName, String.class, false);

					CyRow row = myNetwork.getRow(node);
					if (list_domains_or_conflicted_residues.size() > 0)
						row.set(columnName, Arrays.asList(list_domains_or_conflicted_residues.toArray()));
					else
						myNetwork.getRow(node).set(columnName, new ArrayList<String>());

				} catch (IllegalArgumentException e) {
					try {
						CyRow row = myNetwork.getRow(node);
						if (list_domains_or_conflicted_residues.size() > 0)
							row.set(columnName, Arrays.asList(list_domains_or_conflicted_residues.toArray()));
						else
							myNetwork.getRow(node).set(columnName, new ArrayList<String>());

					} catch (Exception e2) {
					}
				} catch (Exception e) {
				}

			} else {
				CyRow row = myNetwork.getRow(node);
				if (list_domains_or_conflicted_residues.size() > 0)
					row.set(columnName, Arrays.asList(list_domains_or_conflicted_residues.toArray()));
				else
					myNetwork.getRow(node).set(columnName, new ArrayList<String>());
			}
		}
	}

	/**
	 * Method responsible for updating all crosslinks location based on protein
	 * domain name
	 * 
	 * @param myNetwork current network
	 * @param protein   current protein
	 */
	private static void updateCrosslinksLocationBasedOnProteinDomains(CyNetwork myNetwork, Protein protein) {

		if (protein.interLinks == null && protein.intraLinks == null)
			return;

		// Try to retrieve the location information of target protein
		for (CrossLink interXL : protein.interLinks) {

			Protein targetPtn = null;
			if (interXL.protein_a.equals(protein.proteinID)) {// It means the protein B is the target
				targetPtn = getProtein(myNetwork, interXL.protein_b);

			} else {// It means the protein A is the target
				targetPtn = getProtein(myNetwork, interXL.protein_a);
			}

			if (targetPtn != null && targetPtn.interLinks != null) {
				CrossLink targetXL = getXL(targetPtn, protein.proteinID, interXL);

				if (targetXL != null && targetXL.location != null && !targetXL.location.equals("UK"))
					interXL.location = targetXL.location;
			}
		}

		if (protein.domains != null && protein.domains.size() > 0) {
			for (ProteinDomain domain : protein.domains) {

				int startPos = domain.startId;
				int endPos = domain.endId;

				List<CrossLink> links = new ArrayList<CrossLink>();
				if (protein.intraLinks != null) {
					links.addAll(protein.intraLinks.stream()
							.filter(value -> value.pos_site_a >= startPos && value.pos_site_b <= endPos)
							.collect(Collectors.toList()));

				}

				if (protein.interLinks != null) {
					links.addAll(protein.interLinks.stream()
							.filter(value -> (value.protein_a.equals(protein.proteinID) && value.pos_site_a >= startPos
									&& value.pos_site_a <= endPos)
									|| (value.protein_b.equals(protein.proteinID) && value.pos_site_b >= startPos
											&& value.pos_site_b <= endPos))
							.collect(Collectors.toList()));
				}

				for (CrossLink crossLink : links) {
					if (crossLink.location == null || crossLink.location.equals("UK"))
						crossLink.location = domain.name;
				}
			}
		}

		// Try to retrieve the location information of target protein
		for (CrossLink interXL : protein.interLinks) {

			Protein targetPtn = null;
			if (interXL.protein_a.equals(protein.proteinID)) {// It means the protein B is the target
				targetPtn = getProtein(myNetwork, interXL.protein_b);

			} else {// It means the protein A is the target
				targetPtn = getProtein(myNetwork, interXL.protein_a);
			}

			if (targetPtn != null && targetPtn.interLinks != null) {
				CrossLink targetXL = getXL(targetPtn, protein.proteinID, interXL);

				if (targetXL != null && (targetXL.location == null || targetXL.location.equals("UK")))
					targetXL.location = interXL.location;
			}
		}
	}

	/**
	 * Method responsible for retrieving a specific XL
	 * 
	 * @param targetPtn target protein
	 * @param proteinID current protein
	 * @param interXL   source crosslink
	 * @return target xl
	 */
	private static CrossLink getXL(Protein targetPtn, String proteinID, CrossLink interXL) {

		Optional<CrossLink> isPresent = targetPtn.interLinks.stream()
				.filter(value -> (value.protein_a.equals(proteinID) && value.pos_site_a == interXL.pos_site_a
						&& value.protein_b.equals(targetPtn.proteinID))
						|| (value.protein_b.equals(proteinID) && value.pos_site_b == interXL.pos_site_b
								&& value.protein_a.equals(targetPtn.proteinID)))
				.findFirst();

		if (isPresent.isPresent())
			return isPresent.get();
		else
			return null;
	}

	/**
	 * Update monolink column
	 * 
	 * @param taskMonitor             task monitor
	 * @param myNetwork               current network
	 * @param AllproteinWithMonolinks list with all monolinks
	 */
	public static void update_MonolinkColumn(TaskMonitor taskMonitor, CyNetwork myNetwork,
			Map<Long, Protein> AllproteinWithMonolinks) {
		for (Map.Entry<Long, Protein> entry : AllproteinWithMonolinks.entrySet()) {
			Long key = entry.getKey();
			CyNode node = getNode(myNetwork, key);
			if (node == null)
				continue;
			Protein current_protein_with_mononlinks = entry.getValue();
//			if (current_protein_with_mononlinks != null && current_protein_with_mononlinks.monolinks.size() > 0)
//				update_MonolinkColumn(taskMonitor, myNetwork, current_protein_with_mononlinks, node);
		}
	}

	/**
	 * Update Monolink column
	 * 
	 * @param taskMonitor            task monitor
	 * @param myNetwork              current network
	 * @param protein_with_monolinks list of ptms
	 * @param node                   current node
	 */
	public static void update_MonolinkColumn(TaskMonitor taskMonitor, CyNetwork myNetwork,
			Protein protein_with_monolinks, CyNode node) {

		taskMonitor.showMessage(TaskMonitor.Level.INFO, "Updating monolink column...");

		if (myNetwork == null)
			return;

//		if (protein_with_monolinks == null || protein_with_monolinks.monolinks.size() == 0)
//			return;

		StringBuilder sb_monolinks = new StringBuilder();
//		for (final CrossLink current_ptm : protein_with_monolinks.monolinks) {
//
//			sb_monolinks.append(current_ptm.sequence);
//			sb_monolinks.append("[");
//			sb_monolinks.append(current_ptm.pos_site_a);// reaction site position A
//			sb_monolinks.append("-");
//			sb_monolinks.append(current_ptm.pos_site_b);// reaction site position B
//			sb_monolinks.append("][");
//			sb_monolinks.append(current_ptm.start_pos_protein);// start position in protein sequence
//			sb_monolinks.append("-");
//			sb_monolinks.append(current_ptm.end_pos_protein);// end position in protein sequence
//			sb_monolinks.append("], ");
//
//		}

		if (node != null) {
			if (myNetwork.getRow(node).get(MONOLINK_COLUMN, String.class) != null)
				myNetwork.getRow(node).set(MONOLINK_COLUMN, sb_monolinks.substring(0, sb_monolinks.length() - 2));
		}

	}

	/**
	 * Update PTM column
	 * 
	 * @param taskMonitor task monitor
	 * @param myNetwork   current network
	 * @param AllPtmsList list with all ptms
	 */
	public static void update_PTMColumn(TaskMonitor taskMonitor, CyNetwork myNetwork,
			Map<Long, List<PTM>> AllPtmsList) {
		for (Map.Entry<Long, List<PTM>> entry : AllPtmsList.entrySet()) {
			Long key = entry.getKey();
			CyNode node = getNode(myNetwork, key);
			if (node == null)
				continue;
			List<PTM> ptmList = entry.getValue();
			if (ptmList.size() > 0)
				update_PTMColumn(taskMonitor, myNetwork, ptmList, node);
		}
	}

	/**
	 * Update PTM column
	 * 
	 * @param taskMonitor task monitor
	 * @param myNetwork   current network
	 * @param ptmList     list of ptms
	 * @param node        current node
	 */
	public static void update_PTMColumn(TaskMonitor taskMonitor, CyNetwork myNetwork, List<PTM> ptmList, CyNode node) {

		taskMonitor.showMessage(TaskMonitor.Level.INFO, "Updating ptm column...");

		if (myNetwork == null)
			return;

		StringBuilder sb_ptms = new StringBuilder();
		for (final PTM current_ptm : ptmList) {

			sb_ptms.append(current_ptm.name);
			sb_ptms.append("[");
			sb_ptms.append(current_ptm.residue);
			sb_ptms.append("-");
			sb_ptms.append(current_ptm.position);
			sb_ptms.append("], ");

		}

		if (node != null) {
			if (myNetwork.getRow(node).get(PTM_COLUMN, String.class) != null)
				myNetwork.getRow(node).set(PTM_COLUMN, sb_ptms.substring(0, sb_ptms.length() - 2));
		}

	}

	/**
	 * Method responsible for getting node from a name
	 * 
	 * @param myNetwork current network
	 * @param node_name current node name
	 * @return Node
	 */
	public static CyNode getNode(CyNetwork myNetwork, final String node_name) {

		CyNode _node = null;

		if (myNetwork == null)
			return _node;

		// Check if the node exists in the network
		Optional<CyRow> isNodePresent = myNetwork.getDefaultNodeTable().getAllRows().stream()
				.filter(new Predicate<CyRow>() {
					public boolean test(CyRow o) {
						return o.get(CyNetwork.NAME, String.class).equals(node_name);
					}
				}).findFirst();

		if (isNodePresent.isPresent()) {// Get node if exists
			CyRow _node_row = isNodePresent.get();
			_node = myNetwork.getNode(Long.parseLong(_node_row.getRaw(CyIdentifiable.SUID).toString()));
		}

		return _node;
	}

	/**
	 * Method responsible for getting protein from node name
	 * 
	 * @param myNetwork current network
	 * @param node_name node name
	 * @return protein
	 */
	public static Protein getProtein(CyNetwork myNetwork, final String node_name) {

		Protein ptn = null;

		if (myNetwork == null)
			return ptn;

		List<Protein> all_proteins = Util.proteinsMap.get(myNetwork.toString());
		Optional<Protein> isPtnPresent = all_proteins.stream().filter(value -> value.gene.equals(node_name))
				.findFirst();
		if (isPtnPresent.isPresent()) {
			ptn = isPtnPresent.get();
		}

		return ptn;
	}

	/**
	 * Method responsible for getting node from a SUID
	 * 
	 * @param myNetwork current network
	 * @param node_suid current SUID of the node
	 * @return Node
	 */
	public static CyNode getNode(CyNetwork myNetwork, final Long node_suid) {

		CyNode _node = null;

		if (myNetwork == null)
			return _node;

		// Check if the node exists in the network
		Optional<CyRow> isNodePresent = myNetwork.getDefaultNodeTable().getAllRows().stream()
				.filter(new Predicate<CyRow>() {
					public boolean test(CyRow o) {
						return o.get(CyNetwork.SUID, Long.class).equals(node_suid);
					}
				}).findFirst();

		if (isNodePresent.isPresent()) {// Get node if exists
			CyRow _node_row = isNodePresent.get();
			_node = myNetwork.getNode(Long.parseLong(_node_row.getRaw(CyIdentifiable.SUID).toString()));
		}

		return _node;
	}

	/**
	 * Plot all interlinks
	 * 
	 * @param myNetwork     current network
	 * @param nodeView      current node view
	 * @param netView       current network view
	 * @param handleFactory handle factory
	 * @param bendFactory   bend factory
	 * @param style         current style
	 * @param node          current node
	 * @param sourceNode    current source node
	 * @param targetNode    current target node
	 * @param lexicon       lexicon
	 * @param proteinLength length of the current protein
	 * @param interLinks    all interlinks
	 */
	private static void plotInterLinks(CyNetwork myNetwork, View<CyNode> nodeView, CyNetworkView netView,
			HandleFactory handleFactory, BendFactory bendFactory, VisualStyle style, CyNode node, CyNode sourceNode,
			CyNode targetNode, VisualLexicon lexicon, float proteinLength, List<CrossLink> interLinks) {

		final String source_node_name = myNetwork.getDefaultNodeTable().getRow(sourceNode.getSUID())
				.getRaw(CyNetwork.NAME).toString();
		final String target_node_name = myNetwork.getDefaultNodeTable().getRow(targetNode.getSUID())
				.getRaw(CyNetwork.NAME).toString();

		List<CrossLink> current_inter_links = new ArrayList<CrossLink>(interLinks);

		current_inter_links.removeIf(new Predicate<CrossLink>() {

			public boolean test(CrossLink o) {
				return !(o.protein_a.equals(source_node_name) && o.protein_b.equals(target_node_name)
						|| o.protein_a.equals(target_node_name) && o.protein_b.equals(source_node_name));
			}
		});

		if (current_inter_links.size() == 0)
			return;

		double x_or_y_Pos_source = 0;
		double xl_pos_source = 0;

		double x_or_y_Pos_target = 0;
		double xl_pos_target = 0;

		double center_position_source_node = 0;
		double center_position_target_node = 0;

		double initial_position_source_node = 0;
		double initial_position_target_node = 0;

		float other_node_width_or_height = 0;

		Object length_other_protein_a;
		Object length_other_protein_b;
		CyRow other_node_row = null;

		double target_factor_scaling_length_protein = 1;

		boolean isProtein_expansion_horizontal_source = true;
		boolean isProtein_expansion_horizontal_target = true;
		View<CyNode> sourceNodeView = null;
		View<CyNode> targetNodeView = null;

		// Target node will 'always' be the non-focused node (opposite of node.getSUID)
		if (sourceNode.getSUID() == node.getSUID()) {

			other_node_row = myNetwork.getRow(targetNode);

			isProtein_expansion_horizontal_source = myNetwork.getRow(sourceNode)
					.get(Util.HORIZONTAL_EXPANSION_COLUMN_NAME, Boolean.class) == null ? true
							: myNetwork.getRow(sourceNode).get(Util.HORIZONTAL_EXPANSION_COLUMN_NAME, Boolean.class);

			isProtein_expansion_horizontal_target = myNetwork.getRow(targetNode)
					.get(Util.HORIZONTAL_EXPANSION_COLUMN_NAME, Boolean.class) == null ? true
							: myNetwork.getRow(targetNode).get(Util.HORIZONTAL_EXPANSION_COLUMN_NAME, Boolean.class);

			sourceNodeView = netView.getNodeView(sourceNode);
			targetNodeView = netView.getNodeView(targetNode);

		} else {

			other_node_row = myNetwork.getRow(sourceNode);

			isProtein_expansion_horizontal_source = myNetwork.getRow(targetNode)
					.get(Util.HORIZONTAL_EXPANSION_COLUMN_NAME, Boolean.class) == null ? true
							: myNetwork.getRow(targetNode).get(Util.HORIZONTAL_EXPANSION_COLUMN_NAME, Boolean.class);

			isProtein_expansion_horizontal_target = myNetwork.getRow(sourceNode)
					.get(Util.HORIZONTAL_EXPANSION_COLUMN_NAME, Boolean.class) == null ? true
							: myNetwork.getRow(sourceNode).get(Util.HORIZONTAL_EXPANSION_COLUMN_NAME, Boolean.class);

			sourceNodeView = netView.getNodeView(targetNode);
			targetNodeView = netView.getNodeView(sourceNode);
		}

		target_factor_scaling_length_protein = other_node_row.get(PROTEIN_SCALING_FACTOR_COLUMN_NAME,
				Double.class) == null ? 1 : other_node_row.get(PROTEIN_SCALING_FACTOR_COLUMN_NAME, Double.class);

		length_other_protein_a = other_node_row.getRaw(PROTEIN_LENGTH_A);
		length_other_protein_b = other_node_row.getRaw(PROTEIN_LENGTH_B);

		if (length_other_protein_a == null) {
			if (length_other_protein_b == null)
				length_other_protein_a = 10;
			else
				length_other_protein_a = length_other_protein_b;
		}

		/**
		 * Modify node style
		 */

		if (isProtein_expansion_horizontal_source && isProtein_expansion_horizontal_target) {
			// Source: horizontal
			// Target: horizontal

			other_node_width_or_height = ((Number) targetNodeView.getVisualProperty(BasicVisualLexicon.NODE_WIDTH))
					.floatValue();

			if ((Math.round(other_node_width_or_height * 100.0)
					/ 100.0) == (Math.round((proteinLength * Util.node_label_factor_size) * 100.0) / 100.0))
				other_node_width_or_height = ((Number) sourceNodeView.getVisualProperty(BasicVisualLexicon.NODE_WIDTH))
						.floatValue();

			initial_position_source_node = getXPositionOf(sourceNodeView);
			initial_position_target_node = getXPositionOf(targetNodeView);

		} else if (!isProtein_expansion_horizontal_source && !isProtein_expansion_horizontal_target) {
			// Source: vertical
			// Target: vertical

			other_node_width_or_height = ((Number) targetNodeView.getVisualProperty(BasicVisualLexicon.NODE_HEIGHT))
					.floatValue();
			if ((Math.round(other_node_width_or_height * 100.0)
					/ 100.0) == (Math.round((proteinLength * Util.node_label_factor_size) * 100.0) / 100.0))
				other_node_width_or_height = ((Number) sourceNodeView.getVisualProperty(BasicVisualLexicon.NODE_HEIGHT))
						.floatValue();

			initial_position_source_node = getYPositionOf(sourceNodeView);
			initial_position_target_node = getYPositionOf(targetNodeView);

		} else if (!isProtein_expansion_horizontal_source && isProtein_expansion_horizontal_target) {
			// Source: vertical
			// Target: horizontal

			other_node_width_or_height = ((Number) targetNodeView.getVisualProperty(BasicVisualLexicon.NODE_WIDTH))
					.floatValue();
			if ((Math.round(other_node_width_or_height * 100.0)
					/ 100.0) == (Math.round((proteinLength * Util.node_label_factor_size) * 100.0) / 100.0))
				other_node_width_or_height = ((Number) sourceNodeView.getVisualProperty(BasicVisualLexicon.NODE_WIDTH))
						.floatValue();

			initial_position_source_node = getYPositionOf(sourceNodeView);
			initial_position_target_node = getXPositionOf(targetNodeView);

		} else if (isProtein_expansion_horizontal_source && !isProtein_expansion_horizontal_target) {
			// Source: horizontal
			// Target: vertical

			other_node_width_or_height = ((Number) targetNodeView.getVisualProperty(BasicVisualLexicon.NODE_HEIGHT))
					.floatValue();
			if ((Math.round(other_node_width_or_height * 100.0)
					/ 100.0) == (Math.round((proteinLength * Util.node_label_factor_size) * 100.0) / 100.0))
				other_node_width_or_height = ((Number) sourceNodeView.getVisualProperty(BasicVisualLexicon.NODE_HEIGHT))
						.floatValue();

			initial_position_source_node = getXPositionOf(sourceNodeView);
			initial_position_target_node = getYPositionOf(targetNodeView);
		}

		center_position_source_node = (proteinLength * Util.node_label_factor_size) / 2.0;
		center_position_target_node = (other_node_width_or_height) / 2.0;

		for (int countEdge = 0; countEdge < current_inter_links.size(); countEdge++) {

			if (!myNetwork.getDefaultNodeTable().getRow(targetNode.getSUID()).getRaw(CyNetwork.NAME).toString()
					.equals(current_inter_links.get(countEdge).protein_b))
				continue;

			final String egde_name_added_by_app = "[Source: " + current_inter_links.get(countEdge).protein_a + " ("
					+ current_inter_links.get(countEdge).pos_site_a + ")] [Target: "
					+ current_inter_links.get(countEdge).protein_b + " ("
					+ current_inter_links.get(countEdge).pos_site_b + ")] - Score:"
					+ current_inter_links.get(countEdge).score + " - Edge" + countEdge;

			CyEdge newEdge = getEdge(myNetwork, egde_name_added_by_app, false);
			if (newEdge == null) {// Add a new edge if does not exist

				newEdge = myNetwork.addEdge(sourceNode, targetNode, true);// INTERLINK
				myNetwork.getRow(newEdge).set(CyNetwork.NAME, egde_name_added_by_app);

				View<CyEdge> newEdgeView = netView.getEdgeView(newEdge);
				while (newEdgeView == null) {
					netView.updateView();
					try {
						Thread.sleep(200);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
					newEdgeView = netView.getEdgeView(newEdge);
				}

				Color linkColor = InterLinksColor;
				CrossLink link = current_inter_links.get(countEdge);

				if (link != null && link.location != null && !link.location.equals("UK")) {
					linkColor = Util.proteinDomainsColorMap.get(link.location);
				}

				newEdgeView.setLockedValue(BasicVisualLexicon.EDGE_PAINT, linkColor);
				newEdgeView.setLockedValue(BasicVisualLexicon.EDGE_SELECTED_PAINT, Color.RED);
				newEdgeView.setLockedValue(BasicVisualLexicon.EDGE_STROKE_SELECTED_PAINT, Color.RED);
				newEdgeView.setLockedValue(BasicVisualLexicon.EDGE_TRANSPARENCY, edge_link_opacity);
				newEdgeView.setLockedValue(BasicVisualLexicon.EDGE_WIDTH, edge_link_width);
				newEdgeView.setLockedValue(BasicVisualLexicon.EDGE_TARGET_ARROW_SHAPE, ArrowShapeVisualProperty.NONE);
				newEdgeView.setLockedValue(BasicVisualLexicon.EDGE_SOURCE_ARROW_SHAPE, ArrowShapeVisualProperty.NONE);
				newEdgeView.setLockedValue(BasicVisualLexicon.EDGE_LINE_TYPE, LineTypeVisualProperty.SOLID);

				// ##### EDGE_LABEL ########
				String blank_spaces = edge_label_blank_spaces;
				for (int count_bs = 0; count_bs < countEdge; count_bs++) {
					blank_spaces += edge_label_blank_spaces;
				}
				blank_spaces += ".";

				String mainLabel = current_inter_links.get(countEdge).protein_a + " ["
						+ current_inter_links.get(countEdge).pos_site_a + "] - "
						+ current_inter_links.get(countEdge).protein_b + " ["
						+ current_inter_links.get(countEdge).pos_site_b + "]" + blank_spaces;

				VisualProperty<?> vp_edge_label = lexicon.lookup(CyEdge.class, "EDGE_LABEL");
				if (vp_edge_label != null) {

					Object edge_label = vp_edge_label.parseSerializableString(mainLabel);

					if (edge_label != null)
						newEdgeView.setLockedValue(vp_edge_label, edge_label);

				}

				double score = current_inter_links.get(countEdge).score;
				double log_score = round(-Math.log10(current_inter_links.get(countEdge).score), 2);
				String tooltip = "";

				if (Double.isNaN(score)) {
					tooltip = "<html><p>" + current_inter_links.get(countEdge).protein_a + " ["
							+ current_inter_links.get(countEdge).pos_site_a + "] - "
							+ current_inter_links.get(countEdge).protein_b + " ["
							+ current_inter_links.get(countEdge).pos_site_b + "]</p></html>";
				} else {
					tooltip = "<html><p>" + current_inter_links.get(countEdge).protein_a + " ["
							+ current_inter_links.get(countEdge).pos_site_a + "] - "
							+ current_inter_links.get(countEdge).protein_b + " ["
							+ current_inter_links.get(countEdge).pos_site_b + "]</p><p><i>Score: " + score
							+ "</i></p><p><i>-Log(score): " + log_score + "</i></p></html>";
				}
				newEdgeView.setLockedValue(BasicVisualLexicon.EDGE_TOOLTIP, tooltip);

				if (showLinksLegend) {
					newEdgeView.setLockedValue(BasicVisualLexicon.EDGE_LABEL_TRANSPARENCY, edge_label_opacity);
					newEdgeView.setLockedValue(BasicVisualLexicon.EDGE_LABEL_FONT_SIZE, edge_label_font_size);
				} else {
					newEdgeView.setLockedValue(BasicVisualLexicon.EDGE_LABEL_TRANSPARENCY, 0);
				}

				// #########################

				if (sourceNode.getSUID() == node.getSUID()) {
					xl_pos_source = current_inter_links.get(countEdge).pos_site_a * Util.node_label_factor_size;
					xl_pos_target = current_inter_links.get(countEdge).pos_site_b
							* target_factor_scaling_length_protein;
				} else {
					xl_pos_source = current_inter_links.get(countEdge).pos_site_b * Util.node_label_factor_size;
					xl_pos_target = current_inter_links.get(countEdge).pos_site_a
							* target_factor_scaling_length_protein;
				}

				if (xl_pos_source <= center_position_source_node) { // [-protein_length/2, 0]
					x_or_y_Pos_source = (-center_position_source_node) + xl_pos_source;
				} else { // [0, protein_length/2]
					x_or_y_Pos_source = xl_pos_source - center_position_source_node;
				}
				x_or_y_Pos_source += initial_position_source_node;
				if (xl_pos_target <= center_position_target_node) { // [-protein_length/2, 0]
					x_or_y_Pos_target = (-center_position_target_node) + xl_pos_target;
				} else { // [0, protein_length/2]
					x_or_y_Pos_target = xl_pos_target - center_position_target_node;
				}
				x_or_y_Pos_target += initial_position_target_node;

				// ########## GET EDGE_BEND STYLE TO MODIFY #########

				Bend bend = bendFactory.createBend();

				Handle h = null;
				Handle h2 = null;

				if ((Math.round(other_node_width_or_height * 100.0) / 100.0) == (Math.round(
						(((Number) length_other_protein_a).floatValue() * target_factor_scaling_length_protein) * 100.0)
						/ 100.0)) {// Target node has already been modified

					if (isProtein_expansion_horizontal_source && isProtein_expansion_horizontal_target) {
						// Source: horizontal
						// Target: horizontal

						h = handleFactory.createHandle(netView, newEdgeView, (x_or_y_Pos_source - OFFSET_BEND),
								getYPositionOf(sourceNodeView));

						h2 = handleFactory.createHandle(netView, newEdgeView, (x_or_y_Pos_target - OFFSET_BEND),
								getYPositionOf(targetNodeView));

					} else if (!isProtein_expansion_horizontal_source && !isProtein_expansion_horizontal_target) {
						// Source: vertical
						// Target: vertical

						h = handleFactory.createHandle(netView, newEdgeView, getXPositionOf(sourceNodeView),
								(x_or_y_Pos_source - OFFSET_BEND));

						h2 = handleFactory.createHandle(netView, newEdgeView, getXPositionOf(targetNodeView),
								(x_or_y_Pos_target - OFFSET_BEND));

					} else if (isProtein_expansion_horizontal_source && !isProtein_expansion_horizontal_target) {
						// Source: horizontal
						// Target: vertical

						h = handleFactory.createHandle(netView, newEdgeView, (x_or_y_Pos_source - OFFSET_BEND),
								getYPositionOf(sourceNodeView));

						h2 = handleFactory.createHandle(netView, newEdgeView, getXPositionOf(targetNodeView),
								(x_or_y_Pos_target - OFFSET_BEND));

					} else if (!isProtein_expansion_horizontal_source && isProtein_expansion_horizontal_target) {
						// Source: vertical
						// Target: horizontal

						h = handleFactory.createHandle(netView, newEdgeView, getXPositionOf(sourceNodeView),
								(x_or_y_Pos_source - OFFSET_BEND));

						h2 = handleFactory.createHandle(netView, newEdgeView, (x_or_y_Pos_target - OFFSET_BEND),
								getYPositionOf(targetNodeView));
					}

					if (sourceNode.getSUID() == node.getSUID()) {
						bend.insertHandleAt(0, h);
						bend.insertHandleAt(1, h2);
					} else {// If target node is the selected node then first insert h2
						bend.insertHandleAt(0, h2);
						bend.insertHandleAt(1, h);
					}

				} else {// Target node is intact

					if (isProtein_expansion_horizontal) {
						h = handleFactory.createHandle(netView, newEdgeView, (x_or_y_Pos_source - OFFSET_BEND),
								getYPositionOf(sourceNodeView));
					} else {
						h = handleFactory.createHandle(netView, newEdgeView, getXPositionOf(sourceNodeView),
								(x_or_y_Pos_source - OFFSET_BEND));
					}

					bend.insertHandleAt(0, h);
				}

				newEdgeView.setLockedValue(BasicVisualLexicon.EDGE_BEND, bend);

				VisualProperty<?> vp_edge_curved = lexicon.lookup(CyEdge.class, "EDGE_CURVED");
				if (vp_edge_curved != null) {
					Object edge_curved_obj = vp_edge_curved.parseSerializableString("false");
					if (edge_curved_obj != null) {
						newEdgeView.setLockedValue(vp_edge_curved, edge_curved_obj);
					}
				}

				if (current_inter_links.get(countEdge).score != Double.NaN
						&& -Math.log10(current_inter_links.get(countEdge).score) < interlink_threshold_score) {// hide
																												// interlink
					newEdgeView.setLockedValue(BasicVisualLexicon.EDGE_VISIBLE, false);
					newEdgeView.setLockedValue(BasicVisualLexicon.EDGE_LABEL_TRANSPARENCY, 0);
				}
			}
		}
	}

	/**
	 * Update all edges that represent interlinks according to their current
	 * position
	 * 
	 * @param myNetwork     current network
	 * @param node          current node
	 * @param netView       current network view
	 * @param handleFactory handle factory
	 * @param bendFactory   bend factory
	 * @param style         current style
	 * @param lexicon       lexicon
	 * @param edge          edge
	 * @param sourceNode    current source node
	 * @param targetNode    current target node
	 * @param edge_name     current edge name
	 * @param proteinLength length of current protein
	 */
	private static void updateInterLinkEdgesPosition(CyNetwork myNetwork, CyNode node, CyNetworkView netView,
			HandleFactory handleFactory, BendFactory bendFactory, VisualStyle style, VisualLexicon lexicon, CyEdge edge,
			CyNode sourceNode, CyNode targetNode, String edge_name, float proteinLength, String location) {

		View<CyEdge> newEdgeView = netView.getEdgeView(edge);
		while (newEdgeView == null) {
			netView.updateView();
			try {
				Thread.sleep(200);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			newEdgeView = netView.getEdgeView(edge);
		}

		Object length_other_protein_a;
		Object length_other_protein_b;
		CyRow other_node_row = null;

		boolean isProtein_expansion_horizontal_source = true;
		boolean isProtein_expansion_horizontal_target = true;
		View<CyNode> sourceNodeView = null;
		View<CyNode> targetNodeView = null;

		// Target node will 'always' be the non-focused node (opposite of node.getSUID)
		if (sourceNode.getSUID() == node.getSUID()) {

			other_node_row = myNetwork.getRow(targetNode);

			isProtein_expansion_horizontal_source = myNetwork.getRow(sourceNode)
					.get(Util.HORIZONTAL_EXPANSION_COLUMN_NAME, Boolean.class) == null ? true
							: myNetwork.getRow(sourceNode).get(Util.HORIZONTAL_EXPANSION_COLUMN_NAME, Boolean.class);

			isProtein_expansion_horizontal_target = myNetwork.getRow(targetNode)
					.get(Util.HORIZONTAL_EXPANSION_COLUMN_NAME, Boolean.class) == null ? true
							: myNetwork.getRow(targetNode).get(Util.HORIZONTAL_EXPANSION_COLUMN_NAME, Boolean.class);

			sourceNodeView = netView.getNodeView(sourceNode);
			targetNodeView = netView.getNodeView(targetNode);

		} else {

			other_node_row = myNetwork.getRow(sourceNode);

			isProtein_expansion_horizontal_source = myNetwork.getRow(targetNode)
					.get(Util.HORIZONTAL_EXPANSION_COLUMN_NAME, Boolean.class) == null ? true
							: myNetwork.getRow(targetNode).get(Util.HORIZONTAL_EXPANSION_COLUMN_NAME, Boolean.class);

			isProtein_expansion_horizontal_target = myNetwork.getRow(sourceNode)
					.get(Util.HORIZONTAL_EXPANSION_COLUMN_NAME, Boolean.class) == null ? true
							: myNetwork.getRow(sourceNode).get(Util.HORIZONTAL_EXPANSION_COLUMN_NAME, Boolean.class);

			sourceNodeView = netView.getNodeView(targetNode);
			targetNodeView = netView.getNodeView(sourceNode);

		}

		length_other_protein_a = other_node_row.getRaw(PROTEIN_LENGTH_A);
		length_other_protein_b = other_node_row.getRaw(PROTEIN_LENGTH_B);

		if (length_other_protein_a == null) {
			if (length_other_protein_b == null)
				length_other_protein_a = 10;
			else
				length_other_protein_a = length_other_protein_b;
		}

		/**
		 * Modify node style
		 */

		float other_node_width_or_height = 0;
		double initial_position_source_node = 0;
		double initial_position_target_node = 0;

		if (isProtein_expansion_horizontal_source && isProtein_expansion_horizontal_target) {
			// Source: horizontal
			// Target: horizontal

			other_node_width_or_height = ((Number) targetNodeView.getVisualProperty(BasicVisualLexicon.NODE_WIDTH))
					.floatValue();

			if ((Math.round(other_node_width_or_height * 100.0)
					/ 100.0) == (Math.round((proteinLength * Util.node_label_factor_size) * 100.0) / 100.0))
				other_node_width_or_height = ((Number) sourceNodeView.getVisualProperty(BasicVisualLexicon.NODE_WIDTH))
						.floatValue();

			initial_position_source_node = getXPositionOf(sourceNodeView);
			initial_position_target_node = getXPositionOf(targetNodeView);

		} else if (!isProtein_expansion_horizontal_source && !isProtein_expansion_horizontal_target) {
			// Source: vertical
			// Target: vertical

			other_node_width_or_height = ((Number) targetNodeView.getVisualProperty(BasicVisualLexicon.NODE_HEIGHT))
					.floatValue();
			if ((Math.round(other_node_width_or_height * 100.0)
					/ 100.0) == (Math.round((proteinLength * Util.node_label_factor_size) * 100.0) / 100.0))
				other_node_width_or_height = ((Number) sourceNodeView.getVisualProperty(BasicVisualLexicon.NODE_HEIGHT))
						.floatValue();

			initial_position_source_node = getYPositionOf(sourceNodeView);
			initial_position_target_node = getYPositionOf(targetNodeView);

		} else if (!isProtein_expansion_horizontal_source && isProtein_expansion_horizontal_target) {
			// Source: vertical
			// Target: horizontal

			other_node_width_or_height = ((Number) targetNodeView.getVisualProperty(BasicVisualLexicon.NODE_WIDTH))
					.floatValue();
			if ((Math.round(other_node_width_or_height * 100.0)
					/ 100.0) == (Math.round((proteinLength * Util.node_label_factor_size) * 100.0) / 100.0))
				other_node_width_or_height = ((Number) sourceNodeView.getVisualProperty(BasicVisualLexicon.NODE_WIDTH))
						.floatValue();

			initial_position_source_node = getYPositionOf(sourceNodeView);
			initial_position_target_node = getXPositionOf(targetNodeView);

		} else if (isProtein_expansion_horizontal_source && !isProtein_expansion_horizontal_target) {
			// Source: horizontal
			// Target: vertical

			other_node_width_or_height = ((Number) targetNodeView.getVisualProperty(BasicVisualLexicon.NODE_HEIGHT))
					.floatValue();
			if ((Math.round(other_node_width_or_height * 100.0)
					/ 100.0) == (Math.round((proteinLength * Util.node_label_factor_size) * 100.0) / 100.0))
				other_node_width_or_height = ((Number) sourceNodeView.getVisualProperty(BasicVisualLexicon.NODE_HEIGHT))
						.floatValue();

			initial_position_source_node = getXPositionOf(sourceNodeView);
			initial_position_target_node = getYPositionOf(targetNodeView);
		}

		double center_position_source_node = 0;
		double center_position_target_node = 0;
		double x_or_y_Pos_source = 0;
		double xl_pos_source = 0;

		double x_or_y_Pos_target = 0;
		double xl_pos_target = 0;

		String[] edgeNameArr = edge_name.split("\\[|\\]");
		String[] position1 = edgeNameArr[1].split("\\(|\\)");
		String[] position2 = edgeNameArr[3].split("\\(|\\)");

		double target_factor_scaling_length_protein = other_node_row.get(PROTEIN_SCALING_FACTOR_COLUMN_NAME,
				Double.class) == null ? 1 : other_node_row.get(PROTEIN_SCALING_FACTOR_COLUMN_NAME, Double.class);

		center_position_source_node = (proteinLength * Util.node_label_factor_size) / 2.0;
		center_position_target_node = (other_node_width_or_height) / 2.0;

		if (sourceNode.getSUID() == node.getSUID()) {
			xl_pos_source = Double.parseDouble(position1[1]) * Util.node_label_factor_size;
			xl_pos_target = Double.parseDouble(position2[1]) * target_factor_scaling_length_protein;
		} else {
			xl_pos_source = Double.parseDouble(position2[1]) * Util.node_label_factor_size;
			xl_pos_target = Double.parseDouble(position1[1]) * target_factor_scaling_length_protein;
		}

		if (xl_pos_source <= center_position_source_node) { // [-protein_length/2, 0]
			x_or_y_Pos_source = (-center_position_source_node) + xl_pos_source;
		} else { // [0, protein_length/2]
			x_or_y_Pos_source = xl_pos_source - center_position_source_node;
		}
		x_or_y_Pos_source += initial_position_source_node;

		if (xl_pos_target <= center_position_target_node) { // [-protein_length/2, 0]
			x_or_y_Pos_target = (-center_position_target_node) + xl_pos_target;
		} else { // [0, protein_length/2]
			x_or_y_Pos_target = xl_pos_target - center_position_target_node;
		}
		x_or_y_Pos_target += initial_position_target_node;

		// BLEND

		// ########## GET EDGE_BEND STYLE TO MODIFY #########

		Bend bend = bendFactory.createBend();

		Handle h = null;
		Handle h2 = null;

		if ((Math.round(other_node_width_or_height * 100.0) / 100.0) == (Math
				.round((((Number) length_other_protein_a).floatValue() * target_factor_scaling_length_protein) * 100.0)
				/ 100.0)) {// Target node has already been modified

			if (isProtein_expansion_horizontal_source && isProtein_expansion_horizontal_target) {
				// Source: horizontal
				// Target: horizontal

				h = handleFactory.createHandle(netView, newEdgeView, (x_or_y_Pos_source - OFFSET_BEND),
						getYPositionOf(sourceNodeView));

				h2 = handleFactory.createHandle(netView, newEdgeView, (x_or_y_Pos_target - OFFSET_BEND),
						getYPositionOf(targetNodeView));

			} else if (!isProtein_expansion_horizontal_source && !isProtein_expansion_horizontal_target) {
				// Source: vertical
				// Target: vertical

				h = handleFactory.createHandle(netView, newEdgeView, getXPositionOf(sourceNodeView),
						(x_or_y_Pos_source - OFFSET_BEND));

				h2 = handleFactory.createHandle(netView, newEdgeView, getXPositionOf(targetNodeView),
						(x_or_y_Pos_target - OFFSET_BEND));

			} else if (isProtein_expansion_horizontal_source && !isProtein_expansion_horizontal_target) {
				// Source: horizontal
				// Target: vertical

				h = handleFactory.createHandle(netView, newEdgeView, (x_or_y_Pos_source - OFFSET_BEND),
						getYPositionOf(sourceNodeView));

				h2 = handleFactory.createHandle(netView, newEdgeView, getXPositionOf(targetNodeView),
						(x_or_y_Pos_target - OFFSET_BEND));

			} else if (!isProtein_expansion_horizontal_source && isProtein_expansion_horizontal_target) {
				// Source: vertical
				// Target: horizontal

				h = handleFactory.createHandle(netView, newEdgeView, getXPositionOf(sourceNodeView),
						(x_or_y_Pos_source - OFFSET_BEND));

				h2 = handleFactory.createHandle(netView, newEdgeView, (x_or_y_Pos_target - OFFSET_BEND),
						getYPositionOf(targetNodeView));
			}

			if (sourceNode.getSUID() == node.getSUID()) {
				bend.insertHandleAt(0, h);
				bend.insertHandleAt(1, h2);
			} else {// If target node is the selected node then first insert h2
				bend.insertHandleAt(0, h2);
				bend.insertHandleAt(1, h);
			}

		} else {// Target node is intact

			if (isProtein_expansion_horizontal) {
				h = handleFactory.createHandle(netView, newEdgeView, (x_or_y_Pos_source - OFFSET_BEND),
						getYPositionOf(sourceNodeView));
			} else {
				h = handleFactory.createHandle(netView, newEdgeView, getXPositionOf(sourceNodeView),
						(x_or_y_Pos_source - OFFSET_BEND));
			}

			bend.insertHandleAt(0, h);
		}

		newEdgeView.setLockedValue(BasicVisualLexicon.EDGE_BEND, bend);

		VisualProperty<?> vp_edge_curved = lexicon.lookup(CyEdge.class, "EDGE_CURVED");
		if (vp_edge_curved != null) {
			Object edge_curved_obj = vp_edge_curved.parseSerializableString("false");
			if (edge_curved_obj != null) {
				newEdgeView.setLockedValue(vp_edge_curved, edge_curved_obj);
			}
		}

		// ### DISPLAY LINK ###
		String score_edge_str = edgeNameArr[edgeNameArr.length - 1];
		String[] score_edge_splitted = score_edge_str.split("Score:");
		String[] score_edge = score_edge_splitted[1].split("- Edge");
		if (-Math.log10(Double.parseDouble(score_edge[0])) < Util.interlink_threshold_score) {
			newEdgeView.setLockedValue(BasicVisualLexicon.EDGE_VISIBLE, false);
			newEdgeView.setLockedValue(BasicVisualLexicon.EDGE_TRANSPARENCY, 0);
			newEdgeView.setLockedValue(BasicVisualLexicon.EDGE_LABEL_TRANSPARENCY, 0);
		} else {
			newEdgeView.setLockedValue(BasicVisualLexicon.EDGE_VISIBLE, true);

			Color linkColor = InterLinksColor;
			if (!(location.isBlank() || location.isEmpty())) {
				linkColor = Util.proteinDomainsColorMap.get(location);
			}

			// #### UPDATE EDGE STYLE ####
			newEdgeView.setLockedValue(BasicVisualLexicon.EDGE_PAINT, linkColor);
			newEdgeView.setLockedValue(BasicVisualLexicon.EDGE_TRANSPARENCY, edge_link_opacity);
			newEdgeView.setLockedValue(BasicVisualLexicon.EDGE_WIDTH, edge_link_width);
			// ###########################

			// ### DISPLAY LINK LEGEND ###
			if (showLinksLegend) {
				newEdgeView.setLockedValue(BasicVisualLexicon.EDGE_LABEL_TRANSPARENCY, edge_label_opacity);
				newEdgeView.setLockedValue(BasicVisualLexicon.EDGE_LABEL_FONT_SIZE, edge_label_font_size);
			} else {
				newEdgeView.setLockedValue(BasicVisualLexicon.EDGE_LABEL_TRANSPARENCY, 0);
			}
			// ###########################
		}
		// ####################
	}

	/**
	 * Method responsible for updating ptm edges
	 * 
	 * @param myNetwork current network
	 * @param netView   current netview
	 */
	public static void setMonolinkPeptidesStyle(CyNetwork myNetwork, CyNetworkView netView) {
		for (CyNode node : myNetwork.getNodeList()) {

			// Check if the edge was inserted by this app
			String node_name = myNetwork.getDefaultNodeTable().getRow(node.getSUID()).get(CyNetwork.NAME, String.class);

			if (node_name != null && node_name.contains("MONOLINK")) {
				View<CyNode> newNodeView = netView.getNodeView(node);
				if (newNodeView == null)
					continue;

				newNodeView.setLockedValue(BasicVisualLexicon.NODE_FILL_COLOR, Util.MonoLinksPeptideColor);
				newNodeView.setLockedValue(BasicVisualLexicon.NODE_VISIBLE, Util.showMonolinkedPeptides);

			}
		}
	}

	/**
	 * Method responsible for updating ptm edges
	 * 
	 * @param myNetwork current network
	 * @param netView   current netview
	 */
	public static void setPTMStyleForAllNodes(CyNetwork myNetwork, CyNetworkView netView) {
		for (CyEdge edge : myNetwork.getEdgeList()) {

			// Check if the edge was inserted by this app
			String edge_name = myNetwork.getDefaultEdgeTable().getRow(edge.getSUID()).get(CyNetwork.NAME, String.class);

			if (edge_name != null && edge_name.contains("[Source: PTM - ")) {
				View<CyEdge> newEdgeView = netView.getEdgeView(edge);
				if (newEdgeView == null)
					continue;

				newEdgeView.setLockedValue(BasicVisualLexicon.EDGE_PAINT, Util.PTMColor);
				newEdgeView.setLockedValue(BasicVisualLexicon.EDGE_VISIBLE, Util.showPTMs);

			}
		}
	}

	/**
	 * Method responsible for getting all PTMs of a protein
	 * 
	 * @param taskMonitor task monitor
	 * @param myNetwork   current network
	 * @param node        current node
	 */
	private static List<CyEdge> getPTMsEdges(final TaskMonitor taskMonitor, CyNetwork myNetwork, CyNode node) {

		taskMonitor.showMessage(TaskMonitor.Level.INFO, "Updating PTM(s)...");

		List<CyEdge> ptmEdges = new ArrayList<CyEdge>();
		for (CyEdge edge : myNetwork.getAdjacentEdgeIterable(node, CyEdge.Type.ANY)) {

			// Check if the edge was inserted by this app
			String edge_name = myNetwork.getDefaultEdgeTable().getRow(edge.getSUID()).get(CyNetwork.NAME, String.class);

			if (edge_name.contains("[Source: PTM - ")) {
				ptmEdges.add(edge);

			}
		}

		return ptmEdges;

	}

	private static void setPTMStyle(CyNetworkView netView, HandleFactory handleFactory, BendFactory bendFactory,
			VisualLexicon lexicon, final TaskMonitor taskMonitor, CyEdge newEdge, double x_or_y_Pos_source,
			double xl_pos_source, double center_position_source_node, double initial_position_source_node,
			View<CyNode> sourceNodeView, CyNode new_ptm_node, PTM ptm) {

		View<CyEdge> newEdgeView = netView.getEdgeView(newEdge);
		while (newEdgeView == null) {
			netView.updateView();
			try {
				Thread.sleep(200);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			newEdgeView = netView.getEdgeView(newEdge);
		}

		newEdgeView.setLockedValue(BasicVisualLexicon.EDGE_PAINT, Util.PTMColor);
		newEdgeView.setLockedValue(BasicVisualLexicon.EDGE_SELECTED_PAINT, Color.RED);
		newEdgeView.setLockedValue(BasicVisualLexicon.EDGE_STROKE_SELECTED_PAINT, Color.RED);
		newEdgeView.setLockedValue(BasicVisualLexicon.EDGE_TRANSPARENCY, Util.edge_link_opacity);
		newEdgeView.setLockedValue(BasicVisualLexicon.EDGE_WIDTH, Util.edge_link_width);
		newEdgeView.setLockedValue(BasicVisualLexicon.EDGE_TARGET_ARROW_SHAPE, ArrowShapeVisualProperty.DIAMOND);
		newEdgeView.setLockedValue(BasicVisualLexicon.EDGE_SOURCE_ARROW_SHAPE, ArrowShapeVisualProperty.NONE);
		newEdgeView.setLockedValue(BasicVisualLexicon.EDGE_LINE_TYPE, LineTypeVisualProperty.SOLID);

		String tooltip = "<html><p><b>PTM: </b>" + ptm.name + "<br/><b>Residue: </b>" + ptm.residue + "[" + ptm.position
				+ "]</p></html>";

		newEdgeView.setLockedValue(BasicVisualLexicon.EDGE_TOOLTIP, tooltip);
		newEdgeView.setLockedValue(BasicVisualLexicon.EDGE_LABEL,
				"PTM: " + ptm.name + "[" + ptm.residue + "(" + ptm.position + ")]");

		if (Util.showLinksLegend) {
			newEdgeView.setLockedValue(BasicVisualLexicon.EDGE_LABEL_TRANSPARENCY, Util.edge_label_opacity);
			newEdgeView.setLockedValue(BasicVisualLexicon.EDGE_LABEL_FONT_SIZE, Util.edge_label_font_size);
		} else {
			newEdgeView.setLockedValue(BasicVisualLexicon.EDGE_LABEL_TRANSPARENCY, 0);
		}

		xl_pos_source = ptm.position * Util.node_label_factor_size;

		if (xl_pos_source <= center_position_source_node) { // [-protein_length/2, 0]
			x_or_y_Pos_source = (-center_position_source_node) + xl_pos_source;
		} else { // [0, protein_length/2]
			x_or_y_Pos_source = xl_pos_source - center_position_source_node;
		}
		x_or_y_Pos_source += initial_position_source_node;

		View<CyNode> targetNodeView = netView.getNodeView(new_ptm_node);

		if (Util.isProtein_expansion_horizontal) {
			targetNodeView.setLockedValue(BasicVisualLexicon.NODE_X_LOCATION, (x_or_y_Pos_source));
			targetNodeView.setLockedValue(BasicVisualLexicon.NODE_Y_LOCATION,
					Util.getYPositionOf(sourceNodeView) - PTM_LENGTH);
		} else {
			targetNodeView.setLockedValue(BasicVisualLexicon.NODE_X_LOCATION,
					Util.getXPositionOf(sourceNodeView) - PTM_LENGTH);
			targetNodeView.setLockedValue(BasicVisualLexicon.NODE_Y_LOCATION, (x_or_y_Pos_source));
		}

		targetNodeView.setLockedValue(BasicVisualLexicon.NODE_WIDTH, 0.01);
		targetNodeView.setLockedValue(BasicVisualLexicon.NODE_LABEL_TRANSPARENCY, 0);
		targetNodeView.setLockedValue(BasicVisualLexicon.NODE_LABEL, "");
		targetNodeView.setLockedValue(BasicVisualLexicon.NODE_VISIBLE, true);
		targetNodeView.setLockedValue(BasicVisualLexicon.NODE_BORDER_WIDTH, 0.0);
		targetNodeView.setLockedValue(BasicVisualLexicon.NODE_BORDER_PAINT, Color.WHITE);

		Bend bend = bendFactory.createBend();

		Handle h = null;

		if (Util.isProtein_expansion_horizontal) {
			h = handleFactory.createHandle(netView, newEdgeView, (x_or_y_Pos_source - OFFSET_BEND),
					Util.getYPositionOf(sourceNodeView));
		} else {
			h = handleFactory.createHandle(netView, newEdgeView, Util.getXPositionOf(sourceNodeView),
					(x_or_y_Pos_source - OFFSET_BEND));
		}

		bend.insertHandleAt(0, h);
		newEdgeView.setLockedValue(BasicVisualLexicon.EDGE_BEND, bend);

		VisualProperty<?> vp_edge_curved = lexicon.lookup(CyEdge.class, "EDGE_CURVED");
		if (vp_edge_curved != null) {
			Object edge_curved_obj = vp_edge_curved.parseSerializableString("false");
			if (edge_curved_obj != null) {
				newEdgeView.setLockedValue(vp_edge_curved, edge_curved_obj);
			}
		}
	}

	/**
	 * Plot monolink in the protein view
	 * 
	 * @param taskMonitor
	 */
	public static void setMonolinksToNode(final TaskMonitor taskMonitor, CyNetwork myNetwork, CyNetworkView netView,
			CyNode node, VisualStyle style, HandleFactory handleFactory, BendFactory bendFactory, VisualLexicon lexicon,
			ArrayList<CrossLink> myMonolinks, String proteinSequence) {

		if (myNetwork == null || node == null || style == null || netView == null || myMonolinks == null
				|| myMonolinks.size() == 0) {
			return;
		}

		final String node_name = myNetwork.getDefaultNodeTable().getRow(node.getSUID()).getRaw(CyNetwork.NAME)
				.toString();

		View<CyNode> sourceNodeView = netView.getNodeView(node);

		double x_or_y_Pos_source = 0;
		double xl_pos_source = 0;
		double center_position_source_node = (Util.proteinLength * Util.node_label_factor_size) / 2.0;

		double initial_position_source_node = 0;
		if (Util.isProtein_expansion_horizontal) {
			initial_position_source_node = Util.getXPositionOf(sourceNodeView);
		} else {
			initial_position_source_node = Util.getYPositionOf(sourceNodeView);
		}

		int countMonolink = 0;
		for (CrossLink monolink : myMonolinks) {

			final String node_name_added_by_app = "MONOLINK" + countMonolink + " [Source: " + node_name + " ("
					+ monolink.sequence + ")]";

			CyNode _node = Util.getNode(myNetwork, node_name_added_by_app);
			if (_node == null) {// Add a new node if does not exist

				_node = myNetwork.addNode();
				myNetwork.getRow(_node).set(CyNetwork.NAME, node_name_added_by_app);
			}

			setMonolinkStyle(netView, _node, sourceNodeView, monolink, xl_pos_source, center_position_source_node,
					x_or_y_Pos_source, initial_position_source_node);

			countMonolink++;
		}

		String network_name = myNetwork.toString();
		if (Util.monolinksMap.containsKey(network_name)) {

			Map<Long, Protein> protein_with_all_monolinks = Util.monolinksMap.get(network_name);

//			Protein ptn = null;
//			if (protein_with_all_monolinks.containsKey(node.getSUID())) {// Protein belongs to this network
//				ptn = protein_with_all_monolinks.get(node.getSUID());
//				ptn.monolinks = myMonolinks;
//			} else {// Create a new protein
//
//				ptn = new Protein(node_name, proteinSequence, myMonolinks);
//			}
//			protein_with_all_monolinks.put(node.getSUID(), ptn);

		} else {// Network does not exists

			Map<Long, Protein> protein_with_monolinks = new HashMap<Long, Protein>();

//			Protein ptn = new Protein(node_name, proteinSequence, myMonolinks);
//			protein_with_monolinks.put(node.getSUID(), ptn);
//			Util.monolinksMap.put(network_name, protein_with_monolinks);
		}
	}

	/**
	 * Method responsible for plotting residues
	 * 
	 * @param netView                      current net view
	 * @param current_node                 current node
	 * @param sourceNodeView               current source node view
	 * @param residue                      current residue
	 * @param xl_pos_source                position of source xl
	 * @param center_position_source_node  center position of source node
	 * @param x_or_y_Pos_source            x or y position of source node
	 * @param initial_position_source_node initial position of source node
	 */
	public static void setResidueStyle(CyNetworkView netView, CyNode current_node, View<CyNode> sourceNodeView,
			Residue residue, double xl_pos_source, double center_position_source_node, double x_or_y_Pos_source,
			double initial_position_source_node) {

		View<CyNode> newResidueView = netView.getNodeView(current_node);
		while (newResidueView == null) {
			netView.updateView();
			try {
				Thread.sleep(200);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			newResidueView = netView.getNodeView(current_node);
		}

		newResidueView.setLockedValue(BasicVisualLexicon.NODE_LABEL, "");
		newResidueView.setLockedValue(BasicVisualLexicon.NODE_VISIBLE, true);
		newResidueView.setLockedValue(BasicVisualLexicon.NODE_BORDER_WIDTH, 0.0);
		newResidueView.setLockedValue(BasicVisualLexicon.NODE_BORDER_TRANSPARENCY, 0.0);
		newResidueView.setLockedValue(BasicVisualLexicon.NODE_BORDER_PAINT, Color.WHITE);
		if (residue.isConflicted && residue.predicted_epoch <= ProcessProteinLocationTask.epochs)
			newResidueView.setLockedValue(BasicVisualLexicon.NODE_FILL_COLOR, Color.RED);
		else
			newResidueView.setLockedValue(BasicVisualLexicon.NODE_FILL_COLOR, NodeBorderColor);
		newResidueView.setLockedValue(BasicVisualLexicon.NODE_SELECTED_PAINT, Color.RED);
		newResidueView.setLockedValue(BasicVisualLexicon.NODE_Z_LOCATION, 1.0);

		String tooltip = "";
		boolean isPredicted = residue.predicted_epoch != -1;

		if (isPredicted) {
			if (!residue.isConflicted)
				tooltip = "<html><p><b>Residue: </b>" + residue.aminoacid + " [" + residue.position
						+ "]<br/><b>Predicted: </b>" + (residue.predicted_epoch != -1) + "<br/><b>Epoch: </b>"
						+ residue.predicted_epoch + "<br/><b>Score: </b>" + Math.floor(residue.score * 100) / 100;
			else
				tooltip = "<html><p><b>Residue: </b>" + residue.aminoacid + " [" + residue.position
						+ "]<br/><b>Predicted: </b>" + (residue.predicted_epoch != -1) + "<br/><b>Epoch: </b>"
						+ residue.predicted_epoch + "<br/><u><i>There is a conflict.</i></u>";
		} else
			tooltip = "<html><p><b>Residue: </b> " + residue.aminoacid + " [" + residue.position + "]<br/>";

		newResidueView.setLockedValue(BasicVisualLexicon.NODE_TOOLTIP, tooltip);

		plotResidueNodes(netView, current_node, sourceNodeView, residue, center_position_source_node,
				initial_position_source_node);
	}

	private static void plotResidueNodes(CyNetworkView netView, CyNode current_node, View<CyNode> sourceNodeView,
			Residue residue, double center_position_source_node, double initial_position_source_node) {

		View<CyNode> newResidueView = netView.getNodeView(current_node);
		while (newResidueView == null) {
			netView.updateView();
			try {
				Thread.sleep(200);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			newResidueView = netView.getNodeView(current_node);
		}

		double xl_pos_source;
		double x_or_y_Pos_source;
		xl_pos_source = residue.position * Util.node_label_factor_size;

		if (xl_pos_source <= center_position_source_node) { // [-protein_length/2, 0]
			x_or_y_Pos_source = (-center_position_source_node) + xl_pos_source;
		} else { // [0, protein_length/2]
			x_or_y_Pos_source = xl_pos_source - center_position_source_node;
		}
		x_or_y_Pos_source += initial_position_source_node;

		double node_height = 15d;

		if (Util.isProtein_expansion_horizontal) {

			newResidueView.setLockedValue(BasicVisualLexicon.NODE_WIDTH, 1.0);
			newResidueView.setLockedValue(BasicVisualLexicon.NODE_HEIGHT, node_height);

			newResidueView.setLockedValue(BasicVisualLexicon.NODE_X_LOCATION, (x_or_y_Pos_source));
			newResidueView.setLockedValue(BasicVisualLexicon.NODE_Y_LOCATION, Util.getYPositionOf(sourceNodeView));
		} else {

			newResidueView.setLockedValue(BasicVisualLexicon.NODE_WIDTH, node_height);
			newResidueView.setLockedValue(BasicVisualLexicon.NODE_HEIGHT, 1.0);

			newResidueView.setLockedValue(BasicVisualLexicon.NODE_X_LOCATION, Util.getXPositionOf(sourceNodeView));
			newResidueView.setLockedValue(BasicVisualLexicon.NODE_Y_LOCATION, (x_or_y_Pos_source));
		}
	}

	/**
	 * Method responsible for setting style to monolink
	 * 
	 * @param netView                      current netview
	 * @param current_node                 current node
	 * @param sourceNodeView               current source node
	 * @param monolink                     monolink
	 * @param xl_pos_source                xl position
	 * @param center_position_source_node  center position
	 * @param x_or_y_Pos_source            final xl position
	 * @param initial_position_source_node initial source node position
	 */
	private static void setMonolinkStyle(CyNetworkView netView, CyNode current_node, View<CyNode> sourceNodeView,
			CrossLink monolink, double xl_pos_source, double center_position_source_node, double x_or_y_Pos_source,
			double initial_position_source_node) {

		View<CyNode> newMonolinkView = netView.getNodeView(current_node);
		while (newMonolinkView == null) {
			netView.updateView();
			try {
				Thread.sleep(200);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			newMonolinkView = netView.getNodeView(current_node);
		}

		newMonolinkView.setLockedValue(BasicVisualLexicon.NODE_LABEL, "");
		newMonolinkView.setLockedValue(BasicVisualLexicon.NODE_VISIBLE, true);
		newMonolinkView.setLockedValue(BasicVisualLexicon.NODE_BORDER_WIDTH, 0.0);
		newMonolinkView.setLockedValue(BasicVisualLexicon.NODE_BORDER_PAINT, Color.WHITE);
		newMonolinkView.setLockedValue(BasicVisualLexicon.NODE_FILL_COLOR, MonoLinksPeptideColor);
		newMonolinkView.setLockedValue(BasicVisualLexicon.NODE_SELECTED_PAINT, Color.RED);
		newMonolinkView.setLockedValue(BasicVisualLexicon.NODE_Z_LOCATION, 1.0);

		String sequence = "";

		// SEQUENCE[1-4]
		if (monolink.pos_site_a == 1) {
			sequence = "<b>[" + monolink.sequence.charAt(0) + "]</b>"
					+ monolink.sequence.substring(1, monolink.pos_site_b - 1) + "<b>["
					+ monolink.sequence.charAt(monolink.pos_site_b - 1) + "]</b>"
					+ monolink.sequence.substring(monolink.pos_site_b, monolink.sequence.length());
		} else {
			// SEQUENCE[3-6]
			sequence = monolink.sequence.substring(0, monolink.pos_site_a - 1) + "<b>["
					+ monolink.sequence.charAt(monolink.pos_site_a - 1) + "]</b>"
					+ monolink.sequence.substring(monolink.pos_site_a, monolink.pos_site_b - 1) + "<b>["
					+ monolink.sequence.charAt(monolink.pos_site_b - 1) + "]</b>"
					+ monolink.sequence.substring(monolink.pos_site_b, monolink.sequence.length());
		}

		String tooltip = "<html><p><b>Monolink: </b>" + sequence + "<br/><b>Reaction site 1: </b>" + monolink.pos_site_a
				+ "<br/><b>Reaction site 2: </b>" + monolink.pos_site_b + "</p></html>";

		newMonolinkView.setLockedValue(BasicVisualLexicon.NODE_TOOLTIP, tooltip);

		xl_pos_source = monolink.start_pos_protein * Util.node_label_factor_size;

		if (xl_pos_source <= center_position_source_node) { // [-protein_length/2, 0]
			x_or_y_Pos_source = (-center_position_source_node) + xl_pos_source;
		} else { // [0, protein_length/2]
			x_or_y_Pos_source = xl_pos_source - center_position_source_node;
		}
		x_or_y_Pos_source += initial_position_source_node;

		double node_width = (monolink.end_pos_protein - monolink.start_pos_protein + 1) * node_label_factor_size;

		if (Util.isProtein_expansion_horizontal) {

			newMonolinkView.setLockedValue(BasicVisualLexicon.NODE_WIDTH, node_width);
			newMonolinkView.setLockedValue(BasicVisualLexicon.NODE_HEIGHT, 4.0);

			newMonolinkView.setLockedValue(BasicVisualLexicon.NODE_X_LOCATION,
					(x_or_y_Pos_source - OFFSET_MONOLINK + (node_width / 2)));
			newMonolinkView.setLockedValue(BasicVisualLexicon.NODE_Y_LOCATION, Util.getYPositionOf(sourceNodeView));
		} else {

			newMonolinkView.setLockedValue(BasicVisualLexicon.NODE_WIDTH, 4.0);
			newMonolinkView.setLockedValue(BasicVisualLexicon.NODE_HEIGHT, node_width);

			newMonolinkView.setLockedValue(BasicVisualLexicon.NODE_X_LOCATION, Util.getXPositionOf(sourceNodeView));
			newMonolinkView.setLockedValue(BasicVisualLexicon.NODE_Y_LOCATION,
					(x_or_y_Pos_source - OFFSET_MONOLINK + (node_width / 2)));
		}
	}

	/**
	 * Plot PTM in the protein view
	 * 
	 * @param taskMonitor
	 */
	public static void setNodePTMs(final TaskMonitor taskMonitor, CyNetwork myNetwork, CyNetworkView netView,
			CyNode node, VisualStyle style, HandleFactory handleFactory, BendFactory bendFactory, VisualLexicon lexicon,
			ArrayList<PTM> myPTMs, boolean updateEdge) {

		if (myNetwork == null || node == null || style == null || netView == null || myPTMs == null
				|| myPTMs.size() == 0) {
			return;
		}

		final String node_name = myNetwork.getDefaultNodeTable().getRow(node.getSUID()).getRaw(CyNetwork.NAME)
				.toString();

		View<CyNode> sourceNodeView = netView.getNodeView(node);

		List<CyEdge> ptms_edges = new ArrayList<CyEdge>();
		if (!updateEdge) {
			ptms_edges = getPTMsEdges(taskMonitor, myNetwork, node);
		}

		double x_or_y_Pos_source = 0;
		double xl_pos_source = 0;
		double center_position_source_node = (Util.proteinLength * Util.node_label_factor_size) / 2.0;

		double initial_position_source_node = 0;
		if (Util.isProtein_expansion_horizontal) {
			initial_position_source_node = Util.getXPositionOf(sourceNodeView);
		} else {
			initial_position_source_node = Util.getYPositionOf(sourceNodeView);
		}

		int countPTM = 0;
		for (PTM ptm : myPTMs) {

			final String egde_name_added_by_app = "Edge" + countPTM + " [Source: PTM - " + node_name + " ("
					+ ptm.residue + "-" + ptm.position + ")]";

			CyEdge current_edge = Util.getEdge(myNetwork, egde_name_added_by_app, false);
			if (current_edge == null) {// Add a new edge if does not exist

				final String node_name_source = "PTM - " + node_name + " (" + ptm.residue + "-" + ptm.position + ")";

				CyNode new_ptm_node = myNetwork.addNode();
				myNetwork.getRow(new_ptm_node).set(CyNetwork.NAME, node_name_source);

				CyEdge newEdge = myNetwork.addEdge(node, new_ptm_node, true);
				myNetwork.getRow(newEdge).set(CyNetwork.NAME, egde_name_added_by_app);

				setPTMStyle(netView, handleFactory, bendFactory, lexicon, taskMonitor, newEdge, x_or_y_Pos_source,
						xl_pos_source, center_position_source_node, initial_position_source_node, sourceNodeView,
						new_ptm_node, ptm);

			} else { // Update edge position

				View<CyEdge> edgeView = netView.getEdgeView(current_edge);
				edgeView.setLockedValue(BasicVisualLexicon.EDGE_VISIBLE, true);

				String node_name_source = "PTM - " + node_name + " (" + ptm.residue + "-" + ptm.position + ")";
				CyNode target_node = getNode(myNetwork, node_name_source);
				if (target_node != null) {

					setPTMStyle(netView, handleFactory, bendFactory, lexicon, taskMonitor, current_edge,
							x_or_y_Pos_source, xl_pos_source, center_position_source_node, initial_position_source_node,
							sourceNodeView, target_node, ptm);
				}

				ptms_edges.remove(current_edge);
			}
			countPTM++;
		}

		String network_name = myNetwork.toString();
		if (Util.ptmsMap.containsKey(network_name)) {

			Map<Long, List<PTM>> all_ptms = Util.ptmsMap.get(network_name);
			all_ptms.put(node.getSUID(), myPTMs);

		} else {// Network does not exists

			Map<Long, List<PTM>> ptms = new HashMap<Long, List<PTM>>();
			ptms.put(node.getSUID(), myPTMs);
			Util.ptmsMap.put(network_name, ptms);
		}

		if (ptms_edges.size() > 0) {

			List<CyNode> nodes_to_be_removed = new ArrayList<CyNode>();
			for (CyEdge edge : ptms_edges) {
				CyNode node_to_be_removed = null;
				if (node.getSUID() == edge.getSource().getSUID())
					node_to_be_removed = edge.getTarget();
				else
					node_to_be_removed = edge.getSource();

				nodes_to_be_removed.add(node_to_be_removed);
			}

			myNetwork.removeEdges(ptms_edges);
			myNetwork.removeNodes(nodes_to_be_removed);
		}
	}

	/**
	 * Update all edges that represent intralinks according to their current
	 * position
	 * 
	 * @param myNetwork              current network
	 * @param netView                current network view
	 * @param original_node          current original node
	 * @param edgeView               current edge view
	 * @param intraLinks             all intralinks
	 * @param countEdge              amount of edges
	 * @param center_position_node   center position of the current node
	 * @param initial_positionX_node initial position X of the current node
	 * @param initial_positionY_node initial position Y of the current node
	 */
	private static void updateIntraLinkEdgesPosition(CyNetwork myNetwork, CyNetworkView netView, CyNode original_node,
			View<CyEdge> edgeView, List<CrossLink> intraLinks, int countEdge, double center_position_node,
			double initial_positionX_node, double initial_positionY_node) {

		double factor_scaling_protein_length = myNetwork.getRow(original_node).get(PROTEIN_SCALING_FACTOR_COLUMN_NAME,
				Double.class) == null ? 1
						: myNetwork.getRow(original_node).get(PROTEIN_SCALING_FACTOR_COLUMN_NAME, Double.class);

		float offSet = 0;
		if (factor_scaling_protein_length != 1) {
			View<CyNode> new_node_view = netView.getNodeView(original_node);
			if (isProtein_expansion_horizontal) {
				offSet = ((Number) new_node_view.getVisualProperty(BasicVisualLexicon.NODE_HEIGHT)).floatValue() / 2;
			} else {
				offSet = ((Number) new_node_view.getVisualProperty(BasicVisualLexicon.NODE_WIDTH)).floatValue() / 2;
			}
		}

		final String node_name_source = intraLinks.get(countEdge).protein_a + " ["
				+ intraLinks.get(countEdge).pos_site_a + " - " + intraLinks.get(countEdge).pos_site_b + "] - Source";

		CyNode new_node_source = getNode(myNetwork, node_name_source);
		if (new_node_source != null) {

			double x_or_y_Pos_source = 0;
			double xl_pos_source = intraLinks.get(countEdge).pos_site_a * factor_scaling_protein_length;
			if (xl_pos_source <= center_position_node) { // [-protein_length/2, 0]
				x_or_y_Pos_source = (-center_position_node) + xl_pos_source;
			} else { // [0, protein_length/2]
				x_or_y_Pos_source = xl_pos_source - center_position_node;
			}
			if (isProtein_expansion_horizontal) {
				x_or_y_Pos_source += initial_positionX_node;
			} else {
				x_or_y_Pos_source += initial_positionY_node;
			}

			View<CyNode> new_node_source_view = netView.getNodeView(new_node_source);
			if (isProtein_expansion_horizontal) {
				new_node_source_view.setLockedValue(BasicVisualLexicon.NODE_X_LOCATION, x_or_y_Pos_source);
				new_node_source_view.setLockedValue(BasicVisualLexicon.NODE_Y_LOCATION,
						initial_positionY_node + offSet);
			} else {
				new_node_source_view.setLockedValue(BasicVisualLexicon.NODE_X_LOCATION,
						initial_positionX_node + offSet);
				new_node_source_view.setLockedValue(BasicVisualLexicon.NODE_Y_LOCATION, x_or_y_Pos_source);
			}
			new_node_source_view.setLockedValue(BasicVisualLexicon.NODE_VISIBLE, true);
			new_node_source_view.setLockedValue(BasicVisualLexicon.NODE_BORDER_WIDTH, 0.0);
			new_node_source_view.setLockedValue(BasicVisualLexicon.NODE_BORDER_PAINT, Color.WHITE);
		}

		final String node_name_target = intraLinks.get(countEdge).protein_a + " ["
				+ intraLinks.get(countEdge).pos_site_a + " - " + intraLinks.get(countEdge).pos_site_b + "] - Target";

		CyNode new_node_target = getNode(myNetwork, node_name_target);
		if (new_node_target != null) {

			double x_or_y_Pos_target = 0;
			double xl_pos_target = intraLinks.get(countEdge).pos_site_b * factor_scaling_protein_length;
			if (xl_pos_target <= center_position_node) { // [-protein_length/2, 0]
				x_or_y_Pos_target = (-center_position_node) + xl_pos_target;
			} else { // [0, protein_length/2]
				x_or_y_Pos_target = xl_pos_target - center_position_node;
			}
			if (isProtein_expansion_horizontal) {
				x_or_y_Pos_target += initial_positionX_node;
			} else {
				x_or_y_Pos_target += initial_positionY_node;
			}

			View<CyNode> new_node_target_view = netView.getNodeView(new_node_target);
			if (isProtein_expansion_horizontal) {
				new_node_target_view.setLockedValue(BasicVisualLexicon.NODE_X_LOCATION, x_or_y_Pos_target);
				new_node_target_view.setLockedValue(BasicVisualLexicon.NODE_Y_LOCATION,
						initial_positionY_node + offSet);
			} else {
				new_node_target_view.setLockedValue(BasicVisualLexicon.NODE_X_LOCATION,
						initial_positionX_node + offSet);
				new_node_target_view.setLockedValue(BasicVisualLexicon.NODE_Y_LOCATION, x_or_y_Pos_target);
			}
			new_node_target_view.setLockedValue(BasicVisualLexicon.NODE_VISIBLE, true);
			new_node_target_view.setLockedValue(BasicVisualLexicon.NODE_BORDER_WIDTH, 0.0);
			new_node_target_view.setLockedValue(BasicVisualLexicon.NODE_BORDER_PAINT, Color.WHITE);
		}

		// #### UPDATE EDGE STYLE ####

		Color linkColor = IntraLinksColor;
		CrossLink link = intraLinks.get(countEdge);

		if (link != null && link.location != null && !link.location.equals("UK")) {
			linkColor = Util.proteinDomainsColorMap.get(link.location);
		}

		edgeView.setLockedValue(BasicVisualLexicon.EDGE_PAINT, linkColor);
		edgeView.setLockedValue(BasicVisualLexicon.EDGE_TRANSPARENCY, edge_link_opacity);
		edgeView.setLockedValue(BasicVisualLexicon.EDGE_WIDTH, edge_link_width);
		// ###########################

		// ### DISPLAY LINK LEGEND ###
		if (showLinksLegend) {
			edgeView.setLockedValue(BasicVisualLexicon.EDGE_LABEL_TRANSPARENCY, edge_label_opacity);
			edgeView.setLockedValue(BasicVisualLexicon.EDGE_LABEL_FONT_SIZE, edge_label_font_size);
		} else {
			edgeView.setLockedValue(BasicVisualLexicon.EDGE_LABEL_TRANSPARENCY, 0);
		}
		// ###########################

		if (intraLinks.get(countEdge).score != Double.NaN
				&& -Math.log10(intraLinks.get(countEdge).score) < intralink_threshold_score)// hide
		// intralink
		{
			edgeView.setLockedValue(BasicVisualLexicon.EDGE_VISIBLE, false);
			edgeView.setLockedValue(BasicVisualLexicon.EDGE_LABEL_TRANSPARENCY, 0);
		}
	}

	/**
	 * Check and display the edges based on comb_score
	 * 
	 * @param myNetwork            current network
	 * @param cyApplicationManager main app manager
	 * @param netView              current network view
	 * @param handleFactory        handle factory
	 * @param bendFactory          bend factory
	 * @param current_node         current node
	 */
//	public static void checkUnmodifiedEdgeToDisplay(CyNetwork myNetwork, CyApplicationManager cyApplicationManager,
//			CyNetworkView netView, HandleFactory handleFactory, BendFactory bendFactory, CyNode current_node) {
//
//		if (myNetwork == null) {
//			if (cyApplicationManager != null) {
//				myNetwork = cyApplicationManager.getCurrentNetwork();
//				netView = cyApplicationManager.getCurrentNetworkView();
//			}
//		}
//
//		VisualStyle style = MainSingleNodeTask.style;
//		if (style == null)
//			style = LoadProteinDomainTask.style;
//
//		if (style == null)
//			return;
//
//		VisualLexicon lexicon = MainSingleNodeTask.lexicon;
//		if (lexicon == null)
//			lexicon = LoadProteinDomainTask.lexicon;
//
//		if (lexicon == null)
//			return;
//
//		for (CyEdge edge : myNetwork.getAdjacentEdgeIterable(current_node, CyEdge.Type.ANY)) {
//
//			CyRow myCurrentRow = myNetwork.getDefaultEdgeTable().getRow(edge.getSUID());
//			double comb_score = -1;
//
//			if (myCurrentRow.getRaw(XL_COMB_SCORE) != null) {
//				comb_score = Double.parseDouble(myCurrentRow.getRaw(XL_COMB_SCORE).toString());
//				comb_score = -Math.log10(comb_score);
//
//				View<CyEdge> currentEdgeView = netView.getEdgeView(edge);
//				if (comb_score < combinedlink_threshold_score) {
//					currentEdgeView.setLockedValue(BasicVisualLexicon.EDGE_VISIBLE, false);
//				} else {
//					currentEdgeView.setLockedValue(BasicVisualLexicon.EDGE_VISIBLE, true);
//				}
//			}
//		}
//	}

	/**
	 * Check and update all edges of associated nodes
	 * 
	 * @param myNetwork            current network
	 * @param cyApplicationManager main app manager
	 * @param netView              current network view
	 * @param handleFactory        handle factory
	 * @param bendFactory          bend factory
	 * @param current_node         current node
	 */
	public static void updateAllAssiciatedInterlinkNodes(CyNetwork myNetwork, CyApplicationManager cyApplicationManager,
			CyNetworkView netView, HandleFactory handleFactory, BendFactory bendFactory, CyNode current_node) {

		if (myNetwork == null) {
			if (cyApplicationManager != null) {
				myNetwork = cyApplicationManager.getCurrentNetwork();
				netView = cyApplicationManager.getCurrentNetworkView();
			}
		}

		Object length_other_protein_a;
		Object length_other_protein_b;

		Set<Long> nodeSuidList = new HashSet<Long>();
		nodeSuidList.add(current_node.getSUID());

		for (CyEdge edge : myNetwork.getAdjacentEdgeIterable(current_node, CyEdge.Type.ANY)) {
			CyNode sourceNode = myNetwork.getEdge(edge.getSUID()).getSource();
			CyNode targetNode = myNetwork.getEdge(edge.getSUID()).getTarget();

			if (sourceNode.getSUID() == current_node.getSUID())
				nodeSuidList.add(targetNode.getSUID());
			else
				nodeSuidList.add(sourceNode.getSUID());
		}

		VisualStyle style = MainSingleNodeTask.style;
		if (style == null)
			style = ProcessProteinLocationTask.style;

		if (style == null)
			return;

		VisualLexicon lexicon = MainSingleNodeTask.lexicon;
		if (lexicon == null)
			lexicon = ProcessProteinLocationTask.lexicon;

		if (lexicon == null)
			return;

		for (final Long protein_suid : nodeSuidList) {

			try {

				CyNode proteinA_node = getNode(myNetwork, protein_suid);
				if (proteinA_node != null) {

					CyRow proteinA_node_row = myNetwork.getRow(proteinA_node);

					length_other_protein_a = proteinA_node_row.getRaw("length_protein_a");
					length_other_protein_b = proteinA_node_row.getRaw("length_protein_b");

					if (length_other_protein_a == null) {
						if (length_other_protein_b == null)
							length_other_protein_a = 10;
						else
							length_other_protein_a = length_other_protein_b;
					}

					if (IsNodeModified(myNetwork, netView, proteinA_node)) {
						MainSingleNodeTask.node = proteinA_node;
						setProteinLength(((Number) length_other_protein_a).floatValue());

						String node_name = myNetwork.getDefaultNodeTable().getRow(proteinA_node.getSUID())
								.getRaw(CyNetwork.NAME).toString();
						Protein protein = Util.getProtein(myNetwork, node_name);

						View<CyNode> proteinA_nodeView = netView.getNodeView(proteinA_node);
						addOrUpdateEdgesToNetwork(myNetwork, proteinA_node, style, netView, proteinA_nodeView,
								handleFactory, bendFactory, lexicon, ((Number) length_other_protein_a).floatValue(),
								protein, null, null);

//						if (current_node != null) {
//							inter_and_intralinks = getAllLinksFromNode(current_node, myNetwork);// update
//																								// intraLinks
//																								// &
//																								// interLinks
//																								// with
//																								// the
//																								// current
//																								// selected
//																								// node
//							MainSingleNodeTask.interLinks = (ArrayList<CrossLink>) inter_and_intralinks.getFirst();
//							MainSingleNodeTask.intraLinks = (ArrayList<CrossLink>) inter_and_intralinks.getSecond();
//						}
					}
//					else {
//						
//						checkUnmodifiedEdgeToDisplay(myNetwork, cyApplicationManager, netView, handleFactory, bendFactory,
//								current_node);
//						
//					}
				}
			} catch (Exception e) {
				continue;
			}

		}
	}

	/**
	 * Method responsible for updating unmodified edges based on the score
	 * 
	 * @param myNetwork current network
	 * @param netView   current network view
	 */
	public static void filterUnmodifiedEdges(CyNetwork myNetwork, CyNetworkView netView) {
		List<CyEdge> allEdges = myNetwork.getEdgeList();

		try {
			if (allEdges.size() > 0) {
				// Display edge score in all edges
				for (CyEdge edge : allEdges) {

					CyNode sourceNode = myNetwork.getEdge(edge.getSUID()).getSource();
					CyNode targetNode = myNetwork.getEdge(edge.getSUID()).getTarget();

					if (Util.IsNodeModified(myNetwork, netView, sourceNode)
							|| Util.IsNodeModified(myNetwork, netView, targetNode))
						continue;

					CyRow myCurrentRow = myNetwork.getDefaultEdgeTable().getRow(edge.getSUID());
					if (myCurrentRow.getRaw(Util.XL_COMB_SCORE) != null) {

						View<CyEdge> edgeView = netView.getEdgeView(edge);
						edgeView.setLockedValue(BasicVisualLexicon.EDGE_VISIBLE, true);

						double log_comb_score = -1;

						try {
							double comb_score = Double.parseDouble(myCurrentRow.getRaw(Util.XL_COMB_SCORE).toString());
							log_comb_score = Util.round(-Math.log10(comb_score), 2);

							if (edgeView.getVisualProperty(BasicVisualLexicon.EDGE_TOOLTIP).isBlank()
									|| edgeView.getVisualProperty(BasicVisualLexicon.EDGE_TOOLTIP).isEmpty()) {

								String tooltip = "<html><p><i>Score: " + comb_score + "</i></p><p><i>-Log(score): "
										+ log_comb_score + "</i></p></html>";
								edgeView.setLockedValue(BasicVisualLexicon.EDGE_TOOLTIP, tooltip);
							}

							if (log_comb_score < Util.combinedlink_threshold_score) {
								edgeView.setLockedValue(BasicVisualLexicon.EDGE_VISIBLE, false);
							} else {
								edgeView.setLockedValue(BasicVisualLexicon.EDGE_VISIBLE, true);
							}

						} catch (Exception e) {
						}

					} else if (!myCurrentRow.getRaw(CyNetwork.NAME).toString().contains("[Source")) {
						continue;// There is no score information to be displayed in the original edge.
					}
				}
				// Apply the change to the view
				netView.updateView();
			}
		} catch (Exception e) {
		}

	}

	/**
	 * Method responsible for updating edges based on the score
	 * 
	 * @param myNetwork current network
	 * @param netView   current network view
	 */
	public static void updateEdgesStyle(CyNetwork myNetwork, CyNetworkView netView) {
		List<CyEdge> allEdges = myNetwork.getEdgeList();

		if (allEdges.size() > 1) {

			for (CyEdge edge : allEdges) {

				if (!isEdgeModified(myNetwork, netView, edge)) {
					continue;
				}

				// Check if the edge was inserted by this app
				String edge_name = myNetwork.getDefaultEdgeTable().getRow(edge.getSUID()).get(CyNetwork.NAME,
						String.class);

				CyNode sourceNode = myNetwork.getEdge(edge.getSUID()).getSource();
				String sourceName = myNetwork.getDefaultNodeTable().getRow(sourceNode.getSUID()).get(CyNetwork.NAME,
						String.class);
				CyNode targetNode = myNetwork.getEdge(edge.getSUID()).getTarget();
				String targetName = myNetwork.getDefaultNodeTable().getRow(targetNode.getSUID()).get(CyNetwork.NAME,
						String.class);

				boolean IsIntraLink;

				if (sourceName.contains("- Source") && targetName.contains("- Target"))// Intra link nodes
					IsIntraLink = true;
				else
					IsIntraLink = false;

				View<CyEdge> newEdgeView = netView.getEdgeView(edge);
				while (newEdgeView == null) {
					netView.updateView();
					try {
						Thread.sleep(200);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
					newEdgeView = netView.getEdgeView(edge);
				}

				if (!IsIntraLink) { // It's interlink
					if (!Util.IsNodeModified(myNetwork, netView, sourceNode)
							&& !Util.IsNodeModified(myNetwork, netView, targetNode))
						continue;

					try {
						// Edge name e.g.: [Source: Ndufa9 (113)] [Target: Ndufs7 (62)] - Score:2.4E-23
						// - Edge0
						String[] cols = edge_name.split("Score:");
						cols = cols[1].split("- Edge");
						double interlink_score = Double.parseDouble(cols[0]);

						if (interlink_score != Double.NaN && -Math.log10(interlink_score) < interlink_threshold_score) {// hide
																														// interlink

							newEdgeView.setLockedValue(BasicVisualLexicon.EDGE_VISIBLE, false);
							newEdgeView.setLockedValue(BasicVisualLexicon.EDGE_LABEL_TRANSPARENCY, 0);
						} else {

							if (showLinksLegend)
								newEdgeView.setLockedValue(BasicVisualLexicon.EDGE_LABEL_TRANSPARENCY,
										edge_label_opacity);
							newEdgeView.setLockedValue(BasicVisualLexicon.EDGE_VISIBLE, true);
						}

					} catch (Exception e) {
					}

					newEdgeView.setLockedValue(BasicVisualLexicon.EDGE_PAINT, InterLinksColor);

				} else {// It's intralink

					try {
						// EdgeName e.g.: Edge0 [Source: Ndufa9 (113)] [Target: Ndufa9 (157)] - Score:
						// 5.44E-16
						String[] cols = edge_name.split("Score:");
						double intralink_score = Double.parseDouble(cols[1]);

						View<CyNode> sourceNodeView = netView.getNodeView(sourceNode);
						View<CyNode> targetNodeView = netView.getNodeView(targetNode);

						if (intralink_score != Double.NaN && -Math.log10(intralink_score) < intralink_threshold_score) {// hide
							sourceNodeView.setLockedValue(BasicVisualLexicon.NODE_VISIBLE, false);
							targetNodeView.setLockedValue(BasicVisualLexicon.NODE_VISIBLE, false);
						} else {
							sourceNodeView.setLockedValue(BasicVisualLexicon.NODE_VISIBLE, true);
							targetNodeView.setLockedValue(BasicVisualLexicon.NODE_VISIBLE, true);
						}
					} catch (Exception e) {
					}

					newEdgeView.setLockedValue(BasicVisualLexicon.EDGE_PAINT, IntraLinksColor);

				}

				newEdgeView.setLockedValue(BasicVisualLexicon.EDGE_SELECTED_PAINT, Color.RED);
				newEdgeView.setLockedValue(BasicVisualLexicon.EDGE_STROKE_SELECTED_PAINT, Color.RED);
				newEdgeView.setLockedValue(BasicVisualLexicon.EDGE_TRANSPARENCY, edge_link_opacity);
				newEdgeView.setLockedValue(BasicVisualLexicon.EDGE_LABEL_FONT_SIZE, edge_label_font_size);
				if (showLinksLegend) {
					newEdgeView.setLockedValue(BasicVisualLexicon.EDGE_LABEL_TRANSPARENCY, edge_label_opacity);
					newEdgeView.setLockedValue(BasicVisualLexicon.EDGE_LABEL_FONT_SIZE, edge_label_font_size);
				} else {
					newEdgeView.setLockedValue(BasicVisualLexicon.EDGE_LABEL_TRANSPARENCY, 0);
				}
				newEdgeView.setLockedValue(BasicVisualLexicon.EDGE_WIDTH, edge_link_width);

			}

			// Apply the change to the view
			netView.updateView();
		}
	}

	/**
	 * Method responsible for updating the style of all nodes from Settings panel
	 * 
	 * @param myNetwork      current network
	 * @param netView        current netview
	 * @param style          current style
	 * @param handleFactory  current hf
	 * @param bendFactory    current bf
	 * @param lexicon        current lexicon
	 * @param hideInterLinks checkbox on SettingsPanel is unselected
	 */
	public static void updateNodesStyles(CyNetwork myNetwork, CyNetworkView netView, VisualStyle style,
			HandleFactory handleFactory, BendFactory bendFactory, VisualLexicon lexicon, boolean hideInterLinks) {

		List<CyNode> allnodes = myNetwork.getNodeList();

		if (allnodes.size() > 1) {

			for (CyNode cyNode : allnodes) {
				if (IsNodeModified(myNetwork, netView, cyNode)) {

					updateNodeStyle(myNetwork, cyNode, netView);

					String node_name = myNetwork.getDefaultNodeTable().getRow(cyNode.getSUID()).getRaw(CyNetwork.NAME)
							.toString();
					Protein protein = Util.getProtein(myNetwork, node_name);

					updateOrCreateEdgesFromSettingsPanel(myNetwork, cyNode, netView, style, handleFactory, bendFactory,
							lexicon, protein);

					if (hideInterLinks) {// hide all interlinks

						hideAllInterLinks(myNetwork, cyNode, netView);
					}
				}
			}
		}
	}

	/**
	 * Method responsible for updating the style of all nodes
	 * 
	 * @param myNetwork current network
	 * @param netView   current netView
	 */
	public static void updateNodesStyles(CyNetwork myNetwork, CyNetworkView netView) {
		List<CyNode> allnodes = myNetwork.getNodeList();

		if (allnodes.size() > 1) {

			for (CyNode cyNode : allnodes) {
				if (IsNodeModified(myNetwork, netView, cyNode)) {
					updateNodeStyle(myNetwork, cyNode, netView);

				}
			}
		}
	}

	/**
	 * Method responsible for updating the style of a node
	 * 
	 * @param myNetwork
	 * @param node
	 * @param netView
	 */
	private static void updateNodeStyle(CyNetwork myNetwork, CyNode node, CyNetworkView netView) {

		if (myNetwork == null || node == null || netView == null)
			return;

		View<CyNode> nodeView = netView.getNodeView(node);

		nodeView.setLockedValue(BasicVisualLexicon.NODE_TRANSPARENCY, 200);
		nodeView.setLockedValue(BasicVisualLexicon.NODE_BORDER_TRANSPARENCY, Util.node_border_opacity);
		nodeView.setLockedValue(BasicVisualLexicon.NODE_PAINT, Color.WHITE);
		nodeView.setLockedValue(BasicVisualLexicon.NODE_FILL_COLOR, Color.WHITE);
		nodeView.setLockedValue(BasicVisualLexicon.NODE_LABEL_COLOR, Color.GRAY);
		nodeView.setLockedValue(BasicVisualLexicon.NODE_LABEL_FONT_SIZE, Util.node_label_font_size);
		nodeView.setLockedValue(BasicVisualLexicon.NODE_SELECTED_PAINT, new Color(131, 131, 131, 70));
		nodeView.setLockedValue(BasicVisualLexicon.NODE_BORDER_WIDTH, Util.node_border_width);
		nodeView.setLockedValue(BasicVisualLexicon.NODE_BORDER_PAINT, Util.NodeBorderColor);
		nodeView.setLockedValue(BasicVisualLexicon.NODE_SHAPE, NodeShapeVisualProperty.ROUND_RECTANGLE);

		// ######################### NODE_LABEL_POSITION ######################

		VisualLexicon lexicon = MainSingleNodeTask.lexicon;
		if (lexicon == null)
			lexicon = ProcessProteinLocationTask.lexicon;

		if (lexicon == null)
			return;

		// Try to get the label visual property by its ID
		VisualProperty<?> vp_label_position = lexicon.lookup(CyNode.class, Util.NODE_LABEL_POSITION);
		if (vp_label_position != null) {

			// If the property is supported by this rendering engine,
			// use the serialization string value to create the actual property value

			Object position = null;
			if (Util.isProtein_expansion_horizontal)
				position = vp_label_position.parseSerializableString("W,E,r,-10.00,0.00");
			else
				position = (ObjectPosition) vp_label_position.parseSerializableString("N,S,c,0.00,0.00");

			// If the parsed value is ok, apply it to the visual style
			// as default value or a visual mapping

			if (position != null)
				nodeView.setLockedValue(vp_label_position, position);

		}
		// ######################### NODE_LABEL_POSITION ######################

	}

	private static void updateOrCreateEdgesFromSettingsPanel(CyNetwork myNetwork, CyNode node, CyNetworkView netView,
			VisualStyle style, HandleFactory handleFactory, BendFactory bendFactory, VisualLexicon lexicon,
			Protein protein) {

		MainSingleNodeTask.isPlotDone = false;
		ProcessProteinLocationTask.isPlotDone = false;
		Util.stopUpdateViewer = false;

		View<CyNode> nodeView = netView.getNodeView(node);

		node_label_factor_size = myNetwork.getRow(node).get(Util.PROTEIN_SCALING_FACTOR_COLUMN_NAME,
				Double.class) == null ? 1.0
						: myNetwork.getRow(node).get(Util.PROTEIN_SCALING_FACTOR_COLUMN_NAME, Double.class);
		isProtein_expansion_horizontal = myNetwork.getRow(node).get(Util.HORIZONTAL_EXPANSION_COLUMN_NAME,
				Boolean.class) == null ? true
						: myNetwork.getRow(node).get(Util.HORIZONTAL_EXPANSION_COLUMN_NAME, Boolean.class);

		CyRow proteinA_node_row = myNetwork.getRow(node);
		Object length_other_protein_a = proteinA_node_row.getRaw(PROTEIN_LENGTH_A);
		Object length_other_protein_b = proteinA_node_row.getRaw(PROTEIN_LENGTH_B);

		if (length_other_protein_a == null) {
			if (length_other_protein_b == null)
				length_other_protein_a = 10;
			else
				length_other_protein_a = length_other_protein_b;
		}

		setProteinLength((float) ((Number) length_other_protein_a).doubleValue());

		MainSingleNodeTask.isPlotDone = addOrUpdateEdgesToNetwork(myNetwork, node, style, netView, nodeView,
				handleFactory, bendFactory, lexicon, getProteinLength(), protein, null, null);

		// Apply the change to the view
		style.apply(netView);
		netView.updateView();

	}

	/**
	 * Method responsible for getting all amino acid positions in a protein sequence
	 * 
	 * @param aa
	 * @param proteinSequence
	 * @return list with positions
	 */
	private static List<Integer> getAllAminoAcidPosInAProtein(char aa, String proteinSequence) {

		List<Integer> aaPos = new ArrayList<Integer>();
		int index = proteinSequence.indexOf(aa);
		while (index >= 0) {
			aaPos.add(index);
			index = proteinSequence.indexOf(aa, index + 1);
		}
		return aaPos;
	}

	/**
	 * Method responsible for setting residues style
	 * 
	 * @param myNetwork current network
	 * @param node      current node
	 * @param netView   current network view
	 * @param style     current style
	 */
	public static void setNodeResidues(CyNetwork myNetwork, CyNode node, CyNetworkView netView, VisualStyle style) {

		if (myNetwork == null || node == null || style == null || netView == null) {
			return;
		}

		final String node_name = myNetwork.getDefaultNodeTable().getRow(node.getSUID()).getRaw(CyNetwork.NAME)
				.toString();

		Protein protein = getProtein(myNetwork, node_name);

		if (protein == null || protein.reactionSites == null)
			return;

		View<CyNode> sourceNodeView = netView.getNodeView(node);

		double x_or_y_Pos_source = 0;
		double xl_pos_source = 0;
		double center_position_source_node = (Util.proteinLength * Util.node_label_factor_size) / 2.0;

		double initial_position_source_node = 0;
		if (Util.isProtein_expansion_horizontal) {
			initial_position_source_node = Util.getXPositionOf(sourceNodeView);
		} else {
			initial_position_source_node = Util.getYPositionOf(sourceNodeView);
		}

		int countMonolink = 0;

		for (Residue residue : protein.reactionSites) {

			final String node_name_added_by_app = "RESIDUE" + countMonolink + " [Source: " + node_name + " ("
					+ residue.position + ")]";

			CyNode _node = Util.getNode(myNetwork, node_name_added_by_app);
			if (_node == null) {// Add a new node if does not exist

				_node = myNetwork.addNode();
				myNetwork.getRow(_node).set(CyNetwork.NAME, node_name_added_by_app);
				setResidueStyle(netView, _node, sourceNodeView, residue, xl_pos_source, center_position_source_node,
						x_or_y_Pos_source, initial_position_source_node);
			} else
				plotResidueNodes(netView, _node, sourceNodeView, residue, center_position_source_node,
						initial_position_source_node);

			countMonolink++;
		}
	}

	/**
	 * Set style to node
	 * 
	 * @param myNetwork current network
	 * @param node      current node
	 * @param netView   current network view
	 */
	public static void setNodeStyles(CyNetwork myNetwork, CyNode node, CyNetworkView netView, VisualStyle style) {

		updateNodeStyle(myNetwork, node, netView);

		View<CyNode> nodeView = netView.getNodeView(node);

		if (isProtein_expansion_horizontal) {
			nodeView.setLockedValue(BasicVisualLexicon.NODE_WIDTH,
					((Number) getProteinLengthScalingFactor()).doubleValue());
			nodeView.setLockedValue(BasicVisualLexicon.NODE_HEIGHT, 15d);
		} else {
			nodeView.setLockedValue(BasicVisualLexicon.NODE_WIDTH, 15d);
			nodeView.setLockedValue(BasicVisualLexicon.NODE_HEIGHT,
					((Number) getProteinLengthScalingFactor()).doubleValue());
		}

		if (myNetwork.getRow(node).get(Util.PROTEIN_SCALING_FACTOR_COLUMN_NAME, Double.class) != null)
			myNetwork.getRow(node).set(PROTEIN_SCALING_FACTOR_COLUMN_NAME, node_label_factor_size);
		if (myNetwork.getRow(node).get(Util.HORIZONTAL_EXPANSION_COLUMN_NAME, Boolean.class) != null)
			myNetwork.getRow(node).set(HORIZONTAL_EXPANSION_COLUMN_NAME, isProtein_expansion_horizontal);

		if (showResidues) {
			setNodeResidues(myNetwork, node, netView, style);
		}
	}

	/**
	 * Set all domains to a node
	 * 
	 * @param taskMonitor
	 * @param myProtein
	 * @param nodeView
	 * @param myNetwork
	 * @param node
	 * @param vgFactory
	 * @param lexicon
	 */
	public static void setNodeDomainColors(final TaskMonitor taskMonitor, Protein myProtein, View<CyNode> nodeView,
			CyNetwork myNetwork, CyNode node, @SuppressWarnings("rawtypes") CyCustomGraphics2Factory vgFactory,
			VisualLexicon lexicon) {
		// ######################### NODE_COLOR_LINEAR_GRADIENT ######################
		boolean hasDomain = false;
		StringBuilder sb_domains = new StringBuilder();
		@SuppressWarnings("unchecked")
		VisualProperty<CyCustomGraphics2<?>> vp_node_linear_gradient = (VisualProperty<CyCustomGraphics2<?>>) lexicon
				.lookup(CyNode.class, "NODE_CUSTOMGRAPHICS_1");

		if (myProtein == null)
			return;

		float protein_length = Util.getProteinLength();
		if (myProtein.sequence != null)
			protein_length = myProtein.sequence.length();

		if (vp_node_linear_gradient != null) {

			Map<String, Object> chartProps = new HashMap<String, Object>();
			List<java.awt.Color> colors = new ArrayList<java.awt.Color>();
			List<Float> values = new ArrayList<Float>();
			values.add(0.0f);
			colors.add(new Color(255, 255, 255, 100));

			Collections.sort(myProtein.domains);

			int countDomain = 1;
			for (ProteinDomain domain : myProtein.domains) {

				int startId = domain.startId;
				int endId = domain.endId;

				if (startId > protein_length)
					continue;

				if (startId < 0)
					continue;

				if (endId > protein_length) {

					if (taskMonitor != null)
						taskMonitor.showMessage(TaskMonitor.Level.ERROR, "ERROR Domain: " + domain.name
								+ " - The position of the final residue is greater than the length of the protein.");
					endId = (int) protein_length;
				}

				float initial_range = ((float) startId / protein_length);
				float initial_range_white = initial_range - 0.0001f >= 0.0 ? initial_range - 0.0001f : initial_range;

				if (initial_range_white == 0) {
					values.add(initial_range_white);
					values.add(initial_range + 0.0001f);

				} else {
					values.add(initial_range_white);
					values.add(initial_range);
				}
				colors.add(new Color(255, 255, 255, 100));
				if (domain.color == null) {
					colors.add(Util.proteinDomainsColorMap.get(domain.name));
				} else {
					colors.add(domain.color);
				}

				float end_range = ((float) endId / protein_length);
				float end_range_white = end_range + 0.0001f <= 1.0 ? end_range + 0.0001f : end_range;

				if (end_range_white == 1.0) {
					values.add(end_range - 0.0001f);

				} else {
					values.add(end_range);
				}

				if (domain.color == null) {
					colors.add(Util.proteinDomainsColorMap.get(domain.name));
				} else {
					colors.add(domain.color);
				}
				values.add(end_range_white);
				colors.add(new Color(255, 255, 255, 100));

				Color legend_color = null;
				if (domain.color == null) {
					legend_color = Util.proteinDomainsColorMap.get(domain.name);
				} else {
					legend_color = domain.color;
				}

				sb_domains.append("<p style=\"color:rgb(" + legend_color.getRed() + ", " + legend_color.getGreen()
						+ ", " + legend_color.getBlue() + ");\">" + countDomain + ". <i>" + domain.name + " </i>["
						+ startId + " - " + endId + "]</p>");
				hasDomain = true;
				countDomain++;

			}
			values.add(1.0f);
			colors.add(new Color(255, 255, 255, 100));
			chartProps.put("cy_gradientFractions", values);
			chartProps.put("cy_gradientColors", colors);

			if (Util.isProtein_expansion_horizontal)
				chartProps.put("cy_angle", 0.0);
			else
				chartProps.put("cy_angle", 270.0);

			@SuppressWarnings("unchecked")
			CyCustomGraphics2<?> customGraphics = vgFactory.getInstance(chartProps);
			if (vp_node_linear_gradient != null)
				nodeView.setLockedValue(vp_node_linear_gradient, customGraphics);
		}

		if (hasDomain) {
			if (myProtein.domains.size() > 1)
				nodeView.setLockedValue(BasicVisualLexicon.NODE_TOOLTIP,
						"<html><p><b>Protein:</b></p><p>" + myProtein.gene + " [1 - " + (int) protein_length
								+ "]</p><br/><p><b>Domains:</i></p>" + sb_domains.toString() + "</html>");
			else
				nodeView.setLockedValue(BasicVisualLexicon.NODE_TOOLTIP,
						"<html><p><b>Protein:</b></p><p>" + myProtein.gene + " [1 - " + (int) protein_length
								+ "]</p><br/><p><b>Domain:</i></p>" + sb_domains.toString() + "</html>");

			String network_name = myNetwork.toString();
			if (Util.proteinsMap.containsKey(network_name)) {

				List<Protein> all_proteins = Util.proteinsMap.get(network_name);
				Optional<Protein> isPtnPresent = all_proteins.stream()
						.filter(value -> value.gene.equals(myProtein.gene)).findFirst();
				if (isPtnPresent.isPresent()) {
					Protein ptn = isPtnPresent.get();
					ptn.checksum = myProtein.checksum;
					ptn.domains = myProtein.domains;
					ptn.fullName = myProtein.fullName;
					ptn.gene = myProtein.gene;
					ptn.interLinks = myProtein.interLinks;
					ptn.intraLinks = myProtein.intraLinks;
					ptn.pdbIds = myProtein.pdbIds;
					ptn.proteinID = myProtein.proteinID;
					ptn.reactionSites = myProtein.reactionSites;
					ptn.sequence = myProtein.sequence;
				}

			} else {// Network does not exists

				List<Protein> proteins = new ArrayList<Protein>();
				proteins.add(myProtein);
				Util.proteinsMap.put(network_name, proteins);
			}

		} else
			nodeView.setLockedValue(BasicVisualLexicon.NODE_TOOLTIP, "<html><p><b>Protein:</b></p><p>" + myProtein.gene
					+ " [1 - " + (int) protein_length + "]</p></html>");
		// ############################### END ################################
	}

	/**
	 * Method responsible for restoring edge style when
	 * showInterlinks/showIntralinks is false
	 * 
	 * @param myNetwork     current network
	 * @param node          current node
	 * @param netView       current network view
	 * @param handleFactory handle factory
	 * @param bendFactory   bend factory
	 * @param style         current style
	 * @param lexicon       lexicon
	 * @param edge          current edge
	 * @param sourceNode    current source node
	 * @param targetNode    current target node
	 * @param edge_name     current edge name
	 * @param proteinLength length of the current protein
	 * @param IsIntraLink   check if there is only intralink
	 */
	public static void restoreEdgeStyle(CyNetwork myNetwork, CyNode node, CyNetworkView netView,
			HandleFactory handleFactory, BendFactory bendFactory, VisualStyle style, VisualLexicon lexicon, CyEdge edge,
			CyNode sourceNode, CyNode targetNode, String edge_name, float proteinLength, boolean IsIntraLink) {

		if (!edge_name.contains("[Source:")) {// original edges

			if (IsIntraLink) {
				View<CyEdge> currentEdgeView = netView.getEdgeView(edge);
				currentEdgeView.setLockedValue(BasicVisualLexicon.EDGE_VISIBLE, true);
			} else {
				View<CyEdge> currentEdgeView = netView.getEdgeView(edge);
				currentEdgeView.setLockedValue(BasicVisualLexicon.EDGE_VISIBLE, true);
			}
		} else { // created edges

			if (IsIntraLink) {
				View<CyEdge> currentEdgeView = netView.getEdgeView(edge);
				currentEdgeView.setLockedValue(BasicVisualLexicon.EDGE_VISIBLE, false);

			} else {
				View<CyEdge> currentEdgeView = netView.getEdgeView(edge);
				currentEdgeView.setLockedValue(BasicVisualLexicon.EDGE_VISIBLE, false);
			}
		}
	}

	/**
	 * Method responsible for restoring edges style
	 * 
	 * @param taskMonitor          task monitor
	 * @param myNetwork            current network
	 * @param cyApplicationManager main app manager
	 * @param netView              current network view
	 * @param handleFactory        handle factory
	 * @param bendFactory          bend factory
	 * @param current_node         current node
	 */
	public static void restoreEdgesStyle(final TaskMonitor taskMonitor, CyNetwork myNetwork,
			CyApplicationManager cyApplicationManager, CyNetworkView netView, HandleFactory handleFactory,
			BendFactory bendFactory, CyNode current_node) {

		taskMonitor.showMessage(TaskMonitor.Level.INFO, "Restoring edges...");

		boolean IsModified_source_node = false;
		boolean IsModified_target_node = false;
		boolean IsIntraLink = false;

		int total_edges = 0;
		int old_progress = 0;
		int summary_processed = 0;
		if (taskMonitor != null)
			total_edges = myNetwork.getAdjacentEdgeList(current_node, CyEdge.Type.ANY).size();

		for (CyEdge edge : myNetwork.getAdjacentEdgeIterable(current_node, CyEdge.Type.ANY)) {

			// Check if the edge was inserted by this app
			CyRow myCurrentRow = myNetwork.getDefaultEdgeTable().getRow(edge.getSUID());
			String edge_name = myCurrentRow.get(CyNetwork.NAME, String.class);

			CyNode sourceNode = myNetwork.getEdge(edge.getSUID()).getSource();
			CyNode targetNode = myNetwork.getEdge(edge.getSUID()).getTarget();

			if (sourceNode.getSUID() == targetNode.getSUID()) {
				IsIntraLink = true;
			} else {
				IsIntraLink = false;
			}
			IsModified_source_node = IsNodeModified(myNetwork, netView, sourceNode);
			IsModified_target_node = IsNodeModified(myNetwork, netView, targetNode);

			double comb_score = -1;
			if (myCurrentRow.getRaw(XL_COMB_SCORE) != null) {
				try {
					comb_score = Double.parseDouble(myCurrentRow.getRaw(XL_COMB_SCORE).toString());
					comb_score = -Math.log10(comb_score);
				} catch (Exception e) {
					comb_score = -1;
				}

			}
			View<CyEdge> currentEdgeView = netView.getEdgeView(edge);
			if (!edge_name.contains("[Source:")) {// original edges

				if (IsIntraLink) {

					if (comb_score == -1) {// There is no comb_score
						currentEdgeView.setLockedValue(BasicVisualLexicon.EDGE_VISIBLE, true);
					} else if (comb_score < combinedlink_threshold_score) {
						currentEdgeView.setLockedValue(BasicVisualLexicon.EDGE_VISIBLE, false);
					} else {
						currentEdgeView.setLockedValue(BasicVisualLexicon.EDGE_VISIBLE, true);
					}

				} else if (!IsModified_source_node && !IsModified_target_node) {

					if (comb_score == -1) {// There is no comb_score
						currentEdgeView.setLockedValue(BasicVisualLexicon.EDGE_VISIBLE, true);
					} else if (comb_score < combinedlink_threshold_score) {
						currentEdgeView.setLockedValue(BasicVisualLexicon.EDGE_VISIBLE, false);
					} else {
						currentEdgeView.setLockedValue(BasicVisualLexicon.EDGE_VISIBLE, true);
					}
				}
			} else { // created edges

				if (IsIntraLink || (!IsModified_source_node && !IsModified_target_node)) {
					currentEdgeView.setLockedValue(BasicVisualLexicon.EDGE_VISIBLE, false);
				}
			}

			summary_processed++;
			progressBar(summary_processed, old_progress, total_edges, "Restoring edges styles: ", taskMonitor, null);

		}

		String node_name = myNetwork.getDefaultNodeTable().getRow(current_node.getSUID()).getRaw(CyNetwork.NAME)
				.toString();
		Protein protein = Util.getProtein(myNetwork, node_name);

		if (protein.interLinks.size() > 0) {
			if (taskMonitor != null) {
				taskMonitor.showMessage(TaskMonitor.Level.INFO, "Setting styles on the inter link edges: 95%");
			}
			updateAllAssiciatedInterlinkNodes(myNetwork, cyApplicationManager, netView, handleFactory, bendFactory,
					current_node);
		}
		if (protein.intraLinks.size() > 0) {
			if (taskMonitor != null) {
				taskMonitor.showMessage(TaskMonitor.Level.INFO, "Setting styles on the intra link edges: 99%");
			}
			hideAllIntraLinks(myNetwork, netView, protein);
		}

		// ######################### UPDATE EDGES #########################
	}

	/**
	 * Method responsible for displaying progress bar
	 * 
	 * @param summary_processed current processed value
	 * @param old_progress      old progress value
	 * @param total_process     total number to be processed
	 * @param message           message to display
	 * @param taskMonitor       task monitor
	 */
	private static void progressBar(int summary_processed, int old_progress, int total_process, String message,
			TaskMonitor taskMonitor, JLabel textLabel_status_result) {

		int new_progress = (int) ((double) summary_processed / (total_process) * 100);
		if (new_progress > old_progress) {
			old_progress = new_progress;

			if (taskMonitor != null)
				taskMonitor.showMessage(TaskMonitor.Level.INFO, message + old_progress + "%");

			if (textLabel_status_result != null)
				textLabel_status_result.setText("Defining styles for cross-links: " + old_progress + "%");
		}
	}

	/**
	 * Method responsible for hiding all interlinks of a node
	 * 
	 * @param myNetwork current network
	 * @param cyNode    current node
	 * @param netView   current network view
	 */
	public static void hideAllInterLinks(CyNetwork myNetwork, CyNode cyNode, CyNetworkView netView) {

		for (CyEdge edge : myNetwork.getAdjacentEdgeIterable(cyNode, CyEdge.Type.ANY)) {

			View<CyEdge> newEdgeView = netView.getEdgeView(edge);
			while (newEdgeView == null) {
				netView.updateView();
				try {
					Thread.sleep(200);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
				newEdgeView = netView.getEdgeView(edge);
			}

			newEdgeView.setLockedValue(BasicVisualLexicon.EDGE_VISIBLE, false);
		}
	}

	/**
	 * Method responsible for removing all intralinks of a node when the layout is
	 * restored
	 * 
	 * @param myNetwork current network
	 * @param netView   current network view
	 */
	public static void hideAllIntraLinks(CyNetwork myNetwork, CyNetworkView netView, Protein protein) {

		CyNode current_node_source = null;
		CyNode current_node_target = null;
		CyEdge current_edge_intra = null;

		for (int countEdge = 0; countEdge < protein.intraLinks.size(); countEdge++) {

			final String egde_name_added_by_app = "Edge" + countEdge + " [Source: "
					+ protein.intraLinks.get(countEdge).protein_a + " (" + protein.intraLinks.get(countEdge).pos_site_a
					+ ")] [Target: " + protein.intraLinks.get(countEdge).protein_b + " ("
					+ protein.intraLinks.get(countEdge).pos_site_b + ")]";

			current_edge_intra = getEdge(myNetwork, egde_name_added_by_app, false);
			if (current_edge_intra != null) {
				View<CyEdge> currentEdgeView = netView.getEdgeView(current_edge_intra);
				currentEdgeView.setLockedValue(BasicVisualLexicon.EDGE_VISIBLE, false);
			}

			final String node_name_source = protein.intraLinks.get(countEdge).protein_a + " ["
					+ protein.intraLinks.get(countEdge).pos_site_a + " - "
					+ protein.intraLinks.get(countEdge).pos_site_b + "] - Source";

			current_node_source = Util.getNode(myNetwork, node_name_source);
			if (current_node_source != null) {
				View<CyNode> currentNodeView = netView.getNodeView(current_node_source);
				currentNodeView.setLockedValue(BasicVisualLexicon.NODE_VISIBLE, false);
			}

			final String node_name_target = protein.intraLinks.get(countEdge).protein_a + " ["
					+ protein.intraLinks.get(countEdge).pos_site_a + " - "
					+ protein.intraLinks.get(countEdge).pos_site_b + "] - Target";

			current_node_target = getNode(myNetwork, node_name_target);
			if (current_node_target != null) {
				View<CyNode> currentNodeView = netView.getNodeView(current_node_target);
				currentNodeView.setLockedValue(BasicVisualLexicon.NODE_VISIBLE, false);
			}
		}
	}

	/**
	 * Method responsible for updating table row header
	 * 
	 * @param number_lines     total number of lines
	 * @param mainTable        main table
	 * @param rowHeader        row header of the table
	 * @param tableScrollPanel scroll panel of the table
	 */
	public static void updateRowHeader(int number_lines, JTable mainTable, JList rowHeader,
			JScrollPane tableScrollPanel) {

		final String[] headers = new String[number_lines];
		for (int count = 0; count < number_lines; count++) {
			headers[count] = String.valueOf(count + 1);
		}

		ListModel lm = new AbstractListModel() {

			@Override
			public int getSize() {
				return headers.length;
			}

			@Override
			public Object getElementAt(int index) {
				return headers[index];
			}

		};

		rowHeader = new JList(lm);
		rowHeader.setFixedCellWidth(50);
		rowHeader.setFixedCellHeight(mainTable.getRowHeight());
		rowHeader.setCellRenderer(new JTableRowRenderer(mainTable));
		if (tableScrollPanel != null)
			tableScrollPanel.setRowHeaderView(rowHeader);
	}

	// Utility function
	public static <T> Predicate<T> distinctByKey(Function<? super T, Object> keyExtractor) {
		Map<Object, Boolean> map = new ConcurrentHashMap<>();
		return t -> map.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
	}

	/**
	 * Get all links from adjacent edges of a node (crosslinks_ab and crosslinks_ba
	 * columns are set as edge attribute)
	 * 
	 * @param node      current node
	 * @param myNetwork current network
	 * @return all intra and interlinks
	 */
	public static Tuple2 getAllLinksFromNode(CyNode node, CyNetwork myNetwork) {
		if (node == null || myNetwork == null) {
			return new Tuple2(new ArrayList<CrossLink>(), new ArrayList<CrossLink>());
		}

		Set<Tuple2> cross_links_with_score = new HashSet<Tuple2>();
		Set<String> cross_links_set = new HashSet<String>();

		for (CyEdge edge : myNetwork.getAdjacentEdgeIterable(node, CyEdge.Type.ANY)) {

			CyRow myCurrentRow = myNetwork.getRow(edge);
			if (myCurrentRow.getRaw(XL_PROTEIN_A_B) != null) {
				if (myCurrentRow.getRaw(XL_SCORE_AB) != null) {

					List<String> xls = Arrays.asList(myCurrentRow.getRaw(XL_PROTEIN_A_B).toString().split("#"));
					List<String> scores = Arrays.asList(myCurrentRow.getRaw(XL_SCORE_AB).toString().split("#"));
					for (int i = 0; i < xls.size(); i++) {
						cross_links_with_score.add(new Tuple2(xls.get(i), scores.get(i)));
					}

				} else {
					cross_links_set.addAll(Arrays.asList(myCurrentRow.getRaw(XL_PROTEIN_A_B).toString().split("#")));

				}
			}

			if (myCurrentRow.getRaw(XL_PROTEIN_B_A) != null) {
				if (myCurrentRow.getRaw(XL_SCORE_BA) != null) {

					List<String> xls = Arrays.asList(myCurrentRow.getRaw(XL_PROTEIN_B_A).toString().split("#"));
					List<String> scores = Arrays.asList(myCurrentRow.getRaw(XL_SCORE_BA).toString().split("#"));
					for (int i = 0; i < xls.size(); i++) {
						cross_links_with_score.add(new Tuple2(xls.get(i), scores.get(i)));
					}

				} else {
					cross_links_set.addAll(Arrays.asList(myCurrentRow.getRaw(XL_PROTEIN_B_A).toString().split("#")));
				}
			}

		}

		// ############ GET ALL EDGES THAT BELONG TO THE SELECTED NODE #############

		List<CrossLink> interLinks = new ArrayList<CrossLink>();
		List<CrossLink> intraLinks = new ArrayList<CrossLink>();
		final String selected_node_name = myNetwork.getDefaultNodeTable().getRow(node.getSUID()).getRaw(CyNetwork.NAME)
				.toString();

		if (cross_links_with_score.size() > 0) {

			// Get only links that belong to the selected node
			cross_links_with_score.removeIf(new Predicate<Tuple2>() {

				public boolean test(Tuple2 xl) {
					if (((String) xl.getFirst()).isBlank() || ((String) xl.getFirst()).isEmpty()
							|| ((String) xl.getFirst()).equals("0") || ((String) xl.getFirst()).equals("NA")
							|| ((String) xl.getSecond()).isBlank() || ((String) xl.getSecond()).isEmpty()
							|| ((String) xl.getSecond()).equals("0") || ((String) xl.getSecond()).equals("NA"))
						return true;
					String[] current_xl = ((String) xl.getFirst()).split(selected_node_name);
					return (current_xl.length == 1);
				}
			});

			for (Tuple2 xl : cross_links_with_score.stream().filter(distinctByKey(p -> p.getFirst()))
					.collect(Collectors.toList())) {

				try {
					String[] current_xl = splitLinks(((String) xl.getFirst()), selected_node_name);// xl.split("-");

					if (current_xl[0].equals(current_xl[2])) {// it's intralink

						int pos_a = Integer.parseInt(current_xl[1]);
						int pos_b = Integer.parseInt(current_xl[3]);
						if (pos_a > pos_b) {
							int tmp_ = pos_a;
							pos_a = pos_b;
							pos_b = tmp_;
						}
						intraLinks.add(new CrossLink(current_xl[0], current_xl[2], pos_a, pos_b,
								Double.parseDouble(((String) xl.getSecond()))));

					} else {// it's interlink

						interLinks.add(new CrossLink(current_xl[0], current_xl[2], Integer.parseInt(current_xl[1]),
								Integer.parseInt(current_xl[3]), Double.parseDouble(((String) xl.getSecond()))));

					}
				} catch (Exception e) {
				}
			}

		} else {

			// Get only links that belong to the selected node
			cross_links_set.removeIf(new Predicate<String>() {

				public boolean test(String xl) {
					if (xl.isBlank() || xl.isEmpty() || xl.equals("0") || xl.equals("NA"))
						return true;
					String[] current_xl = xl.split(selected_node_name);
					return (current_xl.length == 1);
				}
			});

			for (

			String xl : cross_links_set) {

				try {
					String[] current_xl = splitLinks(xl, selected_node_name);// xl.split("-");

					if (current_xl[0].equals(current_xl[2])) {// it's intralink

						int pos_a = Integer.parseInt(current_xl[1]);
						int pos_b = Integer.parseInt(current_xl[3]);
						if (pos_a > pos_b) {
							int tmp_ = pos_a;
							pos_a = pos_b;
							pos_b = tmp_;
						}
						intraLinks.add(new CrossLink(current_xl[0], current_xl[2], pos_a, pos_b));

					} else {// it's interlink

						interLinks.add(new CrossLink(current_xl[0], current_xl[2], Integer.parseInt(current_xl[1]),
								Integer.parseInt(current_xl[3])));

					}
				} catch (Exception e) {
				}
			}
		}

		// Remove duplicate values
		interLinks = new ArrayList<CrossLink>(new HashSet<CrossLink>(interLinks));
		intraLinks = new ArrayList<CrossLink>(new HashSet<CrossLink>(intraLinks));

		Collections.sort(interLinks);
		Collections.sort(intraLinks);

		// If both lists are empty, try to retrieve cross-links from NodeTable
		if (interLinks.size() == 0 && intraLinks.size() == 0) {
			Tuple2 inter_and_intralinks = getAllLinksFromAdjacentEdgesNode(node, myNetwork);
			return inter_and_intralinks;
		}

		return new Tuple2(interLinks, intraLinks);
	}

	/**
	 * Get all links from adjacent edges of a node
	 * 
	 * @param node      current node
	 * @param myNetwork current network
	 * @return all intra and interlinks
	 */
	public static Tuple2 getAllLinksFromAdjacentEdgesNode(CyNode node, CyNetwork myNetwork) {

		if (node == null || myNetwork == null) {
			return new Tuple2(new ArrayList<CrossLink>(), new ArrayList<CrossLink>());
		}
		Set<Long> nodeSuidList = new HashSet<Long>();
		nodeSuidList.add(node.getSUID());

		for (CyEdge edge : myNetwork.getAdjacentEdgeIterable(node, CyEdge.Type.ANY)) {
			CyNode sourceNode = myNetwork.getEdge(edge.getSUID()).getSource();
			CyNode targetNode = myNetwork.getEdge(edge.getSUID()).getTarget();

			if (sourceNode.getSUID() == node.getSUID())
				nodeSuidList.add(targetNode.getSUID());
			else
				nodeSuidList.add(sourceNode.getSUID());
		}

		Set<Tuple2> cross_links_with_score = new HashSet<Tuple2>();
		Set<String> cross_links_set = new HashSet<String>();

		for (Long nodeSUID : nodeSuidList) {
			CyNode currentNode = myNetwork.getNode(nodeSUID);

			CyRow myCurrentRow = myNetwork.getRow(currentNode);

			if (myCurrentRow.getRaw(XL_PROTEIN_A_B) != null) {
				if (myCurrentRow.getRaw(XL_SCORE_AB) != null) {

					List<String> xls = Arrays.asList(myCurrentRow.getRaw(XL_PROTEIN_A_B).toString().split("#"));
					List<String> scores = Arrays.asList(myCurrentRow.getRaw(XL_SCORE_AB).toString().split("#"));
					for (int i = 0; i < xls.size(); i++) {
						cross_links_with_score.add(new Tuple2(xls.get(i), scores.get(i)));
					}

				} else {
					cross_links_set.addAll(Arrays.asList(myCurrentRow.getRaw(XL_PROTEIN_A_B).toString().split("#")));

				}
			}
			if (myCurrentRow.getRaw(XL_PROTEIN_B_A) != null) {
				if (myCurrentRow.getRaw(XL_SCORE_BA) != null) {

					List<String> xls = Arrays.asList(myCurrentRow.getRaw(XL_PROTEIN_B_A).toString().split("#"));
					List<String> scores = Arrays.asList(myCurrentRow.getRaw(XL_SCORE_BA).toString().split("#"));
					for (int i = 0; i < xls.size(); i++) {
						cross_links_with_score.add(new Tuple2(xls.get(i), scores.get(i)));
					}

				} else {
					cross_links_set.addAll(Arrays.asList(myCurrentRow.getRaw(XL_PROTEIN_B_A).toString().split("#")));
				}
			}
		}

		// ############ GET ALL EDGES THAT BELONG TO THE SELECTED NODE #############

		List<CrossLink> interLinks = new ArrayList<CrossLink>();
		List<CrossLink> intraLinks = new ArrayList<CrossLink>();
		final String selected_node_name = myNetwork.getDefaultNodeTable().getRow(node.getSUID()).getRaw(CyNetwork.NAME)
				.toString();

		if (cross_links_with_score.size() > 0) {

			// Get only links that belong to the selected node
			cross_links_with_score.removeIf(new Predicate<Tuple2>() {

				public boolean test(Tuple2 xl) {
					if (((String) xl.getFirst()).isBlank() || ((String) xl.getFirst()).isEmpty()
							|| ((String) xl.getFirst()).equals("0") || ((String) xl.getFirst()).equals("NA")
							|| ((String) xl.getSecond()).isBlank() || ((String) xl.getSecond()).isEmpty()
							|| ((String) xl.getSecond()).equals("0") || ((String) xl.getSecond()).equals("NA"))
						return true;
					String[] current_xl = ((String) xl.getFirst()).split(selected_node_name);
					return (current_xl.length == 1);
				}
			});

			for (Tuple2 xl : cross_links_with_score.stream().filter(distinctByKey(p -> p.getFirst()))
					.collect(Collectors.toList())) {

				try {
					String[] current_xl = splitLinks(((String) xl.getFirst()), selected_node_name);// xl.split("-");

					if (current_xl[0].equals(current_xl[2])) {// it's intralink

						int pos_a = Integer.parseInt(current_xl[1]);
						int pos_b = Integer.parseInt(current_xl[3]);
						if (pos_a > pos_b) {
							int tmp_ = pos_a;
							pos_a = pos_b;
							pos_b = tmp_;
						}
						intraLinks.add(new CrossLink(current_xl[0], current_xl[2], pos_a, pos_b,
								Double.parseDouble(((String) xl.getSecond()))));

					} else {// it's interlink

						interLinks.add(new CrossLink(current_xl[0], current_xl[2], Integer.parseInt(current_xl[1]),
								Integer.parseInt(current_xl[3]), Double.parseDouble(((String) xl.getSecond()))));

					}
				} catch (Exception e) {
				}
			}

		} else {

			// Get only links that belong to the selected node
			cross_links_set.removeIf(new Predicate<String>() {

				public boolean test(String xl) {
					if (xl.isBlank() || xl.isEmpty() || xl.equals("0") || xl.equals("NA"))
						return true;
					String[] current_xl = xl.split(selected_node_name);
					return (current_xl.length == 1);
				}
			});

			for (

			String xl : cross_links_set) {

				try {
					String[] current_xl = splitLinks(xl, selected_node_name);// xl.split("-");

					if (current_xl[0].equals(current_xl[2])) {// it's intralink

						int pos_a = Integer.parseInt(current_xl[1]);
						int pos_b = Integer.parseInt(current_xl[3]);
						if (pos_a > pos_b) {
							int tmp_ = pos_a;
							pos_a = pos_b;
							pos_b = tmp_;
						}
						intraLinks.add(new CrossLink(current_xl[0], current_xl[2], pos_a, pos_b));

					} else {// it's interlink

						interLinks.add(new CrossLink(current_xl[0], current_xl[2], Integer.parseInt(current_xl[1]),
								Integer.parseInt(current_xl[3])));

					}
				} catch (Exception e) {
				}
			}
		}

// Remove duplicate values
		interLinks = new ArrayList<CrossLink>(new HashSet<CrossLink>(interLinks));
		intraLinks = new ArrayList<CrossLink>(new HashSet<CrossLink>(intraLinks));

		Collections.sort(interLinks);
		Collections.sort(intraLinks);

		return new Tuple2(interLinks, intraLinks);
	}

	private static String[] splitLinks(String xl, String selected_node_name) {
		String[] current_xl = xl.split("-");

		if (current_xl.length == 4) {// name-position-name-position
			return current_xl;
		} else {
			StringBuilder new_link = new StringBuilder();
			current_xl = xl.split(selected_node_name);
			if (current_xl.length == 3) {// intralink
				new_link.append(selected_node_name);
				new_link.append("#");
				new_link.append(current_xl[1].replace("-", ""));
				new_link.append("#");
				new_link.append(selected_node_name);
				new_link.append("#");
				new_link.append(current_xl[2].replace("-", ""));

				return new_link.toString().split("#");
			} else if (current_xl.length == 2) {// interlink

				if (current_xl[0].isBlank() || current_xl[0].isEmpty()) {// selected_node_name-position-name-position
					new_link.append(selected_node_name);
					new_link.append("#");
					String[] _xl = current_xl[1].split("-");
					if (_xl.length == 4) {// second node has no dashes
						new_link.append(_xl[1]);
						new_link.append("#");
						new_link.append(_xl[2]);
						new_link.append("#");
						new_link.append(_xl[3]);
						return new_link.toString().split("#");
					}
				} else {// name-position-selected_node_name-position
					String[] _xl = current_xl[0].substring(0, current_xl[0].length() - 1).split("-");
					String startPos = _xl[_xl.length - 1];
					_xl = Arrays.copyOf(_xl, _xl.length - 1);
					new_link.append(String.join("-", _xl));
					new_link.append("#");
					new_link.append(startPos);
					new_link.append("#");
					new_link.append(selected_node_name);
					new_link.append("#");
					new_link.append(current_xl[1].replace("-", ""));
					return new_link.toString().split("#");
				}

			}
		}

		return null;
	}

	/**
	 * Method responsible for getting the proteinID from Cytoscape Table
	 * 
	 * @param myCurrentRow current row
	 * @return the proteinID
	 */
	private static String getProteinID(CyRow myCurrentRow) {
		Object protein_a_name = myCurrentRow.getRaw(PROTEIN_A);
		Object protein_b_name = myCurrentRow.getRaw(PROTEIN_B);

		if (protein_a_name == null || protein_a_name.toString().isBlank() || protein_a_name.toString().isEmpty()) {
			if (protein_b_name == null || protein_b_name.toString().isBlank() || protein_b_name.toString().isEmpty())
				protein_a_name = 10;
			else
				protein_a_name = protein_b_name;
		}

		String ptnID = protein_a_name.toString();
		String[] cols = ptnID.split("\\|");

		if (cols.length == 3) { // Correct format: sp|XXX|YYYY or tr|XXX|YYY
			return cols[1];
		} else {
			return "";
		}
	}

	/**
	 * Get post-translational modifications from Uniprot
	 * 
	 * @param myCurrentRow current row of the table
	 * @param taskMonitor  domains
	 * @return
	 */
	public static ArrayList<PTM> getPTMs(CyRow myCurrentRow, TaskMonitor taskMonitor) {

		ArrayList<PTM> ptmsServer = new ArrayList<PTM>(0);
		String proteinID = getProteinID(myCurrentRow);
		if (!(proteinID.isBlank() || proteinID.isEmpty())) {
			ptmsServer = getPTMs(proteinID, taskMonitor);
			Collections.sort(ptmsServer);
		}
		return ptmsServer;
	}

	/**
	 * Get post-translational modifications from Uniprot
	 * 
	 * @param myCurrentRow current row of the table
	 * @param taskMonitor  domains
	 * @return
	 */
	private static ArrayList<PTM> getPTMs(String proteinID, TaskMonitor taskMonitor) {

		String description = "";
		int position = -1;

		ArrayList<PTM> ptmList = new ArrayList<PTM>();

		try {
			String _url = "https://www.uniprot.org/uniprot/" + proteinID + ".xml";
			final URL url = new URL(_url);
			final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("POST");
			connection.setDoOutput(true);
			connection.setReadTimeout(5000);
			connection.setConnectTimeout(5000);
			connection.connect();

			if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {

				// Get Response
				InputStream inputStream = connection.getErrorStream(); // first check for error.
				if (inputStream == null) {
					inputStream = connection.getInputStream();
				}
				BufferedReader rd = new BufferedReader(new InputStreamReader(inputStream));
				String line;
				StringBuilder response = new StringBuilder();
				int total_lines = connection.getContentLength();

				int old_progress = 0;
				int summary_processed = 0;
				while ((line = rd.readLine()) != null) {
					response.append(line);
					response.append('\r');

					summary_processed += line.toCharArray().length + 1;
					progressBar(summary_processed, old_progress, total_lines,
							"Downloading protein information from Uniprot: ", taskMonitor, null);

				}
				rd.close();
				String responseString = response.toString();

				if (responseString.startsWith("<!DOCTYPE html PUBLIC"))
					return new ArrayList<PTM>();

				// Use method to convert XML string content to XML Document object
				Document doc = convertStringToXMLDocument(responseString);

				if (doc == null)
					return new ArrayList<PTM>();

				// check if exists error
				NodeList xmlnodes = doc.getElementsByTagName("error");
				if (xmlnodes.getLength() > 0) {
					throw new Exception("P2Location ERROR: " + xmlnodes.item(0).getNodeValue());
				}

				taskMonitor.showMessage(TaskMonitor.Level.INFO, "Getting protein sequence...");
				xmlnodes = doc.getElementsByTagName("sequence");

				String ptnSequence = "";
				for (int i = 0; i < xmlnodes.getLength(); i++) {
					Node node = xmlnodes.item(i);
					if (node instanceof Element) {
						if (node.getAttributes().item(0).getNodeName().equals("checksum")) {
							Node nodeChild = node.getFirstChild();
							ptnSequence = nodeChild.getNodeValue();
							break;
						}
					}
				}

				taskMonitor.showMessage(TaskMonitor.Level.INFO, "Getting protein features...");
				xmlnodes = doc.getElementsByTagName("feature");
				if (xmlnodes.getLength() == 0) {
					throw new Exception("P2Location ERROR: There is no feature tag");
				}
				for (int i = 0; i < xmlnodes.getLength(); i++) {
					Node node = xmlnodes.item(i);
					if (node.getAttributes().getLength() < 3)
						continue;
					String featureType = node.getAttributes().item(2).getNodeValue();
					if (featureType.equals("modified residue") || featureType.equals("disulfide bond")) {
						description = node.getAttributes().item(0).getNodeValue().replaceAll(",", "_");
						Node child = node.getChildNodes().item(1).getChildNodes().item(1);
						position = Integer.parseInt(child.getAttributes().item(0).getNodeValue());
						PTM ptm = new PTM(description, ptnSequence.charAt(position - 1), position);
						ptmList.add(ptm);
					}
				}
				return ptmList;

			} else {
				return new ArrayList<PTM>();
			}

		} catch (Exception e) {
			System.out.println(e.getMessage());
			return new ArrayList<PTM>();
		}
	}

	/**
	 * Get protein domains in PFam database
	 * 
	 * @param myCurrentRow current row of the table
	 * @return all protein domains
	 */
	public static ArrayList<ProteinDomain> getProteinDomainsFromServer(CyRow myCurrentRow, TaskMonitor taskMonitor) {

		// ############ GET PROTEIN DOMAINS #################
		ArrayList<ProteinDomain> proteinDomainsServer = new ArrayList<ProteinDomain>(0);

		String proteinID = getProteinID(myCurrentRow);
		if (!(proteinID.isBlank() || proteinID.isEmpty())) {
			proteinDomainsServer = getProteinDomains(proteinID, taskMonitor);
			Collections.sort(proteinDomainsServer);
		}
		// ############################### END ################################

		return proteinDomainsServer;
	}

	/**
	 * Get protein domains from Server
	 * 
	 * @param proteinID protein ID
	 * @return list with protein domains
	 */
	private static ArrayList<ProteinDomain> getProteinDomains(String proteinID, TaskMonitor taskMonitor) {
		if (isProteinDomainPfam)
			return getProteinDomainsFromPfam(proteinID, taskMonitor);
		else
			return getProteinDomainsFromSupfam(proteinID, taskMonitor);
	}

	/**
	 * Get PDB file from SwissModel server
	 * 
	 * @param _url link to download pdb file
	 * @return pdb file name
	 */
	public static String[] getPDBfileFromSwissModelServer(String _url, TaskMonitor taskMonitor) {

		try {
			final URL url = new URL(_url);
			final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("GET");
			connection.setRequestProperty("Accept", "text/html");
			connection.setRequestProperty("Accept-Language", "en-US");
			connection.setRequestProperty("Connection", "close");
			connection.setDoOutput(true);
			connection.setReadTimeout(1000);
			connection.setConnectTimeout(1000);
			connection.connect();

			if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {

				// Get Response
				InputStream inputStream = connection.getErrorStream(); // first check for error.
				if (inputStream == null) {
					inputStream = connection.getInputStream();
				}
				BufferedReader rd = new BufferedReader(new InputStreamReader(inputStream));
				String line;

				StringBuilder response = new StringBuilder();
				int total_lines = connection.getContentLength();

				int old_progress = 0;
				int summary_processed = 0;

				while ((line = rd.readLine()) != null) {
					response.append(line);
					response.append('\r');

					summary_processed += line.toCharArray().length + 1;
					progressBar(summary_processed, old_progress, total_lines, "Downloading PDB file from server: ",
							taskMonitor, null);

				}
				rd.close();
				return new String[] { "PDB", response.toString() };

			} else {

				taskMonitor.showMessage(TaskMonitor.Level.WARN, "There is no PDB file.");
				return new String[] { "" };
			}

		} catch (Exception e) {
			taskMonitor.showMessage(TaskMonitor.Level.WARN, e.getMessage());
			return new String[] { "" };
		}
	}

	/**
	 * Get PDB file from RCSB server
	 * 
	 * @param pdbID protein id
	 * @return pdb file name
	 */
	public static String[] getPDBorCIFfileFromServer(String pdbID, TaskMonitor taskMonitor) {

		try {
			String _url = "https://files.rcsb.org/view/" + pdbID + ".pdb";
			final URL url = new URL(_url);
			final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("POST");
			connection.setDoOutput(true);
			connection.setReadTimeout(1000);
			connection.setConnectTimeout(1000);
			connection.connect();

			if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {

				// Get Response
				InputStream inputStream = connection.getErrorStream(); // first check for error.
				if (inputStream == null) {
					inputStream = connection.getInputStream();
				}
				BufferedReader rd = new BufferedReader(new InputStreamReader(inputStream));
				String line;

				StringBuilder response = new StringBuilder();
				int total_lines = connection.getContentLength();

				int old_progress = 0;
				int summary_processed = 0;

				while ((line = rd.readLine()) != null) {
					response.append(line);
					response.append('\r');

					summary_processed += line.toCharArray().length + 1;
					progressBar(summary_processed, old_progress, total_lines, "Downloading PDB file from server: ",
							taskMonitor, null);
				}
				rd.close();
				return new String[] { "PDB", response.toString() };

			} else if (connection.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND) {

				taskMonitor.showMessage(TaskMonitor.Level.WARN,
						"There is no PDB for this ID: " + pdbID + ". Trying to retrieve CIF file...");

				return new String[] { "CIF", getCiFfileFromServer(pdbID, taskMonitor) };

			} else {

				taskMonitor.showMessage(TaskMonitor.Level.WARN, "There is no PDB for this ID: " + pdbID + ".");
				return new String[] { "" };
			}

		} catch (Exception e) {
			taskMonitor.showMessage(TaskMonitor.Level.WARN, e.getMessage());
			return new String[] { "" };
		}
	}

	/**
	 * Get CIF file from RCSB server
	 * 
	 * @param pdbID protein id
	 * @return cif file name
	 */
	public static String getCiFfileFromServer(String pdbID, TaskMonitor taskMonitor) {

		try {
			String _url = "https://files.rcsb.org/view/" + pdbID + ".cif";
			final URL url = new URL(_url);
			final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("POST");
			connection.setDoOutput(true);
			connection.setReadTimeout(1000);
			connection.setConnectTimeout(1000);
			connection.connect();

			if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {

				// Get Response
				InputStream inputStream = connection.getErrorStream(); // first check for error.
				if (inputStream == null) {
					inputStream = connection.getInputStream();
				}
				BufferedReader rd = new BufferedReader(new InputStreamReader(inputStream));
				String line;
				StringBuilder response = new StringBuilder();
				int total_lines = connection.getContentLength();

				int old_progress = 0;
				int summary_processed = 0;
				while ((line = rd.readLine()) != null) {
					response.append(line);
					response.append('\r');

					summary_processed += line.toCharArray().length + 1;
					progressBar(summary_processed, old_progress, total_lines, "Downloading CIF file from server: ",
							taskMonitor, null);
				}
				rd.close();
				return response.toString();

			} else {

				taskMonitor.showMessage(TaskMonitor.Level.WARN, "There is no CIF for this ID: " + pdbID + ".");
				return "";
			}

		} catch (Exception e) {
			taskMonitor.showMessage(TaskMonitor.Level.WARN, e.getMessage());
			return "";
		}
	}

	/**
	 * Method responsible for getting fasta file from RCSB PDB server
	 * 
	 * @param pdbID pbd identifier
	 * @return list of fasta files
	 */
	public static List<Fasta> getProteinSequenceFromPDBServer(String pdbID, TaskMonitor taskMonitor) {

		List<Fasta> fastaList = new ArrayList<Fasta>();
		try {
			String _url = "https://www.rcsb.org/fasta/entry/" + pdbID + "/download";
			final URL url = new URL(_url);
			final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("GET");
			connection.setRequestProperty("Accept", "text/html");
			connection.setRequestProperty("Accept-Language", "en-US");
			connection.setRequestProperty("Connection", "close");
			connection.setDoOutput(true);
			connection.setReadTimeout(1000);
			connection.setConnectTimeout(1000);
			connection.connect();

			StringBuilder response = new StringBuilder();
			if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {

				// Get Response
				InputStream inputStream = connection.getErrorStream(); // first check for error.
				if (inputStream == null) {
					inputStream = connection.getInputStream();
				}
				BufferedReader rd = new BufferedReader(new InputStreamReader(inputStream));
				String line;

				while ((line = rd.readLine()) != null) {
					response.append(line);
					response.append('\r');
				}
				rd.close();

			} else {
				return new ArrayList<Fasta>();
			}

			String[] cols = response.toString().split("\r");
			for (int i = 0; i < cols.length - 1; i += 2) {
				fastaList.add(new Fasta(cols[i], cols[i + 1]));
			}

		} catch (Exception e) {
			taskMonitor.showMessage(TaskMonitor.Level.ERROR,
					"ERROR: Download fasta file from RCSB PDB server:" + e.getMessage());
			System.out.println(e.getMessage());
			return new ArrayList<Fasta>();
		}
		return fastaList;
	}

	public static String getPDBidOrURLFromSwissModel(String swissID, String checksum, TaskMonitor taskMonitor) {

		try {
			String _url = "https://swissmodel.expasy.org/repository/uniprot/" + swissID + "?csm=" + checksum;
			final URL url = new URL(_url);
			final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("GET");
			connection.setRequestProperty("Accept", "text/html");
			connection.setRequestProperty("Accept-Language", "en-US");
			connection.setRequestProperty("Connection", "close");
			connection.setDoOutput(true);
			connection.setReadTimeout(1000);
			connection.setConnectTimeout(1000);
			connection.connect();
			if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {

				// Get Response
				InputStream inputStream = connection.getErrorStream(); // first check for error.
				if (inputStream == null) {
					inputStream = connection.getInputStream();
				}
				BufferedReader rd = new BufferedReader(new InputStreamReader(inputStream));
				String line;
				StringBuilder response = new StringBuilder();
				while ((line = rd.readLine()) != null) {
					response.append(line);
					response.append('\r');
				}
				rd.close();
				String responseString = response.toString();

				// Try to find out second RCSB link. Example:
				// href="https://www.rcsb.org/structure/6lgk"

				int firstRCSBIndex = responseString.indexOf("href=\"https://www.rcsb.org/structure/");

				if (firstRCSBIndex != -1) {
					int secondRCSBIndex = responseString.indexOf("href=\"https://www.rcsb.org/structure/",
							firstRCSBIndex + 1);

					if (secondRCSBIndex != -1) {

						// 37 = link length
						int quoteIndex = responseString.indexOf("\"", secondRCSBIndex + 37);
						String _pdbID = responseString.substring(secondRCSBIndex + 37, quoteIndex);

						return _pdbID;

					} else {
						String pdbID = retrievePDBIDfromSwissModelServer(responseString);
						return "https://swissmodel.expasy.org/repository/" + pdbID + ".pdb";
					}

				} else { // There is no RCSB link. Try to find out pdb file on Swiss-Model website

					String pdbID = retrievePDBIDfromSwissModelServer(responseString);
					return "https://swissmodel.expasy.org/repository/" + pdbID + ".pdb";
				}

			} else {
				taskMonitor.showMessage(TaskMonitor.Level.WARN, "Error retrieving PDB file from SWISS-MODEL server.");
				return "";
			}

		} catch (Exception e) {

			taskMonitor.showMessage(TaskMonitor.Level.WARN, e.getMessage());
		}
		return "";
	}

	/**
	 * Retrieve PDB id from SwissModel server
	 * 
	 * @param responseString whole html
	 * @return pdb id
	 */
	private static String retrievePDBIDfromSwissModelServer(String responseString) {

		int endPdbIndex = responseString.indexOf(".pdb");

		if (endPdbIndex != -1) {

			int startPdbIndex = responseString.indexOf("/repository/", endPdbIndex - 50);
			String pdbFile = responseString.substring(startPdbIndex + 12, endPdbIndex);

			return pdbFile;
		} else {
			return "";
		}

	}

	/**
	 * Get PDB IDs from Uniprot
	 * 
	 * @param myCurrentRow current row
	 * @return pdb ids
	 */
	public static Protein getPDBidFromUniprot(CyRow myCurrentRow, TaskMonitor taskMonitor) {

		String proteinID = getProteinID(myCurrentRow);
		if (proteinID.isBlank() || proteinID.isEmpty()) {
			return new Protein();
		}

		try {
			String _url = "https://www.uniprot.org/uniprot/" + proteinID + ".xml";
			final URL url = new URL(_url);
			final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("POST");
			connection.setDoOutput(true);
			connection.setReadTimeout(5000);
			connection.setConnectTimeout(5000);
			connection.connect();

			if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {

				// Get Response
				InputStream inputStream = connection.getErrorStream(); // first check for error.
				if (inputStream == null) {
					inputStream = connection.getInputStream();
				}
				BufferedReader rd = new BufferedReader(new InputStreamReader(inputStream));
				String line;
				StringBuilder response = new StringBuilder();
				int total_lines = connection.getContentLength();

				int old_progress = 0;
				int summary_processed = 0;
				while ((line = rd.readLine()) != null) {
					response.append(line);
					response.append('\r');

					summary_processed += line.toCharArray().length + 1;
					progressBar(summary_processed, old_progress, total_lines,
							"Downloading protein information from Uniprot: ", taskMonitor, null);
				}
				rd.close();
				String responseString = response.toString();

				if (responseString.startsWith("<!DOCTYPE html PUBLIC"))
					return new Protein();

				// Use method to convert XML string content to XML Document object
				Document doc = convertStringToXMLDocument(responseString);

				if (doc == null)
					return new Protein();

				// check if exists error
				NodeList xmlnodes = doc.getElementsByTagName("error");
				if (xmlnodes.getLength() > 0) {
					throw new Exception("P2Location ERROR: " + xmlnodes.item(0).getNodeValue());
				}

				taskMonitor.showMessage(TaskMonitor.Level.INFO, "Getting protein description...");
				xmlnodes = doc.getElementsByTagName("recommendedName");
				if (xmlnodes.getLength() == 0) {
					xmlnodes = doc.getElementsByTagName("submittedName");
				}
				NodeList nodes = xmlnodes.item(0).getChildNodes();

				String fullName = "";
				for (int i = 0; i < nodes.getLength(); i++) {
					Node node = nodes.item(i);
					if (node instanceof Element) {
						if (node.getNodeName().equals("fullName")) {
							Node nodeChild = node.getFirstChild();
							fullName = nodeChild.getNodeValue();
							break;
						}
					}
				}

				taskMonitor.showMessage(TaskMonitor.Level.INFO, "Getting gene name...");
				xmlnodes = doc.getElementsByTagName("gene");
				nodes = xmlnodes.item(0).getChildNodes();

				String geneName = "";
				for (int i = 0; i < nodes.getLength(); i++) {
					Node node = nodes.item(i);
					if (node instanceof Element) {
						if (node.getNodeName().equals("name")) {
							Node nodeChild = node.getFirstChild();
							geneName = nodeChild.getNodeValue();
							break;
						}
					}
				}

				taskMonitor.showMessage(TaskMonitor.Level.INFO, "Getting PDB IDs...");
				xmlnodes = doc.getElementsByTagName("dbReference");

				boolean containsPDBtags = false;
				List<PDB> pdbs = new ArrayList<PDB>();
				for (int i = 0; i < xmlnodes.getLength(); i++) {
					Node nNode = xmlnodes.item(i);
					String pdbType = nNode.getAttributes().item(1).getNodeValue();
					if (pdbType.equals("PDB")) {
						if (nNode.hasChildNodes()) {
							String entry = nNode.getAttributes().item(0).getNodeValue();
							String resolution = "0.00";
							String[] chain_positions = null;
							if (nNode.getChildNodes().item(3).getAttributes().item(0).getNodeValue()
									.equals("resolution")) {
								resolution = nNode.getChildNodes().item(3).getAttributes().item(1).getNodeValue();
								chain_positions = nNode.getChildNodes().item(5).getAttributes().item(1).getNodeValue()
										.split("=");
							} else if (nNode.getChildNodes().item(3).getAttributes().item(0).getNodeValue()
									.equals("chains"))
								chain_positions = nNode.getChildNodes().item(3).getAttributes().item(1).getNodeValue()
										.split("=");
							String chain = chain_positions[0];
							String positions = chain_positions[1];
							PDB pdb = new PDB(entry, resolution, chain, positions);
							pdbs.add(pdb);
							containsPDBtags = true;
						}
					} else if (pdbType.equals("SMR") && !containsPDBtags) {
						String entry = nNode.getAttributes().item(0).getNodeValue();
						PDB pdb = new PDB(entry, "SMR", "", "");
						pdbs.add(pdb);
					} else {
						continue;
					}

				}

				taskMonitor.showMessage(TaskMonitor.Level.INFO, "Getting protein sequence...");
				xmlnodes = doc.getElementsByTagName("sequence");

				String ptnSequence = "";
				String checksum = "";
				for (int i = 0; i < xmlnodes.getLength(); i++) {
					Node node = xmlnodes.item(i);
					if (node instanceof Element) {
						if (node.getAttributes().item(0).getNodeName().equals("checksum")) {
							checksum = node.getAttributes().item(0).getNodeValue();
							Node nodeChild = node.getFirstChild();
							ptnSequence = nodeChild.getNodeValue();
							break;
						}
					}
				}

				Collections.sort(pdbs);
				Protein ptn = new Protein(proteinID, geneName, fullName, ptnSequence, checksum, pdbs, null);
				return ptn;

			} else {
				return new Protein();
			}

		} catch (Exception e) {
			System.out.println(e.getMessage());
			return new Protein();
		}
	}

	/**
	 * Method responsible for getting protein sequence from Uniprot
	 * 
	 * @param myCurrentRow current row
	 * @return protein sequence
	 */
	public static String getProteinSequenceFromUniprot(CyRow myCurrentRow) {

		String proteinID = getProteinID(myCurrentRow);
		if (proteinID.isBlank() || proteinID.isEmpty()) {
			return "";
		}

		try {
			String _url = "https://www.uniprot.org/uniprot/" + proteinID + ".fasta";
			final URL url = new URL(_url);
			final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("GET");
			connection.setRequestProperty("Accept", "text/html");
			connection.setRequestProperty("Accept-Language", "en-US");
			connection.setRequestProperty("Connection", "close");
			connection.setDoOutput(true);
			connection.connect();

			if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {

				// Get Response
				InputStream inputStream = connection.getErrorStream(); // first check for error.
				if (inputStream == null) {
					inputStream = connection.getInputStream();
				}
				BufferedReader rd = new BufferedReader(new InputStreamReader(inputStream));
				String line;
				StringBuilder response = new StringBuilder();
				while ((line = rd.readLine()) != null) {
					if (line.startsWith(">"))
						continue;
					response.append(line);
				}
				rd.close();
				return response.toString();

			} else {
				return "";
			}

		} catch (Exception e) {
			System.out.println(e.getMessage());
			return "";
		}
	}

	/**
	 * Connect to Supfam and get domains
	 * 
	 * @param proteinID protein id
	 * @return all protein domains
	 */
	private static ArrayList<ProteinDomain> getProteinDomainsFromSupfam(String proteinID, TaskMonitor taskMonitor) {

		try {
			String _url = "https://supfam.org/SUPERFAMILY/cgi-bin/das/up/features?segment=" + proteinID;
			final URL url = new URL(_url);
			final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("GET");
			connection.setRequestProperty("Accept", "text/html");
			connection.setRequestProperty("Accept-Language", "en-US");
			connection.setRequestProperty("Connection", "close");
			connection.setDoOutput(true);
			connection.setReadTimeout(1000);
			connection.setConnectTimeout(1000);
			connection.connect();

			if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {

				// Get Response
				InputStream inputStream = connection.getErrorStream(); // first check for error.
				if (inputStream == null) {
					inputStream = connection.getInputStream();
				}
				BufferedReader rd = new BufferedReader(new InputStreamReader(inputStream));
				String line;
				StringBuilder response = new StringBuilder();
				while ((line = rd.readLine()) != null) {
					response.append(line);
					response.append('\r');
				}
				rd.close();
				String responseString = response.toString();

				if (responseString.startsWith("<!DOCTYPE html PUBLIC"))
					return new ArrayList<ProteinDomain>();

				responseString = responseString
						.replace("<!DOCTYPE DASGFF SYSTEM \"http://www.biodas.org/dtd/dasgff.dtd\">", "");

				// Use method to convert XML string content to XML Document object
				Document doc = convertStringToXMLDocument(responseString);

				if (doc == null)
					return new ArrayList<ProteinDomain>();

				NodeList xmlnodes = doc.getElementsByTagName("FEATURE");

				taskMonitor.showMessage(TaskMonitor.Level.INFO, "Getting domains...");
				ArrayList<ProteinDomain> proteinDomainList = new ArrayList<ProteinDomain>();
				for (int i = 0; i < xmlnodes.getLength(); i++) {
					String domain = "";
					String method = "";
					int startId = -1;
					int endId = -1;
					String eValue = "";
					boolean all_fields_filled_in = false;
					for (int j = 0; j < xmlnodes.item(i).getChildNodes().getLength(); j++) {
						Node nNode = xmlnodes.item(i).getChildNodes().item(j);
						if (nNode.getNodeType() == Node.ELEMENT_NODE) {
							if (nNode.getNodeName().equals("TYPE")) {
								domain = nNode.getAttributes().item(1).getNodeValue();
							} else if (nNode.getNodeName().equals("METHOD")) {
								method = nNode.getAttributes().item(0).getNodeValue();
							} else if (nNode.getNodeName().equals("START")) {
								startId = Integer.parseInt(nNode.getChildNodes().item(0).getNodeValue());
							} else if (nNode.getNodeName().equals("END")) {
								endId = Integer.parseInt(nNode.getChildNodes().item(0).getNodeValue());
							} else if (nNode.getNodeName().equals("SCORE")) {
								eValue = nNode.getChildNodes().item(0).getNodeValue();
								all_fields_filled_in = true;
							}

							if ((!method.isBlank() || !method.isEmpty()) && !method.equals("SUPERFAMILY_"))
								break;
							else if (method.equals("SUPERFAMILY_") && all_fields_filled_in)
								break;
						}
					}
					if (startId > -1 && endId > -1) {
						domain = domain.replace(",", "_");
						proteinDomainList.add(new ProteinDomain(domain, startId, endId, eValue));
					}
				}
				taskMonitor.showMessage(TaskMonitor.Level.INFO, "Done!");
				return proteinDomainList;
			} else {
				taskMonitor.showMessage(TaskMonitor.Level.WARN, "There is no Supfam file for the ID: " + proteinID);
				return new ArrayList<ProteinDomain>();
			}

		} catch (Exception e) {

			taskMonitor.showMessage(TaskMonitor.Level.WARN, e.getMessage());
			return new ArrayList<ProteinDomain>();
		}
	}

	/**
	 * Connect to PFam and get domains
	 * 
	 * @param proteinID protein id
	 * @return all protein domains
	 */
	private static ArrayList<ProteinDomain> getProteinDomainsFromPfam(String proteinID, TaskMonitor taskMonitor) {

		try {
			String _url = "https://pfam.xfam.org/protein?entry=" + proteinID + "&output=xml";
			final URL url = new URL(_url);
			final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("POST");
			connection.setDoOutput(true);
			connection.setReadTimeout(1000);
			connection.setConnectTimeout(1000);
			connection.connect();
			if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {

				// Get Response
				InputStream inputStream = connection.getErrorStream(); // first check for error.
				if (inputStream == null) {
					inputStream = connection.getInputStream();
				}

				int total_lines = connection.getContentLength();

				BufferedReader rd = new BufferedReader(new InputStreamReader(inputStream));
				String line;

				int old_progress = 0;
				int summary_processed = 0;
				StringBuilder response = new StringBuilder();
				while ((line = rd.readLine()) != null) {
					response.append(line);
					response.append('\r');

					summary_processed += line.toCharArray().length + 1;
					progressBar(summary_processed, old_progress, total_lines, "Downloading Pfam file from server: ",
							taskMonitor, null);
				}
				rd.close();
				String responseString = response.toString();

				if (responseString.startsWith("<!DOCTYPE html PUBLIC"))
					return new ArrayList<ProteinDomain>();

				// Use method to convert XML string content to XML Document object
				Document doc = convertStringToXMLDocument(responseString);

				if (doc == null)
					return new ArrayList<ProteinDomain>();

				// check if exists error
				NodeList xmlnodes = doc.getElementsByTagName("error");
				if (xmlnodes.getLength() > 0) {
					throw new Exception("P2Location ERROR: " + xmlnodes.item(0).getNodeValue());
				}

				xmlnodes = doc.getElementsByTagName("matches");

				taskMonitor.showMessage(TaskMonitor.Level.INFO, "Getting domains...");

				ArrayList<ProteinDomain> proteinDomainList = new ArrayList<ProteinDomain>();
				for (int i = 0; i < xmlnodes.getLength(); i++) {
					for (int j = 0; j < xmlnodes.item(i).getChildNodes().getLength(); j++) {
						Node nNode = xmlnodes.item(i).getChildNodes().item(j);
						if (nNode.getNodeType() == Node.ELEMENT_NODE) {
							// get attributes names and values
							String domain = nNode.getAttributes().item(1).getNodeValue();
							domain = domain.replace(",", "_");

							int startId = Integer
									.parseInt(nNode.getChildNodes().item(1).getAttributes().item(7).getNodeValue());
							int endId = Integer
									.parseInt(nNode.getChildNodes().item(1).getAttributes().item(3).getNodeValue());
							String eValue = nNode.getChildNodes().item(1).getAttributes().item(4).getNodeValue();
							proteinDomainList.add(new ProteinDomain(domain, startId, endId, eValue));
						}
					}
				}
				return proteinDomainList;
			} else {
				taskMonitor.showMessage(TaskMonitor.Level.WARN, "There is no Pfam file for the ID: " + proteinID);
				return new ArrayList<ProteinDomain>();
			}

		} catch (Exception e) {
			taskMonitor.showMessage(TaskMonitor.Level.WARN, e.getMessage());
			return new ArrayList<ProteinDomain>();
		}
	}

	/**
	 * Convert Subpfam / Pfam object to XML
	 * 
	 * @param xmlString return from webservice
	 * @return the document object
	 */
	private static Document convertStringToXMLDocument(String xmlString) {
		// Parser that produces DOM object trees from XML content
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

		// API to obtain DOM Document instance
		DocumentBuilder builder = null;
		try {
			// Create DocumentBuilder with default configuration
			builder = factory.newDocumentBuilder();

			// Parse the content to Document object
			Document doc = builder.parse(new InputSource(new StringReader(xmlString)));

			return doc;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Returns X position of a node
	 * 
	 * @param nodeView current node view
	 * @return position
	 */
	public static double getXPositionOf(View<CyNode> nodeView) {
		return nodeView.getVisualProperty(BasicVisualLexicon.NODE_X_LOCATION);
	}

	/**
	 * Returns Y position of a node
	 * 
	 * @param nodeView current node view
	 * @return position
	 */
	public static double getYPositionOf(View<CyNode> nodeView) {
		return nodeView.getVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION);
	}

	/**
	 * Check if the operating system is Windows
	 * 
	 * @return true if the operating system is Windows.
	 */
	public static boolean isWindows() {
		return (OS.indexOf("win") >= 0);
	}

	/**
	 * Check if the operating system is Linux
	 * 
	 * @return true if the operating system is Linux.
	 */
	public static boolean isUnix() {
		return (OS.indexOf("nix") >= 0 || OS.indexOf("nux") >= 0 || OS.indexOf("aix") > 0);
	}

	/**
	 * Check if the operating system is MacOS
	 * 
	 * @return true if the operating system is MacOS.
	 */
	public static boolean isMac() {
		return (OS.indexOf("mac") >= 0);
	}

	/**
	 * Round a double value
	 * 
	 * @param value
	 * @param places
	 * @return rounded value
	 */
	public static double round(double value, int places) {
		if (places < 0)
			throw new IllegalArgumentException();

		long factor = (long) Math.pow(10, places);
		value = value * factor;
		long tmp = Math.round(value);
		return (double) tmp / factor;
	}
}