/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.language.codeStyle.ui.internal.arrangement;

import consulo.application.ApplicationBundle;
import consulo.language.codeStyle.CommonCodeStyleSettings;
import consulo.ui.ex.awt.EnumComboBoxModel;
import consulo.ui.ex.awt.OptionGroup;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import java.awt.*;

public class ForceArrangementPanel {

  @Nonnull
  private final JComboBox myForceRearrangeComboBox;
  @Nonnull
  private final JPanel myPanel;

  public ForceArrangementPanel() {
    myForceRearrangeComboBox = new JComboBox();
    myForceRearrangeComboBox.setModel(new EnumComboBoxModel<SelectedMode>(SelectedMode.class));
    myForceRearrangeComboBox.setMaximumSize(myForceRearrangeComboBox.getPreferredSize());
    myPanel = createPanel();
  }

  public int getRearrangeMode() {
    return getSelectedMode().rearrangeMode;
  }

  public void setSelectedMode(@Nonnull SelectedMode mode) {
    myForceRearrangeComboBox.setSelectedItem(mode);
  }

  public void setSelectedMode(int mode) {
    SelectedMode toSetUp = SelectedMode.getByMode(mode);
    assert(toSetUp != null);
    setSelectedMode(toSetUp);
  }

  @Nonnull
  public JPanel getPanel() {
    return myPanel;
  }

  @Nonnull
  private JPanel createPanel() {
    OptionGroup group = new OptionGroup(ApplicationBundle.message("arrangement.settings.additional.title"));
    JPanel textWithComboPanel = new JPanel();
    textWithComboPanel.setLayout(new BoxLayout(textWithComboPanel, BoxLayout.LINE_AXIS));
    textWithComboPanel.add(new JLabel(ApplicationBundle.message("arrangement.settings.additional.force.combobox.name")));
    textWithComboPanel.add(Box.createRigidArea(new Dimension(5, 0)));
    textWithComboPanel.add(myForceRearrangeComboBox);
    group.add(textWithComboPanel);
    return group.createPanel();
  }

  @Nonnull
  private SelectedMode getSelectedMode() {
    return (SelectedMode)myForceRearrangeComboBox.getSelectedItem();
  }

  private enum SelectedMode {
    FROM_DIALOG(ApplicationBundle.message("arrangement.settings.additional.force.rearrange.according.to.dialog"), CommonCodeStyleSettings.REARRANGE_ACCORDIND_TO_DIALOG),
    ALWAYS(ApplicationBundle.message("arrangement.settings.additional.force.rearrange.always"), CommonCodeStyleSettings.REARRANGE_ALWAYS),
    NEVER(ApplicationBundle.message("arrangement.settings.additional.force.rearrange.never"), CommonCodeStyleSettings.REARRANGE_NEVER);

    public final int rearrangeMode;
    @Nonnull
    private final String myName;

    SelectedMode(@Nonnull String name, int mode) {
      myName = name;
      rearrangeMode = mode;
    }

    @Nullable
    private static SelectedMode getByMode(int mode) {
      for (SelectedMode currentMode: values()) {
        if (currentMode.rearrangeMode == mode) return currentMode;
      }
      return null;
    }

    @Override
    @Nonnull
    public String toString() {
      return myName;
    }
  }
}
