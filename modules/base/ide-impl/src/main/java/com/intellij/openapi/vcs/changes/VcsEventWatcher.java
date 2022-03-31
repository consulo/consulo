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

package com.intellij.openapi.vcs.changes;

import consulo.module.content.ProjectTopics;
import consulo.application.ApplicationManager;
import consulo.application.impl.internal.IdeaModalityState;
import consulo.project.ProjectComponent;
import consulo.project.Project;
import consulo.module.content.layer.event.ModuleRootAdapter;
import consulo.module.content.layer.event.ModuleRootEvent;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.editor.wolfAnalyzer.ProblemListener;
import consulo.component.messagebus.MessageBusConnection;

import javax.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * @author yole
 */
@Singleton
public class VcsEventWatcher implements ProjectComponent {
  private Project myProject;

  @Inject
  public VcsEventWatcher(Project project) {
    myProject = project;
  }

  @Override
  public void projectOpened() {
    MessageBusConnection connection = myProject.getMessageBus().connect(myProject);
    connection.subscribe(ProjectTopics.PROJECT_ROOTS, new ModuleRootAdapter() {
      @Override
      public void rootsChanged(ModuleRootEvent event) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
          @Override
          public void run() {
            if (myProject.isDisposed()) return;
            VcsDirtyScopeManager.getInstance(myProject).markEverythingDirty();
          }
        }, IdeaModalityState.NON_MODAL);
      }
    });
    connection.subscribe(ProblemListener.TOPIC, new MyProblemListener());
  }

  private class MyProblemListener implements ProblemListener {
    @Override
    public void problemsAppeared(@Nonnull final VirtualFile file) {
      ChangesViewManager.getInstance(myProject).refreshChangesViewNodeAsync(file);
    }

    @Override
    public void problemsDisappeared(@Nonnull VirtualFile file) {
      ChangesViewManager.getInstance(myProject).refreshChangesViewNodeAsync(file);
    }
  }
}