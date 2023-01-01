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

package consulo.ide.impl.idea.openapi.vcs.changes;

import consulo.annotation.component.ExtensionImpl;
import consulo.component.messagebus.MessageBusConnection;
import consulo.language.editor.wolfAnalyzer.ProblemListener;
import consulo.module.content.layer.event.ModuleRootEvent;
import consulo.module.content.layer.event.ModuleRootListener;
import consulo.project.Project;
import consulo.project.startup.PostStartupActivity;
import consulo.ui.UIAccess;
import consulo.versionControlSystem.change.VcsDirtyScopeManager;
import consulo.virtualFileSystem.VirtualFile;

import javax.annotation.Nonnull;

/**
 * @author yole
 */
@ExtensionImpl
public class VcsEventWatcher implements PostStartupActivity {
  @Override
  public void runActivity(@Nonnull Project project, @Nonnull UIAccess uiAccess) {
    MessageBusConnection connection = project.getMessageBus().connect();
    connection.subscribe(ModuleRootListener.class, new ModuleRootListener() {
      @Override
      public void rootsChanged(ModuleRootEvent event) {
        project.getApplication().invokeLater(() -> {
          if (project.isDisposed()) return;
          VcsDirtyScopeManager.getInstance(project).markEverythingDirty();
        }, project.getApplication().getNoneModalityState());
      }
    });
    connection.subscribe(ProblemListener.class, new MyProblemListener(project));
  }


  private class MyProblemListener implements ProblemListener {
    private final Project myProject;

    public MyProblemListener(Project project) {
      myProject = project;
    }

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