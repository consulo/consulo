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
package com.intellij.ide.fileTemplates.impl;

import com.intellij.ide.fileTemplates.FileTemplatesScheme;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.ex.ComboBoxAction;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.util.Condition;
import consulo.ui.annotation.RequiredUIAccess;

import javax.annotation.Nonnull;
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
