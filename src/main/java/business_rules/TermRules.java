package business_rules;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import catalogue.Catalogue;
import catalogue_browser_dao.TermDAO;
import catalogue_object.Attribute;
import catalogue_object.Hierarchy;
import catalogue_object.Term;
import catalogue_object.TermAttribute;
import ui_implicit_facet.DescriptorTreeItem;
import ui_implicit_facet.FacetDescriptor;
import ui_implicit_facet.FacetType;

/**
 * The TermRules class provide all the business rules applied to FoodEx2 terms
 * v.2.1 29-11-2019
 * 
 * @author shahaal
 *
 */
public abstract class TermRules {

	private static final Logger LOGGER = LogManager.getLogger(TermRules.class);

	protected Catalogue currentCat;

	// list of all the processes which may cause a warning
	protected ArrayList<ForbiddenProcess> forbiddenProcesses;

	// load the colour options for the warning console and messages
	protected WarningOptions warnOptions;

	// load into memory all the warning messages from the text file
	protected ArrayList<WarningMessage> warningMessages;
	
	protected boolean btCorrect = false;
	
	/**
	 * Enum type: identify the warning messages to print
	 * 
	 * Check in doc/CatalogueBrowser_business_rules_v2.xlsx
	 * 
	 * @author shahaal
	 *
	 */	
	protected static enum WarningEvent {
		BR01, BR02, BR03, BR04, BR05, BR06, BR07, BR08, BR09, BR10, BR11, BR12, BR13, BR14, BR15, BR16, BR17, BR18,
		BR19, BR20, BR21, BR22, BR23, BR24, BR25, BR26, BR27, BR28, BR29, BR30, BR31
	}

	/**
	 * Enum type: identify the level of warning
	 * 
	 * @author shahaal
	 *
	 */
	protected enum WarningLevel {
		NONE, LOW, HIGH, ERROR
	}
	
	/**
	 * BR01
	 * Check if the user adds a source-commodity facet which is not specifying the already present implicit one
	 * 
	 * @param bt
	 * @param allFacets
	 * @param stdOut
	 */
	protected void sourceCommodityRawCheck(Term bt, String allFacets, boolean stdOut) {

		// if the base term is not a raw commodity no checks have to be done
		if (!isRawCommodityTerm(bt))
			return;

		// count the source commodity
		int sourceCommodityFacetCount = 0;

		ArrayList<FacetDescriptor> implicitFacets = bt.getFacets(true);

		TermDAO termDao = new TermDAO(currentCat);
		ArrayList<Term> implicitTerms = new ArrayList<>();
		
		// add implicit facets of the term
		for (FacetDescriptor fd : implicitFacets)
			implicitTerms.add(termDao.getByCode(fd.getFacetCode()));

		// populate the explicit facets
		ArrayList<FacetDescriptor> explicitFacets = new ArrayList<>();

		// split facets => the implicit are not considered since
		// raw commodities have their self as source commodity and should not
		// be taken into account
		StringTokenizer st = new StringTokenizer(allFacets, "$");

		// for each explicit facet
		while (st.hasMoreTokens()) {

			String code = st.nextToken();

			// split the facet in facet header and facet code
			String[] split = splitFacetFullCode(code);

			Term term = termDao.getByCode(split[1]);

			FacetDescriptor fd = new FacetDescriptor(term, new TermAttribute(term, null, code), FacetType.EXPLICIT);

			explicitFacets.add(fd);
		}

		// diagnostic string builder
		StringBuilder sb = new StringBuilder();

		// get the racsource hierarchy
		Hierarchy hierarchy = currentCat.getHierarchyByCode("racsource");

		// restrict if explicit is child of an implicit
		for (FacetDescriptor fd : explicitFacets) {
			// check if the explicit is specification of the implicit
			boolean isSpecification = false;
			
			for (Term implicit : implicitTerms) {
				if (fd.getDescriptor().hasAncestor(implicit, hierarchy)) {
					isSpecification = true;
					break;
				}
			}
			// check if the explicit facet is specification of the bt
			isSpecification = isSpecification || fd.getDescriptor().hasAncestor(bt, hierarchy);
			// count the number of source commodities facets
			if (isSourceCommodityFacet(fd.getFacetHeader()) && !isSpecification  ) {
				sourceCommodityFacetCount += 1;
				sb.append(fd.getFacetCode());
				sb.append(" - ");
			}
		}

		// get all the involved terms
		String termsInvolved = sb.toString();

		// if # of explicit source commodities that are not specifying implicit facets > 0 than raise warning
		if (sourceCommodityFacetCount > 0) {
			// remove the last " - " if present (i.e. at least one source c. was added)
			termsInvolved = termsInvolved.substring(0, termsInvolved.length() - " - ".length());
			// warn user if adding an explicit facet which is not better specifying the already present implicit one
			printWarning(WarningEvent.BR01, termsInvolved, false, stdOut);
		}	

	}

	/**
	 * BR03
	 * Check if a source is added to a composite food
	 * 
	 * @param baseTerm
	 * @param facetIndex
	 * @param facetCode
	 */
	private void sourceInCompositeCheck(Term baseTerm, String facetIndex, String facetCode, boolean stdOut) {
		if (isCompositeTerm(baseTerm) && isSourceFacet(facetIndex))
			printWarning(WarningEvent.BR03, facetCode, false, stdOut);
	}
	
	/**
	 * 
	 * BR04
	 * Check if a source commodity is added to a composite food
	 * 
	 * @param baseTerm
	 * @param facetIndex
	 * @param facetCode
	 */
	private void sourceCommodityInCompositeCheck(Term baseTerm, String facetIndex, String facetCode, boolean stdOut) {
		if (isCompositeTerm(baseTerm) && isSourceCommodityFacet(facetIndex))
			printWarning(WarningEvent.BR04, facetCode, false, stdOut);
	}

