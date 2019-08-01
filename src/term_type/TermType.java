package term_type;

import catalogue_object.TermAttribute;

/**
 * This class model the type of term object of the database (if present). The old state flag
 * The term type code represents the term attribute value related to a term type attribute
 * contained in a term
 * @author avonva
 *
 */
public class TermType {

	private int id;
	private String code, label;
	
	public TermType( int id, String code, String label ) {
		this.id = id;
		this.code = code.trim();  // remove spaces from the code (it is a single character)
		this.label = label;
	}
	
	public int getId() {
		return id;
	}
	public String getCode() {
		return code;
	}
	public String getLabel() {
		return label;
	}
	
	@Override
	public boolean equals(Object obj) {

		if (obj instanceof TermType ) {
			TermType tt = (TermType) obj;
			
			return code.equals( tt.getCode() );
		}
		else if ( obj instanceof TermAttribute ) {
			TermAttribute ta = (TermAttribute) obj;
			return code.equals(ta.getValue());
		}
		
		return super.equals(obj);
	}
	
	@Override
	public String toString() {
		return "TERM TYPE: id=" + id + ";code=" + code + ";label=" + label;
	}
}
