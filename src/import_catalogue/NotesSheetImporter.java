package import_catalogue;

import java.sql.SQLException;
import java.util.Collection;

import catalogue.Catalogue;
import catalogue.ReleaseNotesOperation;
import catalogue_browser_dao.ReleaseNotesOperationDAO;
import excel_file_management.ResultDataSet;

public class NotesSheetImporter extends SheetImporter<ReleaseNotesOperation> {

	private Catalogue catalogue;
	
	public NotesSheetImporter( Catalogue catalogue, ResultDataSet data) {
		super(data);
		this.catalogue = catalogue;
	}

	@Override
	public ReleaseNotesOperation getByResultSet(ResultDataSet rs) {
		
		ReleaseNotesOperationDAO opDao = 
				new ReleaseNotesOperationDAO( catalogue );
		
		// get all the ops related to the current excel row
		// separating the operation info (they are $ separated)
		ReleaseNotesOperation op = null;
		try {
			op = opDao.getByExcelResultSet( rs );
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return op;
	}

	@Override
	public Collection<ReleaseNotesOperation> getAllByResultSet(ResultDataSet rs) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void insert( Collection<ReleaseNotesOperation> ops ) {
		
		ReleaseNotesOperationDAO opDao = 
				new ReleaseNotesOperationDAO( catalogue );
		
		opDao.insert( ops );
	}
}
