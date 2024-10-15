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
package consulo.desktop.awt.internal.diff.merge;

import consulo.annotation.access.RequiredWriteAction;
import consulo.application.AccessRule;
import consulo.application.AllIcons;
import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.application.dumb.DumbAware;
import consulo.application.progress.EmptyProgressIndicator;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.application.progress.Task;
import consulo.application.util.LineTokenizer;
import consulo.application.util.function.ThrowableComputable;
import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorEx;
import consulo.component.ProcessCanceledException;
import consulo.desktop.awt.internal.diff.simple.ThreesideTextDiffViewerEx;
import consulo.desktop.awt.internal.diff.util.*;
import consulo.desktop.awt.internal.diff.util.side.ThreesideTextDiffViewer;
import consulo.diff.DiffContext;
import consulo.diff.DiffUserDataKeys;
import consulo.diff.FrameDiffTool;
import consulo.diff.comparison.ComparisonManager;
import consulo.diff.comparison.ComparisonMergeUtil;
import consulo.diff.comparison.ComparisonPolicy;
import consulo.diff.comparison.DiffTooBigException;
import consulo.diff.content.DiffContent;
import consulo.diff.content.DocumentContent;
import consulo.diff.fragment.MergeLineFragment;
import consulo.diff.impl.internal.action.ProxyUndoRedoAction;
import consulo.diff.impl.internal.merge.MergeInnerDifferences;
import consulo.diff.impl.internal.util.DiffImplUtil;
import consulo.diff.impl.internal.util.HighlightPolicy;
import consulo.diff.localize.DiffLocalize;
import consulo.diff.merge.MergeContext;
import consulo.diff.merge.MergeResult;
import consulo.diff.merge.MergeTool;
import consulo.diff.merge.TextMergeRequest;
import consulo.diff.request.ContentDiffRequest;
import consulo.diff.request.SimpleDiffRequest;
import consulo.diff.util.*;
import consulo.disposer.Disposer;
import consulo.document.Document;
import consulo.document.event.DocumentEvent;
import consulo.ide.impl.idea.openapi.actionSystem.ex.ActionUtil;
import consulo.ide.impl.idea.openapi.progress.util.ProgressWindow;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.util.Alarm;
import consulo.undoRedo.UndoConfirmationPolicy;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.primitive.ints.IntList;
import consulo.util.collection.primitive.ints.IntLists;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.*;

public class TextMergeViewer implements MergeTool.MergeViewer {
    private static final Logger LOG = Logger.getInstance(TextMergeViewer.class);

    @Nonnull
    private final MergeContext myMergeContext;
    @Nonnull
    private final TextMergeRequest myMergeRequest;

    @Nonnull
    private final MyThreesideViewer myViewer;

    public TextMergeViewer(@Nonnull MergeContext context, @Nonnull TextMergeRequest request) {
        myMergeContext = context;
        myMergeRequest = request;

        DiffContext diffContext = new MergeImplUtil.ProxyDiffContext(myMergeContext);
        ContentDiffRequest diffRequest =
            new SimpleDiffRequest(myMergeRequest.getTitle(), getDiffContents(myMergeRequest), getDiffContentTitles(myMergeRequest));
        diffRequest.putUserData(DiffUserDataKeys.FORCE_READ_ONLY_CONTENTS, new boolean[]{true, false, true});

        myViewer = new MyThreesideViewer(diffContext, diffRequest);
    }

    @Nonnull
    private static List<DiffContent> getDiffContents(@Nonnull TextMergeRequest mergeRequest) {
        List<DocumentContent> contents = mergeRequest.getContents();

        final DocumentContent left = ThreeSide.LEFT.select(contents);
        final DocumentContent right = ThreeSide.RIGHT.select(contents);
        final DocumentContent output = mergeRequest.getOutputContent();

        return ContainerUtil.<DiffContent>list(left, output, right);
    }

    @Nonnull
    private static List<String> getDiffContentTitles(@Nonnull TextMergeRequest mergeRequest) {
        List<String> titles = MergeImplUtil.notNullizeContentTitles(mergeRequest.getContentTitles());
        titles.set(ThreeSide.BASE.getIndex(), "Result");
        return titles;
    }

    //
    // Impl
    //

    @Nonnull
    @Override
    public JComponent getComponent() {
        return myViewer.getComponent();
    }

    @Nullable
    @Override
    public JComponent getPreferredFocusedComponent() {
        return myViewer.getPreferredFocusedComponent();
    }

    @Nonnull
    @Override
    public MergeTool.ToolbarComponents init() {
        MergeTool.ToolbarComponents components = new MergeTool.ToolbarComponents();

        FrameDiffTool.ToolbarComponents init = myViewer.init();
        components.statusPanel = init.statusPanel;
        components.toolbarActions = init.toolbarActions;

        components.closeHandler = () -> MergeImplUtil.showExitWithoutApplyingChangesDialog(this, myMergeRequest, myMergeContext);

        return components;
    }

    @Nullable
    @Override
    public Action getResolveAction(@Nonnull MergeResult result) {
        return myViewer.getResolveAction(result);
    }

    @Override
    public void dispose() {
        Disposer.dispose(myViewer);
    }

