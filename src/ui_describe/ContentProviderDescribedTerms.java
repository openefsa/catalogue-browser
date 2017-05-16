package ui_describe;
import java.util.ArrayList;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

import already_described_terms.DescribedTerm;

public class ContentProviderDescribedTerms implements IStructuredContentProvider {

	public void dispose ( ) {
	}

	public void inputChanged ( Viewer arg0 , Object arg1 , Object arg2 ) {
		// System.out.println("Input changed: old=" + arg1 + ", new=" + arg2);
	}

	public Object[] getElements ( Object fullCodes ) {
		
		@SuppressWarnings("unchecked")
		ArrayList< DescribedTerm > l = (ArrayList< DescribedTerm >) fullCodes;

		return l.toArray();
	}
}
