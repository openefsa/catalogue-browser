package catalogue_browser_dao;

import java.io.File;

public class ExportCatalogueFileMock {

	public File exportLastInternalVersion() {
		File file = new File("test-files/lastInternalVersion.xml");
		return file;
	}
}
