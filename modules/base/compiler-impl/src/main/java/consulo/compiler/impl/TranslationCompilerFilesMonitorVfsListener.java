/*
 * Copyright 2013-2019 consulo.io
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
package consulo.compiler.impl;

import com.intellij.compiler.impl.TranslatingCompilerFilesMonitorImpl;
import com.intellij.compiler.impl.TranslationOutputFileInfo;
import com.intellij.compiler.impl.TranslationSourceFileInfo;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.compiler.DummyCompileContext;
import com.intellij.openapi.compiler.TranslatingCompiler;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectCoreUtil;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.*;
import com.intellij.openapi.vfs.newvfs.NewVirtualFile;
import com.intellij.openapi.vfs.newvfs.events.*;
import consulo.compiler.server.BuildManager;
import consulo.logging.Logger;
import consulo.util.collection.Sets;
import consulo.util.collection.primitive.ints.IntList;
import consulo.util.collection.primitive.ints.IntLists;
import gnu.trove.TIntHashSet;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2019-10-26
 */
public class TranslationCompilerFilesMonitorVfsListener implements AsyncFileListener {
  private static final Logger LOG = Logger.getInstance(TranslationCompilerFilesMonitorVfsListener.class);

  private final Provider<TranslatingCompilerFilesMonitor> myMonitorProvider;
  private final Provider<ProjectManager> myProjectManagerProvider;

  @Inject
  TranslationCompilerFilesMonitorVfsListener(Provider<TranslatingCompilerFilesMonitor> monitorProvider, Provider<ProjectManager> projectManagerProvider) {
    myMonitorProvider = monitorProvider;
    myProjectManagerProvider = projectManagerProvider;
  }

  @Nullable
  @Override
  public ChangeApplier prepareChange(@Nonnull List<? extends VFileEvent> events) {
    List<VFileEvent> beforeEvents = new ArrayList<>();
    List<VFileEvent> afterEvents = new ArrayList<>();

    CompactVirtualFileSet newFilesSet = new CompactVirtualFileSet();

    for (VFileEvent event : events) {
      VirtualFile file = event.getFile();
      if (file == null || ProjectCoreUtil.isProjectOrWorkspaceFile(file)) {
        continue;
      }

      if (event instanceof VFileMoveEvent) {
        beforeEvents.add(event);

        newFilesSet.add(file);
      }
      else if (event instanceof VFileDeleteEvent) {
        beforeEvents.add(event);
      }
      else if (event instanceof VFileCreateEvent || event instanceof VFileCopyEvent) {
        newFilesSet.add(file);
      }
      else if (event instanceof VFilePropertyChangeEvent || event instanceof VFileContentChangeEvent) {
        afterEvents.add(event);
      }
    }

    if(beforeEvents.isEmpty() && afterEvents.isEmpty() && newFilesSet.isEmpty()) {
      return null;
    }

    return new ChangeApplier() {
      @Override
      public void beforeVfsChange() {
        for (VFileEvent event : beforeEvents) {
          if (event instanceof VFileMoveEvent) {
            markDirtyIfSource(event.getFile(), true);
          }
          else if (event instanceof VFileDeleteEvent) {
            beforeFileDeletion((VFileDeleteEvent)event);
          }
        }
      }

      @Override
      public void afterVfsChange() {
        newFilesSet.process(it -> {
          processNewFile(it, true);
          return true;
        });

        for (VFileEvent event : afterEvents) {
          if (event instanceof VFileContentChangeEvent) {
            contentsChanged((VFileContentChangeEvent)event);
          }
          else if (event instanceof VFilePropertyChangeEvent) {
            propertyChanged((VFilePropertyChangeEvent)event);
          }
        }
      }
    };
  }

