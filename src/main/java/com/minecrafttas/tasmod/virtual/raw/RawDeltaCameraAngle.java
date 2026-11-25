package com.minecrafttas.tasmod.virtual.raw;

import java.io.Serializable;

public class RawDeltaCameraAngle implements Serializable {
	int deltaX;
	int deltaY;

	public RawDeltaCameraAngle(int deltaX, int deltaY) {
		this.deltaX = deltaX;
		this.deltaY = deltaY;
	}

	public int getDeltaX() {
		return deltaX;
	}

	public int getDeltaY() {
		return deltaY;
	}
}