	/**
	 * BR05 - BR06 - BR07
	 * check if a source is added to derivative. If a source
	 * commodity is already specified then the source must be used only to specify
	 * better the source commodity. If more than one source commodity is already
	 * specified, then a source cannot be used. Warnings are raised in the warning
	 * situations.
	 * 
	 * @param bt
	 * @param allFacets
	 */
	protected void sourceCommodityDerivativeCheck(Term bt, String allFacets, boolean stdOut) {

		// rule only applicable to derivatives
		if (!isDerivativeTerm(bt))
			return;

		ArrayList<FacetDescriptor> implicitFacets = bt.getFacets(true);
		ArrayList<Term> implicitTerms = new ArrayList<>();

		int implicitSourceCommCount, explicitSourceCommCount, explicitRestrictedSourceCommCount, sourceFacetCount;

		// initialize the counters
		implicitSourceCommCount = explicitSourceCommCount = explicitRestrictedSourceCommCount = sourceFacetCount = 0;

		// string builder for generating the diagnostic string
		StringBuilder sb = new StringBuilder();

		TermDAO termDao = new TermDAO(currentCat);

		// check implicit facets
		for (FacetDescriptor fd : implicitFacets) {

			implicitTerms.add(termDao.getByCode(fd.getFacetCode()));

			String header = fd.getFacetHeader();

			if (isSourceCommodityFacet(header))
				implicitSourceCommCount++;
			else
				continue;

			// append for diagnostic
			sb.append(fd.getFacetCode());
			sb.append(" - ");

		}

		// check explicit facets
		StringTokenizer st = new StringTokenizer(allFacets, "$");

		ArrayList<FacetDescriptor> explicitFacets = new ArrayList<>();

		// for each facet
		while (st.hasMoreTokens()) {

			String code = st.nextToken();

			// split the facet in facet header and facet code
			String[] split = splitFacetFullCode(code);

			Term term = termDao.getByCode(split[1]);

			FacetDescriptor fd = new FacetDescriptor(term, new TermAttribute(term, null, code), FacetType.EXPLICIT);

			explicitFacets.add(fd);
		}

		// count for explicit facets
		for (FacetDescriptor fd : explicitFacets) {

			boolean skip = false;

			Hierarchy hierarchy = currentCat.getHierarchyByCode("racsource");
			for (Term implicit : implicitTerms) {
				if (fd.getDescriptor().hasAncestor(implicit, hierarchy)) {
					skip = true;
					break;
				}
			}

			String header = fd.getFacetHeader();

			// count the number of source commodities facets
			if (isSourceCommodityFacet(header)) {

				// if restricted
				if (skip)
					explicitRestrictedSourceCommCount++;
				else
					explicitSourceCommCount++;

				// append for diagnostic
				sb.append(fd.getFacetCode());
				sb.append(" - ");

			}

			if (isSourceFacet(header)) {

				sourceFacetCount++;

				// append for diagnostic
				sb.append(fd.getFacetCode());
				sb.append(" - ");
			}
		}

		int totalSourceCommCount = explicitSourceCommCount + implicitSourceCommCount;

		// get all the involved terms
		String termsInvolved = sb.toString();

		// remove the last " - " if present (i.e. at least one element between source
		// and source c. was added)
		if (sourceFacetCount > 0 || totalSourceCommCount > 0)
			termsInvolved = termsInvolved.substring(0, termsInvolved.length() - " - ".length());

		// if we have an implicit sc, an explicit sc and not a source
		if (explicitSourceCommCount > 0 && implicitSourceCommCount > 0)
			printWarning(WarningEvent.BR05, termsInvolved, false, stdOut);

		int implAndSpecifications = explicitRestrictedSourceCommCount + implicitSourceCommCount;
		
		// if one or more sources are present
		if (sourceFacetCount > 0) {
			
			// if source without source commodities
			if (totalSourceCommCount == 0)
				printWarning(WarningEvent.BR06, termsInvolved, false, stdOut);
			
			// check if user is adding multiple sc (note that specification of already present are treated differently)
			if((implAndSpecifications==0 && explicitSourceCommCount>1) || 
					(implAndSpecifications>2 && explicitSourceCommCount == 0) ||
					(implAndSpecifications>0 && explicitSourceCommCount>0)) {
				printWarning(WarningEvent.BR07, termsInvolved, false, stdOut);
			}
		}
	}
	
	/**
	 * BR08
	 * Warn user if selected a not reportable term as base term if the term is reportable
	 * 
	 * @param term
	 * @param stdOut
	 */
	protected void isNotReportable(Term term, boolean stdOut) {
		// skip the rule if the term is dismissed
		if (term.isDismissed(currentCat.getDefaultHierarchy()))
			return;
		
		if(!term.isReportable(currentCat.getDefaultHierarchy())) {
			printWarning(WarningEvent.BR08, term.getCode(), false, stdOut);
		}
	}
	
	/**
	 * BR10
	 * Check if a non-specific term is selected
	 * 
	 * @param bt
	 * @param stdOut
	 */
	protected void nonSpecificTermCheck(Term bt, String facetIndex, boolean stdOut) {
		if (isNonSpecificTerm(bt)) {
			if(isSourceFacet(facetIndex)||isSourceCommodityFacet(facetIndex)) {
				return;
			} else {
				printWarning(WarningEvent.BR10, bt.getCode(), false, stdOut);
			}
		}
	}

	/**
	 * BR11
	 * Check if the "processed" facet is generic
	 * 
	 * @param facet
	 * @param stdOut
	 */
	protected void genericProcessedFacetCheck(Term facet, boolean stdOut) {
		if (isGenericProcessFacet(facet)) 
			printWarning(WarningEvent.BR11, facet.getCode(), false, stdOut);
		
	}

	/**
	 * BR12
	 * Check if the user added an ingredient to a raw commodity or to a
	 * derivative
	 * 
	 * @param bt
	 * @param facetIndex
	 * @param facet
	 * @param stdOut
	 */
	protected void minorIngredientCheck(Term bt, String facetIndex, Term facet, boolean stdOut) {

		// rule valid only for raw commodities or derivatives
		if (isRawCommodityTerm(bt) || isDerivativeTerm(bt)) {
		
			// if the baseterm is not flavored and the facet is an ingredient
			if (!isFlavoured(bt) && isIngredientFacet(facetIndex)) {
				
				// get the ingredient facet category
				Attribute facetCategory = currentCat.getAttributeById(20);
	
				// if the explicit facet is more detailed than the implicit don't print the warning
				if (facetCategory != null) {
					for (DescriptorTreeItem dti : bt.getInheritedImplicitFacets(facetCategory)) {
						if (facet.hasAncestor(dti.getTerm(), facetCategory.getHierarchy()))
							return;
					}
				}
	
				// otherwise print the warning
				printWarning(WarningEvent.BR12, facet.getCode(), false, stdOut);
			}
		}
	}
	
	/**
	 * BR13
	 * Check if a physical state facet is added to rpc term
	 * 
	 * @param bt
	 * @param allFacets
	 * @param stdOut
	 */
	protected void physicalStateRawCheck(Term bt, String fcIndex, String fcCode, boolean stdOut) {
		if (isRawCommodityTerm(bt) 
				&& isPhysicalStateFacet(fcIndex) 
				&& isForbiddenPhysicalState(fcCode)) {
			printWarning(WarningEvent.BR13, fcCode, false, stdOut);
		}
	}
	
