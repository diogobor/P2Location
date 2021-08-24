package de.fmp.liulab.core;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.JLabel;

import org.cytoscape.work.TaskMonitor;

import de.fmp.liulab.model.CrossLink;
import de.fmp.liulab.model.Fasta;
import de.fmp.liulab.model.Protein;
import de.fmp.liulab.parser.ReaderWriterTextFile;
import de.fmp.liulab.utils.Tuple2;
import de.fmp.liulab.utils.Util;

/**
 * Class responsible for creating PyMOL script (*.pml file) and calling software
 * to be executed This class was adapted from CyToStruct, a Cytoscape app that
 * makes a bridge between molecular viewers and Cytoscape (doi:
 * 10.1016/j.str.2015.02.013)
 * 
 * @author diogobor
 *
 */
public class ProteinStructureManager {

	private static ReaderWriterTextFile parserFile;
	private static int proteinOffsetInPDBSource = -1;
	private static int proteinOffsetInPDBTarget = -1;
	private static Aligner align = new Aligner();

	public static void execUnix(String[] cmdarray, TaskMonitor taskMonitor) throws IOException {
		// instead of calling command directly, we'll call the shell
		Runtime rt = Runtime.getRuntime();
		File rDir = new File(System.getProperty("user.dir")).getAbsoluteFile();
		String cmd = cmdarray[0] + " " + cmdarray[1]; // should be exactly 2 elements
		String path = writeToTempAndGetPath("cd " + rDir.getAbsolutePath() + " \n " + cmd, "run_", "sh", taskMonitor);
		if (path.isEmpty()) {
			taskMonitor.showMessage(TaskMonitor.Level.ERROR, "Could not produce script");
		}
		String[] cmdA = { "sh", path };
		taskMonitor.showMessage(TaskMonitor.Level.INFO,
				"Executing: '" + cmdA[0] + "' '" + cmdA[1] + "' @" + rDir.getAbsolutePath());
		rt.exec(cmdA);
	}

	public static void execWindows(String[] cmdarray, TaskMonitor taskMonitor) throws IOException {
		String cmd = cmdarray[0] + " " + cmdarray[1]; // should be exactly 2 elements
		File rDir = new File(System.getProperty("user.dir")).getAbsoluteFile();
		ProcessBuilder pb = new ProcessBuilder("cmd", "/C", cmdarray[0], cmdarray[1]);
		pb.directory(rDir);

		taskMonitor.showMessage(TaskMonitor.Level.INFO, "Executing: '" + cmd + "' @ " + rDir.getAbsolutePath());
		pb.start();
	}

	// IMPORTED FROM CYTOSTRCUT PROJECT

