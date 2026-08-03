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
package consulo.web.internal.ui.base;

import com.vaadin.flow.dom.Element;
import consulo.ui.Component;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.KeyboardShortcut;
import consulo.ui.ex.action.Shortcut;
import consulo.ui.ex.impl.internal.action.MenuItemPresentationFactory;
import consulo.ui.ex.keymap.Keymap;
import consulo.ui.ex.keymap.KeymapManager;
import consulo.web.internal.ui.action.WebActionMenuExpander;

import javax.swing.KeyStroke;
import java.awt.event.InputEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Runs the keyboard shortcuts of the keymap. The browser owns some of the same combinations - ctrl s saves the
 * page - so the combinations the keymap uses are handed to {@code frontend/shortcuts.js}, which takes those and
 * leaves everything else to the page.
 * <p/>
 * Only single stroke shortcuts are handled; a two stroke one needs a state machine of its own.
 *
 * @author VISTALL
 * @since 2026-08-02
 */
public final class WebShortcutDispatcher {
    private WebShortcutDispatcher() {
    }

    public static void installRoot(Component root) {
        if (!(root instanceof ToVaadinComponentWrapper wrapper)) {
            return;
        }

        Map<String, List<String>> shortcuts = collectShortcuts();

        Element element = wrapper.toVaadinComponent().getElement();

        element.executeJs("window.consuloShortcuts.setShortcuts(this, $0)", String.join("\n", shortcuts.keySet()));

        element.addEventListener("consulo-shortcut", event -> {
            String combo = event.getEventData().path("event.detail.combo").asString("");

            perform(shortcuts.getOrDefault(combo, List.of()), root);
        }).addEventData("event.detail.combo");
    }

    private static void perform(List<String> actionIds, Component root) {
        ActionManager actionManager = ActionManager.getInstance();
        MenuItemPresentationFactory presentationFactory = new MenuItemPresentationFactory();

        for (String actionId : actionIds) {
            AnAction action = actionManager.getAction(actionId);
            if (action != null) {
                // the update of the action decides whether it runs, the same way it does for a menu item
                WebActionMenuExpander.performAction(
                    action,
                    WebFocusTracker.createDataContext(root),
                    ActionPlaces.MAIN_MENU,
                    presentationFactory,
                    // a shortcut is not pointed at anything - the awt key dispatcher leaves this out as well
                    null
                );
                return;
            }
        }
    }

    private static Map<String, List<String>> collectShortcuts() {
        Map<String, List<String>> result = new LinkedHashMap<>();

        Keymap keymap = KeymapManager.getInstance().getActiveKeymap();
        if (keymap == null) {
            return result;
        }

        for (String actionId : keymap.getActionIds()) {
            for (Shortcut shortcut : keymap.getShortcuts(actionId)) {
                if (shortcut instanceof KeyboardShortcut keyboardShortcut && keyboardShortcut.getSecondKeyStroke() == null) {
                    String combo = toCombo(keyboardShortcut.getFirstKeyStroke());
                    if (combo != null) {
                        result.computeIfAbsent(combo, key -> new ArrayList<>()).add(actionId);
                    }
                }
            }
        }

        return result;
    }

    /**
     * The same shape the browser builds from a key event - the modifiers in a fixed order and the key by its awt
     * code, which is what {@code KeyEvent.keyCode} of the browser answers for the keys a shortcut is made of.
     */
    private static String toCombo(KeyStroke keyStroke) {
        int keyCode = keyStroke.getKeyCode();
        if (keyCode == 0) {
            return null;
        }

        int modifiers = keyStroke.getModifiers();

        StringBuilder builder = new StringBuilder();
        if (isSet(modifiers, InputEvent.ALT_DOWN_MASK, InputEvent.ALT_MASK)) {
            builder.append("ALT+");
        }
        if (isSet(modifiers, InputEvent.CTRL_DOWN_MASK, InputEvent.CTRL_MASK)) {
            builder.append("CTRL+");
        }
        if (isSet(modifiers, InputEvent.META_DOWN_MASK, InputEvent.META_MASK)) {
            builder.append("META+");
        }
        if (isSet(modifiers, InputEvent.SHIFT_DOWN_MASK, InputEvent.SHIFT_MASK)) {
            builder.append("SHIFT+");
        }

        return builder.append(keyCode).toString();
    }

    @SuppressWarnings("deprecation")
    private static boolean isSet(int modifiers, int downMask, int legacyMask) {
        return (modifiers & (downMask | legacyMask)) != 0;
    }
}
