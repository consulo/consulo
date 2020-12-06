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

import com.intellij.ide.CopyProvider;
import com.intellij.ide.CutProvider;
import com.intellij.ide.DeleteProvider;
import com.intellij.ide.PasteProvider;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.event.EditorMouseListener;
import com.intellij.openapi.editor.event.EditorMouseMotionListener;
import com.intellij.openapi.editor.ex.*;
import com.intellij.openapi.editor.highlighter.EditorHighlighter;
import com.intellij.openapi.editor.impl.SettingsImpl;
import com.intellij.openapi.editor.impl.TextDrawingCallback;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import consulo.annotation.access.RequiredReadAction;
import consulo.disposer.Disposable;
import consulo.internal.arquill.editor.server.ArquillEditor;
import consulo.ui.Component;
import consulo.ui.web.internal.base.ComponentHolder;
import consulo.ui.web.internal.base.FromVaadinComponentWrapper;
import consulo.ui.web.internal.base.UIComponentWithVaadinComponent;
import kava.beans.PropertyChangeListener;
import org.intellij.lang.annotations.MagicConstant;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.util.Collection;
import java.util.function.IntFunction;

/**
 * @author VISTALL
 * @since 2019-02-18
 */
public class WebEditorImpl extends UIComponentWithVaadinComponent<WebEditorImpl.Vaadin> implements Component, EditorEx {
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

  private final DocumentEx myDocument;
  private final Project myProject;

  private final WebFoldingModelImpl myFoldingModel = new WebFoldingModelImpl();
  private final WebSelectionModelImpl mySelectionModel = new WebSelectionModelImpl();
  private final WebCaretModelImpl myCaretModel = new WebCaretModelImpl(this);

  private EditorColorsScheme myEditorColorsScheme;

  private EditorSettings mySettings;

  @RequiredReadAction
  public WebEditorImpl(Project project, VirtualFile file) {
    myProject = project;
    myDocument = (DocumentEx)FileDocumentManager.getInstance().getDocument(file);

    assert myDocument != null;

    myEditorColorsScheme = EditorColorsManager.getInstance().getGlobalScheme();
    mySettings = new SettingsImpl(this, project, EditorKind.MAIN_EDITOR);

    Vaadin vaadin = toVaadinComponent();
    vaadin.setWidth("100%");
    vaadin.setHeight("100%");
    vaadin.setText(myDocument.getText());
  }

  @Override
  @Nonnull
  public Vaadin createVaadinComponent() {
    return new Vaadin("");
  }

  @Nonnull
  @Override
  public Component getUIComponent() {
    return this;
  }

  @Nonnull
  @Override
  public DocumentEx getDocument() {
    return myDocument;
  }

  @Override
  public boolean isViewer() {
    return false;
  }

  @Nonnull
  @Override
  public SelectionModel getSelectionModel() {
    return mySelectionModel;
  }

  @Nonnull
  @Override
  public MarkupModelEx getMarkupModel() {
    return null;
  }

  @Nonnull
  @Override
  public MarkupModelEx getFilteredDocumentMarkupModel() {
    return null;
  }

  @Nonnull
  @Override
  public EditorGutterComponentEx getGutterComponentEx() {
    return null;
  }

  @Nonnull
  @Override
  public EditorHighlighter getHighlighter() {
    return null;
  }

  @Override
  public void setViewer(boolean isViewer) {

  }

  @Override
  public void setHighlighter(@Nonnull EditorHighlighter highlighter) {

  }

  @Override
  public void setColorsScheme(@Nonnull EditorColorsScheme scheme) {

  }

  @Override
  public void setInsertMode(boolean val) {

  }

