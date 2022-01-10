/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package com.intellij.openapi.wm.impl;

import com.intellij.ide.RemoteDesktopService;
import com.intellij.ide.ui.UISettings;
import com.intellij.ide.ui.UISettingsListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Splitter;
import com.intellij.openapi.ui.ThreeComponentsSplitter;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowType;
import com.intellij.reference.SoftReference;
import com.intellij.ui.ScreenUtil;
import com.intellij.ui.components.JBLayeredPane;
import com.intellij.util.ui.UIUtil;
import consulo.awt.TargetAWT;
import consulo.desktop.util.awt.migration.AWTComponentProviderUtil;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.logging.Logger;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.ToolWindowInternalDecorator;
import consulo.ui.ex.ToolWindowStripeButton;
import consulo.ui.impl.ToolWindowPanelImplEx;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.ref.Reference;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

/**
 * This panel contains all tool stripes and JLayeredPanle at the center area. All tool windows are
 * located inside this layered pane.
 *
 * @author Anton Katilin
 * @author Vladimir Kondratyev
 */
public final class DesktopToolWindowPanelImpl extends JBLayeredPane implements UISettingsListener, Disposable, ToolWindowPanelImplEx {
  private static final Logger LOG = Logger.getInstance(DesktopToolWindowPanelImpl.class);

  private final DesktopIdeFrameImpl myIdeFrame;

  private final HashMap<String, DesktopStripeButton> myId2Button = new HashMap<>();
  private final HashMap<String, DesktopInternalDecorator> myId2Decorator = new HashMap<>();
  private final HashMap<DesktopStripeButton, WindowInfoImpl> myButton2Info = new HashMap<>();
  private final HashMap<DesktopInternalDecorator, WindowInfoImpl> myDecorator2Info = new HashMap<>();
  private final HashMap<String, Float> myId2SplitProportion = new HashMap<>();
  private Pair<ToolWindow, Integer> myMaximizedProportion;
  /**
   * This panel is the layered pane where all sliding tool windows are located. The DEFAULT
   * layer contains splitters. The PALETTE layer contains all sliding tool windows.
   */
  private final MyLayeredPane myLayeredPane;
  /*
   * Splitters.
   */
  private final ThreeComponentsSplitter myVerticalSplitter;
  private final ThreeComponentsSplitter myHorizontalSplitter;

  /*
   * Tool stripes.
   */
  private final DesktopStripePanelImpl myLeftStripe;
  private final DesktopStripePanelImpl myRightStripe;
  private final DesktopStripePanelImpl myBottomStripe;
  private final DesktopStripePanelImpl myTopStripe;

  private final List<DesktopStripePanelImpl> myStripes = new ArrayList<>();

  private final DesktopToolWindowManagerImpl myManager;

  private boolean myStripesOverlayed;
  private final Disposable myDisposable = Disposable.newDisposable();
  private boolean myWidescreen;
  private boolean myLeftHorizontalSplit;
  private boolean myRightHorizontalSplit;

  DesktopToolWindowPanelImpl(@Nonnull DesktopIdeFrameImpl ideFrame, @Nonnull DesktopToolWindowManagerImpl manager) {
    myManager = manager;

    AWTComponentProviderUtil.putMark(this, this);

    setOpaque(false);
    myIdeFrame = ideFrame;

    // Splitters
    myVerticalSplitter = new ThreeComponentsSplitter(true);
    Disposer.register(this, myVerticalSplitter);
    myVerticalSplitter.setDividerWidth(0);
    myVerticalSplitter.setDividerMouseZoneSize(Registry.intValue("ide.splitter.mouseZone"));
    myVerticalSplitter.setBackground(Color.gray);
    myHorizontalSplitter = new ThreeComponentsSplitter(false);
    Disposer.register(this, myHorizontalSplitter);
    myHorizontalSplitter.setDividerWidth(0);
    myHorizontalSplitter.setDividerMouseZoneSize(Registry.intValue("ide.splitter.mouseZone"));
    myHorizontalSplitter.setBackground(Color.gray);
    myWidescreen = UISettings.getInstance().getWideScreenSupport();
    myLeftHorizontalSplit = UISettings.getInstance().getLeftHorizontalSplit();
    myRightHorizontalSplit = UISettings.getInstance().getRightHorizontalSplit();
    if (myWidescreen) {
      myHorizontalSplitter.setInnerComponent(myVerticalSplitter);
    }
    else {
      myVerticalSplitter.setInnerComponent(myHorizontalSplitter);
    }

    // Tool stripes

    myTopStripe = new DesktopStripePanelImpl(SwingConstants.TOP, manager);
    myStripes.add(myTopStripe);
    myLeftStripe = new DesktopStripePanelImpl(SwingConstants.LEFT, manager);
    myStripes.add(myLeftStripe);
    myBottomStripe = new DesktopStripePanelImpl(SwingConstants.BOTTOM, manager);
    myStripes.add(myBottomStripe);
    myRightStripe = new DesktopStripePanelImpl(SwingConstants.RIGHT, manager);
    myStripes.add(myRightStripe);

    updateToolStripesVisibility();

    // Layered pane

    myLayeredPane = new MyLayeredPane(myWidescreen ? myHorizontalSplitter : myVerticalSplitter);

    // Compose layout

    add(myTopStripe, JLayeredPane.POPUP_LAYER);
    add(myLeftStripe, JLayeredPane.POPUP_LAYER);
    add(myBottomStripe, JLayeredPane.POPUP_LAYER);
    add(myRightStripe, JLayeredPane.POPUP_LAYER);
    add(myLayeredPane, JLayeredPane.DEFAULT_LAYER);
  }

