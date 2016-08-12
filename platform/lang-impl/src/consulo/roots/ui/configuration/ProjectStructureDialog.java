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
package consulo.roots.ui.configuration;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ex.WholeWestSingleConfigurableEditor;
import com.intellij.openapi.options.newEditor.OptionsEditorDialog;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ui.configuration.ProjectStructureConfigurable;
import com.intellij.openapi.util.Couple;
import com.intellij.openapi.util.Key;
import com.intellij.ui.border.CustomLineBorder;
import com.intellij.util.Consumer;
import com.intellij.util.EmptyConsumer;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.components.BorderLayoutPanel;
import com.intellij.util.ui.update.Activatable;
import com.intellij.util.ui.update.UiNotifyConnector;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mustbe.consulo.RequiredDispatchThread;
import consulo.roots.ui.StripeTabPanel;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 28.07.2015
 */
public class ProjectStructureDialog extends WholeWestSingleConfigurableEditor {
  private static final Key<Configurable> CONFIGURABLE_KEY = Key.create("configurable.key");

  private final List<Configurable> myName2Config = new ArrayList<Configurable>();
  private JPanel myRightPanel;
  private StripeTabPanel myStripeTabPanel;
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

    structureConfigurable.setConfigurablesForDispose(myName2Config);

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
    return JBUI.size(1024, 768);
  }

  @Nullable
  @Override
  protected Border createContentPaneBorder() {
    return JBUI.Borders.empty();
  }

  @Nullable
  @Override
  protected JComponent createSouthPanel() {
    JComponent southPanel = super.createSouthPanel();
    if(southPanel != null) {
      southPanel.setBorder(JBUI.Borders.empty(ourDefaultBorderInsets));
      BorderLayoutPanel borderLayoutPanel = JBUI.Panels.simplePanel(southPanel);
      borderLayoutPanel.setBorder(new CustomLineBorder(JBUI.scale(1), 0, 0, 0));
      return borderLayoutPanel;
    }
    return null;
  }

  @NotNull
  @Override
  @RequiredDispatchThread
  public Couple<JComponent> createSplitterComponents(final JPanel rootPanel) {
    myRightPanel = new JPanel(new CardLayout());

    myStripeTabPanel = new StripeTabPanel();
    myStripeTabPanel.addSelectListener(new StripeTabPanel.SelectListener() {
      @RequiredDispatchThread
      @Override
      public void selected(@NotNull StripeTabPanel.TabInfo tabInfo) {
        Configurable configurable = tabInfo.getUserData(CONFIGURABLE_KEY);
        CardLayout right = (CardLayout)myRightPanel.getLayout();

        if (configurable instanceof WholeWestConfigurable) {
          right.show(myRightPanel, configurable.getDisplayName());
          myStructureConfigurable.setSelectedConfigurable(configurable);
        }
      }
    });

    for (Configurable sub : myName2Config) {
      final JComponent component;
      if (sub instanceof WholeWestConfigurable) {
        Couple<JComponent> splitterComponents = ((WholeWestConfigurable)sub).createSplitterComponents();

        myRightPanel.add(splitterComponents.getSecond(), sub.getDisplayName());
        component = splitterComponents.getFirst();
      }
      else {
        component = new JPanel();
      }

      StripeTabPanel.TabInfo tabInfo = myStripeTabPanel.addTab(sub.getDisplayName(), component);
      tabInfo.putUserData(CONFIGURABLE_KEY, sub);
    }

    return Couple.<JComponent>of(myStripeTabPanel, myRightPanel);
  }

  @RequiredDispatchThread
  public void select(@NotNull Configurable configurable) {
    List<StripeTabPanel.TabInfo> tabs = myStripeTabPanel.getTabs();
    for (StripeTabPanel.TabInfo tab : tabs) {
      Configurable other = tab.getUserData(CONFIGURABLE_KEY);
      if(other == configurable) {
        tab.select();
        break;
      }
    }
  }

  @NotNull
  public Configurable getSelectedConfigurable() {
    List<StripeTabPanel.TabInfo> tabs = myStripeTabPanel.getTabs();
    for (StripeTabPanel.TabInfo tab : tabs) {
      if(tab.isSelected()) {
        Configurable data = tab.getUserData(CONFIGURABLE_KEY);
        assert data != null;
        return data;
      }
    }

    throw new UnsupportedOperationException();
  }
}
