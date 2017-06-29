package ui_main_menu;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;

import messages.Messages;

/**
 * View menu in the main menu
 * @author avonva
 *
 */
public class ViewMenu implements MainMenuItem {

	public static final int EXPAND_MI = 0;
	public static final int COLLAPSE_NODE_MI = 1;
	public static final int COLLAPSE_TREE_MI = 2;
	
	private MenuListener listener;
	
	private MainMenu mainMenu;
	private Shell shell;

	private MenuItem viewItem;            // the button which shows the view menu
	private MenuItem expandMI;            // expande node
	private MenuItem collapseMI;          // collapse node
	private MenuItem collapseTreeMI;      // collapse tree

	public ViewMenu( MainMenu mainMenu, Menu menu ) {
		this.mainMenu = mainMenu;
		this.shell = mainMenu.getShell();
		create ( menu );
	}

	/**
	 * Set the listener to the menu buttons
	 * @param listener
	 */
	public void setListener(MenuListener listener) {
		this.listener = listener;
	}
	
	/**
	 * Add the view menu
	 * @param menu
	 */
	public MenuItem create ( Menu menu ) {

		viewItem = new MenuItem( menu , SWT.CASCADE );
		viewItem.setText( Messages.getString( "BrowserMenu.ViewMenuName" ) );

		Menu editMenu = new Menu( menu );

		expandMI = addExpandMI ( editMenu );
		collapseMI = addCollapseSingleNodeMI ( editMenu );
		collapseTreeMI = addCollapseAllMI ( editMenu );

		viewItem.setMenu( editMenu );

		viewItem.setEnabled( false );

		return viewItem;
	}

	/**
	 * Add menu item which allows to expand a single node and its children
	 * @param menu
	 * @return 
	 */
	private MenuItem addExpandMI ( Menu menu ) {

		final MenuItem expandItem = new MenuItem( menu , SWT.NONE );
		expandItem.setText( Messages.getString("BrowserMenu.ExpandNodeCmd") );
		expandItem.addSelectionListener( new SelectionAdapter() {
			@Override
			public void widgetSelected ( SelectionEvent e ) {

				// call the button listener if it was set
				if ( listener != null )
					listener.buttonPressed( expandItem, EXPAND_MI, null );
			}
		} );

		return expandItem;
	}

	/**
	 * Menu item which allows collapsing a single node
	 * @param menu
	 * @return 
	 */
	private MenuItem addCollapseSingleNodeMI ( Menu menu ) {

		final MenuItem collapseSingleItem = new MenuItem( menu , SWT.NONE );
		collapseSingleItem.setText( Messages.getString("BrowserMenu.CollapseNodeCmd") );
		collapseSingleItem.addSelectionListener( new SelectionAdapter() {
			@Override
			public void widgetSelected ( SelectionEvent e ) {

				// call the button listener if it was set
				if ( listener != null )
					listener.buttonPressed( collapseSingleItem, 
							COLLAPSE_NODE_MI, null );
			}
		} );

		return collapseSingleItem;
	}

	/**
	 * add a menu item which allows collapsing the entire tree
	 * @param menu
	 */
	private MenuItem addCollapseAllMI ( Menu menu ) {

		final MenuItem collapseItem = new MenuItem( menu , SWT.NONE );
		collapseItem.setText( Messages.getString("BrowserMenu.CollapseTreeCmd") );

		collapseItem.addSelectionListener( new SelectionAdapter() {
			@Override
			public void widgetSelected ( SelectionEvent e ) {

				// call the button listener if it was set
				if ( listener != null )
					listener.buttonPressed( collapseItem, 
							COLLAPSE_TREE_MI, null );
			}
		} );

		return collapseItem;
	}

	/**
	 * Refresh the menu
	 */
	public void refresh() {
		
		if ( viewItem.isDisposed() )
			return;
		
		// enable the edit menu only if there is a catalogue open and it is not empty
		viewItem.setEnabled( mainMenu.getCatalogue() != null 
				&& !mainMenu.getCatalogue().isEmpty() );
	}
}
