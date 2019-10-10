package ooxml;

import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

import progress_bar.FormProgressBar;

public class ExcelThread extends Thread {

	private String filename;
	private FormProgressBar progressForm;
	private Listener errorListener, doneListener;
	
	/**
	 * Called when errors occur
	 * @param errorListener
	 */
	public void addErrorListener ( Listener errorListener ) {
		this.errorListener = errorListener;
	}
	
	/**
	 * Called when all the operations are finished
	 * @param doneListener
	 */
	public void addDoneListener ( Listener doneListener ) {
		this.doneListener = doneListener;
	}
	
	public ExcelThread( String threadName, String filename ) {
		super ( threadName );
		this.filename = filename;
	}
	
	public ExcelThread( String filename ) {
		this ( "excel", filename );
	}
	
	/**
	 * Call the error listener if it was set
	 */
	public void handleError () {
		if ( errorListener != null ) {
			errorListener.handleEvent( new Event() );
		}
	}
	
	/**
	 * Call the done listener if it was set
	 * Pass as data the xlsx filename
	 */
	public void handleDone() {
		if ( doneListener != null ) {
			
			Event event = new Event();
			event.data = filename;
			doneListener.handleEvent( event );
		}
	}
	
	public String getFilename() {
		return filename;
	}
	public FormProgressBar getProgressForm() {
		return progressForm;
	}
	
	/**
	 * Set the progress bar for the thread
	 * @param progressForm
	 */
	public void setProgressBar( FormProgressBar progressForm ) {
		this.progressForm = progressForm;
	}
}
