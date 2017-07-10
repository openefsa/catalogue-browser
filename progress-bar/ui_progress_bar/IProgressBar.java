package ui_progress_bar;

import org.eclipse.swt.widgets.ProgressBar;

public interface IProgressBar {
	public ProgressBar getProgressBar();
	public void addProgress( double progress );
	public void setLabel ( String label );
	public void stop( String message );
	public void close();
	public void open();
	public void addProgressListener( ProgressListener listener );
	public void fillToMax();
}
