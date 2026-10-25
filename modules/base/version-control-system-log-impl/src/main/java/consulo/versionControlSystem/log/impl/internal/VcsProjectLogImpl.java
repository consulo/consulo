/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.versionControlSystem.log.impl.internal;

import consulo.annotation.component.ServiceImpl;
import consulo.application.CachesInvalidator;
import consulo.component.messagebus.MessageBus;
import consulo.disposer.Disposer;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.versionControlSystem.VcsInitObject;
import consulo.versionControlSystem.internal.ProjectLevelVcsManagerEx;
import consulo.versionControlSystem.log.VcsLogFilterCollection;
import consulo.versionControlSystem.log.VcsLogStorage;
import consulo.versionControlSystem.log.VcsProjectLog;
import consulo.versionControlSystem.log.impl.internal.data.VcsLogDataImpl;
import consulo.versionControlSystem.log.impl.internal.data.VcsLogStorageImpl;
import consulo.versionControlSystem.log.impl.internal.data.VcsLogTabsProperties;
import consulo.versionControlSystem.log.impl.internal.data.index.VcsLogIndex;
import consulo.versionControlSystem.log.impl.internal.ui.AbstractVcsLogUi;
import consulo.versionControlSystem.log.impl.internal.util.PersistentUtil;
import consulo.versionControlSystem.root.VcsRoot;
import org.jspecify.annotations.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Singleton
@ServiceImpl
public class VcsProjectLogImpl implements VcsProjectLog {
    private static final Logger LOG = Logger.getInstance(VcsProjectLogImpl.class);

    private static final int INVALIDATE_CACHES_COUNT = 5;

    private final Map<String, Integer> myErrorCountBySource = new ConcurrentHashMap<>();
    
    private final Project myProject;
    
    private final MessageBus myMessageBus;
    
    private final VcsLogTabsProperties myUiProperties;

    
    private final LazyVcsLogManager myLogManager = new LazyVcsLogManager();
    private volatile AbstractVcsLogUi myUi;
    private @Nullable VcsLogTabsManager myTabsManager;

    @Inject
    public VcsProjectLogImpl(Project project, VcsLogTabsProperties uiProperties) {
        myProject = project;
        myMessageBus = project.getMessageBus();
        myUiProperties = uiProperties;
    }

    public @Nullable VcsLogDataImpl getDataManager() {
        VcsLogManager cached = myLogManager.getCached();
        if (cached == null) {
            return null;
        }
        return cached.getDataManager();
    }

    
    private Collection<VcsRoot> getVcsRoots() {
        return Arrays.asList(ProjectLevelVcsManager.getInstance(myProject).getAllVcsRoots());
    }

    
    @RequiredUIAccess
    public Component initMainLog(String contentTabName) {
        VcsLogManager logManager = myLogManager.getValue();
        AbstractVcsLogUi ui = logManager.createLogUi(VcsLogTabsProperties.MAIN_LOG_ID, contentTabName, null);
        myUi = ui;
        return ui.createPanel(logManager);
    }

    /**
     * The instance of the {@link AbstractVcsLogUi} or null if the log was not initialized yet.
     */
    public @Nullable AbstractVcsLogUi getMainLogUi() {
        return myUi;
    }

    public @Nullable VcsLogManager getLogManager() {
        return myLogManager.getCached();
    }

    protected void recreateLog() {
        myProject.getApplication().invokeLater(() -> {
            disposeLog();

            if (hasDvcsRoots()) {
                createLog();
            }
        }, myProject.getDisposed());
    }

    protected void recreateOnError(@Nullable Object source, Throwable error) {
        String errorSource = source instanceof VcsLogStorage ? "Storage" : source instanceof VcsLogIndex ? "Index" : "Other";
        int count = myErrorCountBySource.merge(errorSource, 1, Integer::sum);
        boolean invalidateCaches = count % INVALIDATE_CACHES_COUNT == 0;
        if (invalidateCaches) {
            LOG.error("Invalidating Vcs Log caches after " + errorSource + " corruption (count=" + count + ").", error);
        }
        else {
            LOG.debug("Recreating Vcs Log after " + errorSource + " corruption (count=" + count + ").", error);
        }

        myProject.getApplication().invokeLater(() -> {
            VcsLogManager logManager = myLogManager.getCached();
            String logId = invalidateCaches && logManager != null
                ? PersistentUtil.calcLogId(myProject, logManager.getDataManager().getLogProviders())
                : null;

            disposeLog();

            if (logId == null) {
                if (hasDvcsRoots()) {
                    createLog();
                }
                return;
            }

            myProject.getApplication().executeOnPooledThread(() -> {
                VcsLogStorageImpl.cleanupStorageFiles(logId);
                myProject.getApplication().invokeLater(() -> {
                    if (hasDvcsRoots()) {
                        createLog();
                    }
                }, myProject.getDisposed());
            });
        }, myProject.getDisposed());
    }

