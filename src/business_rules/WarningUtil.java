package business_rules;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
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
import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import catalogue.Catalogue;
import catalogue_browser_dao.CatalogueDAO;
import catalogue_browser_dao.TermDAO;
import catalogue_object.Hierarchy;
import catalogue_object.Term;
import catalogue_object.TermAttribute;
import data_transformation.BooleanConverter;
import dcf_manager.Dcf.DcfType;
import global_manager.GlobalManager;
import instance_checker.InstanceChecker;
import naming_convention.Headers;
import ui_implicit_facet.FacetDescriptor;
import ui_implicit_facet.FacetType;
import utilities.GlobalUtil;

/**
 * This class is used to manage all the software related to the warnings in the
 * describe function In particular, it is necessary to initialize it with a
 * table viewer of the warnings and a semaphore (canvas) in order to use this
 * class. Example: TableViewer warningTable = ... Canvas semaphore = ...
 * WarningUtils warnUtils = new WarningUtils( warningTable, semaphore );
 * 
 * Calling from WarningUtils the method refreshWarningsTable will check if the
 * full code of a term raise warnings or not ( and possibly it shows the related
 * warnings in the warningTable ).
 * 
 * @author Valentino
 *
 */

public class WarningUtil {

	private static final Logger LOGGER = LogManager.getLogger(WarningUtil.class);

	private Catalogue currentCat;

	// log console
	private TableViewer warningsTable;

	// semaphore
	private Canvas semaphore;

	// load into memory all the warning messages from the text file
	private ArrayList<WarningMessage> warningMessages;

	// Enum type: it identifies the warning messages events (i.e. which message
	// should be shown in a determined event?)
	// the message id is given by the order of the event definition. So the message
	// with id 1 will be hierarchyBaseTerm etc...
	private static enum WarningEvent {
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
		SourceInDerivative // if a source is added to a derivative without sourcecommodities
	}

	// Enum type for identifing the level of warnings
	private static enum WarningLevel {
		NONE, // no warnings
		LOW, // soft warnings
		HIGH // hard warnings
	}

	// maintain the current warningLevel, which is the highest warning level
	// in the warning log
	private WarningLevel currentWarningLevel = WarningLevel.NONE;

	// list of all the processes which may cause a warning (independentely of the
	// base term)
	private ArrayList<ForbiddenProcess> forbiddenProcesses;

	// load the color options for the warning console and messages
	private WarningOptions warnOptions;

