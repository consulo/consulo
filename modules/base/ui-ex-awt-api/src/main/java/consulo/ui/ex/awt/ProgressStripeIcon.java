/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.ui.ex.awt;

import consulo.ui.ex.Gray;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.awt.util.ColorUtil;
import consulo.ui.ex.awt.util.GraphicsUtil;
import consulo.ui.image.Image;
import consulo.util.collection.ContainerUtil;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;

// TODO [VISTALL] we need impl UI Image, due we don't have unified impl
public abstract class ProgressStripeIcon implements Icon, Image {
    private static final int TRANSLATE = 1;
    private static final int HEIGHT = 3;
    @Nonnull
    private final JComponent myReferenceComponent;
    private final int myShift;

    private ProgressStripeIcon(@Nonnull JComponent component, int shift) {
        myReferenceComponent = component;
        myShift = shift;
    }

    public abstract int getChunkWidth();

    protected abstract void paint(@Nonnull Graphics2D g2, int x, int y, int shift);

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        GraphicsConfig config = GraphicsUtil.setupAAPainting(g);
        Graphics2D g2 = (Graphics2D) g;

        int shift = myShift - getChunkWidth();
        while (shift < getIconWidth()) {
            paint(g2, x, y, shift);
            shift += getChunkWidth();
        }

        config.restore();
    }

    @Override
    public int getIconWidth() {
        return myReferenceComponent.getWidth();
    }

    @Override
    public int getIconHeight() {
        return getScaledHeight();
    }

    @Override
    public int getHeight() {
        return getIconHeight();
    }

    @Override
    public int getWidth() {
        return getIconWidth();
    }

    public static int getScaledHeight() {
        return JBUI.scale(HEIGHT);
    }

    private static class StripeIcon extends ProgressStripeIcon {
        private static final double ALPHA = 0.8;
        private static final JBColor BG_COLOR = new JBColor(ColorUtil.withAlpha(Gray._165, ALPHA), ColorUtil.withAlpha(Gray._110, ALPHA));
        private static final int WIDTH = 16;

        private StripeIcon(@Nonnull JComponent component, int shift) {
            super(component, shift);
        }

        @Override
        public int getChunkWidth() {
            return JBUI.scale(WIDTH);
        }

        @Override
        protected void paint(@Nonnull Graphics2D g2, int x, int y, int shift) {
            g2.setColor(BG_COLOR);

            Path2D.Double path = new Path2D.Double();
            int height = JBUI.scale(HEIGHT);
            float incline = height / 2.0f;
            float length = JBUI.scale(WIDTH) / 2.0f;
            float start = length / 2.0f;
            path.moveTo(x + shift + start, y + height);
            path.lineTo(x + shift + start + incline, y);
            path.lineTo(x + shift + start + incline + length, y);
            path.lineTo(x + shift + start + length, y + height);
            path.lineTo(x + shift + start, y + height);
            path.closePath();

            g2.fill(new Area(path));
        }
    }

    @Nonnull
    public static AsyncProcessIcon generateIcon(@Nonnull JComponent component) {
        List<Image> result = new ArrayList<>();

        for (int i = 0; i < JBUI.scale(StripeIcon.WIDTH); i += JBUI.scale(TRANSLATE)) {
            result.add(new StripeIcon(component, i));
        }
        result = ContainerUtil.reverse(result);

        Image passive = result.get(0);
        AsyncProcessIcon icon = new AsyncProcessIcon("ProgressWithStripes", result.toArray(new Image[result.size()]), passive) {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(component.getWidth(), passive.getHeight());
            }
        };
        component.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                super.componentResized(e);
                icon.revalidate();
            }
        });
        return icon;
    }
}
