/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.language.editor.internal;

import consulo.codeEditor.EditorHighlighter;
import consulo.document.Document;
import consulo.language.editor.highlight.EditorHighlighterFactory;
import consulo.language.editor.highlight.LexerEditorHighlighter;
import consulo.language.editor.highlight.LexerEditorHighlighterLexer;
import consulo.language.lexer.Lexer;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import consulo.util.lang.ref.SoftReference;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.lang.ref.WeakReference;

/**
 * @author yole
 */
public class EditorHighlighterCache {
    private static final Key<WeakReference<EditorHighlighter>> ourSomeEditorSyntaxHighlighter = Key.create("some editor highlighter");

    private EditorHighlighterCache() {
    }

    public static void rememberEditorHighlighterForCachesOptimization(Document document, @Nonnull EditorHighlighter highlighter) {
        document.putUserData(ourSomeEditorSyntaxHighlighter, new WeakReference<EditorHighlighter>(highlighter));
    }

    @Nullable
    public static EditorHighlighter getEditorHighlighterForCachesBuilding(Document document) {
        if (document == null) {
            return null;
        }
        WeakReference<EditorHighlighter> editorHighlighterWeakReference = document.getUserData(ourSomeEditorSyntaxHighlighter);
        EditorHighlighter someEditorHighlighter = SoftReference.dereference(editorHighlighterWeakReference);

        if (someEditorHighlighter instanceof LexerEditorHighlighter &&
            ((LexerEditorHighlighter) someEditorHighlighter).isValid()) {
            return someEditorHighlighter;
        }
        document.putUserData(ourSomeEditorSyntaxHighlighter, null);
        return null;
    }

    @Nullable
    public static Lexer getLexerBasedOnLexerHighlighter(CharSequence text, VirtualFile virtualFile, Project project) {
        EditorHighlighter highlighter = null;

        PsiFile psiFile = virtualFile != null ? PsiManager.getInstance(project).findFile(virtualFile) : null;
        Document document = psiFile != null ? PsiDocumentManager.getInstance(project).getDocument(psiFile) : null;
        EditorHighlighter cachedEditorHighlighter;
        boolean alreadyInitializedHighlighter = false;

        if (document != null &&
            (cachedEditorHighlighter = getEditorHighlighterForCachesBuilding(document)) != null &&
            checkCanUseCachedEditorHighlighter(text, cachedEditorHighlighter)) {
            highlighter = cachedEditorHighlighter;
            alreadyInitializedHighlighter = true;
        }
        else if (virtualFile != null) {
            highlighter = EditorHighlighterFactory.getInstance().createEditorHighlighter(project, virtualFile);
        }

        if (highlighter != null) {
            return new LexerEditorHighlighterLexer(highlighter, alreadyInitializedHighlighter);
        }
        return null;
    }

    public static boolean checkCanUseCachedEditorHighlighter(CharSequence chars, EditorHighlighter editorHighlighter) {
        assert editorHighlighter instanceof LexerEditorHighlighter;
        boolean b = ((LexerEditorHighlighter) editorHighlighter).checkContentIsEqualTo(chars);
        if (!b) {
            Logger logger = Logger.getInstance(EditorHighlighterCache.class);
            logger.warn("Unexpected mismatch of editor highlighter content with indexing content");
        }
        return b;
    }
}
