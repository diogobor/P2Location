package de.fmp.liulab.model;

/**
 * Model class for Protein domains
 * 
 * @author borges.diogo
 *
 */
public class ProteinDomain implements Comparable<ProteinDomain>, Cloneable {
	public String name;
	public int startId;
	public int endId;
	public String eValue;
	public java.awt.Color color;
	public boolean isPredicted = false;

	public int getEndId() {
		return endId;
	}

	public int getStartId() {
		return startId;
	}

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
	 * Constructor
	 * 
	 * @param name        protein domain name
	 * @param startId     start index
	 * @param endId       end index
	 * @param isPredicted predicted or original domain
	 * @param eValue      score
	 */
	public ProteinDomain(String name, int startId, int endId, boolean isPredicted, String eValue) {
		this.name = name;
		this.startId = startId;
		this.endId = endId;
		this.eValue = eValue;
		this.isPredicted = isPredicted;
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
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((color == null) ? 0 : color.hashCode());
		result = prime * result + ((eValue == null) ? 0 : eValue.hashCode());
		result = prime * result + endId;
		result = prime * result + (isPredicted ? 1231 : 1237);
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + startId;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ProteinDomain other = (ProteinDomain) obj;
		if (color == null) {
			if (other.color != null)
				return false;
		} else if (!color.equals(other.color))
			return false;
		if (eValue == null) {
			if (other.eValue != null)
				return false;
		} else if (!eValue.equals(other.eValue))
			return false;
		if (endId != other.endId)
			return false;
		if (isPredicted != other.isPredicted)
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (startId != other.startId)
			return false;
		return true;
	}

	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

}