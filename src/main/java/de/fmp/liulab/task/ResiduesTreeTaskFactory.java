package de.fmp.liulab.task;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.presentation.customgraphics.CyCustomGraphics2Factory;
import org.cytoscape.view.presentation.property.values.BendFactory;
import org.cytoscape.view.presentation.property.values.HandleFactory;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

import de.fmp.liulab.internal.CustomChartListener;

public class ResiduesTreeTaskFactory extends AbstractTaskFactory {

	private CyApplicationManager cyApplicationManager;
	private CyNetworkFactory netFactory;
	private CyNetworkManager networkManager;
	private CyNetworkViewManager viewManager;
	private CyNetworkViewFactory viewFactory;
	private VisualMappingManager vmmServiceRef;
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
	public ResiduesTreeTaskFactory(CyApplicationManager cyApplicationManager, CyNetworkFactory netFactory,
			CyNetworkManager networkManager, CyNetworkViewManager viewManager, CyNetworkViewFactory viewFactory,
			final VisualMappingManager vmmServiceRef, CustomChartListener customChartListener, BendFactory bendFactory,
			HandleFactory handleFactory, boolean forcedWindowOpen) {
		this.cyApplicationManager = cyApplicationManager;
		this.vmmServiceRef = vmmServiceRef;
		this.vgFactory = customChartListener.getFactory();
		this.bendFactory = bendFactory;
		this.handleFactory = handleFactory;
		this.forcedWindowOpen = forcedWindowOpen;
		this.netFactory = netFactory;
		this.viewManager = viewManager;
		this.viewFactory = viewFactory;
		this.networkManager = networkManager;
	}

	/**
	 * Empty constructor
	 */
	public ResiduesTreeTaskFactory() {
	}

	/**
	 * Method responsible for initializing task
	 */
	@Override
	public TaskIterator createTaskIterator() {
		return new TaskIterator(new ResiduesTreeTask(cyApplicationManager, netFactory, networkManager, viewManager,
				viewFactory, vmmServiceRef, vgFactory, bendFactory, handleFactory, forcedWindowOpen, false));
	}

	public TaskIterator createTaskIterator(boolean isCommand) {
		return new TaskIterator(new ResiduesTreeTask(cyApplicationManager, netFactory, networkManager, viewManager,
				viewFactory, vmmServiceRef, vgFactory, bendFactory, handleFactory, forcedWindowOpen, isCommand));
	}

}
