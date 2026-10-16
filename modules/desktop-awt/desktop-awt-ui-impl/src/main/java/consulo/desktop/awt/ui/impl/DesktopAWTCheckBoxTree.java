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
package consulo.desktop.awt.ui.impl;

import consulo.ui.Component;

import javax.swing.SwingUtilities;
import javax.swing.tree.TreeModel;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.util.function.Predicate;

/**
 * @author VISTALL
 * @since 2026-10-10
 */
public class DesktopAWTCheckBoxTree extends DesktopAWTTree {
    private final Predicate<MouseEvent> myCheckBoxPress;
    private final Predicate<Point> myOverCheckBox;

    private boolean myPressedOnCheckBox;

    public DesktopAWTCheckBoxTree(TreeModel model, Component owner, Predicate<MouseEvent> checkBoxPress, Predicate<Point> overCheckBox) {
        super(model, owner);
        myCheckBoxPress = checkBoxPress;
        myOverCheckBox = overCheckBox;
    }

    @Override
    public boolean isOverSelection(Point point) {
        return !myOverCheckBox.test(point) && super.isOverSelection(point);
    }

    @Override
    public void processMouseEvent(MouseEvent e) {
        switch (e.getID()) {
            case MouseEvent.MOUSE_PRESSED -> {
                myPressedOnCheckBox = SwingUtilities.isLeftMouseButton(e) && !e.isPopupTrigger() && myCheckBoxPress.test(e);
                if (myPressedOnCheckBox) {
                    e.consume();
                    return;
                }
            }
            case MouseEvent.MOUSE_RELEASED -> {
                if (myPressedOnCheckBox) {
                    e.consume();
                    return;
                }
            }
            case MouseEvent.MOUSE_CLICKED -> {
                if (myPressedOnCheckBox) {
                    myPressedOnCheckBox = false;
                    e.consume();
                    return;
                }
            }
            default -> {
            }
        }

        super.processMouseEvent(e);
    }
}
