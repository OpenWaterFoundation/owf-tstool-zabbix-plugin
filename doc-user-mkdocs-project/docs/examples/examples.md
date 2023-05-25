# TSTool / Examples of Use #

This chapter provides examples for using TSTool with Zabbix.
See also the examples provided in TSTool documentation.

This documentation is incomplete and will be filled out as
workflows are implemented for Zabbix data processing.

-------------

## Quality Control Examples ##

TSTool can be used to implement automated quality control.

### Checking for Bad Data ###

See the
[`CheckTimeSeries`](https://opencdss.state.co.us/tstool/latest/doc-user/command-ref/CheckTimeSeries/CheckTimeSeries/) command.

### Comparing Data in Two Systems ###

The
[`CompareTimeSeries`](https://opencdss.state.co.us/tstool/latest/doc-user/command-ref/CompareTimeSeries/CompareTimeSeries/)
command can be used to compare time series from two data sources,
for example to ensure that data sharing is working as expected.

### Validating Software and Workflows ###

TSTool can be used to validate software and workflows.
It is always good to compare the results or two programs that are performing the same task,
and to validate a workflow with test data.
See the [TSTool Quality Control](https://opencdss.state.co.us/tstool/latest/doc-user/quality-control/quality-control/) documentation.

## Creating Information Products ##

TSTool can be run on schedule to create products with the
[`ProcessTSProduct`](https://opencdss.state.co.us/tstool/latest/doc-user/command-ref/ProcessTSProduct/ProcessTSProduct/)
and other commands.
