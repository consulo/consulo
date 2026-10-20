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
package consulo.desktop.awt.ui.impl.popup;

import consulo.desktop.awt.ui.impl.DesktopGlassyBackdrop;
import consulo.desktop.awt.ui.impl.facade.FromSwingComponentWrapper;
import consulo.ui.Component;
import consulo.ui.ex.awt.JBUIScale;
import org.jspecify.annotations.Nullable;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

class DesktopAWTPopupPanel extends JPanel implements FromSwingComponentWrapper {
    private final DesktopAWTLightPopupImpl myPopup;

    private java.awt.@Nullable Component myContent;
    private @Nullable JLabel myTitle;
    private int myMinimumWidth;

    private DesktopAWTPopupSide mySide = DesktopAWTPopupSide.NONE;
    private int myTip;

    private @Nullable BufferedImage myShadow;
    private @Nullable Rectangle myShadowKey;
    private @Nullable DesktopAWTPopupSide myShadowSide;
    private int myShadowTip;

    DesktopAWTPopupPanel(DesktopAWTLightPopupImpl popup) {
        super(null);
        myPopup = popup;
        setOpaque(false);
    }

    @Override
    public Component toUIComponent() {
        return myPopup;
    }

    static int arrowLength() {
        return JBUIScale.scale(8);
    }

    static int arrowWidth() {
        return JBUIScale.scale(16);
    }

    static int arc() {
        int radius = UIManager.getInt("Popup.borderCornerRadius");
        return JBUIScale.scale(radius > 0 ? radius : 4) * 2;
    }

    void setPopupContent(java.awt.Component content) {
        if (myContent != null) {
            remove(myContent);
        }
        myContent = content;
        add(content);
        revalidate();
        repaint();
    }

    void setTitle(@Nullable String title) {
        if (title == null || title.isEmpty()) {
            if (myTitle != null) {
                remove(myTitle);
                myTitle = null;
            }
        }
        else {
            if (myTitle == null) {
                myTitle = new JLabel();
                myTitle.setBorder(new EmptyBorder(JBUIScale.scale(4), JBUIScale.scale(8), JBUIScale.scale(4), JBUIScale.scale(8)));
                add(myTitle);
            }
            myTitle.setText(title);
        }
        revalidate();
        repaint();
    }

    void setMinimumWidth(int width) {
        myMinimumWidth = Math.max(width, 0);
    }

    void setArrow(DesktopAWTPopupSide side, int tip) {
        mySide = side;
        myTip = tip;
        repaint();
    }

    Insets marginsFor(DesktopAWTPopupSide side) {
        Insets shadow = DesktopAWTPopupShadow.insets();
        int arrow = arrowLength();
        return new Insets(
            shadow.top + (side == DesktopAWTPopupSide.TOP ? arrow : 0),
            shadow.left + (side == DesktopAWTPopupSide.LEFT ? arrow : 0),
            shadow.bottom + (side == DesktopAWTPopupSide.BOTTOM ? arrow : 0),
            shadow.right + (side == DesktopAWTPopupSide.RIGHT ? arrow : 0)
        );
    }

    Dimension bodySize() {
        int width = myMinimumWidth;
        int height = 0;
        if (myTitle != null) {
            Dimension title = myTitle.getPreferredSize();
            width = Math.max(width, title.width);
            height += title.height;
        }
        if (myContent != null) {
            Dimension content = myContent.getPreferredSize();
            width = Math.max(width, content.width);
            height += content.height;
        }
        return new Dimension(width + 2, height + 2);
    }

    Dimension sizeFor(DesktopAWTPopupSide side) {
        Dimension body = bodySize();
        Insets margins = marginsFor(side);
        return new Dimension(body.width + margins.left + margins.right, body.height + margins.top + margins.bottom);
    }

    Rectangle body() {
        Insets margins = marginsFor(mySide);
        return new Rectangle(
            margins.left,
            margins.top,
            getWidth() - margins.left - margins.right,
            getHeight() - margins.top - margins.bottom
        );
    }

    @Override
    public Dimension getPreferredSize() {
        return sizeFor(mySide);
    }

    @Override
    public void doLayout() {
        Rectangle body = body();
        int x = body.x + 1;
        int y = body.y + 1;
        int width = body.width - 2;
        if (myTitle != null) {
            int titleHeight = myTitle.getPreferredSize().height;
            myTitle.setBounds(x, y, width, titleHeight);
            y += titleHeight;
        }
        if (myContent != null) {
            myContent.setBounds(x, y, width, Math.max(body.y + body.height - 1 - y, 0));
        }
    }

    private Shape outline(double inset) {
        Rectangle body = body();
        int arc = arc();
        Area area = new Area(new RoundRectangle2D.Double(
            body.x + inset,
            body.y + inset,
            body.width - 2 * inset,
            body.height - 2 * inset,
            arc,
            arc
        ));
        if (mySide == DesktopAWTPopupSide.NONE) {
            return area;
        }
        double half = arrowWidth() / 2.0;
        int length = arrowLength();
        Path2D.Double arrow = new Path2D.Double();
        switch (mySide) {
            case TOP -> {
                double base = body.y + inset + 1;
                arrow.moveTo(myTip - half, base);
                arrow.lineTo(myTip, body.y - length + inset);
                arrow.lineTo(myTip + half, base);
            }
            case BOTTOM -> {
                double base = body.y + body.height - inset - 1;
                arrow.moveTo(myTip - half, base);
                arrow.lineTo(myTip, body.y + body.height + length - inset);
                arrow.lineTo(myTip + half, base);
            }
            case LEFT -> {
                double base = body.x + inset + 1;
                arrow.moveTo(base, myTip - half);
                arrow.lineTo(body.x - length + inset, myTip);
                arrow.lineTo(base, myTip + half);
            }
            case RIGHT -> {
                double base = body.x + body.width - inset - 1;
                arrow.moveTo(base, myTip - half);
                arrow.lineTo(body.x + body.width + length - inset, myTip);
                arrow.lineTo(base, myTip + half);
            }
            default -> {
            }
        }
        arrow.closePath();
        area.add(new Area(arrow));
        return area;
    }

    void dropShadow() {
        myShadow = null;
    }

    private BufferedImage shadow() {
        Rectangle key = new Rectangle(getWidth(), getHeight());
        BufferedImage shadow = myShadow;
        if (shadow == null || !key.equals(myShadowKey) || mySide != myShadowSide || myTip != myShadowTip) {
            shadow = DesktopAWTPopupShadow.render(outline(0), getWidth(), getHeight());
            if (myPopup.isGlassy()) {
                Graphics2D cut = shadow.createGraphics();
                try {
                    cut.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    cut.setComposite(AlphaComposite.Clear);
                    cut.fill(outline(1));
                }
                finally {
                    cut.dispose();
                }
            }
            myShadow = shadow;
            myShadowKey = key;
            myShadowSide = mySide;
            myShadowTip = myTip;
        }
        return shadow;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.drawImage(shadow(), 0, 0, null);
            Color fill = myPopup.fillColor();
            if (myPopup.isGlassy()) {
                DesktopGlassyBackdrop.paint(this, g, outline(0));
                fill = DesktopGlassyBackdrop.fill(fill);
            }
            g.setColor(fill);
            g.fill(outline(0));
            g.setColor(myPopup.borderColor());
            g.setStroke(new BasicStroke(1));
            g.draw(outline(0.5));
        }
        finally {
            g.dispose();
        }
    }

    @Override
    protected void paintChildren(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.clip(outline(1));
            super.paintChildren(g);
        }
        finally {
            g.dispose();
        }
    }
}
