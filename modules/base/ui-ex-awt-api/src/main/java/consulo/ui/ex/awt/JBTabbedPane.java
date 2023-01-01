/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.ui.ex.awt;

import consulo.ui.ex.awt.util.ScreenUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.image.Image;
import org.intellij.lang.annotations.JdkConstants;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;

/**
 * @author evgeny.zakrevsky
 */
public class JBTabbedPane extends JTabbedPane implements HierarchyListener {
  public static final String LABEL_FROM_TABBED_PANE = "JBTabbedPane.labelFromTabbedPane";
  private int previousSelectedIndex = -1;

  private Insets myTabComponentInsets = UIUtil.PANEL_SMALL_INSETS;

  public JBTabbedPane() {
  }

  public JBTabbedPane(@JdkConstants.TabPlacement int tabPlacement) {
    super(tabPlacement);
  }

  public JBTabbedPane(@JdkConstants.TabPlacement int tabPlacement, @JdkConstants.TabLayoutPolicy int tabLayoutPolicy) {
    super(tabPlacement, tabLayoutPolicy);
  }

  @Override
  public void setComponentAt(int index, Component component) {
    super.setComponentAt(index, component);
    component.addHierarchyListener(this);
    UIUtil.setNotOpaqueRecursively(component);
    setInsets(component);
    revalidate();
    repaint();
  }

  public void setIconAt(int index, Image icon) {
    setIconAt(index, TargetAWT.to(icon));
  }

  public void insertTab(String title, Image icon, Component component, String tip, int index) {
    insertTab(title, TargetAWT.to(icon), component, tip, index);
  }

  @Override
  public void insertTab(String title, Icon icon, Component component, String tip, int index) {
    super.insertTab(title, icon, component, tip, index);

    //set custom label for correct work spotlighting in settings
    JLabel label = new JLabel(title);
    label.setIcon(icon);
    label.setBorder(new EmptyBorder(1, 1, 1, 1));
    setTabComponentAt(index, label);
    label.putClientProperty(LABEL_FROM_TABBED_PANE, Boolean.TRUE);

    component.addHierarchyListener(this);
    UIUtil.setNotOpaqueRecursively(component);
    setInsets(component);

    revalidate();
    repaint();
  }

  @Override
  public void setSelectedIndex(int index) {
    previousSelectedIndex = getSelectedIndex();
    super.setSelectedIndex(index);
    revalidate();
    repaint();
  }

  private void setInsets(Component component) {
    if (component instanceof JComponent) {
      UIUtil.addInsets((JComponent)component, getInsetsForTabComponent());
    }
  }

  /**
   * @deprecated Use {@link JBTabbedPane#setTabComponentInsets(Insets)} instead of overriding
   */
  @Deprecated
  @Nonnull
  protected Insets getInsetsForTabComponent() {
    return myTabComponentInsets;
  }

  @Nullable
  public Insets getTabComponentInsets() {
    return myTabComponentInsets;
  }

  public void setTabComponentInsets(@Nullable Insets tabInsets) {
    myTabComponentInsets = tabInsets;
  }

  @Override
  public void hierarchyChanged(HierarchyEvent e) {
    UIUtil.setNotOpaqueRecursively(e.getComponent());
    repaint();
  }

  @Override
  public void removeNotify() {
    super.removeNotify();
    if (!ScreenUtil.isStandardAddRemoveNotify(this)) return;
    for (int i = 0; i < getTabCount(); i++) {
      getComponentAt(i).removeHierarchyListener(this);
    }
  }
}
