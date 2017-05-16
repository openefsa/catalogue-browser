package ui_main_menu;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;

import catalogue_object.Catalogue;
import dcf_manager.Dcf;
import dcf_reserve_util.ForcedEditingListener;
import dcf_reserve_util.ReserveFinishedListener;
import dcf_user.User;
import dcf_webservice.DcfResponse;
import dcf_webservice.ReserveLevel;
import messages.Messages;
import ui_main_panel.FormDCFLogin;
import ui_main_panel.FormDCFLogin.CredentialListener;
import ui_main_panel.ShellLocker;

public class LoginMenu implements MainMenuItem {

	private MainMenu mainMenu;
	private Shell shell;
	private MenuItem loginItem;
	
	/**
	 * Login button in the main menu
	 * @param mainMenu
	 * @param menu
	 */
	public LoginMenu( MainMenu mainMenu, Menu menu ) {
		this.mainMenu = mainMenu;
		this.shell = mainMenu.getShell();
		loginItem = create( menu );
	}
	
	/**
	 * Create the dcf login button
	 * @param menu
	 * @return
	 */
	public MenuItem create ( Menu menu ) {
		
		final MenuItem loginMI = new MenuItem ( menu, SWT.NONE );
		loginMI.setText( Messages.getString( "BrowserMenu.LoginMenuName" ) );
		
		User user = User.getInstance();

		// enable button only if the user is not logged in
		loginMI.setEnabled( !user.isLogged() );
		
		loginMI.addSelectionListener( new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {

				FormDCFLogin login = new FormDCFLogin( shell, Messages.getString( "BrowserMenu.DCFLoginWindowTitle" ) );

				login.addCredentialListener( new CredentialListener() {
					
					@Override
					public void credentialsSet(String username, String password, 
							boolean correct) {
						
						// if not correct credentials return
						if ( !correct )
							return;
						
						// disable the login button
						loginMI.setEnabled( false );
						
						// disable tools menu until we have
						// obtained the user access level
						// (avoid concurrence editing in db)
						mainMenu.tools.setEnabled( false );

						// Check catalogues updates and 
						// user access level
						Dcf dcf = new Dcf();

						// start checking updates for the catalogues
						dcf.checkUpdates( new Listener() {
							
							@Override
							public void handleEvent(Event arg0) {
								refresh();
							}
						} );

						// start checking the access level of the user
						dcf.setUserLevel( new Listener() {
							
							@Override
							public void handleEvent(Event arg0) {
								
								refresh();
								
								// call the login listener to update the
								// graphics of the main panel
								if ( mainMenu.loginListener != null ) {
									mainMenu.loginListener.handleEvent( new Event() );
								}
							}
						} );
						
						// Start all the pending reserve actions
						dcf.retryReserve( new ReserveFinishedListener() {
							
							@Override
							public void reserveFinished( final Catalogue catalogue, 
									final DcfResponse response ) {
								
								shell.getDisplay().syncExec( new Runnable() {
									
									@Override
									public void run() {
										
										// Warn the user
										mainMenu.warnDcfResponse( catalogue, response, null );
									}
								});
							}
						}, 
							// when the reserve operations is started	
							new Listener() {
							
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
						},
							new ForcedEditingListener() {
								
								@Override
								public void editingForced(Catalogue catalogue, 
										String username, ReserveLevel level ) {
									
									// update the UI based on the forced reserve level
									mainMenu.update( level );
									
									// warn that the dcf is busy and the catalogue
									// editing mode was forced
									mainMenu.warnDcfResponse( catalogue, DcfResponse.BUSY, level );
								}
							},
							
							// if a new version is downloaded
							new Listener() {
								
								@Override
								public void handleEvent(Event arg0) {
									
									// warn that the user cannot modify the current catalogue
									// since he is using an old internal version
									mainMenu.warnUser( Messages.getString( "Reserve.UsingOldVersionTitle" ), 
												Messages.getString( "Reserve.UsingOldVersionMessage" ) );
								}
							});

						refresh();
						
					}
				});
				
				// display the login form
				login.display();
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {}
		});
		
		return loginMI;
	}
	
	public void refresh() {};
}
