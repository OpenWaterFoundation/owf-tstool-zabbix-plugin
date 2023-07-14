# TSTool / Troubleshooting #

Troubleshooting TSTool for Zabbix involves confirming that the core product and plugin are performing as expected.
Issues may also be related to Zabbix data.

*   [Troubleshooting Core TSTool Product](#troubleshooting-core-tstool-product)
*   [Troubleshooting Zabbix TSTool Integration](#troubleshooting-zabbix-tstool-integration)
    +   [***Commands(Plugin)*** Menu Contains Duplicate Commands](#commandsplugin-menu-contains-duplicate-commands)
    +   [Web Service Datastore Returns no Data](#web-service-datastore-returns-no-data)
    +   [Zabbix Web Service Request Hangs](#zabbix-web-service-request-hangs)

------------------

## Troubleshooting Core TSTool Product ##

The core TSTool product is developed and maintained separate from the plugin.
Issues may arise in the core product or user-defined inputs.

See the main [TSTool Troubleshooting documentation](https://opencdss.state.co.us/tstool/latest/doc-user/troubleshooting/troubleshooting/).

## Troubleshooting Zabbix TSTool Integration ##

The following are typical issues that are encountered when using TSTool with Zabbix.
Refer to the following to help with troubleshooting:

*   The ***View / Datastores*** menu item will display the status of datastores.
*   The ***Tools / Diagnostics - View Log File (Startup)...*** menu item will display the TSTool startup log file, 
    which contains information about initial datastore connections.
    Use this log file when troubleshooting startup issues,
    for example errors related to Zabbix connections and time series browsing.
*   The ***Tools / Diagnostics - View Log File...*** menu item will display the current log file,
    for example if a log file has been specified for a workflow using the
    [`StartLog`](https://opencdss.state.co.us/tstool/latest/doc-user/command-ref/StartLog/StartLog/) command.

### ***Commands(Plugin)*** Menu Contains Duplicate Commands ###

If the ***Commands(Plugin)*** menu contains duplicate commands,
TSTool is finding multiple plugin `jar` files.
To fix, check the `plugins` folder and subfolders for the software installation folder
and the user's `.tstool/NN/plugins` folder.
Remove extra jar files, leaving only the version that is desired (typically the most recent version).

### Web Service Datastore Returns no Data ###

If the web service datastore returns no data, check the following:

1.  Review the TSTool log file for errors.
    Typically a message will indicate an HTTP error code for the URL that was requested.
    The URL and JSON request string that is used for queries are shown in the log file.
2.  Use the TSTool [`WebGet`](https://opencdss.state.co.us/tstool/latest/doc-user/command-ref/WebGet/WebGet/)
    command to confirm that the Zabbix API is working.
    It may be necessary to update the plugin code if the API has changed.
3.  See the [Zabbix documentation](https://www.zabbix.com/documentation/current/en/manual/api/reference/)
    to check whether the URL and data arequest are correct.
    It may be necessary to consult a specific version of the Zabbix documentation if the current version is not being used.
    Replace `current` in the above URL with the Zabbix version that is used for the datastore (e.g., `5.4`).

If the issue cannot be resolved, contact the [Open Water Foundation](https://openwaterfoundation.org/about-owf/staff/).

### Zabbix Web Service Request Hangs ###

TSTool may hang at startup if a Zabbix datastore is enabled
and will show the message ***Wait...initializing data connections*** (or similar).
The log file will also show that the Zabbix datastore is being initialized.
Zabbix features may also hang at other times.

The cause may an incompatibility between core TSTool software and the plugin.
Although an attempt is made to ensure compatibility between TSTool and plugin versions,
sometimes design changes occur that result in incompatibilities.

Refer to [TSTool Zabbix plugin release notes](../appendix-release-notes/release-notes.md)
to determine whether TSTool needs to be updated.
In the future, a more rigorous plugin manager will be implemented to help with version compatibility.
