package import_catalogue;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import catalogue.Catalogue;
import catalogue.ReleaseNotesOperation;
import catalogue_browser_dao.CatalogueEntityDAO;
import catalogue_browser_dao.ReleaseNotesOperationDAO;
import naming_convention.Headers;
import open_xml_reader.ResultDataSet;

public class NotesSheetImporter extends SheetImporter<ReleaseNotesOperation> {

	private static final Logger LOGGER = LogManager.getLogger(NotesSheetImporter.class);

	private CatalogueEntityDAO<ReleaseNotesOperation> dao;
	
	/**
	 * Initialize importer giving the dao which will insert the records
	 * @param dao
	 */
	public NotesSheetImporter(CatalogueEntityDAO<ReleaseNotesOperation> dao) {
		this.dao = dao;
	}
	
	/**
	 * Default dao for the catalogue
	 * @param catalogue
	 */
	public NotesSheetImporter(Catalogue catalogue) {
		this(new ReleaseNotesOperationDAO(catalogue));
	}

	@Override
	public ReleaseNotesOperation getByResultSet(ResultDataSet rs) {

		// get all the ops related to the current excel row
		// separating the operation info (they are $ separated)
		ReleaseNotesOperation op = null;
		try {
			op = getByExcelResultSet( rs );
		} catch (SQLException e) {
			e.printStackTrace();
			LOGGER.error("Cannot get release notes from excel", e);
		}
		
		return op;
	}
	
	
	/**
	 * Get the operation starting from an excel result set
	 * @param rs
	 * @return
	 * @throws SQLException
	 */
	public static ReleaseNotesOperation getByExcelResultSet(ResultSet rs) throws SQLException {

		String name = rs.getString( Headers.OP_NAME );
		Timestamp date = rs.getTimestamp( Headers.OP_DATE );
		String info = rs.getString( Headers.OP_INFO );
		int groupId = rs.getInt( Headers.OP_GROUP );

		// create a release note operation with a temp group id
		ReleaseNotesOperation op = new ReleaseNotesOperation(name, 
				date, info, groupId);

		return op;
	}

	@Override
	public Collection<ReleaseNotesOperation> getAllByResultSet(ResultDataSet rs) {
		return null;
	}

	@Override
	public void insert( Collection<ReleaseNotesOperation> ops ) {
		dao.insert(ops);
	}

	@Override
	public void end() {}
}
