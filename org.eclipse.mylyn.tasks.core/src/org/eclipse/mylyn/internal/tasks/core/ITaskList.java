/*******************************************************************************
 * Copyright (c) 2004, 2011 Tasktop Technologies and others.
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

import org.eclipse.mylyn.tasks.core.IRepositoryElement;
import org.eclipse.mylyn.tasks.core.ITask;

/**
 * @author Steffen Pingel
 * @author Robert Elves
 * @author Mik Kersten
 * @since 3.0
 */
public interface ITaskList {

	public abstract void addChangeListener(ITaskListChangeListener listener);

	public abstract void addQuery(RepositoryQuery query) throws IllegalArgumentException;

	/**
	 * Add orphaned task to the task list
	 */
	public abstract void addTask(ITask task) throws IllegalArgumentException;

	/**
	 * Precondition: {@code container} already exists in tasklist (be it a parent task, category, or query) If the
	 * parentContainer is null the task is considered an orphan and added to the appropriate repository's orphaned tasks
	 * container.
	 * 
	 * @param task
	 *            to be added
	 * @param container
	 *            task container, query or parent task must not be null
	 */
	public abstract boolean addTask(ITask task, AbstractTaskContainer parentContainer);

	public abstract void deleteCategory(AbstractTaskCategory category);

	public abstract void deleteQuery(RepositoryQuery query);

	/**
	 * TODO: refactor around querying containers for their tasks Task is removed from all containers: root, archive,
	 * category, and orphan bin Currently subtasks are not deleted but rather are rather potentially orphaned
	 */
	public abstract void deleteTask(ITask task);

	public abstract Set<AbstractTaskCategory> getCategories();

	public abstract Set<RepositoryQuery> getQueries();

	/**
	 * @since 2.0
	 */
	public abstract ITask getTask(String repositoryUrl, String taskId);

	/**
	 * @param task
	 *            list element
	 */
	public abstract void notifyElementChanged(IRepositoryElement element);

	public abstract void notifySynchronizationStateChanged(IRepositoryElement element);

	public abstract void removeChangeListener(ITaskListChangeListener listener);

	/**
	 * @since 3.0
	 */
	public abstract void removeFromContainer(AbstractTaskContainer container, ITask task);

}