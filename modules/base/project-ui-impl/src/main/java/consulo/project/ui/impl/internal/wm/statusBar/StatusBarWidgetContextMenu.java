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
package consulo.project.ui.impl.internal.wm.statusBar;

import consulo.dataContext.UiDataProvider;
import consulo.project.ui.wm.StatusBarWidget;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.details.InputDetails;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.ActionPopupMenu;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-10-17
 */
public final class StatusBarWidgetContextMenu {
    private StatusBarWidgetContextMenu() {
    }

    @RequiredUIAccess
    public static void install(Component component, @Nullable StatusBarWidget widget) {
        if (widget != null) {
            component.putUserData(UiDataProvider.KEY, sink -> sink.set(StatusBarWidget.KEY, widget));
        }

        component.addContextMenuListener(event -> show(component, event.getInputDetails()));
    }

    @RequiredUIAccess
    public static void show(Component target, @Nullable InputDetails inputDetails) {
        ActionManager actionManager = ActionManager.getInstance();

        ActionPopupMenu menu = actionManager.createActionPopupMenu(
            ActionPlaces.STATUS_BAR_PLACE,
            actionManager.getAction(StatusBarWidgetsActionGroup.class)
        );
        menu.setTargetComponent(target);
        menu.show(target, inputDetails == null ? 0 : inputDetails.getX(), inputDetails == null ? 0 : inputDetails.getY());
    }
}
