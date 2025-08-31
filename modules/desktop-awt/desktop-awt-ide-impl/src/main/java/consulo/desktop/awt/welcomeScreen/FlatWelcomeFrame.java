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
package consulo.desktop.awt.welcomeScreen;

import consulo.application.Application;
import consulo.application.ui.ApplicationWindowStateService;
import consulo.desktop.awt.ui.impl.window.JFrameAsUIWindow;
import consulo.desktop.awt.ui.util.AppIconUtil;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ide.impl.application.FrameTitleUtil;
import consulo.ide.impl.idea.openapi.wm.impl.IdeGlassPaneImpl;
import consulo.ide.impl.idea.util.ui.accessibility.AccessibleContextAccessor;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.project.ui.wm.BalloonLayout;
import consulo.project.ui.wm.WelcomeFrameManager;
import consulo.ui.Point2D;
import consulo.ui.Rectangle2D;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.touchBar.TouchBarController;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.MnemonicHelper;
import consulo.ui.ex.awt.TitlelessDecorator;
import consulo.ui.ex.awt.util.ScreenUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import jakarta.annotation.Nullable;

import javax.accessibility.AccessibleContext;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * @author Konstantin Bulenkov
 */
public class FlatWelcomeFrame extends JFrameAsUIWindow implements Disposable, AccessibleContextAccessor {
    private final Runnable myClearInstance;
    
    private WelcomeDesktopBalloonLayoutImpl myBalloonLayout;

    private boolean myDisposed;

    private Disposable muTouchBarDisposable;

    @RequiredUIAccess
    public FlatWelcomeFrame(Application application, Runnable clearInstance) {
        myClearInstance = clearInstance;
        JRootPane rootPane = getRootPane();

        TitlelessDecorator titlelessDecorator = TitlelessDecorator.of(getRootPane(), TitlelessDecorator.WELCOME_WINDOW);

        FlatWelcomeScreen screen = new FlatWelcomeScreen(this, titlelessDecorator);

        IdeGlassPaneImpl glassPane = new IdeGlassPaneImpl(rootPane);

        setGlassPane(glassPane);
        glassPane.setVisible(false);
        setContentPane(screen);
        setDefaultTitle();
        AppIconUtil.updateWindowIcon(this);
        setSize(TargetAWT.to(WelcomeFrameManager.getDefaultWindowSize()));
        setResizable(false);
        Point location = TargetAWT.to(ApplicationWindowStateService.getInstance().getLocation(WelcomeFrameManager.DIMENSION_KEY));
        Rectangle screenBounds = ScreenUtil.getScreenRectangle(location != null ? location : new Point(0, 0));
        setLocation(new Point(screenBounds.x + (screenBounds.width - getWidth()) / 2, screenBounds.y + (screenBounds.height - getHeight()) / 3));

        myBalloonLayout = new WelcomeDesktopBalloonLayoutImpl(rootPane,
            JBUI.insets(8),
            screen.getEventListener(),
            screen.getEventLocation()
        );

        screen.setWelcomeDesktopBalloonLayout(myBalloonLayout);

        setupCloseAction(this);
        MnemonicHelper.init(this);
        Disposer.register(application, this);

        titlelessDecorator.install(this);

        muTouchBarDisposable = TouchBarController.getInstance().showWindowActions(getContentPane());
    }

    static void setupCloseAction(final JFrame frame) {
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                saveLocation(frame.getBounds());

                frame.dispose();

                if (ProjectManager.getInstance().getOpenProjects().length == 0) {
                    Application.get().exit();
                }
            }
        });
    }

    public static void saveLocation(Rectangle location) {
        Point2D middle = new Point2D(location.x + location.width / 2, location.y = location.height / 2);
        ApplicationWindowStateService.getInstance().putLocation(WelcomeFrameManager.DIMENSION_KEY, middle);
    }

    public void setDefaultTitle() {
        setTitle(FrameTitleUtil.buildTitle());
    }

    @Override
    public void setVisible(boolean value) {
        if (myDisposed) {
            throw new IllegalArgumentException("Already disposed");
        }

        super.setVisible(value);

        if (!value) {
            Disposer.dispose(this);
        }
    }

    @Override
    public void dispose() {
        if (myDisposed) {
            return;
        }

        myDisposed = true;
        super.dispose();

        if (myBalloonLayout != null) {
            myBalloonLayout.dispose();
            myBalloonLayout = null;
        }

        if (muTouchBarDisposable != null) {
            muTouchBarDisposable.disposeWithTree();
            muTouchBarDisposable = null;
        }

        myClearInstance.run();
    }

    @Override
    public AccessibleContext getCurrentAccessibleContext() {
        return accessibleContext;
    }

    public BalloonLayout getBalloonLayout() {
        return myBalloonLayout;
    }

    public Rectangle2D suggestChildFrameBounds() {
        return TargetAWT.from(getBounds());
    }

    @Nullable
    public Project getProject() {
        return ProjectManager.getInstance().getDefaultProject();
    }

    public void setFrameTitle(String title) {
        setTitle(title);
    }

    public JComponent getComponent() {
        return getRootPane();
    }
}