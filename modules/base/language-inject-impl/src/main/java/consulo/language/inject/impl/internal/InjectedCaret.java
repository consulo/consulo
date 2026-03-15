// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.inject.impl.internal;

import consulo.language.editor.inject.EditorWindow;
import consulo.codeEditor.*;
import consulo.util.dataholder.Key;
import consulo.document.util.ProperTextRange;
import consulo.document.util.TextRange;
import org.jspecify.annotations.Nullable;

public class InjectedCaret implements CaretDelegate {
  private final EditorWindow myEditorWindow;
  final Caret myDelegate;

  InjectedCaret(EditorWindow window, Caret delegate) {
    myEditorWindow = window;
    myDelegate = delegate;
  }

  
  @Override
  public Editor getEditor() {
    return myEditorWindow;
  }

  
  @Override
  public CaretModel getCaretModel() {
    return myEditorWindow.getCaretModel();
  }

  @Override
  public Caret getDelegate() {
    return myDelegate;
  }

  @Override
  public boolean isValid() {
    return myDelegate.isValid();
  }

  @Override
  public void moveCaretRelatively(int columnShift, int lineShift, boolean withSelection, boolean scrollToCaret) {
    myDelegate.moveCaretRelatively(columnShift, lineShift, withSelection, scrollToCaret);
  }

  @Override
  public void moveToLogicalPosition(LogicalPosition pos) {
    LogicalPosition hostPos = myEditorWindow.injectedToHost(pos);
    myDelegate.moveToLogicalPosition(hostPos);
  }

  @Override
  public void moveToVisualPosition(VisualPosition pos) {
    LogicalPosition hostPos = myEditorWindow.injectedToHost(myEditorWindow.visualToLogicalPosition(pos));
    myDelegate.moveToLogicalPosition(hostPos);
  }

  @Override
  public void moveToOffset(int offset) {
    moveToOffset(offset, false);
  }

  @Override
  public void moveToOffset(int offset, boolean locateBeforeSoftWrap) {
    int hostOffset = myEditorWindow.getDocument().injectedToHost(offset);
    myDelegate.moveToOffset(hostOffset, locateBeforeSoftWrap);
  }

  @Override
  public boolean isUpToDate() {
    return myDelegate.isUpToDate();
  }

  
  @Override
  public LogicalPosition getLogicalPosition() {
    LogicalPosition hostPos = myDelegate.getLogicalPosition();
    return myEditorWindow.hostToInjected(hostPos);
  }

  
  @Override
  public VisualPosition getVisualPosition() {
    LogicalPosition logicalPosition = getLogicalPosition();
    return myEditorWindow.logicalToVisualPosition(logicalPosition);
  }

  @Override
  public int getOffset() {
    return myEditorWindow.getDocument().hostToInjected(myDelegate.getOffset());
  }

  @Override
  public int getVisualLineStart() {
    return myEditorWindow.getDocument().hostToInjected(myDelegate.getVisualLineStart());
  }

  @Override
  public int getVisualLineEnd() {
    return myEditorWindow.getDocument().hostToInjected(myDelegate.getVisualLineEnd());
  }

  @Override
  public int getSelectionStart() {
    return myEditorWindow.getDocument().hostToInjected(myDelegate.getSelectionStart());
  }

  
  @Override
  public VisualPosition getSelectionStartPosition() {
    return myDelegate.getSelectionStartPosition();
  }

  @Override
  public int getSelectionEnd() {
    return myEditorWindow.getDocument().hostToInjected(myDelegate.getSelectionEnd());
  }

  
  @Override
  public VisualPosition getSelectionEndPosition() {
    return myDelegate.getSelectionEndPosition();
  }

  @Nullable
  @Override
  public String getSelectedText() {
    return myDelegate.getSelectedText();
  }

