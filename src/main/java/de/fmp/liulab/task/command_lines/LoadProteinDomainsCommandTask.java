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
import de.fmp.liulab.task.ProcessProteinLocationTask;
import de.fmp.liulab.utils.Util;

/**
 * Class responsible for loading protein domains via command line
 * 
 * @author diogobor
 *
 */
public class LoadProteinDomainsCommandTask extends CyRESTAbstractTask {

	private CyNetwork myNetwork;
	private Parser parserFile;

	private String[] columnNames = { "Node Name", "Sequence", "Topological Domain(s)", "Subcellular location" };
	private final Class[] columnClass = new Class[] { String.class, String.class, String.class, String.class };

	@ProvidesTitle
	public String getTitle() {
		return "Load protein domains";
	}

	@Tunable(description = "Protein domains file name", longDescription = "Name of the protein domain file. (Supported formats: *.tab and *.csv)", exampleStringValue = "proteinDomains.csv")
	public String fileName = "";

	@Tunable(description = "Node(s) to get domain", longDescription = "Give the node(s) name, separated by comma, to get domains. (type 'all' to get domains of all nodes)", exampleStringValue = "PDE12")
	public String nodesName = "";

	@Tunable(description = "Set server to get domain(s).", longDescription = "Set which server the protein domains will be retrieved. (true = Pfam, false = Supfam)", exampleStringValue = "true")
	public boolean fromPfamServer = Util.isProteinDomainPfam;

	/**
	 * Constructor
	 */
	public LoadProteinDomainsCommandTask(CyApplicationManager cyApplicationManager) {
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
			this.getProteinDomainsFromServer(taskMonitor);

	}

	/**
	 * Method responsible for initializing table model of protein domains
	 */
	private void init_table_data_model_protein_domains() {
		Object[][] data = new Object[1][columnNames.length];
		// create table model with data
		ProcessProteinLocationTask.tableDataModel = new DefaultTableModel(data, columnNames) {
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
		parserFile.updateDataModel(0);

		// Store protein domains
		ProcessProteinLocationTask.storeProteins(taskMonitor, myNetwork, false);
	}

	private void getProteinDomainsFromServer(TaskMonitor taskMonitor) throws Exception {

		List<String> names = Arrays.asList(nodesName.split(","));

		if (!names.contains("all")) {

			int countPtnDomain = 0;
			for (String name : names) {

				ProcessProteinLocationTask.tableDataModel.setValueAt(name, countPtnDomain, 0);
				countPtnDomain++;
			}

			// Fill geneListFromTable variable with gene and protein domains
			// If nodesName == 'all', table data model will be empty and getNodesFromTable
			// will retrieve all nodes.
			ProcessProteinLocationTask.getNodesFromTable(myNetwork, false, false);
		} else {
			ProcessProteinLocationTask.getNodesFromTable(myNetwork, true, false);
		}

		// Get protein domains from server.
		// At the end, storeProteinDomains will be called.
		Util.isProteinDomainPfam = fromPfamServer;
		ProcessProteinLocationTask.getProteinDomainsFromServer(taskMonitor, myNetwork, false);

	}

	@SuppressWarnings("unchecked")
	@Override
	public <R> R getResults(Class<? extends R> type) {
		return (R) "Done!";
	}

}
