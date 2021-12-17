/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.ui.tabs.impl;

import com.intellij.ide.DataManager;
import com.intellij.ide.ui.UISettings;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.util.Pass;
import com.intellij.ui.*;
import com.intellij.ui.components.panels.Wrapper;
import com.intellij.ui.tabs.TabInfo;
import com.intellij.ui.tabs.TabsUtil;
import com.intellij.ui.tabs.UiDecorator;
import com.intellij.util.PairConsumer;
import com.intellij.util.ui.Centerizer;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.accessibility.ScreenReader;
import consulo.awt.TargetAWT;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.Objects;

public class TabLabel extends JPanel {
  public class IconInfo {
    public Image myImage;
    public boolean myActive = true;
  }

  protected final SimpleColoredComponent myLabel;

  private final IconInfo[] myIconInfos = new IconInfo[2];

  private Image myOverlayedIcon;

  private final TabInfo myInfo;
  protected ActionPanel myActionPanel;
  private boolean myCentered;

  private final Wrapper myLabelPlaceholder = new Wrapper(false);
  protected final JBTabsImpl myTabs;

  public TabLabel(JBTabsImpl tabs, final TabInfo info) {
    super(new BorderLayout(), false);

    myTabs = tabs;
    myInfo = info;

    myLabel = createLabel();

    // Allow focus so that user can TAB into the selected TabLabel and then
    // navigate through the other tabs using the LEFT/RIGHT keys.
    setFocusable(ScreenReader.isActive());
    setOpaque(false);

    myLabelPlaceholder.setOpaque(false);
    add(myLabelPlaceholder, BorderLayout.CENTER);

    setAlignmentToCenter(true);

    myIconInfos[0] = new IconInfo();
    myIconInfos[1] = new IconInfo();

    addMouseListener(new MouseAdapter() {
      @Override
      public void mousePressed(final MouseEvent e) {
        if (UIUtil.isCloseClick(e, MouseEvent.MOUSE_PRESSED)) return;
        if (JBTabsImpl.isSelectionClick(e, false) && myInfo.isEnabled()) {
          final TabInfo selectedInfo = myTabs.getSelectedInfo();
          if (selectedInfo != myInfo) {
            myInfo.setPreviousSelection(selectedInfo);
          }
          Component c = SwingUtilities.getDeepestComponentAt(e.getComponent(), e.getX(), e.getY());
          if (c instanceof InplaceButton) return;
          myTabs.select(info, true);
        }
        else {
          handlePopup(e);
        }
      }

      @Override
      public void mouseClicked(final MouseEvent e) {
        handlePopup(e);
      }

      @Override
      public void mouseReleased(final MouseEvent e) {
        myInfo.setPreviousSelection(null);
        handlePopup(e);
      }

      @Override
      public void mouseEntered(MouseEvent e) {
        setHovered(true);
      }

      @Override
      public void mouseExited(MouseEvent e) {
        setHovered(false);
      }
    });
  }

  private void setHovered(boolean value) {
    if (myTabs.isHoveredTab(this) == value) return;
    if (value) {
      myTabs.setHovered(this);
    }
    else {
      myTabs.unHover(this);
    }
  }

  @Override
  public boolean isFocusable() {
    // We don't want the focus unless we are the selected tab.
    return myTabs.getSelectedLabel() == this && super.isFocusable();
  }

  private SimpleColoredComponent createLabel() {
    SimpleColoredComponent label = new SimpleColoredComponent() {
      @Override
      public Font getFont() {
        if (isFontSet() || !myTabs.useSmallLabels()) {
          return super.getFont();
        }
        return UIUtil.getLabelFont(UIUtil.FontSize.SMALL);
      }
    };
    label.setOpaque(false);
    label.setIconTextGap((!UISettings.getInstance().HIDE_TABS_IF_NEED ? JBUI.scale(4) : JBUI.scale(2)));
    label.setIconOpaque(false);
    label.setIpad(JBUI.emptyInsets());
    label.setBorder(JBUI.Borders.empty());

    return label;
  }

  @Override
  public Insets getInsets() {
    Insets insets = super.getInsets();
    if (UISettings.getInstance().SHOW_CLOSE_BUTTON) {
      insets.right = JBUI.scale(3);
    }
    return insets;
  }

  public void setAlignmentToCenter(boolean toCenter) {
    if (myCentered == toCenter && getLabelComponent().getParent() != null) return;

    setPlaceholderContent(toCenter, getLabelComponent());
  }

  protected void setPlaceholderContent(boolean toCenter, JComponent component) {
    myLabelPlaceholder.removeAll();

    JComponent centerizer = toCenter ? new Centerizer(component, Centerizer.TYPE.BOTH) : new Centerizer(component, Centerizer.TYPE.VERTICAL);
    myLabelPlaceholder.setContent(centerizer);
    myCentered = toCenter;
  }

  public void paintOffscreen(Graphics g) {
    synchronized (getTreeLock()) {
      validateTree();
    }
    doPaint(g);
  }

