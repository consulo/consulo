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
package com.intellij.diff.tools.simple;

import com.intellij.diff.DiffContext;
import com.intellij.diff.actions.AllLinesIterator;
import com.intellij.diff.actions.BufferedLineIterator;
import com.intellij.diff.comparison.DiffTooBigException;
import com.intellij.diff.fragments.LineFragment;
import com.intellij.diff.requests.ContentDiffRequest;
import com.intellij.diff.requests.DiffRequest;
import com.intellij.diff.tools.util.*;
import com.intellij.diff.tools.util.base.HighlightPolicy;
import com.intellij.diff.tools.util.base.TextDiffViewerUtil;
import com.intellij.diff.tools.util.side.TwosideTextDiffViewer;
import com.intellij.diff.util.*;
import com.intellij.diff.util.DiffUserDataKeysEx.ScrollToPolicy;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.diff.DiffBundle;
import com.intellij.openapi.diff.DiffNavigationContext;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.DumbAware;
import consulo.awt.TargetAWT;
import consulo.disposer.Disposable;
import consulo.util.dataholder.Key;
import com.intellij.openapi.util.ThrowableComputable;
import consulo.util.dataholder.UserDataHolder;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.containers.ContainerUtil;
import consulo.annotation.access.RequiredWriteAction;
import consulo.application.AccessRule;
import consulo.logging.Logger;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;

import static com.intellij.util.ObjectUtils.assertNotNull;

public class SimpleDiffViewer extends TwosideTextDiffViewer {
  public static final Logger LOG = Logger.getInstance(SimpleDiffViewer.class);

  @Nonnull
  private final SyncScrollSupport.SyncScrollable mySyncScrollable;
  @Nonnull
  private final PrevNextDifferenceIterable myPrevNextDifferenceIterable;
  @Nonnull
  private final StatusPanel myStatusPanel;

  @Nonnull
  private final List<SimpleDiffChange> myDiffChanges = new ArrayList<>();
  @Nonnull
  private final List<SimpleDiffChange> myInvalidDiffChanges = new ArrayList<>();
  private boolean myIsContentsEqual;

  @Nonnull
  private final MyFoldingModel myFoldingModel;
  @Nonnull
  private final MyInitialScrollHelper myInitialScrollHelper = new MyInitialScrollHelper();
  @Nonnull
  private final ModifierProvider myModifierProvider;

  public SimpleDiffViewer(@Nonnull DiffContext context, @Nonnull DiffRequest request) {
    super(context, (ContentDiffRequest)request);

    mySyncScrollable = new MySyncScrollable();
    myPrevNextDifferenceIterable = new MyPrevNextDifferenceIterable();
    myStatusPanel = new MyStatusPanel();
    myFoldingModel = new MyFoldingModel(getEditors(), this);

    myModifierProvider = new ModifierProvider();

    DiffUtil.registerAction(new ReplaceSelectedChangesAction(Side.LEFT, true), myPanel);
    DiffUtil.registerAction(new AppendSelectedChangesAction(Side.LEFT, true), myPanel);
    DiffUtil.registerAction(new ReplaceSelectedChangesAction(Side.RIGHT, true), myPanel);
    DiffUtil.registerAction(new AppendSelectedChangesAction(Side.RIGHT, true), myPanel);
  }

  @Override
  @RequiredUIAccess
  protected void onInit() {
    super.onInit();
    myContentPanel.setPainter(new MyDividerPainter());
    myModifierProvider.init();
  }

  @Override
  @RequiredUIAccess
  protected void onDispose() {
    destroyChangedBlocks();
    myFoldingModel.destroy();
    super.onDispose();
  }

  @Nonnull
  @Override
  protected List<AnAction> createToolbarActions() {
    List<AnAction> group = new ArrayList<>();

    group.add(new MyIgnorePolicySettingAction());
    group.add(new MyHighlightPolicySettingAction());
    group.add(new MyToggleExpandByDefaultAction());
    group.add(new MyToggleAutoScrollAction());
    group.add(new MyReadOnlyLockAction());
    group.add(myEditorSettingsAction);

    group.add(AnSeparator.getInstance());
    group.addAll(super.createToolbarActions());

    return group;
  }

