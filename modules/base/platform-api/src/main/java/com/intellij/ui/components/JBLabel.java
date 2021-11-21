/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package com.intellij.ui.components;

import com.intellij.ui.AnchorableComponent;
import com.intellij.ui.ColorUtil;
import com.intellij.util.SystemProperties;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBHtmlEditorKit;
import com.intellij.util.ui.UIUtil;
import consulo.awt.TargetAWT;
import consulo.desktop.util.awt.StringHtmlUtil;
import consulo.ui.image.Image;
import org.intellij.lang.annotations.JdkConstants;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.text.BadLocationException;
import javax.swing.text.EditorKit;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.Collections;

public class JBLabel extends JLabel implements AnchorableComponent {
  private UIUtil.ComponentStyle myComponentStyle = UIUtil.ComponentStyle.REGULAR;
  private UIUtil.FontColor myFontColor = UIUtil.FontColor.NORMAL;
  private JComponent myAnchor = null;
  private JEditorPane myEditorPane = null;
  private JLabel myIconLabel = null;
  private boolean myMultiline = false;
  private boolean myAllowAutoWrapping = false;

  public JBLabel() {
    super();
  }

  public JBLabel(@Nonnull UIUtil.ComponentStyle componentStyle) {
    super();
    setComponentStyle(componentStyle);
  }

  public JBLabel(@Nullable Image image) {
    super(TargetAWT.to(image));
  }
  
  public JBLabel(@Nonnull String text) {
    super(text);
  }

  public JBLabel(@Nonnull String text, @Nonnull UIUtil.ComponentStyle componentStyle) {
    super(text);
    setComponentStyle(componentStyle);
  }

  public JBLabel(@Nonnull String text, @Nonnull UIUtil.ComponentStyle componentStyle, @Nonnull UIUtil.FontColor fontColor) {
    super(text);
    setComponentStyle(componentStyle);
    setFontColor(fontColor);
  }

  public JBLabel(@Nonnull String text, @JdkConstants.HorizontalAlignment int horizontalAlignment) {
    super(text, horizontalAlignment);
  }

  public JBLabel(@Nullable Image image, @JdkConstants.HorizontalAlignment int horizontalAlignment) {
    super(TargetAWT.to(image), horizontalAlignment);
  }

  public JBLabel(@Nullable String text, @Nullable Image icon, @JdkConstants.HorizontalAlignment int horizontalAlignment) {
    super(text, TargetAWT.to(icon), horizontalAlignment);
  }

  public void setComponentStyle(@Nonnull UIUtil.ComponentStyle componentStyle) {
    myComponentStyle = componentStyle;
    UIUtil.applyStyle(componentStyle, this);
  }

  public UIUtil.ComponentStyle getComponentStyle() {
    return myComponentStyle;
  }

  public UIUtil.FontColor getFontColor() {
    return myFontColor;
  }

  public void setFontColor(@Nonnull UIUtil.FontColor fontColor) {
    myFontColor = fontColor;
  }

  @Override
  public Color getForeground() {
    if (!isEnabled()) {
      return UIUtil.getLabelDisabledForeground();
    }
    if (myFontColor != null) {
      return UIUtil.getLabelFontColor(myFontColor);
    }
    return super.getForeground();
  }

  @Override
  public void setForeground(Color fg) {
    myFontColor = null;
    super.setForeground(fg);
    if (myEditorPane != null) {
      updateStyle(myEditorPane);
    }
  }

  @Override
  public void setAnchor(@Nullable JComponent anchor) {
    myAnchor = anchor;
  }

  @Override
  public JComponent getAnchor() {
    return myAnchor;
  }

  @Override
  public Dimension getPreferredSize() {
    if (myAnchor != null && myAnchor != this) return myAnchor.getPreferredSize();
    if (myEditorPane != null) return getLayout().preferredLayoutSize(this);
    return super.getPreferredSize();
  }
  @Override
  public Dimension getMinimumSize() {
    if (myAnchor != null && myAnchor != this) return myAnchor.getMinimumSize();
    if (myEditorPane != null) return getLayout().minimumLayoutSize(this);
    return super.getMinimumSize();
  }


  @Override
  protected void paintComponent(Graphics g) {
    if (myEditorPane == null) {
      super.paintComponent(g);
    }
  }

  @Override
  public void setText(String text) {
    super.setText(text);
    if (myEditorPane != null) {
      myEditorPane.setText(getText());
      updateStyle(myEditorPane);
      checkMultiline();
    }
  }

  @Override
  public void setIcon(Icon icon) {
    super.setIcon(icon);
    if (myIconLabel != null) {
      myIconLabel.setIcon(icon);
      updateLayout();
    }
  }

  public void setIcon(Image icon) {
    setIcon(TargetAWT.to(icon));
  }

