#!/bin/sh
(set -o igncr) 2>/dev/null && set -o igncr; # this comment is required
# The above line ensures that the script can be run on Cygwin/Linux even with Windows CRNL
#
# git-tag-product - tag product repository
# - this script calls the general git utilities script

# Echo to stderr
# - if necessary, quote the string to be printed
# - this function is called to print various message types
echoStderr() {
  echo "$@" 1>&2
}

# Get the plugin version (e.g., 1.2.0):
# - the version is printed to stdout so assign function output to a variable
getPluginVersion() {
  local srcFile

  # Maven folder structure results in duplicate 'owf-tstool-zabbix-plugin'?
  # TODO smalers 2022-05-19 need to enable this.
  srcFile="${repoFolder}/owf-tstool-zabbix-plugin/src/main/java/org/openwaterfoundation/tstool/plugin/zabbix/PluginMeta.java"
  # Get the version from the code
  # line looks like:
  #  public static final String VERSION = "1.0.0 (2022-05-27)";
  if [ -f "${srcFile}" ]; then
    cat ${srcFile} | grep 'VERSION =' | cut -d '"' -f 2 | cut -d ' ' -f 1 | tr -d '"' | tr -d ' '
  else
    # Don't echo error to stdout.
    echoStderr "[ERROR] Source file with version does not exist:"
    echoStderr "[ERROR]   ${srcFile}"
    cat ""
  fi
}

# Entry point into the script.

# Get the location where this script is located since it may have been run from any folder
scriptFolder=$(cd $(dirname "$0") && pwd)

# Git utilities folder is relative to the user's files in a standard development files location
# - determine based on location relative to the script folder
# Specific repository folder for this repository
repoHome=$(dirname ${scriptFolder})
# Want the parent folder to the specific Git repository folder
gitReposHome=$(dirname ${repoHome})

# Product repository.
mainRepo="owf-tstool-zabbix-plugin"
mainRepoFolder="${repoHome}"
repoFolder="${repoHome}"

# Echo information.
echo "repoHome=${repoHome}"
echo "repoFolder=${repoFolder}"
echo "gitReposHome=${gitReposHome}"
echo "mainRepo=${mainRepo}"
echo "mainRepoFolder=${mainRepoFolder}"

# Determine the version from the software product
# - code line looks like:
#   public static final String PROGRAM_VERSION = "12.07.00 (2018-09-19)";
# - this is used as information to help the user specify an intelligent tag name and commit message
# - grep -m 1 means stop after first occurrence
productVersion=$(getPluginVersion)
productName="tstool-zabbix-plugin"

# Run the generic utility script
${scriptFolder}/git-util/git-tag-all.sh -m "${mainRepo}" -g "${gitReposHome}" -N "${productName}" -V "${productVersion}" $@
