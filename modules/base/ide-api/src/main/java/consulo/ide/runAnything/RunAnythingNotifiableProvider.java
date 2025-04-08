// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ide.runAnything;

import consulo.dataContext.DataContext;
import consulo.ide.internal.RunAnythingNotificationGroupContributor;
import consulo.ide.localize.IdeLocalize;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.project.ui.notification.Notification;
import consulo.project.ui.notification.NotificationType;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public abstract class RunAnythingNotifiableProvider<V> extends RunAnythingProviderBase<V> {
    // Map to hold notification configurators keyed by their execution status.
    private final Map<ExecutionStatus, Consumer<NotificationBuilder>> notificationConfigurators = new LinkedHashMap<>();

    /**
     * Runs an activity silently.
     *
     * @param dataContext 'Run Anything' data context
     * @param value       value argument for the run action
     * @return true if succeeded, false if failed
     */
    protected abstract boolean run(DataContext dataContext, V value);

    @Override
    public void execute(DataContext dataContext, V value) {
        try {
            if (run(dataContext, value)) {
                notifyNotificationIfNeeded(ExecutionStatus.SUCCESS, dataContext, value);
            }
            else {
                notifyNotificationIfNeeded(ExecutionStatus.ERROR, dataContext, value);
            }
        }
        catch (Throwable ex) {
            notifyNotificationIfNeeded(ExecutionStatus.ERROR, dataContext, value);
            throw ex;
        }
    }

    private void notifyNotificationIfNeeded(ExecutionStatus status, DataContext dataContext, V value) {
        Consumer<NotificationBuilder> configure = notificationConfigurators.get(status);
        if (configure == null) {
            return;
        }
        NotificationBuilder builder = new NotificationBuilder(dataContext, value);
        // Apply the configuration to the builder.
        configure.accept(builder);
        Notification notification = builder.build();
        notification.notify(dataContext.getData(Project.KEY));
    }

    /**
     * Registers a notification configurator to be used when execution completes with a particular status.
     * This overload defaults to the SUCCESS status.
     *
     * @param configure a consumer to configure the notification builder.
     */
    protected void notification(Consumer<NotificationBuilder> configure) {
        notification(ExecutionStatus.SUCCESS, configure);
    }

    /**
     * Registers a notification configurator to be used when execution completes with a particular status.
     *
     * @param after     the execution status for which this notification should be shown.
     * @param configure a consumer to configure the notification builder.
     */
    protected void notification(ExecutionStatus after, Consumer<NotificationBuilder> configure) {
        notificationConfigurators.put(after, configure);
    }

    /**
     * Builder class used to configure and build notifications.
     */
    protected class NotificationBuilder {
        private final DataContext dataContext;
        private final V value;
        private final ArrayList<ActionData> actions = new ArrayList<>();

        private LocalizeValue title = LocalizeValue.empty();
        private LocalizeValue subtitle = LocalizeValue.empty();
        private LocalizeValue content = LocalizeValue.empty();

        public NotificationBuilder(DataContext dataContext, V value) {
            this.dataContext = dataContext;
            this.value = value;
        }

        public void setTitle(LocalizeValue title) {
            this.title = title;
        }

        public void setSubtitle(LocalizeValue subtitle) {
            this.subtitle = subtitle;
        }

        public void setContent(LocalizeValue content) {
            this.content = content;
        }

        /**
         * Adds an action to the notification.
         *
         * @param name    the action text (annotated with {@code @NlsActions.ActionText})
         * @param perform the action to be executed when selected.
         */
        public void action(LocalizeValue name, Runnable perform) {
            actions.add(new ActionData(name, perform));
        }

        public Notification build() {
            Notification notification = RunAnythingNotificationGroupContributor.GROUP.createNotification(content.get(), NotificationType.INFORMATION)
                .setIcon(PlatformIconGroup.actionsRun_anything())
                .setTitle(title.get(), subtitle.get());
            for (ActionData actionData : actions) {
                AnAction action = new AnAction(actionData.name) {
                    @RequiredUIAccess
                    @Override
                    public void actionPerformed(@Nonnull AnActionEvent e) {
                        actionData.perform.run();
                        notification.expire();
                    }
                };
                notification.addAction(action);
            }
            return notification;
        }
    }

    /**
     * Enumeration to represent the execution status.
     */
    protected enum ExecutionStatus {
        SUCCESS,
        ERROR
    }

    /**
     * Data holder for action information.
     */
    private static record ActionData(LocalizeValue name, Runnable perform) {
    }

    /**
     * In the initialization, we register a default notification for ERROR status.
     * Note that {@code getCommand(V)} is assumed to be defined (likely in the parent class).
     */
    public RunAnythingNotifiableProvider() {
        // Register a configurator for the ERROR status notifications.
        notification(ExecutionStatus.ERROR, builder -> {
            builder.setTitle(IdeLocalize.runAnythingNotificationWarningTitle());
            // getCommand(builder.value) must be defined in RunAnythingProviderBase.
            builder.setContent(IdeLocalize.runAnythingNotificationWarningContent(getCommand(builder.value)));
        });
    }
}
