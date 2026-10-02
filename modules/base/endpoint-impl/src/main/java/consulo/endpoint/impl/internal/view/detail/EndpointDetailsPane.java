package consulo.endpoint.impl.internal.view.detail;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.concurrent.coroutine.ReadLock;
import consulo.component.ProcessCanceledException;
import consulo.component.messagebus.MessageBusConnection;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.endpoint.EndpointChangeTracker;
import consulo.endpoint.EndpointElementItem;
import consulo.endpoint.EndpointListItem;
import consulo.endpoint.EndpointProvider;
import consulo.endpoint.EndpointSidePanel;
import consulo.endpoint.EndpointSidePanelProvider;
import consulo.endpoint.EndpointViewOpener;
import consulo.language.psi.PsiModificationTrackerListener;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.project.event.DumbModeListener;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.project.ui.wm.ToolWindowManagerListener;
import consulo.ui.Space;
import consulo.ui.Tab;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.LoadingLayout;
import consulo.ui.layout.TabbedLayout;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.CoroutineScope;
import consulo.util.concurrent.coroutine.step.Delay;
import consulo.util.dataholder.Key;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

public final class EndpointDetailsPane implements Disposable {
    public static final Key<EndpointDetailsPane> KEY = Key.create("EndpointDetailsPane");

    private static final Logger LOG = Logger.getInstance(EndpointDetailsPane.class);

    private static final long PSI_CHANGE_DELAY_MILLISECONDS = 300;
    private static final long UNKNOWN_STAMP = -1;

    private final Project myProject;
    private final DockLayout myRoot;
    private final TabbedLayout myTabbedLayout;
    private final List<EndpointSidePanelTab> myTabs = new ArrayList<>();

    private @Nullable EndpointSidePanelTab mySelectedTab;
    private List<EndpointListItem> mySelectedItems = List.of();
    private Disposable mySelectionDisposable;
    private @Nullable Disposable myPsiChangeDisposable;
    private long myStamp = UNKNOWN_STAMP;
    private boolean myDirty;
    private boolean myDisposed;
    private volatile boolean myTrackingChanges = true;
    private volatile boolean myDetailsVisible = true;

    @RequiredUIAccess
    public EndpointDetailsPane(Project project, Disposable parentDisposable) {
        myProject = project;
        Disposer.register(parentDisposable, this);
        mySelectionDisposable = newSelectionDisposable();

        myRoot = DockLayout.create(Space.NONE);
        myTabbedLayout = TabbedLayout.create();
        myRoot.center(myTabbedLayout);

        project.getExtensionPoint(EndpointSidePanelProvider.class).forEach(provider -> {
            EndpointSidePanel panel = provider.create();
            if (panel != null) {
                addTab(getTabId(provider), panel);
            }
        });

        myTabbedLayout.addSelectListener(event -> onTabSelected(event.getTab()));

        if (!myTabs.isEmpty()) {
            selectTab(myTabs.get(0));
        }
        for (EndpointSidePanelTab tab : myTabs) {
            tab.updateEnabled(tab == mySelectedTab);
        }

        MessageBusConnection connection = project.getMessageBus().connect(this);
        connection.subscribe(PsiModificationTrackerListener.class, this::onPsiModification);
        connection.subscribe(EndpointChangeTracker.class, enabled -> myTrackingChanges = enabled);
        connection.subscribe(DumbModeListener.class, new DumbModeListener() {
            @Override
            public void exitDumbMode() {
                myProject.getUIAccess().give(() -> onVisibilityChanged());
            }
        });
        connection.subscribe(ToolWindowManagerListener.class, new ToolWindowManagerListener() {
            @Override
            public void stateChanged(ToolWindowManager toolWindowManager) {
                onVisibilityChanged();
            }

            @Override
            public void toolWindowShown(ToolWindow toolWindow) {
                onVisibilityChanged();
            }
        });
    }

    public DockLayout getComponent() {
        return myRoot;
    }

    public List<EndpointListItem> getSelectedItems() {
        return mySelectedItems;
    }

    @RequiredUIAccess
    public void setSelectedItems(List<? extends EndpointListItem> selectedItems) {
        List<EndpointListItem> items = List.copyOf(selectedItems);
        boolean same = items.equals(mySelectedItems);
        mySelectedItems = items;
        if (same) {
            return;
        }
        myStamp = UNKNOWN_STAMP;
        restartSelection();
    }

    @RequiredUIAccess
    public void replaceSelectedItems(List<? extends EndpointListItem> selectedItems) {
        mySelectedItems = List.copyOf(selectedItems);
        myStamp = UNKNOWN_STAMP;
        restartSelection();
    }

    @RequiredUIAccess
    public void forgetData() {
        myStamp = UNKNOWN_STAMP;
        restartSelection();
    }

    public boolean isDetailsVisible() {
        return myDetailsVisible;
    }

