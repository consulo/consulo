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

package consulo.language.editor.refactoring.rename;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.Application;
import consulo.codeEditor.Editor;
import consulo.codeEditor.ScrollType;
import consulo.dataContext.DataContext;
import consulo.externalService.statistic.FeatureUsageTracker;
import consulo.language.editor.refactoring.RefactoringBundle;
import consulo.language.editor.refactoring.action.BaseRefactoringAction;
import consulo.language.editor.refactoring.internal.RefactoringInternalHelper;
import consulo.language.editor.refactoring.localize.RefactoringLocalize;
import consulo.language.editor.refactoring.util.CommonRefactoringUtil;
import consulo.language.editor.scratch.ScratchUtil;
import consulo.language.inject.InjectedLanguageManager;
import consulo.language.inject.InjectedLanguageManagerUtil;
import consulo.language.psi.*;
import consulo.language.psi.meta.PsiMetaOwner;
import consulo.language.psi.meta.PsiWritableMetaData;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.UIUtil;
import consulo.usage.UsageViewUtil;
import consulo.util.dataholder.Key;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Arrays;

/**
 * created at Nov 13, 2001
 *
 * @author Jeka, dsl
 */
public class PsiElementRenameHandler implements RenameHandler {
  private static final Logger LOG = Logger.getInstance(PsiElementRenameHandler.class);

  public static Key<String> DEFAULT_NAME = Key.create("DEFAULT_NAME");

  @Override
  @RequiredUIAccess
  public void invoke(@Nonnull Project project, Editor editor, PsiFile file, DataContext dataContext) {
    PsiElement element = getElement(dataContext);
    if (element == null) {
      element = BaseRefactoringAction.getElementAtCaret(editor, file);
    }

    if (project.getApplication().isUnitTestMode()) {
      final String newName = dataContext.getData(DEFAULT_NAME);
      if (newName != null) {
        rename(element, project, element, editor, newName);
        return;
      }
    }

    editor.getScrollingModel().scrollToCaret(ScrollType.MAKE_VISIBLE);
    final PsiElement nameSuggestionContext =
      InjectedLanguageManager.getInstance(element.getProject()).findElementAtNoCommit(file, editor.getCaretModel().getOffset());
    invoke(element, project, nameSuggestionContext, editor);
  }

  @Override
  @RequiredUIAccess
  public void invoke(@Nonnull Project project, @Nonnull PsiElement[] elements, DataContext dataContext) {
    PsiElement element = elements.length == 1 ? elements[0] : null;
    if (element == null) element = getElement(dataContext);
    LOG.assertTrue(element != null);
    Editor editor = dataContext.getData(Editor.KEY);
    if (project.getApplication().isUnitTestMode()) {
      final String newName = dataContext.getData(DEFAULT_NAME);
      LOG.assertTrue(newName != null);
      rename(element, project, element, editor, newName);
    }
    else {
      invoke(element, project, element, editor);
    }
  }

  @RequiredUIAccess
  public static void invoke(PsiElement element, Project project, PsiElement nameSuggestionContext, @Nullable Editor editor) {
    if (element != null && !canRename(project, editor, element)) {
      return;
    }

    VirtualFile contextFile = PsiUtilCore.getVirtualFile(nameSuggestionContext);

    if (nameSuggestionContext != null && nameSuggestionContext.isPhysical() &&
        (contextFile == null || !ScratchUtil.isScratch(contextFile) && !PsiManager.getInstance(project).isInProject(nameSuggestionContext))) {
      final String message = "Selected element is used from non-project files. These usages won't be renamed. Proceed anyway?";
      if (project.getApplication().isUnitTestMode()) throw new CommonRefactoringUtil.RefactoringErrorHintException(message);
      int buttonPressed =
        Messages.showYesNoDialog(project, message, RefactoringBundle.getCannotRefactorMessage(null), UIUtil.getWarningIcon());
      if (buttonPressed != Messages.YES) {
        return;
      }
    }

    FeatureUsageTracker.getInstance().triggerFeatureUsed("refactoring.rename");

    rename(element, project, nameSuggestionContext, editor);
  }