  @Override
  public void doLayout() {
    Dimension size = getSize();
    if (!myTopStripe.isVisible()) {
      myTopStripe.setBounds(0, 0, 0, 0);
      myBottomStripe.setBounds(0, 0, 0, 0);
      myLeftStripe.setBounds(0, 0, 0, 0);
      myRightStripe.setBounds(0, 0, 0, 0);
      myLayeredPane.setBounds(0, 0, getWidth(), getHeight());
    }
    else {
      Dimension topSize = myTopStripe.getPreferredSize();
      Dimension bottomSize = myBottomStripe.getPreferredSize();
      Dimension leftSize = myLeftStripe.getPreferredSize();
      Dimension rightSize = myRightStripe.getPreferredSize();

      myTopStripe.setBounds(0, 0, size.width, topSize.height);
      myLeftStripe.setBounds(0, topSize.height, leftSize.width, size.height - topSize.height - bottomSize.height);
      myRightStripe.setBounds(size.width - rightSize.width, topSize.height, rightSize.width, size.height - topSize.height - bottomSize.height);
      myBottomStripe.setBounds(0, size.height - bottomSize.height, size.width, bottomSize.height);

      if (UISettings.getInstance().getHideToolStripes() || UISettings.getInstance().getPresentationMode()) {
        myLayeredPane.setBounds(0, 0, size.width, size.height);
      }
      else {
        myLayeredPane.setBounds(leftSize.width, topSize.height, size.width - leftSize.width - rightSize.width, size.height - topSize.height - bottomSize.height);
      }
    }
  }

  @Override
  protected void paintChildren(Graphics g) {
    super.paintChildren(g);
  }

  /**
   * Invoked when enclosed frame is being shown.
   */
  @Override
  public final void addNotify() {
    super.addNotify();
  }

  /**
   * Invoked when enclosed frame is being disposed.
   */
  @Override
  public final void removeNotify() {
    if (ScreenUtil.isStandardAddRemoveNotify(this)) {
      Disposer.dispose(myDisposable);
    }
    super.removeNotify();
  }

  public Project getProject() {
    return myIdeFrame.getProject();
  }

  @Override
  public final void uiSettingsChanged(final UISettings uiSettings) {
    updateToolStripesVisibility();
    updateLayout();
  }

  /**
   * Creates command which adds button into the specified tool stripe.
   * Command uses copy of passed <code>info</code> object.
   *
   * @param button     button which should be added.
   * @param info       window info for the corresponded tool window.
   * @param comparator which is used to sort buttons within the stripe.
   */
  @RequiredUIAccess
  @Override
  public final void addButton(final ToolWindowStripeButton button, @Nonnull WindowInfoImpl info, @Nonnull Comparator<ToolWindowStripeButton> comparator) {
    final WindowInfoImpl copiedInfo = info.copy();
    myId2Button.put(copiedInfo.getId(), (DesktopStripeButton)button);
    myButton2Info.put((DesktopStripeButton)button, copiedInfo);


    final ToolWindowAnchor anchor = copiedInfo.getAnchor();
    if (ToolWindowAnchor.TOP == anchor) {
      myTopStripe.addButton((DesktopStripeButton)button, comparator);
    }
    else if (ToolWindowAnchor.LEFT == anchor) {
      myLeftStripe.addButton((DesktopStripeButton)button, comparator);
    }
    else if (ToolWindowAnchor.BOTTOM == anchor) {
      myBottomStripe.addButton((DesktopStripeButton)button, comparator);
    }
    else if (ToolWindowAnchor.RIGHT == anchor) {
      myRightStripe.addButton((DesktopStripeButton)button, comparator);
    }
    else {
      LOG.error("unknown anchor: " + anchor);
    }
    validate();
    repaint();
  }

  /**
   * Creates command which shows tool window with specified set of parameters.
   * Command uses cloned copy of passed <code>info</code> object.
   *
   * @param dirtyMode if <code>true</code> then JRootPane will not be validated and repainted after adding
   *                  the decorator. Moreover in this (dirty) mode animation doesn't work.
   */
  @Override
  @RequiredUIAccess
  public void addDecorator(@Nonnull ToolWindowInternalDecorator decorator, @Nonnull WindowInfoImpl info, final boolean dirtyMode) {
    final WindowInfoImpl copiedInfo = info.copy();
    final String id = copiedInfo.getId();

    myDecorator2Info.put((DesktopInternalDecorator)decorator, copiedInfo);
    myId2Decorator.put(id, (DesktopInternalDecorator)decorator);

    if (info.isDocked()) {
      WindowInfoImpl sideInfo = getDockedInfoAt(info.getAnchor(), !info.isSplit());
      if (sideInfo == null) {
        new AddDockedComponentCmd((DesktopInternalDecorator)decorator, info, dirtyMode).run();
      }
      else {
        new AddAndSplitDockedComponentCmd((DesktopInternalDecorator)decorator, info, dirtyMode).run();
      }
    }
    else if (info.isSliding()) {
      new AddSlidingComponentCmd((DesktopInternalDecorator)decorator, info, dirtyMode).run();
    }
    else {
      throw new IllegalArgumentException("Unknown window type: " + info.getType());
    }
  }

