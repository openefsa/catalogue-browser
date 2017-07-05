package data_collection;

public interface DCDownloadListener {
	
	public static enum DownloadStep {
		DOWNLOAD_CONFIG,
		IMPORT_DC,
		IMPORT_TABLE,
		DOWNLOAD_CATALOGUES
	};

	/**
	 * Called when the new dc step is started.
	 * @param step the current step of the download process
	 * @param currentStepPhases the number of phases of the current step
	 */
	public void nextStepStarted( DownloadStep step, int currentStepPhases );
	
	/**
	 * Called when a new phase inside a step is started.
	 */
	public void nextPhaseStarted();
}