  @Nonnull
  @Override
  protected List<AnAction> createPopupActions() {
    List<AnAction> group = new ArrayList<>();

    group.add(AnSeparator.getInstance());
    group.add(new MyIgnorePolicySettingAction().getPopupGroup());
    group.add(AnSeparator.getInstance());
    group.add(new MyHighlightPolicySettingAction().getPopupGroup());
    group.add(AnSeparator.getInstance());
    group.add(new MyToggleAutoScrollAction());
    group.add(new MyToggleExpandByDefaultAction());

    group.add(AnSeparator.getInstance());
    group.addAll(super.createPopupActions());

    return group;
  }

  @Nonnull
  @Override
  protected List<AnAction> createEditorPopupActions() {
    List<AnAction> group = new ArrayList<>();

    group.add(new ReplaceSelectedChangesAction(Side.LEFT, false));
    group.add(new AppendSelectedChangesAction(Side.LEFT, false));
    group.add(new ReplaceSelectedChangesAction(Side.RIGHT, false));
    group.add(new AppendSelectedChangesAction(Side.RIGHT, false));

    group.add(AnSeparator.getInstance());
    group.addAll(super.createEditorPopupActions());

    return group;
  }

  @Override
  @RequiredUIAccess
  protected void processContextHints() {
    super.processContextHints();
    myInitialScrollHelper.processContext(myRequest);
  }

  @Override
  @RequiredUIAccess
  protected void updateContextHints() {
    super.updateContextHints();
    myFoldingModel.updateContext(myRequest, getFoldingModelSettings());
    myInitialScrollHelper.updateContext(myRequest);
  }

  //
  // Diff
  //

  @Nonnull
  public FoldingModelSupport.Settings getFoldingModelSettings() {
    return TextDiffViewerUtil.getFoldingModelSettings(myContext);
  }

  @Override
  protected void onSlowRediff() {
    super.onSlowRediff();
    myStatusPanel.setBusy(true);
    myInitialScrollHelper.onSlowRediff();
  }

  @Override
  @Nonnull
  protected Runnable performRediff(@Nonnull final ProgressIndicator indicator) {
    try {
      indicator.checkCanceled();

      final Document document1 = getContent1().getDocument();
      final Document document2 = getContent2().getDocument();

      ThrowableComputable<CharSequence[],RuntimeException> action = () -> {
        return new CharSequence[]{document1.getImmutableCharSequence(), document2.getImmutableCharSequence()};
      };
      CharSequence[] texts = AccessRule.read(action);

      List<LineFragment> lineFragments = null;
      if (getHighlightPolicy().isShouldCompare()) {
        lineFragments = DiffUtil.compare(myRequest, texts[0], texts[1], getDiffConfig(), indicator);
      }

      boolean isContentsEqual = (lineFragments == null || lineFragments.isEmpty()) &&
                                StringUtil.equals(texts[0], texts[1]);

      return apply(new CompareData(lineFragments, isContentsEqual));
    }
    catch (DiffTooBigException e) {
      return applyNotification(DiffNotifications.createDiffTooBig());
    }
    catch (ProcessCanceledException e) {
      throw e;
    }
    catch (Throwable e) {
      LOG.error(e);
      return applyNotification(DiffNotifications.createError());
    }
  }

  @Nonnull
  private Runnable apply(@Nonnull final CompareData data) {
    return () -> {
      myFoldingModel.updateContext(myRequest, getFoldingModelSettings());

      clearDiffPresentation();

      myIsContentsEqual = data.isContentsEqual();
      if (data.isContentsEqual()) {
        boolean equalCharsets = TextDiffViewerUtil.areEqualCharsets(getContents());
        boolean equalSeparators = TextDiffViewerUtil.areEqualLineSeparators(getContents());
        myPanel.addNotification(DiffNotifications.createEqualContents(equalCharsets, equalSeparators));
      }

      List<LineFragment> fragments = data.getFragments();
      if (fragments != null) {
        for (int i = 0; i < fragments.size(); i++) {
          LineFragment fragment = fragments.get(i);
          LineFragment previousFragment = i != 0 ? fragments.get(i - 1) : null;

          myDiffChanges.add(new SimpleDiffChange(this, fragment, previousFragment));
        }
      }

      myFoldingModel.install(fragments, myRequest, getFoldingModelSettings());

      myInitialScrollHelper.onRediff();

      myContentPanel.repaintDivider();
      myStatusPanel.update();
    };
  }

