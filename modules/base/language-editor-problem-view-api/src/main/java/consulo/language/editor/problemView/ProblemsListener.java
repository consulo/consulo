package consulo.language.editor.problemView;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicAPI;

/**
 * All methods are expected to be called in EDT.
 */
@TopicAPI(ComponentScope.PROJECT)
public interface ProblemsListener {
    Class<ProblemsListener> TOPIC = ProblemsListener.class;

    void problemAppeared(Problem problem);

    void problemDisappeared(Problem problem);

    void problemUpdated(Problem problem);
}
