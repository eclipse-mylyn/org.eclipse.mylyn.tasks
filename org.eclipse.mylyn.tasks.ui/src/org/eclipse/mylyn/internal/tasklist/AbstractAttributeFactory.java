/*******************************************************************************
 * Copyright (c) 2004 - 2006 University Of British Columbia and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     University Of British Columbia - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylar.internal.tasklist;

/**
 * @author Rob Elves
 */
public abstract class AbstractAttributeFactory {

	public RepositoryTaskAttribute createAttribute(String key) {
		String mapped = mapCommonAttributeKey(key);
		return new RepositoryTaskAttribute(mapped, getName(mapped), getIsHidden(mapped));
	}

	public void addAttributeValue(RepositoryTaskData taskData, String key, String value) {
		String mapped = mapCommonAttributeKey(key);
		RepositoryTaskAttribute attrib = taskData.getAttribute(mapped);
		if (attrib != null) {
			attrib.addValue(value);
		} else {
			attrib = createAttribute(mapped);
			attrib.addValue(value);
			taskData.addAttribute(mapped, attrib);
		}
	}
	
	/**
	 * sets a value on an attribute, if attribute doesn't exist, appropriate
	 * attribute is created
	 */
	public void setAttributeValue(RepositoryTaskData taskData, String key, String value) {
		String mapped = mapCommonAttributeKey(key);
		RepositoryTaskAttribute attrib = taskData.getAttribute(mapped);
		if (attrib == null) {
			attrib = createAttribute(mapped);
			taskData.addAttribute(mapped, attrib);
		}
		attrib.setValue(value);
	}
	
	public abstract String mapCommonAttributeKey(String key);
	
	public abstract boolean getIsHidden(String key);

	public abstract String getName(String key);
}
