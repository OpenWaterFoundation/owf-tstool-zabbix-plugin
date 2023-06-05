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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openwaterfoundation.tstool.plugin.zabbix.dao.Host;
import org.openwaterfoundation.tstool.plugin.zabbix.dao.HostGroup;
import org.openwaterfoundation.tstool.plugin.zabbix.datastore.ZabbixDataStore;

import RTi.Util.GUI.InputFilter;
import RTi.Util.GUI.InputFilter_JPanel;
import RTi.Util.Message.Message;
import RTi.Util.String.StringUtil;

/**
This class is an input filter for querying ReclamationPisces.
*/
@SuppressWarnings("serial")
public class Zabbix_TimeSeries_InputFilter_JPanel extends InputFilter_JPanel {

	/**
	Datastore for connection.
	*/
	private ZabbixDataStore datastore = null;

	/**
	Constructor for case when no datastore is configured - default panel.
	@param label label for the panel
	*/
	public Zabbix_TimeSeries_InputFilter_JPanel( String label ) {
		super(label);
	}

	/**
	Constructor.
	@param dataStore the data store to use to connect to the test database.  Cannot be null.
	@param numFilterGroups the number of filter groups to display
	*/
	public Zabbix_TimeSeries_InputFilter_JPanel( ZabbixDataStore dataStore, int numFilterGroups ) {
	    super();
	    this.datastore = dataStore;
	    if ( this.datastore != null ) {
	        setFilters ( numFilterGroups );
	    }
	}

	/**
 	* Check the input filter for appropriate combination of choices.
 	* These checks can be performed in the ReadZabbix command and the main TSTool UI,
 	* both of which use this class.
 	* The data type and interval are not checked
 	* @param displayWarning If true, display a warning dialog if there are errors in the input.
 	* If false, do not display a warning, in which case the calling code should generally display a warning and optionally
 	* also perform other checks by overriding this method.
 	* @return null if no issues or a string that indicates issues,
 	* can use \n for line breaks and put at the front of the string.
 	*/
	@Override
	public String checkInputFilters ( boolean displayWarning ) {
		String dataType = null;
		String dataInterval = null;
		return this.checkInputFilters ( dataType, dataInterval, displayWarning );
	}

	/**
 	* Check the input filter for appropriate combination of choices.
 	* These checks can be performed in the ReadZabbix command and the main TSTool UI,
 	* both of which use this class.
 	* @param dataType that is selected
 	* @param dataInterval that is selected
 	* @param displayWarning If true, display a warning dialog if there are errors in the input.
 	* If false, do not display a warning, in which case the calling code should generally display a warning and optionally
 	* also perform other checks by overriding this method.
 	* @return null if no issues or a string that indicates issues,
 	* can use \n for line breaks and put at the front of the string.
 	*/
	public String checkInputFilters ( String dataType, String dataInterval, boolean displayWarning ) {
		// Use the parent class method to check basic input types based on data types:
		// - will return empty string if no issues
		String warning = super.checkInputFilters(displayWarning);
		// Perform specific checks.
		String warning2 = "";

		// Check that at least one limiting filter is specified to constrain the size of the viewed time series catalog:
		// - otherwise the catalog can be very large (e.g., ~100,000 objects)
		int numInputFilters = 0;
		if( (dataType != null) && !dataType.equals("*") ) {
			++numInputFilters;
		}
		if ( hasInput() ) {
			// At least one input filter is specified.
			++numInputFilters;
		}
		if( numInputFilters == 0 ) {
			warning2 += "\nThe data type and/or one of the 'Where' filters must be specified to limit the size of the query.";
		}

		if ( !warning2.isEmpty() ) {
			// Have non-empty specific warnings so append specific warnings.
			warning += warning2;
		}
		// Return the general warnings or the appended results.
		return warning;
	}

