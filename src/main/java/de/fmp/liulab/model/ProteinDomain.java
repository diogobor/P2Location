package de.fmp.liulab.model;

/**
 * Model class for Protein domains
 * 
 * @author borges.diogo
 *
 */
public class ProteinDomain implements Comparable<ProteinDomain> {
	public String name;
	public int startId;
	public int endId;
	public String eValue;
	public java.awt.Color color;
	public boolean isPredicted = false;

	/**
	 * Constructor
	 * 
	 * @param name    protein domain name
	 * @param startId start index
	 * @param endId   end index
	 * @param eValue  score
	 */
	public ProteinDomain(String name, int startId, int endId, String eValue) {
		this.name = name;
		this.startId = startId;
		this.endId = endId;
		this.eValue = eValue;
	}

	/**
	 * Constructor 2
	 * 
	 * @param name    protein domain name
	 * @param startId start index
	 * @param endId   end index
	 * @param color   score
	 */
	public ProteinDomain(String name, int startId, int endId, java.awt.Color color) {
		this.name = name;
		this.startId = startId;
		this.endId = endId;
		this.color = color;
	}

	/**
	 * Method responsible for comparing two objects
	 */
	public int compareTo(ProteinDomain d) {
		return this.startId - d.startId;
	}

	/**
	 * Convert to string
	 */
	@Override
	public String toString() {
		return "Protein domain {" + this.name + "}";
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof ProteinDomain) {
			ProteinDomain p = (ProteinDomain) o;
			return this.name.equals(p.name);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return this.name.hashCode() * 31 + this.startId + this.endId;
	}

}