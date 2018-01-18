package ui_main_panel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;

import catalogue_browser_dao.CatalogueEntityDAO;
import catalogue_object.SortableCatalogueObject;
import messages.Messages;
import property.ContentProviderProperty;
import property.LabelProviderProperty;
import property.SorterDCFProperty;
import session_manager.BrowserWindowPreferenceDao;
import utilities.GlobalUtil;
import window_restorer.RestoreableWindow;

public abstract class CatalogueObjectEditor<T extends SortableCatalogueObject> {

	private RestoreableWindow window;
	private String windowCode;
	private Shell shell;
	private Shell dialog;

	private String title;

	private ArrayList<T> objects;
	private Collection<T> objectsToRemove;

	/**
	 * Initialize the editor variables
	 * @param shell parent shell
	 */
	public CatalogueObjectEditor( Shell shell, String windowCode, ArrayList<T> objects, 
			String title ) {
		this.shell = shell;
		this.windowCode = windowCode;
		this.objects = objects;
		this.title = title;
		this.objectsToRemove = new ArrayList<>();
	}

	/**
	 * display the form
	 */
	public void display() {

		// new form
		dialog = new Shell( shell, SWT.SHELL_TRIM | SWT.APPLICATION_MODAL );

		dialog.setText( title );
		dialog.setSize( 600, 400 );
		dialog.setLayout( new GridLayout( 1 , false ) );
		
		window = new RestoreableWindow(dialog, windowCode);

		Group g = new Group( dialog , SWT.NONE );
		g.setLayout( new GridLayout( 1 , false ) );

		Composite commands = new Composite( g , SWT.NONE );

		GridData gridData = new GridData();
		gridData.verticalAlignment = SWT.TOP;
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = false;
		commands.setLayoutData( gridData );

		commands.setLayout( new GridLayout( 4 , false ) );

		// add button
		final Button commandAdd = new Button( commands , SWT.TOGGLE );
		commandAdd.setText( Messages.getString("Editor.AddCmd") );
		gridData = new GridData();
		gridData.verticalAlignment = SWT.TOP;
		gridData.horizontalAlignment = SWT.LEFT;
		gridData.grabExcessHorizontalSpace = false;
		gridData.grabExcessVerticalSpace = false;
		commandAdd.setLayoutData( gridData );

		// remove button
		final Button commandRemove = new Button( commands , SWT.TOGGLE );
		commandRemove.setText( Messages.getString("Editor.RemoveCmd") );
		gridData = new GridData();
		gridData.verticalAlignment = SWT.TOP;
		gridData.horizontalAlignment = SWT.LEFT;
		gridData.grabExcessHorizontalSpace = false;
		gridData.grabExcessVerticalSpace = false;
		commandRemove.setLayoutData( gridData );

		// move up button
		Button bUp = new Button( commands , SWT.PUSH );
		bUp.setText( Messages.getString("Editor.MoveUpCmd") );
		bUp.pack();
		gridData = new GridData();
		gridData.verticalAlignment = SWT.FILL;
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = false;
		gridData.grabExcessVerticalSpace = true;
		bUp.setLayoutData( gridData );

		// move down button
		Button bDown = new Button( commands , SWT.PUSH );
		bDown.setText( Messages.getString("Editor.MoveDownCmd") );
		bDown.pack();
		gridData = new GridData();
		gridData.verticalAlignment = SWT.FILL;
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = false;
		gridData.grabExcessVerticalSpace = true;
		bDown.setLayoutData( gridData );

		commands.pack();

		// table which shows the objects
		final TableViewer table = new TableViewer( g , SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION );
		table.getTable().setHeaderVisible( true );

		table.setContentProvider( new ContentProviderProperty() );
		table.setLabelProvider( new LabelProviderProperty() );
		table.setSorter( new SorterDCFProperty() );

		// create the table columns
		createColumns ( table );

		// set the table input
		table.setInput( objects );
		
		gridData = new GridData();
		gridData.verticalAlignment = SWT.FILL;
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		table.getTable().setLayoutData( gridData );

		gridData = new GridData();
		gridData.verticalAlignment = SWT.FILL;
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		g.setLayoutData( gridData );
		
		
		Composite c = new Composite( dialog , SWT.NONE );
		gridData = new GridData();
		gridData.verticalAlignment = SWT.CENTER;
		gridData.horizontalAlignment = SWT.CENTER;
		gridData.grabExcessHorizontalSpace = true;
		c.setLayoutData( gridData );

		c.setLayout( new GridLayout( 2 , false ) );

		// ok button
		Button okBtn = new Button( c , SWT.PUSH );
		okBtn.setText( Messages.getString("Editor.OkButton") );
		okBtn.pack();
		gridData = new GridData();
		gridData.verticalAlignment = SWT.FILL;
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		okBtn.setLayoutData( gridData );

		// cancel button
		Button cancelBtn = new Button( c , SWT.PUSH );
		cancelBtn.setText( Messages.getString("Editor.CancelButton") );
		cancelBtn.pack();
		gridData = new GridData();
		gridData.verticalAlignment = SWT.FILL;
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		cancelBtn.setLayoutData( gridData );
		c.pack();


		// add listeners
		setAddListener ( commandAdd, table, objects );
		setRemoveListener ( commandRemove, table );
		setOkListener ( okBtn, table, objects );
		setCancelListener ( cancelBtn, dialog );
		setMoveListener ( bUp, table, objects, true );
		setMoveListener ( bDown, table, objects, false );

		dialog.setMaximized( false );
		dialog.pack();

		// restore window dimensions to previous
		window.restore( BrowserWindowPreferenceDao.class );
		window.saveOnClosure( BrowserWindowPreferenceDao.class );

		dialog.open();

		while ( !dialog.isDisposed() ) {
			if ( !dialog.getDisplay().readAndDispatch() )
				dialog.getDisplay().sleep();
		}

		dialog.dispose();
	}
	
