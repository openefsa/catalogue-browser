package session_manager;

import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;

import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Shell;

/**
 * Class to store the preferences related to the
 * windows dimensions. We can save the dimensions
 * of the resizable windows and restore them, in order
 * to help the user.
 * @author avonva
 *
 */
public class WindowPreference {

	private String code;
	private int x;
	private int y;
	private int width;
	private int height;
	private boolean maximized;
	
	/**
	 * Create a new window preference. Use this constructor
	 * to create a preference fetching the data from the 
	 * database.
	 * @param code the code which identify the window
	 * @param x the x-axis position
	 * @param y the y-axis position
	 * @param width the width of the window
	 * @param height the height of the window
	 */
	public WindowPreference( String code, int x, int y, 
			int width, int height, boolean maximized ) {
		
		this.code = code;
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
		this.maximized = maximized;
	}
	
	/**
	 * Create a window preference using the current shell
	 * dimensions. Use this constructor to save the window
	 * dimensions (use then the {@link WindowPreferenceDAO}
	 * to save them permanently).
	 * @param window the window we want to save
	 */
	public WindowPreference ( RestoreableWindow window ) {
		save ( window );
	}

	/**
	 * Get the code of the window preference
	 * @return
	 */
	public String getCode() {
		return code;
	}
	
	public int getX() {
		return x;
	}
	
	public int getY() {
		return y;
	}
	
	public int getWidth() {
		return width;
	}
	
	public int getHeight() {
		return height;
	}
	
	public boolean isMaximized() {
		return maximized;
	}
	
	/**
	 * Save the shell parameters
	 * @param shell
	 */
	private void save ( RestoreableWindow window ) {
		
		Shell shell = window.getWindowShell();
		
		this.code = window.getWindowCode();
		x = shell.getLocation().x;
		y = shell.getLocation().y;
		width = shell.getSize().x;
		height = shell.getSize().y;
		maximized = shell.getMaximized();

		// if we are using a single screen
		// adjust the variables using the
		// screen bounds
		if ( isSingleScreen() )
			adjustToSingleScreen();
	}
	
	/**
	 * Detect if there is a single screen or if
	 * two or more screens are used together
	 * @return
	 */
	private boolean isSingleScreen() {
		
		// get all the screens
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice[] gs = ge.getScreenDevices();

		return gs.length == 1;
	}
	
	/**
	 * Adjust the coordinates based on the
	 * main screen dimensions, in order to
	 * prevent the window to appear outside
	 * of the screen.
	 */
	private void adjustToSingleScreen() {
		
		// set the maximum limit of the screen
		if ( x < 0 )
			x = 0;
		
		if ( y < 0 )
			y = 0;

		// get the screen dimensions in pixels
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		if ( y + height > dim.getHeight() )
			y = (int) (dim.getHeight() - height);
		
		if ( x + width > dim.getWidth() )
			x = (int) (dim.getWidth() - width);
	}
	
	/**
	 * Restore the dimensions of the window passed in input
	 * using this window preference settings (the shell is specified
	 * in {@link RestoreableWindow#getWindowShell()}). The settings
	 * are retrieved from the database using the {@link RestoreableWindow#getWindowCode()}
	 * as primary key. In particular,
	 * we reset the x,y,width and height of the window
	 * to the last used settings. Moreover, we also set
	 * the window as maximized if it was maximized in
	 * the last session.
	 * @param window the window we want to restore
	 */
	public static void restore ( RestoreableWindow window ) {
		
		// get the preference related to the window passed
		// as input
		WindowPreferenceDAO windDao = new WindowPreferenceDAO();
		
		WindowPreference pref = windDao.getByCode( window.getWindowCode() );

		if ( pref == null ) {
			System.out.println ( "No window preference found related to code " + window.getWindowCode() );
			return;
		}
		
		// if we are using a single screen
		// adjust the variables using the
		// screen bounds
		if ( pref.isSingleScreen() )
			pref.adjustToSingleScreen();
		
		// get the parameters and restore them
		Shell shell = window.getWindowShell();
		int x = pref.getX();
		int y = pref.getY();
		int width = pref.getWidth();
		int height = pref.getHeight();
		boolean max = pref.isMaximized();
		
		shell.setLocation( x, y );
		shell.setSize( width, height );
		shell.setMaximized( max );
	}
	
	/**
	 * Save the dimensions parameters of the shell when it will be
	 * closed. The parameters are stored in the database and will be
	 * usable calling {@link WindowPreference#restore(RestoreableWindow)}.
	 * @param shell
	 */
	public static void saveOnClosure ( final RestoreableWindow window ) {
		
		Shell shell = window.getWindowShell();
		
		// when the shell is closed
		shell.addDisposeListener( new DisposeListener() {
			
			@Override
			public void widgetDisposed(DisposeEvent arg0) {

				// create a preference with the code and the shell dimensions
				WindowPreference pref = new WindowPreference( window );
				
				// insert (or update) the window preference
				WindowPreferenceDAO windDao = new WindowPreferenceDAO();
				windDao.remove( pref );
				windDao.insert( pref );
			}
		});
	}
	
	@Override
	public String toString() {
		return "WINDOW PREFERENCE : x=" + x + ",y=" + y + 
				",width=" + width + ",height=" + height + 
				",maximized=" + maximized;
	}
}
