package catalogue_browser_dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import catalogue.Catalogue;
import catalogue_object.Attribute;
import catalogue_object.Term;
import catalogue_object.TermAttribute;

public class TermAttributeDAOMock implements CatalogueRelationDAO<TermAttribute, Term, Attribute> {

	private Collection<TermAttribute> database;
	
	public TermAttributeDAOMock() {
		this.database = new ArrayList<>();
	}
	
	public void clear() {
		database.clear();
	}

	@Override
	public boolean remove(TermAttribute attr) {
		
		Iterator<TermAttribute> iterator = database.iterator();
		
		while(iterator.hasNext()) {
			
			TermAttribute c = iterator.next();
			
			if (c.getId() == attr.getId())
				iterator.remove();
		}
		
		return true;
	}

	@Override
	public int insert(TermAttribute object) {
		this.database.add(object);
		return 0;
	}

	@Override
	public boolean update(TermAttribute object) {
		this.remove(object);
		this.insert(object);
		return true;
	}

	@Override
	public TermAttribute getById(int id) {
		for (TermAttribute c : database) {
			if (c.getId() == id)
				return c;
		}
		return null;
	}

	@Override
	public TermAttribute getByResultSet(ResultSet rs) throws SQLException {
		
		return null;
	}

	@Override
	public Collection<TermAttribute> getAll() {
		return this.database;
	}

	@Override
	public void setCatalogue(Catalogue catalogue) {}

	@Override
	public List<Integer> insert(Iterable<TermAttribute> attrs) {
		return null;
	}

	@Override
	public Collection<TermAttribute> getByA1(Term object) {
		
		return null;
	}

	@Override
	public Collection<TermAttribute> getByA2(Attribute object) {
		
		return null;
	}

	@Override
	public boolean removeByA1(Term object) {
		
		return false;
	}

	@Override
	public boolean removeByA2(Attribute object) {
		
		return false;
	}

	@Override
	public boolean updateByA1(Term object) {
		
		return false;
	}

	@Override
	public boolean updateByA2(Attribute object) {
		
		return false;
	}
}
