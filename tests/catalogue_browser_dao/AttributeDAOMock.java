package catalogue_browser_dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import catalogue.Catalogue;
import catalogue_object.Attribute;

public class AttributeDAOMock implements CatalogueEntityDAO<Attribute> {

	private Collection<Attribute> database;
	
	public AttributeDAOMock() {
		this.database = new ArrayList<>();
	}
	
	public void clear() {
		database.clear();
	}

	@Override
	public boolean remove(Attribute attr) {
		
		Iterator<Attribute> iterator = database.iterator();
		
		while(iterator.hasNext()) {
			
			Attribute c = iterator.next();
			
			if (c.getCode().equals(attr.getCode()))
				iterator.remove();
		}
		
		return true;
	}

	@Override
	public int insert(Attribute object) {
		this.database.add(object);
		return 0;
	}

	@Override
	public boolean update(Attribute object) {
		this.remove(object);
		this.insert(object);
		return true;
	}

	@Override
	public Attribute getById(int id) {
		for (Attribute c : database) {
			if (c.getId() == id)
				return c;
		}
		return null;
	}

	@Override
	public Attribute getByResultSet(ResultSet rs) throws SQLException {
		return null;
	}

	@Override
	public Collection<Attribute> getAll() {
		return this.database;
	}

	@Override
	public void setCatalogue(Catalogue catalogue) {}

	@Override
	public List<Integer> insert(Iterable<Attribute> attrs) {
		return null;
	}
}
