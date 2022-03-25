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

package com.intellij.ide.util;

import consulo.application.CommonBundle;
import com.intellij.history.LocalHistory;
import com.intellij.history.LocalHistoryAction;
import consulo.dataContext.DataManager;
import consulo.ui.ex.DeleteProvider;
import com.intellij.ide.IdeBundle;
import consulo.language.editor.CommonDataKeys;
import consulo.dataContext.DataContext;
import com.intellij.openapi.actionSystem.LangDataKeys;
import consulo.application.ApplicationBundle;
import consulo.application.ApplicationManager;
import consulo.application.impl.internal.ApplicationNamesInfo;
import consulo.undoRedo.CommandProcessor;
import com.intellij.openapi.fileEditor.impl.NonProjectFileWritingAccessProvider;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.ui.ex.awt.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.ex.MessagesEx;
import consulo.util.lang.ref.Ref;
import consulo.virtualFileSystem.VFileProperty;
import consulo.virtualFileSystem.VirtualFile;
import com.intellij.openapi.vfs.WritingAccessProvider;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiFileSystemItem;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.language.editor.internal.PsiUtilBase;
import consulo.language.psi.PsiUtilCore;
import com.intellij.refactoring.RefactoringBundle;
import com.intellij.refactoring.safeDelete.SafeDeleteDialog;
import com.intellij.refactoring.safeDelete.SafeDeleteProcessor;
import com.intellij.refactoring.util.CommonRefactoringUtil;
import com.intellij.refactoring.util.RefactoringUIUtil;
import consulo.language.util.IncorrectOperationException;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.io.ReadOnlyAttributeUtil;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

public class DeleteHandler {
  private DeleteHandler() {
  }

  public static class DefaultDeleteProvider implements DeleteProvider {
    @Override
    public boolean canDeleteElement(@Nonnull DataContext dataContext) {
      if (dataContext.getData(CommonDataKeys.PROJECT) == null) {
        return false;
      }
      final PsiElement[] elements = getPsiElements(dataContext);
      return elements != null && shouldEnableDeleteAction(elements);
    }

    @Nullable
    private static PsiElement[] getPsiElements(DataContext dataContext) {
      PsiElement[] elements = dataContext.getData(LangDataKeys.PSI_ELEMENT_ARRAY);
      if (elements == null) {
        final Object data = dataContext.getData(CommonDataKeys.PSI_ELEMENT);
        if (data != null) {
          elements = new PsiElement[]{(PsiElement)data};
        }
        else {
          final Object data1 = dataContext.getData(CommonDataKeys.PSI_FILE);
          if (data1 != null) {
            elements = new PsiElement[]{(PsiFile)data1};
          }
        }
      }
      return elements;
    }

    @Override
    public void deleteElement(@Nonnull DataContext dataContext) {
      PsiElement[] elements = getPsiElements(dataContext);
      if (elements == null) return;
      Project project = dataContext.getData(CommonDataKeys.PROJECT);
      if (project == null) return;
      LocalHistoryAction a = LocalHistory.getInstance().startAction(IdeBundle.message("progress.deleting"));
      try {
        deletePsiElement(elements, project);
      }
      finally {
        a.finish();
      }
    }
  }

  public static void deletePsiElement(final PsiElement[] elementsToDelete, final Project project) {
    deletePsiElement(elementsToDelete, project, true);
  }