  /**
   * Creates command which removes tool button from tool stripe.
   *
   * @param id <code>ID</code> of the button to be removed.
   */
  @Override
  @RequiredUIAccess
  public void removeButton(@Nonnull String id) {
    final DesktopStripeButton button = getButtonById(id);
    if (button == null) {
      return;
    }
    final WindowInfoImpl info = getButtonInfoById(id);

    myButton2Info.remove(button);
    myId2Button.remove(id);
    final ToolWindowAnchor anchor = info.getAnchor();
    if (ToolWindowAnchor.TOP == anchor) {
      myTopStripe.removeButton(button);
    }
    else if (ToolWindowAnchor.LEFT == anchor) {
      myLeftStripe.removeButton(button);
    }
    else if (ToolWindowAnchor.BOTTOM == anchor) {
      myBottomStripe.removeButton(button);
    }
    else if (ToolWindowAnchor.RIGHT == anchor) {
      myRightStripe.removeButton(button);
    }
    else {
      LOG.error("unknown anchor: " + anchor);
    }

    validate();
    repaint();
  }

  /**
   * Creates command which hides tool window with specified set of parameters.
   *
   * @param dirtyMode if <code>true</code> then JRootPane will not be validated and repainted after removing
   *                  the decorator. Moreover in this (dirty) mode animation doesn't work.
   */
  @RequiredUIAccess
  @Nonnull
  public void removeDecorator(@Nonnull String id, final boolean dirtyMode) {
    final Component decorator = getDecoratorById(id);
    final WindowInfoImpl info = getDecoratorInfoById(id);

    myDecorator2Info.remove(decorator);
    myId2Decorator.remove(id);

    WindowInfoImpl sideInfo = getDockedInfoAt(info.getAnchor(), !info.isSplit());

    if (info.isDocked()) {
      if (sideInfo == null) {
        new RemoveDockedComponentCmd(info, dirtyMode).run();
      }
      else {
        new RemoveSplitAndDockedComponentCmd(info, dirtyMode).run();
      }
    }
    else if (info.isSliding()) {
      new RemoveSlidingComponentCmd(decorator, info, dirtyMode).run();
    }
    else {
      throw new IllegalArgumentException("Unknown window type");
    }
  }

  /**
   * Creates command which sets specified document component.
   *
   * @param component component to be set.
   */
  @Override
  @RequiredUIAccess
  public void setEditorComponent(final Object component) {
    setDocumentComponent((JComponent)component);

    myLayeredPane.validate();
    myLayeredPane.repaint();
  }

  @Override
  @RequiredUIAccess
  public void updateButtonPosition(@Nonnull String id) {
    DesktopStripeButton stripeButton = getButtonById(id);
    if (stripeButton == null) {
      return;
    }

    WindowInfoImpl info = stripeButton.getWindowInfo();
    ToolWindowAnchor anchor = info.getAnchor();

    if (ToolWindowAnchor.TOP == anchor) {
      myTopStripe.revalidate();
    }
    else if (ToolWindowAnchor.LEFT == anchor) {
      myLeftStripe.revalidate();
    }
    else if (ToolWindowAnchor.BOTTOM == anchor) {
      myBottomStripe.revalidate();
    }
    else if (ToolWindowAnchor.RIGHT == anchor) {
      myRightStripe.revalidate();
    }
    else {
      LOG.error("unknown anchor: " + anchor);
    }
  }

  @Override
  @Nonnull
  public final JComponent getMyLayeredPane() {
    return myLayeredPane;
  }

  @Nullable
  private DesktopStripeButton getButtonById(final String id) {
    return myId2Button.get(id);
  }

  private Component getDecoratorById(final String id) {
    return myId2Decorator.get(id);
  }

  /**
   * @param id <code>ID</code> of tool stripe butoon.
   * @return <code>WindowInfo</code> associated with specified tool stripe button.
   */
  private WindowInfoImpl getButtonInfoById(final String id) {
    return myButton2Info.get(myId2Button.get(id));
  }

  /**
   * @param id <code>ID</code> of decorator.
   * @return <code>WindowInfo</code> associated with specified window decorator.
   */
  private WindowInfoImpl getDecoratorInfoById(final String id) {
    return myDecorator2Info.get(myId2Decorator.get(id));
  }

  /**
   * Sets (docks) specified component to the specified anchor.
   */
  private void setComponent(final JComponent component, @Nonnull ToolWindowAnchor anchor, final float weight) {
    if (ToolWindowAnchor.TOP == anchor) {
      myVerticalSplitter.setFirstComponent(component);
      myVerticalSplitter.setFirstSize((int)(myLayeredPane.getHeight() * weight));
    }
    else if (ToolWindowAnchor.LEFT == anchor) {
      myHorizontalSplitter.setFirstComponent(component);
      myHorizontalSplitter.setFirstSize((int)(myLayeredPane.getWidth() * weight));
    }
    else if (ToolWindowAnchor.BOTTOM == anchor) {
      myVerticalSplitter.setLastComponent(component);
      myVerticalSplitter.setLastSize((int)(myLayeredPane.getHeight() * weight));
    }
    else if (ToolWindowAnchor.RIGHT == anchor) {
      myHorizontalSplitter.setLastComponent(component);
      myHorizontalSplitter.setLastSize((int)(myLayeredPane.getWidth() * weight));
    }
    else {
      LOG.error("unknown anchor: " + anchor);
    }
  }

  private JComponent getComponentAt(@Nonnull ToolWindowAnchor anchor) {
    if (ToolWindowAnchor.TOP == anchor) {
      return myVerticalSplitter.getFirstComponent();
    }
    else if (ToolWindowAnchor.LEFT == anchor) {
      return myHorizontalSplitter.getFirstComponent();
    }
    else if (ToolWindowAnchor.BOTTOM == anchor) {
      return myVerticalSplitter.getLastComponent();
    }
    else if (ToolWindowAnchor.RIGHT == anchor) {
      return myHorizontalSplitter.getLastComponent();
    }
    else {
      LOG.error("unknown anchor: " + anchor);
      return null;
    }
  }

