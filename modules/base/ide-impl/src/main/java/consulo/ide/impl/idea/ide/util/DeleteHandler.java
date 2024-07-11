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

package consulo.ide.impl.idea.ide.util;

import consulo.application.Application;
import consulo.codeEditor.Editor;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.ide.IdeBundle;
import consulo.ide.impl.idea.openapi.fileEditor.impl.NonProjectFileWritingAccessProvider;
import consulo.ide.impl.idea.util.io.ReadOnlyAttributeUtil;
import consulo.language.editor.refactoring.localize.RefactoringLocalize;
import consulo.language.editor.refactoring.safeDelete.SafeDeleteDialog;
import consulo.language.editor.refactoring.safeDelete.SafeDeleteProcessor;
import consulo.language.editor.refactoring.ui.RefactoringUIUtil;
import consulo.language.editor.refactoring.util.CommonRefactoringUtil;
import consulo.language.editor.refactoring.util.DeleteUtil;
import consulo.language.editor.util.PsiUtilBase;
import consulo.language.psi.*;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.language.util.IncorrectOperationException;
import consulo.localHistory.LocalHistory;
import consulo.localHistory.LocalHistoryAction;
import consulo.application.localize.ApplicationLocalize;
import consulo.platform.base.localize.CommonLocalize;
import consulo.ide.localize.IdeLocalize;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.DeleteProvider;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.MessagesEx;
import consulo.ui.ex.awt.UIUtil;
import consulo.undoRedo.CommandProcessor;
import consulo.util.collection.SmartList;
import consulo.util.lang.ref.Ref;
import consulo.virtualFileSystem.VFileProperty;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.WritingAccessProvider;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NonNls;

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
      if (dataContext.getData(Project.KEY) == null) {
        return false;
      }
      final PsiElement[] elements = getPsiElements(dataContext);
      return elements != null && shouldEnableDeleteAction(elements);
    }

    @Nullable
    private static PsiElement[] getPsiElements(DataContext dataContext) {
      PsiElement[] elements = dataContext.getData(PsiElement.KEY_OF_ARRAY);
      if (elements == null) {
        final Object data = dataContext.getData(PsiElement.KEY);
        if (data != null) {
          elements = new PsiElement[]{(PsiElement)data};
        }
        else {
          final Object data1 = dataContext.getData(PsiFile.KEY);
          if (data1 != null) {
            elements = new PsiElement[]{(PsiFile)data1};
          }
        }
      }
      return elements;
    }

    @Override
    @RequiredUIAccess
    public void deleteElement(@Nonnull DataContext dataContext) {
      PsiElement[] elements = getPsiElements(dataContext);
      if (elements == null) return;
      Project project = dataContext.getData(Project.KEY);
      if (project == null) return;
      LocalHistoryAction a = LocalHistory.getInstance().startAction(IdeLocalize.progressDeleting().get());
      try {
        deletePsiElement(elements, project);
      }
      finally {
        a.finish();
      }
    }
  }

  @RequiredUIAccess
  public static void deletePsiElement(final PsiElement[] elementsToDelete, final Project project) {
    deletePsiElement(elementsToDelete, project, true);
  }

  @NonNls
  @RequiredUIAccess
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
      final SafeDeleteDialog dialog = new SafeDeleteDialog(
        project,
        elements,
        (SafeDeleteDialog.Callback) dialog1 -> {
          if (!CommonRefactoringUtil.checkReadOnlyStatusRecursively(project, Arrays.asList(elements), true)) return;

          SafeDeleteProcessor processor = SafeDeleteProcessor.createInstance(
            project,
            () -> {
              exit.set(true);
              dialog1.close(DialogWrapper.OK_EXIT_CODE);
            },
            elements,
            dialog1.isSearchInComments(),
            dialog1.isSearchForTextOccurences(),
            true
          );

          processor.run();
        }
      ) {
        @Override
        protected boolean isDelete() {
          return true;
        }
      };
      if (needConfirmation) {
        dialog.setTitle(RefactoringLocalize.deleteTitle());
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
        if (psiElement instanceof PsiDirectory psiDirectory && !PsiUtilBase.isSymLink(psiDirectory)) {
          anyDirectories = true;
          directoryName = psiDirectory.getName();
          break;
        }
      }
      if (anyDirectories) {
        if (elements.length == 1) {
          warningMessage += IdeLocalize.warningDeleteAllFilesAndSubdirectories(directoryName);
        }
        else {
          warningMessage += IdeLocalize.warningDeleteAllFilesAndSubdirectoriesInTheSelectedDirectory();
        }
      }

      if (safeDeleteApplicable && dumb) {
        warningMessage +=
          "\n\nWarning:\n" +
            "  Safe delete is not available while " + Application.get().getName() + " updates indices,\n" +
            "  no usages will be checked.";
      }

      if (needConfirmation) {
        int result = Messages.showOkCancelDialog(
          project,
          warningMessage,
          IdeLocalize.titleDelete().get(),
          ApplicationLocalize.buttonDelete().get(),
          CommonLocalize.buttonCancel().get(),
          UIUtil.getQuestionIcon()
        );
        if (result != Messages.OK) return;
      }
    }

    CommandProcessor.getInstance().executeCommand(project, () -> NonProjectFileWritingAccessProvider.disableChecksDuring(() -> {
      Collection<PsiElement> directories = new SmartList<>();
      for (PsiElement e : elements) {
        if (e instanceof PsiFileSystemItem && e.getParent() != null) {
          directories.add(e.getParent());
        }
      }

      if (!CommonRefactoringUtil.checkReadOnlyStatus(project, Arrays.asList(elements), directories, false)) {
        return;
      }

      // deleted from project view or something like that.
      if (DataManager.getInstance().getDataContext().getData(Editor.KEY) == null) {
        CommandProcessor.getInstance().markCurrentCommandAsGlobal(project);
      }

      for (final PsiElement elementToDelete : elements) {
        if (!elementToDelete.isValid()) continue; //was already deleted
        if (elementToDelete instanceof PsiDirectory directory) {
          VirtualFile virtualFile = directory.getVirtualFile();
          if (virtualFile.isInLocalFileSystem() && !virtualFile.is(VFileProperty.SYMLINK)) {
            ArrayList<VirtualFile> readOnlyFiles = new ArrayList<>();
            CommonRefactoringUtil.collectReadOnlyFiles(virtualFile, readOnlyFiles);

            if (!readOnlyFiles.isEmpty()) {
              String message = IdeLocalize.promptDirectoryContainsReadOnlyFiles(virtualFile.getPresentableUrl()).get();
              int _result = Messages.showYesNoDialog(project, message, IdeLocalize.titleDelete().get(), UIUtil.getQuestionIcon());
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
        else if (!elementToDelete.isWritable()
          && !(elementToDelete instanceof PsiFileSystemItem psiFileSystemItem && PsiUtilBase.isSymLink(psiFileSystemItem))) {
          final PsiFile file = elementToDelete.getContainingFile();
          if (file != null) {
            final VirtualFile virtualFile = file.getVirtualFile();
            if (virtualFile.isInLocalFileSystem()) {
              int _result = MessagesEx.fileIsReadOnly(project, virtualFile)
                .setTitle(IdeLocalize.titleDelete().get())
                .appendMessage(" " + IdeLocalize.promptDeleteItAnyway().get())
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
          Messages.showMessageDialog(project, ex.getMessage(), CommonLocalize.titleError().get(), UIUtil.getErrorIcon());
          continue;
        }

        project.getApplication().runWriteAction(() -> {
          try {
            elementToDelete.delete();
          }
          catch (final IncorrectOperationException ex) {
            project.getApplication().invokeLater(
              () -> Messages.showMessageDialog(project, ex.getMessage(), CommonLocalize.titleError().get(), UIUtil.getErrorIcon()));
          }
        });
      }
    }), RefactoringLocalize.safeDeleteCommand(RefactoringUIUtil.calculatePsiElementDescriptionList(elements)).get(), null);
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
          Messages.showMessageDialog(project, e1.getMessage(), CommonLocalize.titleError().get(), UIUtil.getErrorIcon());
        }
      };
      project.getApplication().runWriteAction(action);
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