    @RequiredUIAccess
    public void setDetailsVisible(boolean visible) {
        if (myDetailsVisible == visible) {
            return;
        }
        myDetailsVisible = visible;
        myRoot.setVisible(visible);
        if (visible) {
            onVisibilityChanged();
        }
    }

    public @Nullable String getSelectedTabId() {
        EndpointSidePanelTab selectedTab = mySelectedTab;
        return selectedTab == null ? null : selectedTab.getId();
    }

    @RequiredUIAccess
    public void selectTab(String id) {
        for (EndpointSidePanelTab tab : myTabs) {
            if (tab.getId().equals(id)) {
                selectTab(tab);
                return;
            }
        }
    }

    @Override
    public void dispose() {
        myDisposed = true;
    }

    private static String getTabId(EndpointSidePanelProvider provider) {
        ExtensionImpl annotation = provider.getClass().getAnnotation(ExtensionImpl.class);
        return annotation != null && !annotation.id().isEmpty() ? annotation.id() : provider.getClass().getName();
    }

    @RequiredUIAccess
    private void addTab(String id, EndpointSidePanel panel) {
        if (panel instanceof Disposable disposable) {
            Disposer.register(this, disposable);
        }

        DockLayout content = DockLayout.create(Space.NONE);
        content.center(panel.getComponent());
        LoadingLayout<DockLayout> loadingLayout = LoadingLayout.create(content, this);

        DockLayout holder = DockLayout.create(Space.NONE);
        holder.center(loadingLayout);

        LocalizeValue title = panel.getTitle();
        Tab tab = myTabbedLayout.createTab();
        tab.setRenderer((it, presentation) -> presentation.append(title));

        EndpointSidePanelTab panelTab = new EndpointSidePanelTab(id, panel, tab, loadingLayout);
        myTabs.add(panelTab);
        myTabbedLayout.addTab(tab, holder);
    }

    @RequiredUIAccess
    private void selectTab(EndpointSidePanelTab tab) {
        tab.updateEnabled(true);
        tab.getTab().select();
        onTabSelected(tab.getTab());
    }

    @RequiredUIAccess
    private void onTabSelected(Tab tab) {
        EndpointSidePanelTab panelTab = findTab(tab);
        if (panelTab == null || panelTab == mySelectedTab) {
            return;
        }

        EndpointSidePanelTab previous = mySelectedTab;
        mySelectedTab = panelTab;
        if (previous != null) {
            previous.updateEnabled(false);
        }
        panelTab.updateEnabled(true);

        if (canRun()) {
            updateSelectedTab();
        }
        else {
            myDirty = true;
        }
    }

    private @Nullable EndpointSidePanelTab findTab(Tab tab) {
        for (EndpointSidePanelTab panelTab : myTabs) {
            if (panelTab.getTab() == tab) {
                return panelTab;
            }
        }
        return null;
    }

    @RequiredUIAccess
    private void restartSelection() {
        if (myDisposed) {
            return;
        }

        for (EndpointSidePanelTab tab : myTabs) {
            tab.reset();
        }
        Disposer.dispose(mySelectionDisposable);
        mySelectionDisposable = newSelectionDisposable();

        if (!canRun()) {
            myDirty = true;
            return;
        }
        myDirty = false;

        runStampBaseline();
        runAvailability();
        updateSelectedTab();
    }

    @RequiredUIAccess
    private void runStampBaseline() {
        List<EndpointListItem> items = mySelectedItems;
        Coroutine<Object, Long> coroutine = Coroutine.first(ReadLock.<Object, Long>apply(ignored -> computeStamp(items)));
        launch(mySelectionDisposable, coroutine, (stamp, error) -> {
            if (error == null && stamp != null && items.equals(mySelectedItems)) {
                myStamp = stamp;
            }
        });
    }

    @RequiredUIAccess
    private void runAvailability() {
        List<EndpointListItem> items = mySelectedItems;
        Disposable selectionDisposable = mySelectionDisposable;
        for (EndpointSidePanelTab tab : myTabs) {
            Coroutine<?, Boolean> coroutine;
            try {
                coroutine = tab.getPanel().isAvailable(items);
            }
            catch (ProcessCanceledException e) {
                throw e;
            }
            catch (RuntimeException e) {
                LOG.error(e);
                continue;
            }

            launch(selectionDisposable, coroutine, (available, error) -> {
                tab.setAvailable(error == null && Boolean.TRUE.equals(available));
                tab.updateEnabled(tab == mySelectedTab);
            });
        }
    }

    @RequiredUIAccess
    private void updateSelectedTab() {
        EndpointSidePanelTab tab = mySelectedTab;
        if (tab == null || tab.isUpdating()) {
            return;
        }

        if (tab.isUpdated()) {
            tab.getPanel().selected(mySelectedItems);
            return;
        }

        startUpdate(tab);
    }

    @RequiredUIAccess
    private void rerunCancelledUpdate() {
        EndpointSidePanelTab tab = mySelectedTab;
        if (tab == null || tab.isUpdated() || tab.isUpdating()) {
            return;
        }

        startUpdate(tab);
    }

