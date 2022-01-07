/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.diff.tools.util.side;

import com.intellij.diff.DiffContext;
import com.intellij.diff.actions.ProxyUndoRedoAction;
import com.intellij.diff.actions.impl.FocusOppositePaneAction;
import com.intellij.diff.actions.impl.OpenInEditorWithMouseAction;
import com.intellij.diff.actions.impl.SetEditorSettingsAction;
import com.intellij.diff.contents.DocumentContent;
import com.intellij.diff.requests.ContentDiffRequest;
import com.intellij.diff.requests.DiffRequest;
import com.intellij.diff.tools.holders.EditorHolderFactory;
import com.intellij.diff.tools.holders.TextEditorHolder;
import com.intellij.diff.tools.util.DiffDataKeys;
import com.intellij.diff.tools.util.SyncScrollSupport;
import com.intellij.diff.tools.util.SyncScrollSupport.TwosideSyncScrollSupport;
import com.intellij.diff.tools.util.base.InitialScrollPositionSupport;
import com.intellij.diff.tools.util.base.TextDiffSettingsHolder;
import com.intellij.diff.tools.util.base.TextDiffViewerUtil;
import com.intellij.diff.util.DiffUtil;
import com.intellij.diff.util.LineCol;
import com.intellij.diff.util.Side;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.VisibleAreaEvent;
import com.intellij.openapi.editor.event.VisibleAreaListener;
import com.intellij.openapi.editor.ex.EditorEx;
import consulo.util.dataholder.Key;
import com.intellij.pom.Navigatable;
import com.intellij.util.containers.ContainerUtil;
import consulo.logging.Logger;
import consulo.ui.annotation.RequiredUIAccess;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.util.List;

public abstract class TwosideTextDiffViewer extends TwosideDiffViewer<TextEditorHolder> {
  public static final Logger LOG = Logger.getInstance(TwosideTextDiffViewer.class);

  @Nonnull
  private final List<? extends EditorEx> myEditableEditors;
  @javax.annotation.Nullable
  private List<? extends EditorEx> myEditors;

  @Nonnull
  protected final SetEditorSettingsAction myEditorSettingsAction;

  @Nonnull
  private final MyVisibleAreaListener myVisibleAreaListener = new MyVisibleAreaListener();

  @javax.annotation.Nullable
  private TwosideSyncScrollSupport mySyncScrollSupport;

  public TwosideTextDiffViewer(@Nonnull DiffContext context, @Nonnull ContentDiffRequest request) {
    super(context, request, TextEditorHolder.TextEditorHolderFactory.INSTANCE);

    new MyFocusOppositePaneAction(true).install(myPanel);
    new MyFocusOppositePaneAction(false).install(myPanel);

    myEditorSettingsAction = new SetEditorSettingsAction(getTextSettings(), getEditors());
    myEditorSettingsAction.applyDefaults();

    new MyOpenInEditorWithMouseAction().install(getEditors());

    myEditableEditors = TextDiffViewerUtil.getEditableEditors(getEditors());

    TextDiffViewerUtil.checkDifferentDocuments(myRequest);

    boolean editable1 = DiffUtil.canMakeWritable(getContent1().getDocument());
    boolean editable2 = DiffUtil.canMakeWritable(getContent2().getDocument());
    if (editable1 ^ editable2) {
      ProxyUndoRedoAction.register(getProject(), editable1 ? getEditor1() : getEditor2(), myPanel);
    }
  }

  @Override
  @RequiredUIAccess
  protected void onInit() {
    super.onInit();
    installEditorListeners();
  }

  @Override
  @RequiredUIAccess
  protected void onDispose() {
    destroyEditorListeners();
    super.onDispose();
  }

  @Nonnull
  @Override
  protected List<TextEditorHolder> createEditorHolders(@Nonnull EditorHolderFactory<TextEditorHolder> factory) {
    List<TextEditorHolder> holders = super.createEditorHolders(factory);

    boolean[] forceReadOnly = TextDiffViewerUtil.checkForceReadOnly(myContext, myRequest);
    for (int i = 0; i < 2; i++) {
      if (forceReadOnly[i]) holders.get(i).getEditor().setViewer(true);
    }

    Side.LEFT.select(holders).getEditor().setVerticalScrollbarOrientation(EditorEx.VERTICAL_SCROLLBAR_LEFT);

    for (TextEditorHolder holder : holders) {
      DiffUtil.disableBlitting(holder.getEditor());
    }

    return holders;
  }

