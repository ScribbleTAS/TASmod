package com.minecrafttas.mctcommon;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.InvalidPropertiesFormatException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Properties;

import com.minecrafttas.mctcommon.Configuration.ConfigOptions;
import com.minecrafttas.mctcommon.registry.AbstractRegistry;
import com.minecrafttas.mctcommon.registry.Registerable;

/**
 * A <i>very</i> simple configuration class
 * 
 * @author Scribble
 */

public class Configuration extends AbstractRegistry<ConfigOptions> {

	private File file;

	private Properties properties;

	private String comment;

	public Configuration(String comment, File configFile) {
		super("Configuration", new LinkedHashMap<>());

		file = configFile;
		this.comment = comment;
	}

	protected final List<ConfigOptions> configRegistry = new ArrayList<>();
	
	@Override
	public void register(ConfigOptions registryObject) {
		if(registryObject == null) {
			return;
		}
		
		if(configRegistry.contains(registryObject)) {
			return;
		}
		
		configRegistry.add(registryObject);
	}
	
	@Override
	public void unregister(ConfigOptions registryObject) {
		if (registryObject == null) {
			return;
		}

		if (!configRegistry.contains(registryObject)) {
			return;
		}
		
		configRegistry.remove(registryObject);
	}

	public void load() {
		if (file.exists()) {
			properties = loadInner();
		}
		if (properties == null || !file.exists()) {
			properties = generateDefault();
			save();
		}		
	}
	
	private Properties loadInner() {
		FileInputStream fis;
		Properties newProp = new Properties();
		try {
			fis = new FileInputStream(file);
			newProp.loadFromXML(fis);
			fis.close();
		} catch (InvalidPropertiesFormatException e) {
			MCTCommon.LOGGER.error("The config file could not be read", e);
			return null;
		} catch (FileNotFoundException e) {
			MCTCommon.LOGGER.warn("No config file found: {}", file);
			return null;
		} catch (IOException e) {
			MCTCommon.LOGGER.error("An error occured while reading the config", e);
			return null;
		}
		return newProp;
	}

	public void save() {
		save(file);
	}

	public void save(File file) {
		try {
			FileOutputStream fos = new FileOutputStream(file);
			properties.storeToXML(fos, comment, "UTF-8");
			fos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public Properties generateDefault() {
		Properties newProperties = new Properties();
		configRegistry.forEach((configOption)->{
			newProperties.put(configOption.getConfigKey(), configOption.getDefaultValue());
		});
		return newProperties;
	}

	public String get(ConfigOptions configOption) {
		return properties.getProperty(configOption.getConfigKey(), configOption.getDefaultValue());
	}

	public int getInt(ConfigOptions configOption) {
		return Integer.parseInt(get(configOption));
	}

	public boolean getBoolean(ConfigOptions configOption) {
		return Boolean.parseBoolean(get(configOption));
	}

	public boolean has(ConfigOptions configOption) {
		return properties.contains(configOption.getConfigKey());
	}

	public void set(ConfigOptions configOption, String value) {
		if(properties == null) {
			throw new NullPointerException("Config needs to be loaded first, before trying to set a value");
		}
		properties.setProperty(configOption.getConfigKey(), value);
		save();
	}

	public void set(ConfigOptions configOption, int value) {
		String val = Integer.toString(value);
		set(configOption, val);
	}

	public void set(ConfigOptions configOption, boolean value) {
		String val = Boolean.toString(value);
		set(configOption, val);
	}

	public void reset(ConfigOptions configOption) {
		set(configOption, configOption.getDefaultValue());
	}

	public void delete(ConfigOptions configOption) {
		properties.remove(configOption);
		save();
	}

	public interface ConfigOptions extends Registerable {

		public String getDefaultValue();

		public String getConfigKey();
	}
}
