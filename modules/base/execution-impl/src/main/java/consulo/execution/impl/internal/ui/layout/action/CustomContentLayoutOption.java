package consulo.execution.impl.internal.ui.layout.action;

import org.jetbrains.annotations.Nls;

public interface CustomContentLayoutOption {
    boolean isEnabled();

    boolean isSelected();

    void select();

    @Nls
    String getDisplayName();
}
