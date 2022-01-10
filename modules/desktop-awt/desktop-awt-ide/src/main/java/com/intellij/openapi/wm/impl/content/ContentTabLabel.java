// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.wm.impl.content;

import com.intellij.icons.AllIcons;
import com.intellij.ide.DataManager;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.IdeTooltip;
import com.intellij.ide.IdeTooltipManager;
import com.intellij.ide.ui.UISettings;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.ActionManagerEx;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.keymap.KeymapManager;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import com.intellij.util.SmartList;
import com.intellij.util.ui.BaseButtonBehavior;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.TimedDeadzone;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.image.Image;
import consulo.ui.image.ImageState;

import javax.annotation.Nonnull;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicGraphicsUtils;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

class ContentTabLabel extends BaseLabel {
  private class CloseContentAction extends AdditionalIcon {
    private static final String ACTION_NAME = "Close tab";

    public CloseContentAction(@Nonnull Image regularIcon, @Nonnull Image hoveredImage) {
      super(regularIcon, hoveredImage);
    }

    @Nonnull
    @Override
    public Rectangle getRectangle() {
      return new Rectangle(getX(), 0, getIconWidth(), getHeight());
    }

    @Override
    public boolean isHovered() {
      return mouseOverIcon(this);
    }

    @Override
    public boolean getAvailable() {
      return canBeClosed();
    }

    @Nonnull
    @Override
    public Runnable getAction() {
      return () -> {
        Content content = getContent();
        if (content.isPinned()) {
          content.setPinned(false);
          return;
        }

        contentManager().removeContent(getContent(), true);
      };
    }

    @Override
    public boolean getAfterText() {
      return UISettings.getShadowInstance().getCloseTabButtonOnTheRight() || !UISettings.getShadowInstance().getShowCloseButton();
    }

    @Nonnull
    @Override
    public String getTooltip() {
      if (getContent().isPinned()) {
        return IdeBundle.message("action.unpin.tab.tooltip");
      }

      String text = KeymapUtil.getShortcutsText(KeymapManager.getInstance().getActiveKeymap().getShortcuts(IdeActions.ACTION_CLOSE_ACTIVE_TAB));

      return text.isEmpty() || !isSelected() ? ACTION_NAME : ACTION_NAME + " (" + text + ")";
    }
  }

  private static final int MAX_WIDTH = JBUI.scale(300);
  private static final int DEFAULT_HORIZONTAL_INSET = JBUI.scale(12);
  protected static final int ICONS_GAP = JBUI.scale(3);

  private final ImageState<Boolean> myPinImageState = new ImageState<>(Boolean.FALSE);
  private final Image myActiveCloseIcon = Image.stated(myPinImageState, p -> p ? PlatformIconGroup.actionsPinTab() : AllIcons.Actions.CloseHovered);
  private final Image myRegularCloseIcon = Image.stated(myPinImageState, p -> p ? PlatformIconGroup.actionsPinTab() : AllIcons.Actions.Close);

  private final Content myContent;
  private final TabContentLayout myLayout;

  private final List<AdditionalIcon> myAdditionalIcons = new SmartList<>();
  private String myText = null;
  private int myIconWithInsetsWidth;

  private CurrentTooltip currentIconTooltip;

