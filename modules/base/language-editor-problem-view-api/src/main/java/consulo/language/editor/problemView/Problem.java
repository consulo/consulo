package consulo.language.editor.problemView;

import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

public interface Problem {
    /**
     * The problem provider that the problem belongs to.
     */
    ProblemsProvider getProvider();

    /**
     * One line description of the problem.
     */
    String getText();

    /**
     * A name used to group problems.
     */
    default @Nullable String getGroup() {
        return null;
    }

    /**
     * Detailed description of the problem if needed.
     */
    default @Nullable String getDescription() {
        return null;
    }

    /**
     * The problem icon.
     */
    default Image getIcon() {
        return HighlightDisplayLevel.ERROR.getIcon();
    }
}
