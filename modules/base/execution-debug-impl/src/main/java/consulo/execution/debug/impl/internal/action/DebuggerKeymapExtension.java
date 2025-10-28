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
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.IdeActions;
import consulo.ui.ex.keymap.KeymapExtension;
import consulo.ui.ex.keymap.KeymapGroup;
import consulo.ui.ex.keymap.KeymapGroupFactory;
import consulo.ui.ex.keymap.localize.KeyMapLocalize;

import java.util.function.Predicate;

/**
 * @author yole
 */
@ExtensionImpl
public class DebuggerKeymapExtension implements KeymapExtension {
    @Override
    public KeymapGroup createGroup(Predicate<AnAction> filtered, ComponentManager project) {
        return KeymapGroupFactory.getInstance().newBuilder()
            .root(
                KeyMapLocalize.debuggerActionsGroupTitle(),
                IdeActions.GROUP_DEBUGGER,
                PlatformIconGroup.toolwindowsToolwindowdebugger()
            )
            .filter(filtered)
            .addGroup(IdeActions.GROUP_DEBUGGER)
            .build();
    }
}
