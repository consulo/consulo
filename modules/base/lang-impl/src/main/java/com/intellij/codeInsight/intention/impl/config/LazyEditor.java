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
package com.intellij.codeInsight.intention.impl.config;

import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.event.EditorMouseEventArea;
import com.intellij.openapi.editor.event.EditorMouseListener;
import com.intellij.openapi.editor.event.EditorMouseMotionListener;
import com.intellij.openapi.editor.markup.MarkupModel;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import consulo.util.dataholder.UserDataHolderBase;
import com.intellij.psi.PsiFile;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;

/**
 * @author Dmitry Avdeev
 */
class LazyEditor extends UserDataHolderBase implements Editor {

  private final PsiFile myFile;
  private Editor myEditor;

  LazyEditor(PsiFile file) {
    myFile = file;
  }

  private Editor getEditor() {
    if (myEditor == null) {
      final Project project = myFile.getProject();
      myEditor = FileEditorManager.getInstance(project).openTextEditor(new OpenFileDescriptor(project, myFile.getVirtualFile(), 0), false);
      assert myEditor != null;
    }
    return myEditor;
  }

  @Override
  @Nonnull
  public Document getDocument() {
    return getEditor().getDocument();
  }

  @Override
  public boolean isViewer() {
    return getEditor().isViewer();
  }

  @Override
  @Nonnull
  public JComponent getComponent() {
    return getEditor().getComponent();
  }

  @Override
  @Nonnull
  public JComponent getContentComponent() {
    return getEditor().getContentComponent();
  }

  @Override
  public void setBorder(@Nullable Border border) {
    getEditor().setBorder(border);
  }

  @Override
  public Insets getInsets() {
    return getEditor().getInsets();
  }

  @Override
  @Nonnull
  public SelectionModel getSelectionModel() {
    return getEditor().getSelectionModel();
  }

  @Override
  @Nonnull
  public MarkupModel getMarkupModel() {
    return getEditor().getMarkupModel();
  }

  @Override
  @Nonnull
  public FoldingModel getFoldingModel() {
    return getEditor().getFoldingModel();
  }

  @Override
  @Nonnull
  public ScrollingModel getScrollingModel() {
    return getEditor().getScrollingModel();
  }

  @Override
  @Nonnull
  public CaretModel getCaretModel() {
    return getEditor().getCaretModel();
  }

  @Override
  @Nonnull
  public SoftWrapModel getSoftWrapModel() {
    return getEditor().getSoftWrapModel();
  }

  @Nonnull
  @Override
  public InlayModel getInlayModel() {
    return getEditor().getInlayModel();
  }

  @Nonnull
  @Override
  public EditorKind getEditorKind() {
    return getEditor().getEditorKind();
  }

  @Override
  @Nonnull
  public EditorSettings getSettings() {
    return getEditor().getSettings();
  }

  @Override
  @Nonnull
  public EditorColorsScheme getColorsScheme() {
    return getEditor().getColorsScheme();
  }

  @Override
  public int getLineHeight() {
    return getEditor().getLineHeight();
  }

  @Override
  @Nonnull
  public Point logicalPositionToXY(@Nonnull final LogicalPosition pos) {
    return getEditor().logicalPositionToXY(pos);
  }

  @Override
  public int logicalPositionToOffset(@Nonnull final LogicalPosition pos) {
    return getEditor().logicalPositionToOffset(pos);
  }

  @Override
  @Nonnull
  public VisualPosition logicalToVisualPosition(@Nonnull final LogicalPosition logicalPos) {
    return getEditor().logicalToVisualPosition(logicalPos);
  }

  @Override
  @Nonnull
  public Point visualPositionToXY(@Nonnull final VisualPosition visible) {
    return getEditor().visualPositionToXY(visible);
  }

  @Nonnull
  @Override
  public Point2D visualPositionToPoint2D(@Nonnull VisualPosition pos) {
    return getEditor().visualPositionToPoint2D(pos);
  }

  @Override
  @Nonnull
  public LogicalPosition visualToLogicalPosition(@Nonnull final VisualPosition visiblePos) {
    return getEditor().visualToLogicalPosition(visiblePos);
  }

  @Override
  @Nonnull
  public LogicalPosition offsetToLogicalPosition(final int offset) {
    return getEditor().offsetToLogicalPosition(offset);
  }

  @Override
  @Nonnull
  public VisualPosition offsetToVisualPosition(final int offset) {
    return getEditor().offsetToVisualPosition(offset);
  }

  @Override
  @Nonnull
  public VisualPosition offsetToVisualPosition(int offset, boolean leanForward, boolean beforeSoftWrap) {
    return getEditor().offsetToVisualPosition(offset, leanForward, beforeSoftWrap);
  }

  @Override
  @Nonnull
  public LogicalPosition xyToLogicalPosition(@Nonnull final Point p) {
    return getEditor().xyToLogicalPosition(p);
  }

  @Override
  @Nonnull
  public VisualPosition xyToVisualPosition(@Nonnull final Point p) {
    return getEditor().xyToVisualPosition(p);
  }

  @Nonnull
  @Override
  public VisualPosition xyToVisualPosition(@Nonnull Point2D p) {
    return getEditor().xyToVisualPosition(p);
  }

  @Override
  public void addEditorMouseListener(@Nonnull final EditorMouseListener listener) {
    getEditor().addEditorMouseListener(listener);
  }

  @Override
  public void removeEditorMouseListener(@Nonnull final EditorMouseListener listener) {
    getEditor().removeEditorMouseListener(listener);
  }

  @Override
  public void addEditorMouseMotionListener(@Nonnull final EditorMouseMotionListener listener) {
    getEditor().addEditorMouseMotionListener(listener);
  }

  @Override
  public void removeEditorMouseMotionListener(@Nonnull final EditorMouseMotionListener listener) {
    getEditor().removeEditorMouseMotionListener(listener);
  }

  @Override
  public boolean isDisposed() {
    return getEditor().isDisposed();
  }

  @Override
  @Nullable
  public Project getProject() {
    return getEditor().getProject();
  }

  @Override
  public boolean isInsertMode() {
    return getEditor().isInsertMode();
  }

  @Override
  public boolean isColumnMode() {
    return getEditor().isColumnMode();
  }

  @Override
  public boolean isOneLineMode() {
    return getEditor().isOneLineMode();
  }

  @Override
  @Nonnull
  public EditorGutter getGutter() {
    return getEditor().getGutter();
  }

  @Override
  @Nullable
  public EditorMouseEventArea getMouseEventArea(@Nonnull final MouseEvent e) {
    return getEditor().getMouseEventArea(e);
  }

  @Override
  public void setHeaderComponent(@Nullable final JComponent header) {
    getEditor().setHeaderComponent(header);
  }

  @Override
  public boolean hasHeaderComponent() {
    return getEditor().hasHeaderComponent();
  }

  @Override
  @Nullable
  public JComponent getHeaderComponent() {
    return getEditor().getHeaderComponent();
  }

  @Override
  @Nonnull
  public IndentsModel getIndentsModel() {
    return getEditor().getIndentsModel();
  }

  @Override
  public int getAscent() {
    return getEditor().getAscent();
  }
}
