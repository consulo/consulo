/*
 * Copyright 2013-2026 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.task.impl.internal.issue;

import consulo.application.dumb.DumbAware;
import consulo.dataContext.DataContext;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.platform.Platform;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.project.ProjectPropertiesComponent;
import consulo.task.Task;
import consulo.task.TaskRepository;
import consulo.task.TaskType;
import consulo.task.impl.internal.TaskSearcher;
import consulo.task.impl.internal.action.OpenTaskDialog;
import consulo.task.issue.IssuePage;
import consulo.task.issue.IssueQuery;
import consulo.task.localize.TaskLocalize;
import consulo.ui.Button;
import consulo.ui.ButtonStyle;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.ProgressBar;
import consulo.ui.Space;
import consulo.ui.Tab;
import consulo.ui.Table;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.ex.action.ActionToolbarFactory;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.action.ComboBoxAction;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.action.ToggleAction;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListPopup;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.ScrollableLayout;
import consulo.ui.layout.SplitLayoutPosition;
import consulo.ui.layout.TwoComponentSplitLayout;
import consulo.ui.model.FlatDataModel;
import consulo.ui.model.MutableFlatDataModel;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2026-10-31
 */
public final class IssueListPanel implements Disposable {
    private static final int PAGE_SIZE = 100;
    private static final String SEARCHERS_SHOWN_KEY = "tasks.issues.searchers.shown";
    private static final String SEARCHERS_PROPORTION_KEY = "tasks.issues.searchers.proportion";
    private static final int SEARCHERS_DEFAULT_PROPORTION = 20;
    private static final String COLUMNS_KEY = "tasks.issues.columns";

    private final Project myProject;
    private final TaskRepository myRepository;
    private final List<Task> myAllIssues = new ArrayList<>();
    private final MutableFlatDataModel<Task> myModel = FlatDataModel.of(List.of());
    private final DockLayout myTableArea = DockLayout.create(Space.NONE);
    private final ActionToolbar myToolbar;
    private Table<Task> myTable;
    private final ProgressBar myProgress;
    private final Label myStatusLabel;
    private final Button myLoadMoreButton;
    private final IssueDetailsPanel myDetailsPanel;
    private final IssueSearchersPanel mySearchersPanel;
    private final DockLayout myStripe;
    private final TwoComponentSplitLayout mySearchersSplit;
    private final TwoComponentSplitLayout myIssuesSplit;
    private final DockLayout myRoot;
    private final Set<TaskType> myTypeFilter = new LinkedHashSet<>();
    private final Set<String> myStateFilter = new LinkedHashSet<>();

    private TaskSearcher mySearcher;
    private int myLoaded;
    private int myGeneration;

