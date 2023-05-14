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

package consulo.language.editor.util;

import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import consulo.document.util.TextRange;
import consulo.language.Language;
import consulo.language.ast.ASTNode;
import consulo.language.file.FileViewProvider;
import consulo.language.inject.InjectedLanguageManager;
import consulo.language.psi.*;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.virtualFileSystem.VFileProperty;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.*;

public class PsiUtilBase extends PsiUtilCore {
  private static final Logger LOG = Logger.getInstance(PsiUtilBase.class);
  public static final Comparator<Language> LANGUAGE_COMPARATOR = (o1, o2) -> o1.getID().compareTo(o2.getID());

  public static int getRootIndex(PsiElement root) {
    ASTNode node = root.getNode();
    while (node != null && node.getTreeParent() != null) {
      node = node.getTreeParent();
    }
    if (node != null) root = node.getPsi();
    final PsiFile containingFile = root.getContainingFile();
    FileViewProvider provider = containingFile.getViewProvider();
    Set<Language> languages = provider.getLanguages();
    if (languages.size() == 1) {
      return 0;
    }
    List<Language> array = new ArrayList<Language>(languages);
    Collections.sort(array, LANGUAGE_COMPARATOR);
    for (int i = 0; i < array.size(); i++) {
      Language language = array.get(i);
      if (provider.getPsi(language) == containingFile) return i;
    }
    throw new RuntimeException("Cannot find root for: " + root);
  }

  public static boolean isUnderPsiRoot(PsiFile root, PsiElement element) {
    PsiFile containingFile = element.getContainingFile();
    if (containingFile == root) return true;
    for (PsiFile psiRoot : root.getViewProvider().getAllFiles()) {
      if (containingFile == psiRoot) return true;
    }
    PsiLanguageInjectionHost host = InjectedLanguageManager.getInstance(root.getProject()).getInjectionHost(element);
    return host != null && isUnderPsiRoot(root, host);
  }

  @Nullable
  public static Language getLanguageInEditor(@Nonnull final Editor editor, @Nonnull final Project project) {
    return getLanguageInEditor(editor.getCaretModel().getCurrentCaret(), project);
  }

  @Nullable
  public static Language getLanguageInEditor(@Nonnull Caret caret, @Nonnull final Project project) {
    Editor editor = caret.getEditor();
    PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
    if (file == null) return null;

    int caretOffset = caret.getOffset();
    int mostProbablyCorrectLanguageOffset = caretOffset == caret.getSelectionEnd() ? caret.getSelectionStart() : caretOffset;
    PsiElement elt = getElementAtOffset(file, mostProbablyCorrectLanguageOffset);
    Language lang = findLanguageFromElement(elt);

    if (caret.hasSelection()) {
      final Language rangeLanguage = evaluateLanguageInRange(caret.getSelectionStart(), caret.getSelectionEnd(), file);
      if (rangeLanguage == null) return file.getLanguage();

      lang = rangeLanguage;
    }

    return narrowLanguage(lang, file.getLanguage());
  }

  @Nullable
  public static PsiElement getElementAtCaret(@Nonnull Editor editor) {
    Project project = editor.getProject();
    if (project == null) return null;
    PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
    return file == null ? null : file.findElementAt(editor.getCaretModel().getOffset());
  }

  @Nullable
  public static PsiFile getPsiFileInEditor(@Nonnull final Editor editor, @Nonnull final Project project) {
    return getPsiFileInEditor(editor.getCaretModel().getCurrentCaret(), project);
  }

  @Nullable
  public static PsiFile getPsiFileInEditor(@Nonnull Caret caret, @Nonnull final Project project) {
    Editor editor = caret.getEditor();
    final PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
    if (file == null) return null;

    PsiUtilCore.ensureValid(file);

    final Language language = getLanguageInEditor(caret, project);
    if (language == null) return file;

    if (language == file.getLanguage()) return file;

    int caretOffset = caret.getOffset();
    int mostProbablyCorrectLanguageOffset = caretOffset == caret.getSelectionEnd() ? caret.getSelectionStart() : caretOffset;
    return getPsiFileAtOffset(file, mostProbablyCorrectLanguageOffset);
  }

