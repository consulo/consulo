// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.execution.impl.internal.dashboard;

import com.google.common.collect.Sets;
import consulo.annotation.component.ServiceImpl;
import consulo.application.AppUIExecutor;
import consulo.application.Application;
import consulo.application.util.registry.Registry;
import consulo.component.extension.ExtensionPointName;
import consulo.component.messagebus.MessageBusConnection;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.execution.ExecutionManager;
import consulo.execution.RunManager;
import consulo.execution.RunnerAndConfigurationSettings;
import consulo.execution.configuration.ConfigurationType;
import consulo.execution.configuration.ConfigurationTypeUtil;
import consulo.execution.configuration.RunConfiguration;
import consulo.execution.dashboard.*;
import consulo.execution.event.ExecutionListener;
import consulo.execution.event.RunManagerListener;
import consulo.execution.executor.Executor;
import consulo.execution.impl.internal.ExecutionManagerImpl;
import consulo.execution.impl.internal.dashboard.tree.RunConfigurationNode;
import consulo.execution.impl.internal.dashboard.tree.RunDashboardStatusFilter;
import consulo.execution.impl.internal.service.ServiceViewManagerImpl;
import consulo.execution.impl.internal.service.ServiceViewUIUtils;
import consulo.execution.impl.internal.ui.RunContentManagerImpl;
import consulo.execution.internal.layout.RunnerLayoutUiImpl;
import consulo.execution.localize.ExecutionLocalize;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.execution.service.ServiceEventListener;
import consulo.execution.service.ServiceViewDescriptor;
import consulo.execution.service.ServiceViewManager;
import consulo.execution.ui.RunContentDescriptor;
import consulo.execution.ui.RunContentManager;
import consulo.execution.ui.layout.RunnerLayoutUi;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.process.ProcessHandler;
import consulo.project.Project;
import consulo.project.event.DumbModeListener;
import consulo.project.ui.wm.ToolWindowId;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentFactory;
import consulo.ui.ex.content.ContentManager;
import consulo.ui.ex.content.event.ContentManagerEvent;
import consulo.ui.ex.content.event.ContentManagerListener;
import consulo.ui.image.Image;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.SmartList;
import consulo.util.lang.Comparing;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static consulo.execution.impl.internal.dashboard.RunDashboardServiceViewContributor.RUN_DASHBOARD_CONTENT_TOOLBAR;

@State(name = "RunDashboard", storages = @Storage(StoragePathMacros.WORKSPACE_FILE))
@ServiceImpl
@Singleton
public final class RunDashboardManagerImpl implements RunDashboardManager, PersistentStateComponent<RunDashboardManagerImpl.State> {
    private static final ExtensionPointName<RunDashboardCustomizer> CUSTOMIZER_EP_NAME =
        new ExtensionPointName<>(RunDashboardCustomizer.class);
    private static final ExtensionPointName<RunDashboardDefaultTypesProvider> DEFAULT_TYPES_PROVIDER_EP_NAME =
        new ExtensionPointName<>(RunDashboardDefaultTypesProvider.class);
    static final ExtensionPointName<RunDashboardGroupingRule> GROUPING_RULE_EP_NAME =
        new ExtensionPointName<>(RunDashboardGroupingRule.class);

    private final Project myProject;
    private final ContentManager myContentManager;
    private final ContentManagerListener myServiceContentManagerListener;
    private State myState = new State();
    private final Set<String> myTypes = new HashSet<>();
    private final Set<RunConfiguration> myHiddenConfigurations = new HashSet<>();
    private volatile List<List<RunDashboardServiceImpl>> myServices = new SmartList<>();
    private final ReentrantReadWriteLock myServiceLock = new ReentrantReadWriteLock();
    private final RunDashboardStatusFilter myStatusFilter = new RunDashboardStatusFilter();
    private String myToolWindowId;
    private final Predicate<Content> myReuseCondition;
    private final AtomicBoolean myListenersInitialized = new AtomicBoolean();
    private RunDashboardComponentWrapper myContentWrapper;
    private JComponent myEmptyContent;

