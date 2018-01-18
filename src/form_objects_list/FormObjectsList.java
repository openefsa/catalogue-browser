package form_objects_list;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
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

import messages.Messages;
import session_manager.BrowserWindowPreferenceDao;
import ui_general_graphics.TableResizer;
import window_restorer.RestoreableWindow;

public abstract class FormObjectsList<T> {

	private RestoreableWindow window;
	private String windowCode;
	
	public static final String STD_DATE_FORMAT = "yyyy-MM-dd";
	
	// strings used to identify the buttons from their parent composite
	private static final String OK_KEY = "okButton"; 
	private static final String CANCEL_KEY = "cancelButton";
	
	private String okButtonText = Messages.getString("FormCataloguesList.DownloadCmd");
	private String cancelButtonText = Messages.getString("FormCataloguesList.CancelCmd");

	private String title;                        // shell title
	private Shell shell;                         // parent shell
	private Shell dialog;
	private TableViewer table;
	
	private Collection <T> objs;  // input parameter
	private Collection <T> selectedObjs;
	private boolean multiSel;      // multiple selection on or off?
	private Listener innerListener; // listener called when a catalogue is selected
	
	/**
	 * Initialize the form parameters
	 * @param shell, the parent shell
	 * @param title, the title of the window
	 * @param catalogues, the catalogues list from which we can choose
	 * @multiSel can we perform a multiple selection?
	 */
	public FormObjectsList( Shell shell, String windowCode, String title, 
			Collection <T> objs, boolean multiSel ) {
		this.selectedObjs = new ArrayList<>();
		this.shell = shell;
		this.windowCode = windowCode;
		this.title = title;
		this.objs = objs;
		this.multiSel = multiSel;
	}

