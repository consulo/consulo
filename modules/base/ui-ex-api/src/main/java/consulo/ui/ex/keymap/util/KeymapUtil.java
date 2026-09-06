/*
 * Copyright 2013-2022 consulo.io
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
package consulo.ui.ex.keymap.util;

import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.platform.Platform;
import consulo.ui.UIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.action.util.MacKeymapUtil;
import consulo.ui.ex.internal.ActionStubBase;
import consulo.ui.ex.keymap.*;
import consulo.ui.ex.keymap.localize.KeyMapLocalize;
import consulo.ui.image.Image;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.ContainerUtil;
import consulo.util.concurrent.coroutine.CoroutineScope;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

import static consulo.ui.ex.action.util.ShortcutUtil.getKeystrokeText;
import static consulo.ui.ex.action.util.ShortcutUtil.isUseUnicodeShortcuts;

/**
 * @author VISTALL
 * @since 2022-03-08
 */
public class KeymapUtil {
    private static final Logger LOG = Logger.getInstance(KeymapUtil.class);
    private static final String TOOL_ACTION_PREFIX = "Tool_";

    private static final String SHIFT = "shift";
    private static final String CONTROL = "control";
    private static final String CTRL = "ctrl";
    private static final String META = "meta";
    private static final String ALT = "alt";
    private static final String ALT_GRAPH = "altGraph";
    private static final String DOUBLE_CLICK = "doubleClick";

    /**
     * Factory method. It parses passed string and creates <code>MouseShortcut</code>.
     *
     * @param keystrokeString target keystroke
     * @return shortcut for the given keystroke
     * @throws IllegalArgumentException if <code>keystrokeString</code> doesn't represent valid <code>MouseShortcut</code>.
     */
    public static MouseShortcut parseMouseShortcut(String keystrokeString) {
        if (keystrokeString.startsWith("Force touch")) {
            return new PressureShortcut(2);
        }

        int button = -1;
        int modifiers = 0;
        int clickCount = 1;
        for (StringTokenizer tokenizer = new StringTokenizer(keystrokeString); tokenizer.hasMoreTokens(); ) {
            String token = tokenizer.nextToken();
            if (SHIFT.equals(token)) {
                modifiers |= InputEvent.SHIFT_DOWN_MASK;
            }
            else if (CONTROL.equals(token) || CTRL.equals(token)) {
                modifiers |= InputEvent.CTRL_DOWN_MASK;
            }
            else if (META.equals(token)) {
                modifiers |= InputEvent.META_DOWN_MASK;
            }
            else if (ALT.equals(token)) {
                modifiers |= InputEvent.ALT_DOWN_MASK;
            }
            else if (ALT_GRAPH.equals(token)) {
                modifiers |= InputEvent.ALT_GRAPH_DOWN_MASK;
            }
            else if (token.startsWith("button") && token.length() > 6) {
                try {
                    button = Integer.parseInt(token.substring(6));
                }
                catch (NumberFormatException e) {
                    throw new IllegalArgumentException("unparseable token: " + token);
                }
            }
            else if (DOUBLE_CLICK.equals(token)) {
                clickCount = 2;
            }
            else {
                throw new IllegalArgumentException("unknown token: " + token);
            }
        }
        return new MouseShortcut(button, modifiers, clickCount);
    }

    
    public static String createTooltipText(String tooltipText, String actionId) {
        String text = getFirstKeyboardShortcutText(actionId);
        return text.isEmpty() ? tooltipText : tooltipText + " (" + text + ")";
    }

    /**
     * @param actionId action to find the shortcut for
     * @return first keyboard shortcut that activates given action in active keymap; null if not found
     */
    public static @Nullable Shortcut getPrimaryShortcut(@Nullable String actionId) {
        KeymapManager keymapManager = KeymapManager.getInstance();
        if (actionId == null) {
            return null;
        }
        return ArrayUtil.getFirstElement(keymapManager.getActiveKeymap().getShortcuts(actionId));
    }

    
    public static String createTooltipText(@Nullable String name, AnAction action) {
        String toolTipText = name == null ? "" : name;
        while (StringUtil.endsWithChar(toolTipText, '.')) {
            toolTipText = toolTipText.substring(0, toolTipText.length() - 1);
        }
        String shortcutsText = getFirstKeyboardShortcutText(action);
        if (!shortcutsText.isEmpty()) {
            toolTipText += " (" + shortcutsText + ")";
        }
        return toolTipText;
    }

