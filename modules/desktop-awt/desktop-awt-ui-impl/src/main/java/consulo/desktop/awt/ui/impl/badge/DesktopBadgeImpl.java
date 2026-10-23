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

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.util.EnumSet;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2026-10-20
 */
public class DesktopBadgeImpl extends SwingComponentDelegate<DesktopBadgeImpl.BadgePanel> implements Badge {
    public class BadgePanel extends JPanel implements FromSwingComponentWrapper {
        private final JBLabel myLabel = new JBLabel();

        public BadgePanel() {
            super(new BorderLayout(JBUI.scale(BadgePresentation.HORIZONTAL_PADDING) / 2, 0));
            setOpaque(false);
            myLabel.setOpaque(false);
            add(myLabel, BorderLayout.CENTER);
        }

        @Override
        public Component toUIComponent() {
            return DesktopBadgeImpl.this;
        }

        @Override
        public void updateUI() {
            super.updateUI();
            if (myStyles != null && myLabel != null) {
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
    private @Nullable Component myPrefixComponent;
    private @Nullable Component mySuffixComponent;

    public DesktopBadgeImpl(LocalizeValue text) {
        myText = text;
        toAWTComponent().myLabel.setText(text.get());
        updateLook();
    }

    @Override
    protected BadgePanel createComponent() {
        return new BadgePanel();
    }

    private void updateLook() {
        applyLook(toAWTComponent());
    }

    private void applyLook(BadgePanel panel) {
        JBLabel label = panel.myLabel;
        label.setForeground(TargetAWT.to(BadgePresentation.foreground(myStyles)));
        Font base = UIUtil.getLabelFont();
        label.setFont(base.deriveFont(base.getSize2D() + JBUI.scale((float) BadgePresentation.fontPointDelta(myStyles))));
        panel.setBorder(JBUI.Borders.empty(BadgePresentation.VERTICAL_PADDING, BadgePresentation.HORIZONTAL_PADDING));
        panel.repaint();
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
        toAWTComponent().myLabel.setText(text.get());
    }

    @Override
    public @Nullable Image getIcon() {
        return myIcon;
    }

    @RequiredUIAccess
    @Override
    public void setIcon(@Nullable Image icon) {
        myIcon = icon;
        toAWTComponent().myLabel.setIcon(TargetAWT.to(icon));
    }

    @Override
    public void setPrefixComponent(@Nullable Component prefixComponent) {
        replaceSide(myPrefixComponent, prefixComponent, BorderLayout.WEST);
        myPrefixComponent = prefixComponent;
    }

    @Override
    public @Nullable Component getPrefixComponent() {
        return myPrefixComponent;
    }

    @Override
    public void setSuffixComponent(@Nullable Component suffixComponent) {
        replaceSide(mySuffixComponent, suffixComponent, BorderLayout.EAST);
        mySuffixComponent = suffixComponent;
    }

    @Override
    public @Nullable Component getSuffixComponent() {
        return mySuffixComponent;
    }

    private void replaceSide(@Nullable Component oldComponent, @Nullable Component newComponent, String position) {
        BadgePanel panel = toAWTComponent();
        if (oldComponent != null) {
            panel.remove(TargetAWT.to(oldComponent));
        }
        if (newComponent != null) {
            java.awt.Component awtComponent = TargetAWT.to(newComponent);
            if (awtComponent instanceof JComponent jComponent) {
                jComponent.setOpaque(false);
            }
            panel.add(awtComponent, position);
        }
        panel.revalidate();
        panel.repaint();
    }
}
