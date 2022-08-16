package de.fmp.liulab.task;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.view.model.VisualLexicon;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;

import de.fmp.liulab.model.PredictedTransmem;
import de.fmp.liulab.model.Protein;
import de.fmp.liulab.model.ProteinDomain;
import de.fmp.liulab.utils.Util;

public class ProcessTransmemRegionsTask extends AbstractTask implements ActionListener {

	private static CyNetwork myNetwork;
	public static VisualLexicon lexicon;

	private Map<Protein, List<PredictedTransmem>> proteinsWithPredTransmDict;
	private final static String TRANSMEMBRANE = "transmem";

	public ProcessTransmemRegionsTask(CyApplicationManager cyApplicationManager,
			final VisualMappingManager vmmServiceRef) {

		myNetwork = cyApplicationManager.getCurrentNetwork();
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void run(TaskMonitor taskMonitor) throws Exception {

		taskMonitor.setTitle("P2Location - Predict transmembrane regions task");

		if (Util.proteinsMap == null || myNetwork == null || Util.proteinsWithPredTransmDict == null)
			return;

		List<Protein> allProteins = Util.proteinsMap.get(myNetwork.toString());
		this.proteinsWithPredTransmDict = Util.proteinsWithPredTransmDict.get(myNetwork.toString());

		int old_progress = 0;
		taskMonitor.showMessage(TaskMonitor.Level.INFO, "Predicting transmembrane regions: " + old_progress + "%");

		List<Protein> proteinsOfInterest = new ArrayList<Protein>();

		if (this.proteinsWithPredTransmDict.size() == 0) {

			for (Protein protein : allProteins) {

				if (protein.sequence.isBlank() || protein.sequence.isEmpty())
					continue;

				if (protein.domains != null && protein.domains.size() > 0) {

					List<ProteinDomain> non_predict_domains = protein.domains.stream()
							.filter(value -> !value.isPredicted).collect(Collectors.toList());

					if (non_predict_domains == null || non_predict_domains.size() > 0)
						continue;
				}

				proteinsOfInterest.add(protein);
			}
		} else {

			for (Protein protein : allProteins) {

				if (protein.sequence.isBlank() || protein.sequence.isEmpty())
					continue;

				if (protein.domains != null && protein.domains.size() > 0) {

					List<ProteinDomain> non_predict_domains = protein.domains.stream()
							.filter(value -> !value.isPredicted).collect(Collectors.toList());

					if (non_predict_domains == null || non_predict_domains.size() > 0)
						continue;
				}

				Optional<Map.Entry<Protein, List<PredictedTransmem>>> isPresent = this.proteinsWithPredTransmDict
						.entrySet().stream()
						.filter(value -> value.getKey().gene.equals(protein.gene)
								&& value.getKey().proteinID.equals(protein.proteinID)
								&& value.getKey().sequence.equals(protein.sequence) && value.getKey().location != null
								&& protein.location != null && value.getKey().location.equals(protein.location))
						.findFirst();

				if (!isPresent.isPresent()) {
					proteinsOfInterest.add(protein);
				}
			}
		}

		Map<Protein, List<PredictedTransmem>> predictedTransmemList = null;

//		if (Util.transmembraneTool == 1)
//			predictedTransmemList = Util.predictTransmemRegionsFromDeepTMHMM(ProteinsOfInterest, taskMonitor);
//		else if (Util.transmembraneTool == 2)

		predictedTransmemList = Util.predictTransmemRegionsFromTMHMM(proteinsOfInterest, taskMonitor);

//		Map<Protein, List<PredictedTransmem>> predictedTransmemList = Util
//				.predictTransmemRegionsFromPhobius(ProteinsOfInterest, taskMonitor);

		// Update proteinsWithPredTransmDic
		for (Map.Entry<Protein, List<PredictedTransmem>> entry : predictedTransmemList.entrySet()) {
			this.proteinsWithPredTransmDict.put(entry.getKey(), entry.getValue());
		}

		taskMonitor.showMessage(TaskMonitor.Level.INFO, "Applying cutoff filtering...");
		applyFilterToTransmemDic(taskMonitor);

		retrieveTransmemInfoFromUniprot(proteinsOfInterest, taskMonitor);
		updateProteinsMap(taskMonitor);
	}

	/**
	 * Method responsible for filtering the predicted transmembrane regions
	 * 
	 * @param taskMonitor current task monitor
	 */
	private void applyFilterToTransmemDic(TaskMonitor taskMonitor) {

		for (Map.Entry<Protein, List<PredictedTransmem>> entry : this.proteinsWithPredTransmDict.entrySet()) {

			try {
				Protein protein = entry.getKey();
				List<PredictedTransmem> transmemList = entry.getValue();

				List<ProteinDomain> transmemDomains = createTransmemDomains(transmemList);

				List<ProteinDomain> domains_without_predicted_transm = null;

				if (protein.domains != null) {
					domains_without_predicted_transm = protein.domains.stream()
							.filter(value -> (!value.isPredicted && !value.name.toLowerCase().contains(TRANSMEMBRANE))
									|| (value.isPredicted && !value.name.toLowerCase().contains(TRANSMEMBRANE)))
							.collect(Collectors.toList());
				}

				if (domains_without_predicted_transm != null)
					domains_without_predicted_transm.addAll(transmemDomains);
				else if (transmemDomains.size() > 0)
					domains_without_predicted_transm = transmemDomains;

				protein.domains = domains_without_predicted_transm;

				if (protein.domains != null) {
					Collections.sort(protein.domains, new Comparator<ProteinDomain>() {
						@Override
						public int compare(ProteinDomain lhs, ProteinDomain rhs) {
							return lhs.startId > rhs.startId ? 1 : (lhs.startId < rhs.startId) ? -1 : 0;
						}
					});
				}
			} catch (Exception e) {
				System.out.println("ERROR");
			}

		}
	}

	/**
	 * Method responsible for updating proteins map
	 * 
	 * @param taskMonitor
	 */
	private void updateProteinsMap(TaskMonitor taskMonitor) {
		for (Map.Entry<Protein, List<PredictedTransmem>> entry : this.proteinsWithPredTransmDict.entrySet()) {
			Protein protein = entry.getKey();
			if (protein.domains == null)
				continue;

			Optional<Protein> isPresent = Util.proteinsMap.get(myNetwork.toString()).stream()
					.filter(value -> value.gene != null && value.gene.equals(protein.gene) && value.location != null
							&& value.location.equals(protein.location) && value.proteinID != null
							&& value.proteinID.equals(protein.proteinID) && value.sequence != null
							&& value.sequence.equals(protein.sequence))
					.findFirst();

			if (isPresent.isPresent()) {
				Protein current_protein = isPresent.get();
				current_protein.domains = protein.domains;
				current_protein.domains = current_protein.domains.stream().distinct().collect(Collectors.toList());
			}
			Util.updateResiduesBasedOnProteinDomains(protein, false);
		}

		Util.updateProteins(taskMonitor, myNetwork, null, false, false);
	}

	/**
	 * Method responsible for converting Predicted Transmem to ProteinDomain
	 * 
	 * @param transmemList list of predicted transmem
	 * @return list of protein domains
	 */
	private List<ProteinDomain> createTransmemDomains(List<PredictedTransmem> transmemList) {

		List<ProteinDomain> new_transm_list = new ArrayList<ProteinDomain>();
		double transm_score = 0;
		for (int i = 0; i < transmemList.size(); i++) {

			transm_score = 0;
			PredictedTransmem predictedTransmem = transmemList.get(i);
			if (predictedTransmem.score == 0)
				continue;

			if (predictedTransmem.score >= Util.transmemPredictionRegionsLowerScore) {
				int startID = (i + 1);
				int endID = (i + 1);

				int j = i + 1;
				for (; j < transmemList.size(); j++) {
					PredictedTransmem nextPredictedTransmem = transmemList.get(j);
					if (nextPredictedTransmem.score >= Util.transmemPredictionRegionsLowerScore) {
						endID = (j + 1);
						if (nextPredictedTransmem.score > transm_score)
							transm_score = nextPredictedTransmem.score;
					} else
						break;
				}

				ProteinDomain new_transm_domain = new ProteinDomain("TRANSMEM", startID, endID, false,
						"" + transm_score);
				new_transm_list.add(new_transm_domain);
				i = j - 1;
			}
		}

		return new_transm_list;
	}

	/**
	 * Method responsible for merging similar protein domains with different ranges
	 * 
	 * @param protein  current protein
	 * @param delta_aa delta number of amino acids
	 */
	private List<ProteinDomain> unifyResiduesDomains(List<ProteinDomain> current_ptn_domain_list, int delta_aa) {

		for (ProteinDomain domain : current_ptn_domain_list) {

			// e.g. Domain[716-722] and Domain[717-723] => Domain[716-723] -> delta == 0
			// e.g. Domain[136-142] and Domain[144-150] => Domain[136-150] -> delta == 2
			List<ProteinDomain> candidates_domains = current_ptn_domain_list.stream()
					.filter(value -> value.startId >= domain.startId - delta_aa
							&& value.startId <= domain.endId + delta_aa && value.endId >= domain.endId
							&& value.name.equals(domain.name))
					.collect(Collectors.toList());

			if (candidates_domains.size() > 0) {
				double highest_score = candidates_domains.stream().map(value -> Double.parseDouble(value.eValue))
						.max(Comparator.naturalOrder()).get();
				int min_value = candidates_domains.stream().map(value -> value.startId).min(Comparator.naturalOrder())
						.get();
				int max_value = candidates_domains.stream().map(value -> value.endId).max(Comparator.naturalOrder())
						.get();
				for (ProteinDomain expandDomain : candidates_domains) {

					domain.endId = max_value;
					expandDomain.startId = min_value;
					expandDomain.eValue = Double.toString(highest_score);
				}
			}
		}

		current_ptn_domain_list = current_ptn_domain_list.stream().distinct().collect(Collectors.toList());

		return current_ptn_domain_list;
	}

	/**
	 * Method responsible for merging transmembrane regions
	 * 
	 * @param protein               current protein
	 * @param transmembrane_domains list of transmembrane regions
	 */
	private void mergeTransmemDomains(Protein protein, List<ProteinDomain> transmembrane_domains) {

		if (protein.domains == null) {
			protein.domains = transmembrane_domains;
			return;
		}
		List<ProteinDomain> current_transmem_list = protein.domains.stream()
				.filter(value -> value.name.toLowerCase().equals(TRANSMEMBRANE)).collect(Collectors.toList());

		current_transmem_list.addAll(transmembrane_domains);
		Collections.sort(current_transmem_list, new Comparator<ProteinDomain>() {
			@Override
			public int compare(ProteinDomain lhs, ProteinDomain rhs) {
				return lhs.startId > rhs.startId ? 1 : (lhs.startId < rhs.startId) ? -1 : 0;
			}
		});

		List<ProteinDomain> candidates_transmem_list = new ArrayList<ProteinDomain>();

		int offset = 0;
		for (int i = 0; i < current_transmem_list.size(); i++) {
			ProteinDomain current_transm = current_transmem_list.get(i);

			int j = i + 1;
			for (; j < current_transmem_list.size(); j++) {
				ProteinDomain next_transm = current_transmem_list.get(j);
				if ((next_transm.startId >= current_transm.startId && next_transm.endId <= current_transm.endId)
						|| (next_transm.startId >= current_transm.startId && next_transm.endId > current_transm.endId
								&& next_transm.startId <= current_transm.endId)) {
					// get index
					offset = j;
				} else
					break;
			}

			int offset_start = current_transmem_list.get(j - 1).startId - current_transmem_list.get(i).startId;
			int offset_end = current_transmem_list.get(i).endId - current_transmem_list.get(j - 1).endId;
			offset = offset_start > offset_end ? offset_start : offset_end;
			candidates_transmem_list.addAll(unifyResiduesDomains(current_transmem_list.subList(i, j), offset));

			i = j - 1;
		}

		protein.domains.removeIf(value -> value.name.toLowerCase().equals(TRANSMEMBRANE));
		protein.domains.addAll(candidates_transmem_list);
	}

	/**
	 * Method responsible for retrieving transmembrane regions from Uniprot
	 * 
	 * @param proteins    list of proteins
	 * @param taskMonitor current task monitor
	 */
	private void retrieveTransmemInfoFromUniprot(List<Protein> proteins, TaskMonitor taskMonitor) {

		if (proteins == null || proteins.size() == 0)
			return;

		int total_lines = proteins.size();

		int old_progress = 0;
		int summary_processed = 0;
		for (Protein protein : proteins) {

			List<ProteinDomain> transmem_domains = Util.getTransmembraneInfoFromUniprot(myNetwork, protein.proteinID,
					taskMonitor);

			if (transmem_domains != null && transmem_domains.size() > 0) {
				if (protein.domains == null)
					protein.domains = transmem_domains;
				else {
					mergeTransmemDomains(protein, transmem_domains);
					protein.domains = protein.domains.stream().distinct().collect(Collectors.toList());
				}
			}

			summary_processed++;
			Util.progressBar(summary_processed, old_progress, total_lines,
					"Retrieving transmembrane regions from Uniprot: ", taskMonitor, null);
		}
	}
}
