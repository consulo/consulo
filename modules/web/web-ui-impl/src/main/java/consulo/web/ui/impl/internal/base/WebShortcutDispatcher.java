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

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.Element;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.Component;
import consulo.ui.UIAccess;
import consulo.ui.event.ComponentEvent;
import consulo.ui.event.details.KeyCode;
import consulo.ui.event.details.KeyStroke;
import consulo.ui.event.details.KeyboardInputDetails;
import consulo.ui.event.details.ModifiedInputDetails;
import consulo.ui.ex.impl.internal.action.UnifiedCustomShortcutDispatcher;
import consulo.ui.Point2D;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.IdeActions;
import consulo.ui.ex.action.KeyboardShortcut;
import consulo.ui.ex.action.Shortcut;
import consulo.web.ui.impl.internal.clipboard.WebClipboardImpl;
import consulo.ui.ex.keymap.Keymap;
import consulo.ui.ex.keymap.KeymapManager;
import consulo.ui.ex.internal.KeymapManagerEx;
import consulo.web.ui.impl.internal.WebLightPopupImpl;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    /**
     * The shape {@link #toCombo} builds for escape with nothing held down.
     */
    private static final String ESCAPE_COMBO = String.valueOf(KeyCode.ESCAPE.key());

    private static final String STROKE_QUEUE = "consulo.web.strokeQueue";

    private WebShortcutDispatcher() {
    }

    public static void installRoot(Component root) {
        if (!(root instanceof ToVaadinComponentWrapper wrapper)) {
            return;
        }

        Map<String, List<KeyStroke>> shortcuts = collectShortcuts();

        Element element = wrapper.toVaadinComponent().getElement();

        // on every attach, not once when the root is built. the root is made per project and outlives the page - a
        // reloaded browser is a new dom holding none of what was pushed into the old one, and a set which is never
        // sent again leaves the page taking no key of the keymap at all
        String combos = String.join("\n", shortcuts.keySet());
        String clipboardCombos = clipboardCombos(shortcuts);
        element.addAttachListener(event -> {
            pushShortcuts(element, combos);
            pushClipboardShortcuts(element, clipboardCombos);
        });
        pushShortcuts(element, combos);
        pushClipboardShortcuts(element, clipboardCombos);

        element.addEventListener("consulo-shortcut", event -> {
            String combo = event.getEventData().path("event.detail.combo").asString("");

            // escape belongs to whatever is floating over the frame before it belongs to the keymap - the desktop
            // runs in the same order, where the hint manager takes the key ahead of the editor action bound to it.
            // a popup which never takes the focus cannot be given the key by the browser, so it is asked here
            if (ESCAPE_COMBO.equals(combo) && WebLightPopupImpl.closeTopEscapable(wrapper.toVaadinComponent().getUI().orElse(null))) {
                return;
            }

            // a paste carries what the browser handed the page inside the gesture. it is staged before the action
            // runs, which is the only ordering that works - the paste handler reads the clipboard synchronously
            stagePasted(
                event.getEventData().path("event.detail.pasteText").asString(""),
                event.getEventData().path("event.detail.pasteHtml").asString("")
            );

            List<KeyStroke> keyStrokes = shortcuts.getOrDefault(combo, List.of());
            UI ui = wrapper.toVaadinComponent().getUI().orElse(null);
            boolean modalContext = ui != null && ui.hasModalComponent();
            UIAccess uiAccess = UIAccess.current();
            if (ui == null) {
                passUnconsumed(element, combo, perform(keyStrokes, root, modalContext), uiAccess);
                return;
            }

            strokeQueue(ui).updateAndGet(previous -> passUnconsumed(
                element,
                combo,
                previous.exceptionally(throwable -> null).thenCompose(ignored -> perform(keyStrokes, root, modalContext)),
                uiAccess
            ));
        })
            .allowInert()
            .addEventData("event.detail.combo")
            .addEventData("event.detail.pasteText")
            .addEventData("event.detail.pasteHtml");
    }

    /**
     * One stroke finishes before the next is offered. Each perform is asynchronous, and two keys arriving back to back
     * would otherwise run their actions concurrently and complete in either order - a paste overtaken by the caret
     * move pressed before it pastes into the old selection. A typed character waits its turn the same way.
     */
    @SuppressWarnings("unchecked")
    private static AtomicReference<CompletableFuture<?>> strokeQueue(UI ui) {
        Object queue = ComponentUtil.getData(ui, STROKE_QUEUE);
        if (queue == null) {
            queue = new AtomicReference<CompletableFuture<?>>(CompletableFuture.completedFuture(null));
            ComponentUtil.setData(ui, STROKE_QUEUE, queue);
        }
        return (AtomicReference<CompletableFuture<?>>) queue;
    }

    @RequiredUIAccess
    public static void runAfterPendingStrokes(@Nullable UI ui, Runnable runnable) {
        AtomicReference<CompletableFuture<?>> queue = ui == null ? null : strokeQueue(ui);
        if (queue == null || queue.get().isDone()) {
            runnable.run();
            return;
        }

        UIAccess uiAccess = UIAccess.current();
        queue.updateAndGet(previous -> previous
            .exceptionally(throwable -> null)
            .thenRunAsync(runnable, uiAccess));
    }

    private static CompletableFuture<Boolean> passUnconsumed(
        Element element,
        String combo,
        CompletableFuture<Boolean> performed,
        UIAccess uiAccess
    ) {
        return performed.whenCompleteAsync((result, error) -> {
            if (error == null && !Boolean.TRUE.equals(result)) {
                element.executeJs("window.consuloShortcuts.passUnconsumed(this, $0)", combo);
            }
        }, uiAccess);
    }

    @RequiredUIAccess
    private static CompletableFuture<Boolean> perform(List<KeyStroke> keyStrokes, Component root, boolean modalContext) {
        List<AnAction> actions = UnifiedCustomShortcutDispatcher.collectKeymapActions(keyStrokes, modalContext);
        if (actions.isEmpty()) {
            return CompletableFuture.completedFuture(Boolean.FALSE);
        }

        ComponentEvent<Component> uiEvent = new ComponentEvent<>(root, toInputDetails(keyStrokes.get(0)));
        return UnifiedCustomShortcutDispatcher.performFirstEnabled(actions, WebFocusTracker.createDataContext(root), uiEvent);
    }

    private static KeyboardInputDetails toInputDetails(KeyStroke keyStroke) {
        EnumSet<ModifiedInputDetails.Modifier> modifiers = EnumSet.noneOf(ModifiedInputDetails.Modifier.class);
        modifiers.addAll(keyStroke.getModifiers());
        return new KeyboardInputDetails(new Point2D(0, 0), new Point2D(0, 0), modifiers, keyStroke.getKeyCode());
    }

    private static void pushShortcuts(Element element, String combos) {
        element.executeJs("window.consuloShortcuts.setShortcuts(this, $0)", combos);
    }

    private static void pushClipboardShortcuts(Element element, String combos) {
        element.executeJs("window.consuloShortcuts.setClipboardShortcuts(this, $0)", combos);
    }

    /**
     * The strokes whose default action in the browser is what hands the page the system clipboard. Only paste for
     * now - copy and cut need the payload to be on the client before the gesture, which is a separate piece.
     */
    private static String clipboardCombos(Map<String, List<KeyStroke>> shortcuts) {
        Keymap keymap = KeymapManager.getInstance().getActiveKeymap();
        if (keymap == null) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        for (Map.Entry<String, List<KeyStroke>> entry : shortcuts.entrySet()) {
            boolean paste = false;
            for (KeyStroke keyStroke : entry.getValue()) {
                paste |= List.of(keymap.getActionIds(keyStroke)).contains(IdeActions.ACTION_PASTE);
            }

            if (paste) {
                if (!result.isEmpty()) {
                    result.append('\n');
                }
                result.append(entry.getKey()).append("=paste");
            }
        }
        return result.toString();
    }

    /**
     * Hands the payload of a paste gesture to the clipboard of this session, so the read which follows is answered
     * without asking the browser - a request of the server arrives outside the activation and is refused.
     */
    @RequiredUIAccess
    private static void stagePasted(String text, String html) {
        if (text.isEmpty() && html.isEmpty()) {
            return;
        }

        if (UIAccess.current().getClipboard() instanceof WebClipboardImpl clipboard) {
            clipboard.stagePasted(text, html);
        }
    }

    private static Map<String, List<KeyStroke>> collectShortcuts() {
        Map<String, List<KeyStroke>> result = new LinkedHashMap<>();

        Keymap keymap = KeymapManager.getInstance().getActiveKeymap();
        if (keymap == null) {
            return result;
        }

        // the bound actions as well as the keymap's own. an action may take its shortcut from another one rather
        // than declare one - EditorDelete borrows $Delete's - and those never appear in getActionIds, which answers
        // only for what the keymap holds directly. the keymap itself builds its keystroke map from exactly this
        // union, so leaving it out is what made delete a key that reached the server and matched nothing there:
        // $Delete alone was offered the stroke, found no delete provider in an editor, and declined in silence
        Set<String> actionIds = new LinkedHashSet<>(List.of(keymap.getActionIds()));
        actionIds.addAll(KeymapManagerEx.getInstanceEx().getBoundActions());

        for (String actionId : actionIds) {
            // resolves the binding for a bound action, so a borrowed shortcut arrives here as its own
            for (Shortcut shortcut : keymap.getShortcuts(actionId)) {
                if (shortcut instanceof KeyboardShortcut keyboardShortcut && keyboardShortcut.getSecondKeyStroke() == null) {
                    KeyStroke keyStroke = keyboardShortcut.getFirstKeyStroke();
                    String combo = toCombo(keyStroke);
                    if (combo != null) {
                        List<KeyStroke> keyStrokes = result.computeIfAbsent(combo, key -> new ArrayList<>());
                        if (!keyStrokes.contains(keyStroke)) {
                            keyStrokes.add(keyStroke);
                        }
                    }
                }
            }
        }

        return result;
    }

    /**
     * The keys awt and the browser number differently. Everything else a shortcut is made of - the letters, the
     * digits, the arrows, the function keys, tab, escape, backspace - carries the same number on both sides, so only
     * these have to be said out loud.
     * <p>
     * A key missing from here is not a shortcut that misfires, it is a shortcut that never arrives: the combo built
     * from the awt number is one the browser never produces, so nothing matches, nothing is prevented, and the
     * editor in the page handles the key itself. That is what delete did - the platform never saw it, and orion cut
     * against its own idea of what was selected.
     * <p>
     * The numbers on the right are the legacy {@code KeyboardEvent.keyCode}, which is what the page reports.
     */
    private static final Map<KeyCode, Integer> DOM_KEY_CODES = Map.ofEntries(
        Map.entry(KeyCode.ENTER, 13),
        Map.entry(KeyCode.DELETE, 46),
        Map.entry(KeyCode.INSERT, 45),
        // the punctuation awt names by character and the browser by position on the keyboard
        Map.entry(KeyCode.SEMICOLON, 186),
        Map.entry(KeyCode.EQUALS, 187),
        Map.entry(KeyCode.COMMA, 188),
        Map.entry(KeyCode.MINUS, 189),
        Map.entry(KeyCode.PERIOD, 190),
        Map.entry(KeyCode.SLASH, 191),
        Map.entry(KeyCode.OPEN_BRACKET, 219),
        Map.entry(KeyCode.BACK_SLASH, 220),
        Map.entry(KeyCode.CLOSE_BRACKET, 221)
    );

    /**
     * The same shape the browser builds from a key event - the modifiers in a fixed order and the key by the code
     * {@code KeyboardEvent.keyCode} of the browser answers with.
     */
    static @Nullable String toCombo(KeyStroke keyStroke) {
        if (keyStroke.isTyped()) {
            return null;
        }

        int keyCode = DOM_KEY_CODES.getOrDefault(keyStroke.getKeyCode(), keyStroke.getKeyCode().key());

        StringBuilder builder = new StringBuilder();
        if (keyStroke.hasModifier(ModifiedInputDetails.Modifier.ALT)) {
            builder.append("ALT+");
        }
        if (keyStroke.hasModifier(ModifiedInputDetails.Modifier.CTRL)) {
            builder.append("CTRL+");
        }
        if (keyStroke.hasModifier(ModifiedInputDetails.Modifier.META)) {
            builder.append("META+");
        }
        if (keyStroke.hasModifier(ModifiedInputDetails.Modifier.SHIFT)) {
            builder.append("SHIFT+");
        }

        return builder.append(keyCode).toString();
    }
}
