package dcf_log;

import java.util.Collection;

/**
 * Class modeling an xml node contained in the Dcf log document
 * and representing the result of an upload operation.
 * @author avonva
 *
 */
public class LogNode {

	private String name;
	private DcfResponse result;
	private Collection<String> opLogs;
	
	public LogNode( String name, DcfResponse result, Collection<String> opLogs ) {
		this.name = name;
		this.result = result;
		this.opLogs = opLogs;
	}
	
	public String getName() {
		return name;
	}
	
	public DcfResponse getResult() {
		return result;
	}
	
	public Collection<String> getOpLogs() {
		return opLogs;
	}
	
	/**
	 * Check if the log node result is: correct
	 * @return
	 */
	public boolean isOperationCorrect() {
		return result == DcfResponse.OK;
	}
	
	@Override
	public String toString() {
		return "LogNode: name=" + name + ";result=" + result + ";opLogs=" + opLogs;
	}
}
