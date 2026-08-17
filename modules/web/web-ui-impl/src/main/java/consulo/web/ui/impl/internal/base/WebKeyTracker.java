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
import consulo.ui.event.details.KeyCode;
import consulo.ui.event.details.ModifiedInputDetails.Modifier;
import consulo.ui.ex.impl.internal.keymap.ModifierKeyDoubleClickHandlerBase;
import consulo.ui.ex.keymap.internal.ModifierKeyDoubleClickHandler;

import java.util.EnumSet;
import java.util.Set;

/**
 * Feeds the keys of the browser to the platform. {@code frontend/shortcuts.js} decides which of them are worth
 * a round trip - the modifiers always, anything else only while a modifier sequence is running - and reports
 * each as one {@code consulo-key} event.
 * <p/>
 * Deliberately not debounced: the double click is detected by the intervals between the keys.
 *
 * @author VISTALL
 * @since 2026-08-01
 */
public final class WebKeyTracker {
    private WebKeyTracker() {
    }

    /**
     * Idempotent on the browser side - the script keeps a flag per element and a second call does nothing, so
     * re-running it on a dom which already has the listeners costs one message and changes nothing.
     */
    private static void installListeners(Element element) {
        element.executeJs("window.consuloShortcuts.install(this)");
    }

    public static void installRoot(Component root) {
        if (!(root instanceof ToVaadinComponentWrapper wrapper)) {
            return;
        }

        Element element = wrapper.toVaadinComponent().getElement();

        // on every attach, not once when the root is built. the root is made per project and outlives the page, so
        // reloading the browser hands the same component tree to a dom which holds none of what was pushed into the
        // old one - and the listeners this installs are what every shortcut of the ide arrives through
        element.addAttachListener(event -> installListeners(element));
        installListeners(element);

        element.addEventListener("consulo-key", event -> {
            int keyCode = event.getEventData().path("event.detail.keyCode").asInt(0);
            String modifiers = event.getEventData().path("event.detail.modifiers").asString("");
            boolean pressed = event.getEventData().path("event.detail.pressed").asBoolean(false);
            long time = event.getEventData().path("event.detail.time").asLong(0);

            processKey(keyCode, modifiers, pressed, time);
        })
            .addEventData("event.detail.keyCode")
            .addEventData("event.detail.modifiers")
            .addEventData("event.detail.pressed")
            .addEventData("event.detail.time");

        if (ModifierKeyDoubleClickHandler.getInstance() instanceof ModifierKeyDoubleClickHandlerBase handler) {
            handler.setDataContextSupplier(() -> WebFocusTracker.createDataContext(root));
        }
    }

    private static void processKey(int keyCode, String modifiers, boolean pressed, long time) {
        if (!(ModifierKeyDoubleClickHandler.getInstance() instanceof ModifierKeyDoubleClickHandlerBase handler)) {
            return;
        }

        handler.processKey(KeyCode.of(keyCode), toModifiers(modifiers), pressed, time);
    }

    private static Set<Modifier> toModifiers(String modifiers) {
        EnumSet<Modifier> result = EnumSet.noneOf(Modifier.class);
        if (modifiers.isEmpty()) {
            return result;
        }

        for (String modifier : modifiers.split(",")) {
            result.add(Modifier.valueOf(modifier));
        }
        return result;
    }
}
