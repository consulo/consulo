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
package com.intellij.openapi.editor.textarea;

import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.event.EditorMouseEventArea;
import com.intellij.openapi.editor.event.EditorMouseListener;
import com.intellij.openapi.editor.event.EditorMouseMotionListener;
import com.intellij.openapi.editor.impl.EmptyIndentsModel;
import com.intellij.openapi.editor.impl.SettingsImpl;
import com.intellij.openapi.editor.markup.MarkupModel;
import com.intellij.openapi.project.Project;
import consulo.util.dataholder.UserDataHolderBase;
import com.intellij.util.ui.JBUI;
import javax.annotation.Nonnull;

import javax.annotation.Nullable;
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

  public TextComponentEditorImpl(final Project project, @Nonnull JTextComponent textComponent) {
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
  @Nonnull
  public Document getDocument() {
    return myDocument;
  }

  @Override
  public boolean isViewer() {
    return !myTextComponent.isEditable();
  }

  @Override
  @Nonnull
  public JComponent getComponent() {
    return myTextComponent;
  }

  @Override
  @Nonnull
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
  @Nonnull
  public TextComponentSelectionModel getSelectionModel() {
    return mySelectionModel;
  }

  @Override
  @Nonnull
  public MarkupModel getMarkupModel() {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  @Nonnull
  public FoldingModel getFoldingModel() {
    return myFoldingModel;
  }

  @Override
  @Nonnull
  public ScrollingModel getScrollingModel() {
    return myScrollingModel;
  }

  @Override
  @Nonnull
  public CaretModel getCaretModel() {
    return myCaretModel;
  }

  @Override
  @Nonnull
  public SoftWrapModel getSoftWrapModel() {
    return mySoftWrapModel;
  }

  @Nonnull
  @Override
  public EditorKind getEditorKind() {
    return EditorKind.UNTYPED;
  }

  @Nonnull
  @Override
  public InlayModel getInlayModel() {
    return new TextComponentInlayModel();
  }

  @Override
  @Nonnull
  public EditorSettings getSettings() {
    if (mySettings == null) {
      mySettings = new SettingsImpl();
    }
    return mySettings;
  }

  @Override
  @Nonnull
  public EditorColorsScheme getColorsScheme() {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public int getLineHeight() {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  @Nonnull
  public Point logicalPositionToXY(@Nonnull final LogicalPosition pos) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public int logicalPositionToOffset(@Nonnull final LogicalPosition pos) {
    if (pos.line >= myDocument.getLineCount()) {
      return myDocument.getTextLength();
    }
    return myDocument.getLineStartOffset(pos.line) + pos.column;
  }

  @Override
  @Nonnull
  public VisualPosition logicalToVisualPosition(@Nonnull final LogicalPosition logicalPos) {
    return new VisualPosition(logicalPos.line, logicalPos.column);
  }

  @Override
  @Nonnull
  public Point visualPositionToXY(@Nonnull final VisualPosition visible) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Nonnull
  @Override
  public Point2D visualPositionToPoint2D(@Nonnull VisualPosition pos) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  @Nonnull
  public LogicalPosition visualToLogicalPosition(@Nonnull final VisualPosition visiblePos) {
    return new LogicalPosition(visiblePos.line, visiblePos.column);
  }

  @Override
  @Nonnull
  public LogicalPosition offsetToLogicalPosition(final int offset) {
    int line = myDocument.getLineNumber(offset);
    final int lineStartOffset = myDocument.getLineStartOffset(line);
    return new LogicalPosition(line, offset - lineStartOffset);
  }

  @Override
  @Nonnull
  public VisualPosition offsetToVisualPosition(final int offset) {
    int line = myDocument.getLineNumber(offset);
    final int lineStartOffset = myDocument.getLineStartOffset(line);
    return new VisualPosition(line, offset - lineStartOffset);
  }

  @Nonnull
  @Override
  public VisualPosition offsetToVisualPosition(int offset, boolean leanForward, boolean beforeSoftWrap) {
    return offsetToVisualPosition(offset);
  }

  @Override
  @Nonnull
  public LogicalPosition xyToLogicalPosition(@Nonnull final Point p) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  @Nonnull
  public VisualPosition xyToVisualPosition(@Nonnull final Point p) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Nonnull
  @Override
  public VisualPosition xyToVisualPosition(@Nonnull Point2D p) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public void addEditorMouseListener(@Nonnull final EditorMouseListener listener) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public void removeEditorMouseListener(@Nonnull final EditorMouseListener listener) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public void addEditorMouseMotionListener(@Nonnull final EditorMouseMotionListener listener) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public void removeEditorMouseMotionListener(@Nonnull final EditorMouseMotionListener listener) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public boolean isDisposed() {
    return false;
  }

  @Override
  @Nullable
  public Project getProject() {
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
  @Nonnull
  public EditorGutter getGutter() {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  @Nullable
  public EditorMouseEventArea getMouseEventArea(@Nonnull final MouseEvent e) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public void setHeaderComponent(@Nullable final JComponent header) {
    throw new UnsupportedOperationException("Not implemented");
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

  @Nonnull
  @Override
  public IndentsModel getIndentsModel() {
    return new EmptyIndentsModel();
  }
}
