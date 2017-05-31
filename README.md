# Audit Trail Module
This module creates a log history for changes made to objects in your Mendix application.

## Typical Usage Scenario
Keep track of when, by who, how much and what changes are made in your Mendix application.

## Features

* Keep track of objects that are created, changed or deleted.
* Only saves changes made on an object that has the AuditTrail.AudittrailSuperClass as a superclass and where the commit is done with events (this could be a performance risk with to many entities or records.
* Add a commit event which analysis and save the changes
* Log the complete state of the entity (all attributes) or only the changed attributes bt modifying a constant

## Contributing
For more information on contributing to this repository visit [Contributing to a GitHub repository](https://docs.mendix.com/howto/collaboration-project-management/contribute-to-a-github-repository)!

## Configuration

There are two possible ways of configuring the audi trail module.

1. Add events and an association
2. Use a generalization for the entity to 'audit' and let the entity inherit from 'AuditTrail.AudittrailSuperClass' 
    
Changing the audit behavior is easily done by altering the default values of the entity: Configuration

The following settings are configurable
   
* IncludeOnlyChangedAttributes [False]: Determines if the module should create a LogLine for every single Member every time it initiates the Audit, or should only the Changed Members be logged.
* LogAllMembersOnCreate [True]: This property only effects the scenario when "IncludeOnlyChangedAttributes" = False. This indicates if the application will create a LogLine for all attributes when the record is created (regardless if the value changed).  
* IncludeCalculatedAttributes [False]: Should the module resolve calculated attributes and compare if the result has changed since the last commit.
* CreateLogObjectWithoutMemberChanges [False]: If non of the Members have been changed do you want to have a Log record with only the fields changeddate and a changed by populated.
* LogLineDateFormat [MM/dd/yyyy]: When auditing date fields the module will format the date as a string. This determines the notation for all the dates in the audit trail. This uses the same tokens as in microflow
* LogServerTimeZoneDateNotation [True]: Should the date be audited in the 'Server Timezone'. If both the Session timezone and Server timezone are enabled you will see two dates in the audit overview.
* ServerTimeZone [Etc/UTC]: The timezone in which the Server Timezone is printed. This will be a static timezone and should match the notation as used in Java. For example: "http://stackoverflow.com/questions/1694885/timezones-in-java"
* LogSessionTimeZoneDateNotation [True]: Should the date be audited in the Session timezone of the user that makes the change. If both the session timezone and UTC timezone are enabled you will see two dates in the audit overview.
 
### Association & events:
Add a reference set association from your entity to the 'AuditTrail.Log' entity or an association from the 'AuditTrail.Log' entity to the entity that needs to be audited. 
Add en After Create, Before Commit and Before Delete event to your entity identical to the events on the AudittrailSuperClass. 
The Java actions will automatically create the log item and all required loglines based on the changes
Configure the constant: 'LogOnlyChangedAttributes' whether or not you want to log all attributes or just the changes
- Add the snippet "LogOverviewSnippet' to a page in a custom module.
 

### Inheritance:

All objects that you would like to log need to have the AuditTrail.AudittrailSuperClass as a superclass except the subclasses of the System.User object.
The module automatically logs all changes on the create, commit and delete event.
Configure the constant: 'LogOnlyChangedAttributes' whether or not you want to log all attributes or just the changes
- Add the snippet "LogOverviewSnippet' to a page in a custom module.
