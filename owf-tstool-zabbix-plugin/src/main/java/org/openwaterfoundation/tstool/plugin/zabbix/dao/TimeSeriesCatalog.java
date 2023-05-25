// TimeSeriesCatalog - list of time series

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
    along with OWF TSTool KiWIS Plugin.  If not, see <https://www.gnu.org/licenses/>.

NoticeEnd */

package org.openwaterfoundation.tstool.plugin.zabbix.dao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import RTi.Util.Message.Message;

/**
 * Class to store time series catalog (metadata) for Zabbix TSTool time series list.
 * This is a combination of standard time series properties used in TSTool and KiWIS data.
 * More data may be included and shown in the table model while evaluating the web services
 * and will be removed or disabled later.
 * The types are as one would expect, whereas the 'TimeSeries' object uses strings as per web service JSON types.
 */
public class TimeSeriesCatalog {

	// General data, provided by TSTool, extracted/duplicated from Zabbix services.
	private String locId = ""; // From host.host.
	private String dataSource = ""; // From host grup name.
	private String dataType = ""; // From item.name, will have statistic if trend.
	private String dataInterval = ""; // Always IrregSecond?
	private String dataUnits = ""; // From item.units.

	// Host group data, listed alphabetically.
	private String hostGroupId = null;
	private String hostGroupName = null;

	// Host data, listed alphabetically.
	//private Double stationLatitude = null;
	//private Double stationLongitude = null;
	private String host = null;
	private String hostDescription = null;
	private String hostId = null;
	private String hostName = null;

	// Item data, listed alphabetically.
	private String itemDelay = "";
	private String itemHistory = "";
	private String itemId = "";
	private String itemKey = "";
	private String itemName = "";
	private String itemStatus = "";
	private String itemTemplateName = "";
	private String itemTemplateid = "";
	private String itemTrends = "";
	private String itemType = "";
	private String itemUnits = "";
	private String itemValueType = "";

	// List of problems, one string per issue.
	private List<String> problems = null; // Initialize to null to save memory ... must check elsewhere when using.

	/**
	 * Has ReadTSCatalog.checkData() resulted in problems being set?
	 * This is used when there are issues with non-unique time series identifiers.
	 * For example if two catalog are returned for a stationNumId, dataType, and dataInterval,
	 * each of the tscatalog is processed in checkData().  The will each be processed twice.
	 * This data member is set to true the first time so that the 'problems' list is only set once
	 * in TSCatalogDAO.checkData().
	 */
	private boolean haveCheckDataProblemsBeenSet = false;

	/**
	 * Constructor.
	 */
	public TimeSeriesCatalog () {
	}

	/**
	 * Copy constructor.
	 * @param timeSeriesCatalog instance to copy
	 */
	public TimeSeriesCatalog ( TimeSeriesCatalog timeSeriesCatalog ) {
		// Do a deep copy by default as per normal Java conventions.
		this(timeSeriesCatalog, true);
	}

