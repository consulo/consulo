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

import consulo.application.AccessRule;
import consulo.application.progress.ProgressIndicator;
import consulo.application.util.function.ThrowableComputable;
import consulo.codeEditor.Editor;
import consulo.component.ProcessCanceledException;
import consulo.desktop.awt.internal.diff.util.DiffDividerDrawUtil;
import consulo.desktop.awt.internal.diff.util.DiffNotifications;
import consulo.desktop.awt.internal.diff.util.TextDiffViewerUtil;
import consulo.desktop.awt.internal.diff.util.side.ThreesideTextDiffViewer;
import consulo.diff.DiffContext;
import consulo.diff.comparison.ComparisonManager;
import consulo.diff.comparison.ComparisonPolicy;
import consulo.diff.comparison.DiffTooBigException;
import consulo.diff.content.DiffContent;
import consulo.diff.content.DocumentContent;
import consulo.diff.fragment.MergeLineFragment;
import consulo.diff.internal.MergeInnerDifferences;
import consulo.diff.internal.DiffImplUtil;
import consulo.diff.internal.HighlightPolicy;
import consulo.diff.internal.IgnorePolicy;
import consulo.diff.request.ContentDiffRequest;
import consulo.diff.request.DiffRequest;
import consulo.diff.util.LineRange;
import consulo.diff.util.MergeConflictType;
import consulo.diff.util.Side;
import consulo.diff.util.ThreeSide;
import consulo.document.Document;
import consulo.document.event.DocumentEvent;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.logging.Logger;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnSeparator;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class SimpleThreesideDiffViewer extends ThreesideTextDiffViewerEx {
    public static final Logger LOG = Logger.getInstance(SimpleThreesideDiffViewer.class);

    @Nonnull
    private final List<SimpleThreesideDiffChange> myDiffChanges = new ArrayList<>();
    @Nonnull
    private final List<SimpleThreesideDiffChange> myInvalidDiffChanges = new ArrayList<>();

    public SimpleThreesideDiffViewer(@Nonnull DiffContext context, @Nonnull DiffRequest request) {
        super(context, (ContentDiffRequest)request);
    }

    @Nonnull
    @Override
    protected List<AnAction> createToolbarActions() {
        List<AnAction> group = new ArrayList<>();

        group.add(new MyIgnorePolicySettingAction());
        group.add(new MyHighlightPolicySettingAction());
        group.add(new MyToggleExpandByDefaultAction());
        group.add(new MyToggleAutoScrollAction());
        group.add(new MyEditorReadOnlyLockAction());
        group.add(myEditorSettingsAction);

        group.add(AnSeparator.getInstance());
        group.add(new TextShowPartialDiffAction(PartialDiffMode.LEFT_BASE));
        group.add(new TextShowPartialDiffAction(PartialDiffMode.BASE_RIGHT));
        group.add(new TextShowPartialDiffAction(PartialDiffMode.LEFT_RIGHT));

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
        //group.add(Separator.getInstance());
        //group.add(new MyHighlightPolicySettingAction().getPopupGroup());
        group.add(AnSeparator.getInstance());
        group.add(new MyToggleAutoScrollAction());
        group.add(new MyToggleExpandByDefaultAction());

        group.add(AnSeparator.getInstance());
        group.addAll(super.createPopupActions());

        return group;
    }

    //
    // Diff
    //

    @Override
    @RequiredUIAccess
    protected void onSlowRediff() {
        super.onSlowRediff();
        myStatusPanel.setBusy(true);
        myInitialScrollHelper.onSlowRediff();
    }

    @Override
    @Nonnull
    protected Runnable performRediff(@Nonnull ProgressIndicator indicator) {
        try {
            indicator.checkCanceled();

            List<DiffContent> contents = myRequest.getContents();
            List<Document> documents = ContainerUtil.map(contents, content -> ((DocumentContent)content).getDocument());

            ThrowableComputable<List<CharSequence>, RuntimeException> action2 = () -> {
                indicator.checkCanceled();
                return ContainerUtil.map(documents, Document::getImmutableCharSequence);
            };
            List<CharSequence> sequences = AccessRule.read(action2);

            ComparisonPolicy comparisonPolicy = getIgnorePolicy().getComparisonPolicy();

            ComparisonManager manager = ComparisonManager.getInstance();
            List<MergeLineFragment> lineFragments = manager.compareLines(sequences.get(0), sequences.get(1), sequences.get(2),
                comparisonPolicy, indicator
            );

            ThrowableComputable<List<MergeConflictType>, RuntimeException> action1 = () -> {
                indicator.checkCanceled();
                return ContainerUtil.map(lineFragments, (fragment) -> DiffImplUtil.getLineMergeType(fragment, documents, comparisonPolicy));
            };
            List<MergeConflictType> conflictTypes = AccessRule.read(action1);

            List<MergeInnerDifferences> innerFragments = null;
            if (getHighlightPolicy().isFineFragments()) {
                innerFragments = new ArrayList<>(lineFragments.size());

                for (int i = 0; i < lineFragments.size(); i++) {
                    MergeLineFragment fragment = lineFragments.get(i);
                    MergeConflictType conflictType = conflictTypes.get(i);

                    ThrowableComputable<List<CharSequence>, RuntimeException> action = () -> {
                        indicator.checkCanceled();
                        return ThreeSide.map(side -> {
                            if (!conflictType.isChange(side)) {
                                return null;
                            }
                            return getChunkContent(fragment, documents, side);
                        });
                    };
                    List<CharSequence> chunks = AccessRule.read(action);

                    innerFragments.add(DiffImplUtil.compareThreesideInner(chunks, comparisonPolicy, indicator));
                }
            }

            return apply(lineFragments, conflictTypes, innerFragments);
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

    @Nullable
    private static CharSequence getChunkContent(
        @Nonnull MergeLineFragment fragment,
        @Nonnull List<Document> documents,
        @Nonnull ThreeSide side
    ) {
        int startLine = fragment.getStartLine(side);
        int endLine = fragment.getEndLine(side);
        return startLine != endLine ? DiffImplUtil.getLinesContent(side.select(documents), startLine, endLine) : null;
    }

    @Nonnull
    private Runnable apply(
        @Nonnull List<MergeLineFragment> fragments,
        @Nonnull List<MergeConflictType> conflictTypes,
        @Nullable List<MergeInnerDifferences> innerDifferences
    ) {
        return () -> {
            myFoldingModel.updateContext(myRequest, getFoldingModelSettings());
            clearDiffPresentation();

            resetChangeCounters();
            for (int i = 0; i < fragments.size(); i++) {
                MergeLineFragment fragment = fragments.get(i);
                MergeConflictType conflictType = conflictTypes.get(i);
                MergeInnerDifferences innerFragments = innerDifferences != null ? innerDifferences.get(i) : null;

                SimpleThreesideDiffChange change = new SimpleThreesideDiffChange(fragment, conflictType, innerFragments, this);
                myDiffChanges.add(change);
                onChangeAdded(change);
            }

            myFoldingModel.install(fragments, myRequest, getFoldingModelSettings());

            myInitialScrollHelper.onRediff();

            myContentPanel.repaintDividers();
            myStatusPanel.update();
        };
    }

    @Override
    @RequiredUIAccess
    protected void destroyChangedBlocks() {
        super.destroyChangedBlocks();
        for (SimpleThreesideDiffChange change : myDiffChanges) {
            change.destroy();
        }
        myDiffChanges.clear();

        for (SimpleThreesideDiffChange change : myInvalidDiffChanges) {
            change.destroy();
        }
        myInvalidDiffChanges.clear();
    }

    //
    // Impl
    //

    @Override
    @RequiredUIAccess
    protected void onBeforeDocumentChange(@Nonnull DocumentEvent e) {
        super.onBeforeDocumentChange(e);
        if (myDiffChanges.isEmpty()) {
            return;
        }

        List<Document> documents = ContainerUtil.map(getEditors(), Editor::getDocument);
        ThreeSide side = ThreeSide.fromValue(documents, e.getDocument());
        if (side == null) {
            LOG.warn("Unknown document changed");
            return;
        }

        LineRange lineRange = DiffImplUtil.getAffectedLineRange(e);
        int shift = DiffImplUtil.countLinesShift(e);

        List<SimpleThreesideDiffChange> invalid = new ArrayList<>();
        for (SimpleThreesideDiffChange change : myDiffChanges) {
            if (change.processChange(lineRange.start, lineRange.end, shift, side)) {
                invalid.add(change);
            }
        }

        if (!invalid.isEmpty()) {
            myDiffChanges.removeAll(invalid);
            myInvalidDiffChanges.addAll(invalid);
        }
    }

    @Nonnull
    private IgnorePolicy getIgnorePolicy() {
        IgnorePolicy policy = getTextSettings().getIgnorePolicy();
        if (policy == IgnorePolicy.IGNORE_WHITESPACES_CHUNKS) {
            return IgnorePolicy.IGNORE_WHITESPACES;
        }
        return policy;
    }

    @Nonnull
    private HighlightPolicy getHighlightPolicy() {
        HighlightPolicy policy = getTextSettings().getHighlightPolicy();
        if (policy == HighlightPolicy.BY_WORD_SPLIT) {
            return HighlightPolicy.BY_WORD;
        }
        if (policy == HighlightPolicy.DO_NOT_HIGHLIGHT) {
            return HighlightPolicy.BY_LINE;
        }
        return policy;
    }

    //
    // Getters
    //

    @Nonnull
    @Override
    public List<SimpleThreesideDiffChange> getChanges() {
        return myDiffChanges;
    }

    @Nonnull
    @Override
    protected DiffDividerDrawUtil.DividerPaintable getDividerPaintable(@Nonnull Side side) {
        return new MyDividerPaintable(side);
    }

    //
    // Misc
    //

    @SuppressWarnings("MethodOverridesStaticMethodOfSuperclass")
    public static boolean canShowRequest(@Nonnull DiffContext context, @Nonnull DiffRequest request) {
        return ThreesideTextDiffViewer.canShowRequest(context, request);
    }

    //
    // Actions
    //

    private class MyIgnorePolicySettingAction extends TextDiffViewerUtil.IgnorePolicySettingAction {
        public MyIgnorePolicySettingAction() {
            super(getTextSettings());
        }

        @Nonnull
        @Override
        protected IgnorePolicy getCurrentSetting() {
            return getIgnorePolicy();
        }

        @Nonnull
        @Override
        protected List<IgnorePolicy> getAvailableSettings() {
            ArrayList<IgnorePolicy> settings = ContainerUtil.newArrayList(IgnorePolicy.values());
            settings.remove(IgnorePolicy.IGNORE_WHITESPACES_CHUNKS);
            return settings;
        }

        @Override
        @RequiredUIAccess
        protected void onSettingsChanged() {
            rediff();
        }
    }

    private class MyHighlightPolicySettingAction extends TextDiffViewerUtil.HighlightPolicySettingAction {
        public MyHighlightPolicySettingAction() {
            super(getTextSettings());
        }

        @Nonnull
        @Override
        protected HighlightPolicy getCurrentSetting() {
            return getHighlightPolicy();
        }

        @Nonnull
        @Override
        protected List<HighlightPolicy> getAvailableSettings() {
            return ContainerUtil.list(HighlightPolicy.BY_LINE, HighlightPolicy.BY_WORD);
        }

        @Override
        @RequiredUIAccess
        protected void onSettingsChanged() {
            rediff();
        }
    }

    protected class MyEditorReadOnlyLockAction extends TextDiffViewerUtil.EditorReadOnlyLockAction {
        public MyEditorReadOnlyLockAction() {
            super(getContext(), getEditableEditors());
        }
    }

    //
    // Helpers
    //

    private class MyDividerPaintable implements DiffDividerDrawUtil.DividerPaintable {
        @Nonnull
        private final Side mySide;

        public MyDividerPaintable(@Nonnull Side side) {
            mySide = side;
        }

        @Override
        public void process(@Nonnull Handler handler) {
            ThreeSide left = mySide.select(ThreeSide.LEFT, ThreeSide.BASE);
            ThreeSide right = mySide.select(ThreeSide.BASE, ThreeSide.RIGHT);

            for (SimpleThreesideDiffChange diffChange : myDiffChanges) {
                if (!diffChange.isChange(mySide)) {
                    continue;
                }
                if (!handler.process(
                    diffChange.getStartLine(left),
                    diffChange.getEndLine(left),
                    diffChange.getStartLine(right),
                    diffChange.getEndLine(right),
                    diffChange.getDiffType().getColor(getEditor(ThreeSide.BASE))
                )) {
                    return;
                }
            }
        }
    }
}
