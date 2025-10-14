package com.minecrafttas.tasmod.util;

import java.util.function.UnaryOperator;

import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;

/**
 * 1.20 Style Component library backported to 1.12
 * 
 * @author Scribble
 */
public class Component {

	private final ITextComponent component;
	private Style style = new Style();

	private Component(ITextComponent component) {
		this.component = component;
	}

	public Component withStyle(TextFormatting... colors) {
		for (TextFormatting color : colors) {
			switch (color) {
				case BOLD:
					style.setBold(true);
					break;

				case ITALIC:
					style.setItalic(true);
					break;

				case UNDERLINE:
					style.setUnderlined(true);
					break;

				case STRIKETHROUGH:
					style.setStrikethrough(true);
					break;

				case OBFUSCATED:
					style.setObfuscated(true);
					break;

				case RESET:
					style.setBold(false);
					style.setItalic(false);
					style.setUnderlined(false);
					style.setStrikethrough(false);
					style.setObfuscated(false);
					style.setColor(TextFormatting.WHITE);
					break;
				default:
					style.setColor(color);
					break;
			}
		}
		return this;
	}

	public Component withStyle(UnaryOperator<Style> unaryOperator) {
		style = unaryOperator.apply(style);
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

	public static Component wrap(Component saveComponent, TextFormatting color) {
		return wrap(saveComponent).withStyle(color);
	}

	public static Component wrap(Component component) {
		return Component.literal(String.format("[%s]", component.build().getFormattedText()));
	}

	public static class CClickEvent {
		public static ClickEvent create(ClickEvent.Action action, String string) {
			return new ClickEvent(action, string);
		}
	}

	public static class CHoverEvent {
		public static HoverEvent create(HoverEvent.Action action, Component component) {
			return new HoverEvent(action, component.build());
		}
	}
}
