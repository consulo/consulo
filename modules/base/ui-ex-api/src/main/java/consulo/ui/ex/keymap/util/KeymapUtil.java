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
import consulo.platform.Platform;
import consulo.ui.ex.action.*;
import consulo.ui.ex.action.util.MacKeymapUtil;
import consulo.ui.ex.keymap.KeyMapBundle;
import consulo.ui.ex.keymap.KeymapManager;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.StringUtil;
import org.intellij.lang.annotations.JdkConstants;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import static consulo.ui.ex.action.util.ShortcutUtil.getKeystrokeText;

/**
 * @author VISTALL
 * @since 08-Mar-22
 */
public class KeymapUtil {
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
    if (keymapManager == null || actionId == null) return null;
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

  public static String getShortcutText(@Nonnull Shortcut shortcut) {
    String s = "";

    if (shortcut instanceof KeyboardShortcut) {
      KeyboardShortcut keyboardShortcut = (KeyboardShortcut)shortcut;

      String acceleratorText = getKeystrokeText(keyboardShortcut.getFirstKeyStroke());
      if (!acceleratorText.isEmpty()) {
        s = acceleratorText;
      }

      acceleratorText = getKeystrokeText(keyboardShortcut.getSecondKeyStroke());
      if (!acceleratorText.isEmpty()) {
        s += ", " + acceleratorText;
      }
    }
    else if (shortcut instanceof MouseShortcut) {
      MouseShortcut mouseShortcut = (MouseShortcut)shortcut;
      s = getMouseShortcutText(mouseShortcut.getButton(), mouseShortcut.getModifiers(), mouseShortcut.getClickCount());
    }
    else if (shortcut instanceof KeyboardModifierGestureShortcut) {
      final KeyboardModifierGestureShortcut gestureShortcut = (KeyboardModifierGestureShortcut)shortcut;
      s = gestureShortcut.getType() == KeyboardGestureAction.ModifierType.dblClick ? "Press, release and hold " : "Hold ";
      s += getKeystrokeText(gestureShortcut.getStroke());
    }
    else {
      throw new IllegalArgumentException("unknown shortcut class: " + shortcut.getClass().getCanonicalName());
    }
    return s;
  }


  /**
   * @param button     target mouse button
   * @param modifiers  modifiers used within the target click
   * @param clickCount target clicks count
   * @return string representation of passed mouse shortcut.
   */
  public static String getMouseShortcutText(int button, @JdkConstants.InputEventMask int modifiers, int clickCount) {
    if (clickCount < 3) {
      return KeyMapBundle.message("mouse." + (clickCount == 1 ? "" : "double.") + "click.shortcut.text", getModifiersText(mapNewModifiers(modifiers)), button);
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
    KeymapManager keymapManager = application == null ? null : application.getComponent(KeymapManager.class);
    if (keymapManager == null || actionId == null) {
      return new CustomShortcutSet(Shortcut.EMPTY_ARRAY);
    }
    return new CustomShortcutSet(keymapManager.getActiveKeymap().getShortcuts(actionId));
  }

  public static boolean isEventForAction(@Nonnull KeyEvent keyEvent, @Nonnull String actionId) {
    for (KeyboardShortcut shortcut : ContainerUtil.findAll(getActiveKeymapShortcuts(actionId).getShortcuts(), KeyboardShortcut.class)) {
      if (AWTKeyStroke.getAWTKeyStrokeForEvent(keyEvent) == shortcut.getFirstKeyStroke()) return true;
    }
    return false;
  }

  private static String getModifiersText(@JdkConstants.InputEventMask int modifiers) {
    if (Platform.current().os().isMac()) {
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
}
