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
import consulo.codeEditor.ScrollType;
import consulo.codeEditor.internal.bridge.EditorBridge;
import consulo.dataContext.DataSink;
import consulo.diff.DiffContext;
import consulo.diff.DiffDataKeys;
import consulo.diff.content.DocumentContent;
import consulo.diff.impl.internal.TextDiffSettingsHolder;
import consulo.diff.impl.internal.action.FocusOppositePaneAction;
import consulo.diff.impl.internal.action.ProxyUndoRedoAction;
import consulo.diff.impl.internal.util.UnifiedDiffUtil;
import consulo.diff.impl.internal.viewer.EditorHolderFactory;
import consulo.diff.impl.internal.viewer.InitialScrollPositionSupport;
import consulo.diff.impl.internal.viewer.OpenInEditorWithMouseAction;
import consulo.diff.impl.internal.viewer.SetEditorSettingsAction;
import consulo.diff.impl.internal.viewer.TextDiffViewerUtil;
import consulo.diff.impl.internal.viewer.TextEditorHolder;
import consulo.diff.internal.DiffImplUtil;
import consulo.diff.request.ContentDiffRequest;
import consulo.diff.request.DiffRequest;
import consulo.diff.util.LineCol;
import consulo.diff.util.Side;
import consulo.document.event.DocumentEvent;
import consulo.logging.Logger;
import consulo.navigation.Navigatable;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.util.collection.ContainerUtil;
import org.jspecify.annotations.Nullable;

import java.util.List;

public abstract class TwosideTextDiffViewer extends TwosideDiffViewer<TextEditorHolder> {
    public static final Logger LOG = Logger.getInstance(TwosideTextDiffViewer.class);

    private final List<? extends EditorEx> myEditableEditors;
    private @Nullable List<? extends EditorEx> myEditors;

    protected final SetEditorSettingsAction myEditorSettingsAction;

    @RequiredUIAccess
    public TwosideTextDiffViewer(DiffContext context, ContentDiffRequest request) {
        super(context, request, TextEditorHolder.TextEditorHolderFactory.INSTANCE);

        new MyFocusOppositePaneAction(true).install(myPanel.getComponent());
        new MyFocusOppositePaneAction(false).install(myPanel.getComponent());

        myEditorSettingsAction = new SetEditorSettingsAction(getTextSettings(), getEditors());
        myEditorSettingsAction.applyDefaults();

        new MyOpenInEditorWithMouseAction().install(getEditors());

        myEditableEditors = TextDiffViewerUtil.getEditableEditors(getEditors());

        TextDiffViewerUtil.checkDifferentDocuments(myRequest);

        boolean editable1 = DiffImplUtil.canMakeWritable(getContent1().getDocument());
        boolean editable2 = DiffImplUtil.canMakeWritable(getContent2().getDocument());
        if (editable1 ^ editable2) {
            ProxyUndoRedoAction.register(getProject(), editable1 ? getEditor1() : getEditor2(), myPanel.getComponent());
        }
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
        for (int i = 0; i < 2; i++) {
            if (forceReadOnly[i]) {
                holders.get(i).getEditor().setViewer(true);
            }
        }

        Side.LEFT.select(holders).getEditor().setVerticalScrollbarOrientation(EditorEx.VERTICAL_SCROLLBAR_LEFT);

        return holders;
    }

    @Override
    @RequiredUIAccess
    protected List<? extends @Nullable Component> createTitles() {
        return UnifiedDiffUtil.createTextTitles(myRequest, getEditors());
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

    @RequiredUIAccess
    protected void updateBridge() {
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

    //
    // Getters
    //

    protected List<? extends DocumentContent> getContents() {
        //noinspection unchecked
        return (List) myRequest.getContents();
    }

    public List<? extends EditorEx> getEditors() {
        if (myEditors == null) {
            myEditors = ContainerUtil.map(getEditorHolders(), holder -> holder.getEditor());
        }
        return myEditors;
    }

    protected List<? extends EditorEx> getEditableEditors() {
        return myEditableEditors;
    }

    public EditorEx getCurrentEditor() {
        return getEditor(getCurrentSide());
    }

    public DocumentContent getCurrentContent() {
        return getContent(getCurrentSide());
    }

    public EditorEx getEditor1() {
        return getEditor(Side.LEFT);
    }

    public EditorEx getEditor2() {
        return getEditor(Side.RIGHT);
    }

    public EditorEx getEditor(Side side) {
        return side.select(getEditors());
    }

    public DocumentContent getContent(Side side) {
        return side.select(getContents());
    }

    public DocumentContent getContent1() {
        return getContent(Side.LEFT);
    }

    public DocumentContent getContent2() {
        return getContent(Side.RIGHT);
    }

    public @Nullable EditorBridge getBridge() {
        return myContentPanel.getBridge();
    }

    //
    // Abstract
    //

    @RequiredUIAccess
    protected LineCol transferPosition(Side baseSide, LineCol position) {
        return new LineCol(transferLine(baseSide, position.line()), position.column());
    }

    @RequiredUIAccess
    protected int transferLine(Side baseSide, int line) {
        return line;
    }

    @RequiredUIAccess
    protected void scrollToLine(Side side, int line) {
        DiffImplUtil.scrollEditor(getEditor(side), line, false);
        setCurrentSide(side);
    }

    //
    // Misc
    //

    @Override
    @RequiredUIAccess
    protected @Nullable Navigatable getNavigatable() {
        Side side = getCurrentSide();

        LineCol position = LineCol.fromCaret(getEditor(side));
        Navigatable navigatable = getContent(side).getNavigatable(position);
        if (navigatable != null) {
            return navigatable;
        }

        LineCol otherPosition = transferPosition(side, position);
        return getContent(side.other()).getNavigatable(otherPosition);
    }

    public static boolean canShowRequest(DiffContext context, DiffRequest request) {
        return canShowRequest(context, request, TextEditorHolder.TextEditorHolderFactory.INSTANCE);
    }

    //
    // Actions
    //

    private class MyFocusOppositePaneAction extends FocusOppositePaneAction {
        public MyFocusOppositePaneAction(boolean scrollToPosition) {
            super(scrollToPosition);
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(AnActionEvent e) {
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

            myContext.requestFocus();
        }
    }

    private class MyOpenInEditorWithMouseAction extends OpenInEditorWithMouseAction {
        @Override
        protected @Nullable Navigatable getNavigatable(Editor editor, int line) {
            Side side = Side.fromValue(getEditors(), editor);
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

    protected abstract class MyInitialScrollPositionHelper extends InitialScrollPositionSupport.TwosideInitialScrollHelper {
        @Override
        protected List<? extends Editor> getEditors() {
            return TwosideTextDiffViewer.this.getEditors();
        }

        @Override
        @RequiredUIAccess
        protected void runWithoutSyncScroll(Runnable action) {
            TwosideTextDiffViewer.this.runWithoutSyncScroll(action);
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
}
