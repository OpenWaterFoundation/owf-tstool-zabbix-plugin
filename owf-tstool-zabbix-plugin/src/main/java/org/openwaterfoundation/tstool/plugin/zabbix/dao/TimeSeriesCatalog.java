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
import java.util.List;

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
	private String dataInterval = ""; // Always IrregSecond?
	private String dataType = ""; // From item.key.
	private String dataUnits = ""; // From item.units.

	// Host group data, listed alphabetically.
	private String hostGroupId = null;
	private String hostGroupName = null;

	// Host data, listed alphabetically.
	//private Double stationLatitude = null;
	//private Double stationLongitude = null;
	private String hostDescription = null;
	private String hostId = null;
	private String hostName = null;

	// Item data, listed alphabetically.
	private String itemDelay = "";
	private String itemHistory = "";
	private String itemId = "";
	private String itemKey = "";
	private String itemName = "";
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
		// List in the same order as internal data member list.
		this.locId = timeSeriesCatalog.locId;
		this.dataInterval = timeSeriesCatalog.dataInterval;
		this.dataType = timeSeriesCatalog.dataType;
		this.dataUnits = timeSeriesCatalog.dataUnits;

		// Host group data, listed alphabetically.
		this.hostGroupId = timeSeriesCatalog.hostGroupId;
		this.hostGroupName = timeSeriesCatalog.hostGroupName;

		// Host data, listed alphabetically.
		//private Double stationLatitude = null;
		//private Double stationLongitude = null;
		this.hostDescription = timeSeriesCatalog.hostDescription;
		this.hostId = timeSeriesCatalog.hostId;
		this.hostName = timeSeriesCatalog.hostName;

		// Item data, listed alphabetically.
		this.itemDelay = timeSeriesCatalog.itemDelay;
		this.itemHistory = timeSeriesCatalog.itemHistory;
		this.itemId = timeSeriesCatalog.itemId;
		this.itemKey = timeSeriesCatalog.itemKey;
		this.itemName = timeSeriesCatalog.itemName;
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
	    return dataTypesDistinct;
	}

	/**
	 * Return whether checkData() has resulted in problems being set.
	 * @return whether checkData() has resulted in problems being set.
	 */
	public boolean getHaveCheckDataProblemsBeenSet () {
		return this.haveCheckDataProblemsBeenSet;
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

	public String getItemUnits ( ) {
		return this.itemUnits;
	}

	public String getItemValueType ( ) {
		return this.itemValueType;
	}

	public String getLocId ( ) {
		return this.locId;
	}

	public void setDataInterval ( String dataInterval ) {
		this.dataInterval = dataInterval;
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
	 * Simple string to identify the time series catalog, for example for logging, using TSID format.
	 */
	public String toString() {
		return "" + this.locId + ".." + this.dataType + "." + this.dataInterval;
	}
}
