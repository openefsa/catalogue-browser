<p align="center">
	<img src="http://www.efsa.europa.eu/profiles/efsa/themes/responsive_efsa/logo.png" alt="European Food Safety Authority"/>
</p>

# Catalogue browser
The catalogue browser is a Java-based application that allows to use and browse catalogues released by EFSA in the Data Collection Framework. More specifically, the application allows the user to download one or more catalogues of interest from those available on the DCF and then be able to consult the data within it using the graphical interface. In addition, the tool allows also the encoding of terms, present within the database, in alphanumeric codes to which facets (descriptive terms) can be concatenated.

<p align="center">
    <img src="src/main/resources/icons/Catalogue-browser.gif" alt="CatalogueBrowser icon"/>
</p>

## Dependencies
All project dependencies are listed in the [pom.xml](https://github.com/openefsa/catalogue-browser/blob/master/pom.xml) file.

## Import the project
In order to import the project correctly into the integrated development environment (e.g. Eclipse), it is necessary to download the Catalogue browser together with all its dependencies.
The Catalogue browser and all its dependencies are based on the concept of "project object model" and hence Apache Maven is used for the specific purpose.
In order to correctly import the project into the IDE it is firstly required to download or clone all the required dependencies as stated in the list below:

	<dependencies>
		<!-- catalogue browser dependencies -->
		<dependency>catalogue-browser</dependency>
		<dependency>catalogue-xml-to-xlsx</dependency>
		<dependency>open-xml-reader</dependency>
		<dependency>dcf-webservice-framework</dependency>
		<dependency>exceptions-manager</dependency>
		<dependency>http-manager</dependency>
		<dependency>http-manager-gui</dependency>
		<dependency>progress-bar</dependency>
		<dependency>sql-script-executor</dependency>
		<dependency>version-manager</dependency>
		<dependency>window-size-save-restore</dependency>
		<dependency>zip-manager</dependency>
	</dependencies>
	
Next, extract all the zip packages inside your workspace. At this stage you can simply open the IDE and import all projects available in the workspace.


### Notes for developers
Please note that the "business_rules" and the "config" folders are used by the tool and hence errors occur if missing.

