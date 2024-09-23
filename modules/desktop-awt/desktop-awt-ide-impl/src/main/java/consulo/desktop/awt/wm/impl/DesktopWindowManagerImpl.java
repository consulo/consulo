/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

import com.sun.jna.platform.WindowUtils;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.application.util.SystemInfo;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.RoamingType;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.dataContext.DataManager;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ide.impl.idea.ide.AppLifecycleListener;
import consulo.ide.impl.idea.ide.GeneralSettings;
import consulo.ide.impl.idea.util.EventDispatcher;
import consulo.logging.Logger;
import consulo.platform.Platform;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.project.ui.internal.IdeFrameEx;
import consulo.project.ui.internal.ToolWindowLayout;
import consulo.project.ui.internal.WindowManagerEx;
import consulo.project.ui.wm.IdeFrame;
import consulo.project.ui.wm.IdeFrameState;
import consulo.project.ui.wm.StatusBar;
import consulo.project.ui.wm.WelcomeFrameManager;
import consulo.project.ui.wm.event.WindowManagerListener;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.util.ScreenUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.popup.JBPopup;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jdom.Element;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Anton Katilin
 * @author Vladimir Kondratyev
 */
@ServiceImpl
@Singleton
@State(name = WindowManagerEx.ID, storages = @Storage(value = "window.manager.xml", roamingType = RoamingType.DISABLED), defaultStateFilePath = "/defaultState/WindowManager.xml")
public final class DesktopWindowManagerImpl extends WindowManagerEx implements PersistentStateComponent<Element>, Disposable {
    private static final Logger LOG = Logger.getInstance(DesktopWindowManagerImpl.class);

    private Boolean myAlphaModeSupported = null;

    private final EventDispatcher<WindowManagerListener> myEventDispatcher = EventDispatcher.create(WindowManagerListener.class);

    private final DesktopWindowWatcher myWindowWatcher;
    /**
     * That is the default layout.
     */
    private final ToolWindowLayout myLayout;

    private final HashMap<Project, DesktopIdeFrameImpl> myProject2Frame;

    private final HashMap<Project, Set<JDialog>> myDialogsToDispose;

    private final WindowAdapter myActivationListener;
    private final Application myApplication;
    private final DataManager myDataManager;
    private final ActionManager myActionManager;

    @Inject
    public DesktopWindowManagerImpl(Application application, DataManager dataManager, ActionManager actionManager) {
        myApplication = application;
        myDataManager = dataManager;
        myActionManager = actionManager;

        myWindowWatcher = new DesktopWindowWatcher(application, dataManager);
        final KeyboardFocusManager keyboardFocusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        keyboardFocusManager.addPropertyChangeListener("focusedWindow", myWindowWatcher);
        myLayout = new ToolWindowLayout();
        myProject2Frame = new HashMap<>();
        myDialogsToDispose = new HashMap<>();

        myActivationListener = new WindowAdapter() {
            @Override
            public void windowActivated(WindowEvent e) {
                consulo.ui.Window activeWindow = TargetAWT.from(e.getWindow());

                IdeFrame ideFrame = activeWindow.getUserData(IdeFrame.KEY);
                if (ideFrame != null) {
                    proceedDialogDisposalQueue(ideFrame.getProject());
                }
            }
        };

        application.getMessageBus().connect().subscribe(AppLifecycleListener.class, new AppLifecycleListener() {
            @Override
            public void appClosing() {
                // save full screen window states
                if (isFullScreenSupportedInCurrentOS() && GeneralSettings.getInstance().isReopenLastProject()) {
                    Project[] openProjects = ProjectManager.getInstance().getOpenProjects();

                    if (openProjects.length > 0) {
                        WindowManagerEx wm = WindowManagerEx.getInstanceEx();
                        for (Project project : openProjects) {
                            IdeFrameEx frame = wm.getIdeFrame(project);
                            if (frame != null) {
                                frame.storeFullScreenStateIfNeeded();
                            }
                        }
                    }
                }
            }
        });
    }

    @Override
    public void dispose() {
        disposeRootFrame();
    }

    @Override
    @Nonnull
    public DesktopIdeFrameImpl[] getAllProjectFrames() {
        final Collection<DesktopIdeFrameImpl> ideFrames = myProject2Frame.values();
        return ideFrames.toArray(new DesktopIdeFrameImpl[ideFrames.size()]);
    }

