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
import catalogue_browser_dao.AttributeDAO;
import catalogue_browser_dao.DatabaseManager;
import catalogue_object.Catalogue;
import dcf_log_util.LogCodeFoundListener;
import dcf_manager.Dcf;
import dcf_reserve_util.ReserveBuilder;
import dcf_reserve_util.ReserveFinishedListener;
import dcf_reserve_util.ReserveResult;
import dcf_reserve_util.ReserveStartedListener;
import dcf_user.User;
import dcf_webservice.DcfResponse;
import dcf_webservice.ReserveLevel;
import export_catalogue.ExportActions;
import import_catalogue.ImportActions;
import messages.Messages;
import ui_general_graphics.DialogSingleText;
import ui_main_panel.AttributeEditor;
import ui_main_panel.HierarchyEditor;
import ui_main_panel.ShellLocker;
import ui_progress_bar.FormProgressBar;
import user_preferences.CataloguePreferenceDAO;
import user_preferences.FormSearchOptions;
import user_preferences.FormUserPreferences;
import utilities.GlobalUtil;

public class ToolsMenu implements MainMenuItem {

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
	private MenuItem appendMI; 
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
		
		// append operations (only edit)
		if ( mainMenu.getCatalogue() != null &&
				user.canEdit( mainMenu.getCatalogue() ) )
			appendMI = addAppendMI ( toolsMenu );

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
		
