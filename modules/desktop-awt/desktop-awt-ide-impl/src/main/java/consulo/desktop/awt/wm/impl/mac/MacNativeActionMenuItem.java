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
package consulo.desktop.awt.wm.impl.mac;

import consulo.application.Application;
import consulo.application.ui.UISettings;
import consulo.dataContext.DataContext;
import consulo.desktop.awt.ui.mac.screenmenu.MenuItem;
import consulo.ui.ex.action.*;
import consulo.ui.ex.action.util.ShortcutUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.keymap.KeymapManager;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

/**
 * Builds a native {@link MenuItem} peer for a leaf {@link AnAction}.
 *
 * @author VISTALL
 */
public final class MacNativeActionMenuItem {
    private MacNativeActionMenuItem() {
    }

    public static MenuItem create(AnAction action,
                                  Presentation presentation,
                                  String place,
                                  DataContext context,
                                  boolean insideCheckedGroup,
                                  @Nullable Component contextComponent) {
        MenuItem item = new MenuItem();

        String text = MacScreenMenuFiller.menuText(presentation);
        KeyStroke accelerator = findAccelerator(action);
        item.setLabel(text, accelerator);
        // AppKit's key equivalent renders only the mac glyph form (unicode). When unicode shortcuts are off the user
        // wants the text form, which AppKit can't draw, so every item is given a custom item view (with the shortcut
        // text, or empty) for a consistent style; the key equivalent above still triggers the action.
        if (MacScreenMenuFiller.isTextShortcutMode()) {
            item.setAcceleratorText(accelerator != null ? ShortcutUtil.getKeystrokeText(accelerator, false) : "");
        }
        item.setEnabled(presentation.isEnabled());

        if (action instanceof Toggleable || insideCheckedGroup) {
            item.setState(Toggleable.isSelected(presentation));
        }

        if (MacScreenMenuFiller.SHOW_ICONS && UISettings.getInstance().SHOW_ICONS_IN_MENUS) {
            Image icon = presentation.getIcon();
            if (icon != null) {
                item.setIcon(TargetAWT.to(icon));
            }
        }

        // the delegate is invoked on the AppKit thread; the action must run on the EDT
        item.setActionDelegate(() -> Application.get().invokeLater(() -> execute(action, place, contextComponent)));
        return item;
    }

    private static @Nullable KeyStroke findAccelerator(AnAction action) {
        Shortcut[] shortcuts = null;
        String id = ActionManager.getInstance().getId(action);
        if (id != null) {
            shortcuts = KeymapManager.getInstance().getActiveKeymap().getShortcuts(id);
        }
        else {
            ShortcutSet shortcutSet = action.getShortcutSet();
            if (shortcutSet != null) {
                shortcuts = shortcutSet.getShortcuts();
            }
        }

        if (shortcuts != null) {
            for (Shortcut shortcut : shortcuts) {
                if (shortcut instanceof KeyboardShortcut keyboardShortcut) {
                    // a multi-stroke chord can't be an AppKit key equivalent, so show nothing for it
                    if (keyboardShortcut.getSecondKeyStroke() != null) {
                        return null;
                    }
                    KeyStroke firstKeyStroke = keyboardShortcut.getFirstKeyStroke();
                    // skip a bare Enter, otherwise no other item can be chosen with the keyboard
                    if (firstKeyStroke.getKeyCode() == KeyEvent.VK_ENTER && firstKeyStroke.getModifiers() == 0) {
                        return null;
                    }
                    return firstKeyStroke;
                }
            }
        }
        return null;
    }

    private static void execute(AnAction action, String place, @Nullable Component contextComponent) {
        Component source = contextComponent != null ? contextComponent : JOptionPane.getRootFrame();
        InputEvent inputEvent = new MouseEvent(source, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(), 0, 0, 0, 1, false);
        ActionManager.getInstance().tryToExecute(action, inputEvent, contextComponent, place, false);
    }
}
