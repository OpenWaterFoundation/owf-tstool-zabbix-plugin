// ApiVersion - class to hold the Zabbix API version

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
 * Zabbix version information.
 * See: https://www.zabbix.com/documentation/current/en/manual/api/reference/apiinfo/version
 */
@JsonPropertyOrder({"version"})
@JsonIgnoreProperties(ignoreUnknown=true)
public class ApiVersion {
	
	/**
	 * API version 'result'.
	 */
	@JsonProperty("result")
	private String version = "";
	
	/**
	 * Default constructor.
	 */
	public ApiVersion() {
	}
	
	/**
	 * Return the version date.
	 * @return the version date
	 */
	public String getVersion () {
		return this.version;
	}
	
}