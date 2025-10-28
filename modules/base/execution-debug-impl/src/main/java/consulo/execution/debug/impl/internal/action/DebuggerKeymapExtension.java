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
package consulo.execution.debug.impl.internal.action;

import consulo.annotation.component.ExtensionImpl;
import consulo.component.ComponentManager;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.IdeActions;
import consulo.ui.ex.internal.ActionStubBase;
import consulo.ui.ex.keymap.KeymapExtension;
import consulo.ui.ex.keymap.KeymapGroup;
import consulo.ui.ex.keymap.KeymapGroupFactory;
import consulo.ui.ex.keymap.localize.KeyMapLocalize;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

/**
 * @author yole
 */
@ExtensionImpl
public class DebuggerKeymapExtension implements KeymapExtension {
    @Override
    public KeymapGroup createGroup(Predicate<AnAction> filtered, ComponentManager project) {
        ActionManager actionManager = ActionManager.getInstance();
        DefaultActionGroup debuggerGroup = (DefaultActionGroup)actionManager.getActionOrStub(IdeActions.GROUP_DEBUGGER);
        AnAction[] debuggerActions = debuggerGroup.getChildActionsOrStubs();

        List<String> ids = new ArrayList<>();
        for (AnAction debuggerAction : debuggerActions) {
            String actionId =
                debuggerAction instanceof ActionStubBase actionStubBase ? actionStubBase.getId() : actionManager.getId(debuggerAction);
            if (filtered == null || filtered.test(debuggerAction)) {
                ids.add(actionId);
            }
        }

        Collections.sort(ids);
        KeymapGroup group = KeymapGroupFactory.getInstance().createGroup(
            KeyMapLocalize.debuggerActionsGroupTitle(),
            IdeActions.GROUP_DEBUGGER,
            PlatformIconGroup.toolwindowsToolwindowdebugger()
        );
        for (String id : ids) {
            group.addActionId(id);
        }

        return group;
    }
}