    @Nullable
    @Override
    public IdeFrame findVisibleIdeFrame() {
        IdeFrame[] frames = getAllProjectFrames();
        if (frames.length > 0) {
            return frames[0];
        }
        IdeFrame frame = WelcomeFrameManager.getInstance().getCurrentFrame();
        if (frame == null) {
            // will return null at first app start, while customization window
            return null;
        }
        return frame;
    }

    @Override
    public void addListener(final WindowManagerListener listener) {
        myEventDispatcher.addListener(listener);
    }

    @Override
    public void removeListener(final WindowManagerListener listener) {
        myEventDispatcher.removeListener(listener);
    }

    @Override
    public final Rectangle getScreenBounds() {
        return ScreenUtil.getAllScreensRectangle();
    }

    @Override
    public Rectangle getScreenBounds(@Nonnull Project project) {
        final GraphicsEnvironment environment = GraphicsEnvironment.getLocalGraphicsEnvironment();
        final Point onScreen = getFrame(project).getLocationOnScreen();
        final GraphicsDevice[] devices = environment.getScreenDevices();
        for (final GraphicsDevice device : devices) {
            final Rectangle bounds = device.getDefaultConfiguration().getBounds();
            if (bounds.contains(onScreen)) {
                return bounds;
            }
        }

        return null;
    }

    @Override
    public final boolean isInsideScreenBounds(final int x, final int y, final int width) {
        return ScreenUtil.getAllScreensShape().contains(x, y, width, 1);
    }

    @Override
    public final boolean isInsideScreenBounds(final int x, final int y) {
        return ScreenUtil.getAllScreensShape().contains(x, y);
    }

    @Override
    public final boolean isAlphaModeSupported() {
        if (myAlphaModeSupported == null) {
            myAlphaModeSupported = calcAlphaModelSupported();
        }
        return myAlphaModeSupported;
    }

    private static boolean calcAlphaModelSupported() {
        GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        if (device.isWindowTranslucencySupported(GraphicsDevice.WindowTranslucency.TRANSLUCENT)) {
            return true;
        }
        try {
            return WindowUtils.isWindowAlphaSupported();
        }
        catch (Throwable e) {
            return false;
        }
    }

    static boolean isTranslucencySupported() {
        GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        return device.isWindowTranslucencySupported(GraphicsDevice.WindowTranslucency.TRANSLUCENT);
    }

    static boolean isPerPixelTransparencySupported() {
        GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        return device.isWindowTranslucencySupported(GraphicsDevice.WindowTranslucency.PERPIXEL_TRANSPARENT);
    }

    @Override
    public final void setAlphaModeRatio(final Window window, final float ratio) {
        if (!window.isDisplayable() || !window.isShowing()) {
            throw new IllegalArgumentException("window must be displayable and showing. window=" + window);
        }
        if (ratio < 0.0f || ratio > 1.0f) {
            throw new IllegalArgumentException("ratio must be in [0..1] range. ratio=" + ratio);
        }
        if (!isAlphaModeSupported() || !isAlphaModeEnabled(window)) {
            return;
        }

        setAlphaMode(window, ratio);
    }

    private static void setAlphaMode(Window window, float ratio) {
        try {
            if (SystemInfo.isMacOSLeopard) {
                if (window instanceof JWindow) {
                    ((JWindow) window).getRootPane().putClientProperty("Window.alpha", 1.0f - ratio);
                }
                else if (window instanceof JDialog) {
                    ((JDialog) window).getRootPane().putClientProperty("Window.alpha", 1.0f - ratio);
                }
                else if (window instanceof JFrame) {
                    ((JFrame) window).getRootPane().putClientProperty("Window.alpha", 1.0f - ratio);
                }
            }
            else if (isTranslucencySupported()) {
                window.setOpacity(1.0f - ratio);
            }
            else {
                WindowUtils.setWindowAlpha(window, 1.0f - ratio);
            }
        }
        catch (Throwable e) {
            LOG.debug(e);
        }
    }

