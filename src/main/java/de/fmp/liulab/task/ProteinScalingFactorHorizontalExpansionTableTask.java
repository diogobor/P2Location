package de.fmp.liulab.task;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;

import de.fmp.liulab.model.CrossLink;
import de.fmp.liulab.model.PTM;
import de.fmp.liulab.model.Protein;
import de.fmp.liulab.model.ProteinDomain;
import de.fmp.liulab.model.Residue;
import de.fmp.liulab.utils.Util;

/**
 * Class responsible for expanding the protein bar
 * 
 * @author diogobor
 *
 */
public class ProteinScalingFactorHorizontalExpansionTableTask extends AbstractTask {

	private CyNetwork myNetwork;
	private static boolean forcedHorizontalExpansion;

	public static boolean isProcessing;

	/**
	 * Constructor
	 * 
	 * @param myNetwork                 current network
	 * @param forcedHorizontalExpansion Force horizontal expansion param as true
	 */
	@SuppressWarnings("static-access")
	public ProteinScalingFactorHorizontalExpansionTableTask(CyNetwork myNetwork, boolean forcedHorizontalExpansion) {
		this.myNetwork = myNetwork;
		this.forcedHorizontalExpansion = forcedHorizontalExpansion;
	}

	/**
	 * Check if 'crosslinks_ab' or 'crosslinks_ba' columns has been set as 'Edge
	 * Attribute' at import time
	 * 
	 * @throws IOException
	 */
	private void checkCrosslinksColumns() throws IOException {

		CyEdge one_edge = Util.getEdge(myNetwork, "interacts with", true);
		if (one_edge != null) {

			CyRow edge_row = myNetwork.getRow(one_edge);
			Object crosslinks_ab = edge_row.getRaw(Util.XL_PROTEIN_A_B);
			Object crosslinks_ba = edge_row.getRaw(Util.XL_PROTEIN_B_A);

			if (crosslinks_ab == null && crosslinks_ba == null) {

				throw new IOException(
						"There is no information in column 'crosslinks_ab' or 'crosslinks_ba' or they have been set as 'Source/Target Node Attribute'.\nPlease set these columns as 'Edge Attribute' at import time.");

			}
		}
	}

	/**
	 * Default method
	 */
	@Override
	public void run(TaskMonitor taskMonitor) throws IOException {

		if (myNetwork == null)
			return;

		// Check if crosslink_ab and cross_link_ba are set on Edge Table
		checkCrosslinksColumns();

		if (isProcessing)
			return;

		isProcessing = true;// It indicates that there is a process here

		taskMonitor.setTitle("P2Location - Adding extra columns to the tables");

		updateNodeTable(taskMonitor, myNetwork.getDefaultNodeTable(), myNetwork.toString());

		ProcessProteinLocationTask.epochs = 1;
		Util.updateProteins(taskMonitor, myNetwork, null, true, true);

		isProcessing = false;
	}

