package ui_main_panel;
import java.util.ArrayList;
import java.util.Iterator;

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
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

import catalogue_browser_dao.HierarchyDAO;
import catalogue_object.Catalogue;
import catalogue_object.Hierarchy;
import global_manager.GlobalManager;
import messages.Messages;
import property.ContentProviderProperty;
import property.EditingSupportSimpleProperty;
import property.LabelProviderDCFProperty;
import property.LabelProviderProperty;
import property.SorterDCFProperty;
import session_manager.RestoreableWindow;
import session_manager.WindowPreference;
import utilities.GlobalUtil;

// Editor of Facets and Hierarchies. Tools-->Hierarchies Editor
// Open a dialog window which allows to modify the hierarchies and the facets of FoodEx

public class HierarchyEditor implements RestoreableWindow {

	private static final String WINDOW_CODE = "HierarchyEditor";
	private Shell _shell;
	private Shell dialog;

	private ArrayList< Hierarchy > _hierarchies;

	private ArrayList< Hierarchy > _hierarchiesToRemove;

	/**
	 * Apre una finestra per creare la form per l'editing delle Hierarchies e
	 * dei Facets
	 * 
	 */
	public void Display ( ) {
		
		dialog = new Shell( _shell, SWT.SHELL_TRIM | SWT.APPLICATION_MODAL );

		dialog.setText( Messages.getString("HierarchyEditor.HierarchyFacetLabel") ); //$NON-NLS-1$
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

		commands.setLayout( new GridLayout( 4 , false ) );

		final Button commandAdd = new Button( commands , SWT.TOGGLE );
		commandAdd.setText( Messages.getString("HierarchyEditor.AddCmd") ); //$NON-NLS-1$
		gridData = new GridData();
		gridData.verticalAlignment = SWT.TOP;
		gridData.horizontalAlignment = SWT.LEFT;
		gridData.grabExcessHorizontalSpace = false;
		gridData.grabExcessVerticalSpace = false;
		commandAdd.setLayoutData( gridData );
		

		final Button commandRemove = new Button( commands , SWT.TOGGLE );
		commandRemove.setText( Messages.getString("HierarchyEditor.RemoveCmd") ); //$NON-NLS-1$
		gridData = new GridData();
		gridData.verticalAlignment = SWT.TOP;
		gridData.horizontalAlignment = SWT.LEFT;
		gridData.grabExcessHorizontalSpace = false;
		gridData.grabExcessVerticalSpace = false;
		commandRemove.setLayoutData( gridData );
		

		Button bUp = new Button( commands , SWT.PUSH );
		bUp.setText( Messages.getString("HierarchyEditor.MoveUpCmd") ); //$NON-NLS-1$
		bUp.pack();
		gridData = new GridData();
		gridData.verticalAlignment = SWT.FILL;
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = false;
		gridData.grabExcessVerticalSpace = true;
		bUp.setLayoutData( gridData );

		Button bDown = new Button( commands , SWT.PUSH );
		bDown.setText( Messages.getString("HierarchyEditor.MoveDownCmd") ); //$NON-NLS-1$
		bDown.pack();
		gridData = new GridData();
		gridData.verticalAlignment = SWT.FILL;
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = false;
		gridData.grabExcessVerticalSpace = true;
		bDown.setLayoutData( gridData );

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

		// add type column
		TableViewerColumn typeCol = GlobalUtil.addStandardColumn(table, new LabelProviderDCFProperty("Applicability"), 
				Messages.getString("HierarchyEditor.ApplicabilityColumn"), 100, SWT.CENTER );
		typeCol.setEditingSupport( new EditingSupportSimpleProperty( table , "Applicability" ) ); //$NON-NLS-1$
		
		// add order column
		TableViewerColumn orderCol = GlobalUtil.addStandardColumn( table, new LabelProviderDCFProperty("Order"), 
				"Order", 50, SWT.CENTER );
		orderCol.setEditingSupport( new EditingSupportSimpleProperty( table , "Order" ) );

		// add status column
		TableViewerColumn statusCol = GlobalUtil.addStandardColumn( table, new LabelProviderDCFProperty("Status"), 
				"Status", 50 );
		statusCol.setEditingSupport( new EditingSupportSimpleProperty( table , "Status" ) );
		
		
		// sort the element by the order
		table.setSorter( new SorterDCFProperty() );

		table.setInput( _hierarchies );

		Composite c = new Composite( dialog , SWT.NONE );
		gridData = new GridData();
		gridData.verticalAlignment = SWT.CENTER;
		gridData.horizontalAlignment = SWT.CENTER;
		gridData.grabExcessHorizontalSpace = true;
		// gridData.grabExcessVerticalSpace =true;
		c.setLayoutData( gridData );

		c.setLayout( new GridLayout( 2 , false ) );

		Button bOk = new Button( c , SWT.PUSH );
		bOk.setText( Messages.getString("HierarchyEditor.OkButton") ); //$NON-NLS-1$
		bOk.pack();
		gridData = new GridData();
		gridData.verticalAlignment = SWT.FILL;
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		bOk.setLayoutData( gridData );

		Button bCancel = new Button( c , SWT.PUSH );
		bCancel.setText( Messages.getString("HierarchyEditor.CancelButton") ); //$NON-NLS-1$
		bCancel.pack();
		gridData = new GridData();
		gridData.verticalAlignment = SWT.FILL;
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		bCancel.setLayoutData( gridData );
		c.pack();

		
		// if the add button is pressed
		commandAdd.addSelectionListener( new SelectionAdapter() {
			@Override
			public void widgetSelected ( SelectionEvent event ) {
				
				// create a default hierarchy and add to the list of hierarchies
				Hierarchy sp = Hierarchy.getDefaultHierarchy();
				_hierarchies.add( sp );
				table.refresh();
			}
		} );

		
		// if the remove button is pressed
		commandRemove.addSelectionListener( new SelectionAdapter() {
			@Override
			public void widgetSelected ( SelectionEvent event ) {

				// if empty selection then return
				if ( table.getSelection().isEmpty() ) {

					GlobalUtil.showErrorDialog( _shell, 
							Messages.getString("HierarchyEditor.HierarchyErrorTitle"), 
							Messages.getString("HierarchyEditor.HierarchyErrorMessage"));
					return;
				}

				// get the selected hierarchy
				Hierarchy selectedHierarchy = (Hierarchy) ( (IStructuredSelection) ( table.getSelection() ) )
						.getFirstElement();

				// check if the selected hierarchy is the master hierarchy
				// if so, return we cannot remove master
				if ( selectedHierarchy.isMaster() ) {
					GlobalUtil.showErrorDialog( _shell, 
							Messages.getString("HierarchyEditor.DeleteHierarchyErrorTitle"), 
							Messages.getString("HierarchyEditor.DeleteHierarchyErrorMessage"));
					return;
				}

				// Are you sure you want to delete the hierarchy? 
				MessageBox mb = new MessageBox( _shell , SWT.YES | SWT.NO );
				mb.setText( Messages.getString("HierarchyEditor.DeleteHierarchyWarningTitle") ); //$NON-NLS-1$
				mb.setMessage( Messages.getString("HierarchyEditor.DeleteHierarchyWarningMessage") ); //$NON-NLS-1$
				int val = mb.open();
				
				// return if we want to cancel the operation
				if ( val == SWT.NO )
					return;
				
				
				// remove the hierarchy from the current list of hierarchies
				_hierarchies.remove( selectedHierarchy );
				table.refresh();
				
				// add the hierarchy into the list of hierarchies to be removed
				// but only if it is a hierarchy we already added in the DB
				if ( selectedHierarchy.getId() != -1 )
					_hierarchiesToRemove.add( selectedHierarchy );
			}
		} );

		// if the ok button is pressed we perform the insertions/updates and remotions of hierarchies
		bOk.addSelectionListener( new SelectionAdapter() {
			@Override
			public void widgetSelected ( SelectionEvent event ) {

				// first I have to check that no duplicated code is present
				boolean dupcodefound = false;
				for ( int i = 0 ; ( !dupcodefound ) && ( i < _hierarchies.size() ) ; i++ ) {
					for ( int j = i ; ( !dupcodefound ) && ( j < _hierarchies.size() ) ; j++ ) {
						if ( i != j ) {
							if ( _hierarchies.get( i ).getCode().compareTo( _hierarchies.get( j ).getCode() ) == 0 ) {
								dupcodefound = true;
							}
						}
					}
				}
				
				// if a duplicate is found I show the error
				if ( dupcodefound ) {
					GlobalUtil.showErrorDialog(_shell, 
							Messages.getString("HierarchyEditor.DuplicatedCodesTitle"), 
							Messages.getString("HierarchyEditor.DuplicatedCodesMessage"));
					return;
				}

				// get an instance of the global manager
				GlobalManager manager = GlobalManager.getInstance();
				
				// get the current catalogue
				Catalogue currentCat = manager.getCurrentCatalogue();
				
				// I am checking that the default is an hierarchy and not a
				// facet
				for ( int i = 0 ; i < _hierarchies.size() ; i++ ) {

					if ( _hierarchies.get( i ).isMaster() ) {
						// if it is not an hierarchy the change cannot be done
						if ( !_hierarchies.get( i ).isHierarchy() ) {
							
							// dialog and exit
							GlobalUtil.showErrorDialog( _shell, 
									Messages.getString("HierarchyEditor.FacetErrorTitle"), 
									Messages.getString("HierarchyEditor.FacetErrorMessage"));

							setHierarchies( currentCat.getHierarchies() );
							table.refresh();
							return;
						}
					}
				}
				
				HierarchyDAO hierDao = new HierarchyDAO( currentCat );
			
				// Remove all the hierarchies that have to be removed
				for ( int i = 0 ; i < _hierarchiesToRemove.size() ; i++ ) 
					hierDao.remove( _hierarchiesToRemove.get( i ) );
				
				// insert or update all the new/updated hierarchies
				for ( int i = 0 ; i < _hierarchies.size() ; i++ ) {
					
					Hierarchy hierarchy = _hierarchies.get( i );
					
					// convention, if id = -1 then it is a new hierarchy for the database
					if ( hierarchy.getId() == -1 )
						hierDao.insert( hierarchy );
					else
						hierDao.update( hierarchy );
				}
				
				// refetch all the hierarchies
				currentCat.refreshHierarchies();
				
				// TODO create a listener to update the foodex browser combo box
				dialog.close();

			}

		} );

		// if the cancel button is pressed close the dialog
		bCancel.addSelectionListener( new SelectionAdapter() {
			@Override
			public void widgetSelected ( SelectionEvent event ) {
				dialog.close();
			}

		} );

		
		bUp.addSelectionListener( new SelectionAdapter() {
			@Override
			public void widgetSelected ( SelectionEvent event ) {

				if ( table.getSelection().isEmpty() )
					return;

				for ( Iterator< ? > iterator = ( (IStructuredSelection) ( table
						.getSelection() ) ).iterator() ; iterator.hasNext() ; ) {
					
					Hierarchy current = (Hierarchy) iterator.next();
					/* find the previous term */
					Hierarchy previous = null;
					for ( int i = 0 ; i < _hierarchies.size() ; i++ ) {
						Hierarchy test = _hierarchies.get( i );
						if ( test.getOrder() < current.getOrder() ) {
							// good potential previous;
							if ( previous == null )
								previous = test;
							else
								// if test sorting id is higher that the
								// previous sorting id it is a better candidate
								// as previous
								if ( test.getOrder() > previous.getOrder() )
									previous = test;

						}
					}
					if ( previous == null )
						return;
					int curSortId = current.getOrder();
					current.setOrder( previous.getOrder() );
					previous.setOrder( curSortId );
					table.refresh();
				}

			}

		} );

		bDown.addSelectionListener( new SelectionAdapter() {
			@Override
			public void widgetSelected ( SelectionEvent event ) {

				if ( table.getSelection().isEmpty() )
					return;


				for ( Iterator< ? > iterator = ( (IStructuredSelection) ( table
						.getSelection() ) ).iterator() ; iterator.hasNext() ; ) {
					Hierarchy current = (Hierarchy) iterator.next();
					/* find the next term */
					Hierarchy next = null;
					for ( int i = 0 ; i < _hierarchies.size() ; i++ ) {
						Hierarchy test = _hierarchies.get( i );
						if ( test.getOrder() > current.getOrder() ) {
							// good potential previous;
							if ( next == null )
								next = test;
							else
								// if test sorting id is lower that the previous
								// sorting id it is a better candidate as next
								if ( test.getOrder() < next.getOrder() )
									next = test;

						}
					}
					if ( next == null )
						return;
					int curSortId = current.getOrder();
					current.setOrder( next.getOrder() );
					next.setOrder( curSortId );
					table.refresh();
				}
			}
		} );

		dialog.setMaximized( false );
		dialog.pack();
		
		// restore window dimensions to previous
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
	
	/**
	 * svuota le hierarchies in cache e copia le nuove hierarchies passategli.
	 * 
	 * @param hierarchies
	 */
	private void setHierarchies ( ArrayList< Hierarchy > hierarchies ) {
		
		_hierarchies.clear();
		_hierarchies = hierarchies;
	}

	public HierarchyEditor( Shell shell, ArrayList< Hierarchy > hierarchies ) {
		_shell = shell;
		_hierarchies = new ArrayList< Hierarchy >();
		_hierarchiesToRemove = new ArrayList< Hierarchy >();
		this.setHierarchies( hierarchies );
	}
}


