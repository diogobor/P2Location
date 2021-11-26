package de.fmp.liulab.task;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.view.model.VisualLexicon;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;

import de.fmp.liulab.model.PredictedTransmem;
import de.fmp.liulab.model.Protein;
import de.fmp.liulab.model.ProteinDomain;
import de.fmp.liulab.utils.Util;

public class ProcessTransmemRegionsTask extends AbstractTask implements ActionListener {

	private CyApplicationManager cyApplicationManager;
	private static CyNetwork myNetwork;
	public static VisualLexicon lexicon;
	public static VisualStyle style;

	private Map<Protein, List<PredictedTransmem>> proteinsWithPredTransmDict;

	public ProcessTransmemRegionsTask(CyApplicationManager cyApplicationManager,
			final VisualMappingManager vmmServiceRef) {

		this.cyApplicationManager = cyApplicationManager;
		this.style = vmmServiceRef.getCurrentVisualStyle();
		this.myNetwork = cyApplicationManager.getCurrentNetwork();
		proteinsWithPredTransmDict = new HashMap<Protein, List<PredictedTransmem>>();
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void run(TaskMonitor taskMonitor) throws Exception {

		taskMonitor.setTitle("P2Location - Predict transmembrane regions task");

		if (Util.proteinsMap == null || myNetwork == null)
			return;

		List<Protein> allProteins = Util.proteinsMap.get(myNetwork.toString());

		int old_progress = 0;
		int summary_processed = 0;
		int total_ptns = allProteins.size();

		taskMonitor.showMessage(TaskMonitor.Level.INFO, "Predicting transmembrane regions: " + old_progress + "%");
		for (Protein protein : allProteins) {

			if (protein.sequence.isBlank() || protein.sequence.isEmpty())
				continue;

			List<PredictedTransmem> predictedTransmemList = Util.predictTransmemRegions(protein.sequence);
			proteinsWithPredTransmDict.put(protein, predictedTransmemList);

			summary_processed++;

			Util.progressBar(summary_processed, old_progress, total_ptns, "Predicting transmembrane regions: ",
					taskMonitor, null);

		}

		taskMonitor.showMessage(TaskMonitor.Level.INFO, "Applying cutoff filtering...");

		applyFilterToTransmemDic(taskMonitor);
	}

	private void applyFilterToTransmemDic(TaskMonitor taskMonitor) {

		boolean isChanged = false;
		for (Map.Entry<Protein, List<PredictedTransmem>> entry : proteinsWithPredTransmDict.entrySet()) {
			Protein protein = entry.getKey();
			List<PredictedTransmem> transmemList = entry.getValue();

			List<ProteinDomain> transmemDomains = createTransmemDomains(transmemList);

			if (transmemDomains.size() == 0)
				continue;

			isChanged = true;

			if (protein.domains != null)
				protein.domains.addAll(transmemDomains);
			else
				protein.domains = transmemDomains;

			Collections.sort(protein.domains, new Comparator<ProteinDomain>() {
				@Override
				public int compare(ProteinDomain lhs, ProteinDomain rhs) {
					return lhs.startId > rhs.startId ? 1 : (lhs.startId < rhs.startId) ? -1 : 0;
				}
			});
		}

		if (isChanged)
			updateProteinsMap(taskMonitor);

	}

	private void updateProteinsMap(TaskMonitor taskMonitor) {
		for (Map.Entry<Protein, List<PredictedTransmem>> entry : proteinsWithPredTransmDict.entrySet()) {
			Protein protein = entry.getKey();

			Optional<Protein> isPresent = Util.proteinsMap.get(myNetwork.toString()).stream()
					.filter(value -> value.gene.equals(protein.gene) && value.location.equals(protein.location)
							&& value.proteinID.equals(protein.proteinID) && value.sequence.equals(protein.sequence))
					.findFirst();

			if (isPresent.isPresent()) {
				Protein current_protein = isPresent.get();
				current_protein.domains = protein.domains;
				current_protein.domains = current_protein.domains.stream().distinct().collect(Collectors.toList());
			}
		}

		Util.updateProteins(taskMonitor, myNetwork, null, false, true);
	}

	private List<ProteinDomain> createTransmemDomains(List<PredictedTransmem> transmemList) {

		List<ProteinDomain> new_transm_list = new ArrayList<ProteinDomain>();
		for (int i = 0; i < transmemList.size(); i++) {

			PredictedTransmem predictedTransmem = transmemList.get(i);
			if (predictedTransmem.score == 0)
				continue;

			if (predictedTransmem.score >= Util.transmemPredictionRegionsScore) {
				int startID = i;
				int endID = i;

				int j = i + 1;
				for (; j < transmemList.size(); j++) {
					PredictedTransmem nextPredictedTransmem = transmemList.get(j);
					if (nextPredictedTransmem.score >= Util.transmemPredictionRegionsScore) {
						endID = j;
					} else
						break;
				}

				ProteinDomain new_transm_domain = new ProteinDomain("TRANSMEM", startID, endID, true, "predicted");
				new_transm_list.add(new_transm_domain);
				i = j - 1;
			}
		}

		return new_transm_list;
	}
}
