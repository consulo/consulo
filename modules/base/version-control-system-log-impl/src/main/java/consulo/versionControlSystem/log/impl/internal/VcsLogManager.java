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

import com.google.common.util.concurrent.ServiceManager;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ui.notification.NotificationType;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.collection.MultiMap;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.log.VcsLogFilter;
import consulo.versionControlSystem.log.VcsLogProvider;
import consulo.versionControlSystem.log.VcsLogRefresher;
import consulo.versionControlSystem.log.VcsLogStorage;
import consulo.versionControlSystem.log.impl.internal.data.MainVcsLogUiProperties;
import consulo.versionControlSystem.log.impl.internal.data.VcsLogDataImpl;
import consulo.versionControlSystem.log.impl.internal.data.VcsLogFiltererImpl;
import consulo.versionControlSystem.log.impl.internal.data.VcsLogTabsProperties;
import consulo.versionControlSystem.log.impl.internal.ui.VcsLogColorManagerImpl;
import consulo.versionControlSystem.log.impl.internal.ui.AbstractVcsLogUi;
import consulo.versionControlSystem.log.impl.internal.ui.UnifiedVcsLogUi;
import consulo.versionControlSystem.root.VcsRoot;
import consulo.versionControlSystem.ui.VcsBalloonProblemNotifier;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

public class VcsLogManager implements Disposable {
  private static final Logger LOG = Logger.getInstance(VcsLogManager.class);

  
  private final Project myProject;
  
  private final VcsLogTabsProperties myUiProperties;
  private final @Nullable BiConsumer<@Nullable Object, Throwable> myRecreateMainLogHandler;

  
  private final VcsLogDataImpl myLogData;
  
  private final VcsLogColorManagerImpl myColorManager;
  
  private final VcsLogTabsWatcher myTabsLogRefresher;
  
  private final PostponableLogRefresher myPostponableRefresher;
  private boolean myInitialized = false;
  private final Set<String> myLogUiIds = ConcurrentHashMap.newKeySet();

  public VcsLogManager(Project project, VcsLogTabsProperties uiProperties, Collection<VcsRoot> roots) {
    this(project, uiProperties, roots, true, null);
  }

  @RequiredUIAccess
  public VcsLogManager(
    Project project,
    VcsLogTabsProperties uiProperties,
    Collection<VcsRoot> roots,
    boolean scheduleRefreshImmediately,
    @Nullable BiConsumer<@Nullable Object, Throwable> recreateHandler
  ) {
    myProject = project;
    myUiProperties = uiProperties;
    myRecreateMainLogHandler = recreateHandler;

    Map<VirtualFile, VcsLogProvider> logProviders = findLogProviders(roots, myProject);
    myLogData = new VcsLogDataImpl(myProject, logProviders, new MyFatalErrorsHandler());
    myPostponableRefresher = new PostponableLogRefresher(myLogData);
    myTabsLogRefresher = new VcsLogTabsWatcher(myProject, myPostponableRefresher, myLogData);

    refreshLogOnVcsEvents(logProviders, myPostponableRefresher, myLogData);

    myColorManager = new VcsLogColorManagerImpl(logProviders.keySet());

    if (scheduleRefreshImmediately) {
      scheduleInitialization();
    }

    Disposer.register(project, this);
  }

  @RequiredUIAccess
  public void scheduleInitialization() {
    if (!myInitialized) {
      myInitialized = true;
      myLogData.initialize();
    }
  }

  @RequiredUIAccess
  public boolean isLogVisible() {
    return myPostponableRefresher.isLogVisible();
  }

  
  public VcsLogDataImpl getDataManager() {
    return myLogData;
  }

  
  @RequiredUIAccess
  public Component createLogPanel(String logId, @Nullable String contentTabName) {
    AbstractVcsLogUi ui = createLogUi(logId, contentTabName, null);
    return ui.createPanel(this);
  }

