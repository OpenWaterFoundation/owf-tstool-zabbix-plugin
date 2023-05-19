// UserLogin - class to hold the old (pre 6?) user login result

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
 * Zabbix user login information, used with older Zabbix (pre 6.0?).
 * The API key is assigned after a successful login.
 * Newer authentication (6.0 and later?) relies on an API key assigned in the Zabbix website.
 * See: https://www.zabbix.com/documentation/5.4/en/manual/api
 * Results looks like:
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "result": "0424bd59b807674191e7d77572075f33",
 *   "id": 1
 * }
 * </pre>
 */
@JsonPropertyOrder({"apiToken"})
@JsonIgnoreProperties(ignoreUnknown=true)
public class UserLogin {
	
	/**
	 * API token 'result'.
	 */
	@JsonProperty("result")
	private String apiToken = "";
	
	/**
	 * Default constructor.
	 */
	public UserLogin() {
	}
	
	/**
	 * Return the API token.
	 * @return the API token 
	 */
	public String getApiToken () {
		return this.apiToken;
	}
	
}