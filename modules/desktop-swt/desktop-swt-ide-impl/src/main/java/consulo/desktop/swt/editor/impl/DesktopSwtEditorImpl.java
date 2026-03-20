/*
 * Copyright 2013-2021 consulo.io
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
package consulo.desktop.swt.editor.impl;

import consulo.codeEditor.*;
import consulo.codeEditor.impl.*;
import consulo.codeEditor.RealEditor;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.document.Document;
import consulo.project.Project;
import consulo.ui.Component;
import org.intellij.lang.annotations.MagicConstant;

import org.jspecify.annotations.Nullable;
import java.awt.*;

/**
 * @author VISTALL
 * @since 18/12/2021
 */
public class DesktopSwtEditorImpl extends CodeEditorBase implements RealEditor {
  private DesktopSwtEditorComponent myComponent;

  private DesktopSwtEditorGutterComponentImpl myGutterComponent;

  public DesktopSwtEditorImpl(Document document, boolean viewer, @Nullable Project project, EditorKind kind) {
    super(document, viewer, project, kind);

    myComponent = new DesktopSwtEditorComponent(document);
    myGutterComponent = new DesktopSwtEditorGutterComponentImpl();
  }

  
  @Override
  public Component getUIComponent() {
    return myComponent;
  }

  
  @Override
  public Component getContentUIComponent() {
    return myComponent;
  }

  @Override
  public boolean isShowing() {
    return true;
  }

  @Override
  protected CodeEditorSelectionModelBase createSelectionModel() {
    return new DesktopSwtCodeEditorSelectionModelImpl(this);
  }

  @Override
  protected MarkupModelImpl createMarkupModel() {
    return new DesktopSwtMarkupModelImpl(this);
  }

  @Override
  protected CodeEditorFoldingModelBase createFoldingModel() {
    return new DesktopSwtCodeEditorFoldingModelImpl(this);
  }

  @Override
  protected CodeEditorCaretModelBase createCaretModel() {
    return new DesktopSwtCodeEditorCaretModelImpl(this);
  }

  @Override
  protected CodeEditorScrollingModelBase createScrollingModel() {
    return new DesktopSwtCodeEditorScrollingModelImpl(this);
  }

  @Override
  protected CodeEditorInlayModelBase createInlayModel() {
    return new DesktopSwtCodeEditorInlayModelImpl(this);
  }

  @Override
  protected CodeEditorSoftWrapModelBase createSoftWrapModel() {
    return new DesktopSwtCodeEditorSoftWrapModelImpl(this);
  }

  
  @Override
  protected DataContext getComponentContext() {
    return DataManager.getInstance().getDataContext(getUIComponent());
  }

  @Override
  protected void stopDumb() {

  }

  @Override
  public void release() {

  }

  @Override
  public int offsetToVisualLine(int offset, boolean beforeSoftWrap) {
    return 0;
  }

  @Override
  public int visualLineStartOffset(int visualLine) {
    return 0;
  }

  @Override
  public void startDumb() {

  }

  
  @Override
  public EditorGutterComponentEx getGutterComponentEx() {
    return myGutterComponent;
  }

  @Override
  public void setVerticalScrollbarOrientation(@MagicConstant(intValues = {VERTICAL_SCROLLBAR_LEFT, VERTICAL_SCROLLBAR_RIGHT}) int type) {

  }

  @Override
  public int getVerticalScrollbarOrientation() {
    return 0;
  }

  @Override
  public void setVerticalScrollbarVisible(boolean b) {

  }

  @Override
  public void setHorizontalScrollbarVisible(boolean b) {

  }

  @Override
  public void repaint(int startOffset, int endOffset, boolean invalidateTextLayout) {

  }

  @Override
  public void reinitSettings() {

  }

  @Override
  public int getMaxWidthInRange(int startOffset, int endOffset) {
    return 0;
  }

  @Override
  public boolean setCaretVisible(boolean b) {
    return false;
  }

  @Override
  public boolean setCaretEnabled(boolean enabled) {
    return false;
  }

  @Override
  public void setFontSize(int fontSize) {

  }

  @Override
  public boolean isEmbeddedIntoDialogWrapper() {
    return false;
  }

  @Override
  public void setEmbeddedIntoDialogWrapper(boolean b) {

  }

  @Override
  public TextDrawingCallback getTextDrawingCallback() {
    return null;
  }

  @Override
  public int getPrefixTextWidthInPixels() {
    return 0;
  }

  @Override
  public void setCustomCursor(Object requestor, @Nullable Cursor cursor) {

  }

  @Override
  public int getLineHeight() {
    return 0;
  }

  @Override
  public int logicalPositionToOffset(LogicalPosition pos) {
    return 0;
  }

  
  @Override
  public VisualPosition logicalToVisualPosition(LogicalPosition logicalPos) {
    return new VisualPosition(1, 1);
  }

  
  @Override
  public LogicalPosition visualToLogicalPosition(VisualPosition visiblePos) {
    return new LogicalPosition(1, 1);
  }

  
  @Override
  public LogicalPosition offsetToLogicalPosition(int offset) {
    return new LogicalPosition(1, 1);
  }

  
  @Override
  public VisualPosition offsetToVisualPosition(int offset) {
    return new VisualPosition(1 ,1);
  }

  
  @Override
  public VisualPosition offsetToVisualPosition(int offset, boolean leanForward, boolean beforeSoftWrap) {
    // todo impl
    return offsetToVisualPosition(offset);
  }

  
  @Override
  public EditorGutter getGutter() {
    return myGutterComponent;
  }

  @Override
  public boolean hasHeaderComponent() {
    return false;
  }

  
  public LogicalPosition xyToLogicalPosition(java.awt.Point p) {
    // todo fake return
    return new LogicalPosition(0, 0);
  }

  
  public java.awt.Point visualPositionToXY(VisualPosition visible) {
    // todo fake return
    return new Point(1, 1);
  }
}
