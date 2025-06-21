// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.execution.impl.internal.service;


import consulo.annotation.DeprecationInfo;
import consulo.annotation.component.ServiceImpl;
import consulo.application.AppUIExecutor;
import consulo.application.ApplicationManager;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.execution.localize.ExecutionLocalize;
import consulo.execution.service.*;
import consulo.navigation.ItemPresentation;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.project.ui.util.AppUIUtil;
import consulo.project.ui.wm.ToolWindowId;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.project.ui.wm.ToolWindowManagerListener;
import consulo.ui.ModalityState;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.AutoScrollToSourceHandler;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.tree.TreeState;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentFactory;
import consulo.ui.ex.content.ContentManager;
import consulo.ui.ex.content.event.ContentManagerEvent;
import consulo.ui.ex.content.event.ContentManagerListener;
import consulo.ui.ex.internal.ToolWindowEx;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.ui.ex.tree.PresentationData;
import consulo.ui.image.Image;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.FactoryMap;
import consulo.util.collection.SmartHashSet;
import consulo.util.collection.SmartList;
import consulo.util.concurrent.AsyncPromise;
import consulo.util.concurrent.Promise;
import consulo.util.concurrent.Promises;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jdom.Element;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

@Singleton
@State(name = "ServiceViewManager", storages = @Storage(StoragePathMacros.WORKSPACE_FILE))
@ServiceImpl
public final class ServiceViewManagerImpl implements ServiceViewManager, PersistentStateComponent<ServiceViewManagerImpl.State> {
    private static final String HELP_ID = "services.tool.window";

    @Nonnull
    private final Project myProject;
    private State myState = new State();

    private final ServiceModel myModel;
    private final ServiceModelFilter myModelFilter;
    private final Map<String, Collection<ServiceViewContributor<?>>> myGroups = new ConcurrentHashMap<>();
    private final Set<ServiceViewContributor<?>> myNotInitializedContributors = new HashSet<>();
    private final List<ServiceViewContentHolder> myContentHolders = new SmartList<>();
    private boolean myActivationActionsRegistered;
    private AutoScrollToSourceHandler myAutoScrollToSourceHandler;

    private final Set<String> myActiveToolWindowIds = new SmartHashSet<>();

    @Inject
    public ServiceViewManagerImpl(@Nonnull Project project) {
        myProject = project;
        myModel = new ServiceModel(myProject);
        Disposer.register(myProject, myModel);
        myModelFilter = new ServiceModelFilter();
        myProject.getMessageBus().connect(myModel).subscribe(ServiceEventListener.TOPIC, e -> {
            myModel.handle(e).onSuccess(o -> eventHandled(e));
        });
        //CONTRIBUTOR_EP_NAME.addExtensionPointListener(new ServiceViewExtensionPointListener(), myProject);
    }

    private void eventHandled(@Nonnull ServiceEventListener.ServiceEvent e) {
        String toolWindowId = getToolWindowId(e.contributorClass);
        if (toolWindowId == null) {
            return;
        }

        ServiceViewItem eventRoot =
            ContainerUtil.find(myModel.getRoots(), root -> e.contributorClass.isInstance(root.getRootContributor()));
        ServiceViewContributor<?> notInitializedContributor = findNotInitializedContributor(e.contributorClass, eventRoot);
        boolean initialized = notInitializedContributor == null;
        if (!initialized &&
            (e.type == ServiceEventListener.EventType.RESET || e.type == ServiceEventListener.EventType.UNLOAD_SYNC_RESET)) {
            myNotInitializedContributors.remove(notInitializedContributor);
        }
        if (eventRoot != null) {
            boolean show = !(eventRoot.getViewDescriptor() instanceof ServiceViewNonActivatingDescriptor) && initialized;
            updateToolWindow(toolWindowId, true, show);
        }
        else {
            Set<? extends ServiceViewContributor<?>> activeContributors = getActiveContributors();
            Collection<ServiceViewContributor<?>> toolWindowContributors = myGroups.get(toolWindowId);
            updateToolWindow(toolWindowId, ContainerUtil.intersects(activeContributors, toolWindowContributors), false);
        }
    }

    private @Nullable ServiceViewContributor<?> findNotInitializedContributor(Class<?> contributorClass, ServiceViewItem eventRoot) {
        if (eventRoot != null) {
            return myNotInitializedContributors.contains(eventRoot.getRootContributor()) ? eventRoot.getRootContributor() : null;
        }
        for (ServiceViewContributor<?> contributor : myNotInitializedContributors) {
            if (contributorClass.isInstance(contributor)) {
                return contributor;
            }
        }
        return null;
    }

    private Set<? extends ServiceViewContributor<?>> getActiveContributors() {
        return ContainerUtil.map2Set(myModel.getRoots(), ServiceViewItem::getRootContributor);
    }

    private @Nullable ServiceViewContentHolder getContentHolder(@Nonnull Class<?> contributorClass) {
        for (ServiceViewContentHolder holder : myContentHolders) {
            for (ServiceViewContributor<?> rootContributor : holder.rootContributors) {
                if (contributorClass.isInstance(rootContributor)) {
                    return holder;
                }
            }
        }
        return null;
    }

    @RequiredUIAccess
    public void initToolWindow(ToolWindow toolWindow) {
        String toolWindowId = toolWindow.getId();

        Collection<ServiceViewContributor<?>> contributors = myGroups.getOrDefault(toolWindowId, Set.of());
        if (contributors.isEmpty()) {
            toolWindow.setAvailable(false);
            return;
        }

        if (!myActivationActionsRegistered && ToolWindowId.SERVICES.equals(toolWindowId)) {
            myActivationActionsRegistered = true;
            registerActivateByContributorActions(myProject, contributors);
        }

        Set<? extends ServiceViewContributor<?>> activeContributors = getActiveContributors();

        boolean active = !Collections.disjoint(activeContributors, contributors);

        toolWindow.setAvailable(true);

        if (active) {
            myActiveToolWindowIds.add(toolWindowId);
        }

        restoreBrokenToolWindowIfNeeded(toolWindow);
    }

    @Deprecated
    @DeprecationInfo("@see initToolWindow()")
    private void registerToolWindows(Collection<String> toolWindowIds) {
        Set<? extends ServiceViewContributor<?>> activeContributors = getActiveContributors();
        for (Map.Entry<String, Collection<ServiceViewContributor<?>>> entry : myGroups.entrySet()) {
            if (!toolWindowIds.contains(entry.getKey())) {
                continue;
            }

            Collection<ServiceViewContributor<?>> contributors = entry.getValue();
            ServiceViewContributor<?> contributor = ContainerUtil.getFirstItem(contributors, null);
            if (contributor == null) {
                continue;
            }

            ServiceViewToolWindowDescriptor descriptor = ToolWindowId.SERVICES.equals(entry.getKey())
                ? getServicesToolWindowDescriptor()
                : getContributorToolWindowDescriptor(contributor);
            registerToolWindow(descriptor, !Collections.disjoint(activeContributors, contributors));
        }
    }

