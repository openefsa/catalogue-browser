package ui_general_graphics;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Text;

import i18n_messages.CBMessages;

import org.eclipse.swt.widgets.Shell;

public class FormPassword {
	Shell			_shell;

	private String	title	= CBMessages.getString("FormPassword.DialogTitle"); //$NON-NLS-1$
	private Shell	dialog;
	private String	_password = ""; // Initialize to avoid null pointer exception if no password is inserted //$NON-NLS-1$
	private Text	passwordText;

	public FormPassword( Shell sh ) {
		_shell = sh;
	}

	public void setTitle ( String title ) {
		this.title = title;
	}

	public String getTitle ( ) {
		return title;
	}

	public String getPassword ( ) {
		return _password;
	}

	public void display ( ) {
		dialog = new Shell( _shell , SWT.TITLE | SWT.APPLICATION_MODAL );
		dialog.setText( title );
		dialog.setSize( 300, 120 );

		dialog.setLayout( new GridLayout( 1 , false ) );

		Group pwdGroup = new Group( dialog , SWT.NONE );
		pwdGroup.setText( CBMessages.getString("FormPassword.DialogSubtitle") ); //$NON-NLS-1$
		pwdGroup.setLayout( new FillLayout() );

		passwordText = new Text( pwdGroup , SWT.PASSWORD | SWT.SINGLE | SWT.BORDER );
		passwordText.setTextLimit( 20 );
		
		
		// set the layout data for the password textbox
		GridData gridData = new GridData();
		gridData.verticalAlignment = SWT.FILL;
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = false;
		gridData.minimumWidth = 100;
		pwdGroup.setLayoutData( gridData );

		Composite grpButton = new Composite( dialog , SWT.NONE );

		grpButton.setLayout( new GridLayout( 2 , false ) );

		gridData = new GridData();
		gridData.verticalAlignment = SWT.FILL;
		gridData.horizontalAlignment = SWT.CENTER;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		grpButton.setLayoutData( gridData );

		final Button ok = new Button( grpButton , SWT.TOGGLE );
		gridData = new GridData();
		gridData.verticalAlignment = SWT.FILL;
		gridData.horizontalAlignment = SWT.RIGHT;
		gridData.grabExcessHorizontalSpace = false;
		gridData.grabExcessVerticalSpace = false;
		ok.setLayoutData( gridData );
		ok.setText( CBMessages.getString("FormPassword.OkButton") ); //$NON-NLS-1$
		ok.pack();

		final Button cancel = new Button( grpButton , SWT.TOGGLE );
		gridData = new GridData();
		gridData.verticalAlignment = SWT.FILL;
		gridData.horizontalAlignment = SWT.LEFT;
		gridData.grabExcessHorizontalSpace = false;
		gridData.grabExcessVerticalSpace = false;
		cancel.setLayoutData( gridData );
		cancel.setText( CBMessages.getString("FormPassword.CancelButton") ); //$NON-NLS-1$
		cancel.pack();

		dialog.pack();

		Monitor primary = _shell.getMonitor();
		Rectangle bounds = primary.getBounds();
		Rectangle pict = dialog.getBounds();
		int x = bounds.x + ( bounds.width - pict.width ) / 2;
		int y = bounds.y + ( bounds.height - pict.height ) / 2;
		dialog.setLocation( x, y );

		dialog.setVisible( true );

		ok.addSelectionListener( new SelectionAdapter() {

			@Override
			public void widgetSelected ( SelectionEvent event ) {
				_password = passwordText.getText();
				dialog.close();
			}

		} );

		cancel.addSelectionListener( new SelectionAdapter() {

			@Override
			public void widgetSelected ( SelectionEvent event ) {
				dialog.close();
			}

		} );

		// I create a wait on a message queue
		while ( !dialog.isDisposed() ) {
			if ( !dialog.getDisplay().readAndDispatch() )
				dialog.getDisplay().sleep();
		}

	}
}
