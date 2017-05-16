package ui_main_panel;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

import catalogue_object.Catalogue;
import messages.Messages;
import session_manager.RestoreableWindow;
import session_manager.WindowPreference;
import utilities.GlobalUtil;


/**
 * This form displays all the catalogues passed by the input parameter. In particular,
 * it shows several catalogues properties and allows selecting one catalogue to make 
 * further action on it.
 * @author Valentino
 *
 */
public class FormCataloguesList implements RestoreableWindow {

	// strings used to identify the buttons from their parent composite
	private static final String OK_KEY = "okButton"; 
	private static final String CANCEL_KEY = "cancelButton";
	private static final String WINDOW_CODE = "FormCataloguesList";
	
	private String okButtonText = Messages.getString("FormCataloguesList.DownloadButton");
	private String cancelButtonText = Messages.getString("FormCataloguesList.CancelButton");

	private String title;                        // shell title
	private Shell shell;                         // parent shell
	private Shell dialog;
	private ArrayList < Catalogue > catalogues;  // input parameter
	private boolean multiSel;            // multiple selection on or off?
	private Listener innerListener;              // listener called when a catalogue is selected
	
	/**
	 * Initialize the form parameters
	 * @param shell, the parent shell
	 * @param title, the title of the window
	 * @param catalogues, the catalogues list from which we can choose
	 * @multiSel can we perform a multiple selection?
	 */
	public FormCataloguesList( Shell shell, String title, ArrayList < Catalogue > catalogues, boolean multiSel ) {
		this.shell = shell;
		this.title = title;
		this.catalogues = catalogues;
		this.multiSel = multiSel;
	}
	
	public FormCataloguesList( Shell shell, String title, ArrayList < Catalogue > catalogues ) {
		this ( shell, title, catalogues, true );
	}

	/**
	 * Display the form and initialize graphics
	 */
	public void display( String[] columns ) {

		// create a dialog and set its title
		this.dialog = new Shell( shell , SWT.TITLE | SWT.APPLICATION_MODAL | SWT.SHELL_TRIM );
		dialog.setText( title );

		// set the dialog layout
		dialog.setLayout( new GridLayout( 1 , false ) );
		
		// ### catalogue table ###
		
		// create the table which displays the catalogue information
		final TableViewer table = createCatalogueTable ( dialog, columns );

		// set the table input
		table.setInput( catalogues );
		
		table.addDoubleClickListener( createOkClickListener( dialog, table ) );

		
		// ### user buttons ###

		// create the buttons used for choosing ok or cancel
		Composite buttonsComposite = createChooseButtons ( dialog );

		// get the ok button from the composite
		Button okButton = (Button) getCompositeChildByKey ( buttonsComposite, OK_KEY );

		// get the cancel button from the composite
		Button cancelButton = (Button) getCompositeChildByKey ( buttonsComposite, CANCEL_KEY );

		
		// ### buttons listeners ###

		// set the listener for the ok button
		okButton.addSelectionListener( createOkListener( dialog, table ) );

		// set the listener for the cancel button
		cancelButton.addSelectionListener( createCancelListener( dialog ) );

		// resize the dialog to the preferred size (the hints)
		dialog.pack();
		
		// restore the preferred settings if present
		WindowPreference.restore( this );
		
		// save the window dimensions when close
		WindowPreference.saveOnClosure( this );
		
		// show the dialog
		dialog.setVisible( true );  
		dialog.open();
	}
	
	@Override
	public String getWindowCode() {
		return WINDOW_CODE;
	}
	
	@Override
	public Shell getWindowShell() {
		return dialog;
	}
	
	/**
	 * Set the cancel button text
	 * @param cancelButtonText
	 */
	public void setCancelButtonText(String cancelButtonText) {
		this.cancelButtonText = cancelButtonText;
	}
	
	/**
	 * Set the Ok button text
	 * @param okButtonText
	 */
	public void setOkButtonText(String okButtonText) {
		this.okButtonText = okButtonText;
	}

	/**
	 * Create all the buttons used to make a decision on the selected catalogue
	 * @param parent
	 * @return
	 */
	private Composite createChooseButtons ( Composite parent ) {

		// create a composite for hosting the buttons
		Composite buttonsComposite = new Composite( parent, SWT.NONE );
		buttonsComposite.setLayout( new RowLayout() );
		
		GridData gridData = new GridData();
		gridData.horizontalAlignment = SWT.CENTER;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		buttonsComposite.setLayoutData( gridData );

		// add the ok button to the composite
		Button okButton = new Button ( buttonsComposite, SWT.NONE );
		okButton.setText( okButtonText );

		// set the data to be able to recognize the element from the composite
		okButton.setData( OK_KEY );


		// add the cancel button to the composite
		Button cancelButton = new Button ( buttonsComposite, SWT.NONE );
		cancelButton.setText( cancelButtonText );

		// set the data to be able to recognize the element from the composite
		cancelButton.setData( CANCEL_KEY );

		// return the composite
		return buttonsComposite;
	}
	

