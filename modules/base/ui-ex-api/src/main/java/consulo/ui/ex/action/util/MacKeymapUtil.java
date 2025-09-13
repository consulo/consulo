/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

import org.intellij.lang.annotations.JdkConstants;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;

/**
 * Utility class to display action shortcuts in Mac menus
 *
 * @author Konstantin Bulenkov
 */
@SuppressWarnings("UnusedDeclaration")
@Deprecated
public class MacKeymapUtil {
    public static final String ESCAPE = "\u238B";
    public static final String TAB = "\u21E5";
    public static final String TAB_BACK = "\u21E4";
    public static final String CAPS_LOCK = "\u21EA";
    public static final String SHIFT = "\u21E7";
    public static final String CONTROL = "\u2303";
    public static final String OPTION = "\u2325";
    public static final String APPLE = "\uF8FF";
    public static final String COMMAND = "\u2318";
    public static final String SPACE = "\u2423";
    public static final String RETURN = "\u23CE";
    public static final String BACKSPACE = "\u232B";
    public static final String DELETE = "\u2326";
    public static final String HOME = "\u2196";
    public static final String END = "\u2198";
    public static final String PAGE_UP = "\u21DE";
    public static final String PAGE_DOWN = "\u21DF";
    public static final String UP = "\u2191";
    public static final String DOWN = "\u2193";
    public static final String LEFT = "\u2190";
    public static final String RIGHT = "\u2192";
    public static final String CLEAR = "\u2327";
    public static final String NUMBER_LOCK = "\u21ED";
    public static final String ENTER = "\u2324";
    public static final String EJECT = "\u23CF";
    public static final String POWER3 = "\u233D";
    public static final String NUM_PAD = "\u2328";

    public static String getModifiersText(@JdkConstants.InputEventMask int modifiers) {
        StringBuilder buf = new StringBuilder();
        if ((modifiers & InputEvent.CTRL_MASK) != 0) {
            buf.append(CONTROL);
        }
        if ((modifiers & InputEvent.ALT_MASK) != 0) {
            buf.append(OPTION);
        }
        if ((modifiers & InputEvent.SHIFT_MASK) != 0) {
            buf.append(SHIFT);
        }
        if ((modifiers & InputEvent.ALT_GRAPH_MASK) != 0) {
            buf.append(Toolkit.getProperty("AWT.altGraph", "Alt Graph"));
        }
        if ((modifiers & InputEvent.BUTTON1_MASK) != 0) {
            buf.append(Toolkit.getProperty("AWT.button1", "Button1"));
        }
        if ((modifiers & InputEvent.META_MASK) != 0) {
            buf.append(COMMAND);
        }
        return buf.toString();
    }

    public static String getKeyText(int code) {
        return ShortcutLocalizeHolder.getKeyText(code, true).get();
    }

    public static String getKeyStrokeText(KeyStroke keyStroke) {
        String modifiers = getModifiersText(keyStroke.getModifiers());
        String key = getKeyText(keyStroke.getKeyCode());
        return modifiers + key;
    }
}
