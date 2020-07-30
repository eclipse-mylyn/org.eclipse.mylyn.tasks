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
 *     Frank Becker - improvements
 *     Perforce - enhancements for bug 319469
 *******************************************************************************/

package org.eclipse.mylyn.internal.tasks.ui.search;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.mylyn.commons.ui.CommonImages;
import org.eclipse.mylyn.commons.workbench.DecoratingPatternStyledCellLabelProvider;
import org.eclipse.mylyn.commons.workbench.EnhancedFilteredTree;
import org.eclipse.mylyn.commons.workbench.SubstringPatternFilter;
import org.eclipse.mylyn.internal.tasks.core.AbstractTask;
import org.eclipse.mylyn.internal.tasks.core.AbstractTaskCategory;
import org.eclipse.mylyn.internal.tasks.core.TaskGroup;
import org.eclipse.mylyn.internal.tasks.core.UnmatchedTaskContainer;
import org.eclipse.mylyn.internal.tasks.ui.actions.OpenTaskSearchAction;
import org.eclipse.mylyn.internal.tasks.ui.actions.OpenWithBrowserAction;
import org.eclipse.mylyn.internal.tasks.ui.search.SearchResultTreeContentProvider.GroupBy;
import org.eclipse.mylyn.internal.tasks.ui.util.TaskDragSourceListener;
import org.eclipse.mylyn.internal.tasks.ui.util.TasksUiInternal;
import org.eclipse.mylyn.internal.tasks.ui.views.TaskListToolTip;
import org.eclipse.mylyn.internal.tasks.ui.views.TaskListView;
import org.eclipse.mylyn.tasks.core.IRepositoryQuery;
import org.eclipse.mylyn.tasks.core.ITask;
import org.eclipse.mylyn.tasks.ui.ITasksUiConstants;
import org.eclipse.mylyn.tasks.ui.TasksUiImages;
import org.eclipse.search.ui.IContextMenuConstants;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.ISearchResult;
import org.eclipse.search.ui.text.AbstractTextSearchViewPage;
import org.eclipse.search.ui.text.Match;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.part.IShowInTargetList;

/**
 * Displays the results of a Repository search.
 * 
 * @see org.eclipse.search.ui.text.AbstractTextSearchViewPage
 * @author Rob Elves
 * @author Mik Kersten
 * @author Shawn Minto
 * @author Frank Becker
 * @author Steffen Pingel
 */
public class RepositorySearchResultView extends AbstractTextSearchViewPage implements IAdaptable {

	private class GroupingAction extends Action {

		private final GroupBy groupBy;

		public GroupingAction(String text, GroupBy groupBy) {
			super(text, IAction.AS_CHECK_BOX);
			this.groupBy = groupBy;
			groupingActions.add(this);
		}

		@Override
		public void run() {
			for (GroupingAction action : groupingActions) {
				action.setChecked(false);
			}

			SearchResultTreeContentProvider contentProvider = (SearchResultTreeContentProvider) getViewer().getContentProvider();
			if (contentProvider.getSelectedGroup() == groupBy) {
				contentProvider.setSelectedGroup(GroupBy.NONE);
			} else {
				contentProvider.setSelectedGroup(groupBy);
				setChecked(true);
			}
			getViewer().refresh();
		}
	}

	private class FilteringAction extends Action {

		private final ViewerFilter filter;

		public FilteringAction(String text, ViewerFilter filter) {
			super(text, IAction.AS_CHECK_BOX);
			this.filter = filter;
			filterActions.add(this);
		}

		@Override
		public void runWithEvent(Event event) {
			if (isChecked()) {
				getViewer().addFilter(filter);
			} else {
				getViewer().removeFilter(filter);
			}
		}
	}

	private static final String MEMENTO_KEY_SORT = "sort"; //$NON-NLS-1$

	private SearchResultContentProvider searchResultProvider;

	private final OpenSearchResultAction openInEditorAction;

	private final CreateQueryFromSearchAction createQueryAction;

	private final Action refineSearchAction;

