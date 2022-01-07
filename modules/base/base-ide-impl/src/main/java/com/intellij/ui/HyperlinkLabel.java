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

package com.intellij.ui;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.editor.markup.EffectType;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import consulo.awt.TargetAWT;
import consulo.logging.Logger;
import consulo.ui.color.ColorValue;

import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;

/**
 * @author Eugene Belyaev
 */
public class HyperlinkLabel extends HighlightableComponent {
  private static final TextAttributes BOLD_ATTRIBUTES = new TextAttributes(TargetAWT.from(UIUtil.getLabelTextForeground()), null, null, null, Font.BOLD);

  private static final Logger LOG = Logger.getInstance(HyperlinkLabel.class);

  private HighlightedText myHighlightedText;
  private final List<HyperlinkListener> myListeners = ContainerUtil.createLockFreeCopyOnWriteList();
  private boolean myUseIconAsLink;
  private final TextAttributes myAnchorAttributes;
  private HyperlinkListener myHyperlinkListener = null;

  private boolean myMouseHover;
  private boolean myMousePressed;

  public HyperlinkLabel() {
    this("");
  }

  public HyperlinkLabel(String text) {
    this(text, JBColor.BLUE, UIUtil.getLabelBackground(), JBColor.BLUE);
  }

  public HyperlinkLabel(String text, Color background) {
    this(text, JBColor.BLUE, background, JBColor.BLUE);
  }

  public HyperlinkLabel(String text, final Color textForegroundColor, final Color textBackgroundColor, final Color textEffectColor) {
    myAnchorAttributes = new CustomTextAttributes(TargetAWT.from(textBackgroundColor));
    enforceBackgroundOutsideText(textBackgroundColor);
    setHyperlinkText(text);
    enableEvents(AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);
  }

  @Override
  public void addNotify() {
    super.addNotify();
    adjustSize();
  }

  public void setHyperlinkText(String text) {
    setHyperlinkText("", text, "");
  }

  public void setHyperlinkText(String beforeLinkText, String linkText, String afterLinkText) {
    myUseIconAsLink = beforeLinkText.length() == 0;
    prepareText(beforeLinkText, linkText, afterLinkText);
    revalidate();
    adjustSize();
  }

  public void setUseIconAsLink(boolean useIconAsLink) {
    myUseIconAsLink = useIconAsLink;
  }

  protected void adjustSize() {
    final Dimension preferredSize = this.getPreferredSize();
    this.setMinimumSize(preferredSize);
  }


  @Override
  protected void processMouseEvent(MouseEvent e) {
    if (e.getID() == MouseEvent.MOUSE_ENTERED && isOnLink(e.getX())) {
      myMouseHover = true;
      repaint();
    }
    else if (e.getID() == MouseEvent.MOUSE_EXITED) {
      setCursor(null);
      myMouseHover = false;
      myMousePressed = false;
      repaint();
    }
    else if (UIUtil.isActionClick(e, MouseEvent.MOUSE_PRESSED) && isOnLink(e.getX())) {
      myMousePressed = true;
      repaint();
    }
    else if (e.getID() == MouseEvent.MOUSE_RELEASED) {
      if (myMousePressed && isOnLink(e.getX())) {
        fireHyperlinkEvent(e);
      }
      myMousePressed = false;
      repaint();
    }
    super.processMouseEvent(e);
  }

