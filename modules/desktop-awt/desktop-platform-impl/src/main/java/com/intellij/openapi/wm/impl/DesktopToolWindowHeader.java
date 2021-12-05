/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.impl.ActionManagerImpl;
import com.intellij.openapi.actionSystem.impl.MenuItemPresentationFactory;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.impl.content.DesktopToolWindowContentUi;
import com.intellij.ui.DoubleClickListener;
import com.intellij.ui.PopupHandler;
import com.intellij.ui.UIBundle;
import com.intellij.ui.components.panels.HorizontalLayout;
import com.intellij.ui.components.panels.NonOpaquePanel;
import com.intellij.ui.tabs.TabsUtil;
import com.intellij.util.NotNullProducer;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import consulo.desktop.util.awt.MorphColor;
import consulo.disposer.Disposable;
import consulo.ui.decorator.SwingUIDecorator;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.wm.impl.ToolWindowManagerBase;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * @author pegov
 */
public abstract class DesktopToolWindowHeader extends JPanel implements Disposable {
  private class GearAction extends DumbAwareAction {
    private NotNullProducer<ActionGroup> myGearProducer;

    public GearAction(NotNullProducer<ActionGroup> gearProducer) {
      super("Show options", null, AllIcons.General.GearPlain);
      myGearProducer = gearProducer;
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(@Nonnull AnActionEvent e) {
      final InputEvent inputEvent = e.getInputEvent();
      final ActionPopupMenu popupMenu =
              ((ActionManagerImpl)ActionManager.getInstance()).createActionPopupMenu(DesktopToolWindowContentUi.POPUP_PLACE, myGearProducer.produce(), new MenuItemPresentationFactory(true));

      int x = 0;
      int y = 0;
      if (inputEvent instanceof MouseEvent) {
        x = ((MouseEvent)inputEvent).getX();
        y = ((MouseEvent)inputEvent).getY();
      }

      popupMenu.getComponent().show(inputEvent.getComponent(), x, y);
    }
  }

  private class HideAction extends DumbAwareAction {

    @RequiredUIAccess
    @Override
    public void actionPerformed(@Nonnull AnActionEvent e) {
      hideToolWindow();
    }

    @RequiredUIAccess
    @Override
    public final void update(@Nonnull final AnActionEvent event) {
      Presentation presentation = event.getPresentation();

      presentation.setText(UIBundle.message("tool.window.hide.action.name"));
      boolean visible = myToolWindow.isVisible();
      presentation.setEnabled(visible);
      if(visible) {
        presentation.setIcon(getHideIcon(myToolWindow));
      }
    }

    private Image getHideIcon(ToolWindow toolWindow) {
      return AllIcons.General.HideToolWindow;
    }
  }

  private final ToolWindow myToolWindow;

  private final DefaultActionGroup myActionGroup = new DefaultActionGroup();
  private final DefaultActionGroup myActionGroupWest = new DefaultActionGroup();

  private final ActionToolbar myToolbar;
  private ActionToolbar myToolbarWest;
  private final JPanel myWestPanel;

  public DesktopToolWindowHeader(final DesktopToolWindowImpl toolWindow, @Nonnull final NotNullProducer<ActionGroup> gearProducer) {
    super(new BorderLayout());

    myToolWindow = toolWindow;

    myWestPanel = new NonOpaquePanel(new HorizontalLayout(0, SwingConstants.CENTER));

    add(myWestPanel, BorderLayout.CENTER);

    myWestPanel.add(wrapAndFillVertical(toolWindow.getContentUI().getTabComponent()));

    DesktopToolWindowContentUi.initMouseListeners(myWestPanel, toolWindow.getContentUI(), true);

    myToolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.TOOLWINDOW_TITLE, new DefaultActionGroup(myActionGroup, new GearAction(gearProducer), new HideAction()), true);
    myToolbar.setTargetComponent(this);
    myToolbar.setLayoutPolicy(ActionToolbar.NOWRAP_LAYOUT_POLICY);
    myToolbar.setReservePlaceAutoPopupIcon(false);

