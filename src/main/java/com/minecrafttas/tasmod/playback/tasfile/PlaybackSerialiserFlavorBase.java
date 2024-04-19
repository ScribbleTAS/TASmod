package com.minecrafttas.tasmod.playback.tasfile;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.dselent.bigarraylist.BigArrayList;
import com.minecrafttas.tasmod.playback.PlaybackControllerClient;
import com.minecrafttas.tasmod.playback.PlaybackControllerClient.TickInputContainer;

public abstract class PlaybackSerialiserFlavorBase {
	

	public abstract String flavorName();
	
	public void serialise(PlaybackControllerClient controller, File file) {
		
	}
	
	public BigArrayList<String> serialise(BigArrayList<TickInputContainer> container) {
		BigArrayList<String> out = new BigArrayList<>();
		return out;
	}
	
	public BigArrayList<TickInputContainer> deserialise(BigArrayList<String> lines) {
		BigArrayList<TickInputContainer> out = new BigArrayList<>();
		return out;
	}
	
	public List<String> serialiseMetadata(){
		return null;
	}
	
	public void deserialiseMetadata(List<String> metadataString) {
		
	}
	
	public static String createCenteredHeading(String text, char spacingChar, int headingWidth) {
		
		if(text == null || text.isEmpty()) {
			return createPaddedString(spacingChar, headingWidth);
		}
		
		text = " "+text+" ";
		
		int spacingWidth = headingWidth - text.length();
		
		String paddingPre = createPaddedString(spacingChar, spacingWidth % 2 == 1 ? spacingWidth / 2 + 1 : spacingWidth / 2);
		String paddingSuf = createPaddedString(spacingChar, spacingWidth/2);
		
		return String.format("%s%s%s", paddingPre, text, paddingSuf);
	}
	
	private static String createPaddedString(char spacingChar, int width) {
		char[] spacingLine = new char[width];
		for (int i = 0; i < spacingLine.length; i++) {
			spacingLine[i] = spacingChar;
		}
		return new String(spacingLine);
	}
}
