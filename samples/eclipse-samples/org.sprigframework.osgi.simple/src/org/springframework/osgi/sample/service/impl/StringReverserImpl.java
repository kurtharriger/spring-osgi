/*
 * Copyright 2006 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.osgi.sample.service.impl;

import org.apache.log4j.Logger;
import org.springframework.osgi.sample.service.StringReverser;
/**
 * Service implementation
 * 
 * @author Oleg Zhurakousky
 */
public class StringReverserImpl implements StringReverser {
	private static Logger logger = Logger.getLogger(StringReverserImpl.class);

	/**
	 * Will reverse the order of the string passed
	 */
	@Override
	public String reverse(String str) {
		logger.info("Receiveing string: " + str);
		char[] chars = str.toCharArray();
		StringBuffer buf = new StringBuffer();

		for (int i = chars.length - 1, x = 0; i >= 0; i--, x++) {
			buf.append(chars[i]);
		}
		String reveresed = buf.toString();
		logger.info("Returning string: " + reveresed);
		return reveresed;
	}
}
