// Template - class to hold a template

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
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Zabbix template information.
 * See (current): https://www.zabbix.com/documentation/current/en/manual/api/reference/template/get 
 * See (5.4): https://www.zabbix.com/documentation/5.4/en/manual/api/reference/template/get
 */
@JsonPropertyOrder({"templateid,host,name,description,uuid"})
@JsonIgnoreProperties(ignoreUnknown=true)
public class Template {
	
	/**
	 * Template 'templateid'.
	 */
	private String templateid = "";
	
	/**
	 * Template 'host'.
	 */
	private String host = "";

	/**
	 * Template 'name'.
	 */
	private String name = "";

	/**
	 * Template 'description'.
	 */
	private String description = "";

	/**
	 * Template 'uuid'.
	 */
	private String uuid = "";
	
	/**
	 * Default constructor.
	 */
	public Template() {
	}
	
	/**
	 * Return the template description.
	 * @return the host description
	 */
	public String getDescription () {
		return this.description;
	}

	/**
	 * Return the host.
	 * @return the host 
	 */
	public String getHost () {
		return this.host;
	}

	/**
	 * Return the host name.
	 * @return the host name
	 */
	public String getName () {
		return this.name;
	}
	
	/**
	 * Return the template ID.
	 * @return the template ID 
	 */
	public String getTemplateid () {
		return this.templateid;
	}

	/**
	 * Return the template uuid.
	 * @return the template uuid 
	 */
	public String getUuid () {
		return this.uuid;
	}

	/**
	 * Lookup a template given the ID.
	 * @param templateList list of template to search
	 * @param templateid 'templateid' to match
	 * @return the matching Template or null if not found
	 */
	public static Template lookupTemplateForId ( List<Template> templateList, String templateid ) {
		if ( templateList == null ) {
			return null;
		}
		if ( (templateid == null) || templateid.isEmpty() ) {
			return null;
		}
		for ( Template template: templateList ) {
			if ( template.getTemplateid().equals(templateid) ) {
				return template;
			}
		}
		// Not found.
		return null;
	}
}