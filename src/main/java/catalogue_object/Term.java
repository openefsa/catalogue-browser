package catalogue_object;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Queue;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Display;

import catalogue.Catalogue;
import catalogue_browser_dao.ParentTermDAO;
import catalogue_browser_dao.TermDAO;
import data_transformation.BooleanConverter;
import data_transformation.DateTrimmer;
import data_transformation.ValuesGrouper;
import global_manager.GlobalManager;
import i18n_messages.CBMessages;
import naming_convention.SpecialValues;
import term.TermSubtreeIterator;
import ui_implicit_facet.ComparatorAlphaFacetDescriptor;
import ui_implicit_facet.ComparatorFacetDescriptor;
import ui_implicit_facet.DescriptorTreeItem;
import ui_implicit_facet.FacetDescriptor;
import ui_implicit_facet.FacetType;

/**
 * Term of a catalogue
 * 
 * @author shahaal
 * @author avonva
 */
public class Term extends CatalogueObject implements Mappable {

	private static final Logger LOGGER = LogManager.getLogger(Term.class);

	// Term attributes caches

	// corex flag, how much is detailed the term?
	private TermAttribute detailLevel;

	// state flag, which is the term type?
	private TermAttribute termType;

	// the implicit facets of the term
	private ArrayList<FacetDescriptor> implicitFacets;

	// list of attributes and their values related to the term
	private ArrayList<TermAttribute> termAttributes;

	// the applicabilities of the term in its hierarchies
	private ArrayList<Applicability> applicabilities;

	/**
	 * Constructor, initialize array lists
	 */
	public Term(Catalogue catalogue) {

		super(catalogue);
		implicitFacets = new ArrayList<>();
		termAttributes = new ArrayList<>();
		applicabilities = new ArrayList<>();
	}

	/**
	 * Create a term, the name is the extended name and the label is the short name
	 * 
	 * @param id
	 * @param code
	 * @param name
	 * @param label
	 * @param scopenotes
	 * @param status
	 * @param version
	 * @param lastUpdate
	 * @param validFrom
	 * @param validTo
	 * @param deprecated
	 */
	public Term(Catalogue catalogue, int id, String code, String name, String label, String scopenotes, String status,
			String version, Timestamp lastUpdate, Timestamp validFrom, Timestamp validTo, boolean deprecated) {

		super(catalogue, id, code, name, label, scopenotes, version, lastUpdate, validFrom, validTo, status,
				deprecated);

		implicitFacets = new ArrayList<>();
		termAttributes = new ArrayList<>();
		applicabilities = new ArrayList<>();
	}

	public void clear() {
		implicitFacets.clear();
		termAttributes.clear();
		applicabilities.clear();
	}

	public String getFullCode(boolean allFacets, boolean baseTerm) {
		return getFullCode(allFacets, baseTerm, new ComparatorFacetDescriptor());
	}

	/**
	 * Get the full code of a term (base code plus all the term facets) If
	 * includeImplicit = true, the code will contain also the implicit facets codes
	 * If baseTerm = true, the code will contain also the base term code
	 * 
	 * @param t
	 * @param includeInherited
	 * @param baseTerm         include the base term or not
	 * @return
	 */
	public String getFullCode(boolean allFacets, boolean baseTerm, Comparator<FacetDescriptor> sorter) {

		String descriptorCodes = "";
		ArrayList<FacetDescriptor> descriptors = getFacets(allFacets, sorter);

		// add the descriptor full codes into the term full code
		for (FacetDescriptor descriptor : descriptors) {
			descriptorCodes = descriptorCodes + descriptor.getFullFacetCode() + "$";
		}

		String result = "";

		// Add the base term code if required
		if (baseTerm) {
			result = getCode();

			// add the hash if facet will be added
			if (descriptorCodes.length() > 0)
				result = result + "#";
		}

		// I add the # separator if it is the case to be added (I have some
		// facets)
		if (descriptorCodes.length() > 0)
			result = result + descriptorCodes.substring(0, descriptorCodes.length() - 1);
		
		LOGGER.info("full code of term : " + result);
		return result;
	}

	/**
	 * Get the facet descriptors related to this term
	 * 
	 * @param allFacets true to get all the facets (also the inherited), false to
	 *                  get only the facets related to the term (implicit facets).
	 * @return
	 */
	public ArrayList<FacetDescriptor> getFacets(boolean allFacets) {
		return getFacets(allFacets, new ComparatorFacetDescriptor());
	}

	/**
	 * Get the facet descriptors related to this term
	 * 
	 * @param allFacets true to get all the facets (also the inherited), false to
	 *                  get only the facets related to the term (implicit facets).
	 * @return
	 */
	public ArrayList<FacetDescriptor> getFacets(boolean allFacets, Comparator<FacetDescriptor> sorter) {

		ArrayList<FacetDescriptor> descriptors = new ArrayList<>();

		if (allFacets) {

			ArrayList<Attribute> categories = catalogue.getFacetCategories();

			// for each facet category we analyze the implicit facets and the explicit
			// facets
			ListIterator<Attribute> iter = categories.listIterator();

			// for each category
			while (iter.hasNext()) {

				Attribute facetCategory = iter.next();

				for (DescriptorTreeItem item : this.getInheritedImplicitFacets(facetCategory)) {
					descriptors.add(item.getDescriptor());
				}
			}

		} else {
			descriptors.addAll(implicitFacets);
		}

		Collections.sort(descriptors, sorter);

		return descriptors;
	}

	/**
	 * Get all the implicit facets related to a term for a specific facet category
	 * 
	 * @param term
	 * @param facetCategory
	 * @return
	 */
	private ArrayList<DescriptorTreeItem> getImplicitFacetsTree(Attribute facetCategory) {

		ArrayList<DescriptorTreeItem> inTree = new ArrayList<>();

		// start the recursive method
		return getImplicitFacetsTree(facetCategory, inTree, false);
	}

	/**
	 * Get all the implicit facets related to the term for a single facet category
	 * recursive method which uses the tree structure!
	 * 
	 * @param facet, the facet category we considered in this step
	 * @param term,  the term we selected to see its implicit facets
	 * @return
	 */
	private ArrayList<DescriptorTreeItem> getImplicitFacetsTree(Attribute facetCategory,
			ArrayList<DescriptorTreeItem> inTree, boolean processingParents) {

		// For each facet descriptor
		for (FacetDescriptor descriptor : this.getDescriptorsByCategory(facetCategory, true)) {

			// get the term related to the descriptor code
			Term descriptorTerm = catalogue.getTermByCode(descriptor.getFacetCode());

			// we instantiate a node of the tree with the descriptor term
			// if we are processing parents then we set the tree item as inherited = true
			DescriptorTreeItem parent = new DescriptorTreeItem(descriptorTerm, descriptor, processingParents);

			// check the relationships with the descriptors which were already added in
			// previous call of this recursive method. In particular, we check if the
			// node just created have some children in the tree. If so we set the
			// relationship
			Iterator<DescriptorTreeItem> iterator = inTree.iterator();
			while (iterator.hasNext()) {

				// get the current node of the tree
				DescriptorTreeItem child = iterator.next();

				// has the descriptor contained in the tree as ancestor our new node?
				if (child.getTerm().hasAncestor(parent.getTerm(), catalogue.getMasterHierarchy())) {

					// if so, then set the parent child relationship
					child.setParent(parent);
					parent.addChild(child);

					// remove the child, which is not required since we display only the parent
					// element
					iterator.remove();
				}
			}

			// add the new node to the tree
			inTree.add(parent);
		}

		// then get the parent of the term in order to add also its implicit facets
		// to the child term
		Term parentTerm = this.getParent(catalogue.getMasterHierarchy());

		// if a parent is found => go on with the recursion
		if (parentTerm != null)
			inTree = parentTerm.getImplicitFacetsTree(facetCategory, inTree, true);

		return inTree;
	}

