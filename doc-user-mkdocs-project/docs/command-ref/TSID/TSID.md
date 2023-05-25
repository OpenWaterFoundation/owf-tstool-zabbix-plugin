# TSTool / Command / TSID for Zabbix #

* [Overview](#overview)
* [Command Editor](#command-editor)
* [Command Syntax](#command-syntax)
* [Examples](#examples)
* [Troubleshooting](#troubleshooting)
* [See Also](#see-also)

-------------------------

## Overview ##

The TSID command for Zabbix causes a single time series to be read from Zabbix web services using default parameters.
A TSID command is created by copying a time series from the ***Time Series List*** in the main TSTool interface
to the ***Commands*** area.
TSID commands can also be created by editing the command file with a text editor.

See the [Zabbix Datastore Appendix](../../datastore-ref/Zabbix/Zabbix.md) for information about TSID syntax.

See also the [`ReadZabbix`](../ReadZabbix/ReadZabbix.md) command,
which reads one or more time series and provides parameters for control over how data are read.

The TSTool Zabbix plugin automatically manipulates time series timestamps to be consistent
with TSTool, as follows:

*   History (recent irregular interval time series):
    +   use timestamps from Zabbix web services without changing
*   Trend (older, hour-interval time series):
    +   timestamps are adjusted from the beginning of the interval to the end of the interval
        to agree with TSTool conventions
    +   use the [`ReadZabbix`](../ReadZabbix/ReadZabbix.md) command
        to control how the timestamp is handled
        
## Command Editor ##

All TSID commands are edited using the general
[`TSID`](https://opencdss.state.co.us/tstool/latest/doc-user/command-ref/TSID/TSID/)
command editor.

## Command Syntax ##

See the [Zabbix Datastore Appendix](../../datastore-ref/Zabbix/Zabbix.md) for information about TSID syntax.

## Examples ##

See the [automated tests](https://github.com/OpenWaterFoundation/owf-tstool-zabbix-plugin/tree/master/test/commands/TSID/).

## Troubleshooting ##

*   See the [`ReadZabbix` command troubleshooting](../ReadZabbix/ReadZabbix.md#troubleshooting) documentation.

## See Also ##

*   [`ReadZabbix`](../ReadZabbix/ReadZabbix.md) command for full control reading Zabbix time series
*   [`ReadTimeSeries`](https://opencdss.state.co.us/tstool/latest/doc-user/command-ref/ReadTimeSeries/ReadTimeSeries/) command - provides more flexibility than a TSID
