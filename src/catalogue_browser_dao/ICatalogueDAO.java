package catalogue_browser_dao;

import java.sql.SQLException;

import catalogue.Catalogue;
import dcf_manager.Dcf.DcfType;

public interface ICatalogueDAO extends CatalogueEntityDAO<Catalogue> {

	/**
	 * Get the last version of the catalogue
	 * 
	 * @param catalogueCode
	 * @param type
	 * @return
	 */
	public Catalogue getLastVersionByCode(String catalogueCode, DcfType type);

	/**
	 * Compress the database of the catalogue
	 * 
	 * @param catalogue
	 */
	public void compress(Catalogue catalogue);

	/**
	 * Delete the contents of a catalogue without removing it from the list of
	 * catalogues
	 * 
	 * @param catalogue
	 */
	public void deleteContents(Catalogue catalogue) throws SQLException;

	/**
	 * Get a catalogue by code and version
	 * 
	 * @param code
	 * @param version
	 * @param type
	 * @return
	 */
	public Catalogue getCatalogue(String code, String version, DcfType type);
}
