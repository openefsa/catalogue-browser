package ui_progress_bar;

import java.util.ArrayList;
import java.util.Collection;
import java.util.ListIterator;

/**
 * List which plans the execution of several pieces of codes. Each
 * piece of code is contained in a {@link ProgressStep} object in the
 * {@link ProgressStep#execute()} method. It is possible to execute all
 * the progress steps in the order they were inserted by using the
 * {@link #start()} command. The advantage of doing so is that we can
 * use the {@link ProgressStepListener} in order to be notified when each
 * piece of code starts and finishes. This allows for example to create
 * in the caller class a {@link FormProgressBar} which is updated
 * each time the listener is invoked, without having to implement
 * the progress bar into the core process that executes the non-user
 * interface code (modularity improves!).
 * @author avonva
 *
 */
public class ProgressList extends ArrayList<ProgressStep> {
	private static final long serialVersionUID = 1074566667820675565L;

	private int maxProgress;
	private long time;

	private Collection<ProgressStepListener> listeners;
	
	public ProgressList( int maxProgress ) {
		this.maxProgress = maxProgress;
		this.listeners = new ArrayList<>();
	}
	

	/**
	 * Get a progress node by its code
	 * @param code
	 * @return ProgressStep
	 */
	public ProgressStep get ( String code ) {

		for ( ListIterator<ProgressStep> i = this.listIterator(); i.hasNext(); ) {
			ProgressStep step = i.next();
			if ( step.getCode().equals( code ) )
				return step;
		}
		return null;
	}

	/**
	 * Start the execution of all the {@link ProgressStep} in the list.
	 */
	public void start() {
		
		// for each step (using iterator to support concurrent modification)
		for ( ListIterator<ProgressStep> i = this.listIterator(); i.hasNext(); ) {
			
			ProgressStep step = i.next();
			
			// execute progress step code
			try {
				System.out.println( "Starting " + step.getCode() );
				
				for ( ProgressStepListener listener : listeners )
					listener.progressStepStarted( step );
				
				step.start();
			} catch (Exception e) {
				e.printStackTrace();
				
				for ( ProgressStepListener listener : listeners )
					listener.failed( step );
				
				break;
			}
			
			// accumulate time
			time = time + step.getTime();
			
			double singleStepProgress = maxProgress / this.size();
			
			// notify that the progress changed
			for ( ProgressStepListener listener : listeners )
				listener.progressChanged( step, singleStepProgress, maxProgress );
		}
	}
	
	/**
	 * Add a progress listener
	 * @param listener
	 */
	public void addProgressListener ( ProgressStepListener listener ) {
		listeners.add( listener );
	}
	
	/**
	 * Get how long was the process in milliseconds
	 * Note that this quantity is defined only after
	 * calling {@link #start()}.
	 * @return
	 */
	public long getTime() {
		return time;
	}
}
