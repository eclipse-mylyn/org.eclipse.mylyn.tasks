/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.mylyn.tasks.tests.performance;

import org.eclipse.mylyn.context.core.ContextCorePlugin;
import org.eclipse.mylyn.internal.tasks.core.ScheduledTaskContainer;
import org.eclipse.mylyn.internal.tasks.ui.actions.TaskActivateAction;
import org.eclipse.mylyn.tasks.core.AbstractTask;
import org.eclipse.mylyn.tasks.core.ITaskActivityListener;
import org.eclipse.mylyn.tasks.core.ITaskActivityListener2;
import org.eclipse.mylyn.tasks.tests.connector.MockRepositoryTask;
import org.eclipse.mylyn.tasks.ui.TaskListManager;
import org.eclipse.mylyn.tasks.ui.TasksUiPlugin;
import org.eclipse.test.performance.PerformanceTestCase;

/**
 * @author Jingwen Ou
 */
public class ActivateTaskPerformanceTest extends PerformanceTestCase {

	private class MockTaskActivityListener implements ITaskActivityListener2 {

		private boolean hasActivated = false;

		private boolean hasPreActivated = false;

		private boolean hasDeactivated = false;

		private boolean hasPreDeactivated = false;

		public void reset() {
			hasActivated = false;
			hasPreActivated = false;
			hasDeactivated = false;
			hasPreDeactivated = false;
		}

		public void preTaskActivated(AbstractTask task) {
			assertFalse(hasActivated);
			hasPreActivated = true;
		}

		public void preTaskDeactivated(AbstractTask task) {
			assertFalse(hasDeactivated);
			hasPreDeactivated = true;
		}

		public void activityChanged(ScheduledTaskContainer week) {
			// ignore
		}

		public void taskActivated(AbstractTask task) {
			assertTrue(hasPreActivated);
			hasActivated = true;
		}

		public void taskDeactivated(AbstractTask task) {
			assertTrue(hasPreDeactivated);
			hasDeactivated = true;
		}

		public void taskListRead() {
			// ignore
		}
	}

	private TaskListManager manager = TasksUiPlugin.getTaskListManager();

	@Override
	protected void setUp() throws Exception {
		super.setUp();

	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();

		manager.dispose();
	}

	public void testActivivateMockTask() {
		MockRepositoryTask task = new MockRepositoryTask("test:activation");
		MockTaskActivityListener listener = new MockTaskActivityListener();

		manager.addActivityListener(listener);

		for (int i = 0; i < 10; i++) {
			startMeasuring();
			manager.activateTask(task);
			stopMeasuring();
			//manager.deactivateTask(task);
		}

		manager.deactivateTask(task);
		manager.removeActivityListener(listener);

		commitMeasurements();
		assertPerformance();
	}

	public void testActivateLocalTask() {
		AbstractTask task = manager.createNewLocalTask("Owen's performance test");
		manager.getTaskList().addTask(task);

		for (int i = 0; i < 10; i++) {
			startMeasuring();
			(new TaskActivateAction()).run(task);
			stopMeasuring();
		}

		manager.getTaskList().deleteTask(task);

		commitMeasurements();
		assertPerformance();

	}

	public void testActivateContextTask() {
		MockRepositoryTask task = new MockRepositoryTask("test:activation");
		ITaskActivityListener CONTEXT_TASK_ACTIVITY_LISTENER = new ITaskActivityListener() {

			public void taskActivated(final AbstractTask task) {
				ContextCorePlugin.getContextManager().activateContext(task.getHandleIdentifier());
			}

			public void taskDeactivated(final AbstractTask task) {
				ContextCorePlugin.getContextManager().deactivateContext(task.getHandleIdentifier());
			}

			public void activityChanged(ScheduledTaskContainer week) {
				// ignore
			}

			public void taskListRead() {
				// ignore
			}
		};

		manager.addActivityListener(CONTEXT_TASK_ACTIVITY_LISTENER);

		for (int i = 0; i < 6401; i++) {
			startMeasuring();
			(new TaskActivateAction()).run(task);
			stopMeasuring();
			//manager.deactivateTask(task);
		}

		manager.deactivateTask(task);
		manager.removeActivityListener(CONTEXT_TASK_ACTIVITY_LISTENER);

		commitMeasurements();
		assertPerformance();

	}

}
