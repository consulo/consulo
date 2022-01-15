package com.intellij.openapi.ui;

import com.intellij.openapi.project.Project;
import consulo.disposer.Disposable;

import javax.annotation.Nonnull;

import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;

public interface WindowWrapper extends Disposable {
  enum Mode {FRAME, MODAL, NON_MODAL}

  void show();

  @Nullable
  Project getProject();

  @Nonnull
  JComponent getComponent();

  @Nonnull
  Mode getMode();

  @Nonnull
  Window getWindow();

  void setTitle(@Nullable String title);

  void setImage(@Nullable Image image);

  void close();
}
