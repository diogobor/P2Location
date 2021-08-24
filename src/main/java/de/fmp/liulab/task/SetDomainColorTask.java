package de.fmp.liulab.task;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Map.Entry;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.view.presentation.customgraphics.CyCustomGraphics2Factory;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;

import de.fmp.liulab.internal.view.JFrameWithoutMaxAndMinButton;
import de.fmp.liulab.internal.view.MenuBar;
import de.fmp.liulab.utils.Util;

/**
 * Class responsible for set color to protein domains
 * @author diogobor
 *
 */
public class SetDomainColorTask extends AbstractTask implements ActionListener {

	private CyApplicationManager cyApplicationManager;
	private CyNetwork myNetwork;
	private CyCustomGraphics2Factory vgFactory;

	// Window
	private JFrameWithoutMaxAndMinButton mainFrame;
	private JPanel mainPanel;
	private MenuBar menuBar = new MenuBar();

	// Table
	private static JTable mainProteinDomainTable;
	public static DefaultTableModel tableDataModel;
	private String[] columnNames = { "Protein domain", "Color" };
	private final Class[] columnClass = new Class[] { String.class, String.class };
	private static JList rowHeader;
	private static JScrollPane proteinDomainTableScrollPanel;

	/**
	 * Constructor
	 * 
	 * @param cyApplicationManager main app manager
	 * @param vmmServiceRef visual manager
	 * @param vgFactory visual factory
	 */
	public SetDomainColorTask(CyApplicationManager cyApplicationManager, final VisualMappingManager vmmServiceRef,
			CyCustomGraphics2Factory vgFactory) {
		this.cyApplicationManager = cyApplicationManager;
		this.myNetwork = cyApplicationManager.getCurrentNetwork();
		this.vgFactory = vgFactory;

		if (mainFrame == null)
			mainFrame = new JFrameWithoutMaxAndMinButton(new JFrame(), "P2Location - Protein domains colors", -1);

		if (myNetwork == null) {
			return;
		}
		if (Util.proteinDomainsColorMap.size() == 0) {
			JOptionPane.showMessageDialog(null, "No protein domain has been loaded!",
					"P2Location - Protein domains colors", JOptionPane.WARNING_MESSAGE);
			return;
		}

		init_frame();

	}

