package already_described_terms;

import java.util.ArrayList;

/**
 * Model a picklist object (picklist table of catalogue database)
 * @author avonva
 *
 */
public class Picklist {

	private int id;  // this field will be known only when the picklist is inserted into the database
	private String code;
	private ArrayList<PicklistTerm> terms;
	
	public Picklist( String code, ArrayList<PicklistTerm> terms ) {
		this.code = code;
		this.terms = terms;
	}
	
	public Picklist( String code ) {
		this( code, null );
	}
	
	public Picklist( int id, String code ) {
		this.id = id;
		this.code = code;
	}
	
	public void setId(int id) {
		this.id = id;
	}
	
	public int getId() {
		return id;
	}
	public String getCode() {
		return code;
	}
	public ArrayList<PicklistTerm> getTerms() {
		return terms;
	}
	public void setTerms(ArrayList<PicklistTerm> terms) {
		this.terms = terms;
	}
	
	@Override
	public boolean equals(Object obj) {

		Picklist picklist = (Picklist) obj;
		
		return id == picklist.getId() || code == picklist.getCode();
	}
	
	@Override
	public String toString() {
		return "PICKLIST: id=" + id + ";code=" + code + ";terms=" + terms;
	}
}