		MenuItem reserveMI = new MenuItem( menu , SWT.CASCADE );

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
			}
		} );
		
		minorMI.addSelectionListener( new SelectionAdapter() {
			@Override
			public void widgetSelected ( SelectionEvent event ) {
				setReserveLevel( ReserveLevel.MINOR );
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

		MenuItem unreserveMI = new MenuItem( menu , SWT.PUSH );

		unreserveMI.setText( Messages.getString("BrowserMenu.Unreserve") );
		unreserveMI.addSelectionListener( new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent arg0) {
				
				// unreserve the current catalogue
				setReserveLevel ( ReserveLevel.NONE );
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
		
		MenuItem uploadDataMI = new MenuItem( menu , SWT.PUSH );

		uploadDataMI.setText( Messages.getString("BrowserMenu.UploadData") );
		uploadDataMI.addSelectionListener( new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent arg0) {

				//TODO
				// export the open catalogue to excel
				
				
				// copy the excel file into the shared folder
				// to start the sas procedure to upload
				// the catalogue changes
				
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
		
		MenuItem publishMI = new MenuItem( menu , SWT.PUSH );

		publishMI.setText( Messages.getString( "BrowserMenu.Publish" ) );
		publishMI.addSelectionListener( new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent arg0) {

				// publish the catalogue (only for drafts)
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				
			}
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
		
		MenuItem resetMI = new MenuItem( menu , SWT.PUSH );

		resetMI.setText( Messages.getString( "BrowserMenu.Reset" ) );
		resetMI.addSelectionListener( new SelectionListener() {

			@Override
			public void widgetSelected( SelectionEvent arg0 ) {

				// reset the catalogue data to the previous version
				try {
					DatabaseManager.restoreBackup( mainMenu.getCatalogue() );
				} catch (IOException e) {
					
					GlobalUtil.showErrorDialog( shell, 
							Messages.getString( "ResetChanges.ErrorTitle" ), 
							Messages.getString( "ResetChanges.MessageTitle" ) );
				}
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
		
		MenuItem importItem = new MenuItem( menu , SWT.NONE );
		importItem.setText( Messages.getString("BrowserMenu.ImportCmd") );
		
		importItem.addSelectionListener( new SelectionAdapter() {
			@Override
			public void widgetSelected ( SelectionEvent event ) {

				final String filename = GlobalUtil.showExcelFileDialog( shell, 
						Messages.getString("BrowserMenu.ImportWindowTitle") );
				
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
				
				// import the selected excel into the current catalogue
				importAction.importXlsx( mainMenu.getCatalogue().getDbFullPath(), 
						filename, true, new Listener() {

					@Override
					public void handleEvent(Event event) {

						// load catalogue data in ram
						// we do not open it since it is already opened
						mainMenu.getCatalogue().loadData();
						
						// call the import listener if it was set
						if ( mainMenu.importListener != null ) {
							mainMenu.importListener.handleEvent( new Event() );
						}
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
		
		MenuItem exportItem = new MenuItem( menu , SWT.NONE );
		exportItem.setText( Messages.getString( "BrowserMenu.ExportCmd" ) );
		
		exportItem.addSelectionListener( new SelectionAdapter() {
			@Override
			public void widgetSelected ( SelectionEvent event ) {
				
				String defaultFilename = mainMenu.getCatalogue().getCode() + "_" 
				+ mainMenu.getCatalogue().getVersion() + ".xlsx";
				
				final String filename = GlobalUtil.showExcelFileDialog( shell, 
						Messages.getString( "Export.FileDialogTitle" ), defaultFilename );

				// return if no filename retrieved
				if ( filename == null || filename.isEmpty() )
					return;
				
				// export the catalogue
				ExportActions export = new ExportActions();
				
				// set the progress bar
				export.setProgressBar( new FormProgressBar( shell, Messages.getString("Export.ProgressBarTitle") ) );
				
				// export the opened catalogue
				export.exportCatalogueToExcel( mainMenu.getCatalogue(), 
						filename, new Listener() {

					@Override
					public void handleEvent(Event arg0) {
						
						// warn the user that everything went ok
						mainMenu.warnUser( Messages.getString( "Export.DoneTitle" ), 
								Messages.getString( "Export.DoneMessage" ) );
					}
				});
			}
		} );
		
		// enable according to the operation status
		exportItem.setEnabled( false );
		
		return exportItem;
	}
	
	
	

	/**
	 * Add a menu item which allows to append an excel file
	 * @param menu
	 */
	private MenuItem addAppendMI ( Menu menu ) {

		MenuItem appendItem = new MenuItem( menu , SWT.NONE );
		appendItem.setText( Messages.getString("BrowserMenu.AppendCmd") );

		// only if not read mode
		if ( appendItem != null )
			appendItem.addSelectionListener( new SelectionAdapter() {
				@Override
				public void widgetSelected ( SelectionEvent event ) {

					
				}
			} );

		// enable if editing mode or if we are editing a local catalogue
		appendItem.setEnabled( false );
		
		return appendItem;
	}
	
	/**
	 * Add a menu item which allows selecting the favourite picklist to use in the browser
	 * @param menu
	 */
	private MenuItem addImportPicklistMI ( Menu menu ) {
		
		// create a menu item for importing picklists
		MenuItem picklistItem = new MenuItem( menu , SWT.CASCADE );
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
		
		MenuItem picklistItem = new MenuItem( menu , SWT.CASCADE );
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
		
		MenuItem compressDBItem = new MenuItem( menu , SWT.NONE );

		compressDBItem.setText( Messages.getString("BrowserMenu.CompactDBCmd") );

		compressDBItem.addSelectionListener( new SelectionAdapter() {
			@Override
			public void widgetSelected ( SelectionEvent event ) {

				DatabaseManager.compressDatabase();

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
		
		MenuItem hierarchyEditorItem = new MenuItem( menu , SWT.NONE );
		hierarchyEditorItem.setText( Messages.getString("BrowserMenu.HierarchyEditorCmd") );

		// Enable only if there is a catalogue open		
		hierarchyEditorItem.addSelectionListener( new SelectionAdapter() {
			@Override
			public void widgetSelected ( SelectionEvent event ) {
				
				HierarchyEditor e = new HierarchyEditor( shell, 
						mainMenu.getCatalogue().getHierarchies() );
				e.Display();
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
		
		MenuItem attributeEditorItem = new MenuItem( menu , SWT.NONE );
		attributeEditorItem.setText( Messages.getString("BrowserMenu.AttributeEditorCmd") );
		
		attributeEditorItem.addSelectionListener( new SelectionAdapter() {
			@Override
			public void widgetSelected ( SelectionEvent event ) {
				
				// initialize dao of attributes
				AttributeDAO attrDao = new AttributeDAO( mainMenu.getCatalogue() );
				
				AttributeEditor e = new AttributeEditor( shell , attrDao.getAll() );
				e.addUpdateListener( mainMenu.updateListener );
				e.Display();
				
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
		MenuItem searchOptionsItem = new MenuItem( menu , SWT.PUSH );
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
		
		MenuItem userPrefItem = new MenuItem( menu , SWT.NONE );
		
		userPrefItem.setText( Messages.getString( "BrowserMenu.UserPrefCmd" ) );

		userPrefItem.addSelectionListener( new SelectionAdapter() {
			@Override
			public void widgetSelected ( SelectionEvent event ) {

				FormUserPreferences e = new FormUserPreferences( shell );
				e.Display();

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
		boolean canEdit = user.canEdit( mainMenu.getCatalogue() ) || 
				mainMenu.getCatalogue().isLocal();
		
		// check if the current catalogue is not empty (has data in it)
		boolean nonEmptyCat = !mainMenu.getCatalogue().isEmpty();
		
		compactDBMI.setEnabled( true );

		// we can export only for non local catalogues, since if we
		// export a local catalogue we create an excel which has as
		// catalogue code a custom string, but with master hierarchy code
		// the code defined before in the excel import... And give errors!
		exportMI.setEnabled( nonEmptyCat && !mainMenu.getCatalogue().isLocal() );

		importPicklistMI.setEnabled( hasFacets );
		favouritePicklistMI.setEnabled( hasFacets && hasPicklists );
		
		searchOptMI.setEnabled( nonEmptyCat );
		userPrefMI.setEnabled( true );

		// if editing modify also editing buttons
		if ( user.canEdit( mainMenu.getCatalogue() ) ) {
			
			if ( hierarchyEditMI != null )
				hierarchyEditMI.setEnabled ( true );
			
			if ( attributeEditMI != null )
				attributeEditMI.setEnabled( true );
			
			if ( appendMI != null )
				appendMI.setEnabled( canEdit && nonEmptyCat );
		}

		// update catalogue manager buttons
		// if the current user is enabled to
		// reserve the current catalogue
		if ( user.isCatManagerOf( mainMenu.getCatalogue() ) ) {

			// can reserve if the catalogue is not already reserved
			boolean reservable = mainMenu.getCatalogue().isReservable();
			
			// can unreserve if we had reserved the catalogue ( not another user )
			boolean unReservable = mainMenu.getCatalogue().isUnreservable();

			// if we are reserving a catalogue
			// disable the action which potentially 
			// can modify the catalogue
			if ( mainMenu.getCatalogue().isReserving() ) {
				
				if ( reserveMI != null ) {
					reserveMI.setText( Messages.getString("Reserve.WaitingResponse") );
					reserveMI.setEnabled( false );
				}
				
				if ( unreserveMI != null ) {
					unreserveMI.setText( Messages.getString("Reserve.WaitingResponse") );
					unreserveMI.setEnabled( false );
				}
				
				if ( resetMI != null )
					resetMI.setEnabled( false );
				
				if ( importMI != null )
					importMI.setEnabled( false );
				
				if ( hierarchyEditMI != null )
					hierarchyEditMI.setEnabled ( false );
				
				if ( attributeEditMI != null )
					attributeEditMI.setEnabled( false );
				
				if ( appendMI != null )
					appendMI.setEnabled( false );
			}
			else {
				
				if ( reserveMI != null ) {
					// can reserve only if not local and catalogue not reserved
					reserveMI.setEnabled( reservable );
					unreserveMI.setEnabled( unReservable );
				}

				if ( unreserveMI != null ) {
					reserveMI.setText( Messages.getString( "BrowserMenu.Reserve") );
					unreserveMI.setText( Messages.getString( "BrowserMenu.Unreserve" ) );
				}
				
				if ( resetMI != null ) {
					
					// enable resetMI only if the catalogue is an internal version
					// and if the catalogue is reserved by the current user
					resetMI.setEnabled( mainMenu.getCatalogue().isReservedBy( user ) && 
							mainMenu.getCatalogue().getRawVersion().isInternalVersion() );
				}

				if ( importMI != null )
					importMI.setEnabled( canEdit );
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
		
		String text = "";
		
		// ask the reserve description
		DialogSingleText dialog = new DialogSingleText( shell, 10 );
		dialog.setTitle( Messages.getString( "BrowserMenu.ReserveTitle" ) );
		dialog.setMessage( Messages.getString( "BrowserMenu.ReserveMessage" ) );

		text = dialog.open();

		// return if cancel was pressed
		// no description was given
		if ( text == null )
			return;
		
		// set wait cursor
		GlobalUtil.setShellCursor( shell, SWT.CURSOR_WAIT );
		
		// create a reserve request for the catalogue
		ReserveBuilder builder = createReserveRequest( mainMenu.getCatalogue(), 
				level, text );
		
		// send the request
		builder.build();
		
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
	private ReserveBuilder createReserveRequest( Catalogue catalogue, 
			final ReserveLevel level, String text ) {

		// reserve the catalogue
		Dcf dcf = new Dcf();

		// create a progress bar for the possible import process
		final FormProgressBar progressBar = new FormProgressBar( shell, 
				Messages.getString( "Reserve.NewInternalTitle" ) );

		// move down the location of the progress bar
		progressBar.setLocation( progressBar.getLocation().x, 
				progressBar.getLocation().y + 170 );

		// create the request builder
		ReserveBuilder builder = dcf.createReserveRequest( mainMenu.getCatalogue(), level, text );

		// set the progress bar for the request
		builder.setProgressBar( progressBar );
		
		// listener called when the reserve starts
		builder.setStartListener( new ReserveStartedListener() {

			@Override
			public void reserveStarted( final ReserveResult reserveLog ) {
				shell.getDisplay().syncExec( new Runnable() {

					@Override
					public void run() {

						// lock the closure of the window since
						// we are making important things
						ShellLocker.setLock( shell, 
								Messages.getString( "MainPanel.CannotCloseTitle" ), 
								Messages.getString( "MainPanel.CannotCloseMessage" ) );

						// Warn user of the performed actions
						warnLog ( reserveLog );
					}
				});
			}
		});

		// called when the reserve finishes
		builder.setFinishlistener( new ReserveFinishedListener() {
			
			@Override
			public void reserveFinished( final Catalogue catalogue, 
					final DcfResponse response ) {

				// notify the user when the reserve
				// was successfully finished or if
				// errors occurred
				shell.getDisplay().syncExec( new Runnable() {

					@Override
					public void run() {
						
						// remove the lock from the shell
						ShellLocker.removeLock( shell );

						// if busy we force editing => we update UI
						if ( response == DcfResponse.OK || 
								( level.greaterThan( ReserveLevel.NONE ) 
										&& response == DcfResponse.BUSY ) ) {
							
							// notify the observers that the reserve level has changed
							mainMenu.update( level );
						}
						
						// show dialog
						mainMenu.warnDcfResponse( catalogue, response, level );
					}
				});
			}
		});
		
		// called when the log code of the request is found
		builder.setLogCodeListener( new LogCodeFoundListener() {
			
			@Override
			public void logCodeFound(String logCode) {
				
				shell.getDisplay().syncExec( new Runnable() {
					
					@Override
					public void run() {
						// remove the lock from the shell
						ShellLocker.removeLock( shell );
					}
				});
			}
		});
		
		// called if a new version of the catalogue was downloaded
		builder.setNewVersionListener( new Listener() {
			
			@Override
			public void handleEvent(Event arg0) {
				
				shell.getDisplay().syncExec( new Runnable() {
					
					@Override
					public void run() {
						
						// Warn user that a new version of the catalogue
						// is being downloaded
						mainMenu.warnUser ( Messages.getString("Reserve.OldTitle"),
								Messages.getString( "Reserve.OldMessage" ) );
						
						// change progress bar label
						progressBar.setLabel( Messages.getString("Reserve.NewInternalTitle2") );
					}
				});
			}
		});
		
		// called when the catalogue is about to be reserved
		// (just before calling the reserve request)
		builder.setStartReserveListener( new Listener() {
			
			@Override
			public void handleEvent(Event arg0) {
				
				shell.getDisplay().syncExec( new Runnable() {

					@Override
					public void run() {

						// lock the closure of the window since
						// we are creating a new db
						ShellLocker.setLock( shell, 
								Messages.getString( "MainPanel.CannotCloseTitle" ), 
								Messages.getString( "MainPanel.CannotCloseMessage" ) );
					}
				});
			}
		});
		
		return builder;
	}
	
	/**
	 * Warn the user based on the reserve log status
	 * the correct reserve and not reserving are not included
	 * since are covered by the start listener of the reserve log
	 * (Otherwise they would have been called two times)
	 * @param reserveLog
	 */
	private void warnLog ( ReserveResult reserveLog ) {
		
		// Warn user of the performed actions

		switch ( reserveLog ) {
		case ERROR:
			mainMenu.handleError ( Messages.getString("Reserve.GeneralErrorTitle"),
					Messages.getString( "Reserve.GeneralErrorMessage" ) );
			break;
			
		case MINOR_FORBIDDEN:
			mainMenu.handleError ( Messages.getString("Reserve.MinorErrorTitle"),
					Messages.getString( "Reserve.MinorErrorMessage" ) );
			break;
			
		case OLD_VERSION:
			break;
			
		case CORRECT_VERSION:
		case NOT_RESERVING:

			// say to the user that the reserve started
			mainMenu.warnUser ( Messages.getString("Reserve.ReserveStartedTitle"),
					Messages.getString( "Reserve.ReserveStartedMessage" ) );
		default:
			break;
		}
	}
}
