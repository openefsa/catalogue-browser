package ui_general_graphics;

import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

/**
 * This class is used to make the last column on the right
 * of the table responsive of the window resizing. In fact,
 * now the column is resized according to the size of the table.
 * @author avonva
 *
 */
public class TableResizer {

	private Composite parent;
	private Table table;
	private TableColumn column;
	
	private int initialSize;
	private int lastSize;
	
	/**
	 * Initialize the resizer
	 * @param parent the parent in which 
	 * @param table
	 */
	public TableResizer( Table table ) {
		this.parent = table.getParent();
		this.table = table;
		this.column = table.getColumn( table.getColumnCount() - 1 );
	}
	
	/**
	 * Get the current width of the table
	 * @return
	 */
	public int getTableWidth () {
		Rectangle area = parent.getClientArea();
		ScrollBar vBar = table.getVerticalBar();
		int width = area.width - table.computeTrim(0,0,0,0).width - vBar.getSize().x;
		return width;
	}
	
	/**
	 * Resize the column if the table is resized
	 */
	public void apply() {
		
		// save the initial size
		initialSize = column.getWidth();
		lastSize = getTableWidth();
		
		table.addControlListener( new ControlListener() {
			
			@Override
			public void controlResized(ControlEvent arg0) {

				// get the new size difference
				int newSize = column.getWidth() + getTableWidth() - lastSize;

				// do nothing if column width is to minimum
				if ( newSize < initialSize ) {
					return;
				}
				
				// save the new size
				lastSize = getTableWidth();
				
				// add the difference to the column width
				column.setWidth( newSize );
			}
			
			@Override
			public void controlMoved(ControlEvent arg0) {}
		});
	}
}
