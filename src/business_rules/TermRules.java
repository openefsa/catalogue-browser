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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import catalogue.Catalogue;
import catalogue_browser_dao.TermDAO;
import catalogue_object.Attribute;
import catalogue_object.Hierarchy;
import catalogue_object.Term;
import catalogue_object.TermAttribute;
import naming_convention.Headers;
import ui_implicit_facet.DescriptorTreeItem;
import ui_implicit_facet.FacetDescriptor;
import ui_implicit_facet.FacetType;
import utilities.GlobalUtil;

/**
 * The TermRules class provide all the business rules applied to FoodEx2 terms
 * v.2.0 16-05-2019
 * 
 * @author shahaal
 *
 */
public abstract class TermRules {

	private static final Logger LOGGER = LogManager.getLogger(TermRules.class);

	protected Catalogue currentCat;

	// list of all the processes which may cause a warning
	protected ArrayList<ForbiddenProcess> forbiddenProcesses;

	// load into memory all the warning messages from the text file
	protected ArrayList<WarningMessage> warningMessages;

	// Enum type: it identifies the warning messages events (i.e. which message
	// should be shown in a determined event?)
	// the message id is given by the order of the event definition. So the message
	// with id 1 will be hierarchyBaseTerm etc...
	protected static enum WarningEvent {
		HierarchyBaseTerm, // when a hierarchy object is selected as base term
		BaseTermSuccessfullyAdded, // when a non-hierarchy object is selected as base term
		ForbiddenProcess, // when a forbidden process is chosen for the term
		WrongProcessOrder, // when a process is added to a derivative and the process ordcode is < than the
							// implicit ordcode
		ExceptionTermSelected, // when a term which is contained in the BR_Exceptions is selected for the
								// describe function (they are ambiguous)
		MutuallyExPropertyViolated, // when more than one process with the same ord code is chosen
		NoRepNoExpBaseTerm, // when a base term which does not belong to reporting or exposure hierarchy is
							// selected
		NoExpHierarchyTerm, // when a hierarchy is chosen as base term, and the hierarchy is not an exposure
							// hierarchy
		NonSpecificTerm, // when a non specific term is chosen to describe
		GenericProcessing, // when the generic facet "processed" is applied
		MinorIngredient, // when an ingredient is selected for raw commodities or derivatives
		SingleSourceCommodityToRaw, // when one single source commodity is added to raw commodities
		MixedDerivative, // when a source is added to a derivative which has two or more source
							// commodities
		SourceToDerivative, // when a source is added to a derivative and there is only one source commodity
		NoExposureTerm, // when a non exposure base term is selected
		SourceInComposite, // when a source is added to a composite term
		SourceCommodityInComposite, // when a source commodity is added to a composite term
		DecimalForbiddenProcess, // when more than 1 proc with decimal ord code is present (at least one explicit
									// should be present)
		NonGenericDerivativeUsed, // when a derivative with an implicit facet is used to describe mixed derivative
		SourceInDerivative, // if a source is added to a derivative without sourcecommodities
		Error, // if a code is wrongly structured
		ReconstitutionProduct; // when reconstitution is added to concentrate/powder base terms
	}

	/**
	 * Enum type for identifing the level of warnings NONE: no warning LOW: soft
	 * warning HIGH: hard warning ERROR: error warning
	 * 
	 * @author shahaal
	 *
	 */
	protected enum WarningLevel {
		NONE, LOW, HIGH, ERROR
	}

	/**
	 * FIRST WARNING CHECK Check if the base term is a hierarchy. If it is, rise a
	 * warning (discourage its use) Check also if the hierarchy is an exposure
	 * hierarchy or not and rise a warning if it is a non exposure hierarchy
	 */
	protected void hierarchyAsBasetermCheck(Term baseTerm, boolean stdOut) {

		// if the base term is a hierarchy
		if (baseTerm.getDetailLevel().isHierarchyDetailLevel()) {

			ArrayList<Hierarchy> hierarchies = baseTerm.getApplicableHierarchies();

			// if the term is not in the exposure hierarchy
			Hierarchy expHierarchy = currentCat.getHierarchyByCode("expo");

			if (hierarchies.contains(expHierarchy)) // print the message related to the hierarchy as base term
				printWarning(WarningEvent.HierarchyBaseTerm, baseTerm.getCode(), false, stdOut);
			else // print warning that you are using a non exposure hierarchy term
				printWarning(WarningEvent.NoExpHierarchyTerm, baseTerm.getCode(), false, stdOut);

			return;
		}

		// print the message base term successfully added if no warnings
		printWarning(WarningEvent.BaseTermSuccessfullyAdded, baseTerm.getCode(), false, stdOut);

	}

