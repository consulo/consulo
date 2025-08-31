/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

package consulo.ide.impl.idea.packageDependencies.ui;

import consulo.application.impl.internal.IdeaModalityState;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.component.ProcessCanceledException;
import consulo.ide.impl.idea.packageDependencies.DependenciesBuilder;
import consulo.ide.impl.idea.packageDependencies.FindDependencyUtil;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiUtilCore;
import consulo.project.Project;
import consulo.usage.UsageInfo;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DependenciesUsagesPanel extends UsagesPanel {
  private final List<DependenciesBuilder> myBuilders;

  public DependenciesUsagesPanel(Project project, List<DependenciesBuilder> builders) {
    super(project);
    myBuilders = builders;
    setToInitialPosition();
  }

  @Override
  public String getInitialPositionText() {
    return myBuilders.get(0).getInitialUsagesPosition();
  }

  @Override
  public String getCodeUsagesString() {
    return myBuilders.get(0).getRootNodeNameInUsageView();
  }

  public void findUsages(Set<PsiFile> searchIn, Set<PsiFile> searchFor) {
    cancelCurrentFindRequest();

    myAlarm.cancelAllRequests();
    myAlarm.addRequest(() -> myProject.getApplication().executeOnPooledThread(() -> {
      ProgressIndicator progress = new PanelProgressIndicator(myProject.getUIAccess().getScheduler(), this::setToComponent);
      myCurrentProgress = progress;
      ProgressManager.getInstance().runProcess(() -> {
        myProject.getApplication().runReadAction(() -> {
          UsageInfo[] usages = UsageInfo.EMPTY_ARRAY;
          Set<PsiFile> elementsToSearch = null;

          try {
            if (myBuilders.get(0).isBackward()) {
              elementsToSearch = searchIn;
              usages = FindDependencyUtil.findBackwardDependencies(myBuilders, searchFor, searchIn);
            }
            else {
              elementsToSearch = searchFor;
              usages = FindDependencyUtil.findDependencies(myBuilders, searchIn, searchFor);
            }
            assert !new HashSet<>(elementsToSearch).contains(null);
          }
          catch (ProcessCanceledException ignored) {
          }
          catch (Exception e) {
            LOG.error(e);
          }

          if (!progress.isCanceled()) {
            UsageInfo[] finalUsages = usages;
            PsiElement[] _elementsToSearch =
              elementsToSearch != null ? PsiUtilCore.toPsiElementArray(elementsToSearch) : PsiElement.EMPTY_ARRAY;
            myProject.getApplication()
                     .invokeLater(() -> showUsages(_elementsToSearch, finalUsages), IdeaModalityState.stateForComponent(this));
          }
        });
        myCurrentProgress = null;
      }, progress);
    }), 300);
  }

  public void addBuilder(DependenciesBuilder builder) {
    myBuilders.add(builder);
  }
}
