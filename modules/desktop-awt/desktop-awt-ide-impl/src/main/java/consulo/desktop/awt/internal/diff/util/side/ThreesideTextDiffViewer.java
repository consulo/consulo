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
package consulo.desktop.awt.internal.diff.util.side;

import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorEx;
import consulo.codeEditor.LogicalPosition;
import consulo.codeEditor.event.VisibleAreaEvent;
import consulo.codeEditor.event.VisibleAreaListener;
import consulo.desktop.awt.internal.diff.EditorHolderFactory;
import consulo.desktop.awt.internal.diff.TextEditorHolder;
import consulo.desktop.awt.internal.diff.action.OpenInEditorWithMouseAction;
import consulo.desktop.awt.internal.diff.action.SetEditorSettingsAction;
import consulo.desktop.awt.internal.diff.util.*;
import consulo.diff.DiffContext;
import consulo.diff.DiffDataKeys;
import consulo.diff.DiffUserDataKeys;
import consulo.diff.content.DocumentContent;
import consulo.diff.internal.DiffUserDataKeysEx;
import consulo.diff.impl.internal.TextDiffSettingsHolder;
import consulo.diff.impl.internal.util.DiffImplUtil;
import consulo.diff.request.ContentDiffRequest;
import consulo.diff.request.DiffRequest;
import consulo.diff.request.SimpleDiffRequest;
import consulo.diff.util.LineCol;
import consulo.diff.util.Side;
import consulo.diff.util.ThreeSide;
import consulo.document.event.DocumentEvent;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.language.editor.impl.internal.markup.EditorMarkupModel;
import consulo.navigation.Navigatable;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.util.dataholder.Key;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NonNls;

import javax.swing.*;
import java.util.List;

public abstract class ThreesideTextDiffViewer extends ThreesideDiffViewer<TextEditorHolder> {
  @jakarta.annotation.Nullable
  private List<? extends EditorEx> myEditors;
  @Nonnull
  private final List<? extends EditorEx> myEditableEditors;

  @Nonnull
  private final MyVisibleAreaListener myVisibleAreaListener1 = new MyVisibleAreaListener(Side.LEFT);
  @Nonnull
  private final MyVisibleAreaListener myVisibleAreaListener2 = new MyVisibleAreaListener(Side.RIGHT);
  @Nullable
  protected SyncScrollSupport.ThreesideSyncScrollSupport mySyncScrollSupport;

  @Nonnull
  protected final SetEditorSettingsAction myEditorSettingsAction;

  public ThreesideTextDiffViewer(@Nonnull DiffContext context, @Nonnull ContentDiffRequest request) {
    super(context, request, TextEditorHolder.TextEditorHolderFactory.INSTANCE);

    //new MyFocusOppositePaneAction().setupAction(myPanel, this); // TODO

    myEditorSettingsAction = new SetEditorSettingsAction(getTextSettings(), getEditors());
    myEditorSettingsAction.applyDefaults();

    new MyOpenInEditorWithMouseAction().install(getEditors());

    myEditableEditors = TextDiffViewerUtil.getEditableEditors(getEditors());

    TextDiffViewerUtil.checkDifferentDocuments(myRequest);
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
    for (int i = 0; i < 3; i++) {
      if (forceReadOnly[i]) holders.get(i).getEditor().setViewer(true);
    }

    ThreeSide.LEFT.select(holders).getEditor().setVerticalScrollbarOrientation(EditorEx.VERTICAL_SCROLLBAR_LEFT);
    ((EditorMarkupModel)ThreeSide.BASE.select(holders).getEditor().getMarkupModel()).setErrorStripeVisible(false);

    for (TextEditorHolder holder : holders) {
      AWTDiffUtil.disableBlitting(holder.getEditor());
    }

    return holders;
  }

  @Nonnull
  @Override
  protected List<JComponent> createTitles() {
    return AWTDiffUtil.createSyncHeightComponents(AWTDiffUtil.createTextTitles(myRequest, getEditors()));
  }

