/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.execution.debug.impl.internal.ui;

import consulo.annotation.component.ExtensionImpl;
import consulo.execution.debug.XDebuggerActions;
import consulo.localize.LocalizeValue;
import consulo.ui.ex.action.CustomizableActionGroupProvider;

/**
 * @author nik
 */
@ExtensionImpl
public class XDebugTabCustomizableActionGroupProvider extends CustomizableActionGroupProvider {
    @Override
    public void registerGroups(CustomizableActionGroupRegistrar registrar) {
        registrar.addCustomizableActionGroup(XDebuggerActions.TOOL_WINDOW_TOP_TOOLBAR_GROUP, LocalizeValue.localizeTODO("Debug Tool Window Top Toolbar"));
        registrar.addCustomizableActionGroup(XDebuggerActions.TOOL_WINDOW_LEFT_TOOLBAR_GROUP, LocalizeValue.localizeTODO("Debug Tool Window Left Toolbar"));
    }
}
