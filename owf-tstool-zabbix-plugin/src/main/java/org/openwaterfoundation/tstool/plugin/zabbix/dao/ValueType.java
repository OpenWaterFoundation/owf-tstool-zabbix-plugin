// ValueType - enumeration of API value types

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

/**
Value types.
*/
public enum ValueType {
    /**
     * Floating point number
     */
    FLOAT ("Float", 0),

    /**
     * Character.
     */
    CHARACTER("Character", 1),

    /**
     * LOG.
     */
    LOG("Log", 2),

    /**
     * Numeric unsigned (integer).
     */
    INTEGER("Integer", 3),

    /**
     * Text.
     */
    TEXT("Text", 4),

    /**
     * Unknown.
     */
    UNKNOWN("Unknown", 99);

    /**
     * The string name that should be displayed.
     */
    private final String displayName;
    
    /**
     * The numeric code for the type.
     */
    private final int code;

    /**
     * Construct an enumeration value.
     * @param displayName name that should be displayed in choices, etc.
     * @param code integer type value.
     */
    private ValueType(String displayName, int code) {
        this.displayName = displayName;
        this.code = code;
    }

    /**
     * Equals method to prevent common programming error of using the equals method instead of ==.
     * @param value the display name or code (as a string) to check
     */
    public boolean equals ( String value ) {
        if ( displayName.equalsIgnoreCase(this.displayName) || displayName.equals("" + this.code)) {
            return true;
        }
        else {
            return false;
        }
    }
    
    /**
     * Return the code.
     * @return the code.
     */
    public int getCode () {
    	return this.code;
    }

    /**
     * Return the display name.
     * @return the display name.
     */
    public String getDisplayName () {
    	return this.displayName;
    }
    
    /**
     * Indicate whether the value type is numeric (float or integer),
     * which will allow storage in a number.
     * @return true if FLOAT or INTEGER and false otherwise.
     */
    public boolean isNumeric () {
    	if ( (this == FLOAT)  || (this == INTEGER) ) {
    		return true;
    	}
    	else {
    		return false;
    	}
    }

    /**
     * Return the display name for the line style type.
     * This is usually the same as the value but using appropriate mixed case.
     * @return the display name.
     */
    @Override
    public String toString() {
        return displayName;
    }

	/**
	 * Return the enumeration value given a string name (case-independent).
	 * @return the enumeration value given a string name (case-independent), or UNKNOWN if not matched.
	 */
	public static ValueType valueOfIgnoreCase(String name) {
	    if ( name == null ) {
	        return UNKNOWN;
	    }
	    // Currently supported values.
	    for ( ValueType t : values() ) {
	        if ( name.equalsIgnoreCase(t.toString()) || name.equals("" + t.getCode() ) ) {
	            return t;
	        }
	    }
	    return UNKNOWN;
	}
}