  @Nonnull
  private Runnable applyNotification(@Nullable final JComponent notification) {
    return () -> {
      clearDiffPresentation();
      myFoldingModel.destroy();
      if (notification != null) myPanel.addNotification(notification);
    };
  }

  private void clearDiffPresentation() {
    myStatusPanel.setBusy(false);
    myPanel.resetNotifications();
    destroyChangedBlocks();
  }

  @Nonnull
  private DiffUtil.DiffConfig getDiffConfig() {
    return new DiffUtil.DiffConfig(getTextSettings().getIgnorePolicy(), getHighlightPolicy());
  }

  @Nonnull
  private HighlightPolicy getHighlightPolicy() {
    return getTextSettings().getHighlightPolicy();
  }

  //
  // Impl
  //

  private void destroyChangedBlocks() {
    myIsContentsEqual = false;

    for (SimpleDiffChange change : myDiffChanges) {
      change.destroyHighlighter();
    }
    myDiffChanges.clear();

    for (SimpleDiffChange change : myInvalidDiffChanges) {
      change.destroyHighlighter();
    }
    myInvalidDiffChanges.clear();

    myContentPanel.repaintDivider();
    myStatusPanel.update();
  }

  @Override
  @RequiredUIAccess
  protected void onBeforeDocumentChange(@Nonnull DocumentEvent e) {
    super.onBeforeDocumentChange(e);
    if (myDiffChanges.isEmpty()) return;

    List<Document> documents = ContainerUtil.map(getEditors(), Editor::getDocument);
    Side side = Side.fromValue(documents, e.getDocument());
    if (side == null) {
      LOG.warn("Unknown document changed");
      return;
    }

    LineRange lineRange = DiffUtil.getAffectedLineRange(e);
    int shift = DiffUtil.countLinesShift(e);

    List<SimpleDiffChange> invalid = new ArrayList<>();
    for (SimpleDiffChange change : myDiffChanges) {
      if (change.processChange(lineRange.start, lineRange.end, shift, side)) {
        invalid.add(change);
      }
    }

    if (!invalid.isEmpty()) {
      myDiffChanges.removeAll(invalid);
      myInvalidDiffChanges.addAll(invalid);
    }
  }

  @RequiredUIAccess
  protected boolean doScrollToChange(@Nonnull ScrollToPolicy scrollToPolicy) {
    SimpleDiffChange targetChange = scrollToPolicy.select(myDiffChanges);
    if (targetChange == null) return false;

    doScrollToChange(targetChange, false);
    return true;
  }

  private void doScrollToChange(@Nonnull SimpleDiffChange change, final boolean animated) {
    final int line1 = change.getStartLine(Side.LEFT);
    final int line2 = change.getStartLine(Side.RIGHT);
    final int endLine1 = change.getEndLine(Side.LEFT);
    final int endLine2 = change.getEndLine(Side.RIGHT);

    DiffUtil.moveCaret(getEditor1(), line1);
    DiffUtil.moveCaret(getEditor2(), line2);

    getSyncScrollSupport().makeVisible(getCurrentSide(), line1, endLine1, line2, endLine2, animated);
  }

  protected boolean doScrollToContext(@Nonnull DiffNavigationContext context) {
    ChangedLinesIterator changedLinesIterator = new ChangedLinesIterator();
    int line = context.contextMatchCheck(changedLinesIterator);
    if (line == -1) {
      // this will work for the case, when spaces changes are ignored, and corresponding fragments are not reported as changed
      // just try to find target line  -> +-
      AllLinesIterator allLinesIterator = new AllLinesIterator(getEditor(Side.RIGHT).getDocument());
      line = context.contextMatchCheck(allLinesIterator);
    }
    if (line == -1) return false;

    scrollToLine(Side.RIGHT, line);
    return true;
  }

  //
  // Getters
  //

  @Nonnull
  protected List<SimpleDiffChange> getDiffChanges() {
    return myDiffChanges;
  }

  @Nonnull
  @Override
  protected SyncScrollSupport.SyncScrollable getSyncScrollable() {
    return mySyncScrollable;
  }

