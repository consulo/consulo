package consulo.execution.impl.internal.ui.layout.action;

import jakarta.annotation.Nonnull;

public interface ContentLayoutStateSettings {
    boolean isSelected();

    void setSelected(boolean state);

    @Nonnull
    String getDisplayName();

    void restore();

    boolean isEnabled();
}
