package de.fmp.liulab.task.command_lines;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.work.ProvidesTitle;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;

import de.fmp.liulab.internal.action.ExportProteinDomainsAction;

/**
 * Class responsible for exporting protein domains via command line
 * 
 * @author diogobor
 *
 */
public class ExportProteinDomainsCommandTask extends CyRESTAbstractTask {

	private CyNetwork myNetwork;

	@ProvidesTitle
	public String getTitle() {
		return "Export protein domains";
	}

	@Tunable(description = "Protein domains file name", longDescription = "Name of the protein domain file. (Supported format: *.csv)", exampleStringValue = "proteinDomains.csv")
	public String fileName = "";

	/**
	 * Constructor
	 */
	public ExportProteinDomainsCommandTask(CyApplicationManager cyApplicationManager) {
		this.myNetwork = cyApplicationManager.getCurrentNetwork();
	}

	/**
	 * Run
	 */
	public void run(TaskMonitor taskMonitor) throws Exception {

		// Parse file and update data table model
		if (!(fileName.isBlank() || fileName.isEmpty()) && myNetwork != null) {
			
			if(fileName.endsWith(".csv")) {
				
				ExportProteinDomainsAction.createProteinDomainsFile(fileName, myNetwork, taskMonitor);
			}
			else {
				taskMonitor.showMessage(TaskMonitor.Level.ERROR, "ERROR: Format file not acceptable. Supported format: *.csv");
			}
		}
		else {
			taskMonitor.showMessage(TaskMonitor.Level.ERROR, "ERROR: Missing file name. It is not possible to save the file.");
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <R> R getResults(Class<? extends R> type) {
		return (R) "Done!";
	}

}
