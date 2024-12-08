package consulo.execution.impl.internal.ui.layout.action;

import consulo.execution.internal.layout.RunnerContentUi;
import consulo.ui.ex.action.AnAction;
import consulo.util.dataholder.Key;
import jakarta.annotation.Nonnull;

import java.util.List;

public interface CustomContentLayoutSettings {

    Key<CustomContentLayoutSettings> KEY = Key.of(CustomContentLayoutSettings.class);

    @Nonnull
    List<AnAction> getActions(@Nonnull RunnerContentUi runnerContentUi);

    void restore();
}
