package ui_describe;

import java.util.ArrayList;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;

import already_described_terms.DescribedTerm;
import catalogue.Catalogue;
import messages.Messages;
import session_manager.BrowserWindowPreferenceDao;
import utilities.GlobalUtil;
import window_restorer.RestoreableWindow;

public class FormDescribedTerms {

	private static final Logger LOGGER = LogManager.getLogger(FormDescribedTerms.class);
	
	private RestoreableWindow window;
	private static final String WINDOW_CODE = "FormDescribedTerms";
	
	private Shell _shell;
	private Shell _dialog;
	private String _title;

	private TableViewer baseTermsTable;    // table which contains all the items
	private Text searchTextBox = null;     // search the keyword in the list
	private Button findSearch = null;      // button to start the search
	private Button clearSearch = null;     // button to clear the search results
	
	
	private Catalogue catalogue;
	
	// ten elements in cache
	ArrayList<?> describedTerms = new ArrayList<>();
	
	ViewerFilter searchViewerFilter = null;
	
	
	/**
	 * Constructor
	 * @param parentShell, the shell of the form which calls this form
	 * @param title, the shell title
	 * @param filename the name of the file from which extracting the described term (recent or favourite)
	 * @param invertOrder should the order of the term be reversed? (used for visualizing recent terms starting
	 *                    from the more recent
	 */
	public FormDescribedTerms( Shell parentShell, String title, 
			Catalogue catalogue, ArrayList<?> describedTerms ) {

		_shell = parentShell;
		_title = title;
		this.catalogue = catalogue;
		this.describedTerms = describedTerms;
	}
	
	
	/**
	 * set a search filter. This is used for searching the selected term in the foodex
	 * main tree panel across all the element of the favourite picklist. In particular,
	 * we search across implicit and explicit facets
	 * @param filter
	 */
	public void setSearchFilter( ViewerFilter filter ) {
		
		searchViewerFilter = filter;
	}

	/**
	 * Remove the filter from the base terms table
	 */
	public void removeSearchFilter () {
		
		if ( searchViewerFilter == null )
			return;
		
		// remove the filter
		baseTermsTable.removeFilter( searchViewerFilter );
		
		searchViewerFilter = null;
		
		// enable search panel 
		if ( searchTextBox != null )
			searchTextBox.setEnabled( true );
		
		if ( findSearch != null )
			findSearch.setEnabled( true );
		
		if ( clearSearch != null )
			clearSearch.setEnabled( true );
	}
	