  //
  // Listeners
  //

  @RequiredUIAccess
  protected void installEditorListeners() {
    new TextDiffViewerUtil.EditorActionsPopup(createEditorPopupActions()).install(getEditors(), myPanel);

    new TextDiffViewerUtil.EditorFontSizeSynchronizer(getEditors()).install(this);

    getEditor(ThreeSide.LEFT).getScrollingModel().addVisibleAreaListener(myVisibleAreaListener1);
    getEditor(ThreeSide.BASE).getScrollingModel().addVisibleAreaListener(myVisibleAreaListener1);

    getEditor(ThreeSide.BASE).getScrollingModel().addVisibleAreaListener(myVisibleAreaListener2);
    getEditor(ThreeSide.RIGHT).getScrollingModel().addVisibleAreaListener(myVisibleAreaListener2);

    SyncScrollSupport.SyncScrollable scrollable1 = getSyncScrollable(Side.LEFT);
    SyncScrollSupport.SyncScrollable scrollable2 = getSyncScrollable(Side.RIGHT);
    if (scrollable1 != null && scrollable2 != null) {
      mySyncScrollSupport = new SyncScrollSupport.ThreesideSyncScrollSupport(getEditors(), scrollable1, scrollable2);
      myEditorSettingsAction.setSyncScrollSupport(mySyncScrollSupport);
    }
  }

  @RequiredUIAccess
  public void destroyEditorListeners() {
    getEditor(ThreeSide.LEFT).getScrollingModel().removeVisibleAreaListener(myVisibleAreaListener1);
    getEditor(ThreeSide.BASE).getScrollingModel().removeVisibleAreaListener(myVisibleAreaListener1);

    getEditor(ThreeSide.BASE).getScrollingModel().removeVisibleAreaListener(myVisibleAreaListener2);
    getEditor(ThreeSide.RIGHT).getScrollingModel().removeVisibleAreaListener(myVisibleAreaListener2);

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
    myContentPanel.repaintDividers();
  }

  //
  // Getters
  //

  @Nonnull
  public EditorEx getCurrentEditor() {
    return getEditor(getCurrentSide());
  }

  @Nonnull
  public DocumentContent getCurrentContent() {
    return getContent(getCurrentSide());
  }

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
  public EditorEx getEditor(@Nonnull ThreeSide side) {
    return side.select(getEditors());
  }

  @Nonnull
  public DocumentContent getContent(@Nonnull ThreeSide side) {
    return side.select(getContents());
  }

  @jakarta.annotation.Nullable
  public ThreeSide getEditorSide(@jakarta.annotation.Nullable Editor editor) {
    if (getEditor(ThreeSide.BASE) == editor) return ThreeSide.BASE;
    if (getEditor(ThreeSide.RIGHT) == editor) return ThreeSide.RIGHT;
    if (getEditor(ThreeSide.LEFT) == editor) return ThreeSide.LEFT;
    return null;
  }

  //
  // Abstract
  //

  @RequiredUIAccess
  protected void scrollToLine(@Nonnull ThreeSide side, int line) {
    DiffImplUtil.scrollEditor(getEditor(side), line, false);
    setCurrentSide(side);
  }

  @jakarta.annotation.Nullable
  protected abstract SyncScrollSupport.SyncScrollable getSyncScrollable(@Nonnull Side side);

