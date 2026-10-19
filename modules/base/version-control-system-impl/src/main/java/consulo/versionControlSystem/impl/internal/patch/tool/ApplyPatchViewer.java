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
package consulo.versionControlSystem.impl.internal.patch.tool;

import consulo.annotation.access.RequiredWriteAction;
import consulo.application.Application;
import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorEx;
import consulo.codeEditor.LogicalPosition;
import consulo.codeEditor.ScrollType;
import consulo.codeEditor.internal.bridge.EditorBridge;
import consulo.codeEditor.internal.bridge.EditorBridgeScrollUtil;
import consulo.dataContext.DataSink;
import consulo.dataContext.UiDataProvider;
import consulo.diff.DiffContentFactory;
import consulo.diff.DiffContext;
import consulo.diff.DiffDataKeys;
import consulo.diff.DiffDialogHints;
import consulo.diff.DiffManager;
import consulo.diff.DiffUserDataKeys;
import consulo.diff.content.DocumentContent;
import consulo.diff.impl.internal.TextDiffSettingsHolder.TextDiffSettings;
import consulo.diff.impl.internal.action.FocusOppositePaneAction;
import consulo.diff.impl.internal.action.ProxyUndoRedoAction;
import consulo.diff.impl.internal.merge.MergeModelBase;
import consulo.diff.impl.internal.util.DiffDrawUtil;
import consulo.diff.impl.internal.util.DividerPaintable;
import consulo.diff.impl.internal.util.PrevNextDifferenceIterableBase;
import consulo.diff.impl.internal.util.UnifiedDiffUtil;
import consulo.diff.impl.internal.viewer.FocusTrackerSupport;
import consulo.diff.impl.internal.viewer.FoldingModelSupport;
import consulo.diff.impl.internal.viewer.SetEditorSettingsAction;
import consulo.diff.impl.internal.viewer.SimpleDiffPanel;
import consulo.diff.impl.internal.viewer.StatusPanel;
import consulo.diff.impl.internal.viewer.TextDiffViewerUtil;
import consulo.diff.impl.internal.viewer.TextEditorHolder;
import consulo.diff.impl.internal.viewer.side.TwosideContentPanel;
import consulo.diff.internal.DiffImplUtil;
import consulo.diff.internal.LineNumberConvertor;
import consulo.diff.localize.DiffLocalize;
import consulo.diff.request.SimpleDiffRequest;
import consulo.diff.util.LineRange;
import consulo.diff.util.Side;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.document.Document;
import consulo.document.internal.DocumentFactory;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.Point2D;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnSeparator;
import consulo.ui.ex.action.CompositeShortcutSet;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.action.LegacyDumbAwareAction;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.action.util.ActionUtil;
import consulo.undoRedo.builder.RunnableCommandBuilder;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.primitive.ints.IntList;
import consulo.util.lang.Pair;
import consulo.versionControlSystem.impl.internal.patch.apply.AppliedTextPatch;
import consulo.versionControlSystem.localize.VcsLocalize;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;

class ApplyPatchViewer implements UiDataProvider, Disposable {
    public static final Logger LOG = Logger.getInstance(ApplyPatchViewer.class);

    private final @Nullable Project myProject;
    private final DiffContext myContext;
    private final ApplyPatchRequest myPatchRequest;

    private final TextEditorHolder myResultHolder;
    private final TextEditorHolder myPatchHolder;
    private final EditorEx myResultEditor;
    private final EditorEx myPatchEditor;

    private final SimpleDiffPanel myPanel;
    private final TwosideContentPanel myContentPanel;

    private final MyModel myModel;

    private final FocusTrackerSupport<Side> myFocusTrackerSupport;
    private final MyPrevNextDifferenceIterable myPrevNextDifferenceIterable;
    private final StatusPanel myStatusPanel;
    private final MyFoldingModel myFoldingModel;

    private final SetEditorSettingsAction myEditorSettingsAction;

    // Changes with known AppliedTo. Ordered as in result-editor
    private final List<ApplyPatchChange> myResultChanges = new ArrayList<>();
    // All changes. Ordered as in patch-editor
    private final List<ApplyPatchChange> myPatchChanges = new ArrayList<>();
    // All changes. Ordered as in result-editor. Non-applied changes are at the very beginning with model ranges [-1. -1)
    private final List<ApplyPatchChange> myModelChanges = new ArrayList<>();

