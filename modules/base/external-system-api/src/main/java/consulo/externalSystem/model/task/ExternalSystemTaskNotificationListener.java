package consulo.externalSystem.model.task;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import jakarta.annotation.Nonnull;

/**
 * Defines contract for callback to listen external task notifications.
 *
 * @author Denis Zhdanov
 * @since 2011-11-10
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface ExternalSystemTaskNotificationListener {
    ExtensionPointName<ExternalSystemTaskNotificationListener> EP_NAME
        = ExtensionPointName.create(ExternalSystemTaskNotificationListener.class);

    /**
     * Notifies that task with the given id is queued for the execution.
     * <p/>
     * 'Queued' here means that intellij process-local codebase receives request to execute the target task and even has not been
     * sent it to the slave gradle api process.
     *
     * @param id target task's id
     */
    void onQueued(@Nonnull ExternalSystemTaskId id);

    /**
     * Notifies that task with the given id is about to be started.
     *
     * @param id target task's id
     */
    void onStart(@Nonnull ExternalSystemTaskId id);

    /**
     * Notifies about processing state change of task with the given id.
     *
     * @param event event that holds information about processing change state of the
     *              {@link ExternalSystemTaskNotificationEvent#getId() target task}
     */
    void onStatusChange(@Nonnull ExternalSystemTaskNotificationEvent event);

    /**
     * Notifies about text written to stdout/stderr during the task execution
     *
     * @param id     id of the task being executed
     * @param text   text produced by external system during the target task execution
     * @param stdOut flag which identifies output type (stdout or stderr)
     */
    void onTaskOutput(@Nonnull ExternalSystemTaskId id, @Nonnull String text, boolean stdOut);

    /**
     * Notifies that task with the given id is finished.
     *
     * @param id target task's id
     */
    void onEnd(@Nonnull ExternalSystemTaskId id);

    /**
     * Notifies that task with the given id is finished successfully.
     *
     * @param id target task's id
     */
    void onSuccess(@Nonnull ExternalSystemTaskId id);

    /**
     * Notifies that task with the given id is failed.
     *
     * @param id target task's id
     * @param e  failure exception
     */
    void onFailure(@Nonnull ExternalSystemTaskId id, @Nonnull Exception e);
}
