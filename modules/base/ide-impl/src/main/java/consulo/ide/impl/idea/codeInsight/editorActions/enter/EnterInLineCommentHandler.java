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

package consulo.ide.impl.idea.codeInsight.editorActions.enter;

import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.codeEditor.action.EditorActionHandler;
import consulo.dataContext.DataContext;
import consulo.document.Document;
import consulo.util.lang.StringUtil;
import consulo.ide.impl.idea.util.text.CharArrayUtil;
import consulo.language.CodeDocumentationAwareCommenter;
import consulo.language.Commenter;
import consulo.language.Language;
import consulo.language.ast.ASTNode;
import consulo.language.editor.action.EnterHandlerDelegateAdapter;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.util.lang.ref.Ref;

import jakarta.annotation.Nonnull;

@ExtensionImpl(id = "inLineComment", order = "after inStringLiteral")
public class EnterInLineCommentHandler extends EnterHandlerDelegateAdapter {
  @Override
  public Result preprocessEnter(@Nonnull final PsiFile file, @Nonnull final Editor editor, @Nonnull final Ref<Integer> caretOffsetRef, @Nonnull final Ref<Integer> caretAdvance,
                                @Nonnull final DataContext dataContext, final EditorActionHandler originalHandler) {
    int caretOffset = caretOffsetRef.get().intValue();
    PsiElement psiAtOffset = file.findElementAt(caretOffset);
    if (psiAtOffset != null && psiAtOffset.getTextOffset() < caretOffset) {
      ASTNode token = psiAtOffset.getNode();
      Document document = editor.getDocument();
      CharSequence text = document.getText();
      final Language language = psiAtOffset.getLanguage();
      final Commenter languageCommenter = Commenter.forLanguage(language);
      final CodeDocumentationAwareCommenter commenter = languageCommenter instanceof CodeDocumentationAwareCommenter
                                                        ? (CodeDocumentationAwareCommenter)languageCommenter: null;
      if (commenter != null && token.getElementType() == commenter.getLineCommentTokenType() ) {
        final int offset = CharArrayUtil.shiftForward(text, caretOffset, " \t");

        if (offset < document.getTextLength() && text.charAt(offset) != '\n') {
          String prefix = commenter.getLineCommentPrefix();
          assert prefix != null: "Line Comment type is set but Line Comment Prefix is null!";
          if (!StringUtil.startsWith(text, offset, prefix)) {
            if (text.charAt(caretOffset) != ' ' && !prefix.endsWith(" ")) {
              prefix += " ";
            }
            document.insertString(caretOffset, prefix);
            return Result.Default;
          }
        }
      }
    }
    return Result.Continue;
  }
}
