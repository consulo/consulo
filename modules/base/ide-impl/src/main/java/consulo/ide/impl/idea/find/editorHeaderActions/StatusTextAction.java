// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.find.editorHeaderActions;

import consulo.ide.impl.idea.find.SearchSession;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.awt.JBUIScale;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.action.CustomComponentAction;
import jakarta.annotation.Nonnull;

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