	public Shell getShell() {
		return shell;
	}

	/**
	 * Add the add listener to the add button
	 * @param addBtn
	 * @param table
	 */
	private void setAddListener ( Button addBtn, final TableViewer table,
			final Collection<T> objs ) {

		// if the add button is pressed
		addBtn.addSelectionListener( new SelectionAdapter() {
			@Override
			public void widgetSelected ( SelectionEvent event ) {

				// create a default object and add it
				T obj = createNewObject();
				objs.add ( obj );
				table.refresh();
			}
		} );
	}

	/**
	 * Set the remove listener to the remove button
	 * @param removeBtn
	 * @param table
	 */
	private void setRemoveListener ( Button removeBtn, final TableViewer table ) {

		// if the remove button is pressed
		removeBtn.addSelectionListener( new SelectionAdapter() {
			@Override
			public void widgetSelected ( SelectionEvent event ) {

				// if empty selection then return
				if ( table.getSelection().isEmpty() ) {

					GlobalUtil.showErrorDialog( shell, 
							Messages.getString("Editor.ErrorTitle"), 
							Messages.getString("Editor.ErrorMessage"));
					return;
				}

				IStructuredSelection selection = (IStructuredSelection) table.getSelection();

				// get the selected obj
				@SuppressWarnings("unchecked")
				T selectedObj = (T) selection.getFirstElement();

				// return if cannot remove object
				if ( !canRemove( selectedObj ) )
					return;

				// Are you sure you want to delete the obj? 
				int val = GlobalUtil.showDialog( shell, 
						Messages.getString("Editor.DeleteWarningTitle"), 
						Messages.getString("Editor.DeleteWarningMessage"), 
						SWT.YES | SWT.NO );

				// return if we want to cancel the operation
				if ( val == SWT.NO )
					return;

				// remove the object from the current list
				objects.remove( selectedObj );
				table.refresh();

				// add the objects into the list of objs to be removed
				// but only if it is an object we already added in the DB
				if ( selectedObj.getId() != -1 )
					objectsToRemove.add( selectedObj );
			}
		} );
	}
	
