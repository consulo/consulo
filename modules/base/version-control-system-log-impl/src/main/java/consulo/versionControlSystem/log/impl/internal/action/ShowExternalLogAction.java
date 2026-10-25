/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.versionControlSystem.log.impl.internal.action;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionRef;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.component.extension.ExtensionPoint;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.fileChooser.FileChooser;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.ui.Component;
import consulo.ui.UIAccess;
import consulo.ui.Window;
import consulo.ui.WindowOptions;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.LegacyDumbAwareAction;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentFactory;
import consulo.ui.ex.content.ContentManager;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.util.collection.ContainerUtil;
import consulo.util.dataholder.Key;
import consulo.util.lang.Comparing;
import consulo.util.lang.StringUtil;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.versionControlSystem.VcsKey;
import consulo.versionControlSystem.VcsRootChecker;
import consulo.versionControlSystem.VcsToolWindow;
import consulo.versionControlSystem.distributed.repository.Repository;
import consulo.versionControlSystem.distributed.repository.VcsRepositoryCreator;
import consulo.versionControlSystem.distributed.repository.VcsRepositoryManager;
import consulo.versionControlSystem.internal.ChangesViewContentI;
import consulo.versionControlSystem.log.VcsLogFilterCollection;
import consulo.versionControlSystem.log.impl.internal.VcsLogContentProvider;
import consulo.versionControlSystem.log.impl.internal.VcsLogFilterCollectionImpl;
import consulo.versionControlSystem.log.impl.internal.VcsLogManager;
import consulo.versionControlSystem.log.impl.internal.VcsLogTabLocation;
import consulo.versionControlSystem.log.impl.internal.VcsProjectLogImpl;
import consulo.versionControlSystem.log.impl.internal.data.VcsLogTabsProperties;
import consulo.versionControlSystem.log.impl.internal.ui.AbstractVcsLogUi;
import consulo.versionControlSystem.log.impl.internal.ui.VcsLogRootFilterImpl;
import consulo.versionControlSystem.log.localize.VersionControlSystemLogLocalize;
import consulo.versionControlSystem.root.VcsRoot;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.BiConsumer;

/**
 * Shows the log of repositories outside the project, in the Version Control tool window or in a separate window.
 */
@ActionImpl(id = "Vcs.Log.ShowExternalLog", parents = @ActionParentRef(@ActionRef(id = "Vcs.Browse")))
public class ShowExternalLogAction extends LegacyDumbAwareAction {
    private static final Logger LOG = Logger.getInstance(ShowExternalLogAction.class);

    private static final String EXTERNAL = "EXTERNAL";

    private static final Key<List<VirtualFile>> EXTERNAL_ROOTS_KEY = Key.create("ShowExternalLogAction.ExternalRoots");

    public ShowExternalLogAction() {
        super(
            VersionControlSystemLogLocalize.actionShowExternalLogText(),
            VersionControlSystemLogLocalize.actionShowExternalLogDescription(),
            null
        );
    }

    @Override
    public void update(AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(e.hasData(Project.KEY));
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getRequiredData(Project.KEY);
        UIAccess uiAccess = UIAccess.current();

        FileChooserDescriptor descriptor = new FileChooserDescriptor(false, true, false, true, false, true);
        FileChooser.chooseFiles(descriptor, project, null)
            .thenApplyAsync(files -> getRootsFromUser(project, files), AppExecutorUtil.getAppExecutorService())
            .whenCompleteAsync((roots, error) -> {
                if (error != null) {
                    if (!(unwrap(error) instanceof CancellationException)) {
                        LOG.error(error);
                    }
                    return;
                }
                if (project.isDisposed()) {
                    return;
                }
                for (Map.Entry<VcsKey, List<VirtualFile>> entry : roots.entrySet()) {
                    showExternalLog(project, entry.getKey(), entry.getValue());
                }
            }, uiAccess);
    }

    private static Throwable unwrap(Throwable error) {
        return error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
    }

    private static Map<VcsKey, List<VirtualFile>> getRootsFromUser(Project project, VirtualFile @Nullable [] files) {
        Map<VcsKey, List<VirtualFile>> result = new LinkedHashMap<>();
        if (files == null) {
            return result;
        }

        ExtensionPoint<VcsRootChecker> checkers = project.getApplication().getExtensionPoint(VcsRootChecker.class);
        for (VirtualFile file : files) {
            VcsRootChecker checker = checkers.findFirstSafe(it -> it.isRoot(file.getPath()));
            if (checker != null) {
                result.computeIfAbsent(checker.getSupportedVcs(), vcsKey -> new ArrayList<>()).add(file);
            }
        }
        return result;
    }

    @RequiredUIAccess
    private static void showExternalLog(Project project, VcsKey vcsKey, List<VirtualFile> roots) {
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(VcsToolWindow.ID);
        if (project.isDefault() || !ProjectLevelVcsManager.getInstance(project).hasActiveVcss() || toolWindow == null) {
            showLog(project, vcsKey, roots, null, (logManager, ui) -> showLogContentWindow(ui.createPanel(logManager), ui));
            return;
        }

        String description = VersionControlSystemLogLocalize.externalLogTabDescription(
            StringUtil.join(roots, VirtualFile::getPath, "\n")
        ).get();
        Runnable showContent = () -> {
            ContentManager contentManager = toolWindow.getContentManager();
            if (selectProjectLog(project, vcsKey, roots) || selectAlreadyOpened(contentManager, roots)) {
                return;
            }

            String tabName = calcTabName(contentManager, roots);
            showLog(project, vcsKey, roots, tabName, (logManager, ui) -> {
                Content content = ContentFactory.getInstance().createUIContent(ui.createPanel(logManager), tabName, false);
                content.setDisposer(ui);
                content.setDescription(description);
                content.setCloseable(true);
                content.putUserData(Content.TAB_GROUP_NAME_KEY, VcsLogContentProvider.TAB_NAME);
                content.putUserData(EXTERNAL_ROOTS_KEY, List.copyOf(roots));
                contentManager.addContent(content);
                contentManager.setSelectedContent(content);
            });
        };

        if (!toolWindow.isVisible()) {
            toolWindow.activate(showContent, true);
        }
        else {
            showContent.run();
        }
    }

