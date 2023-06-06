// Host - class to hold a host

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
 * Zabbix host information.
 * See (current): https://www.zabbix.com/documentation/current/en/manual/api/reference/host/get 
 * See (5.4): https://www.zabbix.com/documentation/5.4/en/manual/api/reference/host/get
 */
@JsonPropertyOrder({"hostid,host,name,description,groups"})
@JsonIgnoreProperties(ignoreUnknown=true)
public class Host {
	
	/**
	 * Host 'hostid'.
	 */
	private String hostid = "";
	
	/**
	 * Host 'host'.
	 */
	private String host = "";

	/**
	 * Host 'name'.
	 */
	private String name = "";

	/**
	 * Host 'description'.
	 */
	private String description = "";
	
	/**
	 * List of groups that the host belongs to.
	 */
	private List<HostGroup> groups = null;

	/**
	 * Default constructor.
	 */
	public Host() {
	}
	
	/**
	 * Return the host description.
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
	 * Return the host ID.
	 * @return the host ID 
	 */
	public String getHostid () {
		return this.hostid;
	}

	/**
	 * Return the host groups.
	 * @return the host groups 
	 */
	public List<HostGroup> getGroups () {
		return this.groups;
	}

	/**
	 * Return the host name.
	 * @return the host name
	 */
	public String getName () {
		return this.name;
	}
	
	/**
	 * Lookup a host given the ID.
	 * @param hostList list of Host to search
	 * @param hostid 'hostid' to match
	 * @return the matching Host or null if not found
	 */
	public static Host lookupHostForId ( List<Host> hostList, String hostid ) {
		if ( hostList == null ) {
			return null;
		}
		if ( (hostid == null) || hostid.isEmpty() ) {
			return null;
		}
		for ( Host host: hostList ) {
			if ( host.getHostid().equals(hostid) ) {
				return host;
			}
		}
		// Not found.
		return null;
	}

	/**
	 * Lookup the preferred host group.
	 * @param preferredHostGroupNames list of preferred host group names, can include * wildcard
	 * (for example "Clients/*").
	 * @return the host group that matches a preferred host group name, or null if no host group
	 */
	public HostGroup lookupPreferredHostGroup ( List<String> preferredHostGroupNames ) {
		if ( (this.groups == null) || this.groups.isEmpty() ) {
			// No group.
			return null;
		}
		else if ( this.groups.size() == 1 ) {
			// Only one group.
			return this.groups.get(0);
		}
		else {
			// First try to find a group that matches the preferred name.
			for ( HostGroup hostGroup : this.groups ) {
				String groupName = hostGroup.getName();
				for ( String preferredGroupName : preferredHostGroupNames ) {
					if ( groupName.matches(preferredGroupName) ) {
						// Found a preferred name.
						return hostGroup;
					}
				}
			}
			// If not found above, return the first group.
			// This can be unpredictable.
			return this.groups.get(0);
		}
	}

}