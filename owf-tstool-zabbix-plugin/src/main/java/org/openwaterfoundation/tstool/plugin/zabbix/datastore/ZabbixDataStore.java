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
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.openwaterfoundation.tstool.plugin.zabbix.PluginMeta;
import org.openwaterfoundation.tstool.plugin.zabbix.dao.ApiAuthType;
import org.openwaterfoundation.tstool.plugin.zabbix.dao.ApiVersion;
import org.openwaterfoundation.tstool.plugin.zabbix.dao.Host;
import org.openwaterfoundation.tstool.plugin.zabbix.dao.HostGroup;
import org.openwaterfoundation.tstool.plugin.zabbix.dao.Item;
import org.openwaterfoundation.tstool.plugin.zabbix.dao.TimeSeriesCatalog;
import org.openwaterfoundation.tstool.plugin.zabbix.dao.UserLogin;
import org.openwaterfoundation.tstool.plugin.zabbix.dto.JacksonToolkit;
import org.openwaterfoundation.tstool.plugin.zabbix.ui.Zabbix_TimeSeries_CellRenderer;
import org.openwaterfoundation.tstool.plugin.zabbix.ui.Zabbix_TimeSeries_InputFilter_JPanel;
import org.openwaterfoundation.tstool.plugin.zabbix.ui.Zabbix_TimeSeries_TableModel;

import com.fasterxml.jackson.core.type.TypeReference;

