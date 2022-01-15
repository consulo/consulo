/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

package com.intellij.openapi.fileEditor.impl.text;

import com.intellij.ide.IdeView;
import com.intellij.ide.util.EditorHelper;
import com.intellij.injected.editor.EditorWindow;
import com.intellij.lang.Language;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.fileEditor.EditorDataProvider;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import consulo.util.dataholder.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.tree.injected.InjectedCaret;
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageUtil;
import com.intellij.psi.util.PsiUtilCore;
import consulo.codeInsight.TargetElementUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.LinkedHashSet;

import static com.intellij.openapi.actionSystem.AnActionEvent.injectedId;
import static com.intellij.openapi.actionSystem.LangDataKeys.*;
import static com.intellij.util.containers.ContainerUtil.addIfNotNull;

public class TextEditorPsiDataProvider implements EditorDataProvider {
  @Override
  @Nullable
  public Object getData(@Nonnull final Key<?> dataId, @Nonnull final Editor e, @Nonnull final Caret caret) {
    if (!(e instanceof EditorEx)) {
      return null;
    }
    VirtualFile file = ((EditorEx)e).getVirtualFile();
    if (file == null || !file.isValid()) return null;

    Project project = e.getProject();
    if (dataId == injectedId(EDITOR)) {
      if (project == null || PsiDocumentManager.getInstance(project).isUncommited(e.getDocument())) {
        return e;
      }
      else {
        return InjectedLanguageUtil.getEditorForInjectedLanguageNoCommit(e, caret, getPsiFile(e, file));
      }
    }
    if (HOST_EDITOR == dataId) {
      return e instanceof EditorWindow ? ((EditorWindow)e).getDelegate() : e;
    }
    if (CARET == dataId) {
      return caret;
    }
    if (dataId == injectedId(CARET)) {
      Editor editor = (Editor)getData(injectedId(EDITOR), e, caret);
      assert editor != null;
      return getInjectedCaret(editor, caret);
    }
    if (dataId == injectedId(PSI_ELEMENT)) {
      Editor editor = (Editor)getData(injectedId(EDITOR), e, caret);
      assert editor != null;
      Caret injectedCaret = getInjectedCaret(editor, caret);
      return getPsiElementIn(editor, injectedCaret, file);
    }
    if (PSI_ELEMENT == dataId){
      return getPsiElementIn(e, caret, file);
    }
    if (dataId == injectedId(LANGUAGE)) {
      PsiFile psiFile = (PsiFile)getData(injectedId(PSI_FILE), e, caret);
      Editor editor = (Editor)getData(injectedId(EDITOR), e, caret);
      if (psiFile == null || editor == null) return null;
      Caret injectedCaret = getInjectedCaret(editor, caret);
      return getLanguageAtCurrentPositionInEditor(injectedCaret, psiFile);
    }
    if (LANGUAGE == dataId) {
      final PsiFile psiFile = getPsiFile(e, file);
      if (psiFile == null) return null;
      return getLanguageAtCurrentPositionInEditor(caret, psiFile);
    }
    if (dataId == injectedId(VIRTUAL_FILE)) {
      PsiFile psiFile = (PsiFile)getData(injectedId(PSI_FILE), e, caret);
      if (psiFile == null) return null;
      return psiFile.getVirtualFile();
    }
    if (dataId == injectedId(PSI_FILE)) {
      Editor editor = (Editor)getData(injectedId(EDITOR), e, caret);
      if (editor == null) {
        return null;
      }
      if (project == null) {
        return null;
      }
      return PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
    }
    if (PSI_FILE == dataId) {
      return getPsiFile(e, file);
    }
    if (IDE_VIEW == dataId) {
      final PsiFile psiFile = project == null ? null : PsiManager.getInstance(project).findFile(file);
      final PsiDirectory psiDirectory = psiFile != null ? psiFile.getParent() : null;
      if (psiDirectory != null && psiDirectory.isPhysical()) {
        return new IdeView() {

          @Override
          public void selectElement(final PsiElement element) {
            Editor editor = EditorHelper.openInEditor(element);
            if (editor != null) {
              ToolWindowManager.getInstance(element.getProject()).activateEditorComponent();
            }
          }

          @Nonnull
          @Override
          public PsiDirectory[] getDirectories() {
            return new PsiDirectory[]{psiDirectory};
          }

          @Override
          public PsiDirectory getOrChooseDirectory() {
            return psiDirectory;
          }
        };
      }
    }
    if (CONTEXT_LANGUAGES == dataId) {
      return computeLanguages(e, caret);
    }
    return null;
  }