	/**
	 * Given a tree of implicit facets, we retrieve all the leaf nodes. We use a
	 * queue to make a visit of the tree level by level (not deeper first, we use
	 * breath first)
	 * 
	 * @param inTree
	 * @return
	 */
	private ArrayList<DescriptorTreeItem> getImplicitFacetsLeaves(ArrayList<DescriptorTreeItem> inTree) {

		Queue<DescriptorTreeItem> descriptors = new LinkedList<>();
		ArrayList<DescriptorTreeItem> leaves = new ArrayList<>();

		// prepare the queue of descriptors
		descriptors.addAll(inTree);

		// do until we have processed all nodes of the tree
		while (!descriptors.isEmpty()) {

			// get the head of the queue and remove it from the queue
			DescriptorTreeItem item = descriptors.poll();

			// if we found a leaf, add it to the leaf list and go to the next descriptor
			if (item.isLeaf()) {
				leaves.add(item);
				continue; // go to the next descriptor
			}

			// if we did not find a leaf, get all the children and process them
			// (we add them to the queue, they will be processed further on
			for (DescriptorTreeItem child : item.getChildren()) {

				// add the child into the queue
				descriptors.add(child);
			}
		}

		return leaves;
	}

	/**
	 * Get the inherited implicit facets related to a single facet category. For
	 * example, if we want to have all the implicit facets related to the Process
	 * facet, we pass as parameter the Attribute which identifies the process facet.
	 * 
	 * @param facetCategory the facet category we want to consider
	 * @return {@code ArrayList<DescriptorTreeItem>}, the implicit facets tree (we
	 *         can have some parent-child relationships since we can specify more
	 *         facets using their children)
	 */
	public ArrayList<DescriptorTreeItem> getInheritedImplicitFacets(Attribute facetCategory) {
		return this.getImplicitFacetsLeaves(getImplicitFacetsTree(facetCategory));
	}

	/**
	 * Get the term short name of the term
	 * 
	 * @param replace set this to true if you want that if the short name is not
	 *                present to get the extended name instead (same as calling
	 *                {@link #getName()})
	 * @return
	 */
	public String getShortName(boolean replace) {

		if (replace && getLabel().equals(""))
			return getName();

		LOGGER.info("short name of the term : " + getLabel());
		return getLabel();
	}

	/**
	 * Get the detail level of the term if present, otherwise null
	 * 
	 * @return
	 */
	public TermAttribute getDetailLevel() {
		return detailLevel;
	}

	/**
	 * Get the term type if present, otherwise null
	 * 
	 * @return
	 */
	public TermAttribute getTermType() {
		return termType;
	}

	/**
	 * Get the term order in a particular heirarchy
	 * 
	 * @param hierarchy
	 */
	public int getOrder(Hierarchy hierarchy) {

		Applicability appl = getApplicability(hierarchy);

		if (appl != null)
			return appl.getOrder();

		return -1;
	}

	/**
	 * Get the term implicit facets (not inherited, only the ones related to this
	 * term)
	 * 
	 * @return
	 */
	public ArrayList<FacetDescriptor> getImplicitFacets() {
		return implicitFacets;
	}

	/**
	 * Get the attributes
	 * 
	 * @return
	 */
	public ArrayList<TermAttribute> getAttributes() {
		return termAttributes;
	}

	/**
	 * Get a term attribute value by its attribute name note that repeatable
	 * attributes are compacted with a $
	 * 
	 * @param name
	 * @return
	 */
	public String getAttributeValueByName(String name) {

		// values grouper to group several values $ separated
		ValuesGrouper values = new ValuesGrouper();

		for (TermAttribute ta : termAttributes) {
			if (ta.getAttribute().getName().equals(name))
				values.addValue(ta.getValue());
		}

		return values.getCompactValues();
	}

	/**
	 * Get all the term applicabilities
	 * 
	 * @return
	 */
	public ArrayList<Applicability> getApplicabilities() {
		return applicabilities;
	}

	/**
	 * Get all the applicabilities which are in use for this catalogue. No filter is
	 * applied if user is catalogue manager
	 * 
	 * @return
	 */
	public Collection<Applicability> getInUseApplicabilities() {

		Collection<Applicability> inUse = new ArrayList<>();
		Collection<Hierarchy> notUsed = catalogue.getNotUsedHierarchies();

		for (Applicability appl : applicabilities) {
			if (!notUsed.contains(appl.getHierarchy()))
				inUse.add(appl);
		}

		return inUse;
	}

	/**
	 * Get the term applicability related to the selected hierarchy if no
	 * applicability is found => return null
	 * 
	 * @param hierarchy
	 * @return
	 */
	public Applicability getApplicability(Hierarchy hierarchy) {

		for (Applicability appl : applicabilities) {

			// if we have found the correct hierarchy return
			if (appl.relatedToHierarchy(hierarchy)) {
				return appl;
			}
		}

		return null;
	}

	/**
	 * Check if the term is used in at least one hierarchy
	 * 
	 * @return
	 */
	public boolean isInUse() {
		return !getInUseApplicabilities().isEmpty();
	}

	/**
	 * Convert the order integer code to a hierarchy code
	 * 
	 * @param hierarchy
	 * @return
	 */
	public String getSingleHierarchyCode(Hierarchy hierarchy) {

		// get the applicability of the term
		Applicability appl = getApplicability(hierarchy);

		if (appl == null)
			return null;

		// start from the order integer of the term
		String hierarchyCode = String.valueOf(appl.getOrder());

		// add zeros to format code
		while (hierarchyCode.length() < 4) {
			hierarchyCode = "0" + hierarchyCode;
		}

		LOGGER.info("single hierarchy code : " + hierarchyCode);
		return hierarchyCode;
	}

	/**
	 * Create the hierarchy code for this term considering the selected hierarchy. A
	 * hierarchy code is a Z0001.0001.0002.
	 * 
	 * @param hierarchy
	 * @return
	 */
	public String getHierarchyCode(Hierarchy hierarchy) {

		String hierarchyCode = this.getSingleHierarchyCode(hierarchy);

		// if no code found return void
		if (hierarchyCode == null)
			return "";

		// get the parent
		Term parent = this.getParent(hierarchy);

		// do until root
		while (parent != null) {

			// get the single hierarchy code of the parent
			String parentHierarchyCode = parent.getSingleHierarchyCode(hierarchy);

			// if no code found exit cycle
			if (parentHierarchyCode == null)
				break;

			// concatenate the parent hierarchy code with the rest
			// in a dot separated way
			hierarchyCode = parentHierarchyCode + "." + hierarchyCode;

			// get the next parent
			parent = parent.getParent(hierarchy);
		}

		// add the Z at the beginning (to avoid excel issues)
		hierarchyCode = "Z" + hierarchyCode;

		LOGGER.info("hierarchy code : " + hierarchyCode);
		return hierarchyCode;
	}

