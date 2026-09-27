/*
 * Copyright 2013-2026 consulo.io
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
package consulo.it.internal.editor;

import consulo.codeEditor.Editor;
import consulo.codeEditor.impl.CodeEditorBase;
import consulo.codeEditor.impl.MarkupModelImpl;
import consulo.codeEditor.internal.ErrorStripeListener;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.language.editor.impl.internal.markup.EditorMarkupModel;
import consulo.language.editor.impl.internal.markup.ErrorStripTooltipRendererProvider;
import consulo.language.editor.impl.internal.markup.ErrorStripeRenderer;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.PopupHandler;
import org.jspecify.annotations.Nullable;

/**
 * Editor markup model for headless integration tests. The range highlighter storage, the listener
 * dispatch and the overlapping queries are all inherited from {@link MarkupModelImpl} and stay fully
 * functional, since the daemon writes its highlight infos through them and the tests read them back.
 * Only the error stripe side of {@link EditorMarkupModel} is degenerate - there is no stripe surface,
 * so the requested state is kept and nothing is painted.
 *
 * @author VISTALL
 */
public class HeadlessMarkupModel extends MarkupModelImpl implements EditorMarkupModel {
    private final CodeEditorBase myEditor;

    private boolean myErrorStripeVisible;

    private @Nullable ErrorStripeRenderer myErrorStripeRenderer;

    private @Nullable ErrorStripTooltipRendererProvider myTooltipRendererProvider;

    private int myMinMarkHeight = 1;

    public HeadlessMarkupModel(CodeEditorBase editor) {
        super(editor.getDocument());
        myEditor = editor;
    }

    @Override
    public Editor getEditor() {
        return myEditor;
    }

    @Override
    public void setErrorStripeVisible(boolean val) {
        myErrorStripeVisible = val;
    }

    @Override
    public boolean isErrorStripeVisible() {
        return myErrorStripeVisible;
    }

    @RequiredUIAccess
    @Override
    public void setErrorStripeRenderer(ErrorStripeRenderer renderer) {
        if (myErrorStripeRenderer instanceof Disposable disposable) {
            Disposer.dispose(disposable);
        }

        myErrorStripeRenderer = renderer;
    }

    @Override
    public @Nullable ErrorStripeRenderer getErrorStripeRenderer() {
        return myErrorStripeRenderer;
    }

    @Override
    public void dispose() {
        if (myErrorStripeRenderer instanceof Disposable disposable) {
            Disposer.dispose(disposable);
        }

        myErrorStripeRenderer = null;

        super.dispose();
    }

    @Override
    public void addErrorMarkerListener(ErrorStripeListener listener, Disposable parent) {
    }

    @RequiredUIAccess
    @Override
    public void setErrorPanelPopupHandler(PopupHandler handler) {
    }

    @Override
    public void setErrorStripTooltipRendererProvider(ErrorStripTooltipRendererProvider provider) {
        myTooltipRendererProvider = provider;
    }

    @Override
    public @Nullable ErrorStripTooltipRendererProvider getErrorStripTooltipRendererProvider() {
        return myTooltipRendererProvider;
    }

    @Override
    public void setMinMarkHeight(int minMarkHeight) {
        myMinMarkHeight = minMarkHeight;
    }

    @Override
    public int getMinMarkHeight() {
        return myMinMarkHeight;
    }

    @Override
    public void repaintTrafficLightIcon() {
    }
}
