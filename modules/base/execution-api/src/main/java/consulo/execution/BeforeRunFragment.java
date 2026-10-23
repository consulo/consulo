package consulo.execution;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.dataContext.DataContext;
import consulo.disposer.Disposable;
import consulo.execution.configuration.RunConfiguration;
import consulo.execution.configuration.fragment.RunConfigurationFragmentEditor;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.util.xml.serializer.XmlSerializer;
import org.jdom.Element;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * A kind of task run before a configuration launches ("Build", "Run Another Configuration", ...). The task itself is a
 * {@link BeforeRunTask} state POJO; its settings are edited by {@link #createEditor} in a dialog.
 */
@ExtensionAPI(ComponentScope.PROJECT)
public interface BeforeRunFragment<T extends BeforeRunTask> {
    /**
     * Stable id; also the name the task had in the old {@code <method><option name="..."/>} format.
     */
    String getId();

    LocalizeValue getName();

    @Nullable
    Image getIcon(RunConfiguration configuration);

    default LocalizeValue getDescription(T task) {
        return getName();
    }

    default @Nullable Image getTaskIcon(RunConfiguration configuration, T task) {
        return getIcon(configuration);
    }

    Class<T> getStateClass();

    /**
     * A new task for the configuration, {@code null} when the task does not apply to it.
     */
    @Nullable
    T createTask(RunConfiguration configuration);

    /**
     * Whether a configuration can have the task at most once.
     */
    default boolean isSingleton() {
        return false;
    }

    default boolean canExecuteTask(RunConfiguration configuration, T task) {
        return true;
    }

    /**
     * Whether the task has settings, edited by {@link #createEditor} in a dialog.
     */
    default boolean isConfigurable() {
        return false;
    }

    /**
     * The editor of the task's settings, shown in a dialog; {@code null} when the task has no settings.
     */
    @RequiredUIAccess
    default @Nullable RunConfigurationFragmentEditor<T> createEditor(RunConfiguration configuration, Disposable parent) {
        return null;
    }

    /**
     * Reads a task from the old {@code <option name="id" .../>} element.
     */
    default void readLegacy(Element element, T task) {
        String enabled = element.getAttributeValue("enabled");
        if (enabled == null) {
            enabled = element.getAttributeValue("value");
        }
        XmlSerializer.deserializeInto(task, element);
        task.setEnabled(enabled == null || Boolean.parseBoolean(enabled));
    }

    CompletableFuture<?> executeAsync(UIAccess uiAccess, DataContext context, RunConfiguration configuration, ExecutionEnvironment env, T task);

    @SuppressWarnings("unchecked")
    static <T extends BeforeRunTask> @Nullable BeforeRunFragment<T> find(Project project, Class<T> stateClass) {
        return project.getExtensionPoint(BeforeRunFragment.class).findFirstSafe(it -> it.getStateClass() == stateClass);
    }

    @SuppressWarnings("unchecked")
    static <T extends BeforeRunTask> @Nullable BeforeRunFragment<T> find(Project project, T task) {
        return (BeforeRunFragment<T>) find(project, task.getClass());
    }

    static @Nullable BeforeRunFragment<?> find(Project project, String id) {
        return project.getExtensionPoint(BeforeRunFragment.class).findFirstSafe(it -> id.equals(it.getId()));
    }
}
