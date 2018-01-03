package ui_progress_bar;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;


public class FormProgressBar implements IProgressBar {

	private Shell shell;
	private Shell currentShell;
	private int style;
	private CustomProgressBar progressBar;
	private Label label;
	private String title;

	private boolean opened;        // if the bar is opened or not
	private boolean cancelEnabled;

	/**
	 * Constructor, initialize the progress bar
	 * @param shell the shell where to create the progress bar
	 * @param title the title of the progress bar
	 * @param cancelEnabled if the cancel button should be inserted or not
	 */
	public FormProgressBar( Shell shell , String title, boolean cancelEnabled, int style ) {

		opened = false;

		this.shell = shell;
		this.title = title;
		this.cancelEnabled = cancelEnabled;
		this.style = style;
		this.initializeGraphics( shell, style );
	}

	@Override
	public ProgressBar getProgressBar() {
		return progressBar.getProgressBar();
	}

	/**
	 * Initialize the progress bar without cancel button
	 * @param shell the shell where to create the progress bar
	 * @param title the title of the progress bar
	 */
	public FormProgressBar( Shell shell, String title ) {
		this( shell, title, false, SWT.TITLE | SWT.APPLICATION_MODAL );
	}

	/**
	 * Reset the progress bar graphics content
	 */
	public void reset() {

		shell.getDisplay().asyncExec( new Runnable() {

			@Override
			public void run() {
				initializeGraphics ( shell, style );
			}
		});

	}

	/**
	 * Creates all the graphics for the progress bar
	 * @param parentShell
	 */
	public void initializeGraphics ( Shell parentShell, int style ) {

		currentShell = new Shell( parentShell, style );
		currentShell.setText( title );
		currentShell.setSize( 300, 130 );
		currentShell.setLayout( new FillLayout() );

		Composite grp = new Group( currentShell , SWT.NONE );
		grp.setLayout( new GridLayout( 2 , false ) );

		// label for the title
		label = new Label( grp , SWT.NONE);
		label.setText( title );

		GridData gridData = new GridData();
		gridData.verticalAlignment = SWT.CENTER;
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		label.setLayoutData( gridData );
		Label label2 = new Label( grp , SWT.NONE );

		// progress bar
		progressBar = new CustomProgressBar( grp , SWT.SMOOTH );

		Monitor primary = parentShell.getMonitor();
		Rectangle bounds = primary.getBounds();
		Rectangle pict = currentShell.getBounds();
		int x = bounds.x + ( bounds.width - pict.width ) / 2;
		int y = bounds.y + ( bounds.height - pict.height ) / 2;
		currentShell.setLocation( x, y );
	}

	/**
	 * Set the location of the progress bar
	 * @param x
	 * @param y
	 */
	public void setLocation ( int x, int y ) {
		currentShell.setLocation(x, y);
	}

	/**
	 * Get the location of the progress bar
	 */
	public Point getLocation () {
		return currentShell.getLocation();
	}

	/**
	 * Show the progress bar
	 */
	public void open ( ) {
		opened = true;

		if (!shell.isDisposed()) {
			shell.getDisplay().asyncExec( new Runnable() {

				@Override
				public void run() {
					currentShell.open();
				}
			});
		}
	}


	/**
	 * Close the progress bar
	 */
	public void close ( ) {

		// set the opened state accordingly
		opened = false;

		if ( progressBar.isDisposed() )
			return;

		Display disp = progressBar.getDisplay();
		if ( disp.isDisposed() )
			return;
		disp.asyncExec( new Runnable() {
			public void run ( ) {
				if ( currentShell.isDisposed() )
					return;
				currentShell.close();
			}
		} );
	}

	/**
	 * Set a maximum limit for the progress
	 * @param progressLimit
	 */
	public void setProgressLimit(int progressLimit) {
		progressBar.setProgressLimit(progressLimit);
	}

	public void removeProgressLimit() {
		progressBar.removeProgressLimit();
	}

	/**
	 * Set how much should the progress bar increase its
	 * value each operation step. Used with operations that
	 * have several steps to automatize the progress increase
	 * for each step by calling {@link #nextStep()}
	 * @param progressStep
	 */
	public void setProgressStep(double progressStep) {
		progressBar.setProgressStep(progressStep);
	}

	/**
	 * Increase the progress bar according to the
	 * {@link #progressStep} variable
	 */
	public void nextStep() {
		progressBar.nextStep();
	}

	/**
	 * Add a progress to the progress bar. The current progress
	 * is added to the last progress. If a double < 1 is passed
	 * we accumulate the progresses until we reach an integer, in order
	 * to set the progress bar progresses
	 * @param progress
	 */
	public void addProgress ( double progress ) {
		progressBar.addProgress(progress);
	}

	/**
	 * Set the progress of the progress bar
	 * @param percent
	 */
	public void setProgress ( double percent ) {
		progressBar.setProgress(percent);
	}

	/**
	 * Set the bar to 100%
	 */
	public void fillToMax() {
		progressBar.fillToMax();
	}

	/**
	 * Set the label of the progress bar
	 * @param text
	 */
	public void setLabel ( final String text ) {

		if ( progressBar.isDisposed() )
			return;

		// open if necessary
		if ( !isOpened() )
			open();

		Display disp = progressBar.getDisplay();

		if ( disp.isDisposed() )
			return;

		disp.asyncExec( new Runnable() {
			public void run ( ) {

				if ( progressBar.isDisposed() )
					return;

				if ( label != null )
					label.setText( text );
			}
		} );
	}

	/**
	 * Get if the progress bar is open or not
	 * @return
	 */
	public boolean isOpened() {
		return opened;
	}

	/**
	 * Check if the progress bar is filled at 100%
	 * @return
	 */
	public boolean isCompleted() {
		return progressBar.isCompleted();
	}

	@Override
	public void addProgressListener(ProgressListener listener) {
		progressBar.addProgressListener(listener);
	}

	@Override
	public void stop( String message ) {
		progressBar.stop( message );
	}
}
