// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.desktop.awt.wm.impl;

import consulo.application.ApplicationManager;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.desktop.awt.ui.IdeEventQueue;
import consulo.ide.impl.desktop.DesktopIdeFrameUtil;
import consulo.ide.impl.idea.util.ui.BaseButtonBehavior;
import consulo.ide.impl.wm.statusBar.BaseToolWindowsSwitcher;
import consulo.project.ui.impl.internal.wm.action.ActivateToolWindowAction;
import consulo.project.ui.internal.IdeFrameEx;
import consulo.project.ui.wm.StatusBar;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.border.BorderPosition;
import consulo.ui.border.BorderStyle;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.util.Alarm;
import consulo.ui.ex.awt.util.TimedDeadzone;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListPopup;
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
    private class ToolWindowAction extends DumbAwareAction {
        @Nonnull
        private final ToolWindow myToolWindow;

        public ToolWindowAction(ToolWindow toolWindow) {
            super(toolWindow.getDisplayName(), toolWindow.getDisplayName(), toolWindow.getIcon());
            myToolWindow = toolWindow;
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(@Nonnull AnActionEvent e) {
            if (popup != null) {
                popup.closeOk(null);
            }

            if (myToolWindow.isActive()) {
                myToolWindow.hide();
            } else {
                activate();
            }
        }

        public void activate() {
            myToolWindow.activate(null, true, true);
        }
    }

    private final Alarm myAlarm;

    @RequiredUIAccess
    public DesktopToolWindowsSwicher(StatusBar statusBar) {
        super(statusBar);

        myAlarm = new Alarm(this);

        myLabel.addBorder(BorderPosition.RIGHT, BorderStyle.EMPTY, 8);

        JComponent awtLabel = (JComponent) TargetAWT.to(myLabel);

        new BaseButtonBehavior(awtLabel, TimedDeadzone.NULL) {
            @Override
            protected void execute(MouseEvent e) {
                performAction();
            }
        }.setActionTrigger(MouseEvent.MOUSE_PRESSED);

        IdeEventQueue.getInstance().addDispatcher(e -> {
            if (e instanceof MouseEvent) {
                MouseEvent mouseEvent = (MouseEvent) e;
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
            Point screen = popup.getLocationOnScreen();
            Rectangle popupScreenRect = new Rectangle(screen.x, screen.y, popup.getSize().width, popup.getSize().height);
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
        boolean active = ApplicationManager.getApplication().isActive();
        if (!active) {
            return;
        }
        if (myAlarm.getActiveRequestCount() == 0) {
            myAlarm.addRequest(() -> {
                IdeFrameEx frame = DesktopIdeFrameUtil.findIdeFrameExFromParent(TargetAWT.to(myLabel));
                if (frame == null) {
                    return;
                }

                List<ToolWindow> toolWindows = new ArrayList<>();
                ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(frame.getProject());
                for (String id : toolWindowManager.getToolWindowIds()) {
                    ToolWindow tw = toolWindowManager.getToolWindow(id);
                    if (tw.isAvailable() && tw.isShowStripeButton()) {
                        toolWindows.add(tw);
                    }
                }
                toolWindows.sort((o1, o2) -> StringUtil.naturalCompare(o1.getDisplayName().getValue(), o2.getDisplayName().getValue()));

                ActionManager actionManager = ActionManager.getInstance();

                ActionGroup.Builder actionBuilder = ActionGroup.newImmutableBuilder();
                for (ToolWindow window : toolWindows) {
                    ToolWindowAction action = new ToolWindowAction(window);

                    String activateActionId = ActivateToolWindowAction.getActionIdForToolWindow(window.getId());
                    KeyboardShortcut shortcut = actionManager.getKeyboardShortcut(activateActionId);
                    if (shortcut != null) {
                        action.setShortcutSet(new CustomShortcutSet(shortcut));
                    }

                    actionBuilder.add(action);
                }

                DataContext context = DataManager.getInstance().getDataContext(myLabel);
                ListPopup popup = JBPopupFactory.getInstance().createActionGroupPopup(
                    null,
                    actionBuilder.build(),
                    context,
                    false,
                    false,
                    false,
                    null,
                    Integer.MAX_VALUE,
                    null,
                    false
                );

                if (this.popup != null && this.popup.isVisible()) {
                    return;
                }

                popup.pack(true, true);

                this.popup = popup;

                this.popup.show(new RelativePoint(TargetAWT.to(myLabel), new Point(0, 0)));
            }, 300);
        }
    }
}
