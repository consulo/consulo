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

import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ide.ServiceManager;
import consulo.ide.impl.idea.openapi.vcs.CalledInAwt;
import consulo.ide.impl.idea.vcs.log.data.*;
import consulo.ide.impl.idea.vcs.log.ui.VcsLogColorManagerImpl;
import consulo.ide.impl.idea.vcs.log.ui.VcsLogPanel;
import consulo.ide.impl.idea.vcs.log.ui.VcsLogUiImpl;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ui.notification.NotificationType;
import consulo.util.collection.MultiMap;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.log.VcsLogFilter;
import consulo.versionControlSystem.log.VcsLogProvider;
import consulo.versionControlSystem.log.VcsLogRefresher;
import consulo.versionControlSystem.root.VcsRoot;
import consulo.versionControlSystem.ui.VcsBalloonProblemNotifier;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class VcsLogManager implements Disposable {
  private static final Logger LOG = Logger.getInstance(VcsLogManager.class);

  @Nonnull
  private final Project myProject;
  @Nonnull
  private final VcsLogTabsProperties myUiProperties;
  @Nullable private final Runnable myRecreateMainLogHandler;

  @Nonnull
  private final VcsLogDataImpl myLogData;
  @Nonnull
  private final VcsLogColorManagerImpl myColorManager;
  @Nonnull
  private final VcsLogTabsWatcher myTabsLogRefresher;
  @Nonnull
  private final PostponableLogRefresher myPostponableRefresher;
  private boolean myInitialized = false;

  public VcsLogManager(@Nonnull Project project, @Nonnull VcsLogTabsProperties uiProperties, @Nonnull Collection<VcsRoot> roots) {
    this(project, uiProperties, roots, true, null);
  }

  public VcsLogManager(
    @Nonnull Project project,
    @Nonnull VcsLogTabsProperties uiProperties,
    @Nonnull Collection<VcsRoot> roots,
    boolean scheduleRefreshImmediately,
    @Nullable Runnable recreateHandler
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

  @CalledInAwt
  public void scheduleInitialization() {
    if (!myInitialized) {
      myInitialized = true;
      myLogData.initialize();
    }
  }

  @CalledInAwt
  public boolean isLogVisible() {
    return myPostponableRefresher.isLogVisible();
  }

  @Nonnull
  public VcsLogDataImpl getDataManager() {
    return myLogData;
  }

  @Nonnull
  public JComponent createLogPanel(@Nonnull String logId, @Nullable String contentTabName) {
    VcsLogUiImpl ui = createLogUi(logId, contentTabName, null);
    return new VcsLogPanel(this, ui);
  }

  @Nonnull
  public VcsLogUiImpl createLogUi(@Nonnull String logId, @Nullable String contentTabName, @Nullable VcsLogFilter filter) {
    MainVcsLogUiProperties properties = myUiProperties.createProperties(logId);
    VcsLogFiltererImpl filterer = new VcsLogFiltererImpl(myProject, myLogData, properties.get(MainVcsLogUiProperties.BEK_SORT_TYPE));
    VcsLogUiImpl ui = new VcsLogUiImpl(myLogData, myProject, myColorManager, properties, filterer);
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

  private static void refreshLogOnVcsEvents(@Nonnull Map<VirtualFile, VcsLogProvider> logProviders,
                                            @Nonnull VcsLogRefresher refresher,
                                            @Nonnull Disposable disposableParent) {
    MultiMap<VcsLogProvider, VirtualFile> providers2roots = MultiMap.create();
    for (Map.Entry<VirtualFile, VcsLogProvider> entry : logProviders.entrySet()) {
      providers2roots.putValue(entry.getValue(), entry.getKey());
    }

    for (Map.Entry<VcsLogProvider, Collection<VirtualFile>> entry : providers2roots.entrySet()) {
      Disposable disposable = entry.getKey().subscribeToRootRefreshEvents(entry.getValue(), refresher);
      Disposer.register(disposableParent, disposable);
    }
  }

  @Nonnull
  public static Map<VirtualFile, VcsLogProvider> findLogProviders(@Nonnull Collection<VcsRoot> roots, @Nonnull Project project) {
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

  /*
   * Use VcsLogProjectManager to get main log.
   * Left here for upsource plugin.
   * */
  @Nullable
  @Deprecated
  public static VcsLogManager getInstance(@Nonnull Project project) {
    return ServiceManager.getService(project, VcsProjectLog.class).getLogManager();
  }

  /*
   * Use VcsLogProjectManager.getMainLogUi to get main log ui.
   * Left here for upsource plugin.
   * */
  @Nullable
  @Deprecated
  public VcsLogUiImpl getMainLogUi() {
    return ServiceManager.getService(myProject, VcsProjectLog.class).getMainLogUi();
  }

  @Override
  public void dispose() {
    disposeLog();
  }

  private class MyFatalErrorsHandler implements FatalErrorHandler {
    private final AtomicBoolean myIsBroken = new AtomicBoolean(false);

    @Override
    public void consume(@Nullable Object source, @Nonnull final Throwable e) {
      if (myIsBroken.compareAndSet(false, true)) {
        processError(source, e);
      }
      else {
        LOG.debug(e);
      }
    }

    protected void processError(@Nullable Object source, @Nonnull Throwable e) {
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
          myRecreateMainLogHandler.run();
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
    public void displayFatalErrorMessage(@Nonnull String message) {
      VcsBalloonProblemNotifier.showOverChangesView(myProject, message, NotificationType.ERROR);
    }
  }
}
