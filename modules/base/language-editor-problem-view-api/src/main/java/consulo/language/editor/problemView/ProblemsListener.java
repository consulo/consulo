package consulo.language.editor.problemView;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicAPI;
import jakarta.annotation.Nonnull;

/**
 * All methods are expected to be called in EDT.
 */
@TopicAPI(ComponentScope.PROJECT)
public interface ProblemsListener {
    Class<ProblemsListener> TOPIC = ProblemsListener.class;

    void problemAppeared(@Nonnull Problem problem);

    void problemDisappeared(@Nonnull Problem problem);

    void problemUpdated(@Nonnull Problem problem);
}
