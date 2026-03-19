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
package consulo.ide.impl.idea.codeInsight.intention.impl.config;

import consulo.fileEditor.impl.internal.OpenFileDescriptorImpl;
import consulo.codeEditor.*;
import consulo.codeEditor.event.EditorMouseEventArea;
import consulo.codeEditor.event.EditorMouseListener;
import consulo.codeEditor.event.EditorMouseMotionListener;
import consulo.codeEditor.markup.MarkupModel;
import consulo.colorScheme.EditorColorsScheme;
import consulo.dataContext.DataContext;
import consulo.document.Document;
import consulo.fileEditor.FileEditorManager;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.util.dataholder.UserDataHolderBase;

import org.jspecify.annotations.Nullable;
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
      Project project = myFile.getProject();
      myEditor = FileEditorManager.getInstance(project).openTextEditor(new OpenFileDescriptorImpl(project, myFile.getVirtualFile(), 0), false);
      assert myEditor != null;
    }
    return myEditor;
  }

  @Override
  
  public Document getDocument() {
    return getEditor().getDocument();
  }

  @Override
  public boolean isViewer() {
    return getEditor().isViewer();
  }

  @Override
  
  public JComponent getComponent() {
    return getEditor().getComponent();
  }

  @Override
  
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
  
  public SelectionModel getSelectionModel() {
    return getEditor().getSelectionModel();
  }

  @Override
  
  public MarkupModel getMarkupModel() {
    return getEditor().getMarkupModel();
  }

  @Override
  
  public FoldingModel getFoldingModel() {
    return getEditor().getFoldingModel();
  }

  @Override
  
  public ScrollingModel getScrollingModel() {
    return getEditor().getScrollingModel();
  }

  @Override
  
  public CaretModel getCaretModel() {
    return getEditor().getCaretModel();
  }

  @Override
  
  public SoftWrapModel getSoftWrapModel() {
    return getEditor().getSoftWrapModel();
  }

  
  @Override
  public InlayModel getInlayModel() {
    return getEditor().getInlayModel();
  }

  
  @Override
  public EditorKind getEditorKind() {
    return getEditor().getEditorKind();
  }

  @Override
  
  public EditorSettings getSettings() {
    return getEditor().getSettings();
  }

  @Override
  
  public EditorColorsScheme getColorsScheme() {
    return getEditor().getColorsScheme();
  }

  @Override
  public int getLineHeight() {
    return getEditor().getLineHeight();
  }

  @Override
  
  public Point logicalPositionToXY(LogicalPosition pos) {
    return getEditor().logicalPositionToXY(pos);
  }

  @Override
  public int logicalPositionToOffset(LogicalPosition pos) {
    return getEditor().logicalPositionToOffset(pos);
  }

  @Override
  
  public VisualPosition logicalToVisualPosition(LogicalPosition logicalPos) {
    return getEditor().logicalToVisualPosition(logicalPos);
  }

  @Override
  
  public Point visualPositionToXY(VisualPosition visible) {
    return getEditor().visualPositionToXY(visible);
  }

  
  @Override
  public Point2D visualPositionToPoint2D(VisualPosition pos) {
    return getEditor().visualPositionToPoint2D(pos);
  }

  @Override
  
  public LogicalPosition visualToLogicalPosition(VisualPosition visiblePos) {
    return getEditor().visualToLogicalPosition(visiblePos);
  }

  @Override
  
  public LogicalPosition offsetToLogicalPosition(int offset) {
    return getEditor().offsetToLogicalPosition(offset);
  }

  @Override
  
  public VisualPosition offsetToVisualPosition(int offset) {
    return getEditor().offsetToVisualPosition(offset);
  }

  @Override
  
  public VisualPosition offsetToVisualPosition(int offset, boolean leanForward, boolean beforeSoftWrap) {
    return getEditor().offsetToVisualPosition(offset, leanForward, beforeSoftWrap);
  }

  @Override
  
  public LogicalPosition xyToLogicalPosition(Point p) {
    return getEditor().xyToLogicalPosition(p);
  }

  @Override
  
  public VisualPosition xyToVisualPosition(Point p) {
    return getEditor().xyToVisualPosition(p);
  }

  
  @Override
  public VisualPosition xyToVisualPosition(Point2D p) {
    return getEditor().xyToVisualPosition(p);
  }

  @Override
  public void addEditorMouseListener(EditorMouseListener listener) {
    getEditor().addEditorMouseListener(listener);
  }

  @Override
  public void removeEditorMouseListener(EditorMouseListener listener) {
    getEditor().removeEditorMouseListener(listener);
  }

  @Override
  public void addEditorMouseMotionListener(EditorMouseMotionListener listener) {
    getEditor().addEditorMouseMotionListener(listener);
  }

  @Override
  public void removeEditorMouseMotionListener(EditorMouseMotionListener listener) {
    getEditor().removeEditorMouseMotionListener(listener);
  }

  @Override
  public boolean isDisposed() {
    return getEditor().isDisposed();
  }

  @Override
  public @Nullable Project getProject() {
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
  
  public EditorGutter getGutter() {
    return getEditor().getGutter();
  }

  @Override
  public @Nullable EditorMouseEventArea getMouseEventArea(MouseEvent e) {
    return getEditor().getMouseEventArea(e);
  }

  @Override
  public void setHeaderComponent(@Nullable JComponent header) {
    getEditor().setHeaderComponent(header);
  }

  @Override
  public boolean hasHeaderComponent() {
    return getEditor().hasHeaderComponent();
  }

  @Override
  public @Nullable JComponent getHeaderComponent() {
    return getEditor().getHeaderComponent();
  }

  @Override
  
  public IndentsModel getIndentsModel() {
    return getEditor().getIndentsModel();
  }

  @Override
  public int getAscent() {
    return getEditor().getAscent();
  }

  
  @Override
  public DataContext getDataContext() {
    return getEditor().getDataContext();
  }
}