  @Nonnull
  @Override
  protected List<JComponent> createTitles() {
    return DiffUtil.createSyncHeightComponents(DiffUtil.createTextTitles(myRequest, getEditors()));
  }

  //
  // Diff
  //

  @Nonnull
  public TextDiffSettingsHolder.TextDiffSettings getTextSettings() {
    return TextDiffViewerUtil.getTextSettings(myContext);
  }

  @Nonnull
  protected List<AnAction> createEditorPopupActions() {
    return TextDiffViewerUtil.createEditorPopupActions();
  }

  @Override
  protected void onDocumentChange(@Nonnull DocumentEvent event) {
    super.onDocumentChange(event);
    myContentPanel.repaintDivider();
  }

  //
  // Listeners
  //

  @RequiredUIAccess
  protected void installEditorListeners() {
    new TextDiffViewerUtil.EditorActionsPopup(createEditorPopupActions()).install(getEditors(), myPanel);

    new TextDiffViewerUtil.EditorFontSizeSynchronizer(getEditors()).install(this);


    getEditor(Side.LEFT).getScrollingModel().addVisibleAreaListener(myVisibleAreaListener);
    getEditor(Side.RIGHT).getScrollingModel().addVisibleAreaListener(myVisibleAreaListener);

    SyncScrollSupport.SyncScrollable scrollable = getSyncScrollable();
    if (scrollable != null) {
      mySyncScrollSupport = new TwosideSyncScrollSupport(getEditors(), scrollable);
      myEditorSettingsAction.setSyncScrollSupport(mySyncScrollSupport);
    }
  }

  @RequiredUIAccess
  protected void destroyEditorListeners() {
    getEditor(Side.LEFT).getScrollingModel().removeVisibleAreaListener(myVisibleAreaListener);
    getEditor(Side.RIGHT).getScrollingModel().removeVisibleAreaListener(myVisibleAreaListener);

    mySyncScrollSupport = null;
  }

  protected void disableSyncScrollSupport(boolean disable) {
    if (mySyncScrollSupport != null) {
      if (disable) {
        mySyncScrollSupport.enterDisableScrollSection();
      }
      else {
        mySyncScrollSupport.exitDisableScrollSection();
      }
    }
  }

  //
  // Getters
  //


  @Nonnull
  protected List<? extends DocumentContent> getContents() {
    //noinspection unchecked
    return (List)myRequest.getContents();
  }

  @Nonnull
  public List<? extends EditorEx> getEditors() {
    if (myEditors == null) {
      myEditors = ContainerUtil.map(getEditorHolders(), holder -> holder.getEditor());
    }
    return myEditors;
  }

  @Nonnull
  protected List<? extends EditorEx> getEditableEditors() {
    return myEditableEditors;
  }

  @Nonnull
  public EditorEx getCurrentEditor() {
    return getEditor(getCurrentSide());
  }

  @Nonnull
  public DocumentContent getCurrentContent() {
    return getContent(getCurrentSide());
  }

  @Nonnull
  public EditorEx getEditor1() {
    return getEditor(Side.LEFT);
  }

  @Nonnull
  public EditorEx getEditor2() {
    return getEditor(Side.RIGHT);
  }


  @Nonnull
  public EditorEx getEditor(@Nonnull Side side) {
    return side.select(getEditors());
  }

  @Nonnull
  public DocumentContent getContent(@Nonnull Side side) {
    return side.select(getContents());
  }

  @Nonnull
  public DocumentContent getContent1() {
    return getContent(Side.LEFT);
  }

  @Nonnull
  public DocumentContent getContent2() {
    return getContent(Side.RIGHT);
  }

  @javax.annotation.Nullable
  public TwosideSyncScrollSupport getSyncScrollSupport() {
    return mySyncScrollSupport;
  }