	/**
	 * Set the move up/down listener
	 * @param button the button which adds the listener
	 * @param table the table which contains the objects
	 * @param objs the table objects
	 * @param moveUp true for move up, false for move down
	 */
	private void setMoveListener ( Button button, final TableViewer table, 
			final ArrayList<T> objs, final boolean moveUp ) {
		
		button.addSelectionListener( new SelectionAdapter() {
			@Override
			public void widgetSelected ( SelectionEvent event ) {

				// order elements
				Collections.sort( objects, new SorterDCFProperty() );
				
				// get the table selection and get first element
				IStructuredSelection selection = (IStructuredSelection) table.getSelection();
				
				// return if no selection
				if ( selection.isEmpty() )
					return;

				@SuppressWarnings("unchecked")
				T current = (T) selection.getFirstElement();
				T target = null;
				
				// find the target
				for ( int i = 0; i < objs.size(); ++i ) {

					if ( moveUp ) {
						
						// get the previous element
						if ( objs.get( i ).equals( current ) && i > 0 ) {
							target = objs.get( i - 1 );
							break;
						}
					}
					else if ( !moveUp ) {
						// get the next element
						if ( objs.get( i ).equals( current ) && i < objs.size() - 1 ) {
							target = objs.get( i + 1 );
							break;
						}
					}
				}
				
				// if something found go on and swap
				if ( target == null )
					return;

				// swap orders
				int cOrder = current.getOrder();
				current.setOrder( target.getOrder() );
				target.setOrder( cOrder );
				
				// refresh the visualization
				table.refresh();
			}
		} );
	}
	
	/**
	 * Ok listener for the ok button
	 * @param okBtn
	 * @param table
	 * @param objs
	 */
	private void setOkListener ( Button okBtn, final TableViewer table, 
			final Collection<T> objs ) {

		// if the ok button is pressed we perform the insertions/updates and remotions of hierarchies
		okBtn.addSelectionListener( new SelectionAdapter() {
			@Override
			public void widgetSelected ( SelectionEvent event ) {

				// first I have to check that no duplicated code is present
				boolean doubleCode = false;
				
				// double codes? check this
				for ( T obj1 : objs ) {
					
					int occurrences = 0;
					
					for ( T obj2 : objs ) {
						if ( obj1.getCode().equals( obj2.getCode() ) )
							occurrences++;
					}
					
					// if two or more equal codes => error
					if ( occurrences >= 2 ) {
						doubleCode = true;
						break;
					}
				}

				// if a duplicate is found I show the error
				if ( doubleCode ) {
					GlobalUtil.showErrorDialog( shell, 
							Messages.getString("Editor.DuplicatedCodesTitle"), 
							Messages.getString("Editor.DuplicatedCodesMessage"));
					return;
				}
				
				// validate objects
				boolean goOn = true;
				for ( T obj : objs ) {
					if ( !validateObject(obj) ) {
						goOn = false;
						break;
					}
				}
				
				// stop if needed and reset the table content
				if ( !goOn ) {
					objects = reset();
					table.refresh();
					return;
				}

				CatalogueEntityDAO<T> dao = getDao();

				// Remove all the object which need to be removed
				for ( T obj : objectsToRemove ) 
					dao.remove( obj );
				
				// insert or update all the new/updated hierarchies
				for ( T obj : objs ) {
					
					// convention, if id = -1 then it 
					// is a new object for the database
					if ( obj.getId() == -1 )
						dao.insert( obj );
					else
						dao.update( obj );
				}

				refresh();
				dialog.close();
			}
		} );
	}
	
	/**
	 * Cancel listener for cancel button
	 * @param cancelBtn
	 * @param dialog
	 */
	private void setCancelListener ( Button cancelBtn, final Shell dialog ) {
		
		// if the cancel button is pressed close the dialog
		cancelBtn.addSelectionListener( new SelectionAdapter() {
			@Override
			public void widgetSelected ( SelectionEvent event ) {
				dialog.close();
			}
		} );
	}

	/**
	 * Create the table columns
	 * @param table
	 */
	public abstract void createColumns ( TableViewer table );

	/**
	 * Create a new object and add it to the db
	 */
	public abstract T createNewObject ();

	/**
	 * Check if the selected object can be removed or not
	 * @param obj
	 * @return
	 */
	public abstract boolean canRemove ( T obj );
	
	/**
	 * Validate an object, can this object be used and possibly
	 * added to the catalogue objects?
	 * @param obj
	 * @return
	 */
	public abstract boolean validateObject ( T obj );
	
	/**
	 * Refresh action called at the end of the process,
	 * after calling the ok button.
	 */
	public abstract void refresh();
	
	/**
	 * Reset the content of the table if
	 * {@link #validateObject(SortableCatalogueObject)}
	 * is not passed
	 * @return the list of objects which will
	 * reset the table content
	 */
	public abstract ArrayList<T> reset();
	
	/**
	 * Get the object dao
	 * @return
	 */
	public abstract CatalogueEntityDAO<T> getDao();
}
