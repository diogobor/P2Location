package de.fmp.liulab.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import de.fmp.liulab.utils.Tuple2;

/**
 * Class responsible for performing alignment between protein sequences
 * 
 * @author diogobor
 *
 */
public class Aligner {

	int[][] SubstitutionMatrix;
	Map<Character, Integer> SubstitutionMatrixPos;

	/**
	 * Empty constructor
	 */
	public Aligner() {

		String PAM30MS = "A R N D C Q E G H I L K M F P S T W Y V B Z J X U *\n"
				+ "A 6 -7 -4 -3 -6 -4 -2 -2 -7 -5 -6 -7 -5 -8 -2 0 -1 -13 -8 -2 -7 -6 0 0 0 -17\n"
				+ "R -7 8 -6 -10 -8 -2 -9 -9 -2 -5 -7 0 -4 -9 -4 -3 -6 -2 -10 -8 5 -1 0 0 0 -17\n"
				+ "N -4 -6 8 2 -11 -3 -2 -3 0 -5 -6 -1 -9 -9 -6 0 -2 -8 -4 -8 -4 -2 0 0 0 -17\n"
				+ "D -3 -10 2 8 -14 -2 2 -3 -4 -7 -10 -4 -11 -15 -8 -4 -5 -15 -11 -8 -7 -3 0 0 0 -17\n"
				+ "C -6 -8 -11 -14 10 -14 -14 -9 -7 -6 -11 -14 -13 -13 -8 -3 -8 -15 -4 -6 -11 -14 0 0 0 -17\n"
				+ "Q -4 -2 -3 -2 -14 8 1 -7 1 -8 -7 -3 -4 -13 -3 -5 -5 -13 -12 -7 -3 4 0 0 0 -17\n"
				+ "E -2 -9 -2 2 -14 1 8 -4 -5 -5 -7 -4 -7 -14 -5 -4 -6 -17 -8 -6 -7 -2 0 0 0 -17\n"
				+ "G -2 -9 -3 -3 -9 -7 -4 6 -9 -11 -11 -7 -8 -9 -6 -2 -6 -15 -14 -5 -8 -7 0 0 0 -17\n"
				+ "H -7 -2 0 -4 -7 1 -5 -9 9 -9 -8 -6 -10 -6 -4 -6 -7 -7 -3 -6 -4 -3 0 0 0 -17\n"
				+ "I -5 -5 -5 -7 -6 -8 -5 -11 -9 8 5 -6 -1 -2 -8 -7 -2 -14 -6 2 -6 -7 0 0 0 -17\n"
				+ "L -6 -7 -6 -10 -11 -7 -7 -11 -8 5 5 -7 0 -3 -8 -8 -5 -10 -7 0 -7 -7 0 0 0 -17\n"
				+ "K -7 0 -1 -4 -14 -3 -4 -7 -6 -6 -7 7 -2 -14 -6 -4 -3 -12 -9 -9 5 4 0 0 0 -17\n"
				+ "M -5 -4 -9 -11 -13 -4 -7 -8 -10 -1 0 -2 11 -4 -8 -5 -4 -13 -11 -1 -3 -3 0 0 0 -17\n"
				+ "F -8 -9 -9 -15 -13 -13 -14 -9 -6 -2 -3 -14 -4 9 -10 -6 -9 -4 2 -8 -12 -14 0 0 0 -17\n"
				+ "P -2 -4 -6 -8 -8 -3 -5 -6 -4 -8 -8 -6 -8 -10 8 -2 -4 -14 -13 -6 -5 -5 0 0 0 -17\n"
				+ "S 0 -3 0 -4 -3 -5 -4 -2 -6 -7 -8 -4 -5 -6 -2 6 0 -5 -7 -6 -4 -5 0 0 0 -17\n"
				+ "T -1 -6 -2 -5 -8 -5 -6 -6 -7 -2 -5 -3 -4 -9 -4 0 7 -13 -6 -3 -5 -4 0 0 0 -17\n"
				+ "W -13 -2 -8 -15 -15 -13 -17 -15 -7 -14 -10 -12 -13 -4 -14 -5 -13 13 -5 -15 -7 -13 0 0 0 -17\n"
				+ "Y -8 -10 -4 -11 -4 -12 -8 -14 -3 -6 -7 -9 -11 2 -13 -7 -6 -5 10 -7 -10 -11 0 0 0 -17\n"
				+ "V -2 -8 -8 -8 -6 -7 -6 -5 -6 2 0 -9 -1 -8 -6 -6 -3 -15 -7 7 -9 -8 0 0 0 -17\n"
				+ "B -7 5 -4 -7 -11 -3 -7 -8 -4 -6 -7 5 -3 -12 -5 -4 -5 -7 -10 -9 5 1 0 0 0 -17\n"
				+ "Z -6 -1 -2 -3 -14 4 -2 -7 -3 -7 -7 4 -3 -14 -5 -5 -4 -13 -11 -8 1 4 0 0 0 -17\n"
				+ "J 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 -17\n"
				+ "X 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 -17\n"
				+ "U 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 -17\n"
				+ "* -17 -17 -17 -17 -17 -17 -17 -17 -17 -17 -17 -17 -17 -17 -17 -17 -17 -17 -17 -17 -17 -17 -17 -17 -17 1";

		Tuple2 matrices = LoadSubstitutionMatrixFromString(PAM30MS);
		this.SubstitutionMatrix = (int[][]) matrices.getFirst();
		this.SubstitutionMatrixPos = (Map<Character, Integer>) matrices.getSecond();

	}

