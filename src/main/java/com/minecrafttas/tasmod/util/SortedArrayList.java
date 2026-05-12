package com.minecrafttas.tasmod.util;

import java.util.ArrayList;
import java.util.Comparator;

/**
 * Sorts the ArrayList everytime an element is added/removed.
 * @param <E> Type The type of the ArrayList
 * @author Scribble
 */
public class SortedArrayList<E> extends ArrayList<E> {

	private final Comparator<E> comparable;

	public SortedArrayList(Comparator<E> comparable) {
		this.comparable = comparable;
	}

	@Override
	public boolean add(E e) {
		boolean out = super.add(e);
		sort(comparable);
		return out;
	}

	@Override
	public E remove(int index) {
		sort(comparable);
		return super.remove(index);
	}
}
