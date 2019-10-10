package catalogue_generator;

public class DuplicatedCatalogueException extends Exception {

	private static final long serialVersionUID = 2015533084108598425L;

	public DuplicatedCatalogueException() {
		super ( "Cannot create a catalogue with this code, it is already present in the database" );
	}
}
