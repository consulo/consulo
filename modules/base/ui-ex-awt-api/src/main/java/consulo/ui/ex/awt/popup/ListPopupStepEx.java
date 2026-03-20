package consulo.ui.ex.awt.popup;

import consulo.ui.ex.awt.StatusText;
import consulo.ui.ex.popup.ListPopupStep;
import consulo.ui.ex.popup.PopupStep;
import org.jspecify.annotations.Nullable;

import java.awt.event.InputEvent;

public interface ListPopupStepEx<T> extends ListPopupStep<T> {
    @Nullable PopupStep<?> onChosen(T selectedValue, boolean finalChoice, @Nullable InputEvent inputEvent);

    @Nullable String getTooltipTextFor(T value);

    void setEmptyText(StatusText emptyText);

    default @Nullable String getValueFor(T t) {
        return null;
    }
}