    @RequiredUIAccess
    private static void showLog(Project project,
                                VcsKey vcsKey,
                                List<VirtualFile> roots,
                                @Nullable String contentTabName,
                                BiConsumer<VcsLogManager, AbstractVcsLogUi> show) {
        AbstractVcs vcs = ProjectLevelVcsManager.getInstance(project).findVcsByName(vcsKey);
        if (vcs == null) {
            return;
        }

        UIAccess uiAccess = UIAccess.current();
        CompletableFuture.supplyAsync(() -> createRepositories(project, vcsKey, roots), AppExecutorUtil.getAppExecutorService())
            .whenCompleteAsync((repositories, error) -> {
                if (error != null) {
                    LOG.error(error);
                    return;
                }
                if (project.isDisposed()) {
                    repositories.values().forEach(Disposer::dispose);
                    return;
                }

                VcsRepositoryManager repositoryManager = VcsRepositoryManager.getInstance(project);
                repositories.forEach(repositoryManager::addExternalRepository);

                List<VcsRoot> vcsRoots = ContainerUtil.map(roots, root -> new VcsRoot(vcs, root));
                VcsLogManager logManager = new VcsLogManager(
                    project,
                    project.getInstance(VcsLogTabsProperties.class),
                    vcsRoots,
                    true,
                    null
                );
                AbstractVcsLogUi ui = logManager.createLogUi(calcLogId(roots), contentTabName, null);
                Disposer.register(ui, () -> project.getApplication().invokeLater(() -> {
                    Disposer.dispose(logManager);
                    for (Map.Entry<VirtualFile, Repository> entry : repositories.entrySet()) {
                        repositoryManager.removeExternalRepository(entry.getKey());
                        Disposer.dispose(entry.getValue());
                    }
                }));
                show.accept(logManager, ui);
            }, uiAccess);
    }

    private static Map<VirtualFile, Repository> createRepositories(Project project, VcsKey vcsKey, List<VirtualFile> roots) {
        Map<VirtualFile, Repository> result = new LinkedHashMap<>();
        VcsRepositoryCreator creator = project.getExtensionPoint(VcsRepositoryCreator.class)
            .findFirstSafe(it -> vcsKey.equals(it.getVcsKey()));
        if (creator == null) {
            return result;
        }
        for (VirtualFile root : roots) {
            Repository repository = creator.createRepositoryIfValid(root);
            if (repository != null) {
                result.put(root, repository);
            }
        }
        return result;
    }

    @RequiredUIAccess
    private static void showLogContentWindow(Component content, Disposable disposable) {
        Window window = Window.create(
            VersionControlSystemLogLocalize.externalLogWindowTitle().get(),
            WindowOptions.builder().disableModal().dimensionKey(ShowExternalLogAction.class.getName()).build()
        );
        window.setContent(content);
        window.addCloseListener(event -> Disposer.dispose(disposable));
        window.show();
    }

    private static String calcLogId(List<VirtualFile> roots) {
        return EXTERNAL + " " + StringUtil.join(roots, VirtualFile::getPath, File.pathSeparator);
    }

    private static String calcTabName(ContentManager contentManager, List<VirtualFile> roots) {
        String name = VersionControlSystemLogLocalize.tabName().get() + " (" + roots.get(0).getName() + (roots.size() > 1 ? "+" : "") + ")";
        String candidate = name;
        int count = 1;
        while (hasContentWithName(contentManager, candidate)) {
            candidate = name + "-" + count;
            count++;
        }
        return candidate;
    }

    private static boolean hasContentWithName(ContentManager contentManager, String name) {
        for (Content content : contentManager.getContents()) {
            if (name.equals(content.getDisplayName())) {
                return true;
            }
        }
        return false;
    }

    @RequiredUIAccess
    private static boolean selectProjectLog(Project project, VcsKey vcsKey, List<VirtualFile> roots) {
        ProjectLevelVcsManager vcsManager = ProjectLevelVcsManager.getInstance(project);
        AbstractVcs vcs = vcsManager.findVcsByName(vcsKey);
        if (vcs == null) {
            return false;
        }

        List<VirtualFile> projectRoots = List.of(vcsManager.getRootsUnderVcs(vcs));
        if (!projectRoots.containsAll(roots)) {
            return false;
        }

        if (roots.containsAll(projectRoots)) {
            ChangesViewContentI.getInstance(project).selectContent(VcsLogContentProvider.TAB_NAME, true);
            return true;
        }

        VcsLogFilterCollection filters = new VcsLogFilterCollectionImpl.VcsLogFilterCollectionBuilder()
            .with(new VcsLogRootFilterImpl(roots))
            .build();
        return VcsProjectLogImpl.getInstance(project).openLogTab(filters, VcsLogTabLocation.TOOL_WINDOW) != null;
    }

    @RequiredUIAccess
    private static boolean selectAlreadyOpened(ContentManager contentManager, Collection<VirtualFile> roots) {
        for (Content content : contentManager.getContents()) {
            List<VirtualFile> contentRoots = content.getUserData(EXTERNAL_ROOTS_KEY);
            if (contentRoots != null && Comparing.haveEqualElements(roots, contentRoots)) {
                contentManager.setSelectedContent(content);
                return true;
            }
        }
        return false;
    }
}
