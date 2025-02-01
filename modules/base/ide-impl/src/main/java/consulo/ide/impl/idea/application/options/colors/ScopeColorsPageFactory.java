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

import consulo.application.localize.ApplicationLocalize;
import consulo.configurable.Configurable;
import consulo.configurable.internal.ConfigurableWeight;
import consulo.dataContext.DataManager;
import consulo.ide.impl.idea.ide.util.scopeChooser.EditScopesDialog;
import consulo.ide.impl.idea.ide.util.scopeChooser.ScopeChooserConfigurable;
import consulo.configurable.Settings;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.JBUI;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;

class ScopeColorsPageFactory implements ColorAndFontPanelFactory, ConfigurableWeight {
  @Nonnull
  @Override
  @RequiredUIAccess
  public NewColorAndFontPanel createPanel(@Nonnull ColorAndFontOptions options) {
    final JPanel scopePanel = createChooseScopePanel();
    return NewColorAndFontPanel.create(
      new PreviewPanel.Empty(){
        @Override
        public Component getPanel() {
          return scopePanel;
        }
      },
      ColorAndFontOptions.SCOPES_GROUP,
      options,
      null,
      null
    );
  }

  @Nonnull
  @Override
  public String getPanelDisplayName() {
    return ColorAndFontOptions.SCOPES_GROUP;
  }

  private static JPanel createChooseScopePanel() {
    Project[] projects = ProjectManager.getInstance().getOpenProjects();
    JPanel panel = new JPanel(new GridBagLayout());
    //panel.setBorder(new LineBorder(Color.red));
    if (projects.length == 0) return panel;
    GridBagConstraints gc = new GridBagConstraints(
      0, 0, 1, 1, 0, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,
      JBUI.emptyInsets(), 0, 0
    );
    final Project contextProject = DataManager.getInstance().getDataContext().getData(Project.KEY);
    final Project project = contextProject != null ? contextProject : projects[0];

    JButton button = new JButton(ApplicationLocalize.buttonEditScopes().get());
    button.setPreferredSize(new Dimension(230, button.getPreferredSize().height));
    panel.add(button, gc);
    gc.gridx = GridBagConstraints.REMAINDER;
    gc.weightx = 1;
    panel.add(new JPanel(), gc);

    gc.gridy++;
    gc.gridx=0;
    gc.weighty = 1;
    panel.add(new JPanel(), gc);
    button.addActionListener(e -> {
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
    });
    return panel;
  }

  @Override
  public int getConfigurableWeight() {
    return Integer.MAX_VALUE - 1;
  }
}