  @Nonnull
  @Override
  protected JComponent getStatusPanel() {
    return myStatusPanel;
  }

  @Nonnull
  public ModifierProvider getModifierProvider() {
    return myModifierProvider;
  }

  @Nonnull
  @Override
  public SyncScrollSupport.TwosideSyncScrollSupport getSyncScrollSupport() {
    //noinspection ConstantConditions
    return super.getSyncScrollSupport();
  }

  protected boolean isEditable(@Nonnull Side side) {
    return DiffUtil.isEditable(getEditor(side));
  }

  //
  // Misc
  //

  @SuppressWarnings("MethodOverridesStaticMethodOfSuperclass")
  public static boolean canShowRequest(@Nonnull DiffContext context, @Nonnull DiffRequest request) {
    return TwosideTextDiffViewer.canShowRequest(context, request);
  }

  @Nonnull
  @RequiredUIAccess
  private List<SimpleDiffChange> getSelectedChanges(@Nonnull Side side) {
    final BitSet lines = DiffUtil.getSelectedLines(getEditor(side));

    List<SimpleDiffChange> affectedChanges = new ArrayList<>();
    for (int i = myDiffChanges.size() - 1; i >= 0; i--) {
      SimpleDiffChange change = myDiffChanges.get(i);
      int line1 = change.getStartLine(side);
      int line2 = change.getEndLine(side);

      if (DiffUtil.isSelectedByLine(lines, line1, line2)) {
        affectedChanges.add(change);
      }
    }
    return affectedChanges;
  }

  @Nullable
  @RequiredUIAccess
  private SimpleDiffChange getSelectedChange(@Nonnull Side side) {
    int caretLine = getEditor(side).getCaretModel().getLogicalPosition().line;

    for (SimpleDiffChange change : myDiffChanges) {
      int line1 = change.getStartLine(side);
      int line2 = change.getEndLine(side);

      if (DiffUtil.isSelectedByLine(caretLine, line1, line2)) return change;
    }
    return null;
  }

  //
  // Actions
  //

  private class MyPrevNextDifferenceIterable extends PrevNextDifferenceIterableBase<SimpleDiffChange> {
    @Nonnull
    @Override
    protected List<SimpleDiffChange> getChanges() {
      return myDiffChanges;
    }

    @Nonnull
    @Override
    protected EditorEx getEditor() {
      return getCurrentEditor();
    }

    @Override
    protected int getStartLine(@Nonnull SimpleDiffChange change) {
      return change.getStartLine(getCurrentSide());
    }

    @Override
    protected int getEndLine(@Nonnull SimpleDiffChange change) {
      return change.getEndLine(getCurrentSide());
    }

    @Override
    protected void scrollToChange(@Nonnull SimpleDiffChange change) {
      doScrollToChange(change, true);
    }
  }

  private class MyReadOnlyLockAction extends TextDiffViewerUtil.EditorReadOnlyLockAction {
    public MyReadOnlyLockAction() {
      super(getContext(), getEditableEditors());
    }

    @Override
    protected void doApply(boolean readOnly) {
      super.doApply(readOnly);
      for (SimpleDiffChange change : myDiffChanges) {
        change.updateGutterActions(true);
      }
    }
  }

  //
  // Modification operations
  //

  private abstract class ApplySelectedChangesActionBase extends AnAction implements DumbAware {
    @Nonnull
    protected final Side myModifiedSide;
    private final boolean myShortcut;

    public ApplySelectedChangesActionBase(@Nonnull Side modifiedSide, boolean shortcut) {
      myModifiedSide = modifiedSide;
      myShortcut = shortcut;
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
      if (myShortcut) {
        // consume shortcut even if there are nothing to do - avoid calling some other action
        e.getPresentation().setEnabledAndVisible(true);
        return;
      }

      Editor editor = e.getData(CommonDataKeys.EDITOR);
      Side side = Side.fromValue(getEditors(), editor);
      if (side == null || !isVisible(side)) {
        e.getPresentation().setEnabledAndVisible(false);
        return;
      }

      if (!isEditable(myModifiedSide)) {
        e.getPresentation().setEnabledAndVisible(false);
        return;
      }

      e.getPresentation().setText(getText(side));
      e.getPresentation().setIcon(getIcon(side));

      e.getPresentation().setVisible(true);
      e.getPresentation().setEnabled(isSomeChangeSelected(side));
    }

