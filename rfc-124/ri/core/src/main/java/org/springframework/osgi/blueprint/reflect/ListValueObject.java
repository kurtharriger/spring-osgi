/**
 * 
 */
package org.springframework.osgi.blueprint.reflect;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.osgi.service.blueprint.reflect.ListValue;

/**
 * @author acolyer
 *
 */
public class ListValueObject implements ListValue {
	
	private ArrayList<Object> delegate = new ArrayList<Object>();

	public boolean add(Object o) {
		return this.delegate.add(o);
	}

	public void add(int index, Object element) {
		this.delegate.add(index,element);
	}

	@SuppressWarnings("unchecked")
	public boolean addAll(Collection c) {
		return this.delegate.addAll(c);
	}

	@SuppressWarnings("unchecked")
	public boolean addAll(int index, Collection c) {
		return this.delegate.addAll(index,c);
	}

	public void clear() {
		this.delegate.clear();
	}

	public boolean contains(Object o) {
		return this.delegate.contains(o);
	}

	@SuppressWarnings("unchecked")
	public boolean containsAll(Collection c) {
		return this.delegate.containsAll(c);
	}

	public Object get(int index) {
		return this.delegate.get(index);
	}

	public int indexOf(Object o) {
		return this.delegate.indexOf(o);
	}

	public boolean isEmpty() {
		return this.delegate.isEmpty();
	}

	@SuppressWarnings("unchecked")
	public Iterator iterator() {
		return this.delegate.iterator();
	}

	public int lastIndexOf(Object o) {
		return this.delegate.lastIndexOf(o);
	}

	@SuppressWarnings("unchecked")
	public ListIterator listIterator() {
		return this.delegate.listIterator();
	}

	@SuppressWarnings("unchecked")
	public ListIterator listIterator(int index) {
		return this.delegate.listIterator(index);
	}

	public boolean remove(Object o) {
		return this.delegate.remove(o);
	}

	public Object remove(int index) {
		return this.delegate.remove(index);
	}

	@SuppressWarnings("unchecked")
	public boolean removeAll(Collection c) {
		return this.delegate.removeAll(c);
	}

	@SuppressWarnings("unchecked")
	public boolean retainAll(Collection c) {
		return this.delegate.retainAll(c);
	}

	public Object set(int index, Object element) {
		return this.delegate.set(index,element);
	}

	public int size() {
		return this.delegate.size();
	}

	@SuppressWarnings("unchecked")
	public List subList(int fromIndex, int toIndex) {
		return this.delegate.subList(fromIndex, toIndex);
	}

	public Object[] toArray() {
		return this.delegate.toArray();
	}

	@SuppressWarnings("unchecked")
	public Object[] toArray(Object[] a) {
		return this.delegate.toArray(a);
	}

}
