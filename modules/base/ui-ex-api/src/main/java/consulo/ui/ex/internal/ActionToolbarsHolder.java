/*
 * Copyright 2013-2023 consulo.io
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
package consulo.ui.ex.internal;

import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionButton;
import consulo.ui.ex.action.ActionToolbar;

import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author VISTALL
 * @since 15/01/2023
 */
public class ActionToolbarsHolder {
    private static final Set<ActionToolbar> ourToolbars = ConcurrentHashMap.newKeySet();

    @RequiredUIAccess
    public static void updateAllToolbarsImmediately() {
        for (ActionToolbar toolbar : new ArrayList<>(ourToolbars)) {
            toolbar.updateActionsImmediately();

            ((ActionToolbarEx) toolbar).forEachButton(b -> {
                if (b instanceof ActionButton button) {
                    button.updateToolTipText();
                    button.updateIcon();
                }
            });
        }
    }

    public static void add(ActionToolbar toolbarEx) {
        ourToolbars.add(toolbarEx);
    }

    public static void remove(ActionToolbar toolbarEx) {
        ourToolbars.remove(toolbarEx);
    }

    public static boolean contains(ActionToolbar toolbar) {
        return ourToolbars.contains(toolbar);
    }
}
