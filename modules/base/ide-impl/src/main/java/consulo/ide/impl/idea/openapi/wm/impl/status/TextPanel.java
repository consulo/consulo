// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.openapi.wm.impl.status;

import consulo.application.util.SystemInfo;
import consulo.ide.impl.idea.openapi.util.text.StringUtil;
import consulo.ui.ex.awt.NonOpaquePanel;
import consulo.ui.ex.awt.JBUIScale;
import consulo.ui.ex.awt.JBFont;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.util.UISettingsUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import org.jetbrains.annotations.Nls;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.swing.*;
import java.awt.*;

public class TextPanel extends NonOpaquePanel implements Accessible {
  @Nullable
  private String myText;

  private Integer myPrefHeight;
  private Dimension myExplicitSize;

  protected float myAlignment;

  protected TextPanel() {
    updateUI();
  }

  @Override
  public void updateUI() {
    UISettingsUtil.setupComponentAntialiasing(this);
    Object value = UIManager.getDefaults().get(RenderingHints.KEY_FRACTIONALMETRICS);
    if (value == null) value = RenderingHints.VALUE_FRACTIONALMETRICS_OFF;
    putClientProperty(RenderingHints.KEY_FRACTIONALMETRICS, value);
  }

  @Override
  public Font getFont() {
    return SystemInfo.isMac ? JBUI.Fonts.label(11) : JBFont.label();
  }

  public void recomputeSize() {
    final JLabel label = new JLabel("XXX");
    label.setFont(getFont());
    myPrefHeight = label.getPreferredSize().height;
  }

  /**
   * @deprecated no effect
   */
  @Deprecated
  public void resetColor() {
  }

  @Override
  protected void paintComponent(final Graphics g) {
    @Nls String s = getText();
    int panelWidth = getWidth();
    int panelHeight = getHeight();
    if (s == null) return;

    Graphics2D g2 = (Graphics2D)g;
    g2.setFont(getFont());
    UISettingsUtil.setupAntialiasing(g);

    Rectangle bounds = new Rectangle(panelWidth, panelHeight);
    FontMetrics fm = g.getFontMetrics();
    int textWidth = fm.stringWidth(s);
    int x = textWidth > panelWidth ? getInsets().left : getTextX(g2);
    int maxWidth = panelWidth - x - getInsets().right;
    if (textWidth > maxWidth) {
      s = truncateText(s, bounds, fm, new Rectangle(), new Rectangle(), maxWidth);
    }

    int y = UIUtil.getStringY(s, bounds, g2);
    Color foreground = isEnabled() ? getForeground() : UIUtil.getInactiveTextColor();
    g2.setColor(foreground);
    g2.drawString(s, x, y);
  }

  protected int getTextX(Graphics g) {
    String text = getText();
    Insets insets = getInsets();
    if (text == null || myAlignment == Component.LEFT_ALIGNMENT) {
      return insets.left;
    }
    if (myAlignment == Component.RIGHT_ALIGNMENT) {
      FontMetrics fm = g.getFontMetrics();
      int textWidth = fm.stringWidth(text);
      return getWidth() - insets.right - textWidth;
    }
    if (myAlignment == Component.CENTER_ALIGNMENT) {
      FontMetrics fm = g.getFontMetrics();
      int textWidth = fm.stringWidth(text);
      return (getWidth() - insets.left - insets.right - textWidth) / 2 + insets.left;
    }
    return insets.left;
  }

  @Nls
  protected String truncateText(@Nls String text, Rectangle bounds, FontMetrics fm, Rectangle textR, Rectangle iconR, int maxWidth) {
    return SwingUtilities.layoutCompoundLabel(this, fm, text, null, SwingConstants.CENTER, SwingConstants.CENTER, SwingConstants.CENTER, SwingConstants.TRAILING, bounds, iconR, textR, 0);
  }

  public void setTextAlignment(final float alignment) {
    myAlignment = alignment;
  }

