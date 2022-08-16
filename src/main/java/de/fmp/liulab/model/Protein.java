package de.fmp.liulab.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Model class for protein
 * 
 * @author diogobor
 *
 */
public class Protein {

	public String proteinID;
	public String gene;
	public String fullName;
	public String sequence;
	public String checksum;
	public String location;
	public List<PDB> pdbIds;
	public List<CrossLink> interLinks;
	public List<CrossLink> intraLinks;
	public List<ProteinDomain> domains;
	public Map<String, Double> domainScores;
	public List<Residue> reactionSites;
	public boolean isConflictedDomain;
	public boolean isPredictedBasedOnTransmemInfo;
	public boolean isValid = true;

	/**
	 * Constructor
	 * 
	 * @param sequence protein sequence
	 * @param pdbIds   pdb IDs
	 */
	public Protein(String proteinID, String gene, String fullName, String sequence, String checksum, List<PDB> pdbIds,
			List<PTM> ptms) {
		this.proteinID = proteinID;
		this.gene = gene;
		this.fullName = fullName;
		this.sequence = sequence;
		this.checksum = checksum;
		this.pdbIds = pdbIds;
		this.isConflictedDomain = false;
		this.isPredictedBasedOnTransmemInfo = false;
	}

	/**
	 * Constructor
	 * 
	 * @param proteinID protein ID
	 * @param gene      gene name
	 * @param sequence  protein sequence
	 * @param domains   protein domain
	 */
	public Protein(String proteinID, String gene, String fullName, String sequence, String location,
			List<ProteinDomain> domains) {
		this.proteinID = proteinID;
		this.gene = gene;
		this.sequence = sequence;
		this.location = location;
		this.fullName = fullName;
		this.domains = domains;
		this.isConflictedDomain = false;
		this.isPredictedBasedOnTransmemInfo = false;
	}

	/**
	 * Constructor
	 * 
	 * @param proteinID protein ID
	 * @param sequence  protein sequence
	 * @param monolinks monolinks
	 */
	public Protein(String proteinID, String sequence, List<CrossLink> interLinks, List<CrossLink> intraLinks) {
		this.proteinID = proteinID;
		this.sequence = sequence;
		this.interLinks = interLinks;
		this.intraLinks = intraLinks;
		this.isConflictedDomain = false;
		this.isPredictedBasedOnTransmemInfo = false;
	}

	/**
	 * Constructor
	 * 
	 * @param proteinID protein ID
	 * @param sequence  protein sequence
	 */
	public Protein(String proteinID, String gene, String fullName, String location) {
		this.proteinID = proteinID;
		this.gene = gene;
		this.fullName = fullName;
		this.location = location;
		this.isConflictedDomain = false;
		this.isPredictedBasedOnTransmemInfo = false;
	}

	public Protein(String proteinID, String gene, String sequence) {
		this.proteinID = proteinID;
		this.gene = gene;
		this.sequence = sequence;
		this.isConflictedDomain = false;
		this.isPredictedBasedOnTransmemInfo = false;
	}

	public Protein(String proteinID, String gene) {
		this.proteinID = proteinID;
		this.gene = gene;
	}

	/**
	 * Empty constructor
	 */
	public Protein() {
		this.pdbIds = new ArrayList<PDB>();
		this.domains = new ArrayList<ProteinDomain>();
		this.isConflictedDomain = false;
		this.isPredictedBasedOnTransmemInfo = false;
	}
}