    private void registerToolWindow(@Nonnull ServiceViewToolWindowDescriptor descriptor, boolean active) {
        if (myProject.isDefault()) {
            return;
        }

        ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(myProject);
        toolWindowManager.invokeLater(() -> {
            String toolWindowId = descriptor.getToolWindowId();

            if (!myActivationActionsRegistered && ToolWindowId.SERVICES.equals(toolWindowId)) {
                myActivationActionsRegistered = true;
                Collection<ServiceViewContributor<?>> contributors = myGroups.get(ToolWindowId.SERVICES);
                if (contributors != null) {
                    registerActivateByContributorActions(myProject, contributors);
                }
            }

            ToolWindow toolWindow = toolWindowManager.getToolWindow(toolWindowId);
            if (toolWindow == null) {
                throw new IllegalArgumentException("There no toolWindow registered for id: " + toolWindowId);
            }

            toolWindow.setAvailable(true);

            if (active) {
                myActiveToolWindowIds.add(toolWindowId);
            }
            restoreBrokenToolWindowIfNeeded(toolWindow);
        });
    }

    /*
     * Temporary fix for restoring Services Tool Window (IDEA-288804)
     */
    @Deprecated(forRemoval = true)
    private static void restoreBrokenToolWindowIfNeeded(@Nonnull ToolWindow toolWindow) {
        if (!toolWindow.isShowStripeButton() && toolWindow.isVisible()) {
            toolWindow.hide();
            toolWindow.show();
        }
    }

    private void updateToolWindow(@Nonnull String toolWindowId, boolean active, boolean show) {
        if (myProject.isDisposed() || myProject.isDefault()) {
            return;
        }

        ApplicationManager.getApplication().invokeLater(() -> {
            ToolWindow toolWindow = ToolWindowManager.getInstance(myProject).getToolWindow(toolWindowId);
            if (toolWindow == null) {
                return;
            }

            if (active) {
                boolean doShow = show && !myActiveToolWindowIds.contains(toolWindowId);
                myActiveToolWindowIds.add(toolWindowId);
                if (doShow) {
                    toolWindow.show();
                }
            }
            else if (myActiveToolWindowIds.remove(toolWindowId)) {
                // Hide tool window only if model roots became empty and there were some services shown before update.
                toolWindow.hide();
            }
        }, ModalityState.nonModal(), myProject.getDisposed());
    }


    public void createToolWindowContent(@Nonnull ToolWindow toolWindow) {
        String toolWindowId = toolWindow.getId();
        Collection<ServiceViewContributor<?>> contributors = myGroups.get(toolWindowId);
        if (contributors == null) {
            return;
        }

        if (myAutoScrollToSourceHandler == null) {
            myAutoScrollToSourceHandler = ServiceViewSourceScrollHelper.createAutoScrollToSourceHandler(myProject);
        }
        ToolWindowEx toolWindowEx = (ToolWindowEx)toolWindow;
        ServiceViewSourceScrollHelper.installAutoScrollSupport(myProject, toolWindowEx, myAutoScrollToSourceHandler);

        Pair<ServiceViewState, List<ServiceViewState>> states = getServiceViewStates(toolWindowId);
        ServiceViewModel.AllServicesModel mainModel = new ServiceViewModel.AllServicesModel(myModel, myModelFilter, contributors);
        ServiceView mainView = ServiceView.createView(myProject, mainModel, prepareViewState(states.first));
        mainView.setAutoScrollToSourceHandler(myAutoScrollToSourceHandler);

        ContentManager contentManager = toolWindow.getContentManager();
        ServiceViewContentHolder holder = new ServiceViewContentHolder(mainView, contentManager, contributors, toolWindowId);
        myContentHolders.add(holder);
        contentManager.addContentManagerListener(new ServiceViewContentMangerListener(myModelFilter, myAutoScrollToSourceHandler, holder));

        addMainContent(toolWindow.getContentManager(), mainView);
        loadViews(contentManager, mainView, contributors, states.second);
        ServiceViewDragHelper.installDnDSupport(myProject, toolWindowEx.getDecorator(), contentManager);
    }

    private static void addMainContent(ContentManager contentManager, ServiceView mainView) {
        Content mainContent = ContentFactory.getInstance().createContent(mainView, null, false);
        mainContent.putUserData(ToolWindow.SHOW_CONTENT_ICON, Boolean.TRUE);
        mainContent.setHelpId(getToolWindowContextHelpId());
        mainContent.setCloseable(false);

        Disposer.register(mainContent, mainView);
        Disposer.register(mainContent, mainView.getModel());

        contentManager.addContent(mainContent);
        mainView.getModel().addModelListener(() -> {
            boolean isEmpty = mainView.getModel().getRoots().isEmpty();
            AppUIExecutor.onUiThread().expireWith(contentManager).submit(() -> {
                if (isEmpty) {
                    if (contentManager.getIndexOfContent(mainContent) < 0) {
                        if (contentManager.getContentCount() == 0) {
                            contentManager.addContent(mainContent, 0);
                        }
                    }
                    else if (contentManager.getContentCount() > 1) {
                        contentManager.removeContent(mainContent, false);
                    }
                }
                else {
                    if (contentManager.getIndexOfContent(mainContent) < 0) {
                        contentManager.addContent(mainContent, 0);
                    }
                }
            });
        });
    }

    private void loadViews(
        ContentManager contentManager,
        ServiceView mainView,
        Collection<? extends ServiceViewContributor<?>> contributors,
        List<ServiceViewState> viewStates
    ) {
        myModel.getInvoker().invokeLater(() -> {
            Map<String, ServiceViewContributor<?>> contributorsMap = FactoryMap.create(className -> {
                for (ServiceViewContributor<?> contributor : contributors) {
                    if (className.equals(contributor.getClass().getName())) {
                        return contributor;
                    }
                }
                return null;
            });
            List<ServiceModelFilter.ServiceViewFilter> filters = new ArrayList<>();

            List<Pair<ServiceViewModel, ServiceViewState>> loadedModels = new ArrayList<>();
            ServiceViewModel toSelect = null;

            for (ServiceViewState viewState : viewStates) {
                ServiceModelFilter.ServiceViewFilter parentFilter = mainView.getModel().getFilter();
                if (viewState.parentView >= 0 && viewState.parentView < filters.size()) {
                    parentFilter = filters.get(viewState.parentView);
                }
                ServiceModelFilter.ServiceViewFilter filter = parentFilter;
                ServiceViewModel viewModel = ServiceViewModel.loadModel(viewState, myModel, myModelFilter, parentFilter, contributorsMap);
                if (viewModel != null) {
                    loadedModels.add(Pair.create(viewModel, viewState));
                    if (viewState.isSelected) {
                        toSelect = viewModel;
                    }
                    filter = viewModel.getFilter();
                }
                filters.add(filter);
            }

            if (!loadedModels.isEmpty()) {
                ServiceViewModel modelToSelect = toSelect;
                AppUIExecutor.onUiThread().expireWith(contentManager).submit(() -> {
                    for (Pair<ServiceViewModel, ServiceViewState> pair : loadedModels) {
                        extract(contentManager, pair.first, pair.second, false);
                    }
                    selectContentByModel(contentManager, modelToSelect);
                });
            }
        });
    }

