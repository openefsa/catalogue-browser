package ui_dcf_log;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

import dcf_log.DcfLog;
import dcf_log.LogNode;
import i18n_messages.CBMessages;
import utilities.GlobalUtil;

/**
 * Table viewer which shows the {@link DcfLog} operations
 * (modelled by {@link LogNode}) which did not succeed.
 * @author avonva
 *
 */
public class LogNodesTableViewer {

	private Composite parent;
	private TableViewer table;
	
	public LogNodesTableViewer( Composite parent, DcfLog log ) {
		this.parent = parent;
		display( log );
	}
	
	public void display(DcfLog log) {
		
		table = new TableViewer(parent);
		table.setContentProvider( new LogNodesContentProvider());
		
		GlobalUtil.addStandardColumn( table, 
				new LogNodeLabelProvider( LogNodeLabelProvider.NAME ), 
				CBMessages.getString( "LogNodesTable.Name" ), 
				100, true, false, SWT.CENTER );
		
		GlobalUtil.addStandardColumn( table, 
				new LogNodeLabelProvider( LogNodeLabelProvider.RESULT ), 
				CBMessages.getString( "LogNodesTable.Result" ), 
				100, true, false, SWT.CENTER );
		
		GlobalUtil.addStandardColumn( table, 
				new LogNodeLabelProvider( LogNodeLabelProvider.OP_LOG ), 
				CBMessages.getString( "LogNodesTable.OpLog" ), 
				600, true, false, SWT.LEFT );
		
		table.getTable().setHeaderVisible( true );
		
		table.setInput(log);
		
		table.getTable().setLayout( new GridLayout( 1, false ) );
		table.getTable().setLayoutData( new GridData( SWT.FILL, SWT.FILL, true, true ) );
	}
	
	private class LogNodesContentProvider implements IStructuredContentProvider {

		@Override
		public void dispose() {}

		@Override
		public void inputChanged(Viewer arg0, Object arg1, Object arg2) {}

		@Override
		public Object[] getElements(Object arg0) {

			if ( arg0 instanceof DcfLog ) {
				
				ArrayList<LogNodeTableItem> objects = new ArrayList<>();
				
				DcfLog log = (DcfLog) arg0;

				// create the table items using the erroneous
				// log nodes only
				for (String operationLog: log.getMacroOpLogs()) {
					objects.add(new LogNodeTableItem(log.getMacroOpName(), 
							log.getMacroOpResult(), operationLog));
				}
				
				// create the table items using the erroneous
				// log nodes only
				for (LogNode node: log.getLogNodes()) {
					objects.addAll(toItems(node));
				}
				
				for (LogNode node: log.getValidationErrors()) {
					objects.addAll(toItems(node));
				}
				
				return objects.toArray();
			}
			
			return null;
		}
	}
	
	private Collection<LogNodeTableItem> toItems(LogNode node) {
		
		Collection<LogNodeTableItem> objects = new ArrayList<>();
		
		// for each operation log of a single node, 
		// create a new item showing name, result and op log
		for (String operationLog: node.getOpLogs()) {
			
			LogNodeTableItem item = new LogNodeTableItem (node.getName(),
					node.getResult(), operationLog);
			
			objects.add(item);
		}
		
		return objects;
	}
	
	/**
	 * Label provider for the catalogue code column
	 * @author avonva
	 *
	 */
	private class LogNodeLabelProvider extends ColumnLabelProvider {

		public static final String NAME = "name";
		public static final String RESULT = "result";
		public static final String OP_LOG = "log";
		
		private String key;
		
		/**
		 * Initialize the label provider with the key
		 * which identifies the field we want to show
		 * in the column
		 * @param key
		 */
		public LogNodeLabelProvider( String key ) {
			this.key = key;
		}
		
		@Override
		public void addListener(ILabelProviderListener arg0) {}

		@Override
		public void dispose() {}

		@Override
		public boolean isLabelProperty(Object arg0, String arg1) {
			return false;
		}

		@Override
		public void removeListener(ILabelProviderListener arg0) {}

		@Override
		public Image getImage(Object arg0) {
			return null;
		}

		@Override
		public String getText(Object arg0) {

			String text = null;
			
			LogNodeTableItem item = (LogNodeTableItem) arg0;
			
			switch ( key ) {
			case NAME:
				text = item.getName();
				break;
			case RESULT:
				text = item.getResult().toString();
				break;
			case OP_LOG:
				text = item.getOpLog();
				break;
			}
			
			return text;
		}
	}
}