  public final void setText(@Nullable String text) {
    text = StringUtil.notNullize(text);
    if (text.equals(myText)) {
      return;
    }

    String oldAccessibleName = null;
    if (accessibleContext != null) {
      oldAccessibleName = accessibleContext.getAccessibleName();
    }

    myText = text;

    if ((accessibleContext != null) && !StringUtil.equals(accessibleContext.getAccessibleName(), oldAccessibleName)) {
      accessibleContext.firePropertyChange(AccessibleContext.ACCESSIBLE_VISIBLE_DATA_PROPERTY, oldAccessibleName, accessibleContext.getAccessibleName());
    }

    setPreferredSize(getPanelDimensionFromFontMetrics(myText));
    revalidate();
    repaint();
  }

  @Nullable
  @Nls
  public String getText() {
    return myText;
  }

  @Override
  public Dimension getPreferredSize() {
    if (myExplicitSize != null) {
      return myExplicitSize;
    }

    String text = getTextForPreferredSize();
    return getPanelDimensionFromFontMetrics(text);
  }

  private Dimension getPanelDimensionFromFontMetrics(String text) {
    Insets insets = getInsets();
    int width = insets.left + insets.right + (text != null ? getFontMetrics(getFont()).stringWidth(text) : 0);
    int height = (myPrefHeight == null) ? getMinimumSize().height : myPrefHeight;
    return new Dimension(width, height);
  }

  /**
   * @return the text that is used to calculate the preferred size
   */
  @Nullable
  protected String getTextForPreferredSize() {
    return myText;
  }

  public void setExplicitSize(@Nullable Dimension explicitSize) {
    myExplicitSize = explicitSize;
  }

  public static class WithIconAndArrows extends TextPanel {
    private final static int GAP = JBUIScale.scale(2);
    @Nullable
    private Image myIcon;

    @Override
    protected void paintComponent(@Nonnull final Graphics g) {
      super.paintComponent(g);
      Icon icon = TargetAWT.to(myIcon == null || isEnabled() ? myIcon : ImageEffects.grayed(myIcon));
      if (icon != null) {
        icon.paintIcon(this, g, getIconX(g), getHeight() / 2 - icon.getIconHeight() / 2);
      }
    }

    /**
     * @deprecated arrows are not painted anymore
     */
    @Deprecated
    protected boolean shouldPaintArrows() {
      return false;
    }

    @Override
    public Dimension getPreferredSize() {
      Dimension preferredSize = super.getPreferredSize();
      if (myIcon == null) {
        return preferredSize;
      }
      return new Dimension(Math.max(preferredSize.width + myIcon.getWidth(), getHeight()), preferredSize.height);
    }

    @Override
    protected int getTextX(Graphics g) {
      int x = super.getTextX(g);
      if (myIcon == null || myAlignment == RIGHT_ALIGNMENT) {
        return x;
      }
      if (myAlignment == CENTER_ALIGNMENT) {
        return x + (myIcon.getWidth() + GAP) / 2;
      }
      if (myAlignment == LEFT_ALIGNMENT) {
        return x + myIcon.getWidth() + GAP;
      }
      return x;
    }

    private int getIconX(Graphics g) {
      int x = super.getTextX(g);
      if (myIcon == null || getText() == null || myAlignment == LEFT_ALIGNMENT) {
        return x;
      }
      if (myAlignment == CENTER_ALIGNMENT) {
        return x - (myIcon.getWidth() + GAP) / 2;
      }
      if (myAlignment == RIGHT_ALIGNMENT) {
        return x - myIcon.getWidth() - GAP;
      }
      return x;
    }

    public void setIcon(@Nullable Image icon) {
      myIcon = icon;
    }

    public boolean hasIcon() {
      return myIcon != null;
    }

    @Nullable
    public Image getIcon() {
      return myIcon;
    }
  }

  public static class ExtraSize extends TextPanel {
    @Override
    public Dimension getPreferredSize() {
      Dimension size = super.getPreferredSize();
      return new Dimension(size.width + 3, size.height);
    }
  }

  @Override
  public AccessibleContext getAccessibleContext() {
    if (accessibleContext == null) {
      accessibleContext = new AccessibleTextPanel();
    }
    return accessibleContext;
  }

  protected class AccessibleTextPanel extends AccessibleJComponent {
    @Override
    public AccessibleRole getAccessibleRole() {
      return AccessibleRole.LABEL;
    }

    @Override
    public String getAccessibleName() {
      return myText;
    }
  }
}
