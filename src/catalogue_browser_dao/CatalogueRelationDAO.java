package catalogue_browser_dao;

import java.util.Collection;

/**
 * Interface for daos which are binary database relationships (we need
 * additional parameters to model them!)
 * 
 * @author avonva
 * @author shahaal
 * 
 * @param <E>  the type of the relation entity
 * @param <A1> the type of the entity of one table involved in the relation
 * @param <A2> the type of the entity of the other table involved in the
 *             relation
 */
public interface CatalogueRelationDAO<E, A1, A2> extends CatalogueEntityDAO<E> {

	// get all the E using T
	public Collection<E> getByA1(A1 object);

	// get all the E using Z
	public Collection<E> getByA2(A2 object);

	// remove all E which has T
	public boolean removeByA1(A1 object);

	// remove all E which has Z
	public boolean removeByA2(A2 object);

	// update all E which has T
	public boolean updateByA1(A1 object);

	// update all E which has Z
	public boolean updateByA2(A2 object);
}
