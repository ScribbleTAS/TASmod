package com.minecrafttas.tasmod.registries;

import com.minecrafttas.mctcommon.Configuration.ConfigOptions;

public enum TASmodConfig implements ConfigOptions {
	FileToOpen("fileToOpen", ""),
	ServerConnection("serverConnection", "");

	private String configKey;
	private String defaultValue;
	
	private TASmodConfig(String configKey, String defaultValue) {
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
		return "TASmodConfig";
	}

}