	/**
	 * Method responsible for updating node table
	 * 
	 * @param taskMonitor task monitor
	 * @param nodeTable   current node table
	 */
	public static void updateNodeTable(TaskMonitor taskMonitor, CyTable nodeTable, String networkName) {

		// ###### LENGTH_PROTEIN_A
		if (nodeTable.getColumn(Util.PROTEIN_LENGTH_A) == null) {
			try {
				nodeTable.createColumn(Util.PROTEIN_LENGTH_A, Integer.class, false);
			} catch (Exception e) {
			}

		}

		// ###### LENGTH_PROTEIN_B
		if (nodeTable.getColumn(Util.PROTEIN_LENGTH_B) == null) {
			try {
				nodeTable.createColumn(Util.PROTEIN_LENGTH_B, Integer.class, false);
			} catch (Exception e) {
			}

		}

		// ###### PROTEIN_A
		if (nodeTable.getColumn(Util.PROTEIN_A) == null) {
			try {
				nodeTable.createColumn(Util.PROTEIN_A, String.class, false);
			} catch (Exception e) {
			}

		}

		// ###### PROTEIN_B
		if (nodeTable.getColumn(Util.PROTEIN_B) == null) {
			try {
				nodeTable.createColumn(Util.PROTEIN_B, String.class, false);
			} catch (Exception e) {
			}

		}

		// ###### PROTEIN SCALING FACTOR #######
		if (nodeTable.getColumn(Util.PROTEIN_SCALING_FACTOR_COLUMN_NAME) == null) {
			try {
				nodeTable.createColumn(Util.PROTEIN_SCALING_FACTOR_COLUMN_NAME, Double.class, false);

				for (CyRow row : nodeTable.getAllRows()) {
					row.set(Util.PROTEIN_SCALING_FACTOR_COLUMN_NAME, 1.0d);
				}

			} catch (IllegalArgumentException e) {
				try {
					for (CyRow row : nodeTable.getAllRows()) {
						if (row.get(Util.PROTEIN_SCALING_FACTOR_COLUMN_NAME, Double.class) == null)
							row.set(Util.PROTEIN_SCALING_FACTOR_COLUMN_NAME, 1.0d);
					}
				} catch (Exception e2) {
					return;
				}
			} catch (Exception e) {
			}

		} else { // The column exists, but it's necessary to check the cells
			try {
				for (CyRow row : nodeTable.getAllRows()) {
					if (row.get(Util.PROTEIN_SCALING_FACTOR_COLUMN_NAME, Double.class) == null)
						row.set(Util.PROTEIN_SCALING_FACTOR_COLUMN_NAME, 1.0d);
				}
			} catch (Exception e) {
			}

		}

		// ###### HORIZONTAL OR VERTICAL EXPANSION #######
		if (nodeTable.getColumn(Util.HORIZONTAL_EXPANSION_COLUMN_NAME) == null) {
			try {
				nodeTable.createColumn(Util.HORIZONTAL_EXPANSION_COLUMN_NAME, Boolean.class, false);

				for (CyRow row : nodeTable.getAllRows()) {
					row.set(Util.HORIZONTAL_EXPANSION_COLUMN_NAME, true);
				}

			} catch (IllegalArgumentException e) {
				try {
					for (CyRow row : nodeTable.getAllRows()) {
						if (forcedHorizontalExpansion
								|| row.get(Util.HORIZONTAL_EXPANSION_COLUMN_NAME, Boolean.class) == null)
							row.set(Util.HORIZONTAL_EXPANSION_COLUMN_NAME, true);
					}
				} catch (Exception e2) {
					return;
				}
			} catch (Exception e) {
			}

		} else { // The column exists, but it's necessary to check the cells
			try {
				for (CyRow row : nodeTable.getAllRows()) {
					if (forcedHorizontalExpansion
							|| row.get(Util.HORIZONTAL_EXPANSION_COLUMN_NAME, Boolean.class) == null)
						row.set(Util.HORIZONTAL_EXPANSION_COLUMN_NAME, true);
				}
			} catch (Exception e) {
			}

		}

		// ######## PROTEIN SEQUENCE ########
		if (nodeTable.getColumn(Util.PROTEIN_SEQUENCE_COLUMN) == null) {
			try {
				nodeTable.createColumn(Util.PROTEIN_SEQUENCE_COLUMN, String.class, false);

				for (CyRow row : nodeTable.getAllRows()) {
					row.set(Util.PROTEIN_SEQUENCE_COLUMN, "");
				}

			} catch (IllegalArgumentException e) {
				try {
					for (CyRow row : nodeTable.getAllRows()) {
						if (row.get(Util.PROTEIN_SEQUENCE_COLUMN, String.class) == null)
							row.set(Util.PROTEIN_SEQUENCE_COLUMN, "");
					}
				} catch (Exception e2) {
				}
			} catch (Exception e) {
			}

		} else { // The column exists, but it's necessary to check the cells
			try {

				// Check if proteinDomainsMap has been initialized
				boolean proteinsMapOK = true;
				if (Util.proteinsMap == null)
					proteinsMapOK = false;

				// Initialize protein domain colors map if LoadProteinDomainTask has not been
				// initialized
				Util.init_availableProteinDomainColorsMap();

				for (CyRow row : nodeTable.getAllRows()) {
					if (row.get(Util.PROTEIN_SEQUENCE_COLUMN, String.class) == null)
						row.set(Util.PROTEIN_SEQUENCE_COLUMN, "");
					else {
						String nodeName = row.get(CyNetwork.NAME, String.class);
						String sequence = row.get(Util.PROTEIN_SEQUENCE_COLUMN, String.class);

						if (!(sequence.isBlank() || sequence.isEmpty()) && proteinsMapOK) {

							updateProteinMapWithSequence(nodeName, sequence, networkName, taskMonitor);
						}
					}
				}
			} catch (Exception e) {
			}
		}

		// ######## PROTEIN NAME ########
		if (nodeTable.getColumn(Util.PROTEIN_NAME_COLUMN) == null) {
			try {
				nodeTable.createColumn(Util.PROTEIN_NAME_COLUMN, String.class, false);

				for (CyRow row : nodeTable.getAllRows()) {
					row.set(Util.PROTEIN_NAME_COLUMN, "");
				}

			} catch (IllegalArgumentException e) {
				try {
					for (CyRow row : nodeTable.getAllRows()) {
						if (row.get(Util.PROTEIN_NAME_COLUMN, String.class) == null)
							row.set(Util.PROTEIN_NAME_COLUMN, "");
					}
				} catch (Exception e2) {
				}
			} catch (Exception e) {
			}

		} else { // The column exists, but it's necessary to check the cells
			try {

				// Check if proteinDomainsMap has been initialized
				boolean proteinsMapOK = true;
				if (Util.proteinsMap == null)
					proteinsMapOK = false;

				// Initialize protein domain colors map if LoadProteinDomainTask has not been
				// initialized
				Util.init_availableProteinDomainColorsMap();

				for (CyRow row : nodeTable.getAllRows()) {
					if (row.get(Util.PROTEIN_NAME_COLUMN, String.class) == null)
						row.set(Util.PROTEIN_NAME_COLUMN, "");
					else {
						String nodeName = row.get(CyNetwork.NAME, String.class);
						String fullName = row.get(Util.PROTEIN_NAME_COLUMN, String.class);

						if (!(fullName.isBlank() || fullName.isEmpty()) && proteinsMapOK) {

							updateProteinMapWithName(nodeName, fullName, networkName, taskMonitor);
						}
					}
				}
			} catch (Exception e) {
			}
		}

		// ######## SUBCELLULAR LOCATION ########
		if (nodeTable.getColumn(Util.SUBCELLULAR_LOCATION_COLUMN) == null) {
			try {
				nodeTable.createColumn(Util.SUBCELLULAR_LOCATION_COLUMN, String.class, false);

				for (CyRow row : nodeTable.getAllRows()) {
					row.set(Util.SUBCELLULAR_LOCATION_COLUMN, "");
				}

			} catch (IllegalArgumentException e) {
				try {
					for (CyRow row : nodeTable.getAllRows()) {
						if (row.get(Util.SUBCELLULAR_LOCATION_COLUMN, String.class) == null)
							row.set(Util.SUBCELLULAR_LOCATION_COLUMN, "");
					}
				} catch (Exception e2) {
				}
			} catch (Exception e) {
			}

		} else { // The column exists, but it's necessary to check the cells
			try {

				// Check if proteinDomainsMap has been initialized
				boolean proteinsMapOK = true;
				if (Util.proteinsMap == null)
					proteinsMapOK = false;

				// Initialize protein domain colors map if LoadProteinDomainTask has not been
				// initialized
				Util.init_availableProteinDomainColorsMap();

				for (CyRow row : nodeTable.getAllRows()) {
					if (row.get(Util.SUBCELLULAR_LOCATION_COLUMN, String.class) == null)
						row.set(Util.SUBCELLULAR_LOCATION_COLUMN, "");
					else {
						String nodeName = row.get(CyNetwork.NAME, String.class);
						String location = row.get(Util.SUBCELLULAR_LOCATION_COLUMN, String.class);

						if (!(location.isBlank() || location.isEmpty()) && proteinsMapOK) {

							updateProteinMapWithSubcellularLocation(nodeName, location, networkName, taskMonitor);
						}
					}
				}
			} catch (Exception e) {
			}
		}

		// ######## PROTEIN DOMAINS ########
		if (nodeTable.getColumn(Util.PROTEIN_DOMAIN_COLUMN) == null) {
			try {
				nodeTable.createListColumn(Util.PROTEIN_DOMAIN_COLUMN, String.class, false);

				for (CyRow row : nodeTable.getAllRows()) {
					row.set(Util.PROTEIN_DOMAIN_COLUMN, new ArrayList<String>());
				}

			} catch (IllegalArgumentException e) {
				try {
					for (CyRow row : nodeTable.getAllRows()) {
						if (row.get(Util.PROTEIN_DOMAIN_COLUMN, List.class) == null)
							row.set(Util.PROTEIN_DOMAIN_COLUMN, new ArrayList<String>());
					}
				} catch (Exception e2) {
				}
			} catch (Exception e) {
			}

		} else { // The column exists, but it's necessary to check the cells
			try {

				// Check if proteinDomainsMap has been initialized
				boolean proteinDomainsMapOK = true;
				if (Util.proteinsMap == null)
					proteinDomainsMapOK = false;

				// Initialize protein domain colors map if LoadProteinDomainTask has not been
				// initialized
				Util.init_availableProteinDomainColorsMap();

				for (CyRow row : nodeTable.getAllRows()) {
					if (row.get(Util.PROTEIN_DOMAIN_COLUMN, List.class) == null)
						row.set(Util.PROTEIN_DOMAIN_COLUMN, new ArrayList<String>());
					else {
						String nodeName = row.get(CyNetwork.NAME, String.class);
						@SuppressWarnings("unchecked")
						List<String> domains = row.get(Util.PROTEIN_DOMAIN_COLUMN, List.class);

						if (domains != null && domains.size() > 0 && proteinDomainsMapOK) {
							updateProteinDomainsMap(nodeName, domains, false, false, networkName, taskMonitor);
						}
					}
				}
			} catch (Exception e) {
			}
		}

		// ######## PREDICTED PROTEIN DOMAINS ########
		if (nodeTable.getColumn(Util.PREDICTED_PROTEIN_DOMAIN_COLUMN) == null) {
			try {
				nodeTable.createListColumn(Util.PREDICTED_PROTEIN_DOMAIN_COLUMN, String.class, false);

				for (CyRow row : nodeTable.getAllRows()) {
					row.set(Util.PREDICTED_PROTEIN_DOMAIN_COLUMN, new ArrayList<String>());
				}

			} catch (IllegalArgumentException e) {
				try {
					for (CyRow row : nodeTable.getAllRows()) {
						if (row.get(Util.PREDICTED_PROTEIN_DOMAIN_COLUMN, List.class) == null)
							row.set(Util.PREDICTED_PROTEIN_DOMAIN_COLUMN, new ArrayList<String>());
					}
				} catch (Exception e2) {
				}
			} catch (Exception e) {
			}

		} else { // The column exists, but it's necessary to check the cells
			try {

				// Check if proteinDomainsMap has been initialized
				boolean proteinDomainsMapOK = true;
				if (Util.proteinsMap == null)
					proteinDomainsMapOK = false;

				// Initialize protein domain colors map if LoadProteinDomainTask has not been
				// initialized
				Util.init_availableProteinDomainColorsMap();

				for (CyRow row : nodeTable.getAllRows()) {
					if (row.get(Util.PREDICTED_PROTEIN_DOMAIN_COLUMN, List.class) == null)
						row.set(Util.PREDICTED_PROTEIN_DOMAIN_COLUMN, new ArrayList<String>());
					else {
						String nodeName = row.get(CyNetwork.NAME, String.class);
						@SuppressWarnings("unchecked")
						List<String> domains = row.get(Util.PREDICTED_PROTEIN_DOMAIN_COLUMN, List.class);

						if (domains != null && domains.size() > 0 && proteinDomainsMapOK) {
							updateProteinDomainsMap(nodeName, domains, true, false, networkName, taskMonitor);
						}
					}
				}
			} catch (Exception e) {
			}
		}

		// ######## VALID PROTEIN DOMAINS ########
		if (nodeTable.getColumn(Util.VALID_DOMAINS_COLUMN) == null) {
			try {
				nodeTable.createListColumn(Util.VALID_DOMAINS_COLUMN, String.class, false);

				for (CyRow row : nodeTable.getAllRows()) {
					row.set(Util.VALID_DOMAINS_COLUMN, new ArrayList<String>());
				}

			} catch (IllegalArgumentException e) {
				try {
					for (CyRow row : nodeTable.getAllRows()) {
						if (row.get(Util.VALID_DOMAINS_COLUMN, List.class) == null)
							row.set(Util.VALID_DOMAINS_COLUMN, new ArrayList<String>());
					}
				} catch (Exception e2) {
				}
			} catch (Exception e) {
			}

		} else { // The column exists, but it's necessary to check the cells
			try {

				// Check if proteinDomainsMap has been initialized
				boolean proteinDomainsMapOK = true;
				if (Util.proteinsMap == null)
					proteinDomainsMapOK = false;

				// Initialize protein domain colors map if LoadProteinDomainTask has not been
				// initialized
				Util.init_availableProteinDomainColorsMap();

				for (CyRow row : nodeTable.getAllRows()) {
					if (row.get(Util.VALID_DOMAINS_COLUMN, List.class) == null)
						row.set(Util.VALID_DOMAINS_COLUMN, new ArrayList<String>());
					else {
						String nodeName = row.get(CyNetwork.NAME, String.class);
						@SuppressWarnings("unchecked")
						List<String> domains = row.get(Util.VALID_DOMAINS_COLUMN, List.class);

						if (domains != null && domains.size() > 0 && proteinDomainsMapOK) {
							updateProteinDomainsMap(nodeName, domains, true, true, networkName, taskMonitor);
						}
					}
				}
			} catch (Exception e) {
			}
		}

		// ######## CONFLICTED PROTEIN DOMAINS ########
		if (nodeTable.getColumn(Util.CONFLICTED_PROTEIN_DOMAINS_COLUMN) == null) {
			try {
				nodeTable.createColumn(Util.CONFLICTED_PROTEIN_DOMAINS_COLUMN, Boolean.class, false);

				for (CyRow row : nodeTable.getAllRows()) {
					row.set(Util.CONFLICTED_PROTEIN_DOMAINS_COLUMN, false);
				}

			} catch (IllegalArgumentException e) {
			} catch (Exception e) {
			}

		} else { // The column exists, but it's necessary to check the cells
			try {

				// Check if proteinDomainsMap has been initialized
				boolean proteinDomainsMapOK = true;
				if (Util.proteinsMap == null)
					proteinDomainsMapOK = false;

				// Initialize protein domain colors map if LoadProteinDomainTask has not been
				// initialized
				Util.init_availableProteinDomainColorsMap();

				for (CyRow row : nodeTable.getAllRows()) {
					if (row.get(Util.CONFLICTED_PROTEIN_DOMAINS_COLUMN, Boolean.class) == null)
						row.set(Util.CONFLICTED_PROTEIN_DOMAINS_COLUMN, false);
					else {

						Boolean conflictedDomain = row.get(Util.CONFLICTED_PROTEIN_DOMAINS_COLUMN, Boolean.class);
						if (conflictedDomain && proteinDomainsMapOK) {

							String nodeName = row.get(CyNetwork.NAME, String.class);
							updateConflictedDomains(taskMonitor, nodeName, networkName, conflictedDomain);
						}
					}
				}
			} catch (Exception e) {
			}
		}

		// ######## PROTEIN DOMAINS SCORES ########
		if (nodeTable.getColumn(Util.PROTEIN_DOMAINS_SCORES_COLUMN) == null) {
			try {
				nodeTable.createListColumn(Util.PROTEIN_DOMAINS_SCORES_COLUMN, String.class, false);

				for (CyRow row : nodeTable.getAllRows()) {
					row.set(Util.PROTEIN_DOMAINS_SCORES_COLUMN, new ArrayList<String>());
				}

			} catch (IllegalArgumentException e) {
				try {
					for (CyRow row : nodeTable.getAllRows()) {
						if (row.get(Util.PROTEIN_DOMAINS_SCORES_COLUMN, List.class) == null)
							row.set(Util.PROTEIN_DOMAINS_SCORES_COLUMN, new ArrayList<String>());
					}
				} catch (Exception e2) {
				}
			} catch (Exception e) {
			}

		} else { // The column exists, but it's necessary to check the cells
			try {

				// Check if proteinDomainsMap has been initialized
				boolean proteinDomainsMapOK = true;
				if (Util.proteinsMap == null)
					proteinDomainsMapOK = false;

				for (CyRow row : nodeTable.getAllRows()) {
					if (row.get(Util.PROTEIN_DOMAINS_SCORES_COLUMN, List.class) == null)
						row.set(Util.PROTEIN_DOMAINS_SCORES_COLUMN, new ArrayList<String>());
					else {
						String nodeName = row.get(CyNetwork.NAME, String.class);
						@SuppressWarnings("unchecked")
						List<String> scores = row.get(Util.PROTEIN_DOMAINS_SCORES_COLUMN, List.class);

						if (scores != null && scores.size() > 0 && proteinDomainsMapOK) {
							updateProteinDomainsScores(nodeName, networkName, scores, taskMonitor);
						}
					}
				}
			} catch (Exception e) {
			}
		}

		// ######## VALID PROTEINS ########
		if (nodeTable.getColumn(Util.VALID_PROTEINS_COLUMN) == null) {
			try {
				nodeTable.createColumn(Util.VALID_PROTEINS_COLUMN, Boolean.class, false);

				for (CyRow row : nodeTable.getAllRows()) {
					row.set(Util.VALID_PROTEINS_COLUMN, true);
				}

			} catch (IllegalArgumentException e) {
			} catch (Exception e) {
			}

		} else { // The column exists, but it's necessary to check the cells
			try {

				// Check if proteinDomainsMap has been initialized
				boolean proteinDomainsMapOK = true;
				if (Util.proteinsMap == null)
					proteinDomainsMapOK = false;

				// Initialize protein domain colors map if LoadProteinDomainTask has not been
				// initialized
				Util.init_availableProteinDomainColorsMap();

				for (CyRow row : nodeTable.getAllRows()) {
					if (row.get(Util.VALID_PROTEINS_COLUMN, Boolean.class) == null)
						row.set(Util.VALID_PROTEINS_COLUMN, true);
					else {

						Boolean validDomain = row.get(Util.VALID_PROTEINS_COLUMN, Boolean.class);
						if (validDomain && proteinDomainsMapOK) {

							String nodeName = row.get(CyNetwork.NAME, String.class);
							updateValidProtein(taskMonitor, nodeName, networkName, validDomain);
						}
					}
				}
			} catch (Exception e) {
			}
		}

		// ######## CONFLICTED PREDICTED RESIDUES ########
		if (nodeTable.getColumn(Util.CONFLICTED_PREDICTED_RESIDUES_COLUMN) == null) {
			try {
				nodeTable.createListColumn(Util.CONFLICTED_PREDICTED_RESIDUES_COLUMN, String.class, false);

				for (CyRow row : nodeTable.getAllRows()) {
					row.set(Util.CONFLICTED_PREDICTED_RESIDUES_COLUMN, new ArrayList<String>());
				}

			} catch (IllegalArgumentException e) {
				try {
					for (CyRow row : nodeTable.getAllRows()) {
						if (row.get(Util.CONFLICTED_PREDICTED_RESIDUES_COLUMN, List.class) == null)
							row.set(Util.CONFLICTED_PREDICTED_RESIDUES_COLUMN, new ArrayList<String>());
					}
				} catch (Exception e2) {
				}
			} catch (Exception e) {
			}

		} else { // The column exists, but it's necessary to check the cells
			try {

				// Check if proteinDomainsMap has been initialized
				boolean proteinDomainsMapOK = true;
				if (Util.proteinsMap == null)
					proteinDomainsMapOK = false;

				// Initialize protein domain colors map if LoadProteinDomainTask has not been
				// initialized
				Util.init_availableProteinDomainColorsMap();

				for (CyRow row : nodeTable.getAllRows()) {
					if (row.get(Util.CONFLICTED_PREDICTED_RESIDUES_COLUMN, List.class) == null)
						row.set(Util.CONFLICTED_PREDICTED_RESIDUES_COLUMN, new ArrayList<String>());
					else {
						String nodeName = row.get(CyNetwork.NAME, String.class);
						@SuppressWarnings("unchecked")
						List<String> conflictedResidues = row.get(Util.CONFLICTED_PREDICTED_RESIDUES_COLUMN,
								List.class);

						if (conflictedResidues != null && conflictedResidues.size() > 0 && proteinDomainsMapOK) {

							updateConflictedResidues(nodeName, conflictedResidues, networkName, taskMonitor);
						}
					}
				}
			} catch (Exception e) {
			}
		}

		// ####### POST-TRANSLATIONAL MODIFICATIONS #########
		if (nodeTable.getColumn(Util.PTM_COLUMN) == null) {
			try {
				nodeTable.createColumn(Util.PTM_COLUMN, String.class, false);

				for (CyRow row : nodeTable.getAllRows()) {
					row.set(Util.PTM_COLUMN, "");
				}

			} catch (IllegalArgumentException e) {
				try {
					for (CyRow row : nodeTable.getAllRows()) {
						if (row.get(Util.PTM_COLUMN, String.class) == null)
							row.set(Util.PTM_COLUMN, "");
					}
				} catch (Exception e2) {
				}
			} catch (Exception e) {
			}

		} else { // The column exists, but it's necessary to check the cells
			try {

				// Check if ptmsMap has been initialized
				boolean ptmsMapOK = true;
				if (Util.ptmsMap == null)
					ptmsMapOK = false;

				for (CyRow row : nodeTable.getAllRows()) {
					if (row.get(Util.PTM_COLUMN, String.class) == null)
						row.set(Util.PTM_COLUMN, "");
					else {
						String nodeName = row.get(CyNetwork.NAME, String.class);
						String ptms = row.get(Util.PTM_COLUMN, String.class);

						if (!(ptms.isBlank() || ptms.isEmpty()) && ptmsMapOK) {

							updatePTMsMap(nodeName, networkName, ptms, taskMonitor, nodeTable);
						}
					}
				}
			} catch (Exception e) {
			}
		}

		// ####### MONOLINKS #########
		if (nodeTable.getColumn(Util.MONOLINK_COLUMN) == null) {
			try {
				nodeTable.createColumn(Util.MONOLINK_COLUMN, String.class, false);

				for (CyRow row : nodeTable.getAllRows()) {
					row.set(Util.MONOLINK_COLUMN, "");
				}

			} catch (IllegalArgumentException e) {
				try {
					for (CyRow row : nodeTable.getAllRows()) {
						if (row.get(Util.MONOLINK_COLUMN, String.class) == null)
							row.set(Util.MONOLINK_COLUMN, "");
					}
				} catch (Exception e2) {
				}
			} catch (Exception e) {
			}

		} else { // The column exists, but it's necessary to check the cells
			try {

				// Check if monolinksMap has been initialized
				boolean monolinksMapOK = true;
				if (Util.monolinksMap == null)
					monolinksMapOK = false;

				for (CyRow row : nodeTable.getAllRows()) {
					if (row.get(Util.MONOLINK_COLUMN, String.class) == null)
						row.set(Util.MONOLINK_COLUMN, "");
					else {
						String nodeName = row.get(CyNetwork.NAME, String.class);
						String monolinks = row.get(Util.MONOLINK_COLUMN, String.class);

						if (!(monolinks.isBlank() || monolinks.isEmpty()) && monolinksMapOK) {

							updateMonolinksMap(nodeName, monolinks, taskMonitor, nodeTable);
						}
					}
				}
			} catch (Exception e) {
			}
		}
	}

