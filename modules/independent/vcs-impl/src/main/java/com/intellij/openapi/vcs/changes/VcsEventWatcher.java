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

import com.intellij.ProjectTopics;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectManagerListener;
import com.intellij.openapi.roots.ModuleRootAdapter;
import com.intellij.openapi.roots.ModuleRootEvent;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.problems.WolfTheProblemSolver;
import com.intellij.util.messages.MessageBusConnection;
import consulo.annotations.NotLazy;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author yole
 */
@Singleton
@NotLazy
public class VcsEventWatcher {
  private Project myProject;

  @Inject
  public VcsEventWatcher(Project project) {
    myProject = project;

    project.getMessageBus().connect().subscribe(ProjectManager.TOPIC, new ProjectManagerListener() {
      @Override
      public void projectOpened(Project project) {
        if(project == myProject) {
          VcsEventWatcher.this.projectOpened();
        }
      }
    });
  }

  private void projectOpened() {
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
        }, ModalityState.NON_MODAL);
      }
    });
    final WolfTheProblemSolver.ProblemListener myProblemListener = new MyProblemListener();
    WolfTheProblemSolver.getInstance(myProject).addProblemListener(myProblemListener, myProject);
  }

  private class MyProblemListener extends WolfTheProblemSolver.ProblemListener {
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