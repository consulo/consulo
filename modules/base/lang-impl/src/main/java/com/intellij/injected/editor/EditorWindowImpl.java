/*
 * Copyright 2000-2017 JetBrains s.r.o.
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

package com.intellij.injected.editor;

import com.intellij.ide.CopyProvider;
import com.intellij.ide.CutProvider;
import com.intellij.ide.DeleteProvider;
import com.intellij.ide.PasteProvider;
import com.intellij.ide.highlighter.HighlighterFactory;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.event.EditorMouseEvent;
import com.intellij.openapi.editor.event.EditorMouseEventArea;
import com.intellij.openapi.editor.event.EditorMouseListener;
import com.intellij.openapi.editor.event.EditorMouseMotionListener;
import com.intellij.openapi.editor.ex.*;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.editor.highlighter.EditorHighlighter;
import com.intellij.openapi.editor.highlighter.LightHighlighterClient;
import com.intellij.openapi.editor.impl.DesktopEditorImpl;
import com.intellij.openapi.editor.impl.TextDrawingCallback;
import com.intellij.openapi.editor.impl.softwrap.SoftWrapAppliancePlaces;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageUtil;
import com.intellij.psi.impl.source.tree.injected.InlayModelWindow;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.Consumer;
import com.intellij.util.containers.WeakList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.IntFunction;

/**
 * @author Alexey
 */
public class EditorWindowImpl extends UserDataHolderBase implements EditorWindow, EditorEx {
  private final DocumentWindowImpl myDocumentWindow;
  private final DesktopEditorImpl myDelegate;
  private volatile PsiFile myInjectedFile;
  private final boolean myOneLine;
  private final CaretModelWindow myCaretModelDelegate;
  private final SelectionModelWindow mySelectionModelDelegate;
  private static final List<EditorWindowImpl> allEditors = new WeakList<>();
  private boolean myDisposed;
  private final MarkupModelWindow myMarkupModelDelegate;
  private final MarkupModelWindow myDocumentMarkupModelDelegate;
  private final FoldingModelWindow myFoldingModelWindow;
  private final SoftWrapModelWindow mySoftWrapModel;
  private final InlayModelWindow myInlayModel;

  public static Editor create(@Nonnull final DocumentWindowImpl documentRange, @Nonnull final DesktopEditorImpl editor, @Nonnull final PsiFile injectedFile) {
    assert documentRange.isValid();
    assert injectedFile.isValid();
    EditorWindowImpl window;
    synchronized (allEditors) {
      for (EditorWindowImpl editorWindow : allEditors) {
        if (editorWindow.getDocument() == documentRange && editorWindow.getDelegate() == editor) {
          editorWindow.myInjectedFile = injectedFile;
          if (editorWindow.isValid()) {
            return editorWindow;
          }
        }
        if (editorWindow.getDocument().areRangesEqual(documentRange)) {
          //int i = 0;
        }
      }
      window = new EditorWindowImpl(documentRange, editor, injectedFile, documentRange.isOneLine());
      allEditors.add(window);
    }
    assert window.isValid();
    return window;
  }

  private EditorWindowImpl(@Nonnull DocumentWindowImpl documentWindow,
                           @Nonnull final DesktopEditorImpl delegate,
                           @Nonnull PsiFile injectedFile,
                           boolean oneLine) {
    myDocumentWindow = documentWindow;
    myDelegate = delegate;
    myInjectedFile = injectedFile;
    myOneLine = oneLine;
    myCaretModelDelegate = new CaretModelWindow(myDelegate.getCaretModel(), this);
    mySelectionModelDelegate = new SelectionModelWindow(myDelegate, myDocumentWindow,this);
    myMarkupModelDelegate = new MarkupModelWindow(myDelegate.getMarkupModel(), myDocumentWindow);
    myDocumentMarkupModelDelegate = new MarkupModelWindow(myDelegate.getFilteredDocumentMarkupModel(), myDocumentWindow);
    myFoldingModelWindow = new FoldingModelWindow(delegate.getFoldingModel(), documentWindow, this);
    mySoftWrapModel = new SoftWrapModelWindow();
    myInlayModel = new InlayModelWindow();
  }

