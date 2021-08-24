package de.fmp.liulab.model;

/**
 * Model class for Post-translational modifications
 * 
 * @author borges.diogo
 *
 */
public class PTM implements Comparable<PTM> {
	public String name;
	public char residue;
	public int position;

	/**
	 * Constructor
	 * 
	 * @param name
	 * @param residue
	 * @param position
	 */
	public PTM(String name, char residue, int position) {
		this.name = name;
		this.residue = residue;
		this.position = position;
	}

	/**
	 * Method responsible for comparing two objects
	 */
	public int compareTo(PTM d) {
		return this.position - d.position;
	}

	/**
	 * Convert to string
	 */
	@Override
	public String toString() {
		return "PTM {" + this.name + "}";
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof PTM) {
			PTM p = (PTM) o;
			return this.name.equals(p.name);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return this.name.hashCode() * 31 + this.position;
	}

}
