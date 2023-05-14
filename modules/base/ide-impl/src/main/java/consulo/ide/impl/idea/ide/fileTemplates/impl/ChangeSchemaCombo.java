/*
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.ide.impl.idea.ide.fileTemplates.impl;

import consulo.fileTemplate.FileTemplatesScheme;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.awt.action.ComboBoxAction;
import consulo.application.dumb.DumbAware;
import consulo.util.lang.function.Condition;
import consulo.ui.annotation.RequiredUIAccess;

import jakarta.annotation.Nonnull;
import javax.swing.*;

/**
 * @author Dmitry Avdeev
 */
public class ChangeSchemaCombo extends ComboBoxAction implements DumbAware {

  private final AllFileTemplatesConfigurable myConfigurable;

  public ChangeSchemaCombo(AllFileTemplatesConfigurable configurable) {
    myConfigurable = configurable;
  }

  @RequiredUIAccess
  @Override
  public void update(AnActionEvent e) {
    super.update(e);
    e.getPresentation().setText(myConfigurable.getCurrentScheme().getName());
  }

  @Nonnull
  @Override
  public DefaultActionGroup createPopupActionGroup(JComponent component) {
    DefaultActionGroup group = new DefaultActionGroup(new ChangeSchemaAction(FileTemplatesScheme.DEFAULT));
    FileTemplatesScheme scheme = myConfigurable.getManager().getProjectScheme();
    if (scheme != null) {
      group.add(new ChangeSchemaAction(scheme));
    }
    return group;
  }

  @Override
  public Condition<AnAction> getPreselectCondition() {
    return action -> myConfigurable.getCurrentScheme().getName().equals(action.getTemplatePresentation().getText());
  }

  private class ChangeSchemaAction extends AnAction {

    private final FileTemplatesScheme myScheme;

    public ChangeSchemaAction(@Nonnull FileTemplatesScheme scheme) {
      super(scheme.getName());
      myScheme = scheme;
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(AnActionEvent e) {
      myConfigurable.changeScheme(myScheme);
      ChangeSchemaCombo.this.getTemplatePresentation().setText(myScheme.getName());
    }
  }
}
