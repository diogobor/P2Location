package de.fmp.liulab.internal;

import static org.cytoscape.work.ServiceProperties.COMMAND;
import static org.cytoscape.work.ServiceProperties.COMMAND_DESCRIPTION;
import static org.cytoscape.work.ServiceProperties.COMMAND_EXAMPLE_JSON;
import static org.cytoscape.work.ServiceProperties.COMMAND_LONG_DESCRIPTION;
import static org.cytoscape.work.ServiceProperties.COMMAND_NAMESPACE;
import static org.cytoscape.work.ServiceProperties.COMMAND_SUPPORTS_JSON;
import static org.cytoscape.work.ServiceProperties.PREFERRED_MENU;

import java.util.Locale;
import java.util.Properties;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.events.SetCurrentNetworkListener;
import org.cytoscape.application.swing.CyAction;
import org.cytoscape.application.swing.CyEdgeViewContextMenuFactory;
import org.cytoscape.application.swing.CyNodeViewContextMenuFactory;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.events.NetworkAddedListener;
import org.cytoscape.model.events.NetworkDestroyedListener;
import org.cytoscape.model.events.RowsSetListener;
import org.cytoscape.property.CyProperty;
import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.util.swing.OpenBrowser;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.model.events.ViewChangedListener;
import org.cytoscape.view.presentation.customgraphics.CyCustomGraphics2Factory;
import org.cytoscape.view.presentation.property.values.BendFactory;
import org.cytoscape.view.presentation.property.values.HandleFactory;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.work.ServiceProperties;
import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.swing.DialogTaskManager;
import org.osgi.framework.BundleContext;

import de.fmp.liulab.core.ConfigurationManager;
import de.fmp.liulab.internal.action.ControlURLAction;
import de.fmp.liulab.internal.action.ExportMonolinksAction;
import de.fmp.liulab.internal.action.ExportPTMsAction;
import de.fmp.liulab.internal.action.ExportProteinDomainsAction;
import de.fmp.liulab.internal.action.LoadPTMsAction;
import de.fmp.liulab.internal.action.LoadProteinDomainsAction;
import de.fmp.liulab.internal.action.LoadProteinLocationAction;
import de.fmp.liulab.internal.action.MainPanelAction;
import de.fmp.liulab.internal.action.ReadMeAction;
import de.fmp.liulab.internal.action.ResiduesTreeNodeExecuteAction;
import de.fmp.liulab.internal.action.SetDomainColorAction;
import de.fmp.liulab.internal.action.ShortcutWindowSingleNodeLayout;
import de.fmp.liulab.task.LoadPTMsTaskFactory;
import de.fmp.liulab.task.MainSingleEdgeTaskFactory;
import de.fmp.liulab.task.MainSingleNodeTaskFactory;
import de.fmp.liulab.task.ProcessProteinLocationTaskFactory;
import de.fmp.liulab.task.ProteinScalingFactorHorizontalExpansionTableTaskFactory;
import de.fmp.liulab.task.ResiduesTreeTaskFactory;
import de.fmp.liulab.task.SetDomainColorTaskFactory;
import de.fmp.liulab.task.UpdateViewerTaskFactory;
import de.fmp.liulab.task.command_lines.ApplyRestoreStyleCommandTask;
import de.fmp.liulab.task.command_lines.ApplyRestoreStyleCommandTaskFactory;
import de.fmp.liulab.task.command_lines.ExportMonolinksCommandTask;
import de.fmp.liulab.task.command_lines.ExportMonolinksCommandTaskFactory;
import de.fmp.liulab.task.command_lines.ExportPTMsCommandTask;
import de.fmp.liulab.task.command_lines.ExportPTMsCommandTaskFactory;
import de.fmp.liulab.task.command_lines.ExportProteinDomainsCommandTask;
import de.fmp.liulab.task.command_lines.ExportProteinDomainsCommandTaskFactory;
import de.fmp.liulab.task.command_lines.LoadMonolinksCommandTask;
import de.fmp.liulab.task.command_lines.LoadMonolinksCommandTaskFactory;
import de.fmp.liulab.task.command_lines.LoadPTMsCommandTask;
import de.fmp.liulab.task.command_lines.LoadPTMsCommandTaskFactory;
import de.fmp.liulab.task.command_lines.LoadProteinDomainsCommandTask;
import de.fmp.liulab.task.command_lines.LoadProteinDomainsCommandTaskFactory;
import de.fmp.liulab.task.command_lines.ReadMeCommandTask;
import de.fmp.liulab.task.command_lines.ReadMeCommandTaskFactory;
import de.fmp.liulab.task.command_lines.SetParametersCommandTask;
import de.fmp.liulab.task.command_lines.SetParametersCommandTaskFactory;
import de.fmp.liulab.task.command_lines.SetProteinDomainsColorCommandTask;
import de.fmp.liulab.task.command_lines.SetProteinDomainsColorCommandTaskFactory;