	@Override
	public String getValueByKey(String key) {

		String value = "";

		switch (key) {
		case "TERM_CODE":
			value = getCode();
			break;
		case "TERM_EXTENDED_NAME":
			value = getName();
			break;
		case "TERM_SHORT_NAME":
			value = getShortName(false);
			break;
		case "TERM_SCOPENOTE":
			value = getScopenotes();
			break;
		case "TERM_VERSION":
			value = getVersion();
			break;
		case "TERM_LAST_UPDATE":
			if (getLastUpdate() != null)
				value = DateTrimmer.dateToString(getLastUpdate());
			break;
		case "TERM_VALID_FROM":
			if (getValidFrom() != null)
				value = DateTrimmer.dateToString(getValidFrom());
			break;
		case "TERM_VALID_TO":
			if (getValidTo() != null)
				value = DateTrimmer.dateToString(getValidTo());
			break;
		case "TERM_STATUS":
			value = getStatus();
			break;
		case "TERM_DEPRECATED":
			value = BooleanConverter.toNumericBoolean(String.valueOf(isDeprecated()));
			break;
		default:
			break;
		}

		// if we have a parametrized attribute
		if (key.contains("attribute_")) {

			// get the attribute name (convention attribute_ + attribute name)
			String attrName = key.split("_", 2)[1];

			// if all facets, compute them
			if (attrName.equals(SpecialValues.ALL_FACETS_NAME)) {
				value = getFullCode(true, true, new ComparatorAlphaFacetDescriptor());
			} else if (attrName.equals(SpecialValues.IMPLICIT_FACETS_NAME)) {
				value = getFullCode(false, false, new ComparatorAlphaFacetDescriptor());
			} else {
				// get the term attribute value related to the attribute name
				// note that repeatable attributes are compacted $ separated
				value = this.getAttributeValueByName(attrName);
			}
		}

		// if we have a parametrized hierarchy flag
		if (key.contains("flag_"))
			value = getHierarchyProperty(key, ParentField.FLAG);

		// if we have a parametrized hierarchy parent code
		if (key.contains("parent_"))
			value = getHierarchyProperty(key, ParentField.PARENT_CODE);

		// if we have a parametrized hierarchy order
		if (key.contains("order_"))
			value = getHierarchyProperty(key, ParentField.ORDER);

		// if we have a parametrized hierarchy reportable
		if (key.contains("reportable_"))
			value = getHierarchyProperty(key, ParentField.REPORTABLE);

		// if we have a parametrized hierarchy code
		if (key.contains("hierarchyCode_"))
			value = getHierarchyProperty(key, ParentField.HIERARCHY_CODE);

		return value;
	}

	/**
	 * Get the parent field related to the current hierarchy retrieved from the key
	 * string.
	 * 
	 * @param key,   key which contains the hierarchy code
	 * @param field, field which is being analyzed
	 * @return
	 */
	private String getHierarchyProperty(String key, ParentField field) {

		String value = "";

		// get the hierarchy code from the key
		String hierarchyCode = key.split("_", 2)[1];

		Hierarchy hierarchy;

		// if the code is the master code we get the master hierarchy,
		// otherwise we get the hierarchy using its code
		if (hierarchyCode.equals(Hierarchy.MASTER_HIERARCHY_CODE))
			hierarchy = catalogue.getMasterHierarchy();
		else
			hierarchy = catalogue.getHierarchyByCode(hierarchyCode);

		// get the term applicability related to the found hierarchy
		Applicability appl = this.getApplicability(hierarchy);

		// return empty value if no applicability is retrieved
		if (appl == null)
			return "";

		switch (field) {

		// if hierarchy flag
		case FLAG:

			value = "1";
			break;

		// if hierarchy parent code
		case PARENT_CODE:

			// get the parent of the term
			Nameable parent = appl.getParentTerm();

			// if we have a parent which is a term => we get the term code as parent code
			if (parent instanceof Term)
				value = ((Term) parent).getCode();

			// if we have a hierarchy, we have to set as convention "root" as parent code
			// since there is not actually a term parent
			else if (parent instanceof Hierarchy)
				value = "root";
			break;

		// if hierarchy order
		case ORDER:

			// get the order from the applicability
			int order = appl.getOrder();

			// convert it to string
			value = String.valueOf(order);
			break;

		// if hierarchy reportable
		case REPORTABLE:

			// get if the term is reportable in the applicability
			boolean reportable = appl.isReportable();

			// convert the true/false to 1/0 and then convert to string
			value = BooleanConverter.toNumericBoolean(String.valueOf(reportable));
			break;
		case HIERARCHY_CODE:
			value = getHierarchyCode(hierarchy);
			break;
		}

		// return the retrieved value
		return value;
	}

	/**
	 * Set the label of the term to display in tree
	 * 
	 * @author shahaal
	 * @param displayAs
	 */
	public void setDisplayAs(String displayAs) {
		setLabel(displayAs);
	}

	/**
	 * Set the detail level of the term (both attribute and value)
	 * 
	 * @param value
	 */
	public void setDetailLevel(TermAttribute detailLevel) {
		this.detailLevel = detailLevel;

		if (!termAttributes.contains(detailLevel)) {
			termAttributes.add(detailLevel);
		}
	}

	/**
	 * Set the detail level of the term (both attribute and value)
	 * 
	 * @param value
	 */
	public void setTermType(TermAttribute termType) {

		this.termType = termType;

		if (!termAttributes.contains(termType)) {
			termAttributes.add(termType);
		}
	}

	/**
	 * Set the value of the detail level (not the attribute, only the value)
	 * 
	 * @param value
	 */
	public void setDetailLevelValue(String value) {

		if (detailLevel == null)
			return;

		this.detailLevel.setValue(value);
	}

	/**
	 * Set the value of the term type (not the attribute, only the value)
	 * 
	 * @param value
	 */
	public void setTermTypeValue(String value) {

		if (termType == null)
			return;

		this.termType.setValue(value);
	}

	/**
	 * Set the term order in a particular hierarchy
	 * 
	 * @param hierarchy
	 */
	public void setOrder(Hierarchy hierarchy, int order) {

		Applicability appl = getApplicability(hierarchy);

		if (appl != null) {
			appl.setOrder(order);
			appl.update();
		}
	}

	/**
	 * Set the reportability of the term in one hierarchy
	 * 
	 * @param hierarchy
	 * @param reportable
	 */
	public void setReportability(Hierarchy hierarchy, boolean reportable) {

		// check if the hierarchy is indeed into the applicabilities
		boolean found = false;

		for (Applicability appl : applicabilities) {

			// search the chosen hierarchy
			if (appl.relatedToHierarchy(hierarchy)) {

				// update the reportability of the term in the selected hierarchy
				appl.setReportable(reportable);

				found = true;
				break;
			}
		}

		if (!found) {
			LOGGER.error("The hierarchy " + hierarchy.getLabel() + " was not found in the applicabilities of the term "
					+ this);
		}
	}

	/**
	 * Get the truncated name of the term (up to the "last" character). If addDots =
	 * true, three dots are attached at the end of the term name if it was indeed
	 * truncated
	 * 
	 * @param last
	 * @return
	 */
	public String getTruncatedName(int last, boolean addDots) {

		int min = Math.min(getName().length(), last);

		String truncatedName = getName().substring(0, min);

		// if we want to add dots and the name was indeed truncated
		if (addDots && truncatedName.length() < getName().length())
			truncatedName = truncatedName + "...";

		return truncatedName;
	}

