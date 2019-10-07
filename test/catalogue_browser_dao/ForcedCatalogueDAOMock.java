package catalogue_browser_dao;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import catalogue.Catalogue;
import soap.UploadCatalogueFileImpl.ReserveLevel;

public class ForcedCatalogueDAOMock implements IForcedCatalogueDAO {

	private Collection<ForcedCatalogue> database;
	
	public ForcedCatalogueDAOMock() {
		this.database = new ArrayList<>();
	}
	
	private class ForcedCatalogue {
		
		Catalogue catalogue;
		String username;
		ReserveLevel editLevel;
		
		public ForcedCatalogue(Catalogue catalogue, String username, ReserveLevel level) {
			this.catalogue = catalogue;
			this.username = username;
			this.editLevel = level;
		}
		
		public Catalogue getCatalogue() {
			return catalogue;
		}
		public String getUsername() {
			return username;
		}
		public ReserveLevel getEditLevel() {
			return editLevel;
		}
	}
	
	@Override
	public boolean forceEditing(Catalogue catalogue, String username, ReserveLevel editLevel) {
		return this.database.add(new ForcedCatalogue(catalogue, username, editLevel));
	}

	@Override
	public ReserveLevel getEditingLevel(Catalogue catalogue, String username) {
		
		for (ForcedCatalogue fc: database) {
			if (fc.catalogue.getCode().equals(catalogue.getCode()) && fc.getUsername().equals(username))
				return fc.getEditLevel();
		}
		
		return null;
	}

	@Override
	public boolean removeForceEditing(Catalogue catalogue) {
		
		Iterator<ForcedCatalogue> iterator = database.iterator();
		
		while(iterator.hasNext()) {
			
			ForcedCatalogue fc = iterator.next();
			
			if (fc.getCatalogue().getCode().equals(catalogue.getCode()))
				iterator.remove();
		}
		
		return true;
	}
}
