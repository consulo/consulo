package consulo.language.editor.problemView;

import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public interface Problem {
    /**
     * The problem provider that the problem belongs to.
     */
    @Nonnull
    ProblemsProvider getProvider();

    /**
     * One line description of the problem.
     */
    @Nonnull
    String getText();

    /**
     * A name used to group problems.
     */
    @Nullable
    default String getGroup() {
        return null;
    }

    /**
     * Detailed description of the problem if needed.
     */
    @Nullable
    default String getDescription() {
        return null;
    }

    /**
     * The problem icon.
     */
    @Nonnull
    default Image getIcon() {
        return HighlightDisplayLevel.ERROR.getIcon();
    }
}
