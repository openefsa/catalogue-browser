package ict_add_on;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import catalogue_generator.ThreadFinishedListener;
import progress_bar.FormProgressBar;
import utilities.GlobalUtil;

public class ICTDownloader extends Thread {

	private static final Logger LOGGER = LogManager.getLogger(ICTDownloader.class);

	private ThreadFinishedListener doneListener;

	private final String txtURL = "https://github.com/openefsa/Interpreting-and-Checking-Tool/releases/download/1.2.7/utils.zip";
	private FormProgressBar progressBar;

	/**
	 * Start the download of the file
	 */
	public void run() {

		int code = ThreadFinishedListener.OK;
		
		try {
			this.progressBar.open();
			
			URL url = new URL(txtURL);

			LOGGER.info("creating connection...");
			if (progressBar != null)
				progressBar.setLabel("Instantiating the connection...");
			
			URLConnection conexion = url.openConnection();
			conexion.connect();

			int lenghtOfFile = conexion.getContentLength();

			InputStream input = new BufferedInputStream(url.openStream());

			// File Name
			String source = txtURL;
			String fileName = source.substring(source.lastIndexOf('/') + 1, source.length());

			// Copy file
			String saveFile = GlobalUtil.MAIN_DIR + File.separator + fileName;

			OutputStream output = new FileOutputStream(saveFile);

			byte data[] = new byte[4096];
			int count;

			long total = 0;

			LOGGER.info("Downloading the ICT...");
			if (progressBar != null)
				progressBar.setLabel("Downloading the ICT add-on...");
			
			while ((count = input.read(data)) != -1) {
				total += count;
				output.write(data, 0, count);
				
				if(progressBar!=null)
					progressBar.setProgress((double) ((total * 100) / lenghtOfFile));
			}

			LOGGER.info("ICT downloaded correctly.");

			output.flush();
			output.close();
			input.close();

			
		} catch (Exception ex) {
			LOGGER.info("Error occured while downloading the ICT. " + ex);
			code = ThreadFinishedListener.ERROR;
		}
		
		if ( progressBar != null )
			progressBar.close();
		
		// finished
		if ( doneListener != null )
			doneListener.finished( this, code, null );
	}

	/**
	 * Set the progress bar for the process
	 * 
	 * @param progressBar
	 */
	public void setProgressBar(FormProgressBar progressBar) {
		this.progressBar = progressBar;
	}

	/**
	 * Set the listener which will be called when the thread finishes its work
	 * 
	 * @param doneListener
	 */
	public void setDoneListener(ThreadFinishedListener doneListener) {
		this.doneListener = doneListener;
	}
}