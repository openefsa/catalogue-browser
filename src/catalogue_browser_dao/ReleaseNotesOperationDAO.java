package catalogue_browser_dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import catalogue.Catalogue;
import catalogue.ReleaseNotesOperation;

/**
 * Manager of the Release note operation table.
 * 
 * @author avonva
 * @author shahaal
 *
 */
public class ReleaseNotesOperationDAO implements CatalogueEntityDAO<ReleaseNotesOperation> {

	private static final Logger LOGGER = LogManager.getLogger(ReleaseNotesOperationDAO.class);

	private Catalogue catalogue;

	/**
	 * Initialize the dao using the catalogue we want to connect with.
	 * 
	 * @param catalogue
	 */
	public ReleaseNotesOperationDAO(Catalogue catalogue) {
		this.catalogue = catalogue;
	}

	@Override
	public void setCatalogue(Catalogue catalogue) {
		this.catalogue = catalogue;
	}

	@Override
	public int insert(ReleaseNotesOperation op) {

		Collection<ReleaseNotesOperation> ops = new ArrayList<>();
		ops.add(op);

		List<Integer> ids = insert(ops);
		if (ids.isEmpty())
			return -1;

		return ids.get(0);
	}

	/**
	 * Insert a collection of operations into the database
	 * 
	 * @param ops
	 * @return
	 */
	public synchronized List<Integer> insert(Iterable<ReleaseNotesOperation> ops) {

		ArrayList<Integer> ids = new ArrayList<>();

		String query = "insert into APP.RELEASE_NOTES_OP "
				+ "(OP_NAME, OP_DATE, OP_INFO, OP_GROUP_ID) values (?,?,?,?)";

		try (Connection con = catalogue.getConnection();
				PreparedStatement stmt = con.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);) {

			for (ReleaseNotesOperation op : ops) {

				stmt.clearParameters();

				stmt.setString(1, op.getOpName());
				stmt.setTimestamp(2, op.getOpDate());
				stmt.setString(3, op.getOpInfo());
				stmt.setInt(4, op.getGroupId());

				stmt.addBatch();
			}

			stmt.executeBatch();

			try (ResultSet rs = stmt.getGeneratedKeys();) {
				if (rs != null) {
					while (rs.next())
						ids.add(rs.getInt(1));
					
					rs.close();
				}
			}

			stmt.close();
			con.close();
			
		} catch (SQLException e) {
			e.printStackTrace();
			LOGGER.error("DB error", e);
		}

		return ids;
	}

	@Override
	public boolean remove(ReleaseNotesOperation object) {

		return false;
	}

	@Override
	public boolean update(ReleaseNotesOperation object) {

		return false;
	}

	@Override
	public ReleaseNotesOperation getById(int id) {

		return null;
	}

	@Override
	public ReleaseNotesOperation getByResultSet(ResultSet rs) throws SQLException {

		String name = rs.getString("OP_NAME");
		Timestamp date = rs.getTimestamp("OP_DATE");
		String info = rs.getString("OP_INFO");
		int groupId = rs.getInt("OP_GROUP_ID");

		ReleaseNotesOperation op = new ReleaseNotesOperation(name, date, info, groupId);

		return op;
	}

	@Override
	public Collection<ReleaseNotesOperation> getAll() {

		Collection<ReleaseNotesOperation> ops = new ArrayList<>();

		String query = "select * from APP.RELEASE_NOTES_OP";

		try (Connection con = catalogue.getConnection();
				PreparedStatement stmt = con.prepareStatement(query);
				ResultSet rs = stmt.executeQuery();) {

			while (rs.next())
				ops.add(getByResultSet(rs));

			rs.close();
			stmt.close();
			con.close();
			
		} catch (SQLException e) {
			e.printStackTrace();
			LOGGER.error("DB error", e);
		}

		return ops;
	}
}