	private static String writeToTempAndGetPath(String text, String prefix, String suffix, TaskMonitor taskMonitor) {
		File out = getTmpFile(prefix, suffix, taskMonitor);
		if (out == null) {
			taskMonitor.showMessage(TaskMonitor.Level.ERROR, "Error to create tmp file!");
			return "";
		}

		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(out));
			writer.write(text);
			writer.flush();
			writer.close();
		} catch (IOException ex) {
			taskMonitor.showMessage(TaskMonitor.Level.ERROR, "Error to create tmp file!");
		}
		return out.getAbsolutePath();
	}

	private static File getTmpFile(String prefix, String suffix, TaskMonitor taskMonitor) {
		File dr = new File(System.getProperty("java.io.tmpdir"), "cytoTmpScripts");
		if (dr.exists() || dr.mkdir()) {
			try {
				return File.createTempFile(prefix + "_scr_", "." + suffix, dr);
			} catch (IOException e) {
				taskMonitor.showMessage(TaskMonitor.Level.ERROR,
						"Could not work with tmp dir: " + dr.getAbsolutePath());
			}
		}
		return null;
	}

	/**
	 * Create PDB file (temporary file)
	 * 
	 * @param pdbID       pdb ID
	 * @param taskMonitor task monitor
	 * @return file name
	 */
	public static String createPDBFile(String pdbID, TaskMonitor taskMonitor) {

		String finalStr = "";
		File f = null;

		try {

			String[] returnFile = null;
			if (pdbID.startsWith("https://swissmodel.expasy.org/repository/")) {
				// Download file from SwissModel server
				returnFile = Util.getPDBfileFromSwissModelServer(pdbID, taskMonitor);

			} else {

				// Retrieving file from RCSB server
				// [PDB or CIF, file content]
				returnFile = Util.getPDBorCIFfileFromServer(pdbID, taskMonitor);
			}

			// write this script to tmp file and return path
			if (returnFile[0].equals("PDB"))
				f = getTmpFile(pdbID, "pdb", taskMonitor);
			else
				f = getTmpFile(pdbID, "cif", taskMonitor);

			if (f == null) {
				taskMonitor.showMessage(TaskMonitor.Level.ERROR, "Could not create temp file with label: " + pdbID);
				return "";
			}

			FileWriter bw;

			bw = new FileWriter(f);
			finalStr = returnFile[1];
			if (finalStr.isBlank() || finalStr.isEmpty())
				taskMonitor.showMessage(TaskMonitor.Level.ERROR,
						"Error retrieving the PDB file for protein " + pdbID + ".");
			bw.write(finalStr);
			bw.flush();
			bw.close();
		} catch (IOException ex) {
			taskMonitor.showMessage(TaskMonitor.Level.ERROR, "Problems while writing script to " + f.getAbsolutePath());
			finalStr = "";
		}

		if (finalStr.isBlank() || finalStr.isEmpty())
			return "ERROR";
		return f != null ? f.getAbsolutePath() : "ERROR";
	}

	/**
	 * Method responsible for creating PyMOL script (*.pml file) -> temp file
	 * 
	 * @param ptn                        protein
	 * @param crossLinks                 crosslinks
	 * @param taskMonitor                task monitor
	 * @param pdbFile                    pdb file name
	 * @param proteinSequenceFromPDBFile protein sequence in PDB file
	 * @param HasMoreThanOneChain        it has more than one chain in PDB file
	 * @param proteinChain               protein chain
	 * @return pymol script file name
	 */
	public static String createPyMOLScriptFile(Protein ptn, List<CrossLink> crossLinks, TaskMonitor taskMonitor,
			String pdbFile, String proteinSequenceFromPDBFile, boolean HasMoreThanOneChain, String proteinChain) {

		// write this script to tmp file and return path
		File f = getTmpFile(ptn.proteinID, "pml", taskMonitor);
		if (f == null) {
			taskMonitor.showMessage(TaskMonitor.Level.ERROR, "Could not create temp file with label: " + ptn.proteinID);
			return "ERROR";
		}

		FileWriter bw;
		String finalStr = "";
		try {

			if (proteinSequenceFromPDBFile.isBlank() || proteinSequenceFromPDBFile.isEmpty()) {
				taskMonitor.showMessage(TaskMonitor.Level.ERROR, "No sequence has been found in PDB file.");
				return "ERROR";
			}

			finalStr = createPyMOLScript(taskMonitor, ptn, crossLinks, pdbFile, HasMoreThanOneChain, proteinChain,
					proteinSequenceFromPDBFile);

			bw = new FileWriter(f);
			bw.write(finalStr);
			bw.flush();
			bw.close();
		} catch (IOException ex) {
			taskMonitor.showMessage(TaskMonitor.Level.ERROR, "Problems while writing script to " + f.getAbsolutePath());
			finalStr = "";
		}

		if (finalStr.isBlank() || finalStr.isEmpty())
			return "ERROR";

		return f.getAbsolutePath();
	}

	/**
	 * Method responsible for creating PyMOL script (*.pml file) -> temp file
	 * 
	 * @param ptnSource                          protein source
	 * @param crossLinks                         crosslinks
	 * @param taskMonitor                        task monitor
	 * @param pdbFile                            pdb file name
	 * @param proteinSequence_source_FromPDBFile protein sequence in PDB file
	 * @param HasMoreThanOneChain                it has more than one chain in PDB
	 *                                           file
	 * @param HasMoreThanOneChain                it has more than one chain in PDB
	 *                                           file
	 * @param proteinChain                       protein chain
	 * @return pymol script file name
	 */
	public static String createPyMOLScriptFile(Protein ptnSource, Protein ptnTarget, List<CrossLink> crossLinks,
			TaskMonitor taskMonitor, String pdbFile, String proteinSequence_source_FromPDBFile,
			String proteinSequence_target_FromPDBFile, boolean HasMoreThanOneChain_proteinSource,
			boolean HasMoreThanOneChain_proteinTarget, String proteinChain_source, String proteinChain_target,
			String nodeName_source, String nodeName_target) {

		// write this script to tmp file and return path
		File f = getTmpFile(ptnSource.proteinID + "_" + ptnTarget.proteinID, "pml", taskMonitor);
		if (f == null) {
			taskMonitor.showMessage(TaskMonitor.Level.ERROR,
					"Could not create temp file with label: " + ptnSource.proteinID + "_" + ptnTarget.proteinID);
			return "ERROR";
		}

		FileWriter bw;
		String finalStr = "";
		try {

			if (proteinSequence_source_FromPDBFile.isBlank() || proteinSequence_source_FromPDBFile.isEmpty()
					|| proteinSequence_target_FromPDBFile.isBlank() || proteinSequence_target_FromPDBFile.isEmpty()) {
				taskMonitor.showMessage(TaskMonitor.Level.ERROR, "No sequence has been found in PDB file.");
				return "ERROR";
			}

			finalStr = createPyMOLScript(taskMonitor, ptnSource, ptnTarget, crossLinks, pdbFile,
					HasMoreThanOneChain_proteinSource, HasMoreThanOneChain_proteinTarget, proteinChain_source,
					proteinChain_target, proteinSequence_source_FromPDBFile, proteinSequence_target_FromPDBFile,
					nodeName_source, nodeName_target);

			bw = new FileWriter(f);
			bw.write(finalStr);
			bw.flush();
			bw.close();
		} catch (IOException ex) {
			taskMonitor.showMessage(TaskMonitor.Level.ERROR, "Problems while writing script to " + f.getAbsolutePath());
			finalStr = "";
		}

		if (finalStr.isBlank() || finalStr.isEmpty())
			return "ERROR";

		return f.getAbsolutePath();
	}

	/**
	 * Method responsible for creating PyMOL script (*.pml file) -> temp file when
	 * the chain is unknown
	 * 
	 * @param ptn         protein
	 * @param crossLinks  crosslinks
	 * @param taskMonitor task monitor
	 * @param pdbFile     pdb file name
	 * @return return pymol file name or pdb chains
	 */
	public static String[] createPyMOLScriptFileUnknowChain(Protein ptn, List<CrossLink> crossLinks,
			TaskMonitor taskMonitor, String pdbFile, String pdbID) {

		// Check protein sequence
		if (ptn.sequence.isBlank() || ptn.sequence.isEmpty()) {
			taskMonitor.showMessage(TaskMonitor.Level.ERROR, "No sequence has been found in Uniprot.");
			return new String[] { "ERROR" };
		}

		// write this script to tmp file and return path
		File f = getTmpFile(ptn.proteinID, "pml", taskMonitor);
		if (f == null) {
			taskMonitor.showMessage(TaskMonitor.Level.ERROR, "Could not create temp file with label: " + ptn.proteinID);
			return new String[] { "ERROR" };
		}

		FileWriter bw;
		String finalStr = "";
		try {

			taskMonitor.showMessage(TaskMonitor.Level.INFO, "Getting protein sequence from PDB file...");

			boolean foundChain = true;
			String proteinSequenceFromPDBFile = "";
			boolean HasMoreThanOneChain = false;
			String proteinChain = getChainFromPDBFasta(ptn, pdbID, taskMonitor, pdbFile);
			if (proteinChain.isBlank() || proteinChain.isEmpty())
				foundChain = false;

			if (!foundChain) {

				// protein offset will be retrieved in the PDB / CIF file

				// [pdb protein sequence, protein chain, "true" -> there is more than one chain]
				String[] returnPDB = null;
				if (pdbFile.endsWith("pdb"))
					returnPDB = getProteinSequenceAndChainFromPDBFile(pdbFile, ptn, taskMonitor);
				else
					returnPDB = getChainFromCIFFile(pdbFile, ptn, taskMonitor);

				proteinSequenceFromPDBFile = returnPDB[0];
				HasMoreThanOneChain = returnPDB[2].equals("true") ? true : false;

				proteinChain = returnPDB[1];
				if (proteinChain.startsWith("CHAINS:")) {
					taskMonitor.showMessage(TaskMonitor.Level.WARN,
							"No chain matched with protein description. Select one chain...");
					f.delete();
					// return String[0-> 'CHAINS'; 1-> HasMoreThanOneChain; 2-> chains: separated by
					// '#']
					return new String[] { "CHAINS", returnPDB[2], proteinChain };
				}

				if (proteinSequenceFromPDBFile.isBlank() || proteinSequenceFromPDBFile.isEmpty()) {
					taskMonitor.showMessage(TaskMonitor.Level.ERROR, "No sequence has been found in PDB file.");
					return new String[] { "ERROR" };
				}
			} else {

				if (pdbFile.endsWith("pdb"))
					proteinSequenceFromPDBFile = ProteinStructureManager.getProteinSequenceFromPDBFileWithSpecificChain(
							pdbFile, ptn, taskMonitor, proteinChain, false);
				else
					proteinSequenceFromPDBFile = ProteinStructureManager.getProteinSequenceFromCIFFileWithSpecificChain(
							pdbFile, ptn, taskMonitor, proteinChain, false);
			}

			finalStr = createPyMOLScript(taskMonitor, ptn, crossLinks, pdbFile, HasMoreThanOneChain, proteinChain,
					proteinSequenceFromPDBFile);

			bw = new FileWriter(f);
			bw.write(finalStr);
			bw.flush();
			bw.close();
		} catch (IOException ex) {
			taskMonitor.showMessage(TaskMonitor.Level.ERROR, "Problems while writing script to " + f.getAbsolutePath());
			finalStr = "";
		}

		if (finalStr.isBlank() || finalStr.isEmpty())
			return new String[] { "ERROR" };

		return new String[] { f.getAbsolutePath() };
	}

	/**
	 * Get chain from the PDB fasta file
	 * 
	 * @param targetProteinSequence target sequence
	 * @param pdbID                 pdb identifier
	 * @param taskMonitor           task monitor
	 * @return chain
	 */
	public static String getChainFromPDBFasta(Protein ptn, String pdbID, TaskMonitor taskMonitor, String fileName) {

		List<Fasta> fastaList = Util.getProteinSequenceFromPDBServer(pdbID, taskMonitor);

		if (fastaList.size() == 0) {
			fastaList = getProteinSequencesFromPDBFile(fileName, ptn, taskMonitor);
		}

		String chain = "";
		if (fastaList.size() > 0) {

			for (Fasta fasta : fastaList) {

				int offset = fasta.sequence.indexOf(ptn.sequence);
				if (offset == -1)
					offset = ptn.sequence.indexOf(fasta.sequence);

				if (offset == -1) {// Performs alignment between sequences

					try {
						Tuple2 closestPeptideinProteinInfo = align
								.getClosestPeptideInASequence(fasta.sequence.toCharArray(), ptn.sequence.toCharArray());
						String closestPept = (String) closestPeptideinProteinInfo.getSecond();

						Tuple2 closestPeptInfo = null;

						if (fasta.sequence.length() > closestPept.length())
							closestPeptInfo = align.getClosestPeptideInASequence(closestPept.toCharArray(),
									fasta.sequence.toCharArray());
						else
							closestPeptInfo = align.getClosestPeptideInASequence(fasta.sequence.toCharArray(),
									closestPept.toCharArray());
						int indexClosestPet = (int) closestPeptInfo.getFirst();

						int countSameAA = 0;

						int limit = Math.min(closestPept.length(), fasta.sequence.length() - indexClosestPet);
						for (int i = indexClosestPet; i < limit; i++) {
							if (closestPept.toCharArray()[i] == fasta.sequence.toCharArray()[i])
								countSameAA++;
						}

						if (((double) countSameAA / (double) limit) > 0.4)
							offset = 0;
					} catch (Exception e) {
						continue;
					}

				}

				if (offset != -1) {

					// Example: >3J7Y_8|Chain J|uL11|Homo sapiens (9606)
					String[] cols = fasta.header.split("\\|");

					String[] chainCols = cols[1].split("Chain ");
					if (chainCols.length == 1) {// It means that there are more than one chain
						chainCols = cols[1].split("Chains ");
						chainCols = chainCols[1].split(",");
						chain = chainCols[0].trim();
					} else {
						chain = chainCols[1].trim();
					}
					if (proteinOffsetInPDBSource == -1 && fasta.offset != -1)
						proteinOffsetInPDBSource = fasta.offset;

					break;
				}
			}
		}
		return chain;
	}

	/**
	 * Get path and fileName of PDB file
	 * 
	 * @param pdbFile full path
	 * @return [0] -> path; [1] -> name
	 */
	private static String[] getPDBFilePathName(String pdbFile) {
		String separator = File.separator;
		String[] pdbPathAndFileName = null;
		if (Util.isWindows())
			pdbPathAndFileName = pdbFile.split(separator + separator);
		else
			pdbPathAndFileName = pdbFile.split(separator);
		StringBuilder pdbFilePath = new StringBuilder();
		for (int i = 0; i < pdbPathAndFileName.length - 1; i++) {
			pdbFilePath.append(pdbPathAndFileName[i] + separator);

		}
		String pdbFileName = pdbPathAndFileName[pdbPathAndFileName.length - 1];
		String path = pdbFilePath.toString().substring(0, pdbFilePath.length() - 1);

		return new String[] { path, pdbFileName };
	}

	/**
	 * Create the pml script
	 * 
	 * @param taskMonitor task monitor
	 * @return script
	 */
	private static String createPyMOLScript(TaskMonitor taskMonitor, Protein ptn, List<CrossLink> crossLinks,
			String pdbFile, boolean HasMoreThanOneChain, String proteinChain, String proteinSequenceFromPDBFile) {

		// [0]-> Path
		// [1]-> File name
		taskMonitor.showMessage(TaskMonitor.Level.INFO, "Getting PDB file name...");

		String[] pdbFilePathName = getPDBFilePathName(pdbFile);
		StringBuilder sbScript = new StringBuilder();
		sbScript.append("cd " + pdbFilePathName[0] + "\n");
		sbScript.append("load " + pdbFilePathName[1] + "\n");
		sbScript.append("set ignore_case, 0\n");
		sbScript.append("select chain_" + proteinChain + ", chain " + proteinChain + "\n");
		sbScript.append("color green, chain " + proteinChain + "\n");
		sbScript.append("hide all\n");

		taskMonitor.showMessage(TaskMonitor.Level.INFO, "Computing protein offset...");

		int offsetProtein = ptn.sequence.indexOf(proteinSequenceFromPDBFile);
		offsetProtein = offsetProtein > -1 ? offsetProtein : 0;
		String selectedResidueItem = "CA";

		if ((proteinOffsetInPDBSource - 1) == offsetProtein) {
			offsetProtein = 0;
		} else if (offsetProtein > 0) {
			offsetProtein = (proteinOffsetInPDBSource - 1) - offsetProtein > 0
					? (proteinOffsetInPDBSource - 1) - offsetProtein
					: offsetProtein;
		}

		StringBuilder sbDistances = new StringBuilder();
		StringBuilder sbPositions = new StringBuilder();

		for (CrossLink crossLink : crossLinks) {
			int pos1 = crossLink.pos_site_a + offsetProtein;
			int pos2 = crossLink.pos_site_b + offsetProtein;

			sbDistances.append("" + proteinChain + "/" + pos1 + "/" + selectedResidueItem + ", " + proteinChain + "/"
					+ pos2 + "/" + selectedResidueItem + "\n");
			sbPositions.append(proteinChain + "/" + pos1 + "\n").append(proteinChain + "/" + pos2 + "\n");
		}

		ArrayList<String> distancesList = (ArrayList<String>) Arrays.asList(sbDistances.toString().split("[\\n]"))
				.stream().distinct().collect(Collectors.toList());

		ArrayList<String> positionList = (ArrayList<String>) Arrays.asList(sbPositions.toString().split("[\\n]"))
				.stream().distinct().collect(Collectors.toList());

		int countXL = 1;
		for (String dist : distancesList) {
			sbScript.append("distance xl" + countXL + ", " + dist + "\n");
			countXL++;
		}

		sbScript.append("select a, res " + String.join("+", positionList) + ";\n");
		sbScript.append("set dash_width, 5\n");
		sbScript.append("set dash_length, 0.1\n");
		sbScript.append("set dash_color, [1.000, 1.000, 0.000]\n");
		sbScript.append("hide label\n");
		sbScript.append("deselect\n");
		sbScript.append("show sticks, a\n");
		sbScript.append("show cartoon, chain_" + proteinChain + "\n");
		sbScript.append("zoom chain_" + proteinChain + "\n");
		sbScript.append("deselect\n");

		return sbScript.toString();
	}

	/**
	 * Create the pml script
	 * 
	 * @param taskMonitor task monitor
	 * @return script
	 */
	private static String createPyMOLScript(TaskMonitor taskMonitor, Protein ptnSource, Protein ptnTarget,
			List<CrossLink> crossLinks, String pdbFile, boolean HasMoreThanOneChain_proteinSource,
			boolean HasMoreThanOneChain_proteinTarget, String proteinChain_source, String proteinChain_target,
			String proteinSequence_source_FromPDBFile, String proteinSequence_target_FromPDBFile,
			String nodeName_source, String nodeName_target) {

		// [0]-> Path
		// [1]-> File name
		taskMonitor.showMessage(TaskMonitor.Level.INFO, "Getting PDB file name...");

		String[] pdbFilePathName = getPDBFilePathName(pdbFile);
		StringBuilder sbScript = new StringBuilder();
		sbScript.append("cd " + pdbFilePathName[0] + "\n");
		sbScript.append("load " + pdbFilePathName[1] + "\n");
		sbScript.append("set ignore_case, 0\n");
		sbScript.append("select chain_" + proteinChain_source + ", chain " + proteinChain_source + "\n");
		sbScript.append("select chain_" + proteinChain_target + ", chain " + proteinChain_target + "\n");
		sbScript.append("hide all\n");

		taskMonitor.showMessage(TaskMonitor.Level.INFO, "Computing protein offset...");

		int offsetProteinSource = ptnSource.sequence.indexOf(proteinSequence_source_FromPDBFile);
		offsetProteinSource = offsetProteinSource > -1 ? offsetProteinSource : 0;
		String selectedResidueItem = "CA";

		if ((proteinOffsetInPDBSource - 1) == offsetProteinSource) {
			offsetProteinSource = 0;
		} else if (offsetProteinSource > 0) {
			offsetProteinSource = (proteinOffsetInPDBSource - 1) - offsetProteinSource;
		}

		int offsetProteinTarget = ptnTarget.sequence.indexOf(proteinSequence_target_FromPDBFile);
		offsetProteinTarget = offsetProteinTarget > -1 ? offsetProteinTarget : 0;

		if ((proteinOffsetInPDBTarget - 1) == offsetProteinTarget) {
			offsetProteinTarget = 0;
		} else if (offsetProteinTarget > 0) {
			offsetProteinTarget = (proteinOffsetInPDBTarget - 1) - offsetProteinTarget;
		}

		StringBuilder sbDistances = new StringBuilder();
		StringBuilder sbPositions = new StringBuilder();

		int offset_a = -1;
		int offset_b = -1;
		int pos1 = -1;
		int pos2 = -1;
		for (CrossLink crossLink : crossLinks) {

			String chain_a = "";
			String chain_b = "";
			if (crossLink.protein_a.equals(nodeName_source)) {
				chain_a = proteinChain_source;
				offset_a = offsetProteinSource;
			} else {
				chain_a = proteinChain_target;
				offset_a = offsetProteinTarget;
			}

			if (crossLink.protein_b.equals(nodeName_target)) {
				chain_b = proteinChain_target;
				offset_b = offsetProteinTarget;
			} else {
				chain_b = proteinChain_source;
				offset_b = offsetProteinSource;
			}

			pos1 = crossLink.pos_site_a + offset_a;
			pos2 = crossLink.pos_site_b + offset_b;

			sbDistances.append("" + chain_a + "/" + pos1 + "/" + selectedResidueItem + ", " + chain_b + "/" + pos2 + "/"
					+ selectedResidueItem + "\n");
			sbPositions.append(chain_a + "/" + pos1 + "\n").append(chain_b + "/" + pos2 + "\n");
		}

		ArrayList<String> distancesList = (ArrayList<String>) Arrays.asList(sbDistances.toString().split("[\\n]"))
				.stream().distinct().collect(Collectors.toList());

		ArrayList<String> positionList = (ArrayList<String>) Arrays.asList(sbPositions.toString().split("[\\n]"))
				.stream().distinct().collect(Collectors.toList());

		int countXL = 1;
		for (String dist : distancesList) {
			sbScript.append("distance xl" + countXL + ", " + dist + "\n");
			countXL++;
		}

		sbScript.append("select a, res " + String.join("+", positionList) + ";\n");
		sbScript.append("set dash_width, 5\n");
		sbScript.append("set dash_length, 0.1\n");
		sbScript.append("set dash_color, [1.000, 1.000, 0.000]\n");
		sbScript.append("hide label\n");
		sbScript.append("deselect\n");
		sbScript.append("show sticks, a\n");
		sbScript.append("show cartoon, chain_" + proteinChain_source + "\n");
		sbScript.append("color green, chain " + proteinChain_source + "\n");
		sbScript.append("show cartoon, chain_" + proteinChain_target + "\n");
		sbScript.append("color red, chain " + proteinChain_target + "\n");
		sbScript.append("orient chain_" + proteinChain_source + " chain_" + proteinChain_target + "\n");
		sbScript.append("deselect\n");

		return sbScript.toString();
	}

	/**
	 * Get protein sequence from PDB file
	 * 
	 * @param fileName      file name
	 * @param ptn           protein
	 * @param taskMonitor   task monitor
	 * @param protein_chain protein chain
	 * @return protein sequence
	 */
	public static String getProteinSequenceFromPDBFileWithSpecificChain(String fileName, Protein ptn,
			TaskMonitor taskMonitor, String protein_chain, boolean isProteinSource) {

		Map<ByteBuffer, Integer> ResiduesDict = Util.createResiduesDict();

		StringBuilder sbSequence = new StringBuilder();

		try {
			parserFile = new ReaderWriterTextFile(fileName);
			String line = "";
			String lastOffset = "-1";
			int threshold = 15;// qtd aminoacids
			int countAA = 0;
			proteinOffsetInPDBSource = -1;

			while (parserFile.hasLine()) {
				line = parserFile.getLine();
				if (!(line.equals(""))) {

					if (!line.startsWith("ATOM"))
						continue;

					// It starts with 'ATOM'

					String[] cols = line.split("\\s+");

					if (!cols[4].equals(protein_chain))
						continue;

					byte[] pdbResidue = cols[3].getBytes();// Residue -> three characters
					int newResidue = ResiduesDict.get(ByteBuffer.wrap(pdbResidue));
					String currentOffset = cols[5];

					if (!currentOffset.equals(lastOffset)) {
						byte[] _byte = new byte[1];
						_byte[0] = (byte) newResidue;
						String string = new String(_byte);
						sbSequence.append(string);
						countAA++;
						if (countAA > threshold) {
							break;
						}
					}
					lastOffset = currentOffset;

					if (proteinOffsetInPDBSource == -1) {
						proteinOffsetInPDBSource = Integer.parseInt(cols[5]);
					}
				}

			}
		} catch (Exception e) {
			taskMonitor.showMessage(TaskMonitor.Level.ERROR, "Problems while reading PDB file: " + fileName);
		}

		proteinOffsetInPDBTarget = proteinOffsetInPDBSource;

		if (isProteinSource)
			proteinOffsetInPDBSource = -1;
		else
			proteinOffsetInPDBTarget = -1;

		return sbSequence.toString();
	}

	/**
	 * Get protein sequence from CIF file
	 * 
	 * @param fileName      file name
	 * @param ptn           protein
	 * @param taskMonitor   task monitor
	 * @param protein_chain protein chain
	 * @return protein sequence
	 */
	public static String getProteinSequenceFromCIFFileWithSpecificChain(String fileName, Protein ptn,
			TaskMonitor taskMonitor, String protein_chain, boolean isProteinSource) {

		Map<ByteBuffer, Integer> ResiduesDict = Util.createResiduesDict();

		StringBuilder sbSequence = new StringBuilder();

		try {
			parserFile = new ReaderWriterTextFile(fileName);
			String line = "";
			String lastOffset = "-1";
			int threshold = 15;// qtd aminoacids
			int countAA = 0;
			proteinOffsetInPDBSource = -1;

			while (parserFile.hasLine()) {
				line = parserFile.getLine();
				if (!(line.equals(""))) {

					if (!line.startsWith("ATOM"))
						continue;

					// It starts with 'ATOM'

					String[] cols = line.split("\\s+");

					if (!cols[6].equals(protein_chain))
						continue;

					if (!(cols[5].length() == 3))
						continue;// It means that the ATOM is not a residue, it's a gene

					byte[] pdbResidue = cols[5].getBytes();// Residue -> three characters
					int newResidue = ResiduesDict.get(ByteBuffer.wrap(pdbResidue));
					String currentOffset = cols[5];

					if (!currentOffset.equals(lastOffset)) {
						byte[] _byte = new byte[1];
						_byte[0] = (byte) newResidue;
						String string = new String(_byte);
						sbSequence.append(string);
						countAA++;
						if (countAA > threshold) {
							break;
						}
					}
					lastOffset = currentOffset;

					if (proteinOffsetInPDBSource == -1) {
						proteinOffsetInPDBSource = Integer.parseInt(cols[7]);
					}
				}

			}
		} catch (Exception e) {
			taskMonitor.showMessage(TaskMonitor.Level.ERROR, "Problems while reading PDB file: " + fileName);
		}

		return sbSequence.toString();
	}

	/**
	 * Get protein sequence and chain from CIF file
	 * 
	 * @param fileName    file name
	 * @param taskMonitor task monitor
	 * @return [sequence, protein chain, hasMoreThanOneChain]
	 */
	public static String[] getChainFromCIFFile(String fileName, Protein ptn, TaskMonitor taskMonitor) {

		StringBuilder sbSequence = new StringBuilder();
		String protein_chain = "";
		Set<String> chainsSet = new HashSet<String>();
		StringBuilder sbProteinChains = new StringBuilder();
		sbProteinChains.append("CHAINS:");

		int countChains = 0;

		try {
			parserFile = new ReaderWriterTextFile(fileName);
			String line = "";
			proteinOffsetInPDBSource = -1;

			while (parserFile.hasLine()) {
				line = parserFile.getLine();
				if (!(line.equals(""))) {

					if (!line.startsWith("ATOM"))
						continue;

					String[] cols = line.split("\\s+");

					if (cols[5].length() == 3)// It means that the ATOM is a residue, not a gene
						chainsSet.add(cols[6]);
				}
			}
		} catch (Exception e) {
			taskMonitor.showMessage(TaskMonitor.Level.ERROR, "Problems while reading PDB file: " + fileName);
		}

		countChains = sbProteinChains.length();
		sbProteinChains.append(String.join("#", chainsSet));
		protein_chain = sbProteinChains.toString();

		if (countChains > 1) {
			return new String[] { sbSequence.toString(), protein_chain, "true" };
		} else
			return new String[] { sbSequence.toString(), protein_chain, "false" };

	}

	/**
	 * Get protein sequence from pdb file
	 * 
	 * @param fileName    file name
	 * @param ptn         protein
	 * @param taskMonitor task monitor
	 * @return
	 */
	public static List<Fasta> getProteinSequencesFromPDBFile(String fileName, Protein ptn, TaskMonitor taskMonitor) {

		Map<ByteBuffer, Integer> ResiduesDict = Util.createResiduesDict();
		StringBuilder sbSequence = new StringBuilder();
		String proteinChain = "";

		List<Fasta> fastaList = new ArrayList<Fasta>();

		try {
			parserFile = new ReaderWriterTextFile(fileName);
			String line = "";
			String lastOffset = "-1";

			int threshold = 15;// qtd aminoacids
			int countAA = 0;
			boolean getSequence = true;

			while (parserFile.hasLine()) {

				try {
					line = parserFile.getLine();
					if (!(line.equals(""))) {

						if (!line.startsWith("ATOM"))
							continue;

						String[] cols = line.split("\\s+");

						if (!getSequence) {
							if (cols[4].equals(proteinChain)) {
								continue;
							} else {
								getSequence = true;
								countAA = 0;
								sbSequence = new StringBuilder();
							}
						}

						proteinChain = cols[4];

						byte[] pdbResidue = cols[3].getBytes();// Residue -> three characters
						int newResidue = ResiduesDict.get(ByteBuffer.wrap(pdbResidue));
						String currentOffset = cols[5];

						if (!currentOffset.equals(lastOffset)) {

							byte[] _byte = new byte[1];
							_byte[0] = (byte) newResidue;
							String string = new String(_byte);
							sbSequence.append(string);
							countAA++;
							if (countAA > threshold) {
								getSequence = false;

								// Create Fasta

								int proteinOffsetInPDBSource = Integer.parseInt(cols[5]);
								String header = ">" + ptn.proteinID + "|Chain " + proteinChain + "|description";
								Fasta fasta = new Fasta(header, sbSequence.toString(), proteinOffsetInPDBSource);
								fastaList.add(fasta);
							}
						}
						lastOffset = currentOffset;

					}
				} catch (Exception e) {
				}
			}

		} catch (Exception e) {
			taskMonitor.showMessage(TaskMonitor.Level.ERROR, "Problems while reading PDB file: " + fileName);
			return new ArrayList<Fasta>();
		}

		return fastaList;
	}

	/**
	 * Get protein sequence and chain from PDB file
	 * 
	 * @param fileName    file name
	 * @param taskMonitor task monitor
	 * @return [sequence, protein chain, hasMoreThanOneChain]
	 */
	public static String[] getProteinSequenceAndChainFromPDBFile(String fileName, Protein ptn,
			TaskMonitor taskMonitor) {

		Map<ByteBuffer, Integer> ResiduesDict = Util.createResiduesDict();

		StringBuilder sbSequence = new StringBuilder();
		String protein_chain = "";
		StringBuilder sbProteinChains = new StringBuilder();
		sbProteinChains.append("CHAINS:");

		boolean hasMoreThanOneChain = false;
		int countChains = 0;

		try {
			parserFile = new ReaderWriterTextFile(fileName);
			String line = "";
			int lastInsertedResidue = 0;
			int threshold = 10;// qtd aminoacids
			int countAA = 0;
			proteinOffsetInPDBSource = -1;

			boolean isCompleteFullName = false;
			StringBuilder sbProteinFullName = new StringBuilder();

			while (parserFile.hasLine()) {
				line = parserFile.getLine();
				if (!(line.equals(""))) {

					if (!line.startsWith("COMPND") && !line.startsWith("ATOM"))
						continue;

					if (line.startsWith("COMPND")) {
						String[] cols = line.split("\\s+");

						boolean isNumeric = false;
						try {
							Double.parseDouble(cols[1]);
							isNumeric = true;
						} catch (NumberFormatException nfe) {
						}

						if (isNumeric) {

							// Get protein full name
							if (cols[2].equals("MOLECULE:")) {

								for (int i = 3; i < cols.length; i++) {
									sbProteinFullName.append(cols[i] + " ");
								}

								if (cols[cols.length - 1].endsWith(";")) {
									isCompleteFullName = true;
									continue;
								}

							} else if (!isCompleteFullName) {
								for (int i = 2; i < cols.length; i++) {
									sbProteinFullName.append(cols[i] + " ");
								}

								if (cols[cols.length - 1].endsWith(";")) {
									isCompleteFullName = true;
									continue;
								}
							}

							if (isCompleteFullName) {
								if (cols[2].equals("CHAIN:")) {
									countChains++;

									String pdbProteinFullName_moleculeField = sbProteinFullName.toString()
											.replace(';', ' ').trim();

									String[] fullNames = pdbProteinFullName_moleculeField.split(",");
									if (fullNames.length == 1) {
										if (ptn.fullName.toLowerCase()
												.equals(pdbProteinFullName_moleculeField.toLowerCase())) {
											protein_chain = cols[3].toString().replace(';', ' ').replace(',', ' ')
													.trim();
											continue;
										}

									} else { // There is only one molecule, but with more than one chain for this ptn
												// full name

										hasMoreThanOneChain = true;
										for (String fullName : fullNames) {
											if (ptn.fullName.toLowerCase().equals(fullName.toLowerCase().trim())) {
												protein_chain = cols[3].toString().replace(';', ' ').trim()
														.split(",")[0].trim();
												break;
											}
										}
									}

									if ((cols.length - 3) > 1) // Check if 'CHAIN' field has more than 1 chain: CHAIN:
																// A,B
										hasMoreThanOneChain = true;

									for (int i = 3; i < cols.length; i++) {

										sbProteinChains.append(
												cols[i].toString().replace(',', ' ').replace(';', ' ').trim() + "#");
									}

								}
							}

						} else {
							continue;
						}

					} else {// It starts with 'ATOM'

						String[] cols = line.split("\\s+");

						if (!cols[4].equals(protein_chain))
							continue;

						byte[] pdbResidue = cols[3].getBytes();// Residue -> three characters
						int newResidue = ResiduesDict.get(ByteBuffer.wrap(pdbResidue));

						if (newResidue != lastInsertedResidue) {
							byte[] _byte = new byte[1];
							_byte[0] = (byte) newResidue;
							String string = new String(_byte);
							sbSequence.append(string);
							countAA++;
							if (countAA > threshold) {
								break;
							}
						}
						lastInsertedResidue = newResidue;

						if (proteinOffsetInPDBSource == -1) {
							proteinOffsetInPDBSource = Integer.parseInt(cols[5]);
						}
					}

				}
			}
		} catch (Exception e) {
			taskMonitor.showMessage(TaskMonitor.Level.ERROR, "Problems while reading PDB file: " + fileName);
		}

		if (protein_chain.isBlank() || protein_chain.isEmpty())
			protein_chain = sbProteinChains.toString();

		if (countChains > 1 || hasMoreThanOneChain) {
			return new String[] { sbSequence.toString(), protein_chain, "true" };
		} else
			return new String[] { sbSequence.toString(), protein_chain, "false" };

	}

	/**
	 * Method responsible for executing PyMOL
	 * 
	 * @param taskMonitor     task monitor
	 * @param pymolScriptFile pymol script file (*.pml)
	 */
	public static void executePyMOL(TaskMonitor taskMonitor, String pymolScriptFile, JLabel textLabel_status_result) {

		if (textLabel_status_result != null)
			textLabel_status_result.setText("Executing PyMOL script...");
		taskMonitor.showMessage(TaskMonitor.Level.INFO, "Executing PyMOL script...");

		String[] cmdArray = new String[2];
		if (Util.isWindows())
			cmdArray[0] = Util.PYMOL_PATH;
		else if (Util.isMac())
			cmdArray[0] = "open " + "\"" + Util.PYMOL_PATH + "\"";
		else
			cmdArray[0] = "\"" + Util.PYMOL_PATH + "\"";
		cmdArray[1] = pymolScriptFile;

		try {
			if (Util.isWindows())
				ProteinStructureManager.execWindows(cmdArray, taskMonitor);
			else
				ProteinStructureManager.execUnix(cmdArray, taskMonitor);
		} catch (IOException e) {
			if (textLabel_status_result != null)
				textLabel_status_result.setText("WARNING: Check Task History.");
			taskMonitor.showMessage(TaskMonitor.Level.ERROR, "Error when running PyMOL.");
		}
	}
}
