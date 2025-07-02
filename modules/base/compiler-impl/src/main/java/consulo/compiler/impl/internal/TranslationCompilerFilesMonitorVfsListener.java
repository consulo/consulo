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
package consulo.compiler.impl.internal;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.compiler.CompilerManager;
import consulo.compiler.TranslatingCompiler;
import consulo.compiler.TranslatingCompilerFilesMonitor;
import consulo.language.file.FileTypeManager;
import consulo.logging.Logger;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.ProjectRootManager;
import consulo.project.Project;
import consulo.project.ProjectCoreUtil;
import consulo.project.ProjectManager;
import consulo.util.collection.Sets;
import consulo.util.collection.primitive.ints.IntList;
import consulo.util.collection.primitive.ints.IntLists;
import consulo.util.io.FileUtil;
import consulo.util.lang.Comparing;
import consulo.util.lang.ref.SimpleReference;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.NewVirtualFile;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.event.*;
import consulo.virtualFileSystem.internal.CompactVirtualFileSet;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import consulo.virtualFileSystem.util.VirtualFileVisitor;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * @author VISTALL
 * @since 2019-10-26
 */
@ExtensionImpl
public class TranslationCompilerFilesMonitorVfsListener implements AsyncFileListener {
    private static final Logger LOG = Logger.getInstance(TranslationCompilerFilesMonitorVfsListener.class);

    private final Provider<TranslatingCompilerFilesMonitor> myMonitorProvider;
    private final Provider<ProjectManager> myProjectManagerProvider;