	/**
	 * Method responsible for updating protein map with sequence
	 * 
	 * @param nodeName        nome name
	 * @param proteinSequence protein sequence
	 * @param taskMonitor     task monitor
	 */
	private static void updateProteinMapWithSequence(String nodeName, String proteinSequence, String networkName,
			TaskMonitor taskMonitor) {

		if (!(proteinSequence.isBlank() || proteinSequence.isEmpty())) {

			// Update proteinsMap
			List<Protein> proteinList = Util.proteinsMap.get(networkName);
			Optional<Protein> isPtnPresent = proteinList.stream().filter(value -> value.gene.equals(nodeName))
					.findFirst();

			Protein _myProtein;
			if (isPtnPresent.isPresent()) {
				_myProtein = isPtnPresent.get();
				_myProtein.sequence = proteinSequence;
				if (_myProtein.reactionSites == null || _myProtein.reactionSites.size() == 0)
					ProcessProteinLocationTask.addReactionSites(_myProtein, true);
			} else {
				_myProtein = new Protein(nodeName, nodeName, proteinSequence);
				ProcessProteinLocationTask.addReactionSites(_myProtein, true);
				proteinList.add(_myProtein);
			}
		}
	}

	/**
	 * Method responsible for updating protein map with name
	 * 
	 * @param nodeName    nome name
	 * @param proteinName protein name
	 * @param taskMonitor task monitor
	 */
	private static void updateProteinMapWithName(String nodeName, String proteinName, String networkName,
			TaskMonitor taskMonitor) {

		if (!(proteinName.isBlank() || proteinName.isEmpty())) {

			// Update proteinsMap
			List<Protein> proteinList = Util.proteinsMap.get(networkName);
			Optional<Protein> isPtnPresent = proteinList.stream().filter(value -> value.gene.equals(nodeName))
					.findFirst();

			Protein _myProtein;
			if (isPtnPresent.isPresent()) {
				_myProtein = isPtnPresent.get();
				_myProtein.fullName = proteinName;
				if (_myProtein.reactionSites == null || _myProtein.reactionSites.size() == 0)
					ProcessProteinLocationTask.addReactionSites(_myProtein, true);
			} else {
				_myProtein = new Protein(nodeName, nodeName);
				_myProtein.fullName = proteinName;
				ProcessProteinLocationTask.addReactionSites(_myProtein, true);
				proteinList.add(_myProtein);
			}
		}
	}

