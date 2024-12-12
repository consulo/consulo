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
package consulo.desktop.awt.wm.impl;

import consulo.desktop.awt.wm.impl.content.DesktopToolWindowContentUi;
import consulo.disposer.Disposable;
import consulo.ide.impl.idea.openapi.actionSystem.impl.ActionManagerImpl;
import consulo.ide.impl.idea.openapi.actionSystem.impl.MenuItemPresentationFactory;
import consulo.ide.impl.idea.ui.tabs.TabsUtil;
import consulo.ide.impl.idea.util.NotNullProducer;
import consulo.ide.impl.ui.ToolwindowPaintUtil;
import consulo.ide.impl.wm.impl.ToolWindowManagerBase;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.event.DoubleClickListener;
import consulo.ui.ex.localize.UILocalize;
import consulo.ui.ex.toolWindow.ToolWindow;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import javax.swing.border.Border;
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
            super("Options", null, PlatformIconGroup.actionsMorevertical());
            myGearProducer = gearProducer;
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(@Nonnull AnActionEvent e) {
            final InputEvent inputEvent = e.getInputEvent();
            final ActionPopupMenu popupMenu =
                ((ActionManagerImpl) ActionManager.getInstance()).createActionPopupMenu(DesktopToolWindowContentUi.POPUP_PLACE,
                    myGearProducer.produce(),
                    new MenuItemPresentationFactory(true));

            int x = 0;
            int y = 0;
            if (inputEvent instanceof MouseEvent) {
                x = ((MouseEvent) inputEvent).getX();
                y = ((MouseEvent) inputEvent).getY();
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

            presentation.setTextValue(UILocalize.toolWindowHideActionName());
            boolean visible = myToolWindow.isVisible();
            presentation.setEnabled(visible);
            if (visible) {
                presentation.setIcon(PlatformIconGroup.generalHidetoolwindow());
            }
        }
    }

    private final ToolWindow myToolWindow;

    private final DefaultActionGroup myActionGroup = new DefaultActionGroup();
    private final DefaultActionGroup myActionGroupWest = new DefaultActionGroup();

    private final ActionToolbar myToolbar;
    private ActionToolbar myToolbarWest;
    private final JPanel myWestPanel;

    @RequiredUIAccess
    public DesktopToolWindowHeader(final DesktopToolWindowImpl toolWindow, @Nonnull final NotNullProducer<ActionGroup> gearProducer) {
        super(new BorderLayout());

        myToolWindow = toolWindow;

        myWestPanel = new NonOpaquePanel(new HorizontalLayout(0, SwingConstants.CENTER));

        add(myWestPanel, BorderLayout.CENTER);

        myWestPanel.add(wrapAndFillVertical(toolWindow.getContentUI().getTabComponent()));

        DesktopToolWindowContentUi.initMouseListeners(myWestPanel, toolWindow.getContentUI(), true);

        myToolbar = ActionManager.getInstance()
            .createActionToolbar(ActionPlaces.TOOLWINDOW_TITLE,
                ActionGroup.newImmutableBuilder()
                    .addAll(myActionGroup, new GearAction(gearProducer), new HideAction())
                    .build(),
                true);
        myToolbar.setLayoutPolicy(ActionToolbar.NOWRAP_LAYOUT_POLICY);
        myToolbar.setReservePlaceAutoPopupIcon(false);

        JComponent component = myToolbar.getComponent();
        component.setBorder(JBUI.Borders.empty());
        component.setOpaque(false);

        JPanel rightPanel = wrapAndFillVertical(component);
        rightPanel.setBorder(JBUI.Borders.empty(0, 3, 0, 3));

        add(rightPanel, BorderLayout.EAST);

        myWestPanel.addMouseListener(new PopupHandler() {
            @Override
            public void invokePopup(final Component comp, final int x, final int y) {
                toolWindow.getContentUI()
                    .showContextMenu(comp, x, y, toolWindow.getPopupGroup(), toolWindow.getContentManager().getSelectedContent());
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

        boolean fillAsPanel = UIManager.getBoolean(toolWindow.getId() + ".ToolWindow.fillAsPanel");
        if (fillAsPanel) {
            Border topBorder = JBUI.Borders.customLine(UIUtil.getBorderColor(), 1, 0, 0, 0);
            Border bottomBorder = JBUI.Borders.customLine(MorphColor.of(UIUtil::getPanelBackground), 0, 0, 1, 0);

            setBorder(JBUI.Borders.merge(topBorder, bottomBorder, false));
        } else {
            setBackground(MorphColor.ofWithoutCache(() -> {
                return !isActive() ? UIUtil.getPanelBackground() : ToolwindowPaintUtil.getActiveToolWindowHeaderColor();
            }));

            setBorder(JBUI.Borders.customLine(UIUtil.getBorderColor(), 1, 0, 1, 0));
        }

        new DoubleClickListener() {
            @Override
            protected boolean onDoubleClick(MouseEvent event) {
                ToolWindowManagerBase mgr = toolWindow.getToolWindowManager();
                mgr.setMaximized(myToolWindow, !mgr.isMaximized(myToolWindow));
                return true;
            }
        }.installOn(myWestPanel);
    }

    public void setToolbarComponent(JComponent component) {
        myToolbar.setTargetComponent(component);

        if (myToolbarWest != null) {
            myToolbarWest.setTargetComponent(component);
        }
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
        myToolbarWest =
            ActionManager.getInstance().createActionToolbar(ActionPlaces.TOOLWINDOW_TITLE, new DefaultActionGroup(myActionGroupWest), true);

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

        if (myToolbar != null) {
            myToolbar.updateActionsImmediately();
        }
    }

    protected boolean isActive() {
        return myToolWindow.isActive();
    }

    public ActionToolbar getToolbar() {
        return myToolbar;
    }

    public ActionToolbar getToolbarWest() {
        return myToolbarWest;
    }

    public boolean isPopupShowing() {
        // TODO ?
        return false;
    }

    protected abstract void hideToolWindow();

    @Override
    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        return new Dimension(size.width, TabsUtil.getRealTabsHeight());
    }
}
