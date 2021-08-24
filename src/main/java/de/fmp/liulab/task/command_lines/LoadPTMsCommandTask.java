package de.fmp.liulab.task.command_lines;

import java.util.Arrays;
import java.util.List;

import javax.swing.table.DefaultTableModel;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.work.ProvidesTitle;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;

import de.fmp.liulab.parser.Parser;
import de.fmp.liulab.task.LoadPTMsTask;

public class LoadPTMsCommandTask extends CyRESTAbstractTask {

	private CyNetwork myNetwork;
	private Parser parserFile;

	private String[] columnNames = { "Node Name", "PTM(s)" };
	private final Class[] columnClass = new Class[] { String.class, String.class };

	@ProvidesTitle
	public String getTitle() {
		return "Load PTM(s)";
	}

	@Tunable(description = "PTM(s) file name", longDescription = "Name of the PTM(s) file. (Supported formats: *.csv)", exampleStringValue = "ptms.csv")
	public String fileName = "";

	@Tunable(description = "Node(s) to find PTM(s)", longDescription = "Give the node(s) name, separated by comma, to find PTM(s). (type 'all' to get PTM(s) of all nodes)", exampleStringValue = "PDE12")
	public String nodesName = "";

	/**
	 * Constructor
	 */
	public LoadPTMsCommandTask(CyApplicationManager cyApplicationManager) {
		this.myNetwork = cyApplicationManager.getCurrentNetwork();
	}

	/**
	 * Run
	 */
	public void run(TaskMonitor taskMonitor) throws Exception {

		this.init_table_data_model_protein_domains();

		// Parse file and update data table model
		if (!(fileName.isBlank() || fileName.isEmpty()))
			this.parserFile(taskMonitor);

		if (!(nodesName.isBlank() || nodesName.isEmpty()))
			this.getPTMsFromServer(taskMonitor);

	}

	/**
	 * Method responsible for initializing table model of protein domains
	 */
	private void init_table_data_model_protein_domains() {
		Object[][] data = new Object[1][2];
		// create table model with data
		LoadPTMsTask.ptmTableDataModel = new DefaultTableModel(data, columnNames) {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isCellEditable(int row, int column) {
				return true;
			}

			@Override
			public Class<?> getColumnClass(int columnIndex) {
				return columnClass[columnIndex];
			}
		};
	}

	/**
	 * Parse protein domain file (tab or csv).
	 * 
	 * @param taskMonitor task monitor
	 * @throws Exception throw an exception if the file does not exist.
	 */
	private void parserFile(TaskMonitor taskMonitor) throws Exception {

		parserFile = new Parser(fileName);
		// Update data table model
		parserFile.updateDataModel(1);

		// Store PTM(s)
		LoadPTMsTask.storePTMs(taskMonitor, myNetwork, false);
	}

	private void getPTMsFromServer(TaskMonitor taskMonitor) throws Exception {

		List<String> names = Arrays.asList(nodesName.split(","));

		if (!names.contains("all")) {

			int countPtnDomain = 0;
			for (String name : names) {

				LoadPTMsTask.ptmTableDataModel.setValueAt(name, countPtnDomain, 0);
				countPtnDomain++;
			}

			LoadPTMsTask.getNodesFromTable(myNetwork, false);
		} else {
			LoadPTMsTask.getNodesFromTable(myNetwork, true);
		}

		// Get PTM(s) from server.
		// At the end, storePTMs will be called.
		LoadPTMsTask.getPTMsFromServer(taskMonitor, myNetwork, false);

	}

	@SuppressWarnings("unchecked")
	@Override
	public <R> R getResults(Class<? extends R> type) {
		return (R) "Done!";
	}

}