	/**
	 * Get the interpreted code of the term without implicit facets (only explicit
	 * are considered).
	 * 
	 * @return
	 */
	public String getInterpretedCode() {
		return getInterpretedCode(false);
	}

	/**
	 * Get the interpreted code of the term with explicit facets. Implicit facets
	 * are added if the copy implicit boolean is set to true (default false).
	 * 
	 * @return
	 */
	public String getInterpretedCode(boolean copyImplicit) {

		// the first element of the interpreted code is the base term name
		StringBuilder interpCode = new StringBuilder();

		interpCode.append(this.getName());

		// order the facets
		Collections.sort(implicitFacets, new ComparatorFacetDescriptor());

		// then we add all the implicit facets codes comma separated
		// FACET_HIERARCHY = FacetName, ...
		for (FacetDescriptor fd : implicitFacets) {

			// skip if we have an implicit facet and copy implicit is set to false
			if (!copyImplicit && fd.getFacetType() == FacetType.IMPLICIT)
				continue;

			interpCode.append(", ");
			interpCode.append(fd.getFacetCategory().getHierarchy().getLabel().toUpperCase());
			interpCode.append(" = ");
			interpCode.append(catalogue.getTermByCode(fd.getFacetCode()).getName());
		}

		return interpCode.toString();
	}

	/**
	 * Get the extended term name with implicit facets (also inherited)
	 * 
	 * @author shahaal
	 * @return
	 */
	public String getInterpretedExtendedName() {

		// the first element of the interpreted code is the base term name
		StringBuilder interpCode = new StringBuilder();

		interpCode.append(this.getName());

		ArrayList<FacetDescriptor> facets = getFacets(true);
		// order the facets
		Collections.sort(facets, new ComparatorFacetDescriptor());

		// then we add all the implicit facets codes comma separated
		// FACET_HIERARCHY = FacetName, ...
		for (FacetDescriptor fd : facets) {
			interpCode.append(", ");
			interpCode.append(fd.getFacetCategory().getHierarchy().getLabel().toUpperCase());
			interpCode.append(" = ");
			interpCode.append(catalogue.getTermByCode(fd.getFacetCode()).getName());
		}

		return interpCode.toString();
	}

	/**
	 * Get all the descriptors related to the facet category The dcfattribute is
	 * simply the category but it does not own the value, which is the term code
	 * (i.e. the code of the facet descriptor) Here we retrieve all the descriptors
	 * related to the category
	 * 
	 * @param facetCategory
	 * @return
	 */
	public ArrayList<FacetDescriptor> getDescriptorsByCategory(Attribute facetCategory, boolean implicit) {

		// output array
		ArrayList<FacetDescriptor> facets = new ArrayList<>();

		// for each facet descriptor we search for the descriptors of a single category
		for (FacetDescriptor descriptor : implicitFacets) {

			// continue if facet category is null
			if (descriptor.getFacetCategory() == null)
				continue;

			// skip if dont want implicit facets
			if (!implicit && descriptor.getFacetType() == FacetType.IMPLICIT)
				continue;

			// check if correct facet category and add it
			if (descriptor.getFacetCategory().getId() == facetCategory.getId())
				facets.add(descriptor);
		}

		return facets;
	}

	/**
	 * Add a term attribute to the term
	 * 
	 * @param ta
	 */
	public void addAttribute(TermAttribute ta) {

		// add to the attributes
		termAttributes.add(ta);

		// if we have corex flag set accordingly
		if (ta.getAttribute().isDetailLevel())
			setDetailLevel(ta);

		// if we have state flag set accordingly
		if (ta.getAttribute().isTermType())
			setTermType(ta);

		// if we have an implicit facet add it to the implicit facets list
		// as facet descriptor
		if (ta.getAttribute().isImplicitFacet()) {

			// create an implicit facet descriptor
			FacetDescriptor fa = new FacetDescriptor(this, ta, FacetType.IMPLICIT);
			implicitFacets.add(fa);
		}
	}

	/**
	 * Remove an attribute from the term
	 * 
	 * @param ta
	 */
	@SuppressWarnings("unlikely-arg-type")
	public void removeAttribute(TermAttribute ta) {

		termAttributes.remove(ta);

		// if it was an implicit facet remove it also from the cache
		if (ta.getAttribute().isImplicitFacet())
			implicitFacets.remove(ta);

		if (ta.getAttribute().isDetailLevel())
			detailLevel = null;

		if (ta.getAttribute().isTermType())
			termType = null;
	}

	/**
	 * Add directly an implicit facet to the list of facets (and thus to the
	 * attributes)
	 * 
	 * @param fd
	 */
	@SuppressWarnings("unlikely-arg-type")
	public void addImplicitFacet(FacetDescriptor fd) {

		// return if the implicit facet was already added
		if (termAttributes.contains(fd))
			return;

		termAttributes.add(fd.getTermAttribute());
		implicitFacets.add(fd);
	}

	/**
	 * Remove directly an implicit facet from the list of facets (and thus from the
	 * attributes)
	 * 
	 * @param fd
	 */
	public void removeImplicitFacet(FacetDescriptor fd) {

		// remove the term attribute
		termAttributes.remove(fd.getTermAttribute());

		// remove the descriptor
		implicitFacets.remove(fd);
	}

	/**
	 * Remove all the attributes
	 */
	public void clearAttributes() {

		if (termAttributes != null)
			termAttributes.clear();

		if (implicitFacets != null)
			implicitFacets.clear();

		detailLevel = null;
		termType = null;
	}

	/**
	 * Remove all the applicabilities
	 */
	public void clearApplicabilities() {
		applicabilities.clear();
	}

	/**
	 * Get all the attributes that are not catalogue attributes and that are not
	 * detail level, type of term and implicit facets
	 * 
	 * @return
	 */
	public ArrayList<TermAttribute> getGenericAttributes() {

		ArrayList<TermAttribute> nonCatAttrs = new ArrayList<>();

		for (TermAttribute attr : termAttributes)
			if (!attr.getAttribute().isFacetCategory() && !attr.getAttribute().isDetailLevel()
					&& !attr.getAttribute().isTermType() && !attr.getAttribute().isImplicitFacet()
					&& !attr.getAttribute().isAllFacet())
				nonCatAttrs.add(attr);

		return nonCatAttrs;
	}

	/**
	 * Remove a term attribute from the term (we use only the attribute equals,
	 * since we are in the term class)
	 * 
	 * @param ta
	 */
	@SuppressWarnings("unlikely-arg-type")
	public void removeTermAttribute(TermAttribute ta) {

		for (int i = 0; i < termAttributes.size(); i++) {

			if (ta.getAttribute().equals(termAttributes.get(i).getAttribute())) {

				TermAttribute removed = termAttributes.remove(i);

				// if we have an implicit facet then we remove also from the implicit facet
				// array
				if (removed.getAttribute().isImplicitFacet())
					implicitFacets.remove(removed);

				if (removed.getAttribute().isDetailLevel())
					detailLevel = null;

				if (removed.getAttribute().isTermType())
					termType = null;
			}
		}
	}

