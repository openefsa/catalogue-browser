package catalogue_browser_dao;

import java.sql.SQLException;

import catalogue.Catalogue;

public class CatalogueBackup implements ICatalogueBackup {

	@Override
	public void backupCatalogue(Catalogue catalogue, String path) throws SQLException {
		DatabaseManager.backupCatalogue(catalogue, path);
	}
}
