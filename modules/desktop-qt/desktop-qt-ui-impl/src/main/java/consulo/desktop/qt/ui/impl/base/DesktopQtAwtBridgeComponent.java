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
package consulo.desktop.qt.ui.impl.base;

import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.desktop.qt.ui.impl.QtComponentDelegate;
import consulo.desktop.qt.ui.impl.action.DesktopQtActionContextMenu;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.CustomActionsSchema;
import consulo.ui.ex.awt.ActionPopupMenuInstaller;
import consulo.util.dataholder.Key;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.util.function.Supplier;

/**
 * Stands in for a consulo.ui component wherever platform code that has not been migrated off swing asks for a
 * {@link JComponent} - the editor component, the status bar widgets, focus owners. It is a swing component as far
 * as that code is concerned, and it still hands back the qt widget when it reaches the frontend, so the value
 * survives the round trip instead of being replaced by a stub.
 *
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtAwtBridgeComponent extends JComponent implements ActionPopupMenuInstaller {
    private static final Key<DesktopQtAwtBridgeComponent> BRIDGE = Key.create("desktop.qt.awt.bridge");

    private final Component myComponent;

    private boolean myPopupMenuInstalled;

    private DesktopQtAwtBridgeComponent(Component component) {
        myComponent = component;
    }

    /**
     * One bridge per component - swing code compares the instances it is handed, and a fresh wrapper on every
     * call would make the same component look like a different one each time.
     */
    public static DesktopQtAwtBridgeComponent of(Component component) {
        DesktopQtAwtBridgeComponent bridge = component.getUserData(BRIDGE);
        if (bridge == null) {
            component.putUserData(BRIDGE, bridge = new DesktopQtAwtBridgeComponent(component));
        }
        return bridge;
    }

    public Component getUIComponent() {
        return myComponent;
    }

    public @Nullable QWidget toQtComponent() {
        return myComponent instanceof QtComponentDelegate<?> delegate ? delegate.toQtComponent() : null;
    }

    /**
     * Whether the component this stands in for is on screen. A bridge is never added to a swing hierarchy, so the
     * swing answer is that it has no peer and is therefore not showing - which is what the platform asks before it
     * puts anything over an editor. Code completion, the hints, the tooltips and the brace highlighting all give up
     * on that answer, so it has to come from the frontend instead.
     */
    @Override
    public boolean isShowing() {
        QWidget widget = toQtComponent();
        return widget != null && !widget.isDisposed() && widget.isVisible();
    }

    @Override
    @RequiredUIAccess
    public void installActionPopupMenu(ActionGroup group, String place) {
        installActionPopupMenu(() -> group, place);
    }

    @Override
    @RequiredUIAccess
    public void installActionPopupMenu(String groupId, String place) {
        installActionPopupMenu(
            () -> CustomActionsSchema.getInstance().getCorrectedAction(groupId) instanceof ActionGroup group ? group : null,
            place
        );
    }

    @RequiredUIAccess
    private void installActionPopupMenu(Supplier<ActionGroup> groupSupplier, String place) {
        if (myPopupMenuInstalled) {
            return;
        }
        myPopupMenuInstalled = true;

        DesktopQtActionContextMenu.install(myComponent, groupSupplier, place, this::createDataContext);
    }

    private DataContext createDataContext() {
        DataManager dataManager = DataManager.getInstance();

        // the group is expanded off the ui thread, the providers have to be snapshotted before that
        return dataManager.createAsyncDataContext(dataManager.getDataContext(myComponent));
    }

    @Override
    public String toString() {
        return "DesktopQtAwtBridgeComponent(" + myComponent + ")";
    }
}
