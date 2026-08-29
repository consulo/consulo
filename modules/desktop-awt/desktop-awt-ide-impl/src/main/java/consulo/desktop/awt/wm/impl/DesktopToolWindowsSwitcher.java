// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.desktop.awt.wm.impl;

import consulo.application.ApplicationManager;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.desktop.awt.ui.IdeEventQueue;
import consulo.ui.ex.awt.internal.DesktopIdeFrameUtil;
import consulo.ide.impl.wm.statusBar.BaseToolWindowsSwitcher;
import consulo.project.ui.impl.internal.wm.action.ActivateToolWindowAction;
import consulo.project.ui.internal.IdeFrameEx;
import consulo.project.ui.wm.StatusBar;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.util.Alarm;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListPopup;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Konstantin Bulenkov
 */
public class DesktopToolWindowsSwitcher extends BaseToolWindowsSwitcher {
    private class ToolWindowAction extends DumbAwareAction {
        private final ToolWindow myToolWindow;

        public ToolWindowAction(ToolWindow toolWindow) {
            super(toolWindow.getDisplayName(), toolWindow.getDisplayName(), toolWindow.getIcon());
            myToolWindow = toolWindow;
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(AnActionEvent e) {
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
    public DesktopToolWindowsSwitcher(StatusBar statusBar) {
        super(statusBar);

        myAlarm = new Alarm(this);

        JComponent awtButton = (JComponent) TargetAWT.to(myButton);

        IdeEventQueue.getInstance().addDispatcher(
            e -> {
                if (e instanceof MouseEvent) {
                    MouseEvent mouseEvent = (MouseEvent) e;
                    if (mouseEvent.getComponent() == null
                        || !SwingUtilities.isDescendingFrom(mouseEvent.getComponent(), SwingUtilities.getWindowAncestor(awtButton))) {
                        return false;
                    }

                    if (e.getID() == MouseEvent.MOUSE_MOVED && awtButton.isShowing()) {
                        Point p = mouseEvent.getLocationOnScreen();
                        if (buttonRectOnScreen().contains(p)) {
                            mouseEntered();
                            wasExited = false;
                        }
                        else {
                            wasExited = mouseExited(p);
                        }
                    }
                    else if (e.getID() == MouseEvent.MOUSE_EXITED) {
                        mouseExited(mouseEvent.getLocationOnScreen());
                    }
                }
                return false;
            },
            this
        );
    }

    private static @Nullable Point pointerOnScreen() {
        PointerInfo info = MouseInfo.getPointerInfo();
        return info == null ? null : info.getLocation();
    }

    private Rectangle buttonRectOnScreen() {
        JComponent awtButton = (JComponent) TargetAWT.to(myButton);
        if (!awtButton.isShowing()) {
            return new Rectangle();
        }

        Point location = awtButton.getLocationOnScreen();
        return new Rectangle(location.x - 4, location.y - 2, awtButton.getWidth() + 4, awtButton.getHeight() + 4);
    }

    private boolean isOverSwitcherOrPopup(@Nullable Point screenPoint) {
        if (screenPoint == null) {
            return false;
        }

        if (buttonRectOnScreen().contains(screenPoint)) {
            return true;
        }

        if (popup == null || !popup.isVisible()) {
            return false;
        }

        Point location = popup.getLocationOnScreen();
        Dimension size = popup.getSize();
        return new Rectangle(location.x, location.y, size.width, size.height).contains(screenPoint);
    }

    public boolean mouseExited(Point currentLocationOnScreen) {
        myAlarm.cancelAllRequests();

        if (popup != null && popup.isVisible() && !isOverSwitcherOrPopup(currentLocationOnScreen)) {
            myAlarm.addRequest(() -> {
                if (popup != null && popup.isVisible() && !isOverSwitcherOrPopup(pointerOnScreen())) {
                    popup.cancel();
                }
            }, 300);
            return true;
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
                IdeFrameEx frame = DesktopIdeFrameUtil.findIdeFrameExFromParent(TargetAWT.to(myButton));
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

                DataContext context = DataManager.getInstance().getDataContext(myButton);
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

                this.popup.show(new RelativePoint(TargetAWT.to(myButton), new Point(36, 36)));
            }, 300);
        }
    }
}
