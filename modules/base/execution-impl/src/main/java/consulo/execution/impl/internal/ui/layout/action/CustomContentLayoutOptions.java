package consulo.execution.impl.internal.ui.layout.action;

import org.jetbrains.annotations.Nls;
import jakarta.annotation.Nonnull;

public interface CustomContentLayoutOptions {

    @Nonnull
    CustomContentLayoutOption[] getAvailableOptions();

    void select(@Nonnull CustomContentLayoutOption option);

    boolean isSelected(@Nonnull CustomContentLayoutOption option);

    boolean isHidden();

    void restore();

    void onHide();

    @Nonnull
    @Nls
    String getDisplayName();

    boolean isHideOptionVisible();
}
