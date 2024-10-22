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
package consulo.desktop.awt.internal.diff.simple;

import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorEx;
import consulo.desktop.awt.internal.diff.ThreesideDiffChangeBase;
import consulo.desktop.awt.internal.diff.util.*;
import consulo.desktop.awt.internal.diff.util.side.ThreesideTextDiffViewer;
import consulo.diff.DiffContext;
import consulo.diff.DiffDataKeys;
import consulo.diff.PrevNextDifferenceIterable;
import consulo.diff.fragment.MergeLineFragment;
import consulo.diff.impl.internal.util.DiffImplUtil;
import consulo.diff.impl.internal.util.PrevNextDifferenceIterableBase;
import consulo.diff.internal.DiffUserDataKeysEx.ScrollToPolicy;
import consulo.diff.localize.DiffLocalize;
import consulo.diff.request.ContentDiffRequest;
import consulo.diff.util.LineRange;
import consulo.diff.util.Side;
import consulo.diff.util.ThreeSide;
import consulo.disposer.Disposable;
import consulo.document.event.DocumentEvent;
import consulo.ide.impl.diff.DiffDrawUtil;
import consulo.logging.Logger;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.util.dataholder.Key;
import consulo.util.dataholder.UserDataHolder;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Iterator;
import java.util.List;

public abstract class ThreesideTextDiffViewerEx extends ThreesideTextDiffViewer {
    public static final Logger LOG = Logger.getInstance(ThreesideTextDiffViewerEx.class);

    @Nonnull
    private final SyncScrollSupport.SyncScrollable mySyncScrollable1;
    @Nonnull
    private final SyncScrollSupport.SyncScrollable mySyncScrollable2;

    @Nonnull
    protected final PrevNextDifferenceIterable myPrevNextDifferenceIterable;
    @Nonnull
    protected final MyStatusPanel myStatusPanel;

    @Nonnull
    protected final MyFoldingModel myFoldingModel;
    @Nonnull
    protected final MyInitialScrollHelper myInitialScrollHelper = new MyInitialScrollHelper();

    private int myChangesCount = -1;
    private int myConflictsCount = -1;

    public ThreesideTextDiffViewerEx(@Nonnull DiffContext context, @Nonnull ContentDiffRequest request) {
        super(context, request);

        mySyncScrollable1 = new MySyncScrollable(Side.LEFT);
        mySyncScrollable2 = new MySyncScrollable(Side.RIGHT);
        myPrevNextDifferenceIterable = new MyPrevNextDifferenceIterable();
        myStatusPanel = new MyStatusPanel();
        myFoldingModel = new MyFoldingModel(getEditors().toArray(new EditorEx[3]), this);
    }

    @Override
    @RequiredUIAccess
    protected void onInit() {
        super.onInit();
        myContentPanel.setPainter(new MyDividerPainter(Side.LEFT), Side.LEFT);
        myContentPanel.setPainter(new MyDividerPainter(Side.RIGHT), Side.RIGHT);
    }

