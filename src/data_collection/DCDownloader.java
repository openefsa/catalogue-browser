package data_collection;

import javax.xml.soap.SOAPException;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.swt.widgets.Listener;

import progress_bar.FormProgressBar;
import progress_bar.ProgressStep;
import progress_bar.ProgressStepListener;

/**
 * Class used to download a {@link DataCollection} in background.
 * @author avonva
 *
 */
public class DCDownloader extends Thread {

	private static final Logger LOGGER = LogManager.getLogger(DCDownloader.class);
	
	private Listener doneListener;
	private FormProgressBar progressBar;
	private DataCollection dc;

	/**
	 * Initialize a data collection downloader
	 * @param dc the data collection we want to download
	 */
	public DCDownloader( DataCollection dc ) {
		this.dc = dc;
	}
	
	/**
	 * Set a progress bar for the process
	 * @param progressBar
	 */
	public void setProgressBar(FormProgressBar progressBar) {
		this.progressBar = progressBar;
	}
	
	@Override
	public void run() {

		try {
			
			// Create the progress listener for the download
			// process
			ProgressStepListener listener = new ProgressStepListener() {
				
				@Override
				public void progressStepStarted(ProgressStep step) {
					
					if ( step == null )
						return;

					if ( progressBar != null && step.getName() != null )
						progressBar.setLabel( step.getName() );
				}
				
				@Override
				public void progressChanged(ProgressStep step, double addProgress, int maxProgress) {

					if ( progressBar != null )
						progressBar.addProgress( addProgress );
				}
				
				@Override
				public void failed(ProgressStep step) {}
			};

			// download the data collection
			dc.download( listener );
			
			if ( progressBar != null )
				progressBar.close();
			
			if ( listener != null )
				doneListener.handleEvent( null );

		} catch (SOAPException e) {
			e.printStackTrace();
			LOGGER.error("Cannot download data collection=" + dc);
		}
	}
	
	/**
	 * Set the listener which is called when the thread
	 * finishes its work
	 * @param doneListener
	 */
	public void setDoneListener(Listener doneListener) {
		this.doneListener = doneListener;
	}
}
