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
import consulo.component.extension.ExtensionPoint;
import consulo.disposer.Disposable;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.fileChooser.IdeaFileChooser;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentFactory;
import consulo.ui.ex.content.ContentManager;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.Comparing;
import consulo.util.lang.StringUtil;
import consulo.versionControlSystem.*;
import consulo.versionControlSystem.log.impl.internal.VcsLogContentProvider;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.List;
import java.util.*;

@ActionImpl(id = "Vcs.Log.ShowExternalLog", parents = @ActionParentRef(@ActionRef(id = "Vcs.Browse")))
public class ShowExternalLogAction extends DumbAwareAction {
    private static final String EXTERNAL = "EXTERNAL";

    public ShowExternalLogAction() {
        super(LocalizeValue.localizeTODO("Show Repository Log..."));
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(e.hasData(Project.KEY));
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(@Nonnull AnActionEvent e) {
        Project project = e.getRequiredData(Project.KEY);
        LinkedHashMap<VcsKey, List<VirtualFile>> roots = selectRoots(project);
        if (roots.isEmpty()) {
            return;
        }

        ProjectLevelVcsManager projectLevelVcsManager = ProjectLevelVcsManager.getInstance(project);
        if (project.isDefault() || !projectLevelVcsManager.hasActiveVcss()) {
            //  ProgressManager.getInstance().run(new ShowLogInDialogTask(project, roots, vcs));
            return;
        }

        ToolWindow window = ToolWindowManager.getInstance(project).getToolWindow(VcsToolWindow.ID);
        Runnable showContent = () -> {
            ContentManager cm = window.getContentManager();
            if (checkIfProjectLogMatches(project, cm, roots) || checkIfAlreadyOpened(cm, roots)) {
                return;
            }

            for (Map.Entry<VcsKey, List<VirtualFile>> entry : roots.entrySet()) {
                VcsKey vcsKey = entry.getKey();
                List<VirtualFile> vcsRoots = entry.getValue();

                AbstractVcs vcs = projectLevelVcsManager.findVcsByName(vcsKey);
                if (vcs == null) {
                    continue;
                }

                String tabName = calcTabName(cm, vcsRoots);
                MyContentComponent component = createManagerAndContent(project, vcs, vcsRoots, tabName);
                Content content = ContentFactory.getInstance().createContent(component, tabName, false);
                content.setDisposer(component.myDisposable);
                content.setDescription("Log for " + StringUtil.join(vcsRoots, VirtualFile::getPath, "\n"));
                content.setCloseable(true);
                cm.addContent(content);
                cm.setSelectedContent(content);
            }

        };

        if (!window.isVisible()) {
            window.activate(showContent, true);
        }
        else {
            showContent.run();
        }
    }

    @Nonnull
    private static MyContentComponent createManagerAndContent(@Nonnull Project project,
                                                              @Nonnull AbstractVcs vcs,
                                                              @Nonnull List<VirtualFile> roots,
                                                              @Nullable String tabName) {
//        GitRepositoryManager repositoryManager = ServiceManager.getService(project, GitRepositoryManager.class);
//        for (VirtualFile root : roots) {
//            repositoryManager.addExternalRepository(root, GitRepositoryImpl.getInstance(root, project, true));
//        }
//        VcsLogManager manager = new VcsLogManager(project,
//            ServiceManager.getService(project, VcsLogTabsProperties.class),
//            ContainerUtil.map(roots, root -> new VcsRoot(vcs, root)));
//        return new MyContentComponent(manager.createLogPanel(calcLogId(roots), tabName), roots, () ->
//        {
//            for (VirtualFile root : roots) {
//                repositoryManager.removeExternalRepository(root);
//            }
//        });
        return null;
    }

    @Nonnull
    private static String calcLogId(@Nonnull List<VirtualFile> roots) {
        return EXTERNAL + " " + StringUtil.join(roots, VirtualFile::getPath, File.pathSeparator);
    }

    @Nonnull
    private static String calcTabName(@Nonnull ContentManager cm, @Nonnull List<VirtualFile> roots) {
        String name = VcsLogContentProvider.TAB_NAME + " (" + roots.get(0).getName();
        if (roots.size() > 1) {
            name += "+";
        }
        name += ")";

        String candidate = name;
        int cnt = 1;
        while (hasContentsWithName(cm, candidate)) {
            candidate = name + "-" + cnt;
            cnt++;
        }
        return candidate;
    }

    private static boolean hasContentsWithName(@Nonnull ContentManager cm, @Nonnull String candidate) {
        return ContainerUtil.exists(cm.getContents(), content -> content.getDisplayName().equals(candidate));
    }

    @Nonnull
    private static LinkedHashMap<VcsKey, List<VirtualFile>> selectRoots(@Nonnull Project project) {
        ExtensionPoint<VcsRootChecker> checkers = project.getApplication().getExtensionPoint(VcsRootChecker.class);

        FileChooserDescriptor descriptor = new FileChooserDescriptor(false, true, false, true, false, true);
        VirtualFile[] virtualFiles = IdeaFileChooser.chooseFiles(descriptor, project, null);
        descriptor.withFileFilter(file -> checkers.anyMatchSafe(r -> r.isRoot(file.getPath())));

        LinkedHashMap<VcsKey, List<VirtualFile>> result = new LinkedHashMap<>();
        for (VirtualFile file : virtualFiles) {
            VcsRootChecker checker = checkers.findFirstSafe(r -> r.isRoot(file.getPath()));
            if (checker != null) {
                result.computeIfAbsent(checker.getSupportedVcs(), vcsKey -> new ArrayList<>(2)).add(file);
            }

        }
        return result;
    }


    private static boolean checkIfProjectLogMatches(@Nonnull Project project,
                                                    @Nonnull ContentManager cm,
                                                    @Nonnull LinkedHashMap<VcsKey, List<VirtualFile>> roots) {
        ProjectLevelVcsManager manager = ProjectLevelVcsManager.getInstance(project);

        if (roots.size() == 1) {
            Map.Entry<VcsKey, List<VirtualFile>> first = roots.firstEntry();

            AbstractVcs vcs = manager.findVcsByName(first.getKey());
            if (vcs == null) {
                return false;
            }

            VirtualFile[] projectRoots = manager.getRootsUnderVcs(vcs);

            if (Comparing.haveEqualElements(first.getValue(), Arrays.asList(projectRoots))) {
                Content[] contents = cm.getContents();
                for (Content content : contents) {
                    if (VcsLogContentProvider.TAB_NAME.equals(content.getDisplayName())) {
                        cm.setSelectedContent(content);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean checkIfAlreadyOpened(@Nonnull ContentManager cm, @Nonnull LinkedHashMap<VcsKey, List<VirtualFile>> roots) {
        Content toSelect = null;

        for (Content content : cm.getContents()) {
            JComponent component = content.getComponent();
            if (!(component instanceof MyContentComponent targetComponent)) {
                continue;
            }

            if (toSelect == null) {
                toSelect = content;
            }

            for (List<VirtualFile> vcsRoots : roots.values()) {
                if (!Comparing.haveEqualElements(vcsRoots, targetComponent.myRoots)) {
                    return false;
                }
            }
        }

        if (toSelect != null) {
            cm.setSelectedContent(toSelect);
        }
        return true;
    }

    private static class MyContentComponent extends JPanel {
        @Nonnull
        private final Collection<VirtualFile> myRoots;
        @Nonnull
        private final Disposable myDisposable;

        MyContentComponent(@Nonnull JComponent actualComponent, @Nonnull Collection<VirtualFile> roots, @Nonnull Disposable disposable) {
            super(new BorderLayout());
            myDisposable = disposable;
            myRoots = roots;
            add(actualComponent);
        }
    }
//
//    private static class ShowLogInDialogTask extends Task.Backgroundable {
//        @Nonnull
//        private final Project myProject;
//        @Nonnull
//        private final List<VirtualFile> myRoots;
//        @Nonnull
//        private final GitVcs myVcs;
//        private GitVersion myVersion;
//
//        private ShowLogInDialogTask(@Nonnull Project project, @Nonnull List<VirtualFile> roots, @Nonnull GitVcs vcs) {
//            super(project, "Loading Git Log...", true);
//            myProject = project;
//            myRoots = roots;
//            myVcs = vcs;
//        }
//
//        @Override
//        public void run(@Nonnull ProgressIndicator indicator) {
//            myVersion = myVcs.getVersion();
//            if (myVersion.isNull()) {
//                myVcs.checkVersion();
//                myVersion = myVcs.getVersion();
//            }
//        }
//
//        @Override
//        public void onSuccess() {
//            if (!myVersion.isNull() && !myProject.isDisposed()) {
//                MyContentComponent content = createManagerAndContent(myProject, myVcs, myRoots, null);
//                WindowWrapper window = new WindowWrapperBuilder(WindowWrapper.Mode.FRAME, content).setProject(myProject)
//                    .setTitle("Git Log")
//                    .setPreferredFocusedComponent(content)
//                    .setDimensionServiceKey(ShowExternalLogAction.class.getName())
//                    .build();
//                Disposer.register(window, content.myDisposable);
//                window.show();
//            }
//        }
//    }
}
