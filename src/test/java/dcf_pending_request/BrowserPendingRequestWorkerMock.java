package dcf_pending_request;

import java.util.ArrayList;
import java.util.Collection;

import pending_request.PendingRequestStatusChangedEvent;
import ui_main_panel.IBrowserPendingRequestWorker;
import ui_main_panel.IBrowserPendingRequestWorker.PendingRequestWorkerListener.WorkerStatus;

public class BrowserPendingRequestWorkerMock implements IBrowserPendingRequestWorker {

	private Collection<PendingRequestWorkerListener> listeners;
	
	public BrowserPendingRequestWorkerMock() {
		listeners = new ArrayList<>();
	}
	
	@Override
	public WorkerStatus getStatus() {
		return null;
	}

	@Override
	public void statusChanged(PendingRequestStatusChangedEvent event) {
		for (PendingRequestWorkerListener listener : listeners)
			listener.statusChanged(event);
	}

	@Override
	public void addListener(PendingRequestWorkerListener listener) {
		listeners.add(listener);
	}

	@Override
	public void addActionListener(PendingRequestActionsListener event) {
		
	}
}