  private void propertyChanged(@Nonnull final VFilePropertyChangeEvent event) {
    if (VirtualFile.PROP_NAME.equals(event.getPropertyName())) {
      final VirtualFile eventFile = event.getFile();
      VirtualFile file = event.getFile();
      VirtualFile parent = file.getParent();
      if (parent != null) {
        final String oldName = (String)event.getOldValue();
        final String root = parent.getPath() + "/" + oldName;
        final Set<File> toMark = Sets.newHashSet(FileUtil.FILE_HASHING_STRATEGY);
        if (eventFile.isDirectory()) {
          VfsUtilCore.visitChildrenRecursively(eventFile, new VirtualFileVisitor() {
            private StringBuilder filePath = new StringBuilder(root);

            @Override
            public boolean visitFile(@Nonnull VirtualFile child) {
              if (child.isDirectory()) {
                if (!Comparing.equal(child, eventFile)) {
                  filePath.append("/").append(child.getName());
                }
              }
              else {
                String childPath = filePath.toString();
                if (!Comparing.equal(child, eventFile)) {
                  childPath += "/" + child.getName();
                }
                toMark.add(new File(childPath));
              }
              return true;
            }

            @Override
            public void afterChildrenVisited(@Nonnull VirtualFile file) {
              if (file.isDirectory() && !Comparing.equal(file, eventFile)) {
                filePath.delete(filePath.length() - file.getName().length() - 1, filePath.length());
              }
            }
          });
        }
        else {
          toMark.add(new File(root));
        }
        notifyFilesDeleted(toMark);
      }
      markDirtyIfSource(eventFile, false);
    }
  }

  private void contentsChanged(@Nonnull final VFileContentChangeEvent event) {
    markDirtyIfSource(event.getFile(), false);
  }

  private void beforeFileDeletion(@Nonnull final VFileDeleteEvent event) {
    final VirtualFile eventFile = event.getFile();
    if (LOG.isDebugEnabled() && eventFile.isDirectory() || TranslatingCompilerFilesMonitorImpl.DEBUG_MODE) {
      final String message = "Processing file deletion: " + eventFile.getPresentableUrl();
      LOG.debug(message);
      if (TranslatingCompilerFilesMonitorImpl.DEBUG_MODE) {
        System.out.println(message);
      }
    }

    final Set<File> pathsToMark = Sets.newHashSet(FileUtil.FILE_HASHING_STRATEGY);

    TranslatingCompilerFilesMonitorImpl monitor = getMonitor();

    processRecursively(eventFile, true, new Consumer<VirtualFile>() {
      private final IntList myAssociatedProjectIds = IntLists.newArrayList();

      @Override
      public void accept(final VirtualFile file) {
        final String filePath = file.getPath();
        pathsToMark.add(new File(filePath));
        myAssociatedProjectIds.clear();
        try {
          final TranslationOutputFileInfo outputInfo = TranslationOutputFileInfo.loadOutputInfo(file);
          if (outputInfo != null) {
            final VirtualFile srcFile = outputInfo.getSourceFile();
            if (srcFile != null) {
              final TranslationSourceFileInfo srcInfo = TranslationSourceFileInfo.loadSourceInfo(srcFile);
              if (srcInfo != null) {
                final boolean srcWillBeDeleted = VfsUtil.isAncestor(eventFile, srcFile, false);
                for (int projectId : srcInfo.getProjectIds().toArray()) {
                  if (monitor.isSuspended(projectId)) {
                    continue;
                  }
                  if (srcInfo.isAssociated(projectId, filePath)) {
                    myAssociatedProjectIds.add(projectId);
                    if (srcWillBeDeleted) {
                      if (LOG.isDebugEnabled() || TranslatingCompilerFilesMonitorImpl.DEBUG_MODE) {
                        final String message = "Unschedule recompilation because of deletion " + srcFile.getPresentableUrl();
                        LOG.debug(message);
                        if (TranslatingCompilerFilesMonitorImpl.DEBUG_MODE) {
                          System.out.println(message);
                        }
                      }
                      monitor.removeSourceForRecompilation(projectId, Math.abs(TranslatingCompilerFilesMonitorImpl.getFileId(srcFile)));
                    }
                    else {
                      monitor.addSourceForRecompilation(projectId, srcFile, srcInfo);
                    }
                  }
                }
              }
            }
          }

          final TranslationSourceFileInfo srcInfo = TranslationSourceFileInfo.loadSourceInfo(file);
          if (srcInfo != null) {
            final TIntHashSet projects = srcInfo.getProjectIds();
            if (!projects.isEmpty()) {
              final TranslatingCompilerFilesMonitorImpl.ScheduleOutputsForDeletionProc deletionProc = monitor.new ScheduleOutputsForDeletionProc(file.getUrl());
              deletionProc.setRootBeingDeleted(eventFile);
              final int sourceFileId = Math.abs(TranslatingCompilerFilesMonitorImpl.getFileId(file));
              for (int projectId : projects.toArray()) {
                if (monitor.isSuspended(projectId)) {
                  continue;
                }
                if (srcInfo.isAssociated(projectId, filePath)) {
                  myAssociatedProjectIds.add(projectId);
                }
                // mark associated outputs for deletion
                srcInfo.processOutputPaths(projectId, deletionProc);
                if (LOG.isDebugEnabled() || TranslatingCompilerFilesMonitorImpl.DEBUG_MODE) {
                  final String message = "Unschedule recompilation because of deletion " + file.getPresentableUrl();
                  LOG.debug(message);
                  if (TranslatingCompilerFilesMonitorImpl.DEBUG_MODE) {
                    System.out.println(message);
                  }
                }
                monitor.removeSourceForRecompilation(projectId, sourceFileId);
              }
            }
          }
        }
        finally {
          // it is important that update of myOutputsToDelete is done at the end
          // otherwise the filePath of the file that is about to be deleted may be re-scheduled for deletion in addSourceForRecompilation()
          myAssociatedProjectIds.forEach(projectId -> {
            monitor.unmarkOutputPathForDeletion(projectId, filePath);
          });
        }
      }
    });

    notifyFilesDeleted(pathsToMark);
  }