  public static void disposeInvalidEditors() {
    ApplicationManager.getApplication().assertWriteAccessAllowed();
    Iterator<EditorWindowImpl> iterator = allEditors.iterator();
    while (iterator.hasNext()) {
      EditorWindowImpl editorWindow = iterator.next();
      if (!editorWindow.isValid()) {
        editorWindow.dispose();

        InjectedLanguageUtil.clearCaches(editorWindow.myInjectedFile, editorWindow.getDocument());
        iterator.remove();
      }
    }
  }

  @Override
  public boolean isValid() {
    return !isDisposed() && !myInjectedFile.getProject().isDisposed() && myInjectedFile.isValid() && myDocumentWindow.isValid();
  }

  private void checkValid() {
    PsiUtilCore.ensureValid(myInjectedFile);
    if (!isValid()) {
      StringBuilder reason = new StringBuilder("Not valid");
      if (myDisposed) reason.append("; editorWindow: disposed");
      if (!myDocumentWindow.isValid()) reason.append("; documentWindow: invalid");
      if (myDelegate.isDisposed()) reason.append("; editor: disposed");
      if (myInjectedFile.getProject().isDisposed()) reason.append("; project: disposed");
      throw new AssertionError(reason.toString());
    }
  }

  @Override
  @Nonnull
  public PsiFile getInjectedFile() {
    return myInjectedFile;
  }

  @Override
  @Nonnull
  public LogicalPosition hostToInjected(@Nonnull LogicalPosition hPos) {
    checkValid();
    DocumentEx hostDocument = myDelegate.getDocument();
    int hLineEndOffset = hPos.line >= hostDocument.getLineCount() ? hostDocument.getTextLength() : hostDocument.getLineEndOffset(hPos.line);
    LogicalPosition hLineEndPos = myDelegate.offsetToLogicalPosition(hLineEndOffset);
    if (hLineEndPos.column < hPos.column) {
      // in virtual space
      LogicalPosition iPos = myDocumentWindow.hostToInjectedInVirtualSpace(hPos);
      if (iPos != null) {
        return iPos;
      }
    }

    int hOffset = myDelegate.logicalPositionToOffset(hPos);
    int iOffset = myDocumentWindow.hostToInjected(hOffset);
    return offsetToLogicalPosition(iOffset);
  }

  @Override
  @Nonnull
  public LogicalPosition injectedToHost(@Nonnull LogicalPosition pos) {
    checkValid();

    int offset = logicalPositionToOffset(pos);
    LogicalPosition samePos = offsetToLogicalPosition(offset);

    int virtualSpaceDelta = offset < myDocumentWindow.getTextLength() && samePos.line == pos.line && samePos.column < pos.column ?
                            pos.column - samePos.column : 0;

    LogicalPosition hostPos = myDelegate.offsetToLogicalPosition(myDocumentWindow.injectedToHost(offset));
    return new LogicalPosition(hostPos.line, hostPos.column + virtualSpaceDelta);
  }

  private void dispose() {
    assert !myDisposed;
    myCaretModelDelegate.disposeModel();

    for (EditorMouseListener wrapper : myEditorMouseListeners.wrappers()) {
      myDelegate.removeEditorMouseListener(wrapper);
    }
    myEditorMouseListeners.clear();
    for (EditorMouseMotionListener wrapper : myEditorMouseMotionListeners.wrappers()) {
      myDelegate.removeEditorMouseMotionListener(wrapper);
    }
    myEditorMouseMotionListeners.clear();

    myDisposed = true;
    Disposer.dispose(myDocumentWindow);
  }

  @Override
  public void setViewer(boolean isViewer) {
    myDelegate.setViewer(isViewer);
  }

  @Override
  public boolean isViewer() {
    return myDelegate.isViewer();
  }

  @Override
  public boolean isRendererMode() {
    return myDelegate.isRendererMode();
  }

  @Override
  public void setRendererMode(final boolean isRendererMode) {
    myDelegate.setRendererMode(isRendererMode);
  }

