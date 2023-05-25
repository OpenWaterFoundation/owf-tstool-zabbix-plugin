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

import java.util.List;

import org.openwaterfoundation.tstool.plugin.zabbix.dao.ItemType;
import org.openwaterfoundation.tstool.plugin.zabbix.dao.TimeSeriesCatalog;
import org.openwaterfoundation.tstool.plugin.zabbix.dao.ValueType;
import org.openwaterfoundation.tstool.plugin.zabbix.datastore.ZabbixDataStore;

import RTi.Util.GUI.JWorksheet_AbstractRowTableModel;

/**
This class is a table model for time series header information for Zabbix REST web services time series.
By default the sheet will contain row and column numbers.
*/
@SuppressWarnings({ "serial", "rawtypes" })
public class Zabbix_TimeSeries_TableModel extends JWorksheet_AbstractRowTableModel {
	
	/**
	Number of columns in the table model.
	*/
	private final int COLUMNS = 23;

	public final int COL_LOCATION_ID = 0;
	public final int COL_DATA_SOURCE = 1;
	public final int COL_DATA_TYPE = 2;
	public final int COL_DATA_INTERVAL = 3;
	public final int COL_UNITS = 4;

	public final int COL_HOST_NAME = 5;
	public final int COL_HOST_ID = 6;
	public final int COL_HOST_DESCRIPTION = 7;

	// Put host group after host because host group name is already the data source and
	// having the host name near the front is useful.
	public final int COL_HOST_GROUP_NAME = 8;
	public final int COL_HOST_GROUP_ID = 9;

	public final int COL_ITEM_DELAY = 10;
	public final int COL_ITEM_HISTORY = 11;
	public final int COL_ITEM_ID = 12;
	public final int COL_ITEM_KEY = 13;
	public final int COL_ITEM_STATUS = 14;
	public final int COL_ITEM_TEMPLATE_ID = 15;
	public final int COL_ITEM_TEMPLATE_NAME = 16;
	public final int COL_ITEM_TRENDS = 17;
	public final int COL_ITEM_TYPE = 18;
	public final int COL_ITEM_UNITS = 19;
	public final int COL_ITEM_VALUE_TYPE = 20;

	public final int COL_PROBLEMS = 21;
	public final int COL_DATASTORE = 22;
	
	/**
	Datastore corresponding to datastore used to retrieve the data.
	*/
	ZabbixDataStore datastore = null;

	/**
	Data are a list of TimeSeriesCatalog.
	*/
	private List<TimeSeriesCatalog> timeSeriesCatalogList = null;

	/**
	Constructor.  This builds the model for displaying the given Zabbix time series data.
	@param dataStore the data store for the data
	@param data the list of Zabbix TimeSeriesCatalog that will be displayed in the table.
	@throws Exception if an invalid results passed in.
	*/
	@SuppressWarnings("unchecked")
	public Zabbix_TimeSeries_TableModel ( ZabbixDataStore dataStore, List<? extends Object> data ) {
		if ( data == null ) {
			_rows = 0;
		}
		else {
		    _rows = data.size();
		}
	    this.datastore = dataStore;
		_data = data; // Generic
		// TODO SAM 2016-04-17 Need to use instanceof here to check.
		this.timeSeriesCatalogList = (List<TimeSeriesCatalog>)data;
	}

	/**
	From AbstractTableModel.  Returns the class of the data stored in a given column.
	@param columnIndex the column for which to return the data class.
	*/
	@SuppressWarnings({ "unchecked" })
	public Class getColumnClass (int columnIndex) {
		switch (columnIndex) {
			// List in the same order as top of the class.
			default: return String.class; // All others.
		}
	}

	/**
	From AbstractTableMode.  Returns the number of columns of data.
	@return the number of columns of data.
	*/
	public int getColumnCount() {
		return this.COLUMNS;
	}

