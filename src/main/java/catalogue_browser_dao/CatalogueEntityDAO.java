package catalogue_browser_dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

import catalogue.Catalogue;

/**
 * Interface used to create the catalogue DAOs related to entities. A DAO should
 * provide at least insert, remove, update, getById, getByCode, getByResultSet,
 * getAll methods.
 * 
 * @author avonva
 * @author shahaal
 *
 * @param <E>
 */
public interface CatalogueEntityDAO<E> {

	/**
	 * Set the catalogue which contains all the contents of this dao
	 * 
	 * @param catalogue
	 */
	public void setCatalogue(Catalogue catalogue);

	/**
	 * Insert an E object into the database
	 * 
	 * @param object
	 * @return the new id created by the db for this object
	 */
	public int insert(E object);

	/**
	 * Insert multiple elements
	 * 
	 * @param attrs
	 */
	public List<Integer> insert(Iterable<E> attrs);

	/**
	 * Remove an E object from the database
	 * 
	 * @param object
	 * @return true if correctly removed
	 */
	public boolean remove(E object);

	/**
	 * Update an E object in the database
	 * 
	 * @param object
	 * @return true if correctly updated
	 */
	public boolean update(E object);

	/**
	 * Get an E object using its id
	 * 
	 * @param id
	 * @return the E object
	 */
	public E getById(int id);

	/**
	 * Get an E object from a result set obtained by querying the database
	 * 
	 * @param rs
	 * @return the E object
	 * @throws SQLException
	 */
	public E getByResultSet(ResultSet rs) throws SQLException;

	/**
	 * Get all the E object contained in the database
	 * 
	 * @return
	 */
	public Collection<E> getAll();
}
