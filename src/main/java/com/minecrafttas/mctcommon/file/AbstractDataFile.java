package com.minecrafttas.mctcommon.file;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.InvalidPropertiesFormatException;
import java.util.Map.Entry;
import java.util.Properties;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
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
		this.properties = new Properties();

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

	public void load() {
		if (Files.exists(file)) {
			load(file);
		}
	}

	public void load(Path file) {
		InputStream fis;
		Properties newProp = new Properties();
		try {
			fis = Files.newInputStream(file);
			newProp.load(fis);
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
	 * Loads the xml {@link #file} into {@link #properties} if it exists
	 */
	public void loadFromXML() {
		if (Files.exists(file)) {
			loadFromXML(file);
		}
	}

	/**
	 * @param file The xml file to load into {@link #properties}
	 */
	public void loadFromXML(Path file) {
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

	public void loadFromJson() {
		loadFromJson(file);
	}

	public void loadFromJson(Path file) {
		//@formatter:off
		Gson json = new GsonBuilder()
				.registerTypeAdapter(Properties.class, new PropertiesDeserializer())
				.create();
		//@formatter:on

		String in;
		try {
			in = new String(Files.readAllBytes(file));
		} catch (IOException e) {
			MCTCommon.LOGGER.catching(e);
			return;
		}

		properties = json.fromJson(in, Properties.class);
	}

	public void save() {
		this.save(file);
	}

	public void save(Path file) {
		try {
			OutputStream fos = Files.newOutputStream(file);
			properties.store(fos, comment);
			fos.close();
		} catch (IOException e) {
			MCTCommon.LOGGER.catching(e);
		}
	}

	/**
	 * Saves the {@link #properties} to the {@link #file} location
	 */
	public void saveToXML() {
		this.saveToXML(file);
	}

	/**
	 * Saves the {@link #properties} to a specified file
	 * @param file The file to save the {@link #properties} to
	 */
	public void saveToXML(Path file) {
		try {
			OutputStream fos = Files.newOutputStream(file);
			properties.storeToXML(fos, comment, "UTF-8");
			fos.close();
		} catch (IOException e) {
			MCTCommon.LOGGER.catching(e);
		}
	}

	public void saveToJson() {
		saveToJson(file);
	}

	public void saveToJson(Path file) {
		//@formatter:off
		Gson json = new GsonBuilder()
				.registerTypeAdapter(Properties.class, new PropertiesSerializer())
				.setPrettyPrinting()
				.create();
		//@formatter:on
		try {
			String element = json.toJson(properties);
			Files.write(file, element.getBytes(), StandardOpenOption.WRITE, StandardOpenOption.CREATE);
		} catch (IOException e) {
			MCTCommon.LOGGER.catching(e);
		}
	}

	public class PropertiesSerializer implements JsonSerializer<Properties> {

		@Override
		public JsonElement serialize(Properties src, Type typeOfSrc, JsonSerializationContext context) {
			JsonObject obj = new JsonObject();
			src.forEach((key, val) -> {
				obj.addProperty((String) key, (String) val);
			});
			return obj;
		}
	}

	public class PropertiesDeserializer implements JsonDeserializer<Properties> {

		@Override
		public Properties deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			Properties properties = new Properties();
			JsonObject obj = json.getAsJsonObject();
			for (Entry<String, JsonElement> elem : obj.entrySet()) {
				String key = elem.getKey();
				String val = elem.getValue().getAsString();
				properties.put(key, val);
			}
			return properties;
		}
	}
}
