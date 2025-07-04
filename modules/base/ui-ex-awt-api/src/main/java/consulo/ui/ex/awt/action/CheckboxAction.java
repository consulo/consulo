/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package consulo.ui.ex.awt.action;

import consulo.dataContext.DataManager;
import consulo.ui.ex.action.*;
import consulo.ui.ex.internal.LocalizeValueWithMnemonic;
import consulo.ui.util.TextWithMnemonic;
import consulo.ui.ex.awt.UIUtil;
import consulo.dataContext.DataContext;
import consulo.localize.LocalizeValue;
import consulo.ui.image.Image;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author max
 */
public abstract class CheckboxAction extends ToggleAction implements CustomComponentAction {

  protected CheckboxAction() {}

  @Deprecated
  protected CheckboxAction(final String text) {
    super(text);
  }

  protected CheckboxAction(final String text, final String description, final Image icon) {
    super(text, description, icon);
  }

  protected CheckboxAction(@Nonnull LocalizeValue text) {
    super(text);
  }

  protected CheckboxAction(@Nonnull LocalizeValue text, @Nonnull LocalizeValue description, @Nullable Image icon) {
    super(text, description, icon);
  }

  protected CheckboxAction(@Nonnull LocalizeValue text, @Nonnull LocalizeValue description) {
    super(text, description);
  }

  @Nonnull
  @Override
  public JComponent createCustomComponent(Presentation presentation, @Nonnull String place) {
    // this component cannot be stored right here because of action system architecture:
    // one action can be shown on multiple toolbars simultaneously
      TextWithMnemonic textWithMnemonic = LocalizeValueWithMnemonic.get(presentation.getTextValue());

    JCheckBox checkBox = new JCheckBox(textWithMnemonic.getText());
    checkBox.setOpaque(false);
    checkBox.setToolTipText(presentation.getDescription());
    checkBox.setMnemonic(textWithMnemonic.getMnemonic());
    checkBox.setDisplayedMnemonicIndex(textWithMnemonic.getMnemonicIndex());

    checkBox.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        JCheckBox checkBox = (JCheckBox)e.getSource();
        ActionToolbar actionToolbar = UIUtil.getParentOfType(ActionToolbar.class, checkBox);
        DataContext dataContext =
          actionToolbar != null ? actionToolbar.getToolbarDataContext() : DataManager.getInstance().getDataContext(checkBox);
        CheckboxAction.this.actionPerformed(new AnActionEvent(null, dataContext,
                                                              ActionPlaces.UNKNOWN, CheckboxAction.this.getTemplatePresentation(),
                                                              ActionManager.getInstance(), 0));
      }
    });

    return checkBox;
  }

  @Override
  public void update(@Nonnull AnActionEvent e) {
    super.update(e);
    JComponent property = e.getPresentation().getClientProperty(COMPONENT_KEY);
    if (property instanceof JCheckBox) {
      JCheckBox checkBox = (JCheckBox)property;

      checkBox.setSelected(Boolean.TRUE.equals(e.getPresentation().getClientProperty(SELECTED_PROPERTY)));
      checkBox.setEnabled(e.getPresentation().isEnabled());
      checkBox.setVisible(e.getPresentation().isVisible());
    }
  }
}