  @Override
  protected void processMouseMotionEvent(MouseEvent e) {
    if (e.getID() == MouseEvent.MOUSE_MOVED) {
      setCursor(isOnLink(e.getX()) ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
    }
    super.processMouseMotionEvent(e);
  }

  private boolean isOnLink(int x) {
    if (myUseIconAsLink && myIcon != null && x < myIcon.getWidth()) {
      return true;
    }
    final HighlightedRegion region = findRegionByX(x);
    return region != null && region.textAttributes == myAnchorAttributes;
  }

  private void prepareText(String beforeLinkText, String linkText, String afterLinkText) {
    setFont(UIUtil.getLabelFont());
    myHighlightedText = new HighlightedText();
    myHighlightedText.appendText(beforeLinkText, null);
    myHighlightedText.appendText(linkText, myAnchorAttributes);
    myHighlightedText.appendText(afterLinkText, null);
    myHighlightedText.applyToComponent(this);
    adjustSize();
  }

  @Override
  public void setText(String text) {
    myUseIconAsLink = false;
    super.setText(text);
  }

  public void setHyperlinkTarget(@Nullable final String url) {
    if (myHyperlinkListener != null) {
      removeHyperlinkListener(myHyperlinkListener);
    }
    if (url != null) {
      myHyperlinkListener = new HyperlinkListener() {
        @Override
        public void hyperlinkUpdate(HyperlinkEvent e) {
          BrowserUtil.launchBrowser(url);
        }
      };
      addHyperlinkListener(myHyperlinkListener);
    }
  }

  public void addHyperlinkListener(HyperlinkListener listener) {
    myListeners.add(listener);
  }

  public void removeHyperlinkListener(HyperlinkListener listener) {
    myListeners.remove(listener);
  }

  String getText() {
    return myHighlightedText.getText();
  }

  protected void fireHyperlinkEvent(@Nullable InputEvent inputEvent) {
    HyperlinkEvent e = new HyperlinkEvent(this, HyperlinkEvent.EventType.ACTIVATED, null, null);
    for (HyperlinkListener listener : myListeners) {
      listener.hyperlinkUpdate(e);
    }
  }

  public void doClick() {
    fireHyperlinkEvent(null);
  }

  public void setHtmlText(String text) {
    HTMLEditorKit.Parser parse = new ParserDelegator();
    final HighlightedText highlightedText = new HighlightedText();
    try {
      parse.parse(new StringReader(text), new HTMLEditorKit.ParserCallback() {
        private TextAttributes currentAttributes = null;

        @Override
        public void handleText(char[] data, int pos) {
          highlightedText.appendText(new String(data), currentAttributes);
        }

        @Override
        public void handleStartTag(HTML.Tag t, MutableAttributeSet a, int pos) {
          if (t == HTML.Tag.B) {
            currentAttributes = BOLD_ATTRIBUTES;
          }
          else if (t == HTML.Tag.A) {
            currentAttributes = myAnchorAttributes;
          }
        }

        @Override
        public void handleEndTag(HTML.Tag t, int pos) {
          currentAttributes = null;
        }
      }, false);
    }
    catch (IOException e) {
      LOG.error(e);
    }
    highlightedText.applyToComponent(this);
    ((JComponent)getParent()).revalidate();
    adjustSize();
  }

  public static class Croppable extends HyperlinkLabel {
    @Override
    protected void adjustSize() {
      // ignore, keep minimum size default
    }
  }

  private class CustomTextAttributes extends TextAttributes {
    private CustomTextAttributes(ColorValue textBackgroundColor) {
      super(null, textBackgroundColor, null, null, Font.PLAIN);
    }

    @Override
    public ColorValue getForegroundColor() {
      return TargetAWT.from(!isEnabled()
                            ? UIManager.getColor("Label.disabledForeground")
                            : myMousePressed ? JBUI.CurrentTheme.Link.linkPressedColor() : myMouseHover ? JBUI.CurrentTheme.Link.linkHoverColor() : JBUI.CurrentTheme.Link.linkColor());
    }

    @Override
    public ColorValue getEffectColor() {
      return getForegroundColor();
    }

    @Override
    public EffectType getEffectType() {
      return !isEnabled() || myMouseHover || myMousePressed ? EffectType.LINE_UNDERSCORE : null;
    }

    @Override
    public void setForegroundColor(ColorValue color) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void setEffectColor(ColorValue color) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void setEffectType(EffectType effectType) {
      throw new UnsupportedOperationException();
    }
  }
}