  public static PsiFile getPsiFileAtOffset(final PsiFile file, final int offset) {
    PsiElement elt = getElementAtOffset(file, offset);

    assert elt.isValid() : elt + "; file: " + file + "; isvalid: " + file.isValid();
    return elt.getContainingFile();
  }

  @Nullable
  public static Language reallyEvaluateLanguageInRange(final int start, final int end, @Nonnull PsiFile file) {
    if (file instanceof PsiBinaryFile) {
      return file.getLanguage();
    }
    Language lang = null;
    int curOffset = start;
    do {
      PsiElement elt = getElementAtOffset(file, curOffset);

      if (!(elt instanceof PsiWhiteSpace)) {
        final Language language = findLanguageFromElement(elt);
        if (lang == null) {
          lang = language;
        }
        else if (lang != language) {
          return null;
        }
      }
      TextRange range = elt.getTextRange();
      if (range == null) {
        LOG.error("Null range for element " + elt + " of " + elt.getClass() + " in file " + file + " at offset " + curOffset);
        return file.getLanguage();
      }
      int endOffset = range.getEndOffset();
      curOffset = endOffset <= curOffset ? curOffset + 1 : endOffset;
    }
    while (curOffset < end);
    return narrowLanguage(lang, file.getLanguage());
  }

  @Nullable
  public static Language evaluateLanguageInRange(final int start, final int end, @Nonnull PsiFile file) {
    PsiElement elt = getElementAtOffset(file, start);

    TextRange selectionRange = new TextRange(start, end);
    if (!(elt instanceof PsiFile)) {
      elt = elt.getParent();
      TextRange range = elt.getTextRange();
      assert range != null : "Range is null for " + elt + "; " + elt.getClass();
      while (!range.contains(selectionRange) && !(elt instanceof PsiFile)) {
        elt = elt.getParent();
        if (elt == null) break;
        range = elt.getTextRange();
        assert range != null : "Range is null for " + elt + "; " + elt.getClass();
      }

      if (elt != null) {
        return elt.getLanguage();
      }
    }

    return reallyEvaluateLanguageInRange(start, end, file);
  }

  @Nonnull
  public static ASTNode getRoot(@Nonnull ASTNode node) {
    ASTNode child = node;
    do {
      final ASTNode parent = child.getTreeParent();
      if (parent == null) return child;
      child = parent;
    }
    while (true);
  }

  /**
   * Tries to find editor for the given element.
   * <p/>
   * There are at least two approaches to achieve the target. Current method is intended to encapsulate both of them:
   * <ul>
   * <li>target editor works with a real file that remains at file system;</li>
   * <li>target editor works with a virtual file;</li>
   * </ul>
   * <p/>
   * Please don't use this method for finding an editor for quick fix.
   *
   * @param element target element
   * @return editor that works with a given element if the one is found; <code>null</code> otherwise
   * @see {@link consulo.ide.impl.idea.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement}
   */
  @Nullable
  public static Editor findEditor(@Nonnull PsiElement element) {
    return PsiEditorUtil.findEditor(element);
  }

  public static boolean isSymLink(@Nonnull final PsiFileSystemItem element) {
    final VirtualFile virtualFile = element.getVirtualFile();
    return virtualFile != null && virtualFile.is(VFileProperty.SYMLINK);
  }

  @Nullable
  public static VirtualFile asVirtualFile(@Nullable PsiElement element) {
    if (element instanceof PsiFileSystemItem) {
      PsiFileSystemItem psiFileSystemItem = (PsiFileSystemItem)element;
      return psiFileSystemItem.isValid() ? psiFileSystemItem.getVirtualFile() : null;
    }
    return null;
  }
}
