/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package consulo.diagram.providers;

import consulo.diagram.DiagramElementsProviderBase;
import consulo.localize.LocalizeValue;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.IdeActions;
import consulo.ui.ex.action.ShortcutSet;

/**
 * @author Konstantin Bulenkov
 */
public abstract class ImplementationsProvider<T> extends DiagramElementsProviderBase<T> {
    @Override
    public ShortcutSet getShortcutSet() {
        return ActionManager.getInstance().getAction(IdeActions.ACTION_GOTO_IMPLEMENTATION).getShortcutSet();
    }

    @Override
    public LocalizeValue getName() {
        return LocalizeValue.localizeTODO("Show Implementations");
    }
}
