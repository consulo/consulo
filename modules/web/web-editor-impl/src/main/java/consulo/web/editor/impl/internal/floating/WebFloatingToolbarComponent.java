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
package consulo.web.editor.impl.internal.floating;

import com.vaadin.flow.component.html.Div;
import consulo.codeEditor.Editor;
import consulo.codeEditor.toolbar.floating.FloatingToolbarComponent;
import consulo.codeEditor.toolbar.floating.FloatingToolbarProvider;
import consulo.dataContext.DataManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.ex.impl.internal.action.UnifiedActionToolbarImpl;
import consulo.web.ui.impl.internal.base.TargetVaadin;

public class WebFloatingToolbarComponent implements FloatingToolbarComponent {
    private static final String SHOWN = "arquill-floating-toolbar-shown";

    private static final String INSTANT = "arquill-floating-toolbar-instant";

    private static final String AUTO_HIDEABLE = "arquill-floating-toolbar-auto";

    private final Div myHolder = new Div();

    private final UnifiedActionToolbarImpl myToolbar;

    private boolean myAutoHideable;

    private boolean myShown;

    @RequiredUIAccess
    public WebFloatingToolbarComponent(FloatingToolbarProvider provider, Editor editor) {
        myToolbar =
            new UnifiedActionToolbarImpl(ActionPlaces.CONTEXT_TOOLBAR, provider.getActionGroup(), ActionToolbar.Style.HORIZONTAL);
        myToolbar.setDataContextSupplier(() -> DataManager.getInstance().createAsyncDataContext(editor.getDataContext()));

        myHolder.addClassName("arquill-floating-toolbar");
        myHolder.add(TargetVaadin.to(myToolbar.getUIComponent()));

        setAutoHideable(provider.isAutoHideable());
    }

    public Div getHolder() {
        return myHolder;
    }

    @Override
    public boolean isAutoHideable() {
        return myAutoHideable;
    }

    @Override
    @RequiredUIAccess
    public void setAutoHideable(boolean autoHideable) {
        myAutoHideable = autoHideable;
        myHolder.setClassName(AUTO_HIDEABLE, autoHideable);
    }

    @Override
    @RequiredUIAccess
    public void scheduleHide() {
        myHolder.removeClassName(INSTANT);
        hideComponent();
    }

    @Override
    @RequiredUIAccess
    public void scheduleShow() {
        myHolder.removeClassName(INSTANT);
        showComponent();
    }

    @Override
    @RequiredUIAccess
    public void hideImmediately() {
        if (!myAutoHideable) {
            myHolder.addClassName(INSTANT);
        }
        hideComponent();
    }

    @RequiredUIAccess
    private void showComponent() {
        myHolder.addClassName(SHOWN);
        if (!myShown) {
            myShown = true;
            myToolbar.updateActionsImmediately();
        }
    }

    @RequiredUIAccess
    private void hideComponent() {
        myHolder.removeClassName(SHOWN);
        if (myShown) {
            myShown = false;
            myToolbar.updateActionsImmediately();
        }
    }
}
