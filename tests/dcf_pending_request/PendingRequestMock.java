package dcf_pending_request;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.soap.SOAPException;

import config.Environment;
import dcf_log.DcfLog;
import dcf_log.DcfResponse;
import dcf_log.IDcfLogParser;
import pending_request.IPendingRequest;
import pending_request.PendingRequestListener;
import pending_request.PendingRequestPriority;
import pending_request.PendingRequestStatus;
import user.IDcfUser;

public class PendingRequestMock implements IPendingRequest {

	private String logCode;
	private String type;
	private Map<String, String> data;
	private DcfResponse response;
	private PendingRequestStatus status;
	
	public PendingRequestMock(String logCode, String type, DcfResponse response) {
		this.data = new HashMap<>();
		this.logCode = logCode;
		this.type = type;
		this.response = response;
	}
	
	@Override
	public DcfResponse start(IDcfLogParser parser) throws SOAPException, IOException {
		return null;
	}

	public void setResponse(DcfResponse response) {
		this.response = response;
	}
	
	@Override
	public DcfResponse getResponse() {
		return response;
	}

	@Override
	public Environment getEnvironmentUsed() {
		return Environment.TEST;
	}

	@Override
	public PendingRequestStatus getStatus() {
		return status;
	}
	
	public void setStatus(PendingRequestStatus status) {
		this.status = status;
	}

	@Override
	public IDcfUser getRequestor() {
		return null;
	}

	@Override
	public Map<String, String> getData() {
		return data;
	}

	@Override
	public String getLogCode() {
		return logCode;
	}

	@Override
	public DcfLog getLog() {
		return null;
	}

	@Override
	public String getType() {
		return type;
	}

	@Override
	public PendingRequestPriority getPriority() {
		return PendingRequestPriority.HIGH;
	}

	@Override
	public void setEnvironmentUsed(Environment env) {}

	@Override
	public void setRequestor(IDcfUser user) {}

	@Override
	public void setLogCode(String logCode) {
		this.logCode = logCode;
	}

	@Override
	public void setData(Map<String, String> data) {
		this.data = data;
	}

	@Override
	public void setType(String type) {
		this.type = type;
	}

	@Override
	public void setPriority(PendingRequestPriority priority) {}

	@Override
	public void addPendingRequestListener(PendingRequestListener listener) {}

	@Override
	public void restart() {}

	@Override
	public long getRestartTime() {
		return -1;
	}

	@Override
	public boolean isPaused() {
		return false;
	}
}