    @Override
    public @Nonnull Promise<Void> select(@Nonnull Object service, @Nonnull Class<?> contributorClass, boolean activate, boolean focus) {
        return trackingSelect(service, contributorClass, activate, focus).then(result -> null);
    }

    public @Nonnull Promise<Boolean> trackingSelect(
        @Nonnull Object service,
        @Nonnull Class<?> contributorClass,
        boolean activate,
        boolean focus
    ) {
        if (!myState.selectActiveService) {
            if (activate) {
                String toolWindowId = getToolWindowId(contributorClass);
                if (toolWindowId == null) {
                    return Promises.rejectedPromise("Contributor group not found");
                }
                ToolWindowManager.getInstance(myProject).invokeLater(() -> {
                    ToolWindow toolWindow = ToolWindowManager.getInstance(myProject).getToolWindow(toolWindowId);
                    if (toolWindow != null) {
                        toolWindow.activate(null, focus, focus);
                    }
                });
            }
            return expand(service, contributorClass).then(o -> false);
        }
        return doSelect(service, contributorClass, activate, focus).then(o -> true);
    }

    private @Nonnull Promise<Void> doSelect(@Nonnull Object service, @Nonnull Class<?> contributorClass, boolean activate, boolean focus) {
        AsyncPromise<Void> result = new AsyncPromise<>();
        // Ensure model is updated, then iterate over service views on EDT in order to find view with service and select it.
        myModel.getInvoker().invoke(() -> AppUIUtil.invokeLaterIfProjectAlive(myProject, () -> {
            String toolWindowId = getToolWindowId(contributorClass);
            if (toolWindowId == null) {
                result.setError("Contributor group not found");
                return;
            }
            Runnable runnable = () -> promiseFindView(
                contributorClass,
                result,
                serviceView -> serviceView.select(service, contributorClass),
                content -> selectContent(content, focus, myProject)
            );
            ToolWindow toolWindow = activate ? ToolWindowManager.getInstance(myProject).getToolWindow(toolWindowId) : null;
            if (toolWindow != null) {
                toolWindow.activate(runnable, focus, focus);
            }
            else {
                runnable.run();
            }
        }));
        return result;
    }

    private void promiseFindView(
        Class<?> contributorClass,
        AsyncPromise<Void> result,
        Function<? super ServiceView, ? extends Promise<?>> action,
        Consumer<? super Content> onSuccess
    ) {
        ServiceViewContentHolder holder = getContentHolder(contributorClass);
        if (holder == null) {
            result.setError("Content manager not initialized");
            return;
        }
        List<Content> contents = new SmartList<>(holder.contentManager.getContents());
        if (contents.isEmpty()) {
            result.setError("Content not initialized");
            return;
        }
        Collections.reverse(contents);

        promiseFindView(contents.iterator(), result, action, onSuccess);
    }

    private static void promiseFindView(
        Iterator<? extends Content> iterator,
        AsyncPromise<Void> result,
        Function<? super ServiceView, ? extends Promise<?>> action,
        Consumer<? super Content> onSuccess
    ) {
        Content content = iterator.next();
        ServiceView serviceView = getServiceView(content);
        if (serviceView == null || content.getManager() == null) {
            if (iterator.hasNext()) {
                promiseFindView(iterator, result, action, onSuccess);
            }
            else {
                result.setError("Not services content");
            }
            return;
        }
        action.apply(serviceView)
            .onSuccess(v -> {
                if (onSuccess != null) {
                    onSuccess.accept(content);
                }
                result.setResult(null);
            })
            .onError(e -> {
                if (iterator.hasNext()) {
                    AppUIExecutor.onUiThread().expireWith(serviceView.getProject()).submit(() -> {
                        promiseFindView(iterator, result, action, onSuccess);
                    });
                }
                else {
                    result.setError(e);
                }
            });
    }

    private static void selectContent(Content content, boolean focus, Project project) {
        AppUIExecutor.onUiThread().expireWith(content).submit(() -> {
            ContentManager contentManager = content.getManager();
            if (contentManager == null) {
                return;
            }

            if (contentManager.getSelectedContent() != content && contentManager.getIndexOfContent(content) >= 0) {
                contentManager.setSelectedContent(content, focus);
            }
        });
    }

    @Override
    public @Nonnull Promise<Void> expand(@Nonnull Object service, @Nonnull Class<?> contributorClass) {
        AsyncPromise<Void> result = new AsyncPromise<>();
        // Ensure model is updated, then iterate over service views on EDT in order to find view with service and select it.
        myModel.getInvoker().invoke(() -> AppUIUtil.invokeLaterIfProjectAlive(myProject, () ->
            promiseFindView(contributorClass, result,
                serviceView -> serviceView.expand(service, contributorClass),
                null
            )));
        return result;
    }

    @Override
    public @Nonnull Promise<Void> extract(@Nonnull Object service, @Nonnull Class<?> contributorClass) {
        AsyncPromise<Void> result = new AsyncPromise<>();
        myModel.getInvoker().invoke(() -> AppUIUtil.invokeLaterIfProjectAlive(
            myProject,
            () -> promiseFindView(
                contributorClass,
                result,
                serviceView -> serviceView.extract(service, contributorClass),
                null
            )
        ));
        return result;
    }

    @Nonnull
    Promise<Void> select(@Nonnull VirtualFile virtualFile) {
        List<ServiceViewItem> selectedItems = new SmartList<>();
        for (ServiceViewContentHolder contentHolder : myContentHolders) {
            Content content = contentHolder.contentManager.getSelectedContent();
            if (content == null) {
                continue;
            }

            ServiceView serviceView = getServiceView(content);
            if (serviceView == null) {
                continue;
            }

            List<ServiceViewItem> items = serviceView.getSelectedItems();
            ContainerUtil.addIfNotNull(selectedItems, ContainerUtil.getOnlyItem(items));
        }

        AsyncPromise<Void> result = new AsyncPromise<>();
        myModel.getInvoker().invoke(() -> {
            Predicate<? super ServiceViewItem> fileCondition = item -> {
                ServiceViewDescriptor descriptor = item.getViewDescriptor();
                return descriptor instanceof ServiceViewLocatableDescriptor &&
                    virtualFile.equals(((ServiceViewLocatableDescriptor)descriptor).getVirtualFile());
            };

            // Multiple services may target to one virtual file.
            // Do nothing if service, targeting to the given virtual file, is selected,
            // otherwise it may lead to jumping selection,
            // if editor have just been selected due to some service selection.
            if (ContainerUtil.find(selectedItems, fileCondition) != null) {
                result.setResult(null);
                return;
            }

            ServiceViewItem fileItem = myModel.findItem(
                item -> !(item instanceof ServiceModel.ServiceNode) ||
                    item.getViewDescriptor() instanceof ServiceViewLocatableDescriptor,
                fileCondition
            );
            if (fileItem != null) {
                Promise<Void> promise = doSelect(fileItem.getValue(), fileItem.getRootContributor().getClass(), false, false);
                promise.processed(result);
            }
        });
        return result;
    }

