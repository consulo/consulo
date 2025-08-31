// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.editor.completion;

import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import consulo.language.Language;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.util.lang.ObjectUtil;

import jakarta.annotation.Nonnull;

/**
 * @author peter
 */
public class CompletionInitializationContext {
  public static final OffsetKey START_OFFSET = OffsetKey.create("startOffset", false);
  public static final OffsetKey SELECTION_END_OFFSET = OffsetKey.create("selectionEnd");
  public static final OffsetKey IDENTIFIER_END_OFFSET = OffsetKey.create("identifierEnd");

  /**
   * A default string that is inserted to the file before completion to guarantee that there'll always be some non-empty element there
   */
  public static final String DUMMY_IDENTIFIER = CompletionUtilCore.DUMMY_IDENTIFIER;
  public static final String DUMMY_IDENTIFIER_TRIMMED = CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED;
  private final Editor myEditor;
  @Nonnull
  private final Caret myCaret;
  private final PsiFile myFile;
  private final CompletionType myCompletionType;
  private final int myInvocationCount;
  private final OffsetMap myOffsetMap;
  private String myDummyIdentifier = DUMMY_IDENTIFIER;
  private final Language myPositionLanguage;

  public CompletionInitializationContext(Editor editor, @Nonnull Caret caret, Language language, PsiFile file, CompletionType completionType, int invocationCount) {
    myEditor = editor;
    myCaret = caret;
    myPositionLanguage = language;
    myFile = file;
    myCompletionType = completionType;
    myInvocationCount = invocationCount;
    myOffsetMap = new OffsetMap(editor.getDocument());

    myOffsetMap.addOffset(START_OFFSET, calcStartOffset(caret));
    myOffsetMap.addOffset(SELECTION_END_OFFSET, calcSelectionEnd(caret));
    myOffsetMap.addOffset(IDENTIFIER_END_OFFSET, calcDefaultIdentifierEnd(editor, calcSelectionEnd(caret)));
  }

  private static int calcSelectionEnd(Caret caret) {
    return caret.hasSelection() ? caret.getSelectionEnd() : caret.getOffset();
  }

  public static int calcStartOffset(Caret caret) {
    return caret.hasSelection() ? caret.getSelectionStart() : caret.getOffset();
  }

  public static int calcDefaultIdentifierEnd(Editor editor, int startFrom) {
    CharSequence text = editor.getDocument().getCharsSequence();
    int idEnd = startFrom;
    while (idEnd < text.length() && Character.isJavaIdentifierPart(text.charAt(idEnd))) {
      idEnd++;
    }
    return idEnd;
  }

  public void setDummyIdentifier(@Nonnull String dummyIdentifier) {
    myDummyIdentifier = dummyIdentifier;
  }

  @Nonnull
  public Language getPositionLanguage() {
    return ObjectUtil.assertNotNull(myPositionLanguage);
  }

  public String getDummyIdentifier() {
    return myDummyIdentifier;
  }

  @Nonnull
  public Editor getEditor() {
    return myEditor;
  }

  @Nonnull
  public Caret getCaret() {
    return myCaret;
  }

  @Nonnull
  public CompletionType getCompletionType() {
    return myCompletionType;
  }

  @Nonnull
  public Project getProject() {
    return myFile.getProject();
  }

  @Nonnull
  public PsiFile getFile() {
    return myFile;
  }

  @Nonnull
  public OffsetMap getOffsetMap() {
    return myOffsetMap;
  }

  public int getStartOffset() {
    return myOffsetMap.getOffset(START_OFFSET);
  }

  public int getSelectionEndOffset() {
    return myOffsetMap.getOffset(SELECTION_END_OFFSET);
  }

  public int getIdentifierEndOffset() {
    return myOffsetMap.getOffset(IDENTIFIER_END_OFFSET);
  }

  public int getReplacementOffset() {
    return getIdentifierEndOffset();
  }

  public int getInvocationCount() {
    return myInvocationCount;
  }

  /**
   * Mark the offset up to which the text will be deleted if a completion variant is selected using Replace character (Tab)
   */
  public void setReplacementOffset(int idEnd) {
    myOffsetMap.addOffset(IDENTIFIER_END_OFFSET, idEnd);
  }
}
