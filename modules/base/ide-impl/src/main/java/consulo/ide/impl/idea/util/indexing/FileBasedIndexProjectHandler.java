/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
 * @author max
 */
package consulo.ide.impl.idea.util.indexing;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.impl.internal.performance.PerformanceWatcher;
import consulo.application.progress.ProgressIndicator;
import consulo.application.util.function.Processor;
import consulo.application.util.registry.Registry;
import consulo.content.ContentIterator;
import consulo.disposer.Disposable;
import consulo.ide.impl.idea.openapi.project.CacheUpdateRunner;
import consulo.ide.impl.idea.openapi.vfs.VfsUtilCore;
import consulo.ide.localize.IdeLocalize;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.language.psi.stub.IndexableFileSet;
import consulo.logging.Logger;
import consulo.module.content.PushedFilePropertiesUpdater;
import consulo.project.DumbModeTask;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.project.event.DumbModeListener;
import consulo.project.event.ProjectManagerListener;
import consulo.project.startup.StartupManager;
import consulo.util.io.FileUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileVisitor;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.Collection;

@Singleton
@ServiceAPI(value = ComponentScope.PROJECT, lazy = false)
@ServiceImpl
public class FileBasedIndexProjectHandler implements IndexableFileSet, Disposable {
  private static final Logger LOG = Logger.getInstance(FileBasedIndexProjectHandler.class);

  private final FileBasedIndex myIndex;
  private final FileBasedIndexScanRunnableCollector myCollector;

  @Inject
  public FileBasedIndexProjectHandler(@Nonnull Project project, FileBasedIndex index, FileBasedIndexScanRunnableCollector collector) {
    myIndex = index;
    myCollector = collector;

    if (project.isDefault()) {
      return;
    }

    if (project.getApplication().isInternal()) {
      project.getMessageBus().connect(this).subscribe(DumbModeListener.class, new DumbModeListener() {

        @Override
        public void exitDumbMode() {
          LOG.info("Has changed files: " + (createChangedFilesIndexingTask(project) != null) + "; project=" + project);
        }
      });
    }

    StartupManager startupManager = StartupManager.getInstance(project);
    startupManager.registerPreStartupActivity(() -> {
      PushedFilePropertiesUpdater.getInstance(project).initializeProperties();

      // schedule dumb mode start after the read action we're currently in
      if (FileBasedIndex.getInstance() instanceof FileBasedIndexImpl) {
        DumbService.getInstance(project).queueTask(new UnindexedFilesUpdater(project));
      }

      myIndex.registerIndexableSet(this, project);
      project.getMessageBus().connect(this).subscribe(ProjectManagerListener.class, new ProjectManagerListener() {
        private boolean removed;

        @Override
        public void projectClosing(@Nonnull Project eventProject) {
          if (eventProject == project && !removed) {
            removed = true;
            myIndex.removeIndexableSet(FileBasedIndexProjectHandler.this);
          }
        }
      });
    });
  }

  @Override
  public boolean isInSet(@Nonnull final VirtualFile file) {
    return myCollector.shouldCollect(file);
  }

  @Override
  public void iterateIndexableFilesIn(@Nonnull final VirtualFile file, @Nonnull final ContentIterator iterator) {
    VfsUtilCore.visitChildrenRecursively(file, new VirtualFileVisitor<Void>() {
      @Override
      public boolean visitFile(@Nonnull VirtualFile file) {

        if (!isInSet(file)) return false;
        iterator.processFile(file);

        return true;
      }
    });
  }

  @Override
  public void dispose() {
    // done mostly for tests. In real life this is no-op, because the set was removed on project closing
    myIndex.removeIndexableSet(this);
  }

  //@ApiStatus.Internal
  public static final int ourMinFilesToStartDumMode = Registry.intValue("ide.dumb.mode.minFilesToStart", 20);
  private static final int ourMinFilesSizeToStartDumMode = Registry.intValue("ide.dumb.mode.minFilesSizeToStart", 1048576);

  @Nullable
  public static DumbModeTask createChangedFilesIndexingTask(final Project project) {
    final FileBasedIndex i = FileBasedIndex.getInstance();
    if (!(i instanceof FileBasedIndexImpl) || !IndexInfrastructure.hasIndices()) {
      return null;
    }

    FileBasedIndexImpl index = (FileBasedIndexImpl)i;

    if (!mightHaveManyChangedFilesInProject(project, index)) {
      return null;
    }

    return new DumbModeTask(project.getComponent(FileBasedIndexProjectHandler.class)) {
      @Override
      public void performInDumbMode(@Nonnull ProgressIndicator indicator) {
        long start = System.currentTimeMillis();
        Collection<VirtualFile> files = index.getFilesToUpdate(project);
        long calcDuration = System.currentTimeMillis() - start;

        indicator.setIndeterminate(false);
        indicator.setTextValue(IdeLocalize.progressIndexingUpdating());

        LOG.info("Reindexing refreshed files: " + files.size() + " to update, calculated in " + calcDuration + "ms");
        if (!files.isEmpty()) {
          PerformanceWatcher.Snapshot snapshot = PerformanceWatcher.takeSnapshot();
          reindexRefreshedFiles(indicator, files, project, index);
          snapshot.logResponsivenessSinceCreation("Reindexing refreshed files");
        }
      }

      @Override
      public String toString() {
        StringBuilder sampleOfChangedFilePathsToBeIndexed = new StringBuilder();

        index.processChangedFiles(project, new Processor<>() {
          int filesInProjectToBeIndexed;
          final String projectBasePath = project.getBasePath();

          @Override
          public boolean process(VirtualFile file) {
            if (filesInProjectToBeIndexed != 0) sampleOfChangedFilePathsToBeIndexed.append(", ");

            String filePath = file.getPath();
            String loggedPath = projectBasePath != null ? FileUtil.getRelativePath(projectBasePath, filePath, '/') : null;
            if (loggedPath == null) {
              loggedPath = filePath;
            }
            else {
              loggedPath = "%project_path%/" + loggedPath;
            }
            sampleOfChangedFilePathsToBeIndexed.append(loggedPath);

            return ++filesInProjectToBeIndexed < ourMinFilesToStartDumMode;
          }
        });
        return super.toString() + " [" + project + ", " + sampleOfChangedFilePathsToBeIndexed + "]";
      }
    };
  }

  private static boolean mightHaveManyChangedFilesInProject(Project project, FileBasedIndexImpl index) {
    long start = System.currentTimeMillis();
    return !index.processChangedFiles(project, new Processor<>() {
      int filesInProjectToBeIndexed;
      long sizeOfFilesToBeIndexed;

      @Override
      public boolean process(VirtualFile file) {
        ++filesInProjectToBeIndexed;
        if (file.isValid() && !file.isDirectory()) sizeOfFilesToBeIndexed += file.getLength();
        return filesInProjectToBeIndexed < ourMinFilesToStartDumMode && sizeOfFilesToBeIndexed < ourMinFilesSizeToStartDumMode && System.currentTimeMillis() < start + 100;
      }
    });
  }

  private static void reindexRefreshedFiles(ProgressIndicator indicator, Collection<VirtualFile> files, final Project project, final FileBasedIndexImpl index) {
    CacheUpdateRunner.processFiles(indicator, files, project, content -> index.processRefreshedFile(project, content));
  }
}