    @RequiredUIAccess
    private void disposeLog() {
        myUi = null;
        myLogManager.drop();
    }

    @RequiredUIAccess
    public void createLog() {
        VcsLogManager logManager = myLogManager.getValue();
        VcsLogTabsManager tabsManager = myTabsManager;
        if (tabsManager != null) {
            tabsManager.createTabs();
        }

        if (logManager.isLogVisible()) {
            logManager.scheduleInitialization();
        }
        else if (PostponableLogRefresher.keepUpToDate()) {
            VcsLogCachesInvalidator invalidator = myProject.getApplication().getExtensionPoint(CachesInvalidator.class)
                .findExtensionOrFail(VcsLogCachesInvalidator.class);
            if (invalidator.isValid()) {
                HeavyAwareExecutor.executeOutOfHeavyProcessLater(logManager::scheduleInitialization, 5000);
            }
        }
    }

    /**
     * Runs the action once the VCS mappings are initialized and the log exists.
     */
    @RequiredUIAccess
    public void runWhenLogIsReady(Consumer<VcsLogManager> action) {
        VcsLogManager logManager = getLogManager();
        if (logManager != null) {
            action.accept(logManager);
            return;
        }

        ProjectLevelVcsManagerEx.getInstanceEx(myProject).addInitializationRequest(
            VcsInitObject.AFTER_COMMON,
            () -> myProject.getUIAccess().give(() -> {
                if (myProject.isDisposed()) {
                    return;
                }

                VcsLogManager manager = getLogManager();
                if (manager == null && hasDvcsRoots()) {
                    createLog();
                    manager = getLogManager();
                }
                if (manager != null) {
                    action.accept(manager);
                }
            })
        );
    }

    @RequiredUIAccess
    @Override
    public @Nullable AbstractVcsLogUi openLogTab(VcsLogFilterCollection filters) {
        return openLogTab(filters, VcsLogTabLocation.TOOL_WINDOW);
    }

    @RequiredUIAccess
    public @Nullable AbstractVcsLogUi openLogTab(VcsLogFilterCollection filters, VcsLogTabLocation location) {
        myLogManager.getValue();
        VcsLogTabsManager tabsManager = myTabsManager;
        return tabsManager == null ? null : tabsManager.openAnotherLogTab(filters, location);
    }

    @RequiredUIAccess
    public void toolWindowShown(ToolWindow toolWindow) {
        VcsLogTabsManager tabsManager = myTabsManager;
        if (tabsManager != null) {
            tabsManager.toolWindowShown(toolWindow);
        }
    }

    public @Nullable VcsLogTabsManager getTabsManager() {
        return myTabsManager;
    }

    protected boolean hasDvcsRoots() {
        return !VcsLogManager.findLogProviders(getVcsRoots(), myProject).isEmpty();
    }

    public static VcsProjectLogImpl getInstance(Project project) {
        return (VcsProjectLogImpl) project.getInstance(VcsProjectLog.class);
    }

    private class LazyVcsLogManager {
        private @Nullable VcsLogManager myValue;

        
        @RequiredUIAccess
        public synchronized VcsLogManager getValue() {
            if (myValue == null) {
                VcsLogManager value = compute();
                myValue = value;
                VcsLogTabsManager tabsManager = new VcsLogTabsManager(myProject, myUiProperties, value);
                Disposer.register(value, tabsManager);
                myTabsManager = tabsManager;
                myMessageBus.syncPublisher(ProjectLogListener.class).logCreated();
            }
            return myValue;
        }

        
        @RequiredUIAccess
        protected synchronized VcsLogManager compute() {
            return new VcsLogManager(myProject, myUiProperties, getVcsRoots(), false, VcsProjectLogImpl.this::recreateOnError);
        }

        @RequiredUIAccess
        public synchronized void drop() {
            if (myValue != null) {
                VcsLogTabsManager tabsManager = myTabsManager;
                myTabsManager = null;
                if (tabsManager != null) {
                    Disposer.dispose(tabsManager);
                }
                myMessageBus.syncPublisher(ProjectLogListener.class).logDisposed();
                Disposer.dispose(myValue);
            }
            myValue = null;
        }

        public synchronized @Nullable VcsLogManager getCached() {
            return myValue;
        }
    }
}