    @Inject
    public RunDashboardManagerImpl(@Nonnull Project project, @Nonnull ContentFactory contentFactory) {
        myProject = project;
        myContentManager = contentFactory.createContentManager(new PanelContentUI(), false, project);
        myServiceContentManagerListener = new ServiceContentManagerListener();
        myReuseCondition = this::canReuseContent;
        initExtensionPointListeners();

        myContentManager.addContentManagerListener(new ContentManagerListener() {
            @Override
            @RequiredUIAccess
            public void contentAdded(@Nonnull ContentManagerEvent event) {
                initServiceContentListeners();
                myContentManager.removeContentManagerListener(this);
            }
        });
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void initExtensionPointListeners() {
//    ExtensionPointListener dashboardUpdater = new ExtensionPointListener() {
//      @Override
//      public void extensionAdded(@Nonnull Object extension, @Nonnull PluginDescriptor pluginDescriptor) {
//        updateDashboard(true);
//      }
//
//      @Override
//      public void extensionRemoved(@Nonnull Object extension, @Nonnull PluginDescriptor pluginDescriptor) {
//        myProject.getMessageBus().syncPublisher(ServiceEventListener.TOPIC).handle(
//          ServiceEventListener.ServiceEvent.createUnloadSyncResetEvent(RunDashboardServiceViewContributor.class));
//      }
//    };
//    CUSTOMIZER_EP_NAME.addExtensionPointListener(dashboardUpdater, myProject);
//    GROUPING_RULE_EP_NAME.addExtensionPointListener(dashboardUpdater, myProject);
//
//    DEFAULT_TYPES_PROVIDER_EP_NAME.addExtensionPointListener(new ExtensionPointListener<>() {
//      @Override
//      public void extensionAdded(RunDashboardDefaultTypesProvider extension, @Nonnull PluginDescriptor pluginDescriptor) {
//        Set<String> types = new HashSet<>(getTypes());
//        types.addAll(extension.getDefaultTypeIds(myProject));
//        setTypes(types);
//      }
//
//      @Override
//      public void extensionRemoved(RunDashboardDefaultTypesProvider extension, @Nonnull PluginDescriptor pluginDescriptor) {
//        Set<String> types = new HashSet<>(getTypes());
//        types.removeAll(extension.getDefaultTypeIds(myProject));
//        setTypes(types);
//        dashboardUpdater.extensionRemoved(extension, pluginDescriptor);
//      }
//    }, myProject);
//    ConfigurationType.CONFIGURATION_TYPE_EP.addExtensionPointListener(new ExtensionPointListener<>() {
//      @Override
//      public void extensionAdded(ConfigurationType extension, @Nonnull PluginDescriptor pluginDescriptor) {
//        setTypes(new HashSet<>(getTypes()));
//      }
//
//      @Override
//      public void extensionRemoved(ConfigurationType extension, @Nonnull PluginDescriptor pluginDescriptor) {
//        Set<String> types = new HashSet<>(getTypes());
//        types.remove(extension.getId());
//        setTypes(types);
//        dashboardUpdater.extensionRemoved(extension, pluginDescriptor);
//      }
//    }, myProject);
    }

    private void initServiceContentListeners() {
        if (!myListenersInitialized.compareAndSet(false, true)) {
            return;
        }

        MessageBusConnection connection = myProject.getMessageBus().connect(myProject);
        connection.subscribe(RunManagerListener.class, new RunManagerListener() {
            private volatile boolean myUpdateStarted;

            @Override
            public void runConfigurationAdded(@Nonnull RunnerAndConfigurationSettings settings) {
                if (!myUpdateStarted) {
                    syncConfigurations();
                    updateDashboardIfNeeded(settings);
                }
            }

            @Override
            public void runConfigurationRemoved(@Nonnull RunnerAndConfigurationSettings settings) {
                myHiddenConfigurations.remove(settings.getConfiguration());
                if (!myUpdateStarted) {
                    syncConfigurations();
                    updateDashboardIfNeeded(settings);
                }
            }

            @Override
            public void runConfigurationChanged(@Nonnull RunnerAndConfigurationSettings settings) {
                if (!myUpdateStarted) {
                    updateDashboardIfNeeded(settings);
                }
            }

            @Override
            public void beginUpdate() {
                myUpdateStarted = true;
            }

            @Override
            public void endUpdate() {
                myUpdateStarted = false;
                syncConfigurations();
                updateDashboard(true);
            }
        });
        connection.subscribe(ExecutionListener.class, new ExecutionListener() {
            @Override
            public void processStarted(
                @Nonnull String executorId,
                @Nonnull ExecutionEnvironment env,
                @Nonnull ProcessHandler handler
            ) {
                updateDashboardIfNeeded(env.getRunnerAndConfigurationSettings());
            }

            @Override
            public void processTerminated(
                @Nonnull String executorId,
                @Nonnull ExecutionEnvironment env,
                @Nonnull ProcessHandler handler,
                int exitCode
            ) {
                updateDashboardIfNeeded(env.getRunnerAndConfigurationSettings());
            }
        });
        connection.subscribe(RunDashboardListener.class, this::updateDashboardIfNeeded);
        connection.subscribe(DumbModeListener.class, new DumbModeListener() {
            @Override
            public void exitDumbMode() {
                updateDashboard(false);
            }
        });
        myContentManager.addContentManagerListener(myServiceContentManagerListener);
    }

    @Override
    public ContentManager getDashboardContentManager() {
        return myContentManager;
    }

    @Override
    public @Nonnull String getToolWindowId() {
        if (myToolWindowId == null) {
            String toolWindowId = ServiceViewManager.getInstance(myProject).getToolWindowId(RunDashboardServiceViewContributor.class);
            myToolWindowId = toolWindowId != null ? toolWindowId : ToolWindowId.SERVICES;
        }
        return myToolWindowId;
    }

    @Override
    public @Nonnull Image getToolWindowIcon() {
        return PlatformIconGroup.toolwindowsToolwindowservices();
    }

    @Override
    public List<RunDashboardService> getRunConfigurations() {
        myServiceLock.readLock().lock();
        try {
            return myServices.stream().flatMap(Collection::stream).collect(Collectors.toList());
        }
        finally {
            myServiceLock.readLock().unlock();
        }
    }

    private List<RunContentDescriptor> filterByContent(List<? extends RunContentDescriptor> descriptors) {
        return ContainerUtil.filter(descriptors, descriptor -> {
            Content content = descriptor.getAttachedContent();
            return content != null && content.getManager() == myContentManager;
        });
    }

    @Override
    public boolean isShowInDashboard(@Nonnull RunConfiguration runConfiguration) {
        if (isShown(runConfiguration)) {
            return true;
        }

        RunConfiguration baseConfiguration = getBaseConfiguration(runConfiguration);
        return baseConfiguration != null && isShown(baseConfiguration);
    }

    private boolean isShown(@Nonnull RunConfiguration runConfiguration) {
        return myTypes.contains(runConfiguration.getType().getId()) && !myHiddenConfigurations.contains(runConfiguration);
    }

    @Nullable
    private static RunConfiguration getBaseConfiguration(@Nonnull RunConfiguration runConfiguration) {
        return ExecutionManagerImpl.getDelegatedRunProfile(runConfiguration) instanceof RunConfiguration runProfile ? runProfile : null;
    }

    @Override
    public @Nonnull Set<String> getTypes() {
        return Collections.unmodifiableSet(myTypes);
    }

    @Override
    public void setTypes(@Nonnull Set<String> types) {
        Set<String> removed = new HashSet<>(Sets.difference(myTypes, types));
        Set<String> added = new HashSet<>(Sets.difference(types, myTypes));

        myTypes.clear();
        myTypes.addAll(types);
        if (!myTypes.isEmpty()) {
            initServiceContentListeners();
        }

        Set<String> enableByDefaultTypes = getEnableByDefaultTypes();
        myState.configurationTypes.clear();
        myState.configurationTypes.addAll(myTypes);
        myState.configurationTypes.removeAll(enableByDefaultTypes);
        myState.excludedTypes.clear();
        myState.excludedTypes.addAll(enableByDefaultTypes);
        myState.excludedTypes.removeAll(myTypes);

        syncConfigurations();
        if (!removed.isEmpty()) {
            moveRemovedContent(getContainsTypeIdCondition(removed));
        }
        if (!added.isEmpty()) {
            moveAddedContent(getContainsTypeIdCondition(added));
        }
        updateDashboard(true);
    }

    private static Predicate<? super RunnerAndConfigurationSettings> getContainsTypeIdCondition(Collection<String> types) {
        return settings -> {
            if (types.contains(settings.getType().getId())) {
                return true;
            }

            RunConfiguration baseConfiguration = getBaseConfiguration(settings.getConfiguration());
            return baseConfiguration != null && types.contains(baseConfiguration.getType().getId());
        };
    }

    private void moveRemovedContent(Predicate<? super RunnerAndConfigurationSettings> condition) {
        RunContentManagerImpl runContentManager = (RunContentManagerImpl)myProject.getInstance(RunContentManager.class);
        for (RunDashboardService service : getRunConfigurations()) {
            Content content = service.getContent();
            if (content == null || !condition.test(service.getSettings())) {
                continue;
            }

            RunContentDescriptor descriptor = RunContentManagerImpl.getRunContentDescriptorByContent(content);
            if (descriptor == null) {
                continue;
            }

            Executor executor = RunContentManagerImpl.getExecutorByContent(content);
            if (executor == null) {
                continue;
            }

            descriptor.setContentToolWindowId(null);
            updateContentToolbar(content, true);
            runContentManager.moveContent(executor, descriptor);
        }
    }

    private void moveAddedContent(Predicate<? super RunnerAndConfigurationSettings> condition) {
        RunContentManagerImpl runContentManager = (RunContentManagerImpl)myProject.getInstance(RunContentManager.class);
        List<RunContentDescriptor> descriptors = ExecutionManager.getInstance(myProject).getRunningDescriptors(condition);
        for (RunContentDescriptor descriptor : descriptors) {
            Content content = descriptor.getAttachedContent();
            if (content == null) {
                continue;
            }

            Executor executor = RunContentManagerImpl.getExecutorByContent(content);
            if (executor == null) {
                continue;
            }

            descriptor.setContentToolWindowId(getToolWindowId());
            runContentManager.moveContent(executor, descriptor);
        }
    }

    public Set<RunConfiguration> getHiddenConfigurations() {
        return Collections.unmodifiableSet(myHiddenConfigurations);
    }

    public void hideConfigurations(Collection<? extends RunConfiguration> configurations) {
        myHiddenConfigurations.addAll(configurations);
        syncConfigurations();
        if (!configurations.isEmpty()) {
            moveRemovedContent(settings -> configurations.contains(settings.getConfiguration()) ||
                configurations.contains(getBaseConfiguration(settings.getConfiguration())));
        }
        updateDashboard(true);
    }

    public void restoreConfigurations(Collection<? extends RunConfiguration> configurations) {
        myHiddenConfigurations.removeAll(configurations);
        syncConfigurations();
        if (!configurations.isEmpty()) {
            moveAddedContent(settings -> configurations.contains(settings.getConfiguration()) ||
                configurations.contains(getBaseConfiguration(settings.getConfiguration())));
        }
        updateDashboard(true);
    }

    public boolean isOpenRunningConfigInNewTab() {
        return myState.openRunningConfigInTab;
    }

    public void setOpenRunningConfigInNewTab(boolean value) {
        myState.openRunningConfigInTab = value;
    }

    static @Nonnull List<RunDashboardCustomizer> getCustomizers(
        @Nonnull RunnerAndConfigurationSettings settings,
        @Nullable RunContentDescriptor descriptor
    ) {
        List<RunDashboardCustomizer> customizers = new SmartList<>();
        for (RunDashboardCustomizer customizer : CUSTOMIZER_EP_NAME.getExtensions()) {
            if (customizer.isApplicable(settings, descriptor)) {
                customizers.add(customizer);
            }
        }
        return customizers;
    }

    private void updateDashboardIfNeeded(@Nullable RunnerAndConfigurationSettings settings) {
        if (settings != null) {
            updateDashboardIfNeeded(settings.getConfiguration(), true);
        }
    }

    private void updateDashboardIfNeeded(@Nonnull RunConfiguration configuration, boolean withStructure) {
        if (isShowInDashboard(configuration) ||
            !filterByContent(getConfigurationDescriptors(configuration)).isEmpty()) {
            updateDashboard(withStructure);
        }
    }

    private List<RunContentDescriptor> getConfigurationDescriptors(@Nonnull RunConfiguration configuration) {
        ExecutionManager instance = ExecutionManager.getInstance(myProject);
        if (!(instance instanceof ExecutionManagerImpl)) {
            return Collections.emptyList();
        }
        return instance.getDescriptors(s -> configuration.equals(s.getConfiguration()) ||
            configuration.equals(getBaseConfiguration(s.getConfiguration())));
    }

    @Override
    public @Nonnull Predicate<Content> getReuseCondition() {
        return myReuseCondition;
    }

    private boolean canReuseContent(Content content) {
        RunContentDescriptor descriptor = RunContentManagerImpl.getRunContentDescriptorByContent(content);
        if (descriptor == null) {
            return false;
        }

        ExecutionManagerImpl executionManager = ExecutionManagerImpl.getInstance(myProject);
        Set<RunnerAndConfigurationSettings> descriptorConfigurations = executionManager.getConfigurations(descriptor);
        if (descriptorConfigurations.isEmpty()) {
            return true;
        }

        Set<RunConfiguration> storedConfigurations = new HashSet<>(RunManager.getInstance(myProject).getAllConfigurationsList());

        return !ContainerUtil.exists(descriptorConfigurations, descriptorConfiguration -> {
            RunConfiguration configuration = descriptorConfiguration.getConfiguration();
            return isShowInDashboard(configuration) && storedConfigurations.contains(configuration);
        });
    }

    @Override
    public void updateDashboard(boolean withStructure) {
        myProject.getMessageBus().syncPublisher(ServiceEventListener.TOPIC).handle(
            ServiceEventListener.ServiceEvent.createResetEvent(RunDashboardServiceViewContributor.class));
    }

    private void syncConfigurations() {
        List<RunnerAndConfigurationSettings> settingsList = ContainerUtil.filter(
            RunManager.getInstance(myProject).getAllSettings(),
            settings -> isShowInDashboard(settings.getConfiguration())
        );
        List<List<RunDashboardServiceImpl>> result = new ArrayList<>();
        myServiceLock.writeLock().lock();
        try {
            for (RunnerAndConfigurationSettings settings : settingsList) {
                List<RunDashboardServiceImpl> syncedServices = getServices(settings);
                if (syncedServices == null) {
                    syncedServices = new SmartList<>(new RunDashboardServiceImpl(settings, null));
                }
                result.add(syncedServices);
            }
            for (List<RunDashboardServiceImpl> oldServices : myServices) {
                RunDashboardService oldService = oldServices.get(0);
                if (oldService.getContent() != null && !settingsList.contains(oldService.getSettings())) {
                    if (!updateServiceSettings(result, oldServices)) {
                        result.add(oldServices);
                    }
                }
            }
            myServices = result;
        }
        finally {
            myServiceLock.writeLock().unlock();
        }
    }

    private void addServiceContent(@Nonnull Content content) {
        RunnerAndConfigurationSettings settings = findSettings(content);
        if (settings == null) {
            return;
        }

        myServiceLock.writeLock().lock();
        try {
            doAddServiceContent(settings, content);
        }
        finally {
            myServiceLock.writeLock().unlock();
        }
    }

    private void removeServiceContent(@Nonnull Content content) {
        myServiceLock.writeLock().lock();
        try {
            RunDashboardServiceImpl service = findService(content);
            if (service == null) {
                return;
            }

            doRemoveServiceContent(service);
        }
        finally {
            myServiceLock.writeLock().unlock();
            updateDashboard(true);
        }
    }

    private void updateServiceContent(@Nonnull Content content) {
        RunnerAndConfigurationSettings settings = findSettings(content);
        if (settings == null) {
            return;
        }

        myServiceLock.writeLock().lock();
        try {
            RunDashboardServiceImpl service = findService(content);
            if (service == null || service.getSettings().equals(settings)) {
                return;
            }

            doAddServiceContent(settings, content);
            doRemoveServiceContent(service);
        }
        finally {
            myServiceLock.writeLock().unlock();
        }
    }

    private void doAddServiceContent(@Nonnull RunnerAndConfigurationSettings settings, @Nonnull Content content) {
        List<RunDashboardServiceImpl> settingsServices = getServices(settings);
        if (settingsServices == null) {
            settingsServices = new SmartList<>(new RunDashboardServiceImpl(settings, content));
            myServices.add(settingsServices);
            return;
        }

        RunDashboardServiceImpl newService = new RunDashboardServiceImpl(settings, content);
        RunDashboardServiceImpl service = settingsServices.get(0);
        if (service.getContent() == null) {
            settingsServices.remove(0);
            settingsServices.add(0, newService);
        }
        else {
            settingsServices.add(newService);
        }
    }

    private void doRemoveServiceContent(@Nonnull RunDashboardServiceImpl service) {
        RunnerAndConfigurationSettings contentSettings = service.getSettings();
        List<RunDashboardServiceImpl> services = getServices(contentSettings);
        if (services == null) {
            return;
        }

        if (!isShowInDashboard(contentSettings.getConfiguration()) ||
            !RunManager.getInstance(myProject).getAllSettings().contains(contentSettings)) {
            myServices.remove(services);
            return;
        }

        services.remove(service);
        if (services.isEmpty()) {
            services.add(new RunDashboardServiceImpl(contentSettings, null));
        }
    }

    private @Nullable RunDashboardServiceImpl findService(@Nonnull Content content) {
        myServiceLock.readLock().lock();
        try {
            for (List<RunDashboardServiceImpl> services : myServices) {
                for (RunDashboardServiceImpl service : services) {
                    if (content.equals(service.getContent())) {
                        return service;
                    }
                }
            }
        }
        finally {
            myServiceLock.readLock().unlock();
            updateDashboard(true);
        }
        return null;
    }

    private @Nullable RunnerAndConfigurationSettings findSettings(@Nonnull Content content) {
        RunContentDescriptor descriptor = RunContentManagerImpl.getRunContentDescriptorByContent(content);
        if (descriptor == null) {
            return null;
        }

        RunnerAndConfigurationSettings settings = findSettings(descriptor);
        if (settings == null) {
            return null;
        }

        RunConfiguration baseConfiguration = getBaseConfiguration(settings.getConfiguration());
        if (baseConfiguration != null) {
            RunnerAndConfigurationSettings baseSettings = RunManager.getInstance(myProject).findSettings(baseConfiguration);
            if (baseSettings != null) {
                return baseSettings;
            }
        }

        return settings;
    }

    private @Nullable RunnerAndConfigurationSettings findSettings(@Nonnull RunContentDescriptor descriptor) {
        Set<RunnerAndConfigurationSettings> settingsSet = ExecutionManagerImpl.getInstance(myProject).getConfigurations(descriptor);
        RunnerAndConfigurationSettings result = ContainerUtil.getFirstItem(settingsSet);
        if (result != null) {
            return result;
        }

        ProcessHandler processHandler = descriptor.getProcessHandler();
        return processHandler == null ? null : processHandler.getUserData(RunContentManagerImpl.TEMPORARY_CONFIGURATION_KEY);
    }

    private @Nullable List<RunDashboardServiceImpl> getServices(@Nonnull RunnerAndConfigurationSettings settings) {
        for (List<RunDashboardServiceImpl> services : myServices) {
            if (services.get(0).getSettings().equals(settings)) {
                return services;
            }
        }
        return null;
    }

    private static boolean updateServiceSettings(
        List<? extends List<RunDashboardServiceImpl>> newServiceList,
        List<? extends RunDashboardServiceImpl> oldServices
    ) {
        RunDashboardServiceImpl oldService = oldServices.get(0);
        RunnerAndConfigurationSettings oldSettings = oldService.getSettings();
        for (List<RunDashboardServiceImpl> newServices : newServiceList) {
            RunnerAndConfigurationSettings newSettings = newServices.get(0).getSettings();
            if (newSettings.getType().equals(oldSettings.getType()) && newSettings.getName().equals(oldSettings.getName())) {
                newServices.remove(0);
                newServices.add(0, new RunDashboardServiceImpl(newSettings, oldService.getContent()));
                for (int i = 1; i < oldServices.size(); i++) {
                    RunDashboardServiceImpl newService = new RunDashboardServiceImpl(newSettings, oldServices.get(i).getContent());
                    newServices.add(newService);
                }
                return true;
            }
        }
        return false;
    }

    private static void updateContentToolbar(Content content, boolean visible) {
        RunContentDescriptor descriptor = RunContentManagerImpl.getRunContentDescriptorByContent(content);
        RunnerLayoutUiImpl ui = getRunnerLayoutUi(descriptor);
        if (ui != null) {
            if (!ServiceViewUIUtils.isNewServicesUIEnabled()) {
                ui.setLeftToolbarVisible(visible);
            }
            ui.setContentToolbarBefore(visible);
            if (Registry.is("ide.services.debugger.left.toolbar", true)) {
                ui.setTopLeftActionsBefore(!visible);
            }
        }
        else {
            ActionToolbar toolbar = findActionToolbar(descriptor);
            if (toolbar != null) {
                toolbar.getComponent().setVisible(visible);
            }
        }
    }

    void setSelectedContent(@Nonnull Content content) {
        ContentManager contentManager = content.getManager();
        if (contentManager == null || content == contentManager.getSelectedContent()) {
            return;
        }

        if (contentManager != myContentManager) {
            contentManager.setSelectedContent(content);
            return;
        }

        myContentManager.removeContentManagerListener(myServiceContentManagerListener);
        myContentManager.setSelectedContent(content);
        updateContentToolbar(content, false);
        myContentManager.addContentManagerListener(myServiceContentManagerListener);
    }

    void removeFromSelection(@Nonnull Content content) {
        ContentManager contentManager = content.getManager();
        if (contentManager == null || content != contentManager.getSelectedContent()) {
            return;
        }

        if (contentManager != myContentManager) {
            contentManager.removeFromSelection(content);
            return;
        }

        myContentManager.removeContentManagerListener(myServiceContentManagerListener);
        myContentManager.removeFromSelection(content);
        myContentManager.addContentManagerListener(myServiceContentManagerListener);
    }

    public @Nonnull RunDashboardStatusFilter getStatusFilter() {
        return myStatusFilter;
    }

    static @Nullable RunnerLayoutUiImpl getRunnerLayoutUi(@Nullable RunContentDescriptor descriptor) {
        if (descriptor == null) {
            return null;
        }

        RunnerLayoutUi layoutUi = descriptor.getRunnerLayoutUi();
        return layoutUi instanceof RunnerLayoutUiImpl ? (RunnerLayoutUiImpl)layoutUi : null;
    }

    static @Nullable ActionToolbar findActionToolbar(@Nullable RunContentDescriptor descriptor) {
        if (descriptor == null) {
            return null;
        }

        for (Component component : descriptor.getComponent().getComponents()) {
            if (component instanceof ActionToolbar) {
                return ((ActionToolbar)component);
            }
        }
        return null;
    }

    Set<String> getEnableByDefaultTypes() {
        Set<String> result = new HashSet<>();
        for (RunDashboardDefaultTypesProvider provider : DEFAULT_TYPES_PROVIDER_EP_NAME.getExtensionList()) {
            result.addAll(provider.getDefaultTypeIds(myProject));
        }
        return result;
    }

    JComponent getEmptyContent() {
        if (myEmptyContent == null) {
            JBPanelWithEmptyText textPanel = new JBPanelWithEmptyText()
                .withEmptyText(ExecutionLocalize.runDashboardConfigurationsMessage().get());
            textPanel.setFocusable(true);
            JPanel mainPanel = new NonOpaquePanel(new BorderLayout());
            mainPanel.add(textPanel, BorderLayout.CENTER);
            if (ServiceViewUIUtils.isNewServicesUIEnabled()) {
                if (ActionManager.getInstance().getAction(RUN_DASHBOARD_CONTENT_TOOLBAR) instanceof ActionGroup group) {
                    group.registerCustomShortcutSet(textPanel, myProject);
                    ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.SERVICES_TOOLBAR, group, true);
                    toolbar.setTargetComponent(textPanel);
                    mainPanel.add(ServiceViewUIUtils.wrapServicesAligned(toolbar), BorderLayout.NORTH);
                    toolbar.getComponent().setBorder(JBUI.Borders.emptyTop(1));
                    textPanel.setBorder(IdeBorderFactory.createBorder(SideBorder.TOP));
                }
            }
            ClientProperty.put(mainPanel, ServiceViewDescriptor.ACTION_HOLDER_KEY, Boolean.TRUE);
            myEmptyContent = mainPanel;
        }
        return myEmptyContent;
    }

