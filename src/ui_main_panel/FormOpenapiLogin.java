<<<<<<< HEAD
package ui_main_panel;

import javax.xml.soap.SOAPException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import dcf_user.User;
import messages.Messages;
import soap.DetailedSOAPException;
import utilities.GlobalUtil;

/**
 * Form used to login on openapi portal with token
 * 
 * @author shahaal
 *
 */
public class FormOpenapiLogin {

	private static final Logger LOGGER = LogManager.getLogger(FormOpenapiLogin.class);

	private String title;

	private Shell shell;
	private Shell dialog;
	private Text tokenText;
	private final static String OApiUsr = "Openapi_Guest";
	private Button loginBtn;

	// if the credentials are valid or not
	private boolean valid;

	private CredentialListener listener;

	/**
	 * Initialize the login form with the shell and its title
	 * 
	 * @param shell
	 * @param title
	 */
	public FormOpenapiLogin(Shell shell, String title) {
		this.shell = shell;
		this.title = title;
		this.valid = false;
	}

	/**
	 * Display the login form
	 */
	public void display() {

		// create a dialog and set its title
		dialog = new Shell(shell, SWT.TITLE | SWT.APPLICATION_MODAL | SWT.WRAP);
		dialog.setText(title);

		// window = new RestoreableWindow(dialog, WINDOW_CODE);

		// set the dialog layout
		dialog.setLayout(new GridLayout(1, false));

		// add username token text box
		addCredential(dialog);

		// add button to login
		addLoginButton(dialog);

		// resize the dialog to the preferred size (the hints)
		dialog.pack();

		// show the dialog
		dialog.setVisible(true);
		dialog.open();

		while (!dialog.isDisposed()) {
			if (!dialog.getDisplay().readAndDispatch())
				dialog.getDisplay().sleep();
		}
		dialog.dispose();
	}

	/**
	 * Add the credential widgets to the parent
	 * 
	 * @param parent
	 */
	private void addCredential(Shell parent) {
		
		GridData data = new GridData(SWT.FILL, SWT.FILL, true, false);
		data.minimumWidth = 200;
		data.minimumHeight = 10;
		
		// add token
		Label tokenLabel = new Label(parent, SWT.NONE);
		tokenLabel.setText(Messages.getString("FormOpenAPILogin.TokenLabel"));

		tokenText = new Text(parent, SWT.PASSWORD);
		tokenText.setLayoutData(data);

		// if the password text changes
		tokenText.addModifyListener(new ModifyListener() {

			@Override
			public void modifyText(ModifyEvent e) {
				String token = tokenText.getText();
				loginBtn.setEnabled(!token.isEmpty());
			}
		});

	}

	/**
	 * Add the button for saving the openapi credentials
	 * 
	 * @param parent
	 * @return
	 */
	private Button addLoginButton(final Shell parent) {

		loginBtn = new Button(parent, SWT.NONE);
		loginBtn.setText(Messages.getString("FormDCFLogin.LoginButton"));
		loginBtn.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
		loginBtn.setEnabled(false);

		// set as default button the login btn
		parent.setDefaultButton(loginBtn);

		loginBtn.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {

				// wait cursor (we make a 'long' operation)
				GlobalUtil.setShellCursor(parent, SWT.CURSOR_WAIT);

				String token = tokenText.getText();

				LOGGER.info("Checking credentials");

				// check the correctness of credentials
				try {
					valid = checkCredentials(token);
				} catch (DetailedSOAPException e1) {

					e1.printStackTrace();

					// reset the original cursor
					GlobalUtil.setShellCursor(parent, SWT.CURSOR_ARROW);

					String[] warning = GlobalUtil.getSOAPWarning(e1);
					GlobalUtil.showErrorDialog(shell, warning[0], warning[1]);

					return;
				}

				// reset the original cursor
				GlobalUtil.setShellCursor(parent, SWT.CURSOR_ARROW);

				// check if the credentials are correct or not
				if (valid) {

					// close the dialog
					parent.close();
				} else {

					String logTitle = Messages.getString("FormDCFLogin.ErrorTitle");
					String logMessage = Messages.getString("FormDCFLogin.WrongCredentialMessage");

					// show the dialog to show the login results
					GlobalUtil.showErrorDialog(shell, logTitle, logMessage);
				}

				// call the listener if it was set
				if (listener != null)
					listener.credentialsSet(OApiUsr, token, valid);
			}
		});

		return loginBtn;
	}

	/**
	 * Check the credentials
	 * 
	 * @return true if the credentials are correct
	 * @throws SOAPException
	 */
	private boolean checkCredentials(String token) throws DetailedSOAPException {
		return User.getInstance().loginWithOpenapi(OApiUsr, token, true);
	}

	/**
	 * Check if the credentials are correct or not
	 * 
	 * @return
	 */
	public boolean isValid() {
		return valid;
	};

	/**
	 * Add a listener called when the credential are inserted and confirmed.
	 * 
	 * @param listener
	 */
	public void addCredentialListener(CredentialListener listener) {
		this.listener = listener;
	}

	public static String getOapiusr() {
		return OApiUsr;
	}

	/**
	 * Interface for the credential listener
	 *
	 */
	public interface CredentialListener {
		void credentialsSet(String username, String token, boolean correct);
	}
}
=======
package ui_main_panel;

import javax.xml.soap.SOAPException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import dcf_user.User;
import messages.Messages;
import soap.DetailedSOAPException;
import utilities.GlobalUtil;

/**
 * Form used to login on openapi portal with token
 * 
 * @author shahaal
 *
 */
public class FormOpenapiLogin {

	private static final Logger LOGGER = LogManager.getLogger(FormOpenapiLogin.class);

	private String title;

