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
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.platform.LocationNameFieldsBinding;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.ListCellRendererWrapper;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.SeparatorComponent;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBList;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import consulo.ide.welcomeScreen.BaseWelcomeScreenPanel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

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

  private JBList myList;

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

    NewModuleBuilder.EP_NAME.composite().setupContext(context);

    CollectionListModel<Object> model = new CollectionListModel<>();
    myList = new JBList(model);
    myList.setCellRenderer(new ListCellRendererWrapper() {
      @Override
      public void customize(JList list, Object value, int index, boolean selected, boolean hasFocus) {
        setFont(UIUtil.getFont(UIUtil.FontSize.BIGGER, null));

        if (value instanceof NewModuleContext.Group) {
          setText(((NewModuleContext.Group)value).getName());
          setSeparator();
        }
        else if (value instanceof NewModuleContext.Item) {
          setIcon(((NewModuleContext.Item)value).getIcon());
          setText(((NewModuleContext.Item)value).getName());
        }
      }

      @Override
      public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        Component component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        Dimension preferredSize = component.getPreferredSize();
        component.setPreferredSize(new Dimension(preferredSize.width, JBUI.scale(25)));
        return component;
      }
    });

    NewModuleContext.Group[] groups = context.getGroups();

    for (NewModuleContext.Group group : groups) {
      model.add(group);

      for (NewModuleContext.Item item : group.getItems()) {
        model.add(item);
      }
    }
    return ScrollPaneFactory.createScrollPane(myList, true);
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
    JBLabel nodeLabel = new JBLabel("Please select project type", UIUtil.ComponentStyle.SMALL, UIUtil.FontColor.BRIGHTER);
    nodeLabel.setHorizontalAlignment(SwingConstants.CENTER);
    nullPanel.add(nodeLabel, BorderLayout.CENTER);

    myRightPanel = new JPanel(new BorderLayout());
    myRightPanel.add(nullPanel, BorderLayout.CENTER);

    myList.addListSelectionListener(e -> {
      Object selectedValue = myList.getSelectedValue();

      myProcessor = selectedValue instanceof NewModuleContext.Item ? ((NewModuleContext.Item)selectedValue).getProcessor() : null;

      myConfigurationPanel = nullPanel;
      etcPanel.removeAll();

      myRightPanel.removeAll();

      if (selectedValue instanceof NewModuleContext.Item) {
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
