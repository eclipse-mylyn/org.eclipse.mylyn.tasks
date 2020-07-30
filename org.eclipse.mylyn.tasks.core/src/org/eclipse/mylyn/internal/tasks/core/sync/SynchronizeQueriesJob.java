/*******************************************************************************
 * Copyright (c) 2004, 2015 Tasktop Technologies and others.
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

package org.eclipse.mylyn.internal.tasks.core.sync;

import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.mylyn.commons.core.StatusHandler;
import org.eclipse.mylyn.commons.net.Policy;
import org.eclipse.mylyn.internal.tasks.core.AbstractTask;
import org.eclipse.mylyn.internal.tasks.core.ITasksCoreConstants;
import org.eclipse.mylyn.internal.tasks.core.ITasksCoreConstants.ObjectSchedulingRule;
import org.eclipse.mylyn.internal.tasks.core.RepositoryQuery;
import org.eclipse.mylyn.internal.tasks.core.TaskList;
import org.eclipse.mylyn.internal.tasks.core.data.TaskDataManager;
import org.eclipse.mylyn.tasks.core.AbstractRepositoryConnector;
import org.eclipse.mylyn.tasks.core.IRepositoryModel;
import org.eclipse.mylyn.tasks.core.ITask;
import org.eclipse.mylyn.tasks.core.ITask.SynchronizationState;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.core.data.TaskData;
import org.eclipse.mylyn.tasks.core.data.TaskDataCollector;
import org.eclipse.mylyn.tasks.core.data.TaskRelation;
import org.eclipse.mylyn.tasks.core.sync.ISynchronizationSession;
import org.eclipse.mylyn.tasks.core.sync.SynchronizationJob;
import org.eclipse.osgi.util.NLS;

/**
 * @author Mik Kersten
 * @author Rob Elves
 * @author Steffen Pingel
 */
public class SynchronizeQueriesJob extends SynchronizationJob {

	private class TaskCollector extends TaskDataCollector {

		private final Set<ITask> removedQueryResults;

		private final RepositoryQuery repositoryQuery;

		private final SynchronizationSession session;

		public TaskCollector(RepositoryQuery repositoryQuery, SynchronizationSession session) {
			this.repositoryQuery = repositoryQuery;
			this.session = session;
			this.removedQueryResults = new HashSet<ITask>(repositoryQuery.getChildren());
		}

		@Override
		public void accept(TaskData taskData) {
			ITask task = taskList.getTask(taskData.getRepositoryUrl(), taskData.getTaskId());
			if (task == null) {
				task = tasksModel.createTask(repository, taskData.getTaskId());
				((AbstractTask) task).setSynchronizationState(SynchronizationState.INCOMING_NEW);
				if (taskData.isPartial() && connector.canSynchronizeTask(repository, task)) {
					session.markStale(task);
				}
			} else {
				removedQueryResults.remove(task);
			}
			taskList.addTask(task, repositoryQuery);
			try {
				session.putTaskData(task, taskData);
			} catch (CoreException e) {
				StatusHandler.log(new Status(IStatus.ERROR, ITasksCoreConstants.ID_PLUGIN, "Failed to save task", e)); //$NON-NLS-1$
			}
		}

		public Set<ITask> getRemovedChildren() {
			return removedQueryResults;
		}

		@Override
		public void failed(String taskId, IStatus status) {
			ITask task = taskList.getTask(repositoryQuery.getRepositoryUrl(), taskId);
			if (task != null && status != null) {
				statuses.add(status);
				((AbstractTask) task).setStatus(status);
				if (!isUser()) {
					((AbstractTask) task).setSynchronizing(false);
				}
				taskList.notifyElementChanged(task);
			}
		}

	}

	private final AbstractRepositoryConnector connector;

	private final Set<RepositoryQuery> queries;

	private final TaskRepository repository;

	private final TaskDataManager taskDataManager;

	private final TaskList taskList;

	private final IRepositoryModel tasksModel;

	private final List<IStatus> statuses;

