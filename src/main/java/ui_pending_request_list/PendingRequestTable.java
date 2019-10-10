package ui_pending_request_list;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

import i18n_messages.CBMessages;
import pending_request.IPendingRequest;
import pending_request.PendingRequestStatus;
import ui_pending_request_list.RequestLabelProvider.RequestLabelColumn;

public class PendingRequestTable extends Composite {
	
	private TableViewer table;
	private Collection<PendingRequestTableRelaunchListener> listeners;
	
	public PendingRequestTable(Composite parent, int style) {
		super(parent, style);
		this.listeners = new ArrayList<>();
		createContents();
	}
	
	public void addRelaunchListener(PendingRequestTableRelaunchListener listener) {
		listeners.add(listener);
	}
	
	private void createContents() {
		
		this.setLayout(new FillLayout());
		
		this.table = new TableViewer(this, SWT.BORDER | SWT.FULL_SELECTION
				| SWT.V_SCROLL | SWT.H_SCROLL | SWT.NONE);
		
		table.setContentProvider(new RequestContentProvider());
		
		table.getTable().setHeaderVisible(true);

		TableViewerColumn catCodeCol = new TableViewerColumn(table, SWT.NONE);
		catCodeCol.getColumn().setWidth(140);
		catCodeCol.getColumn().setText(CBMessages.getString("request.table.catalogue.header"));
		catCodeCol.setLabelProvider(new RequestLabelProvider(RequestLabelColumn.CATALOGUE));
		
		TableViewerColumn typeCol = new TableViewerColumn(table, SWT.NONE);
		typeCol.getColumn().setWidth(140);
		typeCol.getColumn().setText(CBMessages.getString("request.table.type.header"));
		typeCol.setLabelProvider(new RequestLabelProvider(RequestLabelColumn.TYPE));
		
		TableViewerColumn statusCol = new TableViewerColumn(table, SWT.NONE);
		statusCol.getColumn().setWidth(120);
		statusCol.getColumn().setText(CBMessages.getString("request.table.status.header"));
		statusCol.setLabelProvider(new RequestLabelProvider(RequestLabelColumn.STATUS));
		
		TableViewerColumn responseCol = new TableViewerColumn(table, SWT.NONE);
		responseCol.getColumn().setWidth(95);
		responseCol.getColumn().setText(CBMessages.getString("request.table.response.header"));
		responseCol.setLabelProvider(new RequestLabelProvider(RequestLabelColumn.RESPONSE));
		
		TableViewerColumn relaunchTimeCol = new TableViewerColumn(table, SWT.NONE);
		relaunchTimeCol.getColumn().setWidth(120);
		relaunchTimeCol.getColumn().setText(CBMessages.getString("request.table.relaunch.time.header"));
		relaunchTimeCol.setLabelProvider(new RequestLabelProvider(RequestLabelColumn.RESTART_TIME));
		
		TableViewerColumn logCodeCol = new TableViewerColumn(table, SWT.NONE);
		logCodeCol.getColumn().setWidth(110);
		logCodeCol.getColumn().setText(CBMessages.getString("request.table.log.code.header"));
		logCodeCol.setLabelProvider(new RequestLabelProvider(RequestLabelColumn.LOG_CODE));
		
		addMenu();
	}
	
	public void addMenu() {
		
		Menu menu = new Menu(this);
		
		MenuItem relaunch = new MenuItem(menu, SWT.PUSH);
		relaunch.setText(CBMessages.getString("request.table.launch.now"));
		relaunch.setEnabled(false);
		
		relaunch.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				
				// relaunch the request
				IPendingRequest req = (IPendingRequest) ((IStructuredSelection) table.getSelection())
						.getFirstElement();
				
				// relaunch immediately the request
				req.restart();
				
				table.refresh(req);
				
				// notify
				for(PendingRequestTableRelaunchListener listener: listeners) {
					listener.relaunched(req);
				}
			}
		});
		
		table.addSelectionChangedListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent arg0) {
				ISelection selection = arg0.getSelection();
				if (selection.isEmpty())
					return;
				
				IPendingRequest req = (IPendingRequest) ((IStructuredSelection) selection).getFirstElement();
				
				// relaunch only for queued requests
				relaunch.setEnabled(req.getStatus() == PendingRequestStatus.QUEUED);
			}
		});
		
		table.getTable().setMenu(menu);
	}
	
	public void refresh() {
		this.table.refresh();
	}
	
	public void refresh(IPendingRequest request) {
		this.table.refresh(request);
	}
	
	/**
	 * Add a new message into the console
	 * @param message
	 */
	public void add(IPendingRequest... messages) {
		
		table.add(messages);
		
		// reveal the last
		table.reveal(messages[messages.length - 1]);
	}
}
