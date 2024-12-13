package com.minecrafttas.tasmod.util;

import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import com.minecrafttas.tasmod.events.EventClient.EventDrawHotbarAlways;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;

/**
 * <p>A list of Log4J markers which can be added to logging statements.
 * 
 * <p>Simply add the marker as the first argument:
 * <pre>LOGGER.info({@linkplain LoggerMarkers}.{@link #Event}, "Message");</pre>
 * 
 * <p>You can then turn off log messages by adding a VM option to your run configuration:
 * <pre>-Dtasmod.marker.event=DENY</pre>
 * 
 * <p>To add new markers, follow the pattern in this class then head to src/main/resources/log4j.xml.
 * There you can add the marker to the 'Filters' xml-tag
 * 
 * <pre>
 * 	&lt;Filters&gt;
 * 		&lt;MarkerFilter marker="Event" onMatch="${sys:tasmod.marker.event:-ACCEPT}" onMismatch="NEUTRAL" /&gt;
 * 	&lt;/Filters&gt;
 * 	</pre>
 * 
 * @author Scribble
 *
 */
public class LoggerMarkers implements EventDrawHotbarAlways {

	public static final Marker Event = MarkerManager.getMarker("Event");

	public static final Marker Savestate = MarkerManager.getMarker("Savestate");

	public static final Marker Networking = MarkerManager.getMarker("Networking");

	public static final Marker Tickrate = MarkerManager.getMarker("Tickrate");

	public static final Marker Playback = MarkerManager.getMarker("Playback");

	public static final Marker Keyboard = MarkerManager.getMarker("Keyboard");

	public static final Marker Mouse = MarkerManager.getMarker("Mouse");

	@Override
	public void onDrawHotbarAlways() {
		ScaledResolution scaledresolution = new ScaledResolution(Minecraft.getMinecraft());
		float xpos = scaledresolution.getScaledWidth() / 2 - 2;
		float ypos = scaledresolution.getScaledHeight() - 50f;

		float scale = 1.26f;
		int rotate = 15;
		GlStateManager.translate(xpos, ypos, 0);
		GlStateManager.scale(scale, scale, 1);
		GlStateManager.rotate(rotate, 0, 0, 1);
		int oW = 0xCCAFA5;
		int o = 0xC24218;

		int w = 0xFFFFFF;
		int c = 0x546980;

		int y = 0;
		drawMarker(3, y, o);
		drawMarker(4, y, o);
		drawMarker(5, y, o);

		y = 1;
		drawMarker(2, y, w);
		drawMarker(3, y, oW);
		drawMarker(4, y, oW);
		drawMarker(5, y, oW);
		drawMarker(6, y, w);

		y = 2;
		drawMarker(2, y, w);
		drawMarker(3, y, o);
		drawMarker(4, y, o);
		drawMarker(5, y, o);
		drawMarker(6, y, w);

		y = 3;
		drawMarker(3, y, w);
		drawMarker(5, y, w);

		y = 4;
		drawMarker(3, y, w);
		drawMarker(5, y, w);

		y = 5;
		drawMarker(2, y, w);
		drawMarker(6, y, w);

		y = 6;
		drawMarker(1, y, w);
		drawMarker(2, y, c);
		drawMarker(3, y, w);
		drawMarker(4, y, c);
		drawMarker(5, y, c);
		drawMarker(6, y, c);
		drawMarker(7, y, w);

		y = 7;
		drawMarker(0, y, w);
		drawMarker(1, y, c);
		drawMarker(2, y, w);
		drawMarker(3, y, c);
		drawMarker(4, y, c);
		drawMarker(5, y, c);
		drawMarker(6, y, c);
		drawMarker(7, y, c);
		drawMarker(8, y, w);

		y = 8;
		drawMarker(0, y, w);
		drawMarker(1, y, c);
		drawMarker(2, y, w);
		drawMarker(3, y, c);
		drawMarker(4, y, c);
		drawMarker(5, y, c);
		drawMarker(6, y, c);
		drawMarker(7, y, c);
		drawMarker(8, y, w);

		y = 9;
		drawMarker(0, y, w);
		drawMarker(1, y, c);
		drawMarker(2, y, c);
		drawMarker(3, y, c);
		drawMarker(4, y, c);
		drawMarker(5, y, c);
		drawMarker(6, y, w);
		drawMarker(7, y, c);
		drawMarker(8, y, w);

		y = 10;
		drawMarker(0, y, w);
		drawMarker(1, y, c);
		drawMarker(2, y, c);
		drawMarker(3, y, c);
		drawMarker(4, y, c);
		drawMarker(5, y, c);
		drawMarker(6, y, w);
		drawMarker(7, y, c);
		drawMarker(8, y, w);

		y = 11;
		drawMarker(0, y, w);
		drawMarker(1, y, w);
		drawMarker(2, y, c);
		drawMarker(3, y, c);
		drawMarker(4, y, c);
		drawMarker(5, y, w);
		drawMarker(6, y, c);
		drawMarker(7, y, w);
		drawMarker(8, y, w);

		y = 12;
		drawMarker(2, y, w);
		drawMarker(3, y, w);
		drawMarker(4, y, w);
		drawMarker(5, y, w);
		drawMarker(6, y, w);
		GlStateManager.rotate(-rotate, 0, 0, 1);
		GlStateManager.scale(1 / scale, 1 / scale, 1);
		GlStateManager.translate(-xpos, -ypos, 0);
	}

	private void drawMarker(int posX, int posY, int textColor) {
		int alpha = 0x80000000;
		Gui.drawRect(posX, posY, posX + 1, posY + 1, textColor + alpha);
	}
}
