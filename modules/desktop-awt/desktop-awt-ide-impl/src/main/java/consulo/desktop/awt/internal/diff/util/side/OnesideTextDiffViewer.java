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
package consulo.desktop.awt.internal.diff.util.side;

import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorEx;
import consulo.desktop.awt.internal.diff.EditorHolderFactory;
import consulo.desktop.awt.internal.diff.TextEditorHolder;
import consulo.desktop.awt.internal.diff.action.OpenInEditorWithMouseAction;
import consulo.desktop.awt.internal.diff.action.SetEditorSettingsAction;
import consulo.desktop.awt.internal.diff.util.AWTDiffUtil;
import consulo.desktop.awt.internal.diff.util.InitialScrollPositionSupport;
import consulo.desktop.awt.internal.diff.util.TextDiffViewerUtil;
import consulo.diff.DiffContext;
import consulo.diff.DiffDataKeys;
import consulo.diff.content.DocumentContent;
import consulo.diff.impl.internal.TextDiffSettingsHolder;
import consulo.diff.internal.DiffImplUtil;
import consulo.diff.request.ContentDiffRequest;
import consulo.diff.request.DiffRequest;
import consulo.diff.util.LineCol;
import consulo.diff.util.Side;
import consulo.logging.Logger;
import consulo.navigation.Navigatable;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.util.collection.ContainerUtil;
import consulo.util.dataholder.Key;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.util.Collections;
import java.util.List;

public abstract class OnesideTextDiffViewer extends OnesideDiffViewer<TextEditorHolder> {
    public static final Logger LOG = Logger.getInstance(OnesideTextDiffViewer.class);

    
    private final List<? extends EditorEx> myEditableEditors;

    
    protected final SetEditorSettingsAction myEditorSettingsAction;

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
    protected @Nullable JComponent createTitle() {
        List<JComponent> textTitles = AWTDiffUtil.createTextTitles(myRequest, ContainerUtil.list(getEditor(), getEditor()));
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
        new TextDiffViewerUtil.EditorActionsPopup(createEditorPopupActions()).install(getEditors(), myPanel);
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
        protected Navigatable getNavigatable(Editor editor, int line) {
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
    public @Nullable Object getData(Key<?> dataId) {
        if (DiffDataKeys.CURRENT_EDITOR == dataId) {
            return getEditor();
        }
        return super.getData(dataId);
    }

    protected abstract class MyInitialScrollPositionHelper extends InitialScrollPositionSupport.TwosideInitialScrollHelper {
        
        @Override
        protected List<? extends Editor> getEditors() {
            return OnesideTextDiffViewer.this.getEditors();
        }

        @Override
        protected void disableSyncScroll(boolean value) {
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