	/**
	 * BR16
	 * Raise a warning if the user add an explicit facet which 
	 * is parent of the already present implicit facets
	 * 
	 * @param bt
	 * @param fcIndex
	 * @param fcCode
	 */
	protected void checkIfExplicitLessDetailed(Term bt, String facetIndex, Term fc,boolean stdOut) {
		// get all implicit facets of the base term
		ArrayList<FacetDescriptor> implicitFacets = bt.getFacets(true);
		
		// check for each implicit facets if the explicit is parent
		for(FacetDescriptor fd : implicitFacets) {
			// get the facet category to which appertain the implicit facet
			Attribute fcCat = fd.getFacetCategory();
			// skip implicit facets of different facet categories
			if (!fcCat.getCode().equals(facetIndex))
				continue;
			// get the implicit term info
			Term implTerm = currentCat.getTermByCode(fd.getFacetCode());
			// get category hierarchy
			Hierarchy h = fcCat.getHierarchy();
			// two terms are siblings if have same parent
			boolean areSiblings = (fc.getParent(h)==implTerm.getParent(h));
			// if the explicit has not ancestor the implicit and they are not siblings
			if (implTerm.hasAncestor(fc, h)&&!areSiblings) {
				printWarning(WarningEvent.BR16, fc.getCode(), false, stdOut);
				break;
			}
		}
		
	}
	
	/**
	 * BR17
	 * check if the base term is a facet
	 * 
	 * @param bt
	 * @param stdOut
	 */
	private void isFacet(Term bt, boolean stdOut) {
		if (bt.getTermType().getValue().equals("f")) {
			printWarning(WarningEvent.BR17, bt.getCode(), false, stdOut);
		}
	}
	
	/**
	 * BR19
	 * Raise a warning if the user select a raw commodity and
	 * uses the describe function to create a derivative which is already present in
	 * the main list. In particular, the following code checks if the term belongs
	 * to one of the warn groups. If this is the case, it checks if any of the
	 * processes, which are added to the base term, generate a derivative term which
	 * is already present in the main list. 
	 * ONLY FOR RAW COMMODITIES
	 * 
	 * @param bt
	 * @param fcIndex
	 * @param fcCode
	 */
	protected void checkFpForRawCommodity(Term bt, String fcIndex, String fcCode, boolean stdOut) {

		// return if base term is not a raw commodity or if facet is not a process
		if (!isRawCommodityTerm(bt) || !isProcessFacet(fcIndex))
			return;

		// get the forbidden processes of the base term
		ArrayList<ForbiddenProcess> fps = getForbiddenProcesses(bt, forbiddenProcesses, stdOut);

		// get all the codes for the forbidden processes
		ArrayList<String> currentFPCodes = fps.stream().map(fp -> fp.getCode()).collect(Collectors.toCollection(ArrayList::new));

		// print warning if explicit facet is forbidden
		if (currentFPCodes != null && currentFPCodes.contains(fcCode))
			printWarning(WarningEvent.BR19, fcCode, false, stdOut);

	}

	/**
	 * BR20
	 * check if the base term is deprecated
	 * 
	 * @param bt
	 * @param stdOut
	 */
	private void isDeprecated(Term bt, boolean stdOut) {
		if (bt.isDeprecated()) {
			printWarning(WarningEvent.BR20, bt.getCode(), false, stdOut);
		}
	}

	/**
	 * BR21
	 * check if the base term is dismissed
	 * 
	 * @param bt
	 * @param stdOut
	 */
	private void isDismissed(Term bt, boolean stdOut) {
		if (bt.isDismissed(currentCat.getDefaultHierarchy()))
			printWarning(WarningEvent.BR21, bt.getCode(), false, stdOut);
	}
	
	/**
	 * BR23 - BR24
	 * Check if the base term is a hierarchy. If it is, rise a
	 * warning (discourage its use) Check also if the hierarchy is an exposure
	 * hierarchy or not and rise a warning if it is a non exposure hierarchy
	 * 
	 * @param bt
	 * @param stdOut
	 */
	protected void hierarchyAsBasetermCheck(Term bt, boolean stdOut) {
		// if the base term is a hierarchy
		if (bt.getDetailLevel().isHierarchyDetailLevel()) {
			// get the exposure hierarchy
			Hierarchy expHierarchy = currentCat.getHierarchyByCode("expo");
			if (bt.belongsToHierarchy(expHierarchy)) {
				// print the message related to the hierarchy as base term
				printWarning(WarningEvent.BR23, bt.getCode(), false, stdOut);
			} else {
				// print warning that you are using a non exposure hierarchy term
				printWarning(WarningEvent.BR24, bt.getCode(), false, stdOut);
			}
		}
	}
	
