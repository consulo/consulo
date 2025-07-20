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

package consulo.ide.impl.idea.openapi.module.impl;

import consulo.module.ConfigurationErrorDescription;
import consulo.module.ConfigurationErrorType;
import consulo.project.Project;
import consulo.project.ProjectBundle;
import consulo.project.localize.ProjectLocalize;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.VerticalFlowLayout;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author nik
 */
public class RemoveInvalidElementsDialog extends DialogWrapper {
  private JPanel myContentPanel;
  private JPanel myMainPanel;
  private JLabel myDescriptionLabel;
  private final Map<JCheckBox, ConfigurationErrorDescription> myCheckboxes = new HashMap<JCheckBox, ConfigurationErrorDescription>();

  private RemoveInvalidElementsDialog(final String title,
                                      ConfigurationErrorType type,
                                      String invalidElements,
                                      final Project project,
                                      List<ConfigurationErrorDescription> errors) {
    super(project, true);
    setTitle(title);
    myDescriptionLabel.setText(ProjectBundle.message(type.canIgnore() ? "label.text.0.cannot.be.loaded.ignore" : "label.text.0.cannot.be.loaded.remove", invalidElements));
    myContentPanel.setLayout(new VerticalFlowLayout());
    for (ConfigurationErrorDescription error : errors) {
      JCheckBox checkBox = new JCheckBox(error.getElementName() + ".");
      checkBox.setSelected(true);
      myCheckboxes.put(checkBox, error);
      JPanel panel = new JPanel(new GridBagLayout());
      GridBagConstraints constraints = new GridBagConstraints();
      constraints.anchor = GridBagConstraints.NORTHWEST;
      constraints.ipadx = 5;
      panel.add(checkBox, constraints);
      constraints.anchor = GridBagConstraints.NORTHWEST;
      constraints.insets.top = 5;
      panel.add(new JLabel("<html><body>" + StringUtil.replace(error.getDescription().get(), "\n", "<br>") + "</body></html>"), constraints);
      constraints.weightx = 1;
      panel.add(new JPanel(), constraints);
      myContentPanel.add(panel);
    }
    init();
    setOKButtonText(ProjectBundle.message(type.canIgnore() ? "button.text.ignore.selected" : "button.text.remove.selected"));
    setCancelButtonText(ProjectLocalize.buttonTextKeepAll().get());
  }

  public static void showDialog(
    @Nonnull Project project,
    @Nonnull String title,
    ConfigurationErrorType type,
    @Nonnull String invalidElements,
    @Nonnull List<ConfigurationErrorDescription> errors
  ) {
    if (errors.isEmpty()) {
      return;
    }
    if (errors.size() == 1) {
      ConfigurationErrorDescription error = errors.get(0);
      String message = error.getDescription() + "\n" + error.getIgnoreConfirmationMessage();
      final int answer = Messages.showYesNoDialog(project, message, title, Messages.getErrorIcon());
      if (answer == Messages.YES) {
        error.ignoreInvalidElement();
      }
      return;
    }

    RemoveInvalidElementsDialog dialog = new RemoveInvalidElementsDialog(title, type, invalidElements, project, errors);
    dialog.show();
    if (dialog.isOK()) {
      for (ConfigurationErrorDescription errorDescription : dialog.getSelectedItems()) {
        errorDescription.ignoreInvalidElement();
      }
    }
  }

  private List<ConfigurationErrorDescription> getSelectedItems() {
    List<ConfigurationErrorDescription> items = new ArrayList<ConfigurationErrorDescription>();
    for (Map.Entry<JCheckBox, ConfigurationErrorDescription> entry : myCheckboxes.entrySet()) {
      if (entry.getKey().isSelected()) {
        items.add(entry.getValue());
      }
    }
    return items;
  }

  @Override
  protected JComponent createCenterPanel() {
    return myMainPanel;
  }
}