  @Override
  public void paint(final Graphics g) {
    if (myTabs.isDropTarget(myInfo)) return;

    if (myTabs.getSelectedInfo() != myInfo) {
      doPaint(g);
    }
  }

  public void paintImage(Graphics g) {
    final Rectangle b = getBounds();
    final Graphics lG = g.create(b.x, b.y, b.width, b.height);
    try {
      lG.setColor(JBColor.red);
      doPaint(lG);
    }
    finally {
      lG.dispose();
    }
  }

  public void doTranslate(PairConsumer<Integer, Integer> consumer) {
    if (!myTabs.isDropTarget(myInfo)) {
      if (myTabs.getSelectedInfo() != myInfo) {
        consumer.consume(0, 0);
      }
      else {
        consumer.consume(0, 0);
      }
    }
  }

  private void doPaint(final Graphics g) {
    doTranslate(g::translate);

    final Composite oldComposite = ((Graphics2D)g).getComposite();
    //if (myTabs instanceof JBEditorTabs && !myTabs.isSingleRow() && myTabs.getSelectedInfo() != myInfo) {
    //  ((Graphics2D)g).setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.9f));
    //}
    super.paint(g);
    ((Graphics2D)g).setComposite(oldComposite);

    doTranslate((x, y) -> g.translate(-x, -y));
  }

  @Override
  public Dimension getPreferredSize() {
    final Dimension size = super.getPreferredSize();
    size.height = TabsUtil.getRealTabsHeight();
    if (myActionPanel != null && !myActionPanel.isVisible()) {
      final Dimension actionPanelSize = myActionPanel.getPreferredSize();
      size.width += actionPanelSize.width;
    }

    return size;
  }

  private void handlePopup(final MouseEvent e) {
    if (e.getClickCount() != 1 || !e.isPopupTrigger()) return;

    if (e.getX() < 0 || e.getX() >= e.getComponent().getWidth() || e.getY() < 0 || e.getY() >= e.getComponent().getHeight()) {
      return;
    }

    String place = myTabs.getPopupPlace();
    place = place != null ? place : ActionPlaces.UNKNOWN;
    myTabs.myPopupInfo = myInfo;

    final DefaultActionGroup toShow = new DefaultActionGroup();
    if (myTabs.getPopupGroup() != null) {
      toShow.addAll(myTabs.getPopupGroup());
      toShow.addSeparator();
    }

    JBTabsImpl tabs = DataManager.getInstance().getDataContext(e.getComponent(), e.getX(), e.getY()).getData(JBTabsImpl.NAVIGATION_ACTIONS_KEY);
    if (tabs == myTabs && myTabs.myAddNavigationGroup) {
      toShow.addAll(myTabs.myNavigationActions);
    }

    if (toShow.getChildrenCount() == 0) return;

    myTabs.myActivePopup = myTabs.myActionManager.createActionPopupMenu(place, toShow).getComponent();
    myTabs.myActivePopup.addPopupMenuListener(myTabs.myPopupListener);

    myTabs.myActivePopup.addPopupMenuListener(myTabs);
    myTabs.myActivePopup.show(e.getComponent(), e.getX(), e.getY());
  }


  public void setText(final SimpleColoredText text) {
    myLabel.change(() -> {
      myLabel.clear();
      myLabel.setIcon(hasIcons() ? buildLabelImage() : null);

      if (text != null) {
        SimpleColoredText derive = myTabs.useBoldLabels() ? text.derive(SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES, true) : text;
        derive.appendToComponent(myLabel);
      }
    }, false);

    invalidateIfNeeded();
  }


  private void invalidateIfNeeded() {
    if (getLabelComponent().getRootPane() == null) return;

    Dimension d = getLabelComponent().getSize();
    Dimension pref = getLabelComponent().getPreferredSize();
    if (d != null && d.equals(pref)) {
      return;
    }

    getLabelComponent().invalidate();

    if (myActionPanel != null) {
      myActionPanel.invalidate();
    }

    myTabs.revalidateAndRepaint(false);
  }

  public void setIcon(final Image icon) {
    setIcon(icon, 0);
  }

  private boolean hasIcons() {
    for (IconInfo icon : myIconInfos) {
      if (icon.myImage != null) {
        return true;
      }
    }
    return false;
  }

  private void setIcon(@Nullable final Image icon, int layer) {
    IconInfo info = myIconInfos[layer];
    info.myImage = icon;

    if (hasIcons()) {
      myLabel.setIcon(buildLabelImage());
    }
    else {
      myLabel.setIcon(null);
    }

    invalidateIfNeeded();
  }

  @Nonnull
  private Image buildLabelImage() {
    Image[] images = Arrays.stream(myIconInfos).map(iconInfo -> iconInfo.myImage).filter(Objects::nonNull).toArray(Image[]::new);
    return ImageEffects.layered(images);
  }

  public TabInfo getInfo() {
    return myInfo;
  }

