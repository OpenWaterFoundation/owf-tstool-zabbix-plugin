// ItemType - enumeration of API item types

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
Item types.
*/
public enum ItemType {
    /**
     * Zabbix agent.
     */
    ZABBIX_AGENT ("Zabbix agent", 0),

    /**
     * Zabbix trapper.
     */
    ZABBIX_TRAPPER("Zabbix trapper", 1),

    /**
     * Simple check.
     */
    SIMPLE_CHECK("Simple check", 3),

    /**
     * Zabbix internal.
     */
    ZABBIX_INTERNAL("Zabbix internal", 5),

    /**
     * Zabbix agent (active).
     */
    ZABBIX_AGENT_ACTIVE("Zabbix agent (active)", 7),

    /**
     * Web item.
     */
    WEB_ITEM("Web item", 9),

    /**
     * External check.
     */
    EXTERNAL_CHECK("External check", 10),

    /**
     * Database monitor.
     */
    DATABASE_MONITOR("Database monitor", 11),

    /**
     * IPMI agent.
     */
    IPMI_AGENT("IPMI agent", 12),

    /**
     * SSH agent.
     */
    SSH_AGENT("SSH agent", 13),

    /**
     * Telnet agent.
     */
    TELNET_AGENT("Telnet agent", 14),

    /**
     * Calculated.
     */
    CALCULATED("Calculated", 15),

    /**
     * JMX agent.
     */
    JMX_AGENT("JMX agent", 16),

    /**
     * SNMP trap.
     */
    SNMP_TRAP("SNMP trap", 17),

    /**
     * Dependent item.
     */
    DEPENDENT_ITEM("Dependent item", 18),

    /**
     * HTTP agent.
     */
    HTTP_AGENT("HTTP agent", 19),

    /**
     * SNMP agent.
     */
    SNMP_AGENT("SNMP agent", 20),

    /**
     * Script.
     */
    SCRIPT("Script", 21),

    /**
     * Unknown (need to update code).
     */
    UNKNOWN("Uknown", 99);

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
    private ItemType(String displayName, int code) {
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
	public static ItemType valueOfIgnoreCase(String name) {
	    if ( name == null ) {
	        return UNKNOWN;
	    }
	    // Currently supported values.
	    for ( ItemType t : values() ) {
	        if ( name.equalsIgnoreCase(t.toString()) || name.equals("" + t.getCode() ) ) {
	            return t;
	        }
	    }
	    return UNKNOWN;
	}
}