	/**
	 * Copy constructor.
	 * @param timeSeriesCatalog instance to copy
	 * @param deepCopy indicates whether an exact deep copy should be made (true)
	 * or a shallow copy that is typically used when defining a derived catalog record.
	 * For example, use deepCopy=false when copying a scaled catalog entry for a rated time series.
	 */
	public TimeSeriesCatalog ( TimeSeriesCatalog timeSeriesCatalog, boolean deepCopy ) {
		// List in the same order as time series identifier.
		this.locId = timeSeriesCatalog.locId;
		this.dataSource = timeSeriesCatalog.dataSource;
		this.dataType = timeSeriesCatalog.dataType;
		this.dataInterval = timeSeriesCatalog.dataInterval;
		this.dataUnits = timeSeriesCatalog.dataUnits;

		// Host group data, listed alphabetically.
		this.hostGroupId = timeSeriesCatalog.hostGroupId;
		this.hostGroupName = timeSeriesCatalog.hostGroupName;

		// Host data, listed alphabetically.
		//private Double stationLatitude = null;
		//private Double stationLongitude = null;
		this.host= timeSeriesCatalog.host;
		this.hostDescription = timeSeriesCatalog.hostDescription;
		this.hostId = timeSeriesCatalog.hostId;
		this.hostName = timeSeriesCatalog.hostName;

		// Item data, listed alphabetically.
		this.itemDelay = timeSeriesCatalog.itemDelay;
		this.itemHistory = timeSeriesCatalog.itemHistory;
		this.itemId = timeSeriesCatalog.itemId;
		this.itemKey = timeSeriesCatalog.itemKey;
		this.itemName = timeSeriesCatalog.itemName;
		this.itemStatus = timeSeriesCatalog.itemStatus;
		this.itemTemplateid = timeSeriesCatalog.itemTemplateid;
		this.itemTemplateName = timeSeriesCatalog.itemTemplateName;
		this.itemTrends = timeSeriesCatalog.itemTrends;
		this.itemType = timeSeriesCatalog.itemType;
		this.itemUnits = timeSeriesCatalog.itemUnits;
		this.itemValueType = timeSeriesCatalog.itemValueType;

		if ( deepCopy ) {
			// Time series catalog problems.
			if ( timeSeriesCatalog.problems == null ) {
				this.problems = null;
			}
			else {
				// Create a new list.
				this.problems = new ArrayList<>();
				for ( String s : timeSeriesCatalog.problems ) {
					this.problems.add(s);
				}
			}
		}
		else {
			// Default is null problems list.
		}
	}

	/**
	 * Add a problem to the problem list.
	 * @param problem Single problem string.
	 */
	public void addProblem ( String problem ) {
		if ( this.problems == null ) {
			this.problems = new ArrayList<>();
		}
		this.problems.add(problem);
	}

	/**
	 * Clear the problems.
	 * @return
	 */
	public void clearProblems() {
		if ( this.problems != null ) {
			this.problems.clear();
		}
	}

	/**
	 * Format problems into a single string.
	 * @return formatted problems.
	 */
	public String formatProblems() {
		if ( this.problems == null ) {
			return "";
		}
		StringBuilder b = new StringBuilder();
		for ( int i = 0; i < problems.size(); i++ ) {
			if ( i > 0 ) {
				b.append("; ");
			}
			b.append(problems.get(i));
		}
		return b.toString();
	}

	public String getDataInterval ( ) {
		return this.dataInterval;
	}

	public String getDataSource ( ) {
		return this.dataSource;
	}

	public String getDataType ( ) {
		return this.dataType;
	}

	public String getDataUnits ( ) {
		return this.dataUnits;
	}

	/**
	 * Get the list of distinct data intervals from the catalog, for example "IrregSecond", "15Minute".
	 * @param tscatalogList list of TimeSeriesCatalog to process.
	 * The list may have been filtered by data type previous to calling this method.
	 * @return a list of distinct data interval strings.
	 */
	public static List<String> getDistinctDataIntervals ( List<TimeSeriesCatalog> tscatalogList ) {
	    List<String> dataIntervalsDistinct = new ArrayList<>();
	    String dataInterval;
	    boolean found;
	    for ( TimeSeriesCatalog tscatalog : tscatalogList ) {
	    	// Data interval from the catalog, something like "IrregSecond", "15Minute", "1Hour", "24Hour".
	    	dataInterval = tscatalog.getDataInterval();
	    	if ( dataInterval == null ) {
	    		continue;
	    	}
	    	found = false;
	    	for ( String dataInterval2 : dataIntervalsDistinct ) {
	    		if ( dataInterval2.equals(dataInterval) ) {
	    			found = true;
	    			break;
	    		}
	    	}
	    	if ( !found ) {
	    		// Add to the list of unique data types.
	    		dataIntervalsDistinct.add(dataInterval);
	    	}
	    }
	    return dataIntervalsDistinct;
	}

