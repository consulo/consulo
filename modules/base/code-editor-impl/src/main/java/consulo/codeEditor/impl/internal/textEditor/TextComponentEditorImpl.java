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
package consulo.codeEditor.impl.internal.textEditor;

import consulo.codeEditor.*;
import consulo.codeEditor.event.EditorMouseEventArea;
import consulo.codeEditor.event.EditorMouseListener;
import consulo.codeEditor.event.EditorMouseMotionListener;
import consulo.codeEditor.impl.EmptyIndentsModel;
import consulo.codeEditor.impl.SettingsImpl;
import consulo.codeEditor.markup.MarkupModel;
import consulo.colorScheme.EditorColorsScheme;
import consulo.dataContext.DataContext;
import consulo.document.Document;
import consulo.project.Project;
import consulo.ui.ex.awt.JBUI;
import consulo.util.dataholder.UserDataHolderBase;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;

/**
 * @author yole
 */
public class TextComponentEditorImpl extends UserDataHolderBase implements TextComponentEditor {
  private final Project myProject;
  private final JTextComponent myTextComponent;
  private final TextComponentDocument myDocument;
  private final TextComponentCaretModel myCaretModel;
  private final TextComponentSelectionModel mySelectionModel;
  private final TextComponentScrollingModel myScrollingModel;
  private final TextComponentSoftWrapModel mySoftWrapModel;
  private final TextComponentFoldingModel myFoldingModel;
  private EditorSettings mySettings;

  public TextComponentEditorImpl(Project project, JTextComponent textComponent) {
    myProject = project;
    myTextComponent = textComponent;
    if (textComponent instanceof JTextArea) {
      myDocument = new TextAreaDocument((JTextArea) textComponent);
    }
    else {
      myDocument = new TextComponentDocument(textComponent);
    }
    myCaretModel = new TextComponentCaretModel(textComponent, this);
    mySelectionModel = new TextComponentSelectionModel(textComponent, this);
    myScrollingModel = new TextComponentScrollingModel(textComponent);
    mySoftWrapModel = new TextComponentSoftWrapModel();
    myFoldingModel = new TextComponentFoldingModel();
  }

  @Override
  
  public Document getDocument() {
    return myDocument;
  }

  @Override
  public boolean isViewer() {
    return !myTextComponent.isEditable();
  }

  @Override
  
  public JComponent getComponent() {
    return myTextComponent;
  }

  @Override
  
  public JComponent getContentComponent() {
    return myTextComponent;
  }

  @Override
  public void setBorder(@Nullable Border border) {
  }

  @Override
  public Insets getInsets() {
    return JBUI.emptyInsets();
  }

  @Override
  
  public TextComponentSelectionModel getSelectionModel() {
    return mySelectionModel;
  }

  @Override
  
  public MarkupModel getMarkupModel() {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  
  public FoldingModel getFoldingModel() {
    return myFoldingModel;
  }

  @Override
  
  public ScrollingModel getScrollingModel() {
    return myScrollingModel;
  }

  @Override
  
  public CaretModel getCaretModel() {
    return myCaretModel;
  }

  @Override
  
  public SoftWrapModel getSoftWrapModel() {
    return mySoftWrapModel;
  }

  
  @Override
  public EditorKind getEditorKind() {
    return EditorKind.UNTYPED;
  }

  
  @Override
  public DataContext getDataContext() {
    throw new UnsupportedOperationException();
  }

  
  @Override
  public InlayModel getInlayModel() {
    return new TextComponentInlayModel();
  }

  @Override
  
  public EditorSettings getSettings() {
    if (mySettings == null) {
      mySettings = new SettingsImpl();
    }
    return mySettings;
  }

  @Override
  
  public EditorColorsScheme getColorsScheme() {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public int getLineHeight() {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  
  public Point logicalPositionToXY(LogicalPosition pos) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public int logicalPositionToOffset(LogicalPosition pos) {
    if (pos.line >= myDocument.getLineCount()) {
      return myDocument.getTextLength();
    }
    return myDocument.getLineStartOffset(pos.line) + pos.column;
  }

  @Override
  
  public VisualPosition logicalToVisualPosition(LogicalPosition logicalPos) {
    return new VisualPosition(logicalPos.line, logicalPos.column);
  }

  @Override
  
  public Point visualPositionToXY(VisualPosition visible) {
    throw new UnsupportedOperationException("Not implemented");
  }

  
  @Override
  public Point2D visualPositionToPoint2D(VisualPosition pos) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  
  public LogicalPosition visualToLogicalPosition(VisualPosition visiblePos) {
    return new LogicalPosition(visiblePos.line, visiblePos.column);
  }

  @Override
  
  public LogicalPosition offsetToLogicalPosition(int offset) {
    int line = myDocument.getLineNumber(offset);
    int lineStartOffset = myDocument.getLineStartOffset(line);
    return new LogicalPosition(line, offset - lineStartOffset);
  }

  @Override
  
  public VisualPosition offsetToVisualPosition(int offset) {
    int line = myDocument.getLineNumber(offset);
    int lineStartOffset = myDocument.getLineStartOffset(line);
    return new VisualPosition(line, offset - lineStartOffset);
  }

  
  @Override
  public VisualPosition offsetToVisualPosition(int offset, boolean leanForward, boolean beforeSoftWrap) {
    return offsetToVisualPosition(offset);
  }

  @Override
  
  public LogicalPosition xyToLogicalPosition(Point p) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  
  public VisualPosition xyToVisualPosition(Point p) {
    throw new UnsupportedOperationException("Not implemented");
  }

  
  @Override
  public VisualPosition xyToVisualPosition(Point2D p) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public void addEditorMouseListener(EditorMouseListener listener) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public void removeEditorMouseListener(EditorMouseListener listener) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public void addEditorMouseMotionListener(EditorMouseMotionListener listener) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public void removeEditorMouseMotionListener(EditorMouseMotionListener listener) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public boolean isDisposed() {
    return false;
  }

  @Override
  public @Nullable Project getProject() {
    return myProject;
  }

  @Override
  public boolean isInsertMode() {
    return true;
  }

  @Override
  public boolean isColumnMode() {
    return false;
  }

  @Override
  public boolean isOneLineMode() {
    return !(myTextComponent instanceof JTextArea);
  }

  @Override
  
  public EditorGutter getGutter() {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public @Nullable EditorMouseEventArea getMouseEventArea(MouseEvent e) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public void setHeaderComponent(@Nullable JComponent header) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public boolean hasHeaderComponent() {
    return false;
  }

  @Override
  public @Nullable JComponent getHeaderComponent() {
    return null;
  }

  
  @Override
  public IndentsModel getIndentsModel() {
    return new EmptyIndentsModel();
  }
}