	/**
	 * Add an applicability to the term Return true if the element was correctly
	 * added, otherwise false Set permanent to true if you want to add the
	 * applicability also in the catalogue database
	 * 
	 * @param appl
	 * @param permanent, true = save the applicability into the catalogue database,
	 *                   default is false
	 * @return true if the applicability was added successfully
	 */
	public boolean addApplicability(Applicability appl, boolean permanent) {

		// if the applicability is not already present add it
		if (!applicabilities.contains(appl)) {

			applicabilities.add(appl);

			ParentTermDAO parentDao = new ParentTermDAO(catalogue);

			// add the new applicability permanently if required
			if (permanent)
				parentDao.insert(appl);

			return true;
		}

		LOGGER.warn("Applicability " + appl + " is already present");

		return false;
	}

	/**
	 * Add an applicability to this term (only RAM object is touched, the db is not
	 * updated)
	 * 
	 * @param appl
	 */
	public boolean addApplicability(Applicability appl) {
		return addApplicability(appl, false);
	}

	/**
	 * Remove an applicability from the term
	 * 
	 * @param appl
	 * @param permanent, should the db be updated?
	 */
	public void removeApplicability(Applicability appl, boolean permanent) {

		applicabilities.remove(appl);

		ParentTermDAO parentDao = new ParentTermDAO(catalogue);

		// remove permanently
		if (permanent)
			parentDao.remove(appl);
	}

	/**
	 * Remove an applicability from the term
	 * 
	 * @param appl
	 * @param permanent, should the db be updated?
	 */
	public void removeApplicability(Hierarchy hierarchy, boolean permanent) {

		Applicability appl = getApplicability(hierarchy);

		removeApplicability(appl, permanent);
	}

	/**
	 * Get the parent of the term
	 * 
	 * @param hierarchy
	 * @return
	 */
	public Term getParent(Hierarchy hierarchy) {

		for (Applicability appl : applicabilities) {

			// if we have found the correct hierarchy return
			if (appl.relatedToHierarchy(hierarchy)) {

				// return null if the parent is a hierarchy
				if (appl.getParentTerm() instanceof Hierarchy)
					return null;

				// get the parent and
				Term parent = (Term) appl.getParentTerm();

				// return it
				return parent;
			}
		}

		// null if no parent was found
		return null;
	}

	public void setParent(Hierarchy hierarchy, Nameable parent) {
		Applicability appl = getApplicability(hierarchy);
		appl.setParentTerm(parent);
		appl.update();
	}

	/**
	 * Check if the term has as parent another term or a hierarchy Return true if
	 * the parent is a term, false if the parent is a hierarchy
	 * 
	 * @param hierarchy
	 * @return
	 */
	public boolean hasParent(Hierarchy hierarchy) {
		return getParent(hierarchy) != null;
	}

	/**
	 * Check if this term has as ancestor the 'ancestor' term on the selected
	 * hierarchy
	 * 
	 * @param ancestor  the ancestor of the term
	 * @param hierarchy the hierarchy in which the ancestor should be present
	 * @return
	 */
	public boolean hasAncestor(Term ancestor, Hierarchy hierarchy) {

		boolean found = false;

		// if we found that the target is the ancestor we found the relationship
		// since we go up into the tree parent by parent
		if (this.equals(ancestor))
			found = true;

		else {

			// if we have not found the relationship then we
			// get the parent of the term in the selected hierarchy
			// to go up into the tree
			Term parent = this.getParent(hierarchy);

			// if null parent return false
			if (parent == null)
				found = false;
			else {
				// go on with the recursion
				found = parent.hasAncestor(ancestor, hierarchy);
			}
		}

		return found;
	}

	/**
	 * Get the children of this term in the selected hierarchy. You can filter
	 * deprecated or not reportable terms enabling the booleans.
	 * 
	 * @param hierarchy
	 * @param hideDeprecated
	 * @param hideNotReportable
	 * @return
	 */
	public ArrayList<Term> getChildren(Hierarchy hierarchy, boolean hideDeprecated, boolean hideNotReportable) {

		ParentTermDAO parentDao = new ParentTermDAO(catalogue);

		return parentDao.getChildren(this, hierarchy, hideDeprecated, hideNotReportable);
	}

	/**
	 * Get all the children of the term, without filters
	 * 
	 * @param hierarchy
	 * @return
	 */
	public ArrayList<Term> getAllChildren(Hierarchy hierarchy) {
		return this.getChildren(hierarchy, false, false);
	}

	/**
	 * Check if the term has children or not
	 * 
	 * @param hierarchy
	 * @return
	 */
	public boolean hasChildren(Hierarchy hierarchy, boolean hideDeprecated, boolean hideNotReportable) {
		return !getChildren(hierarchy, hideDeprecated, hideNotReportable).isEmpty();
	}

	/**
	 * Get the hierarchies that contains this term
	 * 
	 * @return
	 */
	public ArrayList<Hierarchy> getApplicableHierarchies() {

		// output array
		ArrayList<Hierarchy> hierarchies = new ArrayList<>();

		Collection<Applicability> appls = getInUseApplicabilities();

		// return if no applicability is found
		if (appls == null)
			return hierarchies;

		// for each applicability add the hierarchy to the output array
		for (Iterator<Applicability> i = appls.iterator(); i.hasNext();)
			hierarchies.add(i.next().getHierarchy());

		return hierarchies;
	}

	/**
	 * Check if the term is contained in the input hierarchy or not
	 * 
	 * @param hierarchy
	 * @return
	 */
	public boolean belongsToHierarchy(Hierarchy hierarchy) {
		return getApplicableHierarchies().contains(hierarchy);
	}

	/**
	 * Get all the hierarchies in which this term is not present.
	 * 
	 * @return
	 */
	public ArrayList<Hierarchy> getNewHierarchies() {

		ArrayList<Hierarchy> applHierarchies = getApplicableHierarchies();
		ArrayList<Hierarchy> hierarchies = new ArrayList<>();

		// get the hierarchies where this term is not present
		for (Hierarchy hierarchy : catalogue.getInUseHierarchies()) {
			if (!applHierarchies.contains(hierarchy))
				hierarchies.add(hierarchy);
		}

		return hierarchies;
	}

	/**
	 * Get the hierarchies where: - the child is not present - the parent is present
	 * and does not have the child in its children
	 * 
	 * @return
	 */
	public ArrayList<Hierarchy> getNewHierarchies(Term child) {

		ArrayList<Hierarchy> hierarchies = new ArrayList<>();

		// get the hierarchies where this term does not have the input
		// child in its children
		for (Hierarchy hierarchy : getApplicableHierarchies()) {

			// continue to the next hierarchy if the selected child
			// is indeed a parent of the term which is supposed to be the parent!
			// this should be an error so we return
			if (this.hasAncestor(child, hierarchy))
				continue;

			ArrayList<Term> children = this.getAllChildren(hierarchy);

			// if the parent in this hierarchy does not already have
			// the child in its children and if the child is not already
			// present in the selected hierarchy
			if (!children.contains(child) && !child.getApplicableHierarchies().contains(hierarchy))
				hierarchies.add(hierarchy);
		}

		return hierarchies;
	}

