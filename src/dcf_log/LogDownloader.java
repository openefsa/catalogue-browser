package dcf_log;

import java.io.File;

import javax.xml.soap.SOAPException;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import dcf_manager.Dcf;
import dcf_pending_action.PendingAction.Priority;

/**
 * Class used to download a dcf Log document
 * starting from a Log code (retrieved when the request is made).
 * This class tries to download the log several times, according
 * to the {@link PendingPriority } and to the other parameters
 * which define the timing behaviour of the downloads.
 * @author avonva
 *
 */
public class LogDownloader {

	private static final Logger LOGGER = LogManager.getLogger(LogDownloader.class);
	
	private String logCode;            // the code of the log we want to download
	private long interAttemptsTime;    // waiting time between attempts
	private int maxAttempts;           // max number of attempts (only HIGH priority)
	private Priority priority;         // the priority of the download process
	
	/**
	 * Initialize the download of the reserve log
	 * with low priority. The process goes on until
	 * we find a log.
	 * @param interAttemptsTime waiting time between
	 * attempts.
	 */
	public LogDownloader( String logCode, long interAttemptsTime ) {
		this( logCode, interAttemptsTime, -1, Priority.LOW );
	}
	
	/**
	 * Initialize the download of the reserve log with
	 * high priority. The process either finds the log
	 * within the allowed number of {@code maxAttempts}
	 * or downgrades to slow priority to continue
	 * searching the log.
	 * @param interAttemptsTime waiting time between
	 * attempts.
	 * @param maxAttempts max number of attempts
	 */
	public LogDownloader( String logCode, long interAttemptsTime, int maxAttempts ) {
		this( logCode, interAttemptsTime, maxAttempts, Priority.HIGH );
	}
	
	/**
	 * General constructor.
	 * @param interAttemptsTime waiting time between
	 * attempts.
	 * @param maxAttempts max number of attempts, ignored if priority is {@link PendingPriority#LOW}
	 * @param priority the download priority
	 */
	public LogDownloader( String logCode, long interAttemptsTime, 
			int maxAttempts, Priority priority ) {
		
		this.logCode = logCode;
		this.interAttemptsTime = interAttemptsTime;
		this.maxAttempts = maxAttempts;
		this.priority = priority;
	}
	
	/**
	 * Download the log from the dcf. If we are using {@link PendingPriority#LOW}
	 * as priority, the process goes on until the log is found.
	 * If we are using {@link PendingPriority#HIGH} instead, the
	 * process goes on until the maximum number of attempts is reached.
	 * @return the log if it was found within the maximum number of attempts
	 * otherwise null.
	 * @throws SOAPException 
	 */
	public File getLog() throws SOAPException {
		
		File log = null;
		
		// number of tried attempts
		int attemptsCount = 1;
		
		// if no log was found and we have low priority or 
		// high priority with another allowed attempt go on
		while ( log == null && ( priority == Priority.LOW || 
				attemptsCount < maxAttempts ) ) {

			// ask for the log to the dcf
			Dcf dcf = new Dcf();
			log = dcf.exportLog( logCode );

			// if log found break, otherwise retry
			if ( log != null ) {
				break;
			}

			String diagnostic = ( "Log " + logCode + " not found, retrying after " 
					+ (interAttemptsTime/1000) + " seconds the attempt n° " 
					+ (attemptsCount+1) );
			
			// add maximum number of attempts if high priority
			// otherwise add only the new line to the message
			if ( priority == Priority.HIGH )
				diagnostic += "/" + maxAttempts;
			
			LOGGER.info(diagnostic);
			
			// wait inter attempts time
			try {
				Thread.sleep( interAttemptsTime );
			} catch (InterruptedException e) {
				e.printStackTrace();
				LOGGER.error("Cannot sleep thread=" + this, e);
			}
			
			// go to the next attempt
			attemptsCount++;
		}
		
		return log;
	}

}
