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
package consulo.desktop.awt.ui.impl.event;

import consulo.ui.Position2D;
import consulo.ui.event.details.*;
import consulo.util.lang.BitUtil;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

/**
 * @author VISTALL
 * @since 06/11/2022
 */
public class DesktopAWTInputDetails {
    public static InputDetails convert(InputEvent event) {
        Set<ModifiedInputDetails.Modifier> modifiers = new HashSet<>();
        if (BitUtil.isSet(event.getModifiersEx(), MouseEvent.CTRL_DOWN_MASK)) {
            modifiers.add(ModifiedInputDetails.Modifier.CTRL);
        }

        if (BitUtil.isSet(event.getModifiersEx(), MouseEvent.ALT_DOWN_MASK)) {
            modifiers.add(ModifiedInputDetails.Modifier.ALT);
        }

        if (BitUtil.isSet(event.getModifiersEx(), MouseEvent.SHIFT_DOWN_MASK)) {
            modifiers.add(ModifiedInputDetails.Modifier.SHIFT);
        }

        EnumSet<MouseInputDetails.Modifier> enumModifiers = modifiers.isEmpty() ? EnumSet.noneOf(ModifiedInputDetails.Modifier.class) : EnumSet.copyOf(modifiers);

        if (event instanceof MouseEvent) {
            int x = ((MouseEvent) event).getX();

            MouseInputDetails.MouseButton button = MouseInputDetails.MouseButton.LEFT;
            if (((MouseEvent) event).getButton() == MouseEvent.BUTTON2) {
                button = MouseInputDetails.MouseButton.MIDDLE;
            }
            else if (((MouseEvent) event).getButton() == MouseEvent.BUTTON3) {
                button = MouseInputDetails.MouseButton.RIGHT;
            }

            Position2D relative = new Position2D(((MouseEvent) event).getX(), ((MouseEvent) event).getY());
            Position2D absolute = new Position2D(((MouseEvent) event).getXOnScreen(), ((MouseEvent) event).getYOnScreen());

            return new MouseInputDetails(relative, absolute, enumModifiers, button);
        }
        else if (event instanceof KeyEvent keyEvent) {
            java.awt.Component component = keyEvent.getComponent();

            Position2D pos = new Position2D(component.getX(), component.getY());

            Point locationOnScreen = component.getLocationOnScreen();
            Position2D posOnScreen = new Position2D(locationOnScreen.x, locationOnScreen.y);
            return new KeyboardInputDetails(pos, posOnScreen, enumModifiers, KeyChar.of(keyEvent.getKeyChar()));
        }

        throw new UnsupportedOperationException("unknown event " + event);
    }
}
