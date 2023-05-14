// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.editor.template.context;

import consulo.annotation.access.RequiredReadAction;
import consulo.codeEditor.Editor;
import consulo.codeEditor.SelectionModel;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiWhiteSpace;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Describes context in which live template supposed to be used.
 */
public final class TemplateActionContext {
  @Nonnull
  private final PsiFile myFile;
  @Nullable
  private final Editor myEditor;

  private final int myStartOffset;
  private final int myEndOffset;
  private final boolean myIsSurrounding;

  private TemplateActionContext(@Nonnull PsiFile file, @Nullable Editor editor, int startOffset, int endOffset, boolean isSurrounding) {
    myFile = file;
    myStartOffset = startOffset;
    myEndOffset = endOffset;
    myIsSurrounding = isSurrounding;
    myEditor = editor;
  }

  @Nonnull
  public PsiFile getFile() {
    return myFile;
  }

  /**
   * @return editor if file is currently opened in one. Sometimes context may be used with fake files, without any editors
   */
  @Nullable
  public Editor getEditor() {
    return myEditor;
  }

  /**
   * @return true iff {@code surround with} action is performed
   */
  public boolean isSurrounding() {
    return myIsSurrounding;
  }

  /**
   * @return a copy of current context with specific {@code file}
   */
  @Nonnull
  public TemplateActionContext withFile(@Nonnull PsiFile file) {
    return new TemplateActionContext(file, myEditor, myStartOffset, myEndOffset, myIsSurrounding);
  }

  /**
   * @return for surround context returns selection start or caret position if there is no selection or it is expanding context
   */
  public int getStartOffset() {
    return myStartOffset;
  }

  /**
   * @return for surround context returns selection end or caret position if there is no selection or it is expanding context
   */
  public int getEndOffset() {
    return myEndOffset;
  }

  @Nonnull
  public static TemplateActionContext expanding(@Nonnull PsiFile psiFile, @Nonnull Editor editor) {
    int editorOffset = editor.getCaretModel().getOffset();
    return create(psiFile, editor, editorOffset, editorOffset, false);
  }

  @Nonnull
  public static TemplateActionContext expanding(@Nonnull PsiFile psiFile, int offset) {
    return create(psiFile, null, offset, offset, false);
  }

  @Nonnull
  @RequiredReadAction
  public static TemplateActionContext surrounding(@Nonnull PsiFile psiFile, @Nonnull Editor editor) {
    SelectionModel selectionModel = editor.getSelectionModel();
    int start = selectionModel.getSelectionStart();
    int end = selectionModel.getSelectionEnd();
    PsiElement startElement = psiFile.findElementAt(start);
    if (startElement instanceof PsiWhiteSpace) {
      start = Math.min(startElement.getTextRange().getEndOffset(), end);
    }
    PsiElement endElement = psiFile.findElementAt(end);
    if (endElement != startElement && endElement instanceof PsiWhiteSpace) {
      end = Math.max(start, endElement.getTextRange().getStartOffset());
    }
    return create(psiFile, editor, start, end, true);
  }

  @Nonnull
  public static TemplateActionContext create(@Nonnull PsiFile psiFile, @Nullable Editor editor, int startOffset, int endOffset, boolean isSurrounding) {
    return new TemplateActionContext(psiFile, editor, startOffset, endOffset, isSurrounding);
  }
}
