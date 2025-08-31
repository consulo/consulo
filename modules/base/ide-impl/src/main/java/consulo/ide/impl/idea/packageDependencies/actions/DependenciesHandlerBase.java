/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.ide.impl.idea.packageDependencies.actions;

import consulo.language.editor.scope.AnalysisScope;
import consulo.ide.impl.idea.analysis.PerformAnalysisInBackgroundOption;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.application.progress.Task;
import consulo.project.Project;
import consulo.ide.impl.idea.packageDependencies.DependenciesBuilder;
import consulo.ide.impl.idea.packageDependencies.DependenciesToolWindow;
import consulo.ide.impl.idea.packageDependencies.ui.DependenciesPanel;
import consulo.language.psi.PsiFile;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentFactory;
import jakarta.annotation.Nonnull;
import consulo.ui.annotation.RequiredUIAccess;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author nik
 */
public abstract class DependenciesHandlerBase {
  protected final Project myProject;
  private final List<AnalysisScope> myScopes;
  private final Set<PsiFile> myExcluded;

  public DependenciesHandlerBase(Project project, List<AnalysisScope> scopes, Set<PsiFile> excluded) {
    myScopes = scopes;
    myExcluded = excluded;
    myProject = project;
  }

  public void analyze() {
    final List<DependenciesBuilder> builders = new ArrayList<DependenciesBuilder>();

    Task task;
    if (canStartInBackground()) {
      task = new Task.Backgroundable(myProject, getProgressTitle(), true, new PerformAnalysisInBackgroundOption(myProject)) {
        @Override
        public void run(@Nonnull ProgressIndicator indicator) {
          perform(builders);
        }

        @RequiredUIAccess
        @Override
        public void onSuccess() {
          DependenciesHandlerBase.this.onSuccess(builders);
        }
      };
    } else {
      task = new Task.Modal(myProject, getProgressTitle(), true) {
        @Override
        public void run(@Nonnull ProgressIndicator indicator) {
          perform(builders);
        }

        @RequiredUIAccess
        @Override
        public void onSuccess() {
          DependenciesHandlerBase.this.onSuccess(builders);
        }
      };
    }
    ProgressManager.getInstance().run(task);
  }

  protected boolean canStartInBackground() {
    return true;
  }

  protected boolean shouldShowDependenciesPanel(List<DependenciesBuilder> builders) {
    return true;
  }

  protected abstract String getProgressTitle();

  protected abstract String getPanelDisplayName(AnalysisScope scope);

  protected abstract DependenciesBuilder createDependenciesBuilder(AnalysisScope scope);

  private void perform(List<DependenciesBuilder> builders) {
    for (AnalysisScope scope : myScopes) {
      builders.add(createDependenciesBuilder(scope));
    }
    for (DependenciesBuilder builder : builders) {
      builder.analyze();
    }
  }

  private void onSuccess(final List<DependenciesBuilder> builders) {
    //noinspection SSBasedInspection
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        if (shouldShowDependenciesPanel(builders)) {
          String displayName = getPanelDisplayName(builders.get(0).getScope());
          DependenciesPanel panel = new DependenciesPanel(myProject, builders, myExcluded);
          Content content = ContentFactory.getInstance().createContent(panel, displayName, false);
          content.setDisposer(panel);
          panel.setContent(content);
          DependenciesToolWindow.getInstance(myProject).addContent(content);
        }
      }
    });
  }
}