  @Override
  public void setFile(final VirtualFile vFile) {
    myDelegate.setFile(vFile);
  }

  @Override
  public void setHeaderComponent(@Nullable JComponent header) {

  }

  @Override
  public boolean hasHeaderComponent() {
    return false;
  }

  @Override
  @Nullable
  public JComponent getHeaderComponent() {
    return null;
  }

  @Override
  public TextDrawingCallback getTextDrawingCallback() {
    return myDelegate.getTextDrawingCallback();
  }

  @Override
  @Nonnull
  public SelectionModel getSelectionModel() {
    return mySelectionModelDelegate;
  }

  @Override
  @Nonnull
  public MarkupModelEx getMarkupModel() {
    return myMarkupModelDelegate;
  }

  @Nonnull
  @Override
  public MarkupModelEx getFilteredDocumentMarkupModel() {
    return myDocumentMarkupModelDelegate;
  }

  @Override
  @Nonnull
  public FoldingModelEx getFoldingModel() {
    return myFoldingModelWindow;
  }

  @Override
  @Nonnull
  public CaretModel getCaretModel() {
    return myCaretModelDelegate;
  }

  @Override
  @Nonnull
  public ScrollingModelEx getScrollingModel() {
    return myDelegate.getScrollingModel();
  }

  @Override
  @Nonnull
  public SoftWrapModelEx getSoftWrapModel() {
    return mySoftWrapModel;
  }

  @Override
  @Nonnull
  public EditorSettings getSettings() {
    return myDelegate.getSettings();
  }

  @Nonnull
  @Override
  public InlayModel getInlayModel() {
    return myInlayModel;
  }

  @Override
  public void reinitSettings() {
    myDelegate.reinitSettings();
  }

  @Override
  public void setFontSize(final int fontSize) {
    myDelegate.setFontSize(fontSize);
  }

  @Override
  public void setHighlighter(@Nonnull final EditorHighlighter highlighter) {
    myDelegate.setHighlighter(highlighter);
  }

  @Nonnull
  @Override
  public EditorHighlighter getHighlighter() {
    EditorColorsScheme scheme = EditorColorsManager.getInstance().getGlobalScheme();
    SyntaxHighlighter syntaxHighlighter =
            SyntaxHighlighterFactory.getSyntaxHighlighter(myInjectedFile.getLanguage(), getProject(), myInjectedFile.getVirtualFile());
    EditorHighlighter highlighter = HighlighterFactory.createHighlighter(syntaxHighlighter, scheme);
    highlighter.setText(getDocument().getText());
    highlighter.setEditor(new LightHighlighterClient(getDocument(), getProject()));
    return highlighter;
  }

  @Override
  public JComponent getPermanentHeaderComponent() {
    return myDelegate.getPermanentHeaderComponent();
  }

  @Override
  public void setPermanentHeaderComponent(JComponent component) {
    myDelegate.setPermanentHeaderComponent(component);
  }

  @Override
  @Nonnull
  public JComponent getContentComponent() {
    return myDelegate.getContentComponent();
  }

  @Nonnull
  @Override
  public EditorGutterComponentEx getGutterComponentEx() {
    return myDelegate.getGutterComponentEx();
  }

  @Override
  public void addPropertyChangeListener(@Nonnull final PropertyChangeListener listener) {
    myDelegate.addPropertyChangeListener(listener);
  }

  @Override
  public void addPropertyChangeListener(@Nonnull PropertyChangeListener listener, @Nonnull Disposable parentDisposable) {
    myDelegate.addPropertyChangeListener(listener, parentDisposable);
  }

  @Override
  public void removePropertyChangeListener(@Nonnull final PropertyChangeListener listener) {
    myDelegate.removePropertyChangeListener(listener);
  }

  @Override
  public void setInsertMode(final boolean mode) {
    myDelegate.setInsertMode(mode);
  }

  @Override
  public boolean isInsertMode() {
    return myDelegate.isInsertMode();
  }

