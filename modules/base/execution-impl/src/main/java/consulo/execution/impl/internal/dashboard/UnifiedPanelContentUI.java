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
package consulo.execution.impl.internal.dashboard;

import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentManager;
import consulo.ui.ex.content.ContentUI;
import consulo.ui.ex.content.event.ContentManagerEvent;
import consulo.ui.ex.content.event.ContentManagerListener;
import consulo.ui.layout.DockLayout;
import org.jspecify.annotations.Nullable;

/**
 * Shows the selected content in a panel - the counterpart of the swing {@link PanelContentUI}.
 *
 * @author VISTALL
 * @since 2026-09-24
 */
final class UnifiedPanelContentUI implements ContentUI {
    private @Nullable DockLayout myPanel;
    private @Nullable ContentManager myContentManager;
    private @Nullable Component myShownComponent;

    UnifiedPanelContentUI() {
    }

    @RequiredUIAccess
    @Override
    public Component getUIComponent() {
        return initUI();
    }

    @Override
    public void setManager(ContentManager manager) {
        assert myContentManager == null;
        myContentManager = manager;
        manager.addContentManagerListener(new ContentManagerListener() {
            @RequiredUIAccess
            @Override
            public void selectionChanged(ContentManagerEvent event) {
                initUI();
                if (ContentManagerEvent.ContentOperation.add == event.getOperation()) {
                    showContent(event.getContent());
                }
                else if (ContentManagerEvent.ContentOperation.remove == event.getOperation()) {
                    hideContent();
                }
            }
        });
    }

    @RequiredUIAccess
    private DockLayout initUI() {
        if (myPanel == null) {
            myPanel = DockLayout.create();
        }
        return myPanel;
    }

    @RequiredUIAccess
    private void showContent(Content content) {
        Component component = content.getUIComponent();
        if (component != null && component != myShownComponent) {
            myShownComponent = component;
            initUI().center(component);
        }
    }

    @RequiredUIAccess
    private void hideContent() {
        myShownComponent = null;
        initUI().removeAll();
    }

    @Override
    public boolean isSingleSelection() {
        return true;
    }

    @Override
    public boolean isToSelectAddedContent() {
        return true;
    }

    @Override
    public boolean canBeEmptySelection() {
        return false;
    }

    @Override
    public void beforeDispose() {
    }

    @Override
    public boolean canChangeSelectionTo(Content content, boolean implicit) {
        return true;
    }

    @Override
    public String getCloseActionName() {
        return "";
    }

    @Override
    public String getCloseAllButThisActionName() {
        return "";
    }

    @Override
    public String getPreviousContentActionName() {
        return "";
    }

    @Override
    public String getNextContentActionName() {
        return "";
    }

    @Override
    public void dispose() {
    }
}
