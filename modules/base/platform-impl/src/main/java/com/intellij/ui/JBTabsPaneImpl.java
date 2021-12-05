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
package com.intellij.ui;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.ui.tabs.*;
import com.intellij.ui.tabs.impl.JBEditorTabs;
import com.intellij.ui.tabs.impl.JBTabsImpl;
import com.intellij.util.ui.UIUtil;
import consulo.disposer.Disposable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.MouseListener;
import java.util.concurrent.CopyOnWriteArraySet;

public class JBTabsPaneImpl implements TabbedPane, SwingConstants {
  private final JBTabsImpl myTabs;
  private final CopyOnWriteArraySet<ChangeListener> myListeners = new CopyOnWriteArraySet<ChangeListener>();

  public JBTabsPaneImpl(@Nullable Project project, int tabPlacement, @Nonnull Disposable parent) {
    myTabs = new JBEditorTabs(project, ActionManager.getInstance(), project == null ? null : IdeFocusManager.getInstance(project), parent) {
      @Override
      public boolean isAlphabeticalMode() {
        return false;
      }

      @Override
      public boolean supportsCompression() {
        return false;
      }
    };
    myTabs.setFirstTabOffset(10);

    myTabs.addListener(new TabsListener.Adapter() {
      @Override
      public void selectionChanged(TabInfo oldSelection, TabInfo newSelection) {
        fireChanged(new ChangeEvent(myTabs));
      }
    });

    setTabPlacement(tabPlacement);
  }

  private void fireChanged(ChangeEvent event) {
    for (ChangeListener each : myListeners) {
      each.stateChanged(event);
    }
  }

  @Override
  public JComponent getComponent() {
    return myTabs.getComponent();
  }

  @Override
  public void putClientProperty(Object key, Object value) {
    myTabs.getComponent().putClientProperty(key, value);
  }

  @Override
  public void setKeyboardNavigation(PrevNextActionsDescriptor installKeyboardNavigation) {
    myTabs.setNavigationActionBinding(installKeyboardNavigation.getPrevActionId(), installKeyboardNavigation.getNextActionId());
  }

  @Override
  public void addChangeListener(ChangeListener listener) {
    myListeners.add(listener);
  }

  @Override
  public int getTabCount() {
    return myTabs.getTabCount();
  }

  @Override
  public void insertTab(String title, consulo.ui.image.Image icon, Component c, String tip, int index) {
    assert c instanceof JComponent;
    myTabs.addTab(new TabInfo((JComponent)c).setText(title).setTooltipText(tip).setIcon(icon), index);
  }

  @Override
  public void setTabPlacement(int tabPlacement) {
    final JBTabsPresentation presentation = myTabs.getPresentation();
    switch (tabPlacement) {
      case TOP:
        presentation.setTabsPosition(JBTabsPosition.top);
        break;
      case BOTTOM:
        presentation.setTabsPosition(JBTabsPosition.bottom);
        break;
      case LEFT:
        presentation.setTabsPosition(JBTabsPosition.left);
        break;
      case RIGHT:
        presentation.setTabsPosition(JBTabsPosition.right);
        break;
      default:
        throw new IllegalArgumentException("Invalid tab placement code=" + tabPlacement);
    }
  }

  @Override
  public void addMouseListener(MouseListener listener) {
    myTabs.getComponent().addMouseListener(listener);
  }

  @Override
  public int getSelectedIndex() {
    return myTabs.getIndexOf(myTabs.getSelectedInfo());
  }

  @Override
  public Component getSelectedComponent() {
    final TabInfo selected = myTabs.getSelectedInfo();
    return selected != null ? selected.getComponent() : null;
  }

  @Override
  public void setSelectedIndex(int index) {
    myTabs.select(getTabAt(index), false);
  }

  @Override
  public void removeTabAt(int index) {
    myTabs.removeTab(getTabAt(index));
  }

  private TabInfo getTabAt(int index) {
    checkIndex(index);
    return myTabs.getTabAt(index);
  }

  private void checkIndex(int index) {
    if (index < 0 || index >= getTabCount()) {
      throw new ArrayIndexOutOfBoundsException("tabCount=" + getTabCount() + " index=" + index);
    }
  }

  @Override
  public void revalidate() {
    myTabs.getComponent().revalidate();
  }

  @Override
  public Color getForegroundAt(int index) {
    return getTabAt(index).getDefaultForeground();
  }

  @Override
  public void setForegroundAt(int index, Color color) {
    getTabAt(index).setDefaultForeground(color);
  }

  @Override
  public Component getComponentAt(int i) {
    return getTabAt(i).getComponent();
  }

  @Override
  public void setTitleAt(int index, String title) {
    getTabAt(index).setText(title);
  }

  @Override
  public void setToolTipTextAt(int index, String toolTipText) {
    getTabAt(index).setTooltipText(toolTipText);
  }

  @Override
  public void setComponentAt(int index, Component c) {
    getTabAt(index).setComponent(c);
  }

  @Override
  public void setIconAt(int index, consulo.ui.image.Image icon) {
    getTabAt(index).setIcon(icon);
  }

  @Override
  public void setEnabledAt(int index, boolean enabled) {
    getTabAt(index).setEnabled(enabled);
  }

  @Override
  public int getTabLayoutPolicy() {
    return myTabs.getPresentation().isSingleRow() ? JTabbedPane.SCROLL_TAB_LAYOUT : JTabbedPane.WRAP_TAB_LAYOUT;
  }

  @Override
  public void setTabLayoutPolicy(int policy) {
    switch (policy) {
      case JTabbedPane.SCROLL_TAB_LAYOUT:
        myTabs.getPresentation().setSingleRow(true);
        break;
      case JTabbedPane.WRAP_TAB_LAYOUT:
        myTabs.getPresentation().setSingleRow(false);
        break;
      default:
        throw new IllegalArgumentException("Unsupported tab layout policy: " + policy);
    }
  }

  @Override
  public void scrollTabToVisible(int index) {
  }

  @Override
  public String getTitleAt(int i) {
    return getTabAt(i).getText();
  }

  @Override
  public void removeAll() {
    myTabs.removeAllTabs();
  }

  @Override
  public void updateUI() {
    myTabs.getComponent().updateUI();
  }

  @Override
  public void removeChangeListener(ChangeListener listener) {
    myListeners.remove(listener);
  }

  public JBTabs getTabs() {
    return myTabs;
  }

  @Override
  public boolean isDisposed() {
    return myTabs.isDisposed();
  }
}
