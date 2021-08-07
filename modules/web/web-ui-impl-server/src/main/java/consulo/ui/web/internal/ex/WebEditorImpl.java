/*
 * Copyright 2013-2019 consulo.io
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
package consulo.ui.web.internal.ex;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.command.UndoConfirmationPolicy;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.actionSystem.DocCommandGroupId;
import com.intellij.openapi.editor.event.EditorMouseEvent;
import com.intellij.openapi.editor.event.EditorMouseEventArea;
import com.intellij.openapi.editor.event.EditorMouseListener;
import com.intellij.openapi.editor.ex.EditorGutterComponentEx;
import com.intellij.openapi.editor.impl.MarkupModelImpl;
import com.intellij.openapi.editor.impl.TextDrawingCallback;
import com.intellij.openapi.fileEditor.ex.IdeDocumentHistory;
import com.intellij.openapi.project.Project;
import consulo.annotation.access.RequiredReadAction;
import consulo.disposer.Disposer;
import consulo.editor.impl.*;
import consulo.internal.arquill.editor.server.ArquillEditor;
import consulo.internal.arquill.editor.server.event.MouseDownEvent;
import consulo.ui.Component;
import consulo.ui.web.internal.base.ComponentHolder;
import consulo.ui.web.internal.base.FromVaadinComponentWrapper;
import consulo.ui.web.internal.base.UIComponentWithVaadinComponent;
import org.intellij.lang.annotations.MagicConstant;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;

/**
 * @author VISTALL
 * @since 2019-02-18
 */
public class WebEditorImpl extends CodeEditorBase {
  public static class Vaadin extends ArquillEditor implements ComponentHolder, FromVaadinComponentWrapper {
    private Component myComponent;

    public Vaadin(String text) {
      super(text);
    }

    @Nullable
    @Override
    public Component toUIComponent() {
      return myComponent;
    }

    @Override
    public void setComponent(Component component) {
      myComponent = component;
    }
  }

  private static class EditorComponent extends UIComponentWithVaadinComponent<WebEditorImpl.Vaadin> implements Component {
    @Nonnull
    @Override
    public Vaadin createVaadinComponent() {
      return new Vaadin("");
    }
  }

  private final EditorComponent myEditorComponent;

  private WebEditorView myView;

  @RequiredReadAction
  public WebEditorImpl(@Nonnull Document document, boolean viewer, @Nullable Project project, @Nonnull EditorKind kind) {
    super(document, viewer, project, kind);

    myView = new WebEditorView(this);
    myView.reset();

    Disposer.register(myDisposable, myView);

    myEditorComponent = new EditorComponent();

    Vaadin vaadin = myEditorComponent.toVaadinComponent();
    vaadin.setWidth("100%");
    vaadin.setHeight("100%");
    vaadin.setText(myDocument.getText());

    vaadin.addMouseDownListener(this::runMousePressedCommand);
  }

  // due EditorMouseEvent use awt Event, we need set fake event, until migrate to own event system
  private static final MouseEvent fakeEvent = new MouseEvent(new JLabel("fake"), 0, 0, 0, 0, 0, 1, false);

