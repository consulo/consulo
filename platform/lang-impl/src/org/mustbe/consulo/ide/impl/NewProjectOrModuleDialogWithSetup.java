/*
 * Copyright 2013-2014 must-be.org
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
package org.mustbe.consulo.ide.impl;

import com.intellij.icons.AllIcons;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.impl.ProjectUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.LabeledComponent;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.platform.LocationNameFieldsBinding;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.SeparatorComponent;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.Consumer;
import org.consulo.ide.eap.EarlyAccessProgramDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mustbe.consulo.ide.impl.ui.DListItem;
import org.mustbe.consulo.ide.impl.ui.DListWithChildren;

import javax.swing.*;
import java.awt.*;

/**
 * @author VISTALL
 * @since 04.06.14
 */
public class NewProjectOrModuleDialogWithSetup extends NewProjectOrModuleDialog {
  private final boolean myModuleCreation;

  public static class EapDescriptor implements EarlyAccessProgramDescriptor {
    @NotNull
    @Override
    public String getName() {
      return "New dialog for creating project/module";
    }

    @Override
    public boolean getDefaultState() {
      return false;
    }

    @NotNull
    @Override
    public String getDescription() {
      return getName();
    }
  }

  private JBSplitter mySplitter;
  private JTextField myNameField;
  private TextFieldWithBrowseButton myLocationField;

  public NewProjectOrModuleDialogWithSetup(@Nullable Project project, @Nullable VirtualFile virtualFile) {
    super(project, true);
    myModuleCreation = virtualFile != null;

    setTitle(myModuleCreation ? IdeBundle.message("title.add.module") : IdeBundle.message("title.new.project"));

    DListItem root = DListItem.builder().withItems(DListItem.builder().withName("Java").withIcon(AllIcons.Nodes.Static)
                                                           .withItems(DListItem.builder().withName("Hello World").withIcon(AllIcons.Nodes.Class)),
                                                   DListItem.builder().withName("C#").withIcon(AllIcons.Nodes.TypeAlias)
                                                           .withItems(DListItem.builder().withName("ASP").withIcon(AllIcons.Nodes.Artifact)),
                                                   DListItem.builder().withName("Empty").withIcon(AllIcons.FileTypes.Text)).create();


    mySplitter = new JBSplitter(0.3f);
    mySplitter.setSplitterProportionKey("#CreateProjectOrModuleDialog.Splitter");

    DListWithChildren list = new DListWithChildren();
    list.select(root);

    mySplitter.setFirstComponent(new JBScrollPane(list));

    final JPanel panel = new JPanel(new VerticalFlowLayout());

    myNameField = new JTextField();
    myLocationField = new TextFieldWithBrowseButton();

    panel.add(LabeledComponent.create(myNameField, "Name").setLabelLocation(BorderLayout.WEST));

    if (virtualFile != null) {
      myNameField.setText(virtualFile.getName());
    }
    else {
      panel.add(LabeledComponent.create(myLocationField, "Path").setLabelLocation(BorderLayout.WEST));

      new LocationNameFieldsBinding(project, myLocationField, myNameField, ProjectUtil.getBaseDir(), "Select Directory");
    }

    panel.add(new SeparatorComponent());

    final JPanel nullPanel = new JPanel(new BorderLayout());

    mySplitter.setSecondComponent(nullPanel);

    list.setConsumer(new Consumer<DListItem>() {
      @Override
      public void consume(DListItem dListItem) {
        if (dListItem != null) {
          mySplitter.setSecondComponent(panel);
        }
        else {
          mySplitter.setSecondComponent(nullPanel);
        }
        mySplitter.invalidate();
      }
    });

    setSize(800, 600);
    init();
  }

  @NotNull
  @Override
  public String getLocationText() {
    return myLocationField.getText();
  }

  @Nullable
  @Override
  public String getNameText() {
    return myNameField.getText();
  }

  @Override
  public boolean isModuleCreation() {
    return myModuleCreation;
  }

  @Override
  protected String getDimensionServiceKey() {
    return "#CreateProjectOrModuleDialog";
  }

  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    return mySplitter;
  }
}
