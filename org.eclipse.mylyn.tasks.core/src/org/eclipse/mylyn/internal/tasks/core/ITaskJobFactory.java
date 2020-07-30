/*******************************************************************************
 * Copyright (c) 2004, 2014 Tasktop Technologies and others.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylyn.internal.tasks.core;

import java.util.Set;

import org.eclipse.mylyn.tasks.core.AbstractRepositoryConnector;
import org.eclipse.mylyn.tasks.core.ITask;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.core.data.AbstractTaskAttachmentSource;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.core.data.TaskData;
import org.eclipse.mylyn.tasks.core.sync.SubmitJob;
import org.eclipse.mylyn.tasks.core.sync.SynchronizationJob;
import org.eclipse.mylyn.tasks.core.sync.TaskJob;

/**
 * @author Steffen Pingel
 * @author Mik Kersten
 */
public interface ITaskJobFactory {

	public abstract SynchronizationJob createSynchronizeTasksJob(AbstractRepositoryConnector connector,
			TaskRepository taskRepository, Set<ITask> tasks);

	public abstract SynchronizationJob createSynchronizeQueriesJob(AbstractRepositoryConnector connector,
			TaskRepository repository, Set<RepositoryQuery> queries);

	public abstract SynchronizationJob createSynchronizeRepositoriesJob(Set<TaskRepository> repositories);

	public abstract SubmitJob createSubmitTaskJob(AbstractRepositoryConnector connector, TaskRepository taskRepository,
			ITask task, TaskData taskData, Set<TaskAttribute> changedAttributes);

	public abstract TaskJob createUpdateRepositoryConfigurationJob(AbstractRepositoryConnector connector,
			TaskRepository taskRepository, ITask task);

	@Deprecated
	public abstract TaskJob createUpdateRepositoryConfigurationJob(AbstractRepositoryConnector connector,
			TaskRepository taskRepository);

	public abstract SubmitJob createSubmitTaskAttachmentJob(AbstractRepositoryConnector connector,
			TaskRepository taskRepository, ITask task, AbstractTaskAttachmentSource source, String comment,
			TaskAttribute attachmentAttribute);

	/**
	 * Specify whether subtasks should be fetched as part of task synchronization. Defaults to true.
	 */
	public void setFetchSubtasks(boolean fetchSubtasks);

	/**
	 * @return whether subtasks should be fetched as part of task synchronization
	 */
	public boolean getFetchSubtasks();

}
