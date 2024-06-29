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

package consulo.ide.impl.idea.openapi.fileEditor.impl.text;

import consulo.annotation.access.RequiredReadAction;
import consulo.codeEditor.EditorKeys;
import consulo.ide.IdeView;
import consulo.language.editor.LangDataKeys;
import consulo.language.editor.util.EditorHelper;
import consulo.language.editor.inject.EditorWindow;
import consulo.language.Language;
import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorEx;
import consulo.fileEditor.EditorDataProvider;
import consulo.language.psi.*;
import consulo.application.dumb.IndexNotReadyException;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.VirtualFile;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.language.inject.impl.internal.InjectedCaret;
import consulo.language.inject.impl.internal.InjectedLanguageUtil;
import consulo.language.psi.PsiUtilCore;
import consulo.language.editor.TargetElementUtil;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.LinkedHashSet;

import static consulo.ui.ex.action.AnActionEvent.injectedId;
import static consulo.ide.impl.idea.util.containers.ContainerUtil.addIfNotNull;

public class TextEditorPsiDataProvider implements EditorDataProvider {
  @Override
  @Nullable
  @RequiredReadAction
  public Object getData(@Nonnull final Key<?> dataId, @Nonnull final Editor e, @Nonnull final Caret caret) {
    if (!(e instanceof EditorEx)) {
      return null;
    }
    VirtualFile file = ((EditorEx)e).getVirtualFile();
    if (file == null || !file.isValid()) return null;

    Project project = e.getProject();
    if (dataId == injectedId(Editor.KEY)) {
      if (project == null || PsiDocumentManager.getInstance(project).isUncommited(e.getDocument())) {
        return e;
      }
      else {
        return InjectedLanguageUtil.getEditorForInjectedLanguageNoCommit(e, caret, getPsiFile(e, file));
      }
    }
    if (EditorKeys.HOST_EDITOR == dataId) {
      return e instanceof EditorWindow editorWindow ? editorWindow.getDelegate() : e;
    }
    if (Caret.KEY == dataId) {
      return caret;
    }
    if (dataId == injectedId(Caret.KEY)) {
      Editor editor = (Editor)getData(injectedId(Editor.KEY), e, caret);
      assert editor != null;
      return getInjectedCaret(editor, caret);
    }
    if (dataId == injectedId(PsiElement.KEY)) {
      Editor editor = (Editor)getData(injectedId(Editor.KEY), e, caret);
      assert editor != null;
      Caret injectedCaret = getInjectedCaret(editor, caret);
      return getPsiElementIn(editor, injectedCaret, file);
    }
    if (PsiElement.KEY == dataId){
      return getPsiElementIn(e, caret, file);
    }
    if (dataId == injectedId(Language.KEY)) {
      PsiFile psiFile = (PsiFile)getData(injectedId(PsiFile.KEY), e, caret);
      Editor editor = (Editor)getData(injectedId(Editor.KEY), e, caret);
      if (psiFile == null || editor == null) return null;
      Caret injectedCaret = getInjectedCaret(editor, caret);
      return getLanguageAtCurrentPositionInEditor(injectedCaret, psiFile);
    }
    if (Language.KEY == dataId) {
      final PsiFile psiFile = getPsiFile(e, file);
      if (psiFile == null) return null;
      return getLanguageAtCurrentPositionInEditor(caret, psiFile);
    }
    if (dataId == injectedId(VirtualFile.KEY)) {
      PsiFile psiFile = (PsiFile)getData(injectedId(PsiFile.KEY), e, caret);
      if (psiFile == null) return null;
      return psiFile.getVirtualFile();
    }
    if (dataId == injectedId(PsiFile.KEY)) {
      Editor editor = (Editor)getData(injectedId(Editor.KEY), e, caret);
      if (editor == null) {
        return null;
      }
      if (project == null) {
        return null;
      }
      return PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
    }
    if (PsiFile.KEY == dataId) {
      return getPsiFile(e, file);
    }
    if (IdeView.KEY == dataId) {
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
    if (LangDataKeys.CONTEXT_LANGUAGES == dataId) {
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

  @RequiredReadAction
  private static Language getLanguageAtCurrentPositionInEditor(Caret caret, final PsiFile psiFile) {
    int caretOffset = caret.getOffset();
    int mostProbablyCorrectLanguageOffset =
      caretOffset == caret.getSelectionStart() || caretOffset == caret.getSelectionEnd() ? caret.getSelectionStart() : caretOffset;
    if (caret.hasSelection()) {
      return getLanguageAtOffset(psiFile, mostProbablyCorrectLanguageOffset, caret.getSelectionEnd());
    }

    return PsiUtilCore.getLanguageAtOffset(psiFile, mostProbablyCorrectLanguageOffset);
  }

  @RequiredReadAction
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
  @RequiredReadAction
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
  @RequiredReadAction
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

  @RequiredReadAction
  private Language[] computeLanguages(@Nonnull Editor editor, @Nonnull Caret caret) {
    LinkedHashSet<Language> set = new LinkedHashSet<>(4);
    Language injectedLanguage = (Language)getData(injectedId(Language.KEY), editor, caret);
    addIfNotNull(set, injectedLanguage);
    Language language = (Language)getData(Language.KEY, editor, caret);
    addIfNotNull(set, language);
    PsiFile psiFile = (PsiFile)getData(PsiFile.KEY, editor, caret);
    if (psiFile != null) {
      addIfNotNull(set, psiFile.getViewProvider().getBaseLanguage());
    }
    return set.toArray(new Language[set.size()]);
  }
}