  @Override
  public void setColumnMode(final boolean mode) {
    myDelegate.setColumnMode(mode);
  }

  @Override
  public boolean isColumnMode() {
    return myDelegate.isColumnMode();
  }

  @Override
  @Nonnull
  public VisualPosition xyToVisualPosition(@Nonnull final Point p) {
    return logicalToVisualPosition(xyToLogicalPosition(p));
  }

  @Nonnull
  @Override
  public VisualPosition xyToVisualPosition(@Nonnull Point2D p) {
    checkValid();
    Point2D pp = p.getX() >= 0 && p.getY() >= 0 ? p : new Point2D.Double(Math.max(p.getX(), 0), Math.max(p.getY(), 0));
    LogicalPosition hostPos = myDelegate.visualToLogicalPosition(myDelegate.xyToVisualPosition(pp));
    return logicalToVisualPosition(hostToInjected(hostPos));
  }

  @Override
  @Nonnull
  public VisualPosition offsetToVisualPosition(final int offset) {
    return logicalToVisualPosition(offsetToLogicalPosition(offset));
  }

  @Override
  @Nonnull
  public VisualPosition offsetToVisualPosition(int offset, boolean leanForward, boolean beforeSoftWrap) {
    return logicalToVisualPosition(offsetToLogicalPosition(offset).leanForward(leanForward));
  }

  @Override
  @Nonnull
  public LogicalPosition offsetToLogicalPosition(final int offset) {
    checkValid();
    int lineNumber = myDocumentWindow.getLineNumber(offset);
    int lineStartOffset = myDocumentWindow.getLineStartOffset(lineNumber);
    int column = calcLogicalColumnNumber(offset-lineStartOffset, lineNumber, lineStartOffset);
    return new LogicalPosition(lineNumber, column);
  }

  @Nonnull
  @Override
  public EditorColorsScheme createBoundColorSchemeDelegate(@Nullable EditorColorsScheme customGlobalScheme) {
    return myDelegate.createBoundColorSchemeDelegate(customGlobalScheme);
  }

  @Override
  @Nonnull
  public LogicalPosition xyToLogicalPosition(@Nonnull final Point p) {
    checkValid();
    LogicalPosition hostPos = myDelegate.xyToLogicalPosition(p);
    return hostToInjected(hostPos);
  }

  @Override
  @Nonnull
  public Point logicalPositionToXY(@Nonnull final LogicalPosition pos) {
    checkValid();
    LogicalPosition hostPos = injectedToHost(pos);
    return myDelegate.logicalPositionToXY(hostPos);
  }

  @Override
  @Nonnull
  public Point visualPositionToXY(@Nonnull final VisualPosition pos) {
    checkValid();
    return logicalPositionToXY(visualToLogicalPosition(pos));
  }

  @Nonnull
  @Override
  public Point2D visualPositionToPoint2D(@Nonnull VisualPosition pos) {
    checkValid();
    LogicalPosition hostLogical = injectedToHost(visualToLogicalPosition(pos));
    VisualPosition hostVisual = myDelegate.logicalToVisualPosition(hostLogical);
    return myDelegate.visualPositionToPoint2D(hostVisual);
  }

  @Override
  public void repaint(final int startOffset, final int endOffset) {
    checkValid();
    myDelegate.repaint(myDocumentWindow.injectedToHost(startOffset), myDocumentWindow.injectedToHost(endOffset));
  }

  @Override
  @Nonnull
  public DocumentWindowImpl getDocument() {
    return myDocumentWindow;
  }

  @Override
  @Nonnull
  public JComponent getComponent() {
    return myDelegate.getComponent();
  }

