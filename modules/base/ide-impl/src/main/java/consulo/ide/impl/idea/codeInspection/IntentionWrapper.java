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
package consulo.ide.impl.idea.codeInspection;

import consulo.language.editor.intention.IntentionAction;
import consulo.ide.impl.idea.codeInsight.intention.IntentionActionDelegate;
import consulo.codeEditor.Editor;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.FileEditorManager;
import consulo.fileEditor.TextEditor;
import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.util.IncorrectOperationException;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Created by IntelliJ IDEA.
 * User: angus
 * Date: 4/20/11
 * Time: 9:27 PM
 */
public class IntentionWrapper implements LocalQuickFix, IntentionAction, ActionClassHolder, IntentionActionDelegate {
  private final IntentionAction myAction;
  private final PsiFile myFile;

  public IntentionWrapper(@Nonnull IntentionAction action, @Nonnull PsiFile file) {
    myAction = action;
    myFile = file;
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
    return myAction.getFamilyName();
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
  public void applyFix(@Nonnull Project project, @Nonnull ProblemDescriptor descriptor) {
    VirtualFile virtualFile = myFile.getVirtualFile();

    if (virtualFile != null) {
      FileEditor editor = FileEditorManager.getInstance(project).getSelectedEditor(virtualFile);
      myAction.invoke(project, editor instanceof TextEditor ? ((TextEditor) editor).getEditor() : null, myFile);
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

