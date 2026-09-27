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

import consulo.ui.DelayedAction;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.image.Image;
import consulo.util.dataholder.Key;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * @author VISTALL
 * @since 2026-09-27
 */
public final class DesktopDelayedActionImpl implements DelayedAction {
    private record GlassPaneState(boolean wasVisible, int spinnerCount) {
    }

    private static final Key<GlassPaneState> GLASS_PANE_STATE = Key.create("DesktopDelayedActionImpl.glassPaneState");

    private static final DelayedAction EMPTY = () -> {
    };

    @RequiredUIAccess
    public static DelayedAction start(Component component, int x, int y) {
        JComponent glassPane = findGlassPane(component);
        if (glassPane == null) {
            return EMPTY;
        }
        return new DesktopDelayedActionImpl(glassPane, SwingUtilities.convertPoint(component, x, y, glassPane));
    }

    @RequiredUIAccess
    public static DelayedAction startOnScreen(Component component, int xOnScreen, int yOnScreen) {
        JComponent glassPane = findGlassPane(component);
        if (glassPane == null) {
            return EMPTY;
        }
        Point point = new Point(xOnScreen, yOnScreen);
        SwingUtilities.convertPointFromScreen(point, glassPane);
        return new DesktopDelayedActionImpl(glassPane, point);
    }

    private static @Nullable JComponent findGlassPane(Component component) {
        JRootPane rootPane = UIUtil.getRootPane(component);
        if (rootPane != null && rootPane.getGlassPane() instanceof JComponent glassPane) {
            return glassPane;
        }
        return null;
    }

    private final JComponent myGlassPane;
    private final JLabel mySpinner;
    private boolean myStopped;

    @RequiredUIAccess
    private DesktopDelayedActionImpl(JComponent glassPane, Point center) {
        myGlassPane = glassPane;
        mySpinner = new JLabel(TargetAWT.to(Image.busy()));
        Dimension size = mySpinner.getPreferredSize();
        mySpinner.setBounds(center.x - size.width / 2, center.y - size.height / 2, size.width, size.height);

        GlassPaneState state = glassPane.getClientProperty(GLASS_PANE_STATE) instanceof GlassPaneState current
            ? new GlassPaneState(current.wasVisible(), current.spinnerCount() + 1)
            : new GlassPaneState(glassPane.isVisible(), 1);
        glassPane.putClientProperty(GLASS_PANE_STATE, state);

        glassPane.add(mySpinner);
        glassPane.setVisible(true);
        glassPane.repaint(mySpinner.getBounds());
    }

    @Override
    @RequiredUIAccess
    public void stop() {
        if (myStopped) {
            return;
        }
        myStopped = true;

        Rectangle bounds = mySpinner.getBounds();
        myGlassPane.remove(mySpinner);

        if (myGlassPane.getClientProperty(GLASS_PANE_STATE) instanceof GlassPaneState state) {
            if (state.spinnerCount() > 1) {
                myGlassPane.putClientProperty(GLASS_PANE_STATE, new GlassPaneState(state.wasVisible(), state.spinnerCount() - 1));
            }
            else {
                myGlassPane.putClientProperty(GLASS_PANE_STATE, null);
                myGlassPane.setVisible(state.wasVisible());
            }
        }
        myGlassPane.repaint(bounds);
    }
}
