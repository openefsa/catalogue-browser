package dcf_log;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Builder to create a single LogNode step by step.
 * @author avonva
 *
 */
public class LogNodeBuilder {

	private String name;
	private DcfResponse result;
	private Collection<String> opLogs;

	public LogNodeBuilder() {
		opLogs = new ArrayList<>();
	}

	public LogNodeBuilder setName(String name) {
		this.name = name;
		return this;
	}
	public LogNodeBuilder setResult(String result) {
		
	    try {
	    	this.result = DcfResponse.valueOf( result );
	    } catch (IllegalArgumentException e) {
	    	this.result = DcfResponse.ERR;
	    }

		return this;
	}
	public LogNodeBuilder setResult(DcfResponse result) {
		this.result = result;
		return this;
	}
	public LogNodeBuilder addOpLog( String opLog ) {
		opLogs.add( opLog );
		return this;
	}

	public LogNode build() {
		return new LogNode( name, result, opLogs );
	}
}
