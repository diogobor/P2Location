package de.fmp.liulab.internal.view;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.ListCellRenderer;
import javax.swing.UIManager;
import javax.swing.table.JTableHeader;

/**
 * Class responsible for controlling JTable RowHeader
 * @author diogobor
 *
 */
public class JTableRowRenderer extends JLabel implements ListCellRenderer<Object> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Constructor
	 * @param table table reference
	 */
	public JTableRowRenderer(JTable table) {
		JTableHeader header = table.getTableHeader();
		setOpaque(true);
		setBorder(UIManager.getBorder("TableHeader.cellBorder"));
		setHorizontalAlignment(CENTER);
		setForeground(header.getForeground());
		setBackground(header.getBackground());
		setFont(header.getFont());
	}

	/**
	 * Method responsible for getting current component.
	 */
	@Override
	public Component getListCellRendererComponent(JList<?> list, Object obj, int index, boolean selected,
			boolean focused) {
		setText((obj == null) ? "" : obj.toString());
		return this;
	}

}