    //
    // Getters
    //

    @Nonnull
    public MyThreesideViewer getViewer() {
        return myViewer;
    }

    //
    // Viewer
    //

    public class MyThreesideViewer extends ThreesideTextDiffViewerEx {
        @Nonnull
        private final MergeModelBase myModel;

        @Nonnull
        private final ModifierProvider myModifierProvider;
        @Nonnull
        private final MyInnerDiffWorker myInnerDiffWorker;

        // all changes - both applied and unapplied ones
        @Nonnull
        private final List<TextMergeChange> myAllMergeChanges = new ArrayList<>();

        private boolean myInitialRediffStarted;
        private boolean myInitialRediffFinished;
        private boolean myContentModified;

        public MyThreesideViewer(@Nonnull DiffContext context, @Nonnull ContentDiffRequest request) {
            super(context, request);

            myModel = new MyMergeModel(getProject(), getEditor().getDocument());

            myModifierProvider = new ModifierProvider();
            myInnerDiffWorker = new MyInnerDiffWorker();

            AWTDiffUtil.registerAction(new ApplySelectedChangesAction(Side.LEFT, true), myPanel);
            AWTDiffUtil.registerAction(new ApplySelectedChangesAction(Side.RIGHT, true), myPanel);
            AWTDiffUtil.registerAction(new IgnoreSelectedChangesSideAction(Side.LEFT, true), myPanel);
            AWTDiffUtil.registerAction(new IgnoreSelectedChangesSideAction(Side.RIGHT, true), myPanel);

            ProxyUndoRedoAction.register(getProject(), getEditor(), myContentPanel);
        }

        @RequiredUIAccess
        @Override
        protected void onInit() {
            super.onInit();
            myModifierProvider.init();
        }

        @RequiredUIAccess
        @Override
        protected void onDispose() {
            Disposer.dispose(myModel);
            super.onDispose();
        }

        @Nonnull
        @Override
        protected List<AnAction> createToolbarActions() {
            List<AnAction> group = new ArrayList<>();

            group.add(new MyHighlightPolicySettingAction());
            group.add(new MyToggleAutoScrollAction());
            group.add(myEditorSettingsAction);

            group.add(AnSeparator.getInstance());
            group.add(new TextShowPartialDiffAction(PartialDiffMode.LEFT_BASE));
            group.add(new ThreesideTextDiffViewer.TextShowPartialDiffAction(ThreesideDiffViewer.PartialDiffMode.BASE_RIGHT));
            group.add(new ThreesideTextDiffViewer.TextShowPartialDiffAction(ThreesideDiffViewer.PartialDiffMode.LEFT_RIGHT));

            group.add(AnSeparator.getInstance());
            group.add(new ApplyNonConflictsAction(ThreeSide.BASE));
            group.add(new ApplyNonConflictsAction(ThreeSide.LEFT));
            group.add(new ApplyNonConflictsAction(ThreeSide.RIGHT));

            return group;
        }

        @Nonnull
        @Override
        protected List<AnAction> createEditorPopupActions() {
            List<AnAction> group = new ArrayList<>();

            group.add(new ApplySelectedChangesAction(Side.LEFT, false));
            group.add(new ApplySelectedChangesAction(Side.RIGHT, false));
            group.add(new ResolveSelectedChangesAction(Side.LEFT));
            group.add(new ResolveSelectedChangesAction(Side.RIGHT));
            group.add(new IgnoreSelectedChangesSideAction(Side.LEFT, false));
            group.add(new IgnoreSelectedChangesSideAction(Side.RIGHT, false));
            group.add(new IgnoreSelectedChangesAction());

            group.add(AnSeparator.getInstance());
            group.addAll(TextDiffViewerUtil.createEditorPopupActions());

            return group;
        }

        @Nullable
        @Override
        protected List<AnAction> createPopupActions() {
            List<AnAction> group = new ArrayList<>();

            group.add(AnSeparator.getInstance());
            group.add(new MyToggleAutoScrollAction());

            return group;
        }

        @Nullable
        public Action getResolveAction(@Nonnull final MergeResult result) {
            String caption = MergeImplUtil.getResolveActionTitle(result, myMergeRequest, myMergeContext);
            return new AbstractAction(caption) {
                @RequiredUIAccess
                @Override
                public void actionPerformed(ActionEvent e) {
                    if ((result == MergeResult.LEFT || result == MergeResult.RIGHT) && myContentModified &&
                        Messages.showYesNoDialog(
                            myPanel.getRootPane(),
                            DiffLocalize.mergeDialogResolveSideWithDiscardMessage(result == MergeResult.LEFT ? 0 : 1).get(),
                            DiffLocalize.mergeDialogResolveSideWithDiscardTitle().get(),
                            UIUtil.getQuestionIcon()
                        ) != Messages.YES) {
                        return;
                    }
                    if (result == MergeResult.RESOLVED) {
                        if ((getChangesCount() != 0 || getConflictsCount() != 0)
                            && Messages.showYesNoDialog(
                            myPanel.getRootPane(),
                            DiffLocalize.mergeDialogApplyPartiallyResolvedChangesConfirmationMessage(
                                getChangesCount(),
                                getConflictsCount()
                            ).get(),
                            DiffLocalize.applyPartiallyResolvedMergeDialogTitle().get(),
                            UIUtil.getQuestionIcon()
                        ) != Messages.YES) {
                            return;
                        }
                    }
                    if (result == MergeResult.CANCEL &&
                        !MergeImplUtil.showExitWithoutApplyingChangesDialog(TextMergeViewer.this, myMergeRequest, myMergeContext)) {
                        return;
                    }
                    destroyChangedBlocks();
                    myMergeContext.finishMerge(result);
                }
            };
        }

