package property;

import org.eclipse.jface.viewers.ColumnLabelProvider;

import catalogue_object.SortableCatalogueObject;


/**
 * Label provider to visualize the information related to a hierarchy
 * The key parameter decide which data should be visualized in the considered column
 * @author avonva
 *
 */
public class LabelProviderDCFProperty extends ColumnLabelProvider {
	
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
		catch ( Exception e ) {
			e.printStackTrace();
			value = "invalid code for label provider";
		}
		
		return value;
	}
}
