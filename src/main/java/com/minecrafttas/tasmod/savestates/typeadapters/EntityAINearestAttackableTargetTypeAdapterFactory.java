package com.minecrafttas.tasmod.savestates.typeadapters;

import java.io.IOException;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;

public class EntityAINearestAttackableTargetTypeAdapterFactory implements TypeAdapterFactory {

	@SuppressWarnings({ "unchecked" })
	@Override
	public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
		Class<T> rawType = (Class<T>) type.getRawType();
		if (!EntityAINearestAttackableTarget.class.equals(rawType))
			return null;

		TypeAdapter<T> adapter = gson.getDelegateAdapter(this, type);

		return new TypeAdapter<T>() {

			@Override
			public void write(JsonWriter out, T value) throws IOException {
				adapter.write(out, value);
			}

			@Override
			public T read(JsonReader in) throws IOException {
				in.beginObject();
				while (in.hasNext()) {
					in.nextString();
				}
				in.endObject();
				T value = adapter.read(in);
				EntityAINearestAttackableTarget<? extends Entity> ai = (EntityAINearestAttackableTarget<? extends Entity>) value;

				return (T) ai;
			}
		};
	}

}