    private boolean myDisposed;

    @RequiredUIAccess
    public ApplyPatchViewer(DiffContext context, ApplyPatchRequest request) {
        myProject = context.getProject();
        myContext = context;
        myPatchRequest = request;


        DocumentContent resultContent = request.getResultContent();
        Document patchDocument = Application.get().getInstance(DocumentFactory.class).createDocument("", false, true);
        DocumentContent patchContent = DiffContentFactory.getInstance().create(patchDocument, resultContent);

        myResultHolder = TextEditorHolder.create(myProject, resultContent);
        myPatchHolder = TextEditorHolder.create(myProject, patchContent);

        myResultEditor = myResultHolder.getEditor();
        myPatchEditor = myPatchHolder.getEditor();

        if (isReadOnly()) {
            myResultEditor.setViewer(true);
        }
        myPatchEditor.setViewer(true);

        myResultEditor.getMarkupModel().setErrorStripeVisible(false);
        myResultEditor.setVerticalScrollbarOrientation(EditorEx.VERTICAL_SCROLLBAR_LEFT);

        myPatchEditor.getGutterComponentEx().setForceShowRightFreePaintersArea(true);
        myPatchEditor.getMarkupModel().setErrorStripeVisible(false);

        List<TextEditorHolder> holders = Arrays.asList(myResultHolder, myPatchHolder);
        List<EditorEx> editors = Arrays.asList(myResultEditor, myPatchEditor);

        myContentPanel = TwosideContentPanel.createFromHolders(holders, this);
        myContentPanel.setTitles(List.of(
            UnifiedDiffUtil.createTitle(myPatchRequest.getResultTitle()),
            UnifiedDiffUtil.createTitle(myPatchRequest.getPatchTitle())
        ));
        myPanel = new SimpleDiffPanel(myContentPanel.getComponent(), this, myContext);

        myModel = new MyModel(myProject, myResultEditor.getDocument());

        myFocusTrackerSupport = new FocusTrackerSupport.Twoside(holders);
        myFocusTrackerSupport.setCurrentSide(Side.LEFT);
        myPrevNextDifferenceIterable = new MyPrevNextDifferenceIterable();
        myStatusPanel = new MyStatusPanel();
        myFoldingModel = new MyFoldingModel(myResultEditor, this);


        new MyFocusOppositePaneAction().install(myPanel.getComponent());
        new TextDiffViewerUtil.EditorActionsPopup(createEditorPopupActions()).install(editors, myPanel.getComponent());

        new TextDiffViewerUtil.EditorFontSizeSynchronizer(editors).install(this);

        myEditorSettingsAction = new SetEditorSettingsAction(getTextSettings(), editors);
        myEditorSettingsAction.applyDefaults();

        EditorBridge bridge = myContentPanel.getBridge();
        if (bridge != null) {
            bridge.setSyncScrollEnabled(false);
            myEditorSettingsAction.setEditorBridge(bridge);
        }

        if (!isReadOnly()) {
            UnifiedDiffUtil.registerAction(new ApplySelectedChangesAction(true), myPanel.getComponent());
            UnifiedDiffUtil.registerAction(new IgnoreSelectedChangesAction(true), myPanel.getComponent());
        }

        ProxyUndoRedoAction.register(myProject, myResultEditor, myContentPanel.getComponent());
    }

    protected List<AnAction> createToolbarActions() {
        List<AnAction> group = new ArrayList<>();

        if (!isReadOnly()) {
            group.add(new MyToggleExpandByDefaultAction());
            group.add(myEditorSettingsAction);
            group.add(AnSeparator.getInstance());
            group.add(new ShowDiffWithLocalAction());
            group.add(new ApplyNonConflictsAction());
        }

        return group;
    }

    private List<AnAction> createEditorPopupActions() {
        List<AnAction> group = new ArrayList<>();

        if (!isReadOnly()) {
            group.add(new ApplySelectedChangesAction(false));
            group.add(new IgnoreSelectedChangesAction(false));
        }

        group.add(AnSeparator.getInstance());
        group.addAll(TextDiffViewerUtil.createEditorPopupActions());

        return group;
    }

