package catalogue_browser_dao;

import catalogue.Catalogue;
import soap.UploadCatalogueFileImpl.ReserveLevel;

public interface IForcedCatalogueDAO {

	public boolean forceEditing(Catalogue catalogue, String username, ReserveLevel editLevel);
<<<<<<< HEAD

	public ReserveLevel getEditingLevel(Catalogue catalogue, String username);

=======
	public ReserveLevel getEditingLevel(Catalogue catalogue, String username);
>>>>>>> 574ffe363e78d250cf6350ff4ea89f2f48352380
	public boolean removeForceEditing(Catalogue catalogue);
}