  @Nonnull
  private static Caret getInjectedCaret(@Nonnull Editor editor, @Nonnull Caret hostCaret) {
    if (!(editor instanceof EditorWindow) || hostCaret instanceof InjectedCaret) {
      return hostCaret;
    }
    for (Caret caret : editor.getCaretModel().getAllCarets()) {
      if (((InjectedCaret)caret).getDelegate() == hostCaret) {
        return caret;
      }
    }
    throw new IllegalArgumentException("Cannot find injected caret corresponding to " + hostCaret);
  }

  private static Language getLanguageAtCurrentPositionInEditor(Caret caret, final PsiFile psiFile) {
    int caretOffset = caret.getOffset();
    int mostProbablyCorrectLanguageOffset = caretOffset == caret.getSelectionStart() ||
                                            caretOffset == caret.getSelectionEnd()
                                            ? caret.getSelectionStart()
                                            : caretOffset;
    if (caret.hasSelection()) {
      return getLanguageAtOffset(psiFile, mostProbablyCorrectLanguageOffset, caret.getSelectionEnd());
    }

    return PsiUtilCore.getLanguageAtOffset(psiFile, mostProbablyCorrectLanguageOffset);
  }

  private static Language getLanguageAtOffset(PsiFile psiFile, int mostProbablyCorrectLanguageOffset, int end) {
    final PsiElement elt = psiFile.findElementAt(mostProbablyCorrectLanguageOffset);
    if (elt == null) return psiFile.getLanguage();
    if (elt instanceof PsiWhiteSpace) {
      final int incremented = elt.getTextRange().getEndOffset() + 1;
      if (incremented <= end) {
        return getLanguageAtOffset(psiFile, incremented, end);
      }
    }
    return PsiUtilCore.findLanguageFromElement(elt);
  }

  @Nullable
  private static PsiElement getPsiElementIn(@Nonnull Editor editor, @Nonnull Caret caret, @Nonnull VirtualFile file) {
    final PsiFile psiFile = getPsiFile(editor, file);
    if (psiFile == null) return null;

    try {
      return TargetElementUtil.findTargetElement(editor, TargetElementUtil.getReferenceSearchFlags(), caret.getOffset());
    }
    catch (IndexNotReadyException e) {
      return null;
    }
  }

  @Nullable
  private static PsiFile getPsiFile(@Nonnull Editor e, @Nonnull VirtualFile file) {
    if (!file.isValid()) {
      return null; // fix for SCR 40329
    }
    final Project project = e.getProject();
    if (project == null) {
      return null;
    }
    PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
    return psiFile != null && psiFile.isValid() ? psiFile : null;
  }

  private Language[] computeLanguages(@Nonnull Editor editor, @Nonnull Caret caret) {
    LinkedHashSet<Language> set = new LinkedHashSet<>(4);
    Language injectedLanguage = (Language)getData(injectedId(LANGUAGE), editor, caret);
    addIfNotNull(set, injectedLanguage);
    Language language = (Language)getData(LANGUAGE, editor, caret);
    addIfNotNull(set, language);
    PsiFile psiFile = (PsiFile)getData(PSI_FILE, editor, caret);
    if (psiFile != null) {
      addIfNotNull(set, psiFile.getViewProvider().getBaseLanguage());
    }
    return set.toArray(new Language[set.size()]);
  }
}