        //
        // Diff
        //

        @RequiredUIAccess
        private void setInitialOutputContent() {
            final Document baseDocument = ThreeSide.BASE.select(myMergeRequest.getContents()).getDocument();
            final Document outputDocument = myMergeRequest.getOutputContent().getDocument();

            DiffImplUtil.executeWriteCommand(outputDocument, getProject(), "Init merge content", () -> {
                outputDocument.setText(baseDocument.getCharsSequence());
                DiffImplUtil.putNonundoableOperation(getProject(), outputDocument);
            });
        }

        @Override
        @RequiredUIAccess
        public void rediff(boolean trySync) {
            if (myInitialRediffStarted) {
                return;
            }
            myInitialRediffStarted = true;
            assert myAllMergeChanges.isEmpty();
            doRediff();
        }

        @Nonnull
        @Override
        protected Runnable performRediff(@Nonnull ProgressIndicator indicator) {
            throw new UnsupportedOperationException();
        }

        @RequiredUIAccess
        private void doRediff() {
            myStatusPanel.setBusy(true);

            // This is made to reduce unwanted modifications before rediff is finished.
            // It could happen between this init() EDT chunk and invokeLater().
            getEditor().setViewer(true);

            // we need invokeLater() here because viewer is partially-initialized (ex: there are no toolbar or status panel)
            // user can see this state while we're showing progress indicator, so we want let init() to finish.
            ApplicationManager.getApplication().invokeLater(
                () -> ProgressManager.getInstance().run(new Task.Modal(getProject(), "Computing Differences...", true) {
                    private Runnable myCallback;

                    @Override
                    public void run(@Nonnull ProgressIndicator indicator) {
                        myCallback = doPerformRediff(indicator);
                    }

                    @Override
                    @RequiredUIAccess
                    public void onCancel() {
                        myMergeContext.finishMerge(MergeResult.CANCEL);
                    }

                    @Override
                    public void onError(@Nonnull Exception error) {
                        myMergeContext.finishMerge(MergeResult.CANCEL);
                    }

                    @Override
                    public void onSuccess() {
                        if (isDisposed()) {
                            return;
                        }
                        myCallback.run();
                    }
                })
            );
        }

        @RequiredUIAccess
        @Nonnull
        protected Runnable doPerformRediff(@Nonnull ProgressIndicator indicator) {
            try {
                indicator.checkCanceled();

                List<DocumentContent> contents = myMergeRequest.getContents();
                List<Document> documents = ContainerUtil.map(contents, DocumentContent::getDocument);
                ThrowableComputable<List<CharSequence>, RuntimeException> action1 =
                    () -> ContainerUtil.map(documents, Document::getImmutableCharSequence);
                List<CharSequence> sequences = AccessRule.read(action1);

                ComparisonManager manager = ComparisonManager.getInstance();
                List<MergeLineFragment> lineFragments = manager.compareLines(
                    sequences.get(0),
                    sequences.get(1),
                    sequences.get(2),
                    ComparisonPolicy.DEFAULT,
                    indicator
                );

                ThrowableComputable<List<MergeConflictType>, RuntimeException> action = () -> {
                    indicator.checkCanceled();
                    return ContainerUtil.map(
                        lineFragments,
                        (fragment -> DiffImplUtil.getLineMergeType(fragment, documents, ComparisonPolicy.DEFAULT))
                    );
                };
                List<MergeConflictType> conflictTypes = AccessRule.read(action);

                return apply(lineFragments, conflictTypes);
            }
            catch (DiffTooBigException e) {
                return applyNotification(DiffNotifications.createDiffTooBig());
            }
            catch (ProcessCanceledException e) {
                throw e;
            }
            catch (Throwable e) {
                LOG.error(e);
                return () -> {
                    clearDiffPresentation();
                    myPanel.setErrorContent();
                };
            }
        }

        @Nonnull
        private Runnable apply(
            @Nonnull final List<MergeLineFragment> fragments,
            @Nonnull final List<MergeConflictType> conflictTypes
        ) {
            return () -> {
                //noinspection RequiredXAction
                setInitialOutputContent();

                clearDiffPresentation();
                resetChangeCounters();

                myModel.setChanges(ContainerUtil.map(
                    fragments,
                    f -> new LineRange(f.getStartLine(ThreeSide.BASE), f.getEndLine(ThreeSide.BASE))
                ));

                for (int index = 0; index < fragments.size(); index++) {
                    MergeLineFragment fragment = fragments.get(index);
                    MergeConflictType conflictType = conflictTypes.get(index);

                    @SuppressWarnings("RequiredXAction")
                    TextMergeChange change = new TextMergeChange(index, fragment, conflictType, TextMergeViewer.this);
                    myAllMergeChanges.add(change);
                    onChangeAdded(change);
                }

                myInitialScrollHelper.onRediff();

                myContentPanel.repaintDividers();
                myStatusPanel.update();

                getEditor().setViewer(false);

                //noinspection RequiredXAction
                myInnerDiffWorker.onSettingsChanged();
                myInitialRediffFinished = true;

                if (myViewer.getTextSettings().isAutoApplyNonConflictedChanges()
                    && getFirstUnresolvedChange(false, ThreeSide.BASE) != null) {
                    applyNonConflictedChanges(ThreeSide.BASE);
                }
            };
        }

