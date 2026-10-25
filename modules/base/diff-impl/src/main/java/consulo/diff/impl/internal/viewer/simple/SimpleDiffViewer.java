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
package consulo.diff.impl.internal.viewer.simple;

import consulo.annotation.access.RequiredWriteAction;
import consulo.application.ReadAction;
import consulo.application.progress.ProgressIndicator;
import consulo.application.util.function.ThrowableComputable;
import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorEx;
import consulo.codeEditor.internal.bridge.EditorBridge;
import consulo.codeEditor.internal.bridge.EditorBridgeConnector;
import consulo.codeEditor.internal.bridge.EditorBridgeFoldConnector;
import consulo.codeEditor.internal.bridge.EditorScrollMap;
import consulo.component.ProcessCanceledException;
import consulo.dataContext.DataSink;
import consulo.diff.DiffContext;
import consulo.diff.DiffDataKeys;
import consulo.diff.DiffNavigationContext;
import consulo.diff.PrevNextDifferenceIterable;
import consulo.diff.comparison.DiffTooBigException;
import consulo.diff.fragment.LineFragment;
import consulo.diff.impl.internal.action.AllLinesIterator;
import consulo.diff.impl.internal.action.BufferedLineIterator;
import consulo.diff.impl.internal.util.PrevNextDifferenceIterableBase;
import consulo.diff.impl.internal.util.UnifiedDiffUtil;
import consulo.diff.impl.internal.viewer.DiffNotifications;
import consulo.diff.impl.internal.viewer.FoldingModelSupport;
import consulo.diff.impl.internal.viewer.StatusPanel;
import consulo.diff.impl.internal.viewer.TextDiffViewerUtil;
import consulo.diff.impl.internal.viewer.side.TwosideTextDiffViewer;
import consulo.diff.internal.DiffImplUtil;
import consulo.diff.internal.DiffUserDataKeysEx.ScrollToPolicy;
import consulo.diff.internal.HighlightPolicy;
import consulo.diff.localize.DiffLocalize;
import consulo.diff.request.ContentDiffRequest;
import consulo.diff.request.DiffRequest;
import consulo.diff.util.LineRange;
import consulo.diff.util.Side;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.document.Document;
import consulo.document.event.DocumentEvent;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnSeparator;
import consulo.ui.ex.action.LegacyDumbAwareAction;
import consulo.ui.image.Image;
import consulo.util.collection.ContainerUtil;
import consulo.util.dataholder.UserDataHolder;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;

import static consulo.util.lang.ObjectUtil.assertNotNull;

public class SimpleDiffViewer extends TwosideTextDiffViewer {
    public static final Logger LOG = Logger.getInstance(SimpleDiffViewer.class);

    private final PrevNextDifferenceIterable myPrevNextDifferenceIterable;
    private final StatusPanel myStatusPanel;

    private final List<SimpleDiffChange> myDiffChanges = new ArrayList<>();
    private final List<SimpleDiffChange> myInvalidDiffChanges = new ArrayList<>();
    private boolean myIsContentsEqual;

    private final MyFoldingModel myFoldingModel;
    private final AlignedDiffModel myAlignedDiffModel;
    private final MyInitialScrollHelper myInitialScrollHelper = new MyInitialScrollHelper();

    @RequiredUIAccess
    public SimpleDiffViewer(DiffContext context, DiffRequest request) {
        super(context, (ContentDiffRequest)request);

        myPrevNextDifferenceIterable = new MyPrevNextDifferenceIterable();
        myStatusPanel = new MyStatusPanel();
        myFoldingModel = new MyFoldingModel(getEditors(), this);
        myAlignedDiffModel = new SimpleAlignedDiffModel(this);

        UnifiedDiffUtil.registerAction(new ReplaceSelectedChangesAction(Side.LEFT, true), myPanel.getComponent());
        UnifiedDiffUtil.registerAction(new AppendSelectedChangesAction(Side.LEFT, true), myPanel.getComponent());
        UnifiedDiffUtil.registerAction(new ReplaceSelectedChangesAction(Side.RIGHT, true), myPanel.getComponent());
        UnifiedDiffUtil.registerAction(new AppendSelectedChangesAction(Side.RIGHT, true), myPanel.getComponent());
    }