    @Override
    @RequiredUIAccess
    public void dispose() {
        if (myDisposed) {
            return;
        }
        myDisposed = true;

        myFoldingModel.destroy();

        Disposer.dispose(myModel);

        Disposer.dispose(myResultHolder);
        Disposer.dispose(myPatchHolder);
    }

    //
    // Getters
    //

    public boolean isReadOnly() {
        return !DiffImplUtil.canMakeWritable(myResultEditor.getDocument());
    }

    public MyModel getModel() {
        return myModel;
    }

    public List<ApplyPatchChange> getModelChanges() {
        return myModelChanges;
    }

    public boolean isDisposed() {
        return myDisposed;
    }

    public StatusPanel getStatusPanel() {
        return myStatusPanel;
    }

    public Component getComponent() {
        return myPanel.getComponent();
    }

    public void addNotification(Component notification) {
        myPanel.addNotification(notification);
    }

    public @Nullable Component getPreferredFocusedComponent() {
        return myResultHolder.getPreferredFocusedComponent();
    }

    public EditorEx getResultEditor() {
        return myResultEditor;
    }

    public EditorEx getPatchEditor() {
        return myPatchEditor;
    }

    public Side getCurrentSide() {
        return myFocusTrackerSupport.getCurrentSide();
    }

    public List<ApplyPatchChange> getPatchChanges() {
        return myPatchChanges;
    }

    @Override
    public void uiDataSnapshot(DataSink sink) {
        sink.set(Project.KEY, myProject);
        sink.set(DiffDataKeys.PREV_NEXT_DIFFERENCE_ITERABLE, myPrevNextDifferenceIterable);
    }

    public TextDiffSettings getTextSettings() {
        return TextDiffSettings.getSettings("ApplyPatch");
    }

    public FoldingModelSupport.Settings getFoldingModelSettings() {
        TextDiffSettings settings = getTextSettings();
        return new FoldingModelSupport.Settings(settings.getContextRange(), settings.isExpandByDefault());
    }

    //
    // Impl
    //

    @RequiredUIAccess
    protected void initPatchViewer() {
        Document outputDocument = myResultEditor.getDocument();
        DiffImplUtil.newWriteCommand()
            .project(myProject)
            .document(myResultEditor.getDocument())
            .name(DiffLocalize.messageInitMergeContentCommand())
            .run(() -> {
                outputDocument.setText(myPatchRequest.getLocalContent());
                if (!isReadOnly()) {
                    DiffImplUtil.putNonundoableOperation(myProject, outputDocument);
                }
            });

        PatchChangeBuilder builder = new PatchChangeBuilder();
        builder.exec(myPatchRequest.getPatch().getHunks());

        Document patchDocument = myPatchEditor.getDocument();
        patchDocument.setText(builder.getPatchContent());

        LineNumberConvertor converter = builder.getLineConvertor();
        myPatchEditor.getGutterComponentEx().setLineNumberConvertor(converter.createConvertor1(), converter.createConvertor2());

        IntList lines = builder.getSeparatorLines();
        for (int i = 0; i < lines.size(); i++) {
            int offset = patchDocument.getLineStartOffset(lines.get(i));
            DiffDrawUtil.createLineSeparatorHighlighter(myPatchEditor, offset, offset, () -> true);
        }

        List<PatchChangeBuilder.Hunk> hunks = builder.getHunks();

        int[] modelToPatchIndexes = DiffImplUtil.getSortedIndexes(
            hunks,
            (h1, h2) -> {
                LineRange lines1 = h1.appliedToLines();
                LineRange lines2 = h2.appliedToLines();
                if (lines1 == null && lines2 == null) {
                    return 0;
                }
                if (lines1 == null) {
                    return -1;
                }
                if (lines2 == null) {
                    return 1;
                }
                return lines1.start() - lines2.start();
            }
        );
        int[] patchToModelIndexes = DiffImplUtil.invertIndexes(modelToPatchIndexes);

        List<LineRange> modelRanges = new ArrayList<>();
        for (int modelIndex = 0; modelIndex < hunks.size(); modelIndex++) {
            int patchIndex = modelToPatchIndexes[modelIndex];
            PatchChangeBuilder.Hunk hunk = hunks.get(patchIndex);
            LineRange resultRange = hunk.appliedToLines();

            ApplyPatchChange change = new ApplyPatchChange(hunk, modelIndex, this);

            myModelChanges.add(change);
            if (resultRange != null) {
                myResultChanges.add(change);
            }

            modelRanges.add(resultRange != null ? resultRange : new LineRange(-1, -1));
        }
        myModel.setChanges(modelRanges);

        for (int index : patchToModelIndexes) {
            myPatchChanges.add(myModelChanges.get(index));
        }

        myFoldingModel.install(myResultChanges, myContext, getFoldingModelSettings());

        for (ApplyPatchChange change : myModelChanges) {
            change.reinstallHighlighters();
        }
        myStatusPanel.update();

        myPatchEditor.getGutterComponentEx().revalidateMarkup();


        if (myResultChanges.size() > 0) {
            scrollToChange(myResultChanges.get(0), Side.LEFT, true);
        }
    }

