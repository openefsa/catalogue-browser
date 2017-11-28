package catalogue_generator;

public interface ThreadFinishedListener {

	public static final int OK = 0;
	public static final int ERROR = 1;
	public static final int EXCEPTION = 2;
	
	/**
	 * Called if the thread finished its {@link Thread#run()}
	 * method.
	 * @param thread the thread which finished the work
	 * @param code result code (correct is {@value #OK},
	 * general error is {@value #ERROR})
	 * otherwise
	 */
	public void finished( Thread thread, int code, Exception e );
}
