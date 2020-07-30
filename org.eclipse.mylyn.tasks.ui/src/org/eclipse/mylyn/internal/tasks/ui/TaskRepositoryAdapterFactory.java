/*******************************************************************************
 * Copyright (c) 2004, 2009 Eugene Kuleshov and others.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Eugene Kuleshov - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylyn.internal.tasks.ui;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.mylyn.internal.tasks.core.LocalRepositoryConnector;
import org.eclipse.mylyn.tasks.core.AbstractRepositoryConnector;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.ui.AbstractRepositoryConnectorUi;
import org.eclipse.mylyn.tasks.ui.TasksUi;
import org.eclipse.ui.IActionFilter;

/**
 * Adapter factory for adapting TaskRepository to org.eclipse.ui.IActionFilter
 * 
 * @author Eugene Kuleshov
 */
public class TaskRepositoryAdapterFactory implements IAdapterFactory {

	@SuppressWarnings("rawtypes")
	private static final Class[] ADAPTER_TYPES = new Class[] { IActionFilter.class };

	@SuppressWarnings("rawtypes")
	public Class[] getAdapterList() {
		return ADAPTER_TYPES;
	}

	@SuppressWarnings("rawtypes")
	public Object getAdapter(final Object adaptable, Class adapterType) {
		if (adaptable instanceof TaskRepository) {
			return new IActionFilter() {
				public boolean testAttribute(Object target, String name, String value) {
					TaskRepository repository = (TaskRepository) target;
					if ("offline".equals(name)) { //$NON-NLS-1$
						return Boolean.valueOf(value).booleanValue() == repository.isOffline();
					} else if ("supportQuery".equals(name)) { //$NON-NLS-1$
						AbstractRepositoryConnectorUi connectorUi = TasksUiPlugin.getConnectorUi(repository.getConnectorKind());
						AbstractRepositoryConnector connector = TasksUiPlugin.getRepositoryManager()
								.getRepositoryConnector(repository.getConnectorKind());
						return null != connectorUi.getQueryWizard(repository, null) && connector.canQuery(repository);
					} else if ("supportNewTask".equals(name)) { //$NON-NLS-1$
						AbstractRepositoryConnector connector = TasksUi.getRepositoryManager().getRepositoryConnector(
								repository.getConnectorKind());
						return connector.canCreateNewTask(repository);
					} else if ("hasRepository".equals(name)) { //$NON-NLS-1$
						return !repository.getConnectorKind().equals(LocalRepositoryConnector.CONNECTOR_KIND);
					}
					return false;
				}
			};
		}
		return null;
	}

}
