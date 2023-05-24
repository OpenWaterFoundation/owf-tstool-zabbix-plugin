// Trend - class to hold a trend record (measurement statistic archive)

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

package org.openwaterfoundation.tstool.plugin.zabbix.dao;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Zabbix trend information.
 * See (current): https://www.zabbix.com/documentation/current/en/manual/api/reference/trend/get 
 * See (5.4): https://www.zabbix.com/documentation/5.4/en/manual/api/reference/trend/get
 */
@JsonPropertyOrder({"itemid,clock,num,valueMin,valueAvg,valueMax"})
@JsonIgnoreProperties(ignoreUnknown=true)
public class Trend {
	
	/**
	 * Trend 'itemid'.
	 */
	private String itemid = "";
	
	/**
	 * Trend 'clock' (hour beginning timestamp.
	 */
	private String clock = "";

	/**
	 * Trend 'value_avg'.
	 * Even though the Zabbix documentation indicates that the value can have a different type
	 * (float or int), it appears from examples that a string is always returned.
	 * Therefore, let Jackson handle as a string and convert into time series float data value.
	 */
	@JsonProperty("value_avg")
	private String valueAvg = "";

	/**
	 * Trend 'value_max'.
	 * Even though the Zabbix documentation indicates that the value can have a different type
	 * (float or int), it appears from examples that a string is always returned.
	 * Therefore, let Jackson handle as a string and convert into time series float data value.
	 */
	@JsonProperty("value_max")
	private String valueMax = "";

	/**
	 * Trend 'value_min'.
	 * Even though the Zabbix documentation indicates that the value can have a different type
	 * (float or int), it appears from examples that a string is always returned.
	 * Therefore, let Jackson handle as a string and convert into time series float data value.
	 */
	@JsonProperty("value_min")
	private String valueMin = "";

	/**
	 * Default constructor.
	 */
	public Trend() {
	}
	
	/**
	 * Return the trend clock.
	 * @return the trend clock
	 */
	public String getClock () {
		return this.clock;
	}

	/**
	 * Return the trend itemid.
	 * @return the trend itemid 
	 */
	public String getItemid () {
		return this.itemid;
	}

	/**
	 * Get the clock minimim and maximum from a list of data objects.
	 * @param trendList a list of Trend to process
	 * @return an array containing the minimum and maximum clock values
	 */
    public static long [] getClockLimits ( List<Trend> trendList ) {
    	long [] clockLimits = new long[2];
    	clockLimits[0] = -1;
    	clockLimits[1] = -1;
    	long clock;
    	for ( Trend trend : trendList ) {
    		clock = Long.parseLong(trend.getClock());
    		if ( (clockLimits[0] < 0) || (clock < clockLimits[0]) ) {
    			clockLimits[0] = clock;
    		}
    		if ( (clockLimits[1] < 0) || (clock > clockLimits[1]) ) {
    			clockLimits[1] = clock;
    		}
    	}
    	return clockLimits;
    }

	/**
	 * Return the trend value_avg.
	 * @return the trend value_avg
	 */
	public String getValueAvg () {
		return this.valueAvg;
	}

	/**
	 * Return the trend value_max.
	 * @return the trend value_max
	 */
	public String getValueMax () {
		return this.valueMax;
	}

	/**
	 * Return the trend value_min.
	 * @return the trend value_min
	 */
	public String getValueMin () {
		return this.valueMin;
	}

}