	private static final String[] SHOW_IN_TARGETS = new String[] { ITasksUiConstants.ID_VIEW_TASKS };

	private TaskListToolTip toolTip;

	private final List<GroupingAction> groupingActions;

	private final List<FilteringAction> filterActions;

	private final OpenWithBrowserAction openSearchWithBrowserAction;

	private final SearchResultSorter searchResultSorter;

	private SearchResultSortAction sortByDialogAction;

	private DecoratingPatternStyledCellLabelProvider styledLabelProvider;

	private static final IShowInTargetList SHOW_IN_TARGET_LIST = new IShowInTargetList() {
		public String[] getShowInTargetIds() {
			return SHOW_IN_TARGETS;
		}
	};

	public RepositorySearchResultView() {
		// Only use the table layout.
		super(FLAG_LAYOUT_TREE);

		openInEditorAction = new OpenSearchResultAction(Messages.RepositorySearchResultView_Open_in_Editor, this);
		createQueryAction = new CreateQueryFromSearchAction(
				Messages.RepositorySearchResultView_Create_Query_from_Search_, this);
		refineSearchAction = new OpenTaskSearchAction();
		refineSearchAction.setText(Messages.RepositorySearchResultView_Refine_Search_);
		openSearchWithBrowserAction = new OpenWithBrowserAction();
		openSearchWithBrowserAction.setText(Messages.RepositorySearchResultView_Open_Search_with_Browser_Label);

		groupingActions = new ArrayList<GroupingAction>();
		GroupingAction groupByOwnerAction = new GroupingAction(Messages.RepositorySearchResultView_Group_By_Owner,
				GroupBy.OWNER);
		groupByOwnerAction.setImageDescriptor(CommonImages.PRESENTATION);
//		new GroupingAction(Messages.RepositorySearchResultView_Group_By_Complete, GroupBy.COMPLETION);

		filterActions = new ArrayList<FilteringAction>();
		FilteringAction filterCompleteAction = new FilteringAction(
				Messages.RepositorySearchResultView_Filter_Completed_Tasks, new ViewerFilter() {
					@Override
					public boolean select(Viewer viewer, Object parentElement, Object element) {
						if (element instanceof ITask) {
							return !((ITask) element).isCompleted();
						} else if (element instanceof TaskGroup) {
							TaskGroup taskGroup = (TaskGroup) element;
							return taskGroup.getHandleIdentifier().equals("group-incompleteIncomplete"); //$NON-NLS-1$
						}
						return true;
					}
				});
		filterCompleteAction.setImageDescriptor(CommonImages.FILTER_COMPLETE);

		// construct early since to be ready when restoreState() is invoked
		searchResultSorter = new SearchResultSorter();
	}

	@Override
	protected void elementsChanged(Object[] objects) {
		if (searchResultProvider != null) {
			searchResultProvider.elementsChanged(objects);
			getViewer().refresh();
		}
	}

	@Override
	protected void clear() {
		if (searchResultProvider != null) {
			searchResultProvider.clear();
			getViewer().refresh();
		}
	}

	// Allows the inherited method "getViewer" to be accessed publicly.
	@Override
	public StructuredViewer getViewer() {
		return super.getViewer();
	}

	@Override
	protected void configureTreeViewer(TreeViewer viewer) {
		viewer.setUseHashlookup(true);
		searchResultProvider = new SearchResultTreeContentProvider();
		viewer.setContentProvider(searchResultProvider);

		styledLabelProvider = new DecoratingPatternStyledCellLabelProvider(new SearchResultsLabelProvider(
				searchResultProvider, viewer), PlatformUI.getWorkbench().getDecoratorManager().getLabelDecorator(),
				null);
		viewer.setLabelProvider(styledLabelProvider);
		viewer.setSorter(searchResultSorter);

		Transfer[] dragTypes = new Transfer[] { LocalSelectionTransfer.getTransfer(), FileTransfer.getInstance() };

		getViewer().addDragSupport(DND.DROP_COPY | DND.DROP_MOVE | DND.DROP_LINK, dragTypes,
				new TaskDragSourceListener(getViewer()));

		sortByDialogAction = new SearchResultSortAction(this);

		toolTip = new TaskListToolTip(viewer.getControl());
	}

