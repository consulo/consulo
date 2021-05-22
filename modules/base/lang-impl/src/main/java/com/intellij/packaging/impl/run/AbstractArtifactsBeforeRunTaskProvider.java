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
import com.intellij.packaging.artifacts.*;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.util.concurrent.AsyncResult;
import consulo.util.dataholder.Key;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
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
    project.getMessageBus().connect().subscribe(ArtifactManager.TOPIC, new ArtifactListener() {
      @Override
      public void artifactRemoved(@Nonnull Artifact artifact) {
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

  @Nonnull
  @Override
  public Key<T> getId() {
    return myId;
  }

  @Override
  public Image getIcon() {
    return AllIcons.Nodes.Artifact;
  }

  @Override
  public Image getTaskIcon(AbstractArtifactsBeforeRunTask task) {
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

  @RequiredUIAccess
  @Nonnull
  @Override
  public AsyncResult<Void> configureTask(RunConfiguration runConfiguration, T task) {
    final Artifact[] artifacts = ArtifactManager.getInstance(myProject).getArtifacts();
    Set<ArtifactPointer> pointers = new HashSet<>();
    for (Artifact artifact : artifacts) {
      pointers.add(ArtifactPointerManager.getInstance(myProject).create(artifact));
    }
    pointers.addAll(task.getArtifactPointers());
    ArtifactChooser chooser = new ArtifactChooser(new ArrayList<>(pointers));
    chooser.markElements(task.getArtifactPointers());
    chooser.setPreferredSize(new Dimension(400, 300));

    DialogBuilder builder = new DialogBuilder(myProject);
    builder.setTitle(CompilerBundle.message("build.artifacts.before.run.selector.title"));
    builder.setDimensionServiceKey("#BuildArtifactsBeforeRunChooser");
    builder.addOkAction();
    builder.addCancelAction();
    builder.setCenterPanel(chooser);
    builder.setPreferredFocusComponent(chooser);

    AsyncResult<Void> result = builder.showAsync();
    result.doWhenDone(() -> task.setArtifactPointers(chooser.getMarkedElements()));
    return result;
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