	public SynchronizeQueriesJob(TaskList taskList, TaskDataManager taskDataManager, IRepositoryModel tasksModel,
			AbstractRepositoryConnector connector, TaskRepository repository, Set<RepositoryQuery> queries) {
		super(Messages.SynchronizeQueriesJob_Synchronizing_Queries + " (" + repository.getRepositoryLabel() + ")"); //$NON-NLS-1$//$NON-NLS-2$
		this.taskList = taskList;
		this.taskDataManager = taskDataManager;
		this.tasksModel = tasksModel;
		this.connector = connector;
		this.repository = repository;
		this.queries = queries;
		this.statuses = new ArrayList<IStatus>();
	}

	@Override
	public IStatus run(IProgressMonitor jobMonitor) {
		try {
			monitor.setCanceled(false);
			monitor.attach(jobMonitor);
			try {
				monitor.beginTask(Messages.SynchronizeQueriesJob_Processing, 20 + queries.size() * 20 + 40 + 10);

				Set<ITask> allTasks;
				if (!isFullSynchronization()) {
					allTasks = new HashSet<ITask>();
					for (RepositoryQuery query : queries) {
						allTasks.addAll(query.getChildren());
					}
				} else {
					allTasks = taskList.getTasks(repository.getRepositoryUrl());
				}

				ObjectSchedulingRule rule = new ObjectSchedulingRule(repository);
				try {
					Job.getJobManager().beginRule(rule, monitor);

					final Map<String, TaskRelation[]> relationsByTaskId = new HashMap<String, TaskRelation[]>();
					SynchronizationSession session = new SynchronizationSession(taskDataManager) {
						@Override
						public void putTaskData(ITask task, TaskData taskData) throws CoreException {
							boolean changed = connector.hasTaskChanged(repository, task, taskData);
							taskDataManager.putUpdatedTaskData(task, taskData, isUser(), this, monitor);
							if (taskData.isPartial()) {
								if (changed && connector.canSynchronizeTask(repository, task)) {
									markStale(task);
								}
							} else {
								Collection<TaskRelation> relations = connector.getTaskRelations(taskData);
								if (relations != null) {
									relationsByTaskId.put(task.getTaskId(), relations.toArray(new TaskRelation[0]));
								}
							}
						}
					};
					session.setTaskRepository(repository);
					session.setFullSynchronization(isFullSynchronization());
					session.setTasks(Collections.unmodifiableSet(allTasks));
					session.setNeedsPerformQueries(true);
					session.setUser(isUser());

					updateQueryStatus(null);
					try {
						boolean success = preSynchronization(session, new SubProgressMonitor(monitor, 20));

						if ((success && session.needsPerformQueries()) || isUser()) {
							// synchronize queries, tasks changed within query are added to set of tasks to be synchronized
							synchronizeQueries(monitor, session);
						} else {
							monitor.worked(queries.size() * 20);
						}
					} finally {
						for (RepositoryQuery repositoryQuery : queries) {
							repositoryQuery.setSynchronizing(false);
						}
						taskList.notifySynchronizationStateChanged(queries);
					}

					Set<ITask> tasksToBeSynchronized = new HashSet<ITask>();
					for (ITask task : session.getStaleTasks()) {
						tasksToBeSynchronized.add(task);
						((AbstractTask) task).setSynchronizing(true);
					}

					// synchronize tasks that were marked by the connector
					SynchronizeTasksJob job = new SynchronizeTasksJob(taskList, taskDataManager, tasksModel, connector,
							repository, tasksToBeSynchronized);
					job.setFetchSubtasks(getFetchSubtasks());
					job.setUser(isUser());
					job.setSession(session);
					if (!tasksToBeSynchronized.isEmpty()) {
						Policy.checkCanceled(monitor);
						IStatus result = job.run(new SubProgressMonitor(monitor, 30));
						if (result == Status.CANCEL_STATUS) {
							throw new OperationCanceledException();
						}
						statuses.addAll(job.getStatuses());
					}
					monitor.subTask(Messages.SynchronizeQueriesJob_Receiving_related_tasks);
					job.synchronizedTaskRelations(monitor, relationsByTaskId);
					monitor.worked(10);

					session.setChangedTasks(tasksToBeSynchronized);
					if (statuses.size() > 0) {
						Status status = new MultiStatus(ITasksCoreConstants.ID_PLUGIN, 0,
								statuses.toArray(new IStatus[0]), "Query synchronization failed", null); //$NON-NLS-1$
						session.setStatus(status);
					}

					// hook into the connector for synchronization time stamp management
					postSynchronization(session, new SubProgressMonitor(monitor, 10));
				} finally {
					Job.getJobManager().endRule(rule);
				}
			} catch (OperationCanceledException e) {
				return Status.CANCEL_STATUS;
			} catch (Exception e) {
				StatusHandler
						.log(new Status(IStatus.ERROR, ITasksCoreConstants.ID_PLUGIN, "Synchronization failed", e)); //$NON-NLS-1$
			} catch (LinkageError e) {
				StatusHandler.log(new Status(IStatus.ERROR, ITasksCoreConstants.ID_PLUGIN,
						NLS.bind("Synchronization for connector ''{0}'' failed", connector.getConnectorKind()), e)); //$NON-NLS-1$
			} finally {
				monitor.done();
			}
		} finally {
			monitor.detach(jobMonitor);
		}
		return Status.OK_STATUS;
	}

