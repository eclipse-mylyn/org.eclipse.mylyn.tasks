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

package org.eclipse.mylar.internal.tasks.ui.actions;

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.mylar.internal.tasks.ui.views.TaskListView;
import org.eclipse.mylar.tasks.core.AbstractQueryHit;
import org.eclipse.mylar.tasks.core.AbstractRepositoryQuery;
import org.eclipse.mylar.tasks.core.AbstractRepositoryTask;
import org.eclipse.mylar.tasks.core.ITask;
import org.eclipse.mylar.tasks.core.TaskCategory;
import org.eclipse.mylar.tasks.ui.AbstractRepositoryConnector;
import org.eclipse.mylar.tasks.ui.TasksUiPlugin;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionDelegate;
import org.eclipse.ui.actions.ActionFactory;

/**
 * @author Mik Kersten
 */
public class SynchronizeSelectedAction extends ActionDelegate implements IViewActionDelegate {

	private void checkSyncResult(final IJobChangeEvent event, final AbstractRepositoryQuery problemQuery) {
		if (event.getResult().getException() != null) {
			PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
				public void run() {
					MessageDialog.openError(Display.getDefault().getActiveShell(), TasksUiPlugin.TITLE_DIALOG, event
							.getResult().getMessage());
				}
			});
		}
	}
	
	public void run(IAction action) {
		if (TaskListView.getFromActivePerspective() != null) {
			ISelection selection = TaskListView.getFromActivePerspective().getViewer().getSelection();
			for (Object obj : ((IStructuredSelection) selection).toList()) {
				if (obj instanceof AbstractRepositoryQuery) {
					final AbstractRepositoryQuery repositoryQuery = (AbstractRepositoryQuery) obj;
					AbstractRepositoryConnector client = TasksUiPlugin.getRepositoryManager()
							.getRepositoryConnector(repositoryQuery.getRepositoryKind());
					if (client != null)
						client.synchronize(repositoryQuery, new JobChangeAdapter() {
							public void done(IJobChangeEvent event) {
								checkSyncResult(event, repositoryQuery);
							}
						});
				} else if (obj instanceof TaskCategory) {
					TaskCategory cat = (TaskCategory) obj;
					for (ITask task : cat.getChildren()) {
						if (task instanceof AbstractRepositoryTask) {
							AbstractRepositoryConnector client = TasksUiPlugin.getRepositoryManager()
									.getRepositoryConnector(((AbstractRepositoryTask) task).getRepositoryKind());
							if (client != null)
								client.forceRefresh((AbstractRepositoryTask) task);
						}
					}
				} else if (obj instanceof AbstractRepositoryTask) {
					AbstractRepositoryTask bugTask = (AbstractRepositoryTask) obj;
					AbstractRepositoryConnector client = TasksUiPlugin.getRepositoryManager()
							.getRepositoryConnector(bugTask.getRepositoryKind());
					if (client != null)
						client.forceRefresh(bugTask);
				} else if (obj instanceof AbstractQueryHit) {
					AbstractQueryHit hit = (AbstractQueryHit) obj;
					if (hit.getOrCreateCorrespondingTask() != null) {
						AbstractRepositoryConnector client = TasksUiPlugin.getRepositoryManager()
								.getRepositoryConnector(hit.getCorrespondingTask().getRepositoryKind());
						if (client != null)
							client.forceRefresh(hit.getCorrespondingTask());
					}
				}
			}
		}
		if (TaskListView.getFromActivePerspective() != null) {
			TaskListView.getFromActivePerspective().getViewer().refresh();
		}
	}
	
	private IAction action;
	
	@Override
	public void init(IAction action) {
		this.action = action;
	}
	
	public void init(IViewPart view) {
		IActionBars actionBars = view.getViewSite().getActionBars();
		actionBars.setGlobalActionHandler(ActionFactory.REFRESH.getId(), action);
		actionBars.updateActionBars();
	}
	
}