	/**
	 * Create the table viewer which contains the catalogues information
	 * The table contains also several columns to display different fields (customizable
	 * inserting the right key string in the columns variable)
	 * @param parent
	 * @return
	 */
	private TableViewer createCatalogueTable ( Composite parent, String[] columns ) {

		// create a table to show the catalogues information
		// SWT.FULL_SELECTION is set to select the entire row of the table, independently of the selected column
		// This was done since we have to select a single catalogue, not a catalogue column
		
		// we create a check box table viewer for multiple selection, otherwise we create a standard
		// single selection table
		TableViewer table;
		if ( multiSel )
			table = CheckboxTableViewer.newCheckList( parent, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | 
					SWT.FULL_SELECTION );
		else
			table = new TableViewer ( parent, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | 
					SWT.FULL_SELECTION | SWT.SINGLE );

		// set the content provider of the table
		table.setContentProvider( new CatalogueContentProvider() );

		// set the layout data for the table (note: these will be used also for the dialog)
		GridData gridData = new GridData();
		gridData.minimumHeight = 400;
		gridData.heightHint = 500;
		gridData.widthHint = 600;
		gridData.minimumWidth = 300;
		gridData.verticalAlignment = SWT.FILL;
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = false;

		// set the layout data of the table
		table.getTable().setLayoutData( gridData );

		// make the columns names visible
		table.getTable().setHeaderVisible( true );


		// ### TABLE COLUMNS ###
		
		// Add column by key
		for ( String columnKey : columns ) {
			addColumnByKey ( table, columnKey );
		}

		return table;
	}

	/**
	 * Add columns by key into the table
	 * TODO IMPLEMENT THE UNIMPLEMENTED COLUMNS IF NECESSARY
	 * @param table
	 * @param columnKey
	 */
	private void addColumnByKey ( TableViewer table, String columnKey ) {
		
		switch ( columnKey.toLowerCase() ) {
		case "code": 
			break;
		case "name": 
			break;
		case "label": 
			// Add the "Label" column
			GlobalUtil.addStandardColumn( table, new CatalogueLabelLabelProvider(), 
					Messages.getString("FormCataloguesList.NameColumn"), 225, true, false ); 
			break;
		case "scopenote": 
			// Add the "Scopenote" column
			GlobalUtil.addStandardColumn( table, new CatalogueScopeNoteLabelProvider(), 
					Messages.getString("FormCataloguesList.ScopenoteColumn"), 300, true, false ); 
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
					Messages.getString("FormCataloguesList.VersionColumn"), 100, true, false, SWT.CENTER ); 
			break;
		case "last_update": 
			break;
		case "valid_from": 
			// Add the "Last release" column
			GlobalUtil.addStandardColumn( table, new CatalogueValidFromLabelProvider(), 
					Messages.getString("FormCataloguesList.LastReleaseColumn"), 90, true, false, SWT.CENTER ); 
			break;
		case "valid_to": 
			break;
		case "status": 
			// Add the "Status" column
			GlobalUtil.addStandardColumn( table, new CatalogueStatusLabelProvider(), 
					Messages.getString("FormCataloguesList.StatusColumn"), 150, true, false, SWT.CENTER ); 
			break;
		case "reserve": 
			// Add the "reserved by" column
			GlobalUtil.addStandardColumn( table, new CatalogueUsernameLabelProvider(), 
					Messages.getString("FormCataloguesList.ReserveColumn"), 100, true, false, SWT.CENTER );
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
			DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd"); 
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


	/**
	 * Content provider for the catalogue table
	 * @author avonva
	 *
	 */
	private class CatalogueContentProvider implements IStructuredContentProvider {

		@Override
		public void dispose() {}

		@Override
		public void inputChanged(Viewer arg0, Object arg1, Object arg2) {}

		@SuppressWarnings("unchecked")
		@Override
		public Object[] getElements(Object arg0) {

			if ( arg0 instanceof ArrayList<?> ) {

				return (  ( (ArrayList <Catalogue>) arg0).toArray() );
			}
			return null;
		}
	}


	/**
	 * Set the selection listener which is called when the ok button is pressed
	 * @param listener
	 */
	public void addListener ( Listener listener ) {
		
		if ( listener == null )
			return;
		
		this.innerListener = listener;
	}


	/**
	 * Call the Class listener if it was set (i.e. call the general
	 * listener of the FormCataloguesList class)
	 * The listener event will contain the selected catalogues as array list
	 * if multiSel = true. Otherwise, it will contain the only selected 
	 * catalogue as Catalogue if multisel=false
	 * @param catalogues, the list of selected/checked catalogues
	 */
	private void callListener ( ArrayList<Catalogue> catalogues ) {

		// if no listener was set => return
		if ( innerListener == null )
			return;

		// create a new event and
		Event event = new Event();

		// set as data the selected catalogues if multisel
		// otherwise set the unique selected catalogue
		
		if ( multiSel )
			event.data = catalogues;
		else
			event.data = catalogues.get( 0 );

		// then call the listener with the just created event
		innerListener.handleEvent( event );
	}


	/**
	 * Get the selected catalogue starting from a selection object
	 * @param selection
	 * @return
	 */
	private ArrayList<Catalogue> getCatalogues ( TableViewer viewer ) {

		// If check box table viewer get the checked elements
		if ( multiSel )
			return getCheckedCatalogues( (CheckboxTableViewer) viewer );		
		
		// otherwise, if we have a simple table viewer, get the selected elements
		return getSelectedCatalogues( viewer );
	}

	
	/**
	 * Get the selected catalogues, works only with tableviewer with single or multi selection
	 * @param viewer
	 * @return
	 */
	private ArrayList<Catalogue> getSelectedCatalogues ( TableViewer viewer ) {
		
		// output array
		ArrayList <Catalogue> selectedCats = new ArrayList<>();
		
		// get the selection
		ISelection selection = viewer.getSelection();

		// return if something is not correct
		if ( selection.isEmpty() || !( selection instanceof IStructuredSelection ) )
			return null;

		// get the selection of the table specifying it as structured
		Iterator<?> iterator = ( (IStructuredSelection) selection ).iterator();

		// add all the selected elements
		while ( iterator.hasNext() ) {
			selectedCats.add( (Catalogue) iterator.next() );
		}
		
		return selectedCats;
	}
	
	/**
	 * Get all the checked catalogues, works only with check box table viewer
	 * @param viewer
	 * @return
	 */
	private ArrayList<Catalogue> getCheckedCatalogues ( CheckboxTableViewer viewer ) {
		
		ArrayList <Catalogue> selectedCats = new ArrayList<>();

		// get all the checked catalogues and return them
		for ( Object cat : ((CheckboxTableViewer) viewer).getCheckedElements() )
			selectedCats.add( (Catalogue) cat );
		
		return selectedCats;
	}

	/**
	 * Perform the actions needed to select a catalogue (download), that is,
	 * get the selected catalogue and call the Class listener to inform
	 * outside the class that a catalogue has been selected.
	 * @param dialog
	 * @param viewer
	 */
	private void performOkActions ( Shell dialog, TableViewer viewer ) {
		
		// get the selected/checked catalogue from the table viewer
		ArrayList<Catalogue> catalogues = getCatalogues ( viewer );
		
		// if no catalogue was selected warn the user and stop the operation
		if ( catalogues == null || catalogues.isEmpty() ) {
			
			// warn the user
			MessageBox mb = new MessageBox ( dialog, SWT.ICON_WARNING );
			mb.setText( Messages.getString("FormCataloguesList.WarningTitle") );
			mb.setMessage( Messages.getString("FormCataloguesList.WarningMessage") );
			mb.open();
			
			return;
		}

		// close the dialog
		dialog.close();
		
		// call the Class listener to notify the parent of the selected catalogue
		callListener ( catalogues );
	}
	

	/**
	 * Search a child widget inside a parent composite
	 * @param parent
	 * @param childKey
	 * @return
	 */
	private Object getCompositeChildByKey ( Composite parent, String childKey ) {

		// for each child of the composite
		for ( Control widget : parent.getChildren() ) {

			// if the child is the one we are searching return
			if ( widget.getData().equals( childKey ) ) {
				return widget;
			}
		}

		return null;
	}


	/**
	 * Create the listener which is called by a cancel operation
	 * @param dialog
	 * @return
	 */
	private SelectionListener createCancelListener ( final Shell dialog ) {

		// create the listener which closes the dialog if called
		SelectionListener listener = new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				dialog.close();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {}
		};

		return listener;
	}


