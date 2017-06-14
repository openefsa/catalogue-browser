package ui_main_menu;

import java.io.IOException;
import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

import already_described_terms.Picklist;
import already_described_terms.PicklistDAO;
import already_described_terms.PicklistParser;
import already_described_terms.PicklistTerm;
import catalogue.Catalogue;
import catalogue_browser_dao.AttributeDAO;
import catalogue_browser_dao.DatabaseManager;
import dcf_manager.Dcf;
import dcf_pending_action.DefaultListeners;
import dcf_user.User;
import dcf_webservice.ReserveLevel;
import dcf_webservice.Publish.PublishLevel;
import export_catalogue.ExportActions;
import import_catalogue.ImportActions;
import messages.Messages;
import ui_general_graphics.DialogSingleText;
import ui_main_panel.AttributeEditor;
import ui_main_panel.HierarchyEditor;
import ui_main_panel.UpdateableUI;
import ui_progress_bar.FormProgressBar;
import user_preferences.CataloguePreferenceDAO;
import user_preferences.FormSearchOptions;
import user_preferences.FormUserPreferences;
import utilities.GlobalUtil;

public class ToolsMenu implements MainMenuItem {

	public static final int RESERVE_CAT_MI = 0;
	public static final int UNRESERVE_CAT_MI = 1;
	public static final int UPLOAD_DATA_MI = 2;
	public static final int PUBLISH_CAT_MI = 3;
	public static final int RESET_CAT_MI = 4;
	public static final int IMPORT_CAT_MI = 5;
	public static final int EXPORT_CAT_MI = 6;
	public static final int IMPORT_PICKLIST_MI = 7;
	public static final int FAV_PICKLIST_MI = 8;
	public static final int COMPACT_DB_MI = 9;
	public static final int HIER_EDITOR_MI = 10;
	public static final int ATTR_EDITOR_MI = 11;
	public static final int SEARCH_OPT_MI = 12;
	public static final int USER_PREF_MI = 13;
	
	private MenuListener listener;
	
	private MainMenu mainMenu;
	private Shell shell;
	
	// tools menu items
	private MenuItem toolsItem;           // tools menu
	private MenuItem reserveMI;           // reserve catalogue
	private MenuItem unreserveMI;         // unreserve catalogue
	private MenuItem uploadDataMI;        // upload changes of the catalogue
	private MenuItem publishMI;           // publish a draft catalogue
	private MenuItem resetMI;             // reset the catalogues data to the previous version
	private MenuItem importMI;
	private MenuItem exportMI;
	private MenuItem importPicklistMI;
	private MenuItem favouritePicklistMI;
	private MenuItem compactDBMI;
	private MenuItem hierarchyEditMI;
	private MenuItem attributeEditMI; 
	private MenuItem searchOptMI;
	private MenuItem userPrefMI;
	
	/**
	 * Tools menu in main menu
	 * @param mainMenu
	 * @param menu
	 */
	public ToolsMenu( MainMenu mainMenu, Menu menu ) {
		this.mainMenu = mainMenu;
		this.shell = mainMenu.getShell();
		toolsItem = create ( menu );
	}
	
	/**
	 * Listener called when a button of the menu is
	 * pressed
	 * @param listener
	 */
	public void setListener(MenuListener listener) {
		this.listener = listener;
	}
	
