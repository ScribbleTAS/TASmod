package com.minecrafttas.tasmod.savestates.exceptions;

public class LoadstateException extends RuntimeException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 827439853208092307L;

	public LoadstateException(String s) {
		super(s);
	}

	public LoadstateException(Throwable t, String msg, Object... args) {
		super(String.format(msg, args), t);
	}
}
