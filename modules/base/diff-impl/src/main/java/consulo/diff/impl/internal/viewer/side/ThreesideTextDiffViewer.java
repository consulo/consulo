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
package consulo.diff.impl.internal.viewer.side;

import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorEx;
import consulo.codeEditor.LogicalPosition;
import consulo.codeEditor.internal.bridge.EditorBridge;
import consulo.codeEditor.internal.bridge.EditorScrollMap;
import consulo.dataContext.DataSink;
import consulo.diff.DiffContext;
import consulo.diff.DiffDataKeys;
import consulo.diff.DiffUserDataKeys;
import consulo.diff.content.DocumentContent;
import consulo.diff.impl.internal.TextDiffSettingsHolder;
import consulo.diff.impl.internal.util.UnifiedDiffUtil;
import consulo.diff.impl.internal.viewer.EditorHolderFactory;
import consulo.diff.impl.internal.viewer.InitialScrollPositionSupport;
import consulo.diff.impl.internal.viewer.OpenInEditorWithMouseAction;
import consulo.diff.impl.internal.viewer.SetEditorSettingsAction;
import consulo.diff.impl.internal.viewer.TextDiffViewerUtil;
import consulo.diff.impl.internal.viewer.TextEditorHolder;
import consulo.diff.internal.DiffImplUtil;
import consulo.diff.internal.DiffUserDataKeysEx;
import consulo.diff.request.ContentDiffRequest;
import consulo.diff.request.DiffRequest;
import consulo.diff.request.SimpleDiffRequest;
import consulo.diff.util.LineCol;
import consulo.diff.util.Side;
import consulo.diff.util.ThreeSide;
import consulo.document.event.DocumentEvent;
import consulo.navigation.Navigatable;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.Pair;
import org.jspecify.annotations.Nullable;

import java.util.List;

public abstract class ThreesideTextDiffViewer extends ThreesideDiffViewer<TextEditorHolder> {
    private @Nullable List<? extends EditorEx> myEditors;
    private final List<? extends EditorEx> myEditableEditors;

    protected final SetEditorSettingsAction myEditorSettingsAction;

