// ZabbixDataStore - class that implements the ZabbixDataStore plugin datastore

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

package org.openwaterfoundation.tstool.plugin.zabbix.datastore;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.openwaterfoundation.tstool.plugin.zabbix.PluginMeta;
import org.openwaterfoundation.tstool.plugin.zabbix.dao.ApiAuthType;
import org.openwaterfoundation.tstool.plugin.zabbix.dao.ApiVersion;
import org.openwaterfoundation.tstool.plugin.zabbix.dao.History;
import org.openwaterfoundation.tstool.plugin.zabbix.dao.Host;
import org.openwaterfoundation.tstool.plugin.zabbix.dao.HostGroup;
import org.openwaterfoundation.tstool.plugin.zabbix.dao.Item;
import org.openwaterfoundation.tstool.plugin.zabbix.dao.ItemType;
import org.openwaterfoundation.tstool.plugin.zabbix.dao.Template;
import org.openwaterfoundation.tstool.plugin.zabbix.dao.TimeSeriesCatalog;
import org.openwaterfoundation.tstool.plugin.zabbix.dao.Trend;
import org.openwaterfoundation.tstool.plugin.zabbix.dao.UserLogin;
import org.openwaterfoundation.tstool.plugin.zabbix.dao.ValueType;
import org.openwaterfoundation.tstool.plugin.zabbix.dto.JacksonToolkit;
import org.openwaterfoundation.tstool.plugin.zabbix.ui.Zabbix_TimeSeries_CellRenderer;
import org.openwaterfoundation.tstool.plugin.zabbix.ui.Zabbix_TimeSeries_InputFilter_JPanel;
import org.openwaterfoundation.tstool.plugin.zabbix.ui.Zabbix_TimeSeries_TableModel;

import com.fasterxml.jackson.core.type.TypeReference;

import RTi.TS.TS;
import RTi.TS.TSIdent;
import RTi.TS.TSUtil;
import RTi.Util.GUI.InputFilter;
import RTi.Util.GUI.InputFilter_JPanel;
import RTi.Util.GUI.JWorksheet_AbstractExcelCellRenderer;
import RTi.Util.GUI.JWorksheet_AbstractRowTableModel;
import RTi.Util.IO.PropList;
import RTi.Util.IO.RequirementCheck;
import RTi.Util.Message.Message;
import RTi.Util.String.EmbeddedPropertiesString;
import RTi.Util.String.StringUtil;
import RTi.Util.Time.DateTime;
import RTi.Util.Time.InvalidTimeIntervalException;
import RTi.Util.Time.TimeUtil;
import riverside.datastore.AbstractWebServiceDataStore;
import riverside.datastore.DataStoreRequirementChecker;
import riverside.datastore.PluginDataStore;

public class ZabbixDataStore extends AbstractWebServiceDataStore implements DataStoreRequirementChecker, PluginDataStore {

	/**
	 * Resource path to find supporting files.
	 */
	public final String RESOURCE_PATH = "/org/openwaterfoundation/tstool/plugin/zabbix/resources";

	/**
	 * API token required by the API, set in the datastore configuration 'ApiToken' property.
	 */
	private String apiToken = "";

	/**
	 * Properties for the plugin, used to help with application integration.
	 */
	private Map<String,Object> pluginProperties = new LinkedHashMap<>();

	/**
	 * Global history time series catalog, used to streamline creating lists for UI choices.
	 */
	private List<TimeSeriesCatalog> globalHistoryTscatalogList = new ArrayList<>();

	/**
	 * Global trend time series catalog, used to streamline creating lists for UI choices.
	 */
	private List<TimeSeriesCatalog> globalTrendTscatalogList = new ArrayList<>();

	/**
	 * Global host group list.
	 */
	private List<HostGroup> globalHostGroupList = new ArrayList<>();

	/**
	 * Global host list.
	 */
	private List<Host> globalHostList = new ArrayList<>();

	/**
	 * Global item list.
	 * CURRENTLY NOT POPULATED.
	 */
	private List<Item> globalItemList = new ArrayList<>();

	/**
	 * Global item list.
	 * CURRENTLY NOT POPULATED.
	 */
	private List<String> globalItemNameList = new ArrayList<>();

	/**
	 * Global template list.
	 */
	private List<Template> globalTemplateList = new ArrayList<>();

	/**
	 * Global debug option for datastore, used for development and troubleshooting.
	 */
	private boolean debug = false;

	/**
	 * Version of the API, in case logic needs to adapt to the version.
	 */
	private String apiVersion = "unknown";

	/**
	 * The API major version, currently used for the authentication check.
	 */
	private int apiMajorVersion = -1;
	
	/**
	 * Preferred host group name patterns, to use for the host when there is more than one group.
	 * The strings can contain * wildcards.
	 */
	private List<String> preferredHostGroupNames = new ArrayList<>();

	/**
	 * Whether to use API token for login (as of version 6.0?, true), or older "auth" login (false).
	 */
	private ApiAuthType apiAuthType = ApiAuthType.UNKNOWN;

	/**
	Constructor for web service.
	@param name identifier for the data store
	@param description name for the data store
	@param serviceRootURI the root of the URL for the API,
	for example 'https://zabbix-web.trilynx-novastar.systems/zabbix/api_jsonrpc.php'.
	@param props properties from the datastore configuration file, which must contain the 'ApiToken' property.
	*/
	public ZabbixDataStore ( String name, String description, URI serviceRootURI, PropList props ) {
		String routine = getClass().getSimpleName() + ".ZabbixDataStore";

		String prop = props.getValue("Debug");
		if ( (prop != null) && prop.equalsIgnoreCase("true") ) {
			Message.printStatus(2, routine, "Datastore \"" + name + "\" - detected Debug=true");
			this.debug = true;
		}
	    setName ( name );
	    setDescription ( description );
	    setServiceRootURI ( serviceRootURI );
	    setProperties ( props );

	    // The API token is used for all requests so set as datastore data.
	    this.apiToken = props.getValue("ApiToken");
	    if ( this.apiToken == null ) {
	    	// Don't warn here.  Use the messages from the service.
	    	this.apiToken = "";
	    }

	    // Set standard plugin properties:
        // - plugin properties can be listed in the main TSTool interface
        // - version is used to create a versioned installer and documentation.
        this.pluginProperties.put("Name", "Open Water Foundation Zabbix data web services plugin");
        this.pluginProperties.put("Description", "Plugin to integrate TSTool with Zabbix web resources.");
        this.pluginProperties.put("Author", "Open Water Foundation, https://openwaterfoundation.org");
        this.pluginProperties.put("Version", PluginMeta.VERSION);
        
        // For now hard-code preferred host group name to use in TSID:
        // - add as a configuration property once discuss options
	    String preferredHostGroupName = props.getValue("PreferredHostGroupName");
       	this.preferredHostGroupNames = new ArrayList<>();
        if ( (preferredHostGroupName != null) && !preferredHostGroupName.isEmpty() ) {
        	// Split the string.
        	String [] parts = preferredHostGroupName.split(",");
        	String preferredName = null;
        	for ( String part : parts ) {
        		// Convert to Java regular expression.
        		preferredName = part.replace("*", ".*").trim();
        		this.preferredHostGroupNames.add(preferredName);
        		Message.printStatus(2, routine, "Using preferred host host group name: '" + preferredName + "'");
        	}
        }

        // Get the version:
        // - if major version is 6, authenticate with the ApiToken
        // - else, authenticate with older user and password and request "auth" once logged in
        this.apiVersion = readVersion();
        Message.printStatus(2, routine, "Zabbix API version = " + this.apiVersion);
        int pos = this.apiVersion.indexOf(".");
        this.apiMajorVersion = -1;
        if ( pos < 0 ) {
        	// Unable to determine version, which will be a problem.
        	Message.printWarning(3, routine, "Unable to determine version for Zabbix API.  Errors will result.");
        }
        else {
        	// Convert the major version to an integer.
        	this.apiMajorVersion = Integer.parseInt(this.apiVersion.substring(0,pos));
        	this.apiAuthType = ApiAuthType.AUTH_JSON;
        }
        if ( apiMajorVersion >= 6 ) {
        	// Newer authentication:
        	// - no need to authenticate because the API token is read from the configuration and
        	this.apiAuthType = ApiAuthType.AUTH_HTTP_API_TOKEN;
        }
        else {
        	// Authenticate the API:
        	// - this is used with older API
	    	String login = props.getValue("SystemLogin");
	    	String password = props.getValue("SystemPassword");
        	authenticate ( login, password );
        }

	    // Read global data used throughout the session:
	    // - in particular a cache of the TimeSeriesCatalog used for further queries

	    readGlobalData();
	}

	/**
	 * Authenticate the session.
	 * This handles the authentication type, which depends on the Zabbix API version.
	 * See (old, 5.4): https://www.zabbix.com/documentation/5.4/en/manual/api
	 * See (current): https://www.zabbix.com/documentation/current/en/manual/api
	 * After authentication, this.apiToken is set for use in API requests
	 * @param login Zabbix login
	 * @param password Zabbix password
	 */
	private void authenticate ( String login, String password ) {
		UserLogin userLogin = readUserLogin ( login, password );
		this.apiToken = userLogin.getApiToken();
	}

