/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

package consulo.ide.impl.idea.ide.fileTemplates.ui;

import consulo.ide.IdeBundle;
import consulo.fileTemplate.FileTemplateUtil;
import consulo.ui.ex.awt.ColoredListCellRenderer;
import consulo.fileTemplate.FileTemplate;
import consulo.fileTemplate.FileTemplateManager;
import consulo.fileTemplate.impl.internal.FileTemplateImplUtil;
import consulo.language.psi.PsiDirectory;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.FixedSizeButton;

import jakarta.annotation.Nonnull;
import javax.swing.*;
import java.awt.*;

/**
 * @author MYakovlev
 * @since 2002-08-22
 */
public class SelectTemplateDialog extends DialogWrapper {
  private JComboBox myCbxTemplates;
  private FileTemplate mySelectedTemplate;
  private final Project myProject;
  private final PsiDirectory myDirectory;

  public SelectTemplateDialog(Project project, PsiDirectory directory) {
    super(project, true);
    myDirectory = directory;
    myProject = project;
    setTitle(IdeBundle.message("title.select.template"));
    init();
  }

  @Override
  protected JComponent createCenterPanel() {
    loadCombo();

    JButton editTemplatesButton = new FixedSizeButton(myCbxTemplates);

    JPanel centerPanel = new JPanel(new GridBagLayout());
    JLabel selectTemplateLabel = new JLabel(IdeBundle.message("label.name"));

    centerPanel.add(selectTemplateLabel, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(2, 2, 2, 2), 0, 0));
    centerPanel.add(myCbxTemplates, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 2), 50, 0));
    centerPanel.add(editTemplatesButton, new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(2, 2, 2, 2), 0, 0));

    editTemplatesButton.addActionListener(e -> onEditTemplates());

    return centerPanel;
  }

  @RequiredUIAccess
  private void loadCombo() {
    DefaultComboBoxModel model = new DefaultComboBoxModel();
    FileTemplate[] allTemplates = FileTemplateManager.getInstance(myProject).getAllTemplates();
    PsiDirectory[] dirs = {myDirectory};
    for (FileTemplate template : allTemplates) {
      if (FileTemplateImplUtil.canCreateFromTemplate(dirs, template)) {
        model.addElement(template);
      }
    }
    if (myCbxTemplates == null) {
      myCbxTemplates = new JComboBox(model);
      myCbxTemplates.setRenderer(new ColoredListCellRenderer<FileTemplate>() {
        @Override
        protected void customizeCellRenderer(@Nonnull JList list, FileTemplate value, int index, boolean selected, boolean hasFocus) {
          if (value != null) {
            setIcon(FileTemplateUtil.getIcon(value));
            append(value.getName());
          }
        }
      });
    }
    else {
      Object selected = myCbxTemplates.getSelectedItem();
      myCbxTemplates.setModel(model);
      if (selected != null) {
        myCbxTemplates.setSelectedItem(selected);
      }
    }
  }

  public FileTemplate getSelectedTemplate() {
    return mySelectedTemplate;
  }

  @Override
  protected void doOKAction() {
    mySelectedTemplate = (FileTemplate)myCbxTemplates.getSelectedItem();
    super.doOKAction();
  }

  @Override
  public void doCancelAction() {
    mySelectedTemplate = null;
    super.doCancelAction();
  }

  @Override
  public JComponent getPreferredFocusedComponent() {
    return myCbxTemplates;
  }

  private void onEditTemplates() {
    new ConfigureTemplatesDialog(myProject).show();
    loadCombo();
  }

}
