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

package consulo.ide.impl.idea.application.options.colors;

import consulo.dataContext.DataManager;
import consulo.ide.impl.idea.ide.util.scopeChooser.EditScopesDialog;
import consulo.ide.impl.idea.ide.util.scopeChooser.ScopeChooserConfigurable;
import consulo.language.editor.CommonDataKeys;
import consulo.application.ApplicationBundle;
import consulo.configurable.Configurable;
import consulo.ide.setting.Settings;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.configurable.internal.ConfigurableWeight;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

class ScopeColorsPageFactory implements ColorAndFontPanelFactory, ConfigurableWeight {
  @Override
  public NewColorAndFontPanel createPanel(ColorAndFontOptions options) {
    final JPanel scopePanel = createChooseScopePanel();
    return NewColorAndFontPanel.create(new PreviewPanel.Empty(){
      @Override
      public Component getPanel() {
        return scopePanel;
      }

    }, ColorAndFontOptions.SCOPES_GROUP, options, null, null);
  }

  @Override
  public String getPanelDisplayName() {
    return ColorAndFontOptions.SCOPES_GROUP;
  }

  private static JPanel createChooseScopePanel() {
    Project[] projects = ProjectManager.getInstance().getOpenProjects();
    JPanel panel = new JPanel(new GridBagLayout());
    //panel.setBorder(new LineBorder(Color.red));
    if (projects.length == 0) return panel;
    GridBagConstraints gc = new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,
                                                   new Insets(0, 0, 0, 0), 0, 0);
    final Project contextProject = DataManager.getInstance().getDataContext().getData(CommonDataKeys.PROJECT);
    final Project project = contextProject != null ? contextProject : projects[0];

    JButton button = new JButton(ApplicationBundle.message("button.edit.scopes"));
    button.setPreferredSize(new Dimension(230, button.getPreferredSize().height));
    panel.add(button, gc);
    gc.gridx = GridBagConstraints.REMAINDER;
    gc.weightx = 1;
    panel.add(new JPanel(), gc);

    gc.gridy++;
    gc.gridx=0;
    gc.weighty = 1;
    panel.add(new JPanel(), gc);
    button.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        final Settings optionsEditor = DataManager.getInstance().getDataContext().getData(Settings.KEY);
        if (optionsEditor != null) {
          try {
            Configurable configurable = optionsEditor.findConfigurableById(ScopeChooserConfigurable.PROJECT_SCOPES);
            if (configurable == null || optionsEditor.clearSearchAndSelect(configurable).isRejected()) {
              EditScopesDialog.showDialog(project, null);
            }
          } catch (IllegalStateException ex) {
            EditScopesDialog.showDialog(project, null);
          }
        }
      }
    });
    return panel;
  }

  @Override
  public int getConfigurableWeight() {
    return Integer.MAX_VALUE - 1;
  }
}
