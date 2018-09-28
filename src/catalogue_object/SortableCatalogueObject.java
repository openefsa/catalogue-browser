package catalogue_object;

import java.sql.Timestamp;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import catalogue.Catalogue;
import term.WrongKeyException;

/**
 * Base class for hierarchies and attributes
 * @author avonva
 *
 */
public class SortableCatalogueObject extends CatalogueObject implements Sortable, 
	Comparable<SortableCatalogueObject> {

	private static final Logger LOGGER = LogManager.getLogger(SortableCatalogueObject.class);
	
	private int order;
	
	public SortableCatalogueObject( Catalogue catalogue ) {
		super( catalogue );
	}
	
	public SortableCatalogueObject( Catalogue catalogue, int id, String code, 
			String name, String label, String scopenotes, int order, String status,
			String version, Timestamp lastUpdate, Timestamp validFrom, Timestamp 
			validTo, boolean deprecated ) {
		
		super( catalogue, id, code, name, label, scopenotes, version, lastUpdate, 
				validFrom, validTo, status, deprecated);
		this.order = order;
	}
	
	public void setOrder(int order) {
		this.order = order;
	}
	
	public int getOrder() {
		return order;
	}

	/**
	 * Set the variable value using a key instead of the different set methods
	 * @param key
	 * @param value
	 * @throws WrongKeyException
	 */
	public void setVariableByKey ( String key, String value ) throws WrongKeyException {

		boolean found = true;
		key = key.toLowerCase();
		
		switch ( key ) {
		case "code":
			setCode( value ); break;
		case "name":
			setName( value ); break;
		case "label":
			setLabel( value ); break;
		case "scopenotes":
			setScopenotes( value ); break;
		case "order":
			try {
				setOrder( Integer.parseInt( value ) ); 
			} catch ( NumberFormatException e ) {
				LOGGER.error ( "Wrong type, integer required", e );
			}
			break;
			
		default:
			found = false; break;
		}
		
		if ( !found )
			throw new WrongKeyException();
	}
	
	/**
	 * Get the content of a variable using a key (useful for editing cells)
	 * @param key
	 * @return
	 */
	public String getVariableByKey ( String key ) throws WrongKeyException {
		
		boolean found = true;
		key = key.toLowerCase();
		String value = null;

		// dcf property general case
		switch ( key ) {
		case "code":
			value = getCode(); break;
		case "name":
			value = getName(); break;
		case "label":
			value = getLabel(); break;
		case "scopenotes":
			value = getScopenotes(); break;
		case "order":
			value = String.valueOf( order ); break;
		default:
			found = false; break;
		}
		
		// if no element is found, then throw exception
		if ( !found )
			throw new WrongKeyException();
		
		return value;
	}

	@Override
	public int compareTo(SortableCatalogueObject arg0) {

		if ( order == arg0.getOrder() )
			return 0;
		
		if ( order < arg0.getOrder() )
			return -1;
			
		return 1;
	}
}