/**
 * Class responsible for initializing cytoscape methods
 * 
 * @author diogobor
 *
 */
public class CyActivator extends AbstractCyActivator {

	private Properties MainProps;
	private ConfigurationManager cm;
	public static final String SOFTWARE_COMMAND_NAMESPACE = "p2location";

	public CyActivator() {
		super();
	}

	/**
	 * Method responsible for starting context
	 */
	public void start(BundleContext bc) {

		// #### 1 - ABOUT ####
		OpenBrowser openBrowser = getService(bc, OpenBrowser.class);
		String version = bc.getBundle().getVersion().toString();
		ControlURLAction controlURLAction = new ControlURLAction(openBrowser, version);
		ReadMeAction readMe = new ReadMeAction(openBrowser);

		// ###############

		CyApplicationManager cyApplicationManager = getService(bc, CyApplicationManager.class);
		VisualMappingManager vmmServiceRef = getService(bc, VisualMappingManager.class);
		CustomChartListener customChartListener = new CustomChartListener();
		HandleFactory handleFactory = getService(bc, HandleFactory.class);
		BendFactory bendFactory = getService(bc, BendFactory.class);
		DialogTaskManager dialogTaskManager = getService(bc, DialogTaskManager.class);

		// ### 3 - MONOLINKED PEPTIDES ####

		// ### 3.1 - EXPORT ###
		ExportMonolinksAction myExportMonolinksAction = new ExportMonolinksAction(cyApplicationManager);

		// ### 3.2 - LOAD ####

		ProcessProteinLocationTaskFactory myProcessProteinLocationTaskFactory = new ProcessProteinLocationTaskFactory(
				cyApplicationManager, vmmServiceRef, customChartListener);

		LoadProteinLocationAction myProteinLocationAction = new LoadProteinLocationAction(dialogTaskManager,
				myProcessProteinLocationTaskFactory);

		// ######################

		// ### 4 - POST-TRANSLATIONAL MODIFICATIONS ###

		// ### 4.1 - EXPORT ###
		ExportPTMsAction myExportPTMsAction = new ExportPTMsAction(cyApplicationManager);

		// ### 4.2 - LOAD ####

		TaskFactory myLoadPTMsFactory = new LoadPTMsTaskFactory(cyApplicationManager, vmmServiceRef,
				customChartListener);

		LoadPTMsAction myLoadPTMsAction = new LoadPTMsAction(dialogTaskManager, myLoadPTMsFactory);

		// ############################################

		// ### 5 - PROTEIN DOMAINS ###

		// ### 5.1 - EXPORT ###
		ExportProteinDomainsAction myExportProteinDomainsAction = new ExportProteinDomainsAction(cyApplicationManager);

		// ####################

		// ### 5.2 - LOAD ####
		TaskFactory myLoadProteinDomainsFactory = new ProcessProteinLocationTaskFactory(cyApplicationManager,
				vmmServiceRef, customChartListener);

		LoadProteinDomainsAction myLoadProteinDomainsAction = new LoadProteinDomainsAction(dialogTaskManager,
				myLoadProteinDomainsFactory);

		// ###################

		// ### 5.3 - SET ####
		TaskFactory mySetProteinDomainsColorFactory = new SetDomainColorTaskFactory(cyApplicationManager, vmmServiceRef,
				customChartListener);

		SetDomainColorAction mySetProteinDomainsAction = new SetDomainColorAction(dialogTaskManager,
				mySetProteinDomainsColorFactory);

		// ###################

		// ##############################

		// #### 6 - EXECUTE SINGLE NODE ####

		registerServiceListener(bc, customChartListener, "addCustomGraphicsFactory", "removeCustomGraphicsFactory",
				CyCustomGraphics2Factory.class);

		// Our menu item should only be enabled if at least one network
		// view exists.
		Properties myNodeViewContextMenuFactoryProps = new Properties();
		myNodeViewContextMenuFactoryProps.put(PREFERRED_MENU, "Apps");
		myNodeViewContextMenuFactoryProps.put(ServiceProperties.ENABLE_FOR, "networkAndView");
		Properties myEdgeViewContextMenuFactoryProps = new Properties();
		myEdgeViewContextMenuFactoryProps.put(PREFERRED_MENU, "Apps.P2Location");
		myEdgeViewContextMenuFactoryProps.put(ServiceProperties.ENABLE_FOR, "networkAndView");

		TaskFactory mySingleNodeShortCutFactory = new MainSingleNodeTaskFactory(cyApplicationManager, vmmServiceRef,
				customChartListener, bendFactory, handleFactory, false);

		CyNetworkFactory networkFactory = getService(bc, CyNetworkFactory.class);
		CyNetworkManager networkManager = getService(bc, CyNetworkManager.class);

		TaskFactory myResiduesTreeFactory = new ResiduesTreeTaskFactory(cyApplicationManager, networkFactory,networkManager,
				vmmServiceRef, customChartListener, bendFactory, handleFactory, false);

		ResiduesTreeNodeExecuteAction myResiduesTreeNodeAction = new ResiduesTreeNodeExecuteAction(dialogTaskManager,
				myResiduesTreeFactory);

		TaskFactory mySingleNodeContextMenuFactory = new MainSingleNodeTaskFactory(cyApplicationManager, vmmServiceRef,
				customChartListener, bendFactory, handleFactory, true);

		TaskFactory mySingleEdgeContextMenuFactory = new MainSingleEdgeTaskFactory(cyApplicationManager, vmmServiceRef,
				customChartListener, bendFactory, handleFactory, true);

		CyNodeViewContextMenuFactory myNodeViewContextMenuFactory = new MainNodeContextMenu(
				mySingleNodeContextMenuFactory, dialogTaskManager);
		CyEdgeViewContextMenuFactory myEdgeViewContextMenuFactory = new MainEdgeContextMenu(
				mySingleEdgeContextMenuFactory, dialogTaskManager);

		ShortcutWindowSingleNodeLayout myShortcutWindowSingleNodeAction = new ShortcutWindowSingleNodeLayout(
				dialogTaskManager, mySingleNodeContextMenuFactory);

		// ##############################

		// ##### PROTEIN SCALING FACTOR TABLE #####
		ProteinScalingFactorHorizontalExpansionTableTaskFactory proteinScalingFactorTableTaskFactory = new ProteinScalingFactorHorizontalExpansionTableTaskFactory();

		// ########################################

		// #### LISTENER ######

		CyNetworkViewManager cyNetworkViewManagerServiceRef = getService(bc, CyNetworkViewManager.class);
		UpdateViewerTaskFactory updateViewerTaskFactory = new UpdateViewerTaskFactory();
		ViewChangedListener updateViewListener = new UpdateViewListener(cyApplicationManager, handleFactory,
				bendFactory, vmmServiceRef, dialogTaskManager, proteinScalingFactorTableTaskFactory,
				updateViewerTaskFactory, cyNetworkViewManagerServiceRef);

		registerService(bc, updateViewListener, ViewChangedListener.class, new Properties());
		registerService(bc, updateViewListener, RowsSetListener.class, new Properties());
		registerService(bc, updateViewListener, SetCurrentNetworkListener.class, new Properties());
		registerService(bc, updateViewListener, NetworkAddedListener.class, new Properties());
		registerService(bc, updateViewListener, NetworkDestroyedListener.class, new Properties());
		// #####################

		// #### 2 - PANEL (SETTINGS) ####
		init_default_params(bc);

		CySwingApplication cytoscapeDesktopService = getService(bc, CySwingApplication.class);
		MainControlPanel mainControlPanel = new MainControlPanel(MainProps, cm, myProcessProteinLocationTaskFactory,
				mySingleNodeShortCutFactory, dialogTaskManager);
		MainPanelAction panelAction = new MainPanelAction(cytoscapeDesktopService, mainControlPanel);

		// ##############################

		// #### SERVICES #####

		registerService(bc, myProteinLocationAction, CyAction.class, new Properties());
//		registerService(bc, myExportMonolinksAction, CyAction.class, new Properties());
//		registerService(bc, myLoadPTMsAction, CyAction.class, new Properties());
//		registerService(bc, myExportPTMsAction, CyAction.class, new Properties());
		registerService(bc, myShortcutWindowSingleNodeAction, CyAction.class, new Properties());
//		registerService(bc, myLoadProteinDomainsAction, CyAction.class, new Properties());
		registerService(bc, myResiduesTreeNodeAction, CyAction.class, new Properties());
		registerService(bc, myExportProteinDomainsAction, CyAction.class, new Properties());
//		registerService(bc, mySetProteinDomainsAction, CyAction.class, new Properties());

		registerService(bc, mainControlPanel, CytoPanelComponent.class, new Properties());
		registerService(bc, panelAction, CyAction.class, new Properties());

		registerService(bc, readMe, CyAction.class, new Properties());
		registerService(bc, controlURLAction, CyAction.class, new Properties());

		registerAllServices(bc, myNodeViewContextMenuFactory, myNodeViewContextMenuFactoryProps);
		registerAllServices(bc, myEdgeViewContextMenuFactory, myEdgeViewContextMenuFactoryProps);
		// ###################

		// ####### COMMANDS ########

		init_commands(bc, cyApplicationManager, mySingleNodeShortCutFactory, dialogTaskManager, openBrowser);

		// #########################
	}