	/**
	 * Method responsible for updating protein map with sequence
	 * 
	 * @param nodeName    nome name
	 * @param location    protein sequence
	 * @param taskMonitor task monitor
	 */
	private static void updateProteinMapWithSubcellularLocation(String nodeName, String location, String networkName,
			TaskMonitor taskMonitor) {

		if (!(location.isBlank() || location.isEmpty())) {

			// Update proteinsMap
			List<Protein> proteinList = Util.proteinsMap.get(networkName);
			Optional<Protein> isPtnPresent = proteinList.stream().filter(value -> value.gene.equals(nodeName))
					.findFirst();

			Protein _myProtein;
			if (isPtnPresent.isPresent()) {
				_myProtein = isPtnPresent.get();
				_myProtein.location = location;
				ProcessProteinLocationTask.addReactionSites(_myProtein, true);
			} else {
				_myProtein = new Protein(nodeName, nodeName, "");
				_myProtein.location = location;
				ProcessProteinLocationTask.addReactionSites(_myProtein, true);
				proteinList.add(_myProtein);
			}
		}
	}

	/**
	 * Method responsible for updating conflicted protein domains based on table
	 * information
	 * 
	 * @param taskMonitor      task monitor
	 * @param nodeName         current node name
	 * @param conflictedDomain isConflicted Domain or not
	 */
	private static void updateConflictedDomains(TaskMonitor taskMonitor, String nodeName, String networkName,
			Boolean conflictedDomain) {

		if (Util.proteinsMap == null)
			return;

		List<Protein> proteinList = Util.proteinsMap.get(networkName);

		if (proteinList != null) {
			Optional<Protein> isPtnPresent = proteinList.stream().filter(value -> value.gene.equals(nodeName))
					.findFirst();

			if (isPtnPresent.isPresent()) {
				Protein _myProtein = isPtnPresent.get();
				_myProtein.isConflictedDomain = conflictedDomain;

			}

			else {
				taskMonitor.showMessage(TaskMonitor.Level.WARN, "WARNING: Node " + nodeName + " has not been found.\n");
			}
		}

	}

