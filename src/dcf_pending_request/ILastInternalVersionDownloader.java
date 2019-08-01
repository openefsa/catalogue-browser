package dcf_pending_request;

import java.io.IOException;
import java.sql.SQLException;

import javax.xml.soap.SOAPException;

import config.Environment;
import import_catalogue.ImportException;

public interface ILastInternalVersionDownloader {

	/**
	 * Download and import the last internal version of a catalogue
	 * @param catalogueCode
	 * @throws SOAPException
	 * @throws ImportException
	 * @throws IOException
	 * @throws SQLException
	 */
	public void downloadAndImport(String catalogueCode, Environment env) 
			throws SOAPException, ImportException, IOException, SQLException;
}
