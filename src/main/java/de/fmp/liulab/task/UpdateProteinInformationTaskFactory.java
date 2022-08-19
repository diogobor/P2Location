package de.fmp.liulab.task;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.view.presentation.customgraphics.CyCustomGraphics2Factory;
import org.cytoscape.view.presentation.property.values.BendFactory;
import org.cytoscape.view.presentation.property.values.HandleFactory;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

import de.fmp.liulab.internal.CustomChartListener;

/**
 * Class responsible for calling the task for updating protein information
 * 
 * @author diogobor
 *
 */
public class UpdateProteinInformationTaskFactory extends AbstractTaskFactory {

	private CyApplicationManager cyApplicationManager;
	private VisualMappingManager vmmServiceRef;
	@SuppressWarnings("rawtypes")
	private CyCustomGraphics2Factory vgFactory;
	private HandleFactory handleFactory;
	private BendFactory bendFactory;

	/**
	 * Constructor
	 */
	public UpdateProteinInformationTaskFactory(CyApplicationManager cyApplicationManager,
			final VisualMappingManager vmmServiceRef, CustomChartListener customChartListener, BendFactory bendFactory,
			HandleFactory handleFactory) {

		this.cyApplicationManager = cyApplicationManager;
		this.vmmServiceRef = vmmServiceRef;
		this.vgFactory = customChartListener.getFactory();
		this.bendFactory = bendFactory;
		this.handleFactory = handleFactory;
	}

	/**
	 * Method responsible for initializing task
	 */
	public TaskIterator createTaskIterator(boolean validateProtein) {
		return new TaskIterator(new UpdateProteinInformationTask(cyApplicationManager, vmmServiceRef, vgFactory, bendFactory,
				handleFactory, validateProtein));
	}

	@Override
	public TaskIterator createTaskIterator() {
		// TODO Auto-generated method stub
		return null;
	}
}