    @RequiredUIAccess
    private void startUpdate(EndpointSidePanelTab tab) {
        List<EndpointListItem> items = mySelectedItems;
        Coroutine<?, ?> coroutine;
        try {
            coroutine = tab.getPanel().update(items);
        }
        catch (ProcessCanceledException e) {
            throw e;
        }
        catch (RuntimeException e) {
            LOG.error(e);
            return;
        }

        Disposable updateDisposable = Disposable.newDisposable("EndpointSidePanelUpdate");
        Disposer.register(mySelectionDisposable, updateDisposable);
        tab.startUpdate(updateDisposable);

        launch(updateDisposable, coroutine, (result, error) -> tab.finishUpdate(updateDisposable, error == null));
    }

    private void onPsiModification() {
        if (!myTrackingChanges || myProject.isDisposed()) {
            return;
        }
        myProject.getUIAccess().give(() -> {
            if (!myDisposed && myTrackingChanges) {
                psiChanged();
            }
        });
    }

    @RequiredUIAccess
    private void psiChanged() {
        for (EndpointSidePanelTab tab : myTabs) {
            tab.cancelUpdate();
        }

        Disposable previous = myPsiChangeDisposable;
        if (previous != null) {
            myPsiChangeDisposable = null;
            Disposer.dispose(previous);
        }

        if (!canRun()) {
            myDirty = true;
            return;
        }
        Disposable psiChangeDisposable = Disposable.newDisposable("EndpointDetailsPsiChange");
        Disposer.register(this, psiChangeDisposable);
        myPsiChangeDisposable = psiChangeDisposable;

        List<EndpointListItem> items = mySelectedItems;
        Coroutine<Object, Long> coroutine = Coroutine
            .first(Delay.<Object>sleep(PSI_CHANGE_DELAY_MILLISECONDS))
            .then(ReadLock.<Object, Long>apply(ignored -> computeStamp(items)));

        launch(psiChangeDisposable, coroutine, (stamp, error) -> {
            if (myPsiChangeDisposable == psiChangeDisposable) {
                myPsiChangeDisposable = null;
            }
            Disposer.dispose(psiChangeDisposable);
            if (error != null || stamp == null) {
                return;
            }
            onPsiChangeSettled(stamp);
        });
    }

    @RequiredUIAccess
    private void onPsiChangeSettled(long stamp) {
        if (!canRun()) {
            myDirty = true;
            return;
        }

        if (stamp != myStamp) {
            myStamp = stamp;
            restartSelection();
        }
        else {
            rerunCancelledUpdate();
        }
    }

    @RequiredUIAccess
    private void onVisibilityChanged() {
        if (myDisposed || !myDirty || !canRun()) {
            return;
        }
        restartSelection();
    }

    private boolean canRun() {
        if (!myDetailsVisible || DumbService.getInstance(myProject).isDumb()) {
            return false;
        }
        ToolWindow toolWindow = ToolWindowManager.getInstance(myProject).getToolWindow(EndpointViewOpener.ENDPOINTS_TOOLWINDOW_ID);
        return toolWindow == null || toolWindow.isVisible();
    }

    private Disposable newSelectionDisposable() {
        Disposable disposable = Disposable.newDisposable("EndpointDetailsSelection");
        Disposer.register(this, disposable);
        return disposable;
    }

    private <O> void launch(
        Disposable parentDisposable,
        Coroutine<@Nullable ?, O> coroutine,
        @RequiredUIAccess BiConsumer<@Nullable O, @Nullable Throwable> consumer
    ) {
        CoroutineScope scope = CoroutineScope.of(myProject.coroutineContext());
        Disposer.register(parentDisposable, scope::cancel);

        CompletableFuture<O> future = coroutine.runAsync(scope, null).toFuture();
        future.whenComplete((result, error) -> {
            if (error != null && !isCancellation(error) && !Disposer.isDisposed(parentDisposable)) {
                LOG.error(error);
            }
            if (myProject.isDisposed()) {
                return;
            }
            UIAccess uiAccess = myProject.getUIAccess();
            uiAccess.give(() -> {
                if (myDisposed || Disposer.isDisposed(parentDisposable)) {
                    return;
                }
                consumer.accept(result, error);
            });
        });
    }

    private static boolean isCancellation(Throwable error) {
        Set<Throwable> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        Throwable current = error;
        while (current != null && visited.add(current)) {
            if (current instanceof CancellationException || current instanceof ProcessCanceledException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static long computeStamp(List<EndpointListItem> items) {
        Map<EndpointProvider<?, ?>, Boolean> providers = new IdentityHashMap<>();
        long stamp = 0;
        for (EndpointListItem item : items) {
            if (item instanceof EndpointElementItem<?, ?> elementItem) {
                EndpointProvider<?, ?> provider = elementItem.getProvider();
                if (providers.put(provider, Boolean.TRUE) == null) {
                    stamp = stamp * 31 + provider.getModificationTracker().getModificationCount();
                }
            }
        }
        return stamp;
    }
}
