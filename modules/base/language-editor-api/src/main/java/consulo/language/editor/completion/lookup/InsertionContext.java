// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.editor.completion.lookup;

import consulo.codeEditor.Editor;
import consulo.document.Document;
import consulo.language.editor.completion.CompletionInitializationContext;
import consulo.language.editor.completion.OffsetKey;
import consulo.language.editor.completion.OffsetMap;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.project.Project;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author peter
 */
public class InsertionContext {
  public static final OffsetKey TAIL_OFFSET = OffsetKey.create("tailOffset", true);

  private final OffsetMap myOffsetMap;
  private final char myCompletionChar;
  private final LookupElement[] myElements;
  private final PsiFile myFile;
  private final Editor myEditor;
  private Runnable myLaterRunnable;
  private boolean myAddCompletionChar;

  public InsertionContext(OffsetMap offsetMap, char completionChar, LookupElement[] elements,
                          @Nonnull PsiFile file,
                          @Nonnull Editor editor, boolean addCompletionChar) {
    myOffsetMap = offsetMap;
    myCompletionChar = completionChar;
    myElements = elements;
    myFile = file;
    myEditor = editor;
    setTailOffset(editor.getCaretModel().getOffset());
    myAddCompletionChar = addCompletionChar;
  }

  public void setTailOffset(int offset) {
    myOffsetMap.addOffset(TAIL_OFFSET, offset);
  }

  public int getTailOffset() {
    return myOffsetMap.getOffset(TAIL_OFFSET);
  }

  @Nonnull
  public PsiFile getFile() {
    return myFile;
  }

  @Nonnull
  public Editor getEditor() {
    return myEditor;
  }

  public void commitDocument() {
    PsiDocumentManager.getInstance(getProject()).commitDocument(getDocument());
  }

  @Nonnull
  public Document getDocument() {
    return getEditor().getDocument();
  }

  public int getOffset(OffsetKey key) {
    return getOffsetMap().getOffset(key);
  }

  public OffsetMap getOffsetMap() {
    return myOffsetMap;
  }

  public OffsetKey trackOffset(int offset, boolean movableToRight) {
    OffsetKey key = OffsetKey.create("tracked", movableToRight);
    getOffsetMap().addOffset(key, offset);
    return key;
  }

  public int getStartOffset() {
    return myOffsetMap.getOffset(CompletionInitializationContext.START_OFFSET);
  }

  public char getCompletionChar() {
    return myCompletionChar;
  }

  public LookupElement[] getElements() {
    return myElements;
  }

  public Project getProject() {
    return myFile.getProject();
  }

  public int getSelectionEndOffset() {
    return myOffsetMap.getOffset(CompletionInitializationContext.SELECTION_END_OFFSET);
  }

  @Nullable
  public Runnable getLaterRunnable() {
    return myLaterRunnable;
  }

  public void setLaterRunnable(@Nullable Runnable laterRunnable) {
    myLaterRunnable = laterRunnable;
  }

  /**
   * @param addCompletionChar Whether completionChar should be added to document at tail offset (see {@link #TAIL_OFFSET}) after insert handler (default: {@code true}).
   */
  public void setAddCompletionChar(boolean addCompletionChar) {
    myAddCompletionChar = addCompletionChar;
  }

  public boolean shouldAddCompletionChar() {
    return myAddCompletionChar;
  }
}
