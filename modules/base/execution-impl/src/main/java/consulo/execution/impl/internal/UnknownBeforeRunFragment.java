package consulo.execution.impl.internal;

import consulo.dataContext.DataContext;
import consulo.execution.BeforeRunFragment;
import consulo.execution.configuration.RunConfiguration;
import consulo.execution.localize.ExecutionLocalize;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.UIAccess;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * Presentation of {@link UnknownBeforeRunTask}s; not an extension.
 */
public final class UnknownBeforeRunFragment implements BeforeRunFragment<UnknownBeforeRunTask> {
    public static final UnknownBeforeRunFragment INSTANCE = new UnknownBeforeRunFragment();

    @Override
    public String getId() {
        return "Unknown";
    }

    @Override
    public LocalizeValue getName() {
        return ExecutionLocalize.beforeLaunchRunUnknownTask();
    }

    @Override
    public @Nullable Image getIcon(RunConfiguration configuration) {
        return PlatformIconGroup.actionsHelp();
    }

    @Override
    public LocalizeValue getDescription(UnknownBeforeRunTask task) {
        return LocalizeValue.join(ExecutionLocalize.beforeLaunchRunUnknownTask(), LocalizeValue.space(), LocalizeValue.of(task.getId()));
    }

    @Override
    public Class<UnknownBeforeRunTask> getStateClass() {
        return UnknownBeforeRunTask.class;
    }

    @Override
    public @Nullable UnknownBeforeRunTask createTask(RunConfiguration configuration) {
        return null;
    }

    @Override
    public boolean canExecuteTask(RunConfiguration configuration, UnknownBeforeRunTask task) {
        return false;
    }

    @Override
    public CompletableFuture<?> executeAsync(
        UIAccess uiAccess,
        DataContext context,
        RunConfiguration configuration,
        ExecutionEnvironment env,
        UnknownBeforeRunTask task
    ) {
        return CompletableFuture.completedFuture(null);
    }
}