    void extract(@Nonnull ServiceViewDragHelper.ServiceViewDragBean dragBean) {
        List<ServiceViewItem> items = dragBean.getItems();
        if (items.isEmpty()) {
            return;
        }

        ServiceView serviceView = dragBean.getServiceView();
        ServiceViewContentHolder holder = getContentHolder(serviceView);
        if (holder == null) {
            return;
        }

        ServiceModelFilter.ServiceViewFilter parentFilter = serviceView.getModel().getFilter();
        ServiceViewModel viewModel = ServiceViewModel.createModel(items, dragBean.getContributor(), myModel, myModelFilter, parentFilter);
        ServiceViewState state = new ServiceViewState();
        serviceView.saveState(state);
        extract(holder.contentManager, viewModel, state, true);
    }

    private void extract(ContentManager contentManager, ServiceViewModel viewModel, ServiceViewState viewState, boolean select) {
        ServiceView serviceView = ServiceView.createView(myProject, viewModel, prepareViewState(viewState));
        ItemPresentation presentation = getContentPresentation(myProject, viewModel, viewState);
        if (presentation == null) {
            return;
        }

        Content content = addServiceContent(contentManager, serviceView, presentation, select);
        if (viewModel instanceof ServiceViewModel.GroupModel) {
            extractGroup((ServiceViewModel.GroupModel)viewModel, content);
        }
        else if (viewModel instanceof ServiceViewModel.SingeServiceModel) {
            extractService((ServiceViewModel.SingeServiceModel)viewModel, content);
        }
        else if (viewModel instanceof ServiceViewModel.ServiceListModel) {
            extractList((ServiceViewModel.ServiceListModel)viewModel, content);
        }
    }

    private static void extractGroup(ServiceViewModel.GroupModel viewModel, Content content) {
        viewModel.addModelListener(() -> updateContentTab(viewModel.getGroup(), content));
        updateContentTab(viewModel.getGroup(), content);
    }

    private void extractService(ServiceViewModel.SingeServiceModel viewModel, Content content) {
        ContentManager contentManager = content.getManager();
        viewModel.addModelListener(() -> {
            ServiceViewItem item = viewModel.getService();
            if (item != null && !viewModel.getChildren(item).isEmpty() && contentManager != null) {
                AppUIExecutor.onUiThread().expireWith(contentManager).submit(() -> {
                    ServiceViewItem viewItem = viewModel.getService();
                    if (viewItem == null) {
                        return;
                    }

                    int index = contentManager.getIndexOfContent(content);
                    if (index < 0) {
                        return;
                    }

                    contentManager.removeContent(content, true);
                    ServiceViewModel.ServiceListModel listModel = new ServiceViewModel.ServiceListModel(
                        myModel,
                        myModelFilter,
                        new SmartList<>(viewItem),
                        viewModel.getFilter().getParent()
                    );
                    ServiceView listView = ServiceView.createView(myProject, listModel, prepareViewState(new ServiceViewState()));
                    Content listContent =
                        addServiceContent(contentManager, listView, viewItem.getViewDescriptor().getContentPresentation(), true, index);
                    extractList(listModel, listContent);
                });
            }
            else {
                updateContentTab(item, content);
            }
        });
        updateContentTab(viewModel.getService(), content);
    }

    private static void extractList(ServiceViewModel.ServiceListModel viewModel, Content content) {
        viewModel.addModelListener(() -> updateContentTab(ContainerUtil.getOnlyItem(viewModel.getRoots()), content));
        updateContentTab(ContainerUtil.getOnlyItem(viewModel.getRoots()), content);
    }

    private static ItemPresentation getContentPresentation(Project project, ServiceViewModel viewModel, ServiceViewState viewState) {
        if (viewModel instanceof ServiceViewModel.ContributorModel) {
            return ((ServiceViewModel.ContributorModel)viewModel).getContributor().getViewDescriptor(project).getContentPresentation();
        }
        else if (viewModel instanceof ServiceViewModel.GroupModel) {
            return ((ServiceViewModel.GroupModel)viewModel).getGroup().getViewDescriptor().getContentPresentation();
        }
        else if (viewModel instanceof ServiceViewModel.SingeServiceModel) {
            return ((ServiceViewModel.SingeServiceModel)viewModel).getService().getViewDescriptor().getContentPresentation();
        }
        else if (viewModel instanceof ServiceViewModel.ServiceListModel) {
            List<ServiceViewItem> items = ((ServiceViewModel.ServiceListModel)viewModel).getItems();
            if (items.size() == 1) {
                return items.get(0).getViewDescriptor().getContentPresentation();
            }
            String name = viewState.id;
            if (StringUtil.isEmpty(name)) {
                name = Messages.showInputDialog(
                    project,
                    ExecutionLocalize.serviceViewGroupLabel().get(),
                    ExecutionLocalize.serviceViewGroupTitle().get(),
                    null,
                    null,
                    null
                );
                if (StringUtil.isEmpty(name)) {
                    return null;
                }
            }
            return new PresentationData(name, null, PlatformIconGroup.nodesFolder(), null);
        }
        return null;
    }

    private static Content addServiceContent(
        ContentManager contentManager,
        ServiceView serviceView,
        ItemPresentation presentation,
        boolean select
    ) {
        return addServiceContent(contentManager, serviceView, presentation, select, -1);
    }

    private static Content addServiceContent(
        ContentManager contentManager,
        ServiceView serviceView,
        ItemPresentation presentation,
        boolean select,
        int index
    ) {
        Content content =
            ContentFactory.getInstance().createContent(serviceView, ServiceViewDragHelper.getDisplayName(presentation), false);
        content.putUserData(ToolWindow.SHOW_CONTENT_ICON, Boolean.TRUE);
        content.setHelpId(getToolWindowContextHelpId());
        content.setCloseable(true);
        content.setIcon(presentation.getIcon(false));

        Disposer.register(content, serviceView);
        Disposer.register(content, serviceView.getModel());

        contentManager.addContent(content, index);
        if (select) {
            contentManager.setSelectedContent(content);
        }
        return content;
    }

    private static void updateContentTab(ServiceViewItem item, Content content) {
        if (item != null) {
            WeakReference<ServiceViewItem> itemRef = new WeakReference<>(item);
            AppUIExecutor.onUiThread().expireWith(content).submit(() -> {
                ServiceViewItem viewItem = itemRef.get();
                if (viewItem == null) {
                    return;
                }

                ItemPresentation itemPresentation = viewItem.getViewDescriptor().getContentPresentation();
                content.setDisplayName(ServiceViewDragHelper.getDisplayName(itemPresentation));
                content.setIcon(itemPresentation.getIcon());
                content.setTabColor(viewItem.getColor());
            });
        }
    }