    public static String getShortcutsText(Shortcut[] shortcuts) {
        if (shortcuts.length == 0) {
            return "";
        }
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < shortcuts.length; i++) {
            Shortcut shortcut = shortcuts[i];
            if (i > 0) {
                buffer.append(' ');
            }
            buffer.append(getShortcutText(shortcut));
        }
        return buffer.toString();
    }

    
    public static String getFirstKeyboardShortcutText(String actionId) {
        Shortcut[] shortcuts = KeymapManager.getInstance().getActiveKeymap().getShortcuts(actionId);
        KeyboardShortcut shortcut = ContainerUtil.findInstance(shortcuts, KeyboardShortcut.class);
        return shortcut == null ? "" : getShortcutText(shortcut);
    }

    
    public static String getFirstKeyboardShortcutText(AnAction action) {
        Shortcut[] shortcuts = action.getShortcutSet().getShortcuts();
        KeyboardShortcut shortcut = ContainerUtil.findInstance(shortcuts, KeyboardShortcut.class);
        return shortcut == null ? "" : getShortcutText(shortcut);
    }

    
    public static String getPreferredShortcutText(Shortcut[] shortcuts) {
        KeyboardShortcut shortcut = ContainerUtil.findInstance(shortcuts, KeyboardShortcut.class);
        return shortcut != null ? getShortcutText(shortcut) : shortcuts.length > 0 ? getShortcutText(shortcuts[0]) : "";
    }

    public static String getShortcutText(Shortcut shortcut) {
        return getShortcutText(shortcut, isUseUnicodeShortcuts());
    }

    public static String getShortcutText(Shortcut shortcut, boolean useUnicodeCharactersForShortcuts) {
        return switch (shortcut) {
            case KeyboardShortcut keyboardShortcut -> {
                String s = "";
                String acceleratorText = getKeystrokeText(keyboardShortcut.getFirstKeyStroke(), useUnicodeCharactersForShortcuts);
                if (!acceleratorText.isEmpty()) {
                    s = acceleratorText;
                }

                acceleratorText = getKeystrokeText(keyboardShortcut.getSecondKeyStroke(), useUnicodeCharactersForShortcuts);
                if (!acceleratorText.isEmpty()) {
                    s += ", " + acceleratorText;
                }

                yield s;
            }

            case MouseShortcut mouseShortcut ->
                getMouseShortcutText(mouseShortcut.getButton(), mouseShortcut.getModifiers(), mouseShortcut.getClickCount()).get();

            case KeyboardModifierGestureShortcut gestureShortcut ->
                (gestureShortcut.getType() == KeyboardGestureAction.ModifierType.dblClick ? "Press, release and hold " : "Hold ") +
                    getKeystrokeText(gestureShortcut.getStroke(), useUnicodeCharactersForShortcuts);

            default -> throw new IllegalArgumentException("unknown shortcut class: " + shortcut.getClass().getCanonicalName());
        };
    }

    
    public static LocalizeValue getMouseShortcutText(MouseShortcut mouseShortcut) {
        return getMouseShortcutText(mouseShortcut.getButton(), mouseShortcut.getModifiers(), mouseShortcut.getClickCount());
    }

    /**
     * @param button     target mouse button
     * @param modifiers  modifiers used within the target click
     * @param clickCount target clicks count
     * @return string representation of passed mouse shortcut.
     */
    public static LocalizeValue getMouseShortcutText(int button, int modifiers, int clickCount) {
        if (clickCount < 3) {
            return clickCount == 1
                ? KeyMapLocalize.mouseClickShortcutText(getModifiersText(mapNewModifiers(modifiers)), button)
                : KeyMapLocalize.mouseDoubleClickShortcutText(getModifiersText(mapNewModifiers(modifiers)), button);
        }
        else {
            throw new IllegalStateException("unknown clickCount: " + clickCount);
        }
    }

    private static int mapNewModifiers(int modifiers) {
        if ((modifiers & InputEvent.SHIFT_DOWN_MASK) != 0) {
            modifiers |= InputEvent.SHIFT_MASK;
        }
        if ((modifiers & InputEvent.ALT_DOWN_MASK) != 0) {
            modifiers |= InputEvent.ALT_MASK;
        }
        if ((modifiers & InputEvent.ALT_GRAPH_DOWN_MASK) != 0) {
            modifiers |= InputEvent.ALT_GRAPH_MASK;
        }
        if ((modifiers & InputEvent.CTRL_DOWN_MASK) != 0) {
            modifiers |= InputEvent.CTRL_MASK;
        }
        if ((modifiers & InputEvent.META_DOWN_MASK) != 0) {
            modifiers |= InputEvent.META_MASK;
        }

        return modifiers;
    }

    
    public static ShortcutSet getActiveKeymapShortcuts(@Nullable String actionId) {
        Application application = ApplicationManager.getApplication();
        KeymapManager keymapManager = application == null ? null : application.getInstance(KeymapManager.class);
        if (keymapManager == null || actionId == null) {
            return new CustomShortcutSet(Shortcut.EMPTY_ARRAY);
        }
        return new CustomShortcutSet(keymapManager.getActiveKeymap().getShortcuts(actionId));
    }

    public static boolean isEventForAction(KeyEvent keyEvent, String actionId) {
        for (KeyboardShortcut shortcut : ContainerUtil.findAll(getActiveKeymapShortcuts(actionId).getShortcuts(), KeyboardShortcut.class)) {
            if (AWTKeyStroke.getAWTKeyStrokeForEvent(keyEvent) == shortcut.getFirstKeyStroke()) {
                return true;
            }
        }
        return false;
    }

    private static String getModifiersText(int modifiers) {
        if (Platform.current().os().isMac()) {
            return MacKeymapUtil.getModifiersText(modifiers);
        }

        String keyModifiersText = KeyEvent.getKeyModifiersText(modifiers);
        if (keyModifiersText.isEmpty()) {
            return keyModifiersText;
        }
        else {
            return keyModifiersText + "+";
        }
    }

    /**
     * Checks that one of the mouse shortcuts assigned to the provided action has the same modifiers as provided
     */
    public static boolean matchActionMouseShortcutsModifiers(Keymap activeKeymap, int modifiers, String actionId) {
        MouseShortcut syntheticShortcut = new MouseShortcut(MouseEvent.BUTTON1, modifiers, 1);
        for (Shortcut shortcut : activeKeymap.getShortcuts(actionId)) {
            if (shortcut instanceof MouseShortcut mouseShortcut && mouseShortcut.getModifiers() == syntheticShortcut.getModifiers()) {
                return true;
            }
        }
        return false;
    }

    public static CompletableFuture<KeymapGroup> createGroupAsync(
        ActionGroup actionGroup,
        LocalizeValue groupName,
        Image icon,
        Image openIcon,
        boolean ignore,
        Predicate<AnAction> filtered
    ) {
        return createGroupAsync(actionGroup, groupName, icon, openIcon, ignore, filtered, true);
    }

    public static CompletableFuture<KeymapGroup> createGroupAsync(
        ActionGroup actionGroup,
        LocalizeValue groupName,
        Image icon,
        Image openIcon,
        boolean ignore,
        Predicate<AnAction> filtered,
        boolean normalizeSeparators
    ) {
        return createGroupAsync(actionGroup, groupName, icon, openIcon, ignore, filtered, normalizeSeparators, currentUIAccess());
    }

    private static CompletableFuture<KeymapGroup> createGroupAsync(
        ActionGroup actionGroup,
        LocalizeValue groupName,
        Image icon,
        Image openIcon,
        boolean ignore,
        Predicate<AnAction> filtered,
        boolean normalizeSeparators,
        @Nullable UIAccess uiAccess
    ) {
        ActionManager actionManager = ActionManager.getInstance();
        KeymapGroup group = KeymapGroupFactory.getInstance().createGroup(groupName, actionManager.getId(actionGroup), icon);

        return getChildrenAsync(actionGroup, uiAccess).thenCompose(children -> {
            CompletableFuture<?> chain = CompletableFuture.completedFuture(null);

            for (AnAction action : children) {
                LOG.assertTrue(action != null, groupName + " contains null actions");

                if (action instanceof ActionGroup childActionGroup) {
                    chain = chain.thenCompose(ignored ->
                        createGroupAsync(childActionGroup, getName(action), null, null, ignore, filtered, normalizeSeparators, uiAccess)
                            .thenAccept(subGroup -> {
                                if (subGroup.getSize() > 0) {
                                    if (!ignore && !childActionGroup.isPopup()) {
                                        group.addAll(subGroup);
                                    }
                                    else {
                                        group.addGroup(subGroup);
                                    }
                                }
                                else if (filtered == null || filtered.test(actionGroup)) {
                                    group.addGroup(subGroup);
                                }
                            })
                    );
                }
                else if (action instanceof AnSeparator) {
                    group.addSeparator();
                }
                else {
                    String id = action instanceof ActionStubBase actionStubBase
                        ? actionStubBase.getId()
                        : actionManager.getId(action);
                    if (id != null && !id.startsWith(TOOL_ACTION_PREFIX) && (filtered == null || filtered.test(action))) {
                        group.addActionId(id);
                    }
                }
            }

            return chain.thenApply(ignored -> {
                if (normalizeSeparators) {
                    group.normalizeSeparators();
                }
                return group;
            });
        });
    }

    public static CompletableFuture<KeymapGroup> createGroupAsync(
        ActionGroup actionGroup,
        boolean ignore,
        Predicate<AnAction> filtered
    ) {
        return createGroupAsync(actionGroup, getName(actionGroup), null, null, ignore, filtered);
    }

    /**
     * Resolves the children of a group without an action event. Groups exposing their children statically are
     * read directly; any other group is reported as having no children, since expanding it would require a data
     * context which does not exist while a keymap tree is built.
     */
    public static List<AnAction> getChildren(ActionGroup actionGroup) {
        return actionGroup instanceof DefaultActionGroup defaultActionGroup
            ? List.of(defaultActionGroup.getChildActionsOrStubs())
            : List.of();
    }

    public static KeymapGroup createGroup(ActionGroup actionGroup, boolean ignore, @Nullable Predicate<AnAction> filtered) {
        return createGroup(actionGroup, getName(actionGroup), null, null, ignore, filtered, true);
    }

    public static KeymapGroup createGroup(
        ActionGroup actionGroup,
        LocalizeValue groupName,
        @Nullable Image icon,
        @Nullable Image openIcon,
        boolean ignore,
        @Nullable Predicate<AnAction> filtered,
        boolean normalizeSeparators
    ) {
        ActionManager actionManager = ActionManager.getInstance();
        KeymapGroup group = KeymapGroupFactory.getInstance().createGroup(groupName, actionManager.getId(actionGroup), icon);

        for (AnAction action : getChildren(actionGroup)) {
            LOG.assertTrue(action != null, groupName + " contains null actions");

            if (action instanceof ActionGroup childActionGroup) {
                KeymapGroup subGroup =
                    createGroup(childActionGroup, getName(action), null, null, ignore, filtered, normalizeSeparators);
                if (subGroup.getSize() > 0) {
                    if (!ignore && !childActionGroup.isPopup()) {
                        group.addAll(subGroup);
                    }
                    else {
                        group.addGroup(subGroup);
                    }
                }
                else if (filtered == null || filtered.test(actionGroup)) {
                    group.addGroup(subGroup);
                }
            }
            else if (action instanceof AnSeparator) {
                group.addSeparator();
            }
            else {
                String id = action instanceof ActionStubBase actionStubBase
                    ? actionStubBase.getId()
                    : actionManager.getId(action);
                if (id != null && !id.startsWith(TOOL_ACTION_PREFIX) && (filtered == null || filtered.test(action))) {
                    group.addActionId(id);
                }
            }
        }

        if (normalizeSeparators) {
            group.normalizeSeparators();
        }
        return group;
    }

    /**
     * Resolves the children of a group for tree building. Groups exposing their children statically are
     * read directly, any other group is expanded through its asynchronous contract without an action
     * event, since no data context exists while a keymap or customization tree is built.
     */
    public static CompletableFuture<List<AnAction>> getChildrenAsync(ActionGroup actionGroup) {
        return getChildrenAsync(actionGroup, currentUIAccess());
    }

    private static CompletableFuture<List<AnAction>> getChildrenAsync(ActionGroup actionGroup, @Nullable UIAccess uiAccess) {
        if (actionGroup instanceof DefaultActionGroup defaultActionGroup) {
            return CompletableFuture.completedFuture(List.of(defaultActionGroup.getChildActionsOrStubs()));
        }

        Application application = Application.get();
        CoroutineScope scope = CoroutineScope.of(application.coroutineContext());
        if (uiAccess != null) {
            scope.putCopyableUserData(UIAccess.KEY, uiAccess);
        }

        return actionGroup.getChildrenAsync(null)
            .runAsync(scope, null)
            .toFuture()
            .thenApply(children -> children == null ? List.<AnAction>of() : children);
    }

    /**
     * The walk starts on the UI thread and continues on the coroutine executor, so the access a group may need to
     * hop back to the UI has to be taken while there still is one - asking for it later throws.
     */
    private static @Nullable UIAccess currentUIAccess() {
        return UIAccess.isUIThread() ? UIAccess.current() : null;
    }

    private static LocalizeValue getName(AnAction action) {
        LocalizeValue name = action.getTemplatePresentation().getTextValue();
        if (name.isNotEmpty()) {
            return name;
        }
        else {
            String id = action instanceof ActionStubBase actionStubBase
                ? actionStubBase.getId()
                : ActionManager.getInstance().getId(action);
            if (id != null) {
                return LocalizeValue.of(id);
            }
            if (action instanceof DefaultActionGroup group) {
                if (group.getChildrenCount() == 0) {
                    return LocalizeValue.localizeTODO("Empty group");
                }
                AnAction[] children = group.getChildActionsOrStubs();
                for (AnAction child : children) {
                    if (!(child instanceof AnSeparator)) {
                        return LocalizeValue.join(LocalizeValue.of("group."), getName(child));
                    }
                }
                return LocalizeValue.localizeTODO("Empty unnamed group");
            }
            return LocalizeValue.of(action.getClass().getName());
        }
    }
}