        @Override
        @RequiredUIAccess
        protected void destroyChangedBlocks() {
            super.destroyChangedBlocks();
            myInnerDiffWorker.stop();

            for (TextMergeChange change : myAllMergeChanges) {
                change.destroy();
            }
            myAllMergeChanges.clear();

            myModel.setChanges(Collections.emptyList());
        }

        //
        // By-word diff
        //

        private class MyInnerDiffWorker {
            @Nonnull
            private final Set<TextMergeChange> myScheduled = new HashSet<>();

            @Nonnull
            private final Alarm myAlarm = new Alarm(MyThreesideViewer.this);
            @Nullable
            private ProgressIndicator myProgress;

            private boolean myEnabled = false;

            @RequiredUIAccess
            public void scheduleRediff(@Nonnull TextMergeChange change) {
                scheduleRediff(Collections.singletonList(change));
            }

            @RequiredUIAccess
            public void scheduleRediff(@Nonnull Collection<TextMergeChange> changes) {
                if (!myEnabled) {
                    return;
                }

                putChanges(changes);
                schedule();
            }

            @RequiredUIAccess
            public void onSettingsChanged() {
                boolean enabled = getHighlightPolicy() == HighlightPolicy.BY_WORD;
                if (myEnabled == enabled) {
                    return;
                }
                myEnabled = enabled;

                if (myProgress != null) {
                    myProgress.cancel();
                }
                myProgress = null;

                if (myEnabled) {
                    putChanges(myAllMergeChanges);
                    launchRediff();
                }
                else {
                    myStatusPanel.setBusy(false);
                    myScheduled.clear();
                    for (TextMergeChange change : myAllMergeChanges) {
                        change.setInnerFragments(null);
                    }
                }
            }

            @RequiredUIAccess
            public void stop() {
                if (myProgress != null) {
                    myProgress.cancel();
                }
                myProgress = null;
                myScheduled.clear();
                myAlarm.cancelAllRequests();
            }

            @RequiredUIAccess
            private void putChanges(@Nonnull Collection<TextMergeChange> changes) {
                for (TextMergeChange change : changes) {
                    if (change.isResolved()) {
                        continue;
                    }
                    myScheduled.add(change);
                }
            }

            @RequiredUIAccess
            private void schedule() {
                if (myProgress != null) {
                    return;
                }
                if (myScheduled.isEmpty()) {
                    return;
                }

                myAlarm.cancelAllRequests();
                //noinspection RequiredXAction
                myAlarm.addRequest(this::launchRediff, ProgressWindow.DEFAULT_PROGRESS_DIALOG_POSTPONE_TIME_MILLIS);
            }

            @RequiredUIAccess
            private void launchRediff() {
                myStatusPanel.setBusy(true);
                myProgress = new EmptyProgressIndicator();

                final List<TextMergeChange> scheduled = ContainerUtil.newArrayList(myScheduled);
                myScheduled.clear();

                List<Document> documents = ThreeSide.map((side) -> getEditor(side).getDocument());
                final List<InnerChunkData> data = ContainerUtil.map(scheduled, change -> new InnerChunkData(change, documents));

                final ProgressIndicator indicator = myProgress;
                Application.get().executeOnPooledThread(() -> performRediff(scheduled, data, indicator));
            }

            private void performRediff(
                @Nonnull final List<TextMergeChange> scheduled,
                @Nonnull final List<InnerChunkData> data,
                @Nonnull final ProgressIndicator indicator
            ) {
                final List<MergeInnerDifferences> result = new ArrayList<>(data.size());
                for (InnerChunkData chunkData : data) {
                    result.add(DiffImplUtil.compareThreesideInner(chunkData.text, ComparisonPolicy.DEFAULT, indicator));
                }

                Application.get().invokeLater(
                    () -> {
                        if (!myEnabled || indicator.isCanceled()) {
                            return;
                        }
                        myProgress = null;

                        for (int i = 0; i < scheduled.size(); i++) {
                            TextMergeChange change = scheduled.get(i);
                            if (myScheduled.contains(change)) {
                                continue;
                            }
                            change.setInnerFragments(result.get(i));
                        }

                        myStatusPanel.setBusy(false);
                        if (!myScheduled.isEmpty()) {
                            launchRediff();
                        }
                    },
                    Application.get().getAnyModalityState()
                );
            }
        }

        //
        // Impl
        //

        @Override
        @RequiredUIAccess
        protected void onBeforeDocumentChange(@Nonnull DocumentEvent e) {
            super.onBeforeDocumentChange(e);
            if (myInitialRediffFinished) {
                myContentModified = true;
            }
        }

