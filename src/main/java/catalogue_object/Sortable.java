package catalogue_object;

/**
 * An object which implements this interface must provide a getOrder method, which
 * give a number which is used to order the current object among similar other objects.
 * @author avonva
 *
 */
public interface Sortable {

	/**
	 * Get an order integer to order this object with other objects
	 * @return
	 */
	public int getOrder();
}
