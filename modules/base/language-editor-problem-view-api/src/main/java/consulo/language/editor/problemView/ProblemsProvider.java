package consulo.language.editor.problemView;

import consulo.disposer.Disposable;
import consulo.project.Project;

public interface ProblemsProvider extends Disposable {
    @Override
    default void dispose() {
    }

    /**
     * The project that the problem provider belongs to.
     */
    
    Project getProject();
}