    @RequiredUIAccess
    public void scrollToChange(ApplyPatchChange change, Side masterSide, boolean forceScroll) {
        if (change.getResultRange() == null) {
            DiffImplUtil.moveCaret(myPatchEditor, change.getPatchRange().start());
            myPatchEditor.getScrollingModel().scrollToCaret(forceScroll ? ScrollType.CENTER : ScrollType.MAKE_VISIBLE);
        }
        else {
            LineRange resultRange = change.getResultRange();
            LineRange patchRange = change.getPatchAffectedRange();

            int topShift = -1;
            if (!forceScroll) {
                int masterLine = masterSide.select(resultRange.start(), patchRange.start());
                EditorEx masterEditor = masterSide.select(myResultEditor, myPatchEditor);
                int targetY = masterEditor.logicalPositionToXY(new LogicalPosition(masterLine, 0)).y;
                int scrollOffset = masterEditor.getScrollingModel().getVerticalScrollOffset();
                topShift = targetY - scrollOffset;
            }

            int[] offsets = EditorBridgeScrollUtil.getTargetOffsets(
                List.of(myResultEditor, myPatchEditor),
                new int[]{resultRange.start(), patchRange.start()},
                new int[]{resultRange.end(), patchRange.end()},
                topShift,
                EditorBridgeScrollUtil::getDocumentMaximumOffset
            );

            DiffImplUtil.moveCaret(myResultEditor, resultRange.start());
            DiffImplUtil.moveCaret(myPatchEditor, patchRange.start());

            UnifiedDiffUtil.scrollToPoint(myResultEditor, new Point2D(0, offsets[0]), false);
            UnifiedDiffUtil.scrollToPoint(myPatchEditor, new Point2D(0, offsets[1]), false);
        }
    }

    //
    // Modification operations
    //

    @RequiredUIAccess
    public void updateBridge() {
        EditorBridge bridge = myContentPanel.getBridge();
        if (bridge == null) {
            return;
        }
        bridge.setConnectors(0, UnifiedDiffUtil.createConnectors(new MyDividerPaintable()), List.of());
    }

    @RequiredUIAccess
    <R> RunnableCommandBuilder<R, ? extends RunnableCommandBuilder> newPatchCommand() {
        return myModel.newMergeCommand(null);
    }

    @Deprecated(forRemoval = true)
    @RequiredUIAccess
    public void executeCommand(@Nullable String commandName, @RequiredUIAccess Runnable task) {
        newPatchCommand().name(LocalizeValue.ofNullable(commandName)).run(task);
    }

    class MyModel extends MergeModelBase<ApplyPatchChange.State> {
        public MyModel(@Nullable Project project, Document document) {
            super(project, document);
        }

        @Override
        @RequiredUIAccess
        protected void reinstallHighlighters(int index) {
            ApplyPatchChange change = myModelChanges.get(index);
            change.reinstallHighlighters();
        }

        @Override
        @RequiredUIAccess
        protected ApplyPatchChange.State storeChangeState(int index) {
            ApplyPatchChange change = myModelChanges.get(index);
            return change.storeState();
        }

        @Override
        @RequiredUIAccess
        protected void restoreChangeState(ApplyPatchChange.State state) {
            super.restoreChangeState(state);
            ApplyPatchChange change = myModelChanges.get(state.myIndex);

            boolean wasResolved = change.isResolved();
            change.restoreState(state);
            if (wasResolved != change.isResolved()) {
                onChangeResolved();
            }
        }
    }