    RunDashboardComponentWrapper getContentWrapper() {
        if (myContentWrapper == null) {
            myContentWrapper = new RunDashboardComponentWrapper(myProject);
            ClientProperty.put(myContentWrapper, ServiceViewDescriptor.ACTION_HOLDER_KEY, Boolean.TRUE);
        }
        return myContentWrapper;
    }

    @Override
    public @Nullable State getState() {
        myState.hiddenConfigurations.clear();
        for (RunConfiguration configuration : myHiddenConfigurations) {
            ConfigurationType type = configuration.getType();
            if (myTypes.contains(type.getId())) {
                Set<String> configurations = myState.hiddenConfigurations.get(type.getId());
                if (configurations == null) {
                    configurations = new HashSet<>();
                    myState.hiddenConfigurations.put(type.getId(), configurations);
                }
                configurations.add(configuration.getName());
            }
        }
        return myState;
    }

    @Override
    public void loadState(@Nonnull State state) {
        myState = state;
        myTypes.clear();
        myTypes.addAll(myState.configurationTypes);
        Set<String> enableByDefaultTypes = getEnableByDefaultTypes();
        enableByDefaultTypes.removeAll(myState.excludedTypes);
        myTypes.addAll(enableByDefaultTypes);
        if (!myTypes.isEmpty()) {
            loadHiddenConfigurations();
            initTypes();
        }
    }

