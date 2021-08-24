package de.fmp.liulab.utils;

import java.awt.Component;
import java.text.DecimalFormat;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * Class responsible for formatting table cell
 * @author diogobor
 *
 */
public class DecimalFormatRenderer extends DefaultTableCellRenderer {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final DecimalFormat formatter = new DecimalFormat("#.000");

	/**
	 * Return customized table cell
	 */
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int column) {

		// First format the cell value as required

		value = formatter.format((Number) value);

		// And pass it on to parent class

		return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
	}
}
