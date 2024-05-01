package com.minecrafttas.tasmod.playback.tasfile.flavor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.dselent.bigarraylist.BigArrayList;
import com.minecrafttas.tasmod.playback.PlaybackControllerClient.TickInputContainer;
import com.minecrafttas.tasmod.playback.metadata.PlaybackMetadata;
import com.minecrafttas.tasmod.virtual.VirtualCameraAngle;
import com.minecrafttas.tasmod.virtual.VirtualKeyboard;
import com.minecrafttas.tasmod.virtual.VirtualMouse;

public abstract class PlaybackSerialiserFlavorBase {

	/**
	 * The current tick that is being serialised or deserialised
	 */
	private int currentTick;

	public abstract String flavorName();

	public List<String> serialiseHeader(List<PlaybackMetadata> metadataList) {
		List<String> out = new ArrayList<>();
		out.add(serialiseFlavorName());
//		out.add(serializeExtensionNames());
		out.addAll(serialiseMetadata(metadataList));
		return out;
	}

	protected String serialiseFlavorName() {
		return "Flavor:" + flavorName();
	}

	public List<String> serialiseMetadata(List<PlaybackMetadata> metadataList) {
		return null;
	}

	public BigArrayList<String> serialise(BigArrayList<TickInputContainer> inputs) {
		BigArrayList<String> out = new BigArrayList<>();

		for (int i = 0; i < inputs.size(); i++) {
			currentTick = i;
			TickInputContainer container = inputs.get(i);
			addAll(out, serialiseContainer(container));
		}
		return out;
	}

	protected List<String> serialiseContainer(TickInputContainer container) {
		List<String> serialisedKeyboard = serialiseKeyboard(container.getKeyboard());
		List<String> serialisedMouse = serialiseMouse(container.getMouse());
		List<String> serialisedCameraAngle = serialiseCameraAngle(container.getCameraAngle());

		return mergeInputs(serialisedKeyboard, serialisedMouse, serialisedCameraAngle);
	}

	protected abstract List<String> serialiseKeyboard(VirtualKeyboard keyboard);

	protected abstract List<String> serialiseMouse(VirtualMouse mouse);

	protected abstract List<String> serialiseCameraAngle(VirtualCameraAngle cameraAngle);

	protected List<String> mergeInputs(List<String> serialisedKeyboard, List<String> serialisedMouse, List<String> serialisedCameraAngle) {
		return null;
	}

	public BigArrayList<TickInputContainer> deserialise(BigArrayList<String> lines) {
		BigArrayList<TickInputContainer> out = new BigArrayList<>();
		return out;
	}

	public List<PlaybackMetadata> deserialiseMetadata(List<String> metadataString) {
		return null;
	}

	/**
	 * @return {@link #currentTick}
	 */
	public int getCurrentTick() {
		return currentTick;
	}

	public static String createCenteredHeading(String text, char spacingChar, int headingWidth) {

		if (text == null || text.isEmpty()) {
			return createPaddedString(spacingChar, headingWidth);
		}

		text = " " + text + " ";

		int spacingWidth = headingWidth - text.length();

		String paddingPre = createPaddedString(spacingChar, spacingWidth % 2 == 1 ? spacingWidth / 2 + 1 : spacingWidth / 2);
		String paddingSuf = createPaddedString(spacingChar, spacingWidth / 2);

		return String.format("%s%s%s", paddingPre, text, paddingSuf);
	}

	private static String createPaddedString(char spacingChar, int width) {
		char[] spacingLine = new char[width];
		for (int i = 0; i < spacingLine.length; i++) {
			spacingLine[i] = spacingChar;
		}
		return new String(spacingLine);
	}

	protected static <T extends Serializable> void addAll(BigArrayList<T> list, BigArrayList<T> toAdd) {
		for (int i = 0; i < toAdd.size(); i++) {
			T element = toAdd.get(i);
			list.add(element);
		}
	}

	protected static <T extends Serializable> void addAll(BigArrayList<T> list, List<T> toAdd) {
		for (int i = 0; i < toAdd.size(); i++) {
			T element = toAdd.get(i);
			list.add(element);
		}
	}
}
