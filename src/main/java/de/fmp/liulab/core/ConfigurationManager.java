package de.fmp.liulab.core;

import org.cytoscape.property.AbstractConfigDirPropsReader;
import org.cytoscape.property.CyProperty;

public class ConfigurationManager extends AbstractConfigDirPropsReader {

	public ConfigurationManager() {
		super("", "", CyProperty.SavePolicy.SESSION_FILE_AND_CONFIG_DIR);
	}
	
	public ConfigurationManager(String name, String propFileName) {
		super(name, propFileName, CyProperty.SavePolicy.SESSION_FILE_AND_CONFIG_DIR);
		this.getProperties().list(System.out);
	}

}