  private void runMousePressedCommand(@Nonnull final MouseDownEvent e) {
    //myLastMousePressedLocation = xyToLogicalPosition(e.getPoint());
    //myCaretStateBeforeLastPress = isToggleCaretEvent(e) ? myCaretModel.getCaretsAndSelections() : Collections.emptyList();
    //myCurrentDragIsSubstantial = false;
    //myDragStarted = false;
    //clearDnDContext();

    boolean forceProcessing = false;
    //myMousePressedEvent = e;
    EditorMouseEvent event = new EditorMouseEvent(this, fakeEvent, EditorMouseEventArea.EDITING_AREA);

    myExpectedCaretOffset = e.getTextOffset();
    try {
      for (EditorMouseListener mouseListener : myMouseListeners) {
        boolean wasConsumed = event.isConsumed();
        mouseListener.mousePressed(event);
        //noinspection deprecation
        if (!wasConsumed && event.isConsumed() && mouseListener instanceof com.intellij.util.EditorPopupHandler) {
          // compatibility with legacy code, this logic should be removed along with EditorPopupHandler
          forceProcessing = true;
        }
        if (isReleased) return;
      }
    }
    finally {
      myExpectedCaretOffset = -1;
    }

    //if (event.getArea() == EditorMouseEventArea.LINE_MARKERS_AREA || event.getArea() == EditorMouseEventArea.FOLDING_OUTLINE_AREA && !isInsideGutterWhitespaceArea(e)) {
    //  myDragOnGutterSelectionStartLine = EditorUtil.yPositionToLogicalLine(DesktopEditorImpl.this, e);
    //}

    if (event.isConsumed() && !forceProcessing) return;

    if (myCommandProcessor != null) {
      Runnable runnable = () -> {
        if (processMousePressed(e) && myProject != null && !myProject.isDefault()) {
          IdeDocumentHistory.getInstance(myProject).includeCurrentCommandAsNavigation();
        }
      };
      myCommandProcessor.executeCommand(myProject, runnable, "", DocCommandGroupId.noneGroupId(getDocument()), UndoConfirmationPolicy.DEFAULT, getDocument());
    }
    else {
      processMousePressed(e);
    }

    invokePopupIfNeeded(event);
  }

  private boolean processMousePressed(MouseDownEvent e) {
    CodeEditorCaretBase primaryCaret = getCaretModel().getPrimaryCaret();

    primaryCaret.moveToOffset(e.getTextOffset());
    return true;
  }

  @Override
  protected void bulkUpdateFinished() {
    myView.reset();

    super.bulkUpdateFinished();
  }

  @Nonnull
  @Override
  public Component getUIComponent() {
    return myEditorComponent;
  }

  @Override
  protected CodeEditorSelectionModelBase createSelectionModel() {
    return new WebSelectionModelImpl(this);
  }

  @Override
  protected MarkupModelImpl createMarkupModel() {
    return new WebEditorMarkupModelImpl(this);
  }

  @Override
  protected CodeEditorFoldingModelBase createFoldingModel() {
    return new WebFoldingModelImpl(this);
  }

  @Override
  protected CodeEditorCaretModelBase createCaretModel() {
    return new WebCaretModelImpl(this);
  }

  @Override
  protected CodeEditorScrollingModelBase createScrollingModel() {
    return new WebScrollingModelImpl(this);
  }

  @Override
  protected CodeEditorInlayModelBase createInlayModel() {
    return new WebInlayModelImpl(this);
  }

  @Override
  protected CodeEditorSoftWrapModelBase createSoftWrapModel() {
    return new WebSoftWrapModelImpl(this);
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
    Disposer.dispose(myDisposable);
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
    return null;
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
    myView.reset();
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
    return myView.logicalPositionToOffset(pos);
  }

  @Override
  public int visualLineToY(int visualLine) {
    return 0;
  }

  @Override
  public boolean isShowing() {
    return myEditorComponent.isVisible();
  }

  @Nonnull
  @Override
  public VisualPosition logicalToVisualPosition(@Nonnull LogicalPosition logicalPos) {
    return new VisualPosition(logicalPos.line, logicalPos.column, logicalPos.visualPositionLeansRight);
  }

  @Nonnull
  @Override
  public LogicalPosition visualToLogicalPosition(@Nonnull VisualPosition visiblePos) {
    return new LogicalPosition(visiblePos.getLine(), visiblePos.getColumn(), visiblePos.leansRight);
  }

  @Nonnull
  @Override
  public LogicalPosition offsetToLogicalPosition(int offset) {
    return myView.offsetToLogicalPosition(offset);
  }

  @Nonnull
  @Override
  public VisualPosition offsetToVisualPosition(int offset) {
    LogicalPosition position = myView.offsetToLogicalPosition(offset);
    return logicalToVisualPosition(position);
  }

  @Nonnull
  @Override
  public VisualPosition offsetToVisualPosition(int offset, boolean leanForward, boolean beforeSoftWrap) {
    return null;
  }

  @Nonnull
  @Override
  public EditorGutter getGutter() {
    return null;
  }

  @Override
  public boolean hasHeaderComponent() {
    return false;
  }
}