    protected void onChangeResolved() {
        if (isDisposed()) {
            return;
        }
        myStatusPanel.update();
    }

    @RequiredUIAccess
    public void markChangeResolved(ApplyPatchChange change) {
        if (change.isResolved()) {
            return;
        }

        change.setResolved(true);
        myModel.invalidateHighlighters(change.getIndex());
        onChangeResolved();
    }

    @RequiredWriteAction
    public void replaceChange(ApplyPatchChange change) {
        LineRange resultRange = change.getResultRange();
        LineRange patchRange = change.getPatchInsertionRange();
        if (resultRange == null || change.isResolved()) {
            return;
        }
        if (change.getStatus() != AppliedTextPatch.HunkStatus.EXACTLY_APPLIED) {
            return;
        }

        List<String> newContent = DiffImplUtil.getLines(myPatchEditor.getDocument(), patchRange.start(), patchRange.end());
        myModel.replaceChange(change.getIndex(), newContent);

        markChangeResolved(change);
    }

    private class ApplySelectedChangesAction extends ApplySelectedChangesActionBase {
        private ApplySelectedChangesAction(boolean shortcut) {
            super(shortcut);
            getTemplatePresentation().setText(LocalizeValue.localizeTODO("Accept"));
            getTemplatePresentation().setIcon(PlatformIconGroup.actionsChecked());
            copyShortcutFrom(ActionManager.getInstance().getAction("Diff.ApplyRightSide"));
        }

        @Override
        protected boolean isEnabled(ApplyPatchChange change) {
            return !change.isResolved() && change.getStatus() == AppliedTextPatch.HunkStatus.EXACTLY_APPLIED;
        }

        @Override
        @RequiredWriteAction
        protected void apply(List<ApplyPatchChange> changes) {
            for (int i = changes.size() - 1; i >= 0; i--) {
                replaceChange(changes.get(i));
            }
        }
    }

    private class IgnoreSelectedChangesAction extends ApplySelectedChangesActionBase {
        private IgnoreSelectedChangesAction(boolean shortcut) {
            super(shortcut);
            getTemplatePresentation().setText(LocalizeValue.localizeTODO("Ignore"));
            getTemplatePresentation().setIcon(PlatformIconGroup.diffRemove());
            setShortcutSet(new CompositeShortcutSet(
                ActionManager.getInstance().getAction("Diff.IgnoreRightSide").getShortcutSet(),
                ActionManager.getInstance().getAction("Diff.ApplyLeftSide").getShortcutSet()
            ));
        }

        @Override
        protected boolean isEnabled(ApplyPatchChange change) {
            return !change.isResolved();
        }

        @Override
        @RequiredUIAccess
        protected void apply(List<ApplyPatchChange> changes) {
            for (ApplyPatchChange change : changes) {
                markChangeResolved(change);
            }
        }
    }

    private abstract class ApplySelectedChangesActionBase extends LegacyDumbAwareAction {
        private final boolean myShortcut;

        public ApplySelectedChangesActionBase(boolean shortcut) {
            myShortcut = shortcut;
        }

