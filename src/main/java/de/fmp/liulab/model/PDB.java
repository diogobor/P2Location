package de.fmp.liulab.model;

/**
 * Model class for Protein Data Bank (PDB)
 * 
 * @author diogobor
 *
 */
public class PDB implements Comparable<PDB> {

	public String entry;
	public String resolution;
	public String chain;
	public String positions;

	/**
	 * Constructor
	 * 
	 * @param entry      entry
	 * @param resolution resolution
	 * @param chain      chain
	 * @param positions  positions
	 */
	public PDB(String entry, String resolution, String chain, String positions) {
		this.entry = entry;
		this.resolution = resolution;
		this.chain = chain;
		this.positions = positions;

	}

	/**
	 * Method responsible for comparing two objects
	 */
	public int compareTo(PDB d) {

		try {

			double result = Double.parseDouble(this.resolution) - Double.parseDouble(d.resolution);
			if (result == 0) {
				return 0;
			} else if (Double.parseDouble(this.resolution) > Double.parseDouble(d.resolution))
				return -1;
			else
				return 1;

		} catch (Exception e) {

		}
		return 0;
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj == null) {
			return false;
		}
		final PDB crosslink = (PDB) obj;
		if (this == crosslink) {
			return true;
		} else {
			return (this.entry.equals(crosslink.entry) && this.resolution.equals(crosslink.resolution)
					&& this.chain.equals(crosslink.chain) && this.positions.equals(crosslink.positions));

		}
	}

	@Override
	public int hashCode() {
		int hashno = 7;
		hashno = 13 * hashno + (entry == null ? 0 : entry.hashCode());
		return hashno;
	}
}