	/**
	 * Get the list of distinct data sources from the catalog.
	 * @param tscatalogList list of TimeSeriesCatalog to process.
	 * @return a list of distinct data type strings, sorted alphabetically ignoring case.
	 */
	public static List<String> getDistinctDataSources ( List<TimeSeriesCatalog> tscatalogList ) {
	    List<String> dataSourcesDistinct = new ArrayList<>();
	    String dataSource;
	    boolean found;
	    for ( TimeSeriesCatalog tscatalog : tscatalogList ) {
	    	// Data source from the catalog, the host group name.
	    	dataSource = tscatalog.getDataSource();
	    	if ( dataSource == null ) {
	    		continue;
	    	}
	    	found = false;
	    	for ( String dataSource2 : dataSourcesDistinct ) {
	    		if ( dataSource2.equals(dataSource) ) {
	    			found = true;
	    			break;
	    		}
	    	}
	    	if ( !found ) {
	    		// Add to the list of unique data types.
	    		dataSourcesDistinct.add(dataSource);
	    	}
	    }
	    Collections.sort(dataSourcesDistinct, String.CASE_INSENSITIVE_ORDER);
	    return dataSourcesDistinct;
	}

	/**
	 * Get the list of distinct data types from the catalog.
	 * @param tscatalogList list of TimeSeriesCatalog to process.
	 * @return a list of distinct data type strings.
	 */
	public static List<String> getDistinctDataTypes ( List<TimeSeriesCatalog> tscatalogList ) {
	    List<String> dataTypesDistinct = new ArrayList<>();
	    String dataType;
	    boolean found;
	    for ( TimeSeriesCatalog tscatalog : tscatalogList ) {
	    	// Data type from the catalog, something like "WaterLevelRiver".
	    	dataType = tscatalog.getDataType();
	    	if ( dataType == null ) {
	    		continue;
	    	}
	    	found = false;
	    	for ( String dataType2 : dataTypesDistinct ) {
	    		if ( dataType2.equals(dataType) ) {
	    			found = true;
	    			break;
	    		}
	    	}
	    	if ( !found ) {
	    		// Add to the list of unique data types.
	    		dataTypesDistinct.add(dataType);
	    	}
	    }
	    Collections.sort(dataTypesDistinct, String.CASE_INSENSITIVE_ORDER);
	    return dataTypesDistinct;
	}

	/**
	 * Get the list of distinct Location IDs from the catalog.
	 * @param tscatalogList list of TimeSeriesCatalog to process.
	 * @param includeNote if true, include a note with the host name
	 * @return a list of distinct location ID strings.
	 */
	public static List<String> getDistinctLocIds ( List<TimeSeriesCatalog> tscatalogList, boolean includeNote ) {
	    List<String> LocIdsDistinct = new ArrayList<>();
	    String locId;
	    boolean found;
	    for ( TimeSeriesCatalog tscatalog : tscatalogList ) {
	    	// Data type from the catalog, something like "WaterLevelRiver".
	    	locId = tscatalog.getLocId();
	    	if ( locId == null ) {
	    		continue;
	    	}
    		if ( includeNote ) {
    			// Add the note at the end.
    			locId = locId + " - " + tscatalog.getHostName();
    		}
	    	found = false;
	    	for ( String locId2 : LocIdsDistinct ) {
	    		if ( locId2.equals(locId) ) {
	    			found = true;
	    			break;
	    		}
	    	}
	    	if ( !found ) {
	    		// Add to the list of unique data types.
	    		LocIdsDistinct.add(locId);
	    	}
	    }
	    Collections.sort(LocIdsDistinct, String.CASE_INSENSITIVE_ORDER);
	    return LocIdsDistinct;
	}

	/**
	 * Return whether checkData() has resulted in problems being set.
	 * @return whether checkData() has resulted in problems being set.
	 */
	public boolean getHaveCheckDataProblemsBeenSet () {
		return this.haveCheckDataProblemsBeenSet;
	}

	public String getHost( ) {
		return this.host;
	}

	public String getHostDescription ( ) {
		return this.hostDescription;
	}

	public String getHostGroupId ( ) {
		return this.hostGroupId;
	}

	public String getHostGroupName ( ) {
		return this.hostGroupName;
	}

