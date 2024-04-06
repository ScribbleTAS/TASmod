package com.minecrafttas.tasmod.playback;

import java.util.ArrayList;
import java.util.List;

public abstract class PlaybackSerialiserBase {
	
	public PlaybackSerialiserBase(PlaybackControllerClient controller) {
		if(controller == null) {
			throw new NullPointerException("Parameter controller can't be null");
		}
		
		
	}
	
	public void onSave() {
		
	}
	
	public void onLoad() {
		
	}
	
	public List<String> serialize() {
		List<String> out = new ArrayList<>();
		return out;
	}
	
	public void deserialize(List<String> in) {
		
	}
	
	public List<String> serializeMetadata(){
		return null;
	}
	
	public void deserializeMetadata(List<String> metadataString) {
		
	}
}
