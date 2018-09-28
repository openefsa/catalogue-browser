package property;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jface.viewers.ColumnLabelProvider;

import catalogue_object.SortableCatalogueObject;
import term.WrongKeyException;


/**
 * Label provider to visualize the information related to a hierarchy
 * The key parameter decide which data should be visualized in the considered column
 * @author avonva
 *
 */
public class LabelProviderDCFProperty extends ColumnLabelProvider {
	
	private static final Logger LOGGER = LogManager.getLogger(LabelProviderDCFProperty.class);
	private String key;
	
	public LabelProviderDCFProperty( String key ) {
		this.key = key.toLowerCase();
	}
	
	@Override
	public String getText ( Object element ) {
		
		SortableCatalogueObject property = (SortableCatalogueObject) element;
		
		String value;
		
		try {
			value = property.getVariableByKey( key );
		}
		catch ( WrongKeyException e ) {
			e.printStackTrace();
			LOGGER.error("Wrong key", e);
			value = "invalid code for label provider";
		}
		
		return value;
	}
}