	public String getHostId ( ) {
		return this.hostId;
	}

	public String getHostName ( ) {
		return this.hostName;
	}

	public String getItemDelay ( ) {
		return this.itemDelay;
	}

	public String getItemHistory ( ) {
		return this.itemHistory;
	}

	public String getItemId ( ) {
		return this.itemId;
	}

	public String getItemKey ( ) {
		return this.itemKey;
	}

	public String getItemName ( ) {
		return this.itemName;
	}

	public String getItemStatus ( ) {
		return this.itemStatus;
	}

	/**
	 * Return the template 
	 * @return
	 */
	public String getItemTemplateName ( ) {
		return this.itemTemplateName;
	}

	public String getItemTemplateId ( ) {
		return this.itemTemplateid;
	}

	public String getItemTrends ( ) {
		return this.itemTrends;
	}

	public String getItemType ( ) {
		return this.itemType;
	}

	public String getItemUnits ( ) {
		return this.itemUnits;
	}

	public String getItemValueType ( ) {
		return this.itemValueType;
	}

	public String getLocId ( ) {
		return this.locId;
	}

	/**
	 * Lookup time series catalog using parts from the TSTool main UI and input filters.
	 * This is called when reading a time series using a TSID.
	 * @param tscatalogListToSearch list of TimeSeriesCatalog to search, for example the global list.
	 * @param dataType the data type to match, null or "*" to ignore
	 * @param dataInterval the data interval to match, null or "*" to ignore
	 * @param hostgroup.name HostGroup.name to match, null to ignore
	 * @param host Host.host to match, null to ignore
	 * @param hostName Host.name to match, null to ignore
	 * @param itemName Item.name to match, null to ignore
	 */
	public static List<TimeSeriesCatalog> lookupCatalog ( List<TimeSeriesCatalog> tscatalogListToSearch,
		String dataType, String dataInterval,
		String hostGroupName,
		String host,
		String hostName,
		String itemName ) {
		//String routine = TimeSeriesCatalog.class.getSimpleName() + ".lookupCatalog";
		List<TimeSeriesCatalog> tscatalogList = new ArrayList<>();
		boolean doAdd = false;
		boolean doCheckDataType = true;
		boolean doCheckDataInterval = true;
		if ( (dataType != null) && dataType.equals("*") ) {
			// No need to check data type (all will match).
			doCheckDataType = false;
		}
		if ( (dataInterval != null) && dataInterval.equals("*") ) {
			// No need to check data interval (all will match).
			doCheckDataInterval = false;
		}
		if ( tscatalogListToSearch != null ) { 
			for ( TimeSeriesCatalog tscatalog : tscatalogListToSearch ) {
				/*
				Message.printStatus(2, routine, "Comparing to dataType=\"" + tscatalog.getDataType()
					+ "\" dataInterval=\"" + tscatalog.getDataInterval()
					+ "\" hostGroupName=\"" + tscatalog.getHostGroupName()
					+ "\" host=\"" + tscatalog.getHost()
					+ "\" hostName=\"" + tscatalog.getHostName()
					+ "\" itemName=\"" + tscatalog.getItemName() + "\"");
					*/
				// Default is the catalog is matched.
				doAdd = true;
				if ( doCheckDataType && (dataType != null) ) {
					if ( !tscatalog.getDataType().equals(dataType) ) {
						doAdd = false;
					}	
				}
				if ( doCheckDataInterval && (dataInterval != null) ) {
					if ( !tscatalog.getDataInterval().equals(dataInterval) ) {
						doAdd = false;
					}	
				}
				if ( hostGroupName != null ) {
					if ( !tscatalog.getHostGroupName().equals(hostGroupName) ) {
						doAdd = false;
					}	
				}
				if ( host != null ) {
					if ( !tscatalog.getHost().equals(host) ) {
						doAdd = false;
					}	
				}
				if ( hostName != null ) {
					if ( !tscatalog.getHostName().equals(hostName) ) {
						doAdd = false;
					}	
				}
				if ( itemName != null ) {
					if ( !tscatalog.getItemName().equals(itemName) ) {
						doAdd = false;
					}
				}
				if ( doAdd ) {
					tscatalogList.add(tscatalog);
				}
			}
		}
		return tscatalogList;
	}

