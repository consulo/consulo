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
package consulo.web.ui.impl.internal.base;

import com.vaadin.flow.dom.Element;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.KeyboardShortcut;
import consulo.ui.ex.action.Shortcut;
import consulo.ui.ex.impl.internal.action.UnifiedCustomShortcutDispatcher;
import consulo.util.dataholder.Key;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2026-10-02
 */
public final class WebComponentShortcuts {
    private static final String ATTRIBUTE = "consulo-component-shortcuts";
    private static final String EVENT = "consulo-component-shortcut";
    private static final String COMBO_DATA = "event.detail.combo";

    private static final Key<Boolean> LISTENER_INSTALLED = Key.create("WebComponentShortcuts.listenerInstalled");

    private WebComponentShortcuts() {
    }

    @RequiredUIAccess
    public static void update(Component component) {
        if (!(component instanceof ToVaadinComponentWrapper wrapper)) {
            return;
        }

        Element element = wrapper.toVaadinComponent().getElement();

        Set<String> combos = new LinkedHashSet<>();
        List<AnAction> actions = component.getUserData(AnAction.ACTIONS_KEY);
        if (actions != null) {
            for (AnAction action : actions) {
                for (Shortcut shortcut : action.getShortcutSet().getShortcuts()) {
                    if (shortcut instanceof KeyboardShortcut keyboardShortcut && keyboardShortcut.getSecondKeyStroke() == null) {
                        String combo = WebShortcutDispatcher.toCombo(keyboardShortcut.getFirstKeyStroke());
                        if (combo != null) {
                            combos.add(combo);
                        }
                    }
                }
            }
        }

        if (combos.isEmpty()) {
            element.removeAttribute(ATTRIBUTE);
        }
        else {
            element.setAttribute(ATTRIBUTE, String.join("\n", combos));
        }

        if (component.getUserData(LISTENER_INSTALLED) == null) {
            component.putUserData(LISTENER_INSTALLED, Boolean.TRUE);

            element.addEventListener(EVENT, event -> perform(component, event.getEventData().path(COMBO_DATA).asString("")))
                .addEventData(COMBO_DATA);
        }
    }

    @RequiredUIAccess
    private static void perform(Component component, String combo) {
        List<Component> components = new ArrayList<>();
        for (Component current = component; current != null; current = current.getParent()) {
            components.add(current);
        }

        List<AnAction> actions =
            UnifiedCustomShortcutDispatcher.collectActions(components, keyStroke -> combo.equals(WebShortcutDispatcher.toCombo(keyStroke)));
        if (actions.isEmpty()) {
            return;
        }

        UnifiedCustomShortcutDispatcher.performFirstEnabled(actions, WebFocusTracker.createDataContext(component));
    }
}
