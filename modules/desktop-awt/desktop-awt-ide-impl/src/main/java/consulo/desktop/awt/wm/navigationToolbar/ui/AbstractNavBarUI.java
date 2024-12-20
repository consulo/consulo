// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.desktop.awt.wm.navigationToolbar.ui;

import consulo.application.ui.UISettings;
import consulo.desktop.awt.wm.navigationToolbar.NavBarItem;
import consulo.desktop.awt.wm.navigationToolbar.NavBarPanel;
import consulo.ui.ex.Gray;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.awt.JBInsets;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.JBUIScale;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.paint.PaintUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.image.Image;
import consulo.ui.style.StyleManager;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

import static consulo.ui.ex.awt.RelativeFont.SMALL;

/**
 * @author Konstantin Bulenkov
 */
public abstract class AbstractNavBarUI implements NavBarUI {

  private final static Map<NavBarItem, Map<ImageType, JBUI.ScaleContext.Cache<BufferedImage>>> ourCache = new HashMap<>();

  private enum ImageType {
    INACTIVE,
    NEXT_ACTIVE,
    ACTIVE,
    INACTIVE_FLOATING,
    NEXT_ACTIVE_FLOATING,
    ACTIVE_FLOATING,
    INACTIVE_NO_TOOLBAR,
    NEXT_ACTIVE_NO_TOOLBAR,
    ACTIVE_NO_TOOLBAR
  }

  @Override
  public Insets getElementIpad(boolean isPopupElement) {
    return isPopupElement ? JBInsets.create(1, 2) : JBUI.emptyInsets();
  }

  @Override
  public JBInsets getElementPadding() {
    return JBUI.insets(3);
  }

  @Override
  public Font getElementFont(NavBarItem navBarItem) {
    Font font = UIUtil.getLabelFont();
    return UISettings.getInstance().getUseSmallLabelsOnTabs() ? SMALL.derive(font) : font;
  }

  @Override
  public Color getBackground(boolean selected, boolean focused) {
    return selected && focused ? UIUtil.getListSelectionBackground(true) : UIUtil.getListBackground();
  }

  @Nullable
  @Override
  public Color getForeground(boolean selected, boolean focused, boolean inactive) {
    return (selected && focused) ? UIUtil.getListSelectionForeground() : inactive ? UIUtil.getInactiveTextColor() : null;
  }

  @Override
  public void doPaintNavBarItem(Graphics2D g, NavBarItem item, NavBarPanel navbar) {
    final boolean floating = navbar.isInFloatingMode();
    boolean toolbarVisible = UISettings.getInstance().getShowMainToolbar();
    final boolean selected = item.isSelected() && item.isFocused();
    boolean nextSelected = item.isNextSelected() && navbar.isFocused();


    ImageType type;
    if (floating) {
      type = selected ? ImageType.ACTIVE_FLOATING : nextSelected ? ImageType.NEXT_ACTIVE_FLOATING : ImageType.INACTIVE_FLOATING;
    }
    else {
      if (toolbarVisible) {
        type = selected ? ImageType.ACTIVE : nextSelected ? ImageType.NEXT_ACTIVE : ImageType.INACTIVE;
      }
      else {
        type = selected ? ImageType.ACTIVE_NO_TOOLBAR : nextSelected ? ImageType.NEXT_ACTIVE_NO_TOOLBAR : ImageType.INACTIVE_NO_TOOLBAR;
      }
    }

    // see: https://github.com/JetBrains/intellij-community/pull/1111
    Map<ImageType, JBUI.ScaleContext.Cache<BufferedImage>> cache = ourCache.computeIfAbsent(item, k -> new HashMap<>());
    JBUI.ScaleContext.Cache<BufferedImage> imageCache = cache.computeIfAbsent(type, k -> new JBUI.ScaleContext.Cache<>((ctx) -> drawToBuffer(item, ctx, floating, toolbarVisible, selected, navbar)));
    BufferedImage image = imageCache.getOrProvide(JBUI.ScaleContext.create(g));
    if (image == null) return;

    UIUtil.drawImage(g, image, 0, 0, null);

    final int offset = item.isFirstElement() ? getFirstElementLeftOffset() : 0;
    int textOffset = getElementPadding().width() + offset;
    if (item.needPaintIcon()) {
      Image icon = item.getIcon();
      if (icon != null) {
        int iconOffset = getElementPadding().left + offset;
        TargetAWT.to(icon).paintIcon(item, g, iconOffset, (item.getHeight() - icon.getHeight()) / 2);
        textOffset += icon.getWidth();
      }
    }

    item.doPaintText(g, textOffset);
  }

