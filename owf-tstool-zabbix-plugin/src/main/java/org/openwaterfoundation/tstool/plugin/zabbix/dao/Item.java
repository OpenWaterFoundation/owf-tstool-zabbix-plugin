// Item - class to hold an item

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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Zabbix host information.
 * See (current): https://www.zabbix.com/documentation/current/en/manual/api/reference/item/get 
 * See (5.4): https://www.zabbix.com/documentation/5.4/en/manual/api/reference/item/get
 */
@JsonPropertyOrder({"itemid,hostid,name,key,delay,history,lastValue,lastClock,lastNs,valueType,units,error,description"})
@JsonIgnoreProperties(ignoreUnknown=true)
public class Item {
	
	// List in the order of the API documentation (might alphabetize later).
	
	/**
	 * Item 'itemid'.
	 */
	private String itemid = "";

	/**
	 * Item 'hostid'.
	 */
	private String hostid = "";
	
	/**
	 * Item 'name'.
	 */
	private String name = "";

	/**
	 * Item 'type'.
	 */
	private String type = "";

	/**
	 * Item 'key'.
	 */
	@JsonProperty("key_")
	private String key = "";

	/**
	 * Item 'delay'.
	 */
	private String delay = "";

	/**
	 * Item 'history'.
	 */
	private String history = "";

	/**
	 * Item 'lastvalue'.
	 */
	@JsonProperty("lastvalue")
	private String lastValue = "";

	/**
	 * Item 'lastclock'.
	 */
	@JsonProperty("lastclock")
	private String lastClock = "";

	/**
	 * Item 'lastns'.
	 */
	@JsonProperty("lastns")
	private String lastNs = "";

	/**
	 * Item 'value_type'.
	 */
	@JsonProperty("value_type")
	private String valueType = "";

	/**
	 * Item 'units'.
	 */
	private String units = "";

	/**
	 * Item 'error'.
	 */
	private String error = "";

	/**
	 * Item 'description'.
	 */
	private String description = "";

	/**
	 * Default constructor.
	 */
	public Item() {
	}
	
	/**
	 * Return the item delay.
	 * @return the item delay
	 */
	public String getDelay () {
		return this.delay;
	}

	/**
	 * Return the item description.
	 * @return the item description
	 */
	public String getDescription () {
		return this.description;
	}

	/**
	 * Return the item error.
	 * @return the item error
	 */
	public String getError () {
		return this.error;
	}

	/**
	 * Return the item history.
	 * @return the item history
	 */
	public String getHistory () {
		return this.history;
	}

	/**
	 * Return the item host ID.
	 * @return the item host ID 
	 */
	public String getHostid () {
		return this.hostid;
	}

	/**
	 * Return the item ID.
	 * @return the item ID 
	 */
	public String getItemid () {
		return this.itemid;
	}

	/**
	 * Return the item key.
	 * @return the item key
	 */
	public String getKey () {
		return this.key;
	}

	/**
	 * Return the item last clock.
	 * @return the item last clock
	 */
	public String getLastClock () {
		return this.lastClock;
	}

	/**
	 * Return the item last ns.
	 * @return the item last ns
	 */
	public String getLastNs () {
		return this.lastNs;
	}

	/**
	 * Return the item last value.
	 * @return the item last value
	 */
	public String getLastValue () {
		return this.lastValue;
	}

	/**
	 * Return the item name.
	 * @return the item name
	 */
	public String getName () {
		return this.name;
	}

	/**
	 * Return the item type.
	 * @return the item type
	 */
	public String getType () {
		return this.type;
	}

	/**
	 * Return the item units.
	 * @return the item units
	 */
	public String getUnits () {
		return this.units;
	}

	/**
	 * Return the item value type.
	 * @return the item value type
	 */
	public String getValueType () {
		return this.valueType;
	}

}