  public void apply(UiDecorator.UiDecoration decoration) {
    if (decoration.getLabelFont() != null) {
      setFont(decoration.getLabelFont());
      getLabelComponent().setFont(decoration.getLabelFont());
    }

    Insets insets = decoration.getLabelInsets();
    if (insets != null) {
      Insets current = JBTabsImpl.ourDefaultDecorator.getDecoration().getLabelInsets();
      if (current != null) {
        setBorder(new EmptyBorder(getValue(current.top, insets.top), getValue(current.left, insets.left), getValue(current.bottom, insets.bottom), getValue(current.right, insets.right)));
      }
    }
  }

  private static int getValue(int currentValue, int newValue) {
    return newValue != -1 ? newValue : currentValue;
  }

  public void setTabActions(ActionGroup group) {
    removeOldActionPanel();

    if (group == null) return;

    myActionPanel = new ActionPanel(myTabs, myInfo, new Pass<>() {
      @Override
      public void pass(final MouseEvent event) {
        final MouseEvent me = SwingUtilities.convertMouseEvent(event.getComponent(), event, TabLabel.this);
        processMouseEvent(me);
      }
    });

    toggleShowActions(false);

    add(myActionPanel, BorderLayout.EAST);

    myTabs.revalidateAndRepaint(false);
  }


  private void removeOldActionPanel() {
    if (myActionPanel != null) {
      myActionPanel.getParent().remove(myActionPanel);
      myActionPanel = null;
    }
  }

  public boolean updateTabActions() {
    return myActionPanel != null && myActionPanel.update();
  }

  private void setAttractionIcon(@Nullable Image icon) {
    if (myIconInfos[0].myImage == null) {
      setIcon(null, 1);
      myOverlayedIcon = icon;
    }
    else {
      setIcon(icon, 1);
      myOverlayedIcon = null;
    }
  }

  public boolean repaintAttraction() {
    if (!myTabs.myAttractions.contains(myInfo)) {
      if (isLayerIconEnabled(1)) {
        setLayerIconEnabled(1, false);
        setAttractionIcon(null);
        invalidateIfNeeded();
        return true;
      }
      return false;
    }

    boolean needsUpdate = false;

    if (getLayeredIcon(1) != myInfo.getAlertIcon()) {
      setAttractionIcon(myInfo.getAlertIcon());
      needsUpdate = true;
    }

    int maxInitialBlinkCount = 5;
    int maxRefireBlinkCount = maxInitialBlinkCount + 2;
    if (myInfo.getBlinkCount() < maxInitialBlinkCount && myInfo.isAlertRequested()) {
      setLayerIconEnabled(1, !isLayerIconEnabled(1));
      if (myInfo.getBlinkCount() == 0) {
        needsUpdate = true;
      }
      myInfo.setBlinkCount(myInfo.getBlinkCount() + 1);

      if (myInfo.getBlinkCount() == maxInitialBlinkCount) {
        myInfo.resetAlertRequest();
      }

      repaint();
    }
    else {
      if (myInfo.getBlinkCount() < maxRefireBlinkCount && myInfo.isAlertRequested()) {
        setLayerIconEnabled(1, !isLayerIconEnabled(1));
        myInfo.setBlinkCount(myInfo.getBlinkCount() + 1);

        if (myInfo.getBlinkCount() == maxRefireBlinkCount) {
          myInfo.setBlinkCount(maxInitialBlinkCount);
          myInfo.resetAlertRequest();
        }

        repaint();
      }
      else {
        needsUpdate = !isLayerIconEnabled(1);
        setLayerIconEnabled(1, true);
      }
    }

    invalidateIfNeeded();

    return needsUpdate;
  }

  @Override
  protected void paintChildren(final Graphics g) {
    super.paintChildren(g);

    if (myOverlayedIcon == null || getLabelComponent().getParent() == null) return;

    final Rectangle textBounds = SwingUtilities.convertRectangle(getLabelComponent().getParent(), getLabelComponent().getBounds(), this);

    if (isLayerIconEnabled(1)) {
      final int top = (getSize().height - myOverlayedIcon.getHeight()) / 2;

      TargetAWT.to(myOverlayedIcon).paintIcon(this, g, textBounds.x - myOverlayedIcon.getWidth() / 2, top);
    }
  }

  private boolean isLayerIconEnabled(int layer) {
    return myIconInfos[layer].myActive;
  }

  private void setLayerIconEnabled(int layer, boolean enabled) {
    myIconInfos[layer].myActive = enabled;
  }

  private Image getLayeredIcon(int layer) {
    return myIconInfos[layer].myImage;
  }

  public void setTabActionsAutoHide(final boolean autoHide) {
    if (myActionPanel == null || myActionPanel.isAutoHide() == autoHide) {
      return;
    }

    myActionPanel.setAutoHide(autoHide);
  }

  public void toggleShowActions(boolean show) {
    if (myActionPanel != null) {
      myActionPanel.toggleShowActions(show);
    }
  }

  public void setActionPanelVisible(boolean visible) {
    if (myActionPanel != null) {
      myActionPanel.setVisible(visible);
    }
  }

  @Override
  public String toString() {
    return myInfo.getText();
  }

  public void setTabEnabled(boolean enabled) {
    getLabelComponent().setEnabled(enabled);
  }

  public JComponent getLabelComponent() {
    return myLabel;
  }
}
