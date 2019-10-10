package catalogue_browser_dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import catalogue.Catalogue;
import dcf_manager.Dcf.DcfType;
import version_manager.VersionComparator;

public class CatalogueDAOMock implements ICatalogueDAO {

	private Collection<Catalogue> database;

	public CatalogueDAOMock() {
		this.database = new ArrayList<>();
	}

	public void clear() {
		database.clear();
	}

	@Override
	public boolean remove(Catalogue catalogue) {

		Iterator<Catalogue> iterator = database.iterator();

		while (iterator.hasNext()) {

			Catalogue c = iterator.next();

			if (c.sameAs(catalogue))
				iterator.remove();
		}

		return true;
	}

	@Override
	public int insert(Catalogue object) {
		int id = (int) (Math.random() * 100000);
		object.setId(id);
		this.database.add(object);
		return id;
	}

	@Override
	public boolean update(Catalogue object) {
		this.remove(object);
		this.insert(object);
		return true;
	}

	@Override
	public Catalogue getById(int id) {
		for (Catalogue c : database) {
			if (c.getId() == id)
				return c;
		}
		return null;
	}

	@Override
	public Catalogue getByResultSet(ResultSet rs) throws SQLException {
		return null;
	}

	@Override
	public Collection<Catalogue> getAll() {
		return this.database;
	}

	public Catalogue getLastInvalidVersion(String catalogueCode) {

		ArrayList<Catalogue> catalogues = new ArrayList<>();
		ArrayList<String> versions = new ArrayList<>();

		for (Catalogue c : database) {
			if (c.getCode().equals(catalogueCode) && c.getCatalogueVersion().isInvalid()) {
				catalogues.add(c);
				versions.add(c.getVersion());
			}
		}

		Collections.sort(versions, new VersionComparator());

		if (versions.isEmpty())
			return null;

		String lastVersion = versions.get(versions.size() - 1);

		for (Catalogue c : catalogues) {
			if (c.getVersion().equals(lastVersion)) {
				return c;
			}
		}

		return null;
	}

	@Override
	public Catalogue getLastVersionByCode(String catalogueCode, DcfType type) {

		ArrayList<Catalogue> catalogues = new ArrayList<>();
		ArrayList<String> versions = new ArrayList<>();

		for (Catalogue c : database) {
			if (c.getCode().equals(catalogueCode) && !c.getCatalogueVersion().isInvalid()) {
				catalogues.add(c);
				versions.add(c.getVersion());
			}
		}

		Collections.sort(versions, new VersionComparator());

		if (versions.isEmpty())
			return null;

		String lastVersion = versions.get(versions.size() - 1);

		for (Catalogue c : catalogues) {
			if (c.getVersion().equals(lastVersion)) {
				return c;
			}
		}

		return null;
	}

	@Override
	public void setCatalogue(Catalogue catalogue) {
	}

	@Override
	public List<Integer> insert(Iterable<Catalogue> attrs) {
		return null;
	}

	@Override
	public void compress(Catalogue catalogue) {
	}

	@Override
	public void deleteContents(Catalogue catalogue) throws SQLException {
	}

	@Override
	public Catalogue getCatalogue(String code, String version, DcfType type) {
		for (Catalogue c : database) {
			if (c.getCode().equals(code) && c.getVersion().equals(version))
				return c;
		}

		return null;
	}
}
