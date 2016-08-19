/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.packaging.impl.run;

import com.intellij.execution.BeforeRunTaskProvider;
import com.intellij.execution.RunManagerEx;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.compiler.CompilerBundle;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogBuilder;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.Key;
import com.intellij.packaging.artifacts.*;
import consulo.packaging.artifacts.ArtifactPointerUtil;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author nik
 */
public abstract class AbstractArtifactsBeforeRunTaskProvider<T extends AbstractArtifactsBeforeRunTask<T>> extends BeforeRunTaskProvider<T> {
  protected final Project myProject;
  private final Key<T> myId;

  public AbstractArtifactsBeforeRunTaskProvider(Project project, Key<T> id) {
    myProject = project;
    myId = id;
    project.getMessageBus().connect().subscribe(ArtifactManager.TOPIC, new ArtifactAdapter() {
      @Override
      public void artifactRemoved(@NotNull Artifact artifact) {
        final RunManagerEx runManager = RunManagerEx.getInstanceEx(myProject);
        for (RunConfiguration configuration : runManager.getAllConfigurationsList()) {
          final List<T> tasks = runManager.getBeforeRunTasks(configuration, getId());
          for (AbstractArtifactsBeforeRunTask task : tasks) {
            final String artifactName = artifact.getName();
            final List<ArtifactPointer> pointersList = task.getArtifactPointers();
            final ArtifactPointer[] pointers = pointersList.toArray(new ArtifactPointer[pointersList.size()]);
            for (ArtifactPointer pointer : pointers) {
              if (pointer.getName().equals(artifactName) && ArtifactManager.getInstance(myProject).findArtifact(artifactName) == null) {
                task.removeArtifact(pointer);
              }
            }
          }
        }
      }
    });
  }

  @Override
  public Key<T> getId() {
    return myId;
  }

  @Override
  public Icon getIcon() {
    return AllIcons.Nodes.Artifact;
  }

  @Override
  public Icon getTaskIcon(AbstractArtifactsBeforeRunTask task) {
    List<ArtifactPointer> pointers = task.getArtifactPointers();
    if (pointers == null || pointers.isEmpty())
      return getIcon();
    Artifact artifact = pointers.get(0).get();
    if (artifact == null)
      return getIcon();
    return artifact.getArtifactType().getIcon();
  }

  @Override
  public boolean isConfigurable() {
    return true;
  }

  @Override
  public boolean configureTask(RunConfiguration runConfiguration, T task) {
    final Artifact[] artifacts = ArtifactManager.getInstance(myProject).getArtifacts();
    Set<ArtifactPointer> pointers = new THashSet<ArtifactPointer>();
    for (Artifact artifact : artifacts) {
      pointers.add(ArtifactPointerUtil.getPointerManager(myProject).create(artifact));
    }
    pointers.addAll(task.getArtifactPointers());
    ArtifactChooser chooser = new ArtifactChooser(new ArrayList<ArtifactPointer>(pointers));
    chooser.markElements(task.getArtifactPointers());
    chooser.setPreferredSize(new Dimension(400, 300));

    DialogBuilder builder = new DialogBuilder(myProject);
    builder.setTitle(CompilerBundle.message("build.artifacts.before.run.selector.title"));
    builder.setDimensionServiceKey("#BuildArtifactsBeforeRunChooser");
    builder.addOkAction();
    builder.addCancelAction();
    builder.setCenterPanel(chooser);
    builder.setPreferredFocusComponent(chooser);
    if (builder.show() == DialogWrapper.OK_EXIT_CODE) {
      task.setArtifactPointers(chooser.getMarkedElements());
      return true;
    }
    return false;
  }

  @Override
  public boolean canExecuteTask(RunConfiguration configuration, T task) {
    for (ArtifactPointer pointer:  task.getArtifactPointers()) {
      if (pointer.get() != null)
        return true;
    }
    return false;
  }
}
