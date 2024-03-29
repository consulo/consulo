// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.editor.completion;

import consulo.codeEditor.Editor;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiUtilCore;
import consulo.language.psi.PsiReference;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author peter
 */
public final class CompletionParameters {
  private final PsiElement myPosition;
  private final PsiFile myOriginalFile;
  private final CompletionType myCompletionType;
  @Nonnull
  private final Editor myEditor;
  private final int myOffset;
  private final int myInvocationCount;
  private final CompletionProcess myProcess;
  private boolean isTestingMode = false;

  public CompletionParameters(@Nonnull final PsiElement position, @Nonnull final PsiFile originalFile,
                       @Nonnull CompletionType completionType, int offset, int invocationCount, @Nonnull Editor editor,
                       @Nonnull CompletionProcess process) {
    PsiUtilCore.ensureValid(position);
    assert position.getTextRange().containsOffset(offset) : position;
    myPosition = position;
    myOriginalFile = originalFile;
    myCompletionType = completionType;
    myOffset = offset;
    myInvocationCount = invocationCount;
    myEditor = editor;
    myProcess = process;
  }

  @Nonnull
  public CompletionParameters delegateToClassName() {
    return withType(CompletionType.CLASS_NAME).withInvocationCount(myInvocationCount - 1);
  }

  @Nonnull
  public CompletionParameters withType(@Nonnull CompletionType type) {
    return new CompletionParameters(myPosition, myOriginalFile, type, myOffset, myInvocationCount, myEditor, myProcess);
  }

  @Nonnull
  public CompletionParameters withInvocationCount(int newCount) {
    return new CompletionParameters(myPosition, myOriginalFile, myCompletionType, myOffset, newCount, myEditor, myProcess);
  }

  /**
   * Return the leaf PSI element in the "completion file" at offset {@link #getOffset()}.<p></p>
   * <p>
   * "Completion file" is a PSI file used for completion purposes. Most often it's a non-physical copy of the file being edited
   * (the original file can be accessed from {@link PsiFile#getOriginalFile()} or {@link #getOriginalFile()}).<p></p>
   * <p>
   * A special 'dummy identifier' string is inserted to the copied file at caret offset (removing the selection).
   * Most often this string is an identifier (see {@link CompletionInitializationContext#DUMMY_IDENTIFIER}).
   * It can be changed via {@link CompletionContributor#beforeCompletion(CompletionInitializationContext)} method.<p></p>
   * <p>
   * Why? This way there'll always be some non-empty element there, which usually reduces the number of
   * possible cases to be considered inside a {@link CompletionContributor}.
   * Also, even if completion was invoked in the middle of a white space, a reference might appear there after dummy identifier is inserted,
   * and its {@link PsiReference#getVariants()} can then be suggested.<p></p>
   * <p>
   * If the dummy identifier is empty, then the file isn't copied and this method returns whatever is at caret in the original file.
   */
  @Nonnull
  public PsiElement getPosition() {
    return myPosition;
  }

  @Nullable
  public PsiElement getOriginalPosition() {
    return myOriginalFile.findElementAt(myPosition.getTextRange().getStartOffset());
  }

  /**
   * @return the file being edited, possibly injected, where code completion was invoked.
   */
  @Nonnull
  public PsiFile getOriginalFile() {
    return myOriginalFile;
  }

  @Nonnull
  public CompletionType getCompletionType() {
    return myCompletionType;
  }

  /**
   * @return the offset (relative to the file) where code completion was invoked.
   */
  public int getOffset() {
    return myOffset;
  }

  /**
   * @return 0 for autopopup<br>
   * 1 for explicitly invoked completion<br>
   * >1 for next completion invocations when one lookup is already active
   */
  public int getInvocationCount() {
    return myInvocationCount;
  }

  public boolean isAutoPopup() {
    return myInvocationCount == 0;
  }

  @Nonnull
  public CompletionParameters withPosition(@Nonnull PsiElement element, int offset) {
    return new CompletionParameters(element, myOriginalFile, myCompletionType, offset, myInvocationCount, myEditor, myProcess);
  }

  public boolean isExtendedCompletion() {
    return myCompletionType == CompletionType.BASIC && myInvocationCount >= 2;
  }

  /**
   * @return the editor where the completion was started
   */
  @Nonnull
  public Editor getEditor() {
    return myEditor;
  }

  @Nonnull
  public CompletionProcess getProcess() {
    return myProcess;
  }

  public boolean isTestingMode() {
    return isTestingMode;
  }

  public void setIsTestingMode(boolean runTestingMode) {
    isTestingMode = runTestingMode;
  }
}
