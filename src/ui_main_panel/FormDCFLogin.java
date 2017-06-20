package ui_main_panel;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import dcf_manager.Dcf;
import dcf_manager.Dcf.DcfType;
import dcf_user.User;
import messages.Messages;
import session_manager.RestoreableWindow;
import session_manager.WindowPreference;
import utilities.GlobalUtil;

/**
 * Form used to login into the DCF using username and
 * password. It is possible to listen the form in order
 * to get the credentials and if they are valid or not
 * @author avonva
 *
 */
public class FormDCFLogin implements RestoreableWindow {
	
	private static final String WINDOW_CODE = "FormDCFLogin";
	
	private String title;
	
	private Shell shell;
	private Shell dialog;
	private Text usernameText;
	private Text passwdText;
	private Button loginBtn;
	
	// if the credentials are valid or not
	private boolean valid;
	
	private CredentialListener listener;
	
	/**
	 * Initialize the login form with the shell and
	 * its title
	 * @param shell
	 * @param title
	 */
	public FormDCFLogin( Shell shell, String title ) {
		this.shell = shell;
		this.title = title;
		this.valid = false;
	}
	
	/**
	 * Display the login form
	 */
	public void display () {
		
		// create a dialog and set its title
		dialog = new Shell( shell , SWT.TITLE | SWT.APPLICATION_MODAL | SWT.SHELL_TRIM );
		dialog.setText( title );
		
		// set the dialog layout
		dialog.setLayout( new GridLayout( 1 , false ) );
		GridData gridData = new GridData();
		gridData.horizontalAlignment = SWT.CENTER;
		gridData.verticalAlignment = SWT.CENTER;
		gridData.grabExcessHorizontalSpace = true;
		gridData.minimumHeight = 100;
		gridData.minimumWidth = 100;
		gridData.widthHint = 200;
		
		dialog.setLayoutData( gridData );
		
		Label dcfTypeLabel = new Label ( dialog, SWT.CENTER );
		
		String dcfTypeText = Dcf.dcfType == DcfType.PRODUCTION ?
				Messages.getString( "BrowserMenu.DCFLoginProduction" ) :
					Messages.getString( "BrowserMenu.DCFLoginTest" );
		dcfTypeLabel.setText( dcfTypeText );
		
		// set the label font to italic and bold
		FontData fontData = Display.getCurrent().getSystemFont().getFontData()[0];

		Font font = new Font( Display.getCurrent(), 
				new FontData( fontData.getName(), fontData.getHeight() + 3, SWT.NONE ) );

		dcfTypeLabel.setFont ( font );
		
		// add username password text box
		addCredential ( dialog );
		
		// add button to make login
		addLoginButton( dialog );

		// resize the dialog to the preferred size (the hints)
		dialog.pack();
		
		// show the dialog
		dialog.setVisible( true );  
		dialog.open();

		WindowPreference.restore( this );
		WindowPreference.saveOnClosure( this );
		
		while ( !dialog.isDisposed() ) {
			if ( !dialog.getDisplay().readAndDispatch() )
				dialog.getDisplay().sleep();
		}
		dialog.dispose();
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
	 * Add the credential widgets to the parent
	 * @param parent
	 */
	private void addCredential ( Shell parent ) {
		
		GridData gridData = new GridData();
		gridData.horizontalAlignment = SWT.CENTER;
		gridData.verticalAlignment = SWT.CENTER;
		gridData.grabExcessHorizontalSpace = true;
		gridData.minimumHeight = 100;
		gridData.minimumWidth = 100;
		gridData.widthHint = 200;
		
		// add username
		Label usernameLabel = new Label ( parent, SWT.NONE );
		usernameLabel.setText( Messages.getString("FormDCFLogin.UsernameLabel"));
		usernameLabel.setLayoutData( gridData );
		
		usernameText = new Text ( parent, SWT.NONE );
		usernameText.setLayoutData( gridData );
		
		// if the username text changes
		usernameText.addModifyListener( new ModifyListener() {
			
			@Override
			public void modifyText(ModifyEvent e) {
				
				String username = usernameText.getText();
				String passwd = passwdText.getText();
				loginBtn.setEnabled( !username.isEmpty() && !passwd.isEmpty() );
			}
		});
		
		// add password
		Label passwdLabel = new Label ( parent, SWT.NONE );
		passwdLabel.setText( Messages.getString("FormDCFLogin.PasswordLabel") );
		passwdLabel.setLayoutData( gridData );
		
		passwdText = new Text ( parent, SWT.PASSWORD );
		passwdText.setLayoutData( gridData );
		
		// if the password text changes
		passwdText.addModifyListener( new ModifyListener() {

			@Override
			public void modifyText(ModifyEvent e) {

				String username = usernameText.getText();
				String passwd = passwdText.getText();
				loginBtn.setEnabled( !username.isEmpty() && !passwd.isEmpty() );
			}
		});
		
	}
	
	/**
	 * Add the button for the login in
	 * @param parent
	 * @return
	 */
	private Button addLoginButton ( final Shell parent ) {
		
		GridData gridData = new GridData();
		gridData.horizontalAlignment = SWT.CENTER;
		gridData.verticalAlignment = SWT.CENTER;
		
		loginBtn = new Button ( parent, SWT.NONE );
		loginBtn.setText( Messages.getString( "FormDCFLogin.LoginButton") );
		loginBtn.setLayoutData( gridData );
		loginBtn.setEnabled( false );
		
		// set as default button the login btn
		parent.setDefaultButton( loginBtn );
		
		loginBtn.addSelectionListener( new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				
				// wait cursor (we make a 'long' operation)
				GlobalUtil.setShellCursor( parent, SWT.CURSOR_WAIT );
				
				String username = usernameText.getText();
				String password = passwdText.getText();
				String logTitle;
				String logMessage;
				
				System.out.println( "Check credentials ");
				
				// check the correctness of credentials
				valid = checkCredentials( username, password );

				// reset the original cursor
				GlobalUtil.setShellCursor( parent, SWT.CURSOR_ARROW );
				
				// check if the credentials are correct or not
				if ( valid ) {
					
					logTitle = Messages.getString("FormDCFLogin.WelcomeTitle");
					logMessage = Messages.getString("FormDCFLogin.WelcomeMessage") + usernameText.getText();

					// close the dialog
					parent.close();
				}
				else {
					
					logTitle = Messages.getString("FormDCFLogin.ErrorTitle");
					logMessage = Messages.getString("FormDCFLogin.WrongCredentialMessage");
				}
				
				// call the listener if it was set
				if ( listener != null )
					listener.credentialsSet( username, password, valid );
				
				// show the dialog to show the login results
				GlobalUtil.showDialog( shell, logTitle, 
						logMessage, SWT.ICON_INFORMATION );
			}
		});
		
		return loginBtn;
	}
	
	/**
	 * Check the credentials
	 * @return true if the credentials are correct
	 */
	private boolean checkCredentials ( String username, String password ) {
		
		User user = User.getInstance();
		boolean correctCredentials = user.login( usernameText.getText(), 
				passwdText.getText() );
		
		return correctCredentials;
	}
	
	/**
	 * Check if the credentials are correct or not
	 * @return
	 */
	public boolean isValid() {
		return valid;
	};

	/**
	 * Add a listener called when the credential are
	 * inserted and confirmed.
	 * @param listener
	 */
	public void addCredentialListener(CredentialListener listener) {
		this.listener = listener;
	}
	
	/**
	 * Interface for the credential listener
	 * @author avonva
	 *
	 */
	public interface CredentialListener {
		void credentialsSet ( String username, String password, boolean correct );
	}
}
