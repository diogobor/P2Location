package de.fmp.liulab.task;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.view.presentation.customgraphics.CyCustomGraphics2Factory;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

import de.fmp.liulab.internal.CustomChartListener;

/**
 * Class responsible for calling post-translational modifications task
 * @author borges.diogo
 *
 */
public class LoadPTMsTaskFactory extends AbstractTaskFactory {

	private CyApplicationManager cyApplicationManager;
	private VisualMappingManager vmmServiceRef;
	private CyCustomGraphics2Factory vgFactory;

	/**
	 * Constructor
	 * 
	 * @param cyApplicationManager main app manager
	 * @param vmmServiceRef        visual mapping manager
	 * @param customChartListener  chart style listener
	 */
	public LoadPTMsTaskFactory(CyApplicationManager cyApplicationManager, final VisualMappingManager vmmServiceRef,
			CustomChartListener customChartListener) {
		this.cyApplicationManager = cyApplicationManager;
		this.vmmServiceRef = vmmServiceRef;
		this.vgFactory = customChartListener.getFactory();
	}

	/**
	 * Empty constructor
	 */
	public LoadPTMsTaskFactory() {
	}

	/**
	 * Method responsible for initializing task
	 */
	public TaskIterator createTaskIterator() {
		return new TaskIterator(new LoadPTMsTask(cyApplicationManager, vmmServiceRef, vgFactory));
	}

}
