# Template product configuration file for memory utilization:
# - must expand before using by providing a table named TSID_List with TSID column

[Product]

ProductType = "Graph"
TotalWidth = "1500"
TotalHeight = "800"

[SubProduct 1]

GraphType = "Raster"
MainTitleString = "NovaStar Systems CPU Utilization (Hourly Maximum)"
LeftYAxisLegendPosition = "Left"
LegendFormat = "%S: %L"
LeftYAxisIgnoreUnits = "true"
RightYAxisGraphType = "None"
RightYAxisIgnoreUnits = "false"
# Location of the symbol table is the parent since the expanded product is in a subfolder.
SymbolTablePath = "../example-heatmap-cpu-utilization-symtable.csv"

<#list TSID_List as tsid>
<#assign tsid_index1 = tsid_index + 1>
[Data 1.${tsid_index1}]

TSID = "${tsid}"

</#list>
