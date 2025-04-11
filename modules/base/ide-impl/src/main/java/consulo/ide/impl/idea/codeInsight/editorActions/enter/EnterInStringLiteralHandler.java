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
import consulo.codeEditor.EditorHighlighter;
import consulo.codeEditor.HighlighterIterator;
import consulo.codeEditor.action.EditorActionHandler;
import consulo.dataContext.DataContext;
import consulo.document.Document;
import consulo.document.util.TextRange;
import consulo.ide.impl.idea.codeInsight.editorActions.EnterHandler;
import consulo.ide.impl.idea.codeInsight.editorActions.TypedHandler;
import consulo.language.Language;
import consulo.language.ast.ASTNode;
import consulo.language.ast.IElementType;
import consulo.language.ast.StringEscapesTokenTypes;
import consulo.language.codeStyle.CodeStyleSettingsManager;
import consulo.language.codeStyle.CommonCodeStyleSettings;
import consulo.language.editor.action.EnterHandlerDelegateAdapter;
import consulo.language.editor.action.JavaLikeQuoteHandler;
import consulo.language.editor.action.LanguageQuoteHandler;
import consulo.language.editor.action.QuoteHandler;
import consulo.language.lexer.StringLiteralLexer;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.util.lang.ref.Ref;
import consulo.util.lang.ref.SimpleReference;
import consulo.virtualFileSystem.fileType.FileType;
import jakarta.annotation.Nonnull;

@ExtensionImpl(id = "inStringLiteral", order = "first")
public class EnterInStringLiteralHandler extends EnterHandlerDelegateAdapter {
    @Override
    public Result preprocessEnter(
        @Nonnull PsiFile file,
        @Nonnull Editor editor,
        @Nonnull SimpleReference<Integer> caretOffsetRef,
        @Nonnull SimpleReference<Integer> caretAdvanceRef,
        @Nonnull DataContext dataContext,
        EditorActionHandler originalHandler
    ) {
        int caretOffset = caretOffsetRef.get().intValue();
        int caretAdvance = caretAdvanceRef.get().intValue();
        if (!isInStringLiteral(editor, dataContext, caretOffset)) {
            return Result.Continue;
        }
        PsiDocumentManager.getInstance(file.getProject()).commitDocument(editor.getDocument());
        PsiElement psiAtOffset = file.findElementAt(caretOffset);
        if (psiAtOffset != null && psiAtOffset.getTextOffset() < caretOffset) {
            Document document = editor.getDocument();
            CharSequence text = document.getText();
            ASTNode token = psiAtOffset.getNode();
            JavaLikeQuoteHandler quoteHandler = getJavaLikeQuoteHandler(editor, psiAtOffset);

            if (quoteHandler != null &&
                quoteHandler.getConcatenatableStringTokenTypes() != null &&
                quoteHandler.getConcatenatableStringTokenTypes().contains(token.getElementType())) {
                TextRange range = token.getTextRange();
                char literalStart = token.getText().charAt(0);
                StringLiteralLexer lexer = new StringLiteralLexer(literalStart, token.getElementType());
                lexer.start(text, range.getStartOffset(), range.getEndOffset());

                while (lexer.getTokenType() != null) {
                    if (lexer.getTokenStart() < caretOffset && caretOffset < lexer.getTokenEnd()) {
                        if (StringEscapesTokenTypes.STRING_LITERAL_ESCAPES.contains(lexer.getTokenType())) {
                            caretOffset = lexer.getTokenEnd();
                        }
                        break;
                    }
                    lexer.advance();
                }

                if (quoteHandler.needParenthesesAroundConcatenation(psiAtOffset)) {
                    document.insertString(psiAtOffset.getTextRange().getEndOffset(), ")");
                    document.insertString(psiAtOffset.getTextRange().getStartOffset(), "(");
                    caretOffset++;
                    caretAdvance++;
                }

                String insertedFragment = literalStart + " " + quoteHandler.getStringConcatenationOperatorRepresentation();
                document.insertString(caretOffset, insertedFragment + " " + literalStart);
                caretOffset += insertedFragment.length();
                caretAdvance = 1;
                CommonCodeStyleSettings langSettings =
                    CodeStyleSettingsManager.getSettings(file.getProject()).getCommonSettings(file.getLanguage());
                if (langSettings.BINARY_OPERATION_SIGN_ON_NEXT_LINE) {
                    caretOffset -= 1;
                    caretAdvance = 3;
                }
                caretOffsetRef.set(caretOffset);
                caretAdvanceRef.set(caretAdvance);
                return Result.DefaultForceIndent;
            }
        }
        return Result.Continue;
    }

    protected JavaLikeQuoteHandler getJavaLikeQuoteHandler(Editor editor, PsiElement psiAtOffset) {
        QuoteHandler fileTypeQuoteHandler = TypedHandler.getQuoteHandler(psiAtOffset.getContainingFile(), editor);
        return fileTypeQuoteHandler instanceof JavaLikeQuoteHandler quoteHandler ? quoteHandler : null;
    }

    private static boolean isInStringLiteral(@Nonnull Editor editor, @Nonnull DataContext dataContext, int offset) {
        Language language = EnterHandler.getLanguage(dataContext);
        if (offset > 0 && language != null) {
            QuoteHandler quoteHandler = LanguageQuoteHandler.forLanguage(language);
            if (quoteHandler == null) {
                FileType fileType = language.getAssociatedFileType();
                quoteHandler = fileType != null ? TypedHandler.getQuoteHandlerForType(fileType) : null;
            }
            if (quoteHandler != null) {
                EditorHighlighter highlighter = editor.getHighlighter();
                HighlighterIterator iterator = highlighter.createIterator(offset - 1);
                return StringEscapesTokenTypes.STRING_LITERAL_ESCAPES.contains((IElementType)iterator.getTokenType())
                    || quoteHandler.isInsideLiteral(iterator);
            }
        }
        return false;
    }
}