	/**
	 * Check if the term is reportable in the hierarchy
	 * 
	 * @param hierarchy
	 * @return
	 */
	public boolean isReportable(Hierarchy hierarchy) {

		boolean reportable = true;

		// for each applicability
		for (Applicability appl : applicabilities) {

			// the current applicability contains the hierarchy?
			// if so check reportability
			if (appl.relatedToHierarchy(hierarchy)) {
				reportable = appl.isReportable();
				break;
			}
		}

		return reportable;
	}

	/**
	 * Check if a term is dismissed, that is, a term which is not reportable and do
	 * not have reportable children
	 * 
	 * @param hierarchy
	 * @return
	 */
	public boolean isDismissed(Hierarchy hierarchy) {

		boolean repChilden = hasOnlyDeprecatedOrNotReportableChildren(hierarchy);

		// if all the children are deprecated or not reportable
		// and the term is not reportable and not deprecated,
		// then it is dismissed (if it is deprecated then it is
		// only deprecated, not dismissed)
		return (repChilden) && !isReportable(hierarchy) && !isDeprecated();
	}

	/**
	 * How the term name should be visualised according to its applicability?
	 * 
	 * @param hierarchy
	 * @return
	 */
	public Font getApplicabilityFont(Hierarchy hierarchy) {

		// if we have a deprecated term or a non reportable term
		if (this.isDeprecated() || this.isDismissed(hierarchy)) {

			FontData fontData = Display.getCurrent().getSystemFont().getFontData()[0];

			// make italic name
			Font italic = new Font(Display.getCurrent(),
					new FontData(fontData.getName(), fontData.getHeight(), SWT.ITALIC));

			return italic;
		}

		// otherwise return default font
		return Display.getCurrent().getSystemFont();
	}

	/**
	 * Check if the term is deprecable or not. A term is deprecable if all its
	 * children (in all the applicable hierarchies) are deprecated
	 * 
	 * @return
	 */
	public boolean hasAllChildrenDeprecated() {

		// for each applicable hierarchy of the term we check if all the sub tree is
		// deprecated or not
		for (Hierarchy hierarchy : getApplicableHierarchies()) {

			// create a subtree iterator for all the terms contained in the subtree
			// related to this parent term in the current hierarchy
			TermSubtreeIterator iterator = new TermSubtreeIterator(this, hierarchy);

			Term currentChild;

			// do until we have processed all the children of the entire sub tree
			while ((currentChild = iterator.next()) != null) {

				// check if the child is deprecated or not. If the child is
				// deprecated go on, otherwise the term cannot be deprecated
				// since it has a child which is not deprecated
				if (!currentChild.isDeprecated())
					return false;
			}
		}

		// if we have processed all the children and we arrive here, this means that all
		// the children of the sub tree are deprecated
		return true;
	}

	/**
	 * Check if the term is deprecable or not. A term is deprecable if all its
	 * children (in all the applicable hierarchies) are deprecated
	 * 
	 * @return
	 */
	public boolean hasReportableChildren(Hierarchy hierarchy) {

		// create a subtree iterator for all the terms contained in the subtree
		// related to this parent term in the current hierarchy
		TermSubtreeIterator iterator = new TermSubtreeIterator(this, hierarchy);

		Term currentChild;

		// do until we have processed all the children of the entire sub tree
		while ((currentChild = iterator.next()) != null) {

			// check if the child is reportable or not
			if (currentChild.isReportable(hierarchy))
				return true;
		}

		// if we have processed all the children and we arrive here, this means that all
		// the children of the sub tree are not reportable
		return false;
	}

	/**
	 * Check if we can remove the deprecation from a term or not. We can only if all
	 * the term parents are not deprecated.
	 * 
	 * @return
	 */
	public boolean hasDeprecatedParents() {

		// for each applicable hierarchy of the term we check if all its parents
		// are deprecated or not
		for (Hierarchy hierarchy : getApplicableHierarchies()) {

			Term parent = this.getParent(hierarchy);

			while (parent != null) {
				// check if the parent is deprecated or not. If it is deprecated we cannot
				// remove the deprecation from the child since it is a wrong operation
				if (parent.isDeprecated())
					return true;

				// get the next parent and go on
				parent = parent.getParent(hierarchy);
			}
		}

		// if we have processed all the parents and we arrive here, this means that all
		// the parents are not deprecated!
		return false;
	}

	/**
	 * Check if the term is deprecable or not. A term is deprecable if all its
	 * children (in all the applicable hierarchies) are deprecated
	 * 
	 * @return
	 */
	public boolean hasOnlyDeprecatedOrNotReportableChildren(Hierarchy hierarchy) {

		// create a subtree iterator for all the terms contained in the subtree
		// related to this parent term in the current hierarchy
		TermSubtreeIterator iterator = new TermSubtreeIterator(this, hierarchy);

		Term currentChild;

		// do until we have processed all the children of the entire sub tree
		while ((currentChild = iterator.next()) != null) {

			// check if the child is reportable and not deprecated
			if (currentChild.isReportable(hierarchy) && !currentChild.isDeprecated())
				return false;
		}

		return true;
	}

	/**
	 * Get the note text without the links
	 * 
	 * @param delim : the character from which the links start
	 * @return
	 */
	public String getNotesWithoutLink(String delim, String notes) {

		String outputNotes = "";
		// if no term is selected avoid calling getnotes on null object (it would thrown
		// an error)

		// Recognize where links start using the delim character
		StringTokenizer st = new StringTokenizer(notes, delim);

		// Get the first token (it is the note text)
		if (st.hasMoreTokens()) {
			outputNotes = st.nextToken();
		}

		return (outputNotes);
	}

	/**
	 * Get the html links. This means that each link is formatted with the hypertext
	 * flag all is returned as a single string to be able to print directly into the
	 * display
	 * 
	 * @param delim
	 * @return
	 */
	public String getHtmlLinksFromNotes(String delim, String notes) {

		// parse the links with the £ separator
		ArrayList<String> links = getLinksFromNotes(delim, notes);
		String htmlLinks = "";

		if (links == null || links.isEmpty()) {
			return ("No available links. ");
		}

		// for each link in the list
		for (int i = 0; i < links.size(); i++) {

			// get the current link string
			String currentLink = links.get(i);

			// set the default name of the link
			int j = i + 1;
			String linkName = "<a href=\"" + currentLink + "\">Link" + j + "</a>";

			// try to inpute the web name
			// get the part after the http (the //)
			String[] split = currentLink.split("//");

			// get the string between the // and the first /
			// ( from https://www.google.com/ => google.com will be extracted )
			if (split.length > 1) {
				StringTokenizer st = new StringTokenizer(split[1], "/");

				// if there is a / go on
				if (st.hasMoreTokens()) {

					// set the link name using the web name
					linkName = "<a href=\"" + currentLink + "\">" + st.nextToken() + "</a>";

					// remove the www. string
					linkName = linkName.replaceAll("www.", "");
				}
			}

			// add the link to the list of links
			htmlLinks = htmlLinks + linkName;
			// add a separator between links
			if (i < links.size() - 1) {
				htmlLinks = htmlLinks + " - ";
			}
		}

		return (htmlLinks);
	}

