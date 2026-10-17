// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.externalService.impl.internal.errorReport;

import consulo.application.internal.MessagePool;
import consulo.application.internal.MessagePoolListener;
import consulo.externalService.localize.ExternalServiceLocalize;
import consulo.localize.LocalizeValue;
import consulo.logging.internal.LogMessage;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.project.ui.notification.Notification;
import consulo.project.ui.notification.NotificationService;
import consulo.project.ui.wm.IdeFrame;
import consulo.project.ui.wm.StatusBar;
import consulo.project.ui.wm.StatusBarWidget;
import consulo.project.ui.wm.StatusBarWidgetFactory;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ClickEvent;
import consulo.ui.event.ComponentEventListener;
import consulo.ui.ex.dialog.Dialog;
import consulo.ui.ex.dialog.DialogService;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import org.jspecify.annotations.Nullable;

public final class IdeMessagePanel implements StatusBarWidget, StatusBarWidget.IconPresentation, MessagePoolListener {
    private final @Nullable IdeFrame myFrame;
    private final MessagePool myMessagePool;
    private final DialogService myDialogService;
    private final StatusBarWidgetFactory myFactory;
    private final Image myUnreadIcon;

    private volatile MessagePool.State myState = MessagePool.State.NoErrors;
    private @Nullable StatusBar myStatusBar;
    private final Object myNotificationLock = new Object();
    private @Nullable Notification myNotification;
    private volatile @Nullable Dialog myDialog;

    public IdeMessagePanel(StatusBarWidgetFactory factory, @Nullable IdeFrame frame, MessagePool messagePool, DialogService dialogService) {
        myFactory = factory;
        myFrame = frame;
        myMessagePool = messagePool;
        myDialogService = dialogService;
        myUnreadIcon = frame != null ? ImageEffects.blinking(PlatformIconGroup.ideFatalerror()) : PlatformIconGroup.ideFatalerror();

        messagePool.addListener(this);
        updateIconAndNotify();
    }

    @Override
    public String getId() {
        return myFactory.getId();
    }

    @Override
    public WidgetPresentation getPresentation() {
        return this;
    }

    @Override
    public void dispose() {
        myMessagePool.removeListener(this);
    }

    @Override
    public void install(StatusBar statusBar) {
        myStatusBar = statusBar;
    }

    @Override
    public @Nullable Image getIcon() {
        return switch (myState) {
            case NoErrors -> null;
            case ReadErrors -> PlatformIconGroup.ideFatalerror_read();
            case UnreadErrors -> myUnreadIcon;
        };
    }

    @Override
    public LocalizeValue getTooltipText() {
        return myState == MessagePool.State.NoErrors ? LocalizeValue.empty() : ExternalServiceLocalize.errorNotificationTooltip();
    }

    @Override
    public ComponentEventListener<Component, ClickEvent> getClickListener() {
        return event -> openErrorsDialog(null);
    }

    @RequiredUIAccess
    public void openErrorsDialog(@Nullable LogMessage message) {
        if (myDialog != null) {
            return;
        }

        Project project = myFrame != null ? myFrame.getProject() : null;
        myDialog = IdeErrorsDialogDescriptor.show(
            myDialogService,
            myMessagePool,
            project,
            message,
            () -> updateIcon(myMessagePool.getState()),
            () -> {
                myDialog = null;
                updateIconAndNotify();
            }
        );
    }

    private void updateIcon(MessagePool.State state) {
        myState = state;

        StatusBar statusBar = myStatusBar;
        if (statusBar != null) {
            statusBar.updateWidget(getId());
        }
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

    private void updateIconAndNotify() {
        updateIcon(myMessagePool.getState());

        Project project = myFrame != null ? myFrame.getProject() : null;
        if (project == null || project.isDisposed()) {
            return;
        }

        Notification toShow = null;
        Notification toExpire = null;
        synchronized (myNotificationLock) {
            Notification notification = myNotification;
            if (myMessagePool.getState() == MessagePool.State.UnreadErrors) {
                if (myDialog == null && (notification == null || notification.isExpired())) {
                    toShow = createErrorNotification();
                    myNotification = toShow;
                }
            }
            else if (notification != null) {
                myNotification = null;
                toExpire = notification;
            }
        }

        if (toShow != null) {
            toShow.notify(project);
        }
        if (toExpire != null) {
            toExpire.expire();
        }
    }

    private Notification createErrorNotification() {
        return NotificationService.getInstance()
            .newError(ReportMessages.GROUP)
            .icon(PlatformIconGroup.ideFatalerror())
            .title(ExternalServiceLocalize.errorNewNotificationTitle())
            .removeWhenExpired()
            .addClosingAction(ExternalServiceLocalize.errorNewNotificationLink(), () -> openErrorsDialog(null))
            .create();
    }
}
