package org.openwaterfoundation.tstool.plugin.zabbix.util;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.openwaterfoundation.tstool.plugin.zabbix.PluginMeta;

import RTi.Util.Help.HelpViewerUrlFormatter;
import RTi.Util.Message.Message;

/**
 * Singleton to provide a HelpViewerUrlFormatter for plugin documentation.
 */
public class ZabbixHelpViewerUrlFormatter implements HelpViewerUrlFormatter {

	/**
	 * Singleton instance.
	 */
	private static HelpViewerUrlFormatter urlFormatter = null;
	
	/**
	 * Constructor for the singleton instance, private so it can only be called internally.
	 */
	private ZabbixHelpViewerUrlFormatter() {
	}

	/**
 	* Format a URL to display help for a topic.
 	* The document root is taken from the PluginMeta class.
 	* @param group a group (category) to organize items.
 	* For example, the group might be "command".
 	* @param item the specific item for the URL.
 	* For example, the item might be a command name.
 	*/
	public String formatHelpViewerUrl ( String group, String item ) {
		return formatHelpViewerUrl ( group, item, null );
	}

	/**
 	* Format a URL to display help for a topic.
 	* The document root is taken from the PluginMeta class.
 	* @param group a group (category) to organize items. For example, the group might be "command".
 	* @param item the specific item for the URL. For example, the item might be a command name.
 	* @param rootUrl this can be ignored since the URL formatter knows how to get the root URL.
 	*/
	public String formatHelpViewerUrl ( String group, String item, String rootUrl ) {
		String routine = getClass().getSimpleName() + ".formatHelpViewerUrl";
		// The location of the documentation is relative to root URI on the web.
		String docRootUrl = PluginMeta.getDocumentationRootUrl();
   		String version = PluginMeta.getSemanticVersion();
    	List<String> docRootUrlList = new ArrayList<>();
    	if ( docRootUrl != null ) {
    		// First replace "latest" with the software version so that specific version is shown.
    		String docRootUrlVersion = docRootUrl.replace("latest", version);
    		docRootUrlList.add(docRootUrlVersion);
    		if ( !docRootUrlVersion.equals(docRootUrl) ) {
    			// Also add the URL with "latest".
    			docRootUrlList.add(docRootUrl);
    		}
    	}
    	if ( (rootUrl != null) && !rootUrl.isEmpty() ) {
    		// Add the URLS for the specific root URL.
    		// First replace "latest" with the software version so that specific version is shown.
    		String docRootUrlVersion = rootUrl.replace("latest", version);
    		docRootUrlList.add(docRootUrlVersion);
    		if ( !docRootUrlVersion.equals(rootUrl) ) {
    			// Add the URL with "latest".
    			docRootUrlList.add(rootUrl);
    		}
    	}
    	if ( (docRootUrl == null) || docRootUrl.isEmpty() ) {
    		Message.printWarning(2, "",
    			"Unable to determine documentation for group \"" + group + "\" and item \"" +
    			item + "\" - no TSTool.UserDocumenationUri configuration property defined." );
    	}
    	else {
    		int failCount = 0;
    		int [] responseCode = new int[docRootUrlList.size()];
    		int i = -1;
    		for ( String urlString : docRootUrlList ) {
    			Message.printStatus(2, routine, "URL is " + urlString );
    			// Initialize response code to -1 which means unchecked.
    			++i;
    			responseCode[i] = -1;
	    		// Make sure the URL has a slash at end.
    			if ( (urlString != null) && !urlString.isEmpty() ) {
		    		String docUrl = null;
		    		if ( !urlString.endsWith("/") ) {
		    			urlString += "/";
		    		}
			    	// Generic requests by group, such as for command reference from editors.
			    	if ( group.equalsIgnoreCase("command") ) {
			    		docUrl = urlString + "command-ref/" + item + "/" + item + "/";
			    	}
			    	if ( docUrl != null ) {
			    		// Display using the default application for the file extension.
			    		Message.printStatus(2, routine, "Trying to connect to documentation \"" + docUrl + "\"" );
			    		// The Desktop.browse() method will always open, even if the page does not exist,
			    		// and it won't return the HTTP error code in this case.
			    		// Therefore, do a check to see if the URI is available before opening in a browser.
			    		URL url = null;
			    		try {
			    			url = new URL(docUrl);
			    			HttpURLConnection huc = (HttpURLConnection)url.openConnection();
			    			huc.connect();
			    			responseCode[i] = huc.getResponseCode();
			    		}
			    		catch ( MalformedURLException e ) {
			    			Message.printWarning(3, "", "Unable to display documentation at \"" + docUrl + "\" - malformed URL." );
			    		}
			    		catch ( IOException e ) {
			    			Message.printWarning(3, "", "Unable to display documentation at \"" + docUrl + "\" - IOException (" + e + ")." );
			    		}
			    		catch ( Exception e ) {
			    			Message.printWarning(3, "", "Unable to display documentation at \"" + docUrl + "\" - Exception (" + e + ")." );
			    		}
			    		finally {
			    			// Any cleanup?
			    		}
			    		if ( responseCode[i] == 200 ) {
			    			// Looks like a valid URI to display.
			    			Message.printStatus(2, routine, "  Document for link is valid.");
			    			return docUrl;
			    		}
			    		else {
			    			Message.printStatus(2, routine, "  Document for link is not available.");
			    			++failCount;
			    		}
			    	}
			    	else {
			    		// URL could not be determined.
			    		++failCount;
			    	}
    			}
    		}
        	if ( failCount == docRootUrlList.size() ) {
        		// Log the a message - show a visible dialog in calling code.
        		Message.printWarning(2, "",
        			"Unable to determine documentation for group \"" + group + "\" and item \"" +
        			item + "\" - all URLs that were tried return error code." );
        	}
    	}
		return null;
	}

	/**
	 * Get the singleton instance.
	 */
	public static HelpViewerUrlFormatter getInstance () {
		if ( urlFormatter == null ) {
			// Create the singleton the first time.
			urlFormatter = new ZabbixHelpViewerUrlFormatter();
		}
		
		// Return the singleton.
		return urlFormatter;
	}
}
