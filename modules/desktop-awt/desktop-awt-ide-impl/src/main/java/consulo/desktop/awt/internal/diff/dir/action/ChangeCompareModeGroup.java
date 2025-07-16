/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.desktop.awt.internal.diff.dir.action;

import consulo.desktop.awt.internal.diff.dir.DirDiffTableModel;
import consulo.diff.dir.DirDiffSettings;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.IdeBorderFactory;
import consulo.ui.ex.awt.action.ComboBoxAction;
import consulo.ui.ex.awt.action.ComboBoxButton;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

/**
 * @author Konstantin Bulenkov
 */
public class ChangeCompareModeGroup extends ComboBoxAction implements ShortcutProvider {
  private final DefaultActionGroup myGroup;
  private DirDiffSettings mySettings;
  private ComboBoxButton myButton;

  public ChangeCompareModeGroup(DirDiffTableModel model) {
    mySettings = model.getSettings();
    getTemplatePresentation().setText(mySettings.compareMode.getPresentableName(mySettings));
    final ArrayList<ChangeCompareModeAction> actions = new ArrayList<ChangeCompareModeAction>();
    if (model.getSettings().showCompareModes) {
      for (DirDiffSettings.CompareMode mode : DirDiffSettings.CompareMode.values()) {
        actions.add(new ChangeCompareModeAction(model, mode));
      }
    }
    else {
      getTemplatePresentation().setEnabledAndVisible(false);
    }
    myGroup = new DefaultActionGroup(actions.toArray(new ChangeCompareModeAction[actions.size()]));
  }

  @Override
  @RequiredUIAccess
  public void actionPerformed(@Nonnull AnActionEvent e) {
    myButton.showPopup();
  }

  @Override
  public void update(@Nonnull AnActionEvent e) {
    super.update(e);
    getTemplatePresentation().setText(mySettings.compareMode.getPresentableName(mySettings));
    e.getPresentation().setText(mySettings.compareMode.getPresentableName(mySettings));
  }

  @Nonnull
  @Override
  public JComponent createCustomComponent(Presentation presentation, String place) {
    JPanel panel = new JPanel(new BorderLayout());
    final JLabel label = new JLabel("Compare by:");
    label.setDisplayedMnemonicIndex(0);
    panel.add(label, BorderLayout.WEST);
    myButton = (ComboBoxButton)super.createCustomComponent(presentation, place).getComponent(0);
    panel.add(myButton.getComponent(), BorderLayout.CENTER);
    panel.setBorder(IdeBorderFactory.createEmptyBorder(2, 6, 2, 0));
    return panel;
  }

  @Nonnull
  @Override
  public DefaultActionGroup createPopupActionGroup(JComponent button) {
    return myGroup;
  }

  @Override
  public ShortcutSet getShortcut() {
    return CustomShortcutSet.fromString("alt C");
  }
}
