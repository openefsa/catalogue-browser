package form_objects_list;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;

import data_collection.DataCollection;
import i18n_messages.CBMessages;
import utilities.GlobalUtil;

/**
 * Form which displays a list of {@link DataCollection} objects.
 * @author avonva
 *
 */
public class FormDataCollectionsList extends FormObjectsList<DataCollection> {

	public static final String CODE = "code";
	public static final String DESCRIPTION = "desc";
	public static final String ACTIVE_FROM = "activeFrom";
	public static final String ACTIVE_TO = "activeTo";
	
	private static final String WINDOW_CODE = "FormDataCollectionsList";
	
	/**
	 * Initialize the form
	 * @param shell parent shell
	 * @param title form title
	 * @param objs the list of data collections to show
	 */
	public FormDataCollectionsList(Shell shell, String title, Collection<DataCollection> objs) {
		super(shell, WINDOW_CODE, title, objs, false);
	}

	@Override
	public void addColumnByKey(TableViewer table, String key) {
		
		switch ( key ) {
		case CODE:
			// Add the "Label" column
			GlobalUtil.addStandardColumn( table, new DCLabelProvider(key), 
					CBMessages.getString("FormDCList.CodeColumn"), 200, true, false ); 
			break;
		case DESCRIPTION: 
			// Add the "Scopenote" column
			GlobalUtil.addStandardColumn( table, new DCLabelProvider(key),
					CBMessages.getString("FormDCList.DescriptionColumn"), 300, true, false ); 
			break;
		case ACTIVE_FROM: 
			// Add the "Scopenote" column
			GlobalUtil.addStandardColumn( table, new DCLabelProvider(key),
					CBMessages.getString("FormDCList.ActiveFromColumn"), 120, true, false ); 
			break;
		case ACTIVE_TO: 
			// Add the "Scopenote" column
			GlobalUtil.addStandardColumn( table, new DCLabelProvider(key),
					CBMessages.getString("FormDCList.ActiveToColumn"), 120, true, false ); 
			break;
		default:
			break;
		}
		
	}
	
	/**
	 * Label provider for the catalogue code column
	 * @author avonva
	 *
	 */
	private class DCLabelProvider extends ColumnLabelProvider {
		
		private String key;
		
		public DCLabelProvider( String key ) {
			this.key = key;
		}
		
		@Override
		public void addListener(ILabelProviderListener arg0) {}

		@Override
		public void dispose() {}

		@Override
		public boolean isLabelProperty(Object arg0, String arg1) {
			return false;
		}

		@Override
		public void removeListener(ILabelProviderListener arg0) {}

		@Override
		public Image getImage(Object arg0) {
			return null;
		}

		private String convertTs ( Timestamp ts ) {
			
			if ( ts == null )
				return null;
			
			DateFormat sdf = new SimpleDateFormat( FormObjectsList.STD_DATE_FORMAT ); 
			return sdf.format( ts );
		}
		
		@Override
		public String getText(Object arg0) {

			DataCollection dc = ( DataCollection ) arg0;

			String value = null;
			switch ( key ) {
			case CODE:
				value = dc.getCode(); break;
				
			case DESCRIPTION:
				value = dc.getDescription(); break;
				
			case ACTIVE_FROM:
				value = convertTs( dc.getActiveFrom() );
				break;
				
			case ACTIVE_TO:
				value = convertTs( dc.getActiveTo() );
				break;
			default:
				break;
			}
			
			return value;
		}
	}
}