  private float getPreferredSplitProportion(@Nonnull String id, float defaultValue) {
    Float f = myId2SplitProportion.get(id);
    return f == null ? defaultValue : f;
  }

  private WindowInfoImpl getDockedInfoAt(@Nonnull ToolWindowAnchor anchor, boolean side) {
    for (WindowInfoImpl info : myDecorator2Info.values()) {
      if (info.isVisible() && info.isDocked() && info.getAnchor() == anchor && side == info.isSplit()) {
        return info;
      }
    }

    return null;
  }

  private void setDocumentComponent(final JComponent component) {
    (myWidescreen ? myVerticalSplitter : myHorizontalSplitter).setInnerComponent(component);
  }

  private void updateToolStripesVisibility() {
    boolean oldVisible = myLeftStripe.isVisible();

    final boolean showButtons = !UISettings.getInstance().getHideToolStripes() && !UISettings.getInstance().getPresentationMode();
    boolean visible = showButtons || myStripesOverlayed;
    myLeftStripe.setVisible(visible);
    myRightStripe.setVisible(visible);
    myTopStripe.setVisible(visible);
    myBottomStripe.setVisible(visible);

    boolean overlayed = !showButtons && myStripesOverlayed;

    myLeftStripe.setOverlayed(overlayed);
    myRightStripe.setOverlayed(overlayed);
    myTopStripe.setOverlayed(overlayed);
    myBottomStripe.setOverlayed(overlayed);


    if (oldVisible != visible) {
      revalidate();
      repaint();
    }
  }

  public int getBottomHeight() {
    return myBottomStripe.isVisible() ? myBottomStripe.getHeight() : 0;
  }

  public boolean isBottomSideToolWindowsVisible() {
    return getComponentAt(ToolWindowAnchor.BOTTOM) != null;
  }

  @Nullable
  DesktopStripePanelImpl getStripeFor(String id) {
    ToolWindow window = myManager.getToolWindow(id);
    if (window == null) {
      return null;
    }

    final ToolWindowAnchor anchor = myManager.getToolWindow(id).getAnchor();
    if (ToolWindowAnchor.TOP == anchor) {
      return myTopStripe;
    }
    if (ToolWindowAnchor.BOTTOM == anchor) {
      return myBottomStripe;
    }
    if (ToolWindowAnchor.LEFT == anchor) {
      return myLeftStripe;
    }
    if (ToolWindowAnchor.RIGHT == anchor) {
      return myRightStripe;
    }

    throw new IllegalArgumentException("Anchor=" + anchor);
  }

  @Nullable
  DesktopStripePanelImpl getStripeFor(@Nonnull Rectangle screenRec, @Nonnull DesktopStripePanelImpl preferred) {
    if (preferred.containsScreen(screenRec)) {
      return myStripes.get(myStripes.indexOf(preferred));
    }

    for (DesktopStripePanelImpl each : myStripes) {
      if (each.containsScreen(screenRec)) {
        return myStripes.get(myStripes.indexOf(each));
      }
    }

    return null;
  }

  void startDrag() {
    for (DesktopStripePanelImpl each : myStripes) {
      each.startDrag();
    }
  }

  void stopDrag() {
    for (DesktopStripePanelImpl each : myStripes) {
      each.stopDrag();
    }
  }

  void stretchWidth(@Nonnull ToolWindow wnd, int value) {
    stretch(wnd, value);
  }

  void stretchHeight(@Nonnull ToolWindow wnd, int value) {
    stretch(wnd, value);
  }

  private void stretch(@Nonnull ToolWindow wnd, int value) {
    Pair<Resizer, Component> pair = findResizerAndComponent(wnd);
    if (pair == null) return;

    boolean vertical = wnd.getAnchor() == ToolWindowAnchor.TOP || wnd.getAnchor() == ToolWindowAnchor.BOTTOM;
    int actualSize = (vertical ? pair.second.getHeight() : pair.second.getWidth()) + value;
    boolean first = wnd.getAnchor() == ToolWindowAnchor.LEFT || wnd.getAnchor() == ToolWindowAnchor.TOP;
    int maxValue = vertical ? myVerticalSplitter.getMaxSize(first) : myHorizontalSplitter.getMaxSize(first);
    int minValue = vertical ? myVerticalSplitter.getMinSize(first) : myHorizontalSplitter.getMinSize(first);

    pair.first.setSize(Math.max(minValue, Math.min(maxValue, actualSize)));
  }

  @Nullable
  private Pair<Resizer, Component> findResizerAndComponent(@Nonnull ToolWindow wnd) {
    if (!wnd.isVisible()) return null;

    Resizer resizer = null;
    Component cmp = null;

    if (wnd.getType() == ToolWindowType.DOCKED) {
      cmp = getComponentAt(wnd.getAnchor());

      if (cmp != null) {
        if (wnd.getAnchor().isHorizontal()) {
          resizer = myVerticalSplitter.getFirstComponent() == cmp ? new Resizer.Splitter.FirstComponent(myVerticalSplitter) : new Resizer.Splitter.LastComponent(myVerticalSplitter);
        }
        else {
          resizer = myHorizontalSplitter.getFirstComponent() == cmp ? new Resizer.Splitter.FirstComponent(myHorizontalSplitter) : new Resizer.Splitter.LastComponent(myHorizontalSplitter);
        }
      }
    }
    else if (wnd.getType() == ToolWindowType.SLIDING) {
      cmp = wnd.getComponent();
      while (cmp != null) {
        if (cmp.getParent() == myLayeredPane) break;
        cmp = cmp.getParent();
      }

      if (cmp != null) {
        if (wnd.getAnchor() == ToolWindowAnchor.TOP) {
          resizer = new Resizer.LayeredPane.Top(cmp);
        }
        else if (wnd.getAnchor() == ToolWindowAnchor.BOTTOM) {
          resizer = new Resizer.LayeredPane.Bottom(cmp);
        }
        else if (wnd.getAnchor() == ToolWindowAnchor.LEFT) {
          resizer = new Resizer.LayeredPane.Left(cmp);
        }
        else if (wnd.getAnchor() == ToolWindowAnchor.RIGHT) {
          resizer = new Resizer.LayeredPane.Right(cmp);
        }
      }
    }

    return resizer != null ? Pair.create(resizer, cmp) : null;
  }

