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

import consulo.platform.Platform;
import consulo.ui.ex.action.KeyboardShortcut;
import consulo.ui.ex.action.Shortcut;
import consulo.ui.ex.action.ShortcutSet;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.intellij.lang.annotations.JdkConstants;

import javax.swing.*;
import java.awt.event.KeyEvent;

/**
 * @author VISTALL
 * @since 30/01/2022
 */
public class ShortcutUtil {
    private static final String APPLE_LAF_AQUA_LOOK_AND_FEEL_CLASS_NAME = "apple.laf.AquaLookAndFeel";
    private static final String GET_KEY_MODIFIERS_TEXT_METHOD = "getKeyModifiersText";

    private static final String CANCEL_KEY_TEXT = "Cancel";
    private static final String BREAK_KEY_TEXT = "Break";

    private static String getModifiersText(@JdkConstants.InputEventMask int modifiers) {
        if (Platform.current().os().isMac()) {
            //try {
            //  Class appleLaf = Class.forName(APPLE_LAF_AQUA_LOOK_AND_FEEL_CLASS_NAME);
            //  Method getModifiers = appleLaf.getMethod(GET_KEY_MODIFIERS_TEXT_METHOD, int.class, boolean.class);
            //  return (String)getModifiers.invoke(appleLaf, modifiers, Boolean.FALSE);
            //}
            //catch (Exception e) {
            //  if (SystemInfo.isMacOSLeopard) {
            //    return getKeyModifiersTextForMacOSLeopard(modifiers);
            //  }
            //
            //  // OK do nothing here.
            //}
            return MacKeymapUtil.getModifiersText(modifiers);
        }

        final String keyModifiersText = KeyEvent.getKeyModifiersText(modifiers);
        if (keyModifiersText.isEmpty()) {
            return keyModifiersText;
        }
        else {
            return keyModifiersText + "+";
        }
    }


    public static String getKeystrokeText(KeyStroke accelerator) {
        if (accelerator == null) {
            return "";
        }
        if (Platform.current().os().isMac()) {
            return MacKeymapUtil.getKeyStrokeText(accelerator);
        }
        String acceleratorText = "";
        int modifiers = accelerator.getModifiers();
        if (modifiers > 0) {
            acceleratorText = getModifiersText(modifiers);
        }

        final int code = accelerator.getKeyCode();
        String keyText = Platform.current().os().isMac() ? MacKeymapUtil.getKeyText(code) : KeyEvent.getKeyText(code);
        // [vova] this is dirty fix for bug #35092
        if (CANCEL_KEY_TEXT.equals(keyText)) {
            keyText = BREAK_KEY_TEXT;
        }

        acceleratorText += keyText;
        return acceleratorText.trim();
    }

    @Nullable
    public static KeyStroke getKeyStroke(@Nonnull final ShortcutSet shortcutSet) {
        final Shortcut[] shortcuts = shortcutSet.getShortcuts();
        if (shortcuts.length == 0 || !(shortcuts[0] instanceof KeyboardShortcut)) {
            return null;
        }
        final KeyboardShortcut shortcut = (KeyboardShortcut) shortcuts[0];
        if (shortcut.getSecondKeyStroke() != null) {
            return null;
        }
        return shortcut.getFirstKeyStroke();
    }
}
