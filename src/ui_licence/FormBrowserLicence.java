package ui_licence;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;

import messages.Messages;
import ui_main_panel.CatalogueBrowserMain;

/**
 * Dialog which shows the licence of the catalogue browser.
 * @author avonva
 *
 */
public class FormBrowserLicence {

	private boolean	_dialog			= false;
	private Display	_display;
	private Image	_image;

	private Shell	startupWindow	= null;

	public void setDialog ( ) {
		_dialog = true;
	}

	/**
	 * Display the dialog
	 */
	public void display ( ) {
		if ( !_dialog ) {
			startupWindow = new Shell( _display , SWT.NO_TRIM | SWT.ON_TOP );
			startupWindow.setSize( 417, 500 );
		} else {
			startupWindow = new Shell( _display , SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL );
			startupWindow.setSize( 417, 500 );
		}

		startupWindow.setLayout( new GridLayout( 1 , false ) );

		_image = new Image( Display.getCurrent(), this.getClass().getClassLoader()
				.getResourceAsStream( "Catalogue-browser.gif" ) );

		final Canvas canvas = new Canvas( startupWindow , SWT.NONE );
		canvas.setBackgroundImage( _image );

		GridData shellGridData = new GridData();
		shellGridData.horizontalAlignment = SWT.FILL;
		shellGridData.verticalAlignment = SWT.FILL;
		shellGridData.grabExcessHorizontalSpace = true;
		shellGridData.grabExcessVerticalSpace = false;
		shellGridData.minimumHeight = 282;
		shellGridData.heightHint = 282;
		canvas.setLayoutData( shellGridData );

		if ( _dialog ) {
			Label l = new Label( startupWindow , SWT.NONE );
			l.setText( Messages.getString( "Startup.AppVersion" ) + " " + CatalogueBrowserMain.APP_VERSION );

			shellGridData = new GridData();
			shellGridData.horizontalAlignment = SWT.FILL;
			shellGridData.verticalAlignment = SWT.TOP;
			shellGridData.grabExcessHorizontalSpace = true;
			shellGridData.grabExcessVerticalSpace = false;

			l.setLayoutData( shellGridData );

			Label l3 = new Label( startupWindow , SWT.NONE );
			l3.setText( Messages.getString( "Startup.EFSACopyright" ) );
			shellGridData = new GridData();
			shellGridData.horizontalAlignment = SWT.FILL;
			shellGridData.verticalAlignment = SWT.TOP;
			shellGridData.grabExcessHorizontalSpace = true;
			shellGridData.grabExcessVerticalSpace = false;

			l3.setLayoutData( shellGridData );

			Label l4 = new Label( startupWindow , SWT.NONE );
			l4.setText( Messages.getString( "Startup.LicenceStmt" ) );
			shellGridData = new GridData();
			shellGridData.horizontalAlignment = SWT.FILL;
			shellGridData.verticalAlignment = SWT.TOP;
			shellGridData.grabExcessHorizontalSpace = true;
			shellGridData.grabExcessVerticalSpace = false;

			l4.setLayoutData( shellGridData );

			StyledText t1 = new StyledText( startupWindow , SWT.BORDER | SWT.MULTI | SWT.V_SCROLL
					| SWT.H_SCROLL );

			t1.setEditable( false );
			
			try {
				t1.setText( readLicenceFile ( "LICENCE.txt" ) );
			} catch (IOException e) {
				t1.setText( "No licence file was found (LICENCE.txt)" );
				e.printStackTrace();
			}
			
			shellGridData = new GridData();
			shellGridData.horizontalAlignment = SWT.FILL;
			shellGridData.verticalAlignment = SWT.FILL;
			shellGridData.grabExcessHorizontalSpace = true;
			shellGridData.grabExcessVerticalSpace = true;

			t1.setLayoutData( shellGridData );

		}

		Monitor primary = _display.getPrimaryMonitor();
		Rectangle bounds = primary.getBounds();
		Rectangle pict = startupWindow.getBounds();
		int x = bounds.x + ( bounds.width - pict.width ) / 2;
		int y = bounds.y + ( bounds.height - pict.height ) / 2;
		startupWindow.setLocation( x, y );
		startupWindow.open();
		if ( !_dialog )
			_display.timerExec( 3000, new Runnable() {
				public void run ( ) {
					startupWindow.close();
				}
			} );

	}
	
	/**
	 * Read the licence text file to display it
	 * @param filename the licence filename
	 * @return the string contained in the file
	 * @throws IOException
	 */
	private String readLicenceFile ( String filename ) throws IOException {

		InputStream input = FormBrowserLicence.class.getClassLoader().getResourceAsStream( filename );

		BufferedInputStream bis = new BufferedInputStream( input );
		ByteArrayOutputStream buf = new ByteArrayOutputStream();
		
		int result = bis.read();
		
		while( result != -1 ) {
		    buf.write((byte) result);
		    result = bis.read();
		}

		// StandardCharsets.UTF_8.name() > JDK 7
		String output = buf.toString( "UTF-8" );
		
		buf.close();
		bis.close();
		
		return output;
	}

	public FormBrowserLicence( Display display ) {
		_display = display;
	}

}
