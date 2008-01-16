/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.osgi.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Utility class used for creating 'degradable' loggers for critical parts of
 * the applications. In the future, this class might be used across the entire
 * product.
 * 
 * @author Costin Leau
 * 
 */
class LogUtils {

	/**
	 * Create the logger using LogFactory but use a simple implementation if
	 * something goes wrong.
	 * 
	 * @param logName log name
	 * @return logger implementation
	 */
	public static Log createLogger(Class logName) {
		Log logger;
		try {
			logger = LogFactory.getLog(DebugUtils.class);
		}
		catch (Throwable th) {
			logger = new SimpleLogger();
		}
		return logger;
	}
}
