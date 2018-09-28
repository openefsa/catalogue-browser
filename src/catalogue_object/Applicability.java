package catalogue_object;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import catalogue_browser_dao.ParentTermDAO;

/**
 * Link terms with their parents in each hierarchy
 * @author avonva
 *
 */
public class Applicability {

	private static final Logger LOGGER = LogManager.getLogger(Applicability.class);
	
	private Term child;
	private Nameable parentTerm;
	private Hierarchy hierarchy;
	private int order;
	private boolean reportable;
	
	
	public Applicability( Term child, Nameable parentTerm, Hierarchy hierarchy, int order, boolean reportable ) {
		this.child = child;
		this.parentTerm = parentTerm;
		this.hierarchy = hierarchy;
		this.order = order;
		this.reportable = reportable;
	}
	
	public Term getChild() {
		return child;
	}
	public Nameable getParentTerm() {
		return parentTerm;
	}
	public Hierarchy getHierarchy() {
		return hierarchy;
	}
	public int getOrder() {
		return order;
	}
	public boolean isReportable() {
		return reportable;
	}
	public void setParentTerm(Nameable parentTerm) {
		this.parentTerm = parentTerm;
	}
	public void setReportable(boolean reportable) {
		this.reportable = reportable;
	}
	public void setOrder(int order) {
		this.order = order;
	}
	
	/**
	 * Is the hierarchy of the applicability the one passed as input?
	 * @param h
	 * @return
	 */
	public boolean relatedToHierarchy ( Hierarchy h ) {
		
		if ( h == null ) {
			try {
				throw new Exception( "Null argument for Applicability:hasHierarchy regarding " + this );
			} catch (Exception e) {
				e.printStackTrace();
				LOGGER.error("Null argument for Applicability:hasHierarchy regarding applicability=" + this, e);
			}
			return false;
		}
		
		return hierarchy.getId() == h.getId();
	}
	
	public void update() {
		ParentTermDAO dao = new ParentTermDAO(child.getCatalogue());
		dao.update(this);
	}
	
	@Override
	public boolean equals(Object obj) {
		
		Applicability appl = (Applicability) obj;
		
		// if same term and same hierarchy => we have the same applicability (only one parent is allowed)
		boolean equals = appl.getChild().getId() == child.getId() && 
				appl.getHierarchy().getId() == hierarchy.getId();
		
		return equals;
	}
	
	@Override
	public String toString() {
		return "APPLICABILITY : child=" + child + ";parent=" + parentTerm + ";hierarchy=" + hierarchy + 
				";order=" + order + ";reportable=" + reportable;
	}
	
}
