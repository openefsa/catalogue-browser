package dcf_pending_request;

public interface PendingRequestActionsListener {

	public enum ActionPerformed {
		
		/**
		 * Temporary version of a catalogue created
		 */
		TEMP_CAT_CREATED,
		
		/**
		 * Temporary version of a catalogue invalidated
		 * because last internal version not present locally
		 */
		TEMP_CAT_INVALIDATED_LIV,
		
		/**
		 * Temporary version of a catalogue invalidated 
		 * because reserve failed
		 */
		TEMP_CAT_INVALIDATED_NO_RESERVE,
		
		/**
		 * Temporary version of a catalogue confirmed
		 * to the correct last internal version
		 */
		TEMP_CAT_CONFIRMED,
		
		/**
		 * The last internal version of the catalogue
		 * was imported
		 */
		LIV_IMPORTED,
		
		/**
		 * Started the download and import of the last
		 * internal version of a catalogue
		 */
		LIV_IMPORT_STARTED,
		
		/**
		 * A new internal version of the catalogue is created (after reserve)
		 */
		NEW_INTERNAL_VERSION_CREATED,
		
	}
	
	public class PendingRequestActionsEvent {
		
		private ActionPerformed action;  // performed action which triggered the event
		private String catalogueCode;         // code of the involved catalogue
		private String oldVersion;            // previous version of the involved catalogue, only for version changes events
		private String version;               // version of the involved catalogue
		private String lastInternalCode;      // code of the last internal version of the catalogue
		private String lastInternalVersion;   // version of the last internal version of the catalogue
		
		public PendingRequestActionsEvent(ActionPerformed action, 
				String catalogueCode, String oldVersion, String version, 
				String lastInternalCode, String lastInternalVersion) {
			this.action = action;
			this.catalogueCode = catalogueCode;
			this.oldVersion = oldVersion;
			this.version = version;
			this.lastInternalCode = lastInternalCode;
			this.lastInternalVersion = lastInternalVersion;
		}
		
		public PendingRequestActionsEvent(ActionPerformed action, 
				String catalogueCode, String version) {
			this(action, catalogueCode, version, version, catalogueCode, version);
		}
		
		public PendingRequestActionsEvent(ActionPerformed action, 
				String catalogueCode, String oldVersion, String version) {
			this(action, catalogueCode, oldVersion, version, catalogueCode, version);
		}
		
		public ActionPerformed getAction() {
			return action;
		}
		
		public String getCatalogueCode() {
			return catalogueCode;
		}
		
		public String getVersion() {
			return version;
		}
		
		public String getOldVersion() {
			return oldVersion;
		}
		
		public String getLastInternalCode() {
			return lastInternalCode;
		}
		
		public String getLastInternalVersion() {
			return lastInternalVersion;
		}
	}
	
	public void actionPerformed(PendingRequestActionsEvent event);
}
