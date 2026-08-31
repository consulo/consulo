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
package consulo.compiler.impl.internal;

import consulo.annotation.component.ExtensionImpl;
import consulo.compiler.CompilerManager;
import consulo.compiler.TranslatingCompiler;
import consulo.compiler.TranslatingCompilerFilesMonitor;
import consulo.compiler.impl.internal.state.OutputSourceInfo;
import consulo.compiler.impl.internal.state.ProjectCompilerState;
import consulo.language.file.FileTypeManager;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.ProjectRootManager;
import consulo.project.Project;
import consulo.project.ProjectCoreUtil;
import consulo.project.ProjectManager;
import consulo.util.io.FileUtil;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.NewVirtualFile;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.event.*;
import consulo.virtualFileSystem.internal.CompactVirtualFileSet;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import consulo.virtualFileSystem.util.VirtualFileVisitor;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * @author VISTALL
 * @since 2019-10-26
 */
@ExtensionImpl
public class TranslationCompilerFilesMonitorVfsListener implements AsyncFileListener {
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

    @Override
    public @Nullable ChangeApplier prepareChange(List<? extends VFileEvent> events) {
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
                        beforeFileMovedOrDeleted(event.getFile(), false);
                    }
                    else if (event instanceof VFileDeleteEvent deleteEvent) {
                        beforeFileMovedOrDeleted(deleteEvent.getFile(), true);
                    }
                }
            }

            @Override
            public void afterVfsChange() {
                newFilesSet.process(it -> {
                    processNewFile(it);
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

    private void propertyChanged(VFilePropertyChangeEvent event) {
        if (VirtualFile.PROP_NAME.equals(event.getPropertyName())) {
            VirtualFile eventFile = event.getFile();
            VirtualFile parent = eventFile.getParent();
            if (parent != null) {
                String oldName = (String) event.getOldValue();
                String oldRootPath = TranslatingCompilerFilesMonitorImpl.normalizePath(parent.getPath() + "/" + oldName);
                forgetPathsUnder(oldRootPath, eventFile.isDirectory());
            }
            markDirtyIfSource(eventFile);
        }
    }

    private void contentsChanged(VFileContentChangeEvent event) {
        markDirtyIfSource(event.getFile());
    }

    private void beforeFileMovedOrDeleted(VirtualFile eventFile, boolean deletion) {
        TranslatingCompilerFilesMonitorImpl monitor = getMonitor();
        ProjectManager projectManager = getProjectManager();
        String rootPath = TranslatingCompilerFilesMonitorImpl.pathOf(eventFile);

        List<ProjectCompilerState> states = new ArrayList<>();
        for (Project project : projectManager.getOpenProjects()) {
            if (!project.isInitialized() || monitor.isSuspended(project)) {
                continue;
            }
            states.add(ProjectCompilerState.getInstance(project));
        }
        if (states.isEmpty()) {
            return;
        }

        Predicate<String> underEventRoot = path -> FileUtil.isAncestor(rootPath, path, false);
        Predicate<String> skipOutputDeletion = deletion ? underEventRoot : null;

        processRecursively(
            eventFile,
            deletion,
            file -> {
                String path = TranslatingCompilerFilesMonitorImpl.pathOf(file);
                for (ProjectCompilerState state : states) {
                    OutputSourceInfo outputInfo = state.getOutputInfo(path);
                    if (outputInfo != null) {
                        String sourcePath = outputInfo.sourcePath();
                        if (underEventRoot.test(sourcePath)) {
                            state.unmarkDirty(sourcePath);
                        }
                        else {
                            state.markSourceDirty(sourcePath);
                        }
                        state.removeOutputInfo(path);
                    }

                    if (state.isKnownSource(path)) {
                        state.removeSource(path, skipOutputDeletion);
                    }
                    // the path itself is going away - it must not stay in the deletion queue
                    state.unscheduleOutputDeletion(path);
                }
            }
        );
    }

    private void forgetPathsUnder(String oldRootPath, boolean directory) {
        TranslatingCompilerFilesMonitorImpl monitor = getMonitor();
        for (Project project : getProjectManager().getOpenProjects()) {
            if (!project.isInitialized() || monitor.isSuspended(project)) {
                continue;
            }
            ProjectCompilerState state = ProjectCompilerState.getInstance(project);
            if (directory) {
                List<String> orphaned = new ArrayList<>();
                state.processSourcePaths(path -> {
                    if (FileUtil.isAncestor(oldRootPath, path, false)) {
                        orphaned.add(path);
                    }
                    return true;
                });
                for (String path : orphaned) {
                    state.removeSource(path, null);
                }
            }
            else if (state.isKnownSource(oldRootPath)) {
                state.removeSource(oldRootPath, null);
            }
        }
    }

    private void markDirtyIfSource(VirtualFile file) {
        TranslatingCompilerFilesMonitorImpl monitor = getMonitor();
        ProjectManager projectManager = getProjectManager();

        processRecursively(
            file,
            false,
            thisFile -> {
                String path = TranslatingCompilerFilesMonitorImpl.pathOf(thisFile);
                boolean known = false;
                for (Project project : projectManager.getOpenProjects()) {
                    if (!project.isInitialized() || monitor.isSuspended(project)) {
                        continue;
                    }
                    ProjectCompilerState state = ProjectCompilerState.getInstance(project);
                    if (state.isKnownSource(path)) {
                        known = true;
                        state.markSourceDirty(path);
                    }
                }
                if (!known) {
                    processNewFile(thisFile);
                }
            }
        );
    }

    private void processNewFile(VirtualFile file) {
        ProjectManager projectManager = getProjectManager();
        TranslatingCompilerFilesMonitorImpl monitor = getMonitor();

        for (Project project : projectManager.getOpenProjects()) {
            if (!project.isInitialized()) {
                continue; // the content of this project will be scanned during its post-startup activities
            }
            if (monitor.isSuspended(project)) {
                continue;
            }
            ProjectCompilerState state = ProjectCompilerState.getInstance(project);
            ProjectFileIndex fileIndex = ProjectRootManager.getInstance(project).getFileIndex();

            if (fileIndex.isInSourceContent(file)) {
                TranslatingCompiler[] translators = CompilerManager.getInstance(project).getCompilers(TranslatingCompiler.class);
                Predicate<VirtualFile> isCompilable = thisFile -> {
                    for (TranslatingCompiler translator : translators) {
                        if (translator.isCompilableFile(thisFile.toNioPath(), DummyCompileContext.getInstance())) {
                            return true;
                        }
                    }
                    return false;
                };
                processRecursively(
                    file,
                    false,
                    thisFile -> {
                        if (isCompilable.test(thisFile)) {
                            state.markSourceDirty(TranslatingCompilerFilesMonitorImpl.pathOf(thisFile));
                        }
                    }
                );
            }
            else if (belongsToIntermediateSources(file, project)) {
                processRecursively(
                    file,
                    false,
                    thisFile -> state.markSourceDirty(TranslatingCompilerFilesMonitorImpl.pathOf(thisFile))
                );
            }
        }
    }

    public static void processRecursively(VirtualFile file, boolean dbOnly, Consumer<VirtualFile> processor) {
        if (!(file.getFileSystem() instanceof LocalFileSystem)) {
            return;
        }

        FileTypeManager fileTypeManager = FileTypeManager.getInstance();
        VirtualFileUtil.visitChildrenRecursively(
            file,
            new VirtualFileVisitor() {
                @Override
                public Result visitFileEx(VirtualFile file) {
                    if (fileTypeManager.isFileIgnored(file)) {
                        return SKIP_CHILDREN;
                    }

                    if (!file.isDirectory()) {
                        processor.accept(file);
                    }
                    return CONTINUE;
                }

                @Override
                public @Nullable Iterable<VirtualFile> getChildrenIterable(VirtualFile file) {
                    return file.isDirectory() && dbOnly ? ((NewVirtualFile) file).iterInDbChildren() : null;
                }
            }
        );
    }

    private boolean belongsToIntermediateSources(VirtualFile file, Project project) {
        return FileUtil.isAncestor(
            TranslatingCompilerFilesMonitorImpl.normalizePath(getMonitor().getGeneratedPath(project).getPath()),
            TranslatingCompilerFilesMonitorImpl.pathOf(file),
            true
        );
    }

    TranslatingCompilerFilesMonitorImpl getMonitor() {
        return (TranslatingCompilerFilesMonitorImpl) myMonitorProvider.get();
    }

    ProjectManager getProjectManager() {
        return myProjectManagerProvider.get();
    }
}
