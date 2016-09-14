/*
 * Copyright 2013-2016 must-be.org
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
package consulo.ide.newProject;

import com.intellij.ide.impl.ProjectUtil;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.LabeledComponent;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.platform.LocationNameFieldsBinding;
import com.intellij.ui.SeparatorComponent;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.Consumer;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import consulo.ide.impl.NewModuleBuilder;
import consulo.ide.impl.NewModuleBuilderProcessor;
import consulo.ide.impl.NewModuleContext;
import consulo.ide.impl.ui.DListItem;
import consulo.ide.impl.ui.DListWithChildren;
import consulo.ide.welcomeScreen.BaseWelcomeScreenPanel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * @author VISTALL
 * @since 14-Sep-16
 */
public abstract class NewProjectPanel extends BaseWelcomeScreenPanel {
  private JPanel myRightPanel;
  private JTextField myNameField;
  private TextFieldWithBrowseButton myLocationField;

  private JComponent myConfigurationPanel;
  private NewModuleBuilderProcessor myProcessor;

  @Nullable
  private Project myProject;
  @Nullable
  private VirtualFile myVirtualFile;

  public NewProjectPanel(@NotNull Disposable parentDisposable, @Nullable Project project, @Nullable VirtualFile virtualFile) {
    super(parentDisposable);
    myProject = project;
    myVirtualFile = virtualFile;
  }

  @NotNull
  public String getLocationText() {
    return myLocationField.getText();
  }

  @Nullable
  public String getNameText() {
    return myNameField.getText();
  }

  public NewModuleBuilderProcessor getProcessor() {
    return myProcessor;
  }

  public JComponent getConfigurationPanel() {
    return myConfigurationPanel;
  }

  public boolean isModuleCreation() {
    return myVirtualFile != null;
  }

  protected abstract JComponent createSouthPanel();

  public abstract void setOKActionEnabled(boolean enabled);

  @NotNull
  @Override
  protected JComponent createLeftComponent(Disposable parentDisposable) {
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

    DListWithChildren list = new DListWithChildren();
    list.select(root.create());

    return list;
  }

  @NotNull
  @Override
  protected JComponent createRightComponent() {
    final JPanel panel = new JPanel(new VerticalFlowLayout());

    myNameField = new JTextField();
    myLocationField = new TextFieldWithBrowseButton();

    panel.add(LabeledComponent.create(myNameField, "Name").setLabelLocation(BorderLayout.WEST));

    if (myVirtualFile != null) {
      myNameField.setText(myVirtualFile.getName());
    }
    else {
      panel.add(LabeledComponent.create(myLocationField, "Path").setLabelLocation(BorderLayout.WEST));

      new LocationNameFieldsBinding(myProject, myLocationField, myNameField, ProjectUtil.getBaseDir(), "Select Directory");
    }

    panel.add(new SeparatorComponent());

    final JPanel etcPanel = new JPanel(new BorderLayout());

    panel.add(etcPanel);

    final JPanel nullPanel = new JPanel(new BorderLayout());
    JBLabel nodeLabel =
            new JBLabel("Please select project type, groups are bold (double click for open)", UIUtil.ComponentStyle.SMALL, UIUtil.FontColor.BRIGHTER);
    nodeLabel.setHorizontalAlignment(SwingConstants.CENTER);
    nullPanel.add(nodeLabel, BorderLayout.CENTER);

    myRightPanel = new JPanel(new BorderLayout());
    myRightPanel.add(nullPanel, BorderLayout.CENTER);

    DListWithChildren listWithChildren = (DListWithChildren)myLeftComponent;
    listWithChildren.setConsumer(new Consumer<DListItem>() {
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

    JPanel root = new JPanel(new BorderLayout());
    root.add(myRightPanel, BorderLayout.CENTER);
    JComponent southPanel = createSouthPanel();
    assert southPanel != nullPanel;
    southPanel.setBorder(JBUI.Borders.empty(DialogWrapper.ourDefaultBorderInsets));
    root.add(southPanel, BorderLayout.SOUTH);
    return root;
  }
}