	private void synchronizeQueries(IProgressMonitor monitor, SynchronizationSession session) {
		for (RepositoryQuery repositoryQuery : queries) {
			Policy.checkCanceled(monitor);
			monitor.subTask(MessageFormat.format(Messages.SynchronizeQueriesJob_Synchronizing_query_X,
					repositoryQuery.getSummary()));
			synchronizeQuery(repositoryQuery, session, new SubProgressMonitor(monitor, 20));
		}
	}

	private boolean postSynchronization(SynchronizationSession event, IProgressMonitor monitor) {
		try {
			Policy.checkCanceled(monitor);
			monitor.subTask(Messages.SynchronizeQueriesJob_Updating_repository_state);
			if (!isUser()) {
				monitor = Policy.backgroundMonitorFor(monitor);
			}
			connector.postSynchronization(event, monitor);
			return true;
		} catch (CoreException e) {
			updateQueryStatus(e.getStatus());
			return false;
		}
	}

	private boolean preSynchronization(ISynchronizationSession event, IProgressMonitor monitor) {
		try {
			Policy.checkCanceled(monitor);
			monitor.subTask(Messages.SynchronizeQueriesJob_Querying_repository);
			if (!isUser()) {
				monitor = Policy.backgroundMonitorFor(monitor);
			}
			connector.preSynchronization(event, monitor);
			return true;
		} catch (CoreException e) {
			// synchronization is unlikely to succeed, inform user and exit
			updateQueryStatus(e.getStatus());
			statuses.add(e.getStatus());
			return false;
		}
	}

	private void synchronizeQuery(RepositoryQuery repositoryQuery, SynchronizationSession event,
			IProgressMonitor monitor) {
		TaskCollector collector = new TaskCollector(repositoryQuery, event);

		if (!isUser()) {
			monitor = Policy.backgroundMonitorFor(monitor);
		}
		IStatus result = connector.performQuery(repository, repositoryQuery, collector, event, monitor);
		if (result == null || result.isOK()) {
			Set<ITask> removedChildren = collector.getRemovedChildren();
			if (!removedChildren.isEmpty()) {
				taskList.removeFromContainer(repositoryQuery, removedChildren);
			}

			repositoryQuery.setLastSynchronizedStamp(new SimpleDateFormat("MMM d, H:mm:ss").format(new Date())); //$NON-NLS-1$
		} else if (result.getSeverity() == IStatus.CANCEL) {
			throw new OperationCanceledException();
		} else {
			repositoryQuery.setStatus(result);
			statuses.add(result);
		}
	}

	private void updateQueryStatus(final IStatus status) {
		for (RepositoryQuery repositoryQuery : queries) {
			repositoryQuery.setStatus(status);
		}
		taskList.notifySynchronizationStateChanged(queries);
	}

	public Collection<IStatus> getStatuses() {
		return Collections.unmodifiableCollection(statuses);
	}

}
