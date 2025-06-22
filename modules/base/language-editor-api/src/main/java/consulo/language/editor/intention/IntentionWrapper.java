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
package consulo.language.editor.intention;

import consulo.annotation.access.RequiredReadAction;
import consulo.codeEditor.Editor;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.FileEditorManager;
import consulo.fileEditor.TextEditor;
import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.language.editor.internal.intention.ActionClassHolder;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.util.IncorrectOperationException;
import consulo.project.Project;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author angus
 * @since 2011-04-20
 */
public class IntentionWrapper implements LocalQuickFix, IntentionAction, ActionClassHolder, IntentionActionDelegate {
  private final IntentionAction myAction;

  @Deprecated
  public IntentionWrapper(@Nonnull IntentionAction action) {
    myAction = action;
  }

  public IntentionWrapper(@Nonnull IntentionAction action, @Nonnull PsiFile file) {
    myAction = action;
  }

  @Nonnull
  @Override
  public String getName() {
    return myAction.getText();
  }

  @Nonnull
  @Override
  public String getText() {
    return myAction.getText();
  }

  @Nonnull
  @Override
  public String getFamilyName() {
    return getText();
  }

  @Override
  public boolean isAvailable(@Nonnull Project project, Editor editor, PsiFile file) {
    return myAction.isAvailable(project, editor, file);
  }

  @Override
  public void invoke(@Nonnull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
    myAction.invoke(project, editor, file);
  }
  @Nullable
  @Override
  public PsiElement getElementToMakeWritable(@Nonnull PsiFile file) {
    return myAction.getElementToMakeWritable(file);
  }

  @Override
  public boolean startInWriteAction() {
    return myAction.startInWriteAction();
  }

  @Nonnull
  public IntentionAction getAction() {
    return myAction;
  }

  @Override
  @RequiredReadAction
  public void applyFix(@Nonnull Project project, @Nonnull ProblemDescriptor descriptor) {
    PsiElement element = descriptor.getPsiElement();
    PsiFile file = element == null ? null : element.getContainingFile();
    if (file != null) {
      FileEditor editor = FileEditorManager.getInstance(project).getSelectedEditor(file.getVirtualFile());
      myAction.invoke(project, editor instanceof TextEditor ? ((TextEditor)editor).getEditor() : null, file);
    }
  }

  @Nonnull
  @Override
  public Class getActionClass() {
    return getAction().getClass();
  }

  @Nonnull
  @Override
  public IntentionAction getDelegate() {
    return myAction;
  }
}

