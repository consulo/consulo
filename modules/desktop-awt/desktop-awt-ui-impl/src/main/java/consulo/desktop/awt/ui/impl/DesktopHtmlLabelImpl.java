/*
 * Copyright 2013-2016 consulo.io
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

import consulo.desktop.awt.ui.impl.event.DesktopAWTInputDetails;
import consulo.localize.LocalizeValue;
import consulo.ui.HtmlLabel;
import consulo.ui.LabelOptions;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.HyperlinkEvent;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import javax.swing.plaf.basic.BasicHTML;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Position;
import javax.swing.text.View;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * @author VISTALL
 * @since 2016-06-12
 */
class DesktopHtmlLabelImpl extends DesktopLabelImpl implements HtmlLabel {
    public DesktopHtmlLabelImpl(LocalizeValue localizeValue, LabelOptions options) {
        super(wrap(localizeValue), options);
    }

    private static LocalizeValue wrap(LocalizeValue html) {
        return html.map(value -> "<html><body>" + value + "</body></html>");
    }

    @RequiredUIAccess
    @Override
    public void setText(LocalizeValue text) {
        super.setText(wrap(text));
    }

    @Override
    protected boolean isMnemonicParsed() {
        return false;
    }

    @Override
    protected MyJLabel createComponent() {
        MyJLabel label = super.createComponent();

        MouseAdapter linkTracker = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() != MouseEvent.BUTTON1) {
                    return;
                }

                String href = findHref(label, e.getPoint());
                if (href != null) {
                    getListenerDispatcher(HyperlinkEvent.class)
                        .onEvent(new HyperlinkEvent(DesktopHtmlLabelImpl.this, href, DesktopAWTInputDetails.convert(label, e)));
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                label.setCursor(findHref(label, e.getPoint()) != null ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : null);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                label.setCursor(null);
            }
        };
        label.addMouseListener(linkTracker);
        label.addMouseMotionListener(linkTracker);
        return label;
    }

    private static @Nullable String findHref(JLabel label, Point point) {
        if (!(label.getClientProperty(BasicHTML.propertyKey) instanceof View view)
            || !(view.getDocument() instanceof HTMLDocument document)) {
            return null;
        }

        Rectangle textBounds = textBounds(label);
        if (!textBounds.contains(point)) {
            return null;
        }

        int offset = view.viewToModel(point.x, point.y, textBounds, new Position.Bias[1]);
        for (int candidate : new int[]{offset, offset - 1}) {
            if (candidate < 0 || candidate >= document.getLength() || !charBounds(view, textBounds, candidate).contains(point)) {
                continue;
            }

            AttributeSet attributes = document.getCharacterElement(candidate).getAttributes();
            if (attributes.getAttribute(HTML.Tag.A) instanceof AttributeSet anchor) {
                Object href = anchor.getAttribute(HTML.Attribute.HREF);
                return href == null ? "" : href.toString();
            }
            return null;
        }
        return null;
    }

    private static Rectangle textBounds(JLabel label) {
        Insets insets = label.getInsets();
        Rectangle viewBounds = new Rectangle(
            insets.left,
            insets.top,
            label.getWidth() - insets.left - insets.right,
            label.getHeight() - insets.top - insets.bottom
        );
        Rectangle iconBounds = new Rectangle();
        Rectangle textBounds = new Rectangle();
        SwingUtilities.layoutCompoundLabel(
            label,
            label.getFontMetrics(label.getFont()),
            label.getText(),
            label.isEnabled() ? label.getIcon() : label.getDisabledIcon(),
            label.getVerticalAlignment(),
            label.getHorizontalAlignment(),
            label.getVerticalTextPosition(),
            label.getHorizontalTextPosition(),
            viewBounds,
            iconBounds,
            textBounds,
            label.getIconTextGap()
        );
        return textBounds;
    }

    private static Rectangle charBounds(View view, Rectangle textBounds, int offset) {
        try {
            Shape shape = view.modelToView(offset, Position.Bias.Forward, offset + 1, Position.Bias.Backward, textBounds);
            return shape == null ? new Rectangle() : shape.getBounds();
        }
        catch (BadLocationException e) {
            return new Rectangle();
        }
    }
}