    @Override
    public void actionPerformed(@Nonnull final AnActionEvent e) {
      Editor editor = e.getData(CommonDataKeys.EDITOR);
      final Side side = assertNotNull(Side.fromValue(getEditors(), editor));
      final List<SimpleDiffChange> selectedChanges = getSelectedChanges(side);
      if (selectedChanges.isEmpty()) return;

      if (!isEditable(myModifiedSide)) return;

      String title = e.getPresentation().getText() + " selected changes";
      DiffUtil.executeWriteCommand(getEditor(myModifiedSide).getDocument(), e.getProject(), title, () -> {
        apply(selectedChanges);
      });
    }

    protected boolean isSomeChangeSelected(@Nonnull Side side) {
      if (myDiffChanges.isEmpty()) return false;

      EditorEx editor = getEditor(side);
      List<Caret> carets = editor.getCaretModel().getAllCarets();
      if (carets.size() != 1) return true;
      Caret caret = carets.get(0);
      if (caret.hasSelection()) return true;
      int line = editor.getDocument().getLineNumber(editor.getExpectedCaretOffset());

      for (SimpleDiffChange change : myDiffChanges) {
        if (change.isSelectedByLine(line, side)) return true;
      }
      return false;
    }

    protected boolean isBothEditable() {
      return isEditable(Side.LEFT) && isEditable(Side.RIGHT);
    }

    protected abstract boolean isVisible(@Nonnull Side side);

    @Nonnull
    protected abstract String getText(@Nonnull Side side);

    @Nullable
    protected abstract consulo.ui.image.Image getIcon(@Nonnull Side side);

    @RequiredWriteAction
    protected abstract void apply(@Nonnull List<SimpleDiffChange> changes);
  }

  private class ReplaceSelectedChangesAction extends ApplySelectedChangesActionBase {
    public ReplaceSelectedChangesAction(@Nonnull Side focusedSide, boolean shortcut) {
      super(focusedSide.other(), shortcut);
      setShortcutSet(ActionManager.getInstance().getAction(focusedSide.select("Diff.ApplyLeftSide", "Diff.ApplyRightSide")).getShortcutSet());
    }

    @Override
    protected boolean isVisible(@Nonnull Side side) {
      return !isBothEditable() || side == myModifiedSide.other();
    }

    @Nonnull
    @Override
    protected String getText(@Nonnull Side side) {
      return "Accept";
    }

    @Nullable
    @Override
    protected Image getIcon(@Nonnull Side side) {
      return DiffUtil.getArrowIcon(myModifiedSide.other());
    }

    @Override
    protected void apply(@Nonnull List<SimpleDiffChange> changes) {
      for (SimpleDiffChange change : changes) {
        replaceChange(change, myModifiedSide.other());
      }
    }
  }

  private class AppendSelectedChangesAction extends ApplySelectedChangesActionBase {
    public AppendSelectedChangesAction(@Nonnull Side focusedSide, boolean shortcut) {
      super(focusedSide.other(), shortcut);
      setShortcutSet(ActionManager.getInstance().getAction(focusedSide.select("Diff.AppendLeftSide", "Diff.AppendRightSide")).getShortcutSet());
    }

    @Override
    protected boolean isVisible(@Nonnull Side side) {
      return !isBothEditable() || side == myModifiedSide.other();
    }

    @Nonnull
    @Override
    protected String getText(@Nonnull Side side) {
      return isBothEditable() ? myModifiedSide.select("Append to the Left", "Append to the Right") : "Append";
    }

    @Nullable
    @Override
    protected Image getIcon(@Nonnull Side side) {
      return DiffUtil.getArrowDownIcon(myModifiedSide.other());
    }

    @Override
    protected void apply(@Nonnull List<SimpleDiffChange> changes) {
      for (SimpleDiffChange change : changes) {
        appendChange(change, myModifiedSide.other());
      }
    }
  }

  @RequiredWriteAction
  public void replaceChange(@Nonnull SimpleDiffChange change, @Nonnull final Side sourceSide) {
    if (!change.isValid()) return;
    Side outputSide = sourceSide.other();

    DiffUtil.applyModification(getEditor(outputSide).getDocument(), change.getStartLine(outputSide), change.getEndLine(outputSide),
                               getEditor(sourceSide).getDocument(), change.getStartLine(sourceSide), change.getEndLine(sourceSide));

    change.destroyHighlighter();
    myDiffChanges.remove(change);
  }