  private static BufferedImage drawToBuffer(NavBarItem item, JBUI.ScaleContext ctx, boolean floating, boolean toolbarVisible, boolean selected, NavBarPanel navbar) {
    int w = item.getWidth();
    int h = item.getHeight();
    int offset = (w - getDecorationOffset());
    int h2 = h / 2;

    BufferedImage result = UIUtil.createImage(ctx, w, h, BufferedImage.TYPE_INT_ARGB, PaintUtil.RoundingMode.FLOOR);

    Color defaultBg = StyleManager.get().getCurrentStyle().isDark() ? Gray._100 : JBColor.WHITE;
    final Paint bg = floating ? defaultBg : null;
    final Color selection = UIUtil.getListSelectionBackground(true);

    Graphics2D g2 = result.createGraphics();
    g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);


    Path2D.Double shape = new Path2D.Double();
    shape.moveTo(0, 0);

    shape.lineTo(offset, 0);
    shape.lineTo(w, h2);
    shape.lineTo(offset, h);
    shape.lineTo(0, h);
    shape.closePath();

    Path2D.Double endShape = new Path2D.Double();
    endShape.moveTo(offset, 0);
    endShape.lineTo(w, 0);
    endShape.lineTo(w, h);
    endShape.lineTo(offset, h);
    endShape.lineTo(w, h2);
    endShape.closePath();

    if (bg != null && toolbarVisible) {
      g2.setPaint(bg);
      g2.fill(shape);
      g2.fill(endShape);
    }

    if (selected) {
      Path2D.Double focusShape = new Path2D.Double();
      if (toolbarVisible || floating) {
        focusShape.moveTo(offset, 0);
      }
      else {
        focusShape.moveTo(0, 0);
        focusShape.lineTo(offset, 0);
      }
      focusShape.lineTo(w - 1, h2);
      focusShape.lineTo(offset, h - 1);
      if (!toolbarVisible && !floating) {
        focusShape.lineTo(0, h - 1);

      }

      g2.setColor(selection);
      if (floating && item.isLastElement()) {
        g2.fillRect(0, 0, w, h);
      }
      else {
        g2.fill(shape);
      }
    }

    if (item.isNextSelected() && navbar.isFocused()) {
      g2.setColor(selection);
      g2.fill(endShape);
    }

    if (!item.isLastElement()) {
      if (!selected && (!navbar.isFocused() | !item.isNextSelected())) {
        Icon icon = UIUtil.getTreeCollapsedIcon();
        icon.paintIcon(item, g2, w - icon.getIconWidth() + JBUI.scale(2), h2 - icon.getIconHeight() / 2);
      }
    }

    g2.dispose();
    return result;
  }

  private static int getDecorationOffset() {
    return JBUIScale.scale(8);
  }

  private static int getFirstElementLeftOffset() {
    return JBUIScale.scale(6);
  }

  @Override
  public Dimension getOffsets(NavBarItem item) {
    final Dimension size = new Dimension();
    if (!item.isPopupElement()) {
      size.width += getDecorationOffset() + getElementPadding().width() + (item.isFirstElement() ? getFirstElementLeftOffset() : 0);
      size.height += getElementPadding().height();
    }
    return size;
  }

  @Override
  public Insets getWrapperPanelInsets(Insets insets) {
    final JBInsets result = JBUI.insets(insets);
    if (shouldPaintWrapperPanel()) {
      result.top += JBUIScale.scale(1);
    }
    return result;
  }

  private static boolean shouldPaintWrapperPanel() {
    return false; //return !UISettings.getInstance().SHOW_MAIN_TOOLBAR && NavBarRootPaneExtension.runToolbarExists();
  }

  @Override
  public void doPaintNavBarPanel(Graphics2D g, Rectangle r, boolean mainToolbarVisible, boolean undocked) {
  }

  @Override
  public void clearItems() {
    ourCache.clear();
  }

  @Override
  public int getPopupOffset(@Nonnull NavBarItem item) {
    return item.isFirstElement() ? 0 : JBUIScale.scale(5);
  }
}
