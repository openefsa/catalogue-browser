package i18n_messages;

import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class CBMessages {
	private static final String BUNDLE_NAME = "cb_messages_en"; //$NON-NLS-1$

	private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);

	public static String getString(String key, Object... args) {
		
		MessageFormat formatter = new MessageFormat("");
		
		try {
			
			String message = RESOURCE_BUNDLE.getString(key);
			
			formatter.applyPattern(message);
			
		    String output = formatter.format(args);
		    
		    return output;
			
		} catch (MissingResourceException e) {
			return '!' + key + '!';
		}
	}
	
	public static String getString(String key) {
		try {
			return RESOURCE_BUNDLE.getString(key);
		} catch (MissingResourceException e) {
			return '!' + key + '!';
		}
	}
}
