package de.fmp.liulab.model;

/**
 * Model class for fasta sequences
 * 
 * @author diogobor
 *
 */
public class Fasta {

	public String header;
	public String sequence;
	public int offset = -1;

	/**
	 * Constructor
	 * 
	 * @param header   header
	 * @param sequence protein sequence
	 */
	public Fasta(String header, String sequence) {
		this.header = header;
		this.sequence = sequence;
	}

	/**
	 * Constructor
	 * 
	 * @param header   header
	 * @param sequence protein sequence
	 * @param offset   offset of the protein in the pdb file
	 */
	public Fasta(String header, String sequence, int offset) {
		this.header = header;
		this.sequence = sequence;
		this.offset = offset;
	}
}
