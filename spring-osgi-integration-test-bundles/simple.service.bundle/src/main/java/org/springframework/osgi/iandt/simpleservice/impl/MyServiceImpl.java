package org.springframework.osgi.iandt.simpleservice.impl;

import org.springframework.osgi.iandt.simpleservice.MyService;

/**
 * @author Hal Hildebrand
 *         Date: Dec 1, 2006
 *         Time: 3:06:01 PM
 */
public class MyServiceImpl implements MyService {  
    public String stringValue() {
		return "Bond.  James Bond.";
	}

}