    private void addToGroup(ServiceViewContributor<?> contributor) {
        String id = contributor.getViewDescriptor(myProject).getId();
        ServiceViewToolWindowDescriptor descriptor = getContributorToolWindowDescriptor(contributor);
        String toolWindowId = ToolWindowId.SERVICES;
        if ((descriptor.isExcludedByDefault() && !myState.included.contains(id)) ||
            !descriptor.isExcludedByDefault() && myState.excluded.contains(id)) {
            toolWindowId = descriptor.getToolWindowId();
        }
        Collection<ServiceViewContributor<?>> contributors =
            myGroups.computeIfAbsent(toolWindowId, __ -> ConcurrentHashMap.newKeySet());
        contributors.add(contributor);
    }

    private @Nonnull Pair<ServiceViewState, List<ServiceViewState>> getServiceViewStates(@Nonnull String groupId) {
        List<ServiceViewState> states =
            ContainerUtil.filter(myState.viewStates, state -> groupId.equals(state.groupId) && !StringUtil.isEmpty(state.viewType));
        ServiceViewState mainState =
            ContainerUtil.find(myState.viewStates, state -> groupId.equals(state.groupId) && StringUtil.isEmpty(state.viewType));
        if (mainState == null) {
            mainState = new ServiceViewState();
        }
        return Pair.create(mainState, states);
    }

    @Override
    public @Nonnull State getState() {
        List<String> services = ContainerUtil.mapNotNull(
            myGroups.getOrDefault(ToolWindowId.SERVICES, Collections.emptyList()),
            contributor -> contributor.getViewDescriptor(myProject).getId()
        );
        List<String> includedByDefault = new ArrayList<>();
        List<String> excludedByDefault = new ArrayList<>();
        myProject.getApplication().getExtensionPoint(ServiceViewContributor.class).forEach(contributor -> {
            String id = contributor.getViewDescriptor(myProject).getId();
            if (id == null) {
                return;
            }
            if (getContributorToolWindowDescriptor(contributor).isExcludedByDefault()) {
                excludedByDefault.add(id);
            }
            else {
                includedByDefault.add(id);
            }
        });
        myState.included.clear();
        myState.included.addAll(excludedByDefault);
        myState.included.retainAll(services);
        myState.excluded.clear();
        myState.excluded.addAll(includedByDefault);
        myState.excluded.removeAll(services);

        ContainerUtil.retainAll(myState.viewStates, state -> myGroups.containsKey(state.groupId));
        for (ServiceViewContentHolder holder : myContentHolders) {
            ContainerUtil.retainAll(myState.viewStates, state -> !holder.toolWindowId.equals(state.groupId));

            ServiceModelFilter.ServiceViewFilter mainFilter = holder.mainView.getModel().getFilter();
            ServiceViewState mainState = new ServiceViewState();
            myState.viewStates.add(mainState);
            holder.mainView.saveState(mainState);
            mainState.groupId = holder.toolWindowId;
            mainState.treeStateElement = new Element("root");
            mainState.treeState.writeExternal(mainState.treeStateElement);
            mainState.clearTreeState();

            List<ServiceView> processedViews = new SmartList<>();
            for (Content content : holder.contentManager.getContents()) {
                ServiceView serviceView = getServiceView(content);
                if (serviceView == null || isMainView(serviceView)) {
                    continue;
                }

                ServiceViewState viewState = new ServiceViewState();
                processedViews.add(serviceView);
                myState.viewStates.add(viewState);
                serviceView.saveState(viewState);
                viewState.groupId = holder.toolWindowId;
                viewState.isSelected = holder.contentManager.isSelected(content);
                ServiceViewModel viewModel = serviceView.getModel();
                if (viewModel instanceof ServiceViewModel.ServiceListModel) {
                    viewState.id = content.getDisplayName();
                }
                ServiceModelFilter.ServiceViewFilter parentFilter = viewModel.getFilter().getParent();
                if (parentFilter != null && !parentFilter.equals(mainFilter)) {
                    for (int i = 0; i < processedViews.size(); i++) {
                        ServiceView parentView = processedViews.get(i);
                        if (parentView.getModel().getFilter().equals(parentFilter)) {
                            viewState.parentView = i;
                            break;
                        }
                    }
                }

                viewState.treeStateElement = new Element("root");
                viewState.treeState.writeExternal(viewState.treeStateElement);
                viewState.clearTreeState();
            }
        }

        return myState;
    }

    @Override
    public void loadState(@Nonnull State state) {
        clearViewStateIfNeeded(state);
        myState = state;
        for (ServiceViewState viewState : myState.viewStates) {
            viewState.treeState = TreeState.createFrom(viewState.treeStateElement);
        }
    }

    void loadGroups() {
        myProject.getApplication().getExtensionPoint(ServiceViewContributor.class).forEach(contributor -> {
            addToGroup(contributor);
            myNotInitializedContributors.add(contributor);
        });

        Disposable disposable = Disposable.newDisposable();
        Disposer.register(myProject, disposable);
        myProject.getMessageBus().connect(disposable).subscribe(ToolWindowManagerListener.class, new ToolWindowManagerListener() {
            @Override
            public void toolWindowShown(@Nonnull ToolWindow toolWindow) {
                Collection<ServiceViewContributor<?>> contributors = myGroups.get(toolWindow.getId());
                if (contributors != null) {
                    for (ServiceViewContributor<?> contributor : contributors) {
                        if (myNotInitializedContributors.remove(contributor)) {
                            ServiceEventListener.ServiceEvent e =
                                ServiceEventListener.ServiceEvent.createResetEvent(contributor.getClass());
                            myModel.handle(e);
                        }
                    }
                }
                if (myNotInitializedContributors.isEmpty()) {
                    Disposer.dispose(disposable);
                }
            }
        });
    }

    private static void clearViewStateIfNeeded(@Nonnull State state) {
        // TODO [konstantin.aleev] temporary check state for invalid values cause by 2399fc301031caea7fa90916a87114b1a98c0177
        if (state.viewStates == null) {
            state.viewStates = new SmartList<>();
            return;
        }
        for (Object o : state.viewStates) {
            if (!(o instanceof ServiceViewState)) {
                state.viewStates = new SmartList<>();
                return;
            }
        }
    }

    public static final class State {
        public List<ServiceViewState> viewStates = new ArrayList<>();

        public boolean showServicesTree = true;
        public boolean selectActiveService = true;
        public final Set<String> included = new HashSet<>();
        public final Set<String> excluded = new HashSet<>();
    }

    static String getToolWindowContextHelpId() {
        return HELP_ID;
    }

    private ServiceViewState prepareViewState(ServiceViewState state) {
        state.showServicesTree = myState.showServicesTree;
        return state;
    }

    boolean isShowServicesTree() {
        return myState.showServicesTree;
    }

    void setShowServicesTree(boolean value) {
        myState.showServicesTree = value;
        for (ServiceViewContentHolder holder : myContentHolders) {
            for (ServiceView serviceView : holder.getServiceViews()) {
                serviceView.getUi().setMasterComponentVisible(value);
            }
        }
    }

