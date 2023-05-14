// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.openapi.actionSystem.ex;

import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ide.impl.idea.openapi.util.text.StringUtil;
import consulo.ui.ex.awt.JBLabel;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.action.CustomComponentAction;
import kava.beans.PropertyChangeEvent;
import kava.beans.PropertyChangeListener;

import jakarta.annotation.Nonnull;
import javax.swing.*;

public abstract class ToolbarLabelAction extends DumbAwareAction implements CustomComponentAction {
  @RequiredUIAccess
  @Override
  public void update(@Nonnull AnActionEvent e) {
    e.getPresentation().setEnabled(false);
  }

  @RequiredUIAccess
  @Override
  public final void actionPerformed(@Nonnull AnActionEvent e) {
    //do nothing
  }

  @Nonnull
  @Override
  public JComponent createCustomComponent(@Nonnull Presentation presentation, @Nonnull String place) {
    return new MyLabel(presentation).withFont(JBUI.Fonts.toolbarFont()).withBorder(JBUI.Borders.empty(0, 6, 0, 5));
  }

  private static class MyLabel extends JBLabel {
    @Nonnull
    private final Presentation myPresentation;

    MyLabel(@Nonnull Presentation presentation) {
      myPresentation = presentation;

      presentation.addPropertyChangeListener(new PropertyChangeListener() {
        @Override
        public void propertyChange(PropertyChangeEvent e) {
          String propertyName = e.getPropertyName();
          if (Presentation.PROP_TEXT.equals(propertyName) || Presentation.PROP_DESCRIPTION.equals(propertyName) || Presentation.PROP_ICON.equals(propertyName)) {
            updatePresentation();
          }
        }
      });
      updatePresentation();
    }

    private void updatePresentation() {
      setText(StringUtil.notNullize(myPresentation.getText()));
      setToolTipText(StringUtil.nullize(myPresentation.getDescription()));
      setIcon(myPresentation.getIcon());
    }
  }
}