	/**
	 * BR26
	 * Check if more than one process with the same ordCode is chosen
	 * 
	 * @param bt
	 * @param impProcesses
	 * @param expProcesses
	 * @param stdOut
	 * @deprecated
	 */
	protected void mutuallyExclusiveCheck(Term bt, ArrayList<ForbiddenProcess> impProcesses,
			ArrayList<ForbiddenProcess> expProcesses, boolean stdOut) {

		// Return if null parameters
		if (expProcesses == null || impProcesses == null)
			return;

		// if no explicit process or basterm not derivative return (the implicit processes
		// alone do not have to be checked)
		if (expProcesses.isEmpty() || !isDerivativeTerm(bt))
			return;

		// create a unique list of processes (used later)
		ArrayList<ForbiddenProcess> allProcess = new ArrayList<>();
		allProcess.addAll(expProcesses);
		allProcess.addAll(impProcesses);

		// get all the ordCodes starting from the forbidden processes
		ArrayList<Double> ordCodes = new ArrayList<>();

		// the 0 items are treated separately, in particular
		// we can use different 0 items together without
		// incurring in the mutually exclusive problem
		// therefore we exclude them from this check
		for (ForbiddenProcess proc : expProcesses) {
			double ordCode = proc.getOrdCode();
			if ( ordCode != 0)
				ordCodes.add(ordCode);
			else
				// if it is 0 remove from the all process
				allProcess.remove(proc); 
		}
		
		for (ForbiddenProcess proc : impProcesses) {
			double ordCode = proc.getOrdCode();
			if (ordCode != 0)
				ordCodes.add(ordCode);
			else
				// if it is 0 remove from the all process
				allProcess.remove(proc); 
		}
		
		// get all the distinct ord codes and see if there are duplicates
		// if this is the case print a warning! You have violated the mutually exclusive
		// property
		// the case in which only implicit facet are present was already managed at the
		// beginning
		// of the function
		Set<Double> uniqueOrdCodes = new HashSet<Double>(ordCodes);

		// for each ord code in the unique set of ordCodes of the selected processes
		for (double ordCode : uniqueOrdCodes) {

			// Are there any duplicated ord code? If yes => warning! you can use only one
			// process
			// with this ord code
			if (Collections.frequency(ordCodes, ordCode) > 1) {

				// get the processes codes and attach them into the warning message
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < ordCodes.size(); i++) {

					// if the process owns the same ord code add its code to the warning string
					if (ordCodes.get(i) == ordCode)
						sb.append(allProcess.get(i).getCode());

					// unless the last process code is appended, add a - to separate the processes
					// codes
					if (i < ordCodes.size() - 1) {
						sb.append(" - ");
					}
				}

				// print the warning
				printWarning(WarningEvent.BR26, sb.toString(), false, stdOut);
			}
		}
	}

	/**
	 * BR27
	 * Check if processes with ordCode x.1, x.2, x.3 ... are
	 * added to the baseterm. If multiple processes of this type with the same
	 * integer part are added, then rise a warning! These processes create another
	 * derivative which is already present in the list. Also the implicit processes
	 * are taken into account. NOTE: if only implicit facets have these properties,
	 * then no warning are risen.
	 * 
	 * @TODO this method and its sub modules has to be optimized
	 * 
	 * @param bt
	 * @param implicitProcesses
	 * @param explicitProcesses
	 * @param stdOut
	 */
	protected void decimalOrderCheck(Term bt, ArrayList<ForbiddenProcess> implicitProcesses,
			ArrayList<ForbiddenProcess> explicitProcesses, boolean stdOut) {

		// only check for derivatives
		if (!isDerivativeTerm(bt))
			return;

		// if there are not implicit or explicit processes
		if (implicitProcesses == null || explicitProcesses == null)
			return;

		// get the ord codes of the processes with decimal ord code
		ArrayList<Double> decimalImplicitOrdCodes = getDecimalOrdCodes(implicitProcesses);
		ArrayList<Double> decimalExplicitOrdCodes = getDecimalOrdCodes(explicitProcesses);

		// get the ord codes of the processes with decimal ord code
		ArrayList<ForbiddenProcess> decimalImplicitProcesses = getDecimalProcesses(implicitProcesses);
		ArrayList<ForbiddenProcess> decimalExplicitProcesses = getDecimalProcesses(explicitProcesses);

		// all the element to retrieve the maximum integer
		ArrayList<Double> decimalAllOrdcodes = new ArrayList<>();
		decimalAllOrdcodes.addAll(decimalExplicitOrdCodes);
		decimalAllOrdcodes.addAll(decimalImplicitOrdCodes);

		// get the max integer of the two arrays
		double maxValue = 0;
		for (double d : decimalAllOrdcodes) {
			int integerPart = getNumberPart(d, true);
			if (integerPart > maxValue) {
				maxValue = integerPart;
			}
		}

		// for each integer ( 1,2,3...), check the fract part
		for (int i = 1; i <= maxValue; i++) {

			// array list to save the forbidden processes which have ordcodes with the
			// integer part = i
			ArrayList<ForbiddenProcess> imp = new ArrayList<>();
			ArrayList<ForbiddenProcess> exp = new ArrayList<>();

			// array list to save the processes fractional part of the ord codes which have
			// the integer part = i
			ArrayList<Integer> impOrd = new ArrayList<>(); // for implicit processes
			ArrayList<Integer> expOrd = new ArrayList<>(); // for explicit processes

			// for implicit processes
			for (int j = 0; j < decimalImplicitOrdCodes.size(); j++) {

				// if the integer part is equal to i
				if (getNumberPart(decimalImplicitOrdCodes.get(j), true) == i) {

					// get the fract part and add it to the implicit ord codes
					impOrd.add(getNumberPart(decimalImplicitOrdCodes.get(j), false));

					// add the process
					imp.add(decimalImplicitProcesses.get(j));
				}
			}

			// for explicit processes
			for (int j = 0; j < decimalExplicitOrdCodes.size(); j++) {

				// if the integer part is equal to i
				if (getNumberPart(decimalExplicitOrdCodes.get(j), true) == i) {

					// get the fract part and add it to the explicit ord codes
					expOrd.add(getNumberPart(decimalExplicitOrdCodes.get(j), false));

					// add the process
					exp.add(decimalExplicitProcesses.get(j));
				}
			}

			// get only the distinct ordodes of implicit and explicit processes
			Set<Integer> uniqueExpl = new HashSet<Integer>(expOrd);
			Set<Integer> uniqueImpl = new HashSet<Integer>(impOrd);

			// take distinc values of all ordCodes (implicit and explicit) 
			ArrayList<Integer> allOrdCodes = new ArrayList<>();
			allOrdCodes.addAll(uniqueExpl);
			allOrdCodes.addAll(uniqueImpl);
			Set<Integer> uniqueAllCodes = new HashSet<Integer>(allOrdCodes);

			// if at least one facet is implicit and one is explicit and they have a
			// different ord code => warning
			// we check if they have a different ord code because in that case the mutually
			// exclusive warning is risen
			// if no facets are implicit but the user add more than one explicit => warning

			boolean firstCheck = (uniqueImpl.size() > 0 && uniqueExpl.size() > 0);
			boolean secondCheck = (uniqueImpl.size() == 0 && uniqueExpl.size() > 1);

			if ((firstCheck || secondCheck) && uniqueAllCodes.size() > 1) {

				// get all the codes of the processes involved
				StringBuilder sb = new StringBuilder();

				// in the exp processes with ord code with decimal and integer part = i
				for (int j = 0; j < decimalExplicitProcesses.size(); j++) {
					sb.append(decimalExplicitProcesses.get(j).getCode());
					if (j < decimalExplicitProcesses.size() - 1 && decimalExplicitProcesses.size() > 0)
						sb.append(" - ");
				}

				// in the imp processes with ord code with decimal and integer part = i
				for (int j = 0; j < decimalImplicitProcesses.size(); j++) {
					sb.append(decimalImplicitProcesses.get(j).getCode());
					if (j < decimalImplicitProcesses.size() - 1 && decimalImplicitProcesses.size() > 0)
						sb.append(" - ");
				}

				// warning: these processes generate a derivative which is already existing
				printWarning(WarningEvent.BR27, sb.toString(), false, stdOut);
			}
		}
	}

	/**
	 * BR28
	 * Check if a reconstitution process facet is added to concentrate/dehydrated
	 * term
	 * 
	 * @param bt
	 * @param facetIndex
	 * @param facetCode
	 */
	private void reconstitutionCheck(Term bt, String facetIndex, String facetCode, boolean stdOut) {
		
		// if the explicit facet is a process and the baseterm is concentrate or powder
		if(isProcessFacet(facetIndex) && isConcOrPowdTerm(bt)) {
			// if the explicit facet is reconstitution(A07MR) or dilution(A07MQ) 
			if (facetCode.equals("A07MR") || facetCode.equals("A07MQ"))
				printWarning(WarningEvent.BR28, facetCode, false, stdOut);
		}
	}


	// ######### TERM/FACET BOOLEAN CHECKS ##############

	/**
	 * check if the base term is concentrate or powder
	 * 
	 * @param term
	 * @return
	 */
	private boolean isConcOrPowdTerm(Term term) {

		String conc = "concentrate", powd = "powder", termName = term.getName(), termNote = term.getScopenotes();

		boolean concentrate = (termName.contains(conc) || termNote.contains(conc));
		boolean powder = (termName.contains(powd) || termNote.contains(powd));

		return (concentrate || powder);
	}

	/**
	 * check if the term is a flavored term
	 * 
	 * @param baseTerm
	 * @return
	 */
	private boolean isFlavoured(Term baseTerm) {
		return baseTerm.getName().toLowerCase().contains("flavoured");
	}

	/**
	 * Check if the term is a raw commodity
	 * 
	 * @param term
	 * @return
	 */
	private boolean isRawCommodityTerm(Term term) {
		return (term.getTermType().getValue().equals("r"));
	}

	/**
	 * check if the term is a derivative
	 * 
	 * @param term
	 * @return
	 */
	private boolean isDerivativeTerm(Term term) {
		return (term.getTermType().getValue().equals("d"));
	}

	/**
	 * Check if the term is a non-specific term
	 * 
	 * @param term
	 * @return
	 */
	private boolean isNonSpecificTerm(Term term) {
		return (term.getDetailLevel().getValue().equals("P"));
	}
	
	/**
	 * Check if the facet is a physical state facet
	 * 
	 * @param facetIndex
	 * @return
	 */
	private boolean isPhysicalStateFacet(String facetIndex) {
		return (facetIndex.equals("F03"));
	}

	/**
	 * Check if the facet is a process facet
	 * 
	 * @param facetIndex
	 * @return
	 */
	private boolean isProcessFacet(String facetIndex) {
		return (facetIndex.equals("F28"));
	}

	/**
	 * check if the facet is an ingredient facet
	 * 
	 * @param facetIndex
	 * @return
	 */
	private boolean isIngredientFacet(String facetIndex) {
		return (facetIndex.equals("F04"));
	}

	/**
	 * Check if the process facet is the generic "processed" or one of its children
	 * 
	 * @param facetCode
	 * @return
	 */
	private boolean isGenericProcessFacet(Term facet) {
		return (facet.getCode().equals("A0C0R") || facet.getCode().equals("A0CHR") || facet.getCode().equals("A0CHS"));
	}
	
	/**
	 * check if forbidden physical state is applied to rpc
	 * 
	 * A06JD    Powder
	 * A07Y3    Coarse powder
	 * A07Y2    Fine powder
	 * A06JG    Puree-type
	 * A06JF    Paste
	 * A06JE    coarse paste / minced
	 * A07Y4    Fine paste
	 * 
	 * @param facetCode
	 * @return
	 */
	private boolean isForbiddenPhysicalState(String facetCode) {
		return (facetCode.equals("A06JD") || 
				facetCode.equals("A07Y3") ||
				facetCode.equals("A07Y2") ||
				facetCode.equals("A06JG") ||
				facetCode.equals("A06JF") ||
				facetCode.equals("A06JE") ||
				facetCode.equals("A07Y4"));
	}

	/**
	 * check if the facet is a source commodity facet
	 * 
	 * @param facetIndex
	 * @return
	 */
	protected boolean isSourceCommodityFacet(String facetIndex) {
		return (facetIndex.equals("F27"));
	}

	/**
	 * check if the facet is a source facet
	 * 
	 * @param facetIndex
	 * @return
	 */
	protected boolean isSourceFacet(String facetIndex) {
		return (facetIndex.equals("F01"));
	}

	/**
	 * Check if the term is a composite (simple or complex)
	 * 
	 * @param term
	 * @return
	 */
	protected boolean isCompositeTerm(Term term) {
		return (term.getTermType().getValue().equals("s") || term.getTermType().getValue().equals("c"));
	}

	/**
	 * Check if the selected term identified by the groupCode is one of the warn
	 * groups ( i.e. a group which could raise a warning defined in BR_Data.csv or
	 * BR_Exceptions.csv )
	 * 
	 * @param groupCode
	 * @return
	 */
	protected boolean isWarnGroup(String groupCode, ArrayList<ForbiddenProcess> forbiddenProcesses) {

		// check for security
		if (forbiddenProcesses == null)
			return false;

		// get all the warn groups codes
		ArrayList<String> warnGroupsCodes = new ArrayList<>();
		for (int i = 0; i < forbiddenProcesses.size(); i++) {
			warnGroupsCodes.add(forbiddenProcesses.get(i).getGroupCode());
		}

		// get the unique codes set (delete duplicates)
		Set<String> uniqueGroupsCodes = new HashSet<String>(warnGroupsCodes);

		// return true if the group is one of the warn groups
		return uniqueGroupsCodes.contains(groupCode);
	}

	/**
	 * Retrieve the ordCodes with decimal points starting from the forbidden processes
	 * 
	 * @param forbiddenProcesses
	 * @return
	 */
	private ArrayList<Double> getDecimalOrdCodes(ArrayList<ForbiddenProcess> forbiddenProcesses) {

		// if there is no forbidden process
		if (forbiddenProcesses == null)
			return null;

		// prepare output vector
		ArrayList<Double> decimalOrdCodes = new ArrayList<>();

		// search for decimal ord codes in forbidden processes
		for (ForbiddenProcess fp : forbiddenProcesses) {
			// get the ordCode of the process
			double currentOrdCode = fp.getOrdCode();
			// if the integer version of the ordCode lost a bit of information => it is a
			// decimal ordcode
			if (((int) currentOrdCode) != currentOrdCode)
				decimalOrdCodes.add(currentOrdCode);
		}

		return decimalOrdCodes;
	}

	/**
	 * Get the processes which has a decimal ord code
	 * 
	 * @param forbiddenProcesses
	 * @return
	 */
	private ArrayList<ForbiddenProcess> getDecimalProcesses(ArrayList<ForbiddenProcess> forbiddenProcesses) {

		// if there is no forbidden process
		if (forbiddenProcesses == null)
			return null;

		// prepare output vector
		ArrayList<ForbiddenProcess> decimalProcess = new ArrayList<>();

		// search for decimal ord codes in forbidden processes
		for (ForbiddenProcess fp : forbiddenProcesses) {
			// get the ordCode of the process
			double currentOrdCode = fp.getOrdCode();
			// if the integer version of the ord code lost a bit of information => it is a
			// decimal ordcode
			if (((int) currentOrdCode) != currentOrdCode)
				decimalProcess.add(fp);
		}

		return decimalProcess;
	}

	/**
	 * Get the integer part / decimal part of a double number
	 * 
	 * @param number
	 * @boolean integer if we want the integer part or the decimal part
	 * @return
	 */
	private int getNumberPart(double number, boolean integer) {

		String stringNumber = String.valueOf(number);

		// split the number on the decimal point
		String[] splits = stringNumber.split("\\.");

		// get the interested part
		int interestedPart;

		// get the integer part if requested, otherwise get the decimal part
		if (integer)
			interestedPart = Integer.parseInt(splits[0]);
		else
			interestedPart = Integer.parseInt(splits[1]);

		return (interestedPart);
	}

	/**
	 * Get the facet index and the facet code starting from a composite string which
	 * is like : F01.ADE0A
	 * 
	 * @param facetFullCode
	 * @return
	 */
	protected String[] splitFacetFullCode(String facetFullCode) {

		String[] split = facetFullCode.split("\\.");

		// the facet index
		String facetIndex = split[0];
		String facetCode = "";

		if (split.length > 1)
			facetCode = split[1];

		return (new String[] { facetIndex, facetCode });
	}

	/**
	 * Open the file filename and retrieve the forbidden processes for hierarchies
	 * the file must be a CSV file with 5 fields: baseTermGroupCode,
	 * baseTermGroupName, forbiddenProcessCode, forbiddenProcessName, ordCode
	 * 
	 * @param filename, the csv filename
	 * @return an array list of forbidden processes
	 */
	protected ArrayList<ForbiddenProcess> loadForbiddenProcesses(String filename) {

		try {
			// initialize the array of forbidden processes
			ArrayList<ForbiddenProcess> forbiddenProcesses = new ArrayList<>();

			// read the file
			BufferedReader bufferedReader = new BufferedReader(new FileReader(filename));

			// skip the header
			boolean skipFirstLine = true;

			// while there is a line to be red
			String line;
			while ((line = bufferedReader.readLine()) != null) {

				// Skip the header
				if (skipFirstLine) {
					skipFirstLine=false;
					continue;
				}

				// analyze the line tokens
				StringTokenizer st = new StringTokenizer(line, ";");

				// parse the string, get the base term group code
				String baseTermGroupCode = st.nextToken();

				// token related to the base term group name, it is useless for the checks
				st.nextToken();

				// get the process code related to the base term group
				String forbiddenProcessCode = st.nextToken();

				// token related to the forbidden process name, it is useless for the checks
				st.nextToken();

				// get the ordCode (ordCode could be also decimals useful for particular situations)
				double ordCode = Double.parseDouble(st.nextToken());
				
				// create the forbidden process with the retrieved information
				forbiddenProcesses.add(new ForbiddenProcess(baseTermGroupCode, forbiddenProcessCode, ordCode));

			}

			// close the connection and return the array
			bufferedReader.close();
			return forbiddenProcesses;

		} catch (Exception e) {
			// print error and return null if error occurred
			LOGGER.error(filename + " not found or parsing errors.", e);
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Open the file filename and retrieve the warning messages (ID and message) the
	 * file must be a CSV file with 3 fields: idMessage, description of the warning
	 * event, message
	 * 
	 * @param filename
	 * @return an array list of warning messages (the private class defined below)
	 */
	protected ArrayList<WarningMessage> loadWarningMessages(String filename) {
		try {

			File file = new File(filename);
			if (!file.exists())
				WarningMessage.createDefaultWarningMessagesFile(filename);

			ArrayList<WarningMessage> warningMessages = new ArrayList<>();

			// FileReader reads text files in the default encoding.
			FileReader fileReader = new FileReader(filename);

			// Always wrap FileReader in BufferedReader.
			BufferedReader bufferedReader = new BufferedReader(fileReader);

			// skip the header
			boolean skipFirstLine = true;

			// while there is a line to be red
			String line;
			while ((line = bufferedReader.readLine()) != null) {

				// Skip the header
				if (skipFirstLine) {
					skipFirstLine=false;
					continue;
				}

				// Analyze the line tokens
				StringTokenizer st = new StringTokenizer(line, ";");

				// parse the string, get the message id
				int messageId = Integer.parseInt(st.nextToken());

				// token related to the message description, it is useless
				st.nextToken();

				String message = st.nextToken();

				WarningLevel warningLevel;
				WarningLevel textWarningLevel;

				// get the warning level related to this message
				String warningLevelToken = st.nextToken();
				warningLevelToken = warningLevelToken.toLowerCase().replace(" ", "");

				// if the level is set to HIGH
				switch (warningLevelToken) {
				case "high":
					warningLevel = WarningLevel.HIGH;
					break;
				case "low":
					warningLevel = WarningLevel.LOW;
					break;
				case "none":
					warningLevel = WarningLevel.NONE;
					break;
				default:
					warningLevel = WarningLevel.ERROR;
				}

				// get the text warning level related to this message
				String textWarningLevelToken = st.nextToken();
				textWarningLevelToken = textWarningLevelToken.toLowerCase().replace(" ", "");

				// if the level is set to HIGH
				switch (textWarningLevelToken) {
				case "high":
					textWarningLevel = WarningLevel.HIGH;
					break;
				case "low":
					textWarningLevel = WarningLevel.LOW;
					break;
				case "none":
					textWarningLevel = WarningLevel.NONE;
					break;
				default:
					textWarningLevel = WarningLevel.ERROR;
				}

				// create a warning message with id and content 
				warningMessages.add(new WarningMessage(messageId, message, warningLevel, textWarningLevel));
			}

			// sort the warning messages using their ID
			Collections.sort(warningMessages, new Comparator<WarningMessage>() {
				@Override
				public int compare(WarningMessage wm2, WarningMessage wm1) {
					if (wm2.getId() > wm1.getId())
						return 1;
					else if (wm2.getId() < wm1.getId())
						return -1;
					else
						return 0;
				}
			});

			// Close the connection
			bufferedReader.close();
			return (warningMessages);

		} catch (Exception e) {
			LOGGER.error(filename + " not found.", e);
			e.printStackTrace();
			return null;
		}
	}

	
	/**
	 * Get the implicit forbidden processes of a term
	 * 
	 * @param term,               
	 * @param forbiddenProcesses
	 * @return
	 */
	protected ArrayList<ForbiddenProcess> getImplicitForbiddenProcesses(Term bt,
			ArrayList<ForbiddenProcess> forbiddenProcesses, boolean stdOut) {

		// initialize the output array
		ArrayList<ForbiddenProcess> implicitForbiddenProcesses = new ArrayList<>();

		// get the warn group of the term
		Term warnGroup = getWarnGroup(bt, stdOut);

		// if it is not a warn group => no forbidden processes are defined
		if (warnGroup == null)
			return implicitForbiddenProcesses;

		// get the full code of the term
		String fullCode = bt.getFullCode(true, true);

		// if there are not facets, return (it is only the base term)
		if (fullCode.split("#").length < 2)
			return implicitForbiddenProcesses;

		// parse the facet codes
		StringTokenizer st = new StringTokenizer(fullCode.split("#")[1], "$");

		// iterate the facets
		while (st.hasMoreTokens()) {
			// get the entire
			String fullFacetCode = st.nextToken();
			// split the facet code
			String[] codeSplit = fullFacetCode.split("\\.");
			// retrieve the interesting information
			String implicitFacetIndex = codeSplit[0];
			String implicitFacetCode = codeSplit[1];

			// if the facet is a process
			if (implicitFacetIndex.equals("F28")) {
				// iterate the forbidden processes of the term
				for (ForbiddenProcess proc : getForbiddenProcesses(bt, forbiddenProcesses, stdOut)) {
					// if there is an implicit facet which is forbidden then add it
					if (proc.getCode().equals(implicitFacetCode))
						implicitForbiddenProcesses.add(proc);
				}
			}
		}

		return implicitForbiddenProcesses;
	}

	/**
	 * This method retrieves the forbidden processes related to a single warn group,
	 * which is given as input
	 * 
	 * @param bt
	 * @param fps
	 * @param stdOut
	 * @return
	 */
	private ArrayList<ForbiddenProcess> getForbiddenProcesses(Term bt,
			ArrayList<ForbiddenProcess> fps, boolean stdOut) {

		// get the warnGroup related to the chosen base term
		Term warnGroup = getWarnGroup(bt, stdOut);

		// return if the term is not a warn group or if there are no forbidden processes
		if (fps == null || warnGroup == null)
			return null;

		// output array
		ArrayList<ForbiddenProcess> currentFP = new ArrayList<>();

		// for each forbidden process
		for (ForbiddenProcess fp : fps) {
			// if the warn group related to the current forbidden process is actually the warn group of the baseTerm
			if (fp.getGroupCode().equals(warnGroup.getCode()))
				// add the related process to the forbidden processes
				currentFP.add(fp);
		}

		return currentFP;
	}

	/**
	 * Check if the selected term belongs to a warn group ( i.e. a group which could
	 * raise a warning defined in BR_Data.csv)
	 * 
	 * @param bt
	 * @param stdOut
	 * @return
	 */
	private Term getWarnGroup(Term bt, boolean stdOut) {

		// start to go up in the tree, parent by parent
		while (bt != null) {

			// if the parent is a warn group => break cycle and return the warn group
			if (isWarnGroup(bt.getCode(), forbiddenProcesses))
				return (bt);

			// get the parent of the current term and continue the loop, we use the
			// reporting hierarchy for warnings
			bt = bt.getParent(currentCat.getHierarchyByCode("report"));
		}

		// if no warn group is discovered, then return null
		return null;
	}

	/**
	 * Print the warning messages
	 * 
	 * @param event
	 * @param postMessage
	 * @param dateTime
	 * @param stdOut
	 */
	protected abstract void printWarning(WarningEvent event, String postMessage, boolean dateTime, boolean stdOut);
	
	/**
	 * Check if high warnings are present
	 */
	protected abstract boolean highWarningsPresent();
	
	
	/**
	 * get the warning level of the semaphore
	 * 
	 * @param event
	 * @return
	 */
	protected WarningLevel getSemaphoreLevel(WarningEvent event) {
		return warningMessages.get(event.ordinal()).getWarningLevel();
	}

	/**
	 * get the warning level of the text message
	 * 
	 * @param event
	 * @return
	 */
	protected WarningLevel getTextLevel(WarningEvent event) {
		return warningMessages.get(event.ordinal()).getTextWarningLevel();
	}

	/**
	 * Create the message string to be printed into the console (or to be used in
	 * excel files for macros)
	 * 
	 * @param event
	 * @param postMessageString, string between brackets after the warning
	 * @param attachDatetime
	 * @return
	 */
	protected String createMessage(WarningEvent event, String postMessageString, boolean attachDatetime) {

		// get the message from the array list of warning messages
		// ( it uses the eventID to retrieve the related message which has as ID the
		// same as the eventID,
		// we can do this thanks to the pre-sorting action made when the messages are
		// loaded

		String message = warningMessages.get(event.ordinal()).getMessage();

		// attach title
		if (postMessageString != null && !postMessageString.equals(""))
			message = message + "(" + postMessageString + ") ";

		// get the warning level from the message
		// WarningLevel warningLevel = warningMessages.get( event.ordinal()
		// ).getWarningLevel();

		// if we want the date time in the message
		if (attachDatetime) {
			// append the date time to the message
			DateFormat dateFormat = new SimpleDateFormat("HH:mm, yyyy/MM/dd");
			Date date = new Date();
			message = message + " (time: " + dateFormat.format(date) + ")";
		}

		return (message);
	}

	/**
	 * Given a full code, perform all the implemented checks
	 * 
	 * @param fullCode
	 * @param stdOut   should be the warnings messages printed in the stdOut? used
	 *                 with batch checking tool
	 * @param fromICT
	 */
	protected void performWarningChecks(String fullCode, boolean stdOut, boolean fromICT) {
		
		// force code to upper case
		fullCode = fullCode.toUpperCase();
		
		////////////////// RETRIEVE BASE TERM FROM FULL CODE
		
		// split the full code in order to get the base term code and the facets
		String[] splits = fullCode.split("#");
		boolean onlybt = splits.length < 2;
		
		// get the base term code (the first part of the full code)
		String baseTermCode = splits[0];

		TermDAO termDao = new TermDAO(currentCat);

		Term baseTerm = termDao.getByCode(baseTermCode);

		// if the base term is not in the database
		if (baseTerm == null) {
			printWarning(WarningEvent.BR29, baseTermCode, false, stdOut);
			return;
		}

		////////////////// MAKE SOME WARNING CHECKS AND WARN THE USER IF NECESSARY

		// check if term is not re-portable in dft hierarchy
		isNotReportable(baseTerm, stdOut);
		
		// check if an non specific base term is selected
		if(onlybt) {
			nonSpecificTermCheck(baseTerm, "", stdOut);
		}
		
		// check if base term type is f
		isFacet(baseTerm, stdOut);
		
		// check if the base term is deprecated
		isDeprecated(baseTerm, stdOut);

		// check if the base term is dismissed
		isDismissed(baseTerm, stdOut);
		
		// check if the base term is a hierarchy or not
		hierarchyAsBasetermCheck(baseTerm, stdOut);
		
		// print successful added bt when high warnings are not present
		if (!fromICT && !highWarningsPresent()) {
			printWarning(WarningEvent.BR22, baseTermCode, false, stdOut);
		}
		
		// return if there is nothing else to parse (i.e. no facets)
		if (onlybt) {
			return;
		}
		
		// get the warnGroup related to the chosen base term
		// that is, the father which is subjected to restrictions
		// in term of applicability of processes (they are defined in the BR_Data.csv)
		boolean warnGroup = getWarnGroup(baseTerm, stdOut) != null;

		// Contains at position i how many processes with ordCode = i
		// are added by the user in an explicit way
		// used to check the mutually exclusive property
		ArrayList<ForbiddenProcess> explicit = new ArrayList<>();

		// Here we takes the facet codes from the full code
		String fullFacetsCodes = splits[1];

		// implicit facets of the base term
		ArrayList<ForbiddenProcess> implicit = getImplicitForbiddenProcesses(baseTerm, forbiddenProcesses, stdOut);

		// tokenise the rest of the full code to get all the facets codes separately
		StringTokenizer st = new StringTokenizer(fullFacetsCodes, "$");

		// get all the facets codes parsing the fullFacetsCodes
		while (st.hasMoreTokens()) {

			// get the next facet code
			String facetFullcode = st.nextToken();

			// get the code of each facet (i.e. F01.A0FGM)
			// F01 = facet index, A0FGM = facet.
			String[] facetComponents = splitFacetFullCode(facetFullcode);

			// the facet index
			String facetIndex = facetComponents[0];

			// delete the first part of the code (i.e. the facet index)
			String facetCode = facetComponents[1];

			// get the facet by code
			Term facet = termDao.getByCode(facetCode);

			// if the facet is not present into the database return (for excel macro)
			if (facet == null) {
				printWarning(WarningEvent.BR29, facetCode, false, stdOut);
				return;
			}

			// only if running from ict check if facet belongs to facet category
			if (fromICT) {
				// get the facet category
				Attribute facetCategory = currentCat.getAttributeByCode(facetIndex);
				if (facetCategory == null) {
					// if the facet category does not exists
					printWarning(WarningEvent.BR30, facetIndex, false, stdOut);
				} else if(!facet.belongsToHierarchy(facetCategory.getHierarchy())) {
					// if facet does not belong to facet category
					printWarning(WarningEvent.BR31, facetCode, false, stdOut);
				}
			}

			if (warnGroup) {
				// check if a forbidden process is used or raw commodities
				checkFpForRawCommodity(baseTerm, facetIndex, facetCode, stdOut);
				// check if the order of processes is violated for derivatives
				checkIfExplicitLessDetailed(baseTerm, facetIndex, facet, stdOut);
			}

			// VALID ONLY FOR BT: check if term is not re-portable in default hierarchy
			// isNotReportable(facet, stdOut);
			
			// check if an non specific base term is selected
			nonSpecificTermCheck(baseTerm, facetIndex, stdOut);
			
			// check if the generic process facet is selected
			genericProcessedFacetCheck(facet, stdOut);

			// check if the user added an ingredient to a raw commodity or to a derivative
			minorIngredientCheck(baseTerm, facetIndex, facet, stdOut);

			// check if a source is added to a composite term
			sourceInCompositeCheck(baseTerm, facetIndex, facetCode, stdOut);

			// check if a source commodity is added to a composite term
			sourceCommodityInCompositeCheck(baseTerm, facetIndex, facetCode, stdOut);

			// check if reconstitution process is added to concentrate or powder terms
			reconstitutionCheck(baseTerm, facetIndex, facetCode, stdOut);

			// check if forbidden physical state facet is added to rpc
			physicalStateRawCheck(baseTerm, facetIndex, facetCode, stdOut);
			
			// if it is indeed a warn group
			if (warnGroup) {

				// get all the forbidden processes related to the base term
				// (defined in the BR_Data.csv or BR_exceptions.csv)
				ArrayList<ForbiddenProcess> currentFP = getForbiddenProcesses(baseTerm, forbiddenProcesses, stdOut);

				// get the forbidden processes codes (NOT ord code!) related to the base term
				ArrayList<String> currentFPCodes = new ArrayList<>();

				for (ForbiddenProcess proc : currentFP)
					currentFPCodes.add(proc.getCode());

				// if the process just added is present into the current forbidden processes
				if (currentFPCodes.contains(facetCode)) {

					// get the position in the array list
					int index = currentFPCodes.indexOf(facetCode);

					boolean isAncestor = false;

					// check if the explicit process is a descendant of some implicit facet
					for (ForbiddenProcess proc : implicit) {

						// get the facet terms related to the forbidden processes codes
						Term ancestor = termDao.getByCode(proc.getCode());
						Term descendant = termDao.getByCode(currentFP.get(index).getCode());

						// if the added process is a son of one of the implicit process
						// add it but remove the implicit, in order to ignore it
						if (descendant.hasAncestor(ancestor, currentCat.getHierarchyByCode("process"))) {
							isAncestor = true;
							// add since we want to check only the forbidden processes mutually exclusivity
							explicit.add(currentFP.get(index));
							// remove the implicit
							implicit.remove(proc);
							break;
						}
					}

					// if no relation => add the process without taking care of implicit processes
					if (!isAncestor)
						explicit.add(currentFP.get(index));
				}
			}
		}

		// check if the user added only one source commodity to a raw commodity
		sourceCommodityRawCheck(baseTerm, fullFacetsCodes, stdOut);

		// check if the user added more than one source commodity to a derivative or if
		// he added a source to a derivative and only one source commodity
		sourceCommodityDerivativeCheck(baseTerm, fullFacetsCodes, stdOut);

		// check if decimal order of the processes ordCodes is correctly applied
		if (warnGroup) {
			// check ord code are correct (no implicit ord code >= explicit ord codes)
			decimalOrderCheck(baseTerm, implicit, explicit, stdOut);
			// check if the mutually exclusive property is violated
			// mutuallyExclusiveCheck(baseTerm, explicit, implicit, stdOut);
		}
	}

}