	/**
	* THIS IS PLACEHOLDER CODE - NEED TO IMPLEMENT.
	*
 	* Check the database requirement for DataStoreRequirementChecker interface, for example one of:
 	* <pre>
 	* @require datastore kiwis-northern version >= 1.5.5
 	* @require datastore kiwis-northern ?configproperty propname? == Something
 	* @require datastore kiwis-northern configuration system_id == CO-District-MHFD
 	*
 	* @enabledif datastore nsdataws-mhfd version >= 1.5.5
 	* </pre>
 	* @param check a RequirementCheck object that has been initialized with the check text and
 	* will be updated in this method.
 	* @return whether the requirement condition is met, from call to check.isRequirementMet()
 	*/
	public boolean checkRequirement ( RequirementCheck check ) {
		String routine = getClass().getSimpleName() + ".checkRequirement";
		// Parse the string into parts:
		// - calling code has already interpreted the first 3 parts to be able to do this call
		String requirement = check.getRequirementText();
		Message.printStatus(2, routine, "Checking requirement: " + requirement);
		// Get the annotation that is being checked, so messages are appropriate.
		String annotation = check.getAnnotation();
		String [] requireParts = requirement.split(" ");
		// Datastore name may be an original name but a substitute is used, via TSTool command line.
		String dsName = requireParts[2];
		String dsNameNote = ""; // Note to add on messages to help confirm how substitutions are being handled.
		String checkerName = "ZabbixDataStore";
		if ( !dsName.equals(this.getName())) {
			// A substitute datastore name is being used, such as in testing.
			dsNameNote = "\nCommand file datastore name '" + dsName + "' substitute that is actually used is '" + this.getName() + "'";
		}
		if ( requireParts.length < 4 ) {
			check.setIsRequirementMet(checkerName, false, "Requirement does not contain check type as one of: version, configuration, "
				+ "for example: " + annotation + " datastore nsdataws-mhfd version...");
			return check.isRequirementMet();
		}
		String checkType = requireParts[3];
		if ( checkType.equalsIgnoreCase("configuration") ) {
			// Checking requirement of form:
			// 0        1         2             3             4         5  6
			// @require datastore nws-afos configuration system_id == CO-District-MHFD
			String propertyName = requireParts[4];
			String operator = requireParts[5];
			String checkValue = requireParts[6];
			// Get the configuration table property of interest:
			// - currently only support checking system_id
			if ( propertyName.equals("system_id") ) {
				// Know how to handle "system_id" property.
				if ( (checkValue == null) || checkValue.isEmpty() ) {
					// Unable to do check.
					check.setIsRequirementMet ( checkerName, false, "'system_id' value to check is not specified in the requirement." + dsNameNote );
					return check.isRequirementMet();
				}
				else {
					// TODO smalers 2023-01-03 need to evaluate whether Zabbix has configuration properties.
					//String propertyValue = readConfigurationProperty(propertyName);
					String propertyValue = "";
					if ( (propertyValue == null) || propertyValue.isEmpty() ) {
						// Unable to do check.
						check.setIsRequirementMet ( checkerName, false, "Zabbix configuration 'system_id' value is not defined in the database." + dsNameNote );
						return check.isRequirementMet();
					}
					else {
						if ( StringUtil.compareUsingOperator(propertyValue, operator, checkValue) ) {
							check.setIsRequirementMet ( checkerName, true, "Zabbix configuration property '" + propertyName + "' value (" + propertyValue +
								") does meet the requirement: " + operator + " " + checkValue + dsNameNote );
						}
						else {
							check.setIsRequirementMet ( checkerName, false, "Zabbix configuration property '" + propertyName + "' value (" + propertyValue +
								") does not meet the requirement:" + operator + " " + checkValue + dsNameNote );
						}
						return check.isRequirementMet();
					}
				}
			}
			else {
				// Other properties may not be easy to compare.  Probably need to use "contains" and other operators.
				check.setIsRequirementMet ( checkerName, false, "Check type '" + checkType + "' configuration property '" + propertyName + "' is not supported.");
				return check.isRequirementMet();
			}
		}
		/* TODO smalers 2021-07-29 need to implement, maybe need to define the system ID in the configuration file as a cross check for testing.
		else if ( checkType.equalsIgnoreCase("configproperty") ) {
			if ( parts.length < 7 ) {
				// 'property' requires 7 parts
				throw new RuntimeException( "'configproperty' requirement does not contain at least 7 parts for: " + requirement);
			}
		}
		*/
		else if ( checkType.equalsIgnoreCase("version") ) {
			// Checking requirement of form:
			// 0        1         2             3       4  5
			// @require datastore nws-afos version >= 1.5.5
			Message.printStatus(2, routine, "Checking web service version.");
			// Do a web service round trip to check version since it may change with software updates.
			String wsVersion = readVersion();
			if ( (wsVersion == null) || wsVersion.isEmpty() ) {
				// Unable to do check.
				check.setIsRequirementMet ( checkerName, false, "Web service version is unknown (services are down or software problem).");
				return check.isRequirementMet();
			}
			else {
				// Web service versions are strings of format A.B.C.D so can do semantic version comparison:
				// - only compare the first 3 parts
				//Message.printStatus(2, "checkRequirement", "Comparing " + wsVersion + " " + operator + " " + checkValue);
				String operator = requireParts[4];
				String checkValue = requireParts[5];
				boolean verCheck = StringUtil.compareSemanticVersions(wsVersion, operator, checkValue, 3);
				String message = "";
				if ( !verCheck ) {
					message = annotation + " web service version (" + wsVersion + ") does not meet requirement: " + operator + " " + checkValue+dsNameNote;
					check.setIsRequirementMet ( checkerName, verCheck, message );
				}
				else {
					message = annotation + " web service version (" + wsVersion + ") does meet requirement: " + operator + " " + checkValue+dsNameNote;
					check.setIsRequirementMet ( checkerName, verCheck, message );
				}
				return check.isRequirementMet();
			}
		}
		else {
			// Unknown check type.
			check.setIsRequirementMet ( checkerName, false, "Requirement check type '" + checkType + "' is unknown.");
			return check.isRequirementMet();
		}

	}

	/**
	 * Create a time series input filter, used to initialize user interfaces.
	 */
	public InputFilter_JPanel createTimeSeriesListInputFilterPanel () {
		Zabbix_TimeSeries_InputFilter_JPanel ifp = new Zabbix_TimeSeries_InputFilter_JPanel(this, 4);
		return ifp;
	}

	/**
	 * Create a time series list table model given the desired data type, time step (interval), and input filter.
	 * The datastore performs a suitable query and creates objects to manage in the time series list.
	 * @param dataType time series data type to query, controlled by the datastore
	 * @param timeStep time interval to query, controlled by the datastore
	 * @param ifp input filter panel that provides additional filter options
	 * @return a TableModel containing the defined columns and rows.
	 */
	@SuppressWarnings("rawtypes")
	public JWorksheet_AbstractRowTableModel createTimeSeriesListTableModel(String dataType, String timeStep, InputFilter_JPanel ifp ) {
		// First query the database for the specified input.
		List<TimeSeriesCatalog> tsmetaList = readTimeSeriesMeta ( dataType, timeStep, ifp );
		return getTimeSeriesListTableModel(tsmetaList);
	}
	
	/**
	 * Escape (protect) a TSID part to ensure that it works well with TSID.
	 * If the part contains a space or period, the returned value will be surrounded by single quotes.
	 * @param tsidPart time series identifier part
	 * @return the part surrounded by single quotes if necessary, otherwise the original value
	 */
	public String escapeTsidPart ( String tsidPart ) {
		if ( (tsidPart.indexOf(" ") > 0) || (tsidPart.indexOf(".") > 0) ) {
			if ( !tsidPart.startsWith("'") ) {
				// Escape the part by surrounding with single quotes.
				return "'" + tsidPart + "'";
			}
			else {
				// Already escaped.
				return tsidPart;
			}
		}
		else {
			// Return the original part.
			return tsidPart;
		}
	}

	/**
	 * Get the list of location identifier (station_no) strings used in the UI.
	 * The list is determined from the cached list of time series catalog.
	 * @param dataType to match, or * or null to return all, should be a value of stationparameter_no
	 * @return a unique sorted list of the location identifiers (station_no)
	 */
	public List<String> getStationIdStrings ( String dataType ) {
		List<String> locIdList = new ArrayList<>();
		/*
		if ( (dataType == null) || dataType.isEmpty() || dataType.equals("*") ) {
			// Return the cached list of all locations.
			return this.locIdList;
		}
		else {
			// Get the list of locations from the cached list of time series catalog
			List<String> locIdList = new ArrayList<>();
			String stationNo = null;
			String stationParameterNo = null;
			boolean found = false;
			for ( TimeSeriesCatalog tscatalog : this.tscatalogList ) {
				stationNo = tscatalog.getStationNo();
				stationParameterNo = tscatalog.getStationParameterNo();

				if ( !stationParameterNo.equals(dataType) ) {
					// Requested data type does not match.
					continue;
				}

				found = false;
				for ( String locId2 : locIdList ) {
					if ( locId2.equals(stationNo) ) {
						found = true;
						break;
					}
				}
				if ( !found ) {
					locIdList.add(stationNo);
				}
			}
			Collections.sort(locIdList, String.CASE_INSENSITIVE_ORDER);
			return locIdList;
		}
		*/
		return locIdList;
	}

	/**
	 * Return the API token.
	 */
	public String getApiToken() {
		return this.apiToken;
	}

	/**
	 * Return the API token query parameter (token=API_TOKEN).
	 */
	public String getApiTokenParameter() {
		return "token=" + this.apiToken;
	}

	/**
	 * Get the 'auth' JSON for requests, formatted as:
	 * <pre>
	 *   , "auth": "apiToken"
	 * </pre>
	 * If the API version is newer, an empty string is returned because the API token is specified
	 * in the HTTP request headers.
	 * The result from this method should be added at the end of the request data, before the closing }.
	 * @return the 'auth' JSON or a blank string
	 */
	private String getAuthJSON () {
		if ( this.apiAuthType == ApiAuthType.AUTH_HTTP_API_TOKEN ) {
			// The API token is specified with HTTP headers so return an empty string.
			return "";
		}
		else {
			// Return the 'auth' JSON.
			return ", \"auth\": \"" + getApiToken() + "\"";
		}
	}

	/**
	 * Return the list of hosts.
	 * @param readData if false, return the global cached data, if true read the data and reset in the cache
	 */
	public List<Host> getHosts(boolean readData) throws IOException {
		if ( readData ) {
			this.globalHostList = readHostList();
		}
		return this.globalHostList;
	}

	/**
	 * Return the list of host groups.
	 * @param readData if false, return the global cached data, if true read the data and reset in the cache
	 */
	public List<HostGroup> getHostGroups(boolean readData) throws IOException {
		if ( readData ) {
			this.globalHostGroupList = readHostGroupList();
		}
		return this.globalHostGroupList;
	}

	/**
	 * Get the requested where from the input filter.
	 * @param ifp InputFilter_JPanel from the UI.
	 * @param whereName where to find for example "hostgroup.name"
	 * @return the selected where, or null if not matched
	 */
	private String getInputFilterWhere ( InputFilter_JPanel ifp, String whereName ) {
		//String routine = getClass().getSimpleName() + ".getHostGroupFromInputFilter";
		if ( ifp != null ) {
        	int nfg = ifp.getNumFilterGroups ();
        	InputFilter filter;
        	for ( int ifg = 0; ifg < nfg; ifg++ ) {
            	filter = ifp.getInputFilter ( ifg );
            	//Message.printStatus(2, routine, "IFP whereLabel =\"" + whereLabel + "\"");
            	boolean special = false; // TODO smalers 2022-12-26 might add special filters.
            	if ( special ) {
            	}
            	else {
            		// Add the query parameter to the URL.
			    	filter = ifp.getInputFilter(ifg);
			    	if ( filter.getWhereInternal2().equals(whereName) ) {
			    		return filter.getInput(true);
			    	}
            	}
        	}
        }
		return null;
	}

	/**
	 * Return the list of item names determined from the TimeSeriesCatalog at initialization.
	 */
	public List<String> getItemNames() {
		return this.globalItemNameList;
	}
	
	/**
	 * Get the location identifier strings.
	 * This is used by the ReadZabbix command editor.
	 * The locIds are looked up from the global history and trend catalog.
	 * @param dataSource data source of interest (host group name)
	 * @param dataType data type of interest (item name)
	 * @param dataInterval data interval of interest (IrregSecond or Hour)
	 * @param includeWildcards whether to include "*" wildcards
	 * @param includeNote if true add host name at after " - " at the end
	 * @return a unique list of locId from the time series catalog, sorted and always not null (but may be an empty list)
	 */
	public List<String> getLocIdStrings ( String dataSource, String dataType, String dataInterval,
		boolean includeWildcards, boolean includeNote ) {
		List<String> locIdList = new ArrayList<>();
		List<TimeSeriesCatalog> tscatalogList = null;
		String locId = null; // null, since it is what is being looked up.
		String hostName = null; // Not used in this lookup.
		String itemName = null; // itemName is not used since data type (possibly with trend statistic) controls.
		if ( isTrendDataType(dataType) ) {
			tscatalogList = TimeSeriesCatalog.lookupCatalog (
				this.globalTrendTscatalogList, dataType, dataInterval, dataSource, locId, hostName, itemName );
		}
		else {
			tscatalogList = TimeSeriesCatalog.lookupCatalog (
				this.globalHistoryTscatalogList, dataType, dataInterval, dataSource, locId, hostName, itemName );
		}
		locIdList = TimeSeriesCatalog.getDistinctLocIds(tscatalogList, includeNote );
		return locIdList;
	}

	/**
	 * Get the "params" filter JSON for a list of items.
	 *  "filter": {
     *      "host": [
     *          "Zabbix server",
     *          "Linux server"
     *      ]
     *  }
     * @param addLeadingComma if true and the filter is non-empty, add a leading comma
     * @param propertyName property name to match for filter (e.g., 'host', 'name')
     * @param propertyValueList list of property values to match, or null (or empty) to read all
	 */
	private String getParamFilter ( boolean addLeadingComma, String propertyName, List<String> propertyValueList ) {
		if ( (propertyValueList != null) && (propertyValueList.size() > 0) ) {
			StringBuilder b = new StringBuilder ( "\"filter\": { \"" + propertyName + "\": [");
			int i = -1;
			for ( String propertyValue : propertyValueList ) {
				++i;
				if ( i > 0 ) {
					b.append(", ");
				}
				b.append ( "\"" + propertyValue + "\"" );
			}
			b.append("] }");
			if ( addLeadingComma ) {
				return ", " + b.toString();
			}
			else {
				return b.toString();
			}
		}
		else {
			return "";
		}
	}

