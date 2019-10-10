package term_clipboard;

/**
 * 
 * @author avonva
 *
 * @param <T>
 */
public interface IClipboard<T> {
	
	/**
	 * Copy a <T> object
	 * @param source
	 */
	public void copy(T source);
	
	/**
	 * Copy multiple <T> objects
	 * @param sources
	 */
	public void copy(Iterable<T> sources);
}
