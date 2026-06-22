package consulo.language.editor.problemView;

import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.localize.LocalizeValue;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

public interface Problem {
    /**
     * The problem provider that the problem belongs to.
     */
    ProblemsProvider getProvider();

    /**
     * One line description of the problem.
     */
    LocalizeValue getText();

    /**
     * A name used to group problems.
     */
    default @Nullable String getGroup() {
        return null;
    }

    /**
     * Detailed description of the problem if needed.
     */
    default LocalizeValue getDescription() {
        return LocalizeValue.empty();
    }

    /**
     * The problem icon.
     */
    default Image getIcon() {
        return Objects.requireNonNull(HighlightDisplayLevel.ERROR.getIcon());
    }
}
