package data_collection;

import java.io.IOException;

import javax.xml.soap.SOAPException;
import javax.xml.stream.XMLStreamException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

import progress_bar.FormProgressBar;
import progress_bar.ProgressStep;
import progress_bar.ProgressStepListener;
import soap.DetailedSOAPException;
import utilities.GlobalUtil;

/**
 * Class used to download a {@link DataCollection} in background.
 * 
 * @author avonva
 * @author shahaal
 */
public class DCDownloader extends Thread {

	private static final Logger LOGGER = LogManager.getLogger(DCDownloader.class);

	private Listener doneListener;
	private FormProgressBar progressBar;
	private DataCollection dc;

	/**
	 * Initialize a data collection downloader
	 * 
	 * @param dc the data collection we want to download
	 */
	public DCDownloader(DataCollection dc) {
		this.dc = dc;
	}

	/**
	 * Set a progress bar for the process
	 * 
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

					if (step == null)
						return;

					if (progressBar != null && step.getName() != null)
						progressBar.setLabel(step.getName());
				}

				@Override
				public void progressChanged(ProgressStep step, double addProgress, int maxProgress) {

					if (progressBar != null)
						progressBar.addProgress(addProgress);
				}

				@Override
				public void failed(ProgressStep step) {
				}
			};

			// download the data collection
			dc.download(listener);

			if (progressBar != null)
				progressBar.close();

			if (listener != null)
				doneListener.handleEvent(null);

		} catch (SOAPException e) {

			// show the error message for openapi users
			if (e instanceof DetailedSOAPException) {

				String[] warning = GlobalUtil.getSOAPWarning((DetailedSOAPException) e);
				Display display = new Display();
				GlobalUtil.showErrorDialog(new Shell(display, SWT.ON_TOP), warning[0], warning[1]);
			}

			LOGGER.error("Cannot download data collection=" + dc);
			e.printStackTrace();
			
		} catch (IOException | XMLStreamException e) {
			LOGGER.error("IO exception or error during the XML parsing for data collection=" + dc);
			e.printStackTrace();
		}
	}

	/**
	 * Set the listener which is called when the thread finishes its work
	 * 
	 * @param doneListener
	 */
	public void setDoneListener(Listener doneListener) {
		this.doneListener = doneListener;
	}
}
