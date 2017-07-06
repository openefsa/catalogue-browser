package ui_progress_bar;

/**
 * Class used to model a single progress step of a process
 * which is subdivided in several steps. A step contains the 
 * code that needs to be executed in the {@link #execute()}
 * method.
 * @author avonva
 *
 */
public abstract class ProgressStep {
	
	private String code;
	private String name;
	private Object data;
	
	private long time;
	
	/**
	 * Create a progress step
	 * @param code the unique code that identifies the
	 * progress step in the list of steps
	 * @param name the step name (usually showed in the progress bar)
	 */
	public ProgressStep( String code, String name ) {
		this.code = code;
		this.name = name;
	}
	
	public ProgressStep( String code ) {
		this ( code, null );
	}
	
	/**
	 * Set a data object which the progress step
	 * carries on
	 * @param data
	 */
	public void setData ( Object data ) {
		this.data = data;
	}
	
	/**
	 * Get the data of the progress step if
	 * set. Otherwise null.
	 * @return
	 */
	public Object getData() {
		return data;
	};
	
	/**
	 * Start the execution of the progress step
	 * @throws Exception
	 */
	public void start() throws Exception {
		
		long start = System.currentTimeMillis();
		
		execute();
		
		time = System.currentTimeMillis() - start;
	}
	
	/**
	 * Get how long was the single step
	 * Note that this is defined only after
	 * calling {@link #start()}
	 * @return
	 */
	public long getTime() {
		return time;
	}
	
	/**
	 * Get the progress step name
	 * @return
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Get the progress step identifier
	 * @return
	 */
	public String getCode() {
		return code;
	}

	/**
	 * Code which is executed when {@link #start()} is
	 * called.
	 * @return
	 */
	public abstract void execute() throws Exception;
}
