package ui_describe;
import messages.Messages;
import org.eclipse.swt.graphics.Image;

import already_described_terms.DescribedTerm;
import already_described_terms.PicklistTerm;
import catalogue_browser_dao.TermDAO;
import catalogue_object.Catalogue;
import global_manager.GlobalManager;
import term.LabelProviderTerm;

/**
 * This class load all the visualization Label in GUI TableView to view the
 * result of search query.
 * 
 * @author thomm
 * 
 */
public class LabelProviderDescribedTerm extends LabelProviderTerm {


	public LabelProviderDescribedTerm(Catalogue catalogue) {
		super();
	}

	public Image getImage ( Object dt ) {
		
		DescribedTerm describedTerm = (DescribedTerm) dt;
		
		// get an instance of the global manager
		GlobalManager manager = GlobalManager.getInstance();
		
		// get the current catalogue
		Catalogue currentCat = manager.getCurrentCatalogue();
		
		TermDAO termDao = new TermDAO( currentCat );
		
		return super.getImage( termDao.getByCode( describedTerm.getBaseTermCode() ) );
	}

	public String getText ( Object dt ) {
		
		// use the picklist level if picklist
		if ( dt instanceof PicklistTerm ) {
			
			PicklistTerm picklistTerm = (PicklistTerm) dt;
			
			return picklistTerm.getIndentedLabel();
		}
		
		// get the term label otherwise
		if ( dt instanceof DescribedTerm ) {
			DescribedTerm describedTerm = (DescribedTerm) dt;
			return describedTerm.getLabel();
		}
		
		return Messages.getString("LabelProviderRecentlyTerm.NameNotAvailable"); //$NON-NLS-1$
	}
}
