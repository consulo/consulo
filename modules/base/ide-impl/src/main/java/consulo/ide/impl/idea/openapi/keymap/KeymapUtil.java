/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.keymap;

import consulo.application.AllIcons;
import consulo.application.util.registry.Registry;
import consulo.application.util.registry.RegistryValue;
import consulo.application.util.registry.RegistryValueListener;
import consulo.disposer.Disposer;
import consulo.ide.impl.idea.util.ArrayUtil;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.ui.ex.action.*;
import consulo.ui.ex.action.util.ShortcutUtil;
import consulo.ui.ex.keymap.Keymap;
import consulo.ui.ex.keymap.KeymapManager;
import consulo.ui.image.Image;
import consulo.util.xml.serializer.InvalidDataException;
import org.intellij.lang.annotations.JdkConstants;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

public class KeymapUtil {
  private static final String SHIFT = "shift";
  private static final String CONTROL = "control";
  private static final String CTRL = "ctrl";
  private static final String META = "meta";
  private static final String ALT = "alt";
  private static final String ALT_GRAPH = "altGraph";
  private static final String DOUBLE_CLICK = "doubleClick";

  private static final Set<Integer> ourTooltipKeys = new HashSet<Integer>();
  private static final Set<Integer> ourOtherTooltipKeys = new HashSet<Integer>();
  private static RegistryValue ourTooltipKeysProperty;

  private KeymapUtil() {
  }

  public static String getShortcutText(@Nonnull Shortcut shortcut) {
    return consulo.ui.ex.keymap.util.KeymapUtil.getShortcutText(shortcut);
  }

  public static Image getShortcutIcon(Shortcut shortcut) {
    if (shortcut instanceof KeyboardShortcut) {
      return AllIcons.General.KeyboardShortcut;
    }
    else if (shortcut instanceof MouseShortcut) {
      return AllIcons.General.MouseShortcut;
    }
    else {
      throw new IllegalArgumentException("unknown shortcut class: " + shortcut);
    }
  }

  /**
   * @param button     target mouse button
   * @param modifiers  modifiers used within the target click
   * @param clickCount target clicks count
   * @return string representation of passed mouse shortcut.
   */
  public static String getMouseShortcutText(int button, @JdkConstants.InputEventMask int modifiers, int clickCount) {
    return consulo.ui.ex.keymap.util.KeymapUtil.getMouseShortcutText(button, modifiers, clickCount);
  }

  @Nonnull
  public static ShortcutSet getActiveKeymapShortcuts(@Nullable String actionId) {
    return consulo.ui.ex.keymap.util.KeymapUtil.getActiveKeymapShortcuts(actionId);
  }

  /**
   * Creates shortcut corresponding to a single-click event
   */
  public static MouseShortcut createMouseShortcut(@Nonnull MouseEvent e) {
    int button = MouseShortcut.getButton(e);
    int modifiers = e.getModifiersEx();
    if (button == MouseEvent.NOBUTTON && e.getID() == MouseEvent.MOUSE_DRAGGED) {
      // mouse drag events don't have button field set due to some reason
      if ((modifiers & InputEvent.BUTTON1_DOWN_MASK) != 0) {
        button = MouseEvent.BUTTON1;
      }
      else if ((modifiers & InputEvent.BUTTON2_DOWN_MASK) != 0) {
        button = MouseEvent.BUTTON2;
      }
    }
    return new MouseShortcut(button, modifiers, 1);
  }

