package de.fmp.liulab.task.command_lines;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.work.ProvidesTitle;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;

import de.fmp.liulab.internal.action.ExportPTMsAction;

/**
 * Class responsible for exporting post-translational modifications via command line
 * 
 * @author diogobor
 *
 */
public class ExportPTMsCommandTask extends CyRESTAbstractTask {

	private CyNetwork myNetwork;

	@ProvidesTitle
	public String getTitle() {
		return "Export PTM(s)";
	}

	@Tunable(description = "PTM(s) file name", longDescription = "Name of the PTM(s) file. (Supported format: *.csv)", exampleStringValue = "ptms.csv")
	public String fileName = "";

	/**
	 * Constructor
	 */
	public ExportPTMsCommandTask(CyApplicationManager cyApplicationManager) {
		this.myNetwork = cyApplicationManager.getCurrentNetwork();
	}

	/**
	 * Run
	 */
	public void run(TaskMonitor taskMonitor) throws Exception {

		// Parse file and update data table model
		if (!(fileName.isBlank() || fileName.isEmpty()) && myNetwork != null) {

			if (fileName.endsWith(".csv")) {

				ExportPTMsAction.createPTMsFile(fileName, myNetwork, taskMonitor);
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