	public void setDataInterval ( String dataInterval ) {
		this.dataInterval = dataInterval;
	}

	public void setDataSource ( String dataSource ) {
		this.dataSource = dataSource;
	}

	public void setDataType ( String dataType ) {
		this.dataType = dataType;
	}

	public void setDataUnits ( String dataUnits ) {
		this.dataUnits = dataUnits;
	}

	/**
	 * Set whether checkData() has resulted in problems being set.
	 * - TODO smalers 2020-12-15 not sure this is needed with the latest code.
	 *   Take out once tested out.
	 */
	public void setHaveCheckDataProblemsBeenSet ( boolean haveCheckDataProblemsBeenSet ) {
		this.haveCheckDataProblemsBeenSet = haveCheckDataProblemsBeenSet;
	}

	public void setHost ( String host ) {
		this.host = host;
	}

	public void setHostDescription ( String hostDescription ) {
		this.hostDescription = hostDescription;
	}

	public void setHostGroupId ( String hostGroupId ) {
		this.hostGroupId = hostGroupId;
	}

	public void setHostGroupName ( String hostGroupName ) {
		this.hostGroupName = hostGroupName;
	}

	public void setHostId ( String hostId ) {
		this.hostId = hostId;
	}

	public void setHostName ( String hostName ) {
		this.hostName = hostName;
	}

	public void setItemDelay ( String itemDelay ) {
		this.itemDelay = itemDelay;
	}

	public void setItemHistory ( String itemHistory ) {
		this.itemHistory = itemHistory;
	}

	public void setItemId ( String itemId ) {
		this.itemId = itemId;
	}

	public void setItemKey ( String itemKey ) {
		this.itemKey = itemKey;
	}

	public void setItemName ( String itemName ) {
		this.itemName = itemName;
	}

	public void setItemStatus ( String itemStatus ) {
		this.itemStatus = itemStatus;
	}

	public void setItemTemplateId ( String itemTemplateid ) {
		this.itemTemplateid = itemTemplateid;
	}

	public void setItemTrends ( String itemTrends ) {
		this.itemTrends = itemTrends;
	}

	public void setItemType ( String itemType ) {
		this.itemType = itemType;
	}

	public void setItemUnits ( String itemUnits ) {
		this.itemUnits = itemUnits;
	}

	public void setItemValueType ( String itemValueType ) {
		this.itemValueType = itemValueType;
	}

	public void setLocId ( String locId ) {
		this.locId = locId;
	}

	/**
	 * Set the derived data including template name (from template).
	 * @param templateList the list of Template to use as input for derived data.
	 */
	public void setDerivedData ( List<Template> templateList ) {
		Template template = Template.lookupTemplateForId(templateList, getItemTemplateId());
		if ( template != null ) {
			this.itemTemplateName = template.getName();
		}
		else {
			Message.printStatus(2, "", "Could not find template with ID " + getItemTemplateId() );
		}
	}

	/**
	 * Set the derived data including template name (from template).
	 * @param tscatalogList the list of TimeSeriesCatalog to process.
	 * @param templateList the list of Template to use as input for derived data.
	 */
	public static void setDerivedData ( List<TimeSeriesCatalog> tscatalogList, List<Template> templateList ) {
		if ( tscatalogList == null ) {
			return;
		}
		for ( TimeSeriesCatalog tscatalog : tscatalogList ) {
			tscatalog.setDerivedData( templateList);
			Template template = Template.lookupTemplateForId(templateList, tscatalog.getItemTemplateId());
			if ( template != null ) {
				tscatalog.itemTemplateName = template.getName();
			}
		}
	}

	/**
	 * Simple string to identify the time series catalog, for example for logging, using TSID format.
	 */
	public String toString() {
		return "" + this.locId + ".." + this.dataType + "." + this.dataInterval;
	}
}
