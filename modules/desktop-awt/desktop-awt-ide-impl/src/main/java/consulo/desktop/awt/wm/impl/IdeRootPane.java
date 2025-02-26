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
package consulo.desktop.awt.wm.impl;

import consulo.application.Application;
import consulo.application.ui.UISettings;
import consulo.application.ui.event.UISettingsListener;
import consulo.dataContext.DataManager;
import consulo.desktop.awt.uiOld.DesktopBalloonLayoutImpl;
import consulo.desktop.awt.wm.navigationToolbar.IdeRootPaneNorthExtensionWithDecorator;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ide.impl.desktop.DesktopIdeFrameUtil;
import consulo.ide.impl.idea.ide.actions.CustomizeUIAction;
import consulo.ide.impl.idea.ide.actions.ViewToolbarAction;
import consulo.ide.impl.idea.ide.ui.customization.CustomActionsSchemaImpl;
import consulo.ide.impl.idea.openapi.wm.impl.IdeGlassPaneImpl;
import consulo.ide.impl.idea.openapi.wm.impl.IdePanePanel;
import consulo.project.Project;
import consulo.project.ui.internal.WindowManagerEx;
import consulo.project.ui.wm.*;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.util.ScreenUtil;
import consulo.ui.ex.toolWindow.ToolWindowPanel;
import consulo.util.collection.impl.map.LinkedHashMap;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.MouseMotionAdapter;
import java.util.Map;

/**
 * @author Anton Katilin
 * @author Vladimir Kondratyev
 */
public class IdeRootPane extends JRootPane implements Disposable, UISettingsListener {
    /**
     * Toolbar and status bar.
     */
    private JComponent myToolbar;
    private IdeStatusBarImpl myStatusBar;
    private boolean myStatusBarDisposed;

    private final Box myNorthPanel = Box.createVerticalBox();
    private final Map<Class<? extends IdeRootPaneNorthExtension>, IdeRootPaneNorthExtension> myNorthComponents = new LinkedHashMap<>();

    /**
     * Current {@code ToolWindowPanel}. If there is no such pane then this field is null.
     */
    private ToolWindowPanel myToolWindowsPane;
    private JBPanel myContentPane;
    private final ActionManager myActionManager;
    private final DesktopIdeFrameImpl myFrame;

    private final boolean myGlassPaneInitialized;

    private boolean myFullScreen;

    public IdeRootPane(ActionManager actionManager, DataManager dataManager, Application application, final DesktopIdeFrameImpl frame) {
        myActionManager = actionManager;
        myFrame = frame;

        myContentPane.add(myNorthPanel, BorderLayout.NORTH);

        myContentPane.addMouseMotionListener(new MouseMotionAdapter() {
        }); // listen to mouse motion events for a11y

        createStatusBar(application, frame);

        updateStatusBarVisibility();

        myContentPane.add(myStatusBar, BorderLayout.SOUTH);

        frame.addFullScreenListener((v) -> {
            myFullScreen = frame.isInFullScreen();

            updateToolbar();

            for (IdeRootPaneNorthExtension extension : myNorthComponents.values()) {
                extension.handleFullScreen(myFullScreen);
            }
        }, this);

        if (WindowManagerEx.getInstanceEx().isFloatingMenuBarSupported()) {
            menuBar = new IdeMenuBar(frame, actionManager, dataManager);
            
            getLayeredPane().add(menuBar, Integer.valueOf(JLayeredPane.DEFAULT_LAYER - 1));
        }
        else {
            setJMenuBar(new IdeMenuBar(null, actionManager, dataManager));
        }

        IdeGlassPaneImpl glassPane = new IdeGlassPaneImpl(this);
        setGlassPane(glassPane);
        myGlassPaneInitialized = true;
        glassPane.setVisible(false);
    }

    @Override
    protected LayoutManager createRootLayout() {
        return WindowManagerEx.getInstanceEx().isFloatingMenuBarSupported() ? new MyRootLayout() : super.createRootLayout();
    }

    @Override
    public void setGlassPane(final Component glass) {
        if (myGlassPaneInitialized) {
            throw new IllegalStateException("Setting of glass pane for IdeFrame is prohibited");
        }
        super.setGlassPane(glass);
    }


    /**
     * Invoked when enclosed frame is being shown.
     */
    @Override
    public final void addNotify() {
        super.addNotify();
    }

