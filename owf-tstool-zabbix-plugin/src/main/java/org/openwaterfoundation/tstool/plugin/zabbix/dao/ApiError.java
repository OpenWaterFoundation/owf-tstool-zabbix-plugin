// ApiError - class to hold a Zabbix API error

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
 * Zabbix error.
 * See: https://www.zabbix.com/documentation/current/en/manual/api
 */
@JsonPropertyOrder({"code, message, detailedMessage"})
@JsonIgnoreProperties(ignoreUnknown=true)
public class ApiError {
	
	/**
	 * Error 'data'.
	 */
	private String detailedMessage = "";
	
	/**
	 * Error 'code'.
	 */
	private Integer code = null;

	/**
	 * Error 'message'.
	 */
	private String message = "";
	
	/**
	 * Default constructor.
	 */
	public ApiError() {
	}
	
	/**
	 * Return the error code.
	 * @return the error code
	 */
	public Integer getCode () {
		return this.code;
	}

	/**
	 * Return the detailed error message.
	 * @return the detailed error message
	 */
	@JsonProperty("data")
	public String getDetailedMessage () {
		return this.detailedMessage;
	}

	/**
	 * Return the error message.
	 * @return the error message
	 */
	public String getMessage () {
		return this.message;
	}
	
}