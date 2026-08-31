/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.versionControlSystem.impl.internal;

import consulo.application.Application;
import consulo.application.dumb.DumbAware;
import consulo.application.internal.BackgroundTaskUtil;
import consulo.application.util.registry.Registry;
import consulo.codeEditor.Editor;
import consulo.codeEditor.ScrollType;
import consulo.codeEditor.markup.RangeHighlighter;
import consulo.diff.comparison.ByWord;
import consulo.diff.comparison.ComparisonPolicy;
import consulo.diff.fragment.DiffFragment;
import consulo.diff.internal.DiffInternal;
import consulo.diff.util.TextDiffType;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.document.Document;
import consulo.language.plain.PlainTextFileType;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.details.InputDetails;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.ToggleAction;
import consulo.ui.ex.action.util.ActionUtil;
import consulo.versionControlSystem.VcsApplicationSettings;
import consulo.versionControlSystem.impl.internal.action.CopyLineStatusRangeAction;
import consulo.versionControlSystem.impl.internal.action.ShowLineStatusRangeDiffAction;
import consulo.versionControlSystem.impl.internal.action.ShowNextChangeMarkerAction;
import consulo.versionControlSystem.impl.internal.action.ShowPrevChangeMarkerAction;
import consulo.versionControlSystem.internal.LineStatusMarkerPopup;
import consulo.versionControlSystem.internal.LineStatusMarkerPopupFactory;
import consulo.versionControlSystem.internal.VcsRange;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;
import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;
import java.util.ArrayList;
import java.util.List;

import static consulo.diff.internal.DiffImplUtil.getDiffType;
import static consulo.diff.internal.DiffImplUtil.getLineCount;

public abstract class LineStatusMarkerPopupBase implements LineStatusMarkerPopup {
    public final LineStatusTracker myTracker;

    public final Editor myEditor;

    public final VcsRange myRange;

    public LineStatusMarkerPopupBase(LineStatusTracker tracker, Editor editor, VcsRange range) {
        myTracker = tracker;
        myEditor = editor;
        myRange = range;
    }

    protected FileType getFileType() {
        VirtualFile file = myTracker.getVirtualFile();
        return file != null ? file.getFileType() : PlainTextFileType.INSTANCE;
    }

    protected boolean isShowInnerDifferences() {
        return VcsApplicationSettings.getInstance().SHOW_LST_WORD_DIFFERENCES;
    }

    @Override
    public void scrollAndShow() {
        if (!myTracker.isValid()) {
            return;
        }
        Document document = myTracker.getDocument();
        int line = Math.min(
            myRange.getType() == VcsRange.DELETED ? myRange.getLine2() : myRange.getLine2() - 1,
            getLineCount(document) - 1
        );
        int lastOffset = document.getLineStartOffset(line);
        myEditor.getCaretModel().moveToOffset(lastOffset);
        myEditor.getScrollingModel().scrollToCaret(ScrollType.CENTER);

        showAfterScroll();
    }

    @Override
    public void showAfterScroll() {
        myEditor.getScrollingModel().runActionOnScrollingFinished(() -> showHintAt(null));
    }

