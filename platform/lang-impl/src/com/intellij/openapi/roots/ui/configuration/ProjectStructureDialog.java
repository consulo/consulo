/*
 * Copyright 2013-2015 must-be.org
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
package com.intellij.openapi.roots.ui.configuration;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ex.WholeWestSingleConfigurableEditor;
import com.intellij.openapi.options.newEditor.OptionsEditorDialog;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.openapi.util.Couple;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.impl.StaticAnchoredButton;
import com.intellij.openapi.wm.impl.StripeButtonUI;
import com.intellij.ui.border.CustomLineBorder;
import com.intellij.util.Consumer;
import com.intellij.util.EmptyConsumer;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.update.Activatable;
import com.intellij.util.ui.update.UiNotifyConnector;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.plaf.ButtonUI;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 28.07.2015
 */
public class ProjectStructureDialog extends WholeWestSingleConfigurableEditor {

  private final List<Configurable> myName2Config = new ArrayList<Configurable>();
  private final List<StaticAnchoredButton> myButtons = new ArrayList<StaticAnchoredButton>();
  private JPanel myListBoxPanel;
  private JPanel myRightPanel;
  private ProjectStructureConfigurable myStructureConfigurable;

  public static boolean show(@NotNull Project project) {
    return show(project, EmptyConsumer.<ProjectStructureConfigurable>getInstance());
  }

  public static boolean show(@NotNull Project project, final Consumer<ProjectStructureConfigurable> configurableConsumer) {
    final ProjectStructureConfigurable configurable = ProjectStructureConfigurable.getInstance(project);
    ProjectStructureDialog dialog =
            new ProjectStructureDialog(project, configurable, OptionsEditorDialog.DIMENSION_KEY, true, IdeModalityType.PROJECT, configurable);
    if (configurableConsumer != null) {
      new UiNotifyConnector.Once(dialog.getContentPane(), new Activatable.Adapter() {
        @Override
        public void showNotify() {
          configurableConsumer.consume(configurable);
        }
      });
    }
    return dialog.showAndGet();
  }

  public ProjectStructureDialog(@NotNull Project project,
                                Configurable configurable,
                                @NonNls String dimensionKey,
                                boolean showApplyButton,
                                IdeModalityType ideModalityType,
                                ProjectStructureConfigurable structureConfigurable) {
    super(project, configurable, dimensionKey, showApplyButton, ideModalityType, true);
    myStructureConfigurable = structureConfigurable;

    boolean isDefaultProject = project.isDefault();

    if (!isDefaultProject) {
      addConfigurable(structureConfigurable.getProjectConfigurable());
      addConfigurable(structureConfigurable.getModulesConfigurable());
    }
    addConfigurable(structureConfigurable.getProjectLibrariesConfigurable());

    if (!isDefaultProject) {
      addConfigurable(structureConfigurable.getArtifactsStructureConfigurable());
    }

    addConfigurable(structureConfigurable.getSdkConfigurable());

    structureConfigurable.setName2Config(myName2Config);

    structureConfigurable.setProjectStructureDialog(this);

    init();

    getConfigurable().reset();
  }

  private void addConfigurable(Configurable configurable) {
    myName2Config.add(configurable);
  }

  @Override
  public float getSplitterDefaultValue() {
    return 0.2f;
  }

  @Override
  protected String getDimensionServiceKey() {
    return OptionsEditorDialog.DIMENSION_KEY;
  }

  @NotNull
  @Override
  public String getSplitterKey() {
    return "ProjectStructureDialog.Splitter";
  }

  @Override
  public Dimension getDefaultSize() {
    return new Dimension(1024, 768);
  }

  @NotNull
  @Override
  public Couple<JComponent> createSplitterComponents(final JPanel rootPanel) {
    final JPanel tabPanel = new JPanel(new VerticalFlowLayout(VerticalFlowLayout.TOP, 0, 0, true, false));
    tabPanel.setBorder(new CustomLineBorder(0, 0, 0, 1));
    myListBoxPanel = new JPanel(new CardLayout());
    myRightPanel = new JPanel(new CardLayout());

    final JPanel leftPanel = new JPanel(new BorderLayout());
    ButtonGroup buttonGroup = new ButtonGroup();
    ItemListener itemListener = new ItemListener() {
      @Override
      public void itemStateChanged(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED) {
          StaticAnchoredButton source = (StaticAnchoredButton)e.getSource();

          CardLayout listBox = (CardLayout)myListBoxPanel.getLayout();
          CardLayout right = (CardLayout)myRightPanel.getLayout();

          Object clientProperty = source.getClientProperty(Configurable.class);
          if (clientProperty instanceof WholeWestConfigurable) {
            listBox.show(myListBoxPanel, ((WholeWestConfigurable)clientProperty).getDisplayName());
            right.show(myRightPanel, ((WholeWestConfigurable)clientProperty).getDisplayName());
            myStructureConfigurable.setSelectedConfigurable((Configurable)clientProperty);
          }
        }
      }
    };

    StaticAnchoredButton firstButton = null;
    for (Configurable sub : myName2Config) {
      StaticAnchoredButton button = new StaticAnchoredButton(sub.getDisplayName(), ToolWindowAnchor.LEFT);
      if (firstButton == null) {
        firstButton = button;
      }

      button.addItemListener(itemListener);
      button.setFont(UIUtil.getLabelFont(UIUtil.FontSize.SMALL));
      button.setBackground(new Color(247, 243, 239));
      button.setUI((ButtonUI)StripeButtonUI.createUI(button));

      tabPanel.add(button);

      button.putClientProperty(Configurable.class, sub);

      if (sub instanceof WholeWestConfigurable) {
        Couple<JComponent> splitterComponents = ((WholeWestConfigurable)sub).createSplitterComponents();

        myListBoxPanel.add(splitterComponents.getFirst(), sub.getDisplayName());
        myRightPanel.add(splitterComponents.getSecond(), sub.getDisplayName());
      }

      buttonGroup.add(button);
      myButtons.add(button);
    }

    if (buttonGroup.getSelection() == null) {
      assert firstButton != null;
      firstButton.setSelected(true);
    }

    leftPanel.add(tabPanel, BorderLayout.WEST);
    leftPanel.add(myListBoxPanel, BorderLayout.CENTER);
    return Couple.<JComponent>of(leftPanel, myRightPanel);
  }

  public void select(@NotNull Configurable configurable) {
    for (StaticAnchoredButton button : myButtons) {
      Object clientProperty = button.getClientProperty(Configurable.class);
      if (clientProperty == configurable) {
        button.setSelected(true);
      }
    }
  }

  @NotNull
  public Configurable getSelectedConfigurable() {
    for (StaticAnchoredButton button : myButtons) {
      if (button.isSelected()) {
        return (Configurable)button.getClientProperty(Configurable.class);
      }
    }
    throw new UnsupportedOperationException();
  }
}
