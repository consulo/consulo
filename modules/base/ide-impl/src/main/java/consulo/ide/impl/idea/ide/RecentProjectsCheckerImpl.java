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
package consulo.ide.impl.idea.ide;

import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.application.PowerSaveMode;
import consulo.application.PowerSaveModeListener;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.platform.Platform;
import consulo.project.RecentProjectsBranchesProvider;
import consulo.project.internal.RecentProjectsChecker;
import consulo.project.internal.RecentProjectsManager;
import consulo.project.ui.wm.IdeFrame;
import consulo.project.ui.wm.event.ApplicationActivationListener;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Runtime checker for recent projects. Holds no persistent state: branch values are stored by
 * {@link RecentProjectsManager}; this service only re-reads validity and branch off the EDT and pushes the results back.
 */
@Singleton
@ServiceImpl
public class RecentProjectsCheckerImpl implements RecentProjectsChecker, ApplicationActivationListener, PowerSaveModeListener {
    private static final int MIN_AUTO_UPDATE_MILLIS = 2500;

    private final Application myApplication;
    private final RecentProjectsManager myRecentProjectsManager;

    private final Object myLock = new Object();
    private final Set<String> myInvalidPaths = new HashSet<>();
    private final Map<Runnable, Collection<String>> myCallbacks = new LinkedHashMap<>();
    private final Set<String> myScheduledPaths = new HashSet<>();

    private ScheduledExecutorService myService = null;

    @Inject
    public RecentProjectsCheckerImpl(Application application, RecentProjectsManager recentProjectsManager) {
        myApplication = application;
        myRecentProjectsManager = recentProjectsManager;
        application.getMessageBus().connect().subscribe(ApplicationActivationListener.class, this);
        application.getMessageBus().connect().subscribe(PowerSaveModeListener.class, this);
    }

    @Override
    public boolean isValid(String path) {
        synchronized (myLock) {
            return !myInvalidPaths.contains(path);
        }
    }

    @Nullable
    @Override
    public String getBranch(String path) {
        return myRecentProjectsManager.getBranch(path);
    }

    @Override
    public void addCallback(Runnable callback, Collection<String> paths) {
        synchronized (myLock) {
            myCallbacks.put(callback, new ArrayList<>(paths));
        }
        onAppStateChanged();
    }

    @Override
    public void removeCallback(Runnable callback) {
        synchronized (myLock) {
            myCallbacks.remove(callback);
        }
        onAppStateChanged();
    }

    @Override
    public void applicationActivated(IdeFrame ideFrame) {
        onAppStateChanged();
    }

    @Override
    public void delayedApplicationDeactivated(IdeFrame ideFrame) {
        onAppStateChanged();
    }

    @Override
    public void applicationDeactivated(IdeFrame ideFrame) {
    }

    @Override
    public void powerSaveStateChanged() {
        onAppStateChanged();
    }

    private boolean hasCallbacks() {
        synchronized (myLock) {
            return !myCallbacks.isEmpty();
        }
    }

    private Set<String> unionPaths() {
        Set<String> union = new HashSet<>();
        synchronized (myLock) {
            for (Collection<String> paths : myCallbacks.values()) {
                union.addAll(paths);
            }
        }
        return union;
    }

    private void onAppStateChanged() {
        boolean settingsAreOK = !PowerSaveMode.isEnabled();
        boolean shouldRun = settingsAreOK && myApplication.isActive() && hasCallbacks();

        if (shouldRun) {
            if (myService == null) {
                myService = AppExecutorUtil.createBoundedScheduledExecutorService("CheckRecentProjects Service", 2);
                fireCallbacks();
            }
            for (String path : unionPaths()) {
                if (myScheduledPaths.add(path)) {
                    scheduleCheck(path, 0);
                }
            }
        }
        else if (myService != null) {
            if (!settingsAreOK) {
                synchronized (myLock) {
                    myInvalidPaths.clear();
                }
            }
            if (!myService.isShutdown()) {
                myService.shutdown();
            }
            myService = null;
            myScheduledPaths.clear();
            fireCallbacks();
        }
    }

    private void scheduleCheck(String path, long delay) {
        ScheduledExecutorService service = myService;
        if (service == null || service.isShutdown()) {
            myScheduledPaths.remove(path);
            return;
        }

        service.schedule(() -> {
            long startTime = System.currentTimeMillis();

            boolean pathIsValid;
            try {
                pathIsValid = !RecentProjectsManagerImpl.isFileSystemPath(path) || isPathAvailable(path);
            }
            catch (Exception e) {
                pathIsValid = false;
            }

            String branch = myApplication.getExtensionPoint(RecentProjectsBranchesProvider.class)
                .computeSafeIfAny(provider -> provider.getCurrentBranch(path));

            boolean changed;
            synchronized (myLock) {
                boolean wasInvalid = myInvalidPaths.contains(path);
                boolean nowInvalid = !pathIsValid;
                changed = wasInvalid != nowInvalid;
                if (nowInvalid) {
                    myInvalidPaths.add(path);
                }
                else {
                    myInvalidPaths.remove(path);
                }
            }

            if (!Objects.equals(myRecentProjectsManager.getBranch(path), branch)) {
                myRecentProjectsManager.setBranch(path, branch);
                changed = true;
            }

            if (changed) {
                fireCallbacks();
            }

            if (myService != null && unionPaths().contains(path)) {
                scheduleCheck(path, Math.max(MIN_AUTO_UPDATE_MILLIS, 10 * (System.currentTimeMillis() - startTime)));
            }
            else {
                myScheduledPaths.remove(path);
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    private void fireCallbacks() {
        List<Runnable> snapshot;
        synchronized (myLock) {
            snapshot = new ArrayList<>(myCallbacks.keySet());
        }
        myApplication.invokeLater(() -> snapshot.forEach(Runnable::run));
    }

    private static boolean isPathAvailable(String pathStr) {
        Path path = Paths.get(pathStr), pathRoot = path.getRoot();
        if (pathRoot == null) {
            return false;
        }
        if (Platform.current().os().isWindows() && pathRoot.toString().startsWith("\\\\")) {
            return true;
        }
        for (Path fsRoot : pathRoot.getFileSystem().getRootDirectories()) {
            if (pathRoot.equals(fsRoot)) {
                return Files.exists(path);
            }
        }
        return false;
    }
}
