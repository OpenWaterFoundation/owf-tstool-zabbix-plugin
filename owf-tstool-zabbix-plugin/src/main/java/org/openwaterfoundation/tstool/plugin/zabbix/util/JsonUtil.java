// JsonUtil - class for JSON utility methods

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

NoticeEnd
*/

package org.openwaterfoundation.tstool.plugin.zabbix.util;

import RTi.Util.Message.Message;

/**
 * General JSON utilities.
 */
public class JsonUtil {

	/**
	 * Extract an object from a larger JSON string.
	 * This can be used, for example, to extract a substring from a larger collection object.
	 * This class is not intended to replace other JSON parsing code but is useful to split apart large JSON files
	 * so that parts can be parsed individually.
	 * This does not parse the JSON into nodes in order to minimize memory and time use.
	 * @param jsonString the full JSON string to process
	 * @param objectName the name of the object to extract
	 * @param includeName whether to include the name in returned result (true) or not (false)
	 * @return the individual object string or null if the object does not exist in the input string.
	 */
	public static String extractObjectString ( String jsonString, String objectName, boolean includeName ) {
		String routine = JsonUtil.class.getSimpleName() + ".extractObjectString";
		// Find the object in the string:
		// - expected to be surrounded by double quotes
		// - the first instance is processed
		int posName = jsonString.indexOf ( "\"" + objectName + "\"" );
		int len = jsonString.length();
		if ( posName < 0 ) {
			if ( Message.isDebugOn ) {
				Message.printStatus(2, routine, "Did not find requested name \"" + objectName + "\" in JSON - returning null.");
			}
			return null;
		}
		// Have a the location of the object name:
		// - find the colon after the name
		int posStart = jsonString.indexOf ( ":", posName);
		if ( posStart < 0 ) {
			// Bad JSON.
			if ( Message.isDebugOn ) {
				Message.printStatus(2, routine, "Did not find : after name \"" + objectName + "\" in JSON - returning null.");
			}
		}
		// Determine the object-bounding character, either [ or {.
		boolean objectIsArray = false;
		boolean bracketFound = false;
		boolean inQuotedString = false;
		char c;
		int i;
		posStart = posStart + 1; // Increment past colon.
		for ( i = posStart; i < len; i++ ) {
			c = jsonString.charAt(i);
			if ( c == '\\' ) {
				// Swallow the next character, could be a \", \[, or \{ and don't want to detect quoted string.
				++i;
				continue;
			}
			else if ( (c == '[') && !inQuotedString ) {
				objectIsArray = true;
				bracketFound = true;
				break;
			}
			else if ( (c == '{') && !inQuotedString ) {
				objectIsArray = false;
				bracketFound = true;
				break;
			}
			else if ( c == '"' ) {
				// Indicates a start or end of a quoted string.
				if ( inQuotedString ) {
					inQuotedString = false;
				}
				else {
					inQuotedString = true;
				}
			}
		}
		if ( !bracketFound ) {
			// Something wrong with the JSON.
			if ( Message.isDebugOn ) {
				Message.printStatus(2, routine, "Did not find [ or { after name \"" + objectName + "\" in JSON - returning null.");
			}
			return null;
		}
		// Search for the matching closing bracket, starting from previous character in the string:
		// - increment bracket count until the bracket open and close counts are equal
		int bracketOpenCount = 1;
		int bracketCloseCount = 0;
		int posEnd = -1;
		inQuotedString = false;
		for ( i = (i + 1); i < len; i++ ) {
			c = jsonString.charAt(i);
			if ( c == '\\' ) {
				// Swallow the next character, could be a \", \[, or \{ and don't want to detect quoted string.
				++i;
				continue;
			}
			else if ( !inQuotedString && (c == '[') && objectIsArray ) {
				++bracketOpenCount;
			}
			else if ( !inQuotedString && (c == ']') && objectIsArray ) {
				++bracketCloseCount;
			}
			else if ( !inQuotedString && (c == '{') && !objectIsArray ) {
				++bracketOpenCount;
			}
			else if ( !inQuotedString && (c == '}') && !objectIsArray ) {
				++bracketCloseCount;
			}
			else if ( c == '"' ) {
				// Indicates a start or end of a quoted string.
				if ( inQuotedString ) {
					inQuotedString = false;
				}
				else {
					inQuotedString = true;
				}
			}
			if ( bracketOpenCount == bracketCloseCount ) {
				// Have matched the ending bracket.
				posEnd = i;
				break;
			}
		}
		if ( posEnd > 0 ) {
			// Found the closing bracket.
			if ( includeName ) {
				// Include the object name in the result.
				return jsonString.substring(posName, (posEnd + 1)).trim();
			}
			else {
				// Include the data after the object name in the result.
				return jsonString.substring(posStart, (posEnd + 1)).trim();
			}
		}
		else {
			// Did not find the closing bracket.
			if ( Message.isDebugOn ) {
				if ( objectIsArray ) {
					Message.printStatus(2, routine, "Did not find closing ] after name \"" + objectName + "\" in JSON - returning null.");
				}
				else {
					Message.printStatus(2, routine, "Did not find closing } after name \"" + objectName + "\" in JSON - returning null.");
				}
			}
			return null;
		}
	}

}