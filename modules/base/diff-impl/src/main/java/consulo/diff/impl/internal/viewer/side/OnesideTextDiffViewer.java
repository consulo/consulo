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
import consulo.dataContext.DataSink;
import consulo.diff.DiffContext;
import consulo.diff.DiffDataKeys;
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
import consulo.diff.request.ContentDiffRequest;
import consulo.diff.request.DiffRequest;
import consulo.diff.util.LineCol;
import consulo.diff.util.Side;
import consulo.logging.Logger;
import consulo.navigation.Navigatable;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.util.collection.ContainerUtil;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public abstract class OnesideTextDiffViewer extends OnesideDiffViewer<TextEditorHolder> {
    public static final Logger LOG = Logger.getInstance(OnesideTextDiffViewer.class);

    private final List<? extends EditorEx> myEditableEditors;

    protected final SetEditorSettingsAction myEditorSettingsAction;

    @RequiredUIAccess
    public OnesideTextDiffViewer(DiffContext context, ContentDiffRequest request) {
        super(context, request, TextEditorHolder.TextEditorHolderFactory.INSTANCE);

        myEditableEditors = TextDiffViewerUtil.getEditableEditors(getEditors());

        myEditorSettingsAction = new SetEditorSettingsAction(getTextSettings(), getEditors());
        myEditorSettingsAction.applyDefaults();

        new MyOpenInEditorWithMouseAction().install(getEditors());
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

    @Override
    protected TextEditorHolder createEditorHolder(EditorHolderFactory<TextEditorHolder> factory) {
        TextEditorHolder holder = super.createEditorHolder(factory);

        boolean[] forceReadOnly = TextDiffViewerUtil.checkForceReadOnly(myContext, myRequest);
        if (forceReadOnly[0]) {
            holder.getEditor().setViewer(true);
        }

        return holder;
    }

    @Override
    @RequiredUIAccess
    protected @Nullable Component createTitle() {
        List<@Nullable Component> textTitles = UnifiedDiffUtil.createTextTitles(myRequest, ContainerUtil.list(getEditor(), getEditor()));
        return getSide().select(textTitles);
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

    //
    // Listeners
    //

    @RequiredUIAccess
    protected void installEditorListeners() {
        new TextDiffViewerUtil.EditorActionsPopup(createEditorPopupActions()).install(getEditors(), myPanel.getComponent());
    }

    @RequiredUIAccess
    protected void destroyEditorListeners() {
    }

    //
    // Getters
    //

    public List<? extends EditorEx> getEditors() {
        return Collections.singletonList(getEditor());
    }

    protected List<? extends EditorEx> getEditableEditors() {
        return myEditableEditors;
    }

    public EditorEx getEditor() {
        return getEditorHolder().getEditor();
    }

    @Override
    public DocumentContent getContent() {
        //noinspection unchecked
        return (DocumentContent) super.getContent();
    }

    //
    // Abstract
    //

    @RequiredUIAccess
    protected void scrollToLine(int line) {
        DiffImplUtil.scrollEditor(getEditor(), line, false);
    }

    //
    // Misc
    //

    @Override
    protected @Nullable Navigatable getNavigatable() {
        return getContent().getNavigatable(LineCol.fromCaret(getEditor()));
    }

    public static boolean canShowRequest(DiffContext context, DiffRequest request) {
        return canShowRequest(context, request, TextEditorHolder.TextEditorHolderFactory.INSTANCE);
    }

    //
    // Actions
    //

    private class MyOpenInEditorWithMouseAction extends OpenInEditorWithMouseAction {
        @Override
        protected @Nullable Navigatable getNavigatable(Editor editor, int line) {
            if (editor != getEditor()) {
                return null;
            }
            return getContent().getNavigatable(new LineCol(line));
        }
    }

    //
    // Helpers
    //

    @Override
    public void uiDataSnapshot(DataSink sink) {
        super.uiDataSnapshot(sink);
        sink.set(DiffDataKeys.CURRENT_EDITOR, getEditor());
    }

    protected abstract class MyInitialScrollPositionHelper extends InitialScrollPositionSupport.TwosideInitialScrollHelper {
        @Override
        protected List<? extends Editor> getEditors() {
            return OnesideTextDiffViewer.this.getEditors();
        }

        @Override
        @RequiredUIAccess
        protected void runWithoutSyncScroll(Runnable action) {
            action.run();
        }

        @Override
        @RequiredUIAccess
        protected boolean doScrollToLine() {
            if (myScrollToLine == null) {
                return false;
            }
            Side side = myScrollToLine.first;
            if (side != getSide()) {
                return false;
            }

            scrollToLine(myScrollToLine.second);
            return true;
        }
    }
}
