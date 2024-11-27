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

import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.application.impl.internal.IdeaModalityState;
import consulo.application.progress.Task;
import consulo.application.util.SystemInfo;
import consulo.awt.hacking.AWTAccessorHacking;
import consulo.dataContext.DataManager;
import consulo.desktop.awt.ui.impl.window.JFrameAsUIWindow;
import consulo.desktop.awt.ui.util.AppIconUtil;
import consulo.desktop.awt.uiOld.DesktopBalloonLayoutImpl;
import consulo.disposer.Disposer;
import consulo.ide.impl.actionSystem.ex.TopApplicationMenuUtil;
import consulo.ide.impl.application.FrameTitleUtil;
import consulo.ide.impl.idea.ide.AppLifecycleListener;
import consulo.ide.impl.idea.ide.util.PropertiesComponent;
import consulo.ide.impl.idea.openapi.wm.impl.status.widget.StatusBarWidgetsActionGroup;
import consulo.ide.impl.idea.util.ui.accessibility.AccessibleContextAccessor;
import consulo.logging.Logger;
import consulo.platform.Platform;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.project.internal.ProjectManagerEx;
import consulo.project.ui.internal.IdeFrameEx;
import consulo.project.ui.wm.*;
import consulo.ui.Rectangle2D;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.internal.MouseGestureManager;
import consulo.ui.ex.awt.util.Alarm;
import consulo.ui.ex.awt.util.ScreenUtil;
import consulo.ui.ex.awt.util.UISettingsUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.util.concurrent.ActionCallback;
import consulo.util.dataholder.Key;
import consulo.util.lang.BitUtil;
import consulo.util.lang.StringUtil;
import consulo.util.lang.ref.Ref;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.accessibility.AccessibleContext;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.lang.reflect.Field;
import java.util.Objects;

/**
 * @author Anton Katilin
 * @author Vladimir Kondratyev
 */
public final class DesktopIdeFrameImpl implements IdeFrameEx, AccessibleContextAccessor {
    private class MyFrame extends JFrameAsUIWindow {
        protected class AccessibleIdeFrameImpl extends AccessibleJFrame {
            @Override
            public String getAccessibleName() {
                final StringBuilder builder = new StringBuilder();

                if (myProject != null) {
                    builder.append(myProject.getName());
                    builder.append(" - ");
                }
                builder.append(FrameTitleUtil.buildTitle());
                return builder.toString();
            }
        }

        @Override
        public void setRootPane(JRootPane root) {
            super.setRootPane(root);
        }

        @Nonnull
        @Override
        public Insets getInsets() {
            if (Platform.current().os().isMac() && isInFullScreen()) {
                return JBUI.emptyInsets();
            }
            return super.getInsets();
        }

        @Override
        public void show() {
            super.show();
            SwingUtilities.invokeLater(() -> setFocusableWindowState(true));
        }

        @SuppressWarnings("SSBasedInspection")
        @Override
        public void setVisible(boolean value) {
            super.setVisible(value);

            if (value && myRestoreFullScreen) {
                SwingUtilities.invokeLater(() -> {
                    toggleFullScreen(true);
                    if (SystemInfo.isMacOSLion) {
                        setBounds(ScreenUtil.getScreenRectangle(getLocationOnScreen()));
                    }
                    myRestoreFullScreen = false;
                });
            }
        }

        @Override
        public void setTitle(final String title) {
            if (myUpdatingTitle) {
                super.setTitle(title);
            }
            else {
                myTitle = title;
            }

            updateTitle();
        }

        @Override
        public void paint(@Nonnull Graphics g) {
            UISettingsUtil.setupAntialiasing(g);
            //noinspection Since15
            super.paint(g);
        }

        @Override
        public Color getBackground() {
            return super.getBackground();
        }

        @Override
        public void doLayout() {
            super.doLayout();
        }

