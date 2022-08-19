package de.fmp.liulab.task;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTableUtil;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.view.model.VisualLexicon;
import org.cytoscape.view.presentation.customgraphics.CyCustomGraphics2Factory;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.presentation.property.values.BendFactory;
import org.cytoscape.view.presentation.property.values.HandleFactory;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;

import de.fmp.liulab.internal.view.JFrameWithoutMaxAndMinButton;
import de.fmp.liulab.model.Protein;
import de.fmp.liulab.utils.Util;

public class UpdateProteinInformationTask extends AbstractTask implements ActionListener {

	private CyApplicationManager cyApplicationManager;
	private CyNetwork myNetwork;
	private CyNetworkView netView;
	private HandleFactory handleFactory;
	private BendFactory bendFactory;
	public VisualStyle style;
	public VisualLexicon lexicon;

	private boolean validateProtein;

	private List<CyNode> nodes;

	public static CyNode node;

	// Window
	private JFrameWithoutMaxAndMinButton mainFrame;
	private JPanel mainPanel;

	public UpdateProteinInformationTask(CyApplicationManager cyApplicationManager,
			final VisualMappingManager vmmServiceRef, CyCustomGraphics2Factory<?> vgFactory, BendFactory bendFactory,
			HandleFactory handleFactory, boolean validateProtein) {
		this.cyApplicationManager = cyApplicationManager;
		this.netView = cyApplicationManager.getCurrentNetworkView();
		this.myNetwork = cyApplicationManager.getCurrentNetwork();
		style = vmmServiceRef.getCurrentVisualStyle();
		// Get the current Visual Lexicon
		lexicon = cyApplicationManager.getCurrentRenderingEngine().getVisualLexicon();
		this.bendFactory = bendFactory;
		this.handleFactory = handleFactory;
		this.validateProtein = validateProtein;

		if (mainFrame == null)
			mainFrame = new JFrameWithoutMaxAndMinButton(new JFrame(), "P2Location - Set predicted protein domains", 0);

		mainFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		Dimension appSize = null;

		if (Util.isWindows()) {
			appSize = new Dimension(540, 465);
		} else if (Util.isMac()) {
			appSize = new Dimension(520, 435);
		} else {
			appSize = new Dimension(520, 460);
		}

		mainFrame.setSize(appSize);
		mainFrame.setResizable(false);

		if (mainPanel == null)
			mainPanel = new JPanel();
		mainPanel.setBounds(10, 10, 490, 365);
		mainPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), ""));
		mainPanel.setLayout(null);

		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		mainFrame.setLocation((screenSize.width - appSize.width) / 2, (screenSize.height - appSize.height) / 2);

	}

	@Override
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void run(TaskMonitor taskMonitor) throws Exception {

		taskMonitor.setTitle("P2Location - Update Protein Information task");

		if (cyApplicationManager.getCurrentNetwork() == null) {
			throw new Exception("ERROR: No network has been loaded.");
		}

		checkSingleOrMultipleSelectedNodes(taskMonitor);

	}

	/**
	 * Method responsible for checking how many nodes have been selected
	 * 
	 * @param taskMonitor
	 * @throws Exception
	 */
	private void checkSingleOrMultipleSelectedNodes(final TaskMonitor taskMonitor) throws Exception {

		nodes = CyTableUtil.getNodesInState(myNetwork, CyNetwork.SELECTED, true);

		if (nodes.size() == 0) {

			taskMonitor.showMessage(TaskMonitor.Level.INFO, "No node has been selected.");

		} else if (nodes.size() > 1) {

			executeMultipleNodes(taskMonitor);

		} else {
			node = nodes.get(0);
			executeSingleNode(taskMonitor);
		}

	}

	/**
	 * Method responsible for executing layout to multiple nodes
	 * 
	 * @param taskMonitor
	 * @throws Exception
	 */
	private void executeMultipleNodes(final TaskMonitor taskMonitor) throws Exception {

		int old_progress = 0;
		int summary_processed = 0;
		int total_rows = nodes.size();

		for (CyNode current_node : nodes) {
			node = current_node;
			executeSingleNode(taskMonitor);

			summary_processed++;
			Util.progressBar(summary_processed, old_progress, total_rows, "Updating protein information: ", taskMonitor,
					null);
		}
	}

	/**
	 * Method responsible for executing layout to a single node
	 * 
	 * @param taskMonitor
	 * @throws Exception
	 */
	private void executeSingleNode(final TaskMonitor taskMonitor) throws Exception {

		// ##### GET THE SELECTED NODE - ONLY ONE IS POSSIBLE TO APPLY CHANGES ######

		if (myNetwork == null || node == null || netView == null)
			return;

		CyRow myCurrentRow = myNetwork.getRow(node);
		if (myCurrentRow == null)
			return;

		String nodeName = (String) myCurrentRow.getRaw(CyNetwork.NAME);
		Protein myProtein = Util.getProtein(myNetwork, nodeName);
		if (myProtein == null) {
			throw new Exception(
					"There is no information in column 'sequence' or 'domain_annotation' for the protein: " + nodeName);
		}

		if (validateProtein) {

			myProtein.isValid = !myProtein.isValid;

			View<CyNode> new_node_source_view = netView.getNodeView(node);
			if (myProtein.isValid)
				new_node_source_view.setLockedValue(BasicVisualLexicon.NODE_VISIBLE, true);
			else
				new_node_source_view.setLockedValue(BasicVisualLexicon.NODE_VISIBLE, false);

			Util.restoreDefaultStyle(null, style, netView, cyApplicationManager, new_node_source_view, handleFactory,
					bendFactory, node, myNetwork, myCurrentRow, lexicon);
			Util.updateValidProteinInformationInCytoScapeNodeTable(myNetwork, node, myProtein,
					Util.VALID_PROTEINS_COLUMN);
		} else {

			this.init_predictedDomain_window(taskMonitor);

			// TODO: Create Window to set protein domains
//			@SuppressWarnings("unchecked")
//			List<String> domains = (List<String>) myCurrentRow.getRaw(Util.PREDICTED_PROTEIN_DOMAIN_COLUMN);
//			Util.updateProteinDomains(myNetwork, node, domains);
		}

	}

	/**
	 * Method responsible for opening the Single Node Layout window
	 * 
	 * @param taskMonitor
	 */
	private void init_predictedDomain_window(final TaskMonitor taskMonitor) {

//		isPlotDone = false;
//		setFrameObjects(taskMonitor);
		// Display the window
		mainFrame.add(mainPanel, BorderLayout.CENTER);
		mainFrame.setLocationRelativeTo(null);
		mainFrame.setVisible(true);
	}
}