  private void updateLayout() {
    UISettings uiSettings = UISettings.getInstance();
    if (myWidescreen != uiSettings.getWideScreenSupport()) {
      JComponent documentComponent = (myWidescreen ? myVerticalSplitter : myHorizontalSplitter).getInnerComponent();
      myWidescreen = uiSettings.getWideScreenSupport();
      if (myWidescreen) {
        myVerticalSplitter.setInnerComponent(null);
        myHorizontalSplitter.setInnerComponent(myVerticalSplitter);
      }
      else {
        myHorizontalSplitter.setInnerComponent(null);
        myVerticalSplitter.setInnerComponent(myHorizontalSplitter);
      }
      myLayeredPane.remove(myWidescreen ? myVerticalSplitter : myHorizontalSplitter);
      myLayeredPane.add(myWidescreen ? myHorizontalSplitter : myVerticalSplitter, DEFAULT_LAYER);
      setDocumentComponent(documentComponent);
    }
    if (myLeftHorizontalSplit != uiSettings.getLeftHorizontalSplit()) {
      JComponent component = getComponentAt(ToolWindowAnchor.LEFT);
      if (component instanceof Splitter) {
        Splitter splitter = (Splitter)component;
        DesktopInternalDecorator first = (DesktopInternalDecorator)splitter.getFirstComponent();
        DesktopInternalDecorator second = (DesktopInternalDecorator)splitter.getSecondComponent();
        setComponent(splitter, ToolWindowAnchor.LEFT,
                     ToolWindowAnchor.LEFT.isSplitVertically() ? first.getWindowInfo().getWeight() : first.getWindowInfo().getWeight() + second.getWindowInfo().getWeight());
      }
      myLeftHorizontalSplit = uiSettings.getLeftHorizontalSplit();
    }
    if (myRightHorizontalSplit != uiSettings.getRightHorizontalSplit()) {
      JComponent component = getComponentAt(ToolWindowAnchor.RIGHT);
      if (component instanceof Splitter) {
        Splitter splitter = (Splitter)component;
        DesktopInternalDecorator first = (DesktopInternalDecorator)splitter.getFirstComponent();
        DesktopInternalDecorator second = (DesktopInternalDecorator)splitter.getSecondComponent();
        setComponent(splitter, ToolWindowAnchor.RIGHT,
                     ToolWindowAnchor.RIGHT.isSplitVertically() ? first.getWindowInfo().getWeight() : first.getWindowInfo().getWeight() + second.getWindowInfo().getWeight());
      }
      myRightHorizontalSplit = uiSettings.getRightHorizontalSplit();
    }
  }

  public boolean isMaximized(@Nonnull ToolWindow wnd) {
    return myMaximizedProportion != null && myMaximizedProportion.first == wnd;
  }

  void setMaximized(@Nonnull ToolWindow wnd, boolean maximized) {
    Pair<Resizer, Component> resizerAndComponent = findResizerAndComponent(wnd);
    if (resizerAndComponent == null) return;

    if (!maximized) {
      ToolWindow maximizedWindow = myMaximizedProportion.first;
      assert maximizedWindow == wnd;
      resizerAndComponent.first.setSize(myMaximizedProportion.second);
      myMaximizedProportion = null;
    }
    else {
      int size = wnd.getAnchor().isHorizontal() ? resizerAndComponent.second.getHeight() : resizerAndComponent.second.getWidth();
      stretch(wnd, Short.MAX_VALUE);
      myMaximizedProportion = Pair.create(wnd, size);
    }
    doLayout();
  }

  @Nullable
  @Override
  public Component getComponent() {
    return this;
  }


  @FunctionalInterface
  interface Resizer {
    void setSize(int size);


    abstract class Splitter implements Resizer {
      ThreeComponentsSplitter mySplitter;

      Splitter(@Nonnull ThreeComponentsSplitter splitter) {
        mySplitter = splitter;
      }

      static class FirstComponent extends Splitter {
        FirstComponent(@Nonnull ThreeComponentsSplitter splitter) {
          super(splitter);
        }

        @Override
        public void setSize(int size) {
          mySplitter.setFirstSize(size);
        }
      }

      static class LastComponent extends Splitter {
        LastComponent(@Nonnull ThreeComponentsSplitter splitter) {
          super(splitter);
        }

        @Override
        public void setSize(int size) {
          mySplitter.setLastSize(size);
        }
      }
    }

    abstract class LayeredPane implements Resizer {
      Component myComponent;

      LayeredPane(@Nonnull Component component) {
        myComponent = component;
      }

      @Override
      public final void setSize(int size) {
        _setSize(size);
        if (myComponent.getParent() instanceof JComponent) {
          JComponent parent = (JComponent)myComponent;
          parent.revalidate();
          parent.repaint();
        }
      }

      abstract void _setSize(int size);

      static class Left extends LayeredPane {

        Left(@Nonnull Component component) {
          super(component);
        }

        @Override
        public void _setSize(int size) {
          myComponent.setSize(size, myComponent.getHeight());
        }
      }

