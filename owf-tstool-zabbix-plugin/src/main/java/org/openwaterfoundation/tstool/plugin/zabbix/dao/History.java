// History - class to hold a history record (measurement)

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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Zabbix history information.
 * See (current): https://www.zabbix.com/documentation/current/en/manual/api/reference/history/get 
 * See (5.4): https://www.zabbix.com/documentation/5.4/en/manual/api/reference/history/get
 */
@JsonPropertyOrder({"itemid,clock,ns,value"})
@JsonIgnoreProperties(ignoreUnknown=true)
public class History {
	
	/**
	 * History 'itemid'.
	 */
	private String itemid = "";
	
	/**
	 * History 'clock'.
	 */
	private String clock = "";

	/**
	 * History 'value'.
	 * Even though the Zabbix documentation indicates that the value can have a different type
	 * (float, int, string, text, log), it appears from examples that a string is always returned.
	 * Therefore, let Jackson handle as a string and convert into time series float data value,
	 * or use the flags for text.
	 */
	private String value = "";

	/**
	 * History 'ns'.
	 */
	private String ns = "";

	/**
	 * Default constructor.
	 */
	public History() {
	}
	
	/**
	 * Return the history clock.
	 * @return the history clock
	 */
	public String getClock () {
		return this.clock;
	}

	/**
	 * Return the history itemid.
	 * @return the history itemid 
	 */
	public String getItemid () {
		return this.itemid;
	}

	/**
	 * Return the history ns.
	 * @return the history ns
	 */
	public String getNs () {
		return this.ns;
	}

	/**
	 * Return the history value.
	 * @return the history value
	 */
	public String getValue () {
		return this.value;
	}

}