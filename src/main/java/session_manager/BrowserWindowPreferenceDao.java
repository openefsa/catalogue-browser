package session_manager;

import java.io.File;

import utilities.GlobalUtil;
import window_restorer.RestoreableWindowDao;

public class BrowserWindowPreferenceDao extends RestoreableWindowDao {

	public static final String WINDOWS_SIZES_FILENAME = "windows-sizes.json";
	
	@Override
	public File getConfigFile() {
		return new File(GlobalUtil.getPrefDir() + WINDOWS_SIZES_FILENAME);
	}
}
