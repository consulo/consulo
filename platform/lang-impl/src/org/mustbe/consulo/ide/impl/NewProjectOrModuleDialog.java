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
import com.intellij.openapi.application.Result;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.module.ModifiableModuleModel;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.ui.LabeledComponent;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.openapi.ui.WholeWestDialogWrapper;
import com.intellij.openapi.util.Couple;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.platform.LocationNameFieldsBinding;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.SeparatorComponent;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.Consumer;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mustbe.consulo.RequiredDispatchThread;
import org.mustbe.consulo.RequiredReadAction;
import org.mustbe.consulo.RequiredWriteAction;
import org.mustbe.consulo.ide.impl.ui.DListItem;
import org.mustbe.consulo.ide.impl.ui.DListWithChildren;
import org.mustbe.consulo.roots.impl.ExcludedContentFolderTypeProvider;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * @author VISTALL
 * @since 04.06.14
 */
public class NewProjectOrModuleDialog extends WholeWestDialogWrapper {
  private final boolean myModuleCreation;

  private JPanel myRightPanel;
  private JTextField myNameField;
  private TextFieldWithBrowseButton myLocationField;
  private DListWithChildren myListWithChildren;

  private JComponent myConfigurationPanel;
  private NewModuleBuilderProcessor myProcessor;

  public NewProjectOrModuleDialog(@Nullable Project project, @Nullable VirtualFile virtualFile) {
    super(project, true);
    myModuleCreation = virtualFile != null;

    setTitle(myModuleCreation ? IdeBundle.message("title.add.module") : IdeBundle.message("title.new.project"));

    NewModuleContext context = new NewModuleContext();

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

    myListWithChildren = new DListWithChildren();
    myListWithChildren.select(root.create());

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
    JBLabel nodeLabel = new JBLabel("Please select project type, groups are bold (double click for open)", UIUtil.ComponentStyle.SMALL, UIUtil.FontColor.BRIGHTER);
    nodeLabel.setHorizontalAlignment(SwingConstants.CENTER);
    nullPanel.add(nodeLabel, BorderLayout.CENTER);

    myRightPanel = new JPanel(new BorderLayout());
    myRightPanel.add(nullPanel, BorderLayout.CENTER);

    myListWithChildren.setConsumer(new Consumer<DListItem>() {
      @Override
      public void consume(final DListItem dListItem) {
        myProcessor = dListItem == null ? null : (NewModuleBuilderProcessor)dListItem.getAttach();

        myConfigurationPanel = nullPanel;
        etcPanel.removeAll();

        myRightPanel.removeAll();

        if (dListItem != null) {
          if (myProcessor != null) {
            myConfigurationPanel = myProcessor.createConfigurationPanel();
            etcPanel.add(myConfigurationPanel, BorderLayout.NORTH);
          }
          myRightPanel.add(panel, BorderLayout.CENTER);
        }

        if (myRightPanel.getComponentCount() == 0) {
          myRightPanel.add(nullPanel, BorderLayout.CENTER);
        }
        myRightPanel.validate();
        myRightPanel.repaint();

        setOKActionEnabled(myProcessor != null);
      }
    });

    setOKActionEnabled(false);
    init();
  }

  @NotNull
  @RequiredReadAction
  public Module doCreate(@NotNull final Project project, @NotNull final VirtualFile baseDir) {
    return doCreate(ModuleManager.getInstance(project).getModifiableModel(), baseDir, true);
  }

  @NotNull
  public Module doCreate(@NotNull final ModifiableModuleModel modifiableModel, @NotNull final VirtualFile baseDir, final boolean requireModelCommit) {
    return new WriteAction<Module>() {
      @Override
      protected void run(Result<Module> result) throws Throwable {
        result.setResult(doCreateImpl(modifiableModel, baseDir, requireModelCommit));
      }
    }.execute().getResultObject();
  }

  @SuppressWarnings("unchecked")
  @NotNull
  @RequiredWriteAction
  private Module doCreateImpl(@NotNull final ModifiableModuleModel modifiableModel, @NotNull final VirtualFile baseDir, boolean requireModelCommit) {
    String name = StringUtil.notNullize(getNameText(), baseDir.getName());

    Module newModule = modifiableModel.newModule(name, baseDir.getPath());

    ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(newModule);
    ModifiableRootModel modifiableModelForModule = moduleRootManager.getModifiableModel();
    ContentEntry contentEntry = modifiableModelForModule.addContentEntry(baseDir);

    if (!isModuleCreation()) {
      contentEntry.addFolder(contentEntry.getUrl() + "/" + Project.DIRECTORY_STORE_FOLDER, ExcludedContentFolderTypeProvider.getInstance());
    }

    if (myProcessor != null) {
      myProcessor.setupModule(myConfigurationPanel, contentEntry, modifiableModelForModule);
    }

    modifiableModelForModule.commit();

    if (requireModelCommit) {
      modifiableModel.commit();
    }

    baseDir.refresh(true, true);
    return newModule;
  }

  @NotNull
  public String getLocationText() {
    return myLocationField.getText();
  }

  @Nullable
  public String getNameText() {
    return myNameField.getText();
  }

  @Nullable
  @Override
  public JComponent getPreferredFocusedComponent() {
    return myListWithChildren;
  }

  public boolean isModuleCreation() {
    return myModuleCreation;
  }

  @RequiredDispatchThread
  @NotNull
  @Override
  public Couple<JComponent> createSplitterComponents(JPanel rootPanel) {
    return Couple.<JComponent>of(ScrollPaneFactory.createScrollPane(myListWithChildren, true), myRightPanel);
  }

  @Override
  public float getSplitterDefaultValue() {
    return 0.3f;
  }

  @Nullable
  @Override
  protected String getDimensionServiceKey() {
    return "#NewProjectOrModuleDialog.size";
  }

  @Override
  public Dimension getDefaultSize() {
    return new Dimension(600, 300);
  }

  @NotNull
  @Override
  public String getSplitterKey() {
    return "#NewProjectOrModuleDialog.splitter";
  }
}
