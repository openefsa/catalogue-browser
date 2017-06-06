package ui_main_panel;
import java.util.ArrayList;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

import catalogue.Catalogue;
import catalogue_browser_dao.AttributeDAO;
import catalogue_object.Attribute;
import global_manager.GlobalManager;
import messages.Messages;
import property.ContentProviderProperty;
import property.EditingSupportSimpleProperty;
import property.LabelProviderDCFProperty;
import property.LabelProviderProperty;
import session_manager.RestoreableWindow;
import session_manager.WindowPreference;
import utilities.GlobalUtil;

public class AttributeEditor implements RestoreableWindow {

	private static final String WINDOW_CODE = "AttributeEditor";
	private Shell dialog;
	private Shell _shell;

	ArrayList< Attribute > _attributes;

	private ArrayList< Attribute > _attributesToRemove;
	
	private Listener updateListener;

	
	/**
	 * Set the listener which is called when a graphics update is necessary
	 * @param listener
	 */
	public void addUpdateListener ( Listener listener ) {
		updateListener = listener;
	}
	
	/**
	 * GUI Creation for editing attribute = Facets.
	 */
	public void Display ( ) {

		dialog = new Shell( _shell, SWT.SHELL_TRIM | SWT.APPLICATION_MODAL );

		dialog.setText( Messages.getString("FormAttribute.DialogTitle") ); //$NON-NLS-1$
		dialog.setSize( 600, 400 );
		dialog.setLayout( new GridLayout( 1 , false ) );

		Group g = new Group( dialog , SWT.NONE );
		g.setLayout( new GridLayout( 1 , false ) );

		Composite commands = new Composite( g , SWT.NONE );

		GridData gridData = new GridData();
		gridData.verticalAlignment = SWT.TOP;
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = false;
		commands.setLayoutData( gridData );

		commands.setLayout( new GridLayout( 2 , false ) );

		final Button commandAdd = new Button( commands , SWT.TOGGLE );
		commandAdd.setText( Messages.getString("FormAttribute.AddCmd") ); //$NON-NLS-1$
		gridData = new GridData();
		gridData.verticalAlignment = SWT.TOP;
		gridData.horizontalAlignment = SWT.LEFT;
		gridData.grabExcessHorizontalSpace = false;
		gridData.grabExcessVerticalSpace = false;
		commandAdd.setLayoutData( gridData );

		final Button commandRemove = new Button( commands , SWT.TOGGLE );
		commandRemove.setText( Messages.getString("FormAttribute.RemoveCmd") ); //$NON-NLS-1$
		gridData = new GridData();
		gridData.verticalAlignment = SWT.TOP;
		gridData.horizontalAlignment = SWT.LEFT;
		gridData.grabExcessHorizontalSpace = false;
		gridData.grabExcessVerticalSpace = false;
		commandRemove.setLayoutData( gridData );

		commands.pack();

		final TableViewer table = new TableViewer( g , SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION );
		table.getTable().setHeaderVisible( true );

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

		table.setContentProvider( new ContentProviderProperty() );
		table.setLabelProvider( new LabelProviderProperty() );

		
		// add code column
		TableViewerColumn codeCol = GlobalUtil.addStandardColumn( table, new LabelProviderDCFProperty("Code"), 
				Messages.getString("HierarchyEditor.CodeColumn"), 100 );
		codeCol.setEditingSupport( new EditingSupportSimpleProperty( table , "Code" ) ); //$NON-NLS-1$
		
		// add name column
		TableViewerColumn nameCol = GlobalUtil.addStandardColumn( table, new LabelProviderDCFProperty("Name"), 
				Messages.getString("HierarchyEditor.NameColumn") );
		nameCol.setEditingSupport( new EditingSupportSimpleProperty( table , "Name" ) );
		
		// add label column
		TableViewerColumn labelCol = GlobalUtil.addStandardColumn( table, new LabelProviderDCFProperty("Label"), 
				"Label" );
		labelCol.setEditingSupport( new EditingSupportSimpleProperty( table , "Label" ) );

		// add scopenotes column
		TableViewerColumn noteCol = GlobalUtil.addStandardColumn( table, new LabelProviderDCFProperty("Scopenotes"), 
				"Scopenotes" );
		noteCol.setEditingSupport( new EditingSupportSimpleProperty( table , "Scopenotes" ) );
		
		// add Reportable column
		TableViewerColumn reportableCol = GlobalUtil.addStandardColumn( table, new LabelProviderDCFProperty("Reportable"), 
				"Reportable", 80 );
		reportableCol.setEditingSupport( new EditingSupportSimpleProperty( table , "Reportable" ) );

		// add Visible column
		TableViewerColumn visibleCol = GlobalUtil.addStandardColumn( table, new LabelProviderDCFProperty("Visible"), 
				"Visible", 80 );
		visibleCol.setEditingSupport( new EditingSupportSimpleProperty( table , "Visible" ) );

		// add Searchable column
		TableViewerColumn searchableCol = GlobalUtil.addStandardColumn( table, new LabelProviderDCFProperty("Searchable"), 
				"Searchable", 80 );
		searchableCol.setEditingSupport( new EditingSupportSimpleProperty( table , "Searchable" ) );

		// add order column
		TableViewerColumn orderCol = GlobalUtil.addStandardColumn( table, new LabelProviderDCFProperty("Order"), 
				"Order", 50, SWT.CENTER );
		orderCol.setEditingSupport( new EditingSupportSimpleProperty( table , "Order" ) );

		// add type column
		TableViewerColumn typeCol = GlobalUtil.addStandardColumn(table, new LabelProviderDCFProperty("Type"), 
				Messages.getString("HierarchyEditor.TypeColumn"), 100, SWT.CENTER );
		typeCol.setEditingSupport( new EditingSupportSimpleProperty( table , "Type" ) );
		
		// add Maxlength column
		TableViewerColumn lengthCol = GlobalUtil.addStandardColumn( table, new LabelProviderDCFProperty("Maxlength"), 
				"Max Length", 50, SWT.CENTER );
		lengthCol.setEditingSupport( new EditingSupportSimpleProperty( table , "Maxlength" ) );
		
		// add Precision column
		TableViewerColumn precisionCol = GlobalUtil.addStandardColumn( table, new LabelProviderDCFProperty("Precision"), 
				"Precision", 50, SWT.CENTER );
		precisionCol.setEditingSupport( new EditingSupportSimpleProperty( table , "Precision" ) );

		// add Scale column
		TableViewerColumn scaleCol = GlobalUtil.addStandardColumn( table, new LabelProviderDCFProperty("Scale"), 
				"Scale", 50, SWT.CENTER );
		scaleCol.setEditingSupport( new EditingSupportSimpleProperty( table , "Scale" ) );

		// add Catalogue Code column
		TableViewerColumn catCodeCol = GlobalUtil.addStandardColumn( table, new LabelProviderDCFProperty("catcode"), 
				"Catalogue Code", 100, SWT.CENTER );
		catCodeCol.setEditingSupport( new EditingSupportSimpleProperty( table , "catcode" ) );

		// add single_repeatable column
		TableViewerColumn singRepCol = GlobalUtil.addStandardColumn( table, new LabelProviderDCFProperty("single_repeatable"), 
				"Single/Repeatable", 100, SWT.CENTER );
		singRepCol.setEditingSupport( new EditingSupportSimpleProperty( table , "single_repeatable" ) );

		// add Inheritance column
		TableViewerColumn inheritanceCol = GlobalUtil.addStandardColumn( table, new LabelProviderDCFProperty("Inheritance"), 
				"Inheritance", 80, SWT.CENTER );
		inheritanceCol.setEditingSupport( new EditingSupportSimpleProperty( table , "Inheritance" ) );

		// add Uniqueness column
		TableViewerColumn uniqueCol = GlobalUtil.addStandardColumn( table, new LabelProviderDCFProperty("Uniqueness"), 
				"Uniqueness", 80, SWT.CENTER );
		uniqueCol.setEditingSupport( new EditingSupportSimpleProperty( table , "Uniqueness" ) );		
		
		// add termcodealias column
		TableViewerColumn aliasCol = GlobalUtil.addStandardColumn( table, new LabelProviderDCFProperty("termcodealias"), 
				"Term Code Alias", 80, SWT.CENTER );
		aliasCol.setEditingSupport( new EditingSupportSimpleProperty( table , "termcodealias" ) );	
		

		table.setInput( _attributes );

		Composite c = new Composite( dialog , SWT.NONE );
		gridData = new GridData();
		gridData.verticalAlignment = SWT.CENTER;
		gridData.horizontalAlignment = SWT.CENTER;
		gridData.grabExcessHorizontalSpace = true;
		// gridData.grabExcessVerticalSpace =true;
		c.setLayoutData( gridData );

		c.setLayout( new GridLayout( 2 , false ) );

		Button bOk = new Button( c , SWT.PUSH );
		bOk.setText( Messages.getString("FormAttribute.OkButton") ); //$NON-NLS-1$
		bOk.pack();
		gridData = new GridData();
		gridData.verticalAlignment = SWT.FILL;
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		bOk.setLayoutData( gridData );

		Button bCancel = new Button( c , SWT.PUSH );
		bCancel.setText( Messages.getString("FormAttribute.CancelButton") ); //$NON-NLS-1$
		bCancel.pack();
		gridData = new GridData();
		gridData.verticalAlignment = SWT.FILL;
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		bCancel.setLayoutData( gridData );
		c.pack();

		
		// is add is pressed
		commandAdd.addSelectionListener( new SelectionAdapter() {
			@Override
			public void widgetSelected ( SelectionEvent event ) {
				
				Attribute sp = Attribute.getDefaultAttribute();
				_attributes.add( sp );
				table.refresh();
			}

		} );

		
		// if remove is pressed
		commandRemove.addSelectionListener( new SelectionAdapter() {
			@Override
			public void widgetSelected ( SelectionEvent event ) {

				if ( table.getSelection().isEmpty() ) {
					
					GlobalUtil.showErrorDialog( _shell, 
							Messages.getString("FormAttribute.HierarchyErrorTitle"), 
							Messages.getString("FormAttribute.HierarchyErrorMessage"));
					return;
				}

				// get the selected attribute
				Attribute selectedAttribute = (Attribute) ( (IStructuredSelection) ( table.getSelection() ) )
						.getFirstElement();


				/* Are you sure you want to delete the hierarchy? */
				MessageBox mb = new MessageBox( _shell , SWT.YES | SWT.NO );
				mb.setText( Messages.getString("FormAttribute.RemoveWarningTitle") ); //$NON-NLS-1$
				mb.setMessage( Messages.getString("FormAttribute.RemoveWarningMessage") ); //$NON-NLS-1$
				int val = mb.open();

				if ( val == SWT.NO )
					return;

				// remove the attribute from the view
				_attributes.remove( selectedAttribute );
				table.refresh();
	
				// add the attribute to the attributes to be removed
				// remove from the db only if was already inserted before
				if ( selectedAttribute.getId() != -1 )
					_attributesToRemove.add( selectedAttribute );

			}

		} );

		
		// if ok is pressed make all actions permanent
		bOk.addSelectionListener( new SelectionAdapter() {
			@Override
			public void widgetSelected ( SelectionEvent event ) {

				// I have to check that no duplicated code is present
				boolean found = false;
				if ( _attributes.size() > 1 ) {
					// Only if I have more then 1 element
					for ( int i = 0 ; ( !found ) && ( i < _attributes.size() ) ; i++ ) {
						for ( int j = i + 1 ; ( !found ) && ( j < _attributes.size() ) ; j++ ) {
							if ( _attributes.get( i ).getCode().compareTo( _attributes.get( j ).getCode() ) == 0 ) {
								found = true;
								break;
							}
						}
					}
				}

				// if a duplicate is found I present the error
				if ( found ) {
					GlobalUtil.showErrorDialog( _shell, 
							Messages.getString("FormAttribute.DuplicatedCodesTitle"), 
							Messages.getString("FormAttribute.DuplicatedCodesMessage"));
					return;
				}
				
				// get an instance of the global manager
				GlobalManager manager = GlobalManager.getInstance();
				
				// get the current catalogue
				Catalogue currentCat = manager.getCurrentCatalogue();
				
				// initialize dao of attributes
				AttributeDAO attrDao = new AttributeDAO( currentCat );
				
				// Detect changes, update database and UI
				for ( int i = 0 ; i < _attributes.size() ; i++ ) {
					
					// current attribute
					Attribute p = _attributes.get( i );

					// if id is -1 the element is new, otherwise the element is just
					// to update
					if ( p.getId() == -1 )
						attrDao.insert( p );
					else
						attrDao.update( p );
				}

				// Detect delete and delete attributes
				for ( int i = 0 ; i < _attributesToRemove.size() ; i++ ) {
					try {
						attrDao.remove( _attributesToRemove.get( i ) );
					} catch ( Exception e ) {
						GlobalUtil.showErrorDialog( _shell, 
								Messages.getString("FormAttribute.RemovePropertyErrorTitle"), 
								e.getMessage() );
						return;
					}
				}

				// update the terms attributes also in RAM
				currentCat.refreshTermAttributes();
				
				// call the update listener if it was set
				if ( updateListener != null ) {
					updateListener.handleEvent( new Event() );
				}
				
				dialog.close();

			}

		} );

		// if cancel is pressed close the dialog
		bCancel.addSelectionListener( new SelectionAdapter() {
			@Override
			public void widgetSelected ( SelectionEvent event ) {
				dialog.close();
			}

		} );
		
		dialog.setMaximized( false );
		dialog.pack();
		
		// restore windows dimensions
		WindowPreference.restore( this );
		WindowPreference.saveOnClosure( this );
		
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

	public AttributeEditor( Shell shell, ArrayList< Attribute > attributes ) {
		_shell = shell;
		_attributes = attributes;
		_attributesToRemove = new ArrayList< Attribute >();
	}

}