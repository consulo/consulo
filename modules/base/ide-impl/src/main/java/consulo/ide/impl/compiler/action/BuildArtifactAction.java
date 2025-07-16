/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.ide.impl.compiler.action;

import consulo.annotation.component.ActionImpl;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.Task;
import consulo.compiler.CompilerManager;
import consulo.compiler.artifact.Artifact;
import consulo.compiler.artifact.ArtifactUtil;
import consulo.compiler.impl.internal.artifact.ArtifactCompileScope;
import consulo.compiler.scope.CompileScope;
import consulo.ide.impl.idea.openapi.keymap.KeymapUtil;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.ModulesConfiguratorImpl;
import consulo.ide.impl.idea.packaging.impl.compiler.ArtifactsWorkspaceSettings;
import consulo.ide.impl.idea.ui.popup.list.ListPopupImpl;
import consulo.localize.LocalizeValue;
import consulo.module.content.ProjectRootManager;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.Project;
import consulo.project.ui.notification.NotificationGroup;
import consulo.project.ui.notification.NotificationService;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.CommonShortcuts;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.popup.*;
import consulo.ui.image.Image;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.primitive.ints.IntList;
import consulo.util.collection.primitive.ints.IntLists;
import consulo.util.io.FileUtil;
import consulo.util.lang.Comparing;
import consulo.util.lang.Pair;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.*;

/**
 * @author nik
 */
@ActionImpl(id = "BuildArtifact")
public class BuildArtifactAction extends DumbAwareAction {
    public static final NotificationGroup NOTIFICATION_GROUP = NotificationGroup.balloonGroup("Clean artifact");

    private final NotificationService myNotificationService;

    @Inject
    public BuildArtifactAction(NotificationService notificationService) {
        super(ActionLocalize.actionBuildartifactText(), ActionLocalize.actionBuildartifactDescription());
        myNotificationService = notificationService;
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        e.getPresentation().setEnabled(project != null && !ArtifactUtil.getArtifactWithOutputPaths(project).isEmpty());
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        final Project project = e.getRequiredData(Project.KEY);

        List<Artifact> artifacts = ArtifactUtil.getArtifactWithOutputPaths(project);
        if (artifacts.isEmpty()) {
            return;
        }

        List<ArtifactPopupItem> items = new ArrayList<>();
        if (artifacts.size() > 1) {
            items.add(0, new ArtifactPopupItem(null, "All Artifacts", Image.empty(16)));
        }
        Set<Artifact> selectedArtifacts = new HashSet<>(ArtifactsWorkspaceSettings.getInstance(project).getArtifactsToBuild());
        IntList selectedIndices = IntLists.newArrayList();
        if (Comparing.haveEqualElements(artifacts, selectedArtifacts) && selectedArtifacts.size() > 1) {
            selectedIndices.add(0);
            selectedArtifacts.clear();
        }

        for (Artifact artifact : artifacts) {
            ArtifactPopupItem item = new ArtifactPopupItem(artifact, artifact.getName(), artifact.getArtifactType().getIcon());
            if (selectedArtifacts.contains(artifact)) {
                selectedIndices.add(items.size());
            }
            items.add(item);
        }

        ChooseArtifactStep step = new ChooseArtifactStep(items, artifacts.get(0), project, myNotificationService);
        step.setDefaultOptionIndices(selectedIndices.toArray());

        final ListPopupImpl popup = (ListPopupImpl) JBPopupFactory.getInstance().createListPopup(step);
        KeyStroke editKeyStroke = KeymapUtil.getKeyStroke(CommonShortcuts.getEditSource());
        if (editKeyStroke != null) {
            popup.registerAction(
                "editArtifact",
                editKeyStroke,
                new AbstractAction() {
                    @Override
                    @RequiredUIAccess
                    public void actionPerformed(ActionEvent e) {
                        Object[] values = popup.getSelectedValues();
                        popup.cancel();
                        ModulesConfiguratorImpl.showArtifactSettings(
                            project,
                            values.length > 0 ? ((ArtifactPopupItem) values[0]).getArtifact() : null
                        );
                    }
                }
            );
        }
        popup.showCenteredInCurrentWindow(project);
    }

