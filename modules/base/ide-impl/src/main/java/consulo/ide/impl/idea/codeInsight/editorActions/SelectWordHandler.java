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

package consulo.ide.impl.idea.codeInsight.editorActions;

import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.codeEditor.action.EditorActionHandler;
import consulo.codeEditor.action.ExtensionEditorActionHandler;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.document.Document;
import consulo.document.util.TextRange;
import consulo.externalService.statistic.FeatureUsageTracker;
import consulo.language.CompositeLanguage;
import consulo.language.Language;
import consulo.language.editor.action.SelectWordUtil;
import consulo.language.editor.inject.EditorWindow;
import consulo.language.inject.InjectedLanguageManager;
import consulo.language.psi.*;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.ex.action.IdeActions;
import consulo.util.lang.StringUtil;
import consulo.util.lang.ref.Ref;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

@ExtensionImpl(order = "first")
public class SelectWordHandler extends EditorActionHandler implements ExtensionEditorActionHandler {
  private static final Logger LOG = Logger.getInstance(SelectWordHandler.class);

  private EditorActionHandler myOriginalHandler;

  public SelectWordHandler() {
    super(true);
  }

  @Override
  public void execute(@Nonnull Editor editor, DataContext dataContext) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("enter: execute(editor='" + editor + "')");
    }
    Project project = DataManager.getInstance().getDataContext(editor.getComponent()).getData(Project.KEY);
    if (project == null) {
      if (myOriginalHandler != null) {
        myOriginalHandler.execute(editor, dataContext);
      }
      return;
    }
    PsiDocumentManager.getInstance(project).commitAllDocuments();

    TextRange range = selectWord(editor, project);
    if (editor instanceof EditorWindow) {
      if (range == null || !isInsideEditableInjection((EditorWindow)editor, range, project) || TextRange.from(0, editor.getDocument().getTextLength()).equals(
              new TextRange(editor.getSelectionModel().getSelectionStart(), editor.getSelectionModel().getSelectionEnd()))) {
        editor = ((EditorWindow)editor).getDelegate();
        range = selectWord(editor, project);
      }
    }
    if (range == null) {
      if (myOriginalHandler != null) {
        myOriginalHandler.execute(editor, dataContext);
      }
    }
    else {
      editor.getSelectionModel().setSelection(range.getStartOffset(), range.getEndOffset());
    }
  }

  private static boolean isInsideEditableInjection(EditorWindow editor, TextRange range, Project project) {
    PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
    if (file == null) return true;
    List<TextRange> editables = InjectedLanguageManager.getInstance(project).intersectWithAllEditableFragments(file, range);

    return editables.size() == 1 && range.equals(editables.get(0));
  }

  /**
   *
   * @param editor
   * @param project
   * @return null means unable to select
   */
  @Nullable
  private static TextRange selectWord(@Nonnull Editor editor, @Nonnull Project project) {
    Document document = editor.getDocument();
    PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(document);
    if (file instanceof PsiCompiledFile) {
      file = ((PsiCompiledFile)file).getDecompiledPsiFile();
    }
    if (file == null) return null;

    FeatureUsageTracker.getInstance().triggerFeatureUsed("editing.select.word");

    int caretOffset = adjustCaretOffset(editor);

    PsiElement element = findElementAt(file, caretOffset);

    if (element instanceof PsiWhiteSpace && caretOffset > 0) {
      PsiElement anotherElement = findElementAt(file, caretOffset - 1);

      if (!(anotherElement instanceof PsiWhiteSpace)) {
        element = anotherElement;
      }
    }

    while (element instanceof PsiWhiteSpace || element != null && StringUtil.isEmptyOrSpaces(element.getText())) {
      while (element.getNextSibling() == null) {
        if (element instanceof PsiFile) return null;
        PsiElement parent = element.getParent();
        PsiElement[] children = parent.getChildren();

        if (children.length > 0 && children[children.length - 1] == element) {
          element = parent;
        }
        else {
          element = parent;
          break;
        }
      }

      element = element.getNextSibling();
      if (element == null) return null;
      TextRange range = element.getTextRange();
      if (range == null) return null; // Fix NPE (EA-29110)
      caretOffset = range.getStartOffset();
    }

    if (element instanceof OuterLanguageElement) {
      PsiElement elementInOtherTree = file.getViewProvider().findElementAt(element.getTextOffset(), element.getLanguage());
      if (elementInOtherTree == null || elementInOtherTree.getContainingFile() != element.getContainingFile()) {
        while (elementInOtherTree != null && elementInOtherTree.getPrevSibling() == null) {
          elementInOtherTree = elementInOtherTree.getParent();
        }

        if (elementInOtherTree != null) {
          assert elementInOtherTree.getTextOffset() == caretOffset;
          element = elementInOtherTree;
        }
      }
    }

    TextRange selectionRange = new TextRange(editor.getSelectionModel().getSelectionStart(), editor.getSelectionModel().getSelectionEnd());

    Ref<TextRange> minimumRange = new Ref<>(new TextRange(0, editor.getDocument().getTextLength()));

    SelectWordUtil.processRanges(element, editor.getDocument().getCharsSequence(), caretOffset, editor, range -> {
      if (range.contains(selectionRange) && !range.equals(selectionRange)) {
        if (minimumRange.get().contains(range)) {
          minimumRange.set(range);
          return true;
        }
      }
      return false;
    });

    return minimumRange.get();
  }

  private static int adjustCaretOffset(@Nonnull Editor editor) {
    int caretOffset = editor.getCaretModel().getOffset();
    if (caretOffset == 0) {
      return caretOffset;
    }

    CharSequence text = editor.getDocument().getCharsSequence();
    char prev = text.charAt(caretOffset - 1);
    if (caretOffset < text.length() &&
        !Character.isJavaIdentifierPart(text.charAt(caretOffset)) && Character.isJavaIdentifierPart(prev)) {
      return caretOffset - 1;
    }
    if ((caretOffset == text.length() || Character.isWhitespace(text.charAt(caretOffset))) && !Character.isWhitespace(prev)) {
      return caretOffset - 1;
    }
    return caretOffset;
  }

  @Nullable
  private static PsiElement findElementAt(@Nonnull PsiFile file, int caretOffset) {
    PsiElement elementAt = file.findElementAt(caretOffset);
    if (elementAt != null && isLanguageExtension(file, elementAt)) {
      return file.getViewProvider().findElementAt(caretOffset, file.getLanguage());
    }
    return elementAt;
  }

  private static boolean isLanguageExtension(@Nonnull PsiFile file, @Nonnull PsiElement elementAt) {
    Language elementLanguage = elementAt.getLanguage();
    if (file.getLanguage() instanceof CompositeLanguage) {
      CompositeLanguage compositeLanguage = (CompositeLanguage) file.getLanguage();
      Language[] extensions = compositeLanguage.getLanguageExtensionsForFile(file);
      for(Language extension: extensions) {
        if (extension == elementLanguage) {
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public void init(@Nullable EditorActionHandler originalHandler) {
    myOriginalHandler = originalHandler;
  }

  @Nonnull
  @Override
  public String getActionId() {
    return IdeActions.ACTION_EDITOR_SELECT_WORD_AT_CARET;
  }
}