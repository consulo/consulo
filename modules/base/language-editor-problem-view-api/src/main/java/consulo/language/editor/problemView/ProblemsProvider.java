package consulo.language.editor.problemView;

import consulo.disposer.Disposable;
import consulo.project.Project;
import jakarta.annotation.Nonnull;

public interface ProblemsProvider extends Disposable {
    @Override
    default void dispose() {
    }

    /**
     * The project that the problem provider belongs to.
     */
    @Nonnull
    Project getProject();
}