	/**
	 * Get the "params" JSON for a list of Host.
     * @param addLeadingComma if true and the filter is non-empty, add a leading comma
	 * @param hostList list of Host to include
	 */
	private String getParamHostIds ( boolean addLeadingComma, List<Host> hostList ) {
		if ( (hostList != null) && (hostList.size() > 0) ) {
			StringBuilder b = new StringBuilder ();
			if ( addLeadingComma ) {
				b.append(", ");
			}
			b.append("\"hostids\": [");
			int i = -1;
			for ( Host host : hostList ) {
				++i;
				if ( i > 0 ) {
					b.append(", ");
				}
				b.append ( "\"" + host.getHostid() + "\"" );
			}
			b.append("]");
			return b.toString();
		}
		else {
			return "";
		}
	}

	/**
	 * Get the "params" JSON for a list of itemid.
     * @param addLeadingComma if true and the filter is non-empty, add a leading comma
	 * @param itemidList list of 'itemid' to include
	 */
	private String getParamItemIds ( boolean addLeadingComma, List<String> itemidList ) {
		if ( (itemidList != null) && (itemidList.size() > 0) ) {
			StringBuilder b = new StringBuilder ();
			if ( addLeadingComma ) {
				b.append( ", ");
			}
			b.append("\"itemids\": [");
			int i = -1;
			for ( String itemid : itemidList ) {
				++i;
				if ( i > 0 ) {
					b.append(", ");
				}
				b.append ( "\"" + itemid + "\"" );
			}
			b.append("]");
			return b.toString();
		}
		else {
			return "";
		}
	}

	/**
	 * Get the "params" JSON for a 'timefrom'.
     * @param addLeadingComma if true and the filter is non-empty, add a leading comma
	 * @param timefrom UNIX seconds for 'tomefrom' or -1 to not use
	 */
	private String getParamTimeFrom ( boolean addLeadingComma, long timefrom ) {
		if ( timefrom > 0 ) {
			StringBuilder b = new StringBuilder ();
			if ( addLeadingComma ) {
				b.append( ", ");
			}
			b.append("\"time_from\": " + timefrom);
			return b.toString();
		}
		else {
			return "";
		}
	}

	/**
	 * Get the "params" JSON for a 'timetill'.
     * @param addLeadingComma if true and the filter is non-empty, add a leading comma
	 * @param timetill UNIX seconds for 'tometill' or -1 to not use
	 */
	private String getParamTimeTill ( boolean addLeadingComma, long timetill ) {
		if ( timetill > 0 ) {
			StringBuilder b = new StringBuilder ();
			if ( addLeadingComma ) {
				b.append( ", ");
			}
			b.append("\"time_till\": " + timetill);
			return b.toString();
		}
		else {
			return "";
		}
	}

	/**
 	* Get the properties for the plugin.
 	* A copy of the properties map is returned so that calling code cannot change the properties for the plugin.
 	* @return plugin properties map.
 	*/
	public Map<String,Object> getPluginProperties () {
		Map<String,Object> pluginProperties = new LinkedHashMap<>();
		// For now the properties are all strings so it is easy to copy.
    	for (Map.Entry<String, Object> entry : this.pluginProperties.entrySet()) {
        	pluginProperties.put(entry.getKey(),
                    	entry.getValue());
    	}
		return pluginProperties;
	}

	/**
	 * Return the list of history time series catalog.
	 * @param readData if false, return the global cached data, if true read the data and reset in he cache
	 */
	public List<TimeSeriesCatalog> getHistoryTimeSeriesCatalog(boolean readData) {
		if ( readData ) {
			String tsid = null;
			String dataTypeReq = null;
			String dataIntervalReq = null;
    		InputFilter_JPanel ifp = null;
			this.globalHistoryTscatalogList = readTimeSeriesCatalog(tsid, dataTypeReq, dataIntervalReq, ifp );
		}
		return this.globalHistoryTscatalogList;
	}

	/**
	 * This version is required by TSTool UI.
	 * Return the list of time series data interval strings.
	 * Interval strings match TSTool conventions such as NewTimeSeries command, which uses "1Hour" rather than "1hour".
	 * This should result from calls like:  TimeInterval.getName(TimeInterval.HOUR, 0)
	 * @param dataType data type string to filter the list of data intervals.
	 * If null, blank, or "*" the data type is not considered when determining the list of data intervals.
	 */
	public List<String> getTimeSeriesDataIntervalStrings(String dataType) {
		boolean includeWildcards = true;
		return getTimeSeriesDataIntervalStrings(dataType, includeWildcards);
	}

	/**
	 * This version is required by TSTool UI.
	 * Return the list of time series data interval strings.
	 * Interval strings match TSTool conventions such as NewTimeSeries command.
	 * This should result from calls like:  TimeInterval.getName(TimeInterval.HOUR, 0)
	 * @param dataType data type string to filter the list of data intervals.
	 * If null, blank, or "*" the data type is not considered when determining the list of data intervals.
	 * @param includeWildcards if true, include "*" wildcard.
	 */
	public List<String> getTimeSeriesDataIntervalStrings(String dataType, boolean includeWildcards ) {
		//String routine = getClass().getSimpleName() + ".getTimeSeriesDataIntervalStrings";
		List<String> dataIntervals = new ArrayList<>();

		// Use IrregSecond for history, event, and problem time series and Hour for trend time series.
		if ( (dataType == null) || ((dataType != null) && dataType.equals("*")) ) {
			// Add all intervals.
			dataIntervals.add("IrregSecond");
			dataIntervals.add("Hour");

			if ( includeWildcards ) {
				// Always allow querying list of time series for all intervals:
				// - always add so that people can get a full list

				dataIntervals.add("*");
				if ( dataIntervals.size() > 1 ) {
					// Also add at the beginning to simplify selections:
					// - could check for a small number like 5 but there should always be a few
					dataIntervals.add(0,"*");
				}
			}
		}
		else if ( (dataType != null) && isTrendDataType(dataType) ) {
			// Trend time series.
			dataIntervals.add("Hour");
		}
		else {
			dataIntervals.add("IrregSecond");
		}
		return dataIntervals;
	}

	/**
	 * Get the data source strings.
	 * This is used by the ReadZabbix command editor.
	 * The data sources are looked up from the global history and trend catalog.
	 * @param dataType data type of interest (item name)
	 * @param dataInterval data interval of interest (IrregSecond or Hour)
	 * @param includeWildcards whether to include "*" wildcards
	 * @return a unique list of locId from the time series catalog, sorted and always not null (but may be an empty list)
	 */
	public List<String> getTimeSeriesDataSourceStrings ( String dataType, String dataInterval, boolean includeWildcards ) {
		List<String> dataSourceList = new ArrayList<>();
		List<TimeSeriesCatalog> tscatalogList = null;
		String dataSource = null; // null, since this is being looked up.
		String locId = null; // null, since don't have that yet (have to pick data source first).
		String hostName = null; // Not used in this lookup.
		String itemName = null; // itemName is not used since data type (possibly with trend statistic) controls.
		if ( isTrendDataType(dataType) ) {
			tscatalogList = TimeSeriesCatalog.lookupCatalog (
				this.globalTrendTscatalogList, dataType, dataInterval, dataSource, locId, hostName, itemName );
		}
		else {
			tscatalogList = TimeSeriesCatalog.lookupCatalog (
				this.globalHistoryTscatalogList, dataType, dataInterval, dataSource, locId, hostName, itemName );
		}
		dataSourceList = TimeSeriesCatalog.getDistinctDataSources(tscatalogList);
		return dataSourceList;
	}

	/**
	 * Return the list of time series data type strings.
	 * This is the version that is required by TSTool UI.
	 * These strings are the same as the dataTypes.name properties from the stationSummaries web service request.
	 * @param dataInterval data interval from TimeInterval.getName(TimeInterval.HOUR,0) to filter the list of data types.
	 * If null, blank, or "*" the interval is not considered when determining the list of data types (treat as if "*").
	 */
	public List<String> getTimeSeriesDataTypeStrings(String dataInterval) {
		boolean includeWildcards = true;
		return getTimeSeriesDataTypeStrings(dataInterval, includeWildcards );
	}

	/**
	 * Return the list of time series data type strings.
	 * These are the same as Item names.
	 * @param dataInterval the data interval to filter data types
	 * @param includeWildcards whether "*" should be included at the start and end of the list
	 */
	public List<String> getTimeSeriesDataTypeStrings(String dataInterval, boolean includeWildcards ) {
		//String routine = getClass().getSimpleName() + ".getTimeSeriesDataTypeStrings";

		// Currently the data types are a static list, not determined from an API call.
		List<String> dataTypes = new ArrayList<>();

		boolean addTrendTypes = true;
		for ( String itemName : this.globalItemNameList ) {
			// Item names are for history time series.
			dataTypes.add( itemName );
			if ( addTrendTypes ) {
				// Add data types for trend time series:
				// - use the Zabbix statistics as is to avoid confusion
				dataTypes.add( itemName + "-Avg");
				dataTypes.add( itemName + "-Max");
				dataTypes.add( itemName + "-Min");
			}
		}

		// Sort the names.
		Collections.sort(dataTypes, String.CASE_INSENSITIVE_ORDER);

		if ( includeWildcards ) {
			// Add wildcard at the front and end - allows querying all data types for the location:
			// - always add so that people can get a full list
			// - adding at the top makes it easy to explore data without having to scroll to the end

			dataTypes.add("*");
			if ( dataTypes.size() > 1 ) {
				// Also add at the beginning to simplify selections:
				// - could check for a small number like 5 but there should always be a few
				dataTypes.add(0,"*");
			}
		}

		return dataTypes;
	}

	/**
 	* Return the identifier for a time series in the table model.
 	* The TSIdent parts will be uses as TSID commands.
 	* @param tableModel the table model from which to extract data
 	* @param row the displayed table row, may have been sorted
 	*/
	public TSIdent getTimeSeriesIdentifierFromTableModel( @SuppressWarnings("rawtypes") JWorksheet_AbstractRowTableModel tableModel,
		int row ) {
		//String routine = getClass().getSimpleName() + ".getTimeSeriesIdentifierFromTableModel";
    	Zabbix_TimeSeries_TableModel tm = (Zabbix_TimeSeries_TableModel)tableModel;
    	// Should not have any nulls.
   		String locId = (String)tableModel.getValueAt(row,tm.COL_LOCATION_ID);
   		locId = escapeTsidPart(locId);
    	String source = (String)tableModel.getValueAt(row,tm.COL_DATA_SOURCE);
   		source = escapeTsidPart(source);
    	String dataType = (String)tableModel.getValueAt(row,tm.COL_DATA_TYPE);
   		dataType = escapeTsidPart(dataType);
    	String interval = (String)tableModel.getValueAt(row,tm.COL_DATA_INTERVAL);
    	String scenario = "";
    	String inputName = ""; // Only used for files.
    	TSIdent tsid = null;
		String datastoreName = this.getName();
    	try {
    		tsid = new TSIdent(locId, source, dataType, interval, scenario, datastoreName, inputName );
    	}
    	catch ( Exception e ) {
    		throw new RuntimeException ( e );
    	}
    	return tsid;
	}

    /**
     * Get the CellRenderer used for displaying the time series in a TableModel.
     */
    @SuppressWarnings("rawtypes")
	public JWorksheet_AbstractExcelCellRenderer getTimeSeriesListCellRenderer(JWorksheet_AbstractRowTableModel tableModel) {
    	return new Zabbix_TimeSeries_CellRenderer ((Zabbix_TimeSeries_TableModel)tableModel);
    }

    /**
     * Get the TableModel used for displaying the time series.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
	public JWorksheet_AbstractRowTableModel getTimeSeriesListTableModel(List<? extends Object> data) {
    	return new Zabbix_TimeSeries_TableModel(this,(List<TimeSeriesCatalog>)data);
    }

    /**
     * Determine whether a data type is for trend data (ends in "-Avg", "-Max", or "-Min").
     * This does not check for "*" so must do that separately.
     * @param dataType data type to query
     * @return true if a data type is for trend data.
     */
    private boolean isTrendDataType ( String dataType ) {
		if ( dataType.endsWith("-Avg") || dataType.endsWith("Max") || dataType.endsWith("Min") ) {
			return true;
		}
		else {
			return false;
		}
	}

	/**
	 * Indicate whether the datastore provides a time series input filter.
	 * This datastore does provide an input filter panel.
	 */
	public boolean providesTimeSeriesListInputFilterPanel () {
		return true;
	}

