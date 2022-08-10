package de.fmp.liulab.core;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.JLabel;

import org.cytoscape.work.TaskMonitor;

import de.fmp.liulab.utils.Util;

public class PythonManager {

	public static void execUnix(String cmdarray, TaskMonitor taskMonitor) throws IOException, InterruptedException {
		// instead of calling command directly, we'll call the shell
		File rDir = new File(System.getProperty("user.dir")).getAbsoluteFile();
		String[] cmdA = { "sh", cmdarray };
		taskMonitor.showMessage(TaskMonitor.Level.INFO,
				"Executing: '" + cmdA[0] + "' '" + cmdA[1] + "' @" + rDir.getAbsolutePath());

		Process p = Runtime.getRuntime().exec(cmdA);
		// waits for the process until you terminate.
		p.waitFor();

	}

	public static void execWindows(String cmdarray, TaskMonitor taskMonitor) throws IOException, InterruptedException {
		String cmd = cmdarray;
		File rDir = new File(System.getProperty("user.dir")).getAbsoluteFile();
		ProcessBuilder pb = new ProcessBuilder("cmd", "/C", cmdarray);
		pb.directory(rDir);

		taskMonitor.showMessage(TaskMonitor.Level.INFO, "Executing: '" + cmd + "' @ " + rDir.getAbsolutePath());
		pb.start();
		pb.wait();
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
	 * Create python calling file (temporary file)
	 * 
	 * @param taskMonitor task monitor
	 * @return file name
	 */
	public static String createPythonShell(String run_python_full_path, TaskMonitor taskMonitor) {
		String run_python = run_python_full_path;

		if (Util.isWindows())
			run_python = "py " + run_python;
		else
			run_python = "python3 " + run_python;

		File f = null;

		try {

			f = getTmpFile("run_python", "sh", taskMonitor);
			FileWriter bw;

			bw = new FileWriter(f);
			bw.write(run_python);
			bw.flush();
			bw.close();
		} catch (IOException ex) {
			run_python = "";
		}

		if (run_python.isBlank() || run_python.isEmpty())
			return "ERROR";
		return f != null ? f.getAbsolutePath() : "ERROR";
	}

	/**
	 * Create install biolib library file (temporary file)
	 * 
	 * @param pdbID       pdb ID
	 * @param taskMonitor task monitor
	 * @return file name
	 */
	public static String createInstallBiolibLib(TaskMonitor taskMonitor) {

		String run_python = "pip3 install pybiolib";
		File f = null;

		try {

			f = getTmpFile("install_pybiolib", "sh", taskMonitor);
			FileWriter bw;

			bw = new FileWriter(f);
			bw.write(run_python);
			bw.flush();
			bw.close();
		} catch (IOException ex) {
			run_python = "";
		}

		if (run_python.isBlank() || run_python.isEmpty())
			return "ERROR";
		return f != null ? f.getAbsolutePath() : "ERROR";
	}

	/**
	 * Create python calling file (temporary file)
	 * 
	 * @param taskMonitor task monitor
	 * @return [python script path, results folder path]
	 */
	public static String[] createPythonCalling(String fastaFileFullPath, TaskMonitor taskMonitor) {

		long num = Util.random.nextLong();
		String results_num = Long.toUnsignedString(num);

		String results_tmp_folder = "results_" + results_num;
		String python_tmp_folder = System.getProperty("java.io.tmpdir") + "/cytoTmpScripts/";
		StringBuilder sb_run_python = new StringBuilder();
		sb_run_python.append("import biolib\n");
		sb_run_python.append("deeptmhmm = biolib.load('DTU/DeepTMHMM')\n");
		sb_run_python.append("\n");
		sb_run_python.append("deeptmhmm_res = deeptmhmm.cli(args='--fasta " + fastaFileFullPath + "')\n");
		sb_run_python.append("deeptmhmm_res.save_files(\"" + python_tmp_folder + results_tmp_folder + "\")");

		String run_python = sb_run_python.toString();
		File f = null;

		try {

			f = getTmpFile("run_python", "py", taskMonitor);
			FileWriter bw;

			bw = new FileWriter(f);
			bw.write(run_python);
			bw.flush();
			bw.close();
		} catch (IOException ex) {
			run_python = "";
		}

		if (run_python.isBlank() || run_python.isEmpty())
			return new String[] { "ERROR", "" };
		return f != null ? new String[] { f.getAbsolutePath(), results_tmp_folder } : new String[] { "ERROR", "" };
	}

	/**
	 * Create fasta file (temporary file)
	 * 
	 * @param taskMonitor task monitor
	 * @return file name
	 */
	public static String createFastaFile(String fastaInformation, TaskMonitor taskMonitor) {

		String run_python = fastaInformation;
		File f = null;

		try {

			f = getTmpFile("input", "fasta", taskMonitor);
			FileWriter bw;

			bw = new FileWriter(f);
			bw.write(run_python);
			bw.flush();
			bw.close();
		} catch (IOException ex) {
			run_python = "";
		}

		if (run_python.isBlank() || run_python.isEmpty())
			return "ERROR";
		return f != null ? f.getAbsolutePath() : "ERROR";
	}

	/**
	 * Method responsible for executing PyMOL
	 * 
	 * @param taskMonitor           task monitor
	 * @param python_run_scriptFile pymol script file (*.pml)
	 */
	public static void executePython(TaskMonitor taskMonitor, String python_run_scriptFile,
			JLabel textLabel_status_result) {

		if (textLabel_status_result != null)
			textLabel_status_result.setText("Executing python...");
		taskMonitor.showMessage(TaskMonitor.Level.INFO, "Executing python...");

		try {
			if (Util.isWindows())
				execWindows(python_run_scriptFile, taskMonitor);
			else
				execUnix(python_run_scriptFile, taskMonitor);
		} catch (IOException e) {
			if (textLabel_status_result != null)
				textLabel_status_result.setText("WARNING: Check Task History.");
			taskMonitor.showMessage(TaskMonitor.Level.ERROR, "Error when running python.");
		} catch (InterruptedException e) {
			if (textLabel_status_result != null)
				textLabel_status_result.setText("WARNING: Check Task History.");
			taskMonitor.showMessage(TaskMonitor.Level.ERROR, "Error when running python.");
		}
	}

}
