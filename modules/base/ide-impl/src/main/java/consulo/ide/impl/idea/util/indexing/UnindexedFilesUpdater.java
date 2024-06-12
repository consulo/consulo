// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.util.indexing;

import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.application.impl.internal.performance.PerformanceWatcher;
import consulo.application.progress.ProgressIndicator;
import consulo.component.ProcessCanceledException;
import consulo.component.messagebus.MessageBusConnection;
import consulo.content.CollectingContentIterator;
import consulo.ide.impl.idea.openapi.project.CacheUpdateRunner;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.logging.Logger;
import consulo.module.content.PushedFilePropertiesUpdater;
import consulo.module.content.internal.ProjectRootManagerEx;
import consulo.module.content.layer.event.ModuleRootEvent;
import consulo.module.content.layer.event.ModuleRootListener;
import consulo.platform.base.localize.IdeLocalize;
import consulo.project.DumbModeTask;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.project.event.ProjectManagerListener;
import consulo.ui.UIAccess;
import consulo.virtualFileSystem.RefreshQueue;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * @author Eugene Zhuravlev
 */
public class UnindexedFilesUpdater extends DumbModeTask {
  private static final Logger LOG = Logger.getInstance(UnindexedFilesUpdater.class);

  private final FileBasedIndexImpl myIndex = (FileBasedIndexImpl)FileBasedIndex.getInstance();
  private final Project myProject;
  private final PushedFilePropertiesUpdater myPusher;

  public UnindexedFilesUpdater(final Project project) {
    myProject = project;
    myPusher = PushedFilePropertiesUpdater.getInstance(myProject);
    project.getMessageBus().connect(this).subscribe(ModuleRootListener.class, new ModuleRootListener() {
      @Override
      public void rootsChanged(@Nonnull ModuleRootEvent event) {
        DumbService.getInstance(project).cancelTask(UnindexedFilesUpdater.this);
      }
    });
  }

  private void updateUnindexedFiles(ProgressIndicator indicator) {
    if (!IndexInfrastructure.hasIndices()) return;

    PerformanceWatcher.Snapshot snapshot = PerformanceWatcher.takeSnapshot();
    myPusher.pushAllPropertiesNow();
    boolean trackResponsiveness = !ApplicationManager.getApplication().isUnitTestMode();

    if (trackResponsiveness) snapshot.logResponsivenessSinceCreation("Pushing properties");

    indicator.setIndeterminate(true);
    indicator.setTextValue(IdeLocalize.progressIndexingScanning());

    myIndex.clearIndicesIfNecessary();

    CollectingContentIterator finder = myIndex.createContentIterator();
    snapshot = PerformanceWatcher.takeSnapshot();

    myIndex.iterateIndexableFilesConcurrently(finder, myProject, indicator);

    if (trackResponsiveness) snapshot.logResponsivenessSinceCreation("Indexable file iteration");

    List<VirtualFile> files = finder.getFiles();

    if (!ApplicationManager.getApplication().isUnitTestMode()) {
      // full VFS refresh makes sense only after it's loaded, i.e. after scanning files to index is finished
      scheduleInitialVfsRefresh();
    }

    if (files.isEmpty()) {
      return;
    }

    snapshot = PerformanceWatcher.takeSnapshot();

    if (trackResponsiveness) LOG.info("Unindexed files update started: " + files.size() + " files to update");

    indicator.setIndeterminate(false);
    indicator.setTextValue(IdeLocalize.progressIndexingUpdating());

    indexFiles(indicator, files);

    if (trackResponsiveness) snapshot.logResponsivenessSinceCreation("Unindexed files update");
  }

  private void scheduleInitialVfsRefresh() {
    ProjectRootManagerEx.getInstanceEx(myProject).markRootsForRefresh();

    Application app = ApplicationManager.getApplication();
    if (!app.isCommandLine()) {
      long sessionId = VirtualFileManager.getInstance().asyncRefresh(null);
      MessageBusConnection connection = app.getMessageBus().connect();
      connection.subscribe(ProjectManagerListener.class, new ProjectManagerListener() {
        @Override
        public void projectClosed(@Nonnull Project project, @Nonnull UIAccess uiAccess) {
          if (project == myProject) {
            RefreshQueue.getInstance().cancelSession(sessionId);
            connection.disconnect();
          }
        }
      });
    }
    else {
      VirtualFileManager.getInstance().syncRefresh();
    }
  }

  private void indexFiles(ProgressIndicator indicator, List<VirtualFile> files) {
    CacheUpdateRunner.processFiles(indicator, files, myProject, content -> myIndex.indexFileContent(myProject, content));
  }

  @Override
  public void performInDumbMode(@Nonnull ProgressIndicator indicator) {
    myIndex.filesUpdateStarted(myProject);
    try {
      updateUnindexedFiles(indicator);
    }
    catch (ProcessCanceledException e) {
      LOG.info("Unindexed files update canceled");
      throw e;
    }
    finally {
      myIndex.filesUpdateFinished(myProject);
    }
  }
}