    @Inject
    TranslationCompilerFilesMonitorVfsListener(
        Provider<TranslatingCompilerFilesMonitor> monitorProvider,
        Provider<ProjectManager> projectManagerProvider
    ) {
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

        if (beforeEvents.isEmpty() && afterEvents.isEmpty() && newFilesSet.isEmpty()) {
            return null;
        }

        return new ChangeApplier() {
            @Override
            public void beforeVfsChange() {
                for (VFileEvent event : beforeEvents) {
                    if (event instanceof VFileMoveEvent) {
                        markDirtyIfSource(event.getFile(), true);
                    }
                    else if (event instanceof VFileDeleteEvent deleteEvent) {
                        beforeFileDeletion(deleteEvent);
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
                    if (event instanceof VFileContentChangeEvent contentChangeEvent) {
                        contentsChanged(contentChangeEvent);
                    }
                    else if (event instanceof VFilePropertyChangeEvent propertyChangeEvent) {
                        propertyChanged(propertyChangeEvent);
                    }
                }
            }
        };
    }

    private void propertyChanged(@Nonnull VFilePropertyChangeEvent event) {
        if (VirtualFile.PROP_NAME.equals(event.getPropertyName())) {
            final VirtualFile eventFile = event.getFile();
            VirtualFile file = event.getFile();
            VirtualFile parent = file.getParent();
            if (parent != null) {
                String oldName = (String) event.getOldValue();
                final String root = parent.getPath() + "/" + oldName;
                final Set<File> toMark = Sets.newHashSet(FileUtil.FILE_HASHING_STRATEGY);
                if (eventFile.isDirectory()) {
                    VirtualFileUtil.visitChildrenRecursively(eventFile, new VirtualFileVisitor() {
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

    private void contentsChanged(@Nonnull VFileContentChangeEvent event) {
        markDirtyIfSource(event.getFile(), false);
    }

    private void beforeFileDeletion(@Nonnull VFileDeleteEvent event) {
        final VirtualFile eventFile = event.getFile();
        if (LOG.isDebugEnabled() && eventFile.isDirectory() || TranslatingCompilerFilesMonitorImpl.DEBUG_MODE) {
            String message = "Processing file deletion: " + eventFile.getPresentableUrl();
            LOG.debug(message);
            if (TranslatingCompilerFilesMonitorImpl.DEBUG_MODE) {
                System.out.println(message);
            }
        }

        final Set<File> pathsToMark = Sets.newHashSet(FileUtil.FILE_HASHING_STRATEGY);

        TranslatingCompilerFilesMonitorImpl monitor = getMonitor();

        processRecursively(
            eventFile,
            true,
            new Consumer<>() {
                private final IntList myAssociatedProjectIds = IntLists.newArrayList();

                @Override
                public void accept(VirtualFile file) {
                    String filePath = file.getPath();
                    pathsToMark.add(new File(filePath));
                    myAssociatedProjectIds.clear();
                    try {
                        TranslationOutputFileInfo outputInfo = TranslationOutputFileInfo.loadOutputInfo(file);
                        if (outputInfo != null) {
                            VirtualFile srcFile = outputInfo.getSourceFile();
                            if (srcFile != null) {
                                TranslationSourceFileInfo srcInfo = TranslationSourceFileInfo.loadSourceInfo(srcFile);
                                if (srcInfo != null) {
                                    boolean srcWillBeDeleted = VirtualFileUtil.isAncestor(eventFile, srcFile, false);
                                    for (Integer projectId : srcInfo.getProjectIds().toArray(Integer[]::new)) {
                                        if (monitor.isSuspended(projectId)) {
                                            continue;
                                        }
                                        if (srcInfo.isAssociated(projectId, filePath)) {
                                            myAssociatedProjectIds.add(projectId);
                                            if (srcWillBeDeleted) {
                                                if (LOG.isDebugEnabled() || TranslatingCompilerFilesMonitorImpl.DEBUG_MODE) {
                                                    String message =
                                                        "Unschedule recompilation because of deletion " + srcFile.getPresentableUrl();
                                                    LOG.debug(message);
                                                    if (TranslatingCompilerFilesMonitorImpl.DEBUG_MODE) {
                                                        System.out.println(message);
                                                    }
                                                }
                                                monitor.removeSourceForRecompilation(
                                                    projectId,
                                                    Math.abs(TranslatingCompilerFilesMonitorImpl.getFileId(srcFile))
                                                );
                                            }
                                            else {
                                                monitor.addSourceForRecompilation(projectId, srcFile, srcInfo);
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        TranslationSourceFileInfo srcInfo = TranslationSourceFileInfo.loadSourceInfo(file);
                        if (srcInfo != null) {
                            Set<Integer> projects = srcInfo.getProjectIds();
                            if (!projects.isEmpty()) {
                                TranslatingCompilerFilesMonitorImpl.ScheduleOutputsForDeletionProc deletionProc =
                                    monitor.new ScheduleOutputsForDeletionProc(file.getUrl());
                                deletionProc.setRootBeingDeleted(eventFile);
                                int sourceFileId = Math.abs(TranslatingCompilerFilesMonitorImpl.getFileId(file));
                                for (Integer projectId : projects.toArray(Integer[]::new)) {
                                    if (monitor.isSuspended(projectId)) {
                                        continue;
                                    }
                                    if (srcInfo.isAssociated(projectId, filePath)) {
                                        myAssociatedProjectIds.add(projectId);
                                    }
                                    // mark associated outputs for deletion
                                    srcInfo.processOutputPaths(projectId, deletionProc);
                                    if (LOG.isDebugEnabled() || TranslatingCompilerFilesMonitorImpl.DEBUG_MODE) {
                                        String message = "Unschedule recompilation because of deletion " + file.getPresentableUrl();
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
                        // otherwise the filePath of the file that is about to be deleted may be re-scheduled for deletion
                        // in addSourceForRecompilation()
                        myAssociatedProjectIds.forEach(projectId -> monitor.unmarkOutputPathForDeletion(projectId, filePath));
                    }
                }
            }
        );

        notifyFilesDeleted(pathsToMark);
    }

    private void markDirtyIfSource(VirtualFile file, boolean fromMove) {
        TranslatingCompilerFilesMonitorImpl monitor = getMonitor();

        Set<File> pathsToMark = Sets.newHashSet(FileUtil.FILE_HASHING_STRATEGY);
        processRecursively(
            file,
            false,
            thisFile -> {
                pathsToMark.add(new File(thisFile.getPath()));
                TranslationSourceFileInfo srcInfo = thisFile.isValid() ? TranslationSourceFileInfo.loadSourceInfo(thisFile) : null;
                if (srcInfo != null) {
                    for (Integer projectId : srcInfo.getProjectIds().toArray(Integer[]::new)) {
                        if (monitor.isSuspended(projectId)) {
                            if (srcInfo.clearPaths(projectId)) {
                                srcInfo.updateTimestamp(projectId, -1L);
                                TranslationSourceFileInfo.saveSourceInfo(thisFile, srcInfo);
                            }
                        }
                        else {
                            monitor.addSourceForRecompilation(projectId, thisFile, srcInfo);
                            // when the file is moved to a new location, we should 'forget' previous associations
                            if (fromMove) {
                                if (srcInfo.clearPaths(projectId)) {
                                    TranslationSourceFileInfo.saveSourceInfo(thisFile, srcInfo);
                                }
                            }
                        }
                    }
                }
                else {
                    processNewFile(thisFile, false);
                }
            }
        );
        if (fromMove) {
            notifyFilesDeleted(pathsToMark);
        }
        else if (!monitor.isIgnoredOrUnderIgnoredDirectory(getProjectManager(), file)) {
            notifyFilesChanged(pathsToMark);
        }
    }

    private void processNewFile(VirtualFile file, boolean notifyServer) {
        ProjectManager projectManager = getProjectManager();
        TranslatingCompilerFilesMonitorImpl monitor = getMonitor();

        SimpleReference<Boolean> isInContent = SimpleReference.create(false);
        // need read action to ensure that the project was not disposed during the iteration over the project list
        Application.get().runReadAction(() -> {
            for (Project project : projectManager.getOpenProjects()) {
                if (!project.isInitialized()) {
                    continue; // the content of this project will be scanned during its post-startup activities
                }
                int projectId = monitor.getProjectId(project);
                boolean projectSuspended = monitor.isSuspended(projectId);
                ProjectRootManager rootManager = ProjectRootManager.getInstance(project);
                ProjectFileIndex fileIndex = rootManager.getFileIndex();
                if (fileIndex.isInContent(file)) {
                    isInContent.set(true);
                }

                if (fileIndex.isInSourceContent(file)) {
                    TranslatingCompiler[] translators = CompilerManager.getInstance(project).getCompilers(TranslatingCompiler.class);
                    Predicate<VirtualFile> isCompilable = thisFile -> {
                        for (TranslatingCompiler translator : translators) {
                            if (translator.isCompilableFile(thisFile, DummyCompileContext.getInstance())) {
                                return true;
                            }
                        }
                        return false;
                    };
                    processRecursively(
                        file,
                        false,
                        thisFile -> {
                            if (!projectSuspended && isCompilable.test(thisFile)) {
                                monitor.loadInfoAndAddSourceForRecompilation(projectId, thisFile);
                            }
                        }
                    );
                }
                else if (!projectSuspended && belongsToIntermediateSources(file, project)) {
                    processRecursively(file, false, thisFile -> monitor.loadInfoAndAddSourceForRecompilation(projectId, thisFile));
                }
            }
        });
        if (notifyServer && !monitor.isIgnoredOrUnderIgnoredDirectory(projectManager, file)) {
            Set<File> pathsToMark = Sets.newHashSet(FileUtil.FILE_HASHING_STRATEGY);
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
        VirtualFileUtil.visitChildrenRecursively(
            file,
            new VirtualFileVisitor() {
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
                    return file.isDirectory() && dbOnly ? ((NewVirtualFile) file).iterInDbChildren() : null;
                }
            }
        );
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
        return (TranslatingCompilerFilesMonitorImpl) myMonitorProvider.get();
    }

    @Nonnull
    ProjectManager getProjectManager() {
        return myProjectManagerProvider.get();
    }
}
