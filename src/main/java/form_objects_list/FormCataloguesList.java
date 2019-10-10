package form_objects_list;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;

import catalogue.Catalogue;
import i18n_messages.CBMessages;
import utilities.GlobalUtil;


/**
 * This form displays all the catalogues passed by the input parameter. In particular,
 * it shows several catalogues properties and allows selecting one catalogue to make 
 * further action on it.
 * @author avonva
 *
 */
public class FormCataloguesList extends FormObjectsList<Catalogue> {

	private static final String WINDOW_CODE = "FormCataloguesList";
	
	public FormCataloguesList(Shell shell, String title, Collection<Catalogue> objs) {
		super(shell, WINDOW_CODE, title, objs);
	}
	
	public FormCataloguesList(Shell shell, String title, 
			Collection<Catalogue> objs, boolean multisel ) {
		super(shell, WINDOW_CODE, title, objs, multisel );
	}

	/**
	 * Add columns by key into the table TODO IMPLEMENT THE UNIMPLEMENTED COLUMNS IF
	 * NECESSARY
	 * 
	 * @param table
	 * @param columnKey
	 * @param rightFill true if you want the column to be extended if the window is
	 *                  stretched
	 */
	public void addColumnByKey ( TableViewer table, String columnKey ) {

		switch ( columnKey.toLowerCase() ) {
		case "code": 
			break;
		case "name": 
			break;
		case "label": 
			// Add the "Label" column
			GlobalUtil.addStandardColumn( table, new CatalogueLabelLabelProvider(), 
					CBMessages.getString("FormCataloguesList.NameColumn"), 225, true, false ); 
			break;
		case "scopenote": 
			// Add the "Scopenote" column
			GlobalUtil.addStandardColumn( table, new CatalogueScopeNoteLabelProvider(), 
					CBMessages.getString("FormCataloguesList.ScopenoteColumn"), 300, true, false ); 
			break;
		case "code_mask": 
			break;
		case "code_length": 
			break;
		case "non_standard_codes": 
			break;
		case "gen_missing_codes": 
			break;
		case "version": 
			// Add the "Version" column
			GlobalUtil.addStandardColumn( table, new CatalogueVersionLabelProvider(),
					CBMessages.getString("FormCataloguesList.VersionColumn"), 100, true, false, SWT.CENTER ); 
			break;
		case "last_update": 
			break;
		case "valid_from": 
			// Add the "Last release" column
			GlobalUtil.addStandardColumn( table, new CatalogueValidFromLabelProvider(), 
					CBMessages.getString("FormCataloguesList.LastReleaseColumn"), 90, true, false, SWT.CENTER ); 
			break;
		case "valid_to": 
			break;
		case "status": 
			// Add the "Status" column
			GlobalUtil.addStandardColumn( table, new CatalogueStatusLabelProvider(), 
					CBMessages.getString("FormCataloguesList.StatusColumn"), 150, true, false, SWT.CENTER ); 
			break;
		case "reserve": 
			// Add the "reserved by" column
			GlobalUtil.addStandardColumn( table, new CatalogueUsernameLabelProvider(), 
					CBMessages.getString("FormCataloguesList.ReserveColumn"), 115, true, false, SWT.CENTER );
			break;
		}
	}

	/*==========================================
	 * 
	 * COLUMN LABEL PROVIDERS
	 * In the following the label provider of 
	 * all the table columns are implemented. The
	 * structure is always the same, the only difference
	 * among them lies on what is shown in the 
	 * getText method
	 * 
	 * 
	 ==========================================*/


	/**
	 * Label provider for the catalogue code column
	 * @author avonva
	 *
	 */
	private class CatalogueLabelLabelProvider extends ColumnLabelProvider {

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

			Catalogue catalogue = ( Catalogue ) arg0;

			return catalogue.getLabel();
		}
	}


	/**
	 * Label provider for the catalogue name column
	 * @author avonva
	 *
	 */
	private class CatalogueVersionLabelProvider extends ColumnLabelProvider {

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

			Catalogue catalogue = ( Catalogue ) arg0;

			return String.valueOf( catalogue.getVersion() );
		}	
	}



	/**
	 * Label provider for the catalogue scopenote column
	 * @author avonva
	 *
	 */
	private class CatalogueScopeNoteLabelProvider extends ColumnLabelProvider {

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

			Catalogue catalogue = ( Catalogue ) arg0;

			return catalogue.getScopenotes();
		}
	}


	/**
	 * Label provider for the catalogue status column
	 * @author avonva
	 *
	 */
	private class CatalogueStatusLabelProvider extends ColumnLabelProvider {

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

			Catalogue catalogue = ( Catalogue ) arg0;

			return catalogue.getStatus();
		}
	}


	/**
	 * Label provider for the catalogue valid from
	 * @author avonva
	 *
	 */
	private class CatalogueValidFromLabelProvider extends ColumnLabelProvider {

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

			// get the last release date as year-month-day
			Catalogue catalogue = ( Catalogue ) arg0;
			Date date = catalogue.getValidFrom();
			DateFormat sdf = new SimpleDateFormat( FormObjectsList.STD_DATE_FORMAT ); 
			return sdf.format( date );
		}
	}

	/**
	 * Label provider for the catalogue reserved by
	 * @author avonva
	 *
	 */
	private class CatalogueUsernameLabelProvider extends ColumnLabelProvider {

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

			Catalogue catalogue = ( Catalogue ) arg0;
			return catalogue.getReserveUsername();
		}
	}
}
