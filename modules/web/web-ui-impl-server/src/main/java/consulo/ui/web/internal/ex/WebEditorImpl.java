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
import com.intellij.openapi.application.Application;
import com.intellij.openapi.command.UndoConfirmationPolicy;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.actionSystem.DocCommandGroupId;
import com.intellij.openapi.editor.event.*;
import com.intellij.openapi.editor.ex.EditorGutterComponentEx;
import com.intellij.openapi.editor.ex.RangeHighlighterEx;
import com.intellij.openapi.editor.highlighter.HighlighterIterator;
import com.intellij.openapi.editor.impl.MarkupModelImpl;
import com.intellij.openapi.editor.impl.TextDrawingCallback;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileEditor.ex.IdeDocumentHistory;
import com.intellij.openapi.project.Project;
import consulo.annotation.access.RequiredReadAction;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.editor.impl.*;
import consulo.internal.arquill.editor.server.ArquillEditor;
import consulo.internal.arquill.editor.server.event.MouseDownEvent;
import consulo.ui.Component;
import consulo.ui.FocusableComponent;
import consulo.ui.color.ColorValue;
import consulo.ui.event.details.MouseInputDetails;
import consulo.ui.web.internal.base.ComponentHolder;
import consulo.ui.web.internal.base.FromVaadinComponentWrapper;
import consulo.ui.web.internal.base.VaadinComponentDelegate;
import consulo.ui.web.internal.util.Mappers;
import consulo.util.dataholder.Key;
import consulo.util.lang.BitUtil;
import consulo.web.gwt.shared.ui.state.RGBColorShared;
import org.intellij.lang.annotations.MagicConstant;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

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

  private static class EditorComponent extends VaadinComponentDelegate<Vaadin> implements Component, FocusableComponent {
    @Nonnull
    @Override
    public Vaadin createVaadinComponent() {
      return new Vaadin("");
    }

    @Override
    public boolean hasFocus() {
      // TODO [VISTALL] fake
      return true;
    }
  }

  private static final Key<Integer> ANNOTATION_ID = Key.create("annotation.id");

  private final EditorComponent myEditorComponent;

  private final WebEditorView myView;

  private final WebEditorGutterComponentImpl myGutterComponent;

  @RequiredReadAction
  public WebEditorImpl(@Nonnull Document document, boolean viewer, @Nullable Project project, @Nonnull EditorKind kind) {
    super(document, viewer, project, kind);

    myGutterComponent = new WebEditorGutterComponentImpl();

    myView = new WebEditorView(this);
    myView.reset();

    Disposer.register(myDisposable, myView);

    myEditorComponent = new EditorComponent();

    Vaadin vaadin = myEditorComponent.toVaadinComponent();
    vaadin.setWidth("100%");
    vaadin.setHeight("100%");
    vaadin.setText(myDocument.getText());

    vaadin.addMouseDownListener(this::runMousePressedCommand);

    Disposable firstCall = Disposable.newDisposable("caret first");
    myCaretModel.addCaretListener(new CaretListener() {
      @Override
      public void caretPositionChanged(CaretEvent e) {
        firstCall.disposeWithTree();

        int offset = e.getCaret().getOffset();

        Application.get().getLastUIAccess().give(() -> vaadin.setCaretOffset(offset));
      }
    }, firstCall);

    vaadin.addAttachListener(attachEvent -> update());
  }

  // due EditorMouseEvent use awt Event, we need set fake event, until migrate to own event system
  private static final MouseEvent fakeEvent = new MouseEvent(new JLabel("fake"), 0, 0, 0, 0, 0, 1, false);

  public void update() {
    HighlighterIterator iterator = myHighlighter.createIterator(0);
    while (!iterator.atEnd()) {
      int start = iterator.getStart();
      int end = iterator.getEnd();

      TextAttributes textAttributes = iterator.getTextAttributes();

      myEditorComponent.toVaadinComponent().addAnnotation(start, end, "orion.annotation.info", convertToCssProperties(textAttributes));

      iterator.advance();
    }
  }

  private Map<String, String> convertToCssProperties(TextAttributes textAttributes) {
    Map<String, String> map = new HashMap<>();
    ColorValue foregroundColor = textAttributes.getForegroundColor();
    if (foregroundColor != null) {
      RGBColorShared rgb = Mappers.map(foregroundColor.toRGB());
      map.put("color", rgb.toString());
    }

    ColorValue backgroundColor = textAttributes.getBackgroundColor();
    if (backgroundColor != null) {
      RGBColorShared rgb = Mappers.map(backgroundColor.toRGB());
      map.put("backgroundColor", rgb.toString());
    }

    int fontType = textAttributes.getFontType();
    if (BitUtil.isSet(fontType, Font.BOLD)) {
      map.put("fontWeight", "bold");
    }

    if (BitUtil.isSet(fontType, Font.ITALIC)) {
      map.put("fontStyle", "italic");
    }

    return map;
  }

  @Override
  protected void onHighlighterChanged(@Nonnull RangeHighlighterEx highlighter, boolean canImpactGutterSize, boolean fontStyleOrColorChanged, boolean remove) {
    if (myDocument.isInBulkUpdate()) return; // bulkUpdateFinished() will repaint anything

    Vaadin vaadin = myEditorComponent.toVaadinComponent();

    Integer annId = highlighter.getUserData(ANNOTATION_ID);
    if (annId != null) {
      vaadin.removeAnnotation(annId);
    }

    if (remove) {
      return;
    }

    int textLength = myDocument.getTextLength();

    int start = Math.min(Math.max(highlighter.getAffectedAreaStartOffset(), 0), textLength);
    int end = Math.min(Math.max(highlighter.getAffectedAreaEndOffset(), 0), textLength);

    TextAttributes textAttributes = highlighter.getTextAttributes();
    if (textAttributes == null) {
      return;
    }

    annId = vaadin.addAnnotation(start, end, "orion.annotation.info", convertToCssProperties(textAttributes));

    highlighter.putUserData(ANNOTATION_ID, annId);
  }

  private void runMousePressedCommand(@Nonnull final MouseDownEvent e) {
    //myLastMousePressedLocation = xyToLogicalPosition(e.getPoint());
    //myCaretStateBeforeLastPress = isToggleCaretEvent(e) ? myCaretModel.getCaretsAndSelections() : Collections.emptyList();
    //myCurrentDragIsSubstantial = false;
    //myDragStarted = false;
    //clearDnDContext();

    boolean forceProcessing = false;
    //myMousePressedEvent = e;
    MouseInputDetails.MouseButton mouseButton = MouseInputDetails.MouseButton.values()[e.getButton()];
    EditorMouseEvent event =
            new EditorMouseEvent(this, new MouseInputDetails(e.getX(), e.getY(), EnumSet.noneOf(MouseInputDetails.Modifier.class), mouseButton), mouseButton == MouseInputDetails.MouseButton.RIGHT,
                                 EditorMouseEventArea.EDITING_AREA);

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

  @Nonnull
  @Override
  public Component getContentUIComponent() {
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
    assertIsDispatchThread();
    if (isReleased) {
      throwDisposalError("Double release of editor:");
    }
    myTraceableDisposable.kill(null);

    isReleased = true;
    //mySizeAdjustmentStrategy.cancelAllRequests();
    //cancelAutoResetForMouseSelectionState();

    myFoldingModel.dispose();
    mySoftWrapModel.release();
    myMarkupModel.dispose();

    myScrollingModel.dispose();
    //myGutterComponent.dispose();
    //myMousePressedEvent = null;
    //myMouseMovedEvent = null;
    Disposer.dispose(myCaretModel);
    Disposer.dispose(mySoftWrapModel);
    Disposer.dispose(myView);
    //clearCaretThread();

    myFocusListeners.clear();
    myMouseListeners.clear();
    myMouseMotionListeners.clear();

    //myEditorComponent.removeMouseListener(myMouseListener);
    //myGutterComponent.removeMouseListener(myMouseListener);
    //myEditorComponent.removeMouseMotionListener(myMouseMotionListener);
    //myGutterComponent.removeMouseMotionListener(myMouseMotionListener);

    //CodeStyleSettingsManager.removeListener(myProject, this);

    Disposer.dispose(myDisposable);
    //myVerticalScrollBar.setUI(null); // clear error panel's cached image
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
    // todo impl
    return offsetToVisualPosition(offset);
  }

  @Nonnull
  @Override
  public EditorGutter getGutter() {
    return getGutterComponentEx();
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
