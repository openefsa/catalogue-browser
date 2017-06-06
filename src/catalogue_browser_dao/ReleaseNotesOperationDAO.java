package catalogue_browser_dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;

import catalogue.Catalogue;
import catalogue.ReleaseNotesOperation;
import sheet_converter.NotesSheetConverter;

/**
 * Manager of the Release note operation table.
 * @author avonva
 *
 */
public class ReleaseNotesOperationDAO implements CatalogueEntityDAO<ReleaseNotesOperation>{

	private Catalogue catalogue;

	/**
	 * Initialize the dao using the catalogue we want to connect with.
	 * @param catalogue
	 */
	public ReleaseNotesOperationDAO( Catalogue catalogue ) {
		this.catalogue = catalogue;
	}

	@Override
	public int insert( ReleaseNotesOperation op ) {

		int id = -1;

		String query = "insert into APP.RELEASE_NOTES_OP "
				+ "(OP_NAME, OP_DATE, OP_INFO, OP_GROUP_ID) values (?,?,?,?)";

		try {

			Connection con = catalogue.getConnection();
			PreparedStatement stmt = con.prepareStatement( query, 
					Statement.RETURN_GENERATED_KEYS );

			stmt.setString( 1, op.getOpName() );
			stmt.setTimestamp( 2, op.getOpDate() );
			stmt.setString( 3, op.getOpInfo() );
			stmt.setInt( 4, op.getGroupId() );

			stmt.executeUpdate();

			ResultSet rs = stmt.getGeneratedKeys();
			if ( rs.next() )
				id = rs.getInt( 1 );

			rs.close();
			stmt.close();
			con.close();

		} catch ( SQLException e ) {
			e.printStackTrace();
		}

		return id;
	}

	@Override
	public boolean remove(ReleaseNotesOperation object) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean update(ReleaseNotesOperation object) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public ReleaseNotesOperation getById(int id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ReleaseNotesOperation getByResultSet(ResultSet rs) throws SQLException {

		String name = rs.getString( "OP_NAME" );
		Timestamp date = rs.getTimestamp( "OP_DATE" );
		String info = rs.getString( "OP_INFO" );
		int groupId = rs.getInt( "OP_GROUP_ID" );
		
		ReleaseNotesOperation op = new ReleaseNotesOperation(name, 
				date, info, groupId);
		
		return op;
	}

	@Override
	public Collection<ReleaseNotesOperation> getAll() {

		Collection<ReleaseNotesOperation> ops = new ArrayList<>();
		
		String query = "select * from APP.RELEASE_NOTES_OP";

		try {

			Connection con = catalogue.getConnection();
			PreparedStatement stmt = con.prepareStatement( query );

			ResultSet rs = stmt.executeQuery();

			while( rs.next() )
				ops.add( getByResultSet( rs ) );

			rs.close();
			stmt.close();
			con.close();

		} catch ( SQLException e ) {
			e.printStackTrace();
		}

		return ops;
	}
	
	/**
	 * Get the operation starting from an excel result set
	 * @param rs
	 * @return
	 * @throws SQLException
	 */
	public ReleaseNotesOperation getByExcelResultSet(ResultSet rs) throws SQLException {

		String name = rs.getString( NotesSheetConverter.OP_NAME_NODE );
		Timestamp date = rs.getTimestamp( NotesSheetConverter.OP_DATE_NODE );
		String info = rs.getString( NotesSheetConverter.OP_INFO_NODE );
		int groupId = rs.getInt( NotesSheetConverter.OP_GROUP_NODE );

		// create a release note operation with a temp group id
		ReleaseNotesOperation op = new ReleaseNotesOperation(name, 
				date, info, groupId);

		return op;
	}
}
