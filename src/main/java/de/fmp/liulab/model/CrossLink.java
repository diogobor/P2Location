package de.fmp.liulab.model;

/**
 * Model class for Cross links
 * 
 * @author borges.diogo
 *
 */
public class CrossLink implements Comparable<CrossLink> {
	public String protein_a;
	public String protein_b;
	public int pos_site_a;
	public int pos_site_b;
	public double score;
	public String sequence;
	public int start_pos_protein;
	public int end_pos_protein;
	public String location;

	/**
	 * Empty Constructor
	 */
	public CrossLink() {

	}

	/**
	 * Constructor with score
	 * 
	 * @param ptn_a protein a name
	 * @param ptn_b protein b name
	 * @param pos_a position a
	 * @param pos_b position b
	 * @param score a
	 */
	public CrossLink(String ptn_a, String ptn_b, int pos_a, int pos_b, double score) {
		this.protein_a = ptn_a;
		this.protein_b = ptn_b;
		this.pos_site_a = pos_a;
		this.pos_site_b = pos_b;
		this.score = score;
	}

	/**
	 * Constructor without score
	 * 
	 * @param ptn_a protein a name
	 * @param ptn_b protein b name
	 * @param pos_a position a
	 * @param pos_b position b
	 */
	public CrossLink(String ptn_a, String ptn_b, int pos_a, int pos_b) {
		this.protein_a = ptn_a;
		this.protein_b = ptn_b;
		this.pos_site_a = pos_a;
		this.pos_site_b = pos_b;
		this.score = 1.0;
	}

	/**
	 * Constructor with sequence and start/end positions
	 * 
	 * @param sequence      peptide sequence
	 * @param xl_a          reaction site position a
	 * @param xl_b          reaction site position b
	 * @param start_pos_ptn start position in the protein
	 * @param end_pos_ptn   end position in the protein
	 */
	public CrossLink(String sequence, int xl_a, int xl_b, int start_pos_ptn, int end_pos_ptn) {
		this.protein_a = "";
		this.protein_b = "";
		this.pos_site_a = xl_a;
		this.pos_site_b = xl_b;
		this.score = 1.0;
		this.sequence = sequence;
		this.start_pos_protein = start_pos_ptn;
		this.end_pos_protein = end_pos_ptn;
	}

	/**
	 * Method responsible for comparing two objects
	 */
	public int compareTo(CrossLink d) {

		int result = this.pos_site_a - d.pos_site_a;
		if (result != 0) { // pos_site_b is equal, then sort by pos_site_b
			return result;
		}

		return this.pos_site_b - d.pos_site_b;
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj == null) {
			return false;
		}
		final CrossLink crosslink = (CrossLink) obj;
		if (this == crosslink) {
			return true;
		} else {
			return (this.protein_a.equals(crosslink.protein_a) && this.protein_b.equals(crosslink.protein_b)
					&& this.pos_site_a == crosslink.pos_site_a && this.pos_site_b == crosslink.pos_site_b);
		}
	}

	@Override
	public int hashCode() {
		int hashno = 7;
		hashno = 13 * hashno + (protein_a == null ? 0 : protein_a.hashCode());
		return hashno;
	}

	/**
	 * Convert to string
	 */
	@Override
	public String toString() {
		return "CrossLink {" + this.protein_a + " - " + this.protein_b + " [" + this.pos_site_a + " - "
				+ this.pos_site_b + "]}";
	}

}