	/**
	From AbstractTableMode.  Returns the name of the column at the given position.
	@return the name of the column at the given position.
	*/
	public String getColumnName(int columnIndex) {
		switch (columnIndex) {
			case COL_LOCATION_ID: return "Host";
			case COL_DATA_SOURCE: return "Host Group";
			case COL_DATA_TYPE: return "Item Name";
			case COL_DATA_INTERVAL: return "Interval";
			case COL_UNITS: return "Units";

			case COL_HOST_NAME: return "Host Name";
			case COL_HOST_ID: return "Host ID";
			case COL_HOST_DESCRIPTION: return "Host Description";

			case COL_HOST_GROUP_NAME: return "Host Group Name";
			case COL_HOST_GROUP_ID: return "Host Group ID";

			case COL_ITEM_DELAY: return "Item Delay";
			case COL_ITEM_HISTORY: return "Item History";
			case COL_ITEM_ID: return "Item ID";
			case COL_ITEM_KEY: return "Item Key";
			case COL_ITEM_STATUS: return "Item Status";
			case COL_ITEM_TEMPLATE_ID: return "Item Template ID";
			case COL_ITEM_TEMPLATE_NAME: return "Item Template Name";
			case COL_ITEM_TRENDS: return "Item Trends";
			case COL_ITEM_TYPE: return "Item Type";
			case COL_ITEM_UNITS: return "Item Units";
			case COL_ITEM_VALUE_TYPE: return "Item Value Type";

			case COL_PROBLEMS: return "Problems";
			case COL_DATASTORE: return "Datastore";

			default: return "";
		}
	}

	/**
	Returns an array containing the column widths (in number of characters).
	@return an integer array containing the widths for each field.
	*/
	public String[] getColumnToolTips() {
	    String[] toolTips = new String[this.COLUMNS];
	    toolTips[COL_LOCATION_ID] = "Location identifier (host.host)";
	    toolTips[COL_DATA_SOURCE] = "Data source (hostgroup.name)";
	    toolTips[COL_DATA_TYPE] = "Data type (item.name)";
	    toolTips[COL_DATA_INTERVAL] = "Data interval";
	    toolTips[COL_UNITS] = "Units (item.units)";

	    toolTips[COL_HOST_NAME] = "Host name (host.name)";
	    toolTips[COL_HOST_ID] = "Host ID (host.hostid)";
	    toolTips[COL_HOST_DESCRIPTION] = "Host description (host.description)";

	    toolTips[COL_HOST_GROUP_NAME] = "Host group name (hostgroup.name)";
	    toolTips[COL_HOST_GROUP_ID] = "Host group ID (hostgroup.groupid";

		toolTips[COL_ITEM_DELAY] = "Item delay (item.delay)";
		toolTips[COL_ITEM_HISTORY] = "Item history duration (item.history)";
		toolTips[COL_ITEM_ID] = "Item ID (item.itemid)";
		toolTips[COL_ITEM_KEY] = "Item key (item.key)";
		toolTips[COL_ITEM_STATUS] = "Item status (item.status, 0=enabled, 1=disabled)";
		toolTips[COL_ITEM_TEMPLATE_ID] = "Item template ID (item.templateid)";
		toolTips[COL_ITEM_TEMPLATE_NAME] = "Item template name (from item.templateid)";
		toolTips[COL_ITEM_TRENDS] = "Item trends save duration (item.trend)";
		toolTips[COL_ITEM_TYPE] = "Item type (item.type)";
		toolTips[COL_ITEM_UNITS] = "Item units (item.units)";
		toolTips[COL_ITEM_VALUE_TYPE] = "Item value type (item.value_type)";

		toolTips[COL_PROBLEMS] = "Problems";
		toolTips[COL_DATASTORE] = "Datastore name";
	    return toolTips;
	}

