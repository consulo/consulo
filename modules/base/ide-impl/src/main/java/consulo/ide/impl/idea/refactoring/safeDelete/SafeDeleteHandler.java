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

package consulo.ide.impl.idea.refactoring.safeDelete;

import consulo.application.ApplicationManager;
import consulo.codeEditor.Editor;
import consulo.codeEditor.ScrollType;
import consulo.dataContext.DataContext;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.language.editor.CommonDataKeys;
import consulo.language.editor.LangDataKeys;
import consulo.language.editor.refactoring.RefactoringActionHandler;
import consulo.language.editor.refactoring.RefactoringBundle;
import consulo.language.editor.refactoring.RefactoringSettings;
import consulo.language.editor.refactoring.util.CommonRefactoringUtil;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiUtilCore;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.module.Module;
import consulo.project.Project;
import consulo.ui.ex.awt.DialogWrapper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * @author dsl
 */
public class SafeDeleteHandler implements RefactoringActionHandler {
  public static final String REFACTORING_NAME = RefactoringBundle.message("safe.delete.title");

  @Override
  public void invoke(@Nonnull Project project, Editor editor, PsiFile file, DataContext dataContext) {
    PsiElement element = dataContext.getData(CommonDataKeys.PSI_ELEMENT);
    editor.getScrollingModel().scrollToCaret(ScrollType.MAKE_VISIBLE);
    if (element == null || !SafeDeleteProcessor.validElement(element)) {
      String message = RefactoringBundle.getCannotRefactorMessage(RefactoringBundle.message("is.not.supported.in.the.current.context", REFACTORING_NAME));
      CommonRefactoringUtil.showErrorHint(project, editor, message, REFACTORING_NAME, "refactoring.safeDelete");
      return;
    }
    invoke(project, new PsiElement[]{element}, dataContext);
  }

  @Override
  public void invoke(@Nonnull final Project project, @Nonnull PsiElement[] elements, DataContext dataContext) {
    invoke(project, elements, dataContext.getData(LangDataKeys.MODULE), true, null);
  }

  public static void invoke(final Project project, PsiElement[] elements, boolean checkDelegates) {
    invoke(project, elements, checkDelegates, null);
  }

  public static void invoke(final Project project, PsiElement[] elements, boolean checkDelegates, @Nullable final Runnable successRunnable) {
    invoke(project, elements, null, checkDelegates, successRunnable);
  }

  public static void invoke(final Project project, PsiElement[] elements, @Nullable Module module, boolean checkDelegates, @Nullable final Runnable successRunnable) {
    for (PsiElement element : elements) {
      if (!SafeDeleteProcessor.validElement(element)) {
        return;
      }
    }
    final PsiElement[] temptoDelete = PsiTreeUtil.filterAncestors(elements);
    Set<PsiElement> elementsSet = new HashSet<PsiElement>(Arrays.asList(temptoDelete));
    Set<PsiElement> fullElementsSet = new LinkedHashSet<PsiElement>();

    if (checkDelegates) {
      for (PsiElement element : temptoDelete) {
        boolean found = false;
        for(SafeDeleteProcessorDelegate delegate: SafeDeleteProcessorDelegate.EP_NAME.getExtensionList()) {
          if (delegate.handlesElement(element)) {
            found = true;
            Collection<? extends PsiElement> addElements = delegate instanceof SafeDeleteProcessorDelegateBase
                                                           ? ((SafeDeleteProcessorDelegateBase)delegate).getElementsToSearch(element, module, elementsSet)
                                                           : delegate.getElementsToSearch(element, elementsSet);
            if (addElements == null) return;
            fullElementsSet.addAll(addElements);
            break;
          }
        }
        if (!found) {
          fullElementsSet.add(element);
        }
      }
    } else {
      ContainerUtil.addAll(fullElementsSet, temptoDelete);
    }

    if (!CommonRefactoringUtil.checkReadOnlyStatusRecursively(project, fullElementsSet, true)) return;

    final PsiElement[] elementsToDelete = PsiUtilCore.toPsiElementArray(fullElementsSet);

    if (ApplicationManager.getApplication().isUnitTestMode()) {
      RefactoringSettings settings = RefactoringSettings.getInstance();
      SafeDeleteProcessor.createInstance(project, null, elementsToDelete, settings.SAFE_DELETE_SEARCH_IN_COMMENTS,
                                         settings.SAFE_DELETE_SEARCH_IN_NON_JAVA, true).run();
      if (successRunnable != null) successRunnable.run();
    }
    else {
      final SafeDeleteDialog.Callback callback = new SafeDeleteDialog.Callback() {
        @Override
        public void run(final SafeDeleteDialog dialog) {
          SafeDeleteProcessor.createInstance(project, new Runnable() {
            @Override
            public void run() {
              if (successRunnable != null) {
                successRunnable.run();
              }
              dialog.close(DialogWrapper.CANCEL_EXIT_CODE);
            }
          }, elementsToDelete, dialog.isSearchInComments(), dialog.isSearchForTextOccurences(), true).run();
        }

      };

      SafeDeleteDialog dialog = new SafeDeleteDialog(project, elementsToDelete, callback);
      dialog.show();
    }
  }
}
