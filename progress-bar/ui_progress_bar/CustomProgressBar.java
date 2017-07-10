package ui_progress_bar;


import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ProgressBar;

public class CustomProgressBar implements IProgressBar {

	private Composite parent;
	private ProgressBar progressBar;
	private Collection<ProgressListener> listeners;
	
	private int done = 0;
	private double doneFract = 0;  // used to manage fractional progresses
	private int progressLimit = 100;  // set this to limit the bar progress
	private double progressStep = 1;  // progress gained by a single operation step
	
	public CustomProgressBar( Composite parent, int style ) {
		this.parent = parent;
		this.listeners = new ArrayList<>();
		initializeGraphics( style );
	}
	
	/**
	 * Creates all the graphics for the progress bar
	 * @param parentShell
	 */
	public void initializeGraphics ( int style ) {

		// progress bar
		progressBar = new ProgressBar( parent, style );
		progressBar.setMaximum( 100 );

		GridData gridData = new GridData();
		gridData.verticalAlignment = SWT.CENTER;
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		progressBar.setLayoutData( gridData );
	}
	
	/**
	 * Get the inner progress bar
	 */
	public ProgressBar getProgressBar() {
		return progressBar;
	}
	
	/**
	 * Add a progress to the progress bar. The current progress
	 * is added to the last progress. If a double < 1 is passed
	 * we accumulate the progresses until we reach an integer, in order
	 * to set the progress bar progresses
	 * @param progress
	 */
	public void addProgress ( double progress ) {

		// add to the done fract the double progress
		doneFract = doneFract + progress;

		// when we reach the 1 with the done progress
		// we can refresh the progress bar adding
		// the integer part of the doneFract
		if ( doneFract >= 1 ) {
			
			setProgress ( done + (int) doneFract );

			// reset the doneFract double
			doneFract = 0;
		}
	}
	
	/**
	 * Set the progress of the progress bar
	 * @param percent
	 */
	public void setProgress ( double percent ) {

		if ( percent >= 100 ) {
			done = 100;
		}
		else if ( percent < 0 ) {
			done = 0;
		}
		else {
			done = (int) percent;
		}
		
		// limit progress if required
		if ( done > progressLimit ) {
			done = progressLimit;
		}

		for ( ProgressListener listener : listeners )
			listener.progressChanged( done, progressLimit, this );
		
		refreshProgressBar( done );
	}
	
	/**
	 * Refresh the progress bar state
	 */
	public void refreshProgressBar ( final int done ) {

		if ( progressBar.isDisposed() )
			return;

		Display disp = progressBar.getDisplay();

		if ( disp.isDisposed() )
			return;

		disp.asyncExec( new Runnable() {
			public void run ( ) {

				if ( progressBar.isDisposed() )
					return;

				progressBar.setSelection( done );
				progressBar.update();
			}
		} );

		try {
			// set a small value! Otherwise the bar will slow all the Tool if often called
			Thread.sleep(11);  
		}
		catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Set a maximum limit for the progress
	 * @param progressLimit
	 */
	public void setProgressLimit(int progressLimit) {
		this.progressLimit = progressLimit;
	}
	
	/**
	 * Remove the limit of progress if it was set
	 * with {@link #setProgressLimit(int)}
	 */
	public void removeProgressLimit() {
		this.progressLimit = 100;
	}
	

	/**
	 * Check if the progress bar is filled at 100%
	 * @return
	 */
	public boolean isCompleted() {
		return done >= progressLimit;
	}
	
	/**
	 * Set how much should the progress bar increase its
	 * value each operation step. Used with operations that
	 * have several steps to automatize the progress increase
	 * for each step by calling {@link #nextStep()}
	 * @param progressStep
	 */
	public void setProgressStep(double progressStep) {
		this.progressStep = progressStep;
	}
	
	/**
	 * Increase the progress bar according to the
	 * {@link #progressStep} variable
	 */
	public void nextStep() {
		addProgress( progressStep );
	}
	
	public boolean isDisposed() {
		return progressBar.isDisposed();
	}
	
	public Display getDisplay() {
		return progressBar.getDisplay();
	}

	@Override
	public void setLabel(String label) {}

	@Override
	public void close() {}

	@Override
	public void open() {}

	@Override
	public void addProgressListener(ProgressListener listener) {
		listeners.add( listener );
	}

	@Override
	public void stop( String message ) {
		for ( ProgressListener listener : listeners ) {
			listener.progressStopped( message, this );
		}
	}

	@Override
	public void fillToMax() {
		setProgress( progressLimit );
	}
}