	@Override
	protected TreeViewer createTreeViewer(Composite parent) {
		// create a filtered tree
		Composite treeComposite = parent;
		Layout parentLayout = parent.getLayout();
		if (!(parentLayout instanceof GridLayout)) {
			treeComposite = new Composite(parent, SWT.NONE);
			GridLayout layout = new GridLayout();
			layout.marginHeight = 0;
			layout.marginWidth = 0;
			treeComposite.setLayout(layout);
		}

		FilteredTree searchTree = new EnhancedFilteredTree(treeComposite, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL,
				new SubstringPatternFilter() {

					@Override
					public void setPattern(String patternString) {
						styledLabelProvider.setPattern(patternString);
						super.setPattern(patternString);
					}

				}, true);
		return searchTree.getViewer();
	}

	@Override
	protected void configureTableViewer(TableViewer viewer) {
//		viewer.setUseHashlookup(true);
//		String[] columnNames = new String[] { "Summary" };
//		TableColumn[] columns = new TableColumn[columnNames.length];
//		int[] columnWidths = new int[] { 500 };
//		viewer.setColumnProperties(columnNames);
//
//		viewer.getTable().setHeaderVisible(false);
//		for (int i = 0; i < columnNames.length; i++) {
//			columns[i] = new TableColumn(viewer.getTable(), 0, i); // SWT.LEFT
//			columns[i].setText(columnNames[i]);
//			columns[i].setWidth(columnWidths[i]);
//			columns[i].setData(new Integer(i));
//			columns[i].addSelectionListener(new SelectionAdapter() {
//
//				@Override
//				public void widgetSelected(SelectionEvent e) {
//					TableColumn col = (TableColumn) e.getSource();
//					Integer integer = (Integer) col.getData();
//					setSortOrder(integer.intValue());
//				}
//			});
//		}
//
//		IThemeManager themeManager = getSite().getWorkbenchWindow().getWorkbench().getThemeManager();
//		Color categoryBackground = themeManager.getCurrentTheme().getColorRegistry().get(
//				TaskListColorsAndFonts.THEME_COLOR_TASKLIST_CATEGORY);
//
//		SearchViewTableLabelProvider taskListTableLabelProvider = new SearchViewTableLabelProvider(
//				new TaskElementLabelProvider(true),
//				PlatformUI.getWorkbench().getDecoratorManager().getLabelDecorator(), categoryBackground);
//
//		viewer.setLabelProvider(taskListTableLabelProvider);
//		viewer.setContentProvider(new SearchResultTableContentProvider(this));
//
//		// Set the order when the search view is loading so that the items are
//		// sorted right away
//		setSortOrder(currentSortOrder);
//
//		taskContentProvider = (SearchResultContentProvider) viewer.getContentProvider();
	}

	@Override
	public void dispose() {
		toolTip.dispose();
		super.dispose();
	}

	@SuppressWarnings("rawtypes")
	public Object getAdapter(Class adapter) {
		return getAdapterDelegate(adapter);
	}

	private Object getAdapterDelegate(Class<?> adapter) {
		if (IShowInTargetList.class.equals(adapter)) {
			return SHOW_IN_TARGET_LIST;
		}
		return null;
	}

	@Override
	protected void showMatch(Match match, int currentOffset, int currentLength, boolean activate)
			throws PartInitException {
		AbstractTask repositoryHit = (AbstractTask) match.getElement();
		TasksUiInternal.refreshAndOpenTaskListElement(repositoryHit);
	}