  public static String getKeystrokeText(KeyStroke accelerator) {
    return ShortcutUtil.getKeystrokeText(accelerator);
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
  public static String getFirstKeyboardShortcutText(@Nonnull String actionId) {
    Shortcut[] shortcuts = KeymapManager.getInstance().getActiveKeymap().getShortcuts(actionId);
    KeyboardShortcut shortcut = ContainerUtil.findInstance(shortcuts, KeyboardShortcut.class);
    return shortcut == null ? "" : getShortcutText(shortcut);
  }

  @Nonnull
  public static String getFirstKeyboardShortcutText(@Nonnull ShortcutSet set) {
    Shortcut[] shortcuts = set.getShortcuts();
    KeyboardShortcut shortcut = ContainerUtil.findInstance(shortcuts, KeyboardShortcut.class);
    return shortcut == null ? "" : getShortcutText(shortcut);
  }

  @Nonnull
  public static String getPreferredShortcutText(@Nonnull Shortcut[] shortcuts) {
    KeyboardShortcut shortcut = ContainerUtil.findInstance(shortcuts, KeyboardShortcut.class);
    return shortcut != null ? getShortcutText(shortcut) : shortcuts.length > 0 ? getShortcutText(shortcuts[0]) : "";
  }

  @Nonnull
  public static String getFirstKeyboardShortcutText(@Nonnull AnAction action) {
    return consulo.ui.ex.keymap.util.KeymapUtil.getFirstKeyboardShortcutText(action);
  }


  public static boolean isEventForAction(@Nonnull KeyEvent keyEvent, @Nonnull String actionId) {
    return consulo.ui.ex.keymap.util.KeymapUtil.isEventForAction(keyEvent, actionId);
  }

  public static String getShortcutsText(Shortcut[] shortcuts) {
    return consulo.ui.ex.keymap.util.KeymapUtil.getShortcutsText(shortcuts);
  }

  /**
   * Factory method. It parses passed string and creates <code>MouseShortcut</code>.
   *
   * @param keystrokeString target keystroke
   * @return shortcut for the given keystroke
   * @throws InvalidDataException if <code>keystrokeString</code> doesn't represent valid <code>MouseShortcut</code>.
   */
  public static MouseShortcut parseMouseShortcut(String keystrokeString) throws InvalidDataException {
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
          throw new InvalidDataException("unparseable token: " + token);
        }
      }
      else if (DOUBLE_CLICK.equals(token)) {
        clickCount = 2;
      }
      else {
        throw new InvalidDataException("unknown token: " + token);
      }
    }
    return new MouseShortcut(button, modifiers, clickCount);
  }

  public static String getKeyModifiersTextForMacOSLeopard(@JdkConstants.InputEventMask int modifiers) {
    StringBuilder buf = new StringBuilder();
    if ((modifiers & InputEvent.META_MASK) != 0) {
      buf.append("\u2318");
    }
    if ((modifiers & InputEvent.CTRL_MASK) != 0) {
      buf.append(Toolkit.getProperty("AWT.control", "Ctrl"));
    }
    if ((modifiers & InputEvent.ALT_MASK) != 0) {
      buf.append("\2325");
    }
    if ((modifiers & InputEvent.SHIFT_MASK) != 0) {
      buf.append(Toolkit.getProperty("AWT.shift", "Shift"));
    }
    if ((modifiers & InputEvent.ALT_GRAPH_MASK) != 0) {
      buf.append(Toolkit.getProperty("AWT.altGraph", "Alt Graph"));
    }
    if ((modifiers & InputEvent.BUTTON1_MASK) != 0) {
      buf.append(Toolkit.getProperty("AWT.button1", "Button1"));
    }
    return buf.toString();
  }

  public static boolean isTooltipRequest(KeyEvent keyEvent) {
    if (ourTooltipKeysProperty == null) {
      ourTooltipKeysProperty = Registry.get("ide.forcedShowTooltip");
      ourTooltipKeysProperty.addListener(new RegistryValueListener() {
        @Override
        public void afterValueChanged(RegistryValue value) {
          updateTooltipRequestKey(value);
        }
      }, Disposer.get("ui"));

      updateTooltipRequestKey(ourTooltipKeysProperty);
    }

    if (keyEvent.getID() != KeyEvent.KEY_PRESSED) return false;

    for (Integer each : ourTooltipKeys) {
      if ((keyEvent.getModifiers() & each.intValue()) == 0) return false;
    }

    for (Integer each : ourOtherTooltipKeys) {
      if ((keyEvent.getModifiers() & each.intValue()) > 0) return false;
    }

    final int code = keyEvent.getKeyCode();

    return code == KeyEvent.VK_META || code == KeyEvent.VK_CONTROL || code == KeyEvent.VK_SHIFT || code == KeyEvent.VK_ALT;
  }

  private static void updateTooltipRequestKey(RegistryValue value) {
    final String text = value.asString();

    ourTooltipKeys.clear();
    ourOtherTooltipKeys.clear();

    processKey(text.contains("meta"), InputEvent.META_MASK);
    processKey(text.contains("control") || text.contains("ctrl"), InputEvent.CTRL_MASK);
    processKey(text.contains("shift"), InputEvent.SHIFT_MASK);
    processKey(text.contains("alt"), InputEvent.ALT_MASK);

  }

  private static void processKey(boolean condition, int value) {
    if (condition) {
      ourTooltipKeys.add(value);
    }
    else {
      ourOtherTooltipKeys.add(value);
    }
  }

  public static boolean isEmacsKeymap() {
    return isEmacsKeymap(KeymapManager.getInstance().getActiveKeymap());
  }

  public static boolean isEmacsKeymap(@Nullable Keymap keymap) {
    for (; keymap != null; keymap = keymap.getParent()) {
      if ("Emacs".equalsIgnoreCase(keymap.getName())) {
        return true;
      }
    }
    return false;
  }

  @Nullable
  public static KeyStroke getKeyStroke(@Nonnull final ShortcutSet shortcutSet) {
    return ShortcutUtil.getKeyStroke(shortcutSet);
  }

  @Nonnull
  public static String createTooltipText(@Nonnull String tooltipText, @Nonnull String actionId) {
    return consulo.ui.ex.keymap.util.KeymapUtil.createTooltipText(tooltipText, actionId);
  }

  @Nonnull
  public static String createTooltipText(@Nullable String name, @Nonnull AnAction action) {
    return consulo.ui.ex.keymap.util.KeymapUtil.createTooltipText(name, action);
  }

  /**
   * Checks that one of the mouse shortcuts assigned to the provided action has the same modifiers as provided
   */
  public static boolean matchActionMouseShortcutsModifiers(final Keymap activeKeymap, @JdkConstants.InputEventMask int modifiers, final String actionId) {
    final MouseShortcut syntheticShortcut = new MouseShortcut(MouseEvent.BUTTON1, modifiers, 1);
    for (Shortcut shortcut : activeKeymap.getShortcuts(actionId)) {
      if (shortcut instanceof MouseShortcut) {
        final MouseShortcut mouseShortcut = (MouseShortcut)shortcut;
        if (mouseShortcut.getModifiers() == syntheticShortcut.getModifiers()) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Checks whether mouse event's button and modifiers match a shortcut configured in active keymap for given action id.
   * Only shortcuts having click count of 1 can be matched, mouse event's click count is ignored.
   */
  public static boolean isMouseActionEvent(@Nonnull MouseEvent e, @Nonnull String actionId) {
    KeymapManager keymapManager = KeymapManager.getInstance();
    if (keymapManager == null) {
      return false;
    }
    Keymap keymap = keymapManager.getActiveKeymap();
    if (keymap == null) {
      return false;
    }
    int button = e.getButton();
    int modifiers = e.getModifiersEx();
    if (button == MouseEvent.NOBUTTON && e.getID() == MouseEvent.MOUSE_DRAGGED) {
      // mouse drag events don't have button field set due to some reason
      if ((modifiers & InputEvent.BUTTON1_DOWN_MASK) != 0) {
        button = MouseEvent.BUTTON1;
      }
      else if ((modifiers & InputEvent.BUTTON2_DOWN_MASK) != 0) {
        button = MouseEvent.BUTTON2;
      }
    }
    String[] actionIds = keymap.getActionIds(new MouseShortcut(button, modifiers, 1));
    if (actionIds == null) {
      return false;
    }
    for (String id : actionIds) {
      if (actionId.equals(id)) {
        return true;
      }
    }
    return false;
  }

  /**
   * @param component    target component to reassign previously mapped action (if any)
   * @param oldKeyStroke previously mapped keystroke (e.g. standard one that you want to use in some different way)
   * @param newKeyStroke new keystroke to be assigned. <code>null</code> value means 'just unregister previously mapped action'
   * @param condition    one of
   *                     <ul>
   *                     <li>JComponent.WHEN_FOCUSED,</li>
   *                     <li>JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT</li>
   *                     <li>JComponent.WHEN_IN_FOCUSED_WINDOW</li>
   *                     <li>JComponent.UNDEFINED_CONDITION</li>
   *                     </ul>
   * @return <code>true</code> if the action is reassigned successfully
   */
  public static boolean reassignAction(@Nonnull JComponent component, @Nonnull KeyStroke oldKeyStroke, @Nullable KeyStroke newKeyStroke, int condition) {
    return reassignAction(component, oldKeyStroke, newKeyStroke, condition, true);
  }

  /**
   * @param component        target component to reassign previously mapped action (if any)
   * @param oldKeyStroke     previously mapped keystroke (e.g. standard one that you want to use in some different way)
   * @param newKeyStroke     new keystroke to be assigned. <code>null</code> value means 'just unregister previously mapped action'
   * @param condition        one of
   *                         <ul>
   *                         <li>JComponent.WHEN_FOCUSED,</li>
   *                         <li>JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT</li>
   *                         <li>JComponent.WHEN_IN_FOCUSED_WINDOW</li>
   *                         <li>JComponent.UNDEFINED_CONDITION</li>
   *                         </ul>
   * @param muteOldKeystroke if <code>true</code> old keystroke wouldn't work anymore
   * @return <code>true</code> if the action is reassigned successfully
   */
  public static boolean reassignAction(@Nonnull JComponent component,
                                       @Nonnull KeyStroke oldKeyStroke,
                                       @Nullable KeyStroke newKeyStroke,
                                       int condition,
                                       boolean muteOldKeystroke) {
    ActionListener action = component.getActionForKeyStroke(oldKeyStroke);
    if (action == null) return false;
    if (newKeyStroke != null) {
      component.registerKeyboardAction(action, newKeyStroke, condition);
    }
    if (muteOldKeystroke) {
      component.registerKeyboardAction(new RedispatchEventAction(component), oldKeyStroke, condition);
    }
    return true;
  }

  private static final class RedispatchEventAction extends AbstractAction {
    private final Component myComponent;

    public RedispatchEventAction(Component component) {
      myComponent = component;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      AWTEvent event = EventQueue.getCurrentEvent();
      if (event instanceof KeyEvent && event.getSource() == myComponent) {
        Container parent = myComponent.getParent();
        if (parent != null) {
          KeyEvent keyEvent = (KeyEvent)event;
          parent.dispatchEvent(
                  new KeyEvent(parent, event.getID(), ((KeyEvent)event).getWhen(), keyEvent.getModifiers(), keyEvent.getKeyCode(), keyEvent.getKeyChar(),
                               keyEvent.getKeyLocation()));
        }
      }
    }
  }
}