    /**
     * Invoked when enclosed frame is being disposed.
     */
    @Override
    public final void removeNotify() {
        if (ScreenUtil.isStandardAddRemoveNotify(this)) {
            if (!myStatusBarDisposed) {
                myStatusBarDisposed = true;
                Disposer.dispose(myStatusBar);
            }
            removeToolbar();
            setJMenuBar(null);
        }
        super.removeNotify();
    }

    /**
     * Sets current tool windows pane (panel where all tool windows are located).
     * If {@code toolWindowsPane} is {@code null} then the method just removes
     * the current tool windows pane.
     */
    public final void setToolWindowsPane(@Nullable final ToolWindowPanel toolWindowsPane) {
        final JComponent contentPane = (JComponent) getContentPane();
        if (myToolWindowsPane != null) {
            contentPane.remove((Component) myToolWindowsPane);
        }

        myToolWindowsPane = toolWindowsPane;
        if (myToolWindowsPane != null) {
            contentPane.add((Component) myToolWindowsPane, BorderLayout.CENTER);
        }

        contentPane.revalidate();
    }

    @Override
    protected JLayeredPane createLayeredPane() {
        JLayeredPane p = new JBLayeredPane();
        p.setName(getName() + ".layeredPane");
        return p;
    }

    @Override
    protected final Container createContentPane() {
        return myContentPane = new IdePanePanel(new BorderLayout());
    }

