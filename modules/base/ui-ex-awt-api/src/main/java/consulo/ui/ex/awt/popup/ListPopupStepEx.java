package consulo.ui.ex.awt.popup;

import consulo.ui.ex.awt.StatusText;
import consulo.ui.ex.popup.ListPopupStep;
import consulo.ui.ex.popup.PopupStep;
import org.intellij.lang.annotations.MagicConstant;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.awt.event.InputEvent;

public interface ListPopupStepEx<T> extends ListPopupStep<T> {
  PopupStep onChosen(T selectedValue, boolean finalChoice, @MagicConstant(flagsFromClass = InputEvent.class) int eventModifiers);

  @Nullable
  String getTooltipTextFor(T value);

  void setEmptyText(@Nonnull StatusText emptyText);

  @Nullable
  default String getValueFor(T t) {
    return null;
  }
}