	/**
	 * Get the links related to the term notes, save the result into an array list
	 * of strings
	 * 
	 * @param delim separator between the links in the scopenotes
	 * @return
	 */
	public ArrayList<String> getLinksFromNotes(String delim, String notes) {

		ArrayList<String> links = new ArrayList<>(); // it will store all the links which are present in the scopenotes

		int i = 0; // used to count the number of tokens

		// Recognize links separated by the $ character
		StringTokenizer st = new StringTokenizer(notes, delim);

		// Analyze all the tokens
		while (st.hasMoreTokens()) {

			String token = st.nextToken();

			// Avoid the first token (it is the standard scopenote, not the links)
			if (i != 0 || token.startsWith("http")) {
				links.add(token);
			}

			i++; // count the number of tokens
		}
		return (links);
	}

	/**
	 * Get a default new term ( created when we add a new term into the browser )
	 * 
	 * @param code the code of the new term
	 * @return
	 */
	public static Term getDefaultTerm(String code) {

		// get an instance of the global manager
		GlobalManager manager = GlobalManager.getInstance();

		// get the current catalogue
		Catalogue currentCat = manager.getCurrentCatalogue();

		return getDefaultTerm(currentCat, code);
	}

	/**
	 * Get a default new term
	 * 
	 * @param catalogue the catalogue which will contain the new term
	 * @param code      the code of the new term
	 * @return
	 */
	public static Term getDefaultTerm(Catalogue catalogue, String code) {

		Term term = new Term(catalogue);

		term.setCode(code);

		// default names and scopenotes
		term.setName(CBMessages.getString("BrowserTreeMenu.NewTermDefaultName") + code);
		term.setDisplayAs(CBMessages.getString("BrowserTreeMenu.NewTermDefaultName") + code);
		term.setScopenotes("");

		// default the term is not deprecated
		term.setDeprecated(false);

		// set valid from NOW
		term.setValidFrom(new java.sql.Timestamp(System.currentTimeMillis()));

		// if the catalogue supports detail levels add the default
		if (catalogue.hasDetailLevelAttribute())
			term.setDetailLevel(TermAttribute.getDefaultDetailLevel(term));

		// if the catalogue supports term types add the default
		if (catalogue.hasTermTypeAttribute())
			term.setTermType(TermAttribute.getDefaultTermType(term));

		return term;
	}

	/**
	 * Get all the term siblings in the selected hierarchy
	 * 
	 * @return
	 */
	private ArrayList<Term> getSiblings(Hierarchy hierarchy) {

		ParentTermDAO parentDao = new ParentTermDAO(catalogue);

		// get all the children of the parent of this term in order to get the term
		// siblings and the term itself
		ArrayList<Term> siblings = parentDao.getChildren(this.getParent(hierarchy), hierarchy, false, false);

		// remove the current term from the children
		siblings.remove(this);

		return siblings;
	}

	public enum Position {
		BEFORE, AFTER
	}

	/**
	 * Move the term before or after the target depending on the position
	 * {@code pos}
	 * 
	 * @param target    the target of the movement
	 * @param hierarchy the hierarchy in which we move the term
	 * @param pos       before or after the target
	 */
	public void moveAsSibling(Term target, Hierarchy hierarchy, Position pos) {

		// get the parent of the target (since we will set it as the new
		// parent of the current term)
		Nameable targetParent = target.getParent(hierarchy);

		// if no parent, then we have the hierarchy as parent
		if (targetParent == null) {
			targetParent = hierarchy;
		}

		// cannot move parent under its children
		if (target.hasAncestor(this, hierarchy)) {
			LOGGER.info("Cannot move parent as child of its children");
			return;
		}

		ArrayList<Term> termsToNormalize = this.getSiblings(hierarchy);

		// change the source parent with the target parent
		this.setParent(hierarchy, targetParent);

		// save the target order
		int targetOrder = target.getOrder(hierarchy);

		// get all the target siblings
		Collection<Term> targetSiblings = target.getSiblings(hierarchy);
		for (Term sibling : targetSiblings) {

			int siblingOrder = sibling.getOrder(hierarchy);

			// if below target, then move down to free one space
			if (siblingOrder > targetOrder) {
				sibling.setOrder(hierarchy, siblingOrder + 1);
			}
		}

		// if the source was placed before the target,
		// then move also the target to free one space
		if (pos == Position.BEFORE) {

			// put the source in the target position
			this.setOrder(hierarchy, targetOrder);

			// move the target down to free space
			target.setOrder(hierarchy, targetOrder + 1);
		} else {
			// if after the target, do not touch target order
			// and place the source under the target
			this.setOrder(hierarchy, targetOrder + 1);
		}

		if (!termsToNormalize.isEmpty()) {

			Nameable parent = termsToNormalize.get(0).getParent(hierarchy);

			if (parent == null) {
				parent = hierarchy;
			}

			// if same parent, then add also the source to the
			// list of terms to normalize
			if (targetParent.equals(parent)) {
				termsToNormalize.add(this);
			}

			normalizeLevel(termsToNormalize, hierarchy);
		}
	}

	public void moveAsChild(Nameable target, Hierarchy hierarchy) {

		ArrayList<Term> termsToNormalize = new ArrayList<>(this.getSiblings(hierarchy));

		int newOrder = getFirstAvailableChildrenOrder(target, hierarchy);

		// change the order with the first available
		this.setOrder(hierarchy, newOrder);

		// change the source parent with the target parent
		this.setParent(hierarchy, target);

		// normalize source level
		if (!termsToNormalize.isEmpty()) {
			normalizeLevel(termsToNormalize, hierarchy);
		}
	}

	public boolean isRootTerm(Hierarchy hierarchy) {

		Applicability appl = this.getApplicability(hierarchy);
		return appl != null && appl.getParentTerm() instanceof Hierarchy;
	}

	/**
	 * Get the next available order for the terms in a specific branch in a
	 * hierarchy
	 * 
	 * @param hierarchy
	 * @return
	 */
	public int getFirstAvailableOrder(Hierarchy hierarchy) {

		// get all the target siblings
		List<Term> termsOnLevel = this.getSiblings(hierarchy);
		termsOnLevel.add(this);

		return getNextOrder(termsOnLevel, hierarchy);
	}

	public static int getFirstAvailableChildrenOrder(Nameable parent, Hierarchy hierarchy) {

		List<Term> termsOnLevel;

		if (parent instanceof Term) {
			termsOnLevel = ((Term) parent).getAllChildren(hierarchy);
		} else {
			termsOnLevel = ((Hierarchy) parent).getFirstLevelNodes(false, false);
		}

		return getNextOrder(termsOnLevel, hierarchy);
	}

	private static int getNextOrder(List<Term> terms, Hierarchy hierarchy) {

		int maxOrder = -1;
		for (Term sibling : terms) {

			int siblingOrder = sibling.getOrder(hierarchy);

			if (siblingOrder > maxOrder) {
				maxOrder = siblingOrder;
			}
		}

		int available = terms.isEmpty() ? 1 : maxOrder + 1;
		return available;
	}

	/**
	 * Normalize order of term siblings and term itself
	 * 
	 * @param hierarchy
	 * @return
	 */
	public void normalizeLevel(final Hierarchy hierarchy) {

		ArrayList<Term> terms = this.getSiblings(hierarchy);
		// terms.add( this );

		normalizeLevel(terms, hierarchy);
	}
	