	private Shell shell;
	private Shell dialog;
	private Text tokenText;
	private final static String OApiUsr = "Openapi_Guest";
	private Button loginBtn;

	// if the credentials are valid or not
	private boolean valid;

	private CredentialListener listener;

	/**
	 * Initialize the login form with the shell and its title
	 * 
	 * @param shell
	 * @param title
	 */
	public FormOpenapiLogin(Shell shell, String title) {
		this.shell = shell;
		this.title = title;
		this.valid = false;
	}

	/**
	 * Display the login form
	 */
	public void display() {

		// create a dialog and set its title
		dialog = new Shell(shell, SWT.TITLE | SWT.APPLICATION_MODAL | SWT.WRAP);
		dialog.setText(title);

		// set the shell position
		dialog.setLocation(100, 100);

		// window = new RestoreableWindow(dialog, WINDOW_CODE);

		// set the dialog layout
		dialog.setLayout(new GridLayout(1, false));
		GridData gridData = new GridData();
		gridData.horizontalAlignment = SWT.CENTER;
		gridData.verticalAlignment = SWT.CENTER;
		gridData.grabExcessHorizontalSpace = true;
		gridData.minimumHeight = 200;
		gridData.minimumWidth = 200;
		gridData.widthHint = 400;

		dialog.setLayoutData(gridData);

		// add username token text box
		addCredential(dialog);

		// add button to login
		addLoginButton(dialog);

		// resize the dialog to the preferred size (the hints)
		dialog.pack();

		// show the dialog
		dialog.setVisible(true);
		dialog.open();

		while (!dialog.isDisposed()) {
			if (!dialog.getDisplay().readAndDispatch())
				dialog.getDisplay().sleep();
		}
		dialog.dispose();
	}

	/**
	 * Add the credential widgets to the parent
	 * 
	 * @param parent
	 */
	private void addCredential(Shell parent) {

		GridData gridData = new GridData();
		gridData.horizontalAlignment = SWT.CENTER;
		gridData.verticalAlignment = SWT.CENTER;
		gridData.grabExcessHorizontalSpace = true;
		gridData.minimumHeight = 100;
		gridData.minimumWidth = 100;
		gridData.widthHint = 200;
		gridData.heightHint = 20;

		// add token
		Label tokenLabel = new Label(parent, SWT.NONE);
		tokenLabel.setText(Messages.getString("FormOpenAPILogin.TokenLabel"));
		tokenLabel.setLayoutData(gridData);

		tokenText = new Text(parent, SWT.PASSWORD);
		tokenText.setLayoutData(gridData);

		// if the password text changes
		tokenText.addModifyListener(new ModifyListener() {

			@Override
			public void modifyText(ModifyEvent e) {
				String token = tokenText.getText();
				loginBtn.setEnabled(!token.isEmpty());
			}
		});

	}

	/**
	 * Add the button for saving the openapi credentials
	 * 
	 * @param parent
	 * @return
	 */
	private Button addLoginButton(final Shell parent) {

		GridData gridData = new GridData();
		gridData.horizontalAlignment = SWT.CENTER;
		gridData.verticalAlignment = SWT.CENTER;

		loginBtn = new Button(parent, SWT.NONE);
		loginBtn.setText(Messages.getString("FormDCFLogin.LoginButton"));
		loginBtn.setLayoutData(gridData);
		loginBtn.setEnabled(false);

		// set as default button the login btn
		parent.setDefaultButton(loginBtn);

		loginBtn.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {

				// wait cursor (we make a 'long' operation)
				GlobalUtil.setShellCursor(parent, SWT.CURSOR_WAIT);

				String token = tokenText.getText();

				LOGGER.info("Checking credentials");

				// check the correctness of credentials
				try {
					valid = checkCredentials(token);
				} catch (DetailedSOAPException e1) {
					
					e1.printStackTrace();
					
					// reset the original cursor
					GlobalUtil.setShellCursor(parent, SWT.CURSOR_ARROW);

					String[] warning = GlobalUtil.getSOAPWarning(e1);
					GlobalUtil.showErrorDialog(shell, warning[0], warning[1]);

					return;
				}
				
				
				// reset the original cursor
				GlobalUtil.setShellCursor(parent, SWT.CURSOR_ARROW);
				
				// check if the credentials are correct or not
				if (valid) {

					// close the dialog
					parent.close();
				} else {

					String logTitle = Messages.getString("FormDCFLogin.ErrorTitle");
					String logMessage = Messages.getString("FormDCFLogin.WrongCredentialMessage");

					// show the dialog to show the login results
					GlobalUtil.showErrorDialog(shell, logTitle, logMessage);
				}

				// call the listener if it was set
				if (listener != null)
					listener.credentialsSet(OApiUsr, token, valid);
			}
		});

		return loginBtn;
	}

	/**
	 * Check the credentials
	 * 
	 * @return true if the credentials are correct
	 * @throws SOAPException
	 */
	private boolean checkCredentials(String token) throws DetailedSOAPException {
		return User.getInstance().loginWithOpenapi(OApiUsr, token, true);
	}

	/**
	 * Check if the credentials are correct or not
	 * 
	 * @return
	 */
	public boolean isValid() {
		return valid;
	};

	/**
	 * Add a listener called when the credential are inserted and confirmed.
	 * 
	 * @param listener
	 */
	public void addCredentialListener(CredentialListener listener) {
		this.listener = listener;
	}

	public static String getOapiusr() {
		return OApiUsr;
	}

	/**
	 * Interface for the credential listener
	 *
	 */
	public interface CredentialListener {
		void credentialsSet(String username, String token, boolean correct);
	}
}
>>>>>>> 574ffe363e78d250cf6350ff4ea89f2f48352380
