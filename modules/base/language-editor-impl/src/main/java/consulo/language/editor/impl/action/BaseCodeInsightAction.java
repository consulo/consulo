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

package consulo.language.editor.impl.action;

import consulo.codeEditor.Editor;
import consulo.dataContext.DataContext;
import consulo.language.editor.action.CodeInsightAction;
import consulo.language.editor.completion.lookup.Lookup;
import consulo.language.editor.completion.lookup.LookupManager;
import consulo.language.inject.impl.internal.InjectedLanguageUtil;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public abstract class BaseCodeInsightAction extends CodeInsightAction {
  private final boolean myLookForInjectedEditor;

  protected BaseCodeInsightAction() {
    this(true);
  }

  protected BaseCodeInsightAction(boolean lookForInjectedEditor) {
    myLookForInjectedEditor = lookForInjectedEditor;
  }

  @RequiredUIAccess
  @Override
  @Nullable
  protected Editor getEditor(@Nonnull final DataContext dataContext, @Nonnull final Project project, boolean forUpdate) {
    Editor editor = getBaseEditor(dataContext, project);
    if (!myLookForInjectedEditor) return editor;
    return getInjectedEditor(project, editor, !forUpdate);
  }

  @RequiredUIAccess
  public static Editor getInjectedEditor(@Nonnull Project project, final Editor editor) {
    return getInjectedEditor(project, editor, true);
  }

  @RequiredUIAccess
  public static Editor getInjectedEditor(@Nonnull Project project, final Editor editor, boolean commit) {
    Editor injectedEditor = editor;
    if (editor != null) {
      PsiDocumentManager documentManager = PsiDocumentManager.getInstance(project);
      PsiFile psiFile = documentManager.getCachedPsiFile(editor.getDocument());
      if (psiFile != null) {
        if (commit) documentManager.commitAllDocuments();
        injectedEditor = InjectedLanguageUtil.getEditorForInjectedLanguageNoCommit(editor, psiFile);
      }
    }
    return injectedEditor;
  }

  @Nullable
  protected Editor getBaseEditor(final DataContext dataContext, final Project project) {
    return super.getEditor(dataContext, project, true);
  }

  @Override
  public void update(@Nonnull AnActionEvent event) {
    Presentation presentation = event.getPresentation();
    DataContext dataContext = event.getDataContext();
    Project project = dataContext.getData(Project.KEY);
    if (project == null){
      presentation.setEnabled(false);
      return;
    }

    final Lookup activeLookup = LookupManager.getInstance(project).getActiveLookup();
    if (activeLookup != null){
      presentation.setEnabled(isValidForLookup());
    }
    else {
      super.update(event);
    }
  }

  protected boolean isValidForLookup() {
    return false;
  }
}