    @Override
    public void setWindowMask(final Window window, @Nullable final Shape mask) {
        try {
            if (isPerPixelTransparencySupported()) {
                window.setShape(mask);
            }
            else {
                WindowUtils.setWindowMask(window, mask);
            }
        }
        catch (Throwable e) {
            LOG.debug(e);
        }
    }

    @Override
    public void setWindowShadow(Window window, WindowShadowMode mode) {
        if (window instanceof JWindow) {
            JRootPane root = ((JWindow) window).getRootPane();
            root.putClientProperty("Window.shadow", mode == WindowShadowMode.DISABLED ? Boolean.FALSE : Boolean.TRUE);
            root.putClientProperty("Window.style", mode == WindowShadowMode.SMALL ? "small" : null);
        }
    }

    @Override
    public void resetWindow(final Window window) {
        try {
            if (!isAlphaModeSupported()) {
                return;
            }

            setWindowMask(window, null);
            setAlphaMode(window, 0f);
            setWindowShadow(window, WindowShadowMode.NORMAL);
        }
        catch (Throwable e) {
            LOG.debug(e);
        }
    }

    @Override
    public final boolean isAlphaModeEnabled(final Window window) {
        if (!window.isDisplayable() || !window.isShowing()) {
            throw new IllegalArgumentException("window must be displayable and showing. window=" + window);
        }
        return isAlphaModeSupported();
    }

    @Override
    public final void setAlphaModeEnabled(final Window window, final boolean state) {
        if (!window.isDisplayable() || !window.isShowing()) {
            throw new IllegalArgumentException("window must be displayable and showing. window=" + window);
        }
    }

    @Override
    public void hideDialog(JDialog dialog, Project project) {
        if (project == null) {
            dialog.dispose();
        }
        else {
            IdeFrameEx frame = getIdeFrame(project);
            if (frame.isActive()) {
                dialog.dispose();
            }
            else {
                queueForDisposal(dialog, project);
                dialog.setVisible(false);
            }
        }
    }

    @Override
    public void adjustContainerWindow(Component c, Dimension oldSize, Dimension newSize) {
        if (c == null) {
            return;
        }

        Window wnd = SwingUtilities.getWindowAncestor(c);

        if (wnd instanceof JWindow) {
            JBPopup popup = (JBPopup) ((JWindow) wnd).getRootPane().getClientProperty(JBPopup.KEY);
            if (popup != null) {
                if (oldSize.height < newSize.height) {
                    Dimension size = popup.getSize();
                    size.height += newSize.height - oldSize.height;
                    popup.setSize(size);
                    popup.moveToFitScreen();
                }
            }
        }
    }

    @Override
    public final void doNotSuggestAsParent(final consulo.ui.Window window) {
        myWindowWatcher.doNotSuggestAsParent(window);
    }

    @Override
    public final void dispatchComponentEvent(final ComponentEvent e) {
        myWindowWatcher.dispatchComponentEvent(e);
    }

    @Override
    @Nullable
    public consulo.ui.Window suggestParentWindow(@Nullable final Project project) {
        return myWindowWatcher.suggestParentWindow(project);
    }

    @Override
    public final StatusBar getStatusBar(final Project project) {
        if (!myProject2Frame.containsKey(project)) {
            return null;
        }
        final IdeFrameEx frame = getIdeFrame(project);
        LOG.assertTrue(frame != null);
        return frame.getStatusBar();
    }

    @Override
    public StatusBar getStatusBar(@Nonnull Component c) {
        return getStatusBar(c, null);
    }

    @Override
    public StatusBar getStatusBar(@Nonnull consulo.ui.Component c, @Nullable Project project) {
        return getStatusBar(TargetAWT.to(c), project);
    }

    @Override
    public StatusBar getStatusBar(@Nonnull Component c, @Nullable Project project) {
        Component parent = UIUtil.findUltimateParent(c);
        if (parent instanceof Window) {
            consulo.ui.Window uiWindow = TargetAWT.from((Window) parent);

            IdeFrame ideFrame = uiWindow.getUserData(IdeFrame.KEY);
            if (ideFrame != null) {
                StatusBar statusBar = ideFrame.getStatusBar();
                if (statusBar != null) {
                    return statusBar.findChild(c);
                }
            }
        }

        IdeFrame frame = findFrameFor(project);
        if (frame != null) {
            StatusBar statusBar = frame.getStatusBar();
            if (statusBar != null) {
                return statusBar.findChild(c);
            }
        }

        assert false : "Cannot find status bar for " + c;

        return null;
    }