        @Override
        public void dispose() {
            if (Platform.current().os().isMac() && isInFullScreen()) {
                ((MacMainFrameDecorator) myFrameDecorator).toggleFullScreenNow();
            }
            if (isTemporaryDisposed()) {
                super.dispose();
                return;
            }
            MouseGestureManager.getInstance().remove(DesktopIdeFrameImpl.this);

            if (myBalloonLayout != null) {
                ((DesktopBalloonLayoutImpl) myBalloonLayout).dispose();
                myBalloonLayout = null;
            }

            // clear both our and swing hard refs
            if (myRootPane != null) {
                if (ApplicationManager.getApplication().isUnitTestMode()) {
                    myRootPane.removeNotify();
                }
                setRootPane(new JRootPane());
                Disposer.dispose(myRootPane);
                myRootPane = null;
            }

            if (myFrameDecorator != null) {
                Disposer.dispose(myFrameDecorator);
                myFrameDecorator = null;
            }

            if (myWindowsBorderUpdater != null) {
                Toolkit.getDefaultToolkit().removePropertyChangeListener("win.xpstyle.themeActive", myWindowsBorderUpdater);
                myWindowsBorderUpdater = null;
            }

            super.dispose();
        }

        @Override
        public AccessibleContext getAccessibleContext() {
            if (accessibleContext == null) {
                accessibleContext = new AccessibleIdeFrameImpl();
            }
            return accessibleContext;
        }


        public void setTitleWithoutCheck(String title) {
            super.setTitle(title);
        }

        public AccessibleContext getAccessibleContextWithoutInitialization() {
            return accessibleContext;
        }
    }

    private static final class TitleBuilder {
        public StringBuilder sb = new StringBuilder();

        public TitleBuilder append(@Nullable final String s) {
            if (s == null || s.isEmpty()) {
                return this;
            }
            if (sb.length() > 0) {
                sb.append(" - ");
            }
            sb.append(s);
            return this;
        }
    }

    private static final Logger LOG = Logger.getInstance(DesktopIdeFrameImpl.class);

    private static final String FULL_SCREEN = "FullScreen";

    private static boolean myUpdatingTitle;

    private String myTitle;
    private String myFileTitle;
    private File myCurrentFile;

    private Project myProject;

    private IdeRootPane myRootPane;
    private BalloonLayout myBalloonLayout;
    private IdeFrameDecorator myFrameDecorator;
    private PropertyChangeListener myWindowsBorderUpdater;
    private boolean myRestoreFullScreen;

    private MyFrame myJFrame;

    private final TitlelessDecorator myTitlelessDecorator;

