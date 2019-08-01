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
<<<<<<< HEAD

	public HierarchyDAOMock() {
		this.database = new ArrayList<>();
	}

=======
	
	public HierarchyDAOMock() {
		this.database = new ArrayList<>();
	}
	
>>>>>>> 574ffe363e78d250cf6350ff4ea89f2f48352380
	public void clear() {
		database.clear();
	}

	@Override
	public boolean remove(Hierarchy attr) {
<<<<<<< HEAD

		Iterator<Hierarchy> iterator = database.iterator();

		while (iterator.hasNext()) {

			Hierarchy c = iterator.next();

			if (c.getCode().equals(attr.getCode()))
				iterator.remove();
		}

=======
		
		Iterator<Hierarchy> iterator = database.iterator();
		
		while(iterator.hasNext()) {
			
			Hierarchy c = iterator.next();
			
			if (c.getCode().equals(attr.getCode()))
				iterator.remove();
		}
		
>>>>>>> 574ffe363e78d250cf6350ff4ea89f2f48352380
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
<<<<<<< HEAD
=======
		// TODO Auto-generated method stub
>>>>>>> 574ffe363e78d250cf6350ff4ea89f2f48352380
		return null;
	}

	@Override
	public Collection<Hierarchy> getAll() {
		return this.database;
	}

	@Override
<<<<<<< HEAD
	public void setCatalogue(Catalogue catalogue) {
	}
=======
	public void setCatalogue(Catalogue catalogue) {}
>>>>>>> 574ffe363e78d250cf6350ff4ea89f2f48352380

	@Override
	public List<Integer> insert(Iterable<Hierarchy> attrs) {
		return null;
	}
}
