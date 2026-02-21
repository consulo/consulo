/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ui;

import consulo.util.collection.Lists;
import consulo.util.lang.StringUtil;
import consulo.ui.ex.awt.ClickListener;
import consulo.ui.ex.JBColor;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.plaf.basic.BasicHTML;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * @author Eugene Belyaev
 */
public class HoverHyperlinkLabel extends JLabel {
    private String myOriginalText;
    private final List<HyperlinkListener> myListeners = Lists.newLockFreeCopyOnWriteList();

    public HoverHyperlinkLabel(String text) {
        this(text, JBColor.BLUE);
    }

    public HoverHyperlinkLabel(String text, Color color) {
        super(text);
        myOriginalText = text;
        setForeground(color);
        setupListener();
    }

    private void setupListener() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                HoverHyperlinkLabel.super.setText(underlineTextInHtml(myOriginalText));
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                HoverHyperlinkLabel.super.setText(myOriginalText);
                setCursor(Cursor.getDefaultCursor());
            }
        });

        new ClickListener() {
            @Override
            public boolean onClick(@Nonnull MouseEvent e, int clickCount) {
                HyperlinkEvent event = new HyperlinkEvent(HoverHyperlinkLabel.this, HyperlinkEvent.EventType.ACTIVATED, null);
                for (HyperlinkListener listener : myListeners) {
                    listener.hyperlinkUpdate(event);
                }
                return true;
            }
        }.installOn(this);
    }

    @Override
    public void setText(String text) {
        if (BasicHTML.isHTMLString(getText())) { // if is currently showing string as html
            super.setText(underlineTextInHtml(text));
        }
        else {
            super.setText(text);
        }
        myOriginalText = text;
    }

    private static String underlineTextInHtml(String text) {
        return "<html><u>" + StringUtil.escapeXml(text) + "</u></html>";
    }

    public String getOriginalText() {
        return myOriginalText;
    }

    public void addHyperlinkListener(HyperlinkListener listener) {
        myListeners.add(listener);
    }

    public void removeHyperlinkListener(HyperlinkListener listener) {
        myListeners.remove(listener);
    }
}
