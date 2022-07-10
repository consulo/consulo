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
package consulo.ide.impl.idea.ide.actions;

import consulo.project.Project;
import consulo.ui.ex.awt.ComboBox;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.internal.laf.MultiLineLabelUI;
import consulo.vcs.AbstractVcs;
import consulo.vcs.VcsBundle;
import consulo.ide.impl.idea.openapi.vcs.impl.projectlevelman.AllVcses;
import consulo.ui.ex.awt.CollectionComboBoxModel;
import consulo.ui.ex.awt.ColoredListCellRenderer;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.lang.ObjectUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

class StartUseVcsDialog extends DialogWrapper {
  @Nonnull
  private final Project myProject;

  private ComboBox<Object> myVcsComboBox;

  StartUseVcsDialog(@Nonnull Project project) {
    super(project, true);
    myProject = project;
    setTitle(VcsBundle.message("dialog.enable.version.control.integration.title"));

    init();
  }

  @RequiredUIAccess
  @Override
  public JComponent getPreferredFocusedComponent() {
    return myVcsComboBox;
  }

  @Nullable
  public AbstractVcs getSelectedVcs() {
    Object selectedItem = myVcsComboBox.getSelectedItem();
    return selectedItem == ObjectUtil.NULL ? null : (AbstractVcs)selectedItem;
  }

  @Override
  protected JComponent createCenterPanel() {
    final JLabel selectText = new JLabel(VcsBundle.message("dialog.enable.version.control.integration.select.vcs.label.text"));
    selectText.setUI(new MultiLineLabelUI());

    final JPanel mainPanel = new JPanel(new GridBagLayout());
    final GridBagConstraints gb = new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0);

    mainPanel.add(selectText, gb);

    ++gb.gridx;
    gb.anchor = GridBagConstraints.NORTHEAST;

    List<Object> vcses = new ArrayList<>();
    vcses.add(ObjectUtil.NULL);

    List<AbstractVcs> sortedVcs = new ArrayList<>(AllVcses.getInstance(myProject).getSupportedVcses());
    sortedVcs.sort((o1, o2) -> String.CASE_INSENSITIVE_ORDER.compare(o1.getDisplayName(), o2.getDisplayName()));

    vcses.addAll(sortedVcs);
    
    myVcsComboBox = new ComboBox<>(new CollectionComboBoxModel<>(vcses));
    myVcsComboBox.setRenderer(new ColoredListCellRenderer() {
      @Override
      protected void customizeCellRenderer(@Nonnull JList list, Object value, int index, boolean selected, boolean hasFocus) {
        if (value == ObjectUtil.NULL) {
          append("");
        }
        else {
          append(((AbstractVcs)value).getDisplayName());
        }
      }
    });
    mainPanel.add(myVcsComboBox, gb);

    myVcsComboBox.addActionListener(e -> validateVcs());
    validateVcs();

    final JLabel helpText = new JLabel(VcsBundle.message("dialog.enable.version.control.integration.hint.text"));
    helpText.setUI(new MultiLineLabelUI());
    helpText.setForeground(UIUtil.getInactiveTextColor());

    gb.anchor = GridBagConstraints.NORTHWEST;
    gb.gridx = 0;
    ++gb.gridy;
    gb.gridwidth = 2;
    mainPanel.add(helpText, gb);

    final JPanel wrapper = new JPanel(new GridBagLayout());
    wrapper.add(mainPanel, new GridBagConstraints(0, 0, 1, 1, 1, 1, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, JBUI.emptyInsets(), 0, 0));
    return wrapper;
  }

  private void validateVcs() {
    setOKActionEnabled(getSelectedVcs() != null);
  }

  @Override
  protected String getHelpId() {
    return "reference.version.control.enable.version.control.integration";
  }
}
