package form_objects_list;

import java.util.Collection;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;

import data_collection.DCTableConfig;
import i18n_messages.CBMessages;
import utilities.GlobalUtil;

/**
 * List of tables and configurations (related to a data collection)
 * @author avonva
 *
 */
public class FormDCTableConfigsList extends FormObjectsList<DCTableConfig> {

	public static final String TABLE_NAME = "tableName";
	public static final String VARIABLE_NAME = "variableName";
	public static final String CAT_CODE = "catCode";
	public static final String HIER_CODE = "hierCode";
	
	private static final String WINDOW_CODE = "FormDCTableConfigsList";
	
	public FormDCTableConfigsList(Shell shell, String title, Collection<DCTableConfig> objs) {
		super(shell, WINDOW_CODE, title, objs, false);
	}

	@Override
	public void addColumnByKey(TableViewer table, String key) {

		switch ( key ) {
		case TABLE_NAME:
			GlobalUtil.addStandardColumn( table, 
					new DCTableConfigLabelProvider(key), 
					CBMessages.getString("FormConfigList.NameColumn"), 
					200, true, false ); 
			break;
			
		case VARIABLE_NAME: 
			GlobalUtil.addStandardColumn( table, 
					new DCTableConfigLabelProvider(key),
					CBMessages.getString("FormConfigList.VariableColumn"), 
					200, true, false ); 
			break;
			
		case CAT_CODE: 
			GlobalUtil.addStandardColumn( table, 
					new DCTableConfigLabelProvider(key),
					CBMessages.getString("FormConfigList.CatalogueColumn"), 200, true, false ); 
			break;
			
		case HIER_CODE: 
			GlobalUtil.addStandardColumn( table, 
					new DCTableConfigLabelProvider(key),
					CBMessages.getString("FormConfigList.HierarchyColumn"), 
					200, true, false ); 
			break;
		default:
			break;
		}
	}
	
	/**
	 * Label provider for table columns
	 * @author avonva
	 *
	 */
	private class DCTableConfigLabelProvider extends ColumnLabelProvider {
		
		private String key;
		
		public DCTableConfigLabelProvider( String key ) {
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
		
		@Override
		public String getText(Object arg0) {

			DCTableConfig tConfig = ( DCTableConfig ) arg0;

			String value = null;
			switch ( key ) {
			case TABLE_NAME:
				value = tConfig.getTable().getName(); 
				break;
				
			case VARIABLE_NAME:
				value = tConfig.getConfig().getDataElementName(); 
				break;
				
			case CAT_CODE:
				value = tConfig.getConfig().getCatalogueCode();
				break;
				
			case HIER_CODE:
				value = tConfig.getConfig().getHierarchyCode();
				break;
			default:
				break;
			}
			
			return value;
		}
	}
}