    @RequiredUIAccess
    public IssueListPanel(Project project, TaskRepository repository) {
        myProject = project;
        myRepository = repository;

        myDetailsPanel = new IssueDetailsPanel(project, repository, task -> myModel.update(task));
        myTable = createTable();

        DefaultActionGroup group = new DefaultActionGroup();
        group.add(new FilterGroup<>(TaskLocalize.issuesFilterType(), myTypeFilter, Task::getType, IssueListPanel::getTypeText));
        group.add(new FilterGroup<>(TaskLocalize.issuesFilterStatus(), myStateFilter, IssueColumn::getStateText, Function.identity()));
        group.addSeparator();
        group.add(new RefreshAction());
        group.add(new StartWorkingAction());
        group.add(new OpenInBrowserAction());
        myToolbar = ActionToolbarFactory.getInstance().createActionToolbar("TaskIssues", group, ActionToolbar.Style.HORIZONTAL);
        myToolbar.setTargetUIComponent(myTable);

        DefaultActionGroup rightGroup = new DefaultActionGroup();
        rightGroup.add(new ColumnsAction());
        ActionToolbar rightToolbar = ActionToolbarFactory.getInstance().createActionToolbar("TaskIssues", rightGroup, ActionToolbar.Style.HORIZONTAL);
        rightToolbar.setTargetUIComponent(myTable);

        DockLayout toolbarRow = DockLayout.create(Space.NONE);
        toolbarRow.center(myToolbar.getUIComponent());
        toolbarRow.right(rightToolbar.getUIComponent());

        myProgress = ProgressBar.create();
        myProgress.setIndeterminate(true);
        myProgress.setVisible(false);

        myStatusLabel = Label.create();
        myLoadMoreButton = Button.create(TaskLocalize.issuesButtonLoadMore(), event -> loadPage());
        myLoadMoreButton.setVisible(false);
        HorizontalLayout footer = HorizontalLayout.create(Space.SMALL);
        footer.add(myStatusLabel);
        footer.add(myLoadMoreButton);

        myTableArea.top(myProgress);
        myTableArea.center(ScrollableLayout.create(myTable));

        DockLayout listArea = DockLayout.create(Space.NONE);
        listArea.top(toolbarRow);
        listArea.center(myTableArea);
        listArea.bottom(footer);

        myIssuesSplit = TwoComponentSplitLayout.create(SplitLayoutPosition.HORIZONTAL);
        myIssuesSplit.setFirstComponent(listArea);
        myIssuesSplit.setSecondComponent(myDetailsPanel.getComponent());
        myIssuesSplit.setProportion(60);

        mySearchersPanel = new IssueSearchersPanel(project, repository.getRepositoryType(), () -> setSearchersShown(false), this::selectSearcher);
        mySearcher = mySearchersPanel.getDefaultSearcher();

        Button stripeButton = Button.create(TaskLocalize.searchersTitle(), event -> setSearchersShown(true));
        stripeButton.setIcon(PlatformIconGroup.generalArrowright());
        stripeButton.addStyle(ButtonStyle.VERTICAL);
        myStripe = DockLayout.create(Space.NONE);
        myStripe.top(stripeButton);
        myStripe.borderBuilder().rightSet().apply();

        ProjectPropertiesComponent properties = ProjectPropertiesComponent.getInstance(project);
        mySearchersSplit = TwoComponentSplitLayout.create(SplitLayoutPosition.HORIZONTAL)
            .withProportion(properties.getInt(SEARCHERS_PROPORTION_KEY, SEARCHERS_DEFAULT_PROPORTION));
        mySearchersSplit.addSplitProportionChangedListener(event ->
            properties.setValue(SEARCHERS_PROPORTION_KEY, event.getProportion(), SEARCHERS_DEFAULT_PROPORTION));

        myRoot = DockLayout.create(Space.NONE);
        setSearchersShown(properties.getBoolean(SEARCHERS_SHOWN_KEY, false));

        myDetailsPanel.show(null);
        reload();
    }

    @RequiredUIAccess
    private Table<Task> createTable() {
        Table<Task> table = Table.create(myModel);
        for (IssueColumn column : getVisibleColumns()) {
            table.addColumn(column.getHeader(), column::getValue).setWidth(column.getWidth());
        }
        table.addSelectListener(event -> myDetailsPanel.show(table.getSelectedItem()));
        table.addDoubleClickListener(event -> startWorking(table.getSelectedItem()));
        return table;
    }

    private Set<IssueColumn> getVisibleColumns() {
        String stored = ProjectPropertiesComponent.getInstance(myProject).getValue(COLUMNS_KEY);
        Set<IssueColumn> columns = new LinkedHashSet<>();
        if (stored == null) {
            for (IssueColumn column : IssueColumn.values()) {
                if (column.isVisibleByDefault()) {
                    columns.add(column);
                }
            }
            return columns;
        }
        for (IssueColumn column : IssueColumn.values()) {
            if (StringUtil.split(stored, ",").contains(column.name())) {
                columns.add(column);
            }
        }
        return columns;
    }

    @RequiredUIAccess
    private void setColumnVisible(IssueColumn column, boolean visible) {
        Set<IssueColumn> columns = getVisibleColumns();
        if (visible) {
            columns.add(column);
        }
        else if (columns.size() > 1) {
            columns.remove(column);
        }
        List<String> names = new ArrayList<>();
        for (IssueColumn item : IssueColumn.values()) {
            if (columns.contains(item)) {
                names.add(item.name());
            }
        }
        ProjectPropertiesComponent.getInstance(myProject).setValue(COLUMNS_KEY, String.join(",", names));

        Task selected = myTable.getSelectedItem();
        myTable = createTable();
        myTableArea.center(ScrollableLayout.create(myTable));
        myToolbar.setTargetUIComponent(myTable);
        if (selected != null) {
            myTable.select(selected);
        }
    }