  private void checkMultiline() {
    myMultiline = StringHtmlUtil.removeHtmlTags(getText()).contains(SystemProperties.getLineSeparator());
  }

  @Override
  public void setFont(Font font) {
    super.setFont(font);
    if (myEditorPane != null) {
      updateStyle(myEditorPane);
    }
  }

  @Override
  public void setIconTextGap(int iconTextGap) {
    super.setIconTextGap(iconTextGap);
    if (myEditorPane != null) {
      updateLayout();
    }
  }

  protected void updateLayout() {
    setLayout(new BorderLayout(getIcon() == null ? 0 : getIconTextGap(), 0));
    add(myIconLabel, BorderLayout.WEST);
    add(myEditorPane, BorderLayout.CENTER);
  }

  @Override
  public void updateUI() {
    super.updateUI();
    if (myEditorPane != null) {
      //init inner components again (if any) to provide proper colors when LAF is being changed
      setCopyable(false);
      setCopyable(true);
    }
  }

  public JBLabel withBorder(Border border) {
    setBorder(border);
    return this;
  }

  public JBLabel withFont(JBFont font) {
    setFont(font);
    return this;
  }

  /**
   *
   * In 'copyable' mode JBLabel has the same appearance but user can select text with mouse and copy it to clipboard with standard shortcut.
   * @return 'this' (the same instance)
   */
  //
  // By default JBLabel is NOT copyable
  // This method re
  public JBLabel setCopyable(boolean copyable) {
    if (copyable ^ myEditorPane != null) {
      if (myEditorPane == null) {
        final JLabel ellipsisLabel = new JBLabel("...");
        myIconLabel = new JLabel(getIcon());
        myEditorPane = new JEditorPane() {
          @Override
          public void paint(Graphics g) {
            Dimension size = getSize();
            boolean paintEllipsis = getPreferredSize().width > size.width && !myMultiline && !myAllowAutoWrapping;

            if (!paintEllipsis) {
              super.paint(g);
            }
            else {
              Dimension ellipsisSize = ellipsisLabel.getPreferredSize();
              int endOffset = size.width - ellipsisSize.width;
              try {
                // do not paint half of the letter
                endOffset = modelToView(viewToModel(new Point(endOffset, 0)) - 1).x;
              }
              catch (BadLocationException ignore) {
              }
              Shape oldClip = g.getClip();
              g.clipRect(0, 0, endOffset, size.height);

              super.paint(g);
              g.setClip(oldClip);

              g.translate(endOffset, 0);
              ellipsisLabel.setSize(ellipsisSize);
              ellipsisLabel.paint(g);
              g.translate(-endOffset, 0);
            }
          }
        };
        myEditorPane.addFocusListener(new FocusAdapter() {
          @Override
          public void focusLost(FocusEvent e) {
            if (myEditorPane == null) return;
            int caretPosition = myEditorPane.getCaretPosition();
            myEditorPane.setSelectionStart(caretPosition);
            myEditorPane.setSelectionEnd(caretPosition);
          }
        });
        myEditorPane.setContentType("text/html");
        myEditorPane.setEditable(false);
        myEditorPane.setBackground(UIUtil.TRANSPARENT_COLOR);
        myEditorPane.setOpaque(false);
        myEditorPane.setBorder(null);
        UIUtil.putClientProperty(myEditorPane, UIUtil.NOT_IN_HIERARCHY_COMPONENTS, Collections.singleton(ellipsisLabel));

        myEditorPane.setEditorKit(JBHtmlEditorKit.create());
        updateStyle(myEditorPane);

        myEditorPane.setText(getText());
        checkMultiline();
        myEditorPane.setCaretPosition(0);
        updateLayout();
      } else {
        removeAll();
        myEditorPane = null;
        myIconLabel = null;
      }
    }
    return this;
  }

  /**
   * In 'copyable' mode auto-wrapping is disabled by default.
   * (In this case you have to markup your HTML with P or BR tags explicitly)
   */
  public JBLabel setAllowAutoWrapping(boolean allowAutoWrapping) {
    myAllowAutoWrapping = allowAutoWrapping;
    return this;
  }

  private void updateStyle(@Nonnull JEditorPane pane) {
    myEditorPane.setFont(getFont());
    myEditorPane.setForeground(getForeground());
    EditorKit kit = pane.getEditorKit();
    if (kit instanceof HTMLEditorKit) {
      StyleSheet css = ((HTMLEditorKit)kit).getStyleSheet();
      css.addRule("body, p {" +
                  "color:#" + ColorUtil.toHex(getForeground()) + ";" +
                  "font-family:" + getFont().getFamily() + ";" +
                  "font-size:" + getFont().getSize() + "pt;" +
                  "white-space:" + (myAllowAutoWrapping ? "normal" : "nowrap") + ";}");
    }
  }
}
