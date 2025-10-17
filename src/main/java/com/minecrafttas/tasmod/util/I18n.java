package com.minecrafttas.tasmod.util;

public class I18n {

	public static String format(String string, Object... args) {
		return net.minecraft.client.resources.I18n.format(string, args);
	}

	public static boolean hasKey(String key) {
		return net.minecraft.client.resources.I18n.hasKey(key);
	}
}
