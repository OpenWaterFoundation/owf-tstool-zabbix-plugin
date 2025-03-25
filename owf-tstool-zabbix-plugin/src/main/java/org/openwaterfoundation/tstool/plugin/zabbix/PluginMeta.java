// PluginMeta - metadata for the plugin

/* NoticeStart

OWF TSTool Zabbix Plugin
Copyright (C) 2023 Open Water Foundation

OWF TSTool AWS Plugin is free software:  you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

OWF TSTool AWS Plugin is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

You should have received a copy of the GNU General Public License
    along with OWF TSTool AWS Plugin.  If not, see <https://www.gnu.org/licenses/>.

NoticeEnd */

package org.openwaterfoundation.tstool.plugin.zabbix;

public class PluginMeta {
	/**
	 * Plugin version.
	 */
	public static final String VERSION = "2.0.0 (2025-03-25)";

	/**
	 * Get the documentation root URL, used for command help.
	 * This should be the folder in which the index.html file exists, for example:
	 *	  https://software.openwaterfoundation.org/tstool-zabbix-plugin/latest/doc-user/
	 * Command documentation can be determined by appending "/command-ref/CommandName/" to this.
	 */
	public static String getDocumentationRootUrl() {
		// Hard code for now until figure out how to configure in the META:
		// - the ZabbixHelpViewerUrlFormatter will try matching the current version and then use 'latest'
		String url = "https://software.openwaterfoundation.org/tstool-zabbix-plugin/latest/doc-user/";
		return url;
	}

	/**
	 * Get the semantic version (e.g., "1.2.3").
	 * @return the semantic version (e.g., "1.2.3").
	 */
	public static String getSemanticVersion() {
		// Parse from VERSION.
		String [] parts = VERSION.split(" ");
		return parts[0].trim();
	}

}