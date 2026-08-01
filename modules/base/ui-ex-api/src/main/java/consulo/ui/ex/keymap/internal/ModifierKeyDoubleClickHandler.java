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
package consulo.ui.ex.keymap.internal;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.ui.event.details.KeyCode;
import consulo.ui.event.details.ModifiedInputDetails.Modifier;
import org.jspecify.annotations.Nullable;

/**
 * Shortcuts of the shape modifier-double-click, optionally followed by a key - a double shift opens search
 * everywhere, and control-control-up clones a caret.
 * <p/>
 * Where the keys come from is up to the frontend; what a double click is, is not - the timings were tuned for
 * search everywhere and are the same everywhere.
 *
 * @author VISTALL
 */
@ServiceAPI(value = ComponentScope.APPLICATION, lazy = false)
public interface ModifierKeyDoubleClickHandler {
    static ModifierKeyDoubleClickHandler getInstance() {
        return Application.get().getInstance(ModifierKeyDoubleClickHandler.class);
    }

    /**
     * @param actionId                action to trigger on modifier+modifier[+actionKey]
     * @param modifier                the modifier to double click
     * @param actionKey               key pressed while the modifier is held down the second time, or null to
     *                                trigger on the bare double click
     * @param skipIfActionHasShortcut do not invoke the action if the keymap already binds a shortcut to it
     */
    void registerAction(String actionId, Modifier modifier, @Nullable KeyCode actionKey, boolean skipIfActionHasShortcut);

    default void registerAction(String actionId, Modifier modifier, @Nullable KeyCode actionKey) {
        registerAction(actionId, modifier, actionKey, true);
    }

    void unregisterAction(String actionId);

    boolean isRunningAction();
}