      static class Right extends LayeredPane {
        Right(@Nonnull Component component) {
          super(component);
        }

        @Override
        public void _setSize(int size) {
          Rectangle bounds = myComponent.getBounds();
          int delta = size - bounds.width;
          bounds.x -= delta;
          bounds.width += delta;
          myComponent.setBounds(bounds);
        }
      }

      static class Top extends LayeredPane {
        Top(@Nonnull Component component) {
          super(component);
        }

        @Override
        public void _setSize(int size) {
          myComponent.setSize(myComponent.getWidth(), size);
        }
      }

      static class Bottom extends LayeredPane {
        Bottom(@Nonnull Component component) {
          super(component);
        }

        @Override
        public void _setSize(int size) {
          Rectangle bounds = myComponent.getBounds();
          int delta = size - bounds.height;
          bounds.y -= delta;
          bounds.height += delta;
          myComponent.setBounds(bounds);
        }
      }
    }
  }

  private final class AddDockedComponentCmd implements Runnable {
    private final JComponent myComponent;
    private final WindowInfoImpl myInfo;
    private final boolean myDirtyMode;

    public AddDockedComponentCmd(@Nonnull JComponent component, @Nonnull WindowInfoImpl info, final boolean dirtyMode) {
      myComponent = component;
      myInfo = info;
      myDirtyMode = dirtyMode;
    }

    @Override
    public final void run() {
      final ToolWindowAnchor anchor = myInfo.getAnchor();
      setComponent(myComponent, anchor, WindowInfoImpl.normalizeWeigh(myInfo.getWeight()));
      if (!myDirtyMode) {
        myLayeredPane.validate();
        myLayeredPane.repaint();
      }
    }
  }

  private final class AddAndSplitDockedComponentCmd implements Runnable {
    private final JComponent myNewComponent;
    private final WindowInfoImpl myInfo;
    private final boolean myDirtyMode;

    private AddAndSplitDockedComponentCmd(@Nonnull JComponent newComponent, @Nonnull WindowInfoImpl info, final boolean dirtyMode) {
      myNewComponent = newComponent;
      myInfo = info;
      myDirtyMode = dirtyMode;
    }

    @Override
    public void run() {
      final ToolWindowAnchor anchor = myInfo.getAnchor();
      class MySplitter extends Splitter implements UISettingsListener {
        @Override
        public void uiSettingsChanged(UISettings uiSettings) {
          if (anchor == ToolWindowAnchor.LEFT) {
            setOrientation(!uiSettings.getLeftHorizontalSplit());
          }
          else if (anchor == ToolWindowAnchor.RIGHT) {
            setOrientation(!uiSettings.getRightHorizontalSplit());
          }
        }
      }
      Splitter splitter = new MySplitter();
      splitter.setOrientation(anchor.isSplitVertically());
      if (!anchor.isHorizontal()) {
        splitter.setAllowSwitchOrientationByMouseClick(true);
        splitter.addPropertyChangeListener(evt -> {
          if (!Splitter.PROP_ORIENTATION.equals(evt.getPropertyName())) return;
          boolean isSplitterHorizontalNow = !splitter.isVertical();
          UISettings settings = UISettings.getInstance();
          if (anchor == ToolWindowAnchor.LEFT) {
            if (settings.getLeftHorizontalSplit() != isSplitterHorizontalNow) {
              settings.setLeftHorizontalSplit(isSplitterHorizontalNow);
              settings.fireUISettingsChanged();
            }
          }
          if (anchor == ToolWindowAnchor.RIGHT) {
            if (settings.getRightHorizontalSplit() != isSplitterHorizontalNow) {
              settings.setRightHorizontalSplit(isSplitterHorizontalNow);
              settings.fireUISettingsChanged();
            }
          }
        });
      }
      JComponent c = getComponentAt(anchor);
      float newWeight;
      if (c instanceof DesktopInternalDecorator) {
        DesktopInternalDecorator oldComponent = (DesktopInternalDecorator)c;
        if (myInfo.isSplit()) {
          splitter.setFirstComponent(oldComponent);
          splitter.setSecondComponent(myNewComponent);
          float proportion = getPreferredSplitProportion(oldComponent.getWindowInfo().getId(), WindowInfoImpl
                  .normalizeWeigh(oldComponent.getWindowInfo().getSideWeight() / (oldComponent.getWindowInfo().getSideWeight() + myInfo.getSideWeight())));
          splitter.setProportion(proportion);
          if (!anchor.isHorizontal() && !anchor.isSplitVertically()) {
            newWeight = WindowInfoImpl.normalizeWeigh(oldComponent.getWindowInfo().getWeight() + myInfo.getWeight());
          }
          else {
            newWeight = WindowInfoImpl.normalizeWeigh(oldComponent.getWindowInfo().getWeight());
          }
        }
        else {
          splitter.setFirstComponent(myNewComponent);
          splitter.setSecondComponent(oldComponent);
          splitter.setProportion(WindowInfoImpl.normalizeWeigh(myInfo.getSideWeight()));
          if (!anchor.isHorizontal() && !anchor.isSplitVertically()) {
            newWeight = WindowInfoImpl.normalizeWeigh(oldComponent.getWindowInfo().getWeight() + myInfo.getWeight());
          }
          else {
            newWeight = WindowInfoImpl.normalizeWeigh(myInfo.getWeight());
          }
        }
      }
      else {
        newWeight = WindowInfoImpl.normalizeWeigh(myInfo.getWeight());
      }
      setComponent(splitter, anchor, newWeight);

      if (!myDirtyMode) {
        myLayeredPane.validate();
        myLayeredPane.repaint();
      }
    }
  }

