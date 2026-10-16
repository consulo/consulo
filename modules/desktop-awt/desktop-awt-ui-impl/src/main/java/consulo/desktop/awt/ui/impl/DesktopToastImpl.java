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

import consulo.desktop.awt.ui.impl.base.SwingComponentDelegate;
import consulo.desktop.awt.ui.impl.event.DesktopAWTInputDetails;
import consulo.desktop.awt.ui.impl.facade.FromSwingComponentWrapper;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.Toast;
import consulo.ui.ToastStyle;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.HyperlinkEvent;
import consulo.ui.ex.awt.JBFont;
import consulo.ui.ex.awt.JBHtmlEditorKit;
import consulo.ui.ex.awt.JBLabel;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.LinkLabel;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.VerticalLayout;
import consulo.ui.ex.awt.event.HoverStateListener;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.image.Image;
import consulo.ui.impl.ToastPresentation;
import consulo.ui.internal.ToastInternal;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;

/**
 * @author VISTALL
 * @since 2026-10-16
 */
public class DesktopToastImpl extends SwingComponentDelegate<JPanel> implements ToastInternal {
    private static final int ARC = 20;
    private static final int SHADOW_SIZE = 10;
    private static final int SHADOW_BLUR = 8;
    private static final int SHADOW_OFFSET = 2;

    private class MyToastPanel extends JPanel implements FromSwingComponentWrapper {
        private @Nullable BufferedImage myShadow;

        MyToastPanel() {
            super(new BorderLayout(JBUI.scale(8), 0));
            setOpaque(false);
        }

        @Override
        public Component toUIComponent() {
            return DesktopToastImpl.this;
        }

