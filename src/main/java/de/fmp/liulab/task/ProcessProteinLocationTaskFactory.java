package de.fmp.liulab.task;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

/**
 * Class responsible for loading protein domains
 * 
 * @author diogobor
 *
 */
public class ProcessProteinLocationTaskFactory extends AbstractTaskFactory {

	private CyApplicationManager cyApplicationManager;
	private VisualMappingManager vmmServiceRef;

	/**
	 * Constructor
	 * 
	 * @param cyApplicationManager main app manager
	 * @param vmmServiceRef        visual mapping manager
	 */
	public ProcessProteinLocationTaskFactory(CyApplicationManager cyApplicationManager,
			final VisualMappingManager vmmServiceRef) {
		this.cyApplicationManager = cyApplicationManager;
		this.vmmServiceRef = vmmServiceRef;
	}

	/**
	 * Empty constructor
	 */
	public ProcessProteinLocationTaskFactory() {
	}

	/**
	 * Method responsible for initializing task
	 */
	public TaskIterator createTaskIterator(boolean isPredictLocation, boolean updateAnnotationDomain) {
		return new TaskIterator(new ProcessProteinLocationTask(cyApplicationManager, vmmServiceRef,
				isPredictLocation, updateAnnotationDomain));
	}

	@Override
	public TaskIterator createTaskIterator() {
		// TODO Auto-generated method stub
		return null;
	}
}
