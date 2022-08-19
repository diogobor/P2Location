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
 * Class responsible for initializing Layout node task
 * 
 * @author diogobor
 *
 */
public class MainSingleNodeTaskFactory extends AbstractTaskFactory {

	private CyApplicationManager cyApplicationManager;
	private VisualMappingManager vmmServiceRef;
	@SuppressWarnings("rawtypes")
	private CyCustomGraphics2Factory vgFactory;
	private HandleFactory handleFactory;
	private BendFactory bendFactory;
	private boolean forcedWindowOpen;

	/**
	 * Constructor
	 * 
	 * @param cyApplicationManager main app manager
	 * @param vmmServiceRef        visual mapping manager
	 * @param customChartListener  chart style listener
	 * @param bendFactory          bend factory
	 * @param handleFactory        handle factory
	 * @param forcedWindowOpen     forced window open
	 */
	public MainSingleNodeTaskFactory(CyApplicationManager cyApplicationManager,
			final VisualMappingManager vmmServiceRef, CustomChartListener customChartListener, BendFactory bendFactory,
			HandleFactory handleFactory, boolean forcedWindowOpen) {
		this.cyApplicationManager = cyApplicationManager;
		this.vmmServiceRef = vmmServiceRef;
		this.vgFactory = customChartListener.getFactory();
		this.bendFactory = bendFactory;
		this.handleFactory = handleFactory;
		this.forcedWindowOpen = forcedWindowOpen;
	}

	/**
	 * Empty constructor
	 */
	public MainSingleNodeTaskFactory() {
	}

	/**
	 * Method responsible for initializing task
	 */
	@Override
	public TaskIterator createTaskIterator() {
		return new TaskIterator(new MainSingleNodeTask(cyApplicationManager, vmmServiceRef, vgFactory, bendFactory,
				handleFactory, forcedWindowOpen, false));
	}

	public TaskIterator createTaskIterator(boolean isCommand) {
		return new TaskIterator(new MainSingleNodeTask(cyApplicationManager, vmmServiceRef, vgFactory, bendFactory,
				handleFactory, forcedWindowOpen, isCommand));
	}

}