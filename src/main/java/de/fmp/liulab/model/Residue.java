package de.fmp.liulab.model;

import java.util.ArrayList;
import java.util.List;

public class Residue {

	public char aminoacid;
	public String location;
	public String predictedLocation;
	public int position;
	public Protein protein;
	public double score = 0;
	public double conflicted_score = 0;
	public int predicted_epoch = -1;
	public Residue previous_residue;
	public Residue conflicted_residue;
	public List<Residue> history_residues;
	public boolean isConflicted = false;

	public int getPosition() {
        return position;
    }
	/**
	 * Constructor
	 * 
	 * @param aminoacid
	 * @param location
	 * @param position
	 * @param protein
	 */
	public Residue(char aminoacid, String location, int position, Protein protein) {
		this.aminoacid = aminoacid;
		this.location = location;
		this.predictedLocation = location;
		this.position = position;
		this.protein = protein;
	}

	public void addHistoryResidue(Residue previous_res) {

		if (this.history_residues == null)
			this.history_residues = new ArrayList<Residue>();

		this.history_residues.add(previous_res);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + aminoacid;
		result = prime * result + ((location == null) ? 0 : location.hashCode());
		result = prime * result + position;
		result = prime * result + ((predictedLocation == null) ? 0 : predictedLocation.hashCode());
		result = prime * result + predicted_epoch;
		long temp;
		temp = Double.doubleToLongBits(score);
		result = prime * result + (int) (temp ^ (temp >>> 32));
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
		Residue other = (Residue) obj;
		if (aminoacid != other.aminoacid)
			return false;
		if (location == null) {
			if (other.location != null)
				return false;
		} else if (!location.equals(other.location))
			return false;
		if (position != other.position)
			return false;
		if (predictedLocation == null) {
			if (other.predictedLocation != null)
				return false;
		} else if (!predictedLocation.equals(other.predictedLocation))
			return false;
		if (predicted_epoch != other.predicted_epoch)
			return false;
		if (Double.doubleToLongBits(score) != Double.doubleToLongBits(other.score))
			return false;
		return true;
	}
}
