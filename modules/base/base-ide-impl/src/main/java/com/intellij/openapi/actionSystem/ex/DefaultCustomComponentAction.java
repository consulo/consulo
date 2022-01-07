// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.actionSystem.ex;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import javax.annotation.Nonnull;

import javax.swing.*;
import java.util.function.Supplier;

public class DefaultCustomComponentAction extends AnAction implements CustomComponentAction {
  @Nonnull
  private final Supplier<? extends JComponent> myProducer;

  public DefaultCustomComponentAction(@Nonnull Supplier<? extends JComponent> producer) {
    myProducer = producer;
  }

  @Override
  public void actionPerformed(@Nonnull AnActionEvent e) {
    //do nothing
  }

  @Nonnull
  @Override
  public JComponent createCustomComponent(@Nonnull Presentation presentation, @Nonnull String place) {
    return myProducer.get();
  }
}
