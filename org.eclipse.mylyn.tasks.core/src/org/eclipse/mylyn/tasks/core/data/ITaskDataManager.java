/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.mylyn.tasks.core.data;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.mylyn.tasks.core.ITask;
import org.eclipse.mylyn.tasks.core.TaskRepository;

/**
 * @author Steffen Pingel
 * @since 3.0
 * @noimplement
 */
public interface ITaskDataManager {

	public ITaskDataWorkingCopy createWorkingCopy(ITask task, TaskData taskData);

	public abstract ITaskDataWorkingCopy getWorkingCopy(ITask task) throws CoreException;

	public abstract void discardEdits(ITask task) throws CoreException;

	public abstract TaskData getTaskData(ITask task) throws CoreException;

	public abstract TaskData getTaskData(TaskRepository task, String taskId) throws CoreException;

	public abstract boolean hasTaskData(ITask task);

}