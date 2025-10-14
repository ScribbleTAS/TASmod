package com.minecrafttas.tasmod.util;

import java.util.function.UnaryOperator;

import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;

/**
 * 1.20 Style Component library backported to 1.12
 * 
 * @author Scribble
 */
public class Component {

	private final ITextComponent component;
	private final Style style = new Style();

	private Component(ITextComponent component) {
		this.component = component;
	}

	public Component withStyle(TextFormatting color) {
		style.setColor(color);
		return this;
	}

	public Component withStyle(UnaryOperator<Style> unaryOperator) {
		unaryOperator.apply(style);
		return this;
	}

	public ITextComponent build() {
		return component.setStyle(style);
	}

	public static Component literal(String text) {
		return new Component(new TextComponentString(text));
	}

	public static Component translatable(String string) {
		return translatable(string, new Object[] {});
	}

	public static Component translatable(String string, Object... objects) {
		for (int i = 0; i < objects.length; i++) {
			Object object = objects[i];
			if (object instanceof Component) {
				objects[i] = ((Component) object).build();
			}
		}
		return new Component(new TextComponentTranslation(string, objects));
	}

	public static Component wrap(ITextComponent component, TextFormatting color) {
		return wrap(component).withStyle(color);
	}

	public static Component wrap(ITextComponent component) {
		return Component.literal(String.format("[%]", component.getFormattedText()));
	}

	public static class ClickEvent {
		public static net.minecraft.util.text.event.ClickEvent create(net.minecraft.util.text.event.ClickEvent.Action action, String string) {
			return new net.minecraft.util.text.event.ClickEvent(action, string);
		}
	}

	public static class HoverEvent {
		public static net.minecraft.util.text.event.HoverEvent create(net.minecraft.util.text.event.HoverEvent.Action action, ITextComponent string) {
			return new net.minecraft.util.text.event.HoverEvent(action, string);
		}
	}
}