  private final ListenerWrapperMap<EditorMouseListener> myEditorMouseListeners = new ListenerWrapperMap<>();
  @Override
  public void addEditorMouseListener(@Nonnull final EditorMouseListener listener) {
    checkValid();
    EditorMouseListener wrapper = new EditorMouseListener() {
      @Override
      public void mousePressed(EditorMouseEvent e) {
        listener.mousePressed(new EditorMouseEvent(EditorWindowImpl.this, e.getMouseEvent(), e.getArea()));
      }

      @Override
      public void mouseClicked(EditorMouseEvent e) {
        listener.mouseClicked(new EditorMouseEvent(EditorWindowImpl.this, e.getMouseEvent(), e.getArea()));
      }

      @Override
      public void mouseReleased(EditorMouseEvent e) {
        listener.mouseReleased(new EditorMouseEvent(EditorWindowImpl.this, e.getMouseEvent(), e.getArea()));
      }

      @Override
      public void mouseEntered(EditorMouseEvent e) {
        listener.mouseEntered(new EditorMouseEvent(EditorWindowImpl.this, e.getMouseEvent(), e.getArea()));
      }

      @Override
      public void mouseExited(EditorMouseEvent e) {
        listener.mouseExited(new EditorMouseEvent(EditorWindowImpl.this, e.getMouseEvent(), e.getArea()));
      }
    };
    myEditorMouseListeners.registerWrapper(listener, wrapper);

    myDelegate.addEditorMouseListener(wrapper);
  }

  @Override
  public void removeEditorMouseListener(@Nonnull final EditorMouseListener listener) {
    EditorMouseListener wrapper = myEditorMouseListeners.removeWrapper(listener);
    // HintManager might have an old editor instance
    if (wrapper != null) {
      myDelegate.removeEditorMouseListener(wrapper);
    }
  }

  private final ListenerWrapperMap<EditorMouseMotionListener> myEditorMouseMotionListeners = new ListenerWrapperMap<>();
  @Override
  public void addEditorMouseMotionListener(@Nonnull final EditorMouseMotionListener listener) {
    checkValid();
    EditorMouseMotionListener wrapper = new EditorMouseMotionListener() {
      @Override
      public void mouseMoved(EditorMouseEvent e) {
        listener.mouseMoved(new EditorMouseEvent(EditorWindowImpl.this, e.getMouseEvent(), e.getArea()));
      }

      @Override
      public void mouseDragged(EditorMouseEvent e) {
        listener.mouseDragged(new EditorMouseEvent(EditorWindowImpl.this, e.getMouseEvent(), e.getArea()));
      }
    };
    myEditorMouseMotionListeners.registerWrapper(listener, wrapper);
    myDelegate.addEditorMouseMotionListener(wrapper);
  }

  @Override
  public void removeEditorMouseMotionListener(@Nonnull final EditorMouseMotionListener listener) {
    EditorMouseMotionListener wrapper = myEditorMouseMotionListeners.removeWrapper(listener);
    if (wrapper != null) {
      myDelegate.removeEditorMouseMotionListener(wrapper);
    }
  }

  @Override
  public boolean isDisposed() {
    return myDisposed || myDelegate.isDisposed();
  }

  @Override
  public void setBackgroundColor(final Color color) {
    myDelegate.setBackgroundColor(color);
  }

  @Override
  public Color getBackgroundColor() {
    return myDelegate.getBackgroundColor();
  }

  @Override
  public int getMaxWidthInRange(final int startOffset, final int endOffset) {
    return myDelegate.getMaxWidthInRange(startOffset, endOffset);
  }

  @Override
  public int getLineHeight() {
    return myDelegate.getLineHeight();
  }

  @Override
  public Dimension getContentSize() {
    return myDelegate.getContentSize();
  }

  @Nonnull
  @Override
  public JScrollPane getScrollPane() {
    return myDelegate.getScrollPane();
  }

  @Override
  public void setBorder(Border border) {
    myDelegate.setBorder(border);
  }

  @Override
  public Insets getInsets() {
    return myDelegate.getInsets();
  }

  @Override
  public int logicalPositionToOffset(@Nonnull final LogicalPosition pos) {
    int lineStartOffset = myDocumentWindow.getLineStartOffset(pos.line);
    return calcOffset(pos.column, pos.line, lineStartOffset);
  }

