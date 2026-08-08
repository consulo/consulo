package consulo.ui.ex.awt.popup;

import consulo.ui.ex.awt.StatusText;
import consulo.ui.ex.popup.ListPopupStep;
import consulo.ui.ex.popup.PopupStep;
import org.jspecify.annotations.Nullable;

import java.awt.event.InputEvent;
import consulo.util.dataholder.Key;

public interface ListPopupStepEx<T> extends ListPopupStep<T> {
    @Nullable PopupStep<?> onChosen(T selectedValue, boolean finalChoice, @Nullable InputEvent inputEvent);

    @Nullable String getTooltipTextFor(T value);

    void setEmptyText(StatusText emptyText);

    /**
     * Text an action puts on its presentation for {@link #getValueFor} to answer with - the path a copy action
     * would put on the clipboard.
     */
    Key<String> SECONDARY_TEXT = Key.create("listPopupSecondaryText");

    default @Nullable String getValueFor(T t) {
        return null;
    }
}