    boolean isSelectActiveService() {
        return myState.selectActiveService;
    }

    void setSelectActiveService(boolean value) {
        myState.selectActiveService = value;
    }

    boolean isSplitByTypeEnabled(@Nonnull ServiceView selectedView) {
        if (!isMainView(selectedView) ||
            selectedView.getModel().getVisibleRoots().isEmpty()) {
            return false;
        }

        ServiceViewContentHolder holder = getContentHolder(selectedView);
        if (holder == null) {
            return false;
        }

        for (Content content : holder.contentManager.getContents()) {
            ServiceView serviceView = getServiceView(content);
            if (serviceView != null && serviceView != selectedView && !(serviceView.getModel() instanceof ServiceViewModel.ContributorModel)) {
                return false;
            }
        }
        return true;
    }

    void splitByType(@Nonnull ServiceView selectedView) {
        ServiceViewContentHolder holder = getContentHolder(selectedView);
        if (holder == null) {
            return;
        }

        myModel.getInvoker().invokeLater(() -> {
            List<ServiceViewContributor<?>> contributors = ContainerUtil.map(myModel.getRoots(), ServiceViewItem::getRootContributor);
            AppUIUtil.invokeOnEdt(() -> {
                for (ServiceViewContributor<?> contributor : contributors) {
                    splitByType(holder.contentManager, contributor);
                }
            });
        });
    }

    private ServiceViewContentHolder getContentHolder(ServiceView serviceView) {
        for (ServiceViewContentHolder holder : myContentHolders) {
            if (holder.getServiceViews().contains(serviceView)) {
                return holder;
            }
        }
        return null;
    }

    private void splitByType(ContentManager contentManager, ServiceViewContributor<?> contributor) {
        for (Content content : contentManager.getContents()) {
            ServiceView serviceView = getServiceView(content);
            if (serviceView != null) {
                ServiceViewModel viewModel = serviceView.getModel();
                if (viewModel instanceof ServiceViewModel.ContributorModel && contributor.equals(((ServiceViewModel.ContributorModel)viewModel).getContributor())) {
                    return;
                }
            }
        }

        ServiceViewModel.ContributorModel contributorModel =
            new ServiceViewModel.ContributorModel(myModel, myModelFilter, contributor, null);
        extract(contentManager, contributorModel, prepareViewState(new ServiceViewState()), true);
    }

    public @Nonnull List<Object> getChildrenSafe(
        @Nonnull AnActionEvent e,
        @Nonnull List<Object> valueSubPath,
        @Nonnull Class<?> contributorClass
    ) {
        ServiceView serviceView = ServiceViewActionProvider.getSelectedView(e);
        return serviceView != null ? serviceView.getChildrenSafe(valueSubPath, contributorClass) : Collections.emptyList();
    }

    @Override
    public @Nullable String getToolWindowId(@Nonnull Class<?> contributorClass) {
        for (Map.Entry<String, Collection<ServiceViewContributor<?>>> entry : myGroups.entrySet()) {
            if (ContainerUtil.exists(entry.getValue(), contributorClass::isInstance)) {
                return entry.getKey();
            }
        }
        return null;
    }

    private static boolean isMainView(@Nonnull ServiceView serviceView) {
        return serviceView.getModel() instanceof ServiceViewModel.AllServicesModel;
    }

    private static @Nullable Content getMainContent(@Nonnull ContentManager contentManager) {
        for (Content content : contentManager.getContents()) {
            ServiceView serviceView = getServiceView(content);
            if (serviceView != null && isMainView(serviceView)) {
                return content;
            }
        }
        return null;
    }

    private static @Nullable ServiceView getServiceView(Content content) {
        Object component = content.getComponent();
        return component instanceof ServiceView ? (ServiceView)component : null;
    }

    private static void selectContentByModel(@Nonnull ContentManager contentManager, @Nullable ServiceViewModel modelToSelect) {
        if (modelToSelect != null) {
            for (Content content : contentManager.getContents()) {
                ServiceView serviceView = getServiceView(content);
                if (serviceView != null && serviceView.getModel() == modelToSelect) {
                    contentManager.setSelectedContent(content);
                    break;
                }
            }
        }
        else {
            Content content = getMainContent(contentManager);
            if (content != null) {
                contentManager.setSelectedContent(content);
            }
        }
    }

    private static void selectContentByContributor(@Nonnull ContentManager contentManager, @Nonnull ServiceViewContributor<?> contributor) {
        Content mainContent = null;
        for (Content content : contentManager.getContents()) {
            ServiceView serviceView = getServiceView(content);
            if (serviceView != null) {
                if (serviceView.getModel() instanceof ServiceViewModel.ContributorModel &&
                    contributor.equals(((ServiceViewModel.ContributorModel)serviceView.getModel()).getContributor())) {
                    contentManager.setSelectedContent(content, true);
                    return;
                }
                if (isMainView(serviceView)) {
                    mainContent = content;
                }
            }
        }
        if (mainContent != null) {
            contentManager.setSelectedContent(mainContent, true);
        }
    }

    private static final class ServiceViewContentMangerListener implements ContentManagerListener {
        private final ServiceModelFilter myModelFilter;
        private final AutoScrollToSourceHandler myAutoScrollToSourceHandler;
        private final ServiceViewContentHolder myContentHolder;
        private final ContentManager myContentManager;

        ServiceViewContentMangerListener(
            @Nonnull ServiceModelFilter modelFilter,
            @Nonnull AutoScrollToSourceHandler toSourceHandler,
            @Nonnull ServiceViewContentHolder contentHolder
        ) {
            myModelFilter = modelFilter;
            myAutoScrollToSourceHandler = toSourceHandler;
            myContentHolder = contentHolder;
            myContentManager = contentHolder.contentManager;
        }

        @Override
        public void contentAdded(@Nonnull ContentManagerEvent event) {
            Content content = event.getContent();
            ServiceView serviceView = getServiceView(content);
            if (serviceView != null && !isMainView(serviceView)) {
                serviceView.setAutoScrollToSourceHandler(myAutoScrollToSourceHandler);
                myModelFilter.addFilter(serviceView.getModel().getFilter());
                myContentHolder.processAllModels(ServiceViewModel::filtersChanged);

                serviceView.getModel().addModelListener(() -> {
                    if (serviceView.getModel().getRoots().isEmpty()) {
                        AppUIExecutor.onUiThread().expireWith(myContentManager).submit(() -> myContentManager.removeContent(content, true));
                    }
                });
            }

            if (myContentManager.getContentCount() > 1) {
                Content mainContent = getMainContent(myContentManager);
                if (mainContent != null) {
                    mainContent.setDisplayName(ExecutionLocalize.serviceViewAllServices().get());
                }
            }
        }

