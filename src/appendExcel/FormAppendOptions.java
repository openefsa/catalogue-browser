package appendExcel;

import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

import catalogue.Catalogue;
import catalogue_object.Hierarchy;
import global_manager.GlobalManager;
import messages.Messages;

public class FormAppendOptions {

	Shell						_shell;
	Display						_display;
	Shell						dialog;                 // dialog of the form
	public ArrayList< String >	Sheets;                 // sheets to be visualized in the form
	
	private boolean             appendNewCodes = true;  // should new codes be appended?
	private boolean             colorErrors    = false; // should errors be colored in the sheet?
	private boolean             createLog      = true;  // should a log for errors be created?
	private String				SelectedSheet	= null; // sheet that was selected by the user

	private ArrayList <Hierarchy> selectedHierarchies = new ArrayList<>();
	

	public FormAppendOptions() {
		Sheets = new ArrayList< String >();
	}
	
	/**
	 * Retrieve from the outside if new codes should be appended or not
	 * @return
	 */
	public boolean getAppendNewCodes() {
		return appendNewCodes;
	}
	
	public String getSelectedSheet() {
		return SelectedSheet;
	}
	
	public boolean getColorErrors ( ) {
		return colorErrors;
	}
	
	public boolean getCreateLog() {
		return createLog;
	}
	
	/**
	 * Return the selected hierarchies and facets list
	 * @return
	 */
	public ArrayList<Hierarchy> getSelectedHierarchies () {
		return selectedHierarchies;
	}
	
	public void Display ( Shell shell ) {
		final Shell dialog = new Shell( shell , SWT.APPLICATION_MODAL | SWT.SHELL_TRIM );

		dialog.setText( Messages.getString("FormAppendOptions.DialogTitle") ); //$NON-NLS-1$
		dialog.setSize( 600, 700 );
		dialog.setLayout( new GridLayout( 1 , false ) );
		
		Group sheetComposite = new Group( dialog, SWT.APPLICATION_MODAL | SWT.SHELL_TRIM );
		sheetComposite.setText( Messages.getString("FormAppendOptions.SheetTitle")); //$NON-NLS-1$
		
		sheetComposite.setLayout( new GridLayout( 2, false ) );
		
		final List list = new List( sheetComposite , SWT.SINGLE | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL );

		GridData gridData = new GridData();
		gridData.verticalAlignment = SWT.CENTER;
		gridData.horizontalAlignment = SWT.CENTER;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		list.setLayoutData( gridData );
		for ( int i = 0 ; i < Sheets.size() ; i++ ) {
			list.add( Sheets.get( i ) );
		}

		sheetComposite.setLayoutData( gridData );
		
		Composite checkComposite = new Composite( sheetComposite, SWT.NONE );
		checkComposite.setLayout( new GridLayout(1, false) );
		
		// checkbox for choosing if a new sheet will be created with the
		// new codes
		Button appendNewCodesCheckbox = new Button( checkComposite , SWT.CHECK );
		appendNewCodesCheckbox.setText( Messages.getString("FormAppendOptions.Option1Name")); //$NON-NLS-1$
		appendNewCodesCheckbox.setToolTipText( Messages.getString("FormAppendOptions.TooltipOption1Pt1") //$NON-NLS-1$
				+ Messages.getString("FormAppendOptions.TooltipOption1Pt2")); //$NON-NLS-1$
		
		appendNewCodesCheckbox.setSelection( true );  // default it is true
		
		// if check uncheck => update the flag
		appendNewCodesCheckbox.addSelectionListener( new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				Button btn = (Button) e.getSource();
				appendNewCodes = btn.getSelection();
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {}
		});
		
		// checkbox for choosing if terms with errors should be colored in the excel file
		Button logCreationCheckbox = new Button( checkComposite , SWT.CHECK );
		logCreationCheckbox.setText( Messages.getString("FormAppendOptions.Option2Name")); //$NON-NLS-1$
		logCreationCheckbox.setToolTipText( Messages.getString("FormAppendOptions.TooltipOption2")); //$NON-NLS-1$

		logCreationCheckbox.setSelection( true );  // default it is false

