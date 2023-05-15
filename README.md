# owf-tstool-zabbix-plugin #

**This plugin is under development and has not been released for production work.**

This repository contains the Open Water Foundation TSTool Zabbix plugin.
This plugin can be installed to enable commands that integrate TSTool with Zabbix.

TSTool is part of [Colorado's Decision Support Systems (CDSS)](https://www.colorado.gov/cdss).
See the following online resources:

*   [TSTool Zabbix Plugin download page](https://software.openwaterfoundation.org/tstool-zabbix-plugin/)
*   [TSTool Zabbix Plugin Documentation](https://software.openwaterfoundation.org/tstool-zabbix-plugin/latest/doc-user/)
*   [TSTool User Documentation](https://opencdss.state.co.us/tstool/latest/doc-user/)
*   [TSTool Developer Documentation](https://opencdss.state.co.us/tstool/latest/doc-dev/)

See the following sections in this page:

*   [Repository Folder Structure](#repository-folder-structure)
*   [Repository Dependencies](#repository-dependencies)
*   [Adding to TSTool Eclipse Workspace](#adding-to-tstool-eclipse-workspace)
*   [Building the Plugin Jar File](#building-the-plugin-jar-file)
*   [Building an Installer](#building-an-installer)
*   [Contributing](#contributing)
*   [License](#license)
*   [Contact](#contact)

-----

## Repository Folder Structure ##

The following are the main folders and files in this repository, listed alphabetically.

```
C:\Users\user\                      User's files on windows.
/c/Users/user/                      User's files in Git bash.
  cdss-dev/                         Recommended folder for CDSS product development.
    TSTool/                         Recommended folder for TSTool development.
      git-repos/                    Recommended folder for TSTool product repositories.
        owf-tstool-zabbix-plugin/   Repository working files.
        .gitattributes              Git configuration file for repository.
        .gitignore                  Git configuration file for repository.
        build-util/                 Utilities used in the build process.
        dist/                       Folder containing software installers.
        doc-init/                   Documentation for project initialization.
        doc-user-mkdocs-project/    MkDocs project for user documentation.
        owf-tstool-zabbix-plugin/   Maven project source code and supporting files.
          .classpath                Eclipse configuration file.
          .project                  Eclipse configuration file.
          .settings/                Eclipse settings for developer.
          src/                      Plugin source code.
          target/                   Compiled code.
        README.md                   This file.
        test/                       End to end tests for commands.
```

## Repository Dependencies ##

This project is configured using Maven, which manages third-party dependencies.

Additionally, the following table lists Eclipse project dependencies that are not managed by Maven.

|**Repository**|**Description**|
|------------------------------------------------------------------------------------------|----------------------------------------------------|
|[`cdss-lib-common-java`](https://github.com/OpenCDSS/cdss-lib-common-java)                |Library of core utility code used by multiple TSTool repositories (projects).|
|[`cdss-lib-processor-ts-java`](https://github.com/OpenCDSS/cdss-lib-processor-ts-java)    |Library containing processor code for TSTool commands.|

## Adding to TSTool Eclipse Workspace ##

This section explains how to add the TSTool Zabbix plugin to the TSTool core development environment.

1.  If not already installed, install the
    [Maven command line tools](https://maven.apache.org/install),
    which are used by scripts outside of Eclipse's built-in Maven tools.
    Copy the unzipped file to `C:\Program Files\Maven\apache-maven-3.8.6` (or similar).
2.  Start by setting up TSTool according to the
    [TSTool documentation for developers](https://opencdss.state.co.us/tstool/latest/doc-dev/).
3.  Clone this repository similar to other TSTool repositories.
4.  In Eclipse, use the ***File / Import...*** menu to display the ***Import*** dialog.
5.  Select the ****Maven / Existing Maven Projects*** choice and then ***Next***.
6.  Browse to the `cdss-dev/TSTool/git-repos/owf-tstool-zabbix-plugin/owf-tstool-zabbix-plugin` folder
    and ***Select Folder***.
    The folders are redundant because one is for the repository and one is the Maven project.
7.  The ***Maven Projects*** dialog will be shown with the `pom.xml` file selected for this repository.
    Press ***Finish***.
8.  The project will be shown in the ***Package Explorer*** and ***Project Explorer*** views,
    with errors indicated because dependencies have not been configured.
9.  Right click on `owf-tstool-zabbix-plugin` in ***Package Explorer*** or ***Project Explorer***
    and select ***Build Path / Configure Build Path***.
    The Jar file and Maven libraries will already be selected from the project configuration file.
    In the ***Projects*** tab, the project dependencies should also be configured.
10. The project may have compile errors, for example because Eclipse is trying to use an older version of Java
    compliance by default, which is incompatible with current code..
    Right click on the project and select ***Properties***.
    The select ***Java Compiler***.
    If necessary, uncheck the ***Enable project specific settings***
    and press ***Apply and Close***.
    This will cause the project to use the workspace configuration consistent with the core software.
11. Remember to run the `build-util/0-create-plugin-jar.bash` script after the initial compile
    and other code changes, which will build and copy the plugin Jar file to the user's TSTool files.
    Then when TSTool is run from Eclipse, the plugins will be found in the normal location and will be loaded for testing.

## Building the Plugin Jar File ##

Plugin Eclipse projects are not part of the "built in" TSTool code.
During development, plugins are handled as follows:

1.  Eclipse project:
    1.  The `owf-tstool-zabbix-plugin` repository is added as a Maven project.
    2.  The project's build path is configured to use appropriate Maven dependencies,
        such as the AWS libraries and other TSTool projects, such as `cdss-lib-common-java`.
2.  Plugin jar file:
    1.  The plugin is recognized by TSTool via the plugin design, not the Eclipse build path.
        In other words, plugins are handled as per the production code.
    2.  Therefore, run the `build-util/0-create-plugin-jar.bash` script to create the
        plugin `jar` file in the user's `.tstool/NN/plugins` folder in order to test the plugin.
    3.  The above script relies on the `owf-tstool-zabbix-plugin/src/main/resources/META-INF/MANIFEST.MF`
        file to provide information about the plugin to TSTool,
        including the list of plugin commands and third-party `jar` files that are used by the plugin.

## Building an Installer ##

The plugin is currently distributed on Windows using a zip file,
using the following procedure:

1.  Update the version:
    1.  Update the `src/main/java/owf-tstool-zabbix-plugin/org/openwaterfoundation/tstool/plugin/aws/PluginMetadata.java` source file.
    2.  The version is extracted by scripts below to use in the documentation location and zip file name,
        which results in versioned installers being listed in the product landing page.
2.  Update the documentation:
    1.  Update the documentation to be current and include release notes consistent with the version.
    2.  Update the `index.md` file to be consistent with the version.
    3.  Upload to S3 by running `doc-user-mkdocs-project/build-util/copy-to-owf-amazon-s3.bash`.
3.  Create the installer:
    1.  Run the `build-util/0-create-plugin-jar.bash` script to create the plugin jar file.
        Confirm that there is only one jar file version in the plugins folder
        (the script will warn if more than one java file exists).
    2.  Run the `build-util/1-create-installer.bash` script to create the installer.
    3.  Run the `build-util/2-copy-to-owf-amazon-s3.bash` script.
        This will upload the local zip file to OWF's S3 bucket.
        The script prompts as to whether to update the product landing page `index.html`.
    4.  If not run from the above script,
        run the `build-util/3-create-s3-index.bash` script to create the plugin landing page.

## Contributing ##

Contributions to this project can be submitted using the following options:

1.  TSTool software developers with commit privileges can write to this repository.
2.  Post an issue on GitHub with suggested change.
3.  Fork the repository, make changes, and do a pull request.
    Contents of the current master branch should be merged with the fork to minimize
    code review before committing the pull request.

See also the [OpenCDSS / TSTool protocols](https://opencdss.state.co.us/opencdss/tstool/)

## License ##

Copyright Open Water Foundation.

The software is licensed under GPL v3+. See the [LICENSE.md](LICENSE.md) file.

## Contact ##

Steve Malers, @smalers, steve.malers@openwaterfoundation.org.
