/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ide.actions;

import consulo.application.dumb.DumbAware;
import consulo.application.util.registry.Registry;
import consulo.ide.impl.idea.openapi.ui.ShadowAction;
import consulo.ide.impl.idea.openapi.wm.ToolWindowScrollable;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.project.event.ProjectManagerAdapter;
import consulo.project.ui.wm.IdeFrameUtil;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.internal.ToolWindowEx;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.ui.ex.toolWindow.ToolWindowAnchor;
import consulo.ui.ex.toolWindow.ToolWindowType;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;

public abstract class ResizeToolWindowAction extends AnAction implements DumbAware {
    private ToolWindow myLastWindow;
    private ToolWindowManager myLastManager;

    protected JLabel myScrollHelper;

    private ToolWindow myToolWindow;

    private boolean myListenerInstalled;

    protected ResizeToolWindowAction() {
    }

    protected ResizeToolWindowAction(@Nonnull LocalizeValue text, @Nonnull LocalizeValue description) {
        super(text, description);
    }

    protected ResizeToolWindowAction(ToolWindow toolWindow, String originalAction, JComponent c) {
        myToolWindow = toolWindow;
        new ShadowAction(this, ActionManager.getInstance().getAction(originalAction), c);
    }

    @Override
    @RequiredUIAccess
    public final void update(AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        if (project == null) {
            setDisabled(e);
            return;
        }

        if (!myListenerInstalled) {
            myListenerInstalled = true;
            ProjectManager.getInstance().addProjectManagerListener(new ProjectManagerAdapter() {
                @Override
                public void projectClosed(@Nonnull Project project, @Nonnull UIAccess uiAccess) {
                    setDisabled(null);
                }
            });
        }

        Component owner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        if (owner == null) {
            setDisabled(e);
            return;
        }

        Window windowAncestor = SwingUtilities.getWindowAncestor(owner);
        consulo.ui.Window uiWindow = TargetAWT.from(windowAncestor);
        if (!IdeFrameUtil.isRootIdeFrameWindow(uiWindow)) {
            setDisabled(e);
            return;
        }

        ToolWindowManager mgr = ToolWindowManager.getInstance(project);

        ToolWindow window = myToolWindow;

        if (window != null || mgr.getActiveToolWindowId() != null) {
            if (window == null) {
                window = mgr.getToolWindow(mgr.getActiveToolWindowId());
            }

            if (window == null || !window.isAvailable() || !window.isVisible() || window.getType() == ToolWindowType.FLOATING || !window.isActive()) {
                setDisabled(e);
                return;
            }

            update(e, window, mgr);
            if (e.getPresentation().isEnabled()) {
                myLastWindow = window;
                myLastManager = mgr;
            }
            else {
                setDisabled(e);
            }
        }
        else {
            setDisabled(e);
        }
    }

    private void setDisabled(@Nullable AnActionEvent e) {
        if (e != null) {
            e.getPresentation().setEnabled(false);
        }

        myLastWindow = null;
        myLastManager = null;
        myToolWindow = null;
    }

    protected abstract void update(AnActionEvent event, ToolWindow window, ToolWindowManager mgr);

    @Override
    @RequiredUIAccess
    public final void actionPerformed(@Nonnull AnActionEvent e) {
        actionPerformed(e, myLastWindow, myLastManager);
    }

    @Nullable
    private ToolWindowScrollable getScrollable(ToolWindow wnd, boolean isHorizontalStretchingOffered) {
        KeyboardFocusManager mgr = KeyboardFocusManager.getCurrentKeyboardFocusManager();

        Component eachComponent = mgr.getFocusOwner();
        ToolWindowScrollable scrollable = null;
        while (eachComponent != null) {
            if (!SwingUtilities.isDescendingFrom(eachComponent, wnd.getComponent())) {
                break;
            }

            if (eachComponent instanceof ToolWindowScrollable eachScrollable) {
                if (isHorizontalStretchingOffered) {
                    if (eachScrollable.isHorizontalScrollingNeeded()) {
                        scrollable = eachScrollable;
                        break;
                    }
                }
                else {
                    if (eachScrollable.isVerticalScrollingNeeded()) {
                        scrollable = eachScrollable;
                        break;
                    }
                }
            }

            eachComponent = eachComponent.getParent();
        }

        if (scrollable == null) {
            scrollable = new DefaultToolWindowScrollable();
        }

        if (isHorizontalStretchingOffered && scrollable.isHorizontalScrollingNeeded()) {
            return scrollable;
        }
        if (!isHorizontalStretchingOffered && scrollable.isVerticalScrollingNeeded()) {
            return scrollable;
        }

        return null;
    }

    protected abstract void actionPerformed(AnActionEvent e, ToolWindow wnd, ToolWindowManager mgr);

    protected void stretch(ToolWindow wnd, boolean isHorizontalStretching, boolean isIncrementAction) {
        ToolWindowScrollable scrollable = getScrollable(wnd, isHorizontalStretching);
        if (scrollable == null) {
            return;
        }

        ToolWindowAnchor anchor = wnd.getAnchor();
        if (isHorizontalStretching && !anchor.isHorizontal()) {
            incWidth(wnd, scrollable.getNextHorizontalScroll(), (anchor == ToolWindowAnchor.LEFT) == isIncrementAction);
        }
        else if (!isHorizontalStretching && anchor.isHorizontal()) {
            incHeight(wnd, scrollable.getNextVerticalScroll(), (anchor == ToolWindowAnchor.TOP) != isIncrementAction);
        }
    }

    private static void incWidth(ToolWindow wnd, int value, boolean isPositive) {
        ((ToolWindowEx)wnd).stretchWidth(isPositive ? value : -value);
    }

    private static void incHeight(ToolWindow wnd, int value, boolean isPositive) {
        ((ToolWindowEx)wnd).stretchHeight(isPositive ? value : -value);
    }

    private class DefaultToolWindowScrollable implements ToolWindowScrollable {
        @Override
        public boolean isHorizontalScrollingNeeded() {
            return true;
        }

        @Override
        public int getNextHorizontalScroll() {
            return getReferenceSize().width * Registry.intValue("ide.windowSystem.hScrollChars");
        }

        @Override
        public boolean isVerticalScrollingNeeded() {
            return true;
        }

        @Override
        public int getNextVerticalScroll() {
            return getReferenceSize().height * Registry.intValue("ide.windowSystem.vScrollChars");
        }
    }

    private Dimension getReferenceSize() {
        if (myScrollHelper == null) {
            if (SwingUtilities.isEventDispatchThread()) {
                myScrollHelper = new JLabel("W");
            }
            else {
                return new Dimension(1, 1);
            }
        }

        return myScrollHelper.getPreferredSize();
    }
}