	/**
	 * Read global data that should be kept in memory to increase performance.
	 * This is called from the constructor.
	 * The following data are read and are available with get() methods:
	 * <ul>
	 * <li>TimeSeriesCatalog - cache used to find time series without re-requesting from the web service</li>
	 * </ul>
	 * If an error is detected, set on the datastore so that TSTool View / Datastores will show the error.
	 * This is usually an issue with a misconfigured datastore.
	 */
	public void readGlobalData () {
		String routine = getClass().getSimpleName() + ".readGlobalData";
		Message.printStatus ( 2, routine, "Reading global data for datastore \"" + getName() + "\"." );

		// Read the host groups.

		try {
			this.globalHostGroupList = readHostGroupList();
			Message.printStatus(2, routine, "Read " + this.globalHostGroupList.size() + " host groups." );
		}
		catch ( Exception e ) {
			Message.printWarning(3, routine, "Error reading global host group list (" + e + ")");
			Message.printWarning(3, routine, e );
		}

		// Read the hosts.

		try {
			this.globalHostList = readHostList();
			Message.printStatus(2, routine, "Read " + this.globalHostList.size() + " hosts." );
		}
		catch ( Exception e ) {
			Message.printWarning(3, routine, "Error reading global host list (" + e + ")");
			Message.printWarning(3, routine, e );
		}

		// Read the items:
		// - can't seem to query all without a filter such as host group
		// - TODO smalers 2023-05-18 need to decide whether to read up front

		/*
		try {
			this.globalItemList = readItemList();
			Message.printStatus(2, routine, "Initialized " + this.globalItemList.size() + " items." );
		}
		catch ( Exception e ) {
			Message.printWarning(3, routine, "Error initializing global item list (" + e + ")");
			Message.printWarning(3, routine, e );
		}
		*/

		// Read the templates.

		try {
			this.globalTemplateList = readTemplateList(this.globalHostList);
			Message.printStatus(2, routine, "Read " + this.globalTemplateList.size() + " templates." );
		}
		catch ( Exception e ) {
			Message.printWarning(3, routine, "Error reading global templates list (" + e + ")");
			Message.printWarning(3, routine, e );
		}

		// Read the global list of history and trend time series catalog:
		// - process after reading other data because may need to use for derived data
		// - used to look up history time series when reading
		// - used to create choices for the UI
		// - TODO smalers need to create a data pyramid to streamline performance
		// - TODO smalers maybe don't need to create a trend time series catalog list if can do on the fly from the history catalog
		try {
			String tsid = null;
    		String dataTypeReq = null;
    		String dataIntervalReq = null;
    		InputFilter_JPanel ifp = null;
    		// Read the catalog for all time series.
			this.globalHistoryTscatalogList = readTimeSeriesCatalog(tsid, dataTypeReq, dataIntervalReq, ifp );
			Message.printStatus(2, routine, "Read " + this.globalHistoryTscatalogList.size() + " history time series catalog." );

			// Loop through the history time series catalog and create other global data:
			// - these are used in the TSTool UI and commands to improve performance
			// - create unique item name
			// - the lists are unique across all the data but may not be used for each host, etc/

			this.globalItemNameList = new ArrayList<>();
			this.globalTrendTscatalogList = new ArrayList<>();
			String itemName = null;
			boolean found;
			for ( TimeSeriesCatalog tscatalog : this.globalHistoryTscatalogList ) {
				itemName = tscatalog.getItemName();

				found = false;
				for ( String itemName2 : this.globalItemNameList ) {
					if ( itemName2.equals(itemName) ) {
						found = true;
						break;
					}
				}
				if ( !found ) {
					this.globalItemNameList.add(itemName);
				}
				
				// Create the trend time series catalog:
				// - only do for numeric item types
				// - copy the history catalog since most of the data will be the same
				// - modify the data type to include the statistic at the end
				// - set the interval to 'Hour'

				if ( tscatalog.isNumeric() ) {
					// 'Avg' statistic.
					TimeSeriesCatalog tscatalog2 = new TimeSeriesCatalog(tscatalog);
					tscatalog2.setDataType(tscatalog.getItemName() + "-Avg");
					tscatalog2.setDataInterval("Hour");
					this.globalTrendTscatalogList.add(tscatalog2);
					// 'Min' statistic.
					tscatalog2 = new TimeSeriesCatalog(tscatalog);
					tscatalog2.setDataType(tscatalog.getItemName() + "-Min");
					tscatalog2.setDataInterval("Hour");
					this.globalTrendTscatalogList.add(tscatalog2);
					// 'Max' statistic.
					tscatalog2 = new TimeSeriesCatalog(tscatalog);
					tscatalog2.setDataType(tscatalog.getItemName() + "-Max");
					tscatalog2.setDataInterval("Hour");
					this.globalTrendTscatalogList.add(tscatalog2);
				}
			}

			Message.printStatus(2, routine, "Read " + this.globalTrendTscatalogList.size()
				+ " trend time series catalog (3x history catalog minus text items)." );

			// Sort the simple lists.
			Collections.sort(this.globalItemNameList,String.CASE_INSENSITIVE_ORDER);
		}
		catch ( Exception e ) {
			Message.printWarning(3, routine, "Error reading global history and trend time series catalog lists (" + e + ")");
			Message.printWarning(3, routine, e );
		}

	}

    /**
     * Read the history list from the web service.
     * See (current): https://www.zabbix.com/documentation/current/en/manual/api/reference/history/get
     * See (5.4):  https://www.zabbix.com/documentation/5.4/en/manual/api/reference/history/get
     * @param itemid itemid to match
     * @param itemValueType the item value type
     * @param timeFrom timestamp to start read, UNIX epoch seconds
     * @param timeTill timestamp to end read, UNIX epoch seconds
     * @return the history list, may be an empty list if a problem or no data
     */
    private List<History> readHistoryList ( String itemid, int itemValueType, long timeFrom, long timeTill) {
		String routine = getClass().getSimpleName() + ".readHistoryList";
		String requestUrl = getServiceRootURI().toString();
		List<String> itemidList = new ArrayList<>();
		itemidList.add(itemid);
		// Seems to require 'history' and/or 'output'.
		String requestData =
			"{"
				+ "\"jsonrpc\": \"2.0\","
				+ "\"method\": \"history.get\","
				+ "\"params\": {"
					+ "\"history\": " + itemValueType + ","
					+ "\"output\": \"extend\","
					+ "\"sortfield\": \"clock\","
					+ "\"sortorder\": \"ASC\""
					+ getParamTimeFrom(true, timeFrom)
					+ getParamTimeTill(true, timeTill)
					+ getParamItemIds(true, itemidList)
				+ "},"
				+ "\"id\": 1"
				+ getAuthJSON()
    		+ "}";
		Message.printStatus(2, routine, "Request data = " + requestData);
		String dataElement = "result";  // 'result' contains an array of the objects.
		try {
			String historyJson = JacksonToolkit.getInstance().getJsonFromWebServiceUrl ( requestUrl, getApiToken(), requestData, dataElement );
			List<History> history = JacksonToolkit.getInstance().getObjectMapper().readValue(historyJson, new TypeReference<List<History>>(){});
			return history;
		}
		catch ( Exception e ) {
			Message.printWarning(3,routine,e);
			return new ArrayList<History>();
		}
    }

    /**
     * Read the host list for all hosts from the web service.
     * See (current): https://www.zabbix.com/documentation/current/en/manual/api/reference/host/get
     * See (5.4):  https://www.zabbix.com/documentation/5.4/en/manual/api/reference/host/get
     * @param propertyName property name to match for filter (e.g., 'host', 'name')
     * @param propertyValueList list of property values to match, or null (or empty) to read all
     * @return the host list, may be an empty list if a problem
     */
    private List<Host> readHostList () {
    	return readHostList ( null, null );
    }

    /**
     * Read the host list from the web service.
     * See (current): https://www.zabbix.com/documentation/current/en/manual/api/reference/host/get
     * See (5.4):  https://www.zabbix.com/documentation/5.4/en/manual/api/reference/host/get
     * @param propertyName property name to match for filter (e.g., 'host', 'name')
     * @param propertyValueList list of property values to match, or null (or empty) to read all
     * @return the host list, may be an empty list if a problem
     */
    private List<Host> readHostList ( String propertyName, List<String> propertyValueList ) {
		String routine = getClass().getSimpleName() + ".readHostList";
		String requestUrl = getServiceRootURI().toString();
		String requestData =
			"{"
				+ "\"jsonrpc\": \"2.0\","
				+ "\"method\": \"host.get\","
				+ "\"params\": {"
					+ "\"selectGroups\": \"extend\""
					+ getParamFilter(true, propertyName, propertyValueList)
				+ "},"
				+ "\"id\": 1"
				+ getAuthJSON()
    		+ "}";
		Message.printStatus(2, routine, "Reading hosts, requestData = " + requestData);
		String dataElement = "result";  // 'result' contains an array of the objects.
		try {
			String hostJson = JacksonToolkit.getInstance().getJsonFromWebServiceUrl ( requestUrl, getApiToken(), requestData, dataElement );
			List<Host> hosts = JacksonToolkit.getInstance().getObjectMapper().readValue(hostJson, new TypeReference<List<Host>>(){});
			return hosts;
		}
		catch ( Exception e ) {
			Message.printWarning(3,routine,e);
			return new ArrayList<Host>();
		}
    }

    /**
     * Read the host group list from the web service.
     * See (current): https://www.zabbix.com/documentation/current/en/manual/api/reference/hostgroup/get
     * See (5.4):  https://www.zabbix.com/documentation/5.4/en/manual/api/reference/hostgroup/get
     * @return the host group list, may be an empty list if a problem
     */
    private List<HostGroup> readHostGroupList () {
		String routine = getClass().getSimpleName() + ".readHostGroupList";
		String requestUrl = getServiceRootURI().toString();
		// Add filters later, if necessary.
		String requestData =
			"{"
				+ "\"jsonrpc\": \"2.0\","
				+ "\"method\": \"hostgroup.get\","
				+ "\"params\": {"
				+   "\"output\": \"extend\""
				+ "},"
				+ "\"id\": 1"
				+ getAuthJSON()
    		+ "}";
		String dataElement = "result";  // 'result' contains an array of the objects.
		try {
			String hostGroupJson = JacksonToolkit.getInstance().getJsonFromWebServiceUrl ( requestUrl, getApiToken(), requestData, dataElement );
			List<HostGroup> hostGroups = JacksonToolkit.getInstance().getObjectMapper().readValue(hostGroupJson, new TypeReference<List<HostGroup>>(){});
			return hostGroups;
		}
		catch ( Exception e ) {
			Message.printWarning(3,routine,e);
			return new ArrayList<HostGroup>();
		}
    }

    /**
     * Read the item list from the web service.
     * See (current): https://www.zabbix.com/documentation/current/en/manual/api/reference/item/get
     * See (5.4):  https://www.zabbix.com/documentation/5.4/en/manual/api/reference/item/get
     * @param hostList list of Host to filter items
     * (must be non-null an non-empty because items can't be queried without a filter)
     * @return the item list, may be an empty list if a problem
     */
    private List<Item> readItemList ( List<Host> hostList ) {
		String routine = getClass().getSimpleName() + ".readItemList";
		String requestUrl = getServiceRootURI().toString();
		String sortParam = "";
		if ( hostList.size() == 1 ) {
			// Sort by item name:
			// - TODO smalers 2023-05-22 implement a tree for storage to increase optimization and multi-level sort
			sortParam = "\"sortfield\": \"name\",";
		}
		String requestData =
			"{"
				+ "\"jsonrpc\": \"2.0\","
				+ "\"method\": \"item.get\","
				+ "\"params\": {"
					+ sortParam
					+ getParamHostIds(false, hostList)
				+ "},"
				+ "\"id\": 1"
				+ getAuthJSON()
    		+ "}";
		Message.printStatus(2, routine, "Request data = " + requestData);
		String dataElement = "result";  // 'result' contains an array of the objects.
		try {
			String itemJson = JacksonToolkit.getInstance().getJsonFromWebServiceUrl ( requestUrl, getApiToken(), requestData, dataElement );
			List<Item> items = JacksonToolkit.getInstance().getObjectMapper().readValue(itemJson, new TypeReference<List<Item>>(){});
			return items;
		}
		catch ( Exception e ) {
			Message.printWarning(3,routine,e);
			return new ArrayList<Item>();
		}
    }

