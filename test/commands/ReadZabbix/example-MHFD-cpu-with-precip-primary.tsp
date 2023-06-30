[Product]

ProductType = "Graph"
TotalWidth = "900"
TotalHeight = "600"

[SubProduct 1]

GraphType = "Bar"
BarPosition = "LeftOfDate"
MainTitleString = "Carr Street Hourly Precipitation"

[Data 1.1]

TSID = "100.NovaStar5.Precip-Total.1Hour"
Color = "Blue"

[SubProduct 2]

GraphType = "Line"
RightYAxisGraphType = "None"
RightYAxisIgnoreUnits = "false"
MainTitleString = "MHFD Primary Node CPU Utilization"

[Data 2.1]

TSID = "ud-ns518.Clients/CO-District-MHFD.'CPU: utilization-Max'.Hour"
Color = "Red"

[Data 2.2]

TSID = "ud-ns518.Clients/CO-District-MHFD.'CPU: utilization-Avg'.Hour"
Color = "DarkGray"

[Data 2.3]

TSID = "ud-ns518.Clients/CO-District-MHFD.'CPU: utilization-Min'.Hour"

[Data 2.4]

#TSID = "ud-ns518.Clients/CO-District-MHFD.'CPU: utilization'.IrregSecond"
#Color = "Blue"
