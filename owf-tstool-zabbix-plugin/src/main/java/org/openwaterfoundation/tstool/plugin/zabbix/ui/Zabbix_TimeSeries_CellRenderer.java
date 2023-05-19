// Zabbix_TimeSeries_CellRenderer - cell renderer for the time series list table model

/* NoticeStart

OWF TSTool Zabbix Plugin
Copyright (C) 2023 Open Water Foundation

OWF TSTool Zabbix Plugin is free software:  you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

OWF TSTool Zabbix Plugin is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

You should have received a copy of the GNU General Public License
    along with OWF TSTool Zabbix Plugin.  If not, see <https://www.gnu.org/licenses/>.

NoticeEnd */

package org.openwaterfoundation.tstool.plugin.zabbix.ui;

import RTi.Util.GUI.JWorksheet_AbstractExcelCellRenderer;

/**
This class is used to render cells for Zabbix_TimeSeries_TableModel data.
*/
@SuppressWarnings("serial")
public class Zabbix_TimeSeries_CellRenderer extends JWorksheet_AbstractExcelCellRenderer {

	private Zabbix_TimeSeries_TableModel tableModel = null;

	/**
	Constructor.
	@param tableModel The TSTool_ReclamationPisces_TableModel to render.
	*/
	public Zabbix_TimeSeries_CellRenderer ( Zabbix_TimeSeries_TableModel tableModel ) {
		this.tableModel = tableModel;
	}

	/**
	Returns the format for a given column.
	@param column the column for which to return the format.
	@return the column format as used by StringUtil.formatString().
	*/
	public String getFormat(int column) {
		return this.tableModel.getFormat(column);
	}

	/**
	Returns the widths of the columns in the table.
	@return an integer array of the widths of the columns in the table.
	*/
	public int[] getColumnWidths() {
		return this.tableModel.getColumnWidths();
	}

}