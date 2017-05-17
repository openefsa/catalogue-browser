package dcf_reserve_util;

import org.w3c.dom.Document;

import dcf_log_util.BusyDcfListener;
import dcf_manager.Dcf;
import dcf_webservice.DcfResponse;
import dcf_webservice.Reserve;

/**
 * Class used to download a log from the dcf.
 * @author avonva
 *
 */
public class ReserveLogDownloader {
	
	private String logCode;
	private long retryTime;
	
	private BusyDcfListener listener;

	/**
	 * Set the basic information for the thread
	 * @param logCode the code of the log we want to retrieve
	 * @param retryTime wait time between each attempt
	 */
	public ReserveLogDownloader( String logCode, long retryTime ) {

		this.logCode = logCode;
		this.retryTime = retryTime;
	}

	/**
	 * Set the listener which is called when we ask for the log
	 * to the dcf, but the dcf is busy.
	 * @param listener
	 */
	public void setListener(BusyDcfListener listener) {
		this.listener = listener;
	}
	
	/**
	 * Download the log identified by the {@link ReserveLogDownloader#logCode}
	 * variable. Note that this method is a blocking method, we are stuck
	 * here until we find the log.
	 * @return the log response contained in the log
	 */
	public DcfResponse download() {
		
		Document log = null;
		
		// try to get the log
		Reserve reserve = new Reserve();
//int count = 0;
		// do until we found the log
		while ( log == null ) {
			
			Dcf dcf = new Dcf();
			
			// try to get the log
			log = dcf.exportLog( logCode );

			// if no log was found => wait retry time
			// and then restart
			// TODO remove true
			//if ( log == null || count++ == 0 ) {
			if ( log == null ) {
				
				System.err.println ( "The Log " + logCode + " is missing. Waiting " 
						+ ( retryTime / 1000 )
						+ " seconds to retry." );
				
				// notify that the dcf is still busy
				if ( listener != null )
					listener.dcfIsBusy( logCode );
				
				// wait
				try {
					Thread.sleep( retryTime );
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		
		// here the log was found, therefore we can
		// analyze it
		DcfResponse response = reserve.getLogResponse( log );

		// return the log response
		return response;
	}
}
