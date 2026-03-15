package consulo.execution.impl.internal.ui.layout.action;

import consulo.execution.internal.layout.RunnerContentUi;
import consulo.ui.ex.action.AnAction;
import consulo.util.dataholder.Key;

import java.util.List;

public interface CustomContentLayoutSettings {

    Key<CustomContentLayoutSettings> KEY = Key.of(CustomContentLayoutSettings.class);

    
    List<AnAction> getActions(RunnerContentUi runnerContentUi);

    void restore();
}