  @Override
  public int getLeadSelectionOffset() {
    return myEditorWindow.getDocument().hostToInjected(myDelegate.getLeadSelectionOffset());
  }

  
  @Override
  public VisualPosition getLeadSelectionPosition() {
    return myDelegate.getLeadSelectionPosition();
  }

  @Override
  public boolean hasSelection() {
    return myDelegate.hasSelection();
  }

  @Override
  public void setSelection(int startOffset, int endOffset) {
    TextRange hostRange = myEditorWindow.getDocument().injectedToHost(new ProperTextRange(startOffset, endOffset));
    myDelegate.setSelection(hostRange.getStartOffset(), hostRange.getEndOffset());
  }

  @Override
  public void setSelection(int startOffset, int endOffset, boolean updateSystemSelection) {
    TextRange hostRange = myEditorWindow.getDocument().injectedToHost(new ProperTextRange(startOffset, endOffset));
    myDelegate.setSelection(hostRange.getStartOffset(), hostRange.getEndOffset(), updateSystemSelection);
  }

  @Override
  public void setSelection(int startOffset, @Nullable VisualPosition endPosition, int endOffset) {
    TextRange hostRange = myEditorWindow.getDocument().injectedToHost(new ProperTextRange(startOffset, endOffset));
    myDelegate.setSelection(hostRange.getStartOffset(), endPosition, hostRange.getEndOffset());
  }

  @Override
  public void setSelection(@Nullable VisualPosition startPosition, int startOffset, @Nullable VisualPosition endPosition, int endOffset) {
    TextRange hostRange = myEditorWindow.getDocument().injectedToHost(new ProperTextRange(startOffset, endOffset));
    myDelegate.setSelection(startPosition, hostRange.getStartOffset(), endPosition, hostRange.getEndOffset());
  }

  @Override
  public void setSelection(@Nullable VisualPosition startPosition, int startOffset, @Nullable VisualPosition endPosition, int endOffset, boolean updateSystemSelection) {
    TextRange hostRange = myEditorWindow.getDocument().injectedToHost(new ProperTextRange(startOffset, endOffset));
    myDelegate.setSelection(startPosition, hostRange.getStartOffset(), endPosition, hostRange.getEndOffset(), updateSystemSelection);
  }

  @Override
  public void removeSelection() {
    myDelegate.removeSelection();
  }

  @Override
  public void selectLineAtCaret() {
    myDelegate.selectLineAtCaret();
  }

  @Override
  public void selectWordAtCaret(boolean honorCamelWordsSettings) {
    myDelegate.selectWordAtCaret(honorCamelWordsSettings);
  }

  @Nullable
  @Override
  public Caret clone(boolean above) {
    Caret clone = myDelegate.clone(above);
    return clone == null ? null : new InjectedCaret(myEditorWindow, clone);
  }

  @Override
  public void dispose() {
    //noinspection SSBasedInspection
    myDelegate.dispose();
  }

  
  @Override
  public <T> T putUserDataIfAbsent(Key<T> key, T value) {
    return myDelegate.putUserDataIfAbsent(key, value);
  }

  @Override
  public <T> boolean replace(Key<T> key, @Nullable T oldValue, @Nullable T newValue) {
    return myDelegate.replace(key, oldValue, newValue);
  }

  @Nullable
  @Override
  public <T> T getUserData(Key<T> key) {
    return myDelegate.getUserData(key);
  }

  @Override
  public <T> void putUserData(Key<T> key, @Nullable T value) {
    myDelegate.putUserData(key, value);
  }

  @Override
  public boolean isAtRtlLocation() {
    return myDelegate.isAtRtlLocation();
  }

  @Override
  public boolean isAtBidiRunBoundary() {
    return myDelegate.isAtBidiRunBoundary();
  }

  
  @Override
  public CaretVisualAttributes getVisualAttributes() {
    return myDelegate.getVisualAttributes();
  }

  @Override
  public void setVisualAttributes(CaretVisualAttributes attributes) {
    myDelegate.setVisualAttributes(attributes);
  }
}