	/**
	 * SECOND WARNING CHECK Raise a warning if the user select a raw commodity and
	 * uses the describe function to create a derivative which is already present in
	 * the main list. In particular, the following code checks if the term belongs
	 * to one of the warn groups. If this is the case, it checks if any of the
	 * processes, which are added to the base term, generate a derivative term which
	 * is already present in the main list. ONLY FOR RAW COMMODITIES
	 * 
	 * @param baseTerm   the base term selected with the describe button
	 * @param facetIndex
	 * @param facetCode
	 */
	protected void forbiddenProcessForRawCommodityCheck(Term baseTerm, String facetIndex, String facetCode,
			boolean stdOut) {

		// return if the base term is not a raw commodity ( no check has to be done )
		// of if the facet is not a process
		if (!isRawCommodityTerm(baseTerm) || !isProcessFacet(facetIndex))
			return;

		ArrayList<String> currentFPCodes = new ArrayList<>();

		ArrayList<ForbiddenProcess> forbProcesses = getCurrentForbiddenProcesses(baseTerm, forbiddenProcesses, stdOut);

		// get all the forbidden processes codes (NOT ord code!)
		for (ForbiddenProcess proc : forbProcesses)
			currentFPCodes.add(proc.getForbiddenProcessCode());

		if (currentFPCodes != null && currentFPCodes.contains(facetCode))
			printWarning(WarningEvent.ForbiddenProcess, facetCode, false, stdOut);

	}

	/**
	 * THIRD WARNING CHECK Raise a warning if the user add to a derivative product
	 * an explicit process which owns a top down order (ordCode) which is less than
	 * the minimum of the ordCodes of the implicit processes facets (this is because
	 * we want to reflect the logic of processes applicability)
	 * 
	 * @param baseTerm
	 * @param facetIndex
	 * @param facetCode
	 */
	protected void forbiddenProcessesOrderForDerivativesCheck(Term baseTerm, String facetIndex, String facetCode,
			boolean stdOut) {

		// return if the base term is not a derivative ( no check has to be done )
		// return if the current facet is not a process
		if (!isDerivativeTerm(baseTerm) || !isProcessFacet(facetIndex))
			return;

		// get the forbidden processes related to the baseTerm
		ArrayList<ForbiddenProcess> currentFP = getCurrentForbiddenProcesses(baseTerm, forbiddenProcesses, stdOut);

		// get all the processes codes (NOT ord code!)
		ArrayList<String> currentFPCodes = new ArrayList<>();
		for (ForbiddenProcess proc : currentFP)
			currentFPCodes.add(proc.getForbiddenProcessCode());

		// get the implicit processes of the base term
		ArrayList<ForbiddenProcess> implicitProcesses = getImplicitForbiddenProcesses(baseTerm, forbiddenProcesses,
				stdOut);

		// get the minimum ord code of the implicit processes
		double minImplicitOrdCode = getMinOrdCode(implicitProcesses);

		// the position of the process facet which is just added by the user
		// return -1 if the element is not found
		int index = currentFPCodes.indexOf(facetCode);

		// if the process facet is found then check if its ord code is less than the
		// minimum implicit ord code
		if (currentFPCodes != null && index != -1) {

			// get the ordCode of the just applied process
			double currentOrdCode = currentFP.get(index).getOrdCode();

			// if the current ord code of the applied process is less than or equal to the
			// min ord code of the implicit processes
			// raise a warning
			if (currentOrdCode < minImplicitOrdCode) {
				printWarning(WarningEvent.WrongProcessOrder, facetCode, false, stdOut);
			}

		}
	}

	/**
	 * FOURTH WARNING CHECK Check if more than one process with the same ord code is
	 * chosen
	 * 
	 * @param addedProcesses the list of processes that were added to the base term
	 *                       by the user (with describe)
	 */
	protected void mutuallyExclusiveCheck(Term baseTerm, ArrayList<ForbiddenProcess> implicitProcesses,
			ArrayList<ForbiddenProcess> explicitProcesses, boolean stdOut) {

		// Return if null parameters
		if (explicitProcesses == null || implicitProcesses == null)
			return;

		// if no explicit process are present return! In fact, the implicit processes
		// alone
		// do not have to be checked
		// if the base term is not a derivative return, this check is only for
		// derivatives
		if (explicitProcesses.isEmpty() || !isDerivativeTerm(baseTerm))
			return;

		// create a unique list of processes (used later)
		ArrayList<ForbiddenProcess> allProcess = new ArrayList<>();
		allProcess.addAll(explicitProcesses);
		allProcess.addAll(implicitProcesses);

		// get all the ord codes starting from the forbidden processes
		// both explicit and implicit ones
		ArrayList<Double> ordCodes = new ArrayList<>();

		// the 0 items are treated separately, in particular
		// we can use different 0 items together without
		// incurring in the mutually exclusive problem
		// therefore we exclude them from this check

		for (ForbiddenProcess proc : explicitProcesses)
			if (proc.getOrdCode() != 0)
				ordCodes.add(proc.getOrdCode());
			else
				allProcess.remove(proc); // if it is 0 remove from the all process

		for (ForbiddenProcess proc : implicitProcesses)
			if (proc.getOrdCode() != 0)
				ordCodes.add(proc.getOrdCode());
			else
				allProcess.remove(proc); // if it is 0 remove from the all process

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
						sb.append(allProcess.get(i).getForbiddenProcessCode());

					// unless the last process code is appended, add a - to separate the processes
					// codes
					if (i < ordCodes.size() - 1) {
						sb.append(" - ");
					}
				}