	/**
	 * Method responsible for updating valid proteins based on table information
	 * 
	 * @param taskMonitor task monitor
	 * @param nodeName    current node name
	 * @param validDomain it is a valid protein or not
	 */
	private static void updateValidProtein(TaskMonitor taskMonitor, String nodeName, String networkName,
			Boolean validDomain) {

		if (Util.proteinsMap == null)
			return;

		List<Protein> proteinList = Util.proteinsMap.get(networkName);

		if (proteinList != null) {
			Optional<Protein> isPtnPresent = proteinList.stream().filter(value -> value.gene.equals(nodeName))
					.findFirst();

			if (isPtnPresent.isPresent()) {
				Protein _myProtein = isPtnPresent.get();
				_myProtein.isValid = validDomain;

			}

			else {
				taskMonitor.showMessage(TaskMonitor.Level.WARN, "WARNING: Node " + nodeName + " has not been found.\n");
			}
		}

	}

	/**
	 * Method responsible for updating conflicted residues
	 * 
	 * @param nodeName           current node
	 * @param conflictedResidues conflicted residues
	 * @param taskMonitor        task monitor
	 */
	private static void updateConflictedResidues(String nodeName, List<String> conflictedResidues, String networkName,
			TaskMonitor taskMonitor) {

		if (conflictedResidues == null || conflictedResidues.size() == 0 || conflictedResidues.get(0).isBlank()
				|| conflictedResidues.get(0).isEmpty())
			return;

		List<Protein> proteinList = Util.proteinsMap.get(networkName);
		if (proteinList != null) {
			Optional<Protein> isPtnPresent = proteinList.stream().filter(value -> value.gene.equals(nodeName))
					.findFirst();

			if (isPtnPresent.isPresent()) {
				Protein _myProtein = isPtnPresent.get();

				updateResidueStatus(_myProtein.reactionSites, conflictedResidues, nodeName, taskMonitor);
			}

			else {
				taskMonitor.showMessage(TaskMonitor.Level.WARN, "WARNING: Node " + nodeName + " has not been found.\n");
			}
		}
	}

