package org.springframework.osgi.samples.petclinic;

import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * Simple JavaBean domain object adds a name property to <code>BaseEntity</code>.
 * Used as a base class for objects needing these properties.
 *
 * @author Ken Krebs
 * @author Juergen Hoeller
 */
public class NamedEntity extends BaseEntity {

	private String name;
	
	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return this.name;
	}

	public String toString() {
		return ToStringBuilder.reflectionToString(this);
		//return "this is a new version";
	}
}