    public void updateToolbar() {
        removeToolbar();
        myToolbar = createToolbar();
        myToolbar.setBorder(new Border() {
            @Override
            public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            }

            @Override
            public Insets getBorderInsets(Component c) {
                TitlelessDecorator decorator = myFrame.getTitlelessDecorator();
                return JBUI.insets(decorator.getExtraTopTopPadding(), decorator.getExtraTopLeftPadding(myFullScreen), 0, 0);
            }

            @Override
            public boolean isBorderOpaque() {
                return false;
            }
        });
        myNorthPanel.add(myToolbar, 0);
        updateToolbarVisibility();
        myContentPane.revalidate();
    }

    private void removeToolbar() {
        if (myToolbar != null) {
            myNorthPanel.remove(myToolbar);
            myToolbar = null;
        }
    }

    public void updateNorthComponents() {
        for (IdeRootPaneNorthExtension northComponent : myNorthComponents.values()) {
            northComponent.revalidate();
        }
        myContentPane.revalidate();
    }

    @RequiredUIAccess
    public void updateMainMenuActions() {
        ((IdeMenuBar) menuBar).updateMenuActions();
        menuBar.repaint();
    }

    private JComponent createToolbar() {
        ActionGroup group = (ActionGroup) CustomActionsSchemaImpl.getInstance().getCorrectedAction(IdeActions.GROUP_MAIN_TOOLBAR);
        final ActionToolbar toolBar = myActionManager.createActionToolbar(
            ActionPlaces.MAIN_TOOLBAR,
            group,
            true
        );
        toolBar.setLayoutPolicy(ActionToolbar.WRAP_LAYOUT_POLICY);
        toolBar.setTargetComponent(null);

        DefaultActionGroup menuGroup = new DefaultActionGroup();
        menuGroup.add(new ViewToolbarAction());
        menuGroup.add(new CustomizeUIAction());
        PopupHandler.installUnknownPopupHandler(toolBar.getComponent(), menuGroup, myActionManager);

        return toolBar.getComponent();
    }

    @RequiredUIAccess
    private void createStatusBar(Application application, IdeFrame frame) {
        myStatusBar = new IdeStatusBarImpl(application);
        Disposer.register(this, myStatusBar);
        myStatusBar.install(frame);
    }

    @Nullable
    public final StatusBar getStatusBar() {
        return myStatusBar;
    }

    public int getStatusBarHeight() {
        return myStatusBar.isVisible() ? myStatusBar.getHeight() : 0;
    }

    private void updateToolbarVisibility() {
        myToolbar.setVisible(UISettings.getInstance().getShowMainToolbar() && !UISettings.getInstance().getPresentationMode());
    }

    private void updateStatusBarVisibility() {
        myStatusBar.setVisible(UISettings.getInstance().getShowStatusBar() && !UISettings.getInstance().getPresentationMode());
    }

    public void installNorthComponents(final Project project, TitlelessDecorator titlelessDecorator) {
        project.getExtensionPoint(IdeRootPaneNorthExtension.class).forEachExtensionSafe(northComponent -> {
            if (northComponent instanceof IdeRootPaneNorthExtensionWithDecorator decorator) {
                decorator.setTitlelessDecorator(titlelessDecorator);
            }
            
            myNorthComponents.put(northComponent.getApiClass(), northComponent);

            myNorthPanel.add(northComponent.getComponent());

            northComponent.uiSettingsChanged(UISettings.getInstance());
        });
    }

    public void deinstallNorthComponents() {
        for (IdeRootPaneNorthExtension northComponent : myNorthComponents.values()) {
            myNorthPanel.remove(northComponent.getComponent());
            Disposer.dispose(northComponent);
        }
        myNorthComponents.clear();
    }

    @Nullable
    public IdeRootPaneNorthExtension findExtension(Class<? extends IdeRootPaneNorthExtension> apiClass) {
        return myNorthComponents.get(apiClass);
    }

    @Override
    public void uiSettingsChanged(UISettings uiSettings) {
        updateToolbarVisibility();
        updateStatusBarVisibility();
        for (IdeRootPaneNorthExtension component : myNorthComponents.values()) {
            component.uiSettingsChanged(uiSettings);
        }
        IdeFrame frame = DesktopIdeFrameUtil.findIdeFrameFromParent(this);
        BalloonLayout layout = frame != null ? frame.getBalloonLayout() : null;
        if (layout instanceof DesktopBalloonLayoutImpl) {
            ((DesktopBalloonLayoutImpl) layout).queueRelayout();
        }
    }

    @Override
    public void dispose() {
    }

    private class MyRootLayout extends RootLayout {
        @Override
        public Dimension preferredLayoutSize(Container parent) {
            Dimension rd;
            Insets i = getInsets();

            if (contentPane != null) {
                rd = contentPane.getPreferredSize();
            }
            else {
                rd = parent.getSize();
            }
            Dimension mbd;
            if (menuBar != null && menuBar.isVisible() && !myFullScreen) {
                mbd = menuBar.getPreferredSize();
            }
            else {
                mbd = JBUI.emptySize();
            }
            return new Dimension(Math.max(rd.width, mbd.width) + i.left + i.right,
                rd.height + mbd.height + i.top + i.bottom);
        }

        @Override
        public Dimension minimumLayoutSize(Container parent) {
            Dimension rd;
            Insets i = getInsets();
            if (contentPane != null) {
                rd = contentPane.getMinimumSize();
            }
            else {
                rd = parent.getSize();
            }
            Dimension mbd;
            if (menuBar != null && menuBar.isVisible() && !myFullScreen) {
                mbd = menuBar.getMinimumSize();
            }
            else {
                mbd = JBUI.emptySize();
            }
            return new Dimension(Math.max(rd.width, mbd.width) + i.left + i.right,
                rd.height + mbd.height + i.top + i.bottom);
        }

        @Override
        public Dimension maximumLayoutSize(Container target) {
            Dimension mbd;
            Insets i = getInsets();
            if (menuBar != null && menuBar.isVisible() && !myFullScreen) {
                mbd = menuBar.getMaximumSize();
            }
            else {
                mbd = JBUI.emptySize();
            }
            Dimension rd;
            if (contentPane != null) {
                rd = contentPane.getMaximumSize();
            }
            else {
                rd = new Dimension(Integer.MAX_VALUE,
                    Integer.MAX_VALUE - i.top - i.bottom - mbd.height - 1);
            }
            return new Dimension(Math.min(rd.width, mbd.width) + i.left + i.right,
                rd.height + mbd.height + i.top + i.bottom);
        }

        @Override
        public void layoutContainer(Container parent) {
            Rectangle b = parent.getBounds();
            Insets i = getInsets();
            int w = b.width - i.right - i.left;
            int h = b.height - i.top - i.bottom;

            if (layeredPane != null) {
                layeredPane.setBounds(i.left, i.top, w, h);
            }
            if (glassPane != null) {
                glassPane.setBounds(i.left, i.top, w, h);
            }
            int contentY = 0;
            if (menuBar != null && menuBar.isVisible()) {
                Dimension mbd = menuBar.getPreferredSize();
                menuBar.setBounds(0, 0, w, mbd.height);
                if (!myFullScreen) {
                    contentY += mbd.height;
                }
            }
            if (contentPane != null) {
                contentPane.setBounds(0, contentY, w, h - contentY);
            }
        }
    }
}
