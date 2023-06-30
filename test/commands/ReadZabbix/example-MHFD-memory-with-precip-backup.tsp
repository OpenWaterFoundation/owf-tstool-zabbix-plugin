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
MainTitleString = "MHFD Backup Node Memory Utilization"

[Data 2.1]

TSID = "ud-ns518b.Clients/CO-District-MHFD.'Memory: Utilization-Max'.Hour"
Color = "Red"

[Data 2.2]

TSID = "ud-ns518b.Clients/CO-District-MHFD.'Memory: Utilization-Avg'.Hour"
Color = "DarkGray"

[Data 2.3]

TSID = "ud-ns518b.Clients/CO-District-MHFD.'Memory: Utilization-Min'.Hour"

[Data 2.4]

#TSID = "ud-ns518b.Clients/CO-District-MHFD.'Memory: Utilization'.IrregSecond"
#Color = "Blue"
