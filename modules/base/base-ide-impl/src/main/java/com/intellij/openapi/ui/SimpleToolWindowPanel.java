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
package com.intellij.openapi.ui;

import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.ui.components.JBPanelWithEmptyText;
import com.intellij.ui.components.panels.Wrapper;
import com.intellij.ui.switcher.QuickActionProvider;
import com.intellij.util.containers.JBIterable;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import consulo.util.dataholder.Key;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ContainerAdapter;
import java.awt.event.ContainerEvent;
import java.util.Collections;
import java.util.List;

public class SimpleToolWindowPanel extends JBPanelWithEmptyText implements QuickActionProvider, DataProvider {

  private JComponent myToolbar;

  private boolean myBorderless;
  protected boolean myVertical;
  private boolean myProvideQuickActions;

  public SimpleToolWindowPanel(boolean vertical) {
    this(vertical, false);
  }

  public SimpleToolWindowPanel(boolean vertical, boolean borderless) {
    super(new BorderLayout(JBUI.scale(vertical ? 0 : 1), JBUI.scale(vertical ? 0 : 1)));
    myBorderless = borderless;
    myVertical = vertical;
    setProvideQuickActions(true);

    addContainerListener(new ContainerAdapter() {
      @Override
      public void componentAdded(ContainerEvent e) {
        Component child = e.getChild();

        if (child instanceof Container) {
          ((Container)child).addContainerListener(this);
        }
        if (myBorderless) {
          UIUtil.removeScrollBorder(SimpleToolWindowPanel.this);
        }
      }

      @Override
      public void componentRemoved(ContainerEvent e) {
        Component child = e.getChild();
        
        if (child instanceof Container) {
          ((Container)child).removeContainerListener(this);
        }
      }
    });
  }

  public void setToolbar(@Nullable JComponent c) {
    if (c == null) {
      remove(myToolbar);
    }


    if (c != null) {
      Wrapper wrapper = new Wrapper(c);
      myToolbar = wrapper;

      if (myVertical) {
        wrapper.setBorder(JBUI.Borders.customLine(UIUtil.getBorderColor(), 0, 0, 1, 0));
        add(wrapper, BorderLayout.NORTH);
      } else {
        wrapper.setBorder(JBUI.Borders.customLine(UIUtil.getBorderColor(), 0, 0, 0, 1));
        add(wrapper, BorderLayout.WEST);
      }
    } else {
      myToolbar = null;
    }

    revalidate();
    repaint();
  }

  @Override
  @Nullable
  public Object getData(@Nonnull Key<?> dataId) {
    return QuickActionProvider.KEY == dataId && myProvideQuickActions ? this : null;
  }

  public SimpleToolWindowPanel setProvideQuickActions(boolean provide) {
    myProvideQuickActions = provide;
    return this;
  }

  @Override
  public List<AnAction> getActions(boolean originalProvider) {
    JBIterable<ActionToolbar> toolbars = UIUtil.uiTraverser(myToolbar).traverse().filter(ActionToolbar.class);
    if (toolbars.size() == 0) return Collections.emptyList();
    return toolbars.flatten(ActionToolbar::getActions).toList();
  }

  @Override
  public JComponent getComponent() {
    return this;
  }

  @Override
  public boolean isCycleRoot() {
    return false;
  }

  public void setContent(JComponent c) {
    add(c, BorderLayout.CENTER);

    if (myBorderless) {
      UIUtil.removeScrollBorder(c);
    }

    revalidate();
    repaint();
  }
}