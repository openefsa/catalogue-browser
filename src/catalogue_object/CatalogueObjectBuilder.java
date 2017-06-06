package catalogue_object;

import catalogue.Catalogue;

/**
 * Builder for catalogue objects, add the catalogue information
 * to the base object builder
 * @author avonva
 *
 */
public class CatalogueObjectBuilder extends BaseObjectBuilder {

	protected Catalogue catalogue;
	
	public void setCatalogue(Catalogue catalogue) {
		this.catalogue = catalogue;
	}
	
	@Override
	public BaseObject build() {
		return new CatalogueObject ( catalogue, id, code, name, label, scopenotes, version, 
				lastUpdate, validFrom, validTo, status, deprecated );
	}
}
