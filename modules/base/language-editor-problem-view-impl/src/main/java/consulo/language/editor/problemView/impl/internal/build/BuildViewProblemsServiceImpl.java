// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.problemView.impl.internal.build;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.build.ui.BuildProgressObservable;
import consulo.build.ui.event.FileMessageEvent;
import consulo.build.ui.event.MessageEvent;
import consulo.build.ui.event.StartBuildEvent;
import consulo.language.editor.problemView.FileProblem;
import consulo.language.editor.problemView.HighlightingDuplicateProblem;
import consulo.language.editor.problemView.ProblemsCollector;
import consulo.language.editor.problemView.ProblemsProvider;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Singleton
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
public class BuildViewProblemsServiceImpl implements ProblemsProvider {
    private final Project myProject;
    private final Map<String, Object> myWorkingDirToBuildId = new HashMap<>();
    private final Map<Object, Set<FileBuildProblem>> myBuildIdToFileProblems = new HashMap<>();

    @Inject
    public BuildViewProblemsServiceImpl(@Nonnull Project project) {
        myProject = project;
    }

    @Override
    public @Nonnull Project getProject() {
        return myProject;
    }

    @Override
    public void dispose() {
        myBuildIdToFileProblems.clear();
        myWorkingDirToBuildId.clear();
    }

    public void listenToBuildView(@Nonnull BuildProgressObservable buildProgressObservable) {
        ProblemsCollector collector = myProject.getInstance(ProblemsCollector.class);

        buildProgressObservable.addListener((buildId, event) -> {
            if (event instanceof FileMessageEvent fileMessageEvent && fileMessageEvent.getKind() == MessageEvent.Kind.ERROR) {
                Path nioPath = fileMessageEvent.getFilePosition().getFile() != null ? fileMessageEvent.getFilePosition().getFile().toPath() : null;
                VirtualFile virtualFile = nioPath != null ? VirtualFileManager.getInstance().findFileByNioPath(nioPath) : null;
                if (virtualFile == null) {
                    return;
                }

                FileBuildProblem problem = new FileBuildProblem(fileMessageEvent, virtualFile, this);

                Set<FileBuildProblem> problems = myBuildIdToFileProblems.computeIfAbsent(buildId, __ -> new HashSet<>());
                if (problems.add(problem)) {
                    collector.problemAppeared(problem);
                }
                else {
                    collector.problemUpdated(problem);
                }
            }

            if (event instanceof StartBuildEvent startBuildEvent) {
                Object oldBuildId = myWorkingDirToBuildId.put(startBuildEvent.getBuildDescriptor().getWorkingDir(), buildId);
                if (oldBuildId != null) {
                    Set<FileBuildProblem> oldProblems = myBuildIdToFileProblems.get(oldBuildId);
                    if (oldProblems != null) {
                        for (FileBuildProblem oldProblem : oldProblems) {
                            collector.problemDisappeared(oldProblem);
                        }
                    }
                    myBuildIdToFileProblems.remove(oldBuildId);
                }
            }
        }, this);
    }

    public static class FileBuildProblem implements FileProblem, HighlightingDuplicateProblem {
        private final FileMessageEvent myEvent;
        private final VirtualFile myVirtualFile;
        private final ProblemsProvider myProblemsProvider;

        public FileBuildProblem(@Nonnull FileMessageEvent event,
                                @Nonnull VirtualFile virtualFile,
                                @Nonnull ProblemsProvider problemsProvider) {
            myEvent = event;
            myVirtualFile = virtualFile;
            myProblemsProvider = problemsProvider;
        }

        public @Nonnull FileMessageEvent getEvent() {
            return myEvent;
        }

        public @Nonnull VirtualFile getVirtualFile() {
            return myVirtualFile;
        }

        public @Nonnull ProblemsProvider getProblemsProvider() {
            return myProblemsProvider;
        }

        @Override
        public @Nullable String getDescription() {
            return myEvent.getDescription();
        }

        @Override
        public @Nonnull VirtualFile getFile() {
            return myVirtualFile;
        }

        @Override
        public int getLine() {
            return myEvent.getFilePosition().getStartLine();
        }

        @Override
        public int getColumn() {
            return myEvent.getFilePosition().getStartColumn();
        }

        @Override
        public @Nonnull ProblemsProvider getProvider() {
            return myProblemsProvider;
        }

        @Override
        public @Nonnull String getText() {
            return myEvent.getMessage();
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (other == null || getClass() != other.getClass()) {
                return false;
            }

            FileBuildProblem that = (FileBuildProblem) other;
            if (!myEvent.equals(that.myEvent)) {
                return false;
            }
            if (!myVirtualFile.equals(that.myVirtualFile)) {
                return false;
            }
            return myProblemsProvider.equals(that.myProblemsProvider);
        }

        @Override
        public int hashCode() {
            int result = myEvent.hashCode();
            result = 31 * result + myVirtualFile.hashCode();
            result = 31 * result + myProblemsProvider.hashCode();
            return result;
        }
    }
}
