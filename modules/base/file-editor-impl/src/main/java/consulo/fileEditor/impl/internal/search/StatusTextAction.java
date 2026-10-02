// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.fileEditor.impl.internal.search;

import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.HorizontalAlignment;
import consulo.ui.Label;
import consulo.ui.LabelOptions;
import consulo.ui.Length;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.CustomUIComponentAction;
import consulo.ui.ex.action.LegacyDumbAwareAction;
import consulo.ui.ex.action.Presentation;

public class StatusTextAction extends LegacyDumbAwareAction implements CustomUIComponentAction {
  private static final Length MIN_WIDTH = Length.ofFont(4.5f);

  @Override
  @RequiredUIAccess
  public void update(AnActionEvent e) {
    SearchSession search = e.getData(SearchSession.KEY);
    if (!(e.getPresentation().getClientProperty(COMPONENT_KEY) instanceof Label label)) {
      return;
    }

    label.setText(search == null ? LocalizeValue.empty() : LocalizeValue.of(search.getComponent().getStatusText()));
    label.setForegroundColor(search == null ? null : search.getComponent().getStatusColor());
  }

  @Override
  @RequiredUIAccess
  public void actionPerformed(AnActionEvent e) {
  }

  @Override
  @RequiredUIAccess
  public Component createCustomComponent(Presentation presentation, String place) {
    Label label = Label.create(LocalizeValue.empty(), LabelOptions.builder().horizontalAlignment(HorizontalAlignment.CENTER).build());
    label.setMinWidth(MIN_WIDTH);
    return label;
  }
}
