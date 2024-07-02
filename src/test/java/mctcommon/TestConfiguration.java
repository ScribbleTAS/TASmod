package mctcommon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.minecrafttas.mctcommon.Configuration;
import com.minecrafttas.mctcommon.Configuration.ConfigOptions;

class TestConfiguration {
	
	enum TestConfig implements ConfigOptions {
		FileToOpen("fileToOpen", ""),
		ServerConnection("serverConnection", "");

		private String configKey;
		private String defaultValue;
		
		private TestConfig(String configKey, String defaultValue) {
			this.configKey = configKey;
			this.defaultValue = defaultValue;
		}
		
		@Override
		public String getDefaultValue() {
			return defaultValue;
		}

		@Override
		public String getConfigKey() {
			return configKey;
		}
		
		@Override
		public String getExtensionName() {
			return "TestConfig";
		}
	}

	private Configuration config;
	
	private static final File configPath = new File("./config.xml");
	
	@BeforeEach
	void beforeEach() {
		config = new Configuration("Test config", configPath);
		config.register(TestConfig.values());
		config.load();
	}

	@AfterAll
	static void tearDownAfterClass() throws Exception {
		configPath.delete();
	}

	/**
	 * Test if the config is successfully initialized
	 */
	@Test
	void testIfInitialized() {
		assertNotNull(config);
	}
	
	/**
	 * Test if the default option is correctly set
	 */
	@Test
	void testDefault() {
		configPath.delete();
		config = new Configuration("Test config", configPath);
		config.register(TestConfig.values());
		config.load();
		assertEquals("", config.get(TestConfig.FileToOpen));
	}
	
	/**
	 * Setting a value and recreating the config should result in the value still being set
	 */
	@Test
	void testSavingAndLoading() {
		config.set(TestConfig.FileToOpen, "Test");
		config = new Configuration("Test config", configPath);
		config.register(TestConfig.values());
		config.load();
		assertEquals("Test", config.get(TestConfig.FileToOpen));
	}
	
	/**
	 * Test if integers can be set
	 */
	@Test
	void testIntegers() {
		config.set(TestConfig.FileToOpen, 3);
		assertEquals(3, config.getInt(TestConfig.FileToOpen));
	}

	/**
	 * Test if booleans can be set
	 */
	@Test
	void testBooleans() {
		config.set(TestConfig.FileToOpen, true);
		assertEquals(true, config.getBoolean(TestConfig.FileToOpen));
	}
	
	/**
	 * Test if deleting and unsetting a config value works
	 */
	@Test
	void testDeleteAndContains() {
		config.delete(TestConfig.FileToOpen);
		assertFalse(config.has(TestConfig.FileToOpen));
	}
	
	/**
	 * Test if resetting to default works
	 */
	@Test
	void resetToDefault() {
		config.reset(TestConfig.FileToOpen);
		assertEquals("", config.get(TestConfig.FileToOpen));
	}
}