    @RequiredUIAccess
    public ThreesideTextDiffViewer(DiffContext context, ContentDiffRequest request) {
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
    protected List<TextEditorHolder> createEditorHolders(EditorHolderFactory<TextEditorHolder> factory) {
        List<TextEditorHolder> holders = super.createEditorHolders(factory);

        boolean[] forceReadOnly = TextDiffViewerUtil.checkForceReadOnly(myContext, myRequest);
        for (int i = 0; i < 3; i++) {
            if (forceReadOnly[i]) {
                holders.get(i).getEditor().setViewer(true);
            }
        }

        ThreeSide.LEFT.select(holders).getEditor().setVerticalScrollbarOrientation(EditorEx.VERTICAL_SCROLLBAR_LEFT);
        ThreeSide.BASE.select(holders).getEditor().getMarkupModel().setErrorStripeVisible(false);

        return holders;
    }

    @Override
    @RequiredUIAccess
    protected List<? extends @Nullable Component> createTitles() {
        return UnifiedDiffUtil.createTextTitles(myRequest, getEditors());
    }

    //
    // Listeners
    //

    @RequiredUIAccess
    protected void installEditorListeners() {
        new TextDiffViewerUtil.EditorActionsPopup(createEditorPopupActions()).install(getEditors(), myPanel.getComponent());

        new TextDiffViewerUtil.EditorFontSizeSynchronizer(getEditors()).install(this);

        EditorBridge bridge = getBridge();
        if (bridge != null) {
            bridge.setSyncScrollEnabled(getTextSettings().isEnableSyncScroll());
            myEditorSettingsAction.setEditorBridge(bridge);
        }
    }

    @RequiredUIAccess
    protected void runWithoutSyncScroll(Runnable action) {
        EditorBridge bridge = getBridge();
        if (bridge != null) {
            bridge.runWithoutSyncScroll(action);
        }
        else {
            action.run();
        }
    }

    @RequiredUIAccess
    protected void updateBridge() {
    }

    public @Nullable EditorBridge getBridge() {
        return myContentPanel.getBridge();
    }

    //
    // Diff
    //

    public TextDiffSettingsHolder.TextDiffSettings getTextSettings() {
        return TextDiffViewerUtil.getTextSettings(myContext);
    }

    protected List<AnAction> createEditorPopupActions() {
        return TextDiffViewerUtil.createEditorPopupActions();
    }

    @Override
    @RequiredUIAccess
    protected void onDocumentChange(DocumentEvent event) {
        super.onDocumentChange(event);
        updateBridge();
    }

    //
    // Getters
    //

    public EditorEx getCurrentEditor() {
        return getEditor(getCurrentSide());
    }

    public DocumentContent getCurrentContent() {
        return getContent(getCurrentSide());
    }

    protected List<? extends DocumentContent> getContents() {
        //noinspection unchecked
        return (List) myRequest.getContents();
    }

    public List<? extends EditorEx> getEditors() {
        if (myEditors == null) {
            myEditors = ContainerUtil.map(getEditorHolders(), TextEditorHolder::getEditor);
        }
        return myEditors;
    }

    protected List<? extends EditorEx> getEditableEditors() {
        return myEditableEditors;
    }

    public EditorEx getEditor(ThreeSide side) {
        return side.select(getEditors());
    }

    public DocumentContent getContent(ThreeSide side) {
        return side.select(getContents());
    }

    public @Nullable ThreeSide getEditorSide(@Nullable Editor editor) {
        if (getEditor(ThreeSide.BASE) == editor) {
            return ThreeSide.BASE;
        }
        if (getEditor(ThreeSide.RIGHT) == editor) {
            return ThreeSide.RIGHT;
        }
        if (getEditor(ThreeSide.LEFT) == editor) {
            return ThreeSide.LEFT;
        }
        return null;
    }

    //
    // Abstract
    //

    @RequiredUIAccess
    protected void scrollToLine(ThreeSide side, int line) {
        DiffImplUtil.scrollEditor(getEditor(side), line, false);
        setCurrentSide(side);
    }

    protected abstract @Nullable EditorScrollMap createScrollMap();

    @RequiredUIAccess
    protected LogicalPosition transferPosition(
        ThreeSide baseSide,
        ThreeSide targetSide,
        LogicalPosition position
    ) {
        EditorScrollMap scrollMap = createScrollMap();
        if (scrollMap == null) {
            return position;
        }
        if (baseSide == targetSide) {
            return position;
        }

        int base = ThreeSide.BASE.getIndex();

        int baseLine; // line number in BASE
        if (baseSide == ThreeSide.BASE) {
            baseLine = position.line;
        }
        else {
            baseLine = scrollMap.transfer(baseSide.getIndex(), base, position.line);
        }

        int targetLine;
        if (targetSide == ThreeSide.BASE) {
            targetLine = baseLine;
        }
        else {
            targetLine = scrollMap.transfer(base, targetSide.getIndex(), baseLine);
        }

        return new LogicalPosition(targetLine, position.column);
    }

    //
    // Misc
    //

    @Override
    protected @Nullable Navigatable getNavigatable() {
        return getCurrentContent().getNavigatable(LineCol.fromCaret(getCurrentEditor()));
    }

    public static boolean canShowRequest(DiffContext context, DiffRequest request) {
        return ThreesideDiffViewer.canShowRequest(context, request, TextEditorHolder.TextEditorHolderFactory.INSTANCE);
    }

    //
    // Actions
    //

    private class MyOpenInEditorWithMouseAction extends OpenInEditorWithMouseAction {
        @Override
        protected @Nullable Navigatable getNavigatable(Editor editor, int line) {
            ThreeSide side = getEditorSide(editor);
            if (side == null) {
                return null;
            }

            return getContent(side).getNavigatable(new LineCol(line));
        }
    }

    protected class MyToggleAutoScrollAction extends TextDiffViewerUtil.ToggleAutoScrollAction {
        public MyToggleAutoScrollAction() {
            super(getTextSettings());
        }

        @Override
        @RequiredUIAccess
        public void setSelected(AnActionEvent e, boolean state) {
            super.setSelected(e, state);
            EditorBridge bridge = getBridge();
            if (bridge != null) {
                bridge.setSyncScrollEnabled(state);
            }
        }
    }

    //
    // Helpers
    //

    @Override
    public void uiDataSnapshot(DataSink sink) {
        super.uiDataSnapshot(sink);
        sink.set(DiffDataKeys.CURRENT_EDITOR, getCurrentEditor());
    }

    protected abstract class MyInitialScrollPositionHelper extends InitialScrollPositionSupport.ThreesideInitialScrollHelper {
        @Override
        protected List<? extends Editor> getEditors() {
            return ThreesideTextDiffViewer.this.getEditors();
        }

        @Override
        @RequiredUIAccess
        protected void runWithoutSyncScroll(Runnable action) {
            ThreesideTextDiffViewer.this.runWithoutSyncScroll(action);
        }

        @Override
        @RequiredUIAccess
        protected boolean doScrollToLine() {
            if (myScrollToLine == null) {
                return false;
            }

            scrollToLine(myScrollToLine.first, myScrollToLine.second);
            return true;
        }
    }

    public class TextShowPartialDiffAction extends ThreesideDiffViewer.ShowPartialDiffAction {
        public TextShowPartialDiffAction(PartialDiffMode mode) {
            super(mode);
        }

        @Override
        @RequiredUIAccess
        protected SimpleDiffRequest createRequest() {
            SimpleDiffRequest request = super.createRequest();

            ThreeSide currentSide = getCurrentSide();
            LogicalPosition currentPosition = DiffImplUtil.getCaretPosition(getCurrentEditor());

            // we won't use DiffUserDataKeysEx.EDITORS_CARET_POSITION to avoid desync scroll position (as they can point to different places)
            // TODO: pass EditorsVisiblePositions in case if view was scrolled without changing caret position ?
            if (currentSide == mySide1) {
                request.putUserData(DiffUserDataKeys.SCROLL_TO_LINE, Pair.create(Side.LEFT, currentPosition.line));
            }
            else if (currentSide == mySide2) {
                request.putUserData(DiffUserDataKeys.SCROLL_TO_LINE, Pair.create(Side.RIGHT, currentPosition.line));
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
