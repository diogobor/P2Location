package de.fmp.liulab.task;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

public class ProcessTransmemRegionsTaskFactory extends AbstractTaskFactory {

	private CyApplicationManager cyApplicationManager;
	private VisualMappingManager vmmServiceRef;

	/**
	 * Empty constructor
	 */
	public ProcessTransmemRegionsTaskFactory() {
	}

	/**
	 * Constructor
	 * 
	 * @param cyApplicationManager
	 * @param vmmServiceRef
	 */
	public ProcessTransmemRegionsTaskFactory(CyApplicationManager cyApplicationManager,
			VisualMappingManager vmmServiceRef) {
		super();
		this.cyApplicationManager = cyApplicationManager;
		this.vmmServiceRef = vmmServiceRef;
	}

	/**
	 * Method responsible for initializing task
	 */
	public TaskIterator createTaskIterator(boolean isPredictLocation, boolean updateAnnotationDomain) {
		return new TaskIterator(new ProcessTransmemRegionsTask(cyApplicationManager, vmmServiceRef));
	}

	@Override
	public TaskIterator createTaskIterator() {
		// TODO Auto-generated method stub
		return null;
	}

}
