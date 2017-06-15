package dcf_log;

/**
 * This class contains all the xml nodes names which are
 * contained in a generic Dcf Log document.
 * @author avonva
 *
 */
public class LogXmlNodes {

	public static final String ROOT = "transmissionResult";
	
	public static final String ACTION = "action";
	public static final String TRANSMISSION_DATE = "transmissionDateTime";
	public static final String PROCESSING_DATE = "processingDateTime";
	public static final String UPLOADED_FILENAME = "uploadedFileName";
	public static final String CATALOGUE_CODE = "catalogueCode";
	public static final String CATALOGUE_VERSION = "catalogueVersion";
	public static final String CATALOGUE_STATUS = "catalogueStatus";
	public static final String MACRO_OP_NAME = "macroOperationName";
	public static final String MACRO_OP_RESULT = "macroOperationResult";
	public static final String MACRO_OP_LOGS_BLOCK = "macroOperationLogs";
	public static final String MACRO_OP_LOG = "operationLog";
	
	public static final String OPERATIONS_BLOCK = "operations";
	public static final String OPERATION_BLOCK = "operation";
	public static final String OP_NAME = "operationName";
	public static final String OP_RESULT = "operationResult";
	public static final String OP_LOGS_BLOCK = "operationLogs";
	public static final String OP_LOG = "operationLog";
}
