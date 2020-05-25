package catalogue_browser_dao;

import java.io.File;

public class ExportCatalogueFileMock {

	public File exportLastInternalVersion() {
		return new File(getClass().getClassLoader().getResource("/resources/lastInternalVersion.xml").getFile());
	}
}