  @RequiredUIAccess
  public static boolean canRename(Project project, Editor editor, PsiElement element)
    throws CommonRefactoringUtil.RefactoringErrorHintException {
    String message = renameabilityStatus(project, element);
    if (StringUtil.isNotEmpty(message)) {
      showErrorMessage(project, editor, message);
      return false;
    }
    return true;
  }

  @Nullable
  @RequiredReadAction
  static String renameabilityStatus(Project project, PsiElement element) {
    if (element == null) return "";

    boolean hasRenameProcessor = RenamePsiElementProcessor.forElement(element) != RenamePsiElementProcessor.DEFAULT;
    boolean hasWritableMetaData = element instanceof PsiMetaOwner metaOwner && metaOwner.getMetaData() instanceof PsiWritableMetaData;

    if (!hasRenameProcessor && !hasWritableMetaData && !(element instanceof PsiNamedElement)) {
      return RefactoringBundle.getCannotRefactorMessage(RefactoringLocalize.errorWrongCaretPositionSymbolToRename().get());
    }

    if (!PsiManager.getInstance(project).isInProject(element)) {
      if (element.isPhysical()) {
        VirtualFile virtualFile = PsiUtilCore.getVirtualFile(element);
        if (!(virtualFile != null && RefactoringInternalHelper.getInstance().isWriteAccessAllowed(virtualFile, project))) {
          LocalizeValue message = RefactoringLocalize.errorOutOfProjectElement(UsageViewUtil.getType(element));
          return RefactoringBundle.getCannotRefactorMessage(message.get());
        }
      }

      if (!element.isWritable()) {
        return RefactoringBundle.getCannotRefactorMessage(RefactoringLocalize.errorCannotBeRenamed().get());
      }
    }

    if (InjectedLanguageManagerUtil.isInInjectedLanguagePrefixSuffix(element)) {
      final LocalizeValue message = RefactoringLocalize.errorInInjectedLangPrefixSuffix(UsageViewUtil.getType(element));
      return RefactoringBundle.getCannotRefactorMessage(message.get());
    }

    return null;
  }

  @RequiredUIAccess
  static void showErrorMessage(Project project, @Nullable Editor editor, String message) {
    CommonRefactoringUtil.showErrorHint(project, editor, message, RefactoringLocalize.renameTitle().get(), null);
  }

  @RequiredUIAccess
  public static void rename(PsiElement element, final Project project, PsiElement nameSuggestionContext, Editor editor) {
    rename(element, project, nameSuggestionContext, editor, null);
  }

  @RequiredUIAccess
  public static void rename(
    PsiElement element,
    final Project project,
    PsiElement nameSuggestionContext,
    Editor editor,
    String defaultName
  ) {
    RenamePsiElementProcessor processor = RenamePsiElementProcessor.forElement(element);
    PsiElement substituted = processor.substituteElementToRename(element, editor);
    if (substituted == null || !canRename(project, editor, substituted)) return;

    RenameDialog dialog = processor.createRenameDialog(project, substituted, nameSuggestionContext, editor);

    if (defaultName == null && project.getApplication().isUnitTestMode()) {
      String[] strings = dialog.getSuggestedNames();
      if (strings != null && strings.length > 0) {
        Arrays.sort(strings);
        defaultName = strings[0];
      }
      else {
        defaultName = "undefined"; // need to avoid show dialog in test
      }
    }

    if (defaultName != null) {
      try {
        dialog.performRename(defaultName);
      }
      finally {
        dialog.close(DialogWrapper.CANCEL_EXIT_CODE); // to avoid dialog leak
      }
    }
    else {
      dialog.show();
    }
  }

  @Override
  @RequiredReadAction
  public boolean isAvailableOnDataContext(DataContext dataContext) {
    return !isVetoed(getElement(dataContext));
  }

  public static boolean isVetoed(PsiElement element) {
    return element == null || element instanceof SyntheticElement
      || VetoRenameCondition.EP.findFirstSafe(Application.get(), it -> it.isVetoed(element)) != null;
  }

  @Nullable
  public static PsiElement getElement(final DataContext dataContext) {
    PsiElement[] elementArray = BaseRefactoringAction.getPsiElementArray(dataContext);

    if (elementArray.length != 1) {
      return null;
    }
    return elementArray[0];
  }

  @Override
  @RequiredReadAction
  public boolean isRenaming(DataContext dataContext) {
    return isAvailableOnDataContext(dataContext);
  }
}
