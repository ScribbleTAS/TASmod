package com.minecrafttas.mctcommon.json;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * A fine grained gson replacement
 * 
 * @author Scribble
 */
public class FineGson {

	private Map<Class<?>, FineTypeAdapter> typeAdapters = new HashMap<>();

	private final Gson gsonInstance;

	public FineGson(Gson gsonInstance) {
		this.gsonInstance = gsonInstance;
	}

	public void registerTypeAdapter(Class<?> type) {
		if (type.isAnnotationPresent(FineMultiTarget.class)) {
			List<Class<?>> classes = Arrays.asList(type.getDeclaredClasses());
			for (Class<?> clazz : classes) {
				registerTypeAdapter(clazz);
			}
		}
		if (type.isAnnotationPresent(FineTarget.class)) {
			if (!FineTypeAdapter.class.isAssignableFrom(type)) {
				throw new RuntimeException(String.format("Trying to register type adapter %s, but it doesn't extend FineTypeAdapter", type.getName()));
			}
			Class<?> targetType = type.getAnnotation(FineTarget.class).value();
			FineTypeAdapter adapter;
			try {
				adapter = (FineTypeAdapter) type.newInstance();
			} catch (InstantiationException | IllegalAccessException e) {
				throw new RuntimeException(String.format("Failed to instantiate type adapter %s", type.getName()), e);
			}
			registerTypeAdapter(targetType, adapter);
		}
	}

	public void registerTypeAdapter(Class<?> type, FineTypeAdapter adapter) {
		typeAdapters.put(type, adapter);
	}

	public JsonElement serialize(Object obj) {
		Class<?> type = obj.getClass();
		return serialize(obj, type);
	}

	public JsonElement serialize(Object obj, Class<?> type) {
		if (!typeAdapters.containsKey(type))
			return gsonInstance.toJsonTree(obj);

		FineTypeAdapter adapter = typeAdapters.get(type);
		JsonElement out = adapter.serialize(obj, this, type).getAsJsonObject();

		Class<?> superclass = type.getSuperclass();
		if (superclass != Object.class) {
			JsonObject superObject = serialize(obj, superclass).getAsJsonObject();
			JsonObject jsonObj = out.getAsJsonObject();
			for (Entry<String, JsonElement> objects : superObject.entrySet()) {
				jsonObj.add(objects.getKey(), objects.getValue());
			}
			out = jsonObj;
		}

		return out;
	}

	public Object deserialize(JsonElement element, Class<?> type) {
		if (!typeAdapters.containsKey(type))
			return gsonInstance.fromJson(element, type);

		FineTypeAdapter adapter = typeAdapters.get(type);
		Object out = adapter.deserialize(element, type, this);

		Class<?> superclass = type.getSuperclass();
		if (superclass != Object.class) {
			FineTypeAdapter superadapter = typeAdapters.get(superclass);
			out = superadapter.deserialize(element, superclass, this, out);
		}
		return out;
	}

	public Gson getGsonInstance() {
		return gsonInstance;
	}

	public Map<Class<?>, FineTypeAdapter> getTypeAdapters() {
		return typeAdapters;
	}
}