        @Override
        public void contentRemoved(@Nonnull ContentManagerEvent event) {
            ServiceView serviceView = getServiceView(event.getContent());
            if (serviceView != null && !isMainView(serviceView)) {
                myModelFilter.removeFilter(serviceView.getModel().getFilter());
                myContentHolder.processAllModels(ServiceViewModel::filtersChanged);
            }
            if (myContentManager.getContentCount() == 1) {
                Content mainContent = getMainContent(myContentManager);
                if (mainContent != null) {
                    mainContent.setDisplayName(null);
                }
            }
        }

        @Override
        public void selectionChanged(@Nonnull ContentManagerEvent event) {
            ServiceView serviceView = getServiceView(event.getContent());
            if (serviceView == null) {
                return;
            }

            if (event.getOperation() == ContentManagerEvent.ContentOperation.add) {
                serviceView.onViewSelected();
            }
            else {
                serviceView.onViewUnselected();
            }
        }
    }

    private static void registerActivateByContributorActions(
        Project project,
        Collection<? extends ServiceViewContributor<?>> contributors
    ) {
        for (ServiceViewContributor<?> contributor : contributors) {
            ActionManager actionManager = ActionManager.getInstance();
            String actionId = getActivateContributorActionId(contributor);
            if (actionId == null) {
                continue;
            }

            AnAction action = actionManager.getAction(actionId);
            if (action == null) {
                action = new ActivateToolWindowByContributorAction(contributor, contributor.getViewDescriptor(project).getPresentation());
                actionManager.registerAction(actionId, action);
            }
        }
    }

    private static String getActivateContributorActionId(ServiceViewContributor<?> contributor) {
        String name = contributor.getClass().getSimpleName();
        return name.isEmpty() ? null : "ServiceView.Activate" + name;
    }

    private static ServiceViewToolWindowDescriptor getServicesToolWindowDescriptor() {
        return new ServiceViewToolWindowDescriptor() {
            @Override
            public @Nonnull String getToolWindowId() {
                return ToolWindowId.SERVICES;
            }

            @Override
            public @Nonnull Image getToolWindowIcon() {
                return PlatformIconGroup.toolwindowsToolwindowservices();
            }

            @Override
            public @Nonnull String getStripeTitle() {
                return ExecutionLocalize.toolwindowServicesDisplayName().get();
            }
        };
    }

    private ServiceViewToolWindowDescriptor getContributorToolWindowDescriptor(ServiceViewContributor<?> rootContributor) {
        ServiceViewDescriptor descriptor = rootContributor.getViewDescriptor(myProject);
        if (descriptor instanceof ServiceViewToolWindowDescriptor) {
            return (ServiceViewToolWindowDescriptor)descriptor;
        }
        String toolWindowId = descriptor.getId();
        return new ServiceViewToolWindowDescriptor() {
            @Override
            public @Nonnull String getToolWindowId() {
                return toolWindowId;
            }

            @Override
            public @Nonnull Image getToolWindowIcon() {
                return PlatformIconGroup.toolwindowsToolwindowservices();
            }

            @Override
            public @Nonnull String getStripeTitle() {
                return toolWindowId;
            }
        };
    }

    void setExcludedContributors(@Nonnull Collection<? extends ServiceViewContributor<?>> excluded) {
        List<ServiceViewContributor<?>> toExclude = new ArrayList<>();
        List<ServiceViewContributor<?>> toInclude = new ArrayList<>();
        Collection<ServiceViewContributor<?>> services = null;
        for (Map.Entry<String, Collection<ServiceViewContributor<?>>> entry : myGroups.entrySet()) {
            if (ToolWindowId.SERVICES.equals(entry.getKey())) {
                toExclude.addAll(ContainerUtil.filter(entry.getValue(), contributor -> excluded.contains(contributor)));
                services = entry.getValue();
            }
            else {
                toInclude.addAll(ContainerUtil.filter(entry.getValue(), contributor -> !excluded.contains(contributor)));
            }
        }

        Set<String> toolWindowIds = new HashSet<>();
        toolWindowIds.addAll(excludeServices(toExclude, services));
        toolWindowIds.addAll(includeServices(toInclude, services));
        registerToolWindows(toolWindowIds);

        // Notify model listeners to update tool windows' content.
        myModel.getInvoker().invokeLater(
            () -> myProject.getApplication().getExtensionPoint(ServiceViewContributor.class).forEach(contributor -> {
                ServiceEventListener.ServiceEvent e = ServiceEventListener.ServiceEvent.createResetEvent(contributor.getClass());
                myModel.notifyListeners(e);
            })
        );

        if (toExclude.isEmpty() && !toInclude.isEmpty()) {
            toolWindowIds.add(ToolWindowId.SERVICES);
        }
        activateToolWindows(toolWindowIds);
    }

    private Set<String> excludeServices(
        @Nonnull List<ServiceViewContributor<?>> toExclude,
        @Nullable Collection<ServiceViewContributor<?>> services
    ) {
        if (toExclude.isEmpty()) {
            return Collections.emptySet();
        }

        Set<String> toolWindowIds = new HashSet<>();
        if (services != null) {
            services.removeAll(toExclude);
            if (services.isEmpty()) {
                unregisterToolWindow(ToolWindowId.SERVICES);
            }
        }
        for (ServiceViewContributor<?> contributor : toExclude) {
            unregisterActivateByContributorActions(contributor);

            ServiceViewToolWindowDescriptor descriptor = getContributorToolWindowDescriptor(contributor);
            String toolWindowId = descriptor.getToolWindowId();
            Collection<ServiceViewContributor<?>> contributors =
                myGroups.computeIfAbsent(toolWindowId, __ -> ConcurrentHashMap.newKeySet());
            if (contributors.isEmpty()) {
                toolWindowIds.add(toolWindowId);
            }
            contributors.add(contributor);
        }
        return toolWindowIds;
    }

    private Set<String> includeServices(
        @Nonnull List<ServiceViewContributor<?>> toInclude,
        @Nullable Collection<ServiceViewContributor<?>> services
    ) {
        if (toInclude.isEmpty()) {
            return Collections.emptySet();
        }

        Set<String> toolWindowIds = new HashSet<>();
        for (ServiceViewContributor<?> contributor : toInclude) {
            for (Map.Entry<String, Collection<ServiceViewContributor<?>>> entry : myGroups.entrySet()) {
                if (!ToolWindowId.SERVICES.equals(entry.getKey()) && entry.getValue().remove(contributor)) {
                    if (entry.getValue().isEmpty()) {
                        unregisterToolWindow(entry.getKey());
                    }
                    break;
                }
            }
        }

        if (services == null) {
            Collection<ServiceViewContributor<?>> servicesContributors = ConcurrentHashMap.newKeySet();
            servicesContributors.addAll(toInclude);
            myGroups.put(ToolWindowId.SERVICES, servicesContributors);
            toolWindowIds.add(ToolWindowId.SERVICES);
        }
        else {
            services.addAll(toInclude);
            registerActivateByContributorActions(myProject, toInclude);
        }
        return toolWindowIds;
    }