    JComponent component = myToolbar.getComponent();
    component.setBorder(JBUI.Borders.empty());
    component.setOpaque(false);

    add(wrapAndFillVertical(component), BorderLayout.EAST);

    myWestPanel.addMouseListener(new PopupHandler() {
      @Override
      public void invokePopup(final Component comp, final int x, final int y) {
        toolWindow.getContentUI().showContextMenu(comp, x, y, toolWindow.getPopupGroup(), toolWindow.getContentManager().getSelectedContent());
      }
    });

    myWestPanel.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        toolWindow.fireActivated();
      }
    });

    addMouseListener(new MouseAdapter() {
      @Override
      public void mouseReleased(final MouseEvent e) {
        if (!e.isPopupTrigger()) {
          if (UIUtil.isCloseClick(e, MouseEvent.MOUSE_RELEASED)) {
            if (e.isAltDown()) {
              toolWindow.fireHidden();
            }
            else {
              toolWindow.fireHiddenSide();
            }
          }
          else {
            toolWindow.fireActivated();
          }
        }
      }
    });

    setBackground(MorphColor.ofWithoutCache(() -> myToolWindow.isActive() ? SwingUIDecorator.get(SwingUIDecorator::getSidebarColor) : UIUtil.getPanelBackground()));

    setBorder(JBUI.Borders.customLine(UIUtil.getBorderColor(), TabsUtil.TABS_BORDER, 0, TabsUtil.TABS_BORDER, 0));

    new DoubleClickListener() {
      @Override
      protected boolean onDoubleClick(MouseEvent event) {
        ToolWindowManagerBase mgr = toolWindow.getToolWindowManager();
        mgr.setMaximized(myToolWindow, !mgr.isMaximized(myToolWindow));
        return true;
      }
    }.installOn(myWestPanel);
  }

  @Nonnull
  public static JPanel wrapAndFillVertical(JComponent owner) {
    JPanel panel = new JPanel(new VerticalFlowLayout(VerticalFlowLayout.MIDDLE, 0, JBUI.scale(5), false, true));
    panel.add(owner);
    panel.setOpaque(false);
    return panel;
  }

  @Override
  public void dispose() {
    removeAll();
  }

  private void initWestToolBar(JPanel westPanel) {
    myToolbarWest = ActionManager.getInstance().createActionToolbar(ActionPlaces.TOOLWINDOW_TITLE, new DefaultActionGroup(myActionGroupWest), true);

    myToolbarWest.setTargetComponent(this);
    myToolbarWest.setLayoutPolicy(ActionToolbar.NOWRAP_LAYOUT_POLICY);
    myToolbarWest.setReservePlaceAutoPopupIcon(false);

    JComponent component = myToolbarWest.getComponent();
    component.setBorder(JBUI.Borders.empty());
    component.setOpaque(false);

    westPanel.add(wrapAndFillVertical(component));
  }

  public void setTabActions(@Nonnull AnAction[] actions) {
    if (myToolbarWest == null) {
      initWestToolBar(myWestPanel);
    }

    myActionGroupWest.removeAll();
    myActionGroupWest.addSeparator();
    myActionGroupWest.addAll(actions);

    if (myToolbarWest != null) {
      myToolbarWest.updateActionsImmediately();
    }
  }

  public void setAdditionalTitleActions(@Nonnull AnAction[] actions) {
    myActionGroup.removeAll();
    myActionGroup.addAll(actions);
    if (actions.length > 0) {
      myActionGroup.addSeparator();
    }
    if (myToolbar != null) {
      myToolbar.updateActionsImmediately();
    }
  }

  protected boolean isActive() {
    return myToolWindow.isActive();
  }

  protected abstract void hideToolWindow();

  @Override
  public Dimension getPreferredSize() {
    Dimension size = super.getPreferredSize();
    return new Dimension(size.width, TabsUtil.getRealTabsHeight());
  }
}
