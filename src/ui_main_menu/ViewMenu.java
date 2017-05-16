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

		MenuItem expandItem = new MenuItem( menu , SWT.NONE );
		expandItem.setText( Messages.getString("BrowserMenu.ExpandNodeCmd") );
		expandItem.addSelectionListener( new SelectionAdapter() {
			@Override
			public void widgetSelected ( SelectionEvent e ) {

				// call the expand listener if it was set
				if ( mainMenu.expandNodeListener != null )
					mainMenu.expandNodeListener.handleEvent( new Event() );
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

		MenuItem collapseSingleItem = new MenuItem( menu , SWT.NONE );
		collapseSingleItem.setText( Messages.getString("BrowserMenu.CollapseNodeCmd") );
		collapseSingleItem.addSelectionListener( new SelectionAdapter() {
			@Override
			public void widgetSelected ( SelectionEvent e ) {

				// call the listener if it was set
				if ( mainMenu.collapseNodeListener != null )
					mainMenu.collapseNodeListener.handleEvent( new Event() );
			}
		} );

		return collapseSingleItem;
	}

	/**
	 * add a menu item which allows collapsing the entire tree
	 * @param menu
	 */
	private MenuItem addCollapseAllMI ( Menu menu ) {

		MenuItem collapseItem = new MenuItem( menu , SWT.NONE );
		collapseItem.setText( Messages.getString("BrowserMenu.CollapseTreeCmd") );

		collapseItem.addSelectionListener( new SelectionAdapter() {
			@Override
			public void widgetSelected ( SelectionEvent e ) {

				// call the listener if it was set
				if ( mainMenu.collapseTreeListener != null )
					mainMenu.collapseTreeListener.handleEvent( new Event() );
			}
		} );

		return collapseItem;
	}

	/**
	 * Refresh the menu
	 */
	public void refresh() {
		
		// enable the edit menu only if there is a catalogue open and it is not empty
		viewItem.setEnabled( mainMenu.getCatalogue() != null 
				&& !mainMenu.getCatalogue().isEmpty() );
	}
}