	/**
	Returns an array containing the column widths (in number of characters).
	@return an integer array containing the widths for each field.
	*/
	public int[] getColumnWidths() {
		int[] widths = new int[this.COLUMNS];
	    widths[COL_LOCATION_ID] = 11;
	    widths[COL_DATA_SOURCE] = 20;
	    widths[COL_DATA_TYPE] = 40;
	    widths[COL_DATA_INTERVAL] = 8;
	    widths[COL_UNITS] = 8;

	    widths[COL_HOST_NAME] = 30;
	    widths[COL_HOST_ID] = 10;
	    widths[COL_HOST_DESCRIPTION] = 20;

	    widths[COL_HOST_GROUP_NAME] = 25;
	    widths[COL_HOST_GROUP_ID] = 10;

	    widths[COL_ITEM_DELAY] = 8;
	    widths[COL_ITEM_HISTORY] = 9;
	    widths[COL_ITEM_ID] = 6;
	    widths[COL_ITEM_KEY] = 10;
	    widths[COL_ITEM_STATUS] = 8;
	    widths[COL_ITEM_TEMPLATE_ID] = 12;
	    widths[COL_ITEM_TEMPLATE_NAME] = 15;
	    widths[COL_ITEM_TRENDS] = 8;
	    widths[COL_ITEM_TYPE] = 15;
	    widths[COL_ITEM_UNITS] = 6;
	    widths[COL_ITEM_VALUE_TYPE] = 10;

		widths[COL_PROBLEMS] = 30;
		widths[COL_DATASTORE] = 10;
		return widths;
	}

	/**
	Returns the format to display the specified column.
	@param column column for which to return the format.
	@return the format (as used by StringUtil.formatString()).
	*/
	public String getFormat ( int column ) {
		switch (column) {
			default: return "%s"; // All else are strings.
		}
	}

	/**
	From AbstractTableMode.  Returns the number of rows of data in the table.
	*/
	public int getRowCount() {
		return _rows;
	}

	/**
	From AbstractTableMode.  Returns the data that should be placed in the JTable at the given row and column.
	@param row the row for which to return data.
	@param col the column for which to return data.
	@return the data that should be placed in the JTable at the given row and column.
	*/
	public Object getValueAt(int row, int col) {
		// Make sure the row numbers are never sorted.
		if (_sortOrder != null) {
			row = _sortOrder[row];
		}

		TimeSeriesCatalog timeSeriesCatalog = this.timeSeriesCatalogList.get(row);
		switch (col) {
			// OK to allow null because will be displayed as blank.
			case COL_LOCATION_ID: return timeSeriesCatalog.getLocId();
			case COL_DATA_SOURCE: return timeSeriesCatalog.getDataSource();
			case COL_DATA_TYPE: return timeSeriesCatalog.getDataType();
			case COL_DATA_INTERVAL: return timeSeriesCatalog.getDataInterval();
			case COL_UNITS: return timeSeriesCatalog.getDataUnits();

			case COL_HOST_NAME: return timeSeriesCatalog.getHostName();
			case COL_HOST_ID: return timeSeriesCatalog.getHostId();
			case COL_HOST_DESCRIPTION: return timeSeriesCatalog.getHostDescription();

			case COL_HOST_GROUP_NAME: return timeSeriesCatalog.getHostGroupName();
			case COL_HOST_GROUP_ID: return timeSeriesCatalog.getHostGroupId();

			case COL_ITEM_DELAY: return timeSeriesCatalog.getItemDelay();
			case COL_ITEM_HISTORY: return timeSeriesCatalog.getItemHistory();
			case COL_ITEM_ID: return timeSeriesCatalog.getItemId();
			case COL_ITEM_KEY: return timeSeriesCatalog.getItemKey();
			case COL_ITEM_STATUS: return timeSeriesCatalog.getItemStatus();
			case COL_ITEM_TEMPLATE_ID: return timeSeriesCatalog.getItemTemplateId();
			case COL_ITEM_TEMPLATE_NAME: return timeSeriesCatalog.getItemTemplateName();
			case COL_ITEM_TRENDS: return timeSeriesCatalog.getItemTrends();
			case COL_ITEM_TYPE:
				ItemType itemType = ItemType.valueOfIgnoreCase(timeSeriesCatalog.getItemType());
				return itemType.toString() + " (" + timeSeriesCatalog.getItemType() + ")";
			case COL_ITEM_UNITS: return timeSeriesCatalog.getItemUnits();
			//case COL_ITEM_NAME: return timeSeriesCatalog.getItemName();
			case COL_ITEM_VALUE_TYPE:
				ValueType valueType = ValueType.valueOfIgnoreCase(timeSeriesCatalog.getItemValueType());
				return valueType.toString() + " (" + timeSeriesCatalog.getItemValueType() + ")";

			case COL_PROBLEMS: return timeSeriesCatalog.formatProblems();			
			case COL_DATASTORE: return this.datastore.getName();			
			default: return "";
		}
	}

}