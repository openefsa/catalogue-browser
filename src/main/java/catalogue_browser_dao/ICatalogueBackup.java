package catalogue_browser_dao;

import java.sql.SQLException;

import catalogue.Catalogue;

public interface ICatalogueBackup {
	
	/**
	 * Backup the catalogue into a new database
	 * @param catalogue
	 * @param path
	 */
	public void backupCatalogue(Catalogue catalogue, String path) throws SQLException;
}