  @RequiredUIAccess
  public AbstractVcsLogUi createLogUi(String logId, @Nullable String contentTabName, @Nullable VcsLogFilter filter) {
    MainVcsLogUiProperties properties = myUiProperties.createProperties(logId);
    VcsLogFiltererImpl filterer = new VcsLogFiltererImpl(myProject, myLogData, properties.get(MainVcsLogUiProperties.BEK_SORT_TYPE));
    AbstractVcsLogUi ui = new UnifiedVcsLogUi(logId, myLogData, myProject, myColorManager, properties, filterer);
    myLogUiIds.add(logId);
    Disposer.register(ui, () -> myLogUiIds.remove(logId));
    if (filter != null) {
      ui.getFilterUi().setFilter(filter);
    }

    Disposable disposable;
    if (contentTabName != null) {
      disposable = myTabsLogRefresher.addTabToWatch(contentTabName, ui.getFilterer());
    }
    else {
      disposable = myPostponableRefresher.addLogWindow(ui.getFilterer());
    }
    Disposer.register(ui, disposable);

    ui.requestFocus();
    return ui;
  }

  public boolean hasLogUi(String logId) {
    return myLogUiIds.contains(logId);
  }

  public String generateNewLogId() {
    String newId;
    do {
      newId = UUID.randomUUID().toString();
    }
    while (myLogUiIds.contains(newId));
    return newId;
  }

  private static void refreshLogOnVcsEvents(Map<VirtualFile, VcsLogProvider> logProviders,
                                            VcsLogRefresher refresher,
                                            Disposable disposableParent) {
    MultiMap<VcsLogProvider, VirtualFile> providers2roots = MultiMap.create();
    for (Map.Entry<VirtualFile, VcsLogProvider> entry : logProviders.entrySet()) {
      providers2roots.putValue(entry.getValue(), entry.getKey());
    }

    for (Map.Entry<VcsLogProvider, Collection<VirtualFile>> entry : providers2roots.entrySet()) {
      Disposable disposable = entry.getKey().subscribeToRootRefreshEvents(entry.getValue(), refresher);
      Disposer.register(disposableParent, disposable);
    }
  }

  
  public static Map<VirtualFile, VcsLogProvider> findLogProviders(Collection<VcsRoot> roots, Project project) {
    Map<VirtualFile, VcsLogProvider> logProviders = new HashMap<>();
    List<VcsLogProvider> allLogProviders = project.getExtensionPoint(VcsLogProvider.class).getExtensionList();
    for (VcsRoot root : roots) {
      AbstractVcs vcs = root.getVcs();
      VirtualFile path = root.getPath();
      if (vcs == null || path == null) {
        LOG.error("Skipping invalid VCS root: " + root);
        continue;
      }

      for (VcsLogProvider provider : allLogProviders) {
        if (provider.getSupportedVcs().equals(vcs.getKeyInstanceMethod())) {
          logProviders.put(path, provider);
          break;
        }
      }
    }
    return logProviders;
  }

  public void disposeLog() {
    Disposer.dispose(myLogData);
  }

  @Override
  public void dispose() {
    disposeLog();
  }

  private class MyFatalErrorsHandler implements FatalErrorHandler {
    private final AtomicBoolean myIsBroken = new AtomicBoolean(false);

    @Override
    public void consume(@Nullable Object source, Throwable e) {
      if (myIsBroken.compareAndSet(false, true)) {
        processError(source, e);
      }
      else {
        LOG.debug(e);
      }
    }

    protected void processError(@Nullable Object source, Throwable e) {
      if (myRecreateMainLogHandler != null) {
        myProject.getApplication().invokeLater(() -> {
          String message = "Fatal error, VCS Log re-created: " + e.getMessage();
          if (isLogVisible()) {
            LOG.info(e);
            displayFatalErrorMessage(message);
          }
          else {
            LOG.error(message, e);
          }
          myRecreateMainLogHandler.accept(source, e);
        });
      }
      else {
        LOG.error(e);
      }

      if (source instanceof VcsLogStorage) {
        myLogData.getIndex().markCorrupted();
      }
    }

    @Override
    public void displayFatalErrorMessage(String message) {
      VcsBalloonProblemNotifier.showOverChangesView(myProject, message, NotificationType.ERROR);
    }
  }
}
