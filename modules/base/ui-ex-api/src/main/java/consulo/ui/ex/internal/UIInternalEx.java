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
package consulo.ui.ex.internal;

import consulo.ui.Component;
import consulo.ui.ListBox;
import consulo.ui.ex.ComboBoxWithCustomPopup;
import consulo.ui.ex.action.AnAction;
import consulo.ui.internal.UIInternal;
import consulo.ui.model.FlatDataModel;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 2026-08-02
 */
public interface UIInternalEx {
    static UIInternalEx get() {
        return (UIInternalEx) UIInternal.get();
    }

    default <E> ComboBoxWithCustomPopup<E> _Components_comboBoxWithCustomPopup(FlatDataModel<E> model) {
        throw new UnsupportedOperationException();
    }

    default <E> ListBox<E> _Components_popupListBox(FlatDataModel<E> model) {
        return ListBox.create(model);
    }

    default void _AnAction_registerCustomShortcutSet(AnAction action, Component component) {
        List<AnAction> actions = component.getUserData(AnAction.ACTIONS_KEY);
        if (actions == null) {
            actions = new ArrayList<>();
            component.putUserData(AnAction.ACTIONS_KEY, actions);
        }

        if (!actions.contains(action)) {
            actions.add(action);
        }
    }

    default void _AnAction_unregisterCustomShortcutSet(AnAction action, Component component) {
        List<AnAction> actions = component.getUserData(AnAction.ACTIONS_KEY);
        if (actions != null) {
            actions.remove(action);
        }
    }
}