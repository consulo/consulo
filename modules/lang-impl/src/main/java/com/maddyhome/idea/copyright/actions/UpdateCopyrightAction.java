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

package com.maddyhome.idea.copyright.actions;

import com.intellij.analysis.AnalysisScope;
import com.intellij.analysis.BaseAnalysisAction;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.maddyhome.idea.copyright.CopyrightManager;
import com.maddyhome.idea.copyright.CopyrightUpdaters;
import com.maddyhome.idea.copyright.pattern.FileUtil;
import org.jetbrains.annotations.NotNull;

public class UpdateCopyrightAction extends BaseAnalysisAction {
  protected UpdateCopyrightAction() {
    super(UpdateCopyrightProcessor.TITLE, UpdateCopyrightProcessor.TITLE);
  }

  @Override
  public void update(AnActionEvent event) {
    final boolean enabled = isEnabled(event);
    event.getPresentation().setEnabled(enabled);
    if (ActionPlaces.isPopupPlace(event.getPlace())) {
      event.getPresentation().setVisible(enabled);
    }
  }

  private static boolean isEnabled(AnActionEvent event) {
    final DataContext context = event.getDataContext();
    final Project project = CommonDataKeys.PROJECT.getData(context);
    if (project == null) {
      return false;
    }

    if (!CopyrightManager.getInstance(project).hasAnyCopyrights()) {
      return false;
    }
    final VirtualFile[] files = CommonDataKeys.VIRTUAL_FILE_ARRAY.getData(context);
    final Editor editor = CommonDataKeys.EDITOR.getData(context);
    if (editor != null) {
      final PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
      if (file == null || !CopyrightUpdaters.hasExtension(file)) {
        return false;
      }
    }
    else if (files != null && FileUtil.areFiles(files)) {
      boolean copyrightEnabled  = false;
      for (VirtualFile vfile : files) {
        if (CopyrightUpdaters.hasExtension(vfile)) {
          copyrightEnabled = true;
          break;
        }
      }
      if (!copyrightEnabled) {
        return false;
      }

    }
    else if ((files == null || files.length != 1) &&
             LangDataKeys.MODULE_CONTEXT.getData(context) == null &&
             LangDataKeys.MODULE_CONTEXT_ARRAY.getData(context) == null &&
             PlatformDataKeys.PROJECT_CONTEXT.getData(context) == null) {
      final PsiElement[] elems = LangDataKeys.PSI_ELEMENT_ARRAY.getData(context);
      if (elems != null) {
        boolean copyrightEnabled = false;
        for (PsiElement elem : elems) {
          if (!(elem instanceof PsiDirectory)) {
            final PsiFile file = elem.getContainingFile();
            if (file == null || !CopyrightUpdaters.hasExtension(file.getVirtualFile())) {
              copyrightEnabled = true;
              break;
            }
          }
        }
        if (!copyrightEnabled){
          return false;
        }
      }
    }
    return true;
  }

  @Override
  protected void analyze(@NotNull final Project project, @NotNull AnalysisScope scope) {
    if (scope.checkScopeWritable(project)) return;
    scope.accept(new PsiElementVisitor() {
      @Override
      public void visitFile(PsiFile file) {
        new UpdateCopyrightProcessor(project, ModuleUtilCore.findModuleForPsiElement(file), file).run();
      }
    });
  }
}