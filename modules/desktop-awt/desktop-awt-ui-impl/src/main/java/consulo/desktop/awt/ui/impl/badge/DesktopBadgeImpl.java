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
package consulo.desktop.awt.ui.impl.badge;

import consulo.desktop.awt.ui.impl.base.SwingComponentDelegate;
import consulo.desktop.awt.ui.impl.facade.FromSwingComponentWrapper;
import consulo.localize.LocalizeValue;
import consulo.ui.Badge;
import consulo.ui.BadgeStyle;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.JBLabel;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.image.Image;
import consulo.ui.impl.BadgePresentation;
import org.jspecify.annotations.Nullable;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.util.EnumSet;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2026-10-20
 */
public class DesktopBadgeImpl extends SwingComponentDelegate<DesktopBadgeImpl.BadgeLabel> implements Badge {
    public class BadgeLabel extends JBLabel implements FromSwingComponentWrapper {
        @Override
        public Component toUIComponent() {
            return DesktopBadgeImpl.this;
        }

        @Override
        public void updateUI() {
            super.updateUI();
            if (myStyles != null) {
                applyLook(this);
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                float arc = JBUI.scale(BadgePresentation.ARC);
                RoundRectangle2D shape = new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1f, getHeight() - 1f, arc, arc);
                g2.setColor(TargetAWT.to(BadgePresentation.background(myStyles)));
                g2.fill(shape);
                g2.setColor(TargetAWT.to(BadgePresentation.border(myStyles)));
                g2.draw(shape);
            }
            finally {
                g2.dispose();
            }
            super.paintComponent(g);
        }
    }

    private final Set<BadgeStyle> myStyles = EnumSet.noneOf(BadgeStyle.class);
    private LocalizeValue myText;
    private @Nullable Image myIcon;

    public DesktopBadgeImpl(LocalizeValue text) {
        myText = text;
        BadgeLabel label = toAWTComponent();
        label.setOpaque(false);
        label.setText(text.get());
        updateLook();
    }

    @Override
    protected BadgeLabel createComponent() {
        return new BadgeLabel();
    }

    private void updateLook() {
        applyLook(toAWTComponent());
    }

    private void applyLook(BadgeLabel label) {
        label.setForeground(TargetAWT.to(BadgePresentation.foreground(myStyles)));
        Font base = UIUtil.getLabelFont();
        label.setFont(base.deriveFont(base.getSize2D() + JBUI.scale((float) BadgePresentation.fontPointDelta(myStyles))));
        label.setBorder(JBUI.Borders.empty(BadgePresentation.VERTICAL_PADDING, BadgePresentation.HORIZONTAL_PADDING));
        label.repaint();
    }

    @Override
    public void addStyle(BadgeStyle style) {
        if (style != BadgeStyle.FILLED && style != BadgeStyle.SMALL) {
            myStyles.removeIf(each -> each != BadgeStyle.FILLED && each != BadgeStyle.SMALL);
        }
        myStyles.add(style);
        updateLook();
    }

    @Override
    public LocalizeValue getText() {
        return myText;
    }

    @RequiredUIAccess
    @Override
    public void setText(LocalizeValue text) {
        myText = text;
        toAWTComponent().setText(text.get());
    }

    @Override
    public @Nullable Image getIcon() {
        return myIcon;
    }

    @RequiredUIAccess
    @Override
    public void setIcon(@Nullable Image icon) {
        myIcon = icon;
        toAWTComponent().setIcon(TargetAWT.to(icon));
    }
}
