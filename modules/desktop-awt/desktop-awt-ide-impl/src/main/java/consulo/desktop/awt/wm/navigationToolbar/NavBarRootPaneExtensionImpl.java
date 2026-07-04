/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package consulo.desktop.awt.wm.navigationToolbar;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.ui.UISettings;
import consulo.desktop.awt.navbar.ui.NavBarUIController;
import consulo.desktop.awt.navbar.ui.StaticNavBarPanel;
import consulo.navigationBar.model.NavBarVm;
import consulo.application.ui.event.UISettingsListener;
import consulo.disposer.Disposer;
import consulo.desktop.awt.wm.navigationToolbar.ui.NavBarBorder;
import consulo.ide.impl.idea.ide.ui.customization.CustomActionsSchemaImpl;
import consulo.ide.impl.idea.ide.ui.customization.CustomisedActionGroup;
import consulo.project.Project;
import consulo.project.ui.internal.IdeFrameEx;
import consulo.project.ui.wm.IdeFrame;
import consulo.project.ui.wm.IdeRootPaneNorthExtension;
import consulo.project.ui.wm.NavBarRootPaneExtension;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.ScrollPaneFactory;
import consulo.ui.ex.awt.TitlelessDecorator;
import consulo.ui.ex.awt.action.ComboBoxAction;
import consulo.ui.ex.awt.util.JBSwingUtilities;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import org.jspecify.annotations.Nullable;
import jakarta.inject.Inject;

import javax.swing.*;
import java.awt.*;

/**
 * @author anna
 * @author Konstantin Bulenkov
 * @since 2007-11-12
 */
@ExtensionImpl
public class NavBarRootPaneExtensionImpl implements NavBarRootPaneExtension, IdeRootPaneNorthExtensionWithDecorator {
    private JComponent myWrapperPanel;
    private Project myProject;
    private StaticNavBarPanel myNavigationBar;
    private JPanel myRunPanel;
    private final boolean myNavToolbarGroupExist;
    private JScrollPane myScrollPane;

    private TitlelessDecorator myTitlelessDecorator = TitlelessDecorator.NOTHING;

    private boolean myFullScreen;

    @Inject
    public NavBarRootPaneExtensionImpl(Project project) {
        myProject = project;

        myProject.getMessageBus().connect().subscribe(UISettingsListener.class, uiSettings -> toggleRunPanel(!uiSettings.getShowMainToolbar() && uiSettings.getShowNavigationBar() && !uiSettings.getPresentationMode()));

        myNavToolbarGroupExist = runToolbarExists();

        Disposer.register(myProject, this);
    }

    @Override
    public void setTitlelessDecorator(TitlelessDecorator titlelessDecorator) {
        myTitlelessDecorator = titlelessDecorator;
    }

    @Override
    public void handleFullScreen(boolean fullScreen) {
        myFullScreen = fullScreen;

        if (myScrollPane != null) {
            myScrollPane.invalidate();
            myScrollPane.repaint();
        }
    }

    @Override
    public void revalidate() {
        UISettings settings = UISettings.getInstance();
        if (!settings.getShowMainToolbar() && settings.getShowNavigationBar() && !UISettings.getInstance().getPresentationMode()) {
            toggleRunPanel(false);
            toggleRunPanel(true);
        }
    }

    @Override
    public IdeRootPaneNorthExtension copy() {
        return new NavBarRootPaneExtensionImpl(myProject);
    }

    public boolean isMainToolbarVisible() {
        return !UISettings.getInstance().getPresentationMode() && (UISettings.getInstance().getShowMainToolbar() || !myNavToolbarGroupExist);
    }

    public static boolean runToolbarExists() {
        AnAction correctedAction = CustomActionsSchemaImpl.getInstance().getCorrectedAction("NavBarToolBar");
        return correctedAction instanceof DefaultActionGroup && ((DefaultActionGroup) correctedAction).getChildrenCount() > 0 ||
            correctedAction instanceof CustomisedActionGroup && ((CustomisedActionGroup) correctedAction).getFirstAction() != null;
    }

    @Override
    public JComponent getComponent() {
        if (myWrapperPanel == null) {
            myWrapperPanel = new NavBarWrapperPanel(new BorderLayout()) {
                @Override
                public Insets getInsets() {
                    return JBUI.insets(super.getInsets());
                }
            };
            myWrapperPanel.add(buildNavBarPanel(), BorderLayout.CENTER);
            toggleRunPanel(!UISettings.getInstance().getShowMainToolbar() && !UISettings.getInstance().getPresentationMode());
        }

        return myWrapperPanel;
    }

    public static class NavBarWrapperPanel extends JPanel {
        public NavBarWrapperPanel(LayoutManager layout) {
            super(layout);
            setName("navbar");
        }

        @Override
        protected Graphics getComponentGraphics(Graphics graphics) {
            return JBSwingUtilities.runGlobalCGTransform(this, super.getComponentGraphics(graphics));
        }
    }

    private static void alignVertically(Container container) {
        if (container.getComponentCount() == 1) {
            Component c = container.getComponent(0);
            Insets insets = container.getInsets();
            Dimension d = c.getPreferredSize();
            Rectangle r = container.getBounds();
            c.setBounds(insets.left, (r.height - d.height - insets.top - insets.bottom) / 2 + insets.top, r.width - insets.left - insets.right, d.height);
        }
    }

