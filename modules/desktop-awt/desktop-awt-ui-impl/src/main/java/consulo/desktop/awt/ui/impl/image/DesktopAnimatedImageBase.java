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
package consulo.desktop.awt.ui.impl.image;

import com.formdev.flatlaf.FlatLaf;
import consulo.ui.ex.awt.util.ComponentUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * @author VISTALL
 * @since 2026-09-27
 */
public abstract class DesktopAnimatedImageBase extends AnimatedIcon implements DesktopAWTImage, FlatLaf.DisabledIconProvider {
    private final class FrameIcon implements AnimatedIcon.Frame, Icon {
        private final int myIndex;

        private FrameIcon(int index) {
            myIndex = index;
        }

        @Override
        public Icon getIcon() {
            return this;
        }

        @Override
        public int getDelay() {
            return getFrameDelay(myIndex);
        }

        @Override
        public void paintIcon(@Nullable Component c, Graphics g, int x, int y) {
            Icon icon = isAnimated(c) ? getFrameIcon(myIndex) : getStillIcon();
            icon.paintIcon(c, g, x, y);
        }

        @Override
        public int getIconWidth() {
            return DesktopAnimatedImageBase.this.getWidth();
        }

        @Override
        public int getIconHeight() {
            return DesktopAnimatedImageBase.this.getHeight();
        }
    }

    protected DesktopAnimatedImageBase(int frameCount) {
        super(icon -> ((DesktopAnimatedImageBase) icon).createFrames(frameCount));
    }

    protected abstract Icon getFrameIcon(int index);

    protected abstract int getFrameDelay(int index);

    protected abstract Icon getStillIcon();

    @Override
    public abstract DesktopAWTImage copyGrayed();

    @Override
    public Icon getDisabledIcon() {
        return TargetAWT.to(copyGrayed());
    }

    @Override
    protected @Nullable Component getRendererOwner(@Nullable Component component) {
        Component owner = component;
        while (owner != null) {
            CellRendererPane pane = ComponentUtil.getParentOfType(CellRendererPane.class, owner);
            if (pane == null) {
                return owner;
            }
            owner = pane.getParent();
        }
        return null;
    }

    private boolean isAnimated(@Nullable Component component) {
        if (component == null) {
            return false;
        }

        if (component.isShowing()) {
            return true;
        }

        CellRendererPane pane = ComponentUtil.getParentOfType(CellRendererPane.class, component);
        if (pane == null) {
            return false;
        }

        Component owner = getRendererOwner(pane.getParent());
        return owner != null && owner.isShowing();
    }

    private AnimatedIcon.Frame[] createFrames(int count) {
        AnimatedIcon.Frame[] frames = new AnimatedIcon.Frame[count];
        for (int i = 0; i < count; i++) {
            frames[i] = new FrameIcon(i);
        }
        return frames;
    }
}