	/**
	 * Create the tools menu with all the sub menu items
	 * @param menu
	 */
	public MenuItem create ( Menu menu ) {

		MenuItem toolsItem = new MenuItem( menu , SWT.CASCADE );
		toolsItem.setText( Messages.getString( "BrowserMenu.ToolsMenuName" ) );
		
		Menu toolsMenu = new Menu( menu );

		toolsItem.setMenu( toolsMenu );

		// get the current user
		User user = User.getInstance();
		
		// add reserve/unreserve for cm users
		if ( mainMenu.getCatalogue() != null && 
				user.isCatManagerOf( mainMenu.getCatalogue() ) ) {

			reserveMI = addReserveMI ( toolsMenu );
			unreserveMI = addUnreserveMI ( toolsMenu );
			uploadDataMI = addUploadDataMI( toolsMenu );
			publishMI = addPublishMI( toolsMenu );
			resetMI = addResetMI( toolsMenu );
		}
		
		// import operations
		importMI = addImportMI ( toolsMenu );

		// export operations
		exportMI = addExportMI ( toolsMenu );

		// add import picklist
		importPicklistMI = addImportPicklistMI ( toolsMenu );
		
		// favourite picklist
		favouritePicklistMI = addFavouritePicklistMI ( toolsMenu );

		// compact database
		compactDBMI = addCompactDBMI ( toolsMenu );

		// editors only if the catalogue can be edited
		if ( mainMenu.getCatalogue() != null && 
				user.canEdit( mainMenu.getCatalogue() ) ) {
			
			new MenuItem( toolsMenu , SWT.SEPARATOR );

			hierarchyEditMI = addHierarchyEditorMI ( toolsMenu );

			attributeEditMI = addAttributeEditorMI ( toolsMenu );
		}
		
		// horizontal bar to divide the menu elements
		new MenuItem( toolsMenu , SWT.SEPARATOR );

		// search preferences
		searchOptMI = addSearchOptionsMI ( toolsMenu );

		// general user preferences
		userPrefMI = addUserPreferencesMI ( toolsMenu );
		
		// called when the tools menu is shown
		toolsMenu.addListener( SWT.Show, new Listener() {
			
			@Override
			public void handleEvent(Event event) {

				// refresh the tool menu items
				refresh();
			}
		});
		
		toolsItem.setEnabled( false );
		
		return toolsItem;
	}
	
	
	/**
	 * Add reserve menu item (major and minor)
	 * @param menu
	 */
	private MenuItem addReserveMI ( Menu menu ) {
		
		final MenuItem reserveMI = new MenuItem( menu , SWT.CASCADE );

		reserveMI.setText( Messages.getString("BrowserMenu.Reserve") );
		
		// create menu which hosts major and minor reserve
		Menu reserveOpMI = new Menu( shell , SWT.DROP_DOWN );
		reserveMI.setMenu( reserveOpMI );

		// major release
		MenuItem majorMI = new MenuItem( reserveOpMI , SWT.PUSH );
		majorMI.setText( Messages.getString( "BrowserMenu.MajorCmd" ) );
		
		// minor release
		MenuItem minorMI = new MenuItem( reserveOpMI , SWT.PUSH );
		minorMI.setText( Messages.getString( "BrowserMenu.MinorCmd" ) );

		majorMI.addSelectionListener( new SelectionAdapter() {
			@Override
			public void widgetSelected ( SelectionEvent event ) {
				
				setReserveLevel( ReserveLevel.MAJOR );
				
				if ( listener != null )
					listener.buttonPressed( reserveMI, 
							RESERVE_CAT_MI, null );
			}
		} );
		
		minorMI.addSelectionListener( new SelectionAdapter() {
			@Override
			public void widgetSelected ( SelectionEvent event ) {
				
				setReserveLevel( ReserveLevel.MINOR );
				
				if ( listener != null )
					listener.buttonPressed( reserveMI, 
							RESERVE_CAT_MI, null );
			}
		} );

		reserveMI.setEnabled( false );
		
		return reserveMI;
	}
	