    protected ActionGroup buildActionGroup(@Nullable InputDetails details, Disposable parentDisposable) {
        DefaultActionGroup group = new DefaultActionGroup();

        ShowPrevChangeMarkerAction localShowPrevAction =
            new ShowPrevChangeMarkerAction(myTracker.getPrevRange(myRange), myTracker, myEditor);
        ShowNextChangeMarkerAction localShowNextAction =
            new ShowNextChangeMarkerAction(myTracker.getNextRange(myRange), myTracker, myEditor);
        RollbackLineStatusRangeAction rollback = new RollbackLineStatusRangeAction(myTracker, myRange, myEditor);
        ShowLineStatusRangeDiffAction showDiff = new ShowLineStatusRangeDiffAction(myTracker, myRange, myEditor);
        CopyLineStatusRangeAction copyRange = new CopyLineStatusRangeAction(myTracker, myRange);
        ToggleByWordDiffAction toggleWordDiff = new ToggleByWordDiffAction(myRange, myEditor, myTracker, details);

        group.add(localShowPrevAction);
        group.add(localShowNextAction);
        group.add(rollback);
        group.add(showDiff);
        group.add(copyRange);
        group.add(toggleWordDiff);

        JComponent editorComponent = myEditor.getComponent();
        registerAction(localShowPrevAction, editorComponent);
        registerAction(localShowNextAction, editorComponent);
        registerAction(rollback, editorComponent);
        registerAction(showDiff, editorComponent);
        registerAction(copyRange, editorComponent);

        List<AnAction> actionList = ActionUtil.getActions(editorComponent);
        Disposer.register(parentDisposable, () -> {
            actionList.remove(localShowPrevAction);
            actionList.remove(localShowNextAction);
            actionList.remove(rollback);
            actionList.remove(showDiff);
            actionList.remove(copyRange);
        });

        return group;
    }

    private static void registerAction(AnAction action, JComponent component) {
        action.registerCustomShortcutSet(action.getShortcutSet(), component);
    }

    @RequiredUIAccess
    protected @Nullable List<DiffFragment> computeWordDiff() {
        if (!isShowInnerDifferences()) {
            return null;
        }
        if (myRange.getType() != VcsRange.MODIFIED) {
            return null;
        }

        CharSequence vcsContent = myTracker.getVcsContent(myRange);
        CharSequence currentContent = myTracker.getCurrentContent(myRange);

        return BackgroundTaskUtil.tryComputeFast(
            indicator -> ByWord.compare(vcsContent, currentContent, ComparisonPolicy.DEFAULT, indicator),
            Registry.intValue("diff.status.tracker.byword.delay")
        );
    }

    protected void installMasterEditorHighlighters(@Nullable List<DiffFragment> wordDiff, Disposable parentDisposable) {
        if (wordDiff == null) {
            return;
        }
        List<RangeHighlighter> highlighters = new ArrayList<>();

        DiffInternal diffInternal = Application.get().getInstance(DiffInternal.class);

        int currentStartShift = myTracker.getCurrentTextRange(myRange).getStartOffset();
        for (DiffFragment fragment : wordDiff) {
            int currentStart = currentStartShift + fragment.getStartOffset2();
            int currentEnd = currentStartShift + fragment.getEndOffset2();
            TextDiffType type = getDiffType(fragment);

            highlighters.addAll(diffInternal.createInlineHighlighter(myEditor, currentStart, currentEnd, type));
        }

        Disposer.register(
            parentDisposable,
            () -> {
                for (RangeHighlighter highlighter : highlighters) {
                    highlighter.dispose();
                }
            }
        );
    }

    private static class ToggleByWordDiffAction extends ToggleAction implements DumbAware {
        private final VcsRange myRange;

        private final Editor myEditor;

        private final LineStatusTracker myTracker;

        private final @Nullable InputDetails myDetails;

        public ToggleByWordDiffAction(
            VcsRange range,
            Editor editor,
            LineStatusTracker tracker,
            @Nullable InputDetails details
        ) {
            super("Highlight Words", null, PlatformIconGroup.generalHighlighting());
            myRange = range;
            myEditor = editor;
            myTracker = tracker;
            myDetails = details;
        }

        @Override
        public boolean isSelected(AnActionEvent e) {
            return VcsApplicationSettings.getInstance().SHOW_LST_WORD_DIFFERENCES;
        }

        @Override
        public void setSelected(AnActionEvent e, boolean state) {
            VcsApplicationSettings.getInstance().SHOW_LST_WORD_DIFFERENCES = state;
            LineStatusMarkerPopupFactory.getInstance().create(myTracker, myEditor, myRange).showHintAt(myDetails);
        }
    }
}
