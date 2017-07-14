package ui_main_menu;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;

import catalogue_generator.ThreadFinishedListener;
import dcf_manager.Dcf;
import dcf_pending_action.PendingPublish;
import dcf_pending_action.PendingReserve;
import dcf_pending_action.PendingUploadData;
import dcf_pending_action.PendingXmlDownload;
import dcf_user.User;
import messages.Messages;
import ui_main_panel.FormDCFLogin;
import ui_progress_bar.FormProgressBar;
import utilities.GlobalUtil;

/**
 * Login button of the main panel
 * @author avonva
 *
 */
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

				login.display();

				// if wrong credentials return
				if ( !login.isValid() )
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

				dcf.setUserLevel( new ThreadFinishedListener() {

					@Override
					public void finished(Thread thread, int code) {

						// if correct
						if ( code == ThreadFinishedListener.OK ) {

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
						else { // errors
							GlobalUtil.showErrorDialog(shell, 
									Messages.getString("ExportCatalogue.ErrorTitle"), 
									Messages.getString("ExportCatUsers.ErrorMessage"));
						}
					}
				});
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

		FormProgressBar bar = new FormProgressBar( shell, 
				Messages.getString("Reserve.NewInternalTitle") );
		
		bar.setLocation( bar.getLocation().x, bar.getLocation().y + 170 );
		
		dcf.setProgressBar( bar );
		
		
		
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