	/**
	 * Create a button to unreserve the current catalogue
	 * @param menu
	 * @return
	 */
	private MenuItem addUnreserveMI ( Menu menu ) {

		final MenuItem unreserveMI = new MenuItem( menu , SWT.PUSH );

		unreserveMI.setText( Messages.getString("BrowserMenu.Unreserve") );
		unreserveMI.addSelectionListener( new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent arg0) {
				
				// unreserve the current catalogue
				setReserveLevel ( ReserveLevel.NONE );
				
				if ( listener != null )
					listener.buttonPressed( unreserveMI, 
							UNRESERVE_CAT_MI, null );
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				
			}
		});
		
		unreserveMI.setEnabled( false );
		
		return unreserveMI;
	}
	
	/**
	 * Add the upload data menu item to the menu.
	 * Here we upload the catalogue (in xlsx format)
	 * to a shared folder in order to start the
	 * sas procedure to upload only the real
	 * changes of the catalogue to the DCF
	 * @param menu
	 * @return
	 */
	private MenuItem addUploadDataMI ( Menu menu ) {
		
		final MenuItem uploadDataMI = new MenuItem( menu , SWT.PUSH );

		uploadDataMI.setText( Messages.getString("BrowserMenu.UploadData") );
		uploadDataMI.addSelectionListener( new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent arg0) {

				//TODO
				// export the open catalogue to excel
				
				
				// copy the excel file into the shared folder
				// to start the sas procedure to upload
				// the catalogue changes
				
				
				if ( listener != null )
					listener.buttonPressed( uploadDataMI, 
							UPLOAD_DATA_MI, null );
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				
			}
		});
		
		uploadDataMI.setEnabled( false );
		
		return uploadDataMI;
	}
	
	/**
	 * Add the publish MI. If a catalogue is in draft, we
	 * can publish it using this button.
	 * @param menu
	 * @return
	 */
	private MenuItem addPublishMI ( Menu menu ) {
		
		final MenuItem publishMI = new MenuItem( menu , SWT.CASCADE );

		publishMI.setText( Messages.getString( "BrowserMenu.Publish" ) );
		
		// create menu which hosts major and minor reserve
		Menu publishOpMI = new Menu( shell , SWT.DROP_DOWN );
		publishMI.setMenu( publishOpMI );

		// major release
		MenuItem majorMI = new MenuItem( publishOpMI , SWT.PUSH );
		majorMI.setText( Messages.getString( "BrowserMenu.PublishMajorCmd" ) );

		// minor release
		MenuItem minorMI = new MenuItem( publishOpMI , SWT.PUSH );
		minorMI.setText( Messages.getString( "BrowserMenu.PublishMinorCmd" ) );

		// publish major
		majorMI.addSelectionListener( new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent arg0) {

				publish ( mainMenu.getCatalogue(), PublishLevel.MAJOR );
				
				if ( listener != null )
					listener.buttonPressed( publishMI, 
							PUBLISH_CAT_MI, null );
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {}
		});
		
		// publish minor
		minorMI.addSelectionListener( new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				
				publish ( mainMenu.getCatalogue(), PublishLevel.MINOR );
				
				if ( listener != null )
					listener.buttonPressed( publishMI, 
							PUBLISH_CAT_MI, null );
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {}
		});
		
		publishMI.setEnabled( false );
		
		return publishMI;
	}
	
	/**
	 * Add the reset mi, which allows resetting the catalogue data
	 * to the previous version. Note that this button is enabled only
	 * for internal version of catalogues which were reserved!
	 * @param menu
	 * @return
	 */
	private MenuItem addResetMI ( Menu menu ) {
		
		final MenuItem resetMI = new MenuItem( menu , SWT.PUSH );

		resetMI.setText( Messages.getString( "BrowserMenu.Reset" ) );
		resetMI.addSelectionListener( new SelectionListener() {

			@Override
			public void widgetSelected( SelectionEvent arg0 ) {

				// ask for confirmation
				int val = GlobalUtil.showDialog( shell, 
						Messages.getString( "ResetChanges.ConfirmTitle" ), 
						Messages.getString( "ResetChanges.ConfirmMessage" ), 
						SWT.YES | SWT.NO );
				
				if ( val == SWT.NO )
					return;
				
				// reset the catalogue data to the previous version
				try {
					DatabaseManager.restoreBackup( mainMenu.getCatalogue() );
				} catch (IOException e) {
					
					GlobalUtil.showErrorDialog( shell, 
							Messages.getString( "ResetChanges.ErrorTitle" ), 
							Messages.getString( "ResetChanges.ErrorMessage" ) );
				}
				
				if ( listener != null )
					listener.buttonPressed( resetMI, 
							RESET_CAT_MI, null );
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				
			}
		});
		
		resetMI.setEnabled( false );
		
		return resetMI;
	}
	
	/**
	 * Add a menu item which allows to import excels into the DB
	 * @param menu
	 */
	private MenuItem addImportMI ( Menu menu ) {
		
		final MenuItem importItem = new MenuItem( menu , SWT.NONE );
		importItem.setText( Messages.getString("BrowserMenu.ImportCmd") );
		
		importItem.addSelectionListener( new SelectionAdapter() {
			@Override
			public void widgetSelected ( SelectionEvent event ) {

				final String filename = GlobalUtil.showExcelFileDialog( shell, 
						Messages.getString("BrowserMenu.ImportWindowTitle"), SWT.OPEN );
				
				// return if no filename retrieved
				if ( filename == null || filename.isEmpty() )
					return;
					
				// ask for final confirmation
				MessageBox alertBox = new MessageBox( shell, SWT.OK | SWT.CANCEL | SWT.ICON_QUESTION );
				alertBox.setText( Messages.getString("BrowserMenu.ImportWarningTitle") );
				alertBox.setMessage( Messages.getString( "BrowserMenu.ImportWarningMessage" ) );
				int val = alertBox.open();

				// return if cancel was pressed
				if ( val == SWT.CANCEL )
					return;

				ImportActions importAction = new ImportActions();
				importAction.setProgressBar( new FormProgressBar( shell, "") );
				
				// set the opened catalogue since we are importing
				// in an already existing catalogue
				importAction.setOpenedCatalogue( mainMenu.getCatalogue() );
				
				// import the selected excel into the current catalogue
				importAction.importXlsx( mainMenu.getCatalogue().getDbFullPath(), 
						filename, false, new Listener() {

					@Override
					public void handleEvent(Event event) {

						// load catalogue data in ram
						// we do not open it since it is already opened
						mainMenu.getCatalogue().loadData();
						
						
						if ( listener != null )
							listener.buttonPressed( importItem, 
									IMPORT_CAT_MI, event );
						
						GlobalUtil.showDialog( shell, 
								Messages.getString("Import.ImportSuccessTitle"), 
								Messages.getString("Import.ImportSuccessMessage"), 
								SWT.ICON_INFORMATION );
					}
				});
			}
		} );
		
		importItem.setEnabled( false );
		
		return importItem;
	}
	
	
	
	
	/**
	 * Add a menu item which allows to export the database into an excel file
	 * @param menu
	 */
	private MenuItem addExportMI ( Menu menu ) {
		
		final MenuItem exportItem = new MenuItem( menu , SWT.NONE );
		exportItem.setText( Messages.getString( "BrowserMenu.ExportCmd" ) );
		
		exportItem.addSelectionListener( new SelectionAdapter() {
			@Override
			public void widgetSelected ( SelectionEvent event ) {
				
				String defaultFilename = mainMenu.getCatalogue().getCode() + "_" 
				+ mainMenu.getCatalogue().getVersion() + ".xlsx";
				
				final String filename = GlobalUtil.showExcelFileDialog( shell, 
						Messages.getString( "Export.FileDialogTitle"), 
						defaultFilename, SWT.SAVE  );

				// return if no filename retrieved
				if ( filename == null || filename.isEmpty() )
					return;
				
				// export the catalogue
				ExportActions export = new ExportActions();
				
				// set the progress bar
				export.setProgressBar( new FormProgressBar( shell, 
						Messages.getString("Export.ProgressBarTitle") ) );
				
				// export the opened catalogue
				export.exportCatalogueToExcel( mainMenu.getCatalogue(), 
						filename, new Listener() {

					@Override
					public void handleEvent(Event arg0) {
						
						// warn the user that everything went ok
						GlobalUtil.showDialog( shell, 
								Messages.getString( "Export.DoneTitle" ), 
								Messages.getString( "Export.DoneMessage" ),
								SWT.ICON_INFORMATION );
						
						if ( listener != null )
							listener.buttonPressed( exportItem, 
									EXPORT_CAT_MI, null );
					}
				});
			}
		} );
		
		// enable according to the operation status
		exportItem.setEnabled( false );
		
		return exportItem;
	}

	/**
	 * Add a menu item which allows selecting the favourite picklist to use in the browser
	 * @param menu
	 */
	private MenuItem addImportPicklistMI ( Menu menu ) {
		
		// create a menu item for importing picklists
		final MenuItem picklistItem = new MenuItem( menu , SWT.CASCADE );
		picklistItem.setText( Messages.getString("BrowserMenu.ImportPicklistCmd") );
		
		// open a dialog to select a picklist file
		
		picklistItem.addSelectionListener( new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				
				// create dialog
				FileDialog fd = new FileDialog( shell , SWT.OPEN );
				
				// set dialog title
				fd.setText( Messages.getString("BrowserMenu.ImportPicklistDialogTitle") );
				
				// set working directory
				// get the working directory from the user preferences
				fd.setFilterPath( DatabaseManager.MAIN_CAT_DB_FOLDER  );
				
				// select only csv files
				String[] filterExt = { "*.csv" };
				fd.setFilterExtensions( filterExt );
				
				// open dialog a listen to get the selected filename
				String filename = fd.open();
				
				if ( ( filename != null ) && ( filename.length() > 0 ) ) {
					
					GlobalUtil.setShellCursor( shell, SWT.CURSOR_WAIT );
					
					// parse the picklist as a csv semicolon separated file
					PicklistParser parse = new PicklistParser ( filename, ";" );
					
					ArrayList <PicklistTerm> picklistTerms = new ArrayList<>();
					PicklistTerm currentTerm;
					
					// for each picklist term add it to the array list
					while ( ( currentTerm = parse.nextTerm() ) != null )
						picklistTerms.add( currentTerm );
					
					// create a picklist using the filename as code
					Picklist picklist = new Picklist ( filename, picklistTerms );
					
					PicklistDAO pickDao = new PicklistDAO( mainMenu.getCatalogue() );
					
					// insert the new picklist if it is new
					pickDao.importPicklist( picklist );
					
					GlobalUtil.setShellCursor( shell, SWT.CURSOR_ARROW );
				}
				
				if ( listener != null )
					listener.buttonPressed( picklistItem, 
							IMPORT_PICKLIST_MI, null );
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {}
		});
		
		picklistItem.setEnabled( false );
		
		return picklistItem;
	}
	
	/**
	 * Add a menu item which allows selecting the favourite picklist to use in the browser
	 * @param menu
	 */
	private MenuItem addFavouritePicklistMI ( Menu menu ) {
		
		final MenuItem picklistItem = new MenuItem( menu , SWT.CASCADE );
		picklistItem.setText( Messages.getString("BrowserMenu.PicklistCmd") );

		// Initialize the menu
		final Menu selectPicklistMenu = new Menu( shell , SWT.DROP_DOWN );

		// add the menu
		picklistItem.setMenu( selectPicklistMenu );

		// when the menu is showed
		selectPicklistMenu.addListener(SWT.Show, new Listener() {

			@Override
			public void handleEvent(Event event) {

				// reset the item of the menu, in order to update with the current picklists in the app.jar folder
				for (MenuItem item : selectPicklistMenu.getItems() ) {
					item.dispose();
				}
				
				CataloguePreferenceDAO prefDao = new CataloguePreferenceDAO( 
						mainMenu.getCatalogue() );

				// get the favourite picklist
				Picklist currentPicklist = prefDao.getFavouritePicklist();
				
				
				PicklistDAO pickDao = new PicklistDAO( mainMenu.getCatalogue() );
				
				// for each imported picklist we create a menu item in order to allow choosing
				// the favourite picklist
				for ( Picklist picklist : pickDao.getAll() ) {
				
					final MenuItem mi = new MenuItem( selectPicklistMenu, SWT.RADIO );
					mi.setText( picklist.getCode() );
					
					// select the current menu item if the old selected picklist is the current one
					// if there was a favourite picklist indeed
					if ( currentPicklist != null )
						mi.setSelection( picklist.equals( currentPicklist ) );
					
					// set the data for the menu item
					mi.setData( picklist );
					
					// actions taken when this menu item is pressed
					mi.addSelectionListener( new SelectionListener() {
						
						@Override
						public void widgetSelected(SelectionEvent e) {
							
							// Update the selected picklist into the preference database:
							
							// get the selected picklist id starting from the picklist code
							// the menu items has the picklist code in the data field
							Picklist selectedPicklist = (Picklist) mi.getData();
							
							CataloguePreferenceDAO prefDao = new CataloguePreferenceDAO( 
									mainMenu.getCatalogue() );
							
							// set the favourite picklist
							prefDao.setFavouritePicklist( selectedPicklist );
							
							
							if ( listener != null )
								listener.buttonPressed( picklistItem, 
										FAV_PICKLIST_MI, null );
						}
						
						@Override
						public void widgetDefaultSelected(SelectionEvent e) {}
					});
				}
			}
		});
		
		picklistItem.setEnabled( false );
		
		return picklistItem;
	}
	
	
	/**
	 * Add a menu item which allows compacting the DB
	 * @param menu
	 */
	private MenuItem addCompactDBMI ( Menu menu ) {
		
		final MenuItem compressDBItem = new MenuItem( menu , SWT.NONE );

		compressDBItem.setText( Messages.getString("BrowserMenu.CompactDBCmd") );

		compressDBItem.addSelectionListener( new SelectionAdapter() {
			@Override
			public void widgetSelected ( SelectionEvent event ) {

				DatabaseManager.compressDatabase();

				if ( listener != null )
					listener.buttonPressed( compressDBItem, 
							COMPACT_DB_MI, null );
			}
		} );
		
		compressDBItem.setEnabled( false );
		
		return compressDBItem;
	}
	
	/**
	 * Add a menu item which allows modifying the hierarchies names
	 * @param menu
	 */
	private MenuItem addHierarchyEditorMI ( Menu menu ) {
		
		final MenuItem hierarchyEditorItem = new MenuItem( menu , SWT.NONE );
		hierarchyEditorItem.setText( Messages.getString("BrowserMenu.HierarchyEditorCmd") );

		// Enable only if there is a catalogue open		
		hierarchyEditorItem.addSelectionListener( new SelectionAdapter() {
			@Override
			public void widgetSelected ( SelectionEvent event ) {
				
				HierarchyEditor editor = new HierarchyEditor( shell, 
						mainMenu.getCatalogue() );
				editor.Display();
			
				boolean changed = editor.isChanged();
				Event e = new Event();
				event.data = changed;
				
				if ( listener != null )
					listener.buttonPressed( hierarchyEditorItem, 
							HIER_EDITOR_MI, e );
			}
		} );
		
		// enable according to the operation status
		hierarchyEditorItem.setEnabled( false );
		
		return hierarchyEditorItem;
	}
	
	/**
	 * Add a menu item which allows modifying the facet names
	 * @param menu
	 */
	private MenuItem addAttributeEditorMI ( Menu menu ) {
		
		final MenuItem attributeEditorItem = new MenuItem( menu , SWT.NONE );
		attributeEditorItem.setText( Messages.getString("BrowserMenu.AttributeEditorCmd") );
		
		attributeEditorItem.addSelectionListener( new SelectionAdapter() {
			@Override
			public void widgetSelected ( SelectionEvent event ) {
				
				// initialize dao of attributes
				AttributeDAO attrDao = new AttributeDAO( mainMenu.getCatalogue() );
				
				AttributeEditor e = new AttributeEditor( shell , attrDao.getAll() );
				e.Display();
				
				if ( listener != null )
					listener.buttonPressed( attributeEditorItem, 
							ATTR_EDITOR_MI, null );
				
			}
		} );

		// enable according to the operation status
		attributeEditorItem.setEnabled( false );

		return attributeEditorItem;
	}

	/**
	 * Add a menu item which allows choosing the 
	 * search preferences
	 * @param menu
	 */
	private MenuItem addSearchOptionsMI ( Menu menu ) {
		
		// Search options form
		final MenuItem searchOptionsItem = new MenuItem( menu , SWT.PUSH );
		searchOptionsItem.setText( Messages.getString("BrowserMenu.GeneralSearchOptionsCmd") );

		// if search options is clicked
		searchOptionsItem.addSelectionListener( new SelectionAdapter() {
			@Override
			public void widgetSelected ( SelectionEvent event ) {

				// open the form for choosing the search options
				FormSearchOptions sof = new FormSearchOptions(shell, 
						Messages.getString( "BrowserMenu.SearchOptionsWindowTitle"), 
						mainMenu.getCatalogue() );
				
				// display the form
				sof.display();
				
				if ( listener != null )
					listener.buttonPressed( searchOptionsItem, 
							SEARCH_OPT_MI, null );

			}
		} );
		
		searchOptionsItem.setEnabled( false );
		
		return searchOptionsItem;
	}
	
	/**
	 * Add a menu item which allows choosing among the user preferences
	 * @param menu
	 */
	private MenuItem addUserPreferencesMI ( Menu menu ) {
		
		final MenuItem userPrefItem = new MenuItem( menu , SWT.NONE );
		
		userPrefItem.setText( Messages.getString( "BrowserMenu.UserPrefCmd" ) );

		userPrefItem.addSelectionListener( new SelectionAdapter() {
			@Override
			public void widgetSelected ( SelectionEvent event ) {

				FormUserPreferences e = new FormUserPreferences( shell );
				e.Display();
				
				if ( listener != null )
					listener.buttonPressed( userPrefItem, 
							USER_PREF_MI, null );

			}
		} );
		
		userPrefItem.setEnabled( false );
		
		return userPrefItem;
	}
	
	/**
	 * Refresh all the menu items contained in the tool menu
	 */
	public void refresh () {
		
		User user = User.getInstance();
		
		// enable the tools menu only if there is a catalogue open
		// and if we know that the user is cm or dp
		toolsItem.setEnabled( mainMenu.getCatalogue() != null &&
				!user.isGettingUserLevel() );
		
		if ( mainMenu.getCatalogue() == null )
			return;
		
		PicklistDAO pickDao = new PicklistDAO( mainMenu.getCatalogue() );
		
		// check if the db contains at least one picklist
		boolean hasPicklists = !pickDao.isEmpty();
		
		// check if the catalogue contains facet or not
		boolean hasFacets = mainMenu.getCatalogue().hasAttributeHierarchies();

		// check if the current user can edit the current catalogue or not
		// we can edit if we are in editing mode or if we are modifying a local catalogue
		boolean canEdit = user.canEdit( mainMenu.getCatalogue() );
		
		// check if the current catalogue is not empty (has data in it)
		boolean nonEmptyCat = !mainMenu.getCatalogue().isEmpty();
		
		compactDBMI.setEnabled( true );

		// we can export only for non local catalogues, since if we
		// export a local catalogue we create an excel which has as
		// catalogue code a custom string, but with master hierarchy code
		// the code defined before in the excel import... And give errors!
		exportMI.setEnabled( true );

		importPicklistMI.setEnabled( hasFacets );
		favouritePicklistMI.setEnabled( hasFacets && hasPicklists );
		
		searchOptMI.setEnabled( nonEmptyCat );
		userPrefMI.setEnabled( true );

		// enable disable publish mi
		if ( publishMI != null )
			publishMI.setEnabled( mainMenu.getCatalogue().canBePublished() );
		
		// if editing modify also editing buttons
		if ( user.canEdit( mainMenu.getCatalogue() ) ) {
			
			if ( hierarchyEditMI != null )
				hierarchyEditMI.setEnabled ( true );
			
			if ( attributeEditMI != null )
				attributeEditMI.setEnabled( true );
		}

		// update catalogue manager buttons
		// if the current user is enabled to
		// reserve the current catalogue
		if ( user.isCatManagerOf( mainMenu.getCatalogue() ) ) {

			// enable reserve if the catalogue is not already reserved by me
			boolean reservable = !mainMenu.getCatalogue().isReservedBy( User.getInstance() );
			
			// can unreserve if we had reserved the catalogue ( not another user )
			boolean unReservable = mainMenu.getCatalogue().isUnreservable();

			// if we are requesting a web service
			// disable the action which can send another
			// web service request
			if ( mainMenu.getCatalogue().isRequestingAction() ) {
				
				if ( reserveMI != null ) {
					reserveMI.setText( Messages.getString( "Reserve.WaitingResponse" ) );
					reserveMI.setEnabled( false );
				}
				
				if ( unreserveMI != null ) {
					unreserveMI.setText( Messages.getString( "Reserve.WaitingResponse" ) );
					unreserveMI.setEnabled( false );
				}
				
				if ( publishMI != null ) {
					publishMI.setText( Messages.getString( "Reserve.WaitingResponse" ) );
					publishMI.setEnabled( false );
				}
				
				if ( uploadDataMI != null ) {
					uploadDataMI.setText( Messages.getString( "Reserve.WaitingResponse" ) );
					uploadDataMI.setEnabled( false );
				}

				
				// if we are reserving but we have forced the
				// editing, we leave these buttons enabled
				if ( !mainMenu.getCatalogue().isForceEdit( 
						User.getInstance().getUsername() ) ) {

					if ( resetMI != null )
						resetMI.setEnabled( false );

					if ( importMI != null )
						importMI.setEnabled( false );

					if ( hierarchyEditMI != null )
						hierarchyEditMI.setEnabled ( false );

					if ( attributeEditMI != null )
						attributeEditMI.setEnabled( false );
				}
			}
			else {
				
				if ( reserveMI != null ) {
					// can reserve only if not local and catalogue not reserved
					reserveMI.setText( Messages.getString( "BrowserMenu.Reserve" ) );
					reserveMI.setEnabled( reservable );
				}

				if ( unreserveMI != null ) {
					unreserveMI.setText( Messages.getString( "BrowserMenu.Unreserve" ) );
					unreserveMI.setEnabled( unReservable );
				}
				
				if ( uploadDataMI != null ) {
					uploadDataMI.setText( Messages.getString( "BrowserMenu.UploadData" ) );
				}

				if ( publishMI != null ) {
					publishMI.setText( Messages.getString( "BrowserMenu.Publish" ) );
				}
				
				if ( resetMI != null && user.canEdit( mainMenu.getCatalogue() ) ) {
					
					// enable resetMI only if the catalogue is an internal version
					// and if the catalogue is reserved by the current user
					resetMI.setEnabled( mainMenu.getCatalogue().isReservedBy( user ) && 
							mainMenu.getCatalogue().getCatalogueVersion().isInternalVersion() );
				}

				if ( importMI != null )
					importMI.setEnabled( canEdit );
			}
		}
		else {
			if ( mainMenu.getCatalogue().isLocal() ) {
				
				if ( importMI != null )
					importMI.setEnabled( true );
				
				if ( exportMI != null )
					exportMI.setEnabled( true );
			}
		}
	}
	
	/**
	 * Set the menu as enabled or not
	 * @param enabled
	 */
	public void setEnabled ( boolean enabled ) {
		toolsItem.setEnabled( enabled );
	}
	
	/**
	 * Reserve or unreserve the current catalogue
	 * Ask a short description of the reserve/unreserve
	 * reason and start the reserve webservice using
	 * a separated thread.
	 * @param level the enum {@linkplain ReserveLevel}, 
	 * a level of MINOR or MAJOR performs a reserve operation
	 * a level of NONE performs an unreserve operation.
	 */
	private void setReserveLevel( final ReserveLevel level ) {
		
		Catalogue catalogue = mainMenu.getCatalogue();
		String note = "";
		
		Warnings wrn = new Warnings( shell );
		
		if ( level.greaterThan( ReserveLevel.NONE ) ) {
			
			// check if we have errors
			boolean block = wrn.reserve( catalogue.getCatalogueStatus() );
			
			// if errors => return
			if ( block )
				return;
			
			// ask the reserve description
			DialogSingleText dialog = new DialogSingleText( shell, 10 );
			dialog.setTitle( Messages.getString( "BrowserMenu.ReserveTitle" ) );
			dialog.setMessage( Messages.getString( "BrowserMenu.ReserveMessage" ) );

			note = dialog.open();

			// return if cancel was pressed
			// no description was given
			if ( note == null )
				return;
		}
		else {
			// get the note which was written during
			// the reserve note and use it for the unreserve
			note = mainMenu.getCatalogue().getReserveNote();
		}
		
		// set wait cursor
		GlobalUtil.setShellCursor( shell, SWT.CURSOR_WAIT );
		
		reserve( mainMenu.getCatalogue(), level, note );
		
		// restore cursor
		GlobalUtil.setShellCursor( shell, SWT.CURSOR_ARROW );
	}
	
	/**
	 * Create the reserve request for the current catalogue.
	 * Note that in the listeners we need to use the
	 * {@link Display#syncExec(Runnable)} to perform UI actions
	 * since we are not in the UI thread.
	 * @param catalogue
	 * @param level
	 * @param text
	 * @return the {@link ReserveBuilder} with all the listeners set
	 */
	private void reserve( Catalogue catalogue, 
			final ReserveLevel level, String description ) {

		// reserve the catalogue
		Dcf dcf = new Dcf();

		// create a progress bar for the possible import process
		final FormProgressBar progressBar = new FormProgressBar( shell, 
				Messages.getString( "Reserve.NewInternalTitle" ), false, SWT.TITLE );

		// move down the location of the progress bar
		progressBar.setLocation( progressBar.getLocation().x, 
				progressBar.getLocation().y + 170 );

		dcf.setProgressBar( progressBar );
		
		dcf.reserveBG( catalogue, 
				level, 
				description, 
				DefaultListeners.getReserveListener( new UpdateableUI() {

					@Override
					public void updateUI(Object data) {
						mainMenu.update( data );
					}

					@Override
					public Shell getShell() {
						return shell;
					}
				} ) 
			);
	}
	
	/**
	 * Publish a catalogue
	 * @param catalogue the catalogue we want to publish
	 * @param level the publish level
	 */
	private void publish ( Catalogue catalogue, PublishLevel level ) {

		Warnings wrn = new Warnings( shell );
		
		// check if we have errors
		boolean block = wrn.publish( catalogue.getCatalogueStatus() );

		// if errors => return
		if ( block )
			return;

		// set wait cursor
		GlobalUtil.setShellCursor( shell, SWT.CURSOR_WAIT );
		
		// publish the catalogue (only for drafts)
		Dcf dcf = new Dcf();
		dcf.publishBG( catalogue,
				level,
				DefaultListeners.getPublishListener( new UpdateableUI() {

					@Override
					public void updateUI(Object data) {
						mainMenu.update( data );
					}

					@Override
					public Shell getShell() {
						return shell;
					}
				} ) );
		
		// restore cursor
		GlobalUtil.setShellCursor( shell, SWT.CURSOR_ARROW );
	}
}