  private final class AddSlidingComponentCmd implements Runnable {
    private final Component myComponent;
    private final WindowInfoImpl myInfo;
    private final boolean myDirtyMode;

    public AddSlidingComponentCmd(@Nonnull Component component, @Nonnull WindowInfoImpl info, final boolean dirtyMode) {
      myComponent = component;
      myInfo = info;
      myDirtyMode = dirtyMode;
    }

    @Override
    public final void run() {
      // Show component.
      if (!myDirtyMode && UISettings.getInstance().getAnimateWindows() && !RemoteDesktopService.isRemoteSession()) {
        // Prepare top image. This image is scrolling over bottom image.
        final Image topImage = myLayeredPane.getTopImage();
        final Graphics topGraphics = topImage.getGraphics();

        Rectangle bounds;

        try {
          myLayeredPane.add(myComponent, JLayeredPane.PALETTE_LAYER);
          myLayeredPane.moveToFront(myComponent);
          myLayeredPane.setBoundsInPaletteLayer(myComponent, myInfo.getAnchor(), myInfo.getWeight());
          bounds = myComponent.getBounds();
          myComponent.paint(topGraphics);
          myLayeredPane.remove(myComponent);
        }
        finally {
          topGraphics.dispose();
        }
        // Prepare bottom image.
        final Image bottomImage = myLayeredPane.getBottomImage();
        final Graphics bottomGraphics = bottomImage.getGraphics();
        try {
          bottomGraphics.setClip(0, 0, bounds.width, bounds.height);
          bottomGraphics.translate(-bounds.x, -bounds.y);
          myLayeredPane.paint(bottomGraphics);
        }
        finally {
          bottomGraphics.dispose();
        }
        // Start animation.
        final Surface surface = new Surface(topImage, bottomImage, 1, myInfo.getAnchor(), UISettings.ANIMATION_DURATION);
        myLayeredPane.add(surface, JLayeredPane.PALETTE_LAYER);
        surface.setBounds(bounds);
        myLayeredPane.validate();
        myLayeredPane.repaint();

        surface.runMovement();
        myLayeredPane.remove(surface);
        myLayeredPane.add(myComponent, JLayeredPane.PALETTE_LAYER);
      }
      else { // not animated
        myLayeredPane.add(myComponent, JLayeredPane.PALETTE_LAYER);
        myLayeredPane.setBoundsInPaletteLayer(myComponent, myInfo.getAnchor(), myInfo.getWeight());
      }
      if (!myDirtyMode) {
        myLayeredPane.validate();
        myLayeredPane.repaint();
      }
    }
  }

  private final class RemoveDockedComponentCmd implements Runnable {
    private final WindowInfoImpl myInfo;
    private final boolean myDirtyMode;

    public RemoveDockedComponentCmd(@Nonnull WindowInfoImpl info, final boolean dirtyMode) {
      myInfo = info;
      myDirtyMode = dirtyMode;
    }

    @Override
    public final void run() {
      setComponent(null, myInfo.getAnchor(), 0);
      if (!myDirtyMode) {
        myLayeredPane.validate();
        myLayeredPane.repaint();
      }
    }
  }

  private final class RemoveSplitAndDockedComponentCmd implements Runnable {
    private final WindowInfoImpl myInfo;
    private final boolean myDirtyMode;

    private RemoveSplitAndDockedComponentCmd(@Nonnull WindowInfoImpl info, boolean dirtyMode) {
      myInfo = info;
      myDirtyMode = dirtyMode;
    }

    @Override
    public void run() {
      ToolWindowAnchor anchor = myInfo.getAnchor();
      JComponent c = getComponentAt(anchor);
      if (c instanceof Splitter) {
        Splitter splitter = (Splitter)c;
        final DesktopInternalDecorator component = myInfo.isSplit() ? (DesktopInternalDecorator)splitter.getFirstComponent() : (DesktopInternalDecorator)splitter.getSecondComponent();
        if (myInfo.isSplit() && component != null) {
          myId2SplitProportion.put(component.getWindowInfo().getId(), splitter.getProportion());
        }
        setComponent(component, anchor, component != null ? component.getWindowInfo().getWeight() : 0);
      }
      else {
        setComponent(null, anchor, 0);
      }
      if (!myDirtyMode) {
        myLayeredPane.validate();
        myLayeredPane.repaint();
      }
    }
  }

  private final class RemoveSlidingComponentCmd implements Runnable {
    private final Component myComponent;
    private final WindowInfoImpl myInfo;
    private final boolean myDirtyMode;

    public RemoveSlidingComponentCmd(Component component, @Nonnull WindowInfoImpl info, boolean dirtyMode) {
      myComponent = component;
      myInfo = info;
      myDirtyMode = dirtyMode;
    }