		// if check uncheck => update the flag
		logCreationCheckbox.addSelectionListener( new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				Button btn = (Button) e.getSource();
				createLog = btn.getSelection();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {}
		});

		// checkbox for choosing if terms with errors should be colored in the excel file
		Button colorErrorsCheckbox = new Button( checkComposite , SWT.CHECK );
		colorErrorsCheckbox.setText( Messages.getString("FormAppendOptions.Option3Name")); //$NON-NLS-1$
		colorErrorsCheckbox.setToolTipText( Messages.getString("FormAppendOptions.TooltipOption3")); //$NON-NLS-1$

		colorErrorsCheckbox.setSelection( false );  // default it is false

		// if check uncheck => update the flag
		colorErrorsCheckbox.addSelectionListener( new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				Button btn = (Button) e.getSource();
				colorErrors = btn.getSelection();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {}
		});

		

		
		Label label = new Label( dialog, SWT.NONE );
		label.setText( Messages.getString("FormAppendOptions.HierarchyLabel")); //$NON-NLS-1$
		
		// create a table for checking the hierarchies that we want to use for the append
		final Table table = new Table( dialog, SWT.CHECK | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL );
		
		// get an instance of the global manager
		GlobalManager manager = GlobalManager.getInstance();
		
		// get the current catalogue
		Catalogue currentCat = manager.getCurrentCatalogue();
		
		for ( Hierarchy hierarchy : currentCat.getHierarchies() ) {
			TableItem item = new TableItem( table, SWT.NONE );
			item.setText( hierarchy.getName() );
			item.setData( hierarchy );
		}
	
		gridData = new GridData();
		gridData.verticalAlignment = SWT.CENTER;
		gridData.horizontalAlignment = SWT.CENTER;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		gridData.minimumHeight = 200;
		gridData.heightHint = 400;
		table.setLayoutData( gridData );
		
		gridData = new GridData();
		gridData.verticalAlignment = SWT.CENTER;
		gridData.horizontalAlignment = SWT.CENTER;
		label.setLayoutData( gridData );
		
		Composite c = new Composite( dialog , SWT.NONE );
		gridData = new GridData();
		gridData.verticalAlignment = SWT.CENTER;
		gridData.horizontalAlignment = SWT.CENTER;
		gridData.grabExcessHorizontalSpace = true;
		// gridData.grabExcessVerticalSpace =true;
		c.setLayoutData( gridData );

		c.setLayout( new GridLayout( 2 , false ) );

		final Button bOk = new Button( c , SWT.PUSH );
		bOk.setText( Messages.getString("FormAppendOptions.OkButton") ); //$NON-NLS-1$
		bOk.setToolTipText( Messages.getString("FormAppendOptions.TooltipOkButton")); //$NON-NLS-1$
		bOk.pack();
		gridData = new GridData();
		gridData.verticalAlignment = SWT.FILL;
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		bOk.setLayoutData( gridData );
		bOk.setEnabled( false );

		Button bCancel = new Button( c , SWT.PUSH );
		bCancel.setText( Messages.getString("FormAppendOptions.CancelButton") ); //$NON-NLS-1$
		bCancel.pack();
		gridData = new GridData();
		gridData.verticalAlignment = SWT.FILL;
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		bCancel.setLayoutData( gridData );
		c.pack();

		bOk.addSelectionListener( new SelectionAdapter() {
			@Override
			public void widgetSelected ( SelectionEvent event ) {
				// Detect changes, update database and UI
				if ( list.getSelection().length > 0 ) {
					SelectedSheet = list.getSelection()[0];
					dialog.close();
				}
			}
		} );
		
		// When checks occurs
		table.addListener( SWT.Selection, new Listener() {

			@Override
			public void handleEvent(Event event) {
				
				// only checks actions
				if ( event.detail != SWT.CHECK )
					return;
				
				// get the hierarchy related to the item
				Hierarchy hierarchy = (Hierarchy) event.item.getData();
				
				// add and remove the hierarchies accordingly to the 
				// checked elements
				if ( ( (TableItem) event.item ).getChecked() )
					selectedHierarchies.add( hierarchy );
				else
					selectedHierarchies.remove( hierarchy );
				
				// update the ok button
				bOk.setEnabled( getSelectedHierarchies().size() > 0 && 
						list.getSelection().length > 0 );
			}
		});
	
		
		// Disable Ok button if no hierarchy is selected and no sheet 
		list.addSelectionListener( new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				bOk.setEnabled( getSelectedHierarchies().size() > 0 && 
						list.getSelection().length > 0 );
				}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {}
		});

		bCancel.addSelectionListener( new SelectionAdapter() {
			@Override
			public void widgetSelected ( SelectionEvent event ) {
				dialog.close();
			}

		} );

		dialog.open();

		while ( !dialog.isDisposed() ) {
			if ( !shell.getDisplay().readAndDispatch() )
				shell.getDisplay().sleep();
		}
		
		dialog.dispose();

	}
}