	/**
	 * Constructor 1
	 * 
	 * @param pam30ms
	 */
	public Aligner(String pam30ms) {

		Tuple2 matrices = LoadSubstitutionMatrixFromString(pam30ms);
		this.SubstitutionMatrix = (int[][]) matrices.getFirst();
		this.SubstitutionMatrixPos = (Map<Character, Integer>) matrices.getSecond();

	}

	/**
	 * Constructor 2
	 * 
	 * @param SubstitutionMatrix
	 * @param SubstitutionMatrixPos
	 */
	public Aligner(int[][] SubstitutionMatrix, Map<Character, Integer> SubstitutionMatrixPos) {
		this.SubstitutionMatrix = SubstitutionMatrix;
		this.SubstitutionMatrixPos = SubstitutionMatrixPos;
	}

	/**
	 * Get maximum value
	 * 
	 * @param numbers array
	 * @return max value and its index
	 */
	public static int[] getMaxValue(int[] numbers) {
		int maxValue = numbers[0];
		int index = 0;
		int bestIndex = index;
		for (index = 1; index < numbers.length; index++) {
			if (numbers[index] > maxValue) {
				maxValue = numbers[index];
				bestIndex = index;
			}
		}
		return new int[] { bestIndex, maxValue };
	}

	/**
	 * Get the closest peptide in a protein sequence
	 * 
	 * @param peptide
	 * @param protein
	 * @return
	 */
	public Tuple2 getClosestPeptideInASequence(char[] peptide, char[] protein) {
		// Here we get an array having the size of sequence 2. Each value in the array
		// in the alignment score.
		int[] res = Align(peptide, protein);

		// recover the position of best alignment;

		int[] maxScores = getMaxValue(res);
		int MaxScorePos = maxScores[0];

		/// -----------------
		int correction = 0;
		if (protein.length - 1 < MaxScorePos + peptide.length) {
			correction = MaxScorePos + peptide.length - protein.length;
		}
		String closestPeptideInProteinDB = new String(protein).substring(MaxScorePos,
				MaxScorePos + peptide.length - correction);

		///

		return new Tuple2(MaxScorePos, closestPeptideInProteinDB);
	}

	/**
	 * Performs an alignment between two sequences
	 * 
	 * @param peptide
	 * @param protein
	 * @return
	 */
	public int[] Align(char[] peptide, char[] protein) {

		if (protein.length < peptide.length) {
			System.out.println("Not optimized for peptide larger than proteins");
			return new int[] { 0 };
		}

		int[] alignmentScores = new int[protein.length];

		for (int x = 0; x < protein.length; x++) {

			int sum = 0;

			for (int y = 0; y < peptide.length; y++) {

				if (x + y >= protein.length) {
					break;
				}

				char p1 = peptide[y];
				char p2 = protein[x + y];
				int pos1 = SubstitutionMatrixPos.get(p1);
				int pos2 = SubstitutionMatrixPos.get(p2);
				int add = SubstitutionMatrix[pos1][pos2];

				sum += add;

			}

			alignmentScores[x] = sum;
		}

		return alignmentScores;

	}

	/**
	 * Load matrices from a string
	 * 
	 * @param matrixInText
	 * @return
	 */
	public static Tuple2 LoadSubstitutionMatrixFromString(String matrixInText) {

		matrixInText = matrixInText.replace("\r", "");

		int[][] substitutionMatrix = null;
		String[] lines = matrixInText.split("\n");
		int counter = 0;
		Map<Character, Integer> subsMatrixScores = new HashMap<Character, Integer>();

		for (String line : lines) {

			counter++;

			List<String> cols = Arrays.stream(line.split(" ")).collect(Collectors.toList());

			if (counter == 1) {

				List<Character> index = new ArrayList<Character>();
				for (String col : cols) {
					index.add(col.charAt(0));
				}

				for (int i = 0; i < index.size(); i++) {
					subsMatrixScores.put(index.get(i), i);
				}

				substitutionMatrix = new int[index.size() + 1][index.size() + 1];

			} else {
				cols.remove(0);

				int[] row = new int[cols.size()];

				for (int i = 0; i < cols.size(); i++) {
					row[i] = Integer.parseInt(cols.get(i));
				}

				for (int i = 0; i < row.length; i++) {
					substitutionMatrix[i][counter - 2] = row[i];
				}

			}
		}

		return new Tuple2(substitutionMatrix, subsMatrixScores);

	}
}