  private void markDirtyIfSource(final VirtualFile file, final boolean fromMove) {
    TranslatingCompilerFilesMonitorImpl monitor = getMonitor();

    final Set<File> pathsToMark = Sets.newHashSet(FileUtil.FILE_HASHING_STRATEGY);
    processRecursively(file, false, file1 -> {
      pathsToMark.add(new File(file1.getPath()));
      final TranslationSourceFileInfo srcInfo = file1.isValid() ? TranslationSourceFileInfo.loadSourceInfo(file1) : null;
      if (srcInfo != null) {
        for (int projectId : srcInfo.getProjectIds().toArray()) {
          if (monitor.isSuspended(projectId)) {
            if (srcInfo.clearPaths(projectId)) {
              srcInfo.updateTimestamp(projectId, -1L);
              TranslationSourceFileInfo.saveSourceInfo(file1, srcInfo);
            }
          }
          else {
            monitor.addSourceForRecompilation(projectId, file1, srcInfo);
            // when the file is moved to a new location, we should 'forget' previous associations
            if (fromMove) {
              if (srcInfo.clearPaths(projectId)) {
                TranslationSourceFileInfo.saveSourceInfo(file1, srcInfo);
              }
            }
          }
        }
      }
      else {
        processNewFile(file1, false);
      }
    });
    if (fromMove) {
      notifyFilesDeleted(pathsToMark);
    }
    else if (!monitor.isIgnoredOrUnderIgnoredDirectory(getProjectManager(), file)) {
      notifyFilesChanged(pathsToMark);
    }
  }

