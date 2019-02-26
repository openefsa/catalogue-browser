package ui_main_panel;

import javax.xml.soap.SOAPException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

import config.Config;
import dcf_user.User;
import messages.Messages;
import soap.DetailedSOAPException;
import utilities.GlobalUtil;

/**
 * Form used to login into the DCF using username and
 * password. It is possible to listen the form in order
 * to get the credentials and if they are valid or not
 * @author avonva
 * @author shahaal
 *
 */
public class FormDCFLogin {
	
	private static final Logger LOGGER = LogManager.getLogger(FormDCFLogin.class);
	
	//private RestoreableWindow window;
	//private static final String WINDOW_CODE = "FormDCFLogin";
	
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
		dialog = new Shell( shell , SWT.TITLE | SWT.APPLICATION_MODAL | SWT.WRAP);
		dialog.setText( title );
		
		//set the shell position
		dialog.setLocation(100, 100);
		
		//window = new RestoreableWindow(dialog, WINDOW_CODE);
		
		// set the dialog layout
		dialog.setLayout( new GridLayout( 1 , false ) );
		GridData gridData = new GridData();
		gridData.horizontalAlignment = SWT.CENTER;
		gridData.verticalAlignment = SWT.CENTER;
		gridData.grabExcessHorizontalSpace = true;
		gridData.minimumHeight = 200;
		gridData.minimumWidth = 200;
		gridData.widthHint = 400;
		
		dialog.setLayoutData( gridData );
		
		if (!Config.isProductionEnvironment()) {
			
			Label dcfTypeLabel = new Label ( dialog, SWT.CENTER );
			
			String dcfTypeText = Messages.getString( "BrowserMenu.DCFLoginTest" );
			dcfTypeLabel.setText( dcfTypeText );
			
			// set the label font to italic and bold
			FontData fontData = Display.getCurrent().getSystemFont().getFontData()[0];

			Font font = new Font( Display.getCurrent(), 
					new FontData( fontData.getName(), fontData.getHeight() + 3, SWT.NONE ) );

			dcfTypeLabel.setFont ( font );
		}

		
		// add username password text box
		addCredential ( dialog );
		
		// add button to make login
		addLoginButton( dialog );

		// resize the dialog to the preferred size (the hints)
		dialog.pack();
		
		// show the dialog
		dialog.setVisible( true );  
		dialog.open();

		//window.restore( BrowserWindowPreferenceDao.class );
		//window.saveOnClosure( BrowserWindowPreferenceDao.class );
		
		while ( !dialog.isDisposed() ) {
			if ( !dialog.getDisplay().readAndDispatch() )
				dialog.getDisplay().sleep();
		}
		dialog.dispose();
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
		gridData.heightHint = 20;
		
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
				
				LOGGER.info( "Checking credentials" );
				
				// check the correctness of credentials
				try {
					valid = checkCredentials( username, password );
				} catch (DetailedSOAPException e1) {
					e1.printStackTrace();
					
					// reset the original cursor
					GlobalUtil.setShellCursor( parent, SWT.CURSOR_ARROW );
					
					String[] warning = GlobalUtil.getSOAPWarning(e1);
					GlobalUtil.showErrorDialog(shell, warning[0], warning[1]);

					return;
				}

				// reset the original cursor
				GlobalUtil.setShellCursor( parent, SWT.CURSOR_ARROW );
				
				// check if the credentials are correct or not
				if ( valid ) {

					// close the dialog
					parent.close();
				}
				else {
					
					String logTitle = Messages.getString("FormDCFLogin.ErrorTitle");
					String logMessage = Messages.getString("FormDCFLogin.WrongCredentialMessage");
					
					// show the dialog to show the login results
					GlobalUtil.showErrorDialog( shell, logTitle, 
							logMessage );
				}
				
				// call the listener if it was set
				if ( listener != null )
					listener.credentialsSet( username, password, valid );
			}
		});
		
		return loginBtn;
	}
	
	/**
	 * Check the credentials
	 * @return true if the credentials are correct
	 * @throws SOAPException 
	 */
	private boolean checkCredentials ( String username, String password ) throws DetailedSOAPException {
		
		User user = User.getInstance();
		boolean correctCredentials = user.login( usernameText.getText(), 
				passwdText.getText(), true);
		
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
