package de.fmp.liulab.task.command_lines;

import org.cytoscape.util.swing.OpenBrowser;
import org.cytoscape.work.ProvidesTitle;
import org.cytoscape.work.TaskMonitor;

public class ReadMeCommandTask extends CyRESTAbstractTask {

	@ProvidesTitle
	public String getTitle() {
		return "Read Me";
	}

	private OpenBrowser openBrowser;

	/**
	 * Constructor
	 */
	public ReadMeCommandTask(OpenBrowser openBrowser) {
		this.openBrowser = openBrowser;
	}

	/**
	 * Run
	 */
	public void run(TaskMonitor taskMonitor) throws Exception {
		openBrowser.openURL("http://dx.doi.org/10.21203/rs.3.pex-1172/v1");
	}

	@SuppressWarnings("unchecked")
	@Override
	public <R> R getResults(Class<? extends R> type) {
		return (R) "Done!";
	}
}
