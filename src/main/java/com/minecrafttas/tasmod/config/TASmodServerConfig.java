package com.minecrafttas.tasmod.config;

import com.minecrafttas.mctcommon.ConfigurationRegistry.ConfigOptions;

public enum TASmodServerConfig implements ConfigOptions {
	PauseOnTempSavestate("pauseOnTempSavestate", "false");

	private String configKey;
	private String defaultValue;

	private TASmodServerConfig(String configKey, String defaultValue) {
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
		return "TASmodClientConfig";
	}
}