	@Override
	protected void fillContextMenu(IMenuManager menuManager) {
		super.fillContextMenu(menuManager);

		// open actions
		menuManager.appendToGroup(IContextMenuConstants.GROUP_OPEN, openInEditorAction);

		// Add to Task List menu
		// HACK: this should be a contribution
		final MenuManager subMenuManager = new MenuManager(MessageFormat.format(
				Messages.RepositorySearchResultView_Add_to_X_Category, TaskListView.LABEL_VIEW));
		List<AbstractTaskCategory> categories = new ArrayList<AbstractTaskCategory>(TasksUiInternal.getTaskList()
				.getCategories());
		Collections.sort(categories);
		for (final AbstractTaskCategory category : categories) {
			if (!(category instanceof UnmatchedTaskContainer)) {//.equals(TasksUiPlugin.getTaskList().getArchiveContainer())) {
				Action action = new Action() {
					@Override
					public void run() {
						moveToCategory(category);
					}
				};
				String text = category.getSummary();
				action.setText(text);
				action.setImageDescriptor(TasksUiImages.CATEGORY);
				subMenuManager.add(action);
			}
		}
		menuManager.appendToGroup(IContextMenuConstants.GROUP_OPEN, subMenuManager);

		// search actions

		menuManager.appendToGroup(IContextMenuConstants.GROUP_SEARCH, createQueryAction);
		menuManager.appendToGroup(IContextMenuConstants.GROUP_SEARCH, refineSearchAction);
		menuManager.appendToGroup(IContextMenuConstants.GROUP_SEARCH, openSearchWithBrowserAction);

		menuManager.appendToGroup(IContextMenuConstants.GROUP_VIEWER_SETUP, sortByDialogAction);
		addPresentationActions(menuManager);
	}

	private void moveToCategory(AbstractTaskCategory category) {
		StructuredSelection selection = (StructuredSelection) this.getViewer().getSelection();
		for (Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {
			Object selectedObject = iterator.next();
			if (selectedObject instanceof ITask) {
				ITask task = (ITask) selectedObject;
				TasksUiInternal.getTaskList().addTask(task, category);
			}
		}
	}

	@Override
	public void createControl(Composite parent) {
		super.createControl(parent);
		IMenuManager menuManager = getSite().getActionBars().getMenuManager();
		menuManager.appendToGroup(IContextMenuConstants.GROUP_VIEWER_SETUP, sortByDialogAction);
		addPresentationActions(menuManager);

		addPresentationActions(getSite().getActionBars().getToolBarManager());
	}

	public void addPresentationActions(IContributionManager menuManager) {
		for (Action action : groupingActions) {
			menuManager.appendToGroup(IContextMenuConstants.GROUP_VIEWER_SETUP, action);
		}
		menuManager.appendToGroup(IContextMenuConstants.GROUP_VIEWER_SETUP, new Separator());
		for (Action action : filterActions) {
			menuManager.appendToGroup(IContextMenuConstants.GROUP_VIEWER_SETUP, action);
		}
	}

	@Override
	public void setInput(ISearchResult newSearch, Object viewState) {
		super.setInput(newSearch, viewState);
		if (newSearch != null) {
			ISearchQuery query = ((RepositorySearchResult) newSearch).getQuery();
			IRepositoryQuery repositoryQuery = ((SearchHitCollector) query).getRepositoryQuery();
			openSearchWithBrowserAction.selectionChanged(new StructuredSelection(repositoryQuery));
		} else {
			openSearchWithBrowserAction.selectionChanged(StructuredSelection.EMPTY);
		}
	}

	public SearchResultSorter getSorter() {
		return searchResultSorter;
	}

	@Override
	public void restoreState(IMemento memento) {
		super.restoreState(memento);
		if (memento != null) {
			IMemento child = memento.getChild(MEMENTO_KEY_SORT);
			if (child != null && searchResultSorter != null) {
				searchResultSorter.getTaskComparator().restoreState(child);
			}
		}
	}

	@Override
	public void saveState(IMemento memento) {
		super.saveState(memento);
		if (memento != null) {
			IMemento child = memento.createChild(MEMENTO_KEY_SORT);
			if (searchResultSorter != null) {
				searchResultSorter.getTaskComparator().saveState(child);
			}
		}
	}

}
