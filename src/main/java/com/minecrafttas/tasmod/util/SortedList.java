package com.minecrafttas.tasmod.util;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.ListIterator;

/**
 * Sorts the ArrayList every time an element is added/removed.
 * @param <E> Type The type of the ArrayList
 * @author Scribble
 */
public class SortedList<E> extends LinkedList<E> {

	private final Comparator<E> comparable;

	public SortedList(Comparator<E> comparable) {
		this.comparable = comparable;
	}

	@Override
	public boolean add(E newElement) {
		ListIterator<E> iterator = (ListIterator<E>) iterator();
		while (iterator.hasNext()) {
			E element = iterator.next();
			if (comparable.compare(element, newElement) >= 0) {
				iterator.set(newElement);
				iterator.add(element);
				return true;
			}
		}
		super.add(newElement);
		return true;
	}
}