	/**
	 * Method responsible for updating residues status according to the conflict
	 * status
	 * 
	 * @param residues           current residues
	 * @param conflictedResidues all conflicted residues
	 * @param nodeName           node name
	 * @param taskMonitor        task monitor
	 */
	private static void updateResidueStatus(List<Residue> residues, List<String> conflictedResidues, String nodeName,
			TaskMonitor taskMonitor) {

		try {

			for (String residue : conflictedResidues) {

				String[] residuesArray = residue.split("\\[|\\]");
				char residueName = residuesArray[0].trim().charAt(0);
				int position = Integer.parseInt(residuesArray[1]);

				Optional<Residue> isResiduePresent = residues.stream()
						.filter(value -> value.position == position && value.aminoacid == residueName).findFirst();

				if (isResiduePresent.isPresent()) {
					Residue res = isResiduePresent.get();
					res.isConflicted = true;
				}

			}
		} catch (Exception e) {
			taskMonitor.showMessage(TaskMonitor.Level.WARN,
					"ERROR: Node: " + nodeName + " - Residues don't match with the pattern 'name[position]'\n");
			return;
		}

	}

	/**
	 * Method responsible for updating protein domains map
	 * 
	 * @param nodeName    node name
	 * @param domains     domains stored in Cytoscape Table
	 * @param isPredicted predicted or original domain
	 * @param taskMonitor task monitor
	 */
	private static void updateProteinDomainsMap(String nodeName, List<String> domains, boolean isPredicted,
			boolean isValid, String networkName, TaskMonitor taskMonitor) {

		if (domains == null || domains.size() == 0 || domains.get(0).isBlank() || domains.get(0).isEmpty())
			return;

		List<ProteinDomain> proteinDomains = Util.parserProteinDomainColumnFromNodeCytoTable(domains, taskMonitor,
				isPredicted, isValid, nodeName);

		// Check if the node exists in the network

		if (proteinDomains != null && proteinDomains.size() > 0) {

			// Update proteinsMap
			List<Protein> proteinList = Util.proteinsMap.get(networkName);
			if (proteinList != null) {

				boolean noDomains = false;
				Optional<Protein> isPtnPresent = proteinList.stream().filter(value -> value.gene.equals(nodeName))
						.findFirst();

				if (isPtnPresent.isPresent()) {
					Protein _myProtein = isPtnPresent.get();
					_myProtein.predicted_domain_epoch = 0;

					if (isValid) {

						if (_myProtein.domains != null) {

							for (ProteinDomain domain : proteinDomains) {

								Optional<ProteinDomain> current_domain = _myProtein.domains.stream()
										.filter(value -> value.name.equals(domain.name) && value.endId == domain.endId
												&& value.startId == domain.startId
												&& value.eValue.equals(domain.eValue))
										.findFirst();

								if (current_domain.isPresent()) {
									ProteinDomain current_dom = current_domain.get();
									current_dom.isValid = true;
								} else {
									_myProtein.domains.add(domain);
								}
							}
						} else {
							noDomains = true;
						}
					} else if (isPredicted) {
						if (_myProtein.domains != null && _myProtein.domains.size() > 0) {
							_myProtein.domains.addAll(proteinDomains);
							_myProtein.domains = _myProtein.domains.stream().distinct().collect(Collectors.toList());

							// Check whether exists only transmem regions
							if (_myProtein.domains.stream().filter(value -> value.name.toLowerCase().equals("transmem"))
									.collect(Collectors.toList()).size() == _myProtein.domains.size())
								_myProtein.predicted_domain_epoch = -1;
						} else {
							noDomains = true;
						}
					} else {
						noDomains = true;
					}

					if (noDomains) {
						_myProtein.domains = proteinDomains;

						// Sort protein domains list
						Collections.sort(_myProtein.domains, new Comparator<ProteinDomain>() {
							@Override
							public int compare(ProteinDomain lhs, ProteinDomain rhs) {
								return lhs.startId > rhs.startId ? 1 : (lhs.startId < rhs.startId) ? -1 : 0;
							}
						});

						// Check whether exists only transmem regions
						if (_myProtein.domains.stream().filter(value -> value.name.toLowerCase().equals("transmem"))
								.collect(Collectors.toList()).size() == _myProtein.domains.size())
							_myProtein.predicted_domain_epoch = -1;
						ProcessProteinLocationTask.addReactionSites(_myProtein, true);
					} else {
						ProcessProteinLocationTask.updateResiduesLocationAndScore(_myProtein);
					}
				}

				else {
					taskMonitor.showMessage(TaskMonitor.Level.WARN,
							"WARNING: Node " + nodeName + " has not been found.\n");
				}
			}
		}
	}

