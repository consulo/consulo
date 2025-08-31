/*
 * Copyright 2013-2017 consulo.io
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
package consulo.project.ui.impl.internal.wm;

import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentFactory;
import consulo.ui.ex.content.ContentManager;
import consulo.util.concurrent.AsyncResult;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 25-Sep-17
 */
public class UnifiedToolWindowImpl extends ToolWindowBase {
    private Component myComponent;

    @RequiredUIAccess
    public UnifiedToolWindowImpl(ToolWindowManagerBase toolWindowManager, String id, LocalizeValue displayName, boolean canCloseContent, @Nullable Object component, boolean avaliable) {
        super(toolWindowManager, id, displayName, canCloseContent, component, avaliable);
    }

    @Override
    public AsyncResult<Void> getReady(@Nonnull Object requestor) {
        return AsyncResult.resolved(null);
    }

    @RequiredUIAccess
    @Override
    protected void init(boolean canCloseContent, @Nullable Object component) {
        ContentFactory contentFactory = ContentFactory.getInstance();
        ContentManager contentManager = myContentManager = contentFactory.createContentManager(new UnifiedToolWindowContentUI(this), canCloseContent, myToolWindowManager.getProject());

        if (component != null) {
            Content content = contentFactory.createUIContent((Component) component, "", false);
            contentManager.addContent(content);
            contentManager.setSelectedContent(content, false);
        }

        myComponent = contentManager.getUIComponent();
    }

    @Nonnull
    @Override
    public Component getUIComponent() {
        return myComponent;
    }

    @Override
    public void stretchWidth(int value) {

    }

    @Override
    public void stretchHeight(int value) {

    }

    @Override
    public void setTabDoubleClickActions(@Nonnull AnAction... actions) {

    }
}