    /**
     * Read the template list from the web service.
     * See (current): https://www.zabbix.com/documentation/current/en/manual/api/reference/template/get
     * See (5.4):  https://www.zabbix.com/documentation/5.4/en/manual/api/reference/template/get
     * @param hostList list of hots to read templates
     * @return the template list, may be an empty list if a problem
     */
    private List<Template> readTemplateList ( List<Host> hostList ) {
		String routine = getClass().getSimpleName() + ".readTemplateList";
		String requestUrl = getServiceRootURI().toString();
		// Add filters later, if necessary.
		String propertyName = "hostids";
		List<String> propertyValueList = new ArrayList<>();
		for ( Host host : hostList ) {
			propertyValueList.add(host.getHostid());
		}
		String requestData =
			"{"
				+ "\"jsonrpc\": \"2.0\","
				+ "\"method\": \"template.get\","
				+ "\"params\": {"
				+   "\"output\": \"extend\""
					+ getParamFilter(true, propertyName, propertyValueList)
				+ "},"
				+ "\"id\": 1"
				+ getAuthJSON()
    		+ "}";
		String dataElement = "result";  // 'result' contains an array of the objects.
		Message.printStatus(2, routine, "Reading templates, requestData = " + requestData);
		try {
			String templateJson = JacksonToolkit.getInstance().getJsonFromWebServiceUrl ( requestUrl, getApiToken(), requestData, dataElement );
			List<Template> templates = JacksonToolkit.getInstance().getObjectMapper().readValue(templateJson, new TypeReference<List<Template>>(){});
			/*
			for ( Template template : templates ) {
				Message.printStatus( 2, routine, " templateid=" + template.getTemplateid() + " name=" + template.getName()
					+ " host=" + template.getHost() );
			}
			*/
			return templates;
		}
		catch ( Exception e ) {
			Message.printWarning(3,routine,e);
			return new ArrayList<Template>();
		}
    }

    /**
     * Read a single time series given its time series identifier using default read properties.
     * This is typically called by TSID command, which uses default properties.
     * @param tsid time series identifier.
     * @param readStart start of read, will be set to 'periodStart' service parameter.
     * @param readEnd end of read, will be set to 'periodEnd' service parameter.
     * @return the time series or null if not read
     */
    public TS readTimeSeries ( String tsid, DateTime readStart, DateTime readEnd, boolean readData ) {
    	String routine = getClass().getSimpleName() + ".readTimeSeries";

    	try {
    		Message.printStatus(2, routine, "Reading time series \"" + tsid + "\".");
    		HashMap<String,Object> readProperties = null;
    		return readTimeSeries ( tsid, readStart, readEnd, readData, readProperties );
    	}
    	catch ( Exception e ) {
    		// Throw a RuntimeException since the method interface does not include an exception type.
    		Message.printWarning(2, routine, e);
    		throw new RuntimeException ( e );
    	}
    }