  //
  // Abstract
  //

  @RequiredUIAccess
  @Nonnull
  protected LineCol transferPosition(@Nonnull Side baseSide, @Nonnull LineCol position) {
    if (mySyncScrollSupport == null) return position;
    int line = mySyncScrollSupport.getScrollable().transfer(baseSide, position.line);
    return new LineCol(line, position.column);
  }

  @RequiredUIAccess
  protected void scrollToLine(@Nonnull Side side, int line) {
    DiffUtil.scrollEditor(getEditor(side), line, false);
    setCurrentSide(side);
  }

  @javax.annotation.Nullable
  protected abstract SyncScrollSupport.SyncScrollable getSyncScrollable();

  //
  // Misc
  //

  @javax.annotation.Nullable
  @Override
  protected Navigatable getNavigatable() {
    Side side = getCurrentSide();

    LineCol position = LineCol.fromCaret(getEditor(side));
    Navigatable navigatable = getContent(side).getNavigatable(position);
    if (navigatable != null) return navigatable;

    LineCol otherPosition = transferPosition(side, position);
    return getContent(side.other()).getNavigatable(otherPosition);
  }

  public static boolean canShowRequest(@Nonnull DiffContext context, @Nonnull DiffRequest request) {
    return TwosideDiffViewer.canShowRequest(context, request, TextEditorHolder.TextEditorHolderFactory.INSTANCE);
  }

  //
  // Actions
  //

  private class MyFocusOppositePaneAction extends FocusOppositePaneAction {
    public MyFocusOppositePaneAction(boolean scrollToPosition) {
      super(scrollToPosition);
    }

    @Override
    public void actionPerformed(@Nonnull AnActionEvent e) {
      Side currentSide = getCurrentSide();
      Side targetSide = currentSide.other();

      EditorEx currentEditor = getEditor(currentSide);
      EditorEx targetEditor = getEditor(targetSide);

      if (myScrollToPosition) {
        LineCol position = transferPosition(currentSide, LineCol.fromCaret(currentEditor));
        targetEditor.getCaretModel().moveToOffset(position.toOffset(targetEditor));
      }

      setCurrentSide(targetSide);
      targetEditor.getScrollingModel().scrollToCaret(ScrollType.MAKE_VISIBLE);

      DiffUtil.requestFocus(getProject(), getPreferredFocusedComponent());
    }
  }

  private class MyOpenInEditorWithMouseAction extends OpenInEditorWithMouseAction {
    @Override
    protected Navigatable getNavigatable(@Nonnull Editor editor, int line) {
      Side side = Side.fromValue(getEditors(), editor);
      if (side == null) return null;

      return getContent(side).getNavigatable(new LineCol(line));
    }
  }

  protected class MyToggleAutoScrollAction extends TextDiffViewerUtil.ToggleAutoScrollAction {
    public MyToggleAutoScrollAction() {
      super(getTextSettings());
    }
  }

  //
  // Helpers
  //

  @javax.annotation.Nullable
  @Override
  public Object getData(@Nonnull @NonNls Key<?> dataId) {
    if (DiffDataKeys.CURRENT_EDITOR == dataId) {
      return getCurrentEditor();
    }
    return super.getData(dataId);
  }

  private class MyVisibleAreaListener implements VisibleAreaListener {
    @Override
    public void visibleAreaChanged(VisibleAreaEvent e) {
      if (mySyncScrollSupport != null) mySyncScrollSupport.visibleAreaChanged(e);
      myContentPanel.repaint();
    }
  }

  protected abstract class MyInitialScrollPositionHelper extends InitialScrollPositionSupport.TwosideInitialScrollHelper {
    @Nonnull
    @Override
    protected List<? extends Editor> getEditors() {
      return TwosideTextDiffViewer.this.getEditors();
    }

    @Override
    protected void disableSyncScroll(boolean value) {
      disableSyncScrollSupport(value);
    }

    @Override
    protected boolean doScrollToLine() {
      if (myScrollToLine == null) return false;

      scrollToLine(myScrollToLine.first, myScrollToLine.second);
      return true;
    }
  }
}
