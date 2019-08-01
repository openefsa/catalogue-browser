package user_preferences;

import java.util.Collection;

import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ICheckStateProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;

import catalogue.Catalogue;
import messages.Messages;
import session_manager.BrowserWindowPreferenceDao;
import ui_search_bar.SearchOptionDAO;
import utilities.GlobalUtil;
import window_restorer.RestoreableWindow;

public class FormSearchOptions {
	
	private RestoreableWindow window;
	private static final String WINDOW_CODE = "FormSearchOptions";
	
	private Catalogue catalogue;
	
	private Shell shell;
	private Shell dialog;
	private String title;
	
	/**
	 * Constructor
	 * @param parentShell the shell of the form which calls this form
	 * @param title the shell title
	 * @param catalogue the catalogue we are working with (we pick the options from it)
	 */
	public FormSearchOptions( Shell parentShell, String title, Catalogue catalogue ) {
		this.shell = parentShell;
		this.title = title;
		this.catalogue = catalogue;
	}

	/**
	 * Initialize and display the user interface
	 */
	public void display () {
		
		GridData dialogData = new GridData();
		dialogData.verticalAlignment = SWT.FILL;
		dialogData.horizontalAlignment = SWT.CENTER;
		
		// create a new shell
		dialog = new Shell( shell , SWT.SHELL_TRIM | SWT.APPLICATION_MODAL );
		
		window = new RestoreableWindow(dialog, WINDOW_CODE);
		
		// window icon (on the top left)
		dialog.setImage( new Image( Display.getCurrent() , this.getClass().getClassLoader()
				.getResourceAsStream( "Choose.gif" ) ) );

		dialog.setText( title );
		dialog.setLayout( new GridLayout(1, false) );
		dialog.setLayoutData( dialogData );

		// composite to host options tables
		Composite optionsComp = new Composite( dialog , SWT.NONE );
		optionsComp.setLayout( new GridLayout( 2, false ) );
		optionsComp.setLayoutData( dialogData );
		
		// open the search option dao to get search options
		// related to the catalogue
		SearchOptionDAO optDao = new SearchOptionDAO( catalogue );
		
		
		// table used to show term types
		final OptionTable typeTable = new OptionTable( optionsComp, 
				Messages.getString( "FormSearchOptions.TermType"),
				optDao.getByType( OptionType.TERM_TYPE ) );
		
		// when a check box changes its state, check if it is correct
		typeTable.setValidator( new CheckValidator() {
			
			@Override
			public boolean validate(CheckStateChangedEvent event) {

				if ( typeTable.isAllUnchecked() ) {

					// warning! you cannot remove all the term types,
					// otherwise no term will be found with the search

					GlobalUtil.showErrorDialog( dialog, 
							Messages.getString( "FormSearchOptions.NoTermTypeErrorTitle" ), 
							Messages.getString( "FormSearchOptions.NoTermTypeErrorMessage" ) );

					return false;
				}
				
				return true;
			}
		});
		
		
		// table used to show implicit attributes
		@SuppressWarnings("unused")
		OptionTable attrTable = new OptionTable( optionsComp, 
				Messages.getString( "FormSearchOptions.ImplicitAttribute" ),
				optDao.getByType( OptionType.ATTRIBUTE ) );
		
		
		// close button
		Button closeBtn = new Button ( dialog, SWT.PUSH );
		closeBtn.setText( Messages.getString( "FormSearchOptions.Close" ) );
		
		// close dialog if close is pressed
		closeBtn.addSelectionListener( new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				dialog.close();
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {}
		});
		
		dialog.pack();
		
		// restore old dimensions
		window.restore( BrowserWindowPreferenceDao.class );
		window.saveOnClosure( BrowserWindowPreferenceDao.class );
		
		// show the dialog
		dialog.setVisible( true );

	}
	
	
	/**
	 * Table to manage search options in a check box way
	 * @author avonva
	 *
	 */
	private class OptionTable {
	
		private CheckboxTableViewer table;
		private CheckValidator validator;
		
		/**
		 * Inizialize the option table giving its parent, label and input
		 * @param parent parent composite on which building the table
		 * @param label the label which will be created over the table
		 * @param input the input of the table
		 */
		public OptionTable( Composite parent, String label, Collection<SearchOption> input ) {
			
			validator = new CheckValidator();
			
			Composite tableComp = new Composite( parent , SWT.NONE );
			tableComp.setLayout( new GridLayout( 1, false ) );
			
			GridData gridData = new GridData();
			gridData.verticalAlignment = SWT.FILL;
			gridData.horizontalAlignment = SWT.CENTER;
			
			// Label over the table
			Label srcOption = new Label( tableComp , SWT.NONE );
			srcOption.setText( label );
			srcOption.setLayoutData( gridData );

			gridData = new GridData();
			gridData.verticalAlignment = SWT.FILL;
			gridData.horizontalAlignment = SWT.FILL;
			
			// check box table
			table = new CheckboxTableViewer( 
					new Table( tableComp , SWT.MULTI | SWT.CHECK | SWT.BORDER ) );

			table.getTable().setLayoutData( gridData );
			table.setContentProvider( new ContentProviderSearchOption() );
			table.setLabelProvider( new LabelProviderSearchOption() );

			// set the table input
			table.setInput( input );
			
			// update check list using the search option enabled field
			table.setCheckStateProvider( new ICheckStateProvider() {
				
				@Override
				public boolean isGrayed(Object arg0) {
					return false;
				}
				
				@Override
				public boolean isChecked(Object arg0) {
					return ( (SearchOption) arg0 ).isEnabled();
				}
			});
			
			table.addCheckStateListener( new ICheckStateListener() {
				
				@Override
				public void checkStateChanged(CheckStateChangedEvent arg0) {

					SearchOption opt = ( (SearchOption) arg0.getElement() );
					
					// if validator is ok
					if ( validator.validate( arg0 ) ) {

						// update the checked value in RAM
						opt.setEnabled( arg0.getChecked() );
						
						// update the value in DB
						SearchOptionDAO optDao = new SearchOptionDAO( opt.getCatalogue() );
						optDao.update( opt );
					} else {
						
						// reset the check state
						table.setChecked( opt, opt.isEnabled() );
					}
				}
			});
		}
		
		/**
		 * Validator used to check the correctness of checks
		 * It is called every time a check box changes its state
		 * @param validator
		 */
		public void setValidator( CheckValidator validator ) {
			this.validator = validator;
		}
		
		/**
		 * Check if all the elements are unchecked
		 * @return
		 */
		public boolean isAllUnchecked () {
			return table.getCheckedElements().length <= 0;
		}
	}
	
	/**
	 * Check validator used to validate the table checks
	 * @author avonva
	 *
	 */
	private class CheckValidator {
		
		public boolean validate ( CheckStateChangedEvent event ) {
			return true;
		}
	}
}
