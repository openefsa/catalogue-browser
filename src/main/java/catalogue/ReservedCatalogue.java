package catalogue;

import soap.UploadCatalogueFileImpl.ReserveLevel;

/**
 * Object to model records of the Reserved_Catalogue table.
 * These objects list all the successfully reserved catalogues.
 * @author avonva
 *
 */
public class ReservedCatalogue {

	private int id;
	private Catalogue catalogue;
	private String username;
	private String note;
	private ReserveLevel level;
	
	/**
	 * Initialize a reserved catalogue object
	 * @param catalogue the catalogue which was reserved
	 * @param username the name of the user who reserved the catalogue
	 * @param note the reservation note for the reserve operation
	 * @param level the obtained reserve level
	 */
	public ReservedCatalogue( Catalogue catalogue, String username, 
			String note, ReserveLevel level ) {
		
		this.id = catalogue.getId();
		this.catalogue = catalogue;
		this.username = username;
		this.note = note;
		this.level = level;
	}

	/**
	 * Get the id of the catalogue which
	 * was reserved
	 * @return
	 */
	public int getCatalogueId() {
		return id;
	}
	/**
	 * Get the catalogue which was reserved
	 * by this reserved catalogue object
	 * @return
	 */
	public Catalogue getCatalogue() {
		return catalogue;
	}
	/**
	 * Get the name of the user who
	 * reserved the {@link #catalogue}
	 * @return
	 */
	public String getUsername() {
		return username;
	}
	/**
	 * Get the reservation note which
	 * was written while reserving
	 * {@link #catalogue}
	 * @return
	 */
	public String getNote() {
		return note;
	}
	/**
	 * Get the reserve level which was
	 * required (i.e. MINOR or MAJOR)
	 * @return
	 */
	public ReserveLevel getLevel() {
		return level;
	}
}