    private void toggleRunPanel(boolean show) {
        if (show && myRunPanel == null && runToolbarExists()) {
            ActionManager manager = ActionManager.getInstance();
            AnAction toolbarRunGroup = CustomActionsSchemaImpl.getInstance().getCorrectedAction("NavBarToolBar");
            if (toolbarRunGroup instanceof ActionGroup) {
                boolean needGap = isNeedGap(toolbarRunGroup);
                ActionToolbar actionToolbar = manager.createActionToolbar(ActionPlaces.NAVIGATION_BAR_TOOLBAR, (ActionGroup) toolbarRunGroup, true);
                actionToolbar.setTargetComponent(null);
                JComponent component = actionToolbar.getComponent();
                myRunPanel = new JPanel(new BorderLayout()) {
                    @Override
                    public void doLayout() {
                        alignVertically(this);
                    }
                };
                myRunPanel.setOpaque(false);
                myRunPanel.add(component, BorderLayout.CENTER);
                myRunPanel.setBorder(JBUI.Borders.empty(0, needGap ? 5 : 1, 0, 0));
                myWrapperPanel.add(myRunPanel, BorderLayout.EAST);
            }
        }
        else if (!show && myRunPanel != null) {
            myWrapperPanel.remove(myRunPanel);
            myRunPanel = null;
        }
    }

    private boolean isUndocked() {
        consulo.ui.Window uiWindow = TargetAWT.from(SwingUtilities.getWindowAncestor(myWrapperPanel));
        return (uiWindow != null && !(uiWindow.getUserData(IdeFrame.KEY) instanceof IdeFrameEx)) || !UISettings.getInstance().getShowMainToolbar() || !UISettings.getInstance().getPresentationMode();
    }

    private static boolean isNeedGap(AnAction group) {
        AnAction firstAction = getFirstAction(group);
        return firstAction instanceof ComboBoxAction;
    }

    private static @Nullable AnAction getFirstAction(AnAction group) {
        if (group instanceof DefaultActionGroup) {
            AnAction firstAction = null;
            for (AnAction action : ((DefaultActionGroup) group).getChildActionsOrStubs()) {
                if (action instanceof DefaultActionGroup) {
                    firstAction = getFirstAction(action);
                }
                else if (action instanceof AnSeparator || action instanceof ActionGroup) {
                    continue;
                }
                else {
                    firstAction = action;
                    break;
                }

                if (firstAction != null) {
                    break;
                }
            }

            return firstAction;
        }
        if (group instanceof CustomisedActionGroup) {
            return ((CustomisedActionGroup) group).getFirstAction();
        }
        return null;
    }

    private JComponent buildNavBarPanel() {
        myNavigationBar = (StaticNavBarPanel) NavBarUIController.getInstance(myProject).createNavBarPanel();
        myWrapperPanel.putClientProperty("NavBarPanel", myNavigationBar);
        myScrollPane = ScrollPaneFactory.createScrollPane(myNavigationBar);

        JPanel panel = new JPanel(new BorderLayout()) {

            @Override
            public void doLayout() {
                // align vertically
                Rectangle r = getBounds();
                Insets insets = getInsets();
                int x = insets.left;
                if (myScrollPane == null) {
                    return;
                }
                Component navBar = myScrollPane;

                Dimension preferredSize = navBar.getPreferredSize();

                navBar.setBounds(x, (r.height - preferredSize.height) / 2, r.width - insets.left - insets.right, preferredSize.height);
            }

            @Override
            public void updateUI() {
                super.updateUI();
                setOpaque(true);
                if (myScrollPane == null || myNavigationBar == null) {
                    return;
                }

                myScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
                myScrollPane.setHorizontalScrollBar(null);
                myScrollPane.setBorder(new NavBarBorder(myTitlelessDecorator, () -> myFullScreen));
                myScrollPane.setOpaque(false);
                myScrollPane.getViewport().setOpaque(false);
                myScrollPane.setViewportBorder(null);
                myNavigationBar.setBorder(null);
            }
        };

        panel.add(myScrollPane, BorderLayout.CENTER);
        panel.updateUI();
        return panel;
    }

    @Override
    public void uiSettingsChanged(UISettings settings) {
        if (myNavigationBar != null) {
            NavBarUIController.getInstance(myProject).uiSettingsChanged(settings);
            myWrapperPanel.setVisible(settings.getShowNavigationBar() && !UISettings.getInstance().getPresentationMode());

            myWrapperPanel.revalidate();
            myNavigationBar.revalidate();
            myWrapperPanel.repaint();

            if (myWrapperPanel.getComponentCount() > 0) {
                Component c = myWrapperPanel.getComponent(0);
                if (c instanceof JComponent) {
                    ((JComponent) c).setOpaque(false);
                }
            }
        }
    }

    @Override
    public Class<? extends IdeRootPaneNorthExtension> getApiClass() {
        return NavBarRootPaneExtension.class;
    }

    @Override
    public void dispose() {
        myWrapperPanel.setVisible(false);
        myWrapperPanel = null;
        myRunPanel = null;
        myNavigationBar = null;
        myScrollPane = null;
        myProject = null;
    }

    @Override
    public void rebuildAndSelectTail() {
        JComponent c = getComponent();
        if (c.getClientProperty("NavBarPanel") instanceof StaticNavBarPanel staticNavBarPanel) {
            NavBarVm vm = staticNavBarPanel.getModel();
            if (vm != null) {
                vm.selectTail(false);
            }
        }
    }
}
