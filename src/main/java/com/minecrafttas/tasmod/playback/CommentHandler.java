package com.minecrafttas.tasmod.playback;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.dselent.bigarraylist.BigArrayList;

/**
 * Handles and stores everything related to comments in the TASfile.<br>
 * Comments	also contain calls to playback extensions.
 * 
 * @author Scribble
 */
public class CommentHandler {

	/**
	 * List of all inline comments.<br>
	 * These comments take the form:
	 * 
	 * <pre>
	 * // This is an inline comment
	 * // This is a second inline comment
	 * 1|W;w|;0;0;0|0.0;0.0
	 * 	1|||1.0;1.0
	 * </pre>
	 * 
	 * Inline comments are supposed to describe the tick as a whole and therefore
	 * can not be attached to subticks.<br>
	 * like so:
	 * 
	 * <pre>
	 * 1|W;w|;0;0;0|0.0;0.0
	 * // This is not allowed. This comment won't be saved
	 * 	1|||1.0;1.0
	 * </pre>
	 * 
	 * The index of the BigArrayList is the current tick.
	 */
	private BigArrayList<ArrayList<String>> inlineComments;

	/**
	 * List of all endline comments.<br>
	 * These comments take the form:
	 * 
	 * <pre>
	 * 1|W;w|;0;0;0|0.0;0.0		// This is an endline comment
	 * 	1|||1.0;1.0		// This is a second endline comment
	 * </pre>
	 * 
	 * Endline comments are supposed to describe individual subticks.<br>
	 * The index of the BigArrayList is the tick, the index of the ArrayList inside
	 * of the BigArrayList is the subtick of that tick
	 */
	private BigArrayList<ArrayList<String>> endlineComments;

	public CommentHandler() {
		inlineComments = new BigArrayList<>();
		endlineComments = new BigArrayList<>();
	}

	public void addInlineComment(ArrayList<String> inlineCommentList) {
		this.inlineComments.add(inlineCommentList);
	}
	
	public void addEndlineComment(ArrayList<String> endlineCommentList) {
		this.endlineComments.add(endlineCommentList);
	}
	
	public void runEndlineComment(long index) {
		
	}
	
	public void clear() {
		try {
			inlineComments.clearMemory();
		} catch (IOException e) {
			e.printStackTrace();
		}
		inlineComments = new BigArrayList<>();

		try {
			endlineComments.clearMemory();
		} catch (IOException e) {
			e.printStackTrace();
		}
		endlineComments = new BigArrayList<>();
	}
}
