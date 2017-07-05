package ui_main_menu;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;

import dcf_manager.Dcf;
import dcf_pending_action.PendingPublish;
import dcf_pending_action.PendingReserve;
import dcf_pending_action.PendingUploadData;
import dcf_pending_action.PendingXmlDownload;
import dcf_user.User;
import messages.Messages;
import ui_main_panel.FormDCFLogin;
import ui_main_panel.FormDCFLogin.CredentialListener;
import ui_progress_bar.FormProgressBar;
import utilities.GlobalUtil;

public class LoginMenu implements MainMenuItem {

	public static final int LOGIN_MI = 0;
	
	private MenuListener listener;
	
	private MainMenu mainMenu;
	private Shell shell;
	
	/**
	 * Login button in the main menu
	 * @param mainMenu
	 * @param menu
	 */
	public LoginMenu( MainMenu mainMenu, Menu menu ) {
		this.mainMenu = mainMenu;
		this.shell = mainMenu.getShell();
		create( menu );
	}
	
	/**
	 * Set the listener to the login button
	 * @param listener
	 */
	public void setListener(MenuListener listener) {
		this.listener = listener;
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

				FormDCFLogin login = new FormDCFLogin( shell, 
						Messages.getString( "BrowserMenu.DCFLoginWindowTitle" ) );

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
						
						dcf.refreshDataCollections();

						// progress bar for the user level
						// Note that the progress bar does not block the user interaction
						FormProgressBar progressBar = new FormProgressBar(shell, 
								Messages.getString( "Login.UserLevelProgressBarTitle" ),
								false, SWT.TITLE );

						dcf.setProgressBar( progressBar );
						
						// start checking the access level of the user
						dcf.setUserLevel( new Listener() {
							
							@Override
							public void handleEvent(Event arg0) {
								
								shell.getDisplay().asyncExec( new Runnable() {
									
									@Override
									public void run() {
										
										// once we have finished checking the user
										// level we start with the pending reserves
										// we do this here to avoid concurrence
										// editing of the database
										startPendingActions();
										
										if ( listener != null )
											listener.buttonPressed( loginMI, LOGIN_MI, null );
										
										String title = Messages.getString( "Login.PermissionTitle" );
										String msg;
										
										if ( User.getInstance().isCatManager() )
											msg = Messages.getString("Login.CatalogueManagerMessage");
										else
											msg = Messages.getString("Login.DataProviderMessage");
										
										GlobalUtil.showDialog(shell, title, msg, SWT.ICON_INFORMATION );
									}
								});
							}
						}, new Listener() {
							
							@Override
							public void handleEvent(Event arg0) {

								GlobalUtil.showErrorDialog(shell, 
										Messages.getString("ExportCatalogue.ErrorTitle"), 
										Messages.getString("ExportCatUsers.ErrorMessage"));
							}
						} );
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
	
	/**
	 * Start all the pending actions in the database
	 * @return
	 */
	public void startPendingActions() {
		
		Dcf dcf = new Dcf();
		
		// progress bar for new internal versions
		FormProgressBar progressBar = new FormProgressBar( shell, 
				Messages.getString( "Reserve.NewInternalTitle" ), 
				false, SWT.TITLE );
		
		dcf.setProgressBar( progressBar );
		
		// move down the location of the progress bar
		progressBar.setLocation( progressBar.getLocation().x, 
				progressBar.getLocation().y + 170 );
		
		// start reserve actions
		dcf.startPendingActions( PendingReserve.TYPE, 
				mainMenu.getListener() );
		
		// start publish actions
		dcf.startPendingActions( PendingPublish.TYPE, 
				mainMenu.getListener() );
		
		// start upload data actions
		dcf.startPendingActions( PendingUploadData.TYPE, 
				mainMenu.getListener() );
		
		// start upload data actions
		dcf.startPendingActions( PendingXmlDownload.TYPE, 
				mainMenu.getListener() );
	}
}
