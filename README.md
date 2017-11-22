# CatalogueBrowser
The catalogue browser is a complex Java application which communicates with the EFSA data collection framework (DCF) to manage EFSA catalogues.
In particular, it is possible to download, browse and edit the catalogues contents through a simple user interface (editing is only enabled for authorized users).
The entire application is based on an SQL database, which stores local copies of catalogues.

The data provider user manual can be found [here](https://dms.efsa.europa.eu/otcs/cs.exe?func=ll&objaction=overview&objid=18516413).

### Notes for developers
Do NOT change the business_rules package name, since the Batch checking tool uses it to make the checks.