	/**
	 * Method called when the form is created
	 */
	public void display ( Catalogue catalogue ) {

		this.catalogue = catalogue;
		
		// Set the layout of the form
		_dialog = new Shell( _shell , SWT.SHELL_TRIM | SWT.APPLICATION_MODAL );
		
		window = new RestoreableWindow(_dialog, WINDOW_CODE);

		// window icon (on the top left)
		try {
			_dialog.setImage( new Image( Display.getCurrent() , this.getClass().getClassLoader()
					.getResourceAsStream( "Choose.gif" ) ) );
		}
		catch ( Exception e ) {
			e.printStackTrace();
			LOGGER.error("Cannot get image", e);
		}

		_dialog.setMaximized( true );

		_dialog.setText( _title );  // window title

		_dialog.setLayout( new GridLayout( 2, false ) );  // layout style

		_dialog.setSize( 800, 500 );  // default size

		GridData gridData = new GridData();
		gridData.verticalAlignment = SWT.FILL;
		gridData.grabExcessVerticalSpace = true;
		gridData.grabExcessHorizontalSpace = true;
		

		// window layout
		_dialog.setLayoutData( gridData );
		
		// composite which contains the base term table
		Composite baseTermComposite = new Composite( _dialog, SWT.NONE );

		baseTermComposite.setLayout( new GridLayout(1, false) );
		gridData.minimumWidth = 300;
		gridData.widthHint = 450;
		
		baseTermComposite.setLayoutData( gridData );


		Group searchComposite = new Group( baseTermComposite, SWT.NONE );
		searchComposite.setText( Messages.getString("FormRecentlyDescribe.SearchTermTitle") );
		searchComposite.setLayout( new GridLayout(3, false) );

		
		searchTextBox = new Text ( searchComposite, SWT.SEARCH );
		searchTextBox.setEditable( true );
		searchTextBox.setMessage( Messages.getString("FormRecentlyDescribe.SearchTip"));
		
		
		findSearch = new Button( searchComposite, SWT.NONE );
		findSearch.setText( Messages.getString("FormRecentlyDescribe.FindTermsButton") );
		
		clearSearch = new Button( searchComposite, SWT.NONE );
		clearSearch.setText( Messages.getString("FormRecentlyDescribe.ClearButton") );
		
		
		// label for the base term tables
		Label baseTermLabel = new Label( baseTermComposite, SWT.NONE );
		baseTermLabel.setText( _title + ":" ); //$NON-NLS-1$

		baseTermsTable = new TableViewer( new Table( baseTermComposite , SWT.BORDER | SWT.VIRTUAL ) );

		
		// add the label to the base terms
		baseTermsTable.setLabelProvider( new LabelProviderDescribedTerm( catalogue ) );

		// set the content provider for the table viewer
		baseTermsTable.setContentProvider( new ContentProviderDescribedTerms() );

		// add the terms to the table viewer
		baseTermsTable.setInput( describedTerms );

		// make the table stretchable
		baseTermsTable.getTable().setLayoutData( gridData );
		
	
		/* Add the search filter if it was set */
		if ( searchViewerFilter != null ) {
			// add the filter
			baseTermsTable.addFilter( searchViewerFilter );


			// disable search function in this case
			if ( searchTextBox != null )
				searchTextBox.setEnabled( false );

			if ( findSearch != null )
				findSearch.setEnabled( false );
		}
		
		
		// composite of the text boxes and labels
		Composite codeComposite = new Composite( _dialog, SWT.NONE );
		codeComposite.setLayout( new GridLayout(1, false) );

		// label for full code text box
		Label fullCodeLabel = new Label( codeComposite , SWT.NONE );
		fullCodeLabel.setText( Messages.getString("FormRecentlyDescribe.FullCodeLabel"));

		// text boxes to show the full code 
		final Text fullCode = new Text( codeComposite, SWT.BORDER | SWT.READ_ONLY | SWT.MULTI | SWT.V_SCROLL | SWT.WRAP );

		// label for interpreted text box
		Label interpLabel = new Label( codeComposite , SWT.NONE );
		interpLabel.setText( Messages.getString("FormRecentlyDescribe.InterpretedCodeLabel"));

		// text box for the interpreted code
		final Text interpretedCode = new Text( codeComposite, SWT.BORDER | SWT.READ_ONLY | SWT.MULTI | SWT.V_SCROLL | SWT.WRAP );

		// avoid edit mode in the textboxes
		fullCode.setEditable( false );
		interpretedCode.setEditable( false );

		GridData gridData2 = new GridData();
		gridData2.verticalAlignment = SWT.FILL;
		gridData2.horizontalAlignment = SWT.FILL;
		gridData2.grabExcessHorizontalSpace = true;
		gridData2.grabExcessVerticalSpace = true;
		gridData2.minimumWidth = 400;
		gridData2.widthHint = 400;
		gridData2.minimumHeight = 80;

		// set the layout data for the text boxes and for the composite
		codeComposite.setLayoutData( gridData2 );
		fullCode.setLayoutData( gridData2 );
		interpretedCode.setLayoutData( gridData2 );


		// grid data for the composite
		GridData buttonGD = new GridData();
		buttonGD.verticalAlignment = SWT.FILL;
		buttonGD.grabExcessHorizontalSpace = true;
		buttonGD.minimumWidth = 150;

		// composite which contains the buttons
		Composite buttonComposite = new Composite( _dialog, SWT.NONE );
		buttonComposite.setLayoutData( buttonGD );
		buttonComposite.setLayout( new GridLayout( 2, false ) );

		// open the recent element in the describe window
		Button okButton = new Button( buttonComposite, SWT.PUSH );
		okButton.setText(Messages.getString("FormRecentlyDescribe.LoadButton"));
		okButton.setLayoutData( buttonGD );

		// cancel the operation
		Button cancelButton = new Button( buttonComposite, SWT.PUSH );
		cancelButton.setText(Messages.getString("FormRecentlyDescribe.CancelButton"));
		cancelButton.setLayoutData( buttonGD );

		// when an element is selected from the table
		baseTermsTable.addSelectionChangedListener( new ISelectionChangedListener() {

			@Override
			public void selectionChanged(SelectionChangedEvent arg0) {
				if ( !baseTermsTable.getSelection().isEmpty() ) {
					IStructuredSelection selection = (IStructuredSelection) baseTermsTable.getSelection();

					// get the selected item
					DescribedTerm describedTerm = (DescribedTerm) selection.getFirstElement();
					
					fullCode.setText( describedTerm.getCode() );

					// if not valid stop
					if ( !describedTerm.isValid() ) {
						GlobalUtil.showErrorDialog( _shell, 
								describedTerm.getCode(), 
								Messages.getString( "FormRecentlyDescribe.InvalidTermMessage" ) );
						return;
					}

					// create the interpreted code starting from the fullcode
					interpretedCode.setText( describedTerm.getTerm().getInterpretedCode() );
				}
			}
		});

		
		
		
		/* SEARCH */
		
		// remove filter if pressed
		clearSearch.addSelectionListener( new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				
				searchTextBox.setText("");
				baseTermsTable.addFilter( getSearchFilter( searchTextBox.getText() ) );
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {}
		});
		
		
		