        @Override
        public void update(AnActionEvent e) {
            if (myShortcut) {
                // consume shortcut even if there are nothing to do - avoid calling some other action
                e.getPresentation().setEnabledAndVisible(true);
                return;
            }

            Presentation presentation = e.getPresentation();
            Editor editor = e.getData(Editor.KEY);

            Side side = Side.fromValue(Arrays.asList(myResultEditor, myPatchEditor), editor);
            if (side == null) {
                presentation.setEnabledAndVisible(false);
                return;
            }

            presentation.setVisible(true);
            presentation.setEnabled(isSomeChangeSelected(side));
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(AnActionEvent e) {
            Editor editor = e.getData(Editor.KEY);
            Side side = Side.fromValue(Arrays.asList(myResultEditor, myPatchEditor), editor);
            if (editor == null || side == null) {
                return;
            }

            List<ApplyPatchChange> selectedChanges = getSelectedChanges(side);
            if (selectedChanges.isEmpty()) {
                return;
            }

            newPatchCommand()
                .name(VcsLocalize.patchApplyChangesInPatchResolve(e.getPresentation().getTextValue()))
                .run(() -> apply(selectedChanges));
        }

        private boolean isSomeChangeSelected(Side side) {
            EditorEx editor = side.select(myResultEditor, myPatchEditor);
            List<Caret> carets = editor.getCaretModel().getAllCarets();
            if (carets.size() != 1) {
                return true;
            }
            Caret caret = carets.get(0);
            if (caret.hasSelection()) {
                return true;
            }

            int line = editor.getDocument().getLineNumber(editor.getExpectedCaretOffset());

            List<ApplyPatchChange> changes = myModelChanges;
            for (ApplyPatchChange change : changes) {
                if (!isEnabled(change)) {
                    continue;
                }
                LineRange range = side.select(change.getResultRange(), change.getPatchRange());
                if (range == null) {
                    continue;
                }

                if (DiffImplUtil.isSelectedByLine(line, range.start(), range.end())) {
                    return true;
                }
            }
            return false;
        }

        @RequiredUIAccess
        private List<ApplyPatchChange> getSelectedChanges(Side side) {
            BitSet lines = DiffImplUtil.getSelectedLines(side.select(myResultEditor, myPatchEditor));

            List<ApplyPatchChange> affectedChanges = new ArrayList<>();
            for (ApplyPatchChange change : myModelChanges) {
                if (!isEnabled(change)) {
                    continue;
                }
                LineRange range = side.select(change.getResultRange(), change.getPatchRange());
                if (range == null) {
                    continue;
                }

                if (DiffImplUtil.isSelectedByLine(lines, range.start(), range.end())) {
                    affectedChanges.add(change);
                }
            }
            return affectedChanges;
        }

        protected abstract boolean isEnabled(ApplyPatchChange change);

        @RequiredWriteAction
        protected abstract void apply(List<ApplyPatchChange> changes);
    }

    private class ApplyNonConflictsAction extends LegacyDumbAwareAction {
        public ApplyNonConflictsAction() {
            ActionUtil.copyFrom(this, "Diff.ApplyNonConflicts");
        }

        @Override
        public void update(AnActionEvent e) {
            boolean enabled = ContainerUtil.exists(
                myModelChanges,
                c -> !c.isResolved() && c.getStatus() != AppliedTextPatch.HunkStatus.NOT_APPLIED
            );
            e.getPresentation().setEnabled(enabled);
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(AnActionEvent e) {
            List<ApplyPatchChange> changes = myModelChanges;
            if (changes.isEmpty()) {
                return;
            }

            newPatchCommand()
                .name(DiffLocalize.mergeDialogApplyNonConflictedChangesCommand())
                .run(() -> {
                    for (int i = changes.size() - 1; i >= 0; i--) {
                        ApplyPatchChange change = changes.get(i);
                        switch (change.getStatus()) {
                            case ALREADY_APPLIED -> markChangeResolved(change);
                            case EXACTLY_APPLIED -> replaceChange(change);
                        }
                    }
                });
        }
    }

    //
    // Actions
    //

    private class MyFocusOppositePaneAction extends FocusOppositePaneAction {
        public MyFocusOppositePaneAction() {
            super(false);
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(AnActionEvent e) {
            EditorEx targetEditor = getCurrentSide().other().select(myResultEditor, myPatchEditor);
            UnifiedDiffUtil.requestFocus(targetEditor.getContentUIComponent());
        }
    }

    private class MyToggleExpandByDefaultAction extends TextDiffViewerUtil.ToggleExpandByDefaultAction {
        public MyToggleExpandByDefaultAction() {
            super(getTextSettings());
        }

        @Override
        @RequiredUIAccess
        protected void expandAll(boolean expand) {
            myFoldingModel.expandAll(expand);
        }
    }

