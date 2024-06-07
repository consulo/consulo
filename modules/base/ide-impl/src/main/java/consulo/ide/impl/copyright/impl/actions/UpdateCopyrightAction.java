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

package consulo.ide.impl.copyright.impl.actions;

import consulo.annotation.component.ActionRef;
import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionParentRef;
import consulo.codeEditor.Editor;
import consulo.dataContext.DataContext;
import consulo.ide.impl.copyright.impl.pattern.FileUtil;
import consulo.ide.impl.idea.analysis.BaseAnalysisAction;
import consulo.language.copyright.UpdateCopyrightsProvider;
import consulo.language.copyright.config.CopyrightManager;
import consulo.language.editor.LangDataKeys;
import consulo.language.editor.PlatformDataKeys;
import consulo.language.editor.scope.AnalysisScope;
import consulo.language.psi.*;
import consulo.language.util.ModuleUtilCore;
import consulo.project.Project;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnActionEvent;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;

@ActionImpl(id = "UpdateCopyright", parents = {
        @ActionParentRef(@ActionRef(id = "ProjectViewPopupMenu")),
        @ActionParentRef(@ActionRef(id = "CodeMenu")),
        @ActionParentRef(@ActionRef(id = "NavbarPopupMenu"))
})
public class UpdateCopyrightAction extends BaseAnalysisAction {
  public UpdateCopyrightAction() {
    super(UpdateCopyrightProcessor.TITLE, UpdateCopyrightProcessor.TITLE);
    getTemplatePresentation().setText(UpdateCopyrightProcessor.TITLE);
    getTemplatePresentation().setDescription(UpdateCopyrightProcessor.DESCRIPTION);
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
    final Project project = context.getData(Project.KEY);
    if (project == null) {
      return false;
    }

    if (!CopyrightManager.getInstance(project).hasAnyCopyrights()) {
      return false;
    }
    final VirtualFile[] files = context.getData(VirtualFile.KEY_OF_ARRAY);
    final Editor editor = context.getData(Editor.KEY);
    if (editor != null) {
      final PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
      if (file == null || !UpdateCopyrightsProvider.hasExtension(file)) {
        return false;
      }
    }
    else if (files != null && FileUtil.areFiles(files)) {
      boolean copyrightEnabled  = false;
      for (VirtualFile vfile : files) {
        if (UpdateCopyrightsProvider.hasExtension(vfile)) {
          copyrightEnabled = true;
          break;
        }
      }
      if (!copyrightEnabled) {
        return false;
      }

    }
    else {
      if ((files == null || files.length != 1) &&
          context.getData(LangDataKeys.MODULE_CONTEXT) == null &&
          context.getData(LangDataKeys.MODULE_CONTEXT_ARRAY) == null &&
          context.getData(PlatformDataKeys.PROJECT_CONTEXT) == null) {
        final PsiElement[] elems = context.getData(PsiElement.KEY_OF_ARRAY);
        if (elems != null) {
          boolean copyrightEnabled = false;
          for (PsiElement elem : elems) {
            if (!(elem instanceof PsiDirectory)) {
              final PsiFile file = elem.getContainingFile();
              if (file == null || !UpdateCopyrightsProvider.hasExtension(file.getVirtualFile())) {
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
    }
    return true;
  }

  @Override
  protected void analyze(@Nonnull final Project project, @Nonnull AnalysisScope scope) {
    if (scope.checkScopeWritable(project)) return;
    scope.accept(new PsiElementVisitor() {
      @Override
      public void visitFile(PsiFile file) {
        new UpdateCopyrightProcessor(project, ModuleUtilCore.findModuleForPsiElement(file), file).run();
      }
    });
  }
}