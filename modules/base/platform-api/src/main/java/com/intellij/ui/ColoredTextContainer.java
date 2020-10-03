package com.intellij.ui;

import consulo.awt.TargetAWT;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;

public interface ColoredTextContainer {
  void append(@Nonnull String fragment, @Nonnull SimpleTextAttributes attributes);

  void append(@Nonnull String fragment, @Nonnull SimpleTextAttributes attributes, Object tag);

  @Deprecated
  void setIcon(@Nullable Icon icon);

  default void setIcon(@Nullable Image image) {
    setIcon(TargetAWT.to(image));
  }

  void setToolTipText(@Nullable String text);
}