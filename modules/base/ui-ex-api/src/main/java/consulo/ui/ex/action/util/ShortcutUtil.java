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
import consulo.ui.event.details.KeyCode;
import consulo.ui.event.details.KeyStroke;
import consulo.ui.event.details.ModifiedInputDetails.Modifier;
import consulo.ui.ex.action.KeyboardShortcut;
import consulo.ui.ex.action.Shortcut;
import consulo.ui.ex.action.ShortcutSet;
import consulo.ui.ex.internal.KeyMapSetting;
import consulo.ui.ex.localize.ShortcutLocalize;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2022-01-30
 */
public class ShortcutUtil {
    private static void fillModifiersTexts(Set<Modifier> modifiers, List<LocalizeValue> modifierTexts, boolean useUnicodeShortcuts) {
        if (modifiers.contains(Modifier.CTRL)) {
            modifierTexts.add(ShortcutLocalizeHolder.getKeyText(KeyCode.CONTROL, useUnicodeShortcuts));
        }

        if (modifiers.contains(Modifier.ALT)) {
            modifierTexts.add(ShortcutLocalizeHolder.getKeyText(KeyCode.ALT, useUnicodeShortcuts));
        }

        if (modifiers.contains(Modifier.SHIFT)) {
            modifierTexts.add(ShortcutLocalizeHolder.getKeyText(KeyCode.SHIFT, useUnicodeShortcuts));
        }

        if (modifiers.contains(Modifier.ALT_GRAPH)) {
            modifierTexts.add(ShortcutLocalizeHolder.getKeyText(KeyCode.ALT_GRAPH, useUnicodeShortcuts));
        }

        if (modifiers.contains(Modifier.META)) {
            modifierTexts.add(ShortcutLocalizeHolder.getKeyText(KeyCode.META, useUnicodeShortcuts));
        }
    }

    
    public static String getKeystrokeText(KeyStroke accelerator) {
        return getKeystrokeTextValue(accelerator).get();
    }

    
    public static String getKeystrokeText(KeyStroke accelerator, boolean useUnicodeCharactersForShortcuts) {
        return getKeystrokeTextValue(accelerator, useUnicodeCharactersForShortcuts).get();
    }

    public static LocalizeValue getKeystrokeTextValue(@Nullable KeyStroke accelerator) {
        return getKeystrokeTextValue(accelerator, isUseUnicodeShortcuts());
    }

    public static LocalizeValue getKeystrokeTextValue(@Nullable KeyStroke accelerator, boolean useUnicodeCharactersForShortcuts) {
        if (accelerator == null) {
            return LocalizeValue.empty();
        }

        List<LocalizeValue> values = new ArrayList<>(3);

        fillModifiersTexts(accelerator.getModifiers(), values, useUnicodeCharactersForShortcuts);

        values.add(accelerator.isTyped()
            ? LocalizeValue.of(String.valueOf(accelerator.getKeyChar()))
            : ShortcutLocalizeHolder.getKeyText(accelerator.getKeyCode(), useUnicodeCharactersForShortcuts));

        if (useUnicodeCharactersForShortcuts) {
            return LocalizeValue.join(values.toArray(LocalizeValue[]::new));
        }
        else {
            return LocalizeValue.joinWithSeparator(ShortcutLocalize.shortcutSeparator(), values.toArray(LocalizeValue[]::new));
        }
    }

    public static boolean isUseUnicodeShortcuts() {
        Application application = ApplicationManager.getApplication();
        if (application == null) {
            return Platform.current().os().isMac();
        }

        return application.getInstance(KeyMapSetting.class).isUseUnicodeShortcutsWithDefault();
    }

    public static @Nullable KeyStroke getKeyStroke(ShortcutSet shortcutSet) {
        Shortcut[] shortcuts = shortcutSet.getShortcuts();
        if (shortcuts.length != 0 && shortcuts[0] instanceof KeyboardShortcut shortcut && shortcut.getSecondKeyStroke() == null) {
            return shortcut.getFirstKeyStroke();
        }
        return null;
    }
}
