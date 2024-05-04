// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.desktop.awt.wm.impl;

import consulo.application.ApplicationManager;
import consulo.application.util.SystemInfo;
import consulo.desktop.awt.ui.IdeEventQueue;
import consulo.ide.impl.desktop.DesktopIdeFrameUtil;
import consulo.ide.impl.idea.ide.actions.ActivateToolWindowAction;
import consulo.ide.impl.idea.openapi.keymap.KeymapUtil;
import consulo.ide.impl.idea.util.ui.BaseButtonBehavior;
import consulo.ide.impl.idea.util.ui.TimedDeadzone;
import consulo.ide.impl.ui.impl.PopupChooserBuilder;
import consulo.ide.impl.wm.statusBar.BaseToolWindowsSwitcher;
import consulo.project.ui.internal.IdeFrameEx;
import consulo.project.ui.wm.StatusBar;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.KeyboardShortcut;
import consulo.ui.ex.awt.ColoredListCellRenderer;
import consulo.ui.ex.awt.JBList;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.util.Alarm;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Konstantin Bulenkov
 */
public class DesktopToolWindowsSwicher extends BaseToolWindowsSwitcher {
  private final Alarm myAlarm;

  public DesktopToolWindowsSwicher(StatusBar statusBar) {
    super(statusBar);

    myAlarm = new Alarm(this);

    JComponent awtLabel = (JComponent)TargetAWT.to(myLabel);

    new BaseButtonBehavior(awtLabel, TimedDeadzone.NULL) {
      @Override
      protected void execute(MouseEvent e) {
        performAction();
      }
    }.setActionTrigger(MouseEvent.MOUSE_PRESSED);

    IdeEventQueue.getInstance().addDispatcher(e -> {
      if (e instanceof MouseEvent) {
        MouseEvent mouseEvent = (MouseEvent)e;
        if (mouseEvent.getComponent() == null || !SwingUtilities.isDescendingFrom(mouseEvent.getComponent(),
                                                                                  SwingUtilities.getWindowAncestor(awtLabel))) {
          return false;
        }

        if (e.getID() == MouseEvent.MOUSE_MOVED && awtLabel.isShowing()) {
          Point p = mouseEvent.getLocationOnScreen();
          Point screen = awtLabel.getLocationOnScreen();
          if (new Rectangle(screen.x - 4, screen.y - 2, awtLabel.getWidth() + 4, awtLabel.getHeight() + 4).contains(p)) {
            mouseEntered();
            wasExited = false;
          }
          else {
            if (!wasExited) {
              wasExited = mouseExited(p);
            }
          }
        }
        else if (e.getID() == MouseEvent.MOUSE_EXITED) {
          //mouse exits WND
          mouseExited(mouseEvent.getLocationOnScreen());
        }
      }
      return false;
    }, this);
  }

  public boolean mouseExited(Point currentLocationOnScreen) {
    myAlarm.cancelAllRequests();
    if (popup != null && popup.isVisible()) {
      final Point screen = popup.getLocationOnScreen();
      final Rectangle popupScreenRect = new Rectangle(screen.x, screen.y, popup.getSize().width, popup.getSize().height);
      if (!popupScreenRect.contains(currentLocationOnScreen)) {
        myAlarm.cancelAllRequests();
        myAlarm.addRequest(() -> {
          if (popup != null && popup.isVisible()) {
            popup.cancel();
          }
        }, 300);
        return true;
      }
    }
    return false;
  }

  public void mouseEntered() {
    final boolean active = ApplicationManager.getApplication().isActive();
    if (!active) {
      return;
    }
    if (myAlarm.getActiveRequestCount() == 0) {
      myAlarm.addRequest(() -> {
        final IdeFrameEx frame = DesktopIdeFrameUtil.findIdeFrameExFromParent(TargetAWT.to(myLabel));
        if (frame == null) return;

        List<ToolWindow> toolWindows = new ArrayList<>();
        final ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(frame.getProject());
        for (String id : toolWindowManager.getToolWindowIds()) {
          final ToolWindow tw = toolWindowManager.getToolWindow(id);
          if (tw.isAvailable() && tw.isShowStripeButton()) {
            toolWindows.add(tw);
          }
        }
        toolWindows.sort((o1, o2) -> StringUtil.naturalCompare(o1.getDisplayName().getValue(), o2.getDisplayName().getValue()));

        final JBList<ToolWindow> list = new JBList<>(toolWindows);
        list.setCellRenderer(new ColoredListCellRenderer<>() {
          @Override
          @RequiredUIAccess
          protected void customizeCellRenderer(@Nonnull JList<? extends ToolWindow> list,
                                               ToolWindow value,
                                               int index,
                                               boolean selected,
                                               boolean hasFocus) {
            append(value.getDisplayName().getValue());
            String activateActionId = ActivateToolWindowAction.getActionIdForToolWindow(value.getId());
            KeyboardShortcut shortcut = ActionManager.getInstance().getKeyboardShortcut(activateActionId);
            if (shortcut != null) {
              append(" " + KeymapUtil.getShortcutText(shortcut), SimpleTextAttributes.GRAY_ATTRIBUTES);
            }
            setIcon(value.getIcon());
            setBorder(JBUI.Borders.empty(2, 10));
          }
        });

        final Dimension size = list.getPreferredSize();
        final JComponent c = (JComponent)TargetAWT.to(myLabel);
        final Insets padding = UIUtil.getListViewportPadding();
        final RelativePoint point =
          new RelativePoint(c, new Point(-4, -padding.top - padding.bottom - 4 - size.height + (SystemInfo.isMac ? 2 : 0)));

        if (popup != null && popup.isVisible()) {
          return;
        }

        list.setSelectedIndex(list.getItemsCount() - 1);
        PopupChooserBuilder<ToolWindow> builder = new PopupChooserBuilder<>(list);
        popup = builder.setAutoselectOnMouseMove(true).setRequestFocus(false).setItemChosenCallback((selectedValue) -> {
          if (popup != null) popup.closeOk(null);
          selectedValue.activate(null, true, true);
        }).createPopup();

        list.setVisibleRowCount(30); // override default of 15 set when createPopup() is called

        popup.show(point);
      }, 300);
    }
  }
}
