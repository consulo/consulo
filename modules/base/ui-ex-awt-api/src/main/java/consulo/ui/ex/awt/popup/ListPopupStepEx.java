package consulo.ui.ex.awt.popup;

import consulo.ui.ex.awt.StatusText;
import consulo.ui.ex.popup.ListPopupStep;
import consulo.ui.ex.popup.PopupStep;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.event.InputEvent;

public interface ListPopupStepEx<T> extends ListPopupStep<T> {
    @Nullable
    PopupStep<?> onChosen(T selectedValue, boolean finalChoice, @Nullable InputEvent inputEvent);

    @Nullable
    String getTooltipTextFor(T value);

    void setEmptyText(@Nonnull StatusText emptyText);

    @Nullable
    default String getValueFor(T t) {
        return null;
    }
}