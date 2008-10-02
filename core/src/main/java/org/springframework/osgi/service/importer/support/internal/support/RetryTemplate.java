/*
 * Copyright 2006-2008 the original author or authors.
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

package org.springframework.osgi.service.importer.support.internal.support;

import org.springframework.util.Assert;

/**
 * Wrapper retry template. This class does specialized retries using a given
 * callback and lock.
 * 
 * @author Costin Leau
 */
public class RetryTemplate {

	private static final int hashCode = RetryTemplate.class.hashCode() * 13;

	public static final long DEFAULT_WAIT_TIME = 1000;

	public static final int DEFAULT_RETRY_NUMBER = 3;

	private final Object monitor = new Object();
	private final Object notificationLock;

	private long waitTime = DEFAULT_WAIT_TIME;

	private int retryNumbers = DEFAULT_RETRY_NUMBER;


	public RetryTemplate(int retryNumbers, long waitTime, Object notificationLock) {
		Assert.isTrue(retryNumbers >= 0, "retryNumbers must be positive");
		Assert.isTrue(waitTime >= 0, "waitTime must be positive");
		Assert.notNull(notificationLock, "notificationLock must be non null");

		synchronized (monitor) {
			this.retryNumbers = retryNumbers;
			this.waitTime = waitTime;
			this.notificationLock = notificationLock;
		}
	}

	public RetryTemplate(Object notificationLock) {
		this(DEFAULT_RETRY_NUMBER, DEFAULT_WAIT_TIME, notificationLock);
	}

	/**
	 * Main retry method. Executes the callback until it gets completed. The
	 * callback will get executed the number of {@link #retryNumbers} while
	 * waiting in-between for the {@link #DEFAULT_WAIT_TIME} amount.
	 * 
	 * Before bailing out, the callback will be called one more time. Thus, in
	 * case of being unsuccessful, the default value of the callback is
	 * returned.
	 * 
	 * @param callback
	 * @return
	 */
	public Object execute(RetryCallback callback) {
		Assert.notNull(callback, "callback is required");
		Assert.notNull(notificationLock, "notificationLock is required");

		long waitTime;
		int retryNumbers;

		synchronized (monitor) {
			waitTime = this.waitTime;
			retryNumbers = this.retryNumbers;
		}

		int count = -1;
		synchronized (notificationLock) {
			do {
				Object result = callback.doWithRetry();
				if (callback.isComplete(result))
					return result;

				if (waitTime > 0) {
					// Do NOT use Thread.sleep() here - it does not release
					// locks.
					try {
						notificationLock.wait(waitTime);
					}
					catch (InterruptedException ex) {
						throw new RuntimeException("Retry failed; interrupted while waiting", ex);
					}
				}
				count++;

				// handle reset cases
				synchronized (monitor) {
					// has there been a reset in place ?
					if (waitTime != this.waitTime || retryNumbers != this.retryNumbers) {
						// start counting again
						count = -1;
						waitTime = this.waitTime;
						retryNumbers = this.retryNumbers;
					}
				}
			} while (count < retryNumbers);
		}
		return callback.doWithRetry();
	}

	/**
	 * Reset the retry template, by applying the new values. Any in-flight
	 * waiting is interrupted and restarted using the new values.
	 * 
	 * @param retriesNumber
	 * @param waitTime
	 */
	public void reset(int retriesNumber, long waitTime) {
		synchronized (monitor) {
			this.retryNumbers = retriesNumber;
			this.waitTime = waitTime;
		}

		synchronized (notificationLock) {
			notificationLock.notifyAll();
		}
	}

	public int getRetryNumbers() {
		synchronized (monitor) {
			return retryNumbers;
		}
	}

	public long getWaitTime() {
		synchronized (monitor) {
			return waitTime;
		}
	}

	public boolean equals(Object other) {
		if (this == other)
			return true;
		if (other instanceof RetryTemplate) {
			RetryTemplate oth = (RetryTemplate) other;

			return (getWaitTime() == oth.getWaitTime() && getRetryNumbers() == oth.getRetryNumbers());
		}
		return false;
	}

	public int hashCode() {
		return hashCode;
	}
}
