// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.versionControlSystem.change.ignore.action;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.dumb.DumbAware;
import consulo.language.ignore.IgnoreFileType;
import consulo.language.psi.search.FileTypeIndex;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.content.scope.ProjectScopes;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithAsyncUpdate;
import consulo.ui.ex.action.AsyncActionGroup;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.action.coroutine.ActionSafeReadLock;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.step.CodeExecution;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.versionControlSystem.change.ChangeListManager;
import consulo.versionControlSystem.change.IgnoredFileContentProvider;
import consulo.versionControlSystem.change.action.ScheduleForAdditionAction;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.versionControlSystem.root.VcsRoot;
import consulo.versionControlSystem.util.VcsImplUtil;
import consulo.versionControlSystem.util.VcsUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class IgnoreFileActionGroup extends AsyncActionGroup implements AnActionWithAsyncUpdate, DumbAware {
    private final IgnoreFileType myIgnoreFileType;

    public IgnoreFileActionGroup(IgnoreFileType ignoreFileType) {
        super(
            VcsLocalize.vcsAddToIgnoreFileActionGroupText(ignoreFileType.getIgnoreLanguage().getFilename()),
            VcsLocalize.vcsAddToIgnoreFileActionGroupDescription(ignoreFileType.getIgnoreLanguage().getFilename()),
            ignoreFileType.getIcon()
        );
        myIgnoreFileType = ignoreFileType;
    }

    protected List<AnAction> createAdditionalActions(
        Project project,
        List<VirtualFile> selectedFiles,
        List<VirtualFile> unversionedFiles
    ) {
        return List.of();
    }

    @RequiredReadAction
    private List<AnAction> createActionsFor(AnActionEvent e) {
        List<VirtualFile> selectedFiles = IgnoreFileAction.getSelectedFiles(e);
        Project project = e.getData(Project.KEY);
        if (project == null) {
            return List.of();
        }

        List<VirtualFile> unversionedFiles = ScheduleForAdditionAction.getUnversionedFiles(e, project).toList();
        if (unversionedFiles.isEmpty()) {
            return List.of();
        }

        List<Collection<VirtualFile>> ignoreFiles = new ArrayList<>();
        for (VirtualFile file : filterSelectedFiles(project, selectedFiles)) {
            Collection<VirtualFile> suitable = findSuitableIgnoreFiles(project, file);
            if (!suitable.isEmpty()) {
                ignoreFiles.add(suitable);
            }
        }

        Set<VirtualFile> resultedIgnoreFiles = new HashSet<>();
        for (Collection<VirtualFile> files : ignoreFiles) {
            resultedIgnoreFiles.addAll(files);
        }
        for (Collection<VirtualFile> files : ignoreFiles) {
            resultedIgnoreFiles.retainAll(files); //only take ignore files which is suitable for all selected files
        }

        List<AnAction> actions = new ArrayList<>();
        List<AnAction> additionalActions = createAdditionalActions(project, selectedFiles, unversionedFiles);
        if (!resultedIgnoreFiles.isEmpty()) {
            actions.addAll(toActions(resultedIgnoreFiles, project, additionalActions.size()));
        }
        else {
            AnAction newIgnoreFileAction = createNewIgnoreFileAction(project, selectedFiles);
            if (newIgnoreFileAction != null) {
                actions.add(newIgnoreFileAction);
            }
        }

        if (!additionalActions.isEmpty()) {
            actions.addAll(additionalActions);
        }
        return actions;
    }

    @Override
    public Coroutine<?, ?> updateAsync(AnActionEvent e) {
        return ActionSafeReadLock.run(e, presentation -> {
            List<AnAction> actions = createActionsFor(e);
            presentation.setPopupGroup(actions.size() > 1);
            presentation.setPerformGroup(actions.size() == 1);
            presentation.setEnabledAndVisible(!actions.isEmpty());
        }).toCoroutine();
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        List<AnAction> actions = createActionsFor(e);
        if (actions.size() != 1) {
            return;
        }
        actions.get(0).actionPerformed(e);
    }

    @Override
    public Coroutine<?, List<AnAction>> getChildrenAsync(@Nullable AnActionEvent e) {
        if (e == null) {
            return Coroutine.first(CodeExecution.<Object, List<AnAction>>supply(() -> List.of()));
        }
        return ActionSafeReadLock.<Object, List<AnAction>>apply(e, presentation -> createActionsFor(e)).toCoroutine();
    }

    private static List<VirtualFile> filterSelectedFiles(Project project, List<VirtualFile> files) {
        ProjectLevelVcsManager vcsManager = ProjectLevelVcsManager.getInstance(project);
        ChangeListManager changeListManager = ChangeListManager.getInstance(project);
        return files.stream()
            .filter(file -> vcsManager.getVcsFor(file) != null && !changeListManager.isIgnoredFile(file))
            .toList();
    }

    @RequiredReadAction
    private Collection<VirtualFile> findSuitableIgnoreFiles(Project project, VirtualFile file) {
        VirtualFile fileParent = file.getParent();
        return FileTypeIndex.getFiles(myIgnoreFileType, ProjectScopes.getProjectScope(project))
            .stream()
            .filter(it -> Objects.equals(fileParent, it.getParent())
                || fileParent != null && it.getParent() != null && VirtualFileUtil.isAncestor(it.getParent(), fileParent, false))
            .toList();
    }

    private Collection<AnAction> toActions(Collection<VirtualFile> files, Project project, int additionalActionsSize) {
        VirtualFile projectDir = project.getBaseDir();
        List<AnAction> actions = new ArrayList<>(files.size());
        for (VirtualFile file : files) {
            IgnoreFileAction action = new IgnoreFileAction(file);
            Presentation presentation = action.getTemplatePresentation();
            presentation.setIcon(myIgnoreFileType.getIcon());
            presentation.setText(toTextRepresentation(file, project, projectDir, files.size() + additionalActionsSize));
            actions.add(action);
        }
        return actions;
    }

    private @Nullable AnAction createNewIgnoreFileAction(Project project, List<VirtualFile> selectedFiles) {
        String filename = myIgnoreFileType.getIgnoreLanguage().getFilename();
        VcsRoot commonVcsRoot = getCommonIgnoreFileRoot(selectedFiles, project);
        if (commonVcsRoot == null) {
            return null;
        }
        if (commonVcsRoot.getVcs() == null) {
            return null;
        }
        VirtualFile commonIgnoreFileRoot = commonVcsRoot.getPath();
        if (commonIgnoreFileRoot.findChild(filename) != null) {
            return null;
        }
        IgnoredFileContentProvider ignoredFileContentProvider = VcsImplUtil.findIgnoredFileContentProvider(commonVcsRoot.getVcs());
        if (ignoredFileContentProvider == null) {
            return null;
        }
        if (!ignoredFileContentProvider.getFileName().equals(filename)) {
            return null;
        }

        CreateNewIgnoreFileAction action = new CreateNewIgnoreFileAction(filename, commonIgnoreFileRoot);
        Presentation presentation = action.getTemplatePresentation();
        presentation.setIcon(myIgnoreFileType.getIcon());
        presentation.setText(VcsLocalize.vcsAddToIgnoreFileActionGroupText(filename));
        return action;
    }

    private LocalizeValue toTextRepresentation(VirtualFile file, Project project, @Nullable VirtualFile projectDir, int size) {
        if (size == 1) {
            return VcsLocalize.vcsAddToIgnoreFileActionGroupText(myIgnoreFileType.getIgnoreLanguage().getFilename());
        }
        VirtualFile projectRootOrVcsRoot = projectDir != null ? projectDir : VcsUtil.getVcsRootFor(project, file);
        if (projectRootOrVcsRoot == null) {
            return LocalizeValue.of(file.getName());
        }
        String relativePath = VirtualFileUtil.getRelativePath(file, projectRootOrVcsRoot);
        return LocalizeValue.of(relativePath != null ? relativePath : file.getName());
    }

    private static @Nullable VcsRoot getCommonIgnoreFileRoot(Collection<VirtualFile> files, Project project) {
        VirtualFile first = files.isEmpty() ? null : files.iterator().next();
        if (first == null) {
            return null;
        }
        ProjectLevelVcsManager vcsManager = ProjectLevelVcsManager.getInstance(project);
        VcsRoot commonVcsRoot = vcsManager.getVcsRootObjectFor(first);
        if (commonVcsRoot == null) {
            return null;
        }
        if (first.equals(commonVcsRoot.getPath())) {
            // trying to ignore vcs root itself
            return null;
        }

        boolean haveCommonRoot = files.stream()
            .skip(1)
            .allMatch(it -> !it.equals(commonVcsRoot.getPath()) && commonVcsRoot.equals(vcsManager.getVcsRootObjectFor(it)));
        return haveCommonRoot ? commonVcsRoot : null;
    }
}
