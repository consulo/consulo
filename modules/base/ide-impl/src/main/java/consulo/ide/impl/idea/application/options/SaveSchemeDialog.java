/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.ide.impl.idea.application.options;

import consulo.application.CommonBundle;
import consulo.application.ApplicationBundle;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.Messages;
import consulo.component.util.text.UniqueNameGenerator;
import consulo.ui.ex.awt.JBUI;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * @author Yura Cangea
 */
public class SaveSchemeDialog extends DialogWrapper {
  private final JTextField mySchemeName = new JTextField();
  private final List<String> myExistingNames;

  public SaveSchemeDialog(@Nonnull Component parent, String title, @Nonnull List<String> existingNames, @Nonnull String selectedName) {
    super(parent, false);
    myExistingNames = existingNames;
    setTitle(title);
    mySchemeName.setText(UniqueNameGenerator.generateUniqueName(selectedName + " copy", existingNames));
    init();
  }

  public String getSchemeName() {
    return mySchemeName.getText();
  }

  @Override
  protected JComponent createNorthPanel() {
    JPanel panel = new JPanel(new GridBagLayout());
    GridBagConstraints gc = new GridBagConstraints();
    gc.gridx = 0;
    gc.gridy = 0;
    gc.weightx = 0;
    gc.insets = new Insets(5, 0, 5, 5);
    panel.add(new JLabel(ApplicationBundle.message("label.name")), gc);

    gc = new GridBagConstraints();
    gc.gridx = 1;
    gc.gridy = 0;
    gc.weightx = 1;
    gc.fill = GridBagConstraints.HORIZONTAL;
    gc.gridwidth = 2;
    gc.insets = new Insets(0, 0, 5, 0);
    panel.add(mySchemeName, gc);

    panel.setPreferredSize(JBUI.size(220, 40));
    return panel;
  }

  @Override
  protected void doOKAction() {
    if (getSchemeName().trim().isEmpty()) {
      Messages.showMessageDialog(getContentPane(), ApplicationBundle.message("error.scheme.must.have.a.name"),
                                 CommonBundle.getErrorTitle(), Messages.getErrorIcon());
      return;
    }
    else if ("default".equals(getSchemeName())) {
      Messages.showMessageDialog(getContentPane(), ApplicationBundle.message("error.illegal.scheme.name"),
                                 CommonBundle.getErrorTitle(), Messages.getErrorIcon());
      return;
    }
    else if (myExistingNames.contains(getSchemeName())) {
      Messages.showMessageDialog(
              getContentPane(),
              ApplicationBundle.message("error.a.scheme.with.this.name.already.exists.or.was.deleted.without.applying.the.changes"),
              CommonBundle.getErrorTitle(),
              Messages.getErrorIcon()
      );
      return;
    }
    super.doOKAction();
  }

  @Override
  protected JComponent createCenterPanel() {
    return null;
  }

  @Override
  public JComponent getPreferredFocusedComponent() {
    return mySchemeName;
  }
}
