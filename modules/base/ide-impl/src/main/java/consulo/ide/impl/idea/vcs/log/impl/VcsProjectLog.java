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
package consulo.ide.impl.idea.vcs.log.impl;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.ApplicationManager;
import consulo.component.messagebus.MessageBus;
import consulo.disposer.Disposer;
import consulo.ide.ServiceManager;
import consulo.application.CachesInvalidator;
import consulo.ide.impl.idea.openapi.vcs.CalledInAny;
import consulo.ide.impl.idea.openapi.vcs.CalledInAwt;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.versionControlSystem.root.VcsRoot;
import consulo.ide.impl.idea.vcs.log.data.VcsLogDataImpl;
import consulo.ide.impl.idea.vcs.log.data.VcsLogTabsProperties;
import consulo.ide.impl.idea.vcs.log.ui.VcsLogPanel;
import consulo.ide.impl.idea.vcs.log.ui.VcsLogUiImpl;
import consulo.project.Project;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.util.Arrays;
import java.util.Collection;

@Singleton
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
public class VcsProjectLog {
    @Nonnull
    private final Project myProject;
    @Nonnull
    private final MessageBus myMessageBus;
    @Nonnull
    private final VcsLogTabsProperties myUiProperties;

    @Nonnull
    private final LazyVcsLogManager myLogManager = new LazyVcsLogManager();
    private volatile VcsLogUiImpl myUi;

    @Inject
    public VcsProjectLog(@Nonnull Project project, @Nonnull VcsLogTabsProperties uiProperties) {
        myProject = project;
        myMessageBus = project.getMessageBus();
        myUiProperties = uiProperties;
    }

    @Nullable
    public VcsLogDataImpl getDataManager() {
        VcsLogManager cached = myLogManager.getCached();
        if (cached == null) {
            return null;
        }
        return cached.getDataManager();
    }

    @Nonnull
    private Collection<VcsRoot> getVcsRoots() {
        return Arrays.asList(ProjectLevelVcsManager.getInstance(myProject).getAllVcsRoots());
    }

    @Nonnull
    public JComponent initMainLog(@Nonnull String contentTabName) {
        myUi = myLogManager.getValue().createLogUi(VcsLogTabsProperties.MAIN_LOG_ID, contentTabName, null);
        return new VcsLogPanel(myLogManager.getValue(), myUi);
    }

    /**
     * The instance of the {@link VcsLogUiImpl} or null if the log was not initialized yet.
     */
    @Nullable
    public VcsLogUiImpl getMainLogUi() {
        return myUi;
    }

    @Nullable
    public VcsLogManager getLogManager() {
        return myLogManager.getCached();
    }

    @CalledInAny
    protected void recreateLog() {
        ApplicationManager.getApplication().invokeLater(() -> {
            disposeLog();

            if (hasDvcsRoots()) {
                createLog();
            }
        });
    }

    @CalledInAwt
    private void disposeLog() {
        myUi = null;
        myLogManager.drop();
    }

    @RequiredUIAccess
    public void createLog() {
        VcsLogManager logManager = myLogManager.getValue();

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

    protected boolean hasDvcsRoots() {
        return !VcsLogManager.findLogProviders(getVcsRoots(), myProject).isEmpty();
    }

    public static VcsProjectLog getInstance(@Nonnull Project project) {
        return ServiceManager.getService(project, VcsProjectLog.class);
    }

    private class LazyVcsLogManager {
        @Nullable
        private VcsLogManager myValue;

        @Nonnull
        @RequiredUIAccess
        public synchronized VcsLogManager getValue() {
            if (myValue == null) {
                myValue = compute();
                myMessageBus.syncPublisher(ProjectLogListener.class).logCreated();
            }
            return myValue;
        }

        @Nonnull
        @RequiredUIAccess
        protected synchronized VcsLogManager compute() {
            return new VcsLogManager(myProject, myUiProperties, getVcsRoots(), false, VcsProjectLog.this::recreateLog);
        }

        @CalledInAwt
        public synchronized void drop() {
            if (myValue != null) {
                myMessageBus.syncPublisher(ProjectLogListener.class).logDisposed();
                Disposer.dispose(myValue);
            }
            myValue = null;
        }

        @Nullable
        public synchronized VcsLogManager getCached() {
            return myValue;
        }
    }
}
