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

import com.intellij.ide.IdeBundle;
import com.intellij.ide.impl.ProjectUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.ui.LabeledComponent;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.platform.LocationNameFieldsBinding;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.SeparatorComponent;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.Consumer;
import lombok.val;
import org.consulo.ide.eap.EarlyAccessProgramDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mustbe.consulo.ide.impl.ui.DListItem;
import org.mustbe.consulo.ide.impl.ui.DListWithChildren;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

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
  private DListWithChildren myListWithChildren;

  private JComponent myConfigurationPanel;
  private NewModuleBuilderProcessor myProcessor;

  public NewProjectOrModuleDialogWithSetup(@Nullable Project project, @Nullable VirtualFile virtualFile) {
    super(project, true);
    myModuleCreation = virtualFile != null;

    setTitle(myModuleCreation ? IdeBundle.message("title.add.module") : IdeBundle.message("title.new.project"));

    val context = new NewModuleContext();

    for (NewModuleBuilder newModuleBuilder : NewModuleBuilder.EP_NAME.getExtensions()) {
      newModuleBuilder.setupContext(context);
    }

    DListItem.Builder root = DListItem.builder();

    Map<String, DListItem.Builder> map = new HashMap<String, DListItem.Builder>();

    for (Pair<String[], NewModuleBuilderProcessor> pair : context.getSetup()) {
      String[] path = pair.getFirst();
      NewModuleBuilderProcessor processor = pair.getSecond();

      DListItem.Builder prev = root;
      for (int i = 0; i < path.length; i++) {
        String item = path[i];

        DListItem.Builder builder = map.get(item);
        if (builder == null) {
          Pair<String, Icon> itemInfo = context.getItem(item);

          map.put(item, builder = DListItem.builder().withName(itemInfo.getFirst()).withIcon(itemInfo.getSecond()).withAttach(processor));

          prev.withItems(builder);
        }

        prev = builder;
      }
    }

    mySplitter = new JBSplitter(0.3f);
    mySplitter.setSplitterProportionKey("#NewProjectOrModuleDialogWithSetup.Splitter");

    myListWithChildren = new DListWithChildren();
    myListWithChildren.select(root.create());

    mySplitter.setFirstComponent(new JBScrollPane(myListWithChildren));

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

    final JPanel etcPanel = new JPanel(new BorderLayout());

    panel.add(etcPanel);

    final JPanel nullPanel = new JPanel(new BorderLayout());

    mySplitter.setSecondComponent(nullPanel);

    myListWithChildren.setConsumer(new Consumer<DListItem>() {
      @Override
      public void consume(DListItem dListItem) {
        NewModuleBuilderProcessor processor = dListItem == null ? null : (NewModuleBuilderProcessor)dListItem.getAttach();

        if(myConfigurationPanel != null) {
          etcPanel.remove(myConfigurationPanel);
          myConfigurationPanel = null;
        }

        myProcessor = processor;

        if (dListItem != null) {

          if (processor != null) {
            myConfigurationPanel = processor.createConfigurationPanel();
            etcPanel.add(myConfigurationPanel, BorderLayout.NORTH);
          }

          mySplitter.setSecondComponent(panel);
        }
        else {
          mySplitter.setSecondComponent(nullPanel);
        }

        setOKActionEnabled(myProcessor != null);
      }
    });

    setOKActionEnabled(false);
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

  @Nullable
  @Override
  public JComponent getPreferredFocusedComponent() {
    return myListWithChildren;
  }

  @Override
  public boolean isModuleCreation() {
    return myModuleCreation;
  }

  @Override
  protected String getDimensionServiceKey() {
    setSize(600, 400);
    return "#NewProjectOrModuleDialogWithSetup";
  }

  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    return mySplitter;
  }

  @Override
  @SuppressWarnings("unchecked")
  protected void postSetupModule(@NotNull ModifiableRootModel modifiableRootModel) {
    if (myProcessor != null) {
      myProcessor.setupModule(myConfigurationPanel, modifiableRootModel);
    }
  }
}