  public static void deletePsiElement(final PsiElement[] elementsToDelete, final Project project, boolean needConfirmation) {
    if (elementsToDelete == null || elementsToDelete.length == 0) return;

    final PsiElement[] elements = PsiTreeUtil.filterAncestors(elementsToDelete);

    boolean safeDeleteApplicable = true;
    for (int i = 0; i < elements.length && safeDeleteApplicable; i++) {
      PsiElement element = elements[i];
      safeDeleteApplicable = SafeDeleteProcessor.validElement(element);
    }

    final boolean dumb = DumbService.getInstance(project).isDumb();
    if (safeDeleteApplicable && !dumb) {
      final Ref<Boolean> exit = Ref.create(false);
      final SafeDeleteDialog dialog = new SafeDeleteDialog(project, elements, new SafeDeleteDialog.Callback() {
        @Override
        public void run(final SafeDeleteDialog dialog) {
          if (!CommonRefactoringUtil.checkReadOnlyStatusRecursively(project, Arrays.asList(elements), true)) return;

          SafeDeleteProcessor processor = SafeDeleteProcessor.createInstance(project, () -> {
            exit.set(true);
            dialog.close(DialogWrapper.OK_EXIT_CODE);
          }, elements, dialog.isSearchInComments(), dialog.isSearchForTextOccurences(), true);

          processor.run();
        }
      }) {
        @Override
        protected boolean isDelete() {
          return true;
        }
      };
      if (needConfirmation) {
        dialog.setTitle(RefactoringBundle.message("delete.title"));
        if (!dialog.showAndGet() || exit.get()) {
          return;
        }
      }
    }
    else {
      @SuppressWarnings({"UnresolvedPropertyKey"})
      String warningMessage = DeleteUtil.generateWarningMessage(IdeBundle.message("prompt.delete.elements"), elements);

      boolean anyDirectories = false;
      String directoryName = null;
      for (PsiElement psiElement : elementsToDelete) {
        if (psiElement instanceof PsiDirectory && !PsiUtilBase.isSymLink((PsiDirectory)psiElement)) {
          anyDirectories = true;
          directoryName = ((PsiDirectory)psiElement).getName();
          break;
        }
      }
      if (anyDirectories) {
        if (elements.length == 1) {
          warningMessage += IdeBundle.message("warning.delete.all.files.and.subdirectories", directoryName);
        }
        else {
          warningMessage += IdeBundle.message("warning.delete.all.files.and.subdirectories.in.the.selected.directory");
        }
      }

      if (safeDeleteApplicable && dumb) {
        warningMessage += "\n\nWarning:\n  Safe delete is not available while " +
                          ApplicationNamesInfo.getInstance().getFullProductName() +
                          " updates indices,\n  no usages will be checked.";
      }

      if (needConfirmation) {
        int result = Messages.showOkCancelDialog(project, warningMessage, IdeBundle.message("title.delete"),
                                                 ApplicationBundle.message("button.delete"), CommonBundle.getCancelButtonText(),
                                                 Messages.getQuestionIcon());
        if (result != Messages.OK) return;
      }
    }

    CommandProcessor.getInstance().executeCommand(project, () -> NonProjectFileWritingAccessProvider.disableChecksDuring(() -> {
      Collection<PsiElement> directories = ContainerUtil.newSmartList();
      for (PsiElement e : elements) {
        if (e instanceof PsiFileSystemItem && e.getParent() != null) {
          directories.add(e.getParent());
        }
      }

      if (!CommonRefactoringUtil.checkReadOnlyStatus(project, Arrays.asList(elements), directories, false)) {
        return;
      }

      // deleted from project view or something like that.
      if (DataManager.getInstance().getDataContext().getData(CommonDataKeys.EDITOR) == null) {
        CommandProcessor.getInstance().markCurrentCommandAsGlobal(project);
      }

      for (final PsiElement elementToDelete : elements) {
        if (!elementToDelete.isValid()) continue; //was already deleted
        if (elementToDelete instanceof PsiDirectory) {
          VirtualFile virtualFile = ((PsiDirectory)elementToDelete).getVirtualFile();
          if (virtualFile.isInLocalFileSystem() && !virtualFile.is(VFileProperty.SYMLINK)) {
            ArrayList<VirtualFile> readOnlyFiles = new ArrayList<>();
            CommonRefactoringUtil.collectReadOnlyFiles(virtualFile, readOnlyFiles);

            if (!readOnlyFiles.isEmpty()) {
              String message = IdeBundle.message("prompt.directory.contains.read.only.files", virtualFile.getPresentableUrl());
              int _result = Messages.showYesNoDialog(project, message, IdeBundle.message("title.delete"), Messages.getQuestionIcon());
              if (_result != Messages.YES) continue;

              boolean success = true;
              for (VirtualFile file : readOnlyFiles) {
                success = clearReadOnlyFlag(file, project);
                if (!success) break;
              }
              if (!success) continue;
            }
          }
        }
        else if (!elementToDelete.isWritable() &&
                 !(elementToDelete instanceof PsiFileSystemItem && PsiUtilBase.isSymLink((PsiFileSystemItem)elementToDelete))) {
          final PsiFile file = elementToDelete.getContainingFile();
          if (file != null) {
            final VirtualFile virtualFile = file.getVirtualFile();
            if (virtualFile.isInLocalFileSystem()) {
              int _result = MessagesEx.fileIsReadOnly(project, virtualFile)
                      .setTitle(IdeBundle.message("title.delete"))
                      .appendMessage(" " + IdeBundle.message("prompt.delete.it.anyway"))
                      .askYesNo();
              if (_result != Messages.YES) continue;

              boolean success = clearReadOnlyFlag(virtualFile, project);
              if (!success) continue;
            }
          }
        }

        try {
          elementToDelete.checkDelete();
        }
        catch (IncorrectOperationException ex) {
          Messages.showMessageDialog(project, ex.getMessage(), CommonBundle.getErrorTitle(), Messages.getErrorIcon());
          continue;
        }

        ApplicationManager.getApplication().runWriteAction(() -> {
          try {
            elementToDelete.delete();
          }
          catch (final IncorrectOperationException ex) {
            ApplicationManager.getApplication().invokeLater(
                    () -> Messages.showMessageDialog(project, ex.getMessage(), CommonBundle.getErrorTitle(), Messages.getErrorIcon()));
          }
        });
      }
    }), RefactoringBundle.message("safe.delete.command", RefactoringUIUtil.calculatePsiElementDescriptionList(elements)), null);
  }

  private static boolean clearReadOnlyFlag(final VirtualFile virtualFile, final Project project) {
    final boolean[] success = new boolean[1];
    CommandProcessor.getInstance().executeCommand(project, () -> {
      Runnable action = () -> {
        try {
          ReadOnlyAttributeUtil.setReadOnlyAttribute(virtualFile, false);
          success[0] = true;
        }
        catch (IOException e1) {
          Messages.showMessageDialog(project, e1.getMessage(), CommonBundle.getErrorTitle(), Messages.getErrorIcon());
        }
      };
      ApplicationManager.getApplication().runWriteAction(action);
    }, "", null);
    return success[0];
  }

  public static boolean shouldEnableDeleteAction(PsiElement[] elements) {
    if (elements == null || elements.length == 0) return false;
    for (PsiElement element : elements) {
      VirtualFile virtualFile = PsiUtilCore.getVirtualFile(element);
      if (virtualFile == null) {
        return false;
      }
      if (!WritingAccessProvider.isPotentiallyWritable(virtualFile, element.getProject())) {
        return false;
      }
    }
    return true;
  }
}