    private static void doBuild(@Nonnull Project project, @Nonnull List<ArtifactPopupItem> items, boolean rebuild) {
        Set<Artifact> artifacts = getArtifacts(items, project);
        CompileScope scope = ArtifactCompileScope.createArtifactsScope(project, artifacts, rebuild);

        ArtifactsWorkspaceSettings.getInstance(project).setArtifactsToBuild(artifacts);
        if (!rebuild) {
            //in external build we can set 'rebuild' flag per target type
            CompilerManager.getInstance(project).make(scope, null);
        }
        else {
            CompilerManager.getInstance(project).compile(scope, null);
        }
    }

    private static Set<Artifact> getArtifacts(List<ArtifactPopupItem> items, Project project) {
        Set<Artifact> artifacts = new LinkedHashSet<>();
        for (ArtifactPopupItem item : items) {
            artifacts.addAll(item.getArtifacts(project));
        }
        return artifacts;
    }

    private static class BuildArtifactItem extends ArtifactActionItem {
        private BuildArtifactItem(List<ArtifactPopupItem> item, Project project) {
            super(item, project, "Build");
        }

        @Override
        public void run() {
            doBuild(myProject, myArtifactPopupItems, false);
        }
    }

    private static class CleanArtifactItem extends ArtifactActionItem {
        private final NotificationService myNotificationService;

        private CleanArtifactItem(
            @Nonnull List<ArtifactPopupItem> item,
            @Nonnull Project project,
            @Nonnull NotificationService notificationService
        ) {
            super(item, project, "Clean");
            myNotificationService = notificationService;
        }

        @Override
        @RequiredUIAccess
        public void run() {
            Set<VirtualFile> parents = new HashSet<>();
            VirtualFile[] roots = ProjectRootManager.getInstance(myProject).getContentSourceRoots();
            for (VirtualFile root : roots) {
                VirtualFile parent = root;
                while (parent != null && !parents.contains(parent)) {
                    parents.add(parent);
                    parent = parent.getParent();
                }
            }

            Map<String, String> outputPathContainingSourceRoots = new HashMap<>();
            final List<Pair<File, Artifact>> toClean = new ArrayList<>();
            Set<Artifact> artifacts = getArtifacts(myArtifactPopupItems, myProject);
            for (Artifact artifact : artifacts) {
                String outputPath = artifact.getOutputFilePath();
                if (outputPath != null) {
                    toClean.add(Pair.create(new File(FileUtil.toSystemDependentName(outputPath)), artifact));
                    VirtualFile outputFile = LocalFileSystem.getInstance().findFileByPath(outputPath);
                    if (parents.contains(outputFile)) {
                        outputPathContainingSourceRoots.put(artifact.getName(), outputPath);
                    }
                }
            }

            if (!outputPathContainingSourceRoots.isEmpty()) {
                String message;
                if (outputPathContainingSourceRoots.size() == 1 && outputPathContainingSourceRoots.values().size() == 1) {
                    String name = ContainerUtil.getFirstItem(outputPathContainingSourceRoots.keySet());
                    String output = outputPathContainingSourceRoots.get(name);
                    message = "The output directory '" + output + "' of '" + name + "' artifact " +
                        "contains source roots of the project. Do you want to continue and clear it?";
                }
                else {
                    StringBuilder info = new StringBuilder();
                    for (String name : outputPathContainingSourceRoots.keySet()) {
                        info.append(" '")
                            .append(name)
                            .append("' artifact ('")
                            .append(outputPathContainingSourceRoots.get(name))
                            .append("')\n");
                    }
                    message = "The output directories of the following artifacts contains source roots:\n" + info +
                        "Do you want to continue and clear these directories?";
                }
                int answer = Messages.showYesNoDialog(myProject, message, "Clean Artifacts", null);
                if (answer != Messages.YES) {
                    return;
                }
            }

            new Task.Backgroundable(myProject, LocalizeValue.localizeTODO("Cleaning artifacts..."), true) {
                @Override
                public void run(@Nonnull ProgressIndicator indicator) {
                    List<File> deleted = new ArrayList<>();
                    for (Pair<File, Artifact> pair : toClean) {
                        indicator.checkCanceled();
                        File file = pair.getFirst();
                        if (!FileUtil.delete(file)) {
                            myNotificationService.newError(NOTIFICATION_GROUP)
                                .title(LocalizeValue.localizeTODO("Cannot clean '" + pair.getSecond().getName() + "' artifact"))
                                .content(LocalizeValue.localizeTODO("cannot delete '" + file.getAbsolutePath() + "'"))
                                .notify((Project) myProject);
                        }
                        else {
                            deleted.add(file);
                        }
                    }
                    LocalFileSystem.getInstance().refreshIoFiles(deleted, true, true, null);
                }
            }.queue();
        }
    }

