package de.fmp.liulab.parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.fmp.liulab.task.LoadPTMsTask;
import de.fmp.liulab.task.ProcessProteinLocationTask;

/**
 * Class responsible for parsing input files
 * 
 * @author diogobor
 *
 */
public class Parser {

	private ReaderWriterTextFile parserFile;
	private List<String> qtdParser = new ArrayList<String>();
	private String[] columnNames = { "Node Name", "Sequence", "Domain(s)" };
	private String[] columnNamesPTMTable = { "Node Name", "PTM(s)" };
	private String[] columnNamesMonolinksTable = { "Node Name", "Sequence", "Monolink(s)" };

	/**
	 * UNIPROT lines
	 */
	private String[] uniprot_header_lines;
	private boolean isUniprot;
	private boolean isCustomizedCSV;

	/**
	 * Constructor
	 * 
	 * @param fileName file name
	 */
	public Parser(String fileName) {

		try {
			parserFile = new ReaderWriterTextFile(fileName);
			isUniprot = false;
			if (fileName.endsWith(".tab")) {
				parserFile.hasLine();// Load current line to 'line' variable
				uniprot_header_lines = parserFile.getLine().split("\t");// Get the current content in 'line' variable
				isUniprot = true;
				isCustomizedCSV = false;
			}

			while (parserFile.hasLine()) {
				if (!(parserFile.getLine().equals(""))) {
					if (parserFile.getLine().contains(",Topological domain,")) {
						uniprot_header_lines = parserFile.getLine().split(",");
						isUniprot = true;
						isCustomizedCSV = true;
						continue;
					}
					
					qtdParser.add(parserFile.getLine());
				}
			}
		} catch (Exception e) {
		}
	}

