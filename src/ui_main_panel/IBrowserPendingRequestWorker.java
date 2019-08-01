package ui_main_panel;

import dcf_pending_request.PendingRequestActionsListener;
import pending_request.PendingRequestListener;
import pending_request.PendingRequestStatusChangedEvent;
import ui_main_panel.IBrowserPendingRequestWorker.PendingRequestWorkerListener.WorkerStatus;

public interface IBrowserPendingRequestWorker {
	
	public interface PendingRequestWorkerListener extends PendingRequestListener {
		public enum WorkerStatus {
			WAITING,
			ONGOING
		}

		public void workerStatusChanged(WorkerStatus newStatus);
	}
	
	public WorkerStatus getStatus();
	public void statusChanged(PendingRequestStatusChangedEvent event);
	public void addListener(PendingRequestWorkerListener listener);
	public void addActionListener(PendingRequestActionsListener event);
}
