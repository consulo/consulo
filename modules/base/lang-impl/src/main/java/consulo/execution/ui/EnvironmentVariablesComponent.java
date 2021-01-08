/*
 * Copyright 2013-2021 consulo.io
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
package consulo.execution.ui;

import com.intellij.execution.ExecutionBundle;
import com.intellij.execution.configuration.EnvironmentVariablesData;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.util.EnvVariablesTable;
import com.intellij.execution.util.EnvironmentVariable;
import com.intellij.openapi.ui.DialogBuilder;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.util.containers.ContainerUtil;
import consulo.awt.TargetAWT;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.Component;
import consulo.ui.PseudoComponent;
import consulo.ui.TextBoxWithExtensions;
import consulo.ui.annotation.RequiredUIAccess;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author VISTALL
 * @since 08/01/2021
 */
public class EnvironmentVariablesComponent implements PseudoComponent {
  private class MyEnvironmentVariablesDialog extends DialogWrapper {
    private final EnvVariablesTable myEnvVariablesTable;
    private final JCheckBox myUseDefaultCb = new JCheckBox(ExecutionBundle.message("env.vars.checkbox.title"));
    private final JPanel myWholePanel = new JPanel(new BorderLayout());

    protected MyEnvironmentVariablesDialog(JComponent component) {
      super(component, true);
      myEnvVariablesTable = new EnvVariablesTable();
      myEnvVariablesTable.setValues(convertToVariables(myData.getEnvs(), false));

      myUseDefaultCb.setSelected(isPassParentEnvs());
      myWholePanel.add(myEnvVariablesTable.getComponent(), BorderLayout.CENTER);
      JPanel useDefaultPanel = new JPanel(new BorderLayout());
      useDefaultPanel.add(myUseDefaultCb, BorderLayout.CENTER);
      HyperlinkLabel showLink = new HyperlinkLabel(ExecutionBundle.message("env.vars.show.system"));
      useDefaultPanel.add(showLink, BorderLayout.EAST);
      showLink.addHyperlinkListener(e -> {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
          showParentEnvironmentDialog(MyEnvironmentVariablesDialog.this.getWindow());
        }
      });

      myWholePanel.add(useDefaultPanel, BorderLayout.SOUTH);
      setTitle(ExecutionBundle.message("environment.variables.dialog.title"));
      init();
    }

    @Override
    @Nullable
    protected JComponent createCenterPanel() {
      return myWholePanel;
    }

    @Override
    protected void doOKAction() {
      myEnvVariablesTable.stopEditing();
      final Map<String, String> envs = new LinkedHashMap<>();
      for (EnvironmentVariable variable : myEnvVariablesTable.getEnvironmentVariables()) {
        envs.put(variable.getName(), variable.getValue());
      }
      setEnvs(envs);
      setPassParentEnvs(myUseDefaultCb.isSelected());
      super.doOKAction();
    }
  }

  private final TextBoxWithExtensions myTextBox;

  private EnvironmentVariablesData myData = EnvironmentVariablesData.DEFAULT;

  public EnvironmentVariablesComponent() {
    myTextBox = TextBoxWithExtensions.create();
    myTextBox.setEditable(false);
    myTextBox.setExtensions(new TextBoxWithExtensions.Extension(false, PlatformIconGroup.generalInlineVariables(), PlatformIconGroup.generalInlineVariablesHover(), event -> {
      new MyEnvironmentVariablesDialog((JComponent)TargetAWT.to(event.getComponent())).showAsync();
    }));
  }

  public static void showParentEnvironmentDialog(@Nonnull java.awt.Component parent) {
    EnvVariablesTable table = new EnvVariablesTable();
    table.setValues(convertToVariables(new TreeMap<>(new GeneralCommandLine().getParentEnvironment()), true));
    table.getActionsPanel().setVisible(false);
    DialogBuilder builder = new DialogBuilder(parent);
    builder.setTitle(ExecutionBundle.message("environment.variables.system.dialog.title"));
    builder.centerPanel(table.getComponent());
    builder.addCloseButton();
    builder.show();
  }

  private static List<EnvironmentVariable> convertToVariables(Map<String, String> map, final boolean readOnly) {
    return ContainerUtil.map(map.entrySet(), entry -> new EnvironmentVariable(entry.getKey(), entry.getValue(), readOnly) {
      @Override
      public boolean getNameIsWriteable() {
        return !readOnly;
      }
    });
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public Component getComponent() {
    return myTextBox;
  }

  public void setEnvs(@Nonnull Map<String, String> envs) {
    myData = EnvironmentVariablesData.create(envs, myData.isPassParentEnvs());
  }

  @Nonnull
  public Map<String, String> getEnvs() {
    return myData.getEnvs();
  }

  public boolean isPassParentEnvs() {
    return myData.isPassParentEnvs();
  }

  public void setPassParentEnvs(final boolean passParentEnvs) {
    myData = EnvironmentVariablesData.create(myData.getEnvs(), false);
  }

  @Nonnull
  public EnvironmentVariablesData getEnvData() {
    return myData;
  }

  public void setEnvData(@Nonnull EnvironmentVariablesData envData) {
    myData = envData;
  }
}
