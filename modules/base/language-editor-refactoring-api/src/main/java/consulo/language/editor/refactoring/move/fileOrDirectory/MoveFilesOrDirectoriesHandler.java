/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.language.editor.refactoring.move.fileOrDirectory;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.refactoring.move.MoveCallback;
import consulo.language.editor.refactoring.move.MoveHandlerDelegate;
import consulo.language.scratch.ScratchFileService;
import consulo.dataContext.DataContext;
import consulo.language.editor.LangDataKeys;
import consulo.language.plain.psi.PsiPlainText;
import consulo.language.psi.*;
import consulo.logging.Logger;
import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.language.psi.PsiUtilCore;

import jakarta.annotation.Nullable;

import java.util.Arrays;
import java.util.HashSet;

@ExtensionImpl(id = "moveFileOrDir")
public class MoveFilesOrDirectoriesHandler extends MoveHandlerDelegate {
  private static final Logger LOG = Logger.getInstance(MoveFilesOrDirectoriesHandler.class);

  @Override
  public boolean canMove(PsiElement[] elements, PsiElement targetContainer) {
    HashSet<String> names = new HashSet<>();
    for (PsiElement element : elements) {
      if (element instanceof PsiFile) {
        PsiFile file = (PsiFile)element;
        String name = file.getName();
        if (names.contains(name)) {
          return false;
        }
        names.add(name);
      }
      else if (!(element instanceof PsiDirectory)) {
        return false;
      }
    }

    return super.canMove(elements, targetContainer);
  }

  @Override
  public boolean isValidTarget(PsiElement psiElement, PsiElement[] sources) {
    return isValidTarget(psiElement);
  }

  public static boolean isValidTarget(PsiElement psiElement) {
    if (!(psiElement instanceof PsiDirectory || psiElement instanceof PsiDirectoryContainer)) return false;
    return psiElement.getManager().isInProject(psiElement) || ScratchFileService.isInScratchRoot(PsiUtilCore.getVirtualFile(psiElement));
  }

  public void doMove(PsiElement[] elements, PsiElement targetContainer) {
    Project project = targetContainer != null ? targetContainer.getProject() : elements[0].getProject();
    doMove(project, elements, targetContainer, null);
  }


  @Nullable
  @Override
  public PsiElement[] adjustForMove(Project project, PsiElement[] sourceElements, PsiElement targetElement) {
    return PsiTreeUtil.filterAncestors(sourceElements);
  }

  @Override
  public void doMove(Project project, PsiElement[] elements, PsiElement targetContainer, @Nullable MoveCallback callback) {
    if (!LOG.assertTrue(targetContainer == null || targetContainer instanceof PsiDirectory || targetContainer instanceof PsiDirectoryContainer,
                        "container: " + targetContainer + "; elements: " + Arrays.toString(elements) + "; working handler: " + toString())) {
      return;
    }
    PsiElement[] adjustedElements = adjustForMove(project, elements, targetContainer);
    if (adjustedElements != null) {
      MoveFilesOrDirectoriesUtil.doMove(project, adjustedElements, new PsiElement[] {targetContainer}, callback);
    }
  }

  @Override
  public boolean tryToMove(PsiElement element, Project project, DataContext dataContext, PsiReference reference,
                           Editor editor) {
    if ((element instanceof PsiFile && ((PsiFile)element).getVirtualFile() != null)
        || element instanceof PsiDirectory) {
      doMove(project, new PsiElement[]{element}, dataContext.getData(LangDataKeys.TARGET_PSI_ELEMENT), null);
      return true;
    }
    if (element instanceof PsiPlainText) {
      PsiFile file = element.getContainingFile();
      if (file != null) {
        doMove(project, new PsiElement[]{file}, null, null);
      }
      return true;
    }
    return false;
  }
}
