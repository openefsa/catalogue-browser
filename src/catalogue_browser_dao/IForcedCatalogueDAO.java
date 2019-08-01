package catalogue_browser_dao;

import catalogue.Catalogue;
import soap.UploadCatalogueFileImpl.ReserveLevel;

public interface IForcedCatalogueDAO {

	public boolean forceEditing(Catalogue catalogue, String username, ReserveLevel editLevel);

	public ReserveLevel getEditingLevel(Catalogue catalogue, String username);

	public boolean removeForceEditing(Catalogue catalogue);
}
