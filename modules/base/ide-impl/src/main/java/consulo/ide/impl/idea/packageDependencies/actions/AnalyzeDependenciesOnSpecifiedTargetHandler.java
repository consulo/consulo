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
import consulo.language.editor.scope.AnalysisScopeBundle;
import consulo.ide.impl.idea.openapi.util.text.StringUtil;
import consulo.ide.impl.idea.packageDependencies.DependenciesBuilder;
import consulo.ide.impl.idea.packageDependencies.ForwardDependenciesBuilder;
import consulo.language.psi.PsiFile;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.project.Project;
import consulo.project.ui.notification.NotificationGroup;
import consulo.project.ui.notification.NotificationType;
import consulo.project.ui.wm.ToolWindowId;
import consulo.virtualFileSystem.VirtualFile;

import javax.annotation.Nonnull;
import java.util.*;

/**
 * @author nik
 */
public class AnalyzeDependenciesOnSpecifiedTargetHandler extends DependenciesHandlerBase {
  private final GlobalSearchScope myTargetScope;

  public AnalyzeDependenciesOnSpecifiedTargetHandler(@Nonnull Project project, @Nonnull AnalysisScope scope, @Nonnull GlobalSearchScope targetScope) {
    super(project, Collections.singletonList(scope), Collections.<PsiFile>emptySet());
    myTargetScope = targetScope;
  }

  @Override
  protected String getProgressTitle() {
    return AnalysisScopeBundle.message("package.dependencies.progress.title");
  }

  @Override
  protected String getPanelDisplayName(AnalysisScope scope) {
    return AnalysisScopeBundle.message("package.dependencies.on.toolwindow.title", scope.getDisplayName(), myTargetScope.getDisplayName());
  }

  @Override
  protected boolean shouldShowDependenciesPanel(List<DependenciesBuilder> builders) {
    for (DependenciesBuilder builder : builders) {
      for (Set<PsiFile> files : builder.getDependencies().values()) {
        if (!files.isEmpty()) {
          return true;
        }
      }
    }
    final String source = StringUtil.decapitalize(builders.get(0).getScope().getDisplayName());
    final String target = StringUtil.decapitalize(myTargetScope.getDisplayName());
    final String message = AnalysisScopeBundle.message("no.dependencies.found.message", source, target);
    NotificationGroup.toolWindowGroup("Dependencies", ToolWindowId.DEPENDENCIES, true).createNotification(message, NotificationType.INFORMATION).notify(myProject);
    return false;
  }

  @Override
  protected DependenciesBuilder createDependenciesBuilder(AnalysisScope scope) {
    return new ForwardDependenciesBuilder(myProject, scope) {
      @Override
      public void analyze() {
        super.analyze();
        final Map<PsiFile,Set<PsiFile>> dependencies = getDependencies();
        for (PsiFile file : dependencies.keySet()) {
          final Set<PsiFile> files = dependencies.get(file);
          final Iterator<PsiFile> iterator = files.iterator();
          while (iterator.hasNext()) {
            PsiFile next = iterator.next();
            final VirtualFile virtualFile = next.getVirtualFile();
            if (virtualFile == null || !myTargetScope.contains(virtualFile)) {
              iterator.remove();
            }
          }
        }
      }
    };
  }
}
