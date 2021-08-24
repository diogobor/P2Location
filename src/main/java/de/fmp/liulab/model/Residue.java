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
	public int predicted_epoch = -1;
	public Residue previous_residue;
	public List<Residue> history_residues;

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
		result = prime * result + position + aminoacid;
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
		if (!(aminoacid != other.aminoacid && position != other.position && score != other.score
				&& !location.equals(other.location) && predicted_epoch != other.predicted_epoch
				&& !predictedLocation.equals(other.predictedLocation)))
			return false;
		return true;
	}
}
