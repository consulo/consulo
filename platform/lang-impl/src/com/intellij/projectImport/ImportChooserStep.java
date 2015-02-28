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
import org.jetbrains.annotations.NonNls;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ImportChooserStep extends ProjectImportWizardStep {
  private static final String PREFERRED = "create.project.preferred.importer";

  private final ProjectImportProvider[] myProviders;
  private final StepSequence mySequence;

  private JBList myList;
  private JPanel myPanel;
  private JBLabel myImportTitleLabel;

  public ImportChooserStep(final ProjectImportProvider[] providers, final StepSequence sequence, final WizardContext context) {
    super(context);
    myProviders = providers;
    mySequence = sequence;

    myImportTitleLabel.setText(ProjectBundle.message("project.new.wizard.import.title", context.getPresentationName()));
    final DefaultListModel model = new DefaultListModel();
    for (ProjectImportProvider provider : sorted(providers)) {
       model.addElement(provider);
    }
    myList.setModel(model);
    myList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    myList.setCellRenderer(new DefaultListCellRenderer() {
      @Override
      public Component getListCellRendererComponent(final JList list,
                                                    final Object value,
                                                    final int index, final boolean isSelected, final boolean cellHasFocus) {
        final Component rendererComponent = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        setText(((ProjectImportProvider)value).getName());
        Icon icon = ((ProjectImportProvider)value).getIcon();
        setIcon(icon);
        setDisabledIcon(IconLoader.getDisabledIcon(icon));
        return rendererComponent;
      }
    });


    myList.addListSelectionListener(new ListSelectionListener() {
      @Override
      public void valueChanged(final ListSelectionEvent e) {
        updateSteps();
      }
    });

    new DoubleClickListener() {
      @Override
      protected boolean onDoubleClick(MouseEvent e) {
        context.requestNextStep();
        return true;
      }
    }.installOn(myList);
  }

  @Override
  public void updateStep() {
    if (myList.getSelectedValue() != null) return;

    if (myList.getSelectedValue() == null) {
      myList.setSelectedIndex(0);
    }
  }

  private void updateSteps() {
    final ProjectImportProvider provider = getSelectedProvider();
    if (provider != null) {
      mySequence.setType(provider.getId());
      PropertiesComponent.getInstance().setValue(PREFERRED, provider.getId());
      getWizardContext().requestWizardButtonsUpdate();
    }
  }

  private static List<ProjectImportProvider> sorted(ProjectImportProvider[] providers) {
    List<ProjectImportProvider> result = new ArrayList<ProjectImportProvider>();
    Collections.addAll(result, providers);
    Collections.sort(result, new Comparator<ProjectImportProvider>() {
      @Override
      public int compare(ProjectImportProvider l, ProjectImportProvider r) {
        return l.getName().compareToIgnoreCase(r.getName());
      }
    });
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
    final ProjectImportProvider provider = getSelectedProvider();
    if (provider != null) {
      mySequence.setType(provider.getId());
      final ProjectImportBuilder builder = provider.getBuilder();
      getWizardContext().setProjectBuilder(builder);
      builder.setUpdate(getWizardContext().getProject() != null);
    }
  }

  private ProjectImportProvider getSelectedProvider() {
    return (ProjectImportProvider)myList.getSelectedValue();
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
