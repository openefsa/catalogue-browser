package term;
import java.util.ArrayList;

import catalogue_object.Term;

@Deprecated
public class TermElem {

	public Term					term;
	public TermElem				parent;
	public ArrayList< TermElem >	children	= new ArrayList< TermElem >();

}
