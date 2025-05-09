# TSTool / Zabbix Data Web Services Plugin / Release Notes #

Release notes are available for the core TSTool product and plugin.
The core software and plugins are maintained separately and may be updated at different times.

*   [TSTool core product release notes](http://opencdss.state.co.us/tstool/latest/doc-user/appendix-release-notes/release-notes/)
*   [TSTool Version Compatibility](#tstool-version-compatibility)
*   [Release Note Details](#release-note-details)

----

## TSTool Version Compatibility ##

The following table lists TSTool and plugin software version compatibility.

**<p style="text-align: center;">
TSTool and Plugin Version Compatibility
</p>**

| **Plugin Version** | **Required TSTool Version** | **Comments** |
| -- | -- | -- |
| 2.0.0 | >=  15.0.0 | TSTool and plugin updated to Java 11, new plugin manager. |
| 1.0.3 | >= 14.8.5 | |
| 1.0.1 | >= 14.8.2 | |
| 1.0.0 | >= 14.8.1 | |

## Release Note Details ##

*   [Version 2.0.0](#version-200)
*   [Version 1.0.3](#version-103)
*   [Version 1.0.2](#version-102)
*   [Version 1.0.1](#version-101)
*   [Version 1.0.0](#version-100)

----------

## Version 2.0.0 ##

**Major release to use Java 11.**

*   ![change](change.png) Update the plugin to use Java 11:
    +   The Java version is consistent with TSTool 15.0.0.
    *   The plugin installation now uses a version folder,
        which allows multiple versions of the plugin to be installed at the same time,
        for use with different versions of TSTool.

## Version 1.0.3 ##

**Maintenance release - fix bug when viewing documentation.**

*   ![change](change.png) [1.0.3] Maintenance release:
    +   **This version requires TSTool 14.8.5 to enable web service request timeouts.**
*   ![bug](bug.png) [1.0.3] The online documentation has been updated:
    +   Help documentation for the 
        [`ReadZabbix`](../command-ref/ReadZabbix/ReadZabbix.md) command
        was not displaying from the command editor due to a bad URL.
        The documentation can be viewed from the plugin download website.
    +   TSTool has been updated to allow documentation for a plugin version to be displayed,
        and if not found the latest documentation is displayed.
*   ![change](change.png) [1.0.3] Zabbix web service requests now default to a timeout of 2 minutes,
    meaning that if a connection is not established or data are not made available to read in 2 minutes,
    an error will result.
    This has been implemented to prevent TSTool from hanging if Zabbix web services are unavailable,
    for example during system maintenance.

## Version 1.0.2 ##

**Maintenance release - fix bugs related to identifying and reading time series.**

*   ![bug](bug.png) [1.0.2] Enhance code that reads Zabbix time series:
    +   Hosts that belong to more than one host group were previously using the first group as the data source
        in time series identifiers, which can lead to confusion.  This is still the default.
    +   The datastore configuration property `PreferredHostGroupName` has been added and
        can be set to comma-separated list of patterns using `*` as a wildcard.
        This ensures that the group name that is used for time series identifiers is appropriate.
    +   Fix the query filter for `host` (was ignored).
*   ![bug](bug.png) [1.0.2] Update the [`ReadZabbix`](../command-ref/ReadZabbix/ReadZabbix.md) command:
    +   Fix bug where the period end for reading time series was not being handled correctly (was set to the start).

## Version 1.0.1 ##

**Maintenance release - fix bugs found in the initial release.**

*   ![change](change.png) [1.0.1] Initial maintenance release:
    +   **This version requires TSTool 14.8.2 to use multiple time series raster graphs.**
    +   Add examples to test data for raster graphs (heat maps) of key system performance data (disk, memory, CPU utilization).
*   ![change](change.png) [1.0.1] Update the time series catalog (list):
    +   Item type and item value type have separate text and number columns to facilitate automated processing.
    +   History data that contain text values are no longer with corresponding trends time series (trends only contain numerical data).
*   ![bug](bug.png) [1.0.1] Update the [`ReadZabbix`](../command-ref/ReadZabbix/ReadZabbix.md) command:
    +   Fix so that the `TimeZone` command parameter is recognized (previously was ignored).
    +   Fix so that the `InputStart` and `InputEnd` command parameters are recognized
        (previously were ignored due to a bug in the web service request).
    +   Fix so that time series properties are set using time series catalog data.
    +   Add the `TextValue` command parameter to provide a numerical value for history time series that contain text.
        The text is set as the data flag and the numerical value is used for graphs.

## Version 1.0.0 ##

**Feature release - initial production release.**

*   ![new](new.png) [1.0.0] Initial production release:
    +   **This version requires TSTool 14.8.1.**
    +   Main TSTool window includes browsing features to list Zabbix time series.
    +   [TSID for Zabbix](../command-ref/TSID/TSID.md) are recognized to read time series with default parameters.
    +   The [`ReadZabbix`](../command-ref/ReadZabbix/ReadZabbix.md) command is provided to automate
        reading 1+ time series.
    +   Documentation is available for the [Zabbix datastore](../datastore-ref/Zabbix/Zabbix.md).