  @RequiredUIAccess
  @Nonnull
  protected LogicalPosition transferPosition(@Nonnull ThreeSide baseSide,
                                             @Nonnull ThreeSide targetSide,
                                             @Nonnull LogicalPosition position) {
    if (mySyncScrollSupport == null) return position;
    if (baseSide == targetSide) return position;

    SyncScrollSupport.SyncScrollable scrollable12 = mySyncScrollSupport.getScrollable12();
    SyncScrollSupport.SyncScrollable scrollable23 = mySyncScrollSupport.getScrollable23();

    int baseLine; // line number in BASE
    if (baseSide == ThreeSide.LEFT) {
      baseLine = scrollable12.transfer(Side.LEFT, position.line);
    }
    else if (baseSide == ThreeSide.RIGHT) {
      baseLine = scrollable23.transfer(Side.RIGHT, position.line);
    }
    else {
      baseLine = position.line;
    }

    int targetLine;
    if (targetSide == ThreeSide.LEFT) {
      targetLine = scrollable12.transfer(Side.RIGHT, baseLine);
    }
    else if (targetSide == ThreeSide.RIGHT) {
      targetLine = scrollable23.transfer(Side.LEFT, baseLine);
    }
    else {
      targetLine = baseLine;
    }

    return new LogicalPosition(targetLine, position.column);
  }

  //
  // Misc
  //

  @jakarta.annotation.Nullable
  @Override
  protected Navigatable getNavigatable() {
    return getCurrentContent().getNavigatable(LineCol.fromCaret(getCurrentEditor()));
  }

  public static boolean canShowRequest(@Nonnull DiffContext context, @Nonnull DiffRequest request) {
    return ThreesideDiffViewer.canShowRequest(context, request, TextEditorHolder.TextEditorHolderFactory.INSTANCE);
  }

  //
  // Actions
  //

  private class MyOpenInEditorWithMouseAction extends OpenInEditorWithMouseAction {
    @Override
    protected Navigatable getNavigatable(@Nonnull Editor editor, int line) {
      ThreeSide side = getEditorSide(editor);
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

  @Nullable
  @Override
  public Object getData(@Nonnull @NonNls Key<?> dataId) {
    if (DiffDataKeys.CURRENT_EDITOR == dataId) {
      return getCurrentEditor();
    }
    return super.getData(dataId);
  }

  private class MyVisibleAreaListener implements VisibleAreaListener {
    @Nonnull
    Side mySide;

    public MyVisibleAreaListener(@Nonnull Side side) {
      mySide = side;
    }

    @Override
    public void visibleAreaChanged(VisibleAreaEvent e) {
      if (mySyncScrollSupport != null) mySyncScrollSupport.visibleAreaChanged(e);
      myContentPanel.repaint();
    }
  }

  protected abstract class MyInitialScrollPositionHelper extends InitialScrollPositionSupport.ThreesideInitialScrollHelper {
    @Nonnull
    @Override
    protected List<? extends Editor> getEditors() {
      return ThreesideTextDiffViewer.this.getEditors();
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

  public class TextShowPartialDiffAction extends ThreesideDiffViewer.ShowPartialDiffAction {
    public TextShowPartialDiffAction(@Nonnull PartialDiffMode mode) {
      super(mode);
    }

    @Nonnull
    @Override
    protected SimpleDiffRequest createRequest() {
      SimpleDiffRequest request = super.createRequest();

      ThreeSide currentSide = getCurrentSide();
      LogicalPosition currentPosition = DiffImplUtil.getCaretPosition(getCurrentEditor());

      // we won't use DiffUserDataKeysEx.EDITORS_CARET_POSITION to avoid desync scroll position (as they can point to different places)
      // TODO: pass EditorsVisiblePositions in case if view was scrolled without changing caret position ?
      if (currentSide == mySide1) {
        request.putUserData(DiffUserDataKeys.SCROLL_TO_LINE, consulo.util.lang.Pair.create(Side.LEFT, currentPosition.line));
      }
      else if (currentSide == mySide2) {
        request.putUserData(DiffUserDataKeys.SCROLL_TO_LINE, consulo.util.lang.Pair.create(Side.RIGHT, currentPosition.line));
      }
      else {
        LogicalPosition position1 = transferPosition(currentSide, mySide1, currentPosition);
        LogicalPosition position2 = transferPosition(currentSide, mySide2, currentPosition);
        request.putUserData(DiffUserDataKeysEx.EDITORS_CARET_POSITION, new LogicalPosition[]{position1, position2});
      }

      return request;
    }
  }
}
