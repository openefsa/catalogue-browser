package global_manager;

import java.util.Observable;

import org.eclipse.swt.widgets.Display;

import catalogue.Catalogue;
import dcf_user.User;

/**
 * Class used to store all the global variables of the
 * application and to access them in a safe way.
 * This class is a singleton!
 * @author avonva
 *
 */
public class GlobalManager extends Observable {

	private static GlobalManager manager;
	
	// the currently opened catalogue
	private static Catalogue currentCatalogue;
	
	//block instantiation
	protected GlobalManager() {}
	
	/**
	 * Get an instance of the global manager
	 * @return
	 */
	public static GlobalManager getInstance() {
		
		// if no instance is already created
		// create it
		if ( manager == null )
			manager = new GlobalManager();
		
		return manager;
	}
	
	/**
	 * Set the current catalogue
	 * @param currentCatalogue
	 */
	public void setCurrentCatalogue( final Catalogue currentCatalogue ) {
		GlobalManager.currentCatalogue = currentCatalogue;
		
		// guarantee that we are running in the 
		// display thread
		Display.getDefault().syncExec( new Runnable() {
			
			@Override
			public void run() {
				setChanged();
				notifyObservers( currentCatalogue );
			}
		});

	}
	
	/**
	 * Get the catalogue which is currently opened
	 * in the application
	 * @return
	 */
	public Catalogue getCurrentCatalogue() {
		return currentCatalogue;
	}
	
	/**
	 * Check if the application is in read
	 * only mode
	 * @return
	 */
	public boolean isReadOnly() {
		
		User user = User.getInstance();
		
		return currentCatalogue != null && !user.canEdit( currentCatalogue );
	}
}
