package com.minecrafttas.mctcommon.file;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.InvalidPropertiesFormatException;
import java.util.Properties;

import com.minecrafttas.mctcommon.MCTCommon;

public abstract class AbstractDataFile {

	/**
	 * The save location of this data file
	 */
	protected final Path file;

	/**
	 * The name of this data file, used in logging
	 */
	protected final String name;
	/**
	 * The comment stored in the data file, to help recognize the file
	 */
	protected final String comment;

	/**
	 * The properties of this data file.
	 */
	protected Properties properties;

	/**
	 * Creates an abstract data file and creates it's directory if it doesn't exist
	 * @param file The {@link #file save location} of the data file
	 * @param name The {@link #name} of the data file, used in logging
	 * @param comment The {@link #comment} in the data file
	 */
	protected AbstractDataFile(Path file, String name, String comment) {
		this.file = file;
		this.name = name;
		this.comment = comment;

		createDirectory(file);
	}

	/**
	 * Creates the directory for the file if it doesn't exist
	 * @param file The file to create the directory for
	 */
	protected void createDirectory(Path file) {
		try {
			Files.createDirectories(file.getParent());
		} catch (IOException e) {
			MCTCommon.LOGGER.catching(e);
		}
	}

	/**
	 * Loads the {@link #file} into {@link #properties} if it exists
	 */
	public void load() {
		if (Files.exists(file)) {
			load(file);
		}
	}

	/**
	 * @param file The file to load into {@link #properties}
	 */
	public void load(Path file) {
		InputStream fis;
		Properties newProp = new Properties();
		try {
			fis = Files.newInputStream(file);
			newProp.loadFromXML(fis);
			fis.close();
		} catch (InvalidPropertiesFormatException e) {
			MCTCommon.LOGGER.error("The {} file could not be read", name, e);
			return;
		} catch (FileNotFoundException e) {
			MCTCommon.LOGGER.warn("No {} file found: {}", name, file);
			return;
		} catch (IOException e) {
			MCTCommon.LOGGER.error("An error occured while reading the {} file", file, e);
			return;
		}
		this.properties = newProp;
	}

	/**
	 * Saves the {@link #properties} to the {@link #file} location
	 */
	public void save() {
		this.save(file);
	}

	/**
	 * Saves the {@link #properties} to a specified file
	 * @param file The file to save the {@link #properties} to
	 */
	public void save(Path file) {
		try {
			OutputStream fos = Files.newOutputStream(file);
			properties.storeToXML(fos, comment, "UTF-8");
			fos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
