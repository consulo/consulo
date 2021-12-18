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

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.ex.EditorGutterComponentEx;
import com.intellij.openapi.editor.impl.MarkupModelImpl;
import com.intellij.openapi.editor.impl.TextDrawingCallback;
import com.intellij.openapi.project.Project;
import consulo.editor.impl.*;
import consulo.editor.internal.EditorInternal;
import consulo.ui.Component;
import org.intellij.lang.annotations.MagicConstant;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;

/**
 * @author VISTALL
 * @since 18/12/2021
 */
public class DesktopSwtEditorImpl extends CodeEditorBase implements EditorInternal {
  private DesktopSwtEditorComponent myComponent;

  private DesktopSwtEditorGutterComponentImpl myGutterComponent;

  public DesktopSwtEditorImpl(@Nonnull Document document, boolean viewer, @Nullable Project project, @Nonnull EditorKind kind) {
    super(document, viewer, project, kind);

    myComponent = new DesktopSwtEditorComponent(document);
    myGutterComponent = new DesktopSwtEditorGutterComponentImpl();
  }

  @Nonnull
  @Override
  public Component getUIComponent() {
    return myComponent;
  }

  @Nonnull
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

  @Nonnull
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

  @Nonnull
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
  public void setCustomCursor(@Nonnull Object requestor, @Nullable Cursor cursor) {

  }

  @Override
  public int getLineHeight() {
    return 0;
  }

  @Override
  public int logicalPositionToOffset(@Nonnull LogicalPosition pos) {
    return 0;
  }

  @Nonnull
  @Override
  public VisualPosition logicalToVisualPosition(@Nonnull LogicalPosition logicalPos) {
    return new VisualPosition(1, 1);
  }

  @Nonnull
  @Override
  public LogicalPosition visualToLogicalPosition(@Nonnull VisualPosition visiblePos) {
    return new LogicalPosition(1, 1);
  }

  @Nonnull
  @Override
  public LogicalPosition offsetToLogicalPosition(int offset) {
    return new LogicalPosition(1, 1);
  }

  @Nonnull
  @Override
  public VisualPosition offsetToVisualPosition(int offset) {
    return new VisualPosition(1 ,1);
  }

  @Nonnull
  @Override
  public VisualPosition offsetToVisualPosition(int offset, boolean leanForward, boolean beforeSoftWrap) {
    // todo impl
    return offsetToVisualPosition(offset);
  }


  @Nonnull
  @Override
  public EditorGutter getGutter() {
    return myGutterComponent;
  }

  @Override
  public boolean hasHeaderComponent() {
    return false;
  }

  @Nonnull
  public LogicalPosition xyToLogicalPosition(@Nonnull java.awt.Point p) {
    // todo fake return
    return new LogicalPosition(0, 0);
  }

  @Nonnull
  public java.awt.Point visualPositionToXY(@Nonnull VisualPosition visible) {
    // todo fake return
    return new Point(1, 1);
  }
}
