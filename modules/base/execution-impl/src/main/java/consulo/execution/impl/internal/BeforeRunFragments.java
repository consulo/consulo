package consulo.execution.impl.internal;

import consulo.execution.BeforeRunFragment;
import consulo.execution.BeforeRunTask;
import consulo.project.Project;
import org.jspecify.annotations.Nullable;

public final class BeforeRunFragments {
    private BeforeRunFragments() {
    }

    /**
     * The fragment owning a task, {@link UnknownBeforeRunFragment} for a task whose fragment is not loaded.
     */
    @SuppressWarnings("unchecked")
    public static @Nullable BeforeRunFragment<BeforeRunTask> find(Project project, BeforeRunTask task) {
        if (task instanceof UnknownBeforeRunTask) {
            return (BeforeRunFragment<BeforeRunTask>) (BeforeRunFragment<?>) UnknownBeforeRunFragment.INSTANCE;
        }
        return (BeforeRunFragment<BeforeRunTask>) (BeforeRunFragment<?>) BeforeRunFragment.find(project, task);
    }

    /**
     * Identity of a task kind: two tasks with the same identity come from the same fragment.
     */
    public static Object identity(BeforeRunTask task) {
        return task instanceof UnknownBeforeRunTask unknown ? unknown.getId() : task.getClass();
    }
}