    @Override
    @RequiredUIAccess
    protected void onDispose() {
        destroyChangedBlocks();
        super.onDispose();
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

    @Nonnull
    protected Runnable applyNotification(@Nullable final JComponent notification) {
        return () -> {
            clearDiffPresentation();
            if (notification != null) {
                myPanel.addNotification(notification);
            }
        };
    }

    protected void clearDiffPresentation() {
        myStatusPanel.setBusy(false);
        myPanel.resetNotifications();
        destroyChangedBlocks();

        myContentPanel.repaintDividers();
        myStatusPanel.update();
    }

    protected void destroyChangedBlocks() {
        myFoldingModel.destroy();
    }

    //
    // Impl
    //

    @Override
    protected void onDocumentChange(@Nonnull DocumentEvent e) {
        super.onDocumentChange(e);
        myFoldingModel.onDocumentChanged(e);
    }

    @RequiredUIAccess
    protected boolean doScrollToChange(@Nonnull ScrollToPolicy scrollToPolicy) {
        ThreesideDiffChangeBase targetChange = scrollToPolicy.select(getChanges());
        if (targetChange == null) {
            return false;
        }

        doScrollToChange(targetChange, false);
        return true;
    }

    protected void doScrollToChange(@Nonnull ThreesideDiffChangeBase change, boolean animated) {
        int[] startLines = new int[3];
        int[] endLines = new int[3];

        for (int i = 0; i < 3; i++) {
            ThreeSide side = ThreeSide.fromIndex(i);
            startLines[i] = change.getStartLine(side);
            endLines[i] = change.getEndLine(side);
            DiffImplUtil.moveCaret(getEditor(side), startLines[i]);
        }

        getSyncScrollSupport().makeVisible(getCurrentSide(), startLines, endLines, animated);
    }

    //
    // Counters
    //

    public int getChangesCount() {
        return myChangesCount;
    }

    public int getConflictsCount() {
        return myConflictsCount;
    }

    protected void resetChangeCounters() {
        myChangesCount = 0;
        myConflictsCount = 0;
    }

    protected void onChangeAdded(@Nonnull ThreesideDiffChangeBase change) {
        if (change.isConflict()) {
            myConflictsCount++;
        }
        else {
            myChangesCount++;
        }
        myStatusPanel.update();
    }

    protected void onChangeRemoved(@Nonnull ThreesideDiffChangeBase change) {
        if (change.isConflict()) {
            myConflictsCount--;
        }
        else {
            myChangesCount--;
        }
        myStatusPanel.update();
    }

    //
    // Getters
    //

    @Nonnull
    protected abstract DiffDividerDrawUtil.DividerPaintable getDividerPaintable(@Nonnull Side side);

    /*
     * Some changes (ex: applied ones) can be excluded from general processing, but should be painted/used for synchronized scrolling
     */
    @Nonnull
    protected List<? extends ThreesideDiffChangeBase> getAllChanges() {
        return getChanges();
    }

    @Nonnull
    protected abstract List<? extends ThreesideDiffChangeBase> getChanges();

    @Nonnull
    @Override
    protected SyncScrollSupport.SyncScrollable getSyncScrollable(@Nonnull Side side) {
        return side.select(mySyncScrollable1, mySyncScrollable2);
    }

    @Nonnull
    @Override
    protected JComponent getStatusPanel() {
        return myStatusPanel;
    }

    @Nonnull
    public SyncScrollSupport.ThreesideSyncScrollSupport getSyncScrollSupport() {
        //noinspection ConstantConditions
        return mySyncScrollSupport;
    }

    //
    // Misc
    //

    @Nullable
    @RequiredUIAccess
    protected ThreesideDiffChangeBase getSelectedChange(@Nonnull ThreeSide side) {
        int caretLine = getEditor(side).getCaretModel().getLogicalPosition().line;

        for (ThreesideDiffChangeBase change : getChanges()) {
            int line1 = change.getStartLine(side);
            int line2 = change.getEndLine(side);

            if (DiffImplUtil.isSelectedByLine(caretLine, line1, line2)) {
                return change;
            }
        }
        return null;
    }

    //
    // Actions
    //

    protected class MyPrevNextDifferenceIterable extends PrevNextDifferenceIterableBase<ThreesideDiffChangeBase> {
        @Nonnull
        @Override
        protected List<? extends ThreesideDiffChangeBase> getChanges() {
            return ThreesideTextDiffViewerEx.this.getChanges();
        }

        @Nonnull
        @Override
        protected EditorEx getEditor() {
            return getCurrentEditor();
        }

        @Override
        protected int getStartLine(@Nonnull ThreesideDiffChangeBase change) {
            return change.getStartLine(getCurrentSide());
        }

        @Override
        protected int getEndLine(@Nonnull ThreesideDiffChangeBase change) {
            return change.getEndLine(getCurrentSide());
        }

        @Override
        protected void scrollToChange(@Nonnull ThreesideDiffChangeBase change) {
            doScrollToChange(change, true);
        }
    }

    protected class MyToggleExpandByDefaultAction extends TextDiffViewerUtil.ToggleExpandByDefaultAction {
        public MyToggleExpandByDefaultAction() {
            super(getTextSettings());
        }

        @Override
        protected void expandAll(boolean expand) {
            myFoldingModel.expandAll(expand);
        }
    }

    //
    // Helpers
    //

    @Nullable
    @Override
    @RequiredUIAccess
    public Object getData(@Nonnull Key<?> dataId) {
        if (DiffDataKeys.PREV_NEXT_DIFFERENCE_ITERABLE == dataId) {
            return myPrevNextDifferenceIterable;
        }
        else if (DiffDataKeys.CURRENT_CHANGE_RANGE == dataId) {
            ThreesideDiffChangeBase change = getSelectedChange(getCurrentSide());
            if (change != null) {
                return new LineRange(change.getStartLine(getCurrentSide()), change.getEndLine(getCurrentSide()));
            }
        }
        return super.getData(dataId);
    }

    protected class MySyncScrollable extends BaseSyncScrollable {
        @Nonnull
        private final Side mySide;

        public MySyncScrollable(@Nonnull Side side) {
            mySide = side;
        }

        @RequiredUIAccess
        @Override
        public boolean isSyncScrollEnabled() {
            return getTextSettings().isEnableSyncScroll();
        }

        @Override
        protected void processHelper(@Nonnull ScrollHelper helper) {
            ThreeSide left = mySide.select(ThreeSide.LEFT, ThreeSide.BASE);
            ThreeSide right = mySide.select(ThreeSide.BASE, ThreeSide.RIGHT);

            if (!helper.process(0, 0)) {
                return;
            }
            for (ThreesideDiffChangeBase diffChange : getAllChanges()) {
                if (!helper.process(diffChange.getStartLine(left), diffChange.getStartLine(right))) {
                    return;
                }
                if (!helper.process(diffChange.getEndLine(left), diffChange.getEndLine(right))) {
                    return;
                }
            }
            helper.process(getEditor(left).getDocument().getLineCount(), getEditor(right).getDocument().getLineCount());
        }
    }

    protected class MyDividerPainter implements DiffSplitter.Painter {
        @Nonnull
        private final Side mySide;
        @Nonnull
        private final DiffDividerDrawUtil.DividerPaintable myPaintable;

        public MyDividerPainter(@Nonnull Side side) {
            mySide = side;
            myPaintable = getDividerPaintable(side);
        }

        @Override
        public void paint(@Nonnull Graphics g, @Nonnull JComponent divider) {
            Graphics2D gg = DiffDividerDrawUtil.getDividerGraphics(g, divider, getEditor(ThreeSide.BASE).getComponent());

            gg.setColor(TargetAWT.to(DiffDrawUtil.getDividerColor(getEditor(ThreeSide.BASE))));
            gg.fill(gg.getClipBounds());

            Editor editor1 = mySide.select(getEditor(ThreeSide.LEFT), getEditor(ThreeSide.BASE));
            Editor editor2 = mySide.select(getEditor(ThreeSide.BASE), getEditor(ThreeSide.RIGHT));

            //DividerPolygonUtil.paintSimplePolygons(gg, divider.getWidth(), editor1, editor2, myPaintable);
            DiffDividerDrawUtil.paintPolygons(gg, divider.getWidth(), editor1, editor2, myPaintable);

            myFoldingModel.paintOnDivider(gg, divider, mySide);

            gg.dispose();
        }
    }

    protected class MyStatusPanel extends StatusPanel {
        @Nullable
        @Override
        protected String getMessage() {
            if (myChangesCount < 0 || myConflictsCount < 0) {
                return null;
            }
            if (myChangesCount == 0 && myConflictsCount == 0) {
                return DiffLocalize.mergeDialogAllConflictsResolvedMessageText().get();
            }
            return makeCounterWord(myChangesCount, "change") + ". " + makeCounterWord(myConflictsCount, "conflict");
        }

        @Nonnull
        private String makeCounterWord(int number, @Nonnull String word) {
            if (number == 0) {
                return "No " + StringUtil.pluralize(word);
            }
            return number + " " + StringUtil.pluralize(word, number);
        }
    }

    protected static class MyFoldingModel extends FoldingModelSupport {
        private final MyPaintable myPaintable1 = new MyPaintable(0, 1);
        private final MyPaintable myPaintable2 = new MyPaintable(1, 2);

        public MyFoldingModel(@Nonnull EditorEx[] editors, @Nonnull Disposable disposable) {
            super(editors, disposable);
            assert editors.length == 3;
        }

        public void install(
            @Nullable List<MergeLineFragment> fragments,
            @Nonnull UserDataHolder context,
            @Nonnull FoldingModelSupport.Settings settings
        ) {
            Iterator<int[]> it = map(fragments, fragment -> new int[]{
                fragment.getStartLine(ThreeSide.LEFT),
                fragment.getEndLine(ThreeSide.LEFT),
                fragment.getStartLine(ThreeSide.BASE),
                fragment.getEndLine(ThreeSide.BASE),
                fragment.getStartLine(ThreeSide.RIGHT),
                fragment.getEndLine(ThreeSide.RIGHT)});
            install(it, context, settings);
        }

        public void paintOnDivider(@Nonnull Graphics2D gg, @Nonnull Component divider, @Nonnull Side side) {
            MyPaintable paintable = side.select(myPaintable1, myPaintable2);
            paintable.paintOnDivider(gg, divider);
        }

        public void paintOnScrollbar(@Nonnull Graphics2D gg, int width) {
            myPaintable2.paintOnScrollbar(gg, width);
        }
    }

    protected class MyInitialScrollHelper extends MyInitialScrollPositionHelper {
        @Override
        @RequiredUIAccess
        protected boolean doScrollToChange() {
            return myScrollToChange != null && ThreesideTextDiffViewerEx.this.doScrollToChange(myScrollToChange);
        }

        @Override
        @RequiredUIAccess
        protected boolean doScrollToFirstChange() {
            return ThreesideTextDiffViewerEx.this.doScrollToChange(ScrollToPolicy.FIRST_CHANGE);
        }
    }
}
