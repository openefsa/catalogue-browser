package catalogue_browser_dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import catalogue.Catalogue;
import catalogue.ReservedCatalogue;

public class ReservedCatalogueDAOMock implements CatalogueEntityDAO<ReservedCatalogue> {

	private Collection<ReservedCatalogue> database;
	
	public ReservedCatalogueDAOMock() {
		this.database = new ArrayList<>();
	}

	@Override
	public boolean remove(ReservedCatalogue catalogue) {
		
		Iterator<ReservedCatalogue> iterator = database.iterator();
		
		while(iterator.hasNext()) {
			
			ReservedCatalogue c = iterator.next();
			
			if (c.getCatalogue().getCode().equals(catalogue.getCatalogue().getCode()))
				iterator.remove();
		}
		
		return true;
	}

	@Override
	public int insert(ReservedCatalogue object) {
		this.database.add(object);
		return 0;
	}

	@Override
	public boolean update(ReservedCatalogue object) {
		this.remove(object);
		this.insert(object);
		return true;
	}

	@Override
	public ReservedCatalogue getById(int id) {
		for (ReservedCatalogue c : database) {
			if (c.getCatalogue().getId() == id)
				return c;
		}
		return null;
	}
	
	public ReservedCatalogue get(String catalogueCode) {
		for (ReservedCatalogue c : database) {
			if (c.getCatalogue().getCode().equals(catalogueCode))
				return c;
		}
		return null;
	}

	@Override
	public ReservedCatalogue getByResultSet(ResultSet rs) throws SQLException {
		
		return null;
	}

	@Override
	public Collection<ReservedCatalogue> getAll() {
		return this.database;
	}

	@Override
	public void setCatalogue(Catalogue catalogue) {
		
		
	}

	@Override
	public List<Integer> insert(Iterable<ReservedCatalogue> attrs) {
		
		return null;
	}
}