	/**
	 * Method responsible for updating protein domains scores map
	 * 
	 * @param nodeName    node name
	 * @param domains     domains stored in Cytoscape Table
	 * @param taskMonitor task monitor
	 */
	private static void updateProteinDomainsScores(String nodeName, String networkName, List<String> domains,
			TaskMonitor taskMonitor) {

		if (domains == null || domains.size() == 0 || domains.get(0).isBlank() || domains.get(0).isEmpty())
			return;

		Map<String, Double> domainScores = new HashMap<String, Double>();
		try {

			for (String domain : domains) {

				String domainName = "";
				String score = "";
				// e.g. TRANSMEM#Score:0.9992
				String[] domainScore = domain.split("#Score:");
				domainName = domainScore[0].trim();
				score = domainScore[1].trim();
				domainScores.put(domainName, Double.valueOf(score));

			}
		} catch (Exception e) {
			taskMonitor.showMessage(TaskMonitor.Level.WARN, "ERROR: Node: " + nodeName
					+ " - Protein domains scores don't match with the pattern 'name#Score'\n");
			return;
		}

		// Check if the node exists in the network

		if (domainScores.size() > 0) {

			// Update proteinsMap
			List<Protein> proteinList = Util.proteinsMap.get(networkName);
			if (proteinList != null) {

				Optional<Protein> isPtnPresent = proteinList.stream().filter(value -> value.gene.equals(nodeName))
						.findFirst();

				if (isPtnPresent.isPresent()) {
					Protein _myProtein = isPtnPresent.get();
					_myProtein.domainScores = domainScores;
				}

				else {
					taskMonitor.showMessage(TaskMonitor.Level.WARN,
							"WARNING: Node " + nodeName + " has not been found.\n");
				}
			}
		}
	}