    private class ShowDiffWithLocalAction extends DumbAwareAction {
        public ShowDiffWithLocalAction() {
            super(LocalizeValue.localizeTODO("Compare with local content"), LocalizeValue.empty(), PlatformIconGroup.actionsDiff());
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(AnActionEvent e) {
            DocumentContent resultContent = myPatchRequest.getResultContent();
            DocumentContent localContent = DiffContentFactory.getInstance().create(myPatchRequest.getLocalContent(), resultContent);

            SimpleDiffRequest request = new SimpleDiffRequest(
                myPatchRequest.getTitle(),
                localContent,
                resultContent,
                myPatchRequest.getLocalTitle(),
                myPatchRequest.getResultTitle()
            );

            LogicalPosition currentPosition = DiffImplUtil.getCaretPosition(myResultEditor);
            request.putUserData(DiffUserDataKeys.SCROLL_TO_LINE, Pair.create(Side.RIGHT, currentPosition.line));

            DiffManager.getInstance().showDiff(myProject, request, new DiffDialogHints(null, myPanel.getComponent()));
        }
    }

    //
    // Helpers
    //

    private class MyPrevNextDifferenceIterable extends PrevNextDifferenceIterableBase<ApplyPatchChange> {
        @Override
        protected List<ApplyPatchChange> getChanges() {
            return getCurrentSide().select(myResultChanges, myPatchChanges);
        }

        @Override
        protected EditorEx getEditor() {
            return getCurrentSide().select(myResultEditor, myPatchEditor);
        }

        @Override
        protected int getStartLine(ApplyPatchChange change) {
            //noinspection ConstantConditions
            return getCurrentSide().select(change.getResultRange(), change.getPatchAffectedRange()).start();
        }

        @Override
        protected int getEndLine(ApplyPatchChange change) {
            //noinspection ConstantConditions
            return getCurrentSide().select(change.getResultRange(), change.getPatchAffectedRange()).end();
        }

        @Override
        @RequiredUIAccess
        protected void scrollToChange(ApplyPatchChange change) {
            ApplyPatchViewer.this.scrollToChange(change, getCurrentSide(), true);
        }
    }

    private class MyDividerPaintable implements DividerPaintable {
        @Override
        public void process(Handler handler) {
            for (ApplyPatchChange change : myResultChanges) {
                LineRange resultRange = change.getResultRange();
                LineRange patchRange = change.getPatchRange();
                assert resultRange != null;

                ColorValue color = change.getDiffType().getColor(myPatchEditor);

                // do not abort - ranges are ordered in patch order, but they can be not ordered in terms of resultRange
                handler.process(
                    resultRange.start(),
                    resultRange.end(),
                    patchRange.start(),
                    patchRange.end(),
                    color,
                    change.isResolved()
                );
            }
        }
    }

    private static class MyFoldingModel extends FoldingModelSupport {
        public MyFoldingModel(EditorEx editor, Disposable disposable) {
            super(new EditorEx[]{editor}, disposable);
        }

        @RequiredUIAccess
        public void install(
            @Nullable List<ApplyPatchChange> changes,
            DiffContext context,
            FoldingModelSupport.Settings settings
        ) {
            //noinspection ConstantConditions
            Iterator<int[]> it = map(
                changes,
                fragment -> new int[]{
                    fragment.getResultRange().start(),
                    fragment.getResultRange().end()
                }
            );
            install(it, context, settings);
        }
    }

    private class MyStatusPanel extends StatusPanel {
        @Override
        protected LocalizeValue getMessage() {
            int totalUnresolved = 0;
            int alreadyApplied = 0;
            int notApplied = 0;
            for (ApplyPatchChange change : myPatchChanges) {
                if (change.isResolved()) {
                    continue;
                }

                totalUnresolved++;
                switch (change.getStatus()) {
                    case ALREADY_APPLIED:
                        alreadyApplied++;
                        break;
                    case NOT_APPLIED:
                        notApplied++;
                        break;
                    case EXACTLY_APPLIED:
                        break;
                }
            }

            if (totalUnresolved == 0) {
                return DiffLocalize.applySomehowStatusMessageAllApplied();
            }
            if (totalUnresolved == notApplied) {
                return DiffLocalize.applySomehowStatusMessageCantApply(notApplied);
            }
            else {
                LocalizeValue message = DiffLocalize.applySomehowStatusMessageCantApplySome(notApplied, totalUnresolved);
                if (alreadyApplied == 0) {
                    return message;
                }
                return LocalizeValue.join(". ", message, DiffLocalize.applySomehowStatusMessageAlreadyApplied(alreadyApplied));
            }
        }
    }
}
