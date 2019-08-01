package catalogue_browser_dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import catalogue.Catalogue;
import catalogue_object.Applicability;
import catalogue_object.Hierarchy;
import catalogue_object.Term;

public class ParentTermDAOMock implements CatalogueRelationDAO<Applicability, Term, Hierarchy> {

	private Collection<Applicability> database;
	
	public ParentTermDAOMock() {
		this.database = new ArrayList<>();
	}
	
	public void clear() {
		database.clear();
	}

	@Override
	public boolean remove(Applicability attr) {
		return true;
	}

	@Override
	public int insert(Applicability object) {
		this.database.add(object);
		return 0;
	}

	@Override
	public boolean update(Applicability object) {
		this.remove(object);
		this.insert(object);
		return true;
	}

	@Override
	public Applicability getById(int id) {
		return null;
	}

	@Override
	public Applicability getByResultSet(ResultSet rs) throws SQLException {
		return null;
	}

	@Override
	public Collection<Applicability> getAll() {
		return this.database;
	}

	@Override
	public void setCatalogue(Catalogue catalogue) {}

	@Override
	public List<Integer> insert(Iterable<Applicability> attrs) {
		return null;
	}

	@Override
	public Collection<Applicability> getByA1(Term object) {
		return null;
	}

	@Override
	public Collection<Applicability> getByA2(Hierarchy object) {
		return null;
	}

	@Override
	public boolean removeByA1(Term object) {
		return false;
	}

	@Override
	public boolean removeByA2(Hierarchy object) {
		return false;
	}

	@Override
	public boolean updateByA1(Term object) {
		return false;
	}

	@Override
	public boolean updateByA2(Hierarchy object) {
		return false;
	}
}
