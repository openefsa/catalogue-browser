package ui_licence;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

public class FormBrowser {

	Shell		_shell;
	String		_title;
	String		_url	= null;
	InputStream	_inputStream;
	Shell		dialog;
	Browser		_browser;

	public FormBrowser( Shell sh, String title, String url ) {
		_shell = sh;
		_title = title;
		_url = url;
	}

	public FormBrowser( Shell sh, String title, InputStream inputStream ) {
		_shell = sh;
		_title = title;
		_inputStream = inputStream;
	}

	public void display ( ) {
		dialog = new Shell( _shell , SWT.SHELL_TRIM | SWT.APPLICATION_MODAL );
		dialog.setImage( new Image( Display.getCurrent() , this.getClass().getClassLoader()
				.getResourceAsStream( "Print24.gif" ) ) );
		dialog.setMaximized( true );

		dialog.setText( _title );
		dialog.setLayout( new GridLayout( 1 , false ) );
		/*
		 * Group grp=new Group(dialog,SWT.NONE); GridData gridData = new
		 * GridData(); gridData.verticalAlignment = SWT.FILL;
		 * gridData.horizontalAlignment = SWT.FILL;
		 * gridData.grabExcessHorizontalSpace =true;
		 * gridData.grabExcessVerticalSpace =false; grp.setLayoutData(gridData);
		 * grp.setLayout(new RowLayout()); final Button printPreview=new
		 * Button(grp,SWT.TOGGLE); printPreview.setText("Print Preview");
		 * //printPreview.pack(); final Button print=new Button(grp,SWT.TOGGLE);
		 * print.setText("Print");
		 * 
		 * //print.pack(); final Button close=new Button(grp,SWT.TOGGLE);
		 * close.setText("Close");
		 */
		// close.pack();
		_browser = new Browser( dialog , SWT.NONE );
		GridData gridData = new GridData();
		gridData.verticalAlignment = SWT.FILL;
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		_browser.setLayoutData( gridData );
		if ( _url != null )
			_browser.setUrl( _url );
		else {
			char[] buf = new char[2048];
			Reader r;
			try {
				r = new InputStreamReader( _inputStream , "UTF-8" );
			} catch ( UnsupportedEncodingException e ) {
				e.printStackTrace();
				r = null;
			}
			StringBuilder s = new StringBuilder();
			while ( true ) {
				int n = -1;
				try {
					n = r.read( buf );
				} catch ( IOException e ) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if ( n < 0 )
					break;

				s.append( buf, 0, n );

			}

			String htmlText = "<html><header><title>Derby Notice</title></header><body><pre>" + s.toString()
					+ "</pre></body></html>";
			_browser.setText( htmlText );
		}

		/*
		 * final Button cancel=new Button(grp,SWT.TOGGLE); gridData = new
		 * GridData(); gridData.verticalAlignment = SWT.FILL;
		 * gridData.horizontalAlignment = SWT.FILL;
		 * gridData.grabExcessHorizontalSpace =false;
		 * gridData.grabExcessVerticalSpace =false; cancel.pack();
		 * cancel.setLayoutData(gridData); cancel.setText("Cancel");
		 */
		/*
		 * Monitor primary = _shell.getMonitor(); Rectangle
		 * bounds=primary.getBounds(); Rectangle pict=dialog.getBounds(); int
		 * x=bounds.x + (bounds.width-pict.width)/2; int y=bounds.y +
		 * (bounds.height-pict.height)/2; dialog.setLocation(x,y);
		 */
		/*
		 * printPreview.addSelectionListener( new SelectionAdapter(){
		 * 
		 * @Override public void widgetSelected(SelectionEvent event) {
		 * 
		 * _browser.execute("this.ExecWB(7,1)");
		 * 
		 * }
		 * 
		 * });
		 * 
		 * print.addSelectionListener( new SelectionAdapter(){
		 * 
		 * @Override public void widgetSelected(SelectionEvent event) {
		 * dialog.close(); }
		 * 
		 * });
		 * 
		 * close.addSelectionListener( new SelectionAdapter(){
		 * 
		 * @Override public void widgetSelected(SelectionEvent event) {
		 * dialog.close(); }
		 * 
		 * });
		 */

		dialog.open();

		/*
		 * OLECMDID values: 64.* 6 - print 65.* 7 - print preview 66.* 1 - open
		 * window 67.* 4 - Save As 68.
		 */

		// this will go in queue on the main window, no problem with this
	}

}
