package org.springframework.osgi.blueprint.reflect;

import org.osgi.service.blueprint.reflect.UnaryServiceReferenceComponentMetadata;

class MutableUnaryServiceReferenceComponentMetadata extends
		MutableServiceReferenceComponentMetadata implements
		UnaryServiceReferenceComponentMetadata {

	private long timeout = -1L;
	
	public MutableUnaryServiceReferenceComponentMetadata(String name) {
		super(name);
	}
	
	public long getTimeout() {
		return this.timeout;
	}
	
	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}

}
