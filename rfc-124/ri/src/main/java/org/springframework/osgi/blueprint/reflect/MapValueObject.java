package org.springframework.osgi.blueprint.reflect;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.osgi.service.blueprint.reflect.MapValue;

public class MapValueObject implements MapValue {

	private Map<Object,Object> delegate = new HashMap<Object, Object>();
	
	public void clear() {
		this.delegate.clear();
	}

	public boolean containsKey(Object key) {
		return this.delegate.containsKey(key);
	}

	public boolean containsValue(Object value) {
		return this.delegate.containsValue(value);
	}

	@SuppressWarnings("unchecked")
	public Set entrySet() {
		return this.delegate.entrySet();
	}

	public Object get(Object key) {
		return this.delegate.get(key);
	}

	public boolean isEmpty() {
		return this.delegate.isEmpty();
	}

	@SuppressWarnings("unchecked")
	public Set keySet() {
		return this.delegate.keySet();
	}

	public Object put(Object key, Object value) {
		return this.delegate.put(key, value);
	}

	@SuppressWarnings("unchecked")
	public void putAll(Map t) {
		this.delegate.putAll(t);
	}

	public Object remove(Object key) {
		return this.delegate.remove(key);
	}

	public int size() {
		return this.delegate.size();
	}

	@SuppressWarnings("unchecked")
	public Collection values() {
		return this.delegate.values();
	}

}
