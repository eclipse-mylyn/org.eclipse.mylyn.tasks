/*******************************************************************************
 * Copyright (c) 2004, 2010 Frank Becker and others.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Frank Becker - initial API and implementation
 *     Tasktop Technologies - improvements
 *******************************************************************************/

package org.eclipse.mylyn.tasks.tests;

import junit.framework.TestCase;

import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.mylyn.internal.tasks.core.TaskComment;
import org.eclipse.mylyn.internal.tasks.ui.actions.NewTaskFromSelectionAction;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.core.data.TaskAttributeMapper;
import org.eclipse.mylyn.tasks.core.data.TaskData;
import org.eclipse.mylyn.tasks.tests.connector.MockRepositoryConnector;
import org.eclipse.mylyn.tasks.tests.connector.MockTask;

/**
 * @author Frank Becker
 * @author Steffen Pingel
 */
public class NewTaskFromSelectionActionTest extends TestCase {

	// FIXME causes display of modal dialog
//	public void testNoSelection() throws Exception {
//		NewTaskFromSelectionAction action = new NewTaskFromSelectionAction();
//		assertNull(action.getTaskSelection());
//		action.run();
//		action.selectionChanged(null);
//		assertNull(action.getTaskSelection());
//	}

	public void testComment() throws Exception {
		TaskRepository taskRepository = new TaskRepository(MockRepositoryConnector.CONNECTOR_KIND,
				MockRepositoryConnector.REPOSITORY_URL);
		TaskData taskData = new TaskData(new TaskAttributeMapper(taskRepository), "kind", "http://url", "1");
		TaskComment comment = new TaskComment(taskRepository, new MockTask("1"), taskData.getRoot().createAttribute(
				"id"));

		NewTaskFromSelectionAction action = new NewTaskFromSelectionAction();
		action.selectionChanged(new StructuredSelection(comment));
		assertNotNull(action.getTaskMapping());
	}

	public void testText() throws Exception {
		NewTaskFromSelectionAction action = new NewTaskFromSelectionAction();
		action.selectionChanged(new TextSelection(0, 0) {
			@Override
			public String getText() {
				return "text";
			}
		});
		assertNotNull(action.getTaskMapping());

		action.selectionChanged(new TextSelection(0, 0));
		assertNull(action.getTaskMapping());
	}

}
