package catalogue_browser_dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import catalogue.Catalogue;
import catalogue_object.Hierarchy;

public class HierarchyDAOMock implements CatalogueEntityDAO<Hierarchy> {

	private Collection<Hierarchy> database;

	public HierarchyDAOMock() {
		this.database = new ArrayList<>();
	}

	public void clear() {
		database.clear();
	}

	@Override
	public boolean remove(Hierarchy attr) {

		Iterator<Hierarchy> iterator = database.iterator();

		while (iterator.hasNext()) {

			Hierarchy c = iterator.next();

			if (c.getCode().equals(attr.getCode()))
				iterator.remove();
		}

		return true;
	}

	@Override
	public int insert(Hierarchy object) {
		this.database.add(object);
		return 0;
	}

	@Override
	public boolean update(Hierarchy object) {
		this.remove(object);
		this.insert(object);
		return true;
	}

	@Override
	public Hierarchy getById(int id) {
		for (Hierarchy c : database) {
			if (c.getId() == id)
				return c;
		}
		return null;
	}

	@Override
	public Hierarchy getByResultSet(ResultSet rs) throws SQLException {
		return null;
	}

	@Override
	public Collection<Hierarchy> getAll() {
		return this.database;
	}

	@Override
	public void setCatalogue(Catalogue catalogue) {
	}

	@Override
	public List<Integer> insert(Iterable<Hierarchy> attrs) {
		return null;
	}
}