	/**
	 * Method responsible for updating data model table
	 * 
	 * @param domain_ptm_or_monolink indicate if the method was called by
	 *                               ProteinDomain (0) or PTM (1) or Monolink (2)
	 * @throws Exception
	 */
	public void updateDataModel(int domain_ptm_or_monolink) throws Exception {

		StringBuilder sb_data_to_be_stored = new StringBuilder();

		for (String line : qtdParser) {
			try {

				// ##### Load Protein Domains ########
				if (domain_ptm_or_monolink == 0) {

					String gene_name = "";
					String sequence = "";
					StringBuilder domainsSB = new StringBuilder();

					if (isUniprot) {

						String splitter = "\t";
						if (isCustomizedCSV)
							splitter = ",";

						String[] each_line_cols = line.split(splitter);

						int index = Arrays.asList(uniprot_header_lines).indexOf("Gene names");
						if (index == -1)
							index = Arrays.asList(uniprot_header_lines).indexOf("Gene names  (primary )");
						if (index == -1)
							index = Arrays.asList(uniprot_header_lines).indexOf("Gene names  (synonym )");
						if (index == -1)
							index = Arrays.asList(uniprot_header_lines).indexOf("Gene names  (ORF )");
						if (index == -1)
							index = Arrays.asList(uniprot_header_lines).indexOf("Gene names  (ordered locus )");
						if (index == -1)
							return;

						if (index < each_line_cols.length)
							gene_name = each_line_cols[index];

						index = Arrays.asList(uniprot_header_lines).indexOf("Topological domain");
						if (index != -1) {
							String col_topological_transmembrane_intramembrane_domains = "";
							if (index < each_line_cols.length)
								col_topological_transmembrane_intramembrane_domains = each_line_cols[index];

							String[] cols_topol_domains = col_topological_transmembrane_intramembrane_domains
									.split(";");// e.g. TOPO_DOM 1..83;
												// /note="Cytoplasmic";
												// /evidence="ECO:0000255";

							if (domainsSB.length() == 0)
								domainsSB.append(uniprotParser(cols_topol_domains, "TOPO_DOM "));
							else {
								String currentDomains = uniprotParser(cols_topol_domains, "TOPO_DOM ");
								if (!currentDomains.isEmpty()) {
									domainsSB.append(",");
									domainsSB.append(currentDomains);
								}
							}
						}

						index = Arrays.asList(uniprot_header_lines).indexOf("Transmembrane");
						if (index != -1) {
							String col_topological__transmembrane_intramembrane_domains = "";
							if (index < each_line_cols.length)
								col_topological__transmembrane_intramembrane_domains = each_line_cols[index];

							String[] cols_topol_domains = col_topological__transmembrane_intramembrane_domains
									.split(";");// e.g. TRANSMEM 1..83;
												// /note="Cytoplasmic";
												// /evidence="ECO:0000255";

							if (domainsSB.length() == 0)
								domainsSB.append(uniprotParser(cols_topol_domains, "TRANSMEM "));
							else {
								String currentDomains = uniprotParser(cols_topol_domains, "TRANSMEM ");
								if (!currentDomains.isEmpty()) {
									domainsSB.append(",");
									domainsSB.append(currentDomains);
								}
							}
						}

						index = Arrays.asList(uniprot_header_lines).indexOf("Intramembrane");
						if (index != -1) {
							String col_topological__transmembrane_intramembrane_domains = "";
							if (index < each_line_cols.length)
								col_topological__transmembrane_intramembrane_domains = each_line_cols[index];

							String[] cols_topol_domains = col_topological__transmembrane_intramembrane_domains
									.split(";");// e.g. TRANSMEM 1..83;
												// /note="Cytoplasmic";
												// /evidence="ECO:0000255";

							if (domainsSB.length() == 0)
								domainsSB.append(uniprotParser(cols_topol_domains, "INTRAMEM "));
							else {
								String currentDomains = uniprotParser(cols_topol_domains, "INTRAMEM ");
								if (!currentDomains.isEmpty()) {
									domainsSB.append(",");
									domainsSB.append(currentDomains);
								}
							}
						}

						index = Arrays.asList(uniprot_header_lines).indexOf("Sequence");
						if (index != -1) {
							sequence = each_line_cols[index];
						}

					} else {
						int firstComma = line.indexOf(',');
						gene_name = line.substring(0, firstComma);
						domainsSB.append(line.substring(firstComma + 1).replace('\"', ' ').trim());
					}

					String[] cols_gene = gene_name.split(" ");
					for (String each_gene : cols_gene) {
						if (each_gene.isBlank() || each_gene.isEmpty() || each_gene.trim().equals("\t"))
							continue;
						sb_data_to_be_stored.append(each_gene);
						sb_data_to_be_stored.append("\t");
						sb_data_to_be_stored.append(sequence);
						sb_data_to_be_stored.append("\t");
						sb_data_to_be_stored.append(domainsSB).append("\n");
					}
				}
				// #### Load PTMs #####
				else if (domain_ptm_or_monolink == 1) {
					int firstComma = line.indexOf(',');
					String gene_name = line.substring(0, firstComma);
					String ptms = line.substring(firstComma + 1).replace('\"', ' ').trim();

					String[] cols_ptm = gene_name.split(" ");
					for (String each_ptm : cols_ptm) {
						if (each_ptm.isBlank() || each_ptm.isEmpty() || each_ptm.trim().equals("\t"))
							continue;
						sb_data_to_be_stored.append(each_ptm);
						sb_data_to_be_stored.append("\t");
						sb_data_to_be_stored.append(ptms).append("\n");
					}

				}
				// #### Load Monolinks ####
				else if (domain_ptm_or_monolink == 2) {
					int firstComma = line.indexOf(',');
					String gene_name = line.substring(0, firstComma);
					int secondComma = line.indexOf(',', firstComma + 1);
					String sequence = line.substring(firstComma + 1, secondComma);
					String monolinks = line.substring(secondComma + 1).replace('\"', ' ').trim();

					String[] cols_monolinks = gene_name.split(" ");
					for (String each_monolink : cols_monolinks) {
						if (each_monolink.isBlank() || each_monolink.isEmpty() || each_monolink.trim().equals("\t"))
							continue;
						sb_data_to_be_stored.append(each_monolink);
						sb_data_to_be_stored.append("\t");
						sb_data_to_be_stored.append(sequence);
						sb_data_to_be_stored.append("\t");
						sb_data_to_be_stored.append(monolinks).append("\n");
					}

				}
			} catch (Exception e) {
			}
		}

		if (qtdParser.size() == 0)
			throw new Exception("ERROR: There is an error reading the file.");

		int countPtnDomain = 0;
		String[] data_to_be_stored = sb_data_to_be_stored.toString().split("\n");

		Object[][] data = new Object[data_to_be_stored.length][3];
		if (domain_ptm_or_monolink == 1)
			LoadPTMsTask.ptmTableDataModel.setDataVector(data, columnNamesPTMTable);
//		else if (domain_ptm_or_monolink == 2)
//			LoadProteinLocationTask.monolinkTableDataModel.setDataVector(data, columnNamesMonolinksTable);
		else
			ProcessProteinLocationTask.tableDataModel.setDataVector(data, columnNames);

		for (String line : data_to_be_stored) {
			try {
				String[] cols_line = line.split("\t");

				if (domain_ptm_or_monolink == 1)
					LoadPTMsTask.ptmTableDataModel.setValueAt(cols_line[0], countPtnDomain, 0);
//				else if (domain_ptm_or_monolink == 2)
//					LoadProteinLocationTask.monolinkTableDataModel.setValueAt(cols_line[0], countPtnDomain, 0);
				else
					ProcessProteinLocationTask.tableDataModel.setValueAt(cols_line[0], countPtnDomain, 0);

				if (cols_line.length > 1) {
					if (domain_ptm_or_monolink == 1)
						LoadPTMsTask.ptmTableDataModel.setValueAt(cols_line[1], countPtnDomain, 1);
					else if (domain_ptm_or_monolink == 2) {
//						LoadProteinLocationTask.monolinkTableDataModel.setValueAt(cols_line[1], countPtnDomain, 1);
//						if (cols_line.length > 2)
//							LoadProteinLocationTask.monolinkTableDataModel.setValueAt(cols_line[2], countPtnDomain, 2);

					} else {
						ProcessProteinLocationTask.tableDataModel.setValueAt(cols_line[1], countPtnDomain, 1);
						ProcessProteinLocationTask.tableDataModel.setValueAt(cols_line[2], countPtnDomain, 2);
					}
				}

			} catch (Exception e) {
				System.out.println();
			} finally {
				countPtnDomain++;
			}

		}

		if (domain_ptm_or_monolink == 1)
			LoadPTMsTask.setTableProperties(countPtnDomain);
//		else if (domain_ptm_or_monolink == 2)
//			LoadProteinLocationTask.setTableProperties(countPtnDomain);
		else
			ProcessProteinLocationTask.setTableProperties(countPtnDomain);
	}

