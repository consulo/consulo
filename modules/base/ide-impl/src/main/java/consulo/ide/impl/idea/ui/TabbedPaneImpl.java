/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ui;

import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.JBTabbedPane;
import consulo.logging.Logger;
import consulo.ui.ex.PrevNextActionsDescriptor;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.TabbedPane;
import consulo.ui.ex.awt.TabbedPaneWrapper;
import jakarta.annotation.Nonnull;
import org.intellij.lang.annotations.JdkConstants;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;;

public class TabbedPaneImpl extends JBTabbedPane implements TabbedPane {
  private static final Logger LOG = Logger.getInstance(TabbedPaneImpl.class);

  private AnAction myNextTabAction = null;
  private AnAction myPreviousTabAction = null;
  public PrevNextActionsDescriptor myInstallKeyboardNavigation = null;

  public TabbedPaneImpl(@JdkConstants.TabPlacement int tabPlacement) {
    super(tabPlacement);
    setFocusable(false);
    setTabComponentInsets(JBUI.emptyInsets());
    addMouseListener(new MouseAdapter() {
      @Override
      public void mousePressed(final MouseEvent e) {
        _requestDefaultFocus();
      }
    });
  }

  @Override
  public void setKeyboardNavigation(PrevNextActionsDescriptor installKeyboardNavigation) {
    myInstallKeyboardNavigation = installKeyboardNavigation;
  }

  @Override
  public JComponent getComponent() {
    return this;
  }

  @Override
  public void addNotify() {
    super.addNotify();
    if (myInstallKeyboardNavigation != null) {
      installKeyboardNavigation(myInstallKeyboardNavigation);
    }
  }

  @Override
  public void removeNotify() {
    super.removeNotify();
    if (myInstallKeyboardNavigation != null) {
      uninstallKeyboardNavigation();
    }
  }

  @SuppressWarnings({"NonStaticInitializer"})
  private void installKeyboardNavigation(final PrevNextActionsDescriptor installKeyboardNavigation) {
    myNextTabAction = new AnAction() {
      {
        setEnabledInModalContext(true);
      }

      @RequiredUIAccess
      @Override
      public void actionPerformed(@Nonnull final AnActionEvent e) {
        int index = getSelectedIndex() + 1;
        if (index >= getTabCount()) {
          index = 0;
        }
        setSelectedIndex(index);
      }
    };
    final AnAction nextAction = ActionManager.getInstance().getAction(installKeyboardNavigation.getNextActionId());
    LOG.assertTrue(nextAction != null, "Cannot find action with specified id: " + installKeyboardNavigation.getNextActionId());
    myNextTabAction.registerCustomShortcutSet(nextAction.getShortcutSet(), this);

    myPreviousTabAction = new AnAction() {
      {
        setEnabledInModalContext(true);
      }

      @RequiredUIAccess
      @Override
      public void actionPerformed(@Nonnull final AnActionEvent e) {
        int index = getSelectedIndex() - 1;
        if (index < 0) {
          index = getTabCount() - 1;
        }
        setSelectedIndex(index);
      }
    };
    final AnAction prevAction = ActionManager.getInstance().getAction(installKeyboardNavigation.getPrevActionId());
    LOG.assertTrue(prevAction != null, "Cannot find action with specified id: " + installKeyboardNavigation.getPrevActionId());
    myPreviousTabAction.registerCustomShortcutSet(prevAction.getShortcutSet(), this);
  }

  private void uninstallKeyboardNavigation() {
    if (myNextTabAction != null) {
      myNextTabAction.unregisterCustomShortcutSet(this);
      myNextTabAction = null;
    }
    if (myPreviousTabAction != null) {
      myPreviousTabAction.unregisterCustomShortcutSet(this);
      myPreviousTabAction = null;
    }
  }

  @Override
  public final void scrollTabToVisible(final int index) {
    setSelectedIndex(index);
  }

  @Override
  public final void removeTabAt(final int index) {
    super.removeTabAt(index);
    //This event should be fired necessarily because when swing fires an event
    // page to be removed is still in the tabbed pane. There can be a situation when
    // event fired according to swing event contains invalid information about selected page.
    fireStateChanged();
  }

  private void _requestDefaultFocus() {
    final Component selectedComponent = getSelectedComponent();
    if (selectedComponent instanceof TabbedPaneWrapper.TabWrapper) {
      ((TabbedPaneWrapper.TabWrapper)selectedComponent).requestDefaultFocus();
    }
    else {
      super.requestDefaultFocus();
    }
  }

  @Override
  public boolean isDisposed() {
    return false;
  }
}
