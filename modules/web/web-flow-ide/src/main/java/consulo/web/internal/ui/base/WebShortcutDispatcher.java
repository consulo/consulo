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
import consulo.dataContext.DataContext;
import consulo.logging.Logger;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.Component;
import consulo.ui.UIAccess;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.IdeActions;
import consulo.ui.ex.action.KeyboardShortcut;
import consulo.ui.ex.action.Shortcut;
import consulo.web.internal.ui.clipboard.WebClipboardImpl;
import consulo.ui.ex.impl.internal.action.MenuItemPresentationFactory;
import consulo.ui.ex.keymap.Keymap;
import consulo.ui.ex.keymap.KeymapManager;
import consulo.ui.ex.internal.KeymapManagerEx;
import consulo.web.internal.ui.WebLightPopupImpl;
import consulo.web.internal.ui.action.WebActionMenuExpander;

import javax.swing.KeyStroke;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.ArrayList;
import java.util.Comparator;
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
    private static final Logger LOG = Logger.getInstance(WebShortcutDispatcher.class);

    /**
     * The shape {@link #toCombo} builds for escape with nothing held down.
     */
    private static final String ESCAPE_COMBO = String.valueOf(KeyEvent.VK_ESCAPE);

    private WebShortcutDispatcher() {
    }

    public static void installRoot(Component root) {
        if (!(root instanceof ToVaadinComponentWrapper wrapper)) {
            return;
        }

        Map<String, List<String>> shortcuts = collectShortcuts();

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

        // one stroke finishes before the next is offered. each perform is asynchronous, and two keys arriving
        // back to back would otherwise run their actions concurrently and complete in either order - a paste
        // overtaken by the caret move pressed before it pastes into the old selection. the awt frontend gets
        // this ordering for free from the single event queue
        AtomicReference<CompletableFuture<?>> strokeQueue = new AtomicReference<>(CompletableFuture.completedFuture(null));

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

            List<String> actionIds = shortcuts.getOrDefault(combo, List.of());
            strokeQueue.updateAndGet(previous -> previous
                .exceptionally(throwable -> null)
                .thenCompose(ignored -> perform(actionIds, root)));
        })
            .addEventData("event.detail.combo")
            .addEventData("event.detail.pasteText")
            .addEventData("event.detail.pasteHtml");
    }

    private static CompletableFuture<Void> perform(List<String> actionIds, Component root) {
        ActionManager actionManager = ActionManager.getInstance();

        List<AnAction> actions = new ArrayList<>();
        for (String actionId : actionIds) {
            AnAction action = actionManager.getAction(actionId);
            if (action != null) {
                actions.add(action);
            }
        }

        // the order the keymap happens to answer in says nothing about which action a stroke belongs to - the weight
        // does, the same way IdeKeyEventDispatcher sorts before it offers the stroke to anything
        actions.sort(Comparator.comparing(AnAction::getExecuteWeight).reversed());

        return performFirstEnabled(actions, 0, root, new MenuItemPresentationFactory());
    }

    /**
     * Offers the stroke to each action bound to it until one takes it, which is what
     * {@code IdeKeyEventDispatcher#performFirstEnabledAsync} does on the desktop.
     * <p/>
     * A shortcut belongs to more than one action - ctrl space is code completion in an editor and a changes view
     * action elsewhere - and which one the user meant is decided by which of them is enabled where they are. Running
     * the first action which merely exists gives the stroke to whichever the keymap listed first, and the one the
     * user wanted never sees it.
     */
    @RequiredUIAccess
    private static CompletableFuture<Void> performFirstEnabled(
        List<AnAction> actions,
        int index,
        Component root,
        MenuItemPresentationFactory presentationFactory
    ) {
        if (index >= actions.size()) {
            return CompletableFuture.completedFuture(null);
        }

        AnAction action = actions.get(index);

        return WebActionMenuExpander.performActionAsync(
                action,
                WebFocusTracker.createDataContext(root),
                ActionPlaces.MAIN_MENU,
                presentationFactory,
                null
            )
            .handle((performed, throwable) -> {
                // an action which threw is not an action which said no. passing the stroke on as if it had
                // declined loses the failure and leaves the key looking dead - the arrow keys moved no caret for
                // exactly this reason, the thread assert inside the caret model arriving here as a throwable
                if (throwable != null) {
                    LOG.error("Shortcut action " + ActionManager.getInstance().getId(action) + " failed", throwable);
                    return CompletableFuture.<Void>completedFuture(null);
                }

                if (!Boolean.TRUE.equals(performed)) {
                    return performFirstEnabled(actions, index + 1, root, presentationFactory);
                }
                return CompletableFuture.<Void>completedFuture(null);
            })
            .thenCompose(next -> next);
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
    private static String clipboardCombos(Map<String, List<String>> shortcuts) {
        StringBuilder result = new StringBuilder();
        for (Map.Entry<String, List<String>> entry : shortcuts.entrySet()) {
            if (entry.getValue().contains(IdeActions.ACTION_PASTE)) {
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

    private static Map<String, List<String>> collectShortcuts() {
        Map<String, List<String>> result = new LinkedHashMap<>();

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
    private static final Map<Integer, Integer> DOM_KEY_CODES = Map.ofEntries(
        Map.entry(KeyEvent.VK_ENTER, 13),
        Map.entry(KeyEvent.VK_DELETE, 46),
        Map.entry(KeyEvent.VK_INSERT, 45),
        // the punctuation awt names by character and the browser by position on the keyboard
        Map.entry(KeyEvent.VK_SEMICOLON, 186),
        Map.entry(KeyEvent.VK_EQUALS, 187),
        Map.entry(KeyEvent.VK_COMMA, 188),
        Map.entry(KeyEvent.VK_MINUS, 189),
        Map.entry(KeyEvent.VK_PERIOD, 190),
        Map.entry(KeyEvent.VK_SLASH, 191),
        Map.entry(KeyEvent.VK_OPEN_BRACKET, 219),
        Map.entry(KeyEvent.VK_BACK_SLASH, 220),
        Map.entry(KeyEvent.VK_CLOSE_BRACKET, 221)
    );

    /**
     * The same shape the browser builds from a key event - the modifiers in a fixed order and the key by the code
     * {@code KeyboardEvent.keyCode} of the browser answers with.
     */
    private static String toCombo(KeyStroke keyStroke) {
        int keyCode = keyStroke.getKeyCode();
        if (keyCode == 0) {
            return null;
        }

        keyCode = DOM_KEY_CODES.getOrDefault(keyCode, keyCode);

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
