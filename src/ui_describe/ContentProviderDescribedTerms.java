<<<<<<< HEAD
package ui_describe;
import java.util.ArrayList;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

import already_described_terms.DescribedTerm;

public class ContentProviderDescribedTerms implements IStructuredContentProvider {

	public void dispose ( ) {
	}

	public void inputChanged ( Viewer arg0 , Object arg1 , Object arg2 ) {}

	public Object[] getElements ( Object fullCodes ) {
		
		ArrayList< DescribedTerm > l = (ArrayList< DescribedTerm >) fullCodes;

		return l.toArray();
	}
}
=======
package ui_describe;
import java.util.ArrayList;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

import already_described_terms.DescribedTerm;

public class ContentProviderDescribedTerms implements IStructuredContentProvider {

	public void dispose ( ) {
	}

	public void inputChanged ( Viewer arg0 , Object arg1 , Object arg2 ) {}

	public Object[] getElements ( Object fullCodes ) {
		
		ArrayList< DescribedTerm > l = (ArrayList< DescribedTerm >) fullCodes;

		return l.toArray();
	}
}
>>>>>>> 574ffe363e78d250cf6350ff4ea89f2f48352380
