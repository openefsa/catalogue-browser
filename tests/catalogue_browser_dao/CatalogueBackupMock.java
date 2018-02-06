package catalogue_browser_dao;

import java.sql.SQLException;

import catalogue.Catalogue;

public class CatalogueBackupMock implements ICatalogueBackup {
	
	@Override
	public void backupCatalogue(Catalogue catalogue, String path) throws SQLException {
		System.out.println("Catalogue=" + catalogue + " backup created at=" + path);
	}

}
