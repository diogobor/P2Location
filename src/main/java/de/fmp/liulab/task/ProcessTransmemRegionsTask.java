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

		List<Protein> ProteinsOfInterest = new ArrayList<Protein>();

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

				ProteinsOfInterest.add(protein);
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
					ProteinsOfInterest.add(protein);
				}
			}
		}

//		Map<Protein, List<PredictedTransmem>> predictedTransmemList = Util.predictTransmemRegionsFromTMHMM(ProteinsOfInterest,
//				taskMonitor);

//		Map<Protein, List<PredictedTransmem>> predictedTransmemList = Util
//				.predictTransmemRegionsFromPhobius(ProteinsOfInterest, taskMonitor);
		
		Map<Protein, List<PredictedTransmem>> predictedTransmemList = Util
				.predictTransmemRegionsFromDeepTMHMM(ProteinsOfInterest, taskMonitor);
		for (Map.Entry<Protein, List<PredictedTransmem>> entry : predictedTransmemList.entrySet()) {
			this.proteinsWithPredTransmDict.put(entry.getKey(), entry.getValue());
		}

		taskMonitor.showMessage(TaskMonitor.Level.INFO, "Applying cutoff filtering...");

		applyFilterToTransmemDic(taskMonitor);
	}

	/**
	 * Method responsible for filtering the predicted transmembrane regions
	 * 
	 * @param taskMonitor current task monitor
	 */
	private void applyFilterToTransmemDic(TaskMonitor taskMonitor) {

		boolean isChanged = false;
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

				isChanged = true;

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

		if (isChanged)
			updateProteinsMap(taskMonitor);

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
}
