/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *    Chris Aniszczyk <zx@us.ibm.com> - bug 208819
 *******************************************************************************/

package org.eclipse.mylyn.internal.tasks.bugs.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.mylyn.commons.core.StatusHandler;
import org.eclipse.mylyn.internal.tasks.bugs.wizards.ErrorLogStatus;
import org.eclipse.pde.internal.runtime.logview.LogEntry;
import org.eclipse.pde.internal.runtime.logview.LogSession;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;

/**
 * Creates a new task from the selected error log entry.
 * 
 * @author Jeff Pound
 * @author Steffen Pingel
 */
public class NewTaskFromErrorAction implements IViewActionDelegate, ISelectionChangedListener {

	public static final String ID = "org.eclipse.mylyn.tasklist.ui.repositories.actions.create";

	private TreeViewer treeViewer;

	/**
	 * Fills a {@link StringBuilder} with {@link LogEntry} information, optionally including subentries too
	 * 
	 * @param entry
	 *            The {@link LogEntry} who provides the information
	 * @param sb
	 *            An {@link StringBuilder} to be filled with
	 * @param includeChildren
	 *            Indicates if it should include subentries, if the {@link LogEntry} have any
	 */
	private void buildDescriptionFromLogEntry(LogEntry entry, StringBuilder sb, boolean includeChildren) {
		sb.append("\n\n-- Error Log --\nDate: ");
		sb.append(entry.getDate());
		sb.append("\nMessage: ");
		sb.append(entry.getMessage());
		sb.append("\nSeverity: " + entry.getSeverityText());
		sb.append("\nPlugin ID: ");
		sb.append(entry.getPluginId());
		sb.append("\nStack Trace:\n");
		if (entry.getStack() == null) {
			sb.append("no stack trace available");
		} else {
			sb.append(entry.getStack());
		}

		if (includeChildren && entry.hasChildren()) {
			Object[] children = entry.getChildren(null);
			for (Object child : children) {
				if (child instanceof LogEntry) {
					buildDescriptionFromLogEntry((LogEntry) child, sb, includeChildren);
				}
			}
		}
	}

	private void createTask(LogEntry entry) {
		// FIXME reenable
//		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
//		boolean includeChildren = false;
//
//		if (entry.hasChildren()
//				&& MessageDialog.openQuestion(shell, "Report Bug", "Include children of this entry in the report?")) {
//			includeChildren = true;
//		}
//		StringBuilder sb = new StringBuilder();
//		buildDescriptionFromLogEntry(entry, sb, includeChildren);

		ErrorLogStatus status = new ErrorLogStatus(entry.getSeverity(), entry.getPluginId(), entry.getCode(),
				entry.getMessage());
		status.setDate(entry.getDate());
		status.setStack(entry.getStack());
		LogSession session = entry.getSession();
		if (session != null) {
			status.setLogSessionData(session.getSessionData());
		}
		StatusHandler.fail(status);
	}

	public void run() {
		TreeItem[] items = treeViewer.getTree().getSelection();
		LogEntry entry = null;
		if (items.length > 0) {
			entry = (LogEntry) items[0].getData();
		}
		if (entry == null) {
			return;
		}

		createTask(entry);
	}

	public void run(IAction action) {
		run();
	}

	public void init(IViewPart view) {
		ISelectionProvider sp = view.getViewSite().getSelectionProvider();
		sp.addSelectionChangedListener(this);
		sp.setSelection(sp.getSelection());
	}

	public void selectionChanged(IAction action, ISelection selection) {
		// this selection is always empty? explicitly register a listener in
		// init() instead
	}

	public void selectionChanged(SelectionChangedEvent event) {
		treeViewer = (TreeViewer) event.getSource();
	}

}