    private void loadHiddenConfigurations() {
        for (Map.Entry<String, Set<String>> entry : myState.hiddenConfigurations.entrySet()) {
            ConfigurationType type = ConfigurationTypeUtil.findConfigurationType(entry.getKey());
            if (type == null) {
                continue;
            }

            List<RunConfiguration> configurations = RunManager.getInstance(myProject).getConfigurationsList(type);
            for (String name : entry.getValue()) {
                for (RunConfiguration configuration : configurations) {
                    if (configuration.getName().equals(name)) {
                        myHiddenConfigurations.add(configuration);
                    }
                }
            }
        }
    }

    private void initTypes() {
        syncConfigurations();
        initServiceContentListeners();
        Application.get().executeOnPooledThread(() -> {
            if (!myProject.isDisposed()) {
                updateDashboard(true);
            }
        });
    }

    @Override
    public void afterLoadState() {
        if (myTypes.isEmpty()) {
            noStateLoaded();
        }
    }

    private void noStateLoaded() {
        myTypes.clear();
        myTypes.addAll(getEnableByDefaultTypes());
        if (!myTypes.isEmpty()) {
            initTypes();
        }
    }

    static final class State {
        public final Set<String> configurationTypes = new HashSet<>();
        public final Set<String> excludedTypes = new HashSet<>();
        public final Map<String, Set<String>> hiddenConfigurations = new HashMap<>();
        public boolean openRunningConfigInTab = false;
    }

