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
package com.intellij.compiler.actions;

import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonShortcuts;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.compiler.CompileScope;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.roots.ui.configuration.ModulesConfiguratorImpl;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListSeparator;
import com.intellij.openapi.ui.popup.MultiSelectionListPopupStep;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.impl.artifacts.ArtifactUtil;
import com.intellij.packaging.impl.compiler.ArtifactCompileScope;
import com.intellij.packaging.impl.compiler.ArtifactsWorkspaceSettings;
import com.intellij.ui.popup.list.ListPopupImpl;
import com.intellij.util.containers.ContainerUtil;
import consulo.ui.image.Image;
import consulo.util.collection.primitive.ints.IntList;
import consulo.util.collection.primitive.ints.IntLists;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.*;

/**
 * @author nik
 */
public class BuildArtifactAction extends DumbAwareAction {
  private static final NotificationGroup NOTIFICATION_GROUP = NotificationGroup.balloonGroup("Clean artifact");

  @Override
  public void update(AnActionEvent e) {
    final Project project = getEventProject(e);
    final Presentation presentation = e.getPresentation();
    presentation.setEnabled(project != null && !ArtifactUtil.getArtifactWithOutputPaths(project).isEmpty());
  }

  @Override
  public void actionPerformed(AnActionEvent e) {
    final Project project = getEventProject(e);
    if (project == null) return;

    final List<Artifact> artifacts = ArtifactUtil.getArtifactWithOutputPaths(project);
    if (artifacts.isEmpty()) return;

    List<ArtifactPopupItem> items = new ArrayList<ArtifactPopupItem>();
    if (artifacts.size() > 1) {
      items.add(0, new ArtifactPopupItem(null, "All Artifacts", Image.empty(16)));
    }
    Set<Artifact> selectedArtifacts = new HashSet<Artifact>(ArtifactsWorkspaceSettings.getInstance(project).getArtifactsToBuild());
    IntList selectedIndices = IntLists.newArrayList();
    if (Comparing.haveEqualElements(artifacts, selectedArtifacts) && selectedArtifacts.size() > 1) {
      selectedIndices.add(0);
      selectedArtifacts.clear();
    }

    for (Artifact artifact : artifacts) {
      final ArtifactPopupItem item = new ArtifactPopupItem(artifact, artifact.getName(), artifact.getArtifactType().getIcon());
      if (selectedArtifacts.contains(artifact)) {
        selectedIndices.add(items.size());
      }
      items.add(item);
    }

    final ChooseArtifactStep step = new ChooseArtifactStep(items, artifacts.get(0), project);
    step.setDefaultOptionIndices(selectedIndices.toArray());

    final ListPopupImpl popup = (ListPopupImpl)JBPopupFactory.getInstance().createListPopup(step);
    final KeyStroke editKeyStroke = KeymapUtil.getKeyStroke(CommonShortcuts.getEditSource());
    if (editKeyStroke != null) {
      popup.registerAction("editArtifact", editKeyStroke, new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
          Object[] values = popup.getSelectedValues();
          popup.cancel();
          ModulesConfiguratorImpl.showArtifactSettings(project, values.length > 0 ? ((ArtifactPopupItem)values[0]).getArtifact() : null);
        }
      });
    }
    popup.showCenteredInCurrentWindow(project);
  }

  private static void doBuild(@Nonnull Project project, final @Nonnull List<ArtifactPopupItem> items, boolean rebuild) {
    final Set<Artifact> artifacts = getArtifacts(items, project);
    final CompileScope scope = ArtifactCompileScope.createArtifactsScope(project, artifacts, rebuild);

    ArtifactsWorkspaceSettings.getInstance(project).setArtifactsToBuild(artifacts);
    if (!rebuild) {
      //in external build we can set 'rebuild' flag per target type
      CompilerManager.getInstance(project).make(scope, null);
    }
    else {
      CompilerManager.getInstance(project).compile(scope, null);
    }
  }

  private static Set<Artifact> getArtifacts(final List<ArtifactPopupItem> items, final Project project) {
    Set<Artifact> artifacts = new LinkedHashSet<Artifact>();
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
    private CleanArtifactItem(@Nonnull List<ArtifactPopupItem> item, @Nonnull Project project) {
      super(item, project, "Clean");
    }

    @Override
    public void run() {
      Set<VirtualFile> parents = new HashSet<VirtualFile>();
      final VirtualFile[] roots = ProjectRootManager.getInstance(myProject).getContentSourceRoots();
      for (VirtualFile root : roots) {
        VirtualFile parent = root;
        while (parent != null && !parents.contains(parent)) {
          parents.add(parent);
          parent = parent.getParent();
        }
      }

      Map<String, String> outputPathContainingSourceRoots = new HashMap<String, String>();
      final List<Pair<File, Artifact>> toClean = new ArrayList<Pair<File, Artifact>>();
      Set<Artifact> artifacts = getArtifacts(myArtifactPopupItems, myProject);
      for (Artifact artifact : artifacts) {
        String outputPath = artifact.getOutputFilePath();
        if (outputPath != null) {
          toClean.add(Pair.create(new File(FileUtil.toSystemDependentName(outputPath)), artifact));
          final VirtualFile outputFile = LocalFileSystem.getInstance().findFileByPath(outputPath);
          if (parents.contains(outputFile)) {
            outputPathContainingSourceRoots.put(artifact.getName(), outputPath);
          }
        }
      }

      if (!outputPathContainingSourceRoots.isEmpty()) {
        final String message;
        if (outputPathContainingSourceRoots.size() == 1 && outputPathContainingSourceRoots.values().size() == 1) {
          final String name = ContainerUtil.getFirstItem(outputPathContainingSourceRoots.keySet());
          final String output = outputPathContainingSourceRoots.get(name);
          message = "The output directory '" + output + "' of '" + name + "' artifact contains source roots of the project. Do you want to continue and clear it?";
        }
        else {
          StringBuilder info = new StringBuilder();
          for (String name : outputPathContainingSourceRoots.keySet()) {
            info.append(" '").append(name).append("' artifact ('").append(outputPathContainingSourceRoots.get(name)).append("')\n");
          }
          message = "The output directories of the following artifacts contains source roots:\n" + info + "Do you want to continue and clear these directories?";
        }
        final int answer = Messages.showYesNoDialog(myProject, message, "Clean Artifacts", null);
        if (answer != Messages.YES) {
          return;
        }
      }

      new Task.Backgroundable(myProject, "Cleaning artifacts...", true) {
        @Override
        public void run(@Nonnull ProgressIndicator indicator) {
          List<File> deleted = new ArrayList<File>();
          for (Pair<File, Artifact> pair : toClean) {
            indicator.checkCanceled();
            File file = pair.getFirst();
            if (!FileUtil.delete(file)) {
              NOTIFICATION_GROUP.createNotification("Cannot clean '" + pair.getSecond().getName() + "' artifact", "cannot delete '" + file.getAbsolutePath() + "'", NotificationType.ERROR, null)
                      .notify(myProject);
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
      final Artifact artifact = getArtifact();
      return artifact != null ? Collections.singletonList(artifact) : ArtifactUtil.getArtifactWithOutputPaths(project);
    }
  }

  private static class ChooseArtifactStep extends MultiSelectionListPopupStep<ArtifactPopupItem> {
    private final Artifact myFirst;
    private final Project myProject;

    public ChooseArtifactStep(List<ArtifactPopupItem> artifacts, Artifact first, Project project) {
      super("Build Artifact", artifacts);
      myFirst = first;
      myProject = project;
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
        return doFinalStep(new Runnable() {
          @Override
          public void run() {
            doBuild(myProject, selectedValues, false);
          }
        });
      }
      final List<ArtifactActionItem> actions = new ArrayList<ArtifactActionItem>();
      actions.add(new BuildArtifactItem(selectedValues, myProject));
      actions.add(new RebuildArtifactItem(selectedValues, myProject));
      actions.add(new CleanArtifactItem(selectedValues, myProject));
      actions.add(new EditArtifactItem(selectedValues, myProject));

      return new BaseListPopupStep<ArtifactActionItem>(selectedValues.size() == 1 ? "Action" : "Action for " + selectedValues.size() + " artifacts", actions) {
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