        @Override
        public void updateUI() {
            super.updateUI();
            myShadow = null;
            updateInsets(this);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int shadow = shadowSize();
                int width = getWidth() - 2 * shadow;
                int height = getHeight() - 2 * shadow;
                if (width <= 0 || height <= 0) {
                    return;
                }

                if (myShadowVisible) {
                    if (myShadow == null || myShadow.getWidth() != getWidth() || myShadow.getHeight() != getHeight()) {
                        myShadow = createShadow(width, height);
                    }
                    g.drawImage(myShadow, 0, 0, null);
                }

                int arc = JBUI.scale(ARC);
                g.translate(shadow, shadow);
                g.setColor(TargetAWT.to(ToastPresentation.background(myStyle)));
                g.fill(new RoundRectangle2D.Float(0, 0, width, height, arc, arc));
                g.setColor(TargetAWT.to(ToastPresentation.border(myStyle)));
                g.draw(new RoundRectangle2D.Float(0.5f, 0.5f, width - 1, height - 1, arc, arc));
            }
            finally {
                g.dispose();
            }
        }
    }

    private class MyToolbarPanel extends JPanel {
        MyToolbarPanel() {
            super(new BorderLayout());
            setOpaque(false);
        }

        @Override
        protected void paintChildren(Graphics g) {
            if (myHovered) {
                super.paintChildren(g);
            }
        }
    }

    private static class MyTitleLabel extends JBLabel {
        private LocalizeValue myValue = LocalizeValue.empty();

        void setValue(LocalizeValue value) {
            myValue = value;
            setText(value.get());
            setVisible(value != LocalizeValue.empty());
        }

        @Override
        public void updateUI() {
            super.updateUI();
            setFont(JBFont.label().asBold());
            if (myValue != null) {
                setText(myValue.get());
            }
        }
    }

    private static class MyTextArea extends JTextArea {
        private LocalizeValue myValue = LocalizeValue.empty();

        MyTextArea() {
            setEditable(false);
            setFocusable(false);
            setLineWrap(true);
            setWrapStyleWord(true);
            setOpaque(false);
            setBorder(null);
        }

        @Override
        public void updateUI() {
            super.updateUI();
            setFont(UIUtil.getLabelFont());
            setForeground(UIUtil.getLabelForeground());
            setOpaque(false);
            setBorder(null);
            if (myValue != null) {
                setText(myValue.get());
            }
        }

        void setValue(LocalizeValue value) {
            myValue = value;
            setText(value.get());
            setVisible(value != LocalizeValue.empty());
        }
    }

    private class MyHtmlPane extends JEditorPane {
        private LocalizeValue myValue = LocalizeValue.empty();

        MyHtmlPane() {
            setEditorKit(JBHtmlEditorKit.create());
            setEditable(false);
            setFocusable(false);
            setOpaque(false);
            setBorder(null);
            putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
            addHyperlinkListener(event -> {
                if (event.getEventType() == javax.swing.event.HyperlinkEvent.EventType.ACTIVATED) {
                    getListenerDispatcher(HyperlinkEvent.class).onEvent(new HyperlinkEvent(
                        DesktopToastImpl.this,
                        event.getDescription(),
                        DesktopAWTInputDetails.convert(this, event.getInputEvent())
                    ));
                }
            });
        }

        @Override
        public void updateUI() {
            super.updateUI();
            setFont(UIUtil.getLabelFont());
            setForeground(UIUtil.getLabelForeground());
            setOpaque(false);
            setBorder(null);
            if (myValue != null && myValue != LocalizeValue.empty()) {
                setText(myValue.get());
            }
        }

        void setValue(LocalizeValue value) {
            myValue = value;
            setText(value == LocalizeValue.empty() ? "" : value.get());
            setVisible(value != LocalizeValue.empty());
        }
    }

    private final JLabel myIconLabel = new JLabel();
    private final MyTitleLabel myTitleLabel = new MyTitleLabel();
    private final MyTextArea myTextArea = new MyTextArea();
    private final MyHtmlPane myHtmlPane = new MyHtmlPane();
    private final JPanel myActionsPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
    private final MyToolbarPanel myToolbarPanel = new MyToolbarPanel();

    private @Nullable ToastStyle myStyle;
    private @Nullable Image myIcon;
    private boolean myShadowVisible;
    private boolean myHovered;

    public DesktopToastImpl() {
        myActionsPanel.setOpaque(false);
        myTitleLabel.setValue(LocalizeValue.empty());
        myTextArea.setValue(LocalizeValue.empty());
        myHtmlPane.setValue(LocalizeValue.empty());
        myActionsPanel.setVisible(false);
        myIconLabel.setVerticalAlignment(SwingConstants.TOP);
        updateIcon();
    }

    @Override
    protected JPanel createComponent() {
        MyToastPanel panel = new MyToastPanel();

        JPanel iconHolder = new JPanel(new BorderLayout());
        iconHolder.setOpaque(false);
        iconHolder.setBorder(JBUI.Borders.emptyTop(1));
        iconHolder.add(myIconLabel, BorderLayout.NORTH);
        panel.add(iconHolder, BorderLayout.WEST);

        JPanel center = new JPanel(new VerticalLayout(JBUI.scale(2)));
        center.setOpaque(false);
        center.add(myTitleLabel);
        center.add(myTextArea);
        center.add(myHtmlPane);
        center.add(myActionsPanel);
        panel.add(center, BorderLayout.CENTER);

        JPanel toolbarHolder = new JPanel(new BorderLayout());
        toolbarHolder.setOpaque(false);
        toolbarHolder.add(myToolbarPanel, BorderLayout.NORTH);
        panel.add(toolbarHolder, BorderLayout.EAST);

        new HoverStateListener() {
            @Override
            protected void hoverChanged(java.awt.Component component, boolean hovered) {
                setHovered(hovered);
            }
        }.addTo(panel);

        updateInsets(panel);
        return panel;
    }

    @Override
    public void addStyle(ToastStyle style) {
        myStyle = style;
        updateIcon();
        toAWTComponent().repaint();
    }

    @RequiredUIAccess
    @Override
    public Toast setTitle(LocalizeValue title) {
        myTitleLabel.setValue(title);
        return this;
    }

    @RequiredUIAccess
    @Override
    public Toast setText(LocalizeValue text) {
        myHtmlPane.setValue(LocalizeValue.empty());
        myTextArea.setValue(text);
        return this;
    }

    @RequiredUIAccess
    @Override
    public Toast setHtmlText(LocalizeValue html) {
        myTextArea.setValue(LocalizeValue.empty());
        myHtmlPane.setValue(html);
        return this;
    }

    @RequiredUIAccess
    @Override
    public Toast setIcon(@Nullable Image icon) {
        myIcon = icon;
        updateIcon();
        return this;
    }

    @RequiredUIAccess
    @Override
    public Toast addAction(LocalizeValue text, @RequiredUIAccess Runnable action) {
        LinkLabel<?> link = LinkLabel.create(text.get(), action);
        if (myActionsPanel.getComponentCount() > 0) {
            link.setBorder(JBUI.Borders.emptyLeft(14));
        }
        myActionsPanel.add(link);
        myActionsPanel.setVisible(true);
        revalidate();
        return this;
    }

    @RequiredUIAccess
    @Override
    public Toast setToolbarComponent(@Nullable Component toolbar) {
        myToolbarPanel.removeAll();
        if (toolbar != null) {
            myToolbarPanel.add(TargetAWT.to(toolbar), BorderLayout.CENTER);
        }
        revalidate();
        return this;
    }

    private void setHovered(boolean hovered) {
        myHovered = hovered;
        myToolbarPanel.repaint();
    }

    @RequiredUIAccess
    @Override
    public void setShadow(boolean shadow) {
        myShadowVisible = shadow;
        updateInsets(toAWTComponent());
        revalidate();
    }

    private int shadowSize() {
        return myShadowVisible ? JBUI.scale(SHADOW_SIZE) : 0;
    }

    private void updateInsets(JPanel panel) {
        int shadow = shadowSize();
        panel.setBorder(BorderFactory.createEmptyBorder(JBUI.scale(8) + shadow, JBUI.scale(10) + shadow, JBUI.scale(8) + shadow, JBUI.scale(8) + shadow));
    }

    private void updateIcon() {
        Image icon = myIcon != null ? myIcon : ToastPresentation.icon(myStyle);
        myIconLabel.setIcon(icon == null ? null : TargetAWT.to(icon));
        myIconLabel.setVisible(icon != null);
    }

    private void revalidate() {
        JPanel panel = toAWTComponent();
        panel.revalidate();
        panel.repaint();
    }

    private BufferedImage createShadow(int width, int height) {
        int shadow = shadowSize();
        int blur = JBUI.scale(SHADOW_BLUR);
        int pad = blur * 2;
        int arc = JBUI.scale(ARC);
        BufferedImage mask = new BufferedImage(width + 2 * shadow + 2 * pad, height + 2 * shadow + 2 * pad, BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D g = mask.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(0, 0, 0, UIUtil.isUnderDarcula() ? 140 : 70));
            g.fill(new RoundRectangle2D.Float(pad + shadow, pad + shadow + JBUI.scale(SHADOW_OFFSET), width, height, arc, arc));
        }
        finally {
            g.dispose();
        }

        float[] kernel = gaussian(blur);
        ConvolveOp horizontal = new ConvolveOp(new Kernel(kernel.length, 1, kernel), ConvolveOp.EDGE_NO_OP, null);
        ConvolveOp vertical = new ConvolveOp(new Kernel(1, kernel.length, kernel), ConvolveOp.EDGE_NO_OP, null);
        BufferedImage blurred = vertical.filter(horizontal.filter(mask, null), null);
        return blurred.getSubimage(pad, pad, width + 2 * shadow, height + 2 * shadow);
    }

    private static float[] gaussian(int radius) {
        double sigma = radius / 2.0;
        float[] kernel = new float[radius * 2 + 1];
        float sum = 0;
        for (int i = -radius; i <= radius; i++) {
            float value = (float) Math.exp(-(i * i) / (2 * sigma * sigma));
            kernel[i + radius] = value;
            sum += value;
        }
        for (int i = 0; i < kernel.length; i++) {
            kernel[i] /= sum;
        }
        return kernel;
    }
}