    private static class RebuildArtifactItem extends ArtifactActionItem {
        private RebuildArtifactItem(List<ArtifactPopupItem> item, Project project) {
            super(item, project, "Rebuild");
        }

        @Override
        public void run() {
            doBuild(myProject, myArtifactPopupItems, true);
        }
    }

    private static class EditArtifactItem extends ArtifactActionItem {
        private EditArtifactItem(List<ArtifactPopupItem> item, Project project) {
            super(item, project, "Edit...");
        }

        @Override
        @RequiredUIAccess
        public void run() {
            ModulesConfiguratorImpl.showArtifactSettings(myProject, myArtifactPopupItems.get(0).getArtifact());
        }
    }

    private static abstract class ArtifactActionItem implements Runnable {
        protected final List<ArtifactPopupItem> myArtifactPopupItems;
        protected final Project myProject;
        private String myActionName;

        protected ArtifactActionItem(@Nonnull List<ArtifactPopupItem> item, @Nonnull Project project, @Nonnull String name) {
            myArtifactPopupItems = item;
            myProject = project;
            myActionName = name;
        }

        public String getActionName() {
            return myActionName;
        }
    }

    private static class ArtifactPopupItem {
        @Nullable
        private final Artifact myArtifact;
        private final String myText;
        private final Image myIcon;

        private ArtifactPopupItem(@Nullable Artifact artifact, String text, Image icon) {
            myArtifact = artifact;
            myText = text;
            myIcon = icon;
        }

        @Nullable
        public Artifact getArtifact() {
            return myArtifact;
        }

        public String getText() {
            return myText;
        }

        public Image getIcon() {
            return myIcon;
        }

        public List<Artifact> getArtifacts(Project project) {
            Artifact artifact = getArtifact();
            return artifact != null ? Collections.singletonList(artifact) : ArtifactUtil.getArtifactWithOutputPaths(project);
        }
    }

    private static class ChooseArtifactStep extends MultiSelectionListPopupStep<ArtifactPopupItem> {
        private final Artifact myFirst;
        private final Project myProject;
        private final NotificationService myNotificationService;

        public ChooseArtifactStep(
            List<ArtifactPopupItem> artifacts,
            Artifact first,
            Project project,
            NotificationService notificationService
        ) {
            super("Build Artifact", artifacts);
            myFirst = first;
            myProject = project;
            myNotificationService = notificationService;
        }

        @Override
        public boolean isSpeedSearchEnabled() {
            return true;
        }

        @Override
        public Image getIconFor(ArtifactPopupItem aValue) {
            return aValue.getIcon();
        }

        @Nonnull
        @Override
        public String getTextFor(ArtifactPopupItem value) {
            return value.getText();
        }

        @Override
        public boolean hasSubstep(List<ArtifactPopupItem> selectedValues) {
            return true;
        }

        @Override
        public ListSeparator getSeparatorAbove(ArtifactPopupItem value) {
            return myFirst.equals(value.getArtifact()) ? new ListSeparator() : null;
        }

        @Override
        public PopupStep<?> onChosen(final List<ArtifactPopupItem> selectedValues, boolean finalChoice) {
            if (finalChoice) {
                return doFinalStep(() -> doBuild(myProject, selectedValues, false));
            }
            final List<ArtifactActionItem> actions = new ArrayList<>();
            actions.add(new BuildArtifactItem(selectedValues, myProject));
            actions.add(new RebuildArtifactItem(selectedValues, myProject));
            actions.add(new CleanArtifactItem(selectedValues, myProject, myNotificationService));
            actions.add(new EditArtifactItem(selectedValues, myProject));

            return new BaseListPopupStep<ArtifactActionItem>(
                selectedValues.size() == 1 ? "Action" : "Action for " + selectedValues.size() + " artifacts",
                actions
            ) {
                @Nonnull
                @Override
                public String getTextFor(ArtifactActionItem value) {
                    return value.getActionName();
                }

                @Override
                public PopupStep onChosen(ArtifactActionItem selectedValue, boolean finalChoice) {
                    return doFinalStep(selectedValue);
                }
            };
        }
    }
}
