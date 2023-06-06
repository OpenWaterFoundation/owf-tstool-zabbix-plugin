# TSTool / Zabbix Data Web Services Plugin / Release Notes #

*   [Changes in Version 1.0.2](#changes-in-version-102)
*   [Changes in Version 1.0.1](#changes-in-version-101)
*   [Changes in Version 1.0.0](#changes-in-version-100)

----------

## Changes in Version 1.0.2 ##

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

## Changes in Version 1.0.1 ##

**Maintenance release - fix bugs found in the initial release.**

*   ![change](change.png) [1.0.1] Initial maintenance release:
    +   **This version requires TSTool 14.8.2 to use multiple time series raster graphs**.
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

## Changes in Version 1.0.0 ##

**Feature release - initial production release.**

*   ![new](new.png) [1.0.0] Initial production release:
    +   **This version requires TSTool 14.8.1**.
    +   Main TSTool window includes browsing features to list Zabbix time series.
    +   [TSID for Zabbix](../command-ref/TSID/TSID.md) are recognized to read time series with default parameters.
    +   The [`ReadZabbix`](../command-ref/ReadZabbix/ReadZabbix.md) command is provided to automate
        reading 1+ time series.
    +   Documentation is available for the [Zabbix datastore](../datastore-ref/Zabbix/Zabbix.md).
