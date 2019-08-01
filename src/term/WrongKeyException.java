package term;

/**
 * Called when a wrong key is used for retrieve a variable in the dcf property (hierarchy/dcfattribute)
 * @author avonva
 *
 */
public class WrongKeyException extends Exception {

	private static final long serialVersionUID = -1846166339066264196L;

	public WrongKeyException() {
		super( "Wrong key! No field was retrieved." );
	}
}