    public Component getComponent() {
        return myRoot;
    }

    public TaskRepository getRepository() {
        return myRepository;
    }

    @RequiredUIAccess
    private void setSearchersShown(boolean shown) {
        if (shown) {
            mySearchersSplit.setFirstComponent(mySearchersPanel.getComponent());
            mySearchersSplit.setSecondComponent(myIssuesSplit);
            myRoot.left(DockLayout.create(Space.NONE));
            myRoot.center(mySearchersSplit);
        }
        else {
            mySearchersSplit.setFirstComponent(null);
            mySearchersSplit.setSecondComponent(null);
            myRoot.left(myStripe);
            myRoot.center(myIssuesSplit);
        }
        myStripe.setVisible(!shown);
        ProjectPropertiesComponent.getInstance(myProject).setValue(SEARCHERS_SHOWN_KEY, shown, false);
    }

    @RequiredUIAccess
    private void selectSearcher(TaskSearcher searcher) {
        if (searcher != mySearcher) {
            mySearcher = searcher;
            reload();
        }
    }

    private static String getTypeText(TaskType type) {
        return StringUtil.capitalize(type.name().toLowerCase(Locale.ROOT));
    }

    @RequiredUIAccess
    private void reload() {
        myGeneration++;
        myLoaded = 0;
        myAllIssues.clear();
        myModel.removeAll();
        myDetailsPanel.show(null);
        loadPage();
    }

    @RequiredUIAccess
    private void loadPage() {
        int generation = myGeneration;
        myLoadMoreButton.setVisible(false);
        if (!myRepository.isConfigured()) {
            myStatusLabel.setText(TaskLocalize.issuesStatusNotLoggedIn(myRepository.getServerUrl()));
            return;
        }
        myStatusLabel.setText(TaskLocalize.issuesStatusLoading());
        myProgress.setVisible(true);

        UIAccess uiAccess = UIAccess.current();
        IssueQuery query = new IssueQuery(StringUtil.nullize(mySearcher.query, true), mySearcher.includeClosed);
        myRepository.getIssueTracker().search(query, myLoaded, PAGE_SIZE).whenCompleteAsync((page, error) -> {
            if (generation != myGeneration) {
                return;
            }
            myProgress.setVisible(false);
            if (error != null) {
                Throwable cause = error.getCause() != null ? error.getCause() : error;
                myStatusLabel.setText(TaskLocalize.issuesStatusError(StringUtil.notNullize(cause.getMessage(), cause.getClass().getSimpleName())));
                return;
            }
            addPage(page);
        }, uiAccess);
    }

    @RequiredUIAccess
    private void addPage(IssuePage page) {
        myAllIssues.addAll(page.getIssues());
        myLoaded += page.getIssues().size();
        applyFilters();
        myLoadMoreButton.setVisible(page.hasMore());
    }

    @RequiredUIAccess
    private void applyFilters() {
        Task selected = myTable.getSelectedItem();
        List<Task> visible = new ArrayList<>();
        for (Task task : myAllIssues) {
            if ((myTypeFilter.isEmpty() || myTypeFilter.contains(task.getType()))
                && (myStateFilter.isEmpty() || myStateFilter.contains(IssueColumn.getStateText(task)))) {
                visible.add(task);
            }
        }
        myModel.replaceAll(visible);
        myStatusLabel.setText(TaskLocalize.issuesStatusCount(visible.size()));
        if (selected != null && visible.contains(selected)) {
            myTable.select(selected);
        }
        else {
            myDetailsPanel.show(null);
        }
    }

    @RequiredUIAccess
    private void startWorking(@Nullable Task task) {
        if (task != null) {
            OpenTaskDialog.show(myProject, task);
        }
    }

    @Override
    public void dispose() {
        myGeneration++;
    }

    private class FilterGroup<V> extends ComboBoxAction implements DumbAware {
        private final LocalizeValue myName;
        private final Set<V> mySelected;
        private final Function<Task, V> myValue;
        private final Function<V, String> myText;

