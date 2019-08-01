package catalogue_browser_dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import catalogue.Catalogue;
import catalogue.ReleaseNotesOperation;

public class NotesDAOMock implements CatalogueEntityDAO<ReleaseNotesOperation> {

	private Collection<ReleaseNotesOperation> database;
<<<<<<< HEAD

	public NotesDAOMock() {
		this.database = new ArrayList<>();
	}

=======
	
	public NotesDAOMock() {
		this.database = new ArrayList<>();
	}
	
>>>>>>> 574ffe363e78d250cf6350ff4ea89f2f48352380
	public void clear() {
		database.clear();
	}

	@Override
	public boolean remove(ReleaseNotesOperation attr) {
<<<<<<< HEAD

		Iterator<ReleaseNotesOperation> iterator = database.iterator();

		while (iterator.hasNext()) {

			ReleaseNotesOperation c = iterator.next();

			if (c.getId() == attr.getId())
				iterator.remove();
		}

=======
		
		Iterator<ReleaseNotesOperation> iterator = database.iterator();
		
		while(iterator.hasNext()) {
			
			ReleaseNotesOperation c = iterator.next();
			
			if (c.getId() == attr.getId())
				iterator.remove();
		}
		
>>>>>>> 574ffe363e78d250cf6350ff4ea89f2f48352380
		return true;
	}

	@Override
	public int insert(ReleaseNotesOperation object) {
		this.database.add(object);
		return 0;
	}

	@Override
	public boolean update(ReleaseNotesOperation object) {
		this.remove(object);
		this.insert(object);
		return true;
	}

	@Override
	public ReleaseNotesOperation getById(int id) {
		for (ReleaseNotesOperation c : database) {
			if (c.getId() == id)
				return c;
		}
		return null;
	}

	@Override
	public ReleaseNotesOperation getByResultSet(ResultSet rs) throws SQLException {
<<<<<<< HEAD
=======
		// TODO Auto-generated method stub
>>>>>>> 574ffe363e78d250cf6350ff4ea89f2f48352380
		return null;
	}

	@Override
	public Collection<ReleaseNotesOperation> getAll() {
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
	public List<Integer> insert(Iterable<ReleaseNotesOperation> attrs) {
		return null;
	}
}
