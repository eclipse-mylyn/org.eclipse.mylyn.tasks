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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.eclipse.mylar.internal.tasklist.util.HtmlStreamTokenizer;

/**
 * @author Rob Elves
 */
public class AttributeContainer implements Serializable {
	
	private static final long serialVersionUID = -3990742719133977940L;

	/** The keys for the report attributes */
	private ArrayList<String> attributeKeys;

	/** report attributes (status, resolution, etc.) */
	private HashMap<String, RepositoryTaskAttribute> attributes;
	
	public AttributeContainer() {
		attributeKeys = new ArrayList<String>();
		attributes = new HashMap<String, RepositoryTaskAttribute>();
	}
	
	public void addAttribute(String key, RepositoryTaskAttribute attribute) {
		if (!attributes.containsKey(attribute.getName())) {
			attributeKeys.add(key);
		}
		attributes.put(key, attribute);
	}
	
	public RepositoryTaskAttribute getAttribute(String key) {
		return attributes.get(key);
	}

	public void removeAttribute(Object key) {
		attributeKeys.remove(key);
		attributes.remove(key);
	}
	
	public List<RepositoryTaskAttribute> getAttributes() {
		ArrayList<RepositoryTaskAttribute> attributeEntries = new ArrayList<RepositoryTaskAttribute>(
				attributeKeys.size());
		for (Iterator<String> it = attributeKeys.iterator(); it.hasNext();) {
			String key = it.next();
			RepositoryTaskAttribute attribute = attributes.get(key);
			attributeEntries.add(attribute);
		}
		return attributeEntries;
	}

	public String getAttributeValue(String key) {
		RepositoryTaskAttribute attribute = getAttribute(key);
		if(attribute != null) {
			// TODO: unescape should happen on connector side not here
			return HtmlStreamTokenizer.unescape(attribute.getValue());
//			return attribute.getValue();
		}
		return "";
	}

	public void removeAllAttributes() {
		attributeKeys.clear();
		attributes.clear();
	}
}
