// ReadZabbix_JDialog - editor for the ReadZabbix() command.

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

package org.openwaterfoundation.tstool.plugin.zabbix.commands;

import java.awt.Color;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.openwaterfoundation.tstool.plugin.zabbix.datastore.ZabbixDataStore;
import org.openwaterfoundation.tstool.plugin.zabbix.ui.Zabbix_TimeSeries_InputFilter_JPanel;

import riverside.datastore.DataStore;
import rti.tscommandprocessor.core.TSCommandProcessor;
import RTi.TS.TSFormatSpecifiersJPanel;
import RTi.Util.GUI.InputFilter_JPanel;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.Help.HelpViewer;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import RTi.Util.Time.TimeInterval;

/**
Editor for the ReadZabbix() command.
*/
@SuppressWarnings("serial")
public class ReadZabbix_JDialog extends JDialog
implements ActionListener, DocumentListener, ItemListener, KeyListener, WindowListener
{
private SimpleJButton dataStoreDocumentation_JButton = null;
private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private SimpleJButton __help_JButton = null;
private ReadZabbix_Command __command = null;
private SimpleJComboBox __DataStore_JComboBox = null;
private SimpleJComboBox __DataType_JComboBox;
private SimpleJComboBox __Interval_JComboBox;
private TSFormatSpecifiersJPanel __Alias_JTextField = null;
private JTabbedPane __tsInfo_JTabbedPane = null;
private JPanel __multipleTS_JPanel = null;
private SimpleJComboBox __DataSource_JComboBox;
private SimpleJComboBox __LocId_JComboBox = null;
private JTextField __TSID_JTextField;
private JTextField __InputStart_JTextField;
private JTextField __InputEnd_JTextField;
private JTextField __TimeZone_JTextField;
private SimpleJComboBox __ShiftTrendToIntervalEnd_JComboBox;
private SimpleJComboBox	__Debug_JComboBox;

private JTextArea __command_JTextArea = null;
// Contains all input filter panels.  Use the ZabbixSDataStore name/description and data type for each to
// figure out which panel is active at any time.
// Using the general panel and casting later causes a ClassCastException since classes are loaded in different ClassLoader.
// private List<InputFilter_JPanel> __inputFilterJPanelList = new ArrayList<>();
private List<Zabbix_TimeSeries_InputFilter_JPanel> __inputFilterJPanelList = new ArrayList<>();
private ZabbixDataStore __dataStore = null; // Selected ZabbixDataStore.
private boolean __error_wait = false; // Is there an error to be cleared up?
private boolean __first_time = true;
private boolean __ok = false; // Was OK pressed when closing the dialog?
private boolean __ignoreEvents = false; // Used to ignore cascading events when initializing the components.

/**
Command editor constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public ReadZabbix_JDialog ( JFrame parent, ReadZabbix_Command command ) {
	super(parent, true);
	initialize ( parent, command );
}

/**
Responds to ActionEvents.
@param event ActionEvent object
*/
public void actionPerformed( ActionEvent event ) {
	if ( __ignoreEvents ) {
        return; // Startup.
    }
    Object o = event.getSource();

    if ( o == __cancel_JButton ) {
        response ( false );
    }
    else if ( o == dataStoreDocumentation_JButton ) {
        try {
            Desktop desktop = Desktop.getDesktop();
            desktop.browse ( new URI(dataStoreDocumentation_JButton.getActionCommand()) );
        }
        catch ( Exception e ) {
            Message.printWarning(1, null, "Unable to display Zabbix web services documentation using \"" +
                dataStoreDocumentation_JButton.getActionCommand() + "\"" );
        }
    }
	else if ( o == __help_JButton ) {
		HelpViewer.getInstance().showHelp("command", "ReadZabbix",
			"https://software.openwaterfoundation.org/tstool-kiwis-plugin/latest/doc-user");
	}
    else if ( o == __ok_JButton ) {
        refresh ();
        checkInput ();
        if ( !__error_wait ) {
            response ( true );
        }
    }
    else {
        // ComboBoxes.
        refresh();
    }
}

/**
Refresh the data type choices in response to the currently selected Zabbix datastore.
*/
private void actionPerformedDataStoreSelected ( ) {
    if ( __DataStore_JComboBox.getSelected() == null ) {
        // Startup initialization.
        return;
    }
    setDataStoreForSelectedInput();
    //Message.printStatus(2, "", "Selected data store " + __dataStore + " __dmi=" + __dmi );
    // Now populate the data type choices corresponding to the data store
    populateDataTypeChoices ( getSelectedDataStore() );
}

/**
Refresh the query choices for the currently selected data source.
*/
private void actionPerformedDataSourceSelected ( ) {
    if ( __DataSource_JComboBox.getSelected() == null ) {
        // Startup initialization.
        return;
    }
    // Populate the location ID corresponding to the data type.
    populateLocIdChoices ( getSelectedDataStore() );
}

/**
Refresh the query choices for the currently selected Zabbix datastore.
*/
private void actionPerformedDataTypeSelected ( ) {
    if ( __DataType_JComboBox.getSelected() == null ) {
        // Startup initialization.
        return;
    }
    // Populate the interval choices corresponding to the data type.
    populateIntervalChoices ( getSelectedDataStore() );
    populateDataSourceChoices ( getSelectedDataStore() );
}

/**
Set visible the appropriate input filter, based on the interval and other previous selections.
*/
private void actionPerformedIntervalSelected ( ) {
    if ( __Interval_JComboBox.getSelected() == null ) {
        // Startup initialization.
        return;
    }
    // Populate the filters corresponding to the data type and interval.
    selectInputFilter ( getDataStore() );
}

/**
Refresh the query choices for the currently selected Zabbix location identifier.
@param value if non-null, then the selection is from the command initialization, in which case the
specified data type should be selected
*/
private void actionPerformedLocIdSelected ( ) {
    if ( __LocId_JComboBox.getSelected() == null ) {
        // Startup initialization.
        return;
    }
}


// Start event handlers for DocumentListener...

/**
Handle DocumentEvent events.
@param e DocumentEvent to handle.
*/
public void changedUpdate ( DocumentEvent e ) {
	checkGUIState();
    refresh();
}

/**
Handle DocumentEvent events.
@param e DocumentEvent to handle.
*/
public void insertUpdate ( DocumentEvent e ) {
	checkGUIState();
    refresh();
}

/**
Handle DocumentEvent events.
@param e DocumentEvent to handle.
*/
public void removeUpdate ( DocumentEvent e ) {
	checkGUIState();
    refresh();
}

// ...end event handlers for DocumentListener.

/**
Check the state of the dialog, disabling/enabling components as appropriate.
*/
private void checkGUIState() {
	// If "AllMatchingTSID", enable the list.
	// Otherwise, clear and disable.
	if ( __DataType_JComboBox != null ) {
		String DataType = getSelectedDataType();
		if ( DataType == null ) {
		    // Initialization.
		    DataType = "*";
		}
	}

    // If datastore is selected and has the property for API documentation, enable the documentation buttons.
    ZabbixDataStore dataStore = getSelectedDataStore();
    if ( dataStore != null ) {
        String urlString = dataStore.getProperty ( "ServiceAPIDocumentationURI" );
        if ( urlString == null ) {
            this.dataStoreDocumentation_JButton.setEnabled(false);
        }
        else {
            this.dataStoreDocumentation_JButton.setActionCommand(urlString);
            this.dataStoreDocumentation_JButton.setEnabled(true);
        }
    }
}

/**
Check the input.  If errors exist, warn the user and set the __error_wait flag
to true.  This should be called before response() is allowed to complete.
*/
private void checkInput () {
	if ( __ignoreEvents ) {
        return; // Startup.
    }
    // Put together a list of parameters to check.
	PropList props = new PropList ( "" );
	__error_wait = false;
	// Check parameters for the two command versions.
    String DataStore = __DataStore_JComboBox.getSelected();
    if ( DataStore.length() > 0 ) {
        props.set ( "DataStore", DataStore );
    }
	String Alias = __Alias_JTextField.getText().trim();
	if ( Alias.length() > 0 ) {
		props.set ( "Alias", Alias );
	}
	//String TSID = __TSID_JTextField.getText().trim();
	//if ( TSID.length() > 0 ) {
	//	props.set ( "TSID", TSID );
	//}
    String DataType = getSelectedDataType();
	if ( DataType.length() > 0 ) {
		props.set ( "DataType", DataType );
	}
	String Interval = getSelectedInterval();
	if ( Interval.length() > 0 ) {
		props.set ( "Interval", Interval );
	}
	String DataSource = getSelectedDataSource();
	if ( DataSource.length() > 0 ) {
		props.set ( "DataSource", DataSource );
	}
	String LocId = getSelectedLocId();
	if ( LocId.length() > 0 ) {
		props.set ( "LocId", LocId );
	}
	InputFilter_JPanel filterPanel = getVisibleInputFilterPanel();
	int whereCount = 0; // Number of non-empty Where parameters specified.
	if ( filterPanel != null ) {
    	for ( int i = 1; i <= filterPanel.getNumFilterGroups(); i++ ) {
    	    String where = getWhere ( i - 1 );
    	    // Blank where is something like: ";operator;"
    	    if ( !where.isEmpty() && !where.startsWith(";") && !where.endsWith(";") ) {
    	    	++whereCount;
    	    }
    	    if ( where.length() > 0 ) {
    	        props.set ( "Where" + i, where );
    	    }
        }
	}
	// Both command types use these.
	String InputStart = __InputStart_JTextField.getText().trim();
	String InputEnd = __InputEnd_JTextField.getText().trim();
	if ( InputStart.length() > 0 ) {
		props.set ( "InputStart", InputStart );
	}
	if ( InputEnd.length() > 0 ) {
		props.set ( "InputEnd", InputEnd );
	}
    if ( whereCount > 0 ) {
        // Input filters are specified so check:
    	// - this is done in the input filter because that code is called from this command and main TSTool UI
        InputFilter_JPanel ifp = getVisibleInputFilterPanel();
        if ( ifp != null ) {
        	// Set a property to pass to the general checkCommandParameters method so that the
        	// results can be combined with the other command parameter checks.
        	props.set("InputFiltersCheck",ifp.checkInputFilters(false));
        }
    }
	String TimeZone = __TimeZone_JTextField.getText().trim();
	if ( TimeZone.length() > 0 ) {
		props.set ( "TimeZone", TimeZone );
	}
	String ShiftTrendToIntervalEnd = __ShiftTrendToIntervalEnd_JComboBox.getSelected();
	if ( ShiftTrendToIntervalEnd.length() > 0 ) {
		props.set ( "ShiftTrendToIntervalEnd", ShiftTrendToIntervalEnd );
	}
	String Debug = __Debug_JComboBox.getSelected();
	if ( Debug.length() > 0 ) {
		props.set ( "Debug", Debug );
	}
	try {
	    // This will warn the user.
		__command.checkCommandParameters ( props, null, 1 );
	}
	catch ( Exception e ) {
		// The warning would have been printed in the check code.
		__error_wait = true;
	}
}

/**
Commit the edits to the command.  In this case the command parameters have
already been checked and no errors were detected.
*/
private void commitEdits () {
	String DataStore = __DataStore_JComboBox.getSelected();
    __command.setCommandParameter ( "DataStore", DataStore );
	String Alias = __Alias_JTextField.getText().trim();
	__command.setCommandParameter ( "Alias", Alias );
	//String TSID = __TSID_JTextField.getText().trim();
	//__command.setCommandParameter ( "TSID", TSID );
	String DataType = getSelectedDataType();
	__command.setCommandParameter ( "DataType", DataType );
	String Interval = getSelectedInterval();
	__command.setCommandParameter ( "Interval", Interval );
	// Match 1 time series.
	String DataSource = getSelectedDataSource();
	__command.setCommandParameter ( "DataSource", DataSource );
	String LocId = getSelectedLocId();
	__command.setCommandParameter ( "LocId", LocId );
	// 1+ time series.
	String delim = ";";
	//InputFilter_JPanel filterPanel = getVisibleInputFilterPanel();
	Zabbix_TimeSeries_InputFilter_JPanel filterPanel = getVisibleInputFilterPanel();
	for ( int i = 1; i <= filterPanel.getNumFilterGroups(); i++ ) {
	    String where = getWhere ( i - 1 );
	    if ( where.startsWith(delim) ) {
	        where = "";
	    }
	    __command.setCommandParameter ( "Where" + i, where );
	}
	// Both versions of the commands use these.
	String InputStart = __InputStart_JTextField.getText().trim();
	__command.setCommandParameter ( "InputStart", InputStart );
	String InputEnd = __InputEnd_JTextField.getText().trim();
	__command.setCommandParameter ( "InputEnd", InputEnd );
	String TimeZone = __TimeZone_JTextField.getText().trim();
	__command.setCommandParameter ( "TimeZone", TimeZone );
	String ShiftTrendToIntervalEnd = __ShiftTrendToIntervalEnd_JComboBox.getSelected();
	__command.setCommandParameter (	"ShiftTrendToIntervalEnd", ShiftTrendToIntervalEnd );
	String Debug = __Debug_JComboBox.getSelected();
	__command.setCommandParameter (	"Debug", Debug );
}

/**
Return the datastore that is in effect.
*/
private ZabbixDataStore getDataStore() {
    return __dataStore;
}

/**
Get the input filter list.
*/
//private List<InputFilter_JPanel> getInputFilterJPanelList ()
private List<Zabbix_TimeSeries_InputFilter_JPanel> getInputFilterJPanelList () {
    return __inputFilterJPanelList;
}

/**
Get the input name to use for the TSID.
*/
private String getInputNameForTSID() {
    // Use the data store name if specified.
    String DataStore = __DataStore_JComboBox.getSelected();
    if ( (DataStore != null) && !DataStore.equals("") ) {
        return DataStore;
    }
    else {
        return "Zabbix"; // Default.
    }
}

/**
Return the selected data interval, currently a simple lookup.
@return the selected data interval (e.g., IrregSecond or Hour)
*/
private String getSelectedDataInterval() {
    if ( __Interval_JComboBox == null ) {
        return null;
    }
    String dataInterval = __Interval_JComboBox.getSelected();
    if ( dataInterval == null ) {
    	return dataInterval;
    }
    // Make sure to use spaces around the dashes because dash without space is used to indicate statistic,
    // and want that included in the data type.
  	int pos = dataInterval.indexOf(" - ");
    if ( pos > 0 ) {
    	// Return the first item.
        dataInterval = dataInterval.substring(0,pos).trim();
    }
    else {
    	// Return the full string.
        dataInterval = dataInterval.trim();
    }
    return dataInterval;
}

/**
Return the selected data source, currently a simple lookup.
@return the selected data source (host group name)
*/
private String getSelectedDataSource() {
    if ( __DataSource_JComboBox == null ) {
        return null;
    }
    String dataSource = __DataSource_JComboBox.getSelected();
    if ( dataSource == null ) {
    	return dataSource;
    }
    // Make sure to use spaces around the dashes because dash without space is used to indicate statistic,
    // and want that included in the data type.
  	int pos = dataSource.indexOf(" - ");
    if ( pos > 0 ) {
    	// Return the first item.
        dataSource = dataSource.substring(0,pos).trim();
    }
    else {
    	// Return the full string.
        dataSource = dataSource.trim();
    }
    return dataSource;
}

/**
Get the selected data store from the processor.
*/
private ZabbixDataStore getSelectedDataStore () {
	String routine = getClass().getSimpleName() + ".getSelectedDataStore";
    String DataStore = __DataStore_JComboBox.getSelected();
    ZabbixDataStore dataStore = (ZabbixDataStore)((TSCommandProcessor)
        __command.getCommandProcessor()).getDataStoreForName( DataStore, ZabbixDataStore.class );
    if ( dataStore != null ) {
        //Message.printStatus(2, routine, "Selected datastore is \"" + dataStore.getName() + "\"." );
    }
    else {
        Message.printStatus(2, routine, "Cannot get datastore for \"" + DataStore + "\"." );
    }
    return dataStore;
}

/**
Return the selected data type, omitting the trailing note.
*/
private String getSelectedDataType() {
    if ( __DataType_JComboBox == null ) {
        return null;
    }
    String dataType = __DataType_JComboBox.getSelected();
    if ( dataType == null ) {
    	return dataType;
    }
    // Make sure to use spaces around the dashes.
  	int pos = dataType.indexOf(" - ");
    if ( pos > 0 ) {
    	// Return the first item.
        dataType = dataType.substring(0,pos).trim();
    }
    else {
    	// Return the full string.
        dataType = dataType.trim();
    }
    return dataType;
}

/**
Return the selected data interval, omitting the trailing note.
*/
private String getSelectedInterval() {
    if ( __Interval_JComboBox == null ) {
        return null;
    }
    String interval = __Interval_JComboBox.getSelected();
    if ( interval == null ) {
    	return interval;
    }
   	int pos = interval.indexOf(" - ");
    if ( pos > 0 ) {
    	// Return the first item.
        interval = interval.substring(0,pos).trim();
    }
    else {
    	// Return the full string.
        interval = interval.trim();
    }
    return interval;
}

/**
Return the selected location ID (host).
If the string has "host - host name", return the trimmed part before the dash.
*/
private String getSelectedLocId() {
    if ( __LocId_JComboBox == null ) {
        return null;
    }
    String locId = __LocId_JComboBox.getSelected();
    if ( locId == null ) {
    	return locId;
    }
   	int pos = locId.indexOf(" - ");
    if ( pos > 0 ) {
    	// Return the first item.
        locId = locId.substring(0,pos).trim();
    }
    else {
    	// Return the full string.
        locId = locId.trim();
    }
    return locId;
}

/**
Return the visible input filter panel, or null if none visible.
*/
//private InputFilter_JPanel getVisibleInputFilterPanel() {
private Zabbix_TimeSeries_InputFilter_JPanel getVisibleInputFilterPanel() {
    //List<InputFilter_JPanel> panelList = getInputFilterJPanelList();
    List<Zabbix_TimeSeries_InputFilter_JPanel> panelList = getInputFilterJPanelList();
    String panelName;
    //for ( InputFilter_JPanel panel : panelList ) {
    for ( Zabbix_TimeSeries_InputFilter_JPanel panel : panelList ) {
        // Skip default.
        panelName = panel.getName();
        if ( (panelName != null) && panelName.equalsIgnoreCase("Default") ) {
            continue;
        }
        if ( panel.isVisible() ) {
        	if ( Message.isDebugOn ) {
        		Message.printStatus(2,"","Visible filter panel name is \"" + panelName + "\"");
        	}
            return panel;
        }
    }
    return null;
}

/**
Return the "WhereN" parameter for the requested input filter.
@return the "WhereN" parameter for the requested input filter.
@param ifg the Input filter to process (zero index).
*/
private String getWhere ( int ifg ) {
	String delim = ";";	// To separate input filter parts.
	//InputFilter_JPanel filterPanel = getVisibleInputFilterPanel();
	Zabbix_TimeSeries_InputFilter_JPanel filterPanel = getVisibleInputFilterPanel();
    String where = "";
    if ( filterPanel != null ) {
    	// Use the internal value for the where to ensure integration.
        where = filterPanel.toString(ifg,delim,3).trim();
    }
	return where;
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
private void initialize ( JFrame parent, ReadZabbix_Command command ) {
	//String routine = getClass().getSimpleName() + ".initialize";
	__command = command;
	CommandProcessor processor = __command.getCommandProcessor();
	addWindowListener( this );
    Insets insetsTLBR = new Insets(2,2,2,2);

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = -1;

    JGUIUtil.addComponent(main_JPanel, new JLabel (
    	"Read 1+ time series from a Zabbix web services datastore, using options from the choices below to select time series."),
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
   	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Refer to the Zabbix web services documentation for information about data types (item name) and intervals." ),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
   	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Specifying the period will limit data that are available " +
		"for later commands but can increase performance." ),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
   	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Reading time series for a single location takes precedence over reading multiple time series." ),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
   	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Requests may be constrained by the software to prevent unintended large bulk queries." ),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    // Add buttons for the documentation:
    // - the checkGUIState() method checks for and sets the URL in the button's action

	this.dataStoreDocumentation_JButton = new SimpleJButton("Zabbix Documentation", this);
	this.dataStoreDocumentation_JButton.setToolTipText("View the Zabbix documentation for the datastore in a web browser.");
    JGUIUtil.addComponent(main_JPanel, this.dataStoreDocumentation_JButton,
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

   	__ignoreEvents = true; // So that a full pass of initialization can occur.

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Zabbix datastore:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DataStore_JComboBox = new SimpleJComboBox ( false );
    TSCommandProcessor tsProcessor = (TSCommandProcessor)processor;
    Message.printStatus(2, "ReadZabbix", "Getting datastores for ZabbixDataStore class");
    List<DataStore> dataStoreList = tsProcessor.getDataStoresByType( ZabbixDataStore.class );
    // Datastore is required, so no blank
    List<String> datastoreChoices = new ArrayList<>();
    for ( DataStore dataStore: dataStoreList ) {
    	datastoreChoices.add ( dataStore.getName() );
    }
    __DataStore_JComboBox.setData(datastoreChoices);
    if ( datastoreChoices.size() > 0 ) {
    	__DataStore_JComboBox.select ( 0 );
    }
    __DataStore_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(main_JPanel, __DataStore_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel("Required - Zabbix datastore."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    //JGUIUtil.addComponent(main_JPanel, inputFilterJPanel,
    //    0, ++y, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Data type (item name):"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__DataType_JComboBox = new SimpleJComboBox ( false );
	__DataType_JComboBox.setToolTipText("Data types from Zabbix 'item.name', used in TSID data type.");
	__DataType_JComboBox.setMaximumRowCount(20);
	__DataType_JComboBox.addItemListener ( this );
        JGUIUtil.addComponent(main_JPanel, __DataType_JComboBox,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Required to match a single location - data type for time series."),
		3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ("Data interval:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__Interval_JComboBox = new SimpleJComboBox ();
	__Interval_JComboBox.setToolTipText("Data interval (IrregSecond for history, Hour for trend data).");
	__Interval_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(main_JPanel, __Interval_JComboBox,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Required to match a single location - data interval (time step) for time series."),
		3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    __tsInfo_JTabbedPane = new JTabbedPane ();
    __tsInfo_JTabbedPane.setBorder(
        BorderFactory.createTitledBorder ( BorderFactory.createLineBorder(Color.black),
        "Indicate how to match time series in Zabbix" ));
    JGUIUtil.addComponent(main_JPanel, __tsInfo_JTabbedPane,
        0, ++y, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JPanel singleTS_JPanel = new JPanel();
    singleTS_JPanel.setLayout(new GridBagLayout());
    __tsInfo_JTabbedPane.addTab ( "Match Single Time Series", singleTS_JPanel );

    int ySingle = -1;
    JGUIUtil.addComponent(singleTS_JPanel, new JLabel(
    	"Match a single time series for a location (host)."),
        0, ++ySingle, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(singleTS_JPanel, new JLabel(
    	"A unique TSID is formed from the Zabbix host, host group, item name, and data interval (Hour for trend, IrregSecond for history)."),
        0, ++ySingle, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(singleTS_JPanel, new JLabel(
    	"The data type and interval must be specified above (DO NOT USE * for data type or interval)."),
        0, ++ySingle, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(singleTS_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
        0, ++ySingle, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(singleTS_JPanel, new JLabel ( "Data source (host group):"),
        0, ++ySingle, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DataSource_JComboBox = new SimpleJComboBox ( false );
    __DataSource_JComboBox.setToolTipText("Data source to match (host group name).");
    __DataSource_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(singleTS_JPanel, __DataSource_JComboBox,
        1, ySingle, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(singleTS_JPanel, new JLabel ( "Required if matching a single location."),
        3, ySingle, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(singleTS_JPanel, new JLabel ( "Location ID (host):"),
        0, ++ySingle, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __LocId_JComboBox = new SimpleJComboBox ( false );
    __LocId_JComboBox.setToolTipText("Location identifier (station_no) to match.");
    __LocId_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(singleTS_JPanel, __LocId_JComboBox,
        1, ySingle, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(singleTS_JPanel, new JLabel ( "Required if matching a single location."),
        3, ySingle, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(singleTS_JPanel, new JLabel ( "TSID (full):"),
        0, ++ySingle, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TSID_JTextField = new JTextField ( "" );
    __TSID_JTextField.setToolTipText("The time series identifier that will be used to read the time series.");
    __TSID_JTextField.setEditable ( false );
    __TSID_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(singleTS_JPanel, __TSID_JTextField,
        1, ySingle, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(singleTS_JPanel, new JLabel ( "Created from above parameters."),
        3, ySingle, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    __multipleTS_JPanel = new JPanel();
    __multipleTS_JPanel.setLayout(new GridBagLayout());
    __tsInfo_JTabbedPane.addTab ( "Match 1+ Time Series", __multipleTS_JPanel );
    // Note to warn about performance.
    int yMult = -1;
    JGUIUtil.addComponent(__multipleTS_JPanel, new JLabel("Use filters (\"where\" clauses) to limit result size and " +
        "increase performance.  Filters are AND'ed."),
        0, ++yMult, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(__multipleTS_JPanel, new JLabel(
    	"The time series catalog is searched and then each matching time series is read."),
        0, ++yMult, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(__multipleTS_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
        0, ++yMult, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    // Initialize all the filters (selection will be based on data store).
    initializeInputFilters ( __multipleTS_JPanel, ++yMult, dataStoreList );

    JGUIUtil.addComponent(main_JPanel, new JLabel("Alias to assign:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Alias_JTextField = new TSFormatSpecifiersJPanel(10);
    __Alias_JTextField.setToolTipText("Use %L for location, %T for data type, %I for interval.");
    __Alias_JTextField.addKeyListener ( this );
    __Alias_JTextField.getDocument().addDocumentListener(this);
    __Alias_JTextField.setToolTipText("%L for location, %T for data type.");
    JGUIUtil.addComponent(main_JPanel, __Alias_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Optional - use %L for location, etc. (default=no alias)."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ("Input start:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __InputStart_JTextField = new JTextField (20);
    __InputStart_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, __InputStart_JTextField,
        1, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Optional - overrides the global input start."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Input end:"),
        0, ++y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __InputEnd_JTextField = new JTextField (20);
    __InputEnd_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, __InputEnd_JTextField,
        1, y, 6, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Optional - overrides the global input end."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Time zone:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TimeZone_JTextField = new JTextField ( "", 20 );
    __TimeZone_JTextField.setToolTipText(
    	"Time zone for response, for example America/Denver (default is host time zone, see commmand documentation).");
    __TimeZone_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __TimeZone_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Optional - to control output time zone."),
        3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Shift trend to interval end:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    List<String> shiftTrendToIntervalEnd_List = new ArrayList<>( 3 );
	shiftTrendToIntervalEnd_List.add ( "" );
	shiftTrendToIntervalEnd_List.add ( __command._False );
	shiftTrendToIntervalEnd_List.add ( __command._True );
	__ShiftTrendToIntervalEnd_JComboBox = new SimpleJComboBox ( false );
	__ShiftTrendToIntervalEnd_JComboBox.setToolTipText("Zabbix stores trend at interval start, TSTool uses interval end.");
	__ShiftTrendToIntervalEnd_JComboBox.setData ( shiftTrendToIntervalEnd_List);
	__ShiftTrendToIntervalEnd_JComboBox.select ( 0 );
	__ShiftTrendToIntervalEnd_JComboBox.addActionListener ( this );
    JGUIUtil.addComponent(main_JPanel, __ShiftTrendToIntervalEnd_JComboBox,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Optional - shift trend data to interval end (default=" + __command._True + ")."),
		3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Debug:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    List<String> Debug_List = new ArrayList<>( 3 );
	Debug_List.add ( "" );
	Debug_List.add ( __command._False );
	Debug_List.add ( __command._True );
	__Debug_JComboBox = new SimpleJComboBox ( false );
	__Debug_JComboBox.setToolTipText("Enable debug for web services, used for troubleshooting).");
	__Debug_JComboBox.setData ( Debug_List);
	__Debug_JComboBox.select ( 0 );
	__Debug_JComboBox.addActionListener ( this );
    JGUIUtil.addComponent(main_JPanel, __Debug_JComboBox,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Optional - enable debug for web services (default=" + __command._False + ")."),
		3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Command:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__command_JTextArea = new JTextArea (4,50);
	__command_JTextArea.setLineWrap ( true );
	__command_JTextArea.setWrapStyleWord ( true );
	__command_JTextArea.setEditable ( false );
	JGUIUtil.addComponent(main_JPanel, new JScrollPane(__command_JTextArea),
		1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);

	// Refresh the contents (still ignoring events).
	refresh ();

	// South Panel: North
	JPanel button_JPanel = new JPanel();
	button_JPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        JGUIUtil.addComponent(main_JPanel, button_JPanel,
		0, ++y, 8, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER);

	__ok_JButton = new SimpleJButton("OK", this);
	__ok_JButton.setToolTipText("Save changes to command");
	button_JPanel.add ( __ok_JButton );
	__cancel_JButton = new SimpleJButton( "Cancel", this);
	button_JPanel.add ( __cancel_JButton );
	__cancel_JButton.setToolTipText("Cancel without saving changes to command");
	button_JPanel.add ( __help_JButton = new SimpleJButton("Help", this) );
	__help_JButton.setToolTipText("Show command documentation in web browser");

	setTitle ( "Edit " + __command.getCommandName() + " Command" );

    // Because it is necessary to select the proper input filter during initialization (to transfer an old command's
    // parameter values), the selected input filter may not be desirable for dialog sizing.  Therefore, manually set
    // all panels to visible and then determine the preferred size as the maximum.  Then reselect the appropriate input
    // filter before continuing.
    setAllFiltersVisible();
    // All filters are visible at this point so pack chooses good sizes.
    pack();
    setPreferredSize(getSize()); // Will reflect all filters being visible
    __multipleTS_JPanel.setPreferredSize(__multipleTS_JPanel.getSize()); // So initial height is maximum height
    selectInputFilter( getDataStore()); // Now go back to the filter for the selected input type and intern
    JGUIUtil.center( this );
    __ignoreEvents = false; // After initialization of components let events happen to dynamically cause cascade.
    // Now refresh once more.
	refresh();
	checkGUIState(); // Do this again because it may not have happened due to the special event handling.
	setResizable ( false );
    super.setVisible( true );
}

/**
Initialize input filters for all of the available Zabbix datastores.
The input filter panels will be layered on top of each other, but only one will be set visible, based on the
other visible selections.
@param parent_JPanel the panel to receive the input filter panels
@param y position in the layout to add the input filter panel
@param dataStoreList the list of available ZabbixDataStore
*/
private void initializeInputFilters ( JPanel parent_JPanel, int y, List<DataStore> dataStoreList ) {
	String routine = getClass().getSimpleName() + ".initializeInputFilters";
    // Loop through data stores and add filters for all data groups.
    for ( DataStore ds : dataStoreList ) {
    	Message.printStatus(2,routine,"Initializing data store list for datastore name \"" + ds.getName() +
    		"\" class: " + ds.getClass() );
    	Message.printStatus(2, routine, "Casting to ZabbixDataStore class: " + ZabbixDataStore.class);
        initializeInputFilters_OneFilter ( parent_JPanel, y, (ZabbixDataStore)ds);
    }

    // Blank panel indicating data type was not matched.
    // Add in the same position as the other filter panels.

    int buffer = 3;
    Insets insets = new Insets(1,buffer,1,buffer);
    //List<InputFilter_JPanel> ifPanelList = getInputFilterJPanelList();
    List<Zabbix_TimeSeries_InputFilter_JPanel> ifPanelList = getInputFilterJPanelList();
    //InputFilter_JPanel panel = new InputFilter_JPanel("Data type and interval have no input filters.");
    Zabbix_TimeSeries_InputFilter_JPanel panel =
    	new Zabbix_TimeSeries_InputFilter_JPanel("Data type and interval have no input filters.");
    panel.setName("Default");
    JGUIUtil.addComponent(parent_JPanel, panel,
        0, y, 7, 1, 0.0, 0.0, insets, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST );
    ifPanelList.add ( panel );
}

/**
Initialize input filters for one NoavStar web service datastore.
@param parent_JPanel the panel to receive the input filter panels
@param y for layout
@param dataStore datastore to use with the filter
*/
private void initializeInputFilters_OneFilter ( JPanel parent_JPanel, int y, ZabbixDataStore dataStore ) {
	String routine = getClass().getSimpleName() + ".initializeInputFilters_OneFilter";
    int buffer = 3;
    Insets insets = new Insets(1,buffer,1,buffer);
    //List<InputFilter_JPanel> inputFilterJPanelList = getInputFilterJPanelList();
    List<Zabbix_TimeSeries_InputFilter_JPanel> inputFilterJPanelList = getInputFilterJPanelList();

    boolean visibility = true; // Set this so that the layout manager will figure out the size of the dialog at startup.
    int x = 0; // Position in layout manager, same for all since overlap.
    //int numVisibleChoices = -1; // For the combobox choices, -1 means size to data list size.
    try {
        // Time series...
        Zabbix_TimeSeries_InputFilter_JPanel panel = new Zabbix_TimeSeries_InputFilter_JPanel ( dataStore, 5 );
        //panel.setName(dataStore.getName() + ".Station" );
        panel.setName(dataStore.getName() );
        JGUIUtil.addComponent(parent_JPanel, panel,
            x, y, 7, 1, 0.0, 0.0, insets, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST );
        inputFilterJPanelList.add ( panel );
        panel.addEventListeners ( this );
        panel.setVisible ( visibility );
    }
    catch ( Exception e ) {
        Message.printWarning ( 2, routine,
        "Unable to initialize input filter for Zabbix time series catalog (" + e + ")." );
        Message.printWarning ( 3, routine, e );
    }
}

/**
Respond to ItemEvents.
*/
public void itemStateChanged ( ItemEvent event ) {
    if ( __ignoreEvents ) {
        return; // Startup.
    }
    if ( (event.getSource() == __DataStore_JComboBox) && (event.getStateChange() == ItemEvent.SELECTED) ) {
        // User has selected a data store.
        actionPerformedDataStoreSelected ();
    }
    else if ( (event.getSource() == __DataSource_JComboBox) && (event.getStateChange() == ItemEvent.SELECTED) ) {
        // User has selected a data source.
        actionPerformedDataSourceSelected ();
    }
    else if ( (event.getSource() == __DataType_JComboBox) && (event.getStateChange() == ItemEvent.SELECTED) ) {
        // User has selected a data type.
        actionPerformedDataTypeSelected ();
    }
    else if ( (event.getSource() == __Interval_JComboBox) && (event.getStateChange() == ItemEvent.SELECTED) ) {
        // User has selected an interval.
        actionPerformedIntervalSelected ();
    }
    else if ( (event.getSource() == __LocId_JComboBox) && (event.getStateChange() == ItemEvent.SELECTED) ) {
        // User has selected a data type.
        actionPerformedLocIdSelected ();
    }
    refresh();
}

/**
Respond to KeyEvents.
*/
public void keyPressed ( KeyEvent event ) {
	refresh();
}

/**
Need this to properly capture key events, especially deletes.
*/
public void keyReleased ( KeyEvent event ) {
	refresh();
}

public void keyTyped ( KeyEvent event ) {
}

/**
Indicate if the user pressed OK (cancel otherwise).
@return true if the edits were committed, false if the user canceled.
*/
public boolean ok () {
	return __ok;
}

/**
Set the data source choices in response to a new datastore being selected.
@param datastore the datastore to use to determine the data types
*/
private void populateDataSourceChoices ( ZabbixDataStore datastore ) {
	if ( datastore == null ) {
		return;
	}
	// No wildcards since this is used to match a single time series.
	boolean includeWildcards = false;
	// Don't include the SHEF types since they just complicate things.
    List<String> dataSources = datastore.getTimeSeriesDataSourceStrings(getSelectedDataType(),
    	getSelectedDataInterval(), includeWildcards);
    __DataSource_JComboBox.setData ( dataSources );
    // Select the default.
    __DataSource_JComboBox.select(0);
}

/**
Set the data type choices in response to a new datastore being selected.
This should match the main TSTool interface.
@param datastore the datastore to use to determine the data types
*/
private void populateDataTypeChoices ( ZabbixDataStore datastore ) {
	if ( datastore == null ) {
		return;
	}
	//boolean includeWildcards = false;
	boolean includeWildcards = true;
	// Don't include the SHEF types since they just complicate things.
    List<String> dataTypes = datastore.getTimeSeriesDataTypeStrings(getSelectedInterval(), includeWildcards);
    __DataType_JComboBox.setData ( dataTypes );
    // Select the default.
    // TODO smalers 2018-06-21 evaluate whether need datastore method for default.
    __DataType_JComboBox.select(0);
}

/**
Populate the data interval choices in response to a new data type being selected.
This code matches the TSTool main interface code.
*/
private void populateIntervalChoices ( ZabbixDataStore datastore ) {
	String routine = getClass().getSimpleName() + ".populateIntervalChoices";
	String selectedDataType = getSelectedDataType();
    Message.printStatus ( 2, routine, "Populating intervals for selected data type \"" + selectedDataType + "\"" );
	List<String> dataIntervals = null;
	if ( datastore == null ) {
		dataIntervals = new ArrayList<>();
	}
	else {
		//boolean includeWildcards = false;
		boolean includeWildcards = true;
		dataIntervals = datastore.getTimeSeriesDataIntervalStrings(selectedDataType, includeWildcards);
	}
    __Interval_JComboBox.setData ( dataIntervals );
    // Select the first item.
    try {
        __Interval_JComboBox.select ( null ); // To force event.
        __Interval_JComboBox.select ( 0 );
    }
    catch ( Exception e ) {
        // Cases when for some reason no choice is available.
        __Interval_JComboBox.add ( "" );
        __Interval_JComboBox.select ( 0 );
    }
}

/**
Set the location ID choices in response to a new datastore being selected.
The location choices are also in the where filter (for multiple time series)
but a single location is needed when reading a single time series.
@param datastore the datastore to use to determine the data types
*/
private void populateLocIdChoices ( ZabbixDataStore datastore ) {
	if ( datastore == null ) {
		return;
	}
	boolean includeWildcards = false;
	boolean includeNote = true;
    List<String> locIds0 = datastore.getLocIdStrings(
    	getSelectedDataSource(), getSelectedDataType(), getSelectedDataInterval(), includeWildcards, includeNote );
    // Make a copy since adding a blank.
    List<String> locIds = new ArrayList<>();
    for ( String locId : locIds0 ) {
    	locIds.add(locId);
    }
    // Add a blank because multiple time series tab might be used.
    locIds.add(0,"");
    __LocId_JComboBox.setData ( locIds );
    // Select the default.
    __LocId_JComboBox.select(0);
}

/**
Refresh the command string from the dialog contents.
*/
private void refresh () {
	String routine = getClass().getSimpleName() + ".refresh";
	String Alias = "";
	__error_wait = false;
	String DataStore = "";
	String DataType = "";
	String Interval = "";
	String DataSource = "";
	String LocId = "";
	String TsShortName = "";
	//String TSID = "";
	String filterDelim = ";";
	String InputStart = "";
	String InputEnd = "";
	String TimeZone = "";
	String ShiftTrendToIntervalEnd = "";
	String Debug = "";
	PropList props = null;
	if ( __first_time ) {
		__first_time = false;
		// Get the parameters from the command.
		props = __command.getCommandParameters();
	    DataStore = props.getValue ( "DataStore" );
	    DataType = props.getValue ( "DataType" );
	    Interval = props.getValue ( "Interval" );
	    DataSource = props.getValue ( "DataSource" );
	    LocId = props.getValue ( "LocId" );
	    TsShortName = props.getValue ( "TsShortName" );
		Alias = props.getValue ( "Alias" );
		//TSID = props.getValue ( "TSID" );
		InputStart = props.getValue ( "InputStart" );
		InputEnd = props.getValue ( "InputEnd" );
		TimeZone = props.getValue ( "TimeZone" );
		ShiftTrendToIntervalEnd = props.getValue ( "ShiftTrendToIntervalEnd" );
		Debug = props.getValue ( "Debug" );
        // The data store list is set up in initialize() but is selected here.
        if ( JGUIUtil.isSimpleJComboBoxItem(__DataStore_JComboBox, DataStore, JGUIUtil.NONE, null, null ) ) {
            __DataStore_JComboBox.select ( null ); // To ensure that following causes an event.
            __DataStore_JComboBox.select ( DataStore ); // This will trigger getting the DMI for use in the editor.
        }
        else {
            if ( (DataStore == null) || DataStore.equals("") ) {
                // New command...select the default.
                __DataStore_JComboBox.select ( null ); // To ensure that following causes an event.
                if ( __DataStore_JComboBox.getItemCount() > 0 ) {
                	__DataStore_JComboBox.select ( 0 );
                }
            }
            else {
                // Bad user command.
                Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
                  "DataStore parameter \"" + DataStore + "\".  Select a\ndifferent value or Cancel." );
            }
        }
        //
        // Also need to make sure that the input type and DMI are actually selected.
        // Call manually because events are disabled at startup to allow cascade to work properly.
        setDataStoreForSelectedInput();
        // First populate the data type choices.
        populateDataTypeChoices(getSelectedDataStore() );
        // Then set to the value from the command.
        int [] index = new int[1]; // Used to pass back index when a token is processed.
        //Message.printStatus(2,routine,"Checking to see if DataType=\"" + DataType + "\" is a choice.");
        //if ( JGUIUtil.isSimpleJComboBoxItem(__DataType_JComboBox, DataType, JGUIUtil.CHECK_SUBSTRINGS, "-", 0, index, true ) ) {
	    if ( JGUIUtil.isSimpleJComboBoxItem( __DataType_JComboBox, DataType, JGUIUtil.NONE, null, null ) ) {
            // Existing command so select the matching choice.
            //Message.printStatus(2,routine,"DataType=\"" + DataType + "\" was a choice, selecting index " + index[0] + "...");
            //__DataType_JComboBox.select(index[0]);
            __DataType_JComboBox.select(DataType);
        }
        else {
            Message.printStatus(2,routine,"DataType=\"" + DataType + "\" is not a choice.");
            if ( (DataType == null) || DataType.equals("") ) {
                // New command...select the default.
                // Populating the list above selects the default that is appropriate so no need to do here.
            	__DataType_JComboBox.select(0);
            }
            else {
                // Bad user command.
                Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
                  "DataType parameter \"" + DataType + "\".  Select a\ndifferent value or Cancel." );
            	__DataType_JComboBox.select(0);
            }
        }
        // Populate the interval choices based on the selected data type.
        populateIntervalChoices(getSelectedDataStore());
        // Now select what the command had previously (if specified).
        //if ( JGUIUtil.isSimpleJComboBoxItem(__Interval_JComboBox, Interval, JGUIUtil.CHECK_SUBSTRINGS, "-", 1, index, true ) ) {
	    if ( JGUIUtil.isSimpleJComboBoxItem( __Interval_JComboBox, Interval, JGUIUtil.NONE, null, null ) ) {
            //__Interval_JComboBox.select (index[0] );
            __Interval_JComboBox.select (Interval);
        }
        else {
            Message.printStatus(2,routine,"Interval=\"" + Interval + "\" is not a choice.");
            if ( (Interval == null) || Interval.equals("") ) {
                // New command...select the default.
                // Populating the list above selects the default that is appropriate so no need to do here.
            	__Interval_JComboBox.select (0);
            }
            else {
                // Bad user command.
                Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
                  "Interval parameter \"" + Interval + "\".  Select a\ndifferent value or Cancel." );
            	__Interval_JComboBox.select (0);
            }
        }
        // Populate the data source (host group name) choices based on the time series list.
        populateDataSourceChoices(getSelectedDataStore());
        // Now select what the command had previously (if specified).
	    if ( JGUIUtil.isSimpleJComboBoxItem( __DataSource_JComboBox, DataSource, JGUIUtil.NONE, null, null ) ) {
            //__DataSource_JComboBox.select (index[0] );
            __DataSource_JComboBox.select (DataSource);
            __tsInfo_JTabbedPane.setSelectedIndex(0);
        }
        else {
            Message.printStatus(2,routine,"DataSource=\"" + DataSource + "\" is not a choice.");
            if ( (DataSource == null) || DataSource.equals("") ) {
                // New command...select the default.
                // Populating the list above selects the default that is appropriate so no need to do here.
            	__DataSource_JComboBox.select (0);
            }
            else {
                // Bad user command.
                Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
                  "DataSource parameter \"" + DataSource + "\".  Select a\ndifferent value or Cancel." );
            	__DataSource_JComboBox.select (0);
            }
        }
        // Populate the location ID (host) choices based on the time series list.
        populateLocIdChoices(getSelectedDataStore());
        // Now select what the command had previously (if specified):
        // - use the first token in the choice since only the 'host' (not "host - name") is in the parameter
	    //if ( JGUIUtil.isSimpleJComboBoxItem( __LocId_JComboBox, LocId, JGUIUtil.NONE, null, null ) ) {
        if ( JGUIUtil.isSimpleJComboBoxItem(__LocId_JComboBox, LocId, JGUIUtil.CHECK_SUBSTRINGS, "seq: - ", 0, index, true ) ) {
            __LocId_JComboBox.select (index[0] );
            //__LocId_JComboBox.select (LocId);
            __tsInfo_JTabbedPane.setSelectedIndex(0);
        }
        else {
            Message.printStatus(2,routine,"LocId=\"" + LocId + "\" is not a choice.");
            if ( (LocId == null) || LocId.equals("") ) {
                // New command...select the default.
                // Populating the list above selects the default that is appropriate so no need to do here.
            	__LocId_JComboBox.select (0);
            }
            else {
                // Bad user command.
                Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
                  "LocId parameter \"" + LocId + "\".  Select a\ndifferent value or Cancel." );
            	__LocId_JComboBox.select (0);
            }
        }
		// Selecting the data type and interval will result in the corresponding filter group being selected.
		selectInputFilter(getDataStore());
		InputFilter_JPanel filterPanel = getVisibleInputFilterPanel();
		if ( filterPanel == null ) {
			Message.printWarning(1, routine, "Trouble finding visible input filter panel for selected Zabbix datastore." );
		}
		else {
    		int nfg = filterPanel.getNumFilterGroups();
    		String where;
    		for ( int ifg = 0; ifg < nfg; ifg++ ) {
    			where = props.getValue ( "Where" + (ifg + 1) );
    			if ( (where != null) && (where.length() > 0) ) {
    				// Set the filter.
    				try {
    				    Message.printStatus(2,routine,"Setting filter Where" + (ifg + 1) + "=\"" + where + "\" from panel " + filterPanel );
    				    filterPanel.setInputFilter (ifg, where, filterDelim );
    				}
    				catch ( Exception e ) {
    					Message.printWarning ( 1, routine,
    					"Error setting where information using \"" + where + "\"" );
    					Message.printWarning ( 3, routine, e );
    				}
    				if ( !where.startsWith(";") ) {
    					// Select the tab.
    					__tsInfo_JTabbedPane.setSelectedIndex(1);
    				}
    			}
    		}
		    // For some reason the values do not always show up so invalidate the component to force redraw.
		    // TODO SAM 2016-08-20 This still does not work.
    		Message.printStatus(2,routine,"Revalidating component to force redraw.");
		    filterPanel.revalidate();
		    //filterPanel.repaint();
		}
	    if ( Alias != null ) {
		    __Alias_JTextField.setText ( Alias );
	    }
		if ( InputStart != null ) {
			__InputStart_JTextField.setText ( InputStart );
		}
		if ( InputEnd != null ) {
			__InputEnd_JTextField.setText ( InputEnd );
		}
		if ( TimeZone != null ) {
			__TimeZone_JTextField.setText ( TimeZone );
		}
	    if ( JGUIUtil.isSimpleJComboBoxItem( __ShiftTrendToIntervalEnd_JComboBox, ShiftTrendToIntervalEnd, JGUIUtil.NONE, null, null ) ) {
            //__ShiftTrendToIntervalEnd_JComboBox.select (index[0] );
            __ShiftTrendToIntervalEnd_JComboBox.select (ShiftTrendToIntervalEnd);
        }
        else {
            Message.printStatus(2,routine,"ShiftTrendToIntervalEnd=\"" + ShiftTrendToIntervalEnd + "\" is invalid.");
            if ( (ShiftTrendToIntervalEnd == null) || ShiftTrendToIntervalEnd.equals("") ) {
                // New command...select the default.
                // Populating the list above selects the default that is appropriate so no need to do here.
            	__ShiftTrendToIntervalEnd_JComboBox.select (0);
            }
            else {
                // Bad user command.
                Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
                  "ShiftTrendToIntervalEnd parameter \"" + Interval + "\".  Select a\ndifferent value or Cancel." );
            	__ShiftTrendToIntervalEnd_JComboBox.select (0);
            }
        }
	    if ( JGUIUtil.isSimpleJComboBoxItem( __Debug_JComboBox, Debug, JGUIUtil.NONE, null, null ) ) {
            //__Debug_JComboBox.select (index[0] );
            __Debug_JComboBox.select (Debug);
        }
        else {
            Message.printStatus(2,routine,"Debug=\"" + Debug + "\" is invalid.");
            if ( (Debug == null) || Debug.equals("") ) {
                // New command...select the default.
                // Populating the list above selects the default that is appropriate so no need to do here.
            	__Debug_JComboBox.select (0);
            }
            else {
                // Bad user command.
                Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
                  "Debug parameter \"" + Interval + "\".  Select a\ndifferent value or Cancel." );
            	__Debug_JComboBox.select (0);
            }
        }
	}
	// Regardless, reset the command from the fields.
    DataStore = __DataStore_JComboBox.getSelected();
    if ( DataStore == null ) {
        DataStore = "";
    }
	Alias = __Alias_JTextField.getText().trim();
	LocId = getSelectedLocId();
	DataSource = getSelectedDataSource();
    DataType = getSelectedDataType();
    Interval = getSelectedInterval();
	StringBuffer tsid = new StringBuffer();
	tsid.append ( LocId );
	tsid.append ( "." );
	String dataSource = DataSource;
	if ( (dataSource.indexOf("-") >= 0) || (dataSource.indexOf(".") >= 0) || (dataSource.indexOf(" ") > 0) ) {
		dataSource = "'" + dataSource + "'";
	}
	tsid.append ( dataSource );
	tsid.append ( "." );
	String dataType = DataType;
	if ( (dataType.indexOf("-") >= 0) || (dataType.indexOf(".") >= 0) || (dataType.indexOf(" ") > 0) ) {
		dataType = "'" + dataType + "'";
	}
	tsid.append ( dataType );
	tsid.append ( "." );
	if ( (Interval != null) && !Interval.equals("*") ) {
		tsid.append ( Interval );
	}
	tsid.append ( "~" + getInputNameForTSID() );
	__TSID_JTextField.setText ( tsid.toString() );
	// Regardless, reset the command from the fields.
	props = new PropList ( __command.getCommandName() );
    props.add ( "DataStore=" + DataStore );
	//props.add ( "TSID=" + TSID );
	if ( (LocId != null) && !LocId.isEmpty() ) {
		props.add ( "LocId=" + LocId );
	}
	if ( (TsShortName != null) && !TsShortName.isEmpty() ) {
		props.add ( "TsShortName=" + TsShortName );
	}
	if ( (DataType != null) && !DataType.isEmpty() ) {
		props.add ( "DataType=" + DataType );
	}
	if ( (Interval != null) && !Interval.isEmpty() ) {
		props.add ( "Interval=" + Interval );
	}
	// Set the where clauses.
	// Since numbers may cause problems, first unset and then set.
	InputFilter_JPanel filterPanel = getVisibleInputFilterPanel();
	if ( filterPanel != null ) {
    	int nfg = filterPanel.getNumFilterGroups();
        //Message.printStatus(2,routine,"Input filter panel has " + nfg + " filter groups.");
    	String where;
    	for ( int ifg = 0; ifg < nfg; ifg ++ ) {
    		// Use the internal value for the where to ensure integration.
    		where = filterPanel.toString(ifg,filterDelim,3).trim();
    		// Make sure there is a field that is being checked in a where clause:
    		// - otherwise, unset the where if blank
    		props.unSet("Where" + (ifg + 1) );
    		if ( (where.length() > 0) && !where.startsWith(filterDelim) ) {
                // FIXME SAM 2010-11-01 The following discards '=' in the quoted string.
                //props.add ( "Where" + (ifg + 1) + "=" + where );
                props.set ( "Where" + (ifg + 1), where );
                //Message.printStatus(2,routine,"Setting command parameter from visible input filter:  Where" +
                //    (ifg + 1) + "=\"" + where + "\"" );
    		}
    		else {
                //Message.printStatus(2,routine,"Visible input filter:  Where" + (ifg + 1) + " is set to blank, "
               	//	+ "where=" + where + " where.length()=" + where.length() + " filterDelim=" + filterDelim );
    		}
    	}
	}
	else {
		//Message.printStatus(2, routine, "Visible input filter panel is null.");
	}
	props.add ( "Alias=" + Alias );
	InputStart = __InputStart_JTextField.getText().trim();
	props.add ( "InputStart=" + InputStart );
	InputEnd = __InputEnd_JTextField.getText().trim();
	props.add ( "InputEnd=" + InputEnd );
	TimeZone = __TimeZone_JTextField.getText().trim();
	props.add ( "TimeZone=" + TimeZone );
	Debug = __Debug_JComboBox.getSelected();
	props.add ( "Debug=" + Debug );
	__command_JTextArea.setText( __command.toString ( props ).trim() );

	// Check the GUI state to determine whether some controls should be disabled.

	checkGUIState();
}

/**
React to the user response.
@param ok if false, then the edit is canceled.
If true, the edit is committed and the dialog is closed.
*/
private void response ( boolean ok ) {
	__ok = ok;	// Save to be returned by ok().
	if ( ok ) {
		// Commit the changes.
		commitEdits ();
		if ( __error_wait ) {
			// Not ready to close.
			return;
		}
	}
	// Now close out...
	setVisible( false );
	dispose();
}

/**
Select (set visible) the appropriate input filter based on the other data choices.
For Zabbix there is currently only one input filter per datastore.
@param dataStore the data store from the DataStore and InputName parameters.
*/
private void selectInputFilter ( ZabbixDataStore dataStore ) {
	String routine = getClass().getSimpleName() + ".selectInputFilter";
    // Selected datastore name.
    if ( dataStore == null ) {
        return;
    }
    String dataStoreName = dataStore.getName();
    // Selected data type and interval must be converted to Zabbix internal convention.
    // The following lookups are currently hard coded and not read from Zabbix.
    String selectedDataType = getSelectedDataType();
    String selectedTimeStep = __Interval_JComboBox.getSelected();
    //List<InputFilter_JPanel> inputFilterJPanelList = getInputFilterJPanelList();
    List<Zabbix_TimeSeries_InputFilter_JPanel> inputFilterJPanelList = getInputFilterJPanelList();
    // Loop through all available input filters and match the data store name, type (whether legacy or new design),
    // and filter for the data type.  If matched, set to visible and otherwise not visible.
    boolean matched;
    int matchCount = 0;
    Message.printStatus(2, routine, "Trying to set visible the input filter given selected datastore name \"" + dataStoreName +
        "\" selectedDataType=\"" + selectedDataType + "\" selectedTimeStep=\"" + selectedTimeStep + "\"" );
    for ( InputFilter_JPanel panel : inputFilterJPanelList ) {
        matched = false; // Does selected datastore name match the filter datastore?
        ZabbixDataStore datastore =
            ((Zabbix_TimeSeries_InputFilter_JPanel)panel).getDataStore();
        if ( (datastore != null) && datastore.getName().equalsIgnoreCase(dataStoreName) ) {
            // Have a match in the datastore name so return the panel.
            matched = true;
        }
        // If the panel was matched, set it visible.
        panel.setVisible(matched);
        if ( matched ) {
            ++matchCount;
        }
    }
    // No normal panels were matched enable the generic panel, which will be last panel in list.
    InputFilter_JPanel defaultPanel = inputFilterJPanelList.get(inputFilterJPanelList.size() - 1);
    if ( matchCount == 0 ) {
        defaultPanel.setVisible(true);
        Message.printStatus(2, routine, "Setting default input filter panel visible.");
    }
    else {
        defaultPanel.setVisible(false);
    }
}

/**
Set all the filters visible, necessary to help compute layout dimensions and dialog size.
*/
private void setAllFiltersVisible() {
    //List<InputFilter_JPanel> panelList = getInputFilterJPanelList();
    List<Zabbix_TimeSeries_InputFilter_JPanel> panelList = getInputFilterJPanelList();
    for ( InputFilter_JPanel panel : panelList ) {
        panel.setVisible(true);
    }
}

/**
Set the datastore to use for queries based on the selected data store and input name.
*/
private void setDataStoreForSelectedInput() {
    // Data store will be used if set.  Otherwise input name is used.
    String dataStoreString = __DataStore_JComboBox.getSelected();
    if ( dataStoreString == null ) {
        dataStoreString = "";
    }
    if ( !dataStoreString.equals("") ) {
        // Use the selected datastore.
        __dataStore = getSelectedDataStore();
    }
}

/**
Responds to WindowEvents.
@param event WindowEvent object
*/
public void windowClosing( WindowEvent event ) {
	response ( false );
}

public void windowActivated( WindowEvent evt ) {
}

public void windowClosed( WindowEvent evt ) {
}

public void windowDeactivated( WindowEvent evt ) {
}

public void windowDeiconified( WindowEvent evt ) {
}

public void windowIconified( WindowEvent evt ) {
}

public void windowOpened( WindowEvent evt ) {
}

}
