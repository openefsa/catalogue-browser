package data_collection;

import javax.xml.soap.SOAPException;

import org.eclipse.swt.widgets.Listener;

import messages.Messages;
import ui_progress_bar.FormProgressBar;

public class DCDownloader extends Thread {

	private Listener doneListener;
	private FormProgressBar progressBar;
	private DataCollection dc;

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
			
			DCDownloadListener listener = new DCDownloadListener() {
				
				@Override
				public void nextStepStarted(DownloadStep step, int currentStepPhases) {

					String label = step.toString();
					switch ( step ) {
					case DOWNLOAD_CONFIG:
						label = Messages.getString( "DCDownload.DownStep" );
						break;
					case IMPORT_DC:
						label = Messages.getString( "DCDownload.ImportDCStep" );
						break;
					case IMPORT_TABLE:
						label = Messages.getString( "DCDownload.ImportTablesStep" );
						break;
					default:
						break;
					}
					
					if ( progressBar != null ) {
						progressBar.setLabel( label );
						
						int stepCount = 3;
						
						// limit the progress for each step based on the number
						// of required phases
						progressBar.setProgressStep( 100 / (stepCount * currentStepPhases) );
					}
				}
				
				@Override
				public void nextPhaseStarted() {
					progressBar.nextStep();
				}
			};
			
			// download the data collection
			dc.download( listener );
			
			if ( progressBar != null )
				progressBar.close();
			
			if ( listener != null )
				doneListener.handleEvent( null );

		} catch (SOAPException e) {
			e.printStackTrace();
		}
	}
	
	public void setDoneListener(Listener doneListener) {
		this.doneListener = doneListener;
	}
}
