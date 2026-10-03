package consulo.endpoint.impl.internal.view;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.Application;
import consulo.application.ReadAction;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.colorScheme.event.EditorColorsListener;
import consulo.component.ProcessCanceledException;
import consulo.component.messagebus.MessageBusConnection;
import consulo.dataContext.DataSink;
import consulo.dataContext.UiDataProvider;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.endpoint.EndpointChangeTracker;
import consulo.endpoint.EndpointDataKeys;
import consulo.endpoint.EndpointListItem;
import consulo.endpoint.EndpointProjectModel;
import consulo.endpoint.EndpointProvider;
import consulo.endpoint.EndpointViewListener;
import consulo.endpoint.EndpointViewOpener;
import consulo.endpoint.impl.internal.view.action.EndpointContextMenuGroup;
import consulo.endpoint.impl.internal.view.action.EndpointToolbarGroup;
import consulo.endpoint.impl.internal.view.detail.EndpointDetailsPane;
import consulo.endpoint.localize.EndpointLocalize;
import consulo.endpoint.url.UrlTargetInfo;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiModificationTrackerListener;
import consulo.language.psi.PsiNavigationSupport;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.module.content.layer.event.ModuleRootEvent;
import consulo.module.content.layer.event.ModuleRootListener;
import consulo.navigation.Navigatable;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.project.event.DumbModeListener;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.project.ui.wm.ToolWindowManagerListener;
import consulo.ui.Component;
import consulo.ui.HorizontalAlignment;
import consulo.ui.Hyperlink;
import consulo.ui.Label;
import consulo.ui.MultiSelectListBox;
import consulo.ui.Space;
import consulo.ui.TextBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ContextMenuEvent;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.ActionPopupMenu;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.ex.action.ActionToolbarFactory;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.LoadingLayout;
import consulo.ui.layout.ScrollableLayout;
import consulo.ui.layout.SplitLayoutPosition;
import consulo.ui.layout.TwoComponentSplitLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.model.FlatDataModel;
import consulo.ui.model.MutableFlatDataModel;
import consulo.ui.style.StyleManager;
import consulo.util.dataholder.Key;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class EndpointView implements Disposable, UiDataProvider {
    public static final Key<EndpointView> KEY = Key.create("EndpointView");

    private static final Logger LOG = Logger.getInstance(EndpointView.class);

    private static final long REFRESH_DELAY_MILLISECONDS = 300;

    private static final Comparator<EndpointRow<?, ?>> ROW_ORDER = Comparator
        .<EndpointRow<?, ?>, String>comparing(row -> row.getData().getUrl(), String.CASE_INSENSITIVE_ORDER)
        .thenComparingInt(row -> row.getData().getMethodOrder())
        .thenComparing(row -> row.getData().getFrameworkTitle(), String.CASE_INSENSITIVE_ORDER);

    private final Project myProject;
    private final ToolWindow myToolWindow;
    private final EndpointViewManager myManager;
    private final EndpointLoader myLoader;

    private final DockLayout myRoot;
    private final TwoComponentSplitLayout mySplit;
    private boolean myVertical;
    private final TextBox mySearchBox;
    private final MutableFlatDataModel<EndpointViewRow> myModel;
    private final MultiSelectListBox<EndpointViewRow> myListBox;
    private final ScrollableLayout myListScroll;
    private final DockLayout myListContent;
    private final LoadingLayout<DockLayout> myLoadingLayout;
    private final ActionToolbar myToolbar;
    private final EndpointFilterChoice myExternalChoice;
    private final EndpointFilterComboBox myModuleFilter;
    private final EndpointFilterComboBox myTypeFilter;
    private final EndpointFilterComboBox myFrameworkFilter;
    private final EndpointDetailsPane myDetailsPane;

    private final Component myNoFrameworksPanel;
    private final Component myNoEndpointsPanel;
    private final Component myNoMatchesPanel;

    private final AtomicBoolean myRefreshScheduled = new AtomicBoolean();

    private EndpointSnapshot mySnapshot = EndpointSnapshot.EMPTY;
    private @Nullable EndpointQuery myLoadedQuery;
    private @Nullable Component myContent;
    private boolean myLoading;
    private boolean myApplyingRows;
    private boolean myForgetData;

    private volatile boolean myDirty = true;
    private volatile boolean myTrackingChanges = true;
    private volatile boolean myDisposed;

    @RequiredUIAccess
    public EndpointView(Project project, ToolWindow toolWindow) {
        myProject = project;
        myToolWindow = toolWindow;
        myManager = EndpointViewManager.getInstance(project);
        myLoader = new EndpointLoader(project, myManager);

        myModel = FlatDataModel.lazyOf(List.of());
        myListBox = MultiSelectListBox.create(myModel);
        myListBox.setRender(new EndpointRowRender());
        myListBox.setSpeedSearchConverter(EndpointViewRow::getSpeedSearchText);
        myListBox.addValueListener(event -> {
            if (!myApplyingRows) {
                selectionChanged();
            }
        });
        myListBox.addDoubleClickListener(event -> {
            if (event.getValue() instanceof EndpointRow<?, ?> row) {
                navigate(row, true);
            }
        });
        myListBox.addContextMenuListener(this::showContextMenu);

        myListScroll = ScrollableLayout.create(myListBox);
        myListContent = DockLayout.create(Space.NONE);
        myListContent.center(myListScroll);
        myContent = myListScroll;
        myLoadingLayout = LoadingLayout.create(myListContent, this);

        myNoFrameworksPanel = createEmptyPanel(EndpointLocalize.endpointsViewEmptyNoFrameworks(), false);
        myNoEndpointsPanel = createEmptyPanel(EndpointLocalize.endpointsViewEmptyNoEndpoints(), false);
        myNoMatchesPanel = createEmptyPanel(EndpointLocalize.endpointsViewEmptyNoMatches(), true);

        mySearchBox = TextBox.create();
        mySearchBox.setPlaceholder(EndpointLocalize.endpointsViewSearchPlaceholder(myManager.getProjectModel().getModuleQueryTag()));
        mySearchBox.addValueListener(event -> applyRows());

        ActionManager actionManager = ActionManager.getInstance();
        ActionGroup toolbarGroup = actionManager.getAction(EndpointToolbarGroup.class);
        myToolbar = ActionToolbarFactory.getInstance()
            .createActionToolbar(EndpointViewOpener.ENDPOINTS_FILTER_TOOLBAR_PLACE, toolbarGroup, ActionToolbar.Style.HORIZONTAL);

        myExternalChoice = new EndpointFilterChoice(
            EndpointModuleSnapshot.EXTERNAL_KEY,
            EndpointLocalize.frameworksFiltersModuleExternal(),
            PlatformIconGroup.generalWeb()
        );
        EndpointProjectModel projectModel = myManager.getProjectModel();
        myModuleFilter = new EndpointFilterComboBox(
            projectModel.getModuleDisplayName(),
            projectModel.getSelectModulesTitle(),
            myManager::isModuleChoiceVisible,
            myManager::setModuleFilter
        );
        myTypeFilter = new EndpointFilterComboBox(
            EndpointLocalize.frameworksFiltersType(),
            EndpointLocalize.frameworksFiltersTypeTitle(),
            myManager::isTypeVisible,
            myManager::setTypeFilter
        );
        myFrameworkFilter = new EndpointFilterComboBox(
            EndpointLocalize.frameworksFiltersFramework(),
            EndpointLocalize.frameworksFiltersFrameworkTitle(),
            myManager::isFrameworkVisible,
            myManager::setFrameworkFilter
        );

        HorizontalLayout filters = HorizontalLayout.create(Space.SMALL);
        filters.add(myModuleFilter.getComponent());
        filters.add(myTypeFilter.getComponent());
        filters.add(myFrameworkFilter.getComponent());

        DockLayout searchRow = DockLayout.create(Space.SMALL);
        searchRow.center(mySearchBox);
        searchRow.right(myToolbar.getUIComponent());

        DockLayout header = DockLayout.create(Space.X_SMALL);
        header.top(filters);
        header.center(searchRow);
        header.paddingBuilder().horizontalSet(Space.SMALL).verticalSet(Space.X_SMALL).apply();

        DockLayout listSide = DockLayout.create(Space.NONE);
        listSide.center(myLoadingLayout);

        myDetailsPane = new EndpointDetailsPane(project, this);
        myDetailsPane.setDetailsVisible(myManager.isDetailsVisible());
        String selectedTabId = myManager.getSelectedTabId();
        if (selectedTabId != null) {
            myDetailsPane.selectTab(selectedTabId);
        }

        myVertical = isToolWindowVertical(toolWindow);
        mySplit = TwoComponentSplitLayout.create(myVertical ? SplitLayoutPosition.VERTICAL : SplitLayoutPosition.HORIZONTAL);
        mySplit.setProportion(myManager.getSplitProportion());
        mySplit.addSplitProportionChangedListener(event -> myManager.setSplitProportion(event.getProportion()));
        mySplit.setFirstComponent(listSide);
        mySplit.setSecondComponent(myDetailsPane.getComponent());

        myRoot = DockLayout.create(Space.NONE);
        myRoot.top(header);
        myRoot.center(mySplit);
        myRoot.putUserData(UiDataProvider.KEY, this);
        myToolbar.setTargetUIComponent(myRoot);

        subscribe();

        myManager.attachView(this);
        Disposer.register(this, () -> myManager.detachView(this));

        reload();
    }

    private void subscribe() {
        MessageBusConnection connection = myProject.getMessageBus().connect(this);
        connection.subscribe(PsiModificationTrackerListener.class, () -> {
            if (myTrackingChanges) {
                scheduleRefresh();
            }
        });
        connection.subscribe(ModuleRootListener.class, new ModuleRootListener() {
            @Override
            public void rootsChanged(ModuleRootEvent event) {
                myLoader.flush();
                scheduleRefresh();
            }
        });
        connection.subscribe(DumbModeListener.class, new DumbModeListener() {
            @Override
            public void exitDumbMode() {
                scheduleRefresh();
            }
        });
        connection.subscribe(EndpointViewListener.class, event -> {
            myLoader.flush();
            myForgetData = true;
            scheduleRefresh();
        });
        connection.subscribe(EndpointChangeTracker.class, enabled -> myTrackingChanges = enabled);
        connection.subscribe(ToolWindowManagerListener.class, new ToolWindowManagerListener() {
            @Override
            public void toolWindowShown(ToolWindow toolWindow) {
                if (!myDisposed && EndpointViewOpener.ENDPOINTS_TOOLWINDOW_ID.equals(toolWindow.getId())) {
                    refreshIfDirty();
                }
            }

            @Override
            public void stateChanged(ToolWindowManager toolWindowManager) {
                if (myDisposed) {
                    return;
                }

                ToolWindow window = toolWindowManager.getToolWindow(EndpointViewOpener.ENDPOINTS_TOOLWINDOW_ID);
                if (window == null || window.isDisposed()) {
                    return;
                }

                updateOrientation(window);
                refreshIfDirty();
            }
        });

        myProject.getApplication().getMessageBus().connect(this).subscribe(EditorColorsListener.class, scheme -> colorsChanged());
        Disposer.register(this, StyleManager.get().addChangeListener((oldStyle, newStyle) -> colorsChanged()));
    }

    private static boolean isToolWindowVertical(ToolWindow toolWindow) {
        return !toolWindow.getAnchor().isHorizontal();
    }

    @RequiredUIAccess
    private void updateOrientation(ToolWindow toolWindow) {
        boolean vertical = isToolWindowVertical(toolWindow);
        if (vertical == myVertical) {
            return;
        }
        myVertical = vertical;
        mySplit.setPosition(vertical ? SplitLayoutPosition.VERTICAL : SplitLayoutPosition.HORIZONTAL);
    }

    public Component getComponent() {
        return myRoot;
    }

    public Project getProject() {
        return myProject;
    }

    public EndpointDetailsPane getDetailsPane() {
        return myDetailsPane;
    }

    public boolean isTrackingChanges() {
        return myTrackingChanges;
    }

    @RequiredUIAccess
    public List<EndpointListItem> getSelectedItems() {
        return List.copyOf(myListBox.getValue());
    }

    @RequiredUIAccess
    public void setSearchText(String text) {
        mySearchBox.setValue(text);
    }

    void saveState(EndpointViewState state) {
        state.detailsVisible = myDetailsPane.isDetailsVisible();
        String selectedTabId = myDetailsPane.getSelectedTabId();
        if (selectedTabId != null) {
            state.selectedTabId = selectedTabId;
        }
    }

    @RequiredUIAccess
    void optionsChanged(boolean reload) {
        if (myDisposed) {
            return;
        }

        updateFilters();
        if (reload) {
            reload();
        }
        else {
            applyRows();
        }
        myToolbar.updateActionsAsync();
    }

    @Override
    public void uiDataSnapshot(DataSink sink) {
        sink.set(KEY, this);
        sink.set(EndpointDetailsPane.KEY, myDetailsPane);

        List<EndpointViewRow> selected = myListBox.getValue();
        sink.set(EndpointDataKeys.SELECTED_ITEMS, List.copyOf(selected));

        List<EndpointRow<?, ?>> rows = new ArrayList<>();
        for (EndpointViewRow row : selected) {
            if (row instanceof EndpointRow<?, ?> endpointRow) {
                rows.add(endpointRow);
            }
        }
        if (rows.isEmpty()) {
            return;
        }

        Navigatable[] navigatables = new Navigatable[rows.size()];
        for (int i = 0; i < rows.size(); i++) {
            navigatables[i] = new EndpointRowNavigatable(this, rows.get(i));
        }
        sink.set(Navigatable.KEY_OF_ARRAY, navigatables);
        sink.lazy(EndpointProvider.URL_TARGET_INFO, () -> collectUrlTargetInfos(rows));

        if (rows.size() == 1) {
            EndpointRow<?, ?> row = rows.get(0);
            sink.lazy(PsiElement.KEY, row::getNavigationElement);
            try {
                row.uiDataSnapshot(sink);
            }
            catch (ProcessCanceledException e) {
                throw e;
            }
            catch (Exception e) {
                LOG.error(e);
            }
        }
    }

    @RequiredReadAction
    private static List<UrlTargetInfo> collectUrlTargetInfos(List<EndpointRow<?, ?>> rows) {
        List<UrlTargetInfo> result = new ArrayList<>();
        for (EndpointRow<?, ?> row : rows) {
            if (!row.isValid()) {
                continue;
            }

            Iterable<UrlTargetInfo> infos = row.getUrlTargetInfos();
            if (infos != null) {
                for (UrlTargetInfo info : infos) {
                    result.add(info);
                }
            }
        }
        return result;
    }

    @RequiredUIAccess
    void navigate(EndpointRow<?, ?> row, boolean requestFocus) {
        ReadAction.nonBlocking(() -> findNavigatable(row))
            .inSmartMode(myProject)
            .expireWith(this)
            .coalesceBy(this, EndpointRowNavigatable.class)
            .finishOnUiThread(Application::getDefaultModalityState, navigatable -> {
                if (navigatable != null) {
                    navigatable.navigate(requestFocus);
                }
            })
            .submitDefault();
    }

    @RequiredReadAction
    private @Nullable Navigatable findNavigatable(EndpointRow<?, ?> row) {
        if (!row.isValid()) {
            scheduleRefresh();
            return null;
        }

        PsiElement element = row.getNavigationElement();
        if (element == null || !element.isValid()) {
            return null;
        }

        return PsiNavigationSupport.getInstance().getDescriptor(element);
    }

    @RequiredUIAccess
    private void showContextMenu(ContextMenuEvent event) {
        ActionManager actionManager = ActionManager.getInstance();
        ActionGroup group = actionManager.getAction(EndpointContextMenuGroup.class);
        ActionPopupMenu menu = actionManager.createActionPopupMenu(EndpointViewOpener.ENDPOINTS_CONTEXT_MENU_PLACE, group);
        menu.setTargetComponent(myListBox);
        menu.show(event.getComponent(), event.getInputDetails().getX(), event.getInputDetails().getY());
    }

    private void scheduleRefresh() {
        myDirty = true;
        if (myDisposed || !myRefreshScheduled.compareAndSet(false, true)) {
            return;
        }

        AppExecutorUtil.getAppScheduledExecutorService().schedule(() -> {
            myRefreshScheduled.set(false);
            if (myDisposed || myProject.isDisposed()) {
                return;
            }
            myProject.getUIAccess().give(this::refreshIfDirty);
        }, REFRESH_DELAY_MILLISECONDS, TimeUnit.MILLISECONDS);
    }

    @RequiredUIAccess
    private void refreshIfDirty() {
        if (myDisposed || !myDirty || !myToolWindow.isVisible()) {
            return;
        }
        reload();
    }

    private void colorsChanged() {
        myLoader.flush();
        scheduleRefresh();
    }

    @RequiredUIAccess
    private void reload() {
        if (myDisposed) {
            return;
        }

        myDirty = false;
        EndpointQuery query = captureQuery();
        if (!query.isSameLoad(myLoadedQuery)) {
            startLoading();
        }

        ReadAction.nonBlocking(() -> myLoader.load(query))
            .inSmartMode(myProject)
            .expireWith(this)
            .coalesceBy(this)
            .finishOnUiThread(Application::getDefaultModalityState, snapshot -> applySnapshot(query, snapshot))
            .submitDefault();
    }

    @RequiredUIAccess
    private void startLoading() {
        if (myLoading) {
            return;
        }

        myLoading = true;
        myContent = null;
        if (DumbService.isDumb(myProject)) {
            myLoadingLayout.startLoading(EndpointLocalize.endpointsViewEmptyIndexing());
        }
        else {
            myLoadingLayout.startLoading();
        }
    }

    @RequiredUIAccess
    private void applySnapshot(EndpointQuery query, EndpointSnapshot snapshot) {
        if (myDisposed) {
            return;
        }

        mySnapshot = snapshot;
        myLoadedQuery = query;

        updateFilters();

        if (myLoading) {
            myLoading = false;
            myLoadingLayout.stopLoading(inner -> {
            });
        }

        applyRows();

        if (myForgetData) {
            myForgetData = false;
            myDetailsPane.forgetData();
        }
        myToolbar.updateActionsAsync();
    }

    @RequiredUIAccess
    private void applyRows() {
        if (myDisposed) {
            return;
        }

        EndpointQuery query = captureQuery();
        List<EndpointViewRow> rows = assembleRows(mySnapshot, query);

        List<EndpointViewRow> selectedBefore = List.copyOf(myListBox.getValue());

        myApplyingRows = true;
        try {
            myModel.replaceAll(rows);
            if (!selectedBefore.isEmpty()) {
                myListBox.setValue(findRows(rows, selectedBefore), false);
            }
        }
        finally {
            myApplyingRows = false;
        }

        if (!myLoading) {
            showContent(query, rows);
        }

        List<EndpointViewRow> selectedAfter = myListBox.getValue();
        if (selectedBefore.equals(selectedAfter) && isDataReplaced(selectedBefore, selectedAfter)) {
            myDetailsPane.replaceSelectedItems(selectedAfter);
        }
        else {
            selectionChanged();
        }
    }

    private static boolean isDataReplaced(List<EndpointViewRow> before, List<EndpointViewRow> after) {
        int size = Math.min(before.size(), after.size());
        for (int i = 0; i < size; i++) {
            if (before.get(i) instanceof EndpointRow<?, ?> previous
                && after.get(i) instanceof EndpointRow<?, ?> current
                && previous.getData() != current.getData()) {
                return true;
            }
        }
        return false;
    }

    private static List<EndpointViewRow> findRows(List<EndpointViewRow> rows, List<EndpointViewRow> selected) {
        Set<EndpointViewRow> keys = new HashSet<>(selected);
        Set<EndpointViewRow> matched = new HashSet<>();
        for (EndpointViewRow row : rows) {
            if (keys.contains(row)) {
                matched.add(row);
            }
        }

        Set<EndpointRowKey> unmatched = new HashSet<>();
        for (EndpointViewRow row : selected) {
            if (row instanceof EndpointRow<?, ?> endpointRow && !matched.contains(row)) {
                unmatched.add(endpointRow.getKey().withModule(null));
            }
        }

        List<EndpointViewRow> result = new ArrayList<>();
        for (EndpointViewRow row : rows) {
            if (matched.contains(row)) {
                result.add(row);
            }
            else if (row instanceof EndpointRow<?, ?> endpointRow && unmatched.remove(endpointRow.getKey().withModule(null))) {
                result.add(row);
            }
        }
        return result;
    }

    @RequiredUIAccess
    private void showContent(EndpointQuery query, List<EndpointViewRow> rows) {
        Component content;
        if (!mySnapshot.hasProviders()) {
            content = myNoFrameworksPanel;
        }
        else if (!rows.isEmpty()) {
            content = myListScroll;
        }
        else if (mySnapshot.hasRows() || query.isLoadFiltered() || isViewFiltered(query)) {
            content = myNoMatchesPanel;
        }
        else {
            content = myNoEndpointsPanel;
        }

        if (content != myContent) {
            myContent = content;
            myListContent.center(content);
        }
    }

    private static boolean isViewFiltered(EndpointQuery query) {
        return !query.getHiddenTypes().isEmpty() || !query.getHiddenFrameworks().isEmpty() || !query.getSearchText().isBlank();
    }

    private List<EndpointViewRow> assembleRows(EndpointSnapshot snapshot, EndpointQuery query) {
        EndpointSearchQuery search = EndpointSearchQuery.parse(query.getSearchText(), myManager.getProjectModel().getModuleQueryTag());

        List<EndpointViewRow> result = new ArrayList<>();
        if (query.isGroupByModule()) {
            for (EndpointModuleSnapshot section : snapshot.getSections()) {
                if (!search.matchesModule(section)) {
                    continue;
                }

                Map<EndpointRowKey, EndpointRow<?, ?>> rows = new LinkedHashMap<>();
                for (EndpointRowData<?, ?> data : section.getRows()) {
                    if (isVisible(data, query, search)) {
                        EndpointRow<?, ?> row = EndpointRow.create(data, section.getModule(), section.getKey(), query.isCompact());
                        rows.putIfAbsent(row.getKey(), row);
                    }
                }

                if (!rows.isEmpty()) {
                    List<EndpointRow<?, ?>> sorted = new ArrayList<>(rows.values());
                    sorted.sort(ROW_ORDER);
                    result.add(new EndpointModuleRow(section));
                    result.addAll(sorted);
                }
            }
            return result;
        }

        Map<EndpointRowKey, EndpointRow<?, ?>> rows = new LinkedHashMap<>();
        for (EndpointModuleSnapshot section : snapshot.getSections()) {
            if (!search.matchesModule(section)) {
                continue;
            }

            for (EndpointRowData<?, ?> data : section.getRows()) {
                if (isVisible(data, query, search)) {
                    EndpointRow<?, ?> row = EndpointRow.create(data, section.getModule(), null, query.isCompact());
                    rows.putIfAbsent(row.getKey(), row);
                }
            }
        }

        List<EndpointRow<?, ?>> sorted = new ArrayList<>(rows.values());
        sorted.sort(ROW_ORDER);
        result.addAll(sorted);
        return result;
    }

    private static boolean isVisible(EndpointRowData<?, ?> data, EndpointQuery query, EndpointSearchQuery search) {
        return !query.getHiddenTypes().contains(data.getTypeTag())
            && !query.getHiddenFrameworks().contains(data.getFrameworkTag())
            && search.matches(data);
    }

    @RequiredUIAccess
    private void updateFilters() {
        List<EndpointFilterChoice> modules = new ArrayList<>(mySnapshot.getModules());
        modules.add(myExternalChoice);
        myModuleFilter.update(modules);
        myTypeFilter.update(mySnapshot.getTypes());
        myFrameworkFilter.update(mySnapshot.getFrameworks());
    }

    @RequiredUIAccess
    private EndpointQuery captureQuery() {
        return myManager.createQuery(mySearchBox.getValue());
    }

    @RequiredUIAccess
    private void selectionChanged() {
        myDetailsPane.setSelectedItems(myListBox.getValue());
    }

    @RequiredUIAccess
    private void resetFilters() {
        mySearchBox.setValue("");
        myManager.resetFilters();
    }

    @RequiredUIAccess
    private Component createEmptyPanel(LocalizeValue text, boolean withReset) {
        VerticalLayout layout = VerticalLayout.create(Space.MEDIUM, HorizontalAlignment.CENTER);
        layout.add(Label.create(text));
        if (withReset) {
            layout.add(Hyperlink.create(EndpointLocalize.endpointsViewResetFilters(), event -> resetFilters()));
        }

        DockLayout panel = DockLayout.create(Space.NONE);
        panel.top(layout);
        return panel;
    }

    @Override
    public void dispose() {
        myDisposed = true;
    }
}
