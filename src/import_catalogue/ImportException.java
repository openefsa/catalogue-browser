package import_catalogue;

import java.io.IOException;

public class ImportException extends IOException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2271711162522344885L;

	public ImportException(String text) {
		super(text);
	}
}