    private void activateToolWindows(Set<String> toolWindowIds) {
        ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(myProject);
        toolWindowManager.invokeLater(() -> {
            for (String toolWindowId : toolWindowIds) {
                if (myActiveToolWindowIds.contains(toolWindowId)) {
                    ToolWindow toolWindow = toolWindowManager.getToolWindow(toolWindowId);
                    if (toolWindow != null) {
                        toolWindow.activate(null);
                    }
                }
            }
        });
    }

    void includeToolWindow(@Nonnull String toolWindowId) {
        Set<ServiceViewContributor<?>> excluded = new HashSet<>();
        Set<ServiceViewContributor<?>> toInclude = new HashSet<>();
        for (Map.Entry<String, Collection<ServiceViewContributor<?>>> entry : myGroups.entrySet()) {
            if (toolWindowId.equals(entry.getKey())) {
                toInclude.addAll(entry.getValue());
            }
            else if (!ToolWindowId.SERVICES.equals(entry.getKey())) {
                excluded.addAll(entry.getValue());
            }
        }

        setExcludedContributors(excluded);
        Set<? extends ServiceViewContributor<?>> activeContributors = getActiveContributors();
        if (!Collections.disjoint(activeContributors, toInclude)) {
            ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(myProject);
            toolWindowManager.invokeLater(() -> {
                ToolWindow toolWindow = ToolWindowManager.getInstance(myProject).getToolWindow(ToolWindowId.SERVICES);
                if (toolWindow != null) {
                    myActiveToolWindowIds.add(ToolWindowId.SERVICES);
                    toolWindow.show();
                }
            });
        }
    }

    private void unregisterToolWindow(String toolWindowId) {
        myActiveToolWindowIds.remove(toolWindowId);
        myGroups.remove(toolWindowId);
        for (ServiceViewContentHolder holder : myContentHolders) {
            if (holder.toolWindowId.equals(toolWindowId)) {
                myContentHolders.remove(holder);
                break;
            }
        }
        ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(myProject);
        toolWindowManager.invokeLater(() -> {
            if (myProject.isDisposed() || myProject.isDefault()) {
                return;
            }

            ToolWindow toolWindow = toolWindowManager.getToolWindow(toolWindowId);
            if (toolWindow != null) {
                toolWindowManager.unregisterToolWindow(toolWindowId);
            }
        });
    }

    private static void unregisterActivateByContributorActions(ServiceViewContributor<?> extension) {
        String actionId = getActivateContributorActionId(extension);
        if (actionId != null) {
            ActionManager actionManager = ActionManager.getInstance();
            AnAction action = actionManager.getAction(actionId);
            if (action != null) {
                actionManager.unregisterAction(actionId);
            }
        }
    }

//  private final class ServiceViewExtensionPointListener implements ExtensionPointListener<ServiceViewContributor<?>> {
//    @Override
//    public void extensionAdded(@Nonnull ServiceViewContributor<?> extension, @Nonnull PluginDescriptor pluginDescriptor) {
//      addToGroup(extension);
//      String toolWindowId = getToolWindowId(extension.getClass());
//      boolean register = myGroups.get(toolWindowId).size() == 1;
//      ServiceEvent e = ServiceEvent.createResetEvent(extension.getClass());
//      myModel.handle(e).onSuccess(o -> {
//        if (register) {
//          ServiceViewItem eventRoot = ContainerUtil.find(myModel.getRoots(), root -> {
//            return extension.getClass().isInstance(root.getRootContributor());
//          });
//          assert toolWindowId != null;
//          registerToolWindow(getContributorToolWindowDescriptor(extension), eventRoot != null);
//        }
//        else {
//          eventHandled(e);
//        }
//        if (ToolWindowId.SERVICES.equals(toolWindowId)) {
//          AppUIExecutor.onUiThread().expireWith(myProject)
//                       .submit(() -> registerActivateByContributorActions(myProject, new SmartList<>(extension)));
//        }
//      });
//    }
//
//    @Override
//    public void extensionRemoved(@Nonnull ServiceViewContributor<?> extension, @Nonnull PluginDescriptor pluginDescriptor) {
//      myNotInitializedContributors.remove(extension);
//      ServiceEvent e = ServiceEvent.createUnloadSyncResetEvent(extension.getClass());
//      myModel.handle(e).onProcessed(o -> {
//        eventHandled(e);
//
//        for (Map.Entry<String, Collection<ServiceViewContributor<?>>> entry : myGroups.entrySet()) {
//          if (entry.getValue().remove(extension)) {
//            if (entry.getValue().isEmpty()) {
//              unregisterToolWindow(entry.getKey());
//            }
//            break;
//          }
//        }
//
//        unregisterActivateByContributorActions(extension);
//      });
//    }
//  }

    private static final class ActivateToolWindowByContributorAction extends DumbAwareAction {
        private final ServiceViewContributor<?> myContributor;

        ActivateToolWindowByContributorAction(ServiceViewContributor<?> contributor, ItemPresentation contributorPresentation) {
            myContributor = contributor;
            Presentation templatePresentation = getTemplatePresentation();
            templatePresentation.setTextValue(
                ExecutionLocalize.serviceViewActivateToolWindowActionName(ServiceViewDragHelper.getDisplayName(contributorPresentation))
            );
            templatePresentation.setIcon(contributorPresentation.getIcon(false));
            templatePresentation.setDescriptionValue(ExecutionLocalize.serviceViewActivateToolWindowActionDescription());
        }

        @Override
        public void actionPerformed(@Nonnull AnActionEvent e) {
            Project project = e.getData(Project.KEY);
            if (project == null) {
                return;
            }

            String toolWindowId = ServiceViewManager.getInstance(project).getToolWindowId(myContributor.getClass());
            if (toolWindowId == null) {
                return;
            }

            ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(toolWindowId);
            if (toolWindow != null) {
                toolWindow.activate(() -> {
                    ServiceViewContentHolder holder =
                        ((ServiceViewManagerImpl)ServiceViewManager.getInstance(project)).getContentHolder(myContributor.getClass());
                    if (holder != null) {
                        selectContentByContributor(holder.contentManager, myContributor);
                    }
                });
            }
        }
    }

    private record ServiceViewContentHolder(
        ServiceView mainView,
        ContentManager contentManager,
        Collection<ServiceViewContributor<?>> rootContributors,
        String toolWindowId
    ) {
        @Nonnull
        List<ServiceView> getServiceViews() {
            List<ServiceView> views = ContainerUtil.mapNotNull(contentManager.getContents(), ServiceViewManagerImpl::getServiceView);
            if (views.isEmpty()) {
                return new SmartList<>(mainView);
            }

            if (!views.contains(mainView)) {
                views = ContainerUtil.prepend(views, mainView);
            }
            return views;
        }

        private void processAllModels(Consumer<? super ServiceViewModel> consumer) {
            List<ServiceViewModel> models = ContainerUtil.map(getServiceViews(), ServiceView::getModel);
            ServiceViewModel model = ContainerUtil.getFirstItem(models);
            if (model != null) {
                model.getInvoker().invokeLater(() -> {
                    for (ServiceViewModel viewModel : models) {
                        consumer.accept(viewModel);
                    }
                });
            }
        }
    }
}