	public FormObjectsList( Shell shell, String windowCode, String title, Collection <T> objs ) {
		this ( shell, windowCode, title, objs, true );
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
		
		window = new RestoreableWindow(dialog, windowCode);

		// ### catalogue table ###

		// create the table which displays the catalogue information
		table = createTable ( dialog, columns );

		// set the table input
		table.setInput( objs );

		table.addDoubleClickListener( createOkClickListener( dialog ) );

		// ### user buttons ###

		// create the buttons used for choosing ok or cancel
		Composite buttonsComposite = createChooseButtons ( dialog );

		// get the ok button from the composite
		Button okButton = (Button) getCompositeChildByKey ( buttonsComposite, OK_KEY );

		// get the cancel button from the composite
		Button cancelButton = (Button) getCompositeChildByKey ( buttonsComposite, CANCEL_KEY );


		// ### buttons listeners ###

		// set the listener for the ok button
		okButton.addSelectionListener( createOkListener( dialog ) );

		// set the listener for the cancel button
		cancelButton.addSelectionListener( createCancelListener( dialog ) );

		// resize the dialog to the preferred size (the hints)
		dialog.pack();

		// restore the preferred settings if present
		window.restore( BrowserWindowPreferenceDao.class );

		// save the window dimensions when close
		window.saveOnClosure( BrowserWindowPreferenceDao.class );

		// show the dialog
		dialog.setVisible( true );  
		dialog.open();
		
		TableResizer resizer = new TableResizer( table.getTable() );
		resizer.apply();
		
		// Event loop
		while ( !dialog.isDisposed() ) {
			if ( !dialog.getDisplay().readAndDispatch() )
				dialog.getDisplay().sleep();
		}
		
		dialog.dispose();
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
		gridData.grabExcessVerticalSpace = false;
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
	private TableViewer createTable ( Composite parent, String[] columns ) {

		// create a table to show the catalogues information
		// SWT.FULL_SELECTION is set to select the entire row of the table, independently of the selected column
		// This was done since we have to select a single catalogue, not a catalogue column

		// we create a check box table viewer for multiple selection, otherwise we create a standard
		// single selection table
		final TableViewer table;
		if ( multiSel )
			table = CheckboxTableViewer.newCheckList( dialog, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | 
					SWT.FULL_SELECTION );
		else
			table = new TableViewer ( dialog, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | 
					SWT.FULL_SELECTION | SWT.SINGLE );

		// set the content provider of the table
		table.setContentProvider( new ObjectContentProvider<T>() );

		// set the layout data for the table (note: these will be used also for the dialog)
		GridData gridData = new GridData();
		gridData.minimumHeight = 250;
		gridData.heightHint = 300;
		gridData.widthHint = 600;
		gridData.minimumWidth = 300;
		gridData.verticalAlignment = SWT.FILL;
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;

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
	 * Get the selected objects
	 * @return
	 */
	public Collection<T> getSelection() {
		return selectedObjs;
	}
	
	/**
	 * Get the first selected objects
	 * @return
	 */
	public T getFirstSelection() {

		if ( selectedObjs == null || selectedObjs.isEmpty() )
			return null;
		
		return selectedObjs.iterator().next();
	}
	
	/**
	 * Content provider for the catalogue table
	 * @author avonva
	 *
	 */
	private class ObjectContentProvider<E> implements IStructuredContentProvider {

		@Override
		public void dispose() {}

		@Override
		public void inputChanged(Viewer arg0, Object arg1, Object arg2) {}

		@SuppressWarnings("unchecked")
		@Override
		public Object[] getElements(Object arg0) {

			if ( arg0 instanceof ArrayList<?> ) {

				return (  ( (ArrayList <E>) arg0).toArray() );
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
	private void callListener ( Collection<T> elements ) {

		// if no listener was set => return
		if ( innerListener == null )
			return;

		// create a new event and
		Event event = new Event();

		// set as data the selected catalogues if multisel
		// otherwise set the unique selected catalogue

		if ( multiSel )
			event.data = elements;
		else
			event.data = elements.iterator().next();

		// then call the listener with the just created event
		innerListener.handleEvent( event );
	}


	/**
	 * Get the selected catalogue starting from a selection object
	 * @param selection
	 * @return
	 */
	private ArrayList<T> getElements () {

		// If check box table viewer get the checked elements
		if ( multiSel )
			return getCheckedElem();

		// otherwise, if we have a simple table viewer, get the selected elements
		return getSelectedElem();
	}


	/**
	 * Get the selected catalogues, works only with tableviewer with single or multi selection
	 * @param viewer
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private ArrayList<T> getSelectedElem () {

		// output array
		ArrayList<T> selObj = new ArrayList<>();

		// get the selection
		ISelection selection = table.getSelection();

		// return if something is not correct
		if ( selection.isEmpty() || !( selection instanceof IStructuredSelection ) )
			return null;

		// get the selection of the table specifying it as structured
		Iterator<?> iterator = ( (IStructuredSelection) selection ).iterator();

		// add all the selected elements
		while ( iterator.hasNext() ) {
			selObj.add( (T) iterator.next() );
		}

		return selObj;
	}

	/**
	 * Get all the checked catalogues, works only with check box table viewer
	 * @param viewer
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private ArrayList<T> getCheckedElem () {

		ArrayList <T> selObj = new ArrayList<>();

		// get all the checked catalogues and return them
		for ( Object cat : ((CheckboxTableViewer) table).getCheckedElements() )
			selObj.add( (T) cat );

		return selObj;
	}

	/**
	 * Perform the actions needed to select a catalogue (download), that is,
	 * get the selected catalogue and call the Class listener to inform
	 * outside the class that a catalogue has been selected.
	 * @param dialog
	 * @param viewer
	 */
	private void performOkActions ( Shell dialog ) {

		// get the selected/checked catalogue from the table viewer
		selectedObjs = getElements ();

		// if no catalogue was selected warn the user and stop the operation
		if ( selectedObjs == null || selectedObjs.isEmpty() ) {

			// warn the user
			MessageBox mb = new MessageBox ( dialog, SWT.ICON_WARNING );
			mb.setText( Messages.getString("FormObjList.WarningTitle") );
			mb.setMessage( Messages.getString("FormObjList.WarningMessage") );
			mb.open();

			return;
		}

		// close the dialog
		dialog.close();

		// call the Class listener to notify the parent of the selected catalogue
		callListener ( selectedObjs );
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
	private SelectionListener createOkListener ( final Shell dialog ) {

		// create the selection listener
		SelectionListener okListener = new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {

				performOkActions( dialog );
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
	private IDoubleClickListener createOkClickListener ( final Shell dialog ) {

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
					performOkActions( dialog );
			}
		};

		return listener;
	}
	
	/**
	 * Add columns to the table using a key-column convention
	 * @param table
	 * @param key
	 */
	public abstract void addColumnByKey ( TableViewer table, String key );
}