import RTi.TS.TS;
import RTi.TS.TSIdent;
import RTi.Util.GUI.InputFilter;
import RTi.Util.GUI.InputFilter_JPanel;
import RTi.Util.GUI.JWorksheet_AbstractExcelCellRenderer;
import RTi.Util.GUI.JWorksheet_AbstractRowTableModel;
import RTi.Util.IO.PropList;
import RTi.Util.IO.RequirementCheck;
import RTi.Util.Message.Message;
import RTi.Util.String.StringUtil;
import RTi.Util.Time.DateTime;
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
	 * Global time series catalog, used to streamline creating lists for UI choices.
	 */
	private List<TimeSeriesCatalog> globalTscatalogList = new ArrayList<>();

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
	 */
	private List<Item> globalItemList = new ArrayList<>();

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
	 * Get the "params" host filter JSON for a list of Host, no trailing comma.
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
	private String getParamHostFilter ( boolean addLeadingComma, String propertyName, List<String> propertyValueList ) {
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
	 * Get the "params" JSON for a list of Host, no trailing comma.
	 * @param hostList list of Host to include
	 */
	private String getParamHostIds ( List<Host> hostList ) {
		if ( (hostList != null) && (hostList.size() > 0) ) {
			StringBuilder b = new StringBuilder ( "\"hostids\": [");
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
	 * Return the list of time series catalog.
	 * @param readData if false, return the global cached data, if true read the data and reset in he cache
	 */
	public List<TimeSeriesCatalog> getTimeSeriesCatalog(boolean readData) {
		if ( readData ) {
			String tsid = null;
			String dataTypeReq = null;
			String dataIntervalReq = null;
    		InputFilter_JPanel ifp = null;
			this.globalTscatalogList = readTimeSeriesCatalog(tsid, dataTypeReq, dataIntervalReq, ifp );
		}
		return this.globalTscatalogList;
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
	 * Interval strings match TSTool conventions such as NewTimeSeries command, which uses "1Hour" rather than "1hour".
	 * This should result from calls like:  TimeInterval.getName(TimeInterval.HOUR, 0)
	 * @param dataType data type string to filter the list of data intervals.
	 * If null, blank, or "*" the data type is not considered when determining the list of data intervals.
	 * @param includeWildcards if true, include "*" wildcard.
	 */
	public List<String> getTimeSeriesDataIntervalStrings(String dataType, boolean includeWildcards ) {
		//String routine = getClass().getSimpleName() + ".getTimeSeriesDataIntervalStrings";
		List<String> dataIntervals = new ArrayList<>();

		// Currently only IrregSecond since not sure how timesteps align and use * wildcards.
		dataIntervals.add("IrregSecond");

		// Sort the intervals:
		// - TODO smalers need to sort by time
		Collections.sort(dataIntervals,String.CASE_INSENSITIVE_ORDER);

		/*
		if ( includeWildcards ) {
			// Always allow querying list of time series for all intervals:
			// - always add so that people can get a full list
			// - adding at top makes it easy to explore data without having to scroll to the end

			dataIntervals.add("*");
			if ( dataIntervals.size() > 1 ) {
				// Also add at the beginning to simplify selections:
				// - could check for a small number like 5 but there should always be a few
				dataIntervals.add(0,"*");
			}
		}
		*/

		return dataIntervals;
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
	 * These strings are the same as the parameter type list 'parametertype_name'.
	 * @param dataInterval the data interval to filter data types
	 * @param includeWildcards whether "*" should be included at the start and end of the list
	 */
	public List<String> getTimeSeriesDataTypeStrings(String dataInterval, boolean includeWildcards ) {
		//String routine = getClass().getSimpleName() + ".getTimeSeriesDataTypeStrings";

		// Currently the data types are a static list, not determined from an API call.
		List<String> dataTypes = new ArrayList<>();

		/*
		for ( Variable variable : this.variableList ) {
			dataTypes.add( variable.getName() );
		}

		// Sort the names.
		Collections.sort(dataTypes, String.CASE_INSENSITIVE_ORDER);
		*/

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
   		if ( (locId.indexOf(' ') > 0) || (locId.indexOf('.') > 0) ) {
   			// Surround the location ID (host) with single quotes.
   			locId = "'" + locId + "'";
   		}
    	String source = (String)tableModel.getValueAt(row,tm.COL_DATA_SOURCE);
   		if ( (source.indexOf(' ') > 0) || (source.indexOf('.') > 0) ) {
   			// Surround the source (host group name) with single quotes.
   			source = "'" + source + "'";
   		}
    	String dataType = (String)tableModel.getValueAt(row,tm.COL_DATA_TYPE);
   		if ( (dataType.indexOf(' ') > 0) || (dataType.indexOf('.') > 0) ) {
   			// Surround the data type (item name) with single quotes.
   			dataType = "'" + dataType + "'";
   		}
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
			Message.printStatus(2, routine, "Initialized " + this.globalHostGroupList.size() + " host groups." );
		}
		catch ( Exception e ) {
			Message.printWarning(3, routine, "Error initializing global host group list (" + e + ")");
			Message.printWarning(3, routine, e );
		}

		// Read the hosts.
		
		try {
			this.globalHostList = readHostList();
			Message.printStatus(2, routine, "Initialized " + this.globalHostList.size() + " hosts." );
		}
		catch ( Exception e ) {
			Message.printWarning(3, routine, "Error initializing global host list (" + e + ")");
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

		// The time series catalog COULD be used more throughout TSTool, such as when reading time series.
		// However, the initial implementation of readTimeSeries reads the list each time.
		// The cached list is used to create choices for the UI in order to ensure fast performance.
		// Therefore the slowdown is only at TSTool startup.
		try {
			String tsid = null;
    		String dataTypeReq = null;
    		String dataIntervalReq = null;
    		InputFilter_JPanel ifp = null;
    		// Read the catalog for all time series.
			this.globalTscatalogList = readTimeSeriesCatalog(tsid, dataTypeReq, dataIntervalReq, ifp );
			Message.printStatus(2, routine, "Read " + this.globalTscatalogList.size() + " time series catalog." );
			
			// Loop through and create the lists of location ID and time series short name used in the ReadZabbix command editor.
			
			/*
			String stationNo;
			String tsShortName;
			this.locIdList = new ArrayList<>();
			this.tsShortNameList = new ArrayList<>();
			boolean found;
			for ( TimeSeriesCatalog tscatalog : this.tscatalogList ) {
				stationNo = tscatalog.getStationNo();
				tsShortName = tscatalog.getTsShortName();
				
				found = false;
				for ( String stationNo2 : this.locIdList ) {
					if ( stationNo2.equals(stationNo) ) {
						found = true;
						break;
					}
				}
				if ( !found ) {
					this.locIdList.add(stationNo);
				}

				found = false;
				for ( String tsShortName2 : this.tsShortNameList ) {
					if ( tsShortName2.equals(tsShortName) ) {
						found = true;
						break;
					}
				}
				if ( !found ) {
					this.tsShortNameList.add(tsShortName);
				}
			}

			// Sort the lists.
			Collections.sort(this.locIdList,String.CASE_INSENSITIVE_ORDER);
			Collections.sort(this.tsShortNameList,String.CASE_INSENSITIVE_ORDER);
			*/
		}
		catch ( Exception e ) {
			Message.printWarning(3, routine, "Error reading global time series catalog list (" + e + ")");
			Message.printWarning(3, routine, e );
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
					+ getParamHostFilter(true, propertyName, propertyValueList)
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
		String requestData =
			"{"
				+ "\"jsonrpc\": \"2.0\","
				+ "\"method\": \"item.get\","
				+ "\"params\": {"
					+ getParamHostIds(hostList)
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
     * Read a single time series given its time series identifier using default read properties.
     * This is typically called by TSID command, which uses default properties.
     * @param tsid time series identifier.
     * @param readStart start of read, will be set to 'periodStart' service parameter.
     * @param readEnd end of read, will be set to 'periodEnd' service parameter.
     * @return the time series or null if not read
     */
    public TS readTimeSeries ( String tsid, DateTime readStart, DateTime readEnd, boolean readData ) {
    	String routine = getClass().getSimpleName() + ".readTimeSeries";
	/*
    	try {
    		Message.printStatus(2, routine, "Reading time series \"" + tsid + "\".");
    		return readTimeSeries ( tsid, readStart, readEnd, readData, null );
    	}
    	catch ( Exception e ) {
    		// Throw a RuntimeException since the method interface does not include an exception type.
    		Message.printWarning(2, routine, e);
    		throw new RuntimeException ( e );
    	}
    */
    	return null;
    }

    /**
     * Read a single time series given its time series identifier.
     * @param tsidReq requested time series identifier.
     * @param readStart start of read, will be set to 'periodStart' service parameter.
     * @param readEnd end of read, will be set to 'periodEnd' service parameter.
     * @param readProperties additional properties to control the query:
     * <ul>
     * <li> "IrregularInterval" - irregular interval (e.g., "IrregHour" to use instead of TSID interval,
     *      where the TSID intervals corresponds to the web services.</li>
     * <li> "Read24HourAsDay" - string "false" (default) or "true" indicating whether 24Hour interval time series
     *      should be output as 1Day time series.</li>
     * <li> "ReadDayAs24Hour" - string "false" (default) or "true" indicating whether day interval time series
     *      should be output as 24Hour time series.</li>
     * <li> "Debug" - if true, turn on debug for the query</li>
     * </ul>
     * @return the time series or null if not read
     */
    /*
    public TS readTimeSeries ( String tsidReq, DateTime readStart, DateTime readEnd,
    	boolean readData, HashMap<String,Object> readProperties ) throws Exception {

    	TS ts = null;
    	return ts;
    }
    */

	/**
	 * Read time series catalog, which uses the "/getTimeseriesList" web service query.
	 * @param tsid requested time series identifier, called from readTimeSeries() to get metadata,
	 *        if null use the filters to read 1+ time series catalog
	 * @param dataTypeReq Requested data type (e.g., "DischargeRiver") or "*" to read all data types,
	 *        or null to use default of "*".
	 * @param dataIntervalReq Requested data interval (e.g., "IrregSecond") or "*" to read all intervals,
	 *        or null to use default of "*".
	 * @param ifp input filter panel with "where" conditions
	 */
	public List<TimeSeriesCatalog> readTimeSeriesCatalog ( String tsid, String dataTypeReq, String dataIntervalReq, InputFilter_JPanel ifp ) {
		String routine = getClass().getSimpleName() + ".readTimeSeriesCatalog";

		StringBuilder requestUrl = null;
		String requestUrlString = null;

		TSIdent tsident = null;
		// The following are checked below to know when the data type contains a _1, etc.
		String tsidDataTypeReq = null;
		String tsidDataSubTypeReq = null;
		Message.printStatus(2,routine,"Reading time series catalog using:" );
		Message.printStatus(2,routine,"  tsid=\"" + tsid + "\"");
		Message.printStatus(2,routine,"  dataTypeReq=\"" + dataTypeReq + "\"" );
		Message.printStatus(2,routine,"  dataIntervalReq=\"" + dataIntervalReq + "\"");
		if ( ifp == null ) {
			Message.printStatus(2,routine,"  ifp=null");
		}
		else {
			Message.printStatus(2,routine,"  ifp=not null");
		}

		// Determine the Item list, which will each match one time series.
		List<Item> itemList = new ArrayList<>();

		if ( (tsid != null) && !tsid.isEmpty() ) {
			// Form the web service request based on the TSID parts.
			try {
				tsident = TSIdent.parseIdentifier(tsid);
			}
			catch ( Exception e ) {
				throw new RuntimeException("Error parsing the requested time series identifier \"" + tsid + "\"");
			}
			requestUrl = new StringBuilder(
				getServiceRootURI() + "/stations/metadata?" + getApiTokenParameter() + "&complete=1&sensorvars=1");
			// Request the specific station.
			requestUrl.append("&stid=" + tsident.getLocation());
			// Request the main variable:
			// - data type may include the main sensor variable and the numbered variable, separated by a dash
			String dataType = tsident.getType();
			if ( dataType.indexOf("-") > 0 ) {
				// Need to use the first part.
				String [] parts = dataType.split("-");
				dataType = parts[0].trim();
				tsidDataTypeReq = parts[0].trim();
				tsidDataSubTypeReq = parts[1].trim();
			}
			else {
				// Only the main data type is requested.
				tsidDataTypeReq = dataType;
				tsidDataSubTypeReq = null;
			}
			requestUrl.append("&var=" + dataType);
			Message.printStatus(2, routine, "Reading 1 station time series metadata using:" );
			Message.printStatus(2, routine, "  " + requestUrlString);
		}
		else {
			// Reading 1+ time series using the provided filter parameters.
			requestUrl = new StringBuilder(
				getServiceRootURI() + "/stations/metadata?" + getApiTokenParameter() + "&complete=1&sensorvars=1");

			// Add filters for the data type and time step.

			if ( (dataTypeReq != null) && !dataTypeReq.isEmpty() && !dataTypeReq.equals("*") ) {
				try {
					//requestUrl.append ( "&var=" + URLEncoder.encode(dataTypeReq,StandardCharsets.UTF_8.toString()) );
					requestUrl.append ( "&var=" + dataTypeReq );
				}
				catch ( Exception e ) {
					// TODO smalers 2023-01-01 should not happen.
				}
			}

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
			if ( (hostGroupName == null) && (hostName == null) ) {
				// No host name or host was requested:
				// - get all the hosts and query all associated items
				// - can use the global list of hosts
				itemList = readItemList ( this.globalHostList );
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

			// Add query parameters based on the input filter:
			// - this includes list type parameters and specific parameters to match web service values
			// - TODO smalers 2023-05-18 need to enable input filters for item data
			/*
			int numFilterWheres = 0; // Number of filter where clauses that are added.
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
				    	String queryClause = WebUtil.getQueryClauseFromInputFilter(filter,ifp.getOperator(ifg));
				    	if ( Message.isDebugOn ) {
				    		Message.printStatus(2,routine,"Filter group " + ifg + " where is: \"" + queryClause + "\"");
				    	}
				    	if ( queryClause != null ) {
				    		requestUrl.append("&" + queryClause);
				    		++numFilterWheres;
				    	}
	            	}
	        	}
	        }
	        */
			//Message.printStatus(2, routine, "Reading 1+ station time series metadata using:" );
			//Message.printStatus(2, routine, "  " + requestUrlString);
		}

		List<TimeSeriesCatalog> tscatalogList = new ArrayList<>();
		String dataInterval = "IrregSecond";

		// Loop through the 'Item' instances and create corresponding TimeSeriesCatalog entries.
		for ( Item item : itemList ) {

			TimeSeriesCatalog tscatalog = new TimeSeriesCatalog();
			
			// Look up the related host from the cached data.
			Host host = Host.lookupHostForId ( this.globalHostList, item.getHostid() );

			tscatalog.setLocId ( host.getHost() );
			tscatalog.setDataInterval ( dataInterval );
			tscatalog.setDataType ( item.getName() );
			tscatalog.setDataUnits ( item.getUnits() );

			// Host group data, listed alphabetically.
			if ( host != null ) {
				// Look up the host group:
				// - Zabbix allows a host to be in multiple groups
				// - however, for identification, use the first group
				List<HostGroup> hostGroupList = host.getGroups();
				if ( (hostGroupList != null) && (hostGroupList.size() > 0) ) {
					HostGroup hostGroup = hostGroupList.get(0);
					if ( hostGroup != null ) {
						tscatalog.setHostGroupId ( hostGroup.getGroupid() );
						tscatalog.setHostGroupName ( hostGroup.getName() );
					}
				}

				// Host data, listed alphabetically.
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
			tscatalog.setItemUnits ( item.getUnits() );
			tscatalog.setItemValueType ( item.getValueType() );
			
			tscatalogList.add(tscatalog);
		}

		Message.printStatus(2, routine, "Created " + tscatalogList.size() + " Zabbix time series catalog.");
		return tscatalogList;
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
	   	ts.setProperty("hostgroup.name", tscatalog.getHostGroupId() );

	   	// Host data, listed alphabetically.
	   	//private Double stationLatitude = null;
	   	//private Double stationLongitude = null;
	   	ts.setProperty("host.description", tscatalog.getHostGroupId() );
	   	ts.setProperty("host.hostid", tscatalog.getHostGroupId() );
	   	ts.setProperty("host.name", tscatalog.getHostGroupId() );

	   	// Item data, listed alphabetically.
	   	ts.setProperty("item.delay", tscatalog.getHostGroupId() );
	   	ts.setProperty("item.history", tscatalog.getHostGroupId() );
	   	ts.setProperty("item.itemid", tscatalog.getHostGroupId() );
	   	ts.setProperty("item.key", tscatalog.getHostGroupId() );
	   	ts.setProperty("item.name", tscatalog.getHostGroupId() );
	   	ts.setProperty("item.units", tscatalog.getHostGroupId() );
	   	ts.setProperty("item.valuetype", tscatalog.getHostGroupId() );
    }

}