    @Override
    @RequiredUIAccess
    protected void onInit() {
        super.onInit();
        updateBridge();
    }

    @Override
    @RequiredUIAccess
    protected void onDispose() {
        destroyChangedBlocks();
        myFoldingModel.destroy();
        Disposer.dispose(myAlignedDiffModel);
        super.onDispose();
    }

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

    @Override
    public @Nullable PrevNextDifferenceIterable getDifferenceIterable() {
        return myPrevNextDifferenceIterable;
    }

    public FoldingModelSupport getFoldingModel() {
        return myFoldingModel;
    }

    public FoldingModelSupport.Settings getFoldingModelSettings() {
        return TextDiffViewerUtil.getFoldingModelSettings(myContext);
    }

    @Override
    @RequiredUIAccess
    protected void onSlowRediff() {
        super.onSlowRediff();
        myStatusPanel.setBusy(true);
        myInitialScrollHelper.onSlowRediff();
    }

    @Override
    protected Runnable performRediff(ProgressIndicator indicator) {
        try {
            indicator.checkCanceled();

            Document document1 = getContent1().getDocument();
            Document document2 = getContent2().getDocument();

            ThrowableComputable<CharSequence[], RuntimeException> action =
                () -> new CharSequence[]{document1.getImmutableCharSequence(), document2.getImmutableCharSequence()};
            CharSequence[] texts = ReadAction.compute(action);

            List<LineFragment> lineFragments = null;
            if (getHighlightPolicy().isShouldCompare()) {
                lineFragments = DiffImplUtil.compare(myRequest, texts[0], texts[1], getDiffConfig(), indicator);
            }

            boolean isContentsEqual = (lineFragments == null || lineFragments.isEmpty())
                && StringUtil.equals(texts[0], texts[1]);

            return apply(new CompareData(lineFragments, isContentsEqual));
        }
        catch (DiffTooBigException e) {
            return applyNotification(DiffNotifications::createDiffTooBig);
        }
        catch (ProcessCanceledException e) {
            throw e;
        }
        catch (Throwable e) {
            LOG.error(e);
            return applyNotification(DiffNotifications::createError);
        }
    }

