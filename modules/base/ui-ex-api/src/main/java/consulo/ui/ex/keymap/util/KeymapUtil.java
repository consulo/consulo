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
import consulo.ui.ex.action.*;
import consulo.ui.ex.action.util.MacKeymapUtil;
import consulo.ui.ex.internal.ActionStubBase;
import consulo.ui.ex.keymap.*;
import consulo.ui.ex.keymap.localize.KeyMapLocalize;
import consulo.ui.image.Image;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.intellij.lang.annotations.JdkConstants;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
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

    @Nonnull
    public static String createTooltipText(@Nonnull String tooltipText, @Nonnull String actionId) {
        String text = getFirstKeyboardShortcutText(actionId);
        return text.isEmpty() ? tooltipText : tooltipText + " (" + text + ")";
    }

    /**
     * @param actionId action to find the shortcut for
     * @return first keyboard shortcut that activates given action in active keymap; null if not found
     */
    @Nullable
    public static Shortcut getPrimaryShortcut(@Nullable String actionId) {
        KeymapManager keymapManager = KeymapManager.getInstance();
        if (actionId == null) {
            return null;
        }
        return ArrayUtil.getFirstElement(keymapManager.getActiveKeymap().getShortcuts(actionId));
    }

    @Nonnull
    public static String createTooltipText(@Nullable String name, @Nonnull AnAction action) {
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

    @Nonnull
    public static String getFirstKeyboardShortcutText(@Nonnull String actionId) {
        Shortcut[] shortcuts = KeymapManager.getInstance().getActiveKeymap().getShortcuts(actionId);
        KeyboardShortcut shortcut = ContainerUtil.findInstance(shortcuts, KeyboardShortcut.class);
        return shortcut == null ? "" : getShortcutText(shortcut);
    }

    @Nonnull
    public static String getFirstKeyboardShortcutText(@Nonnull AnAction action) {
        Shortcut[] shortcuts = action.getShortcutSet().getShortcuts();
        KeyboardShortcut shortcut = ContainerUtil.findInstance(shortcuts, KeyboardShortcut.class);
        return shortcut == null ? "" : getShortcutText(shortcut);
    }

    @Nonnull
    public static String getPreferredShortcutText(@Nonnull Shortcut[] shortcuts) {
        KeyboardShortcut shortcut = ContainerUtil.findInstance(shortcuts, KeyboardShortcut.class);
        return shortcut != null ? getShortcutText(shortcut) : shortcuts.length > 0 ? getShortcutText(shortcuts[0]) : "";
    }

    public static String getShortcutText(@Nonnull Shortcut shortcut) {
        return getShortcutText(shortcut, isUseUnicodeShortcuts());
    }

    public static String getShortcutText(@Nonnull Shortcut shortcut, boolean useUnicodeCharactersForShortcuts) {
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

    /**
     * @param button     target mouse button
     * @param modifiers  modifiers used within the target click
     * @param clickCount target clicks count
     * @return string representation of passed mouse shortcut.
     */
    @Nonnull
    public static LocalizeValue getMouseShortcutText(int button, @JdkConstants.InputEventMask int modifiers, int clickCount) {
        if (clickCount < 3) {
            return clickCount == 1
                ? KeyMapLocalize.mouseClickShortcutText(getModifiersText(mapNewModifiers(modifiers)), button)
                : KeyMapLocalize.mouseDoubleClickShortcutText(getModifiersText(mapNewModifiers(modifiers)), button);
        }
        else {
            throw new IllegalStateException("unknown clickCount: " + clickCount);
        }
    }

    @JdkConstants.InputEventMask
    private static int mapNewModifiers(@JdkConstants.InputEventMask int modifiers) {
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

    @Nonnull
    public static ShortcutSet getActiveKeymapShortcuts(@Nullable String actionId) {
        Application application = ApplicationManager.getApplication();
        KeymapManager keymapManager = application == null ? null : application.getInstance(KeymapManager.class);
        if (keymapManager == null || actionId == null) {
            return new CustomShortcutSet(Shortcut.EMPTY_ARRAY);
        }
        return new CustomShortcutSet(keymapManager.getActiveKeymap().getShortcuts(actionId));
    }

    public static boolean isEventForAction(@Nonnull KeyEvent keyEvent, @Nonnull String actionId) {
        for (KeyboardShortcut shortcut : ContainerUtil.findAll(getActiveKeymapShortcuts(actionId).getShortcuts(), KeyboardShortcut.class)) {
            if (AWTKeyStroke.getAWTKeyStrokeForEvent(keyEvent) == shortcut.getFirstKeyStroke()) {
                return true;
            }
        }
        return false;
    }

    private static String getModifiersText(@JdkConstants.InputEventMask int modifiers) {
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
    public static boolean matchActionMouseShortcutsModifiers(Keymap activeKeymap, @JdkConstants.InputEventMask int modifiers, String actionId) {
        MouseShortcut syntheticShortcut = new MouseShortcut(MouseEvent.BUTTON1, modifiers, 1);
        for (Shortcut shortcut : activeKeymap.getShortcuts(actionId)) {
            if (shortcut instanceof MouseShortcut mouseShortcut && mouseShortcut.getModifiers() == syntheticShortcut.getModifiers()) {
                return true;
            }
        }
        return false;
    }

    public static KeymapGroup createGroup(
        ActionGroup actionGroup,
        @Nonnull LocalizeValue groupName,
        Image icon,
        Image openIcon,
        boolean ignore,
        Predicate<AnAction> filtered
    ) {
        return createGroup(actionGroup, groupName, icon, openIcon, ignore, filtered, true);
    }

    public static KeymapGroup createGroup(
        ActionGroup actionGroup,
        @Nonnull LocalizeValue groupName,
        Image icon,
        Image openIcon,
        boolean ignore,
        Predicate<AnAction> filtered,
        boolean normalizeSeparators
    ) {
        ActionManager actionManager = ActionManager.getInstance();
        KeymapGroup group = KeymapGroupFactory.getInstance().createGroup(groupName, actionManager.getId(actionGroup), icon);
        AnAction[] children = actionGroup instanceof DefaultActionGroup defaultActionGroup
            ? defaultActionGroup.getChildActionsOrStubs()
            : actionGroup.getChildren(null);

        for (AnAction action : children) {
            LOG.assertTrue(action != null, groupName + " contains null actions");
            if (action instanceof ActionGroup childActionGroup) {
                KeymapGroup subGroup = createGroup(childActionGroup, getName(action), null, null, ignore, filtered, normalizeSeparators);
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
            else if (action != null) {
                String id = action instanceof ActionStubBase actionStubBase
                    ? actionStubBase.getId()
                    : actionManager.getId(action);
                if (id != null) {
                    if (id.startsWith(TOOL_ACTION_PREFIX)) {
                        continue;
                    }
                    if (filtered == null || filtered.test(action)) {
                        group.addActionId(id);
                    }
                }
            }
        }
        if (normalizeSeparators) {
            group.normalizeSeparators();
        }
        return group;
    }

    public static KeymapGroup createGroup(ActionGroup actionGroup, boolean ignore, Predicate<AnAction> filtered) {
        return createGroup(actionGroup, getName(actionGroup), null, null, ignore, filtered);
    }

    @Nonnull
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
