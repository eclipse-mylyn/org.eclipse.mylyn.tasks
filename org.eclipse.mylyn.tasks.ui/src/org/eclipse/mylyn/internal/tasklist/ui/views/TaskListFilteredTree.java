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

package org.eclipse.mylar.internal.tasklist.ui.views;

import java.lang.reflect.Field;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.mylar.internal.core.util.MylarStatusHandler;
import org.eclipse.mylar.provisional.tasklist.ITask;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.internal.dialogs.FilteredTree;
import org.eclipse.ui.internal.dialogs.PatternFilter;

/**
 * @author Mik Kersten
 */
public class TaskListFilteredTree extends FilteredTree {

//	private static final int LABEL_MAX_SIZE = 30;

	private static final int DELAY_REFRESH = 700;

//	private static final String LABEL_NO_ACTIVE = "          <no active task>";
	
	private Job refreshJob;
	
//	private Hyperlink activeTaskLabel;
	
	public TaskListFilteredTree(Composite parent, int treeStyle, PatternFilter filter) {
		super(parent, treeStyle, filter);
		Field refreshField;
		try {
			// HACK: using reflection to gain access
			refreshField = FilteredTree.class.getDeclaredField("refreshJob");
			refreshField.setAccessible(true);
			refreshJob = (Job)refreshField.get(this);
		} catch (Exception e) {
			MylarStatusHandler.fail(e, "Could not get refresh job", false);
		}
	}

    protected void textChanged() {
    	textChanged(DELAY_REFRESH);
    }
    
    public void textChanged(int delay) {
    	if (refreshJob != null) {
    		refreshJob.schedule(delay);
    	} 
    }

    public void indicateActiveTask(ITask task) {
//    	String text = task.getDescription();
//    	if (text.length() > LABEL_MAX_SIZE) {
//    		text = text.substring(0, LABEL_MAX_SIZE);
//    	}
//    	activeTaskLabel.setText(text);
//		activeTaskLabel.setUnderlined(true);
    }
    
    public void indicateNoActiveTask() {
//    	activeTaskLabel.setText(LABEL_NO_ACTIVE);
//		activeTaskLabel.setUnderlined(false);
    }
    
}
