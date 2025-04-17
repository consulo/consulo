/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.vcs.impl;

import consulo.annotation.component.ExtensionImpl;
import consulo.component.ComponentManager;
import consulo.ui.ex.keymap.KeymapGroupFactory;
import consulo.ide.impl.idea.openapi.keymap.impl.ui.ActionsTreeUtil;
import consulo.ide.impl.idea.openapi.keymap.impl.ui.KeymapGroupImpl;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.ui.ex.action.*;
import consulo.ui.ex.internal.ActionStubBase;
import consulo.ui.ex.keymap.KeyMapBundle;
import consulo.ui.ex.keymap.KeymapExtension;
import consulo.ui.ex.keymap.KeymapGroup;

import java.util.function.Predicate;

/**
 * @author yole
 */
@ExtensionImpl
public class VcsKeymapExtension implements KeymapExtension {
    @Override
    public KeymapGroup createGroup(final Predicate<AnAction> filtered, final ComponentManager project) {
        KeymapGroup result = KeymapGroupFactory.getInstance().createGroup(KeyMapBundle.message("version.control.group.title"));

        AnAction[] versionControlsGroups = getActions("VcsGroup");
        AnAction[] keymapGroups = getActions("Vcs.KeymapGroup");

        for (AnAction action : ContainerUtil.concat(versionControlsGroups, keymapGroups)) {
            addAction(result, action, filtered, false);
        }

        AnAction[] generalActions = getActions("VcsGeneral.KeymapGroup");
        for (AnAction action : generalActions) {
            addAction(result, action, filtered, true);
        }

        result.normalizeSeparators();

        return result;
    }

    private static void addAction(KeymapGroup result, AnAction action, Predicate<AnAction> filtered, boolean forceNonPopup) {
        if (action instanceof ActionGroup) {
            if (forceNonPopup) {
                AnAction[] actions = getActions((ActionGroup)action);
                for (AnAction childAction : actions) {
                    addAction(result, childAction, filtered, true);
                }
            }
            else {
                KeymapGroupImpl subGroup = ActionsTreeUtil.createGroup((ActionGroup)action, false, filtered);
                if (subGroup.getSize() > 0) {
                    result.addGroup(subGroup);
                }
            }
        }
        else if (action instanceof AnSeparator) {
            if (result instanceof KeymapGroupImpl) {
                ((KeymapGroupImpl)result).addSeparator();
            }
        }
        else {
            if (filtered == null || filtered.test(action)) {
                String id = action instanceof ActionStubBase ? ((ActionStubBase)action).getId() : ActionManager.getInstance().getId(action);
                result.addActionId(id);
            }
        }
    }

    private static AnAction[] getActions(String actionGroup) {
        return getActions((ActionGroup)ActionManager.getInstance().getActionOrStub(actionGroup));
    }

    private static AnAction[] getActions(ActionGroup group) {
        return group instanceof DefaultActionGroup
            ? ((DefaultActionGroup)group).getChildActionsOrStubs()
            : group.getChildren(null);
    }
}