    @Override
    public final void run() {
      final UISettings uiSettings = UISettings.getInstance();
      if (!myDirtyMode && uiSettings.getAnimateWindows() && !RemoteDesktopService.isRemoteSession()) {
        final Rectangle bounds = myComponent.getBounds();
        // Prepare top image. This image is scrolling over bottom image. It contains
        // picture of component is being removed.
        final Image topImage = myLayeredPane.getTopImage();
        final Graphics topGraphics = topImage.getGraphics();
        try {
          myComponent.paint(topGraphics);
        }
        finally {
          topGraphics.dispose();
        }
        // Prepare bottom image. This image contains picture of component that is located
        // under the component to is being removed.
        final Image bottomImage = myLayeredPane.getBottomImage();
        final Graphics bottomGraphics = bottomImage.getGraphics();
        try {
          myLayeredPane.remove(myComponent);
          bottomGraphics.clipRect(0, 0, bounds.width, bounds.height);
          bottomGraphics.translate(-bounds.x, -bounds.y);
          myLayeredPane.paint(bottomGraphics);
        }
        finally {
          bottomGraphics.dispose();
        }
        // Remove component from the layered pane and start animation.
        final Surface surface = new Surface(topImage, bottomImage, -1, myInfo.getAnchor(), UISettings.ANIMATION_DURATION);
        myLayeredPane.add(surface, JLayeredPane.PALETTE_LAYER);
        surface.setBounds(bounds);
        myLayeredPane.validate();
        myLayeredPane.repaint();

        surface.runMovement();
        myLayeredPane.remove(surface);
      }
      else { // not animated
        myLayeredPane.remove(myComponent);
      }
      if (!myDirtyMode) {
        myLayeredPane.validate();
        myLayeredPane.repaint();
      }
    }
  }

  private final class MyLayeredPane extends JBLayeredPane {
    /*
     * These images are used to perform animated showing and hiding of components.
     * They are the member for performance reason.
     */
    private Reference<BufferedImage> myBottomImageRef;
    private Reference<BufferedImage> myTopImageRef;

    public MyLayeredPane(@Nonnull JComponent splitter) {
      setOpaque(false);
      add(splitter, JLayeredPane.DEFAULT_LAYER);
    }

    final Image getBottomImage() {
      Pair<BufferedImage, Reference<BufferedImage>> result = getImage(myBottomImageRef);
      myBottomImageRef = result.second;
      return result.first;
    }

    final Image getTopImage() {
      Pair<BufferedImage, Reference<BufferedImage>> result = getImage(myTopImageRef);
      myTopImageRef = result.second;
      return result.first;
    }

    @Nonnull
    private Pair<BufferedImage, Reference<BufferedImage>> getImage(@Nullable Reference<BufferedImage> imageRef) {
      LOG.assertTrue(UISettings.getInstance().getAnimateWindows());
      Window awtWindow = TargetAWT.to(myIdeFrame.getWindow());
      BufferedImage image = SoftReference.dereference(imageRef);
      if (image == null || image.getWidth(null) < getWidth() || image.getHeight(null) < getHeight()) {
        final int width = Math.max(Math.max(1, getWidth()), awtWindow.getWidth());
        final int height = Math.max(Math.max(1, getHeight()), awtWindow.getHeight());
        if (SystemInfo.isWindows) {
          image = awtWindow.getGraphicsConfiguration().createCompatibleImage(width, height);
        }
        else {
          // Under Linux we have found that images created by createCompatibleImage(),
          // createVolatileImage(), etc extremely slow for rendering. TrueColor buffered image
          // is MUCH faster.
          // On Mac we create a retina-compatible image

          image = UIUtil.createImage(getGraphics(), width, height, BufferedImage.TYPE_INT_RGB);
        }
        imageRef = new SoftReference<>(image);
      }
      return Pair.create(image, imageRef);
    }

    /**
     * When component size becomes larger then bottom and top images should be enlarged.
     */
    @Override
    public void doLayout() {
      final int width = getWidth();
      final int height = getHeight();
      if (width < 0 || height < 0) {
        return;
      }
      // Resize component at the DEFAULT layer. It should be only on component in that layer
      Component[] components = getComponentsInLayer(JLayeredPane.DEFAULT_LAYER.intValue());
      LOG.assertTrue(components.length <= 1);
      for (final Component component : components) {
        component.setBounds(0, 0, getWidth(), getHeight());
      }
      // Resize components at the PALETTE layer
      components = getComponentsInLayer(JLayeredPane.PALETTE_LAYER.intValue());
      for (final Component component : components) {
        if (!(component instanceof DesktopInternalDecorator)) {
          continue;
        }
        final WindowInfoImpl info = myDecorator2Info.get(component);
        // In normal situation info is not null. But sometimes Swing sends resize
        // event to removed component. See SCR #19566.
        if (info == null) {
          continue;
        }

        final float weight;
        if (info.getAnchor().isHorizontal()) {
          weight = (float)component.getHeight() / (float)getHeight();
        }
        else {
          weight = (float)component.getWidth() / (float)getWidth();
        }
        setBoundsInPaletteLayer(component, info.getAnchor(), weight);
      }
    }

    final void setBoundsInPaletteLayer(@Nonnull Component component, @Nonnull ToolWindowAnchor anchor, float weight) {
      if (weight < .0f) {
        weight = WindowInfoImpl.DEFAULT_WEIGHT;
      }
      else if (weight > 1.0f) {
        weight = 1.0f;
      }
      if (ToolWindowAnchor.TOP == anchor) {
        component.setBounds(0, 0, getWidth(), (int)(getHeight() * weight + .5f));
      }
      else if (ToolWindowAnchor.LEFT == anchor) {
        component.setBounds(0, 0, (int)(getWidth() * weight + .5f), getHeight());
      }
      else if (ToolWindowAnchor.BOTTOM == anchor) {
        final int height = (int)(getHeight() * weight + .5f);
        component.setBounds(0, getHeight() - height, getWidth(), height);
      }
      else if (ToolWindowAnchor.RIGHT == anchor) {
        final int width = (int)(getWidth() * weight + .5f);
        component.setBounds(getWidth() - width, 0, width, getHeight());
      }
      else {
        LOG.error("unknown anchor " + anchor);
      }
    }
  }

  void setStripesOverlayed(boolean stripesOverlayed) {
    myStripesOverlayed = stripesOverlayed;
    updateToolStripesVisibility();
  }

  @Override
  public void dispose() {
  }
}
