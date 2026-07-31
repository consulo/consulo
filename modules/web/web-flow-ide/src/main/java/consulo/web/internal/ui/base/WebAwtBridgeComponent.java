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
package consulo.web.internal.ui.base;

import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.awt.ActionPopupMenuInstaller;
import consulo.util.dataholder.Key;
import consulo.web.internal.ui.action.WebActionContextMenu;
import org.jspecify.annotations.Nullable;

import javax.swing.*;

/**
 * Stands in for a consulo.ui component wherever platform code that has not been migrated off swing asks for a
 * {@link JComponent} - the editor component, the status bar widgets, focus owners. It is a swing component as far
 * as that code is concerned, and it still hands back the vaadin component when it reaches the frontend, so the
 * value survives the round trip instead of being replaced by a stub.
 *
 * @author VISTALL
 */
public class WebAwtBridgeComponent extends JComponent implements ToVaadinComponentWrapper, ActionPopupMenuInstaller {
    private static final Key<WebAwtBridgeComponent> BRIDGE = Key.create("web.awt.bridge");

    private final Component myComponent;

    private @Nullable WebActionContextMenu myPopupMenu;

    private WebAwtBridgeComponent(Component component) {
        myComponent = component;
    }

    /**
     * One bridge per component - swing code compares the instances it is handed, and a fresh wrapper on every
     * call would make the same component look like a different one each time.
     */
    public static WebAwtBridgeComponent of(Component component) {
        WebAwtBridgeComponent bridge = component.getUserData(BRIDGE);
        if (bridge == null) {
            component.putUserData(BRIDGE, bridge = new WebAwtBridgeComponent(component));
        }
        return bridge;
    }

    public Component getUIComponent() {
        return myComponent;
    }

    @Override
    public com.vaadin.flow.component.Component toVaadinComponent() {
        return TargetVaadin.to(myComponent);
    }

    @Override
    @RequiredUIAccess
    public void installActionPopupMenu(ActionGroup group, String place) {
        if (myPopupMenu == null) {
            myPopupMenu = new WebActionContextMenu(toVaadinComponent(), group, place, this::createDataContext);
        }
    }

    @Override
    @RequiredUIAccess
    public void installActionPopupMenu(String groupId, String place) {
        if (myPopupMenu == null) {
            myPopupMenu = new WebActionContextMenu(toVaadinComponent(), groupId, place, this::createDataContext);
        }
    }

    private DataContext createDataContext() {
        DataManager dataManager = DataManager.getInstance();

        // the group is expanded off the ui thread, the providers have to be snapshotted before that
        return dataManager.createAsyncDataContext(dataManager.getDataContext(myComponent));
    }

    @Override
    public String toString() {
        return "WebAwtBridgeComponent(" + myComponent + ")";
    }
}
