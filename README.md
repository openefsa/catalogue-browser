![European Food Safety Authority](http://www.efsa.europa.eu/profiles/efsa/themes/responsive_efsa/logo.png)

# Catalogue browser
The catalogue browser is a Java-based application that allows the use and reading of the catalogues released by EFSA in the Data Collection Framework. More specifically, the application allows the user to download one or more catalogues of interest from those available on the DCF and then be able to consult the data within it using the graphical interface. In addition, the tool allows also the encoding of terms, present within the database, in alphanumeric codes to which facets (descriptive terms) can be concatenated.

<p align="center">
    <img src="src/main/resources/icons/Catalogue-browser.gif" alt="CatalogueBrowser icon"/>
</p>

# Dependencies
All project dependencies are listed in the [pom.xml](https://github.com/openefsa/catalogue-browser/blob/master/pom.xml) file.

## Import the project
In order to import the project correctly into the integrated development environment (e.g. Eclipse), it is necessary to download the Catalogue browser together with all its dependencies.
The Catalogue browser and all its dependencies are based on the concept of "project object model" and hence Apache Maven is used for the specific purpose.
In order to correctly import the project into the IDE it is firstly required to create a parent POM Maven project (check the following [link](https://maven.apache.org/guides/introduction/introduction-to-the-pom.html) for further information). 
Once the parent project has been created add the project and all the dependencies as "modules" into the pom.xml file as shown below: 

	<modules>

		<!-- catalogue browser modules -->
		<module>catalogue-browser</module>
		<module>catalogue-xml-to-xlsx</module>
		<module>open-xml-reader</module>
		<module>dcf-webservice-framework</module>
		<module>exceptions-manager</module>
		<module>http-manager</module>
		<module>http-manager-gui</module>
		<module>progress-bar</module>
		<module>sql-script-executor</module>
		<module>version-manager</module>
		<module>window-size-save-restore</module>
		<module>zip-manager</module>
		
	</modules>
	
Next, close the IDE and extract all the zip packets inside the parent project.
At this stage you can simply open the IDE and import back the parent project which will automatically import also the Catalogue browser and all its dependencies.

_Please note that the "SWT.jar" and the "Jface.jar" libraries must be downloaded and installed manually in the Maven local repository since are custom versions used in the tool ((install 3rd party jars)[https://maven.apache.org/guides/mini/guide-3rd-party-jars-local.html]). 
Download the exact version by checking the Catalogue browser pom.xml file._

### Notes for developers
Please note that the "business_rules" and the "config" folders are used by the tool and hence errors occur if missing.

