// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.desktop.awt.internal.notification;

import consulo.application.AllIcons;
import consulo.application.ApplicationManager;
import consulo.application.internal.MessagePool;
import consulo.application.internal.MessagePoolListener;
import consulo.desktop.awt.uiOld.BalloonLayoutData;
import consulo.disposer.Disposer;
import consulo.externalService.impl.internal.errorReport.IdeErrorsDialog;
import consulo.externalService.impl.internal.errorReport.ReportMessages;
import consulo.externalService.localize.ExternalServiceLocalize;
import consulo.logging.internal.LogMessage;
import consulo.project.Project;
import consulo.project.ui.notification.Notification;
import consulo.project.ui.notification.NotificationAction;
import consulo.project.ui.notification.NotificationType;
import consulo.project.ui.wm.*;
import consulo.ui.UIAccessScheduler;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.awt.ClickListener;
import consulo.ui.ex.awt.JBCurrentTheme;
import consulo.ui.ex.awt.NonOpaquePanel;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.popup.Balloon;
import consulo.util.lang.ref.Ref;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.concurrent.TimeUnit;

public final class IdeMessagePanel extends NonOpaquePanel implements MessagePoolListener, IconLikeCustomStatusBarWidget {
    private final IdeErrorsIcon myIcon;
    private final IdeFrame myFrame;
    private final MessagePool myMessagePool;
    private final Project myProject;
    @Nonnull
    private final StatusBarWidgetFactory myFactory;

    private Balloon myBalloon;
    private IdeErrorsDialog myDialog;
    private boolean myOpeningInProgress;
    private boolean myNotificationPopupAlreadyShown;

    public IdeMessagePanel(
        Project project,
        @Nonnull StatusBarWidgetFactory factory,
        @Nullable IdeFrame frame,
        @Nonnull MessagePool messagePool
    ) {
        super(new BorderLayout());
        myProject = project;
        myFactory = factory;

        myIcon = new IdeErrorsIcon(frame != null);
        myIcon.setVerticalAlignment(SwingConstants.CENTER);
        add(myIcon, BorderLayout.CENTER);
        new ClickListener() {
            @Override
            public boolean onClick(@Nonnull MouseEvent event, int clickCount) {
                openErrorsDialog(null);
                return true;
            }
        }.installOn(myIcon);

        myFrame = frame;

        myMessagePool = messagePool;
        messagePool.addListener(this);

        updateIconAndNotify();
    }

    @Nonnull
    @Override
    public String getId() {
        return myFactory.getId();
    }

    @Override
    public WidgetPresentation getPresentation() {
        return null;
    }

    @Override
    public void dispose() {
        UIUtil.dispose(myIcon);
        myMessagePool.removeListener(this);
    }

    @Override
    public void install(@Nonnull StatusBar statusBar) {
    }

    @Override
    public JComponent getComponent() {
        return this;
    }

    public void openErrorsDialog(@Nullable LogMessage message) {
        if (myDialog != null) {
            return;
        }
        if (myOpeningInProgress) {
            return;
        }
        myOpeningInProgress = true;

        new Runnable() {
            @Override
            public void run() {
                if (!isOtherModalWindowActive()) {
                    try {
                        doOpenErrorsDialog(message);
                    }
                    finally {
                        myOpeningInProgress = false;
                    }
                }
                else if (myDialog == null) {
                    UIAccessScheduler scheduler = myProject.getUIAccess().getScheduler();
                    scheduler.schedule(this, 300L, TimeUnit.MILLISECONDS);
                }
            }
        }.run();
    }

    private void doOpenErrorsDialog(@Nullable LogMessage message) {
        Project project = myFrame != null ? myFrame.getProject() : null;
        myDialog = new IdeErrorsDialog(myMessagePool, project, message) {
            @Override
            protected void dispose() {
                super.dispose();
                myDialog = null;
                updateIconAndNotify();
            }

            @Override
            protected void updateOnSubmit() {
                super.updateOnSubmit();
                updateIcon(myMessagePool.getState());
            }
        };
        myDialog.show();
    }

    private void updateIcon(MessagePool.State state) {
        UIUtil.invokeLaterIfNeeded(() -> {
            myIcon.setState(state);
            setVisible(state != MessagePool.State.NoErrors);
        });
    }

    @Override
    public void newEntryAdded() {
        updateIconAndNotify();
    }

    @Override
    public void poolCleared() {
        updateIconAndNotify();
    }

    @Override
    public void entryWasRead() {
        updateIconAndNotify();
    }

    private boolean isOtherModalWindowActive() {
        Window activeWindow = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
        return activeWindow instanceof JDialog && ((JDialog) activeWindow).isModal()
            && (myDialog == null || myDialog.getWindow() != activeWindow);
    }

    private void updateIconAndNotify() {
        MessagePool.State state = myMessagePool.getState();
        updateIcon(state);

        if (state == MessagePool.State.NoErrors) {
            myNotificationPopupAlreadyShown = false;
            if (myBalloon != null) {
                Disposer.dispose(myBalloon);
            }
        }
        else if (state == MessagePool.State.UnreadErrors && !myNotificationPopupAlreadyShown && isActive(myFrame)) {
            Project project = myFrame.getProject();
            if (project != null) {
                ApplicationManager.getApplication().invokeLater(() -> showErrorNotification(project), project.getDisposed());
                myNotificationPopupAlreadyShown = true;
            }
        }
    }

    private static boolean isActive(IdeFrame frame) {
        return frame.isActive();
    }

    private void showErrorNotification(@Nonnull Project project) {
        String title = ExternalServiceLocalize.errorNewNotificationTitle().get();
        Notification notification =
            new Notification(ReportMessages.GROUP, AllIcons.Ide.FatalError, title, null, null, NotificationType.ERROR, null);
        notification.addAction(new NotificationAction(ExternalServiceLocalize.errorNewNotificationLink()) {
            @RequiredUIAccess
            @Override
            public void actionPerformed(@Nonnull AnActionEvent e, @Nonnull Notification notification) {
                notification.expire();
                openErrorsDialog(null);
            }
        });

        BalloonLayout layout = myFrame.getBalloonLayout();
        assert layout != null : myFrame;

        BalloonLayoutData layoutData = BalloonLayoutData.createEmpty();
        layoutData.fadeoutTime = 5000;
        layoutData.textColor = JBCurrentTheme.Notification.Error.FOREGROUND;
        layoutData.fillColor = JBCurrentTheme.Notification.Error.BACKGROUND;
        layoutData.borderColor = JBCurrentTheme.Notification.Error.BORDER_COLOR;

        assert myBalloon == null;
        myBalloon = NotificationsManagerImpl.getNotificationsManager()
            .createBalloon(myFrame, notification, false, false, new Ref<>(layoutData), project);
        Disposer.register(myBalloon, () -> myBalloon = null);
        layout.add(myBalloon);
    }
}