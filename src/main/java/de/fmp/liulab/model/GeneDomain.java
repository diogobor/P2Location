package de.fmp.liulab.model;

import java.util.Collections;
import java.util.List;

/**
 * Model class for protein domains of each gene
 * 
 * @author borges.diogo
 *
 */
public class GeneDomain implements Comparable<GeneDomain> {

	public String geneName;
	public List<ProteinDomain> proteinDomains;

	/**
	 * Constructor
	 * 
	 * @param geneName       gene name
	 * @param proteinDomains all protein domains
	 */
	public GeneDomain(String geneName, List<ProteinDomain> proteinDomains) {
		this.geneName = geneName;
		this.proteinDomains = proteinDomains;
		Collections.sort(this.proteinDomains);
	}

	/**
	 * Method responsible for comparing two objects
	 */
	@Override
	public int compareTo(GeneDomain o) {
		return geneName.compareTo(o.geneName);
	}

	/**
	 * Convert to string
	 */
	@Override
	public String toString() {
		return "Gene {" + this.geneName + "}";
	}
}