    private static final class RunDashboardServiceImpl implements RunDashboardService {
        private final RunnerAndConfigurationSettings mySettings;
        private final Content myContent;

        RunDashboardServiceImpl(
            @Nonnull RunnerAndConfigurationSettings settings,
            @Nullable Content content
        ) {
            mySettings = settings;
            myContent = content;
        }

        @Override
        public @Nonnull RunnerAndConfigurationSettings getSettings() {
            return mySettings;
        }

        @Override
        public @Nullable RunContentDescriptor getDescriptor() {
            Content content = myContent;
            return content == null ? null : RunContentManagerImpl.getRunContentDescriptorByContent(content);
        }

        @Override
        public @Nullable Content getContent() {
            return myContent;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            RunDashboardServiceImpl service = (RunDashboardServiceImpl)o;
            return mySettings.equals(service.mySettings) && Comparing.equal(myContent, service.myContent);
        }

        @Override
        public int hashCode() {
            int result = mySettings.hashCode();
            result = 31 * result + (myContent != null ? myContent.hashCode() : 0);
            return result;
        }
    }

    private final class ServiceContentManagerListener implements ContentManagerListener {
        private volatile Content myPreviousSelection = null;

        @Override
        @RequiredUIAccess
        public void selectionChanged(@Nonnull ContentManagerEvent event) {
            boolean onAdd = event.getOperation() == ContentManagerEvent.ContentOperation.add;
            Content content = event.getContent();
            if (onAdd) {
                updateContentToolbar(content, false);
                updateServiceContent(content);
            }

            updateDashboard(true);

            if (onAdd) {
                RunConfigurationNode node = createNode(content);
                if (node != null) {
                    RunnerAndConfigurationSettings settings = node.getConfigurationSettings();
                    ((ServiceViewManagerImpl)ServiceViewManager.getInstance(myProject))
                        .trackingSelect(
                            node,
                            RunDashboardServiceViewContributor.class,
                            settings.isActivateToolWindowBeforeRun(),
                            settings.isFocusToolWindowBeforeRun()
                        )
                        .onSuccess(selected -> {
                            if (selected != Boolean.TRUE) {
                                selectPreviousContent();
                            }
                        })
                        .onError(t -> selectPreviousContent());
                }
            }
            else {
                myPreviousSelection = content;
            }
        }