        public void repaintDividers() {
            myContentPanel.repaintDividers();
        }

        private void onChangeResolved(@Nonnull TextMergeChange change) {
            if (change.isResolved()) {
                onChangeRemoved(change);
            }
            else {
                onChangeAdded(change);
            }
            if (getChangesCount() == 0 && getConflictsCount() == 0) {
                LOG.assertTrue(getFirstUnresolvedChange(true, ThreeSide.BASE) == null);
                Application.get().invokeLater(() -> {
                    if (isDisposed()) {
                        return;
                    }

                    JComponent component = getEditor().getComponent();
                    RelativePoint point = new RelativePoint(component, new Point(component.getWidth() / 2, JBUI.scale(5)));

                    LocalizeValue message = DiffLocalize.mergeAllChangesProcessedMessageText();
                    AWTDiffUtil.showSuccessPopup(message.get(), point, this, () -> {
                        if (isDisposed()) {
                            return;
                        }
                        //noinspection RequiredXAction
                        destroyChangedBlocks();
                        //noinspection RequiredXAction
                        myMergeContext.finishMerge(MergeResult.RESOLVED);
                    });
                });
            }
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
        public MergeModelBase getModel() {
            return myModel;
        }

        @Nonnull
        @Override
        public List<TextMergeChange> getAllChanges() {
            return myAllMergeChanges;
        }

        @Nonnull
        @Override
        public List<TextMergeChange> getChanges() {
            return ContainerUtil.filter(myAllMergeChanges, mergeChange -> !mergeChange.isResolved());
        }

        @Nonnull
        @Override
        protected DiffDividerDrawUtil.DividerPaintable getDividerPaintable(@Nonnull Side side) {
            return new MyDividerPaintable(side);
        }

        @Nonnull
        public ModifierProvider getModifierProvider() {
            return myModifierProvider;
        }

        @Nonnull
        public EditorEx getEditor() {
            return getEditor(ThreeSide.BASE);
        }

        //
        // Modification operations
        //

        /*
         * affected changes should be sorted
         */
        public void executeMergeCommand(
            @Nullable String commandName,
            boolean underBulkUpdate,
            @Nullable List<TextMergeChange> affected,
            @Nonnull Runnable task
        ) {
            myContentModified = true;

            IntList affectedIndexes = null;
            if (affected != null) {
                affectedIndexes = IntLists.newArrayList(affected.size());
                for (TextMergeChange change : affected) {
                    affectedIndexes.add(change.getIndex());
                }
            }

            myModel.executeMergeCommand(commandName, null, UndoConfirmationPolicy.DEFAULT, underBulkUpdate, affectedIndexes, task);
        }

        public void executeMergeCommand(
            @Nullable String commandName,
            @Nullable List<TextMergeChange> affected,
            @Nonnull Runnable task
        ) {
            executeMergeCommand(commandName, false, affected, task);
        }

        @RequiredUIAccess
        public void markChangeResolved(@Nonnull TextMergeChange change) {
            if (change.isResolved()) {
                return;
            }
            change.setResolved(Side.LEFT, true);
            change.setResolved(Side.RIGHT, true);

            onChangeResolved(change);
            myModel.invalidateHighlighters(change.getIndex());
        }

        @RequiredUIAccess
        public void markChangeResolved(@Nonnull TextMergeChange change, @Nonnull Side side) {
            if (change.isResolved(side)) {
                return;
            }
            change.setResolved(side, true);

            if (change.isResolved()) {
                onChangeResolved(change);
            }
            myModel.invalidateHighlighters(change.getIndex());
        }

        @RequiredUIAccess
        public void ignoreChange(@Nonnull TextMergeChange change, @Nonnull Side side, boolean resolveChange) {
            if (!change.isConflict() || resolveChange) {
                markChangeResolved(change);
            }
            else {
                markChangeResolved(change, side);
            }
        }

        @RequiredWriteAction
        public void replaceChange(@Nonnull TextMergeChange change, @Nonnull Side side, boolean resolveChange) {
            if (change.isResolved(side)) {
                return;
            }
            if (!change.isChange(side)) {
                markChangeResolved(change);
                return;
            }

            ThreeSide sourceSide = side.select(ThreeSide.LEFT, ThreeSide.RIGHT);
            ThreeSide oppositeSide = side.select(ThreeSide.RIGHT, ThreeSide.LEFT);

            Document sourceDocument = getContent(sourceSide).getDocument();
            int sourceStartLine = change.getStartLine(sourceSide);
            int sourceEndLine = change.getEndLine(sourceSide);
            List<String> newContent = DiffImplUtil.getLines(sourceDocument, sourceStartLine, sourceEndLine);

            if (change.isConflict()) {
                boolean append = change.isOnesideAppliedConflict();
                if (append) {
                    myModel.appendChange(change.getIndex(), newContent);
                }
                else {
                    myModel.replaceChange(change.getIndex(), newContent);
                }

                if (resolveChange || change.getStartLine(oppositeSide) == change.getEndLine(oppositeSide)) {
                    markChangeResolved(change);
                }
                else {
                    change.markOnesideAppliedConflict();
                    markChangeResolved(change, side);
                }
            }
            else {
                myModel.replaceChange(change.getIndex(), newContent);

                markChangeResolved(change);
            }
        }

        @Nullable
        public CharSequence resolveConflictUsingInnerDifferences(@Nonnull TextMergeChange change) {
            if (!change.isConflict()) {
                return null;
            }
            if (change.isResolved(Side.LEFT) || change.isResolved(Side.RIGHT)) {
                return null;
            }

            MergeLineFragment changeFragment = change.getFragment();
            if (changeFragment.getStartLine(ThreeSide.LEFT) == changeFragment.getEndLine(ThreeSide.LEFT)) {
                return null;
            }
            if (changeFragment.getStartLine(ThreeSide.BASE) == changeFragment.getEndLine(ThreeSide.BASE)) {
                return null;
            }
            if (changeFragment.getStartLine(ThreeSide.RIGHT) == changeFragment.getEndLine(ThreeSide.RIGHT)) {
                return null;
            }

            int baseStartLine = changeFragment.getStartLine(ThreeSide.BASE);
            int baseEndLine = changeFragment.getEndLine(ThreeSide.BASE);
            DiffContent baseDiffContent = ThreeSide.BASE.select(myMergeRequest.getContents());
            Document baseDocument = ((DocumentContent)baseDiffContent).getDocument();

            int resultStartLine = change.getStartLine();
            int resultEndLine = change.getEndLine();
            Document resultDocument = getEditor().getDocument();

            CharSequence baseContent = DiffImplUtil.getLinesContent(baseDocument, baseStartLine, baseEndLine);
            CharSequence resultContent = DiffImplUtil.getLinesContent(resultDocument, resultStartLine, resultEndLine);
            if (!StringUtil.equals(baseContent, resultContent)) {
                return null;
            }

            List<CharSequence> texts = ThreeSide.map(side -> DiffImplUtil.getLinesContent(
                getEditor(side).getDocument(),
                change.getStartLine(side),
                change.getEndLine(side)
            ));

            return ComparisonMergeUtil.tryResolveConflict(texts.get(0), texts.get(1), texts.get(2));
        }

        @RequiredUIAccess
        public void resolveConflictedChange(@Nonnull TextMergeChange change) {
            CharSequence newContent = resolveConflictUsingInnerDifferences(change);
            if (newContent == null) {
                return;
            }

            String[] newContentLines = LineTokenizer.tokenize(newContent, false);
            myModel.replaceChange(change.getIndex(), Arrays.asList(newContentLines));
            markChangeResolved(change);
        }

        private class MyMergeModel extends MergeModelBase<TextMergeChange.State> {
            public MyMergeModel(@Nullable Project project, @Nonnull Document document) {
                super(project, document);
            }

            @RequiredUIAccess
            @Override
            protected void reinstallHighlighters(int index) {
                TextMergeChange change = myAllMergeChanges.get(index);
                change.reinstallHighlighters();
                myInnerDiffWorker.scheduleRediff(change);
            }

            @Nonnull
            @Override
            protected TextMergeChange.State storeChangeState(int index) {
                TextMergeChange change = myAllMergeChanges.get(index);
                return change.storeState();
            }

            @Override
            @RequiredUIAccess
            protected void restoreChangeState(@Nonnull TextMergeChange.State state) {
                super.restoreChangeState(state);
                TextMergeChange change = myAllMergeChanges.get(state.myIndex);

                boolean wasResolved = change.isResolved();
                change.restoreState(state);
                if (wasResolved != change.isResolved()) {
                    onChangeResolved(change);
                }
            }

            @Nullable
            @Override
            @RequiredUIAccess
            protected TextMergeChange.State processDocumentChange(int index, int oldLine1, int oldLine2, int shift) {
                TextMergeChange.State state = super.processDocumentChange(index, oldLine1, oldLine2, shift);

                TextMergeChange mergeChange = myAllMergeChanges.get(index);
                if (mergeChange.getStartLine() == mergeChange.getEndLine() &&
                    mergeChange.getDiffType() == TextDiffType.DELETED && !mergeChange.isResolved()) {
                    myViewer.markChangeResolved(mergeChange);
                }

                return state;
            }
        }

        //
        // Actions
        //

        @Nullable
        private TextMergeChange getFirstUnresolvedChange(boolean acceptConflicts, @Nonnull ThreeSide side) {
            for (TextMergeChange change : getAllChanges()) {
                if (change.isResolved()) {
                    continue;
                }
                if (!acceptConflicts && change.isConflict()) {
                    continue;
                }
                if (!change.isChange(side)) {
                    continue;
                }
                return change;
            }
            return null;
        }

        private void applyNonConflictedChanges(@Nonnull ThreeSide side) {
            executeMergeCommand("Apply Non Conflicted Changes", true, null, () -> {
                List<TextMergeChange> allChanges = ContainerUtil.newArrayList(getAllChanges());
                for (TextMergeChange change : allChanges) {
                    if (change.isConflict()) {
                        continue;
                    }
                    if (change.isResolved(side)) {
                        continue;
                    }
                    if (!change.isChange(side)) {
                        continue;
                    }
                    Side masterSide = side.select(Side.LEFT, change.isChange(Side.LEFT) ? Side.LEFT : Side.RIGHT, Side.RIGHT);
                    replaceChange(change, masterSide, false);
                }
            });

            TextMergeChange firstConflict = getFirstUnresolvedChange(true, ThreeSide.BASE);
            if (firstConflict != null) {
                doScrollToChange(firstConflict, true);
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
                myInnerDiffWorker.onSettingsChanged();
            }
        }

        private abstract class ApplySelectedChangesActionBase extends AnAction implements DumbAware {
            private final boolean myShortcut;

            public ApplySelectedChangesActionBase(boolean shortcut) {
                myShortcut = shortcut;
            }

            @Override
            public void update(@Nonnull AnActionEvent e) {
                if (myShortcut) {
                    // consume shortcut even if there are nothing to do - avoid calling some other action
                    e.getPresentation().setEnabledAndVisible(true);
                    return;
                }

                Presentation presentation = e.getPresentation();
                Editor editor = e.getData(Editor.KEY);

                ThreeSide side = getEditorSide(editor);
                if (side == null) {
                    presentation.setEnabledAndVisible(false);
                    return;
                }

                if (!isVisible(side)) {
                    presentation.setEnabledAndVisible(false);
                    return;
                }

                presentation.setText(getText(side));

                presentation.setVisible(true);
                presentation.setEnabled(isSomeChangeSelected(side));
            }

            @Override
            @RequiredUIAccess
            public void actionPerformed(@Nonnull final AnActionEvent e) {
                Editor editor = e.getData(Editor.KEY);
                final ThreeSide side = getEditorSide(editor);
                if (editor == null || side == null) {
                    return;
                }

                final List<TextMergeChange> selectedChanges = getSelectedChanges(side);
                if (selectedChanges.isEmpty()) {
                    return;
                }

                String title = e.getPresentation().getText() + " in merge";

                executeMergeCommand(title, selectedChanges.size() > 1, selectedChanges, () -> apply(side, selectedChanges));
            }

            private boolean isSomeChangeSelected(@Nonnull ThreeSide side) {
                EditorEx editor = getEditor(side);
                List<Caret> carets = editor.getCaretModel().getAllCarets();
                if (carets.size() != 1) {
                    return true;
                }
                Caret caret = carets.get(0);
                if (caret.hasSelection()) {
                    return true;
                }

                int line = editor.getDocument().getLineNumber(editor.getExpectedCaretOffset());

                List<TextMergeChange> changes = getAllChanges();
                for (TextMergeChange change : changes) {
                    if (!isEnabled(change)) {
                        continue;
                    }
                    int line1 = change.getStartLine(side);
                    int line2 = change.getEndLine(side);

                    if (DiffImplUtil.isSelectedByLine(line, line1, line2)) {
                        return true;
                    }
                }
                return false;
            }

            @Nonnull
            @RequiredUIAccess
            private List<TextMergeChange> getSelectedChanges(@Nonnull ThreeSide side) {
                final BitSet lines = DiffImplUtil.getSelectedLines(getEditor(side));
                List<TextMergeChange> changes = getChanges();

                List<TextMergeChange> affectedChanges = new ArrayList<>();
                for (TextMergeChange change : changes) {
                    if (!isEnabled(change)) {
                        continue;
                    }
                    int line1 = change.getStartLine(side);
                    int line2 = change.getEndLine(side);

                    if (DiffImplUtil.isSelectedByLine(lines, line1, line2)) {
                        affectedChanges.add(change);
                    }
                }
                return affectedChanges;
            }

            protected abstract String getText(@Nonnull ThreeSide side);

            protected abstract boolean isVisible(@Nonnull ThreeSide side);

            protected abstract boolean isEnabled(@Nonnull TextMergeChange change);

            @RequiredWriteAction
            protected abstract void apply(@Nonnull ThreeSide side, @Nonnull List<TextMergeChange> changes);
        }

        private class IgnoreSelectedChangesSideAction extends ApplySelectedChangesActionBase {
            @Nonnull
            private final Side mySide;

            public IgnoreSelectedChangesSideAction(@Nonnull Side side, boolean shortcut) {
                super(shortcut);
                mySide = side;
                ActionUtil.copyFrom(this, mySide.select("Diff.IgnoreLeftSide", "Diff.IgnoreRightSide"));
            }

            @Override
            protected String getText(@Nonnull ThreeSide side) {
                return "Ignore";
            }

            @Override
            protected boolean isVisible(@Nonnull ThreeSide side) {
                return side == mySide.select(ThreeSide.LEFT, ThreeSide.RIGHT);
            }

            @Override
            protected boolean isEnabled(@Nonnull TextMergeChange change) {
                return !change.isResolved(mySide);
            }

            @RequiredWriteAction
            @Override
            protected void apply(@Nonnull ThreeSide side, @Nonnull List<TextMergeChange> changes) {
                for (TextMergeChange change : changes) {
                    ignoreChange(change, mySide, false);
                }
            }
        }

        private class IgnoreSelectedChangesAction extends ApplySelectedChangesActionBase {
            public IgnoreSelectedChangesAction() {
                super(false);
                getTemplatePresentation().setIcon(AllIcons.Diff.Remove);
            }

            @Override
            protected String getText(@Nonnull ThreeSide side) {
                return "Ignore";
            }

            @Override
            protected boolean isVisible(@Nonnull ThreeSide side) {
                return side == ThreeSide.BASE;
            }

            @Override
            protected boolean isEnabled(@Nonnull TextMergeChange change) {
                return !change.isResolved();
            }

            @Override
            @RequiredWriteAction
            protected void apply(@Nonnull ThreeSide side, @Nonnull List<TextMergeChange> changes) {
                for (TextMergeChange change : changes) {
                    markChangeResolved(change);
                }
            }
        }

        private class ApplySelectedChangesAction extends ApplySelectedChangesActionBase {
            @Nonnull
            private final Side mySide;

            public ApplySelectedChangesAction(@Nonnull Side side, boolean shortcut) {
                super(shortcut);
                mySide = side;
                ActionUtil.copyFrom(this, mySide.select("Diff.ApplyLeftSide", "Diff.ApplyRightSide"));
            }

            @Override
            protected String getText(@Nonnull ThreeSide side) {
                return side != ThreeSide.BASE ? "Accept" : getTemplatePresentation().getText();
            }

            @Override
            protected boolean isVisible(@Nonnull ThreeSide side) {
                return side == ThreeSide.BASE || side == mySide.select(ThreeSide.LEFT, ThreeSide.RIGHT);
            }

            @Override
            protected boolean isEnabled(@Nonnull TextMergeChange change) {
                return !change.isResolved(mySide);
            }

            @RequiredWriteAction
            @Override
            protected void apply(@Nonnull ThreeSide side, @Nonnull List<TextMergeChange> changes) {
                for (int i = changes.size() - 1; i >= 0; i--) {
                    replaceChange(changes.get(i), mySide, false);
                }
            }
        }

        private class ResolveSelectedChangesAction extends ApplySelectedChangesActionBase {
            @Nonnull
            private final Side mySide;

            public ResolveSelectedChangesAction(@Nonnull Side side) {
                super(false);
                mySide = side;
            }

            @Override
            protected String getText(@Nonnull ThreeSide side) {
                return mySide.select("Resolve using Left", "Resolve using Right");
            }

            @Override
            protected boolean isVisible(@Nonnull ThreeSide side) {
                return side == ThreeSide.BASE;
            }

            @Override
            protected boolean isEnabled(@Nonnull TextMergeChange change) {
                return !change.isResolved(mySide);
            }

            @RequiredWriteAction
            @Override
            protected void apply(@Nonnull ThreeSide side, @Nonnull List<TextMergeChange> changes) {
                for (int i = changes.size() - 1; i >= 0; i--) {
                    replaceChange(changes.get(i), mySide, true);
                }
            }
        }

        public class ApplyNonConflictsAction extends DumbAwareAction {
            @Nonnull
            private final ThreeSide mySide;

            public ApplyNonConflictsAction(@Nonnull ThreeSide side) {
                String id = side.select("Diff.ApplyNonConflicts.Left", "Diff.ApplyNonConflicts", "Diff.ApplyNonConflicts.Right");
                ActionUtil.copyFrom(this, id);
                mySide = side;
            }

            @Override
            @RequiredUIAccess
            public void update(AnActionEvent e) {
                e.getPresentation().setEnabled(getFirstUnresolvedChange(false, mySide) != null);
            }

            @Override
            @RequiredUIAccess
            public void actionPerformed(AnActionEvent e) {
                applyNonConflictedChanges(mySide);
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
                for (TextMergeChange mergeChange : myAllMergeChanges) {
                    if (!mergeChange.isChange(mySide)) {
                        continue;
                    }
                    ColorValue color = mergeChange.getDiffType().getColor(getEditor());
                    boolean isResolved = mergeChange.isResolved(mySide);
                    if (!handler.process(mergeChange.getStartLine(left), mergeChange.getEndLine(left),
                        mergeChange.getStartLine(right), mergeChange.getEndLine(right),
                        color, isResolved
                    )) {
                        return;
                    }
                }
            }
        }

        public class ModifierProvider extends KeyboardModifierListener {
            public void init() {
                init(myPanel, TextMergeViewer.this);
            }

            @Override
            public void onModifiersChanged() {
                for (TextMergeChange change : myAllMergeChanges) {
                    change.updateGutterActions(false);
                }
            }
        }

    }

    private static class InnerChunkData {
        @Nonnull
        public final List<CharSequence> text;

        public InnerChunkData(@Nonnull TextMergeChange change, @Nonnull List<Document> documents) {
            text = ThreeSide.map(side -> {
                if (!change.isChange(side) || change.isResolved(side)) {
                    return null;
                }
                return getChunkContent(change, documents, side);
            });
        }

        @Nullable
        @RequiredWriteAction
        private static CharSequence getChunkContent(
            @Nonnull TextMergeChange change,
            @Nonnull List<Document> documents,
            @Nonnull ThreeSide side
        ) {
            int startLine = change.getStartLine(side);
            int endLine = change.getEndLine(side);
            return startLine != endLine ? DiffImplUtil.getLinesContent(side.select(documents), startLine, endLine) : null;
        }
    }
}
