// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ide.runAnything;

import consulo.dataContext.DataContext;
import consulo.ide.internal.RunAnythingNotificationGroupContributor;
import consulo.ide.localize.IdeLocalize;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.ui.notification.Notification;
import consulo.project.ui.notification.NotificationService;
import jakarta.annotation.Nonnull;

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
        protected Notification.Builder myBuilder;
        private final DataContext myDataContext;
        private final V myValue;

        public NotificationBuilder(DataContext dataContext, V value) {
            myBuilder = NotificationService.getInstance()
                .newInfo(RunAnythingNotificationGroupContributor.GROUP);
            this.myDataContext = dataContext;
            this.myValue = value;
        }

        public void setTitle(@Nonnull LocalizeValue title) {
            myBuilder.title(title);
        }

        public void setSubtitle(@Nonnull LocalizeValue subtitle) {
            myBuilder.subtitle(subtitle);
        }

        public void setContent(@Nonnull LocalizeValue content) {
            myBuilder.content(content);
        }

        /**
         * Adds an action to the notification.
         *
         * @param name    the action text (annotated with {@code @NlsActions.ActionText})
         * @param perform the action to be executed when selected.
         */
        public void action(LocalizeValue name, Runnable perform) {
            myBuilder.addClosingAction(name, perform);
        }

        public Notification build() {
            return myBuilder.create();
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
     * In the initialization, we register a default notification for ERROR status.
     * Note that {@code getCommand(V)} is assumed to be defined (likely in the parent class).
     */
    public RunAnythingNotifiableProvider() {
        // Register a configurator for the ERROR status notifications.
        notification(
            ExecutionStatus.ERROR,
            builder -> {
                // getCommand(builder.value) must be defined in RunAnythingProviderBase.
                builder.myBuilder
                    .title(IdeLocalize.runAnythingNotificationWarningTitle())
                    .content(IdeLocalize.runAnythingNotificationWarningContent(getCommand(builder.myValue)));
            }
        );
    }
}
