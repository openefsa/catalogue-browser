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

	public NotesDAOMock() {
		this.database = new ArrayList<>();
	}

	public void clear() {
		database.clear();
	}

	@Override
	public boolean remove(ReleaseNotesOperation attr) {

		Iterator<ReleaseNotesOperation> iterator = database.iterator();

		while (iterator.hasNext()) {

			ReleaseNotesOperation c = iterator.next();

			if (c.getId() == attr.getId())
				iterator.remove();
		}

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
		return null;
	}

	@Override
	public Collection<ReleaseNotesOperation> getAll() {
		return this.database;
	}

	@Override
	public void setCatalogue(Catalogue catalogue) {
	}

	@Override
	public List<Integer> insert(Iterable<ReleaseNotesOperation> attrs) {
		return null;
	}
}
