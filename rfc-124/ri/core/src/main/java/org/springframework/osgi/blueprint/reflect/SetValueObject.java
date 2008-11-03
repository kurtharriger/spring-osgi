package org.springframework.osgi.blueprint.reflect;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.osgi.service.blueprint.reflect.SetValue;

public class SetValueObject implements SetValue {
	
	private Set<Object> delegate = new HashSet<Object>();

	public boolean add(Object o) {
		return this.delegate.add(o);
	}

	@SuppressWarnings("unchecked")
	public boolean addAll(Collection c) {
		return this.delegate.addAll(c);
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

	public boolean isEmpty() {
		return this.delegate.isEmpty();
	}

	@SuppressWarnings("unchecked")
	public Iterator iterator() {
		return this.delegate.iterator();
	}

	public boolean remove(Object o) {
		return this.delegate.remove(o);
	}

	@SuppressWarnings("unchecked")
	public boolean removeAll(Collection c) {
		return this.delegate.removeAll(c);
	}

	@SuppressWarnings("unchecked")
	public boolean retainAll(Collection c) {
		return this.delegate.retainAll(c);
	}

	public int size() {
		return this.delegate.size();
	}

	public Object[] toArray() {
		return this.delegate.toArray();
	}

	@SuppressWarnings("unchecked")
	public Object[] toArray(Object[] a) {
		return this.delegate.toArray(a);
	}

}