  private void processNewFile(final VirtualFile file, final boolean notifyServer) {
    ProjectManager projectManager = getProjectManager();
    TranslatingCompilerFilesMonitorImpl monitor = getMonitor();

    final Ref<Boolean> isInContent = Ref.create(false);
    // need read action to ensure that the project was not disposed during the iteration over the project list
    ApplicationManager.getApplication().runReadAction(() -> {
      for (final Project project : projectManager.getOpenProjects()) {
        if (!project.isInitialized()) {
          continue; // the content of this project will be scanned during its post-startup activities
        }
        final int projectId = monitor.getProjectId(project);
        final boolean projectSuspended = monitor.isSuspended(projectId);
        final ProjectRootManager rootManager = ProjectRootManager.getInstance(project);
        ProjectFileIndex fileIndex = rootManager.getFileIndex();
        if (fileIndex.isInContent(file)) {
          isInContent.set(true);
        }

        if (fileIndex.isInSourceContent(file)) {
          final TranslatingCompiler[] translators = CompilerManager.getInstance(project).getCompilers(TranslatingCompiler.class);
          processRecursively(file, false, new Consumer<VirtualFile>() {
            @Override
            public void accept(final VirtualFile file1) {
              if (!projectSuspended && isCompilable(file1)) {
                monitor.loadInfoAndAddSourceForRecompilation(projectId, file1);
              }
            }

            boolean isCompilable(VirtualFile file1) {
              for (TranslatingCompiler translator : translators) {
                if (translator.isCompilableFile(file1, DummyCompileContext.getInstance())) {
                  return true;
                }
              }
              return false;
            }
          });
        }
        else {
          if (!projectSuspended && belongsToIntermediateSources(file, project)) {
            processRecursively(file, false, new Consumer<VirtualFile>() {
              @Override
              public void accept(final VirtualFile file1) {
                monitor.loadInfoAndAddSourceForRecompilation(projectId, file1);
              }
            });
          }
        }
      }
    });
    if (notifyServer && !monitor.isIgnoredOrUnderIgnoredDirectory(projectManager, file)) {
      final Set<File> pathsToMark = Sets.newHashSet(FileUtil.FILE_HASHING_STRATEGY);
      boolean dbOnly = !isInContent.get();
      processRecursively(file, dbOnly, it -> pathsToMark.add(new File(it.getPath())));
      notifyFilesChanged(pathsToMark);
    }
  }

  public static void processRecursively(VirtualFile file, final boolean dbOnly, final Consumer<VirtualFile> processor) {
    if (!(file.getFileSystem() instanceof LocalFileSystem)) {
      return;
    }

    final FileTypeManager fileTypeManager = FileTypeManager.getInstance();
    VfsUtilCore.visitChildrenRecursively(file, new VirtualFileVisitor() {
      @Nonnull
      @Override
      public Result visitFileEx(@Nonnull VirtualFile file) {
        if (fileTypeManager.isFileIgnored(file)) {
          return SKIP_CHILDREN;
        }

        if (!file.isDirectory()) {
          processor.accept(file);
        }
        return CONTINUE;
      }

      @Nullable
      @Override
      public Iterable<VirtualFile> getChildrenIterable(@Nonnull VirtualFile file) {
        return file.isDirectory() && dbOnly ? ((NewVirtualFile)file).iterInDbChildren() : null;
      }
    });
  }

  private static void notifyFilesChanged(Collection<File> paths) {
    if (!paths.isEmpty()) {
      BuildManager.getInstance().notifyFilesChanged(paths);
    }
  }

  private static void notifyFilesDeleted(Collection<File> paths) {
    if (!paths.isEmpty()) {
      BuildManager.getInstance().notifyFilesDeleted(paths);
    }
  }

  private boolean belongsToIntermediateSources(VirtualFile file, Project project) {
    return FileUtil.isAncestor(getMonitor().getGeneratedPath(project), new File(file.getPath()), true);
  }

  @Nonnull
  TranslatingCompilerFilesMonitorImpl getMonitor() {
    return (TranslatingCompilerFilesMonitorImpl)myMonitorProvider.get();
  }

  @Nonnull
  ProjectManager getProjectManager() {
    return myProjectManagerProvider.get();
  }
}
