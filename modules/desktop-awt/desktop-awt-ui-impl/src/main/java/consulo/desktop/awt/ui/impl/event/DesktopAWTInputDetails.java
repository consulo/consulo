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

import consulo.ui.Point2D;
import consulo.ui.event.details.*;
import consulo.util.lang.BitUtil;
import org.jspecify.annotations.Nullable;

import javax.swing.SwingUtilities;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2022-11-06
 */
public class DesktopAWTInputDetails {
    /**
     * Details of the event the EDT dispatches right now, for the listeners AWT hands no input event -
     * a selection, item or change listener sees only the fact of the change. Null when the change was
     * not driven by the user acting on a showing component - a programmatic call arrives outside of
     * mouse and key dispatch, or during construction while the component is not on the screen yet.
     */
    public static @Nullable InputDetails currentEvent(Component awtComponent) {
        AWTEvent event = EventQueue.getCurrentEvent();
        if (!awtComponent.isShowing()) {
            return null;
        }

        if (event instanceof MouseEvent mouseEvent) {
            Component source = mouseEvent.getComponent();
            if (source != null && source.isShowing()) {
                return convert(awtComponent, SwingUtilities.convertMouseEvent(source, mouseEvent, awtComponent));
            }
            return null;
        }

        if (event instanceof KeyEvent) {
            return convert(awtComponent, event);
        }

        return null;
    }

    public static InputDetails convert(Component awtComponent, AWTEvent event) {
        Set<ModifiedInputDetails.Modifier> modifiers = new HashSet<>();

        if (event instanceof ActionEvent actionEvent) {
            AWTEvent awtEvent = EventQueue.getCurrentEvent();

            Point2D position;
            Point2D positionOnScreen;
            if (awtEvent instanceof MouseEvent || awtEvent instanceof KeyEvent) {
                InputDetails details = convert(awtComponent, awtEvent);
                position = details.getPosition();
                positionOnScreen = details.getPositionOnScreen();
            }
            else {
                // an action fired outside of mouse and key dispatch - a programmatic click - has only the component to place it at
                position = new Point2D(0, 0);
                Point locationOnScreen = awtComponent.isShowing() ? awtComponent.getLocationOnScreen() : new Point(0, 0);
                positionOnScreen = new Point2D(locationOnScreen.x, locationOnScreen.y);
            }

            if (BitUtil.isSet(actionEvent.getModifiers(), ActionEvent.CTRL_MASK)) {
                modifiers.add(ModifiedInputDetails.Modifier.CTRL);
            }

            if (BitUtil.isSet(actionEvent.getModifiers(), ActionEvent.ALT_MASK)) {
                modifiers.add(ModifiedInputDetails.Modifier.ALT);
            }

            if (BitUtil.isSet(actionEvent.getModifiers(), ActionEvent.SHIFT_MASK)) {
                modifiers.add(ModifiedInputDetails.Modifier.SHIFT);
            }

            EnumSet<MouseInputDetails.Modifier> enumModifiers = modifiers.isEmpty() ? EnumSet.noneOf(ModifiedInputDetails.Modifier.class) : EnumSet.copyOf(modifiers);
            return new MouseInputDetails(position, positionOnScreen, enumModifiers, MouseInputDetails.MouseButton.LEFT);
        }
        else {
            if (event instanceof InputEvent inputEvent) {
                modifiers.addAll(toModifiers(inputEvent));
            }

            EnumSet<MouseInputDetails.Modifier> enumModifiers = modifiers.isEmpty() ? EnumSet.noneOf(ModifiedInputDetails.Modifier.class) : EnumSet.copyOf(modifiers);

            if (event instanceof MouseEvent mouseEvent) {
                MouseInputDetails.MouseButton button = MouseInputDetails.MouseButton.LEFT;
                if (mouseEvent.getButton() == MouseEvent.BUTTON2) {
                    button = MouseInputDetails.MouseButton.MIDDLE;
                }
                else if (mouseEvent.getButton() == MouseEvent.BUTTON3) {
                    button = MouseInputDetails.MouseButton.RIGHT;
                }

                Point2D relative = new Point2D(((MouseEvent) event).getX(), ((MouseEvent) event).getY());
                Point2D absolute = new Point2D(((MouseEvent) event).getXOnScreen(), ((MouseEvent) event).getYOnScreen());

                return new MouseInputDetails(relative, absolute, enumModifiers, button);
            }
            else if (event instanceof KeyEvent keyEvent) {
                Point2D pos = new Point2D(awtComponent.getX(), awtComponent.getY());
                Point locationOnScreen = awtComponent.getLocationOnScreen();
                Point2D posOnScreen = new Point2D(locationOnScreen.x, locationOnScreen.y);
                return new KeyboardInputDetails(pos, posOnScreen, enumModifiers, KeyCode.of(keyEvent.getKeyCode()));
            }
        }

        throw new UnsupportedOperationException("unknown event " + event);
    }

    /**
     * The modifiers alone, for the callers that have no component to place the event against - a global key
     * dispatcher sees events of windows it does not own, and a key event carries no position worth reporting.
     */
    public static EnumSet<ModifiedInputDetails.Modifier> toModifiers(InputEvent event) {
        EnumSet<ModifiedInputDetails.Modifier> modifiers = EnumSet.noneOf(ModifiedInputDetails.Modifier.class);

        int modifiersEx = event.getModifiersEx();
        if (BitUtil.isSet(modifiersEx, InputEvent.CTRL_DOWN_MASK)) {
            modifiers.add(ModifiedInputDetails.Modifier.CTRL);
        }
        if (BitUtil.isSet(modifiersEx, InputEvent.ALT_DOWN_MASK)) {
            modifiers.add(ModifiedInputDetails.Modifier.ALT);
        }
        if (BitUtil.isSet(modifiersEx, InputEvent.META_DOWN_MASK)) {
            modifiers.add(ModifiedInputDetails.Modifier.META);
        }
        if (BitUtil.isSet(modifiersEx, InputEvent.SHIFT_DOWN_MASK)) {
            modifiers.add(ModifiedInputDetails.Modifier.SHIFT);
        }

        return modifiers;
    }
}