  private int calcLogicalColumnNumber(int offsetInLine, int lineNumber, int lineStartOffset) {
    if (myDocumentWindow.getTextLength() == 0) return 0;

    if (offsetInLine==0) return 0;
    int end = myDocumentWindow.getLineEndOffset(lineNumber);
    if (offsetInLine > end- lineStartOffset) offsetInLine = end - lineStartOffset;

    CharSequence text = myDocumentWindow.getCharsSequence();
    return EditorUtil.calcColumnNumber(this, text, lineStartOffset, lineStartOffset +offsetInLine);
  }

  private int calcOffset(int col, int lineNumber, int lineStartOffset) {
    CharSequence text = myDocumentWindow.getImmutableCharSequence();
    int tabSize = EditorUtil.getTabSize(myDelegate);
    int end = myDocumentWindow.getLineEndOffset(lineNumber);
    int currentColumn = 0;
    for (int i = lineStartOffset; i < end; i++) {
      char c = text.charAt(i);
      if (c == '\t') {
        currentColumn = (currentColumn / tabSize + 1) * tabSize;
      }
      else {
        currentColumn++;
      }
      if (col < currentColumn) return i;
    }
    return end;
  }

  // assuming there is no folding in injected documents
  @Override
  @Nonnull
  public VisualPosition logicalToVisualPosition(@Nonnull final LogicalPosition pos) {
    checkValid();
    return new VisualPosition(pos.line, pos.column);
  }

  @Override
  @Nonnull
  public LogicalPosition visualToLogicalPosition(@Nonnull final VisualPosition pos) {
    checkValid();
    return new LogicalPosition(pos.line, pos.column);
  }

  @Nonnull
  @Override
  public DataContext getDataContext() {
    return myDelegate.getDataContext();
  }

  @Override
  public EditorMouseEventArea getMouseEventArea(@Nonnull final MouseEvent e) {
    return myDelegate.getMouseEventArea(e);
  }

  @Override
  public boolean setCaretVisible(final boolean b) {
    return myDelegate.setCaretVisible(b);
  }

  @Override
  public boolean setCaretEnabled(boolean enabled) {
    return myDelegate.setCaretEnabled(enabled);
  }

  @Override
  public void addFocusListener(@Nonnull final FocusChangeListener listener) {
    myDelegate.addFocusListener(listener);
  }

  @Override
  public void addFocusListener(@Nonnull FocusChangeListener listener, @Nonnull Disposable parentDisposable) {
    myDelegate.addFocusListener(listener, parentDisposable);
  }

  @Override
  public Project getProject() {
    return myDelegate.getProject();
  }

  @Override
  public boolean isOneLineMode() {
    return myOneLine;
  }

  @Override
  public void setOneLineMode(final boolean isOneLineMode) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isEmbeddedIntoDialogWrapper() {
    return myDelegate.isEmbeddedIntoDialogWrapper();
  }

  @Override
  public void setEmbeddedIntoDialogWrapper(final boolean b) {
    myDelegate.setEmbeddedIntoDialogWrapper(b);
  }

  @Override
  public VirtualFile getVirtualFile() {
    return myDelegate.getVirtualFile();
  }

  @Override
  public CopyProvider getCopyProvider() {
    return myDelegate.getCopyProvider();
  }

  @Override
  public CutProvider getCutProvider() {
    return myDelegate.getCutProvider();
  }

  @Override
  public PasteProvider getPasteProvider() {
    return myDelegate.getPasteProvider();
  }

  @Override
  public DeleteProvider getDeleteProvider() {
    return myDelegate.getDeleteProvider();
  }

  @Override
  public void setColorsScheme(@Nonnull final EditorColorsScheme scheme) {
    myDelegate.setColorsScheme(scheme);
  }

  @Override
  @Nonnull
  public EditorColorsScheme getColorsScheme() {
    return myDelegate.getColorsScheme();
  }

  @Override
  public void setVerticalScrollbarOrientation(final int type) {
    myDelegate.setVerticalScrollbarOrientation(type);
  }

  @Override
  public int getVerticalScrollbarOrientation() {
    return myDelegate.getVerticalScrollbarOrientation();
  }

  @Override
  public void setVerticalScrollbarVisible(final boolean b) {
    myDelegate.setVerticalScrollbarVisible(b);
  }

