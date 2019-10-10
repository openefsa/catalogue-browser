package util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

public class GlobalUtil {

	private static final Logger LOG = LogManager.getLogger(GlobalUtil.class);

	@Test
	public void testLog () {
		LOG.debug("This will be printed on debug");
		LOG.info("This will be printed on info");
		LOG.warn("This will be printed on warn");
		LOG.error("This will be printed on error");
		LOG.fatal("This will be printed on fatal");

		LOG.info("Appending string: {}.", "Hello, World");
	}
}