	private void init_commands(BundleContext bc, CyApplicationManager cyApplicationManager,
			TaskFactory mySingleNodeShortCutFactory, DialogTaskManager dialogTaskManager, OpenBrowser openBrowser) {

		// Register Read Me function
		Properties readmeProperties = new Properties();
		readmeProperties.setProperty(COMMAND_NAMESPACE, SOFTWARE_COMMAND_NAMESPACE);
		readmeProperties.setProperty(COMMAND, "readMe");
		readmeProperties.setProperty(COMMAND_DESCRIPTION, ReadMeCommandTaskFactory.DESCRIPTION);
		readmeProperties.setProperty(COMMAND_LONG_DESCRIPTION, ReadMeCommandTaskFactory.LONG_DESCRIPTION);
		readmeProperties.setProperty(COMMAND_EXAMPLE_JSON, ReadMeCommandTask.getExample());
		readmeProperties.setProperty(COMMAND_SUPPORTS_JSON, "true");

		TaskFactory readMeTaskFactory = new ReadMeCommandTaskFactory(openBrowser);
		registerAllServices(bc, readMeTaskFactory, readmeProperties);

		// Register apply / restore style function
		Properties applyStyleRestoreProperties = new Properties();
		applyStyleRestoreProperties.setProperty(COMMAND_NAMESPACE, SOFTWARE_COMMAND_NAMESPACE);
		applyStyleRestoreProperties.setProperty(COMMAND, "applyRestoreStyle");
		applyStyleRestoreProperties.setProperty(COMMAND_DESCRIPTION, ApplyRestoreStyleCommandTaskFactory.DESCRIPTION);
		applyStyleRestoreProperties.setProperty(COMMAND_LONG_DESCRIPTION,
				ApplyRestoreStyleCommandTaskFactory.LONG_DESCRIPTION);
		applyStyleRestoreProperties.setProperty(COMMAND_EXAMPLE_JSON, ApplyRestoreStyleCommandTask.getExample());
		applyStyleRestoreProperties.setProperty(COMMAND_SUPPORTS_JSON, "true");

		TaskFactory applyRestoreStyleTaskFactory = new ApplyRestoreStyleCommandTaskFactory(cyApplicationManager,
				(MainSingleNodeTaskFactory) mySingleNodeShortCutFactory, dialogTaskManager);
		registerAllServices(bc, applyRestoreStyleTaskFactory, applyStyleRestoreProperties);

		// Register set parameters function
		Properties setParametersProperties = new Properties();
		setParametersProperties.setProperty(COMMAND_NAMESPACE, SOFTWARE_COMMAND_NAMESPACE);
		setParametersProperties.setProperty(COMMAND, "setParameters");
		setParametersProperties.setProperty(COMMAND_DESCRIPTION, SetParametersCommandTaskFactory.DESCRIPTION);
		setParametersProperties.setProperty(COMMAND_LONG_DESCRIPTION, SetParametersCommandTaskFactory.LONG_DESCRIPTION);
		setParametersProperties.setProperty(COMMAND_EXAMPLE_JSON, SetParametersCommandTask.getExample());
		setParametersProperties.setProperty(COMMAND_SUPPORTS_JSON, "true");

		TaskFactory setParametersTaskFactory = new SetParametersCommandTaskFactory();
		registerAllServices(bc, setParametersTaskFactory, setParametersProperties);

		// Register load protein domains function
		Properties loadProteinDomainsProperties = new Properties();
		loadProteinDomainsProperties.setProperty(COMMAND_NAMESPACE, SOFTWARE_COMMAND_NAMESPACE);
		loadProteinDomainsProperties.setProperty(COMMAND, "loadProteinDomains");
		loadProteinDomainsProperties.setProperty(COMMAND_DESCRIPTION, LoadProteinDomainsCommandTaskFactory.DESCRIPTION);
		loadProteinDomainsProperties.setProperty(COMMAND_LONG_DESCRIPTION,
				LoadProteinDomainsCommandTaskFactory.LONG_DESCRIPTION);
		loadProteinDomainsProperties.setProperty(COMMAND_EXAMPLE_JSON, LoadProteinDomainsCommandTask.getExample());
		loadProteinDomainsProperties.setProperty(COMMAND_SUPPORTS_JSON, "true");

		TaskFactory loadProteinDomainsTaskFactory = new LoadProteinDomainsCommandTaskFactory(cyApplicationManager);
		registerAllServices(bc, loadProteinDomainsTaskFactory, loadProteinDomainsProperties);

		// Register export protein domains function
		Properties exportProteinDomainsProperties = new Properties();
		exportProteinDomainsProperties.setProperty(COMMAND_NAMESPACE, SOFTWARE_COMMAND_NAMESPACE);
		exportProteinDomainsProperties.setProperty(COMMAND, "exportProteinDomains");
		exportProteinDomainsProperties.setProperty(COMMAND_DESCRIPTION,
				ExportProteinDomainsCommandTaskFactory.DESCRIPTION);
		exportProteinDomainsProperties.setProperty(COMMAND_LONG_DESCRIPTION,
				ExportProteinDomainsCommandTaskFactory.LONG_DESCRIPTION);
		exportProteinDomainsProperties.setProperty(COMMAND_EXAMPLE_JSON, ExportProteinDomainsCommandTask.getExample());
		exportProteinDomainsProperties.setProperty(COMMAND_SUPPORTS_JSON, "true");

		TaskFactory exportProteinDomainsTaskFactory = new ExportProteinDomainsCommandTaskFactory(cyApplicationManager);
		registerAllServices(bc, exportProteinDomainsTaskFactory, exportProteinDomainsProperties);

		// Register set protein domains color function
		Properties setProteinDomainsColorProperties = new Properties();
		setProteinDomainsColorProperties.setProperty(COMMAND_NAMESPACE, SOFTWARE_COMMAND_NAMESPACE);
		setProteinDomainsColorProperties.setProperty(COMMAND, "setProteinDomainsColor");
		setProteinDomainsColorProperties.setProperty(COMMAND_DESCRIPTION,
				SetProteinDomainsColorCommandTaskFactory.DESCRIPTION);
		setProteinDomainsColorProperties.setProperty(COMMAND_LONG_DESCRIPTION,
				SetProteinDomainsColorCommandTaskFactory.LONG_DESCRIPTION);
		setProteinDomainsColorProperties.setProperty(COMMAND_EXAMPLE_JSON,
				SetProteinDomainsColorCommandTask.getExample());
		setProteinDomainsColorProperties.setProperty(COMMAND_SUPPORTS_JSON, "true");

		TaskFactory setProteinDomainsColorTaskFactory = new SetProteinDomainsColorCommandTaskFactory();
		registerAllServices(bc, setProteinDomainsColorTaskFactory, setProteinDomainsColorProperties);

		// Register load ptms function
		Properties loadPTMsProperties = new Properties();
		loadPTMsProperties.setProperty(COMMAND_NAMESPACE, SOFTWARE_COMMAND_NAMESPACE);
		loadPTMsProperties.setProperty(COMMAND, "loadPTMs");
		loadPTMsProperties.setProperty(COMMAND_DESCRIPTION, LoadPTMsCommandTaskFactory.DESCRIPTION);
		loadPTMsProperties.setProperty(COMMAND_LONG_DESCRIPTION, LoadPTMsCommandTaskFactory.LONG_DESCRIPTION);
		loadPTMsProperties.setProperty(COMMAND_EXAMPLE_JSON, LoadPTMsCommandTask.getExample());
		loadPTMsProperties.setProperty(COMMAND_SUPPORTS_JSON, "true");

		TaskFactory loadPTMsTaskFactory = new LoadPTMsCommandTaskFactory(cyApplicationManager);
		registerAllServices(bc, loadPTMsTaskFactory, loadPTMsProperties);

		// Register export PTM(s) function
		Properties exportPTMsProperties = new Properties();
		exportPTMsProperties.setProperty(COMMAND_NAMESPACE, SOFTWARE_COMMAND_NAMESPACE);
		exportPTMsProperties.setProperty(COMMAND, "exportPTMs");
		exportPTMsProperties.setProperty(COMMAND_DESCRIPTION, ExportPTMsCommandTaskFactory.DESCRIPTION);
		exportPTMsProperties.setProperty(COMMAND_LONG_DESCRIPTION, ExportPTMsCommandTaskFactory.LONG_DESCRIPTION);
		exportPTMsProperties.setProperty(COMMAND_EXAMPLE_JSON, ExportPTMsCommandTask.getExample());
		exportPTMsProperties.setProperty(COMMAND_SUPPORTS_JSON, "true");

		TaskFactory exportPTMsTaskFactory = new ExportPTMsCommandTaskFactory(cyApplicationManager);
		registerAllServices(bc, exportPTMsTaskFactory, exportPTMsProperties);

		// Register load monolinks function
		Properties loadMonolinksProperties = new Properties();
		loadMonolinksProperties.setProperty(COMMAND_NAMESPACE, SOFTWARE_COMMAND_NAMESPACE);
		loadMonolinksProperties.setProperty(COMMAND, "loadMonolinks");
		loadMonolinksProperties.setProperty(COMMAND_DESCRIPTION, LoadMonolinksCommandTaskFactory.DESCRIPTION);
		loadMonolinksProperties.setProperty(COMMAND_LONG_DESCRIPTION, LoadMonolinksCommandTaskFactory.LONG_DESCRIPTION);
		loadMonolinksProperties.setProperty(COMMAND_EXAMPLE_JSON, LoadMonolinksCommandTask.getExample());
		loadMonolinksProperties.setProperty(COMMAND_SUPPORTS_JSON, "true");

		TaskFactory loadMonolinkssTaskFactory = new LoadMonolinksCommandTaskFactory(cyApplicationManager);
		registerAllServices(bc, loadMonolinkssTaskFactory, loadMonolinksProperties);

		// Register export monolink(s) function
		Properties exportMonolinksProperties = new Properties();
		exportMonolinksProperties.setProperty(COMMAND_NAMESPACE, SOFTWARE_COMMAND_NAMESPACE);
		exportMonolinksProperties.setProperty(COMMAND, "exportMonolinks");
		exportMonolinksProperties.setProperty(COMMAND_DESCRIPTION, ExportMonolinksCommandTaskFactory.DESCRIPTION);
		exportMonolinksProperties.setProperty(COMMAND_LONG_DESCRIPTION,
				ExportMonolinksCommandTaskFactory.LONG_DESCRIPTION);
		exportMonolinksProperties.setProperty(COMMAND_EXAMPLE_JSON, ExportMonolinksCommandTask.getExample());
		exportMonolinksProperties.setProperty(COMMAND_SUPPORTS_JSON, "true");

		TaskFactory exportMonolinksTaskFactory = new ExportMonolinksCommandTaskFactory(cyApplicationManager);
		registerAllServices(bc, exportMonolinksTaskFactory, exportMonolinksProperties);

	}

	private void init_default_params(BundleContext bc) {

		try {
			MainProps = (Properties) getService(bc, CyProperty.class, "(cyPropertyName=p2location.props)");

		} catch (Exception e) {
			Properties propsReaderServiceProps = null;
			if (MainProps == null) {
				cm = new ConfigurationManager(SOFTWARE_COMMAND_NAMESPACE, "p2location.props");
				propsReaderServiceProps = new Properties();
				propsReaderServiceProps.setProperty("cyPropertyName", "p2location.props");

				Locale usEnglish = new Locale("en", "US");
				cm.getProperties().setProperty("locale", usEnglish.getLanguage() + usEnglish.getCountry());
				MainProps = cm.getProperties();

				registerAllServices(bc, cm, propsReaderServiceProps);
			}
		}
	}
}