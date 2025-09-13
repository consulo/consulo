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
package consulo.ui.ex.action.util;

import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.localize.LocalizeValue;
import consulo.platform.Platform;
import consulo.ui.ex.action.KeyboardShortcut;
import consulo.ui.ex.action.Shortcut;
import consulo.ui.ex.action.ShortcutSet;
import consulo.ui.ex.internal.KeyMapSetting;
import consulo.ui.ex.localize.ShortcutLocalize;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 30/01/2022
 */
public class ShortcutUtil {
    private static void fillModifiersTexts(int modifiers, List<LocalizeValue> modifierTexts, boolean useUnicodeShortcuts) {
        if ((modifiers & InputEvent.CTRL_MASK) != 0) {
            modifierTexts.add(ShortcutLocalizeHolder.getKeyText(KeyEvent.VK_CONTROL, useUnicodeShortcuts));
        }

        if ((modifiers & InputEvent.ALT_MASK) != 0) {
            modifierTexts.add(ShortcutLocalizeHolder.getKeyText(KeyEvent.VK_ALT, useUnicodeShortcuts));
        }

        if ((modifiers & InputEvent.SHIFT_MASK) != 0) {
            modifierTexts.add(ShortcutLocalizeHolder.getKeyText(KeyEvent.VK_SHIFT, useUnicodeShortcuts));
        }

        if ((modifiers & InputEvent.ALT_GRAPH_MASK) != 0) {
            modifierTexts.add(ShortcutLocalizeHolder.getKeyText(KeyEvent.VK_ALT_GRAPH, useUnicodeShortcuts));
        }

        if ((modifiers & InputEvent.BUTTON1_MASK) != 0) {
            modifierTexts.add(ShortcutLocalize.mouseButton1());
        }

        if ((modifiers & InputEvent.META_MASK) != 0) {
            modifierTexts.add(ShortcutLocalizeHolder.getKeyText(KeyEvent.VK_META, useUnicodeShortcuts));
        }
    }

    @Nonnull
    public static String getKeystrokeText(KeyStroke accelerator) {
        return getKeystrokeTextValue(accelerator).get();
    }

    @Nonnull
    public static String getKeystrokeText(KeyStroke accelerator, boolean useUnicodeCharactersForShortcuts) {
        return getKeystrokeTextValue(accelerator, useUnicodeCharactersForShortcuts).get();
    }

    @Nonnull
    public static LocalizeValue getKeystrokeTextValue(@Nullable KeyStroke accelerator) {
        return getKeystrokeTextValue(accelerator, isUseUnicodeShortcuts());
    }

    @Nonnull
    public static LocalizeValue getKeystrokeTextValue(@Nullable KeyStroke accelerator, boolean useUnicodeCharactersForShortcuts) {
        if (accelerator == null) {
            return LocalizeValue.of();
        }

        List<LocalizeValue> values = new ArrayList<>(3);

        fillModifiersTexts(accelerator.getModifiers(), values, useUnicodeCharactersForShortcuts);

        values.add(ShortcutLocalizeHolder.getKeyText(accelerator.getKeyCode(), useUnicodeCharactersForShortcuts));

        if (useUnicodeCharactersForShortcuts) {
            return LocalizeValue.join(values.toArray(LocalizeValue[]::new));
        }
        else {
            return LocalizeValue.join("+", values.toArray(LocalizeValue[]::new));
        }
    }

    public static boolean isUseUnicodeShortcuts() {
        Application application = ApplicationManager.getApplication();
        if (application == null) {
            return Platform.current().os().isMac();
        }

        return application.getInstance(KeyMapSetting.class).isUseUnicodeShortcutsWithDefault();
    }

    @Nullable
    public static KeyStroke getKeyStroke(@Nonnull ShortcutSet shortcutSet) {
        Shortcut[] shortcuts = shortcutSet.getShortcuts();
        if (shortcuts.length == 0 || !(shortcuts[0] instanceof KeyboardShortcut)) {
            return null;
        }
        KeyboardShortcut shortcut = (KeyboardShortcut) shortcuts[0];
        if (shortcut.getSecondKeyStroke() != null) {
            return null;
        }
        return shortcut.getFirstKeyStroke();
    }
}