	private String uniprotParser(String[] cols_topol_domains, String tag) {
		StringBuilder sb_domains = new StringBuilder();
		if (cols_topol_domains.length > 1) {
			for (int i = 0; i < cols_topol_domains.length; i++) {
				String protein_domain_name = cols_topol_domains[i + 1].replace("/note=\"", "").replace("\"", "")
						.replace(",", "-").trim();
				if (protein_domain_name.contains("evidence"))
					protein_domain_name = "";

				String[] cols_range = cols_topol_domains[i].replace(tag, "").split("\\.");
				int start_index = Integer.parseInt(cols_range[0].trim());
				int end_index = 0;
				if (cols_range.length == 1) {
					end_index = start_index;
				} else {
					end_index = Integer.parseInt(cols_range[2].trim());
				}
				sb_domains.append(tag);
				if (!(protein_domain_name.isBlank() || protein_domain_name.isEmpty())) {
					sb_domains.append("- ");
					sb_domains.append(protein_domain_name);
				}
				sb_domains.append("[");
				sb_domains.append(start_index);
				sb_domains.append("-");
				sb_domains.append(end_index);
				sb_domains.append("],");

				if (i + 2 < cols_topol_domains.length && cols_topol_domains[i + 2].contains("evidence"))
					i += 2;
				else
					i += 1;

			}
		}
		if (sb_domains.length() > 0)
			return sb_domains.toString().substring(0, sb_domains.toString().length() - 1);
		else
			return "";
	}

}
