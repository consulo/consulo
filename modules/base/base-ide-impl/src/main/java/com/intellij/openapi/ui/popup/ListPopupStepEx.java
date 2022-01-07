package com.intellij.openapi.ui.popup;

import com.intellij.util.ui.StatusText;
import org.intellij.lang.annotations.MagicConstant;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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