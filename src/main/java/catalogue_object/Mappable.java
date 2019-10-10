package catalogue_object;

/**
 * An object which implements this interface must provide a method which allows
 * getting the object fields through a key (instead of using getter methods)
 * @author avonva
 *
 */
public interface Mappable {

	/**
	 * Get a field of the object using a key
	 * @param key
	 * @return
	 */
	public String getValueByKey( String key );
}
