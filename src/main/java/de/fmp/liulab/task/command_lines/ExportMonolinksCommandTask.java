package de.fmp.liulab.task.command_lines;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.work.ProvidesTitle;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;

import de.fmp.liulab.internal.action.ExportMonolinksAction;

public class ExportMonolinksCommandTask extends CyRESTAbstractTask {

	private CyNetwork myNetwork;

	@ProvidesTitle
	public String getTitle() {
		return "Export PTM(s)";
	}

	@Tunable(description = "Monolinked peptide(s) file name", longDescription = "Name of the Monolinked peptide(s) file. (Supported format: *.csv)", exampleStringValue = "monolinks.csv")
	public String fileName = "";

	/**
	 * Constructor
	 */
	public ExportMonolinksCommandTask(CyApplicationManager cyApplicationManager) {
		this.myNetwork = cyApplicationManager.getCurrentNetwork();
	}

	/**
	 * Run
	 */
	public void run(TaskMonitor taskMonitor) throws Exception {

		// Parse file and update data table model
		if (!(fileName.isBlank() || fileName.isEmpty()) && myNetwork != null) {

			if (fileName.endsWith(".csv")) {

				ExportMonolinksAction.createMonolinksFile(fileName, myNetwork, taskMonitor);
			} else {
				taskMonitor.showMessage(TaskMonitor.Level.ERROR,
						"ERROR: Format file not acceptable. Supported format: *.csv");
			}
		} else {
			taskMonitor.showMessage(TaskMonitor.Level.ERROR,
					"ERROR: Missing file name. It is not possible to save the file.");
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <R> R getResults(Class<? extends R> type) {
		return (R) "Done!";
	}

}
