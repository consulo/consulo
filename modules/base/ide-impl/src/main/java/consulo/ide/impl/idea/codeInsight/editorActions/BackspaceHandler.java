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

import consulo.annotation.access.RequiredWriteAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.*;
import consulo.codeEditor.action.EditorActionHandler;
import consulo.codeEditor.action.ExtensionEditorActionHandler;
import consulo.dataContext.DataContext;
import consulo.codeEditor.action.EditorWriteActionHandler;
import consulo.language.ast.IElementType;
import consulo.language.editor.action.BackspaceHandlerDelegate;
import consulo.language.editor.action.BraceMatchingUtil;
import consulo.language.editor.action.EditorBackspaceUtil;
import consulo.language.editor.action.QuoteHandler;
import consulo.language.editor.highlight.BraceMatcher;
import consulo.language.editor.util.PsiUtilBase;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.ui.ex.action.IdeActions;
import consulo.virtualFileSystem.fileType.FileType;
import jakarta.inject.Inject;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

@ExtensionImpl(order = "first")
public class BackspaceHandler extends EditorWriteActionHandler implements ExtensionEditorActionHandler {
    protected EditorActionHandler myOriginalHandler;

    @Inject
    public BackspaceHandler() {
        super(true);
    }

    @RequiredWriteAction
    @Override
    public void executeWriteAction(Editor editor, Caret caret, DataContext dataContext) {
        if (!handleBackspace(editor, caret, dataContext, false)) {
            myOriginalHandler.execute(editor, caret, dataContext);
        }
    }

    protected boolean handleBackspace(Editor editor, Caret caret, DataContext dataContext, boolean toWordStart) {
        Project project = dataContext.getData(Project.KEY);
        if (project == null) {
            return false;
        }

        PsiFile file = PsiUtilBase.getPsiFileInEditor(editor, project);

        if (file == null) {
            return false;
        }

        if (editor.getSelectionModel().hasSelection()) {
            return false;
        }

        int offset = editor.getCaretModel().getOffset() - 1;
        if (offset < 0) {
            return false;
        }
        CharSequence chars = editor.getDocument().getCharsSequence();
        char c = chars.charAt(offset);

        final Editor injectedEditor = TypedHandler.injectedEditorIfCharTypedIsSignificant(c, editor, file);
        final Editor originalEditor = editor;
        if (injectedEditor != editor) {
            int injectedOffset = injectedEditor.getCaretModel().getOffset();
            if (EditorBackspaceUtil.isOffsetInsideInjected(injectedEditor, injectedOffset)) {
                file = PsiDocumentManager.getInstance(project).getPsiFile(injectedEditor.getDocument());
                editor = injectedEditor;
                offset = injectedOffset - 1;
            }
        }

        final List<BackspaceHandlerDelegate> delegates = BackspaceHandlerDelegate.EP_NAME.getExtensionList();
        if (!toWordStart) {
            for (BackspaceHandlerDelegate delegate : delegates) {
                delegate.beforeCharDeleted(c, file, editor);
            }
        }

        FileType fileType = file.getFileType();
        final QuoteHandler quoteHandler = TypedHandler.getQuoteHandler(file, editor);

        HighlighterIterator hiterator = editor.getHighlighter().createIterator(offset);
        boolean wasClosingQuote = quoteHandler != null && quoteHandler.isClosingQuote(hiterator, offset);

        myOriginalHandler.execute(originalEditor, caret, dataContext);

        if (!toWordStart) {
            for (BackspaceHandlerDelegate delegate : delegates) {
                if (delegate.charDeleted(c, file, editor)) {
                    return true;
                }
            }
        }

        if (offset >= editor.getDocument().getTextLength()) {
            return true;
        }

        chars = editor.getDocument().getCharsSequence();
        if (c == '(' || c == '[' || c == '{') {
            char c1 = chars.charAt(offset);
            if (c1 != EditorBackspaceUtil.getRightChar(c)) {
                return true;
            }

            HighlighterIterator iterator = editor.getHighlighter().createIterator(offset);
            BraceMatcher braceMatcher = BraceMatchingUtil.getBraceMatcher(fileType, iterator);
            if (!braceMatcher.isLBraceToken(iterator, chars, fileType) && !braceMatcher.isRBraceToken(iterator, chars, fileType)) {
                return true;
            }

            int rparenOffset = BraceMatchingUtil.findRightmostRParen(iterator, (IElementType)iterator.getTokenType(), chars, fileType);
            if (rparenOffset >= 0) {
                iterator = editor.getHighlighter().createIterator(rparenOffset);
                boolean matched = BraceMatchingUtil.matchBrace(chars, fileType, iterator, false);
                if (matched) {
                    return true;
                }
            }

            editor.getDocument().deleteString(offset, offset + 1);
        }
        else if (c == '"' || c == '\'' || c == '`') {
            char c1 = chars.charAt(offset);
            if (c1 != c) {
                return true;
            }
            if (wasClosingQuote) {
                return true;
            }

            HighlighterIterator iterator = editor.getHighlighter().createIterator(offset);
            if (quoteHandler == null || !quoteHandler.isOpeningQuote(iterator, offset)) {
                return true;
            }

            editor.getDocument().deleteString(offset, offset + 1);
        }

        return true;
    }

    @Override
    public void init(@Nullable EditorActionHandler originalHandler) {
        myOriginalHandler = originalHandler;
    }

    @Nonnull
    @Override
    public String getActionId() {
        return IdeActions.ACTION_EDITOR_BACKSPACE;
    }
}
