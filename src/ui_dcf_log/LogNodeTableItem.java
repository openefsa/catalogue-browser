package ui_dcf_log;

import dcf_log.DcfResponse;

public class LogNodeTableItem {

	private String name;
	private DcfResponse result;
	private String opLog;
	
	public LogNodeTableItem( String name, DcfResponse result, String opLog ) {
		this.name = name;
		this.result = result;
		this.opLog = opLog;
	}
	
	public String getName() {
		return name;
	}
	
	public DcfResponse getResult() {
		return result;
	}
	
	public String getOpLog() {
		return opLog;
	}
}