        private void selectPreviousContent() {
            Content previousSelection = myPreviousSelection;
            if (previousSelection != null) {
                AppUIExecutor.onUiThread().expireWith(previousSelection).submit(() -> setSelectedContent(previousSelection));
            }
        }

        @Override
        @RequiredUIAccess
        public void contentAdded(@Nonnull ContentManagerEvent event) {
            Content content = event.getContent();
            addServiceContent(content);
            if (myState.openRunningConfigInTab) {
                RunConfigurationNode node = createNode(content);
                if (node != null) {
                    ServiceViewManager.getInstance(myProject).extract(node, RunDashboardServiceViewContributor.class);
                }
            }
        }

        @Override
        @RequiredUIAccess
        public void contentRemoved(@Nonnull ContentManagerEvent event) {
            Content content = event.getContent();
            if (myPreviousSelection == content) {
                myPreviousSelection = null;
            }
            removeServiceContent(content);
        }

        private RunConfigurationNode createNode(Content content) {
            RunnerAndConfigurationSettings settings = findSettings(content);
            if (settings == null) {
                return null;
            }

            RunDashboardServiceImpl service = new RunDashboardServiceImpl(settings, content);
            RunContentDescriptor descriptor = RunContentManagerImpl.getRunContentDescriptorByContent(content);
            return new RunConfigurationNode(myProject, service, getCustomizers(settings, descriptor));
        }
    }
}