	/**
	 * Method responsible for updating ptms map
	 * 
	 * @param nodeName    node name
	 * @param ptmsStr     ptms stored in Cytoscape Table
	 * @param taskMonitor task monitor
	 */
	private static void updatePTMsMap(String nodeName, String networkName, String ptmsStr, TaskMonitor taskMonitor,
			CyTable nodeTable) {

		List<PTM> ptmsList = new ArrayList<PTM>();
		try {
			String[] cols = ptmsStr.split(",");
			for (String col : cols) {
				String[] domainsArray = col.split("\\[|\\]");
				String ptmName = domainsArray[0].trim();
				String[] colRange = domainsArray[1].split("-");
				char residue = colRange[0].charAt(0);
				int position = Integer.parseInt(colRange[1]);
				ptmsList.add(new PTM(ptmName, residue, position));
			}
		} catch (Exception e) {
			taskMonitor.showMessage(TaskMonitor.Level.WARN,
					"ERROR: Node: " + nodeName + " - PTMs don't match with the pattern 'name[residue-position]'\n");
			return;
		}

		// Check if the node exists in the network
		Optional<CyRow> isNodePresent = nodeTable.getAllRows().stream().filter(new Predicate<CyRow>() {
			public boolean test(CyRow o) {
				return o.get(CyNetwork.NAME, String.class).equals(nodeName);
			}
		}).findFirst();

		if (isNodePresent.isPresent()) {// Get node if exists
			CyRow _node_row = isNodePresent.get();
			Long node_SUID = _node_row.get(CyIdentifiable.SUID, Long.class);
			if (ptmsList.size() > 0) {
				LoadPTMsTask.updatePTMsMap(networkName, node_SUID, ptmsList);

			}
		} else {
			taskMonitor.showMessage(TaskMonitor.Level.WARN, "WARNING: Node " + nodeName + " has not been found.\n");
		}

	}

	/**
	 * Method responsible for updating monolink map
	 * 
	 * @param nodeName     node name
	 * @param monolinksStr monolinks stored in Cytoscape Table
	 * @param taskMonitor  task monitor
	 */
	private static void updateMonolinksMap(String nodeName, String monolinksStr, TaskMonitor taskMonitor,
			CyTable nodeTable) {

		List<CrossLink> monolinksList = new ArrayList<CrossLink>();
		try {
			String[] cols = monolinksStr.split(",");
			for (String col : cols) {
				// SEQUENCE[xl_pos_a-xl_pos_b][pept_pos1-pept_pos2]
				String[] monolinksArray = col.split("\\[|\\]");
				String sequence = monolinksArray[0].trim();

				String[] colXLpositions = monolinksArray[1].split("-");
				int xl_a = Integer.parseInt(colXLpositions[0]);
				int xl_b = Integer.parseInt(colXLpositions[1]);

				String[] colPeptidePosition_Protein = monolinksArray[3].split("-");
				int pos_a = Integer.parseInt(colPeptidePosition_Protein[0]);
				int pos_b = Integer.parseInt(colPeptidePosition_Protein[1]);
				monolinksList.add(new CrossLink(sequence, xl_a, xl_b, pos_a, pos_b));
			}
		} catch (Exception e) {
			taskMonitor.showMessage(TaskMonitor.Level.WARN,
					"ERROR: Node: " + nodeName + " - Monolinks don't match with the pattern 'name[xl_a-xl_b]'\n");
			return;
		}

//		CyNode currentNode = null;

		// Check if the node exists in the network
		Optional<CyRow> isNodePresent = nodeTable.getAllRows().stream().filter(new Predicate<CyRow>() {
			public boolean test(CyRow o) {
				return o.get(CyNetwork.NAME, String.class).equals(nodeName);
			}
		}).findFirst();

		if (isNodePresent.isPresent()) {// Get node if exists
//			CyRow _node_row = isNodePresent.get();
//			currentNode = myNetwork.getNode(Long.parseLong(_node_row.getRaw(CyIdentifiable.SUID).toString()));

			if (monolinksList.size() > 0) {
//				String node_name = nodeTable.getRow(currentNode.getSUID()).get(CyNetwork.NAME,
//						String.class);

//				Protein ptn = new Protein(node_name, "", monolinksList);
//				LoadProteinLocationTask.updateMonolinksMap(myNetwork, currentNode, ptn);

			}
		} else {
			taskMonitor.showMessage(TaskMonitor.Level.WARN, "WARNING: Node " + nodeName + " has not been found.\n");
		}

	}

}