        private FilterGroup(LocalizeValue name, Set<V> selected, Function<Task, V> value, Function<V, String> text) {
            myName = name;
            mySelected = selected;
            myValue = value;
            myText = text;
        }

        @Override
        protected ActionGroup createPopupActionGroup(DataContext context) {
            Set<V> values = new LinkedHashSet<>();
            for (Task task : myAllIssues) {
                values.add(myValue.apply(task));
            }
            List<AnAction> actions = new ArrayList<>();
            actions.add(new DumbAwareAction(TaskLocalize.issuesFilterAll()) {
                @RequiredUIAccess
                @Override
                public void actionPerformed(AnActionEvent event) {
                    mySelected.clear();
                    applyFilters();
                }
            });
            for (V value : values) {
                String text = myText.apply(value);
                if (text.isEmpty()) {
                    continue;
                }
                actions.add(new ToggleAction(LocalizeValue.of(text)) {
                    @Override
                    public boolean isSelected(AnActionEvent event) {
                        return mySelected.contains(value);
                    }

                    @RequiredUIAccess
                    @Override
                    public void setSelected(AnActionEvent event, boolean state) {
                        if (state) {
                            mySelected.add(value);
                        }
                        else {
                            mySelected.remove(value);
                        }
                        applyFilters();
                    }
                });
            }
            DefaultActionGroup group = new DefaultActionGroup();
            group.addAll(actions);
            return group;
        }

        @Override
        public void update(AnActionEvent e) {
            String values = mySelected.isEmpty()
                ? TaskLocalize.issuesFilterAll().get()
                : String.join(", ", mySelected.stream().map(myText).toList());
            e.getPresentation().setText(TaskLocalize.issuesFilterValue(myName.get(), values));
        }
    }

    private class ColumnsAction extends DumbAwareAction {
        private ColumnsAction() {
            super(TaskLocalize.issuesActionColumns(), LocalizeValue.empty(), PlatformIconGroup.generalGearplain());
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(AnActionEvent e) {
            DefaultActionGroup group = new DefaultActionGroup();
            for (IssueColumn column : IssueColumn.values()) {
                group.add(new ToggleAction(column.getHeader()) {
                    @Override
                    public boolean isSelected(AnActionEvent event) {
                        return getVisibleColumns().contains(column);
                    }

                    @RequiredUIAccess
                    @Override
                    public void setSelected(AnActionEvent event, boolean state) {
                        setColumnVisible(column, state);
                    }
                });
            }
            ListPopup popup = JBPopupFactory.getInstance().createActionGroupPopup(
                TaskLocalize.issuesActionColumns().get(),
                group,
                e.getDataContext(),
                JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
                true
            );
            popup.showUnderneathOf(e);
        }
    }

    private class RefreshAction extends DumbAwareAction {
        private RefreshAction() {
            super(TaskLocalize.issuesActionRefresh(), LocalizeValue.empty(), PlatformIconGroup.actionsRefresh());
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(AnActionEvent e) {
            reload();
        }
    }

    private class StartWorkingAction extends DumbAwareAction implements AnActionWithSyncUpdate {
        private StartWorkingAction() {
            super(TaskLocalize.issuesActionStartWorking(), LocalizeValue.empty(), PlatformIconGroup.actionsExecute());
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(AnActionEvent e) {
            startWorking(myTable.getSelectedItem());
        }

        @Override
        public void update(AnActionEvent e) {
            e.getPresentation().setEnabled(myTable.getSelectedItem() != null);
        }
    }

    private class OpenInBrowserAction extends DumbAwareAction implements AnActionWithSyncUpdate {
        private OpenInBrowserAction() {
            super(TaskLocalize.issuesActionOpenInBrowser(), LocalizeValue.empty(), PlatformIconGroup.ideExternallink());
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(AnActionEvent e) {
            Task task = myTable.getSelectedItem();
            String url = task == null ? null : task.getIssueUrl();
            if (url != null) {
                Platform.current().openInBrowser(url);
            }
        }

        @Override
        public void update(AnActionEvent e) {
            Task task = myTable.getSelectedItem();
            e.getPresentation().setEnabled(task != null && task.getIssueUrl() != null);
        }
    }
}
