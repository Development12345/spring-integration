/*
 * Copyright 2002-2010 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.springframework.integration.store;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Dave Syer
 * 
 * @since 2.0
 *
 */
public abstract class AbstractMessageGroupStore implements MessageGroupStore, Iterable<MessageGroup> {

	protected final Log logger = LogFactory.getLog(getClass());

	private Collection<MessageGroupCallback> expiryCallbacks = new LinkedHashSet<MessageGroupCallback>();

	/**
	 * 
	 */
	public AbstractMessageGroupStore() {
		super();
	}

	/**
	 * Convenient injection point for expiry callbacks in the message store. Each of the callbacks provided will simply
	 * be registered with the store using {@link #registerExpiryCallback(MessageGroupCallback)}.
	 * 
	 * @param expiryCallbacks the expiry callbacks to add
	 */
	public void setExpiryCallbacks(Collection<MessageGroupCallback> expiryCallbacks) {
		for (MessageGroupCallback callback : expiryCallbacks) {
			registerExpiryCallback(callback);
		}
	}

	public void registerExpiryCallback(MessageGroupCallback callback) {
		expiryCallbacks.add(callback);
	}

	public int expireMessageGroups(long timeout) {
		int count = 0;
		long threshold = System.currentTimeMillis() - timeout;
		for (MessageGroup group : this) {
			if (group.getTimestamp() < threshold) {
				count++;
				expire(group);
				removeMessageGroup(group.getCorrelationKey());
			}
		}
		return count;
	}
	
	public abstract Iterator<MessageGroup> iterator();

	private void expire(MessageGroup group) {
	
		RuntimeException exception = null;
	
		for (MessageGroupCallback callback : expiryCallbacks) {
			try {
				callback.execute(group);
			} catch (RuntimeException e) {
				if (exception == null) {
					exception = e;
				}
				logger.error("Exception in expiry callback", e);
			}
		}
	
		if (exception != null) {
			throw exception;
		}
	
	}

}