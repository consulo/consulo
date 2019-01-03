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

package com.intellij.codeInsight.actions;

import com.intellij.codeInsight.lookup.Lookup;
import com.intellij.codeInsight.lookup.LookupManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.AsyncResult;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageUtil;
import consulo.ui.RequiredUIAccess;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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
  @Nonnull
  protected AsyncResult<Editor> getEditor(@Nonnull final DataContext dataContext, @Nonnull final Project project, boolean forUpdate) {
    Editor editor = getBaseEditor(dataContext, project);
    if (!myLookForInjectedEditor) return AsyncResult.resolved(editor);
    return getInjectedEditorAsync(project, editor, !forUpdate);
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

  @RequiredUIAccess
  public static AsyncResult<Editor> getInjectedEditorAsync(@Nonnull Project project, final Editor editor) {
    return getInjectedEditorAsync(project, editor, true);
  }

  @RequiredUIAccess
  public static AsyncResult<Editor> getInjectedEditorAsync(@Nonnull Project project, final Editor editor, boolean commit) {
    if (commit) {
      AsyncResult<Editor> result = new AsyncResult<>();
      PsiDocumentManager documentManager = PsiDocumentManager.getInstance(project);
      documentManager.commitAllDocumentsAsync().doWhenDone(() -> {
        Editor injectedEditor = editor;
        if (editor != null) {
          PsiFile psiFile = documentManager.getCachedPsiFile(editor.getDocument());
          if (psiFile != null) {
            injectedEditor = InjectedLanguageUtil.getEditorForInjectedLanguageNoCommit(editor, psiFile);
          }
        }
        result.setDone(injectedEditor);
      });

      return result;
    }
    else {
      Editor injectedEditor = editor;
      if (editor != null) {
        PsiDocumentManager documentManager = PsiDocumentManager.getInstance(project);
        PsiFile psiFile = documentManager.getCachedPsiFile(editor.getDocument());
        if (psiFile != null) {
          injectedEditor = InjectedLanguageUtil.getEditorForInjectedLanguageNoCommit(editor, psiFile);
        }
      }
      return AsyncResult.resolved(injectedEditor);
    }
  }

  @Nullable
  @RequiredUIAccess
  protected Editor getBaseEditor(final DataContext dataContext, final Project project) {
    AsyncResult<Editor> editor = super.getEditor(dataContext, project, true);
    assert editor.isDone();
    return editor.getResult();
  }

  @RequiredUIAccess
  @Override
  public void update(@Nonnull AnActionEvent event) {
    Presentation presentation = event.getPresentation();
    DataContext dataContext = event.getDataContext();
    Project project = dataContext.getData(CommonDataKeys.PROJECT);
    if (project == null) {
      presentation.setEnabled(false);
      return;
    }

    final Lookup activeLookup = LookupManager.getInstance(project).getActiveLookup();
    if (activeLookup != null) {
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