    /**
     * Read a single time series given its time series identifier.
     * @param tsidReq requested time series identifier.
     * @param readStart start of read, will be set to 'periodStart' service parameter.
     * @param readEnd end of read, will be set to 'periodEnd' service parameter.
     * @param readProperties additional properties to control the query:
     * <ul>
     * <li> "Debug" - if true, turn on debug for the query</li>
     * <li> "ShiftTrendToIntervalEnd" - if boolean or String is true, shift trend clock by 3600 seconds to the end of the interval
     *      (Zabbix uses the start of the interval), default value is true</li>
     * <li> "TimeZone" - the time zone to use for time series date/times</li>
     * <li> "TextValue" - the time series numerical value to use when the history time series value is text,
     *      for example use an integer plotting position</li>
     * </ul>
     * @return the time series or null if not read
     */
    public TS readTimeSeries ( String tsidReq, DateTime readStart, DateTime readEnd,
    	boolean readData, HashMap<String,Object> readProperties ) throws Exception {
    	String routine = getClass().getSimpleName() + ".readTimeSeries";
    	
    	Message.printStatus(2, routine, "Reading time series \"" + tsidReq + "\" from " + readStart + " to " + readEnd );

    	// Set properties for specific functionality.
    	
    	// Default is to use TSTool end of interval time for trend.
    	boolean shiftTrendToIntervalEnd = true;

    	String timeZone = null;
    	Double textValue = null;
    	// If true, convert the text value to a number.
    	boolean doTextAsNumber = false;
    	if ( readProperties != null ) {
    		// TODO need to enable debug as temporary setting in this function
    		Object propObject = readProperties.get ( "Debug" );

    		propObject = readProperties.get ( "ShiftTrendToIntervalEnd" );
    		if ( propObject != null ) {
    			if ( propObject instanceof String ) {
    				String propString = (String)propObject;
    				if ( propString.equalsIgnoreCase("true") ) {
    					shiftTrendToIntervalEnd = true;
    				}
    				else if ( propString.equalsIgnoreCase("false") ) {
    					shiftTrendToIntervalEnd = false;
    				}
    			}
    			else if ( propObject instanceof Boolean ) {
    				shiftTrendToIntervalEnd = (Boolean)propObject;
    			}
    		}

    		propObject = readProperties.get ( "TimeZone" );
    		if ( propObject != null ) {
    			timeZone = (String)propObject;
    		}

    		propObject = readProperties.get ( "TextValue" );
    		if ( propObject != null ) {
    			if ( propObject instanceof String ) {
    				// Convert the string to a number for the value.
    				String textValue0 = (String)propObject;
    				if ( textValue0.equalsIgnoreCase("Text") ) {
    					doTextAsNumber = true;
    				}
    				if ( StringUtil.isDouble(textValue0) ) {
    					textValue = Double.valueOf(textValue0);
    				}
    			}
    			else if ( propObject instanceof Double ) {
    				// Double number was passed.
    				textValue = (Double)propObject;
    			}
    			else if ( propObject instanceof Integer ) {
    				// Integer number was passed.
    				textValue = new Double((Integer)propObject);
    			}
    		}
    	}

    	// Create a time series identifier for the requested TSID:
    	// - the actual output may be set to a different identifier based on the above properties
    	// - also save interval base and multiplier for the original request
    	TSIdent tsidentReq = TSIdent.parseIdentifier(tsidReq);

    	// Up front, check for invalid request and throw exceptions:
   		// - some cases are OK as long as IrregularInterval was specified in ReadKiWIS

    	// Time series catalog for the single matching time series.
 		TimeSeriesCatalog tscatalog = null;

 		// Get the location:
 		// - remove surrounding quotes
 		// - the locId corresponds to the host.host
 		String locId = tsidentReq.getLocation();
 		locId = locId.replace("'", "");
 		// Get the data source:
 		// - remove surrounding quotes
 		// - the dataSource corresponds to the hostgroup.name
 		String dataSource = tsidentReq.getSource();
 		dataSource = dataSource.replace("'", "");
 		// Get the data interval:
 		// - for history should be 'IrregSecond'
 		// - for trend should be 'Hour'
 		String dataInterval = tsidentReq.getInterval();
 		// Get the data type:
 		// - remove surrounding quotes
 		// - the dataType corresponds to the item.name
 		String dataType = tsidentReq.getType();
 		dataType = dataType.replace("'", "");
 		// If the dataType ends in '-Avg', '-Max', '-Min', read the trend time series:
 		// - default is to read history
 		boolean readHistory = true;
 		boolean readTrend = false;
 		// Which statistic to read:
 		// -1 = Min
 		// 0 = Avg
 		// 1 = Max
 		int valueStat = -999;
 		if ( isTrendDataType(dataType) ) {
 			// Trend time series.
 			readTrend = true;
 			readHistory = false;
 			if ( !dataInterval.equalsIgnoreCase("Hour") ) {
 				throw new InvalidTimeIntervalException (
 					"Requested time series \"" + tsidReq + "\" is trend but interval is not Hour.");
 			}
 			if ( dataType.endsWith("-Avg") ) {
 				valueStat = 0;
 			}
 			else if ( dataType.endsWith("-Min") ) {
 				valueStat = -1;
 			}
 			else if ( dataType.endsWith("-Max") ) {
 				valueStat = 1;
 			}
 		}
 		else {
 			// History time series.
 			readTrend = false;
 			readHistory = true;
 			if ( !dataInterval.equalsIgnoreCase("IrregSecond") ) {
 				throw new InvalidTimeIntervalException (
 					"Requested time series \"" + tsidReq + "\" is history but interval is not IrregSecond.");
 			}
 		}

 		// Create the time series.
 		TS ts = null;
    	try {
    		ts = TSUtil.newTimeSeries(tsidentReq.toString(), true);
    		ts.setIdentifier(tsidentReq);
    	}
    	catch ( Exception e ) {
    		throw new RuntimeException ( e );
    	}

    	// Get the matching TimeSeriesCatalog by matching the 'tsidReq':
    	// - in this case the TSID controls so set the other filters to null
    	String dataTypeReq = null;
    	String dataIntervalReq = null;
    	InputFilter_JPanel ifp = null;
    	List<TimeSeriesCatalog> tscatalogList = readTimeSeriesCatalog ( tsidReq, dataTypeReq, dataIntervalReq, ifp );
		if ( tscatalogList.size() == 0 ) {
			throw new RuntimeException ( "Did not match any 'tscatalog' for tsid \"" + tsidReq + "\".");
		}
		else if ( tscatalogList.size() == 1 ) {
			tscatalog = tscatalogList.get(0);
		}
		else {
			throw new RuntimeException ( "Matched " + tscatalogList.size()
				+ " 'tscatalog' but expecting 1 for TSID \"" + tsidReq + "\".");
		}

    	if ( tscatalog == null ) {
    		throw new RuntimeException ( "Unable to match 'tscatalog' for tsid = \"" + tsidReq + "\"." );
    	}
    	
   		// Get the host object matching the TimeSeriesCcatalog:
   		// - this is used to check the time zone for output
    	// - it is also used for the time series description
   		Host host = Host.lookupHostForId(this.globalHostList, tscatalog.getHostId() );
   		String hostTimeZone = null;
   		ZoneId hostZoneId = null;
    	if ( timeZone != null ) {
    		// Time zone is specified by the calling code, not host description.
    		hostTimeZone = timeZone;
			hostZoneId = ZoneId.of(hostTimeZone);
			Message.printStatus(2, routine, "Time zone from read parameter is \"" + hostTimeZone + "\".");
    	}
    	else {
    		if ( host == null ) {
   				Message.printStatus(2, routine, "Unable to find cached host ID=" + tscatalog.getHostId() );
    		}
    		else {
    			String description = host.getDescription();
    			// Parse the description to get embedded properties.
    			EmbeddedPropertiesString eps = new EmbeddedPropertiesString(description, "//");
    			hostTimeZone = eps.getPropertyValue ( "Timezone" );
    			if ( hostTimeZone == null ) {
    				// Try the other variation.
    				hostTimeZone = eps.getPropertyValue ( "TimeZone" );
    			}
    			if ( hostTimeZone == null ) {
    				Message.printStatus(2, routine, "Host id=" + host.getHostid()
    					+ " host=\"" + host.getHost() + "\" name=\"" + host.getName()
    					+ "\" does not specify time zone.  Data will use GMT.");
    			}
    			else {
    				// Have a time zone for the host:
    				// - get the ZoneId for use below
    				hostZoneId = ZoneId.of(hostTimeZone);
    				Message.printStatus(2, routine, "Host \"" + host.getHost() + "\" name=\"" + host.getName()
    					+ "\" uses time zone \"" + hostTimeZone + "\".");
    			}
    		}
    	}

   		// Set the standard time series properties from the catalog.
    	ts.setDataUnits(tscatalog.getDataUnits());
    	ts.setDataUnitsOriginal(tscatalog.getDataUnits());
    	// Use the name for the description because the description is multi-line and fouls up legends.
    	ts.setDescription(host.getName());
    	setTimeSeriesProperties(ts, tscatalog);

    	if ( readData ) {
    		// Read the 'history' for the time series.
    		long timeFrom = -1;
    		long timeTill = -1;
    		if ( readStart != null ) {
    			// Read start is specified:
    			// - if not specified, will use default history period
    			// - if specified, assumed to be in local time for the host
				if ( hostZoneId == null ) {
					// Don't have host time zone so use GMT.
					timeFrom = TimeUtil.toUnixTime(readStart, true)/1000;
				}
				else {
					// Convert the 'readStart' (assumed to be in host time zone) to GMT for the API.
					readStart.setTimeZone(hostTimeZone);
					ZonedDateTime readStartHostZone = readStart.toZonedDateTime(null);
					ZoneId zoneIdGmt = ZoneId.of("GMT");
					ZonedDateTime readStartGmt = readStartHostZone.withZoneSameInstant(zoneIdGmt);
					timeFrom = readStartGmt.toEpochSecond();
				}
    		}
    		if ( readEnd != null ) {
    			// Read end is specified:
    			// - if not specified, will use default history period
    			// - if specified, assumed to be in local time for the host
				if ( hostZoneId == null ) {
					// Don't have host time zone so use GMT.
					timeTill = TimeUtil.toUnixTime(readEnd, true)/1000;
				}
				else {
					// Convert the 'readEnd' (assumed to be in host time zone) to GMT for the API.
					readEnd.setTimeZone(hostTimeZone);
					ZonedDateTime readEndHostZone = readEnd.toZonedDateTime(null);
					ZoneId zoneIdGmt = ZoneId.of("GMT");
					ZonedDateTime readEndGmt = readEndHostZone.withZoneSameInstant(zoneIdGmt);
					timeTill = readEndGmt.toEpochSecond();
				}
    		}
    		if ( readHistory ) {
    			// Reading the history data into an irregular interval time series:
    			// - don't need to allocate memory
    			List<History> historyList = readHistoryList (
    				tscatalog.getItemId(), tscatalog.getItemValueTypeNum(), timeFrom, timeTill );
    			Message.printStatus(2,routine,"Read " + historyList.size() + " history records for timefrom="
    				+ timeFrom + " timetill=" + timeTill + ".");
    			// If any data were returned, add to the time series.
    			double value;
    			String flag = null;
    			History history = null;
    			DateTime dt = null;

    			// Check whether the item provides a numerical value:
    			// - if so, then the time series value will contain the item value
    			// - if not, the flag will contain the item value
    			boolean isItemNumeric = tscatalog.isNumeric();
    			double missingValue = ts.getMissing();
    			
    			// Loop through the records and add the data to the time series.
    			if ( historyList.size() > 0 ) {
    				// Set the period:
    				// - note that 'clock' is seconds but TimeUtil.fromUnixTime() accepts ms.
    				DateTime dataStart = null;
    				DateTime dataEnd = null;
    				ZonedDateTime zonedDateTime = null;
    				long clockStart = Long.parseLong(historyList.get(0).getClock());
    				long clockEnd = Long.parseLong(historyList.get(historyList.size() - 1).getClock());
					if ( hostZoneId == null ) {
						// Leave the host time zone as GMT:
						// - the following takes milliseconds
						dataStart = TimeUtil.fromUnixTime(clockStart*1000, null);
						dataEnd = TimeUtil.fromUnixTime(clockEnd*1000, null);
					}
					else {
						// Adjust the time zone from GMT to the host time zone.
    					zonedDateTime = Instant.ofEpochMilli(clockStart*1000).atZone(hostZoneId);
   						dataStart = new DateTime(zonedDateTime, DateTime.PRECISION_SECOND, hostZoneId.toString());
    					zonedDateTime = Instant.ofEpochMilli(clockEnd*1000).atZone(hostZoneId);
   						dataEnd = new DateTime(zonedDateTime, DateTime.PRECISION_SECOND, hostZoneId.toString());
					}
 					dataStart.setPrecision(DateTime.PRECISION_SECOND);
 					dataEnd.setPrecision(DateTime.PRECISION_SECOND);
    				Message.printStatus(2, routine, "Setting period to " + dataStart + " to " + dataEnd );
    				ts.setDate1(dataStart);
    				ts.setDate1Original(dataStart);
    				ts.setDate2(dataEnd);
    				ts.setDate2Original(dataEnd);
    				for ( int i = 0; i < historyList.size(); i++ ) {
    					try {
    						history = historyList.get(i);
    						long clock = Long.parseLong(history.getClock());
   							if ( hostZoneId == null ) {
   								// Using GMT so can directly work with DateTime.
   								if ( i == 0 ) {
   									// Create the DateTime the first time.
   									dt = TimeUtil.fromUnixTime(clock*1000, null);
   									dt.setPrecision(DateTime.PRECISION_SECOND);
   								}
   								else {
   									// Reuse the same DateTime.
   									TimeUtil.fromUnixTime(clock*1000, dt);
   								}
   							}
   							else {
    							// Host time zone is specified (e.g., "America/Denver"):
    							// - the legacy DateTime.shiftTimeZone() does not yet handle new java.time
    							// - therefore use ZonedDateTime to convert to the desired time zone.
   								// - this is slower because a new DateTime instance is created for each value
    							zonedDateTime = Instant.ofEpochMilli(clock*1000).atZone(hostZoneId);
   								dt = new DateTime(zonedDateTime, DateTime.PRECISION_SECOND, hostZoneId.toString());
    						}
   							if ( isItemNumeric ) {
   								// Set the numeric value without a flag (since flag is not used).
   								value = Double.parseDouble(history.getValue());
    							ts.setDataValue(dt, value);
   							}
   							else {
   								// Set the text value as the flag:
   								// - TODO smalers 2023-05-26 perhaps there is a numerical value that makes sense,
   								//   like a constant or a lookup from the text value.
   								if ( doTextAsNumber ) {
   									// Convert the text value to a number.
   									value = Double.parseDouble(history.getValue());
   								}
   								else if ( textValue != null ) {
   									// Use the provided value.
   									value = textValue;
   								}
   								else {
   									// Use the time series missing value.
   									value = missingValue;
   								}
   								flag = history.getValue();
    							ts.setDataValue(dt, value, flag, 0);
   							}
   						}
    					catch ( Exception e ) {
    						// Should not happen:
    						// - catch exceptions so that only bad values are ignored
    					}
    				}
    			}
    		}
    		else if ( readTrend ) {
    			// Reading the trend data into a hour interval time series:
    			// - allocate based on the records that are returned
    			// - all trend data are numeric
    			List<Trend> trendList = readTrendList ( tscatalog.getItemId(), timeFrom, timeTill );
    			Message.printStatus(2,routine,"Read " + trendList.size() + " trend records for timefrom="
    				+ timeFrom + " timetill=" + timeTill + ".");
    			// If any data were returned, add to the time series.
    			double value = ts.getMissing();
    			Trend trend = null;
    			DateTime dt = null;
    			if ( trendList.size() > 0 ) {
    				// Set the period:
    				// - note that 'clock' is seconds but TimeUtil.fromUnixTime() accepts ms.
    				// - clock is also the start of the interval so shift by an hour to agree with TSTool conventions
    				DateTime dataStart = null;
    				DateTime dataEnd = null;
    				ZonedDateTime zonedDateTime = null;
   					long clockStart = 0;
   					long clockEnd = 0;
    				boolean dataIsSorted = false;
    				if ( dataIsSorted ) {
    					// Should be able to do this.
    					clockStart = Long.parseLong(trendList.get(0).getClock());
    					clockEnd = Long.parseLong(trendList.get(trendList.size() - 1).getClock());
    				}
    				else {
    					// However, trend data are apparently not sorted by clock so have to determine the clock limits.
    					// See:  https://support.zabbix.com/browse/ZBXNEXT-3974
    					long [] clockLimits = Trend.getClockLimits(trendList);
    					clockStart = clockLimits[0];
    					clockEnd = clockLimits[1];
    				}
    				if ( shiftTrendToIntervalEnd ) {
    					// Zabbix returns timestamp at interval start.  Shift to interval end consistent with TSTool.
    					clockStart += 3600;
    					clockEnd += 3600;
    				}
					if ( hostZoneId == null ) {
						// Adjust the time zone.
						dataStart = TimeUtil.fromUnixTime(clockStart*1000, null);
						dataEnd = TimeUtil.fromUnixTime(clockEnd*1000, null);
					}
					else {
    					zonedDateTime = Instant.ofEpochMilli(clockStart*1000).atZone(hostZoneId);
   						dataStart = new DateTime(zonedDateTime, DateTime.PRECISION_SECOND, hostZoneId.toString());
    					zonedDateTime = Instant.ofEpochMilli(clockEnd*1000).atZone(hostZoneId);
   						dataEnd = new DateTime(zonedDateTime, DateTime.PRECISION_SECOND, hostZoneId.toString());
					}
 					dataStart.setPrecision(DateTime.PRECISION_SECOND);
 					dataEnd.setPrecision(DateTime.PRECISION_SECOND);
    				Message.printStatus(2, routine, "Setting period to " + dataStart + " to " + dataEnd );
    				ts.setDate1(dataStart);
    				ts.setDate1Original(dataStart);
    				ts.setDate2(dataEnd);
    				ts.setDate2Original(dataEnd);
    				ts.allocateDataSpace();
    				for ( int i = 0; i < trendList.size(); i++ ) {
    					try {
    						trend = trendList.get(i);
    						long clock = Long.parseLong(trend.getClock());
    						if ( shiftTrendToIntervalEnd ) {
    							// Zabbix returns timestamp at interval start.  Shift to interval end consistent with TSTool.
    							clock += 3600;
    						}
    						if ( valueStat < 0 ) {
    							value = Double.parseDouble(trend.getValueMin());
    						}
    						else if ( valueStat == 0 ) {
    							value = Double.parseDouble(trend.getValueAvg());
    						}
    						else if ( valueStat == 1 ) {
    							value = Double.parseDouble(trend.getValueMax());
    						}
   							if ( hostZoneId == null ) {
   								// Using GMT so can directly work with DateTime.
   								if ( i == 0 ) {
   									// Create the DateTime the first time.
   									dt = TimeUtil.fromUnixTime(clock*1000, null);
   									dt.setPrecision(DateTime.PRECISION_SECOND);
   								}
   								else {
   									// Reuse the same DateTime.
   									TimeUtil.fromUnixTime(clock*1000, dt);
   								}
   							}
   							else {
    							// Host time zone is specified (e.g., "America/Denver"):
    							// - the legacy DateTime.shiftTimeZone() does not yet handle new java.time
    							// - therefore use ZonedDateTime to convert to the desired time zone.
   								// - this is slower because a new DateTime instance is created for each value
    							zonedDateTime = Instant.ofEpochMilli(clock*1000).atZone(hostZoneId);
   								dt = new DateTime(zonedDateTime, DateTime.PRECISION_SECOND, hostZoneId.toString());
    						}
    						ts.setDataValue(dt, value);
    					}
    					catch ( Exception e ) {
    						// Should not happen:
    						// - catch exceptions so that only bad values are ignored
    					}
    				}
    			}
    		}
    	}

    	return ts;
    }