    public DesktopIdeFrameImpl(ActionManager actionManager, DataManager dataManager, Application application) {
        myJFrame = new MyFrame();
        myJFrame.toUIWindow().putUserData(IdeFrame.KEY, this);
        myJFrame.toUIWindow().addUserDataProvider(key -> getData(key));

        myJFrame.setTitle(FrameTitleUtil.buildTitle());
        myRootPane = createRootPane(actionManager, dataManager, application);
        myJFrame.setRootPane(myRootPane);
        myTitlelessDecorator = TitlelessDecorator.of(myRootPane);
        myJFrame.setBackground(UIUtil.getPanelBackground());
        AppIconUtil.updateWindowIcon(myJFrame);
        final Dimension size = ScreenUtil.getMainScreenBounds().getSize();

        size.width = Math.min(1400, size.width - 20);
        size.height = Math.min(1000, size.height - 40);

        myJFrame.setSize(size);
        myJFrame.setLocationRelativeTo(null);

        myJFrame.setFocusTraversalPolicy(new IdeFocusTraversalPolicy());

        setupCloseAction();
        MnemonicHelper.init(myJFrame);

        myBalloonLayout = new DesktopBalloonLayoutImpl(myRootPane, JBUI.insets(8));

        // to show window thumbnail under Macs
        // http://lists.apple.com/archives/java-dev/2009/Dec/msg00240.html
        if (Platform.current().os().isMac()) {
            myJFrame.setIconImage(null);
        }

        //MouseGestureManager.getInstance().add(this);

        myFrameDecorator = IdeFrameDecorator.decorate(this);

        myJFrame.addWindowStateListener(new WindowAdapter() {
            @Override
            public void windowStateChanged(WindowEvent e) {
                updateBorder();
            }
        });

        if (Platform.current().os().isWindows()) {
            myWindowsBorderUpdater = __ -> updateBorder();
            Toolkit.getDefaultToolkit().addPropertyChangeListener("win.xpstyle.themeActive", myWindowsBorderUpdater);
            if (!SystemInfo.isJavaVersionAtLeast(8, 0, 0)) {
                final Ref<Dimension> myDimensionRef = new Ref<>(new Dimension());
                final Alarm alarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD);
                final Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        if (myJFrame.isDisplayable() && !myJFrame.getSize().equals(myDimensionRef.get())) {
                            Rectangle bounds = myJFrame.getBounds();
                            bounds.width--;
                            myJFrame.setBounds(bounds);
                            bounds.width++;
                            myJFrame.setBounds(bounds);
                            myDimensionRef.set(myJFrame.getSize());
                        }
                        alarm.addRequest(this, 50);
                    }
                };
                alarm.addRequest(runnable, 50);
            }
        }
        // UIUtil.suppressFocusStealing();
    }

    private void updateBorder() {
        int state = myJFrame.getExtendedState();
        if (!WindowManager.getInstance().isFullScreenSupportedInCurrentOS() || !Platform.current().os().isWindows() || myRootPane == null) {
            return;
        }

        myRootPane.setBorder(null);
        boolean isNotClassic = Boolean.parseBoolean(String.valueOf(Toolkit.getDefaultToolkit().getDesktopProperty("win.xpstyle.themeActive")));
        if (isNotClassic && (state & JFrame.MAXIMIZED_BOTH) != 0) {
            IdeFrame[] projectFrames = WindowManager.getInstance().getAllProjectFrames();
            GraphicsDevice device = ScreenUtil.getScreenDevice(myJFrame.getBounds());

            for (IdeFrame frame : projectFrames) {
                if (frame == this) {
                    continue;
                }

                IdeFrameEx ideFrameEx = (IdeFrameEx) frame;
                if (ideFrameEx.isInFullScreen() && ScreenUtil.getScreenDevice(TargetAWT.to(ideFrameEx.getWindow()).getBounds()) == device) {
                    Insets insets = ScreenUtil.getScreenInsets(device.getDefaultConfiguration());
                    int mask = SideBorder.NONE;
                    if (insets.top != 0) {
                        mask |= SideBorder.TOP;
                    }
                    if (insets.left != 0) {
                        mask |= SideBorder.LEFT;
                    }
                    if (insets.bottom != 0) {
                        mask |= SideBorder.BOTTOM;
                    }
                    if (insets.right != 0) {
                        mask |= SideBorder.RIGHT;
                    }
                    myRootPane.setBorder(new SideBorder(JBColor.BLACK, mask, 3));
                    break;
                }
            }
        }
    }

    protected IdeRootPane createRootPane(ActionManager actionManager, DataManager dataManager, Application application) {
        return new IdeRootPane(actionManager, dataManager, application, this);
    }

    public TitlelessDecorator getTitlelessDecorator() {
        return myTitlelessDecorator;
    }

    @Override
    public JComponent getComponent() {
        return myJFrame.getRootPane();
    }

    @Nonnull
    @Override
    public consulo.ui.Window getWindow() {
        return myJFrame.toUIWindow();
    }

    private void setupCloseAction() {
        myJFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        myJFrame.addWindowListener(new WindowAdapter() {
            @Override
            @RequiredUIAccess
            public void windowClosing(@Nonnull final WindowEvent e) {
                if (isTemporaryDisposed()) {
                    return;
                }

                ProjectManagerEx projectManager = (ProjectManagerEx) ProjectManager.getInstance();

                final Project[] openProjects = projectManager.getOpenProjects();
                if (openProjects.length > 1 || openProjects.length == 1 && TopApplicationMenuUtil.isMacSystemMenu) {
                    if (myProject != null && myProject.isOpen()) {
                        projectManager.closeAndDispose(myProject);
                    }

                    Application.get().getMessageBus().syncPublisher(AppLifecycleListener.class).projectFrameClosed();

                    WelcomeFrameManager.getInstance().showIfNoProjectOpened();
                }
                else {
                    UIAccess uiAccess = UIAccess.current();

                    Task.Modal.queue(myProject, "Closing Project...", false, indicator -> {
                        for (Project project : openProjects) {
                            projectManager.closeAndDisposeAsync(project, uiAccess).getResultSync();
                        }
                    }, () -> {
                        Application.get().exit();
                    });
                }
            }
        });
    }

    @Override
    public StatusBar getStatusBar() {
        return myRootPane == null ? null : myRootPane.getStatusBar();
    }

    @Override
    public void setFrameTitle(final String text) {
        myJFrame.setTitleWithoutCheck(text);
    }

    @Override
    public void setFileTitle(@Nullable final String fileTitle, @Nullable File file) {
        myFileTitle = fileTitle;
        myCurrentFile = file;
        updateTitle();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <E extends IdeRootPaneNorthExtension> E getNorthExtension(@Nonnull Class<? extends E> extensioClass) {
        return (E) myRootPane.findExtension(extensioClass);
    }

    private void updateTitle() {
        updateTitle(myJFrame, myTitle, myFileTitle, myCurrentFile);
    }

    public static void updateTitle(JFrame frame, final String title, final String fileTitle, final File currentFile) {
        if (myUpdatingTitle) {
            return;
        }

        try {
            myUpdatingTitle = true;

            frame.getRootPane().putClientProperty("Window.documentFile", currentFile);

            final String applicationName = FrameTitleUtil.buildTitle();
            final TitleBuilder titleBuilder = new TitleBuilder();
            if (Platform.current().os().isMac()) {
                boolean addAppName = StringUtil.isEmpty(title) || ProjectManager.getInstance().getOpenProjects().length == 0;
                titleBuilder.append(fileTitle).append(title).append(addAppName ? applicationName : null);
            }
            else {
                titleBuilder.append(title).append(fileTitle);

                // only append if title not equal app name
                if (!Objects.equals(title, applicationName)) {
                    titleBuilder.append(applicationName);
                }
            }

            frame.setTitle(titleBuilder.sb.toString());
        }
        finally {
            myUpdatingTitle = false;
        }
    }

    @Override
    public void updateView() {
        ((IdeRootPane) myJFrame.getRootPane()).updateToolbar();
        ((IdeRootPane) myJFrame.getRootPane()).updateMainMenuActions();
        ((IdeRootPane) myJFrame.getRootPane()).updateNorthComponents();
    }

    @Override
    public AccessibleContext getCurrentAccessibleContext() {
        return myJFrame.getAccessibleContextWithoutInitialization();
    }

    private Object getData(@Nonnull Key<?> dataId) {
        if (Project.KEY == dataId) {
            if (myProject != null) {
                return myProject.isInitialized() ? myProject : null;
            }
        }

        if (IdeFrame.KEY == dataId) {
            return this;
        }

        return null;
    }

    public void setProject(final Project project) {
        if (WindowManager.getInstance().isFullScreenSupportedInCurrentOS() && myProject != project && project != null) {
            myRestoreFullScreen = myProject == null && shouldRestoreFullScreen(project);

            if (myProject != null) {
                storeFullScreenStateIfNeeded(false); // disable for old project
            }
        }

        myProject = project;
        if (project != null) {
            // see initialize()
        }
        else {
            if (myRootPane != null) {  //already disposed
                myRootPane.deinstallNorthComponents();
            }
        }

        if (myJFrame.isVisible() && myRestoreFullScreen) {
            toggleFullScreen(true);
            myRestoreFullScreen = false;
        }
    }

    private void installPopupForStatusBar() {
        JComponent component = Objects.requireNonNull(getStatusBar()).getComponent();
        PopupHandler.installPopupHandler(component, StatusBarWidgetsActionGroup.GROUP_ID, ActionPlaces.STATUS_BAR_PLACE);
    }

    @RequiredUIAccess
    @Override
    public void initialize() {
        if (myRootPane != null) {
            myRootPane.installNorthComponents(myProject, myTitlelessDecorator);
            myProject.getMessageBus().connect().subscribe(StatusBarInfo.class, myRootPane.getStatusBar());
        }

        installPopupForStatusBar();
    }

    @Override
    public Project getProject() {
        return myProject;
    }

    private boolean isTemporaryDisposed() {
        return myRootPane != null && myRootPane.getClientProperty(ScreenUtil.DISPOSE_TEMPORARY) != null;
    }

    @Override
    public void storeFullScreenStateIfNeeded() {
        if (myFrameDecorator != null) {
            storeFullScreenStateIfNeeded(myFrameDecorator.isInFullScreen());
        }
    }

    @Override
    public void storeFullScreenStateIfNeeded(boolean state) {
        if (!WindowManager.getInstance().isFullScreenSupportedInCurrentOS()) {
            return;
        }

        if (myProject != null) {
            PropertiesComponent.getInstance(myProject).setValue(FULL_SCREEN, state);
            myJFrame.doLayout();
        }
    }

    private static boolean shouldRestoreFullScreen(@Nullable Project project) {
        return WindowManager.getInstance().isFullScreenSupportedInCurrentOS() &&
            project != null &&
            (SHOULD_OPEN_IN_FULL_SCREEN.get(project) == Boolean.TRUE || PropertiesComponent.getInstance(project).getBoolean(FULL_SCREEN));
    }

    @Nonnull
    @Override
    public IdeFrameState getFrameState() {
        Window window = TargetAWT.to(getWindow());
        if (!(window instanceof Frame frame)) {
            return IdeFrameState.EMPTY;
        }

        int extendedState = frame.getExtendedState();
        if (Platform.current().os().isMac()) {
            extendedState = AWTAccessorHacking.getExtendedStateFromPeer(frame);
        }

        boolean isMaximized = extendedState == Frame.MAXIMIZED_BOTH;
        boolean isFullScreen = isInFullScreen();
        Rectangle bounds = frame.getBounds();
        return new IdeFrameState(bounds.x, bounds.y, bounds.width, bounds.height, isMaximized, isFullScreen);
    }

    @Override
    public Rectangle2D suggestChildFrameBounds() {
        //todo [kirillk] a dummy implementation
        final Rectangle b = myJFrame.getBounds();
        b.x += 100;
        b.width -= 200;
        b.y += 100;
        b.height -= 200;
        return TargetAWT.from(b);
    }

    @Override
    public final BalloonLayout getBalloonLayout() {
        return myBalloonLayout;
    }

    @Override
    public boolean isInFullScreen() {
        return myFrameDecorator != null && myFrameDecorator.isInFullScreen();
    }

    @Nonnull
    @Override
    public ActionCallback toggleFullScreen(boolean state) {

        if (temporaryFixForIdea156004(state)) {
            return ActionCallback.DONE;
        }

        if (myFrameDecorator != null) {
            return myFrameDecorator.toggleFullScreen(state);
        }
        IdeFrame[] frames = WindowManager.getInstance().getAllProjectFrames();
        for (IdeFrame frame : frames) {
            ((DesktopIdeFrameImpl) frame).updateBorder();
        }

        return ActionCallback.DONE;
    }

    @Override
    public void activate() {
        Window awtWindow = TargetAWT.to(getWindow());
        if (awtWindow instanceof Frame frame) {
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    int extendedState = frame.getExtendedState();
                    if (BitUtil.isSet(extendedState, Frame.ICONIFIED)) {
                        extendedState = BitUtil.set(extendedState, Frame.ICONIFIED, false);
                        frame.setExtendedState(extendedState);
                    }

                    // fixme [vistall] dirty hack - show frame on top
                    frame.setAlwaysOnTop(true);
                    frame.setAlwaysOnTop(false);
                    frame.requestFocus();
                }
            };
            //noinspection SSBasedInspection
            SwingUtilities.invokeLater(runnable);
        }
    }

    private boolean temporaryFixForIdea156004(final boolean state) {
        if (Platform.current().os().isMac()) {
            try {
                Field modalBlockerField = Window.class.getDeclaredField("modalBlocker");
                modalBlockerField.setAccessible(true);
                final Window modalBlocker = (Window) modalBlockerField.get(myJFrame);
                if (modalBlocker != null) {
                    ApplicationManager.getApplication().invokeLater(() -> toggleFullScreen(state), IdeaModalityState.nonModal());
                    return true;
                }
            }
            catch (NoSuchFieldException | IllegalAccessException e) {
                LOG.error(e);
            }
        }
        return false;
    }
}
