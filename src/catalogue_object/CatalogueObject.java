package catalogue_object;

import java.sql.Timestamp;

import catalogue.Catalogue;

/**
 * General object to model an object which is part of a catalogue,
 * as terms, hierarchies and attributes.
 * @author avonva
 *
 */
public class CatalogueObject extends BaseObject {

	protected Catalogue catalogue;
	
	public CatalogueObject( Catalogue catalogue ) {
		this.catalogue = catalogue;
	}
	
	/**
	 * Initialize a catalogue object
	 * @param catalogue the catalogue which contains this catalogue object
	 * @param id
	 * @param version
	 * @param lastUpdate
	 * @param validFrom
	 * @param validTo
	 * @param status
	 * @param deprecated
	 */
	public CatalogueObject( Catalogue catalogue, int id, String code, 
			String name, String label, String scopenotes, 
			String version, Timestamp lastUpdate, Timestamp validFrom,
			Timestamp validTo, String status, boolean deprecated ) {

		super( id, code, name, label, scopenotes, version, lastUpdate, 
				validFrom, validTo, status, deprecated );
		
		this.catalogue = catalogue;
	}
	
	/**
	 * Get the catalogue which contains the current element
	 * @return
	 */
	public Catalogue getCatalogue() {
		return catalogue;
	}
}