		// search
		findSearch.addSelectionListener( new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				
				// filter the results with the keyword
				baseTermsTable.addFilter( getSearchFilter( searchTextBox.getText() ) );
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {}
		});

		
		// search also with the enter button
		searchTextBox.addTraverseListener( new TraverseListener() {
			
			@Override
			public void keyTraversed(TraverseEvent e) {
				
				// filter the results with the keyword
				baseTermsTable.addFilter( getSearchFilter( searchTextBox.getText() ) );
			}
		});
		
		
		/* LOAD TERM IN DESCRIBE */
		
		// if ok button is pressed
		okButton.addSelectionListener( new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				
				if ( !baseTermsTable.getSelection().isEmpty() ) {
					
					IStructuredSelection selection = (IStructuredSelection) baseTermsTable.getSelection();
					
					// get the selected item
					DescribedTerm describedTerm = (DescribedTerm) selection.getFirstElement();
					
					// load in the describe window the selected term
					loadTermInDescribe( describedTerm );
				}
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {}
		});




		// create a load term menu item for adding the element in the describe window
		Menu rightClickMenu = new Menu( _dialog , SWT.POP_UP );
		MenuItem loadTerm = new MenuItem( rightClickMenu, SWT.PUSH );
		loadTerm.setText(Messages.getString("FormRecentlyDescribe.LoadMenuItem")); //$NON-NLS-1$
		
		// add image to "add" button
		Image addIcon = new Image( Display.getCurrent() , this.getClass().getClassLoader().getResourceAsStream(
				"add-icon.png" ) ); //$NON-NLS-1$
		loadTerm.setImage(addIcon);
		
		loadTerm.addSelectionListener( new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				if ( !baseTermsTable.getSelection().isEmpty() ) {
					IStructuredSelection selection = (IStructuredSelection) baseTermsTable.getSelection();
					
					// get the selected item
					DescribedTerm fc = (DescribedTerm) selection.getFirstElement();

					// load in the describe window the selected term
					loadTermInDescribe( fc );
				}
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {}
		});

		baseTermsTable.getTable().setMenu( rightClickMenu );


		// double click => load the selected element
		baseTermsTable.addDoubleClickListener( new IDoubleClickListener() {

			@Override
			public void doubleClick(DoubleClickEvent arg0) {
				if ( !baseTermsTable.getSelection().isEmpty() ) {
					IStructuredSelection selection = (IStructuredSelection) baseTermsTable.getSelection();
					
					// get the selected item
					DescribedTerm fc = (DescribedTerm) selection.getFirstElement();

					// load in the describe window the selected term
					loadTermInDescribe( fc );
				}
			}
		});


		// cancel button is pressed?
		cancelButton.addSelectionListener( new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				_dialog.close();  // close the dialog
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});

		_dialog.setMaximized( false );
		_dialog.pack();
		
		
		// restore previous dimensions
		window.restore( BrowserWindowPreferenceDao.class );
		window.saveOnClosure( BrowserWindowPreferenceDao.class );
		
		// show the dialog
		_dialog.setVisible(true);  

	}


	/**
	 * Retrieve the base term and the related facets from the full code, then open
	 * the describe window and load all the terms
	 * @param describedTerm
	 */
	private void loadTermInDescribe ( DescribedTerm describedTerm ) {
		
		// if not valid stop
		if ( !describedTerm.isValid() ) {
			GlobalUtil.showErrorDialog( _shell, 
					describedTerm.getCode(), 
					Messages.getString( "FormRecentlyDescribe.InvalidTermMessage" ) );
			return;
		}
		
		// open the describe window
		FormTermCoder tcf = new FormTermCoder( _shell, 
				Messages.getString("FormRecentlyDescribe.DescribeWindowTitle"), catalogue );
		
		// load the described term into the describe window
		tcf.loadDescribedTerm( describedTerm );
		
		// hide the current form
		_dialog.setVisible(false);

		// show the window and add the facet
		tcf.display( catalogue );

		// close the current dialog
		_dialog.close();
	}

	
	/**
	 * Get the search filter for the selected keywords
	 * @param keyword
	 * @return
	 */
	private ViewerFilter getSearchFilter ( String keyword ) {

		ViewerFilter filter = new ViewerFilter() {

			@Override
			public boolean select(Viewer viewer, Object parent, Object element) {

				// search keyword
				String key = searchTextBox.getText().toLowerCase();

				if ( key == null || key.isEmpty() )
					return true;

				// if an element contains the keyword then return it
				String elementName = ( (DescribedTerm) element ).getLabel().toLowerCase();

				if( elementName.contains( key ) )
					return true;

				return false;
			}
		};
		
		return filter;
	}
}