  @RequiredWriteAction
  public void appendChange(@Nonnull SimpleDiffChange change, @Nonnull final Side sourceSide) {
    if (!change.isValid()) return;
    if (change.getStartLine(sourceSide) == change.getEndLine(sourceSide)) return;
    Side outputSide = sourceSide.other();

    DiffUtil.applyModification(getEditor(outputSide).getDocument(), change.getEndLine(outputSide), change.getEndLine(outputSide),
                               getEditor(sourceSide).getDocument(), change.getStartLine(sourceSide), change.getEndLine(sourceSide));

    change.destroyHighlighter();
    myDiffChanges.remove(change);
  }

  private class MyHighlightPolicySettingAction extends TextDiffViewerUtil.HighlightPolicySettingAction {
    public MyHighlightPolicySettingAction() {
      super(getTextSettings());
    }

    @Override
    protected void onSettingsChanged() {
      rediff();
    }
  }

  private class MyIgnorePolicySettingAction extends TextDiffViewerUtil.IgnorePolicySettingAction {
    public MyIgnorePolicySettingAction() {
      super(getTextSettings());
    }

    @Override
    protected void onSettingsChanged() {
      rediff();
    }
  }

  private class MyToggleExpandByDefaultAction extends TextDiffViewerUtil.ToggleExpandByDefaultAction {
    public MyToggleExpandByDefaultAction() {
      super(getTextSettings());
    }

    @Override
    protected void expandAll(boolean expand) {
      myFoldingModel.expandAll(expand);
    }
  }

  //
  // Scroll from annotate
  //

  private class ChangedLinesIterator extends BufferedLineIterator {
    private int myIndex = 0;

    private ChangedLinesIterator() {
      init();
    }

    @Override
    public boolean hasNextBlock() {
      return myIndex < myDiffChanges.size();
    }

    @Override
    public void loadNextBlock() {
      SimpleDiffChange change = myDiffChanges.get(myIndex);
      myIndex++;

      int line1 = change.getStartLine(Side.RIGHT);
      int line2 = change.getEndLine(Side.RIGHT);

      Document document = getEditor(Side.RIGHT).getDocument();

      for (int i = line1; i < line2; i++) {
        int offset1 = document.getLineStartOffset(i);
        int offset2 = document.getLineEndOffset(i);

        CharSequence text = document.getImmutableCharSequence().subSequence(offset1, offset2);
        addLine(i, text);
      }
    }
  }

  //
  // Helpers
  //

  @Nullable
  @Override
  public Object getData(@Nonnull @NonNls Key<?> dataId) {
    if (DiffDataKeys.PREV_NEXT_DIFFERENCE_ITERABLE == dataId) {
      return myPrevNextDifferenceIterable;
    }
    else if (DiffDataKeys.CURRENT_CHANGE_RANGE == dataId) {
      SimpleDiffChange change = getSelectedChange(getCurrentSide());
      if (change != null) {
        return new LineRange(change.getStartLine(getCurrentSide()), change.getEndLine(getCurrentSide()));
      }
    }
    return super.getData(dataId);
  }

  private class MySyncScrollable extends BaseSyncScrollable {
    @Override
    public boolean isSyncScrollEnabled() {
      return getTextSettings().isEnableSyncScroll();
    }

    @Override
    public int transfer(@Nonnull Side baseSide, int line) {
      if (myDiffChanges.isEmpty()) {
        return line;
      }

      return super.transfer(baseSide, line);
    }

    @Override
    protected void processHelper(@Nonnull ScrollHelper helper) {
      if (!helper.process(0, 0)) return;
      for (SimpleDiffChange diffChange : myDiffChanges) {
        if (!helper.process(diffChange.getStartLine(Side.LEFT), diffChange.getStartLine(Side.RIGHT))) return;
        if (!helper.process(diffChange.getEndLine(Side.LEFT), diffChange.getEndLine(Side.RIGHT))) return;
      }
      helper.process(getEditor1().getDocument().getLineCount(), getEditor2().getDocument().getLineCount());
    }
  }

