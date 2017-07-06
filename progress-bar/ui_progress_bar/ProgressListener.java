package ui_progress_bar;

public interface ProgressListener {
	
	/**
	 * Called when a progress step was started
	 * @param step
	 */
	public void progressStepStarted( ProgressStep step );
	
	/**
	 * Called to notify the caller that the progress
	 * of the process was changed.
	 * @param step the step which was just finished
	 * @param addProgress progress added by this step
	 * @param maxProgress the maximum amount of progress achievable
	 */
	public void progressChanged( ProgressStep step, double addProgress, 
			int maxProgress );
	
	/**
	 * Called if during the process an error occurred
	 * @param step the step where the problem raised
	 */
	public void failed( ProgressStep step );
}