	/**
	 * Create the listener for a ok action (i.e. a catalogue is selected)
	 * NOTE: We cannot pass as parameter only the ISelection instead of the viewer! In fact,
	 * when if the selection changes the ISelection object would not be updated! Only the viewer
	 * is updated (and its selection)
	 *  
	 * @param dialog, dialog which hosts the graphics
	 * @param selection, selection of the table
	 * @param innerSelectionListener, the general listener of the class
	 * @return
	 */
	private SelectionListener createOkListener ( final Shell dialog, final TableViewer viewer ) {
		
		// create the selection listener
		SelectionListener okListener = new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				
				performOkActions( dialog, viewer );
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {}
		};
		
		return okListener;
	}
	
	/**
	 * Same as createOkListener, but for a double click event
	 * @param dialog
	 * @param viewer
	 * @return
	 */
	private IDoubleClickListener createOkClickListener ( final Shell dialog, final TableViewer viewer ) {
		
		IDoubleClickListener listener = new IDoubleClickListener() {
			
			@Override
			public void doubleClick(DoubleClickEvent arg0) {
				
				if ( multiSel ) {
					
				/*
					
					// get the selected elements from the table
					Object element = ( (IStructuredSelection) viewer.getSelection() )
							.getFirstElement();
					
					
					// get if it is checked or not
					boolean isChecked = ( (CheckboxTableViewer) viewer ).getChecked( element );
					
					// invert the check
					( (CheckboxTableViewer) viewer ).setChecked( element, !isChecked );*/
				}
				else  // if single selection => select the catalogue
					performOkActions( dialog, viewer );
			}
		};
		
		return listener;
	}
}
