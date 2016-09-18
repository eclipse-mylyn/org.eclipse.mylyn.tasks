/*******************************************************************************
 * Copyright (c) 2013 Frank Becker and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Frank Becker - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylyn.internal.bugzilla.rest.core;

import java.io.InputStreamReader;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.eclipse.mylyn.commons.repositories.http.core.CommonHttpClient;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class BugzillaRestGetRequest<T> extends BugzillaRestRequest<T> {

	private final TypeToken<?> responseType;

	public BugzillaRestGetRequest(CommonHttpClient client, String urlSuffix, TypeToken<?> responseType) {
		super(client, urlSuffix, true);
		this.responseType = responseType;
	}

	public BugzillaRestGetRequest(CommonHttpClient client, String urlSuffix, TypeToken<?> responseType,
			boolean authenticationRequired) {
		super(client, urlSuffix, authenticationRequired);
		this.responseType = responseType;
	}

	@Override
	protected HttpRequestBase createHttpRequestBase(String url) {
		HttpRequestBase request = new HttpGet(url);
		request.setHeader(CONTENT_TYPE, TEXT_XML_CHARSET_UTF_8);
		return request;
	}

	@Override
	protected T parseFromJson(InputStreamReader in) throws BugzillaRestException {
		return new Gson().fromJson(in, responseType.getType());
	}
}
