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
package consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts;

import consulo.ui.Tree;
import consulo.ui.event.details.InputDetails;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.ActionPopupMenu;

/**
 * @author VISTALL
 * @since 2026-10-10
 */
public final class ArtifactTreeContextMenu {
    private ArtifactTreeContextMenu() {
    }

    public static void install(Tree<?> tree, ActionGroup group) {
        tree.addContextMenuListener(event -> {
            InputDetails inputDetails = event.getInputDetails();
            ActionPopupMenu menu = ActionManager.getInstance().createActionPopupMenu(ActionPlaces.UNKNOWN, group);
            menu.setTargetComponent(tree);
            menu.show(event.getComponent(), inputDetails.getX(), inputDetails.getY());
        });
    }
}
