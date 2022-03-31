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
package com.intellij.openapi.editor.impl;

import consulo.language.lexer.Lexer;
import consulo.document.Document;
import consulo.language.editor.highlight.LexerEditorHighlighter;
import consulo.codeEditor.EditorHighlighter;
import consulo.language.editor.highlight.EditorHighlighterFactory;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.ide.impl.psi.impl.cache.impl.id.PlatformIdTableBuilding;
import consulo.language.editor.highlight.LexerEditorHighlighterLexer;
import com.intellij.reference.SoftReference;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.lang.ref.WeakReference;

/**
 * @author yole
 */
public class EditorHighlighterCache {
  private static final Key<WeakReference<EditorHighlighter>> ourSomeEditorSyntaxHighlighter = Key.create("some editor highlighter");

  private EditorHighlighterCache() {
  }

  public static void rememberEditorHighlighterForCachesOptimization(Document document, @Nonnull final EditorHighlighter highlighter) {
    document.putUserData(ourSomeEditorSyntaxHighlighter, new WeakReference<EditorHighlighter>(highlighter));
  }

  @Nullable
  public static EditorHighlighter getEditorHighlighterForCachesBuilding(Document document) {
    if (document == null) {
      return null;
    }
    final WeakReference<EditorHighlighter> editorHighlighterWeakReference = document.getUserData(ourSomeEditorSyntaxHighlighter);
    final EditorHighlighter someEditorHighlighter = SoftReference.dereference(editorHighlighterWeakReference);

    if (someEditorHighlighter instanceof LexerEditorHighlighter &&
        ((LexerEditorHighlighter)someEditorHighlighter).isValid()
            ) {
      return someEditorHighlighter;
    }
    document.putUserData(ourSomeEditorSyntaxHighlighter, null);
    return null;
  }

  @Nullable
  public static Lexer getLexerBasedOnLexerHighlighter(CharSequence text, VirtualFile virtualFile, Project project) {
    EditorHighlighter highlighter = null;

    PsiFile psiFile = virtualFile != null ? PsiManager.getInstance(project).findFile(virtualFile) : null;
    final Document document = psiFile != null ? PsiDocumentManager.getInstance(project).getDocument(psiFile) : null;
    final EditorHighlighter cachedEditorHighlighter;
    boolean alreadyInitializedHighlighter = false;

    if (document != null &&
        (cachedEditorHighlighter = getEditorHighlighterForCachesBuilding(document)) != null &&
        PlatformIdTableBuilding.checkCanUseCachedEditorHighlighter(text, cachedEditorHighlighter)) {
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
}