    private Runnable apply(CompareData data) {
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

            if (needAlignChanges()) {
                myAlignedDiffModel.realignChanges();
            }

            myInitialScrollHelper.onRediff();

            updateBridge();
            myStatusPanel.update();
        };
    }

    private Runnable applyNotification(Supplier<Component> notification) {
        return () -> {
            clearDiffPresentation();
            myFoldingModel.destroy();
            myPanel.addNotification(notification.get());
        };
    }

    private void clearDiffPresentation() {
        myAlignedDiffModel.clear();
        myStatusPanel.setBusy(false);
        myPanel.resetNotifications();
        destroyChangedBlocks();
    }

    private DiffImplUtil.DiffConfig getDiffConfig() {
        return new DiffImplUtil.DiffConfig(getTextSettings().getIgnorePolicy(), getHighlightPolicy());
    }

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

        updateBridge();
        myStatusPanel.update();
    }

    @Override
    @RequiredUIAccess
    protected void onBeforeDocumentChange(DocumentEvent e) {
        super.onBeforeDocumentChange(e);
        if (myDiffChanges.isEmpty()) {
            return;
        }

        List<Document> documents = ContainerUtil.map(getEditors(), Editor::getDocument);
        Side side = Side.fromValue(documents, e.getDocument());
        if (side == null) {
            LOG.warn("Unknown document changed");
            return;
        }

        LineRange lineRange = DiffImplUtil.getAffectedLineRange(e);
        int shift = DiffImplUtil.countLinesShift(e);

        List<SimpleDiffChange> invalid = new ArrayList<>();
        for (SimpleDiffChange change : myDiffChanges) {
            if (change.processChange(lineRange.start(), lineRange.end(), shift, side)) {
                invalid.add(change);
            }
        }

        if (!invalid.isEmpty()) {
            myDiffChanges.removeAll(invalid);
            myInvalidDiffChanges.addAll(invalid);
        }
    }

    @RequiredUIAccess
    public boolean scrollToChange(ScrollToPolicy scrollToPolicy) {
        return doScrollToChange(scrollToPolicy);
    }

    @RequiredUIAccess
    protected boolean doScrollToChange(ScrollToPolicy scrollToPolicy) {
        SimpleDiffChange targetChange = scrollToPolicy.select(myDiffChanges);
        if (targetChange == null) {
            return false;
        }

        doScrollToChange(targetChange, false);
        return true;
    }

    private void doScrollToChange(SimpleDiffChange change, boolean animated) {
        int line1 = change.getStartLine(Side.LEFT);
        int line2 = change.getStartLine(Side.RIGHT);
        int endLine1 = change.getEndLine(Side.LEFT);
        int endLine2 = change.getEndLine(Side.RIGHT);

        DiffImplUtil.moveCaret(getEditor1(), line1);
        DiffImplUtil.moveCaret(getEditor2(), line2);

        EditorBridge bridge = getBridge();
        if (bridge != null) {
            bridge.makeVisible(getCurrentSide().getIndex(), new int[]{line1, line2}, new int[]{endLine1, endLine2}, animated);
        }
        else {
            DiffImplUtil.scrollEditor(getEditor1(), line1, animated);
            DiffImplUtil.scrollEditor(getEditor2(), line2, animated);
        }
    }

    @RequiredUIAccess
    protected boolean doScrollToContext(DiffNavigationContext context) {
        ChangedLinesIterator changedLinesIterator = new ChangedLinesIterator();
        int line = context.contextMatchCheck(changedLinesIterator);
        if (line == -1) {
            // this will work for the case, when spaces changes are ignored, and corresponding fragments are not reported as changed
            // just try to find target line  -> +-
            AllLinesIterator allLinesIterator = new AllLinesIterator(getEditor(Side.RIGHT).getDocument());
            line = context.contextMatchCheck(allLinesIterator);
        }
        if (line == -1) {
            return false;
        }

        scrollToLine(Side.RIGHT, line);
        return true;
    }

    //
    // Getters
    //

    public boolean needAlignChanges() {
        return myAlignedDiffModel.needAlignChanges();
    }

    protected List<SimpleDiffChange> getDiffChanges() {
        return myDiffChanges;
    }

    @Override
    protected Component getStatusPanel() {
        return myStatusPanel.getComponent();
    }

    protected boolean isEditable(Side side) {
        return DiffImplUtil.isEditable(getEditor(side));
    }

    //
    // Misc
    //

    @SuppressWarnings("MethodOverridesStaticMethodOfSuperclass")
    public static boolean canShowRequest(DiffContext context, DiffRequest request) {
        return TwosideTextDiffViewer.canShowRequest(context, request);
    }

    @RequiredUIAccess
    private List<SimpleDiffChange> getSelectedChanges(Side side) {
        BitSet lines = DiffImplUtil.getSelectedLines(getEditor(side));

        List<SimpleDiffChange> affectedChanges = new ArrayList<>();
        for (int i = myDiffChanges.size() - 1; i >= 0; i--) {
            SimpleDiffChange change = myDiffChanges.get(i);
            int line1 = change.getStartLine(side);
            int line2 = change.getEndLine(side);

            if (DiffImplUtil.isSelectedByLine(lines, line1, line2)) {
                affectedChanges.add(change);
            }
        }
        return affectedChanges;
    }

    @RequiredUIAccess
    private @Nullable SimpleDiffChange getSelectedChange(Side side) {
        int caretLine = getEditor(side).getCaretModel().getLogicalPosition().line;

        for (SimpleDiffChange change : myDiffChanges) {
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

    private class MyPrevNextDifferenceIterable extends PrevNextDifferenceIterableBase<SimpleDiffChange> {
        @Override
        protected List<SimpleDiffChange> getChanges() {
            return myDiffChanges;
        }

        @Override
        protected EditorEx getEditor() {
            return getCurrentEditor();
        }

        @Override
        protected int getStartLine(SimpleDiffChange change) {
            return change.getStartLine(getCurrentSide());
        }

        @Override
        protected int getEndLine(SimpleDiffChange change) {
            return change.getEndLine(getCurrentSide());
        }

        @Override
        protected void scrollToChange(SimpleDiffChange change) {
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

    private abstract class ApplySelectedChangesActionBase extends LegacyDumbAwareAction {
        protected final Side myModifiedSide;
        private final boolean myShortcut;

        public ApplySelectedChangesActionBase(Side modifiedSide, boolean shortcut) {
            myModifiedSide = modifiedSide;
            myShortcut = shortcut;
        }

        @Override
        public void update(AnActionEvent e) {
            if (myShortcut) {
                // consume shortcut even if there are nothing to do - avoid calling some other action
                e.getPresentation().setEnabledAndVisible(true);
                return;
            }

            Editor editor = e.getData(Editor.KEY);
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
        @RequiredUIAccess
        public void actionPerformed(AnActionEvent e) {
            Editor editor = e.getData(Editor.KEY);
            Side side = assertNotNull(Side.fromValue(getEditors(), editor));
            List<SimpleDiffChange> selectedChanges = getSelectedChanges(side);
            if (selectedChanges.isEmpty()) {
                return;
            }

            if (!isEditable(myModifiedSide)) {
                return;
            }

            DiffImplUtil.newWriteCommand()
                .project(e.getData(Project.KEY))
                .document(getEditor(myModifiedSide).getDocument())
                .name(DiffLocalize.messageUseSelectedChangesCommand(e.getPresentation().getTextValue()))
                .run(() -> apply(selectedChanges));
        }

        protected boolean isSomeChangeSelected(Side side) {
            if (myDiffChanges.isEmpty()) {
                return false;
            }

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

            for (SimpleDiffChange change : myDiffChanges) {
                if (change.isSelectedByLine(line, side)) {
                    return true;
                }
            }
            return false;
        }

        protected boolean isBothEditable() {
            return isEditable(Side.LEFT) && isEditable(Side.RIGHT);
        }

        protected abstract boolean isVisible(Side side);

        protected abstract String getText(Side side);

        protected abstract consulo.ui.image.@Nullable Image getIcon(Side side);

        @RequiredWriteAction
        protected abstract void apply(List<SimpleDiffChange> changes);
    }

    private class ReplaceSelectedChangesAction extends ApplySelectedChangesActionBase {
        public ReplaceSelectedChangesAction(Side focusedSide, boolean shortcut) {
            super(focusedSide.other(), shortcut);
            setShortcutSet(
                ActionManager.getInstance()
                    .getAction(focusedSide.select("Diff.ApplyLeftSide", "Diff.ApplyRightSide"))
                    .getShortcutSet()
            );
        }

        @Override
        protected boolean isVisible(Side side) {
            return !isBothEditable() || side == myModifiedSide.other();
        }

        @Override
        protected String getText(Side side) {
            return "Accept";
        }

        @Override
        protected @Nullable Image getIcon(Side side) {
            return DiffImplUtil.getArrowIcon(myModifiedSide.other());
        }

        @Override
        @RequiredWriteAction
        protected void apply(List<SimpleDiffChange> changes) {
            for (SimpleDiffChange change : changes) {
                replaceChange(change, myModifiedSide.other());
            }
        }
    }

    private class AppendSelectedChangesAction extends ApplySelectedChangesActionBase {
        public AppendSelectedChangesAction(Side focusedSide, boolean shortcut) {
            super(focusedSide.other(), shortcut);
            setShortcutSet(
                ActionManager.getInstance()
                    .getAction(focusedSide.select("Diff.AppendLeftSide", "Diff.AppendRightSide"))
                    .getShortcutSet()
            );
        }

        @Override
        protected boolean isVisible(Side side) {
            return !isBothEditable() || side == myModifiedSide.other();
        }

        @Override
        protected String getText(Side side) {
            return isBothEditable() ? myModifiedSide.select("Append to the Left", "Append to the Right") : "Append";
        }

        @Override
        protected @Nullable Image getIcon(Side side) {
            return DiffImplUtil.getArrowDownIcon(myModifiedSide.other());
        }

        @Override
        @RequiredWriteAction
        protected void apply(List<SimpleDiffChange> changes) {
            for (SimpleDiffChange change : changes) {
                appendChange(change, myModifiedSide.other());
            }
        }
    }

    @RequiredWriteAction
    public void replaceChange(SimpleDiffChange change, Side sourceSide) {
        if (!change.isValid()) {
            return;
        }
        Side outputSide = sourceSide.other();

        DiffImplUtil.applyModification(
            getEditor(outputSide).getDocument(),
            change.getStartLine(outputSide),
            change.getEndLine(outputSide),
            getEditor(sourceSide).getDocument(),
            change.getStartLine(sourceSide),
            change.getEndLine(sourceSide)
        );

        change.destroyHighlighter();
        myDiffChanges.remove(change);
    }

    @RequiredWriteAction
    public void appendChange(SimpleDiffChange change, Side sourceSide) {
        if (!change.isValid()) {
            return;
        }
        if (change.getStartLine(sourceSide) == change.getEndLine(sourceSide)) {
            return;
        }
        Side outputSide = sourceSide.other();

        DiffImplUtil.applyModification(
            getEditor(outputSide).getDocument(),
            change.getEndLine(outputSide),
            change.getEndLine(outputSide),
            getEditor(sourceSide).getDocument(),
            change.getStartLine(sourceSide),
            change.getEndLine(sourceSide)
        );

        change.destroyHighlighter();
        myDiffChanges.remove(change);
    }

    private class MyHighlightPolicySettingAction extends TextDiffViewerUtil.HighlightPolicySettingAction {
        public MyHighlightPolicySettingAction() {
            super(getTextSettings());
        }

        @Override
        @RequiredUIAccess
        protected void onSettingsChanged() {
            rediff();
        }
    }

    private class MyIgnorePolicySettingAction extends TextDiffViewerUtil.IgnorePolicySettingAction {
        public MyIgnorePolicySettingAction() {
            super(getTextSettings());
        }

        @Override
        @RequiredUIAccess
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

    @Override
    public void uiDataSnapshot(DataSink sink) {
        super.uiDataSnapshot(sink);
        sink.set(DiffDataKeys.PREV_NEXT_DIFFERENCE_ITERABLE, myPrevNextDifferenceIterable);
        sink.lazy(DiffDataKeys.CURRENT_CHANGE_RANGE, () -> {
            SimpleDiffChange change = getSelectedChange(getCurrentSide());
            if (change != null) {
                return new LineRange(change.getStartLine(getCurrentSide()), change.getEndLine(getCurrentSide()));
            }
            return null;
        });
    }

    private EditorScrollMap createScrollMap() {
        List<int[]> anchors = new ArrayList<>();
        anchors.add(new int[]{0, 0});
        for (SimpleDiffChange diffChange : myDiffChanges) {
            anchors.add(new int[]{diffChange.getStartLine(Side.LEFT), diffChange.getStartLine(Side.RIGHT)});
            anchors.add(new int[]{diffChange.getEndLine(Side.LEFT), diffChange.getEndLine(Side.RIGHT)});
        }
        anchors.add(new int[]{getEditor1().getDocument().getLineCount(), getEditor2().getDocument().getLineCount()});
        return new EditorScrollMap(anchors);
    }

    @Override
    @RequiredUIAccess
    protected int transferLine(Side baseSide, int line) {
        if (myDiffChanges.isEmpty()) {
            return line;
        }
        return createScrollMap().transfer(baseSide.getIndex(), baseSide.other().getIndex(), line);
    }

    @Override
    @RequiredUIAccess
    protected void updateBridge() {
        EditorBridge bridge = getBridge();
        if (bridge == null) {
            return;
        }

        List<EditorBridgeConnector> connectors = new ArrayList<>(myDiffChanges.size());
        for (SimpleDiffChange diffChange : myDiffChanges) {
            connectors.add(new EditorBridgeConnector(
                diffChange.getStartLine(Side.LEFT),
                diffChange.getEndLine(Side.LEFT),
                diffChange.getStartLine(Side.RIGHT),
                diffChange.getEndLine(Side.RIGHT),
                diffChange.getDiffType().getColor(getEditor1()),
                false
            ));
        }

        bridge.setScrollMap(createScrollMap());
        bridge.setConnectors(0, connectors, myFoldingModel.createFoldConnectors());
    }

    private class MyStatusPanel extends StatusPanel {
        @Override
        protected LocalizeValue getMessage() {
            if (getHighlightPolicy() == HighlightPolicy.DO_NOT_HIGHLIGHT) {
                return DiffLocalize.diffHighlightingDisabledText();
            }
            int changesCount = myDiffChanges.size() + myInvalidDiffChanges.size();
            if (changesCount == 0 && !myIsContentsEqual) {
                return DiffLocalize.diffAllDifferencesIgnoredText();
            }
            return DiffLocalize.diffCountDifferencesStatusText(changesCount);
        }
    }

    private static class CompareData {
        private final @Nullable List<LineFragment> myFragments;
        private final boolean myIsContentsEqual;

        public CompareData(@Nullable List<LineFragment> fragments, boolean isContentsEqual) {
            myFragments = fragments;
            myIsContentsEqual = isContentsEqual;
        }

        public @Nullable List<LineFragment> getFragments() {
            return myFragments;
        }

        public boolean isContentsEqual() {
            return myIsContentsEqual;
        }
    }

    private class MyFoldingModel extends FoldingModelSupport {
        private final MyPaintable myPaintable = new MyPaintable(0, 1);

        public MyFoldingModel(List<? extends EditorEx> editors, Disposable disposable) {
            super(editors.toArray(new EditorEx[2]), disposable);
        }

        @RequiredUIAccess
        public void install(
            @Nullable List<LineFragment> fragments,
            UserDataHolder context,
            FoldingModelSupport.Settings settings
        ) {
            Iterator<int[]> it = map(fragments, fragment -> new int[]{
                fragment.getStartLine1(),
                fragment.getEndLine1(),
                fragment.getStartLine2(),
                fragment.getEndLine2()
            });
            install(it, context, settings);
        }

        public List<EditorBridgeFoldConnector> createFoldConnectors() {
            return myPaintable.createFoldConnectors();
        }

        @Override
        @RequiredUIAccess
        protected void onFoldingChanged() {
            updateBridge();
        }
    }

    private class MyInitialScrollHelper extends MyInitialScrollPositionHelper {
        @Override
        @RequiredUIAccess
        protected boolean doScrollToChange() {
            return myScrollToChange != null && SimpleDiffViewer.this.doScrollToChange(myScrollToChange);
        }

        @Override
        @RequiredUIAccess
        protected boolean doScrollToFirstChange() {
            return SimpleDiffViewer.this.doScrollToChange(ScrollToPolicy.FIRST_CHANGE);
        }

        @Override
        @RequiredUIAccess
        protected boolean doScrollToContext() {
            return myNavigationContext != null && SimpleDiffViewer.this.doScrollToContext(myNavigationContext);
        }
    }
}
