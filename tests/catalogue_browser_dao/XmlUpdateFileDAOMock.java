package catalogue_browser_dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import catalogue.Catalogue;
import sas_remote_procedures.XmlUpdateFile;

public class XmlUpdateFileDAOMock implements CatalogueEntityDAO<XmlUpdateFile> {

	private Collection<XmlUpdateFile> database;
	
	public XmlUpdateFileDAOMock() {
		this.database = new ArrayList<>();
	}
	
	public void clear() {
		database.clear();
	}

	@Override
	public boolean remove(XmlUpdateFile attr) {
		
		Iterator<XmlUpdateFile> iterator = database.iterator();
		
		while(iterator.hasNext()) {
			
			XmlUpdateFile c = iterator.next();
			
			if (c.getXmlFilename().equals(attr.getXmlFilename()))
				iterator.remove();
		}
		
		return true;
	}

	@Override
	public int insert(XmlUpdateFile object) {
		this.database.add(object);
		return 0;
	}

	@Override
	public boolean update(XmlUpdateFile object) {
		this.remove(object);
		this.insert(object);
		return true;
	}

	@Override
	public XmlUpdateFile getById(int id) {
		for (XmlUpdateFile file : database) {
			if (file.getCatalogue().getId() == id)
				return file;
		}
		return null;
	}

	@Override
	public XmlUpdateFile getByResultSet(ResultSet rs) throws SQLException {
		
		return null;
	}

	@Override
	public Collection<XmlUpdateFile> getAll() {
		return this.database;
	}

	@Override
	public void setCatalogue(Catalogue catalogue) {}

	@Override
	public List<Integer> insert(Iterable<XmlUpdateFile> attrs) {
		return null;
	}
}
