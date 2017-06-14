package import_catalogue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import catalogue.Catalogue;
import catalogue_object.Term;
import open_xml_reader.ResultDataSet;

/**
 * Sheet importer. Abstract class to create the skeleton program
 * used to import xlsx sheet into the catalogue database.
 * @author avonva
 *
 * @param <T> specify the type of object which is imported. For
 * example, if we are importing the "term" sheet, the object would
 * be {@link Term}.
 */
public abstract class SheetImporter<T> {
	
	private ResultDataSet data;

	/**
	 * Initialize the sheet importer
	 * @param data the sheet data
	 */
	public SheetImporter( ResultDataSet data ) {
		this.data = data;
	}
	
	/**
	 * Start the import process of the sheet
	 * without limitations on the batch size
	 */
	public void importSheet() {
		importSheet( Integer.MAX_VALUE );
	}
	
	/**
	 * Start the import process of the sheet
	 * @param batchSize the maximum number of records
	 * which should be maintained in memory each time
	 * before inserting them into the physical database
	 * A bigger batchSize improves the speed of the process,
	 * but increases the memory usage. On the other hand,
	 * a smaller batchSize slows down the process but uses
	 * less ram memory.
	 */
	public void importSheet( int batchSize ) {
		
		// list of all objects which were parsed
		Collection<T> objs = new ArrayList<>();
		
		while ( data.next() ) {
			
			// read the current line and get the
			// related object
			T obj = getByResultSet( data );
			
			// get all the objects from the results set
			Collection<T> allObjs = getAllByResultSet( data );
			
			// add the object to the list
			// if an object was created
			if ( obj != null )
				objs.add( obj );
			
			// add the multiple objects to the list if they
			// were created
			if ( allObjs != null )
				objs.addAll( allObjs );
			
			// if maximum limit is reached insert
			// the batch of objects and clear the
			// array list memory
			// or if we are forcing the inserting go on
			if ( objs.size() >= batchSize ) {
				
				insert ( objs );
				
				// clear variables
				allObjs = null;
				obj = null;
				objs.clear();
				
				// run garbage collector to
				// remove from memory useless variables
				System.gc();
			}
		}
		
		// insert all the remaining T objects into the db
		if ( !objs.isEmpty() )
			insert( objs );
		
		// end the process
		end();
	}
	
	/**
	 * Create an hashmap to save (key, ids) values (used to save DB ids when we insert the term)
	 * @param rs
	 * @param keys
	 * @return
	 * @throws SQLException
	 * @throws NoCatalogueOpenException 
	 */
	public static HashMap<String, Integer> createIdHashMap ( Catalogue catalogue, 
			String idField, String codeField, String tableName ) 
			throws SQLException {
		
		// output hashmap
		HashMap<String, Integer> hash = new HashMap<>();

		Connection con = catalogue.getConnection();
		
		// get the ids and codes
		PreparedStatement stmt = con.prepareStatement( "SELECT " + idField + "," + codeField + " FROM " + tableName );
		
		// get the results
		ResultSet rs = stmt.executeQuery();
		
		// for each element, set the id and the code and add to the hash map the element
		while ( rs.next() ) {
			
			// get the auto generated id
			int id = rs.getInt( idField );
			
			// get the current key
			String key = rs.getString( codeField );
			
			// add the pair code id into the hash map
			hash.put( key, id );
		}
		
		rs.close();
		stmt.close();
		con.close();
		
		return hash;
	}
	
	/**
	 * Create the object related to the current sheet row data
	 * @param rs the result set which contains the information. Note
	 * that the {@link ResultDataSet#next()} was already called and you
	 * must not use it! If you use this method please do not use
	 * {@link #getAllByResultSet(ResultDataSet)}, otherwise duplicated
	 * records will be created.
	 * @return the object we want to import or null (null will be ignored)
	 */
	public abstract T getByResultSet( ResultDataSet rs );
	
	/**
	 * Get the objects related to the current sheet row data. Note
	 * that this method should be used only if a row corresponds
	 * to multiple objects in the application logic. Note
	 * that the {@link ResultDataSet#next()} was already called and you
	 * must not use it!
	 * If you use this method please do not use {@link #getByResultSet(ResultDataSet)},
	 * otherwise duplicated records will be created.
	 * @param rs
	 * @return a collection of T objects created starting from
	 * a single row of the sheet
	 */
	public abstract Collection<T> getAllByResultSet ( ResultDataSet rs );
	
	/**
	 * Insert all the retrieved T objects into the database.
	 * @param data list of T objects which have to be inserted in the db
	 * @return hashmap which contains the pairs (code, id) of the
	 * objects. This hashmap is used to get the T objects database ids.
	 */
	public abstract void insert ( Collection<T> data );
	
	/**
	 * Called when the parsing is finished.
	 */
	public abstract void end ();
}