	/**
	 * Read time series catalog, which uses the "/getTimeseriesList" web service query.
	 * @param tsid requested time series identifier, called from readTimeSeries() to get metadata,
	 *        if null use the filters to read 1+ time series catalog
	 * @param dataTypeReq Requested data type (e.g., "DischargeRiver") or "*" to read all data types,
	 *        or null to use default of "*".
	 * @param dataIntervalReq Requested data interval (e.g., "IrregSecond") or "*" to read all intervals,
	 *        or null to use default of "*".
	 * @param ifp input filter panel with "where" conditions, if null or all blank will not be used
	 */
	public List<TimeSeriesCatalog> readTimeSeriesCatalog ( String tsid, String dataTypeReq, String dataIntervalReq, InputFilter_JPanel ifp ) {
		String routine = getClass().getSimpleName() + ".readTimeSeriesCatalog";

		// The following are checked below to know when the data type contains a _1, etc.
		Message.printStatus(2,routine,"Reading time series catalog using:" );
		Message.printStatus(2,routine,"  tsid=\"" + tsid + "\"");
		Message.printStatus(2,routine,"  dataTypeReq=\"" + dataTypeReq + "\"" );
		Message.printStatus(2,routine,"  dataIntervalReq=\"" + dataIntervalReq + "\"");
		if ( ifp == null ) {
			Message.printStatus(2,routine,"  ifp=null");
		}
		else {
			Message.printStatus(2,routine,"  ifp=not null");
			Message.printStatus(2,routine,"  ifp has any non-blank input=" + ifp.hasInput());
		}

		// Determine the Item list, which will each match one time series.
		List<Item> itemList = new ArrayList<>();

		// By default, use the cache to look up time series rather than doing a new TimeSeriesCatalog query.
		boolean useCache = true;
		if ( (tsid != null) && !tsid.isEmpty() ) {
			Message.printStatus(2, routine, "Reading single time series \"" + tsid + "\"." );
			// Reading a single time series catalog, for example when reading a single time series.
			TSIdent tsident = null;
			try {
				tsident = TSIdent.parseIdentifier(tsid);
			}
			catch ( Exception e ) {
				throw new RuntimeException ( "Error parsing TSID \"" + tsid + "\"" );
			}
			// Get the location:
			// - remove surrounding quotes
			// - the locId corresponds to the host.host

			String locId = tsident.getLocation();
			locId = locId.replace("'", "");
			// Get the data source:
			// - remove surrounding quotes
			// - the dataSource corresponds to the hostgroup.name
			String dataSource = tsident.getSource();
			dataSource = dataSource.replace("'", "");
			// Get the data interval:
			// - for history should be 'IrregSecond'
			// - for trend should be 'Hour'
			String dataInterval = tsident.getInterval();
			// Get the data type:
			// - remove surrounding quotes
			// - the dataType corresponds to the item.name
			String dataType = tsident.getType();
			dataType = dataType.replace("'", "");
			// If the dataType ends in '-Avg', '-Max', '-Min', read the trend time series:
			// - default is to read history
			boolean readHistory = true;
			boolean readTrend = false;
			// Data type without the statistic, which is in the history catalog.
			String dataTypeNoStat = dataType;
			if ( isTrendDataType(dataType) ) {
				// Trend time series.
				readTrend = true;
				readHistory = false;
				if ( !dataInterval.equalsIgnoreCase("Hour") ) {
					throw new InvalidTimeIntervalException (
						"Requested time series \"" + tsid + "\" is for trend but interval is not Hour.");
				}
				dataTypeNoStat = dataType.substring(0,dataType.length() - 4);
			}
			else {
				// History time series.
				if ( !dataInterval.equalsIgnoreCase("IrregSecond") ) {
					throw new InvalidTimeIntervalException (
						"Requested time series \"" + tsid + "\" is for history but interval is not IrregSecond.");
				}
			}

			if ( useCache ) {
				// Find the time series in the global time series catalog:
				// - dataTypeReq - from the TSID
				// - dataIntervalReq - from the TSID
				// - dataSource - same as host group name
				// - locId - same as the host
				// - hostName - not used with TSID search
				// - itemName - not used with TSID search
				List<TimeSeriesCatalog> tsidCatalogList = null;
				String hostName = null;
				String itemName = null;
				if ( readHistory ) {
					tsidCatalogList = TimeSeriesCatalog.lookupCatalog (
						this.globalHistoryTscatalogList, dataType, dataInterval, dataSource, locId, hostName, itemName );
				}
				else if ( readTrend ) {
					tsidCatalogList = TimeSeriesCatalog.lookupCatalog (
						this.globalTrendTscatalogList, dataType, dataInterval, dataSource, locId, hostName, itemName );
				}
				// Should match a single time series.
				if ( tsidCatalogList.size() == 0 ) {
					throw new RuntimeException ( "Did not match any 'tscatalog' for tsid \"" + tsid + "\".");
				}
				else if ( tsidCatalogList.size() == 1 ) {
					return tsidCatalogList;
				}
				else {
					throw new RuntimeException ( "Matched " + tsidCatalogList.size()
						+ " 'tscatalog' but expecting 1 for TSID \"" + tsid + "\".");
				}
			}
			else {
				// Read the time series catalog for the specific TSID:
				// - need to read the matching Item

				throw new RuntimeException ( "Reading a time series catalog without using the cache is not implemented." );
				//itemList = readItemListForHostGroupAndHost ( dataSource, locId );
			}
		}
		else {
			// Reading one or more time series catalog by matching the query filters:
			// - if no filters, return the full cache if available
			boolean doReadCatalog = false; // Whether to do a read using the API.

			// Requested data type might indicate whether historical or trend time series:
			// - default to reading history but not trend and change below based on input
			boolean readTrend = false;
			boolean readHistory = true;
			String dataTypeReqNoStat = dataTypeReq;
			if ( (dataTypeReq != null) && isTrendDataType(dataTypeReq) ) {
				// Trend time series will be read.
				readTrend = true;
				readHistory = false;
				if ( !dataIntervalReq.equals("*") && !dataIntervalReq.equalsIgnoreCase("Hour") ) {
					throw new InvalidTimeIntervalException (
						"Requested data type \"" + dataTypeReq + "\" is for trend but interval is not Hour.");
				}
				dataTypeReqNoStat = dataTypeReq.substring(0,dataTypeReq.length() - 4);
			}
			else {
				// History time series:
				// - either reading all
				// - or not reading trend (above), so reading history
				readHistory = true;
				if ( (dataTypeReq != null) && !dataTypeReq.equals("*") && !dataIntervalReq.equalsIgnoreCase("IrregSecond") ) {
					throw new InvalidTimeIntervalException (
						"Requested data type \"" + dataTypeReq + "\" is for history but interval is not IrregSecond.");
				}
			}

			if ( useCache ) {
				// Attempt to find the requested time series catalog in the cache:
				// - in certain cases, may need to do a read, which will generally be slower
				Message.printStatus(2, routine, "Initial checks to read time series catalog using the cache.");
				if ( ((dataTypeReq == null) || dataTypeReq.isEmpty() || dataTypeReq.equals("*"))
					&& ((dataIntervalReq == null) || dataIntervalReq.isEmpty() || dataIntervalReq.equals("*"))
					&& ((ifp == null) || !ifp.hasInput()) ) {
					// No filters have been requested, typically:
					// - read global data at startup
					Message.printStatus(2, routine, "  No filters have been specified.");
					if ( (this.globalHistoryTscatalogList != null) && (this.globalHistoryTscatalogList.size() > 0) ) {
						// Have global data so return.
						Message.printStatus(2, routine, "  Returning the global cache (size="
							+ this.globalHistoryTscatalogList.size() + ").");
						return this.globalHistoryTscatalogList;
					}
					else {
						// Don't have the global catalog:
						// - read the full catalog below.
						Message.printStatus(2, routine, "  No cache has been read so read using the API." );
						doReadCatalog = true;
					}
				}
				else {
					// Have some filters to apply when searching the cached TimeSeriesCatalog:
					// - if called from the TSTool UI, data type and interval will always have values
					Message.printStatus(2, routine, "  Getting time series catalog from the cache by matching filters." );
					String hostGroupName = ifp.getInputValue("hostgroup.name", false);
					String host = ifp.getInputValue("host.host", false);
					String hostName = ifp.getInputValue("host.name", false);
					String itemName = ifp.getInputValue("item.name",false);
					Message.printStatus(2, routine, "  dataTypeReq=\"" + dataTypeReq + "\"" );
					Message.printStatus(2, routine, "  dataIntervalReq=" + dataIntervalReq );
					Message.printStatus(2, routine, "  hostgroup.name=\"" + hostGroupName + "\"" );
					Message.printStatus(2, routine, "  host=" + host );
					Message.printStatus(2, routine, "  host.name=\"" + hostName + "\"" );
					Message.printStatus(2, routine, "  item.name=\"" + itemName + "\"" );
					List<TimeSeriesCatalog> tscatalogList = new ArrayList<>();
					// Read the time series matching the filters:
					// - if "*", reading history and trend
					// - otherwise, read one or the other based on the data interval
					if ( dataTypeReq.equals("*") || readHistory ) {
						// Try to match history time series.
						tscatalogList = TimeSeriesCatalog.lookupCatalog(this.globalHistoryTscatalogList,
							dataTypeReq,
							dataIntervalReq,
							hostGroupName,
							host,
							hostName,
							itemName );
						Message.printStatus(2, routine, "  Matched " + tscatalogList.size() + " history time series catalog." );
					}
					if ( dataTypeReq.equals("*") || readTrend ) {
						// Try to match trend time series.
						List<TimeSeriesCatalog> tscatalogList2 = TimeSeriesCatalog.lookupCatalog(this.globalTrendTscatalogList,
							dataTypeReq,
							dataIntervalReq,
							hostGroupName,
							host,
							hostName,
							itemName );
						tscatalogList.addAll(tscatalogList2);
						Message.printStatus(2, routine, "  Matched " + tscatalogList2.size() + " trend time series catalog." );
					}
					return tscatalogList;
				}
			}
			else {
				// Not reading using the cache so read the catalog from the API below.
				doReadCatalog = true;
			}
			if ( doReadCatalog ) {
				// Either the cache was not requested, or cannot use the cache and 'doReadCatalog=true' was set above.
				
				// Read the catalog using API calls:
				// - the global cache is not used
				// - may be the full catalog (e.g., for global cache)
				// - or, may be filtered 
				// Reading multiple time series.
				if ( (dataIntervalReq != null) && !dataIntervalReq.isEmpty() && !dataIntervalReq.equals("*") ) {
					// TODO smalers 2023-05-17 how to determine average, etc.
					// Default to 'IrregSecond'.
				}

				// Zabbix items correspond to data types + interval.
				// However, must limit the 'item' queries using a host group or host.
				// Determine if host group or host is specified in the input filter.

				String hostGroupName = getInputFilterWhere ( ifp, "hostgroup.name" );
				String hostName = getInputFilterWhere ( ifp, "host.name" );
				String hostHost = getInputFilterWhere ( ifp, "host.host" );
				Message.printStatus(2, routine, "hostGroupName=\"" + hostGroupName + "\" hostName=\"" + hostName + "\" host=\"" + hostHost + "\"");

				List<Host> hostList = new ArrayList<>();
				if ( (hostGroupName == null) && (hostName == null) && (hostHost == null) ) {
					// No host name or host was requested:
					// - get all the hosts and query all associated items
					// - can use the global list of hosts
					// - this does not seem to work on the full list so read 25 at a time
					int iChunk = 15;
					int iStart = -iChunk, iEnd = -1;  // Zero index positions in global host to read.
					while ( true ) {
						// Set the indices to get hosts.
						iStart += iChunk;
						iEnd += iChunk;
						if ( iStart >= this.globalHostList.size() ) {
							// No more data to read.
							break;
						}
						if ( iEnd >= this.globalHostList.size() ) {
							iEnd = this.globalHostList.size() - 1;
						}
						// Get a list of Host to read.
						List<Host> hostSubList = new ArrayList<>();
						for ( int i = iStart; i <= iEnd; i++ ) {
							hostSubList.add(this.globalHostList.get(i));
						}
						try {
							Message.printStatus(2, routine, "Reading items for hosts " + iStart + " through " + iEnd + ".");
							List<Item> itemSubList = readItemList ( hostSubList );
							itemList.addAll(itemSubList);
							Message.printStatus(2, routine, "  Read " + itemSubList.size() + " items, now have " + itemList.size() + " total." );
						}
						catch ( Exception e ) {
							Message.printWarning(3, routine, "  Error reading items." );
							Message.printWarning(3, routine, e );
						}
					}
				}
				else if ( hostHost != null ) {
					// Host takes precedence over the group.
					List<String> hostHostList = new ArrayList<>();
					hostHostList.add(hostHost);
					hostList = readHostList ( "host", hostHostList );
					Message.printStatus(2, routine, "Read " + hostList.size() + " hosts for host \"" + hostHost + "\".");
					itemList = readItemList ( hostList );
				}
				else if ( hostName != null ) {
					// Host name takes precedence over the group.
					List<String> hostNameList = new ArrayList<>();
					hostNameList.add(hostName);
					hostList = readHostList ( "name", hostNameList );
					Message.printStatus(2, routine, "Read " + hostList.size() + " hosts for host name \"" + hostName + "\".");
					itemList = readItemList ( hostList );
				}
				else if ( hostGroupName != null ) {
					List<String> hostGroupList = new ArrayList<>();
					hostGroupList.add(hostGroupName);
					hostList = readHostList ( "group", hostGroupList );
					Message.printStatus(2, routine, "Read " + hostList.size() + " hosts for group \"" + hostGroupName + "\".");
					itemList = readItemList ( hostList );
				}
				Message.printStatus(2, routine, "Read " + itemList.size() + " 'item' objects for time series catalog.");

			}

			// If here have matching 'Item' to process into TimeSeriesCatalog.

			List<TimeSeriesCatalog> tscatalogList = new ArrayList<>();
			String dataInterval = "IrregSecond";
			
			// Determine preferred host groups:
			// - to help when there are multiple groups for a host
			// - for example, prefer "Clients/" over other groups
			// - TODO smalers hard-code this for now but add a configuration file property later

			// Loop through the 'Item' instances and create corresponding TimeSeriesCatalog entries.
			for ( Item item : itemList ) {

				TimeSeriesCatalog tscatalog = new TimeSeriesCatalog();

				// Look up the related host from the cached data.
				Host host = Host.lookupHostForId ( this.globalHostList, item.getHostid() );

				tscatalog.setLocId ( host.getHost() );
				// Data source is set below with host group data.
				tscatalog.setDataType ( item.getName() );
				tscatalog.setDataInterval ( dataInterval );
				tscatalog.setDataUnits ( item.getUnits() );

				// Host group data, listed alphabetically.
				if ( host != null ) {
					// Look up the host group:
					// - Zabbix allows a host to be in multiple groups
					// - however, for identification, use the first group
					HostGroup hostGroup = host.lookupPreferredHostGroup ( this.preferredHostGroupNames );
					if ( hostGroup != null ) {
						String hostGroupName = hostGroup.getName();
						tscatalog.setDataSource ( hostGroupName );
						tscatalog.setHostGroupId ( hostGroup.getGroupid() );
						tscatalog.setHostGroupName ( hostGroup.getName() );
						// Set the additional group names as information, but are not used in TSID.
						List<HostGroup> hostGroups = host.getGroups();
						if ( (hostGroups != null) && (hostGroups.size() > 1) ) {
							StringBuilder b = new StringBuilder();
							for ( HostGroup hostGroup2 : hostGroups ) {
								if ( !hostGroup2.getName().equals(hostGroupName) ) {
									// Name does not match so add.
									if ( b.length() > 0 ) {
										b.append ( "," );
									}
									b.append ( hostGroup2.getName() );
								}
							}
							tscatalog.setHostGroupName2(b.toString());
						}
					}

					// Host data, listed alphabetically.
					tscatalog.setHost ( host.getHost() );
					tscatalog.setHostDescription ( host.getDescription() );
					tscatalog.setHostId ( host.getHostid() );
					tscatalog.setHostName ( host.getName() );
				}

				// Item data, listed alphabetically.
				tscatalog.setItemDelay ( item.getDelay() );
				tscatalog.setItemHistory ( item.getHistory() );
				tscatalog.setItemId ( item.getItemid() );
				tscatalog.setItemKey ( item.getKey() );
				tscatalog.setItemName ( item.getName() );
				tscatalog.setItemStatus ( item.getStatus() );
				tscatalog.setItemTemplateId ( item.getTemplateid() );
				tscatalog.setItemTrends ( item.getTrends() );
				ItemType itemType = ItemType.valueOfIgnoreCase(item.getType());
				if ( itemType == null ) {
					// Set to the number (as a String).
					tscatalog.setItemType ( item.getType() );
				}
				else {
					// Set to the name.
					tscatalog.setItemType ( itemType.getDisplayName() );
					// Also set the number.
					tscatalog.setItemTypeNum ( itemType.getCode() );
				}
				tscatalog.setItemUnits ( item.getUnits() );
				ValueType itemValueType = ValueType.valueOfIgnoreCase(item.getValueType());
				if ( itemValueType == null ) {
					// Set to the number (as a String).
					tscatalog.setItemValueType ( item.getValueType() );
				}
				else {
					// Set to the name.
					tscatalog.setItemValueType ( itemValueType.getDisplayName() );
					// Also set the number.
					tscatalog.setItemValueTypeNum ( itemValueType.getCode() );
				}
				
				// Populate derived data that is looked up from other objects:
				// - for example, item template 'name' from 'templateid'
				// - TODO smalers 2023-05-25 disable for now since it does not work
				// tscatalog.setDerivedData ( this.globalTemplateList );

				tscatalogList.add(tscatalog);
			}

			Message.printStatus(2, routine, "Read " + tscatalogList.size() + " Zabbix history time series catalog using 'item' list.");
			return tscatalogList;
		}
	}

