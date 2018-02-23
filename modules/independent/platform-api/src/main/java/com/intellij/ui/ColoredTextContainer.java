package com.intellij.ui;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import javax.swing.*;

public interface ColoredTextContainer {
  void append(@Nonnull String fragment, @Nonnull SimpleTextAttributes attributes);

  void append(@Nonnull String fragment, @Nonnull SimpleTextAttributes attributes, Object tag);

  void setIcon(@Nullable Icon icon);

  void setToolTipText(@Nullable String text);
}