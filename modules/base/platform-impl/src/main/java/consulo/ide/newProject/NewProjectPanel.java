/*
 * Copyright 2013-2016 consulo.io
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
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.platform.LocationNameFieldsBinding;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.SeparatorComponent;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBList;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import consulo.annotations.RequiredDispatchThread;
import consulo.ide.welcomeScreen.BaseWelcomeScreenPanel;
import consulo.ui.SwingUIDecorator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;

/**
 * @author VISTALL
 * @since 14-Sep-16
 */
public abstract class NewProjectPanel extends BaseWelcomeScreenPanel<VirtualFile> {
  private JPanel myRightPanel;
  private JTextField myNameField;
  private TextFieldWithBrowseButton myLocationField;

  private JComponent myConfigurationPanel;
  private NewModuleBuilderProcessor myProcessor;

  @Nullable
  private Project myProject;

  private JBList<Object> myList;

  @RequiredDispatchThread
  public NewProjectPanel(@Nonnull Disposable parentDisposable, @Nullable Project project, @Nullable VirtualFile virtualFile) {
    super(parentDisposable, virtualFile);
    myProject = project;
  }

  @Nonnull
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
    return myParam != null;
  }

  protected abstract JComponent createSouthPanel();

  public abstract void setOKActionEnabled(boolean enabled);

  @Nonnull
  @Override
  protected JComponent createLeftComponent(@Nonnull Disposable parentDisposable, VirtualFile param) {
    NewModuleContext context = new NewModuleContext();

    NewModuleBuilder.EP_NAME.composite().setupContext(context);

    CollectionListModel<Object> model = new CollectionListModel<>();
    myList = new JBList<>(model);
    myList.setBackground(SwingUIDecorator.get(SwingUIDecorator::getSidebarColor));
    myList.setCellRenderer(new ColoredListCellRenderer<Object>() {
      @Override
      protected void customizeCellRenderer(@Nonnull JList list, Object value, int index, boolean selected, boolean hasFocus) {
        setFont(UIUtil.getFont(UIUtil.FontSize.BIGGER, null));

        if (value instanceof NewModuleContext.Group) {
          setSeparator(StringUtil.nullize(((NewModuleContext.Group)value).getName()));
        }
        else if (value instanceof NewModuleContext.Item) {
          setIcon(((NewModuleContext.Item)value).getIcon());
          append(((NewModuleContext.Item)value).getName());
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
      // do not add simple line
      if (!(groups.length == 1 && group.getId().equals(NewModuleContext.UGROUPED))) {
        model.add(group);
      }

      for (NewModuleContext.Item item : group.getItems()) {
        model.add(item);
      }
    }
    return ScrollPaneFactory.createScrollPane(myList, true);
  }

  @RequiredDispatchThread
  @Nonnull
  @Override
  protected JComponent createRightComponent(VirtualFile param) {
    final JPanel panel = new JPanel(new VerticalFlowLayout());

    myNameField = new JTextField();
    myLocationField = new TextFieldWithBrowseButton();

    panel.add(LabeledComponent.left(myNameField, "Name"));

    if (myParam != null) {
      myNameField.setText(myParam.getName());
    }
    else {
      panel.add(LabeledComponent.left(myLocationField, "Path"));

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