	/**
	Set the filter data.  This method is called at setup and when refreshing the list with a new subject type.
	*/
	public void setFilters ( int numFilterGroups ) {
	    String routine = getClass().getSimpleName() + ".setFilters";
		
		// Read the host groups and other data to populate filter choices.
		
		List<Host> hosts = new ArrayList<>();
		try {
			hosts = this.datastore.getHosts(false);
		}
		catch ( IOException e ) {
			Message.printWarning(2, routine, "Exception getting the Zabbix host list");
			Message.printWarning(2, routine, e);
		}

		List<HostGroup> hostGroups = new ArrayList<>();
		try {
			hostGroups = this.datastore.getHostGroups(false);
		}
		catch ( IOException e ) {
			Message.printWarning(2, routine, "Exception getting the Zabbix host group list");
			Message.printWarning(2, routine, e);
		}
		
		// Create choices for the filters.
		
	    List<String> hostGroupNameChoices = new ArrayList<>();
	    List<String> hostNameChoices = new ArrayList<>();
	    List<String> hostChoices = new ArrayList<>();
	    
	    for ( Host host : hosts ) {
	    	hostNameChoices.add(host.getName());
	    	hostChoices.add(host.getHost());
	    }
	    Collections.sort(hostNameChoices, String.CASE_INSENSITIVE_ORDER);
	    Collections.sort(hostChoices, String.CASE_INSENSITIVE_ORDER);

	    for ( HostGroup hostGroup : hostGroups ) {
	    	hostGroupNameChoices.add(hostGroup.getName());
	    }
	    Collections.sort(hostGroupNameChoices, String.CASE_INSENSITIVE_ORDER);
		
		// The internal names for filters match the /tscatalog web service query parameters.
		// TODO smalers 2020-01-24 add more filters for points, point type, etc. as long as the web service API supports

	    List<InputFilter> filters = new ArrayList<>();

	    // Always add blank to top of filter
	    filters.add(new InputFilter("", "", StringUtil.TYPE_STRING, null, null, false)); // Blank

	    filters.add(new InputFilter("Host Group - Name",
            "hostgroup.name", "hostgroup.name", "hostgroup.name",
            StringUtil.TYPE_STRING, hostGroupNameChoices, hostGroupNameChoices, false));

	    filters.add(new InputFilter("Host - Host",
            "host.host", "host.host", "host.host",
            StringUtil.TYPE_STRING, hostChoices, hostChoices, false));

	    filters.add(new InputFilter("Host - Name",
            "host.name", "host.name", "host.name",
            StringUtil.TYPE_STRING, hostNameChoices, hostNameChoices, false));

	    // Additional filters to limit time series list to categories of time series.
	    
	    /*
  		List<String> listAlarmTsChoices = new ArrayList<>();
  		listAlarmTsChoices.add("");
  		listAlarmTsChoices.add("False");
  		listAlarmTsChoices.add("Only");
  		listAlarmTsChoices.add("True");
  		filters.add(new InputFilter(TimeSeriesCatalogFilterType.INCLUDE_ALARM_TS.getWhereDisplay(),
  		    "", TimeSeriesCatalogFilterType.INCLUDE_ALARM_TS.getFilterName(),
  		    StringUtil.TYPE_STRING, listAlarmTsChoices, listAlarmTsChoices, false,
  		    TimeSeriesCatalogFilterType.INCLUDE_ALARM_TS.getDescription() + " (default = " +
  		    TimeSeriesCatalogFilterType.INCLUDE_ALARM_TS.getDefaultState() + ")"));
  		*/

	  	setToolTipText("<html>Specify one or more input filters to limit query, will be ANDed.</html>");
	    
	    int numVisible = 14;
	    setInputFilters(filters, numFilterGroups, numVisible);
	}

	/**
	Return the data store corresponding to this input filter panel.
	@return the data store corresponding to this input filter panel.
	*/
	public ZabbixDataStore getDataStore ( ) {
	    return this.datastore;
	}
}
