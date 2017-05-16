package ui_progress_bar;


import messages.Messages;
import ui_main_panel.MainPanel;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;


public class ProgressBarShell extends Shell {


	private int			total		= 100;
	private Integer		done		= 0;
	private String		_title		= ""; //$NON-NLS-1$
	private boolean		cancelled	= false;
	private boolean		completed;
	private ProgressBar	progressBar;
	private Display _d=null;

	public ProgressBarShell( Display d , String title ) {
		super(d,  SWT.CLOSE | SWT.APPLICATION_MODAL);
		_d=d;
		_title=title;
		this.init();
		this.open();
	}
	public void checkSubclass() {

	}
	
	public void disp(){
		while (!this.isDisposed()) {
			if (!_d.readAndDispatch()) {
				_d.sleep();
			}
		}
		_d.dispose();
	}
	


	void setPartDone ( final int percent ) {
//		if (_d.isDisposed())
//			disp();
		_d.asyncExec(new Runnable() {
			public void run() {	
				if (progressBar.isDisposed())
					return;
				if ( percent >= 100 ) {
					done = 100;
					completed = true;
				} else if ( percent < 0 ) {
					done = 0;
					completed = false;
				} else {
					done = percent;
					completed = false;
				}
				progressBar.setSelection( done );
			}
		});
	}

	public void  init( ) {
		this.setText( _title );
		this.setSize( 300, 100 );
		this.setLayout( new FillLayout() );
		Group grp = new Group( this , SWT.NONE );
		grp.setLayout( new GridLayout( 2 , false ) );

		progressBar = new ProgressBar( grp , SWT.SMOOTH );
		progressBar.setMaximum( total );
		progressBar.setMinimum( 0 );
		GridData gridData = new GridData();
		gridData.verticalAlignment = SWT.CENTER;
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		progressBar.setLayoutData( gridData );

		Button cancel = new Button( grp , SWT.NONE );
		gridData = new GridData();
		gridData.verticalAlignment = SWT.CENTER;
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = false;
		gridData.grabExcessVerticalSpace = true;
		cancel.setLayoutData( gridData );
		cancel.setText( Messages.getString("ProgressBarShell.CancelButton") ); //$NON-NLS-1$
		cancel.pack();

		Monitor primary = _d.getPrimaryMonitor();
		Rectangle bounds = primary.getBounds();
		Rectangle pict = this.getBounds();
		int x = bounds.x + ( bounds.width - pict.width ) / 2;
		int y = bounds.y + ( bounds.height - pict.height ) / 2;
		this.setLocation( x, y );

		cancel.addSelectionListener( new SelectionAdapter() {
			@Override
			public void widgetSelected ( SelectionEvent event ) {
				cancelled = true;
				close();
			}
		} );

	}
	public boolean isDisplayDisposed ( ) {
		return _d.isDisposed();
	}
	
}