	/**
	 * Start JFrame
	 */
	private void init_frame() {

		mainFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		Dimension appSize = null;
		if (Util.isWindows()) {
			appSize = new Dimension(375, 330);
		} else if(Util.isMac()){
			appSize = new Dimension(360, 315);
		}else {
			appSize = new Dimension(360, 325);
		}
		mainFrame.setSize(appSize);
		mainFrame.setResizable(false);

		if (mainPanel == null)
			mainPanel = new JPanel();
		mainPanel.setBounds(10, 10, 350, 335);
		mainPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), ""));
		mainPanel.setLayout(null);

		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		mainFrame.setLocation((screenSize.width - appSize.width) / 2, (screenSize.height - appSize.height) / 2);
		mainFrame.setVisible(true); 
	}

	/**
	 * Method responsible for loading protein domain colors
	 */
	private void loadProteinDomainsColor() {

		Object[][] data = new Object[Util.proteinDomainsColorMap.size()][2];
		tableDataModel.setDataVector(data, columnNames);
		int countPtnDomain = 0;
		for (Entry<String, Color> domain : Util.proteinDomainsColorMap.entrySet()) {

			tableDataModel.setValueAt(domain.getKey(), countPtnDomain, 0);

			Color current_color = domain.getValue();
			if (current_color != null) {
				String colorStr = current_color.getRed() + "#" + current_color.getGreen() + "#"
						+ current_color.getBlue() + "#" + current_color.getAlpha();
				tableDataModel.setValueAt(colorStr, countPtnDomain, 1);
			}
			countPtnDomain++;
		}

		setTableProperties(Util.proteinDomainsColorMap.size());

	}

	/**
	 * Method responsible for activating action.
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		this.myNetwork = cyApplicationManager.getCurrentNetwork();

		if (myNetwork == null) {
			return;
		}
	}

	/**
	 * Method responsible for running task
	 */
	@Override
	public void run(TaskMonitor taskMonitor) throws Exception {

		taskMonitor.setTitle("P2Location - Set domains colors task");

		if (cyApplicationManager.getCurrentNetwork() == null) {
			taskMonitor.showMessage(TaskMonitor.Level.WARN, "No network has been loaded.");
			return;
		}

		if (Util.proteinDomainsColorMap.size() == 0) {
			taskMonitor.showMessage(TaskMonitor.Level.WARN, "No protein domain has been loaded.");
			return;
		}

		setFrameObjects(taskMonitor);

		// Display the window
		mainFrame.add(mainPanel, BorderLayout.CENTER);
		mainFrame.setLocationRelativeTo(null);
		mainFrame.setVisible(true);
	}

	/**
	 * Set all objects to main Frame
	 * 
	 * @param taskMonitor
	 */
	private void setFrameObjects(final TaskMonitor taskMonitor) {

		initFrameLabels();

		initTableScreen();

		initButtons(taskMonitor);

		loadProteinDomainsColor();
	}

	/**
	 * Set all labels in P2Location window / frame
	 */
	private void initFrameLabels() {

		JPanel logo_panel = new JPanel();
		logo_panel.setBorder(BorderFactory.createTitledBorder(""));
		logo_panel.setBounds(10, 10, 335, 112);
		logo_panel.setLayout(null);
		mainPanel.add(logo_panel);

		JLabel jLabelIcon = new JLabel();
		jLabelIcon.setBounds(120, -95, 300, 300);
		jLabelIcon.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/logo.png")));
		logo_panel.add(jLabelIcon);
	}

	/**
	 * Method responsible for initializing the table in the Frame
	 */
	private void initTableScreen() {

		Object[][] data = new Object[1][2];
		// create table model with data
		tableDataModel = new DefaultTableModel(data, columnNames) {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
			}

			@Override
			public Class<?> getColumnClass(int columnIndex) {
				return columnClass[columnIndex];
			}
		};

		mainProteinDomainTable = new JTable(tableDataModel);

		mainProteinDomainTable.addMouseListener(new java.awt.event.MouseAdapter() {
			@Override
			public void mouseClicked(java.awt.event.MouseEvent evt) {
				int viewRow = mainProteinDomainTable.rowAtPoint(evt.getPoint());
				int viewColumn = mainProteinDomainTable.columnAtPoint(evt.getPoint());
				if (viewColumn == 1) {
					String currentColor = (String) tableDataModel.getValueAt(viewRow, viewColumn);
					Color initialcolor = Color.RED;
					if (currentColor != null && !currentColor.equals("")) {
						String[] cols = currentColor.split("#");
						initialcolor = new Color(Integer.parseInt(cols[0]), Integer.parseInt(cols[1]),
								Integer.parseInt(cols[2]), Integer.parseInt(cols[3]));
					}

					Color color = JColorChooser.showDialog(null, "Select a color", initialcolor);
					String colorStr = color.getRed() + "#" + color.getGreen() + "#" + color.getBlue() + "#"
							+ color.getAlpha();
					tableDataModel.setValueAt(colorStr, viewRow, viewColumn);
				}

			}
		});

		// Create the scroll pane and add the table to it.
		proteinDomainTableScrollPanel = new JScrollPane();
		proteinDomainTableScrollPanel.setBounds(10, 130, 335, 105);
		proteinDomainTableScrollPanel.setViewportView(mainProteinDomainTable);
		proteinDomainTableScrollPanel.setRowHeaderView(rowHeader);
		setTableProperties(1);
		mainPanel.add(proteinDomainTableScrollPanel);
	}

	/**
	 * Set properties to the Node domain table
	 * @param number_lines total number of lines
	 */
	public static void setTableProperties(int number_lines) {
		if (mainProteinDomainTable != null) {
			mainProteinDomainTable.setPreferredScrollableViewportSize(new Dimension(335, 90));
			mainProteinDomainTable.getColumnModel().getColumn(0).setPreferredWidth(195);
			mainProteinDomainTable.getColumnModel().getColumn(1).setPreferredWidth(140);
			mainProteinDomainTable.setFillsViewportHeight(true);
			mainProteinDomainTable.setAutoCreateRowSorter(true);

			Util.updateRowHeader(number_lines, mainProteinDomainTable, rowHeader, proteinDomainTableScrollPanel);
		}
	}

	/**
	 * Method responsible for initializing all button in the Frame
	 * 
	 * @param taskMonitor
	 */
	private void initButtons(final TaskMonitor taskMonitor) {

		Icon iconBtnOk = new ImageIcon(getClass().getResource("/images/okBtn.png"));
		JButton okButton = new JButton(iconBtnOk);
		okButton.setText("OK");
		okButton.setBounds(30, 250, 140, 25);

		okButton.addMouseListener(new MouseAdapter() {

			public void mouseClicked(MouseEvent e) {

				String msgError = "";
				try {

					taskMonitor.showMessage(TaskMonitor.Level.INFO, "Updating colors of protein domains...");
					msgError = updateDomainsFromTable();
					if (!msgError.isBlank() && !msgError.isEmpty()) {
						taskMonitor.showMessage(TaskMonitor.Level.ERROR, msgError);
					}

				} catch (Exception e1) {
					taskMonitor.showMessage(TaskMonitor.Level.ERROR, "ERROR: " + e1.getMessage());
					msgError += e1.getMessage();
				}

				if (msgError.isBlank() && msgError.isEmpty()) {
					mainFrame.dispose();
					JOptionPane.showMessageDialog(null, "Colors of protein domains have been updated successfully!",
							"P2Location - Protein domains colors", JOptionPane.INFORMATION_MESSAGE);
				}
			}
		});
		mainPanel.add(okButton);

		Icon iconBtnCancel = new ImageIcon(getClass().getResource("/images/cancelBtn.png"));
		JButton cancelButton = new JButton(iconBtnCancel);
		cancelButton.setText("Cancel");
		cancelButton.setBounds(180, 250, 140, 25);

		cancelButton.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {

				mainFrame.dispose();
			}
		});
		mainPanel.add(cancelButton);
	}

	/**
	 * Get all nodes filled out in JTable
	 */
	private String updateDomainsFromTable() {

		StringBuilder sbError = new StringBuilder();

		for (int row = 0; row < tableDataModel.getRowCount(); row++) {
			try {
				String proteinDomain = tableDataModel.getValueAt(row, 0) != null
						? tableDataModel.getValueAt(row, 0).toString()
						: "";

				String _color = tableDataModel.getValueAt(row, 1) != null ? tableDataModel.getValueAt(row, 1).toString()
						: "";

				if (!_color.isBlank() && !_color.isEmpty()) {
					String[] colorStr = tableDataModel.getValueAt(row, 1).toString().split("#");
					Color color = new Color(Integer.parseInt(colorStr[0]), Integer.parseInt(colorStr[1]),
							Integer.parseInt(colorStr[2]), 100);

					if (!proteinDomain.isBlank() && !proteinDomain.isEmpty()) {
						Util.proteinDomainsColorMap.put(proteinDomain, color);
					}
				}
			} catch (Exception e) {
				sbError.append(e.getMessage() + "\n");
			}
		}
		return sbError.toString();
	}
}
