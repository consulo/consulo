/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

/*
 * @author max
 */
package com.intellij.openapi.fileEditor.impl.text;

import com.intellij.codeHighlighting.BackgroundEditorHighlighter;
import com.intellij.codeInsight.daemon.impl.TextEditorBackgroundHighlighter;
import com.intellij.codeInsight.folding.CodeFoldingManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.fileEditor.FileEditorStateLevel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import consulo.fileEditor.impl.text.TextEditorComponentContainerFactory;
import consulo.logging.Logger;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.inject.Inject;
import org.jdom.Element;

import javax.annotation.Nonnull;

public class PsiAwareTextEditorProviderImpl extends TextEditorProviderImpl {
  private static final Logger LOG = Logger.getInstance(PsiAwareTextEditorProviderImpl.class);

  private static final String FOLDING_ELEMENT = "folding";

  @Inject
  public PsiAwareTextEditorProviderImpl(TextEditorComponentContainerFactory textEditorComponentContainerFactory) {
    super(textEditorComponentContainerFactory);
  }

  @RequiredUIAccess
  @Override
  @Nonnull
  public FileEditor createEditor(@Nonnull final Project project, @Nonnull final VirtualFile file) {
    return new PsiAwareTextEditorImpl(project, file, this);
  }

  @Override
  @Nonnull
  public FileEditorState readState(@Nonnull final Element element, @Nonnull final Project project, @Nonnull final VirtualFile file) {
    final TextEditorState state = (TextEditorState)super.readState(element, project, file);

    // Foldings
    Element child = element.getChild(FOLDING_ELEMENT);
    Document document = FileDocumentManager.getInstance().getCachedDocument(file);
    if (child != null) {
      if (document == null) {
        final Element detachedStateCopy = child.clone();
        state.setDelayedFoldState(() -> {
          Document document1 = FileDocumentManager.getInstance().getCachedDocument(file);
          return document1 == null ? null : CodeFoldingManager.getInstance(project).readFoldingState(detachedStateCopy, document1);
        });
      }
      else {
        //PsiDocumentManager.getInstance(project).commitDocument(document);
        state.setFoldingState(CodeFoldingManager.getInstance(project).readFoldingState(child, document));
      }
    }
    return state;
  }

  @Override
  public void writeState(@Nonnull final FileEditorState _state, @Nonnull final Project project, @Nonnull final Element element) {
    super.writeState(_state, project, element);

    TextEditorState state = (TextEditorState)_state;

    // Foldings
    CodeFoldingState foldingState = state.getFoldingState();
    if (foldingState != null) {
      Element e = new Element(FOLDING_ELEMENT);
      try {
        CodeFoldingManager.getInstance(project).writeFoldingState(foldingState, e);
      }
      catch (WriteExternalException e1) {
        //ignore
      }
      element.addContent(e);
    }
  }

  @Nonnull
  @Override
  public TextEditorState getStateImpl(final Project project, @Nonnull final Editor editor, @Nonnull final FileEditorStateLevel level) {
    final TextEditorState state = super.getStateImpl(project, editor, level);
    // Save folding only on FULL level. It's very expensive to commit document on every
    // type (caused by undo).
    if (FileEditorStateLevel.FULL == level) {
      // Folding
      if (project != null && !project.isDisposed() && !editor.isDisposed() && project.isInitialized()) {
        state.setFoldingState(CodeFoldingManager.getInstance(project).saveFoldingState(editor));
      }
      else {
        state.setFoldingState(null);
      }
    }

    return state;
  }

  @Override
  public void setStateImpl(final Project project, final Editor editor, final TextEditorState state) {
    super.setStateImpl(project, editor, state);
    // Folding
    final CodeFoldingState foldState = state.getFoldingState();
    if (project != null && foldState != null && AsyncEditorLoader.isEditorLoaded(editor)) {
      if (!PsiDocumentManager.getInstance(project).isCommitted(editor.getDocument())) {
        PsiDocumentManager.getInstance(project).commitDocument(editor.getDocument());
        LOG.error("File should be parsed when changing editor state, otherwise UI might be frozen for a considerable time");
      }
      editor.getFoldingModel().runBatchFoldingOperation(() -> CodeFoldingManager.getInstance(project).restoreFoldingState(editor, foldState));
    }
  }

  @Nonnull
  @Override
  protected EditorWrapper createWrapperForEditor(@Nonnull final Editor editor) {
    return new PsiAwareEditorWrapper(editor);
  }

  private final class PsiAwareEditorWrapper extends EditorWrapper {
    private final TextEditorBackgroundHighlighter myBackgroundHighlighter;

    private PsiAwareEditorWrapper(@Nonnull Editor editor) {
      super(editor);
      final Project project = editor.getProject();
      myBackgroundHighlighter = project == null ? null : new TextEditorBackgroundHighlighter(project, editor);
    }

    @Override
    public BackgroundEditorHighlighter getBackgroundHighlighter() {
      return myBackgroundHighlighter;
    }

    @Override
    public boolean isValid() {
      return !getEditor().isDisposed();
    }
  }
}
