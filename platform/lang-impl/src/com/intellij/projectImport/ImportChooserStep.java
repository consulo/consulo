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

/*
 * User: anna
 * Date: 10-Jul-2007
 */
package com.intellij.projectImport;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.ide.util.newProjectWizard.StepSequence;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.DoubleClickListener;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBList;
import consulo.moduleImport.ModuleImportProvider;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ImportChooserStep extends ProjectImportWizardStep {
  private static final String PREFERRED = "create.project.preferred.importer";

  private final StepSequence mySequence;

  private JBList<ModuleImportProvider> myList;
  private JPanel myPanel;
  private JBLabel myImportTitleLabel;

  public ImportChooserStep(final ModuleImportProvider[] providers, final StepSequence sequence, final WizardContext context) {
    super(context);
    mySequence = sequence;

    myImportTitleLabel.setText(ProjectBundle.message("project.new.wizard.import.title", context.getPresentationName()));
    final DefaultListModel<ModuleImportProvider> model = new DefaultListModel<>();
    for (ModuleImportProvider provider : sorted(providers)) {
      model.addElement(provider);
    }
    myList.setModel(model);
    myList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    myList.setCellRenderer(new DefaultListCellRenderer() {
      @Override
      public Component getListCellRendererComponent(final JList list,
                                                    final Object value,
                                                    final int index,
                                                    final boolean isSelected,
                                                    final boolean cellHasFocus) {
        final Component rendererComponent = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        setText(((ModuleImportProvider)value).getName());
        Icon icon = ((ModuleImportProvider)value).getIcon();
        setIcon(icon);
        setDisabledIcon(IconLoader.getDisabledIcon(icon));
        return rendererComponent;
      }
    });


    myList.addListSelectionListener(e -> updateSteps());

    new DoubleClickListener() {
      @Override
      protected boolean onDoubleClick(MouseEvent e) {
        context.requestNextStep();
        return true;
      }
    }.installOn(myList);
  }

  @Override
  public void updateStep(@NotNull WizardContext wizardContext) {
    if (myList.getSelectedValue() != null) return;

    if (myList.getSelectedValue() == null) {
      myList.setSelectedIndex(0);
    }
  }

  private void updateSteps() {
    final ModuleImportProvider provider = getSelectedProvider();
    if (provider != null) {
      mySequence.setType(provider.getClass().getName());
      PropertiesComponent.getInstance().setValue(PREFERRED, provider.getClass().getName());
      getWizardContext().requestWizardButtonsUpdate();
    }
  }

  @NotNull
  private static List<ModuleImportProvider> sorted(ModuleImportProvider[] providers) {
    List<ModuleImportProvider> result = new ArrayList<>();
    Collections.addAll(result, providers);
    Collections.sort(result, (l, r) -> l.getName().compareToIgnoreCase(r.getName()));
    return result;
  }

  @Override
  public JComponent getComponent() {
    return myPanel;
  }

  @Override
  public JComponent getPreferredFocusedComponent() {
    return myList;
  }

  @Override
  public void updateDataModel() {
    final ModuleImportProvider provider = getSelectedProvider();
    if (provider != null) {
      mySequence.setType(provider.getClass().getName());

      getWizardContext().setImportProvider(provider);

      getWizardContext().getModuleImportContext(provider).setUpdate(getWizardContext().getProject() != null);
    }
  }

  private ModuleImportProvider getSelectedProvider() {
    return myList.getSelectedValue();
  }

  @Override
  public String getName() {
    return "Choose External Model";
  }

  @Override
  @NonNls
  public String getHelpId() {
    return "reference.dialogs.new.project.import";
  }
}