  BaseButtonBehavior behavior = new BaseButtonBehavior(this) {
    @Override
    protected void execute(final MouseEvent e) {

      Optional<Runnable> first = myAdditionalIcons.stream().filter(icon -> mouseOverIcon(icon)).map(icon -> icon.getAction()).findFirst();

      if (first.isPresent()) {
        first.get().run();
        return;
      }

      selectContent();

      if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1 && !myLayout.myDoubleClickActions.isEmpty()) {
        DataContext dataContext = DataManager.getInstance().getDataContext(ContentTabLabel.this);
        for (AnAction action : myLayout.myDoubleClickActions) {
          AnActionEvent event = AnActionEvent.createFromInputEvent(e, ActionPlaces.UNKNOWN, null, dataContext);
          if (ActionUtil.lastUpdateAndCheckDumb(action, event, false)) {
            ActionManagerEx.getInstanceEx().fireBeforeActionPerformed(action, dataContext, event);
            ActionUtil.performActionDumbAware(action, event);
          }
        }
      }
    }
  };

  private void showTooltip(AdditionalIcon icon) {

    if (icon != null) {
      if (currentIconTooltip != null) {
        if (currentIconTooltip.icon == icon) {
          IdeTooltipManager.getInstance().show(currentIconTooltip.currentTooltip, false, false);
          return;
        }

        hideCurrentTooltip();
      }

      String toolText = icon.getTooltip();

      if (toolText != null && !toolText.isEmpty()) {
        IdeTooltip tooltip = new IdeTooltip(this, icon.getCenterPoint(), new JLabel(toolText));
        currentIconTooltip = new CurrentTooltip(IdeTooltipManager.getInstance().show(tooltip, false, false), icon);
        return;
      }
    }

    hideCurrentTooltip();
    if (myText != null && !myText.equals(getText())) {
      IdeTooltip tooltip = new IdeTooltip(this, getMousePosition(), new JLabel(myText));
      currentIconTooltip = new CurrentTooltip(IdeTooltipManager.getInstance().show(tooltip, false, false), null);
    }

  }

  private void hideCurrentTooltip() {
    if (currentIconTooltip == null) return;

    currentIconTooltip.currentTooltip.hide();
    currentIconTooltip = null;
  }

  private void updateCloseIcon() {
    myPinImageState.setState(getContent().isPinned());
  }

  @Override
  public void setText(String text) {
    myText = text;
    updateText();
  }

  private void updateText() {
    FontMetrics fm = getFontMetrics(getFont());
    float textWidth = BasicGraphicsUtils.getStringWidth(this, fm, myText);
    float prefWidth = myIconWithInsetsWidth + textWidth;

    int maxWidth = getMaximumSize().width;

    if (prefWidth > maxWidth) {
      int offset = maxWidth - myIconWithInsetsWidth;
      String s = BasicGraphicsUtils.getClippedString(this, fm, myText, offset);
      super.setText(s);
      return;
    }

    super.setText(myText);
  }

  protected final boolean mouseOverIcon(AdditionalIcon icon) {
    if (!isHovered() || !icon.getAvailable()) return false;

    PointerInfo info = MouseInfo.getPointerInfo();
    if (info == null) return false;
    Point point = info.getLocation();
    SwingUtilities.convertPointFromScreen(point, this);
    return icon.contains(point);
  }

  ContentTabLabel(@Nonnull Content content, @Nonnull TabContentLayout layout) {
    super(layout.myUi, false);
    myLayout = layout;
    myContent = content;

    fillIcons(myAdditionalIcons);

    behavior.setActionTrigger(MouseEvent.MOUSE_RELEASED);
    behavior.setMouseDeadzone(TimedDeadzone.NULL);

    myContent.addPropertyChangeListener(event -> {
      final String property = event.getPropertyName();
      if (Content.IS_CLOSABLE.equals(property)) {
        repaint();
      }
      if (Content.PROP_PINNED.equals(property)) {
        updateCloseIcon();
      }
    });

    if (myContent.isPinned()) {
      SwingUtilities.invokeLater(this::updateCloseIcon);
    }

    setMaximumSize(new Dimension(MAX_WIDTH, getMaximumSize().height));
  }

  protected void fillIcons(List<AdditionalIcon> icons) {
    icons.add(new CloseContentAction(myRegularCloseIcon, myActiveCloseIcon));
  }

  @Override
  protected void processMouseMotionEvent(MouseEvent event) {
    super.processMouseMotionEvent(event);

    boolean hovered = isHovered();

    if (hovered) {
      if (invalid()) {
        repaint();
      }

      Optional<AdditionalIcon> first = myAdditionalIcons.stream().filter(icon -> mouseOverIcon(icon)).findFirst();

      if (first.isPresent()) {
        showTooltip(first.get());
        return;
      }
    }

    showTooltip(null);
  }

  protected boolean invalid() {
    return myAdditionalIcons.stream().anyMatch(icon -> icon.getAvailable());
  }

  public final boolean canBeClosed() {
    return myContent.isCloseable() && myUi.myWindow.canCloseContents();
  }

  protected void selectContent() {
    final ContentManager mgr = contentManager();
    if (mgr.getIndexOfContent(myContent) >= 0) {
      mgr.setSelectedContent(myContent, true);
    }
  }

  public void update() {
    setHorizontalAlignment(SwingConstants.LEFT);
    if (!myLayout.isToDrawTabs()) {
      setBorder(null);
    }

    updateTextAndIcon(myContent, isSelected());
  }

  @Override
  public Dimension getPreferredSize() {
    final Dimension size = super.getPreferredSize();
    int iconWidth = 0;
    Map<Boolean, List<AdditionalIcon>> map = new HashMap<>();
    for (AdditionalIcon myAdditionalIcon : myAdditionalIcons) {
      if (myAdditionalIcon.getAvailable()) {
        map.computeIfAbsent(myAdditionalIcon.getAfterText(), k -> new SmartList<>()).add(myAdditionalIcon);
      }
    }

    int right = DEFAULT_HORIZONTAL_INSET;
    int left = DEFAULT_HORIZONTAL_INSET;

    if (map.get(false) != null) {
      iconWidth = ICONS_GAP;

      for (AdditionalIcon icon : map.get(false)) {
        icon.setX(iconWidth);
        iconWidth += icon.getIconWidth() + ICONS_GAP;
      }

      left = iconWidth;
      iconWidth = 0;
    }

    if (map.get(true) != null) {
      right = ICONS_GAP + JBUI.scale(4);

      for (AdditionalIcon icon : map.get(true)) {
        icon.setX(iconWidth + size.width + ICONS_GAP - right);
        iconWidth += icon.getIconWidth() + ICONS_GAP;
      }
    }

    setBorder(new EmptyBorder(0, left, 0, right));
    myIconWithInsetsWidth = iconWidth + right + left;

    return new Dimension(iconWidth + size.width, size.height);
  }

  private void paintIcons(final Graphics g) {
    for (AdditionalIcon icon : myAdditionalIcons) {
      if (icon.getAvailable()) {
        icon.paintIcon(this, g);
      }
    }
  }

  @Override
  protected void paintComponent(final Graphics g) {
    super.paintComponent(g);
    paintIcons(g);
  }

  public boolean isSelected() {
    return contentManager().isSelected(myContent);
  }

  public boolean isHovered() {
    return behavior.isHovered();
  }

  private ContentManager contentManager() {
    return myUi.myWindow.getContentManager();
  }

  @Nonnull
  @Override
  public Content getContent() {
    return myContent;
  }

  private static class CurrentTooltip {
    final IdeTooltip currentTooltip;
    final AdditionalIcon icon;

    CurrentTooltip(IdeTooltip currentTooltip, AdditionalIcon icon) {
      this.currentTooltip = currentTooltip;
      this.icon = icon;
    }
  }
}
