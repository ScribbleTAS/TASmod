package com.minecrafttas.tasmod.savestates.files;

import java.nio.file.Path;

import com.minecrafttas.mctcommon.file.AbstractDataFile;

@Deprecated
public class SavestateDataFile extends AbstractDataFile {

	@Deprecated
	public SavestateDataFile(Path file) {
		super(file, "savestatedata", "Data for this savestate from TASmod");
	}

	@Deprecated
	public enum DataValues {
		@Deprecated
		INDEX("currentIndex"),
		@Deprecated
		NAME("savestateName"),
		@Deprecated
		SEED("ktrngSeed");

		private String configname;

		private DataValues(String configname) {
			this.configname = configname;
		}

		@Deprecated
		public String getConfigName() {
			return configname;
		}
	}

	@Deprecated
	public void set(DataValues key, String val) {
		properties.setProperty(key.getConfigName(), val);
	}

	@Deprecated
	public String get(DataValues key) {
		return properties.getProperty(key.getConfigName());
	}
}