	/**
	 * Start the program by command line
	 * 
	 * @param argv
	 */
	public static void main(String[] args) {

		try {
			// check if another instance using the database
			// is already running
			InstanceChecker.closeIfAlreadyRunning();

			// argument checks
			if (args.length != 5) {
				LOGGER.error("Wrong number of arguments, please check! " + "You have to provide 5 parameters,\n"
						+ "that is, the input file path (collection of codes to be analysed)"
						+ ", the output file path, and the working directory, which is"
						+ "the directory where the catalogue browser files are present."
						+ "Then the code of the FoodEx2 catalogue and if the FoodEx2 catalogue"
						+ "should be searched in the local catalogues or not");

				Shell shell = new Shell(SWT.ON_TOP);

				GlobalUtil.showErrorDialog(shell, "ERROR",
						"Wrong number of parameters passed to app.jar. Expected 5, found " + args.length);
				return;
			}

			WarningUtil.performWarningChecksOnly(args);

			try {
				InstanceChecker.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			// exit from the program, we do not need anything else
			return;
		} catch (Exception e) {

			e.printStackTrace();
			LOGGER.error("Error", e);

			Shell shell = new Shell(SWT.ON_TOP);

			GlobalUtil.showDialog(shell, "Error in Main", e.getMessage(), SWT.ICON_ERROR);
		}
	}

	/**
	 * Initialize the warning util with the MTX catalogue
	 */
	public WarningUtil(String mtxCode, boolean local) throws MtxNotFoundException {

		CatalogueDAO catDao = new CatalogueDAO();
		DcfType type = local ? DcfType.LOCAL : DcfType.PRODUCTION;

		Catalogue mtx = catDao.getLastVersionByCode(mtxCode, type);

		if (mtx == null)
			throw new MtxNotFoundException(mtxCode, type);

		this.currentCat = mtx;

		LOGGER.info("Loading catalogue data into RAM...");

		currentCat.loadData();

		loadFileData();
	}

	public class MtxNotFoundException extends FileNotFoundException {
		private static final long serialVersionUID = 6689235462817235011L;

		public MtxNotFoundException(String mtxCode, DcfType type) {
			super("The " + mtxCode + " catalogue was not found in the " + type + " catalogues metadata database, "
					+ "please download it.");
		}
	}

	/**
	 * constructor
	 * 
	 * @param warningTable
	 * @param semaphore
	 */
	public WarningUtil(TableViewer warningTable, Canvas semaphore) {

		this.warningsTable = warningTable;
		this.semaphore = semaphore;

		// get an instance of the global manager
		GlobalManager manager = GlobalManager.getInstance();

		// get the current catalogue
		currentCat = manager.getCurrentCatalogue();

		loadFileData();
	}

	private void loadFileData() {

		forbiddenProcesses = loadForbiddenProcesses(GlobalUtil.getBRData());
		warnOptions = loadWarningOptions(GlobalUtil.getBRColors());
		warningMessages = loadWarningMessages(GlobalUtil.getBRMessages());
	}

	/**
	 * Given a full code, perform all the implemented checks
	 * 
	 * @param fullCode
	 * @param stdOut   should be the warnings messages printed in the stdOut? used
	 *                 with batch checking tool
	 */
	public void performWarningChecks(String fullCode, boolean stdOut) {

		/*
		 * RETRIEVE BASE TERM FROM FULL CODE
		 */

		// split the full code in order to get the base term code and the facets code
		// separately
		String[] splits = fullCode.split("#");

		// get the base term code (the first part of the full code)
		String baseTermCode = splits[0];

		TermDAO termDao = new TermDAO(currentCat);

		Term baseTerm = termDao.getByCode(baseTermCode);

		// if the base term is not in the database (for macro excel)
		if (baseTerm == null)
			return;

		/*
		 * MAKE SOME WARNING CHECKS AND WARN THE USER IF NECESSARY
		 */

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

		// if there is only the base term name (length == 1)
		// return, there is nothing else to parse (i.e. no facets)
		if (splits.length < 2)
			return;

		// get the warnGroup related to the chosen base term
		// that is, the father which is subjected to restrictions
		// in term of applicability of processes (they are defined in the BR_Data.csv)
		Term warnGroup = getWarnGroup(baseTerm, stdOut);

		// Contains at position i how many processes with ordCode = i
		// are added by the user in an explicit way
		// used to check the mutually exclusive property
		ArrayList<ForbiddenProcess> explicit = new ArrayList<>();

		// Here we takes the facet codes from the full code
		String fullFacetsCodes = splits[1];

		// implicit facets of the base term
		ArrayList<ForbiddenProcess> implicit = getImplicitForbiddenProcesses(baseTerm, forbiddenProcesses, stdOut);

		// tokenize the rest of the full code to get all the facets codes separately
		StringTokenizer st = new StringTokenizer(fullFacetsCodes, "$");

		// get all the facets codes parsing the fullFacetsCodes
		while (st.hasMoreTokens()) {

			// get the next facet code
			String facetFullcode = st.nextToken();

			// get the code of each facet (example of facet code: F01.A0FGM)
			// the first part indicates the facet index, while the second part
			// identify the facet.
			String[] facetComponents = splitFacetFullCode(facetFullcode);

			// the facet index
			String facetIndex = facetComponents[0];

			// delete the first part of the code (i.e. the facet index)
			String facetCode = facetComponents[1];

			// get the facet by code
			Term facet = termDao.getByCode(facetCode);

			// if the facet is not present into the database return (for excel macro)
			if (facet == null)
				return;

			// check if a forbidden process is used or raw commodities
			if (warnGroup != null)
				forbiddenProcessForRawCommodityCheck(baseTerm, facetIndex, facetCode, stdOut);

			// check if the order of processes is violated for derivatives
			if (warnGroup != null)
				forbiddenProcessesOrderForDerivativesCheck(baseTerm, facetIndex, facetCode, stdOut);

			// check if the generic process facet is selected
			genericProcessedFacetCheck(facet, stdOut);

			// check if the user added an ingredient to a raw commodity or to a derivative
			minorIngredientCheck(baseTerm, facetIndex, facetCode, stdOut);

			// check if a source is added to a composite term
			sourceInCompositeCheck(baseTerm, facetIndex, facetCode, stdOut);

			// check if a source commodity is added to a composite term
			sourceCommodityInCompositeCheck(baseTerm, facetIndex, facetCode, stdOut);

			// if it is indeed a warn group
			if (warnGroup != null) {

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

					// get the position in the arraylist
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

							// add the forbidden process into the added processes arraylist
							// since we want to check only the forbidden processes mutually exclusivity

							explicit.add(currentFP.get(index));

							// remove the implicit
							implicit.remove(proc);
							break;
						}
					}

					// if there is no relation => add the process without
					// taking care of implicit processes
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
		if (warnGroup != null) {

			// check if the ord code are correct (i.e. no implicit ord code >= explicit ord
			// codes)
			decimalOrderCheck(baseTerm, implicit, explicit, stdOut);

			// check if the mutually exclusive property is violated
			mutuallyExclusiveCheck(baseTerm, explicit, implicit, stdOut);
		}
	}

	/**
	 * Refresh the warning table, that is, remove all the warnings and recompute
	 * them starting from the fullCode of the term. Examples of full code: A0DPP or
	 * A0DPP#F01.A0FGM or A0DPP#F01.A0FGM$F04.A000J
	 * 
	 * @param fullCode: the full code of a term (IMPORTANT: without the implicit
	 *        facets code if enabled!)
	 */
	public void refreshWarningsTable(String fullCode) {

		/*
		 * GRAPHICS UPDATE
		 */

		// reset the warning messages and level
		resetWarningState();

		// refresh the graphics ( font and colors )
		refreshWarningTableGraphics();

		/*
		 * CHECKS
		 */

		// execute all the warning checks
		performWarningChecks(fullCode, false);
	}

	/**
	 * Reset the warnings contents and level
	 */
	private void resetWarningState() {
		// remove all the warnings
		warningsTable.getTable().removeAll();

		// reset the current warning level
		currentWarningLevel = WarningLevel.NONE;
	}

	/**
	 * Refresh the graphics of the warning table accordingly to the warning options
	 */
	private void refreshWarningTableGraphics() {

		// set the font size of the warnings table accordingly to the warning options
		FontDescriptor descriptor = FontDescriptor.createFrom(warningsTable.getTable().getFont())
				.setHeight(warnOptions.getFontSize());

		warningsTable.getTable().setFont(descriptor.createFont(Display.getCurrent()));

		// set the background color of the table accordingly to the warning options
		// int[] rgb = warnOptions.getConsoleBG();
		// warningsTable.getTable().setBackground( new Color (Display.getCurrent(),
		// rgb[0], rgb[1], rgb[2]) );
		warningsTable.getTable().setBackground(new Color(Display.getCurrent(), 60, 130, 130));

		// refresh the graphical elements of the table
		warningsTable.refresh();
	}

	/**
	 * Check if the selected term belongs to a warn group ( i.e. a group which could
	 * raise a warning defined in BR_Data.csv or BR_Exceptions.csv )
	 * 
	 * @param baseTerm
	 * @return the parent which is a warnGroup, otherwise null
	 */
	private Term getWarnGroup(Term baseTerm, boolean stdOut) {

		// for exceptional terms get exceptions business rules
		if (isExceptionalTerm(baseTerm)) {

			// load exceptions terms and their forbidden processes
			// (these elements have priority over the standard forbidden processes)
			ArrayList<ForbiddenProcess> exceptionForbiddenProcesses = loadForbiddenProcesses(
					GlobalUtil.getBRExceptions());

			forbiddenProcesses = exceptionForbiddenProcesses; // update the forbidden processes

			return baseTerm;
		}

		// set the start element for the parent search
		Term parent = baseTerm;

		// start to go up in the tree, parent by parent
		while (parent != null) {

			// if the parent is a warn group => break cycle and return the warn group
			if (isWarnGroup(parent.getCode(), forbiddenProcesses)) {
				return (parent);
			}

			// get the parent of the current term and continue the loop, we use the
			// reporting hierarchy for warnings
			parent = parent.getParent(currentCat.getHierarchyByCode("report"));
		}

		// if no warn group is discovered, then return null
		return null;
	}

	/**
	 * Check if the selected term identified by the groupCode is one of the warn
	 * groups ( i.e. a group which could raise a warning defined in BR_Data.csv or
	 * BR_Exceptions.csv )
	 * 
	 * @param groupCode
	 * @return
	 */
	private boolean isWarnGroup(String groupCode, ArrayList<ForbiddenProcess> forbiddenProcesses) {

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
	 * Get the implicit forbidden processes of a term
	 * 
	 * @param term, the term to analyze
	 * @param forbiddenProcesses, the processes which are considered as forbidden
	 * @return
	 */
	private ArrayList<ForbiddenProcess> getImplicitForbiddenProcesses(Term term,
			ArrayList<ForbiddenProcess> forbiddenProcesses, boolean stdOut) {

		// initialize the output array
		ArrayList<ForbiddenProcess> implicitForbiddenProcesses = new ArrayList<>();

		// get the warn group of the term
		Term warnGroup = getWarnGroup(term, stdOut);

		// if it is not a warn group => no forbidden processes are defined
		if (warnGroup == null)
			return implicitForbiddenProcesses;

		// get the full code of the term, in order to extract the process code
		// the implicit facets are inserted by default!
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

			// if it is a process and it is one of the dangerous ones, insert it in the
			// array
			if (implicitFacetIndex.equals("F28")) { // if the facet is a process
				for (ForbiddenProcess proc : getCurrentForbiddenProcesses(term, forbiddenProcesses, stdOut)) {
					if (proc.getForbiddenProcessCode().equals(implicitFacetCode)) {
						implicitForbiddenProcesses.add(proc);
					}
				}
			}
		}

		// return all the retrieved implicit forbidden processes
		return implicitForbiddenProcesses;
	}

	/**
	 * Get the facet index and the facet code starting from a composite string which
	 * is like : F01.ADE0A
	 * 
	 * @param facetFullCode
	 * @return
	 */
	private String[] splitFacetFullCode(String facetFullCode) {

		String[] split = facetFullCode.split("\\.");

		// the facet index
		String facetIndex = split[0];
		String facetCode = "";

		if (split.length > 1)
			facetCode = split[1];

		return (new String[] { facetIndex, facetCode });
	}

	/**
	 * This method retrieves the forbidden processes related to a single warn group,
	 * which is given as input
	 * 
	 * @param term, the warnGroup
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
	 * Retrieve the current forbidden processes ord code starting from the current
	 * forbidden processes
	 * 
	 * @param baseTerm
	 * @param currentFP
	 * @return
	 */
	@SuppressWarnings("unused")
	private ArrayList<Double> getCurrentForbiddenProcessesOrdCode(Term baseTerm,
			ArrayList<ForbiddenProcess> currentFP) {
		// return if no forbidden processes
		if (currentFP == null)
			return null;

		// get the forbidden process codes
		ArrayList<Double> fpCodes = new ArrayList<>();
		for (ForbiddenProcess proc : currentFP) {
			fpCodes.add(proc.getOrdCode());
		}

		return (fpCodes);
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

	// ######### TERM/FACET BOOLEAN CHECKS ##############

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
	 * Check if the term is a composite (simple or complex)
	 * 
	 * @param term
	 * @return
	 */
	private boolean isCompositeTerm(Term term) {
		return (term.getTermType().getValue().equals("s") || term.getTermType().getValue().equals("c"));
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
	 * check if the facet is a source commodity facet
	 * 
	 * @param facetIndex
	 * @return
	 */
	private boolean isSourceCommodityFacet(String facetIndex) {
		return (facetIndex.equals("F27"));
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
	 * check if the facet is a source facet
	 * 
	 * @param facetIndex
	 * @return
	 */
	private boolean isSourceFacet(String facetIndex) {
		return (facetIndex.equals("F01"));
	}

	private boolean isExceptionalTerm(Term term) {
		// load exceptions terms and their forbidden processes
		// (these elements have priority over the standard forbidden processes)
		ArrayList<ForbiddenProcess> exceptionForbiddenProcesses = loadForbiddenProcesses(GlobalUtil.getBRExceptions());

		// Warn Group Exceptions, if the base term is an exception then it is itself the
		// warn group
		return (isWarnGroup(term.getCode(), exceptionForbiddenProcesses));
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
	 * Check if the base term is an exceptional term
	 * 
	 * @param baseTerm
	 * @param stdOut
	 */
	private void exceptionTermCheck(Term baseTerm, boolean stdOut) {
		// Warn Group Exceptions, if the base term is an exception
		if (isExceptionalTerm(baseTerm)) {
			// print warning message, ambiguous element selected
			printWarning(WarningEvent.ExceptionTermSelected, baseTerm.getCode(), false, stdOut);
		}
	}

	/**
	 * FIRST WARNING CHECK Check if the base term is a hierarchy. If it is, rise a
	 * warning (discourage its use) Check also if the hierarchy is an exposure
	 * hierarchy or not and rise a warning if it is a non exposure hierarchy
	 */
	private void hierarchyAsBasetermCheck(Term baseTerm, boolean stdOut) {

		// if the base term is a hierarchy
		if (baseTerm.getDetailLevel().isHierarchyDetailLevel()) {

			ArrayList<Hierarchy> hierarchies = baseTerm.getApplicableHierarchies();

			// if the term is not in the exposure hierarchy
			Hierarchy expHierarchy = currentCat.getHierarchyByCode("expo");

			if (!hierarchies.contains(expHierarchy)) {

				// print warning that you are using a non exposure hierarchy term
				printWarning(WarningEvent.NoExpHierarchyTerm, baseTerm.getCode(), false, stdOut);
			} else { // if the term is in the exposure hierarchy
				// print the message related to the hierarchy as base term
				printWarning(WarningEvent.HierarchyBaseTerm, baseTerm.getCode(), false, stdOut);
			}
		} else {
			// print the message: base term successfully added
			printWarning(WarningEvent.BaseTermSuccessfullyAdded, baseTerm.getCode(), false, stdOut);
		}
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
	private void forbiddenProcessForRawCommodityCheck(Term baseTerm, String facetIndex, String facetCode,
			boolean stdOut) {

		// return if the base term is not a raw commodity ( no check has to be done )
		// of if the facet is not a process
		if (!isRawCommodityTerm(baseTerm) || !isProcessFacet(facetIndex))
			return;

		// get the forbidden processes related to the baseTerm
		ArrayList<ForbiddenProcess> currentFP = getCurrentForbiddenProcesses(baseTerm, forbiddenProcesses, stdOut);

		// get all the processes codes (NOT ord code!)
		ArrayList<String> currentFPCodes = new ArrayList<>();
		for (ForbiddenProcess proc : currentFP)
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
	private void forbiddenProcessesOrderForDerivativesCheck(Term baseTerm, String facetIndex, String facetCode,
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

		} // end if
	} // end function

	/**
	 * FOURTH WARNING CHECK Check if more than one process with the same ord code is
	 * chosen
	 * 
	 * @param addedProcesses the list of processes that were added to the base term
	 *                       by the user (with describe)
	 */
	private void mutuallyExclusiveCheck(Term baseTerm, ArrayList<ForbiddenProcess> implicitProcesses,
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
	private void decimalOrderCheck(Term baseTerm, ArrayList<ForbiddenProcess> implicitProcesses,
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

			boolean firstCheck = (uniqueImpl.size() > 0 && uniqueExpl.size() > 0);
			boolean secondCheck = (uniqueImpl.size() == 0 && uniqueExpl.size() > 1);

			if ((firstCheck || secondCheck) && uniqueAllCodes.size() > 1) {

				// get all the codes of the processes involved
				StringBuilder sb = new StringBuilder();

				// for each element in the exp processes with ord code with decimal and integer
				// part = i
				for (int j = 0; j < decimalExplicitProcesses.size(); j++) {
					sb.append(decimalExplicitProcesses.get(j).getForbiddenProcessCode());
					if (j < decimalExplicitProcesses.size() - 1 && decimalExplicitProcesses.size() > 0)
						sb.append(" - ");
				}

				// for each element in the imp processes with ord code with decimal and integer
				// part = i
				for (int j = 0; j < decimalImplicitProcesses.size(); j++) {
					sb.append(decimalImplicitProcesses.get(j).getForbiddenProcessCode());
					if (j < decimalImplicitProcesses.size() - 1 && decimalImplicitProcesses.size() > 0)
						sb.append(" - ");
				}

				// print warning: these processes generate a derivative which is already
				// existing
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
	private void noRepNoExpHierarchyCheck(Term baseTerm, boolean stdOut) {
		try {

			// get all the applicable hierarchies for the selected base term
			ArrayList<Hierarchy> hierarchies = baseTerm.getApplicableHierarchies();

			Hierarchy reportingHierarchy = currentCat.getHierarchyByCode(Headers.REPORT);
			Hierarchy exposureHierarchy = currentCat.getHierarchyByCode(Headers.EXPO);

			// if the term does not belong to both reporting and exposure hierarchy =>
			// warning
			if (!hierarchies.contains(reportingHierarchy) && !hierarchies.contains(exposureHierarchy))
				printWarning(WarningEvent.NoRepNoExpBaseTerm, baseTerm.getCode(), false, stdOut);

		} catch (Exception e) {
			e.printStackTrace();
			LOGGER.error("Error", e);
		}
	}

	/**
	 * SEVENTH CHECK Check if a non-specific term is selected
	 * 
	 * @param term
	 */
	private void nonSpecificTermCheck(Term term, boolean stdOut) {

		// if the term is a non-specific one, rise a warning (semaphore green, text
		// yellow)
		if (isNonSpecificTerm(term)) {
			printWarning(WarningEvent.NonSpecificTerm, term.getCode(), false, stdOut);
		}
	}

	/**
	 * EIGHT CHECK Check if the "processed" facet is selected or not
	 * 
	 * @param facet
	 */
	private void genericProcessedFacetCheck(Term facet, boolean stdOut) {

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
	private void minorIngredientCheck(Term baseTerm, String facetIndex, String facetCode, boolean stdOut) {
		// if the base term is a raw commodity or a derivative
		if (isRawCommodityTerm(baseTerm) || isDerivativeTerm(baseTerm)) {

			if (isIngredientFacet(facetIndex)) // if the facet is an ingredient
				printWarning(WarningEvent.MinorIngredient, facetCode, false, stdOut);
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
	private void sourceCommodityRawCheck(Term baseTerm, String fullFacetCode, boolean stdOut) {

		// if the base term is not a raw commodity no checks have to be done
		if (!isRawCommodityTerm(baseTerm))
			return;

		// tokenize the facets => the implicit are not considered since
		// raw commodities have their self as source commodity and should not
		// be taken into account
		StringTokenizer st = new StringTokenizer(fullFacetCode, "$");

		// count the source commodity
		int sourceCommodityFacetCount = 0;

		// diagnostic string builder
		StringBuilder sb = new StringBuilder();

		ArrayList<FacetDescriptor> implicitFacets = baseTerm.getFacets(true);
		ArrayList<Term> implicitTerms = new ArrayList<>();

		TermDAO termDao = new TermDAO(currentCat);

		// check implicit facets
		for (FacetDescriptor fd : implicitFacets) {
			implicitTerms.add(termDao.getByCode(fd.getFacetCode()));
		}

		ArrayList<FacetDescriptor> explicitFacets = new ArrayList<>();

		// for each explicit facet
		while (st.hasMoreTokens()) {

			String code = st.nextToken();

			// split the facet in facet header and facet code
			String[] split = splitFacetFullCode(code);

			Term term = termDao.getByCode(split[1]);

			FacetDescriptor fd = new FacetDescriptor(term, new TermAttribute(term, null, code), FacetType.EXPLICIT);

			explicitFacets.add(fd);
		}

		// restrict if explicit is child of an implicit
		Hierarchy hierarchy = currentCat.getHierarchyByCode("racsource");
		for (FacetDescriptor fd : explicitFacets) {

			boolean skip = false;

			for (Term implicit : implicitTerms) {
				if (fd.getDescriptor().hasAncestor(implicit, hierarchy))
					skip = true;
			}

			if (skip) {
				continue;
			}

			// count the number of source commodities facets
			if (isSourceCommodityFacet(fd.getFacetHeader())) {
				sourceCommodityFacetCount++;
				sb.append(fd.getFacetCode());
				sb.append(" - ");
			}
		}

		// get all the involved terms
		String termsInvolved = sb.toString();

		// remove the last " - " if present (i.e. at least one source c. was added)
		if (sourceCommodityFacetCount > 0)
			termsInvolved = termsInvolved.substring(0, termsInvolved.length() - " - ".length());

		// if only one source commodity is selected => you cannot use only 1 SC! At
		// least two are required
		if (sourceCommodityFacetCount == 1) {
			printWarning(WarningEvent.SingleSourceCommodityToRaw, termsInvolved, false, stdOut);
		}
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
	private void sourceCommodityDerivativeCheck(Term baseTerm, String fullFacetCode, boolean stdOut) {

		// if the base term is not a derivative no checks have to be done
		if (!isDerivativeTerm(baseTerm))
			return;

		ArrayList<FacetDescriptor> implicitFacets = baseTerm.getFacets(true);
		ArrayList<Term> implicitTerms = new ArrayList<>();

		// counter for counting the number of specific facets
		int implicitSourceCommCount = 0;
		int explicitSourceCommCount = 0;
		int explicitRestrictedSourceCommCount = 0;
		int sourceFacetCount = 0;

		// string builder for generating the diagnostic string
		StringBuilder sb = new StringBuilder();

		TermDAO termDao = new TermDAO(currentCat);

		// check implicit facets
		for (FacetDescriptor fd : implicitFacets) {

			implicitTerms.add(termDao.getByCode(fd.getFacetCode()));

			boolean append = false;

			if (isSourceCommodityFacet(fd.getFacetHeader())) {
				implicitSourceCommCount++;
				append = true;
			}

			if (isSourceFacet(fd.getFacetHeader())) {
				sourceFacetCount++;
				append = true;
			}

			if (append) {
				// append for diagnostic
				sb.append(fd.getFacetCode());
				sb.append(" - ");
			}
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
				if (fd.getDescriptor().hasAncestor(implicit, hierarchy))
					skip = true;
			}

			// count the number of source commodities facets
			if (isSourceCommodityFacet(fd.getFacetHeader())) {

				// source commodity facet found
				if (skip) {
					explicitSourceCommCount++;
				} else {
					explicitSourceCommCount++;
					explicitRestrictedSourceCommCount++;
				}

				// append for diagnostic
				sb.append(fd.getFacetCode());
				sb.append(" - ");
			}

			// count the number of source facets
			if (isSourceFacet(fd.getFacetHeader())) {

				// source facet found
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
		if (explicitRestrictedSourceCommCount > 0 && implicitSourceCommCount > 0) {
			printWarning(WarningEvent.NonGenericDerivativeUsed, termsInvolved, false, stdOut);
		}

		// if source without source commodities
		if (totalSourceCommCount == 0 && sourceFacetCount > 0) {
			printWarning(WarningEvent.SourceInDerivative, termsInvolved, false, stdOut);
		}
		// if more than two source commodities and one source are present => warning
		else if (explicitSourceCommCount >= 2 && sourceFacetCount > 0) {
			printWarning(WarningEvent.MixedDerivative, termsInvolved, false, stdOut);
		}
		// if one explicit source commodity is selected => you cannot use only 1 SC! At
		// least two are required
		else if (explicitRestrictedSourceCommCount + implicitSourceCommCount == 1 && sourceFacetCount > 0) {
			printWarning(WarningEvent.SourceToDerivative, termsInvolved, false, stdOut);
		}
	}

	/**
	 * 12th CHECK Check if the term is in the exposure hierarchy and it is not a
	 * feed term. If not, rise a warning
	 * 
	 * @param baseTerm
	 */
	private void exposureHierarchyCheck(Term baseTerm, boolean stdOut) {

		// if the term is a feed term, no checks have to be done
		if (isFeedTerm(baseTerm))
			return;

		Hierarchy exposureHierarchy = currentCat.getHierarchyByCode("expo");

		// if the term is not in the exposure hierarchy
		if (!baseTerm.getApplicableHierarchies().contains(exposureHierarchy))
			printWarning(WarningEvent.NoExposureTerm, baseTerm.getCode(), false, stdOut);

	}

	/**
	 * Check if a source is added to a composite food
	 * 
	 * @param baseTerm
	 * @param facetIndex
	 * @param facetCode
	 */
	private void sourceInCompositeCheck(Term baseTerm, String facetIndex, String facetCode, boolean stdOut) {

		// return if not composite
		if (!isCompositeTerm(baseTerm))
			return;

		// if a source is added to a composite rise a warning
		if (isSourceFacet(facetIndex))
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

		// return if not composite
		if (!isCompositeTerm(baseTerm))
			return;

		// if a source commodity is added to a composite rise a warning
		if (isSourceCommodityFacet(facetIndex))
			printWarning(WarningEvent.SourceCommodityInComposite, facetCode, false, stdOut);
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
	 * Create the message string to be printed into the console (or to be used in
	 * excel files for macros)
	 * 
	 * @param event
	 * @param                postMessageString, string to be printed between
	 *                       brackets after the warning
	 * @param attachDatetime
	 * @return
	 */
	public String createMessage(WarningEvent event, String postMessageString, boolean attachDatetime) {

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
	 * get the warning level of the semaphore
	 * 
	 * @param event
	 * @return
	 */
	public WarningLevel getSemaphoreLevel(WarningEvent event) {
		return warningMessages.get(event.ordinal()).getWarningLevel();
	}

	/**
	 * get the warning level of the text message
	 * 
	 * @param event
	 * @return
	 */
	public WarningLevel getTextLevel(WarningEvent event) {
		return warningMessages.get(event.ordinal()).getTextWarningLevel();
	}

	/**
	 * Print a warning into the warningsTable It appends the date time to the
	 * message Update the current warning level to the highest retrieved until now
	 * Update the semaphore and text accordingly to the warning level
	 * 
	 * @param event: the event which causes the printWarning
	 * @param postMessageString: a string which is attached to the end of the
	 *        message (used for additional info)
	 * @param attachDatetime: should the datetime be attached at the end of the
	 *        message?
	 */

	private void printWarning(WarningEvent event, String postMessageString, boolean attachDatetime, boolean stdOut) {

		// create the warning message to be printed
		String message = createMessage(event, postMessageString, attachDatetime);

		// get the warning levels for making colours
		WarningLevel semaphoreLevel = getSemaphoreLevel(event);
		WarningLevel textWarningLevel = getTextLevel(event);

		// if the message should be printed into the standard output
		// CSV line semicolon separated
		// do not print the base term successfully added warning! It is useless for the
		// excel macro
		if (stdOut && event != WarningEvent.BaseTermSuccessfullyAdded) {

			StringBuilder sb = new StringBuilder();
			sb.append(message);
			sb.append(";");
			sb.append(semaphoreLevel.toString());
			sb.append(";");
			sb.append(textWarningLevel.toString());

			// print the line
			System.out.print(sb.toString() + "|");
			return;
		}

		// if graphical object are not used
		if (warningsTable == null || semaphore == null)
			return;

		// print the message into the warnings table
		warningsTable.add(message);

		// scroll the table to the new message
		warningsTable.reveal(message);

		// get the index of the last inserted element (in the table)
		// to change its text color
		int lastElementIndex = warningsTable.getTable().getItemCount() - 1;

		// get the warning color (related to the warning level)
		Device device = Display.getCurrent();
		Color warningColor; // semaphore color
		Color txtColor; // message color in the console
		int[] rgb;

		// semaphore color based on warning level
		if (semaphoreLevel == WarningLevel.NONE) { // if the level warning is NONE
			rgb = warnOptions.getSemNoWarnRGB();
			warningColor = new Color(device, rgb[0], rgb[1], rgb[2]);
		} else if (semaphoreLevel == WarningLevel.LOW) { // if level warning is LOW
			rgb = warnOptions.getSemLowWarnRGB();
			warningColor = new Color(device, rgb[0], rgb[1], rgb[2]);
		} else { // if level warning is HIGH
			rgb = warnOptions.getSemHiWarnRGB();
			warningColor = new Color(device, rgb[0], rgb[1], rgb[2]);
		}

		// text color based on warning level
		if (textWarningLevel == WarningLevel.NONE) {
			rgb = warnOptions.getTxtNoWarnRGB();
			txtColor = new Color(device, rgb[0], rgb[1], rgb[2]);
		} else if (textWarningLevel == WarningLevel.LOW) {
			rgb = warnOptions.getTxtLowWarnRGB();
			txtColor = new Color(device, rgb[0], rgb[1], rgb[2]);
		} else {
			rgb = warnOptions.getTxtHiWarnRGB();
			txtColor = new Color(device, rgb[0], rgb[1], rgb[2]);
		}

		// update the text color accordingly to the warning color
		warningsTable.getTable().getItems()[lastElementIndex].setForeground(txtColor);

		// if the warning level of this message is greater than or equal the previous
		// ones
		// ( the equal is used to show the green semaphore for warning level = NONE )

		if (semaphoreLevel.ordinal() >= currentWarningLevel.ordinal()) {

			// update the currentWarningLevel
			currentWarningLevel = semaphoreLevel;

			// change the color of the semaphore
			semaphore.setBackground(warningColor);
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
	private ArrayList<WarningMessage> loadWarningMessages(String filename) {
		try {

			File file = new File(filename);
			if (!file.exists()) {
				createDefaultWarningMessagesFile(filename);
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

				// if the level is set to HIGH
				if (warningLevelToken.toLowerCase().replace(" ", "").equals("high")) {
					warningLevel = WarningLevel.HIGH;
				} else if (warningLevelToken.toLowerCase().replace(" ", "").equals("low")) { // if the level is set to
																								// LOW
					warningLevel = WarningLevel.LOW;
				} else { // otherwise, the default is warning level NONE
					warningLevel = WarningLevel.NONE;
				}

				// get the text warning level related to this message
				String textWarningLevelToken = st.nextToken();

				// if the level is set to HIGH
				if (textWarningLevelToken.toLowerCase().replace(" ", "").equals("high")) {
					textWarningLevel = WarningLevel.HIGH;
				} else if (textWarningLevelToken.toLowerCase().replace(" ", "").equals("low")) { // if the level is set
																									// to LOW
					textWarningLevel = WarningLevel.LOW;
				} else { // otherwise, the default is warning level NONE
					textWarningLevel = WarningLevel.NONE;
				}

				// create a warning message object with the message id and the content of the
				// message (the next token)
				warningMessages.add(new WarningMessage(messageId, message, warningLevel, textWarningLevel));

				// new line
				lineCount++;
			}

			// sort the warning messages using their ID
			// (in order to be accessible using the event ID directly without
			// searching in all the list)
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
	 * Class which store the information related to a single warning message.
	 * 
	 * @author Valentino
	 *
	 */
	private class WarningMessage {

		int id;
		String message;
		WarningLevel warningLevel;
		WarningLevel textWarningLevel;

		private WarningMessage(int id, String message, WarningLevel warningLevel, WarningLevel textWarningLevel) {
			this.id = id;
			this.message = message;
			this.warningLevel = warningLevel;
			this.textWarningLevel = textWarningLevel;
		}

		// the id of the message (used to connect the warning event to the warning
		// message)
		public int getId() {
			return id;
		}

		// the message to be print
		public String getMessage() {
			return message;
		}

		// get the warning level of the message
		public WarningLevel getWarningLevel() {
			return warningLevel;
		}

		public WarningLevel getTextWarningLevel() {
			return textWarningLevel;
		}
	}

	/**
	 * Create the default file which contains the warning messages
	 * 
	 * @param filename
	 * @return
	 */
	private static int createDefaultWarningMessagesFile(String filename) {

		try {

			PrintWriter out = new PrintWriter(filename);

			StringBuilder sb = new StringBuilder();

			sb.append("Message ID;Trigger Event Description;Text;SemaphoreWarningLevel;TextWarningLevel");
			sb.append("\r\n");

			// first message
			sb.append(
					"1;if a hierarchy is selected as base term (describe function);Warning: the base term is a hierarchy!;LOW;LOW");
			sb.append("\r\n");

			// second message
			sb.append(
					"2;if a non-hierarchy is selected as base term (describe function);The base term was successfully added!;NONE;NONE");
			sb.append("\r\n");

			// third message
			sb.append(
					"3;if a forbidden process is chosen (the derivative should be used);You are trying to generate an already existing derivative!;HIGH;HIGH");
			sb.append("\r\n");

			// fourth message
			sb.append(
					"4;if a derivative is described with a process facet with an ordCode value less than the implicit ordCode;"
							+ "Process applied in the wrong order!;HIGH;HIGH");
			sb.append("\r\n");

			// fifth message
			sb.append("5;if an ambiguous term is selected (terms reported in the BR_Exceptions); "
					+ "You have selected an ambiguous term, please specify the element better!;LOW;LOW");
			sb.append("\r\n");

			// sixth message
			sb.append(
					"6;if more than one process with the same ordCode is chosen (mutually exclusive property violated);"
							+ "You cannot use these processes together!;HIGH;HIGH");
			sb.append("\r\n");

			// seventh message
			sb.append("7;if a base term which does not belong to reporting or exposure hierarchy is selected;"
					+ "You cannot use a non reportable term!;LOW;LOW");
			sb.append("\r\n");

			// eight message
			sb.append(
					"8;if a non exposure hierarchy is selected as base term (describe);You have selected a non-exposure hierarchy!;LOW;LOW");
			sb.append("\r\n");

			// ninth message
			sb.append("9;if a non-specific term is selected (describe);You have selected a non-specific term. "
					+ "Are you sure that you cannot be more precise?;NONE;LOW");
			sb.append("\r\n");

			// tenth message
			sb.append("10;if the generic facet processed is selected;"
					+ "You have selected a generic process facet. Are you sure that you cannot be more precise?;LOW;LOW");
			sb.append("\r\n");

			// eleventh message
			sb.append("11;if an ingredient is selected for raw commodity or derivative;"
					+ "Is it a minor ingredient? Please check!;LOW;LOW");
			sb.append("\r\n");

			// 12 message
			sb.append(
					"12;if a single source commodity is selected for raw commodity;Only multiple source commodities allowed, "
							+ "for mixed raw commodities!;HIGH;HIGH");
			sb.append("\r\n");

			// 13 message
			sb.append("13;if a source is selected for mixed derivative (more than one F27.);"
					+ "Source facet not allowed in mixed derivatives;HIGH;HIGH");
			sb.append("\r\n");

			// 14 message
			sb.append("14;if a source is selected for derivative with only one F27.;"
					+ "Make sure that Source is used for better specifying the raw source -otherwise forbidden;LOW;LOW");
			sb.append("\r\n");

			// 15 message
			sb.append("15;if a base term not valid in the exposure hierarchy is chosen;"
					+ "Not valid for human exposure calculation;NONE;HIGH");
			sb.append("\r\n");

			// 16 message
			sb.append(
					"16;if a source is selected for composite (c or s);Source is not applicable for composite food;HIGH;HIGH");
			sb.append("\r\n");

			// 17 message
			sb.append(
					"17;if a source commodity is selected for composite (c or s);Source commodity is not applicable for composite food;HIGH;HIGH");
			sb.append("\r\n");

			// 18 message
			sb.append(
					"18;if two processes (implicit or explicit) with decimal ordcode and same integer part are applied (at least one explicit);"
							+ "You are trying to generate an already existing derivative!;HIGH;HIGH");
			sb.append("\r\n");

			// 19 message
			sb.append(
					"19;if one or more source commodities are added to a derivative already having an implicit source commodity (not parent of the added);"
							+ "Use the generic derivative as base term for describing a mixed derivative;HIGH;HIGH");
			sb.append("\r\n");

			sb.append(
					"20;if a source is selected for a generic derivative without F27 (neither explicit nor implicit);Forbidden to use the Source without the (single) source commodity;HIGH;HIGH");
			sb.append("\r\n");

			out.write(sb.toString());
			out.close();

			return 0;

		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
			return -1;
		}
	}

	/**
	 * Open the file filename and retrieve the forbidden processes for hierarchies
	 * the file must be a CSV file with 5 fields: baseTermGroupCode,
	 * baseTermGroupName, forbiddenProcessCode, forbiddenProcessName, ordCode
	 * 
	 * @param filename, the csv filename
	 * @return an array list of forbidden processes
	 */

	public static ArrayList<ForbiddenProcess> loadForbiddenProcesses(String filename) {

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
					LOGGER.error(" Error: no double format found in " + filename + "!", e);
				}

				// next line
				lineCount++;
			}

			// Close the connection
			bufferedReader.close();

			return (forbiddenProcesses);

		} catch (Exception e) {

			e.printStackTrace();

			Shell shell = new Shell(SWT.ON_TOP);
			GlobalUtil.showErrorDialog(shell, "Error", e.getMessage());

			LOGGER.error(filename + " not found or parsing errors.", e);
			return null;
		}
	}

	/**
	 * This class is used to store all the information related to the forbidden
	 * processes for each hierarchy group which contains derivatives. In fact, a
	 * warning should be raised if the user applies processes to raw commodities in
	 * order to create a derivative and that derivative is already present in the
	 * list (with its own code). Moreover, the processes have to be applied with a
	 * particular order, therefore we check that order with the ordCode field.
	 * 
	 * @author Valentino
	 *
	 */
	public static class ForbiddenProcess {

		// create the variables of interest
		// baseTermGroupCode: the code of hierarchies which could be subjected to
		// warnings
		// forbiddenProcessCode: the code of a forbidden process related to the
		// baseTermGroup selected
		// ordCode: code used to check the order of the process applicability
		String baseTermGroupCode, forbiddenProcessCode;
		double ordCode;

		public ForbiddenProcess(String baseTermGroupCode, String forbiddenProcessCode, double ordCode) {
			this.baseTermGroupCode = baseTermGroupCode;
			this.forbiddenProcessCode = forbiddenProcessCode;
			this.ordCode = ordCode;
		}

		// getter methods

		public String getBaseTermGroupCode() {
			return baseTermGroupCode;
		}

		public String getForbiddenProcessCode() {
			return forbiddenProcessCode;
		}

		public double getOrdCode() {
			return ordCode;
		}
	}

	/**
	 * Parse the file of the warning options and load into memory all the color and
	 * font options required
	 * 
	 * @param filename
	 * @return
	 */
	public static WarningOptions loadWarningOptions(String filename) {
		try {

			File file = new File(filename);
			if (!file.exists()) {
				createDefaultWarnColorOptionsFile(filename);
			}

			WarningOptions options = new WarningOptions();

			// FileReader reads text files in the default encoding.
			FileReader fileReader = new FileReader(filename);

			// Always wrap FileReader in BufferedReader.
			BufferedReader bufferedReader = new BufferedReader(fileReader);

			String line;

			// while there is a line to be red
			while ((line = bufferedReader.readLine()) != null) {

				// remove white spaces
				line = line.replace(" ", "");

				// analyze the line tokens
				StringTokenizer st = new StringTokenizer(line, "=");

				// get the current field
				String fieldName = st.nextToken();

				// font size is the only field which is not an RGB value
				if (!fieldName.equals("WarnFontSize")) {

					// get the RGB values
					int[] rgb = parseRGB(st.nextToken(), ";");

					// add them to the options
					switch (fieldName) {
					case "SemaphoreNoWarn":
						options.setSemNoWarnRGB(rgb);
						break;
					case "SemaphoreLowWarn":
						options.setSemLowWarnRGB(rgb);
						break;
					case "SemaphoreHighWarn":
						options.setSemHiWarnRGB(rgb);
						break;
					case "TxtNoWarn":
						options.setTxtNoWarnRGB(rgb);
						break;
					case "TxtLowWarn":
						options.setTxtLowWarnRGB(rgb);
						break;
					case "TxtHighWarn":
						options.setTxtHiWarnRGB(rgb);
						break;
					case "ConsoleBG":
						options.setConsoleBG(rgb);
						break;
					}
				} else { // font size, parse the integer
					try {
						options.setFontSize(Integer.parseInt(st.nextToken()));
					} catch (Exception e) {
						LOGGER.error("Error parsing font size in warningColors options.", e);
					}
				}
			}

			// Close the connection
			bufferedReader.close();

			return (options);

		} catch (Exception e) {
			LOGGER.error(filename + " not found.", e);
			return null;
		}
	}

	/**
	 * Function to parse RGB values separated by a delim character
	 * 
	 * @param line
	 * @param delim
	 * @return
	 */
	static int[] parseRGB(String line, String delim) {

		StringTokenizer st = new StringTokenizer(line, delim);

		// three numbers have to be present for RGB coding
		if (st.countTokens() != 3)
			return null;

		try {
			// get the RGB values

			String token = st.nextToken();
			int red = Integer.parseInt(token);

			token = st.nextToken();
			int green = Integer.parseInt(token);

			token = st.nextToken();
			int blue = Integer.parseInt(token);

			return (new int[] { red, green, blue });
		} catch (Exception e) {
			LOGGER.error("ERROR IN PARSING RGB VALUES", e);
			return null;
		}
	}

	/**
	 * class which contains all the options related to the warnings colors
	 * 
	 * @author Valentino
	 *
	 */
	public static class WarningOptions {

		/*
		 * RGB values: the color of the semaphore in the three warning levels, the color
		 * of the log messages in the three warning levels the background color of the
		 * console which prints the warnings the font size of the messages
		 */
		int[] semNoWarnRGB, semLowWarnRGB, semHiWarnRGB, txtNoWarnRGB, txtLowWarnRGB, txtHiWarnRGB, consoleBG = null;
		int fontSize = 14;

		/*
		 * SETTER METHODS
		 */
		public void setSemNoWarnRGB(int[] semNoWarnRGB) {
			this.semNoWarnRGB = semNoWarnRGB;
		}

		public void setSemLowWarnRGB(int[] semLowWarnRGB) {
			this.semLowWarnRGB = semLowWarnRGB;
		}

		public void setSemHiWarnRGB(int[] semHiWarnRGB) {
			this.semHiWarnRGB = semHiWarnRGB;
		}

		public void setTxtNoWarnRGB(int[] txtNoWarnRGB) {
			this.txtNoWarnRGB = txtNoWarnRGB;
		}

		public void setTxtLowWarnRGB(int[] txtLowWarnRGB) {
			this.txtLowWarnRGB = txtLowWarnRGB;
		}

		public void setTxtHiWarnRGB(int[] txtHiWarnRGB) {
			this.txtHiWarnRGB = txtHiWarnRGB;
		}

		public void setConsoleBG(int[] consoleBG) {
			this.consoleBG = consoleBG;
		}

		public void setFontSize(int fontSize) {
			this.fontSize = fontSize;
		}

		/*
		 * GETTER METHODS
		 */
		public int[] getSemNoWarnRGB() {
			return semNoWarnRGB;
		}

		public int[] getSemLowWarnRGB() {
			return semLowWarnRGB;
		}

		public int[] getSemHiWarnRGB() {
			return semHiWarnRGB;
		}

		public int[] getTxtNoWarnRGB() {
			return txtNoWarnRGB;
		}

		public int[] getTxtLowWarnRGB() {
			return txtLowWarnRGB;
		}

		public int[] getTxtHiWarnRGB() {
			return txtHiWarnRGB;
		}

		public int[] getConsoleBG() {
			return consoleBG;
		}

		public int getFontSize() {
			return fontSize;
		}
	}

	/**
	 * Function to create the default warning color options
	 * 
	 * @param filename
	 */
	private static void createDefaultWarnColorOptionsFile(String filename) {
		try {

			// write the default warning color options
			PrintWriter out = new PrintWriter(filename);

			// string builder to build the string ( or simply a string can be used... )
			StringBuilder sb = new StringBuilder();

			sb.append("SemaphoreNoWarn = 0;255;0\r\n" + "SemaphoreLowWarn = 255;255;0\r\n"
					+ "SemaphoreHighWarn = 255;0;0\r\n" + "TxtNoWarn = 0;255;0\r\n" + "TxtLowWarn = 255;255;0\r\n"
					+ "TxtHighWarn = 255;0;0\r\n" + "ConsoleBG = 0;0;0\r\n" + "WarnFontSize = 14\r\n");

			// write the string
			out.write(sb.toString());

			// close the connection
			out.close();
		} catch (Exception e) {
			LOGGER.error("Cannot create the file " + filename, e);
		}
	}

	/**
	 * Use only the business rules check of the FoodexBrowser Useful in combination
	 * of excel data
	 * @author shahaal
	 * @author avonva
	 * @param args
	 */
	public static void performWarningChecksOnly(String[] args) {

		// set the working directory to find files
		// with the absolute path
		String workingDir = args[2];
		GlobalUtil.setWorkingDirectory(workingDir);

		File input = new File(args[0]);

		String mtxCode = args[3];
		boolean local = BooleanConverter.getBoolean(args[4]);

		try {

			// start the warning utils with the mtx catalogue
			// if it was found. Exception is raised if the MTX
			// catalogue is not found in the catalogues database
			WarningUtil warnUtils;
			try {
				warnUtils = new WarningUtil(mtxCode, local);
			} catch (MtxNotFoundException e) {
				e.printStackTrace();

				Shell shell = new Shell(SWT.ON_TOP);
				GlobalUtil.showErrorDialog(shell, "Error", e.getMessage());
				return;
			}

			// output file (it will capture all the standard output)
			PrintStream out = new PrintStream(new FileOutputStream(args[1]));
			System.setOut(out); // redirect standard output to the file

			// read the codes from the input file (correct memory leak)
			try (FileReader reader = new FileReader(input)) {
				
				BufferedReader buffReader = new BufferedReader(reader);

				String line; // current line of the file

				int lineCount = 0; // count the line of the file

				// for each code perform the warning checks
				while ((line = buffReader.readLine()) != null) {

					System.err.println("+++++ ANALYZING CODE N° " + (lineCount + 1) + " +++++");

					// add a separator among the warnings related to different codes
					if (lineCount != 0) {
						System.out.println(""); // add new line
					}

					// perform the warnings checks for the current code
					warnUtils.performWarningChecks(line, true);

					// count the lines
					lineCount++;
				}

				// close the input file
				buffReader.close();

				// close the output file
				out.close();

				Shell shell = new Shell(SWT.ON_TOP);
				GlobalUtil.showDialog(shell, "Success", "The checks were successfully completed!",
						SWT.ICON_INFORMATION);
			}
		} catch (Exception e) {
			e.printStackTrace();

			LOGGER.error("Error", e);

			Shell shell = new Shell(SWT.ON_TOP);

			GlobalUtil.showDialog(shell, "General Error", e.getMessage(), SWT.ICON_ERROR);

			return;
		}
	}
}
