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

package consulo.language.editor.action;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.codeEditor.Editor;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.document.Document;
import consulo.document.RangeMarker;
import consulo.document.util.TextRange;
import consulo.language.Language;
import consulo.language.codeStyle.CodeStyleManager;
import consulo.language.extension.ByLanguageValue;
import consulo.language.extension.LanguageExtension;
import consulo.language.extension.LanguageOneToMany;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.util.IncorrectOperationException;
import consulo.project.Project;
import consulo.util.lang.CharArrayUtil;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.List;

/**
 * @author max
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class SmartEnterProcessor implements LanguageExtension {
  private static final ExtensionPointCacheKey<SmartEnterProcessor, ByLanguageValue<List<SmartEnterProcessor>>> KEY =
          ExtensionPointCacheKey.create("SmartEnterProcessor", LanguageOneToMany.build(false));

  @Nonnull
  public static List<SmartEnterProcessor> forLanguage(@Nonnull Language language) {
    return Application.get().getExtensionPoint(SmartEnterProcessor.class).getOrBuildCache(KEY).requiredGet(language);
  }

  public abstract boolean process(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiFile psiFile);

  public boolean processAfterCompletion(@Nonnull Editor editor, @Nonnull PsiFile psiFile) {
    return process(psiFile.getProject(), editor, psiFile);
  }

  protected void reformat(PsiElement atCaret) throws IncorrectOperationException {
    TextRange range = atCaret.getTextRange();
    PsiFile file = atCaret.getContainingFile();
    PsiFile baseFile = file.getViewProvider().getPsi(file.getViewProvider().getBaseLanguage());
    CodeStyleManager.getInstance(atCaret.getProject()).reformatText(baseFile, range.getStartOffset(), range.getEndOffset());
  }

  protected RangeMarker createRangeMarker(PsiElement elt) {
    PsiFile psiFile = elt.getContainingFile();
    PsiDocumentManager instance = PsiDocumentManager.getInstance(elt.getProject());
    Document document = instance.getDocument(psiFile);
    return document.createRangeMarker(elt.getTextRange());
  }

  @Nullable
  protected PsiElement getStatementAtCaret(Editor editor, PsiFile psiFile) {
    int caret = editor.getCaretModel().getOffset();

    Document doc = editor.getDocument();
    CharSequence chars = doc.getCharsSequence();
    int offset = caret == 0 ? 0 : CharArrayUtil.shiftBackward(chars, caret - 1, " \t");
    if (doc.getLineNumber(offset) < doc.getLineNumber(caret)) {
      offset = CharArrayUtil.shiftForward(chars, caret, " \t");
    }

    return psiFile.findElementAt(offset);
  }

  protected static boolean isUncommited(@Nonnull Project project) {
    return PsiDocumentManager.getInstance(project).hasUncommitedDocuments();
  }

  protected void commit(@Nonnull Editor editor) {
    Project project = editor.getProject();
    PsiDocumentManager.getInstance(project).commitDocument(editor.getDocument());

    //some psi operations may block the document, unblock here
    PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(editor.getDocument());
  }
}