				// print the warning
				printWarning(WarningEvent.MutuallyExPropertyViolated, sb.toString(), false, stdOut);
			}
		}
	}

	/**
	 * FIFTH WARNING CHECK Check if processes with ord code x.1, x.2, x.3 ... are
	 * added to the baseterm. If multiple processes of this type with the same
	 * integer part are added, then rise a warning! These processes create another
	 * derivative which is already present in the list. Also the implicit processes
	 * are taken into account. NOTE: if only implicit facets have these properties,
	 * then no warning are risen.
	 * 
	 * @param implicitProcesses
	 * @param explicitProcesses
	 */
	protected void decimalOrderCheck(Term baseTerm, ArrayList<ForbiddenProcess> implicitProcesses,
			ArrayList<ForbiddenProcess> explicitProcesses, boolean stdOut) {

		// only check for derivatives
		if (!isDerivativeTerm(baseTerm))
			return;

		// if there are not implicit or explicit processes
		if (implicitProcesses == null || explicitProcesses == null)
			return;

		/*
		 * HOW THIS CHECK WORKS: 1 - retrieve the processes (implicit and explicit ones)
		 * which have a decimal ord code 2 - compute the max integer part of these ord
		 * codes. For example if we have: 1.1, 1.2, 2.1, 3.1 the maximum integer part
		 * would be 3 (this is done to iterate over all the integer between 1 and the
		 * maximum) 3 - Iterate for each integer between 1 and the maximum integer: 1.
		 * Get all the processes (implicit and explicit ones) with the integer part
		 * equal to the current one 2. get the unique ord codes of the implicit and of
		 * the explicit processes 3. get all the unique ord codes taking account both
		 * implicit and explicit processes (in order to check if a warning has to be
		 * risen) 4. rise a warning if at least 2 fract ord code with the same integer
		 * part are added, and they are not only implicit facets (in fact, in this case
		 * the base term is a derivative which has both the processes and no warn has to
		 * be risen)
		 */

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

			// get only the distinct ord codes of implicit and explicit processes
			Set<Integer> uniqueExpl = new HashSet<Integer>(expOrd);
			Set<Integer> uniqueImpl = new HashSet<Integer>(impOrd);

			// get all the ord codes related to the processes involved (implicit and
			// explicit) and
			// take only the distinct ones
			ArrayList<Integer> allOrdCodes = new ArrayList<>();
			allOrdCodes.addAll(uniqueExpl);
			allOrdCodes.addAll(uniqueImpl);
			Set<Integer> uniqueAllCodes = new HashSet<Integer>(allOrdCodes);

			// if at least one facet is implicit and one is explicit and they have a
			// different ord code => warning
			// we check if they have a different ord code because in that case the mutually
			// exclusive warning is risen
			// if no facets are implicit but the user add more than one explicit => warning

			boolean flag = (impOrd.size() > 0 && expOrd.size() > 0);

			if (flag && uniqueAllCodes.size() > 1) {

				// get all the codes of the processes involved
				StringBuilder sb = new StringBuilder();

				// in the exp processes with ord code with decimal and integer part = i
				for (int j = 0; j < decimalExplicitProcesses.size(); j++) {
					sb.append(decimalExplicitProcesses.get(j).getForbiddenProcessCode());
					if (j < decimalExplicitProcesses.size() - 1 && decimalExplicitProcesses.size() > 0)
						sb.append(" - ");
				}

				// in the imp processes with ord code with decimal and integer part = i
				for (int j = 0; j < decimalImplicitProcesses.size(); j++) {
					sb.append(decimalImplicitProcesses.get(j).getForbiddenProcessCode());
					if (j < decimalImplicitProcesses.size() - 1 && decimalImplicitProcesses.size() > 0)
						sb.append(" - ");
				}

				// warning: these processes generate a derivative which is already existing
				printWarning(WarningEvent.DecimalForbiddenProcess, sb.toString(), false, stdOut);
			}
		}
	}

	/**
	 * SIXTH WARNING CHECK Check if the selected baseTerm belongs to the reporting
	 * hierarchy or to the exposure hierarchy. If not => warning you are using a non
	 * reportable term.
	 * 
	 * @param baseTerm
	 */
	protected void noRepNoExpHierarchyCheck(Term baseTerm, boolean stdOut) {

		// get all the applicable hierarchies for the selected base term
		ArrayList<Hierarchy> hierarchies = baseTerm.getApplicableHierarchies();

		Hierarchy reportingHierarchy = currentCat.getHierarchyByCode(Headers.REPORT);
		Hierarchy exposureHierarchy = currentCat.getHierarchyByCode(Headers.EXPO);

		// if the term does not belong to both reporting and exposure hierarchy =>
		// warning
		if (!hierarchies.contains(reportingHierarchy) && !hierarchies.contains(exposureHierarchy))
			printWarning(WarningEvent.NoRepNoExpBaseTerm, baseTerm.getCode(), false, stdOut);

	}

	/**
	 * SEVENTH CHECK Check if a non-specific term is selected
	 * 
	 * @param term
	 */
	protected void nonSpecificTermCheck(Term term, boolean stdOut) {

		// if the term is non-specific rise a warning (semaphore green, text yellow)
		if (isNonSpecificTerm(term)) {
			printWarning(WarningEvent.NonSpecificTerm, term.getCode(), false, stdOut);
		}
	}

	/**
	 * EIGHT CHECK Check if the "processed" facet is selected or not
	 * 
	 * @param facet
	 */
	protected void genericProcessedFacetCheck(Term facet, boolean stdOut) {

		// if the selected facet is "Processed", which is under generic process facets
		if (isGenericProcessFacet(facet)) {
			// rise a warning, could you be more precise?
			printWarning(WarningEvent.GenericProcessing, facet.getCode(), false, stdOut);
		}
	}

	/**
	 * NINTH CHECK Check if the user added an ingredient to a raw commodity or to a
	 * derivative
	 * 
	 * @param baseTerm
	 * @param facetIndex
	 * @param facetCode
	 */
	protected void minorIngredientCheck(Term baseTerm, String facetIndex, Term facet, boolean stdOut) {

		// if the base term is a raw commodity or a derivative
		if (isRawCommodityTerm(baseTerm) || isDerivativeTerm(baseTerm)) {

			// if the base term is not flavoured and the facet is an ingredient
			if (!isFlavoured(baseTerm) && isIngredientFacet(facetIndex)) {

				// get the ingredient facet category
				Attribute facetCategory = currentCat.getAttributeById(20);

				// if the implicit facet is parent of the explicit then don't print the warning
				if (facetCategory != null) {
					for (DescriptorTreeItem dti : baseTerm.getInheritedImplicitFacets(facetCategory)) {
						if (facet.hasAncestor(dti.getTerm(), facetCategory.getHierarchy()))
							return;
					}
				}

				// otherwise print the warning
				printWarning(WarningEvent.MinorIngredient, facet.getCode(), false, stdOut);
			}
		}
	}

	/**
	 * TENTH CHECK Check if the user added a source to a derivative or to a raw
	 * commodity
	 * 
	 * @param baseTerm
	 * @param facetIndex
	 * @param facetCode
	 */
	protected void sourceCommodityRawCheck(Term baseTerm, String fullFacetCode, boolean stdOut) {

		// if the base term is not a raw commodity no checks have to be done
		if (!isRawCommodityTerm(baseTerm))
			return;

		// count the source commodity
		int sourceCommodityFacetCount = 0;

		ArrayList<FacetDescriptor> implicitFacets = baseTerm.getFacets(true);

		TermDAO termDao = new TermDAO(currentCat);
		ArrayList<Term> implicitTerms = new ArrayList<>();

		// add implicit facets of the term
		for (FacetDescriptor fd : implicitFacets)
			implicitTerms.add(termDao.getByCode(fd.getFacetCode()));

		// populate the explicit facets
		ArrayList<FacetDescriptor> explicitFacets = new ArrayList<>();

		// tokenize the facets => the implicit are not considered since
		// raw commodities have their self as source commodity and should not
		// be taken into account
		StringTokenizer st = new StringTokenizer(fullFacetCode, "$");

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

			boolean isSpecification = false;

			// check if the explicit is specification of the implicit
			for (Term implicit : implicitTerms) {
				if (fd.getDescriptor().hasAncestor(implicit, hierarchy)) {
					isSpecification = true;
					break;
				}
			}

			// count the number of source commodities facets
			if (isSourceCommodityFacet(fd.getFacetHeader()) && !isSpecification) {
				sourceCommodityFacetCount += 1;
				sb.append(fd.getFacetCode());
				sb.append(" - ");
			}
		}

		// get all the involved terms
		String termsInvolved = sb.toString();

		// remove the last " - " if present (i.e. at least one source c. was added)
		if (sourceCommodityFacetCount > 0)
			termsInvolved = termsInvolved.substring(0, termsInvolved.length() - " - ".length());

		// print warning if only one SC; at least two are required
		if (sourceCommodityFacetCount == 1)
			printWarning(WarningEvent.SingleSourceCommodityToRaw, termsInvolved, false, stdOut);

	}

	/**
	 * ELEVENTH CHECK, check if a source is added to derivative. If a source
	 * commodity is already specified then the source must be used only to specify
	 * better the source commodity. If more than one source commodity is already
	 * specified, then a source cannot be used. Warnings are raised in the warning
	 * situations.
	 * 
	 * @param baseTerm
	 * @param fullFacetCode
	 */
	protected void sourceCommodityDerivativeCheck(Term baseTerm, String fullFacetCode, boolean stdOut) {

		// if the base term is not a derivative no checks have to be done
		if (!isDerivativeTerm(baseTerm))
			return;

		ArrayList<FacetDescriptor> implicitFacets = baseTerm.getFacets(true);
		ArrayList<Term> implicitTerms = new ArrayList<>();

		int implicitSourceCommCount, explicitSourceCommCount, explicitRestrictedSourceCommCount, sourceFacetCount;

		// initialise the counters
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
			else if (isSourceFacet(header))
				sourceFacetCount++;
			else
				continue;

			// append for diagnostic
			sb.append(fd.getFacetCode());
			sb.append(" - ");

		}

		// check explicit facets
		StringTokenizer st = new StringTokenizer(fullFacetCode, "$");

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
				if (!skip)
					explicitRestrictedSourceCommCount++;

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

		// if we have an implicit sc, an explicit sc and a source
		if (explicitRestrictedSourceCommCount > 0 && implicitSourceCommCount > 0)
			printWarning(WarningEvent.NonGenericDerivativeUsed, termsInvolved, false, stdOut);

		if (sourceFacetCount > 0) {

			// if source without source commodities
			if (totalSourceCommCount == 0)
				printWarning(WarningEvent.SourceInDerivative, termsInvolved, false, stdOut);

			// if more than two source commodities and one source are present => warning
			if (explicitSourceCommCount >= 2)
				printWarning(WarningEvent.MixedDerivative, termsInvolved, false, stdOut);

			// if one explicit SC is selected -> at least two are required
			if (explicitRestrictedSourceCommCount + implicitSourceCommCount == 1)
				printWarning(WarningEvent.SourceToDerivative, termsInvolved, false, stdOut);
		}
	}

	/**
	 * 12th CHECK Check if the term is in the exposure hierarchy and it is not a
	 * feed term. If not, rise a warning
	 * 
	 * @param baseTerm
	 */
	protected void exposureHierarchyCheck(Term baseTerm, boolean stdOut) {

		Hierarchy exposureHierarchy = currentCat.getHierarchyByCode("expo");

		// if the term is not in the exposure hierarchy and is not a feed
		if (!isFeedTerm(baseTerm) && !baseTerm.getApplicableHierarchies().contains(exposureHierarchy))
			printWarning(WarningEvent.NoExposureTerm, baseTerm.getCode(), false, stdOut);

	}

	// ######### TERM/FACET BOOLEAN CHECKS ##############

	/**
	 * check if the base term is concentrate or powder
	 * 
	 * @author shahaal
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
	 * check if the term is a flavoured term
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
	 * check if the term is a feed term
	 * 
	 * @param term
	 * @return
	 */
	private boolean isFeedTerm(Term term) {
		return (term.getName().contains("(feed)"));
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

	protected boolean isExceptionalTerm(Term term) {

		// load exceptions terms and their forbidden processes
		ArrayList<ForbiddenProcess> exceptionForbiddenProcesses = loadForbiddenProcesses(GlobalUtil.getBRExceptions());

		// if the base term is an exception then it is itself the warn group
		return (isWarnGroup(term.getCode(), exceptionForbiddenProcesses));
	}

	/**
	 * Check if the base term is an exceptional term
	 * 
	 * @param baseTerm
	 * @param stdOut
	 */
	protected void exceptionTermCheck(Term baseTerm, boolean stdOut) {
		// print warning message, ambiguous element selected
		if (isExceptionalTerm(baseTerm))
			printWarning(WarningEvent.ExceptionTermSelected, baseTerm.getCode(), false, stdOut);
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
			warnGroupsCodes.add(forbiddenProcesses.get(i).getBaseTermGroupCode());
		}

		// get the unique codes set (delete duplicates)
		Set<String> uniqueGroupsCodes = new HashSet<String>(warnGroupsCodes);

		// return true if the group is one of the warn groups
		return uniqueGroupsCodes.contains(groupCode);
	}

	/**
	 * Retrieve the minimum ord code contained in the array list of forbidden
	 * processes
	 * 
	 * @param forbiddenProcesses
	 * @return
	 */
	private double getMinOrdCode(ArrayList<ForbiddenProcess> forbiddenProcesses) {

		// if no processes
		if (forbiddenProcesses == null)
			return 0;

		// set the min to the maximum double value in order to be sure to update minimum
		// values correctly
		double minOrdCode = Double.MAX_VALUE;

		// get the minimum ordCode of the forbidden processes
		for (ForbiddenProcess proc : forbiddenProcesses) {
			if (proc.getOrdCode() < minOrdCode)
				minOrdCode = proc.getOrdCode(); // update the minimum ord code
		}

		// If no process is found => then set to 0 the minimum value
		if (minOrdCode == Double.MAX_VALUE)
			minOrdCode = 0;

		return minOrdCode;
	}

	/**
	 * Retrieve the ordCodes with decimal points starting from the forbidden
	 * processes
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

		// search for decimal ord codes
		for (int i = 0; i < forbiddenProcesses.size(); i++) {

			double currentOrdCode = forbiddenProcesses.get(i).getOrdCode();

			// if the integer version of the ord code lost a bit of information => it is a
			// decimal ordcode
			if (((int) currentOrdCode) != currentOrdCode)
				decimalOrdCodes.add(currentOrdCode);
		}

		return (decimalOrdCodes);
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

		// search for decimal ord codes
		for (int i = 0; i < forbiddenProcesses.size(); i++) {

			double currentOrdCode = forbiddenProcesses.get(i).getOrdCode();

			// if the integer version of the ord code lost a bit of information => it is a
			// decimal ordcode
			if (((int) currentOrdCode) != currentOrdCode)
				decimalProcess.add(forbiddenProcesses.get(i));
		}

		return (decimalProcess);
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

			ArrayList<ForbiddenProcess> forbiddenProcesses = new ArrayList<>();

			// FileReader reads text files in the default encoding.
			FileReader fileReader = new FileReader(filename);

			// Always wrap FileReader in BufferedReader.
			BufferedReader bufferedReader = new BufferedReader(fileReader);

			String line;
			int lineCount = 0;

			// while there is a line to be red
			while ((line = bufferedReader.readLine()) != null) {

				// Skip the headers
				if (lineCount == 0) {
					lineCount++;
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

				// get the ord code (an ord code could be also with decimals to manage some
				// particular situations)
				try {
					// create the forbidden process with the retrieved information and
					// add it to the array list
					double ordCode = Double.parseDouble(st.nextToken());
					forbiddenProcesses.add(new ForbiddenProcess(baseTermGroupCode, forbiddenProcessCode, ordCode));
				} catch (Exception e) {
					LOGGER.error(" Error: no double format found in " + filename + "! line: " + lineCount, e);
				}

				// next line
				lineCount++;
			}

			// Close the connection
			bufferedReader.close();

			return (forbiddenProcesses);

		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("ERROR!\n" + e.getMessage());
			LOGGER.error(filename + " not found or parsing errors.", e);
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
			if (!file.exists()) {
				WarningMessage.createDefaultWarningMessagesFile(filename);
			}

			ArrayList<WarningMessage> warningMessages = new ArrayList<>();

			// FileReader reads text files in the default encoding.
			FileReader fileReader = new FileReader(filename);

			// Always wrap FileReader in BufferedReader.
			BufferedReader bufferedReader = new BufferedReader(fileReader);

			String line;
			int lineCount = 0;

			// while there is a line to be red
			while ((line = bufferedReader.readLine()) != null) {
				// skip headers
				if (lineCount == 0) {
					lineCount++;
					continue;
				}

				// analyze the line tokens
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

				// create a warning message object with the message id and the content of the
				// message (the next token)
				warningMessages.add(new WarningMessage(messageId, message, warningLevel, textWarningLevel));

				// new line
				lineCount++;
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
			return null;
		}
	}

	/**
	 * Check if a source is added to a composite food
	 * 
	 * @param baseTerm
	 * @param facetIndex
	 * @param facetCode
	 */
	private void sourceInCompositeCheck(Term baseTerm, String facetIndex, String facetCode, boolean stdOut) {

		// if a source is added to a composite rise a warning
		if (isCompositeTerm(baseTerm) && isSourceFacet(facetIndex))
			printWarning(WarningEvent.SourceInComposite, facetCode, false, stdOut);
	}

	/**
	 * Check if a source commodity is added to a composite food
	 * 
	 * @param baseTerm
	 * @param facetIndex
	 * @param facetCode
	 */
	private void sourceCommodityInCompositeCheck(Term baseTerm, String facetIndex, String facetCode, boolean stdOut) {

		// if a source commodity is added to a composite rise a warning
		if (isCompositeTerm(baseTerm) && isSourceCommodityFacet(facetIndex))
			printWarning(WarningEvent.SourceCommodityInComposite, facetCode, false, stdOut);
	}

	/**
	 * Check if a reconstitution process facet is added to concentrate/dehydrated
	 * term
	 * 
	 * @author shahaal
	 * @param baseTerm
	 * @param facetIndex
	 * @param facetCode
	 */
	private void reconstitutionCheck(Term baseTerm, String facetIndex, String facetCode, boolean stdOut) {

		// if reconstitution(A07MR) or dilution(A07MQ) facet
		boolean isRecoOrDilu = facetCode.equals("A07MR") || facetCode.equals("A07MQ");
		// added as process to baseterm that is concentrate/dehydrated
		if (isRecoOrDilu && isProcessFacet(facetIndex) && isConcOrPowdTerm(baseTerm))
			printWarning(WarningEvent.ReconstitutionProduct, facetCode, false, stdOut);

	}

	/**
	 * Get the implicit forbidden processes of a term
	 * 
	 * @param term,               the term to analyze
	 * @param forbiddenProcesses, the processes which are considered as forbidden
	 * @return
	 */
	protected ArrayList<ForbiddenProcess> getImplicitForbiddenProcesses(Term term,
			ArrayList<ForbiddenProcess> forbiddenProcesses, boolean stdOut) {

		// initialise the output array
		ArrayList<ForbiddenProcess> implicitForbiddenProcesses = new ArrayList<>();

		// get the warn group of the term
		Term warnGroup = getWarnGroup(term, stdOut);

		// if it is not a warn group => no forbidden processes are defined
		if (warnGroup == null)
			return implicitForbiddenProcesses;

		// get the full code of the term
		String fullCode = term.getFullCode(true, true);

		// if there are not facets, return (it is only the base term)
		if (fullCode.split("#").length < 2)
			return implicitForbiddenProcesses;

		// parse the facet codes
		StringTokenizer st = new StringTokenizer(fullCode.split("#")[1], "$");

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
				for (ForbiddenProcess proc : getCurrentForbiddenProcesses(term, forbiddenProcesses, stdOut)) {
					// if a dangerous process, insert it in the array
					if (proc.getForbiddenProcessCode().equals(implicitFacetCode))
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
	 * @param term,               the warnGroup
	 * @param forbiddenProcesses, the considered forbidden processes
	 * @return
	 */
	private ArrayList<ForbiddenProcess> getCurrentForbiddenProcesses(Term baseTerm,
			ArrayList<ForbiddenProcess> forbiddenProcesses, boolean stdOut) {

		// get the warnGroup related to the chosen base term
		Term warnGroup = getWarnGroup(baseTerm, stdOut);

		// return if the term is not a warn group or if there are no forbidden processes
		if (forbiddenProcesses == null || warnGroup == null)
			return null;

		// output array
		ArrayList<ForbiddenProcess> currentFP = new ArrayList<>();

		// for each forbidden process
		for (int i = 0; i < forbiddenProcesses.size(); i++) {

			// if the warn group term related to the current forbidden process is actually
			// the warn group of the baseTerm
			if (forbiddenProcesses.get(i).getBaseTermGroupCode().equals(warnGroup.getCode())) {

				// add the related process to the forbidden processes
				currentFP.add(forbiddenProcesses.get(i));
			}
		}

		return (currentFP);
	}

	/**
	 * Check if the selected term belongs to a warn group ( i.e. a group which could
	 * raise a warning defined in BR_Data.csv or BR_Exceptions.csv )
	 * 
	 * @param baseTerm
	 * @return the parent which is a warnGroup, otherwise null
	 * @throws InterruptedException
	 */
	private Term getWarnGroup(Term baseTerm, boolean stdOut) {

		// for exceptional terms get exceptions business rules
		if (isExceptionalTerm(baseTerm)) {

			// load exceptions terms and their forbidden processes
			forbiddenProcesses = loadForbiddenProcesses(GlobalUtil.getBRExceptions());

			return baseTerm;
		}

		// set the start element for the parent search
		Term parent = baseTerm;

		// start to go up in the tree, parent by parent
		while (parent != null) {

			// if the parent is a warn group => break cycle and return the warn group
			if (isWarnGroup(parent.getCode(), forbiddenProcesses))
				return (parent);

			// get the parent of the current term and continue the loop, we use the
			// reporting hierarchy for warnings
			parent = parent.getParent(currentCat.getHierarchyByCode("report"));
		}

		// if no warn group is discovered, then return null
		return null;
	}

	/**
	 * Print the warning messages
	 * 
	 * @param event
	 * @param postMessageString
	 * @param attachDatetime
	 * @param stdOut
	 */
	protected abstract void printWarning(WarningEvent event, String postMessageString, boolean attachDatetime,
			boolean stdOut);

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
	 * @param postMessageString
	 * @param attachDatetime
	 * @return
	 */
	/**
	 * Create the message string to be printed into the console (or to be used in
	 * excel files for macros)
	 * 
	 * @param event
	 * @param postMessageString, string to be printed between brackets after the
	 *                           warning
	 * @param attachDatetime
	 * @return
	 */
	protected String createMessage(WarningEvent event, String postMessageString, boolean attachDatetime) {

		// get the message from the arraylist of warning messages
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

		////////////////// RETRIEVE BASE TERM FROM FULL CODE

		// split the full code in order to get the base term code and the facets
		String[] splits = fullCode.split("#");

		// get the base term code (the first part of the full code)
		String baseTermCode = splits[0];

		TermDAO termDao = new TermDAO(currentCat);

		Term baseTerm = termDao.getByCode(baseTermCode);

		// if the base term is not in the database
		if (baseTerm == null) {
			printWarning(WarningEvent.Error, baseTermCode, false, stdOut);
			return;
		}

		////////////////// MAKE SOME WARNING CHECKS AND WARN THE USER IF NECESSARY

		// check if the base term is a hierarchy or not
		hierarchyAsBasetermCheck(baseTerm, stdOut);

		// check if the baseTerm belongs to the reporting or the exposure hierarchy
		noRepNoExpHierarchyCheck(baseTerm, stdOut);

		// check if the base term belongs to the exposure hierarchy or not
		exposureHierarchyCheck(baseTerm, stdOut);

		// check if an non specific base term is selected
		nonSpecificTermCheck(baseTerm, stdOut);

		// check if an exceptional term is selected as base term
		exceptionTermCheck(baseTerm, stdOut);

		// return if there is nothing else to parse (i.e. no facets)
		if (splits.length < 2)
			return;

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
				printWarning(WarningEvent.Error, facetCode, false, stdOut);
				return;
			}

			// only if running from ict check if facet belongs to facet category
			if (fromICT) {
				// get the facet category
				Attribute facetCategory = currentCat.getAttributeByCode(facetIndex);
				// if the facet doesn't belong to that category print warning
				if (!facet.belongsToHierarchy(facetCategory.getHierarchy()))
					printWarning(WarningEvent.Error, facetCode, false, stdOut);
			}

			if (warnGroup) {
				// check if a forbidden process is used or raw commodities
				forbiddenProcessForRawCommodityCheck(baseTerm, facetIndex, facetCode, stdOut);
				// check if the order of processes is violated for derivatives
				forbiddenProcessesOrderForDerivativesCheck(baseTerm, facetIndex, facetCode, stdOut);
			}

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

			// if it is indeed a warn group
			if (warnGroup) {

				// get all the forbidden processes related to the base term
				// (defined in the BR_Data.csv or BR_exceptions.csv)
				ArrayList<ForbiddenProcess> currentFP = getCurrentForbiddenProcesses(baseTerm, forbiddenProcesses,
						stdOut);

				// get the forbidden processes codes (NOT ord code!) related to the base term
				ArrayList<String> currentFPCodes = new ArrayList<>();

				for (ForbiddenProcess proc : currentFP)
					currentFPCodes.add(proc.getForbiddenProcessCode());

				// if the process just added is present into the current forbidden processes
				if (currentFPCodes.contains(facetCode)) {

					// get the position in the array list
					int index = currentFPCodes.indexOf(facetCode);

					boolean isAncestor = false;

					// check if the explicit process is a descendant of some implicit facet
					for (ForbiddenProcess proc : implicit) {

						// get the facet terms related to the forbidden processes codes
						Term ancestor = termDao.getByCode(proc.getForbiddenProcessCode());
						Term descendant = termDao.getByCode(currentFP.get(index).getForbiddenProcessCode());

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
			mutuallyExclusiveCheck(baseTerm, explicit, implicit, stdOut);
		}
	}

}
