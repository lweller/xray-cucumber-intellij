# Xray Cucumber Plugin

Allows handling cucumber tests stored in Xray for Jira (e.g. download for edition or execution and upload after
modification).

## Quick Start

Create a `xray-cucumber.json` in package where normally cucumber `.feature` files reside with the following content:

```
{
  "url": "<Base Jira URL, e.g. https://issues.example.com>",
  "projectKey": "<Prefix of ID's used in the Jira project>",
  "filterId": <ID of a Jira filter returning some Xray cucumber tests>
}
```

Now when opening context menu of freshly created `xray-cucumber.json` file (right click), choose _Downloading Cucumber
Xray Tests from Jira_. After entering the username and password, the download of `.feature`
files is started and finally stored along with `xray-cucumber.json` file.

Additionally, opening the context menu of a `xray-cucumber.json` file (right click) and choosing _Upload Cucumber Xray Tests to Jira_
will generate a zip archive of all `.feature` files within the same directory as the `xray-cucumber.json` file. After 
authenticating, this option can be used to update existing tests and pre-conditions that already exist in Jira, or (if no matching existing
tests are found) will create new tests.

## Configuration

The operations of the plugin can be configured with a `xray-cucumber.json` file. This file may reside at any location
within the project tree, but should principally be placed at location where normally the cucumber `.feature` files would
be placed.

It may contain the following parameters:

|Parameter                 | Description                                                                                                      | Default Value                                                                                                      |
|--------------------------|------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------|
| url                      | URL to Jira<br> e.g. if your url for accessing issues is something like <br> `https://issues.example.com/issues`, <br> then you need to set this base url to <br> `https://issues.example.com`| |                                                                                                      |                                                                                                                    |
| username                 | username of an user allow to access Jira                                                                         | should remain undefined, so that the credentials are loaded from password store of IntelliJ or requested from user | 
| password                 | password of an user allow to access Jira                                                                         | should remain undefined, so that the credentials are loaded from password store of IntelliJ or requested from user |
| projectKey               | Jira project key to use when uploading new tests                                                                 |                                                                                                                    |
| filterId                 | Jira filter iD to use for Xray cucumber test selection                                                           |                                                                                                                    |
| fileReplacementBehaviour | Behaviour when downloading a file that is already existing locally. Possible values: KEEP_EXISTING, REPLACE, ASK | ASK                                                                                                                |


## Functions

### Download Xray cucumber tests as feature files from Jira

With the command _Downloading Cucumber Xray Tests from Jira_ in context menu of `xray-cucumber.json` file, the Xray
cucumber tests selected by the configured Jira filter wil be downloaded and stored along with `xray-cucumber.json`
file. They can then be executed and modified locally.

By default, locally existing files will not be overridden during download, so that any local changes won't get lost. 

### Upload modified feature files back to Jira (added in v1.1.0)

With the command _Upload Cucumber Tests to Jira_ in context menu of a `.feature` file, a locally modified test can be 
uploaded to Jira. If some scenarios or background clauses within the `.feature` file not exist in Jira (i.e. they don't
have any annotation with the corresponding Jira ID), new issues are created using the configured project key.

Refer to [Xray's documentation](https://docs.getxray.app/display/XRAY/Importing+Cucumber+Tests+-+REST "Xray - Importing Cucumber Tests - REST v1.0") for best practices and rules for updating existing tests and pre-conditions.

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)