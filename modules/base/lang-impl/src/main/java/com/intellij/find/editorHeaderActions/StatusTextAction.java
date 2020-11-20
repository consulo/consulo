// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.find.editorHeaderActions;

import com.intellij.find.SearchSession;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.ex.CustomComponentAction;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.ui.scale.JBUIScale;
import com.intellij.util.ui.UIUtil;
import consulo.ui.annotation.RequiredUIAccess;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.*;

public class StatusTextAction extends DumbAwareAction implements CustomComponentAction {
  @RequiredUIAccess
  @Override
  public void update(@Nonnull AnActionEvent e) {
    SearchSession search = e.getData(SearchSession.KEY);
    JLabel label = (JLabel)e.getPresentation().getClientProperty(COMPONENT_KEY);
    if (label == null) return;
    label.setText(search == null ? "" : search.getComponent().getStatusText());
    label.setForeground(search == null ? UIUtil.getLabelForeground() : search.getComponent().getStatusColor());
  }

  @RequiredUIAccess
  @Override
  public void actionPerformed(@Nonnull AnActionEvent e) {
  }

  @Nonnull
  @Override
  public JComponent createCustomComponent(@Nonnull Presentation presentation, @Nonnull String place) {
    JLabel label = new JLabel();
    //noinspection HardCodedStringLiteral
    label.setText("9888 results");
    Dimension size = label.getPreferredSize();
    size.height = Math.max(size.height, JBUIScale.scale(ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE.getHeight()));
    label.setPreferredSize(size);
    label.setText(null);
    label.setHorizontalAlignment(SwingConstants.CENTER);
    return label;
  }
}