  @Override
  public void setColumnMode(boolean val) {

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
  public CutProvider getCutProvider() {
    return null;
  }

  @Override
  public CopyProvider getCopyProvider() {
    return null;
  }

  @Override
  public PasteProvider getPasteProvider() {
    return null;
  }

  @Override
  public DeleteProvider getDeleteProvider() {
    return null;
  }

  @Override
  public void repaint(int startOffset, int endOffset) {

  }

  @Override
  public void reinitSettings() {

  }

  @Override
  public void addPropertyChangeListener(@Nonnull PropertyChangeListener listener, @Nonnull Disposable parentDisposable) {

  }

  @Override
  public void addPropertyChangeListener(@Nonnull PropertyChangeListener listener) {

  }

  @Override
  public void removePropertyChangeListener(@Nonnull PropertyChangeListener listener) {

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
  public void addFocusListener(@Nonnull FocusChangeListener listener) {

  }

  @Override
  public void addFocusListener(@Nonnull FocusChangeListener listener, @Nonnull Disposable parentDisposable) {

  }

  @Override
  public void setOneLineMode(boolean b) {

  }

  @Override
  public boolean isRendererMode() {
    return false;
  }

  @Override
  public void setRendererMode(boolean isRendererMode) {

  }

  @Override
  public void setFile(VirtualFile vFile) {

  }

  @Nonnull
  @Override
  public DataContext getDataContext() {
    return null;
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
  public VirtualFile getVirtualFile() {
    return null;
  }

  @Override
  public TextDrawingCallback getTextDrawingCallback() {
    return null;
  }

  @Nonnull
  @Override
  public FoldingModelEx getFoldingModel() {
    return myFoldingModel;
  }

  @Nonnull
  @Override
  public ScrollingModelEx getScrollingModel() {
    return null;
  }

  @Nonnull
  @Override
  public EditorColorsScheme createBoundColorSchemeDelegate(@Nullable EditorColorsScheme customGlobalScheme) {
    return null;
  }

  @Override
  public void setPlaceholder(@Nullable CharSequence text) {

  }

  @Override
  public void setPlaceholderAttributes(@Nullable TextAttributes attributes) {

  }

  @Override
  public void setShowPlaceholderWhenFocused(boolean show) {

  }

  @Override
  public boolean isStickySelection() {
    return false;
  }

  @Override
  public void setStickySelection(boolean enable) {

  }

  @Override
  public int getPrefixTextWidthInPixels() {
    return 0;
  }

  @Override
  public void setPrefixTextAndAttributes(@Nullable String prefixText, @Nullable TextAttributes attributes) {

  }

  @Override
  public boolean isPurePaintingMode() {
    return false;
  }

  @Override
  public void setPurePaintingMode(boolean enabled) {

  }

  @Override
  public void registerLineExtensionPainter(IntFunction<Collection<LineExtensionInfo>> lineExtensionPainter) {

  }

  @Override
  public int getExpectedCaretOffset() {
    return 0;
  }

  @Override
  public void setContextMenuGroupId(@Nullable String groupId) {

  }

  @Nullable
  @Override
  public String getContextMenuGroupId() {
    return null;
  }

  @Override
  public void installPopupHandler(@Nonnull EditorPopupHandler popupHandler) {

  }

  @Override
  public void uninstallPopupHandler(@Nonnull EditorPopupHandler popupHandler) {

  }

  @Override
  public void setCustomCursor(@Nonnull Object requestor, @Nullable Cursor cursor) {

  }

  @Nonnull
  @Override
  public CaretModel getCaretModel() {
    return myCaretModel;
  }

  @Nonnull
  @Override
  public SoftWrapModelEx getSoftWrapModel() {
    return null;
  }

  @Nonnull
  @Override
  public EditorSettings getSettings() {
    return mySettings;
  }

  @Nonnull
  @Override
  public EditorColorsScheme getColorsScheme() {
    return myEditorColorsScheme;
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
    return null;
  }

  @Nonnull
  @Override
  public LogicalPosition visualToLogicalPosition(@Nonnull VisualPosition visiblePos) {
    return null;
  }

  @Nonnull
  @Override
  public LogicalPosition offsetToLogicalPosition(int offset) {
    return null;
  }

  @Nonnull
  @Override
  public VisualPosition offsetToVisualPosition(int offset) {
    return null;
  }

  @Nonnull
  @Override
  public VisualPosition offsetToVisualPosition(int offset, boolean leanForward, boolean beforeSoftWrap) {
    return null;
  }

  @Override
  public void addEditorMouseListener(@Nonnull EditorMouseListener listener) {

  }

  @Override
  public void removeEditorMouseListener(@Nonnull EditorMouseListener listener) {

  }

  @Override
  public void addEditorMouseMotionListener(@Nonnull EditorMouseMotionListener listener) {

  }

  @Override
  public void removeEditorMouseMotionListener(@Nonnull EditorMouseMotionListener listener) {

  }

  @Override
  public boolean isDisposed() {
    return false;
  }

  @Nullable
  @Override
  public Project getProject() {
    return myProject;
  }

  @Override
  public boolean isInsertMode() {
    return false;
  }

  @Override
  public boolean isColumnMode() {
    return false;
  }

  @Override
  public boolean isOneLineMode() {
    return false;
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


  @Nonnull
  @Override
  public IndentsModel getIndentsModel() {
    return null;
  }

  @Nonnull
  @Override
  public InlayModel getInlayModel() {
    return null;
  }

  @Nonnull
  @Override
  public EditorKind getEditorKind() {
    return null;
  }
}