    /**
     * Read time series metadata, which results in a query that joins station, station_type, point, point_class, and point_type.
	 * @param dataTypeReq Requested data type (e.g., item.key) or "*" to read all data types, or null to use default of "*".
	 * @param dataIntervalReq Requested data interval (e.g., "IrregSecond") or "*" to read all intervals, or null to use default of "*".
	 * @param ifp input filter panel with "where" conditions
     */
    List<TimeSeriesCatalog> readTimeSeriesMeta ( String dataTypeReq, String dataIntervalReq, InputFilter_JPanel ifp ) {
    	// Remove note from data type.
	   	int pos = dataTypeReq.indexOf(" - ");
	   	if ( pos > 0 ) {
		   	dataTypeReq = dataTypeReq.substring(0, pos);
	   	}
	   	pos = dataIntervalReq.indexOf(" - ");
	   	if ( pos > 0 ) {
		   	dataIntervalReq = dataIntervalReq.substring(0, pos).trim();
	   	}
		String tsid = null;
	   	// By default all time series are included in the catalog:
	   	// - the filter panel options can be used to constrain
	    return readTimeSeriesCatalog ( tsid, dataTypeReq, dataIntervalReq, ifp );
	}

    /**
     * Read the trend list from the web service.
     * See (current): https://www.zabbix.com/documentation/current/en/manual/api/reference/trend/get
     * See (5.4):  https://www.zabbix.com/documentation/5.4/en/manual/api/reference/trend/get
     * @param itemid itemid to match
     * @param timeFrom timestamp to start read, UNIX epoch time seconds
     * @param timeTill timestamp to end read, UNIX epoch time seconds
     * @return the trend list, may be an empty list if a problem or no data
     */
    private List<Trend> readTrendList ( String itemid, long timeFrom, long timeTill) {
		String routine = getClass().getSimpleName() + ".readTrendList";
		String requestUrl = getServiceRootURI().toString();
		List<String> itemidList = new ArrayList<>();
		itemidList.add(itemid);
		// Seems to require 'output'.
		String requestData =
			"{"
				+ "\"jsonrpc\": \"2.0\","
				+ "\"method\": \"trend.get\","
				+ "\"params\": {"
					+ "\"output\": \"extend\","
					+ "\"sortfield\": \"clock\","
					+ "\"sortorder\": \"ASC\""
					+ getParamTimeFrom(true, timeFrom)
					+ getParamTimeTill(true, timeTill)
					+ getParamItemIds(true, itemidList)
				+ "},"
				+ "\"id\": 1"
				+ getAuthJSON()
    		+ "}";
		Message.printStatus(2, routine, "Request data = " + requestData);
		String dataElement = "result";  // 'result' contains an array of the objects.
		try {
			String trendJson = JacksonToolkit.getInstance().getJsonFromWebServiceUrl ( requestUrl, getApiToken(), requestData, dataElement );
			List<Trend> trend = JacksonToolkit.getInstance().getObjectMapper().readValue(trendJson, new TypeReference<List<Trend>>(){});
			return trend;
		}
		catch ( Exception e ) {
			Message.printWarning(3,routine,e);
			return new ArrayList<Trend>();
		}
    }

    /**
     * Read the user login from the web service, used with older (Zabbix version < 6.0?).
     * See: https://www.zabbix.com/documentation/5.4/en/manual/api
	 * @param login Zabbix login
	 * @param password Zabbix password
     * @return the UserLogin object from authentication, or null if an error
     */
    private UserLogin readUserLogin ( String login, String password ) {
		String routine = getClass().getSimpleName() + ".readUserLogin";
		String requestUrl = getServiceRootURI().toString();
		String requestData =
			"{"
				+ "\"jsonrpc\": \"2.0\","
				+ "\"method\": \"user.login\","
				+ "\"params\": {"
					+ "\"user\": \"" + login + "\","
					+ "\"password\": \"" + password + "\""
				+ "},"
				+ "\"id\": 1,"
				+ "\"auth\": null"
    		+ "}";
		String dataElement = null;  // Version is at the root of the response.
		try {
			String userLoginJson = JacksonToolkit.getInstance().getJsonFromWebServiceUrl ( requestUrl, getApiToken(), requestData, dataElement );
			if ( Message.isDebugOn ) {
				Message.printDebug(1, routine, "User login JSON=" + userLoginJson);
			}
			UserLogin userLogin = JacksonToolkit.getInstance().getObjectMapper().readValue(userLoginJson, new TypeReference<UserLogin>(){});
			return userLogin;
		}
		catch ( Exception e ) {
			Message.printWarning(3,routine,e);
			return null;
		}
    }

    /**
     * Read the version from the web service.
     * See: https://www.zabbix.com/documentation/current/en/manual/api/reference/apiinfo/version
     */
    private String readVersion () {
		String routine = getClass().getSimpleName() + ".readVersion";
		String requestUrl = getServiceRootURI().toString();
		String requestData =
			"{"
				+ "\"jsonrpc\": \"2.0\","
				+ "\"method\": \"apiinfo.version\","
				+ "\"params\": [],"
				+ "\"id\": 1"
    		+ "}";
		String dataElement = null;  // Version is at the root of the response.
		try {
			String versionJson = JacksonToolkit.getInstance().getJsonFromWebServiceUrl ( requestUrl, getApiToken(), requestData, dataElement );
			if ( Message.isDebugOn ) {
				Message.printDebug(1, routine, "Version JSON=" + versionJson);
			}
			ApiVersion version = JacksonToolkit.getInstance().getObjectMapper().readValue(versionJson, new TypeReference<ApiVersion>(){});
			return version.getVersion();
		}
		catch ( Exception e ) {
			Message.printWarning(3,routine,e);
			return null;
		}
    }

    /**
     * Set the time series properties from the TimeSeriesCatalog.
     * @param ts time series to update
     * @param tscatalog time series catalog to supply properties
     */
    private void setTimeSeriesProperties ( TS ts, TimeSeriesCatalog tscatalog ) {
    	// Set all the useful Zabbix properties that are known for the time series.

    	// Host group data, listed alphabetically.
	   	ts.setProperty("hostgroup.hostgroupid", tscatalog.getHostGroupId() );
	   	ts.setProperty("hostgroup.name", tscatalog.getHostGroupName() );

	   	// Host data, listed alphabetically.
	   	//private Double stationLatitude = null;
	   	//private Double stationLongitude = null;
	   	ts.setProperty("host.description", tscatalog.getHostDescription() );
	   	ts.setProperty("host.hostid", tscatalog.getHostId() );
	   	ts.setProperty("host.name", tscatalog.getHostName() );

	   	// Item data, listed alphabetically.
	   	ts.setProperty("item.delay", tscatalog.getItemDelay() );
	   	ts.setProperty("item.history", tscatalog.getItemHistory() );
	   	ts.setProperty("item.itemid", tscatalog.getItemId() );
	   	ts.setProperty("item.key", tscatalog.getItemKey() );
	   	ts.setProperty("item.name", tscatalog.getItemName() );
	   	ts.setProperty("item.units", tscatalog.getItemUnits() );
	   	ts.setProperty("item.type", tscatalog.getItemType() );
	   	ts.setProperty("item.typenum", tscatalog.getItemTypeNum() );
	   	ts.setProperty("item.valuetype", tscatalog.getItemValueType() );
	   	ts.setProperty("item.valuetypenum", tscatalog.getItemValueTypeNum() );
    }

}