    @RequiredUIAccess
    @Override
    public IdeFrame findFrameFor(@Nullable final Project project) {
        IdeFrame frame = null;
        if (project != null) {
            frame = project.isDefault() ? WelcomeFrameManager.getInstance().getCurrentFrame() : getIdeFrame(project);
            if (frame == null) {
                frame = myProject2Frame.get(null);
            }
        }
        else {
            Container eachParent = TargetAWT.to(getMostRecentFocusedWindow());
            while (eachParent != null) {
                if (eachParent instanceof Window) {
                    consulo.ui.Window uiWIndow = TargetAWT.from((Window) eachParent);
                    IdeFrame ideFrame = uiWIndow.getUserData(IdeFrame.KEY);
                    if (ideFrame != null) {
                        frame = ideFrame;
                        break;
                    }
                }
                eachParent = eachParent.getParent();
            }

            if (frame == null) {
                frame = tryToFindTheOnlyFrame();
            }
        }

        return frame;
    }

    private static IdeFrame tryToFindTheOnlyFrame() {
        IdeFrame candidate = null;
        final Frame[] all = Frame.getFrames();
        for (Frame each : all) {
            consulo.ui.Window uiWindow = TargetAWT.from(each);
            if (uiWindow == null) {
                continue;
            }

            IdeFrame ideFrame = uiWindow.getUserData(IdeFrame.KEY);
            if (ideFrame != null) {
                if (candidate == null) {
                    candidate = ideFrame;
                }
                else {
                    candidate = null;
                    break;
                }
            }
        }
        return candidate;
    }

    @Nullable
    @Override
    public consulo.ui.Window getWindow(@Nullable Project project) {
        // no assert! otherwise WindowWatcher.suggestParentWindow fails for default project
        //LOG.assertTrue(myProject2Frame.containsKey(project));
        DesktopIdeFrameImpl frame = myProject2Frame.get(project);
        if (frame != null) {
            return frame.getWindow();
        }
        return null;
    }

    @Override
    public IdeFrameEx getIdeFrame(@Nullable final Project project) {
        if (project != null) {
            return myProject2Frame.get(project);
        }
        final Window window = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
        final Component parentMaybeWindow = UIUtil.findUltimateParent(window);
        if (parentMaybeWindow instanceof Window) {
            consulo.ui.Window uiWindow = TargetAWT.from((Window) parentMaybeWindow);

            IdeFrame ideFrame = uiWindow.getUserData(IdeFrame.KEY);
            if (ideFrame instanceof IdeFrameEx) {
                return (IdeFrameEx) ideFrame;
            }
        }

        final Frame[] frames = Frame.getFrames();
        for (Frame each : frames) {
            consulo.ui.Window uiWindow = TargetAWT.from(each);
            if (uiWindow == null) {
                continue;
            }

            IdeFrame ideFrame = uiWindow.getUserData(IdeFrame.KEY);
            if (ideFrame instanceof IdeFrameEx) {
                return (IdeFrameEx) ideFrame;
            }
        }

        return null;
    }

    public void showFrame() {
        final DesktopIdeFrameImpl frame = new DesktopIdeFrameImpl(myActionManager, myDataManager, myApplication);
        myProject2Frame.put(null, frame);

        JFrame jWindow = (JFrame) TargetAWT.to(frame.getWindow());

        jWindow.setVisible(true);
    }

    private DesktopIdeFrameImpl getDefaultEmptyIdeFrame() {
        return myProject2Frame.get(null);
    }

    @RequiredUIAccess
    @Nonnull
    @Override
    public final IdeFrameEx allocateFrame(@Nonnull final Project project, @Nullable IdeFrameState state) {
        LOG.assertTrue(!myProject2Frame.containsKey(project));

        JFrame jFrame;
        final DesktopIdeFrameImpl ideFrame;
        if (myProject2Frame.containsKey(null)) {
            ideFrame = getDefaultEmptyIdeFrame();
            myProject2Frame.remove(null);
            myProject2Frame.put(project, ideFrame);
            ideFrame.setProject(project);
            jFrame = (JFrame) TargetAWT.to(ideFrame.getWindow());
        }
        else {
            ideFrame = new DesktopIdeFrameImpl(myActionManager, myDataManager, myApplication);

            jFrame = (JFrame) TargetAWT.to(ideFrame.getWindow());

            if (state != null) {
                jFrame.setBounds(new Rectangle(state.x(), state.y(), state.width(), state.height()));

                if (state.maximized()) {
                    jFrame.setExtendedState(Frame.MAXIMIZED_BOTH);
                }
            }

            myProject2Frame.put(project, ideFrame);
            ideFrame.setProject(project);
            jFrame.setVisible(true);
        }

        jFrame.addWindowListener(myActivationListener);
        myEventDispatcher.getMulticaster().frameCreated(ideFrame);

        return ideFrame;
    }

    private void proceedDialogDisposalQueue(Project project) {
        Set<JDialog> dialogs = myDialogsToDispose.get(project);
        if (dialogs == null) {
            return;
        }
        for (JDialog dialog : dialogs) {
            dialog.dispose();
        }
        myDialogsToDispose.put(project, null);
    }

    private void queueForDisposal(JDialog dialog, Project project) {
        Set<JDialog> dialogs = myDialogsToDispose.get(project);
        if (dialogs == null) {
            dialogs = new HashSet<>();
            myDialogsToDispose.put(project, dialogs);
        }
        dialogs.add(dialog);
    }

    @Override
    public final void releaseFrame(final IdeFrameEx frame) {
        DesktopIdeFrameImpl implFrame = (DesktopIdeFrameImpl) frame;

        myEventDispatcher.getMulticaster().beforeFrameReleased(implFrame);

        final Project project = implFrame.getProject();
        LOG.assertTrue(project != null);

        JFrame jFrame = (JFrame) TargetAWT.to(implFrame.getWindow());

        jFrame.removeWindowListener(myActivationListener);
        proceedDialogDisposalQueue(project);

        implFrame.setProject(null);
        jFrame.setTitle(null);
        implFrame.setFileTitle(null, null);

        myProject2Frame.remove(project);
        if (myProject2Frame.isEmpty()) {
            myProject2Frame.put(null, implFrame);
        }
        else {
            Disposer.dispose(implFrame.getStatusBar());
            jFrame.dispose();
        }
    }

    @Override
    public final void disposeRootFrame() {
        if (myProject2Frame.size() == 1) {
            final DesktopIdeFrameImpl rootFrame = myProject2Frame.remove(null);
            if (rootFrame != null) {
                // disposing last frame if quitting
                rootFrame.getWindow().dispose();
            }
        }
    }

    @Override
    public final consulo.ui.Window getMostRecentFocusedWindow() {
        return myWindowWatcher.getFocusedWindow();
    }

    @Override
    public final Component getFocusedComponent(@Nonnull final Window window) {
        return myWindowWatcher.getFocusedComponent(window);
    }

    @Override
    @Nullable
    public final Component getFocusedComponent(@Nullable final Project project) {
        return myWindowWatcher.getFocusedComponent(project);
    }

    @Override
    public void loadState(Element state) {
        final Element desktopElement = state.getChild(ToolWindowLayout.TAG);
        if (desktopElement != null) {
            myLayout.readExternal(desktopElement);
        }
    }

    @Nullable
    @Override
    public Element getState() {
        Element state = new Element("state");

        // Save default layout
        Element layoutElement = myLayout.writeExternal(ToolWindowLayout.TAG);
        if (layoutElement != null) {
            state.addContent(layoutElement);
        }
        return state;
    }

    @Override
    public final ToolWindowLayout getLayout() {
        return myLayout;
    }

    @Override
    public final void setLayout(final ToolWindowLayout layout) {
        myLayout.copyFrom(layout);
    }

    public DesktopWindowWatcher getWindowWatcher() {
        return myWindowWatcher;
    }

    @Override
    public boolean isFullScreenSupportedInCurrentOS() {
        return Platform.current().os().isMac()
            || Platform.current().os().isWindows()
            || Platform.current().os().isXWindow() && X11UiUtil.isFullScreenSupported();
    }

    @Override
    public boolean isFloatingMenuBarSupported() {
        return !Platform.current().os().isMac() && getInstance().isFullScreenSupportedInCurrentOS();
    }
}