  private class MyDividerPainter implements DiffSplitter.Painter, DiffDividerDrawUtil.DividerPaintable {
    @Override
    public void paint(@Nonnull Graphics g, @Nonnull JComponent divider) {
      Graphics2D gg = DiffDividerDrawUtil.getDividerGraphics(g, divider, getEditor1().getComponent());

      gg.setColor(TargetAWT.to(DiffDrawUtil.getDividerColor(getEditor1())));
      gg.fill(gg.getClipBounds());

      //DividerPolygonUtil.paintSimplePolygons(gg, divider.getWidth(), getEditor1(), getEditor2(), this);
      DiffDividerDrawUtil.paintPolygons(gg, divider.getWidth(), getEditor1(), getEditor2(), this);

      myFoldingModel.paintOnDivider(gg, divider);

      gg.dispose();
    }

    @Override
    public void process(@Nonnull Handler handler) {
      for (SimpleDiffChange diffChange : myDiffChanges) {
        if (!handler.process(diffChange.getStartLine(Side.LEFT), diffChange.getEndLine(Side.LEFT),
                             diffChange.getStartLine(Side.RIGHT), diffChange.getEndLine(Side.RIGHT),
                             diffChange.getDiffType().getColor(getEditor1()))) {
          return;
        }
      }
    }
  }

  private class MyStatusPanel extends StatusPanel {
    @Nullable
    @Override
    protected String getMessage() {
      if (getHighlightPolicy() == HighlightPolicy.DO_NOT_HIGHLIGHT) return DiffBundle.message("diff.highlighting.disabled.text");
      int changesCount = myDiffChanges.size() + myInvalidDiffChanges.size();
      if (changesCount == 0 && !myIsContentsEqual) {
        return DiffBundle.message("diff.all.differences.ignored.text");
      }
      return DiffBundle.message("diff.count.differences.status.text", changesCount);
    }
  }

  private static class CompareData {
    @Nullable
    private final List<LineFragment> myFragments;
    private final boolean myIsContentsEqual;

    public CompareData(@Nullable List<LineFragment> fragments, boolean isContentsEqual) {
      myFragments = fragments;
      myIsContentsEqual = isContentsEqual;
    }

    @Nullable
    public List<LineFragment> getFragments() {
      return myFragments;
    }

    public boolean isContentsEqual() {
      return myIsContentsEqual;
    }
  }

  public class ModifierProvider extends KeyboardModifierListener {
    public void init() {
      init(myPanel, SimpleDiffViewer.this);
    }

    @Override
    public void onModifiersChanged() {
      for (SimpleDiffChange change : myDiffChanges) {
        change.updateGutterActions(false);
      }
    }
  }

  private static class MyFoldingModel extends FoldingModelSupport {
    private final MyPaintable myPaintable = new MyPaintable(0, 1);

    public MyFoldingModel(@Nonnull List<? extends EditorEx> editors, @Nonnull Disposable disposable) {
      super(editors.toArray(new EditorEx[2]), disposable);
    }

    public void install(@Nullable final List<LineFragment> fragments,
                        @Nonnull UserDataHolder context,
                        @Nonnull FoldingModelSupport.Settings settings) {
      Iterator<int[]> it = map(fragments, fragment -> new int[]{
              fragment.getStartLine1(),
              fragment.getEndLine1(),
              fragment.getStartLine2(),
              fragment.getEndLine2()
      });
      install(it, context, settings);
    }

    public void paintOnDivider(@Nonnull Graphics2D gg, @Nonnull Component divider) {
      myPaintable.paintOnDivider(gg, divider);
    }
  }

  private class MyInitialScrollHelper extends MyInitialScrollPositionHelper {
    @Override
    protected boolean doScrollToChange() {
      if (myScrollToChange == null) return false;
      return SimpleDiffViewer.this.doScrollToChange(myScrollToChange);
    }

    @Override
    protected boolean doScrollToFirstChange() {
      return SimpleDiffViewer.this.doScrollToChange(ScrollToPolicy.FIRST_CHANGE);
    }

    @Override
    protected boolean doScrollToContext() {
      if (myNavigationContext == null) return false;
      return SimpleDiffViewer.this.doScrollToContext(myNavigationContext);
    }
  }
}
