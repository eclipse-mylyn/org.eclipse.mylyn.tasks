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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.mylar.provisional.tasklist.MylarTaskListPlugin;
import org.eclipse.mylar.provisional.tasklist.TaskRepository;

/**
 * @author Rob Elves
 * @author Mik Kersten
 */
public class TaskListSynchronizationManager implements IPropertyChangeListener {

	private static final int DELAY_QUERY_REFRESH_ON_STARTUP = 10000;

	private ScheduledTaskListSynchJob refreshJob;

	private List<ScheduledTaskListSynchJob> jobs = new ArrayList<ScheduledTaskListSynchJob>();

	private List<ScheduledTaskListSynchJob> jobsQueue = Collections.synchronizedList(jobs);

	private final MutexRule rule = new MutexRule();
	
	public TaskListSynchronizationManager(boolean refreshOnStartup) {		
		boolean enabled = MylarTaskListPlugin.getMylarCorePrefs().getBoolean(
				TaskListPreferenceConstants.REPOSITORY_SYNCH_SCHEDULE_ENABLED);
		if (refreshOnStartup && enabled) {
			addJobToQueue(new ScheduledTaskListSynchJob(DELAY_QUERY_REFRESH_ON_STARTUP, MylarTaskListPlugin
					.getTaskListManager()));
		}
	}

	public synchronized void startSynchJob() {
		if (jobsQueue.size() == 0) {
			scheduleRegularBackupJob();
		}
		if (jobsQueue.size() > 0) {
			refreshJob = jobsQueue.remove(0);
			refreshJob.schedule(refreshJob.getScheduleDelay());
		}
	}

	private void scheduleRegularBackupJob() {
		boolean enabled = MylarTaskListPlugin.getMylarCorePrefs().getBoolean(
				TaskListPreferenceConstants.REPOSITORY_SYNCH_SCHEDULE_ENABLED);
		if (enabled) {
			long miliseconds = MylarTaskListPlugin.getMylarCorePrefs().getLong(
					TaskListPreferenceConstants.REPOSITORY_SYNCH_SCHEDULE_MILISECONDS);
			refreshJob = new ScheduledTaskListSynchJob(miliseconds, MylarTaskListPlugin.getTaskListManager());
			refreshJob.setRule(rule);
			addJobToQueue(refreshJob);
		}
	}

	private void addJobToQueue(final ScheduledTaskListSynchJob jobToAdd) {
		jobToAdd.addJobChangeListener(new JobChangeAdapter() {

			@Override
			public void done(IJobChangeEvent event) {
				synchronized (refreshJob) {
					if(refreshJob == jobToAdd && event.getResult() != Status.CANCEL_STATUS) {
						startSynchJob();						
					}	
				}				
			}
		});
		jobsQueue.add(jobToAdd);
	}

	
	/**
	 * @param delay  sync delay (ms)
	 * @param repositories used to scope sync to queries associated with given repositories, can be null (sync all repositories)
	 */
	public void synchNow(long delay, List<TaskRepository> repositories) {
		cancelAll();
		ScheduledTaskListSynchJob job = new ScheduledTaskListSynchJob(delay, MylarTaskListPlugin.getTaskListManager());
		job.setRepositories(repositories);
		addJobToQueue(job);
		startSynchJob();
	}

	public void propertyChange(PropertyChangeEvent event) {
		if (event.getProperty().equals(TaskListPreferenceConstants.REPOSITORY_SYNCH_SCHEDULE_ENABLED)
				|| event.getProperty().equals(TaskListPreferenceConstants.REPOSITORY_SYNCH_SCHEDULE_MILISECONDS)) {
			cancelAll();
			startSynchJob();
		}
	}

	public ScheduledTaskListSynchJob getRefreshJob() {
		return refreshJob;
	}

	public void cancelAll() {
		jobsQueue.clear();
		if (refreshJob != null) {
			if (!refreshJob.cancel()) {
				try {
					refreshJob.join();
				} catch (InterruptedException e) {
					// ignore
				}
			}
		}
	}
	
	class MutexRule implements ISchedulingRule {
	      public boolean isConflicting(ISchedulingRule rule) {
	         return rule == this;
	      }
	      public boolean contains(ISchedulingRule rule) {
	         return rule == this;
	      }
	   }
}