	/**
	 * get the term level of detail in the tree
	 * 
	 * @param term
	 * @param hierarchy
	 * @return
	 * @unused
	 */
	public int getLevelInTree(Hierarchy hierarchy) {
		// min level of detail
		int level=1;
		
		// get the parent
		Term parent = this.getParent(hierarchy);

		// do until root
		while (parent != null) {
			// increment level
			level+=1;
			// get the next parent
			parent = parent.getParent(hierarchy);
		}

		return level;
	}

	/**
	 * 
	 * @param termsOnLevel
	 * @param hierarchy
	 * @return
	 * @unused
	 */
	public List<Term> orderLevel(List<Term> termsOnLevel, final Hierarchy hierarchy) {

		// sort terms by order
		Collections.sort(termsOnLevel, new Comparator<Term>() {
			public int compare(Term t1, Term t2) {

				int o1 = t1.getOrder(hierarchy);
				int o2 = t2.getOrder(hierarchy);

				if (o1 == o2)
					return 0;

				else if (o1 < o2)
					return -1;

				return 1;
			};
		});

		return termsOnLevel;
	}

	/**
	 * Normalize the terms siblings order, that is, all the integer order holes
	 * between siblings are filled replacing the terms orders with increasing
	 * numbers (maintaining the same order!).
	 * 
	 * @param hierarchy the hierarchy in which the siblings are retrieved
	 * @return the list of siblings with normalized order
	 */
	public static void normalizeLevel(ArrayList<Term> termsOnLevel, final Hierarchy hierarchy) {

		// sort terms by order
		Collections.sort(termsOnLevel, new Comparator<Term>() {
			public int compare(Term t1, Term t2) {

				int o1 = t1.getOrder(hierarchy);
				int o2 = t2.getOrder(hierarchy);

				if (o1 == o2)
					return 0;

				else if (o1 < o2)
					return -1;

				return 1;
			};
		});

		// normalize order integer replacing orders
		// with increasing numbers to cover all the
		// orders holes

		for (int i = 0; i < termsOnLevel.size(); i++) {
			// set order for siblings
			termsOnLevel.get(i).setOrder(hierarchy, i + 1);
		}
	}

	/**
	 * Get the nearest term above this in term of order
	 * 
	 * @param hierarchy
	 * @return
	 */
	public Term getAboveSibling(Hierarchy hierarchy) {
		return getNearestSibling(hierarchy, true);
	}

	/**
	 * Get the nearest term below this in term of order
	 * 
	 * @param hierarchy
	 * @return
	 */
	public Term getBelowSibling(Hierarchy hierarchy) {
		return getNearestSibling(hierarchy, false);
	}

	/**
	 * Check if the term has an above term or not
	 * 
	 * @param hierarchy
	 * @return
	 */
	public boolean hasAboveSibling(Hierarchy hierarchy) {
		return getAboveSibling(hierarchy) != null;
	}

	/**
	 * Check if the term has a below term or not
	 * 
	 * @param hierarchy
	 * @return
	 */
	public boolean hasBelowSibling(Hierarchy hierarchy) {
		return getBelowSibling(hierarchy) != null;
	}

	/**
	 * Get the nearest above/below sibling
	 * 
	 * @param hierarchy
	 * @param above,    should be the above sibling or the below sibling?
	 * @return
	 */
	private Term getNearestSibling(Hierarchy hierarchy, boolean above) {

		Term aboveSibling = null;
		int currentOrder = above ? Integer.MIN_VALUE : Integer.MAX_VALUE;

		// search in the siblings the nearest term with the maximum order less than the
		// term order
		// if above. search in the siblings the nearest term with the minimum order
		// greater than the term order
		// if below.
		for (Term sibling : getSiblings(hierarchy)) {

			// get the sibling order in the selected hierarchy
			int siblingOrder = sibling.getApplicability(hierarchy).getOrder();

			if (above) {

				// if the order is less than this term order, save the sibling
				if (siblingOrder < this.getApplicability(hierarchy).getOrder()) {

					// if it is also greater than the maximum that we have found until now
					if (siblingOrder > currentOrder) {
						aboveSibling = sibling;
						currentOrder = siblingOrder;
					}
				}
			} else {
				
				// if the order is greater than this term order, save the sibling
				if (siblingOrder > this.getApplicability(hierarchy).getOrder())

					// if it is also less then than the minimum that we have found until now
					if (siblingOrder < currentOrder) {
						aboveSibling = sibling;
						currentOrder = siblingOrder;
					}
			}
		}

		// here we have the nearest sibling in term of ord code

		return aboveSibling;
	}

	/**
	 * Swap the term order
	 * 
	 * @param term
	 * @param hierarchy
	 */
	private void swapTermOrder(Term term, Hierarchy hierarchy) {

		ParentTermDAO parentDao = new ParentTermDAO(catalogue);

		parentDao.swapTermOrder(this, term, hierarchy);

		// swap term in RAM
		int order = this.getOrder(hierarchy);
		this.setOrder(hierarchy, term.getOrder(hierarchy));
		term.setOrder(hierarchy, order);

		TermDAO termDao = new TermDAO(catalogue);

		// update the involved terms in ram
		termDao.updateTermInRAM(this);
		termDao.updateTermInRAM(term);
	}

	/**
	 * Move a term up in the hierarchy if possible
	 * 
	 * @param hierarchy
	 */
	public void moveUp(Hierarchy hierarchy) {

		Term sibling = this.getAboveSibling(hierarchy);

		// swap only if there is indeed a sibling
		if (sibling != null)
			swapTermOrder(sibling, hierarchy);
	}

	/**
	 * Move a term down in the hierarchy if possible
	 * 
	 * @param hierarchy
	 */
	public void moveDown(Hierarchy hierarchy) {

		Term sibling = this.getBelowSibling(hierarchy);

		// swap only if there is indeed a sibling
		if (sibling != null)
			swapTermOrder(sibling, hierarchy);
	}

	/**
	 * Move this term one level up into the hierarchy (i.e. we move the term as
	 * child of the parent of the parent of the term)
	 * 
	 * @param hierarchy
	 */
	public void moveLevelUp(Hierarchy hierarchy) {

		// get the term parent
		Nameable parent = this.getParent(hierarchy);

		// if we have not reached the max level (i.e. a hierarchy)
		if (!(parent instanceof Term))
			return;

		// move after the parent under the grandparent
		this.moveAsSibling((Term) parent, hierarchy, Position.AFTER);
	}

	/**
	 * Check if the content of the term are correct and follows the catalogue rules
	 * 
	 * @return
	 */
	public boolean isDataCorrect() {

		boolean correct = true;

		ArrayList<FacetDescriptor> allFacets = getFacets(true);

		Set<String> checks = new HashSet<>();

		for (FacetDescriptor fd : allFacets) {

			// if facet already present for single cardinality
			// facet => error
			if (checks.contains(fd.getFacetHeader()) && !fd.getFacetCategory().isRepeatable()) {
				correct = false;
				break;
			}

			checks.add(fd.getFacetHeader());
		}

		return correct;
	}

	/**
	 * To print terms directly
	 */
	@Override
	public String toString() {
		return "TERM " + getId() + "; code=" + getCode() + ";name=" + getName();
	}

	@Override
	public boolean equals(Object term) {

		if (term instanceof Term) {

			String code = ((Term) term).getCode();
			int id = ((Term) term).getId();

			if (!this.getCode().isEmpty() && !code.isEmpty())
				return this.getCode().equals(code);
			else
				return this.getId() == id;
		}

		return super.equals(term);
	}

}
