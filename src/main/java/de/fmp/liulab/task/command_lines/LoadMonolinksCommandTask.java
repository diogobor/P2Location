package de.fmp.liulab.task.command_lines;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.work.ProvidesTitle;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;

import de.fmp.liulab.parser.Parser;

public class LoadMonolinksCommandTask extends CyRESTAbstractTask {

	private CyNetwork myNetwork;
	private Parser parserFile;

	private String[] columnNames = { "Node Name", "Sequence", "Monolink(s)" };
	private final Class[] columnClass = new Class[] { String.class, String.class , String.class };

	@ProvidesTitle
	public String getTitle() {
		return "Load Monolinked peptide(s)";
	}

	@Tunable(description = "Monolinked peptide(s) file name", longDescription = "Name of the Monolinked peptide(s) file. (Supported formats: *.csv)", exampleStringValue = "monolinks.csv")
	public String fileName = "";

	/**
	 * Constructor
	 */
	public LoadMonolinksCommandTask(CyApplicationManager cyApplicationManager) {
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

	}

	/**
	 * Method responsible for initializing table model of protein domains
	 */
	private void init_table_data_model_protein_domains() {
		Object[][] data = new Object[1][2];
		// create table model with data
//		LoadProteinLocationTask.monolinkTableDataModel = new DefaultTableModel(data, columnNames) {
//			/**
//			 * 
//			 */
//			private static final long serialVersionUID = 1L;
//
//			@Override
//			public boolean isCellEditable(int row, int column) {
//				return true;
//			}
//
//			@Override
//			public Class<?> getColumnClass(int columnIndex) {
//				return columnClass[columnIndex];
//			}
//			
//			@Override
//			public void setValueAt(Object data, int row, int column) {
//				if (column == 1 || column == 2)
//					super.setValueAt(data.toString().toUpperCase(), row, column);
//				else
//					super.setValueAt(data, row, column);
//			}
//		};
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
		parserFile.updateDataModel(2);

		// Store PTM(s)
//		LoadProteinLocationTask.storeMonolinks(taskMonitor, myNetwork, false);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <R> R getResults(Class<? extends R> type) {
		return (R) "Done!";
	}

}
