package dcf_log_util;

import java.util.ArrayList;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;

import dcf_webservice.DcfResponse;
import utilities.GlobalUtil;

public class LogNodesTableViewer {

	private Composite parent;
	private TableViewer table;
	
	public LogNodesTableViewer( Composite parent, DcfLog log ) {
		this.parent = parent;
		display( log );
	}
	
	public void display( DcfLog log ) {
		
		table = new TableViewer( parent );
		table.setContentProvider( new LogNodesContentProvider() );
		
		GlobalUtil.addStandardColumn( table, 
				new LogNodeLabelProvider( LogNodeLabelProvider.NAME ), 
				"Name", 100, true, false, SWT.CENTER );
		
		GlobalUtil.addStandardColumn( table, 
				new LogNodeLabelProvider( LogNodeLabelProvider.RESULT ), 
				"Operation result", 100, true, false, SWT.CENTER );
		
		GlobalUtil.addStandardColumn( table, 
				new LogNodeLabelProvider( LogNodeLabelProvider.OP_LOG ), 
				"Operation log", 600, true, false, SWT.LEFT );
		
		table.getTable().setHeaderVisible( true );
		
		table.setInput( log );
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
				
				// create the table items
				for ( LogNode node : log.getLogNodes() ) {
					
					// skip correct nodes
					if ( node.getResult() == DcfResponse.OK )
						continue;
					
					for ( String operationLog : node.getOpLogs() ) {
						
						LogNodeTableItem item = new LogNodeTableItem ( node.getName(),
								node.getResult(), operationLog );
						
						objects.add( item );
					}
				}
				
				return objects.toArray();
			}
			
			return null;
		}
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