  @Override
  public void setHorizontalScrollbarVisible(final boolean b) {
    myDelegate.setHorizontalScrollbarVisible(b);
  }

  @Override
  public boolean processKeyTyped(@Nonnull final KeyEvent e) {
    return myDelegate.processKeyTyped(e);
  }

  @Override
  @Nonnull
  public EditorGutter getGutter() {
    return myDelegate.getGutter();
  }

  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final EditorWindowImpl that = (EditorWindowImpl)o;

    DocumentWindow thatWindow = that.getDocument();
    return myDelegate.equals(that.myDelegate) && myDocumentWindow.equals(thatWindow);
  }

  public int hashCode() {
    return myDocumentWindow.hashCode();
  }

  @Nonnull
  @Override
  public Editor getDelegate() {
    return myDelegate;
  }

  @Override
  public int calcColumnNumber(@Nonnull final CharSequence text, final int start, final int offset, final int tabSize) {
    int column = 0;
    for (int i = start; i < offset; i++) {
      char c = text.charAt(i);
      if (c == '\t') {
        column = ((column / tabSize) + 1) * tabSize;
      }
      else {
        column++;
      }
    }
    return column;
  }

  @Override
  public int calcColumnNumber(int offset, int lineIndex) {
    return calcColumnNumber(myDocumentWindow.getImmutableCharSequence(), myDocumentWindow.getLineStartOffset(lineIndex), offset,
                            EditorUtil.getTabSize(myDelegate));
  }

  @Nonnull
  @Override
  public IndentsModel getIndentsModel() {
    return myDelegate.getIndentsModel();
  }

  @Override
  public void setSoftWrapAppliancePlace(@Nonnull SoftWrapAppliancePlaces place) {
    myDelegate.setSoftWrapAppliancePlace(place);
  }

  @Override
  public void setPlaceholder(@Nullable CharSequence text) {
    myDelegate.setPlaceholder(text);
  }

  @Override
  public void setPlaceholderAttributes(@Nullable TextAttributes attributes) {
    myDelegate.setPlaceholderAttributes(attributes);
  }

  @Override
  public void setShowPlaceholderWhenFocused(boolean show) {
    myDelegate.setShowPlaceholderWhenFocused(show);
  }

  @Override
  public boolean isStickySelection() {
    return myDelegate.isStickySelection();
  }

  @Override
  public void setStickySelection(boolean enable) {
    myDelegate.setStickySelection(enable);
  }

  @Override
  public boolean isPurePaintingMode() {
    return myDelegate.isPurePaintingMode();
  }

  @Override
  public void setPurePaintingMode(boolean enabled) {
    myDelegate.setPurePaintingMode(enabled);
  }

  @Override
  public void registerLineExtensionPainter(IntFunction<Collection<LineExtensionInfo>> lineExtensionPainter) {
    myDelegate.registerLineExtensionPainter(lineExtensionPainter);
  }

  @Override
  public void registerScrollBarRepaintCallback(@Nullable Consumer<Graphics> callback) {
    myDelegate.registerScrollBarRepaintCallback(callback);
  }

  @Override
  public void setPrefixTextAndAttributes(@Nullable String prefixText, @Nullable TextAttributes attributes) {
    myDelegate.setPrefixTextAndAttributes(prefixText, attributes);
  }

  @Override
  public int getPrefixTextWidthInPixels() {
    return myDelegate.getPrefixTextWidthInPixels();
  }

  @Override
  public String toString() {
    return super.toString() + "[disposed=" + myDisposed + "; valid=" + isValid() + "]";
  }

  @Override
  public int getExpectedCaretOffset() {
    return myDocumentWindow.hostToInjected(myDelegate.getExpectedCaretOffset());
  }

  @Override
  public void setContextMenuGroupId(@Nullable String groupId) {
    myDelegate.setContextMenuGroupId(groupId);
  }

  @Nullable
  @Override
  public String getContextMenuGroupId() {
    return myDelegate.getContextMenuGroupId();
  }
}
