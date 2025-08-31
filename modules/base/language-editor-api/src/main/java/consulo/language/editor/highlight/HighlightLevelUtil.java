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

package consulo.language.editor.highlight;

import consulo.application.ApplicationManager;
import consulo.language.editor.FileHighlightingSetting;
import consulo.language.editor.internal.HighlightingSettingsPerFile;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.ProjectRootManager;
import consulo.project.Project;
import consulo.project.ProjectCoreUtil;
import consulo.project.content.scope.ProjectScopes;
import consulo.virtualFileSystem.RawFileLoaderHelper;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;

public class HighlightLevelUtil {
  private HighlightLevelUtil() {
  }
  public enum AnalysisLevel {
    HIGHLIGHT, HIGHLIGHT_AND_INSPECT
  }
  public static boolean shouldAnalyse(@Nonnull PsiFile root, @Nonnull AnalysisLevel analysisLevel) {
    return analysisLevel == AnalysisLevel.HIGHLIGHT_AND_INSPECT ? shouldInspect(root) : shouldHighlight(root);
  }


  public static boolean shouldHighlight(@Nonnull PsiElement psiRoot) {
    HighlightingSettingsPerFile component = HighlightingSettingsPerFile.getInstance(psiRoot.getProject());

    FileHighlightingSetting settingForRoot = component.getHighlightingSettingForRoot(psiRoot);
    return settingForRoot != FileHighlightingSetting.SKIP_HIGHLIGHTING;
  }

  public static boolean shouldInspect(@Nonnull PsiElement psiRoot) {
    if (ApplicationManager.getApplication().isUnitTestMode()) return true;

    if (!shouldHighlight(psiRoot)) return false;
    Project project = psiRoot.getProject();
    VirtualFile virtualFile = psiRoot.getContainingFile().getVirtualFile();
    if (virtualFile == null || !virtualFile.isValid()) return false;

    if (ProjectCoreUtil.isProjectOrWorkspaceFile(virtualFile)) return false;

    ProjectFileIndex fileIndex = ProjectRootManager.getInstance(project).getFileIndex();
    if (ProjectScopes.getLibrariesScope(project).contains(virtualFile) && !fileIndex.isInContent(virtualFile)) return false;

    if (RawFileLoaderHelper.isTooLargeForIntelligence(virtualFile)) return false;

    HighlightingSettingsPerFile component = HighlightingSettingsPerFile.getInstance(project);

    FileHighlightingSetting settingForRoot = component.getHighlightingSettingForRoot(psiRoot);
    return settingForRoot != FileHighlightingSetting.SKIP_INSPECTION;
  }

  public static void forceRootHighlighting(@Nonnull PsiElement root, @Nonnull FileHighlightingSetting level) {
    HighlightingSettingsPerFile component = HighlightingSettingsPerFile.getInstance(root.getProject());

    component.setHighlightingSettingForRoot(root, level);
  }
}
