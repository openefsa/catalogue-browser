package import_catalogue;

public class ImportException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2271711162522344885L;

	public ImportException(Exception e) {
		super(e);
	}
	
	private String data;
	private String code;
	public ImportException(String text) {
		super(text);
	}
	public ImportException(String text, String code) {
		super(text);
		this.code = code;
	}
	
	public void setData(String data) {
		this.data = data;
	}
	
	public String getData() {
		return data;
	}
	
	public String getCode() {
		return code;
	}
}
