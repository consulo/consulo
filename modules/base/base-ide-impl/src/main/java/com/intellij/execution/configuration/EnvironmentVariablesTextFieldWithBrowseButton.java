/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.execution.configuration;

import com.google.common.collect.ImmutableMap;
import com.intellij.execution.ExecutionBundle;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.util.EnvVariablesTable;
import com.intellij.execution.util.EnvironmentVariable;
import com.intellij.openapi.ui.DialogBuilder;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.UserActivityProviderComponent;
import com.intellij.util.EventDispatcher;
import com.intellij.util.containers.ContainerUtil;
import consulo.awt.TargetAWT;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.TextBoxWithExtensions;
import consulo.util.lang.StringUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class EnvironmentVariablesTextFieldWithBrowseButton implements UserActivityProviderComponent {

  private final EventDispatcher<ChangeListener> myListeners = EventDispatcher.create(ChangeListener.class);

  private EnvironmentVariablesData myData = EnvironmentVariablesData.DEFAULT;

  private final TextBoxWithExtensions myTextBox;

  public EnvironmentVariablesTextFieldWithBrowseButton() {
    myTextBox = TextBoxWithExtensions.create();
    myTextBox.setPlaceholder(LocalizeValue.localizeTODO("Separate variables with semicolon: VAR=value; VAR1=value1"));

    myTextBox.addLastExtension(new TextBoxWithExtensions.Extension(false, PlatformIconGroup.generalInlineVariables(), PlatformIconGroup.generalInlineVariablesHover(),
                                                                   event -> new MyEnvironmentVariablesDialog().showAsync()));

    myTextBox.addValueListener(event -> {
      if (!StringUtil.equals(stringifyEnvs(myData), event.getValue())) {
        Map<String, String> textEnvs = EnvVariablesTable.parseEnvsFromText(event.getValue());
        myData = myData.with(textEnvs);
        fireStateChanged();
      }
    });
  }

  @Nonnull
  public consulo.ui.Component getComponent() {
    return myTextBox;
  }

  /**
   * @return unmodifiable Map instance
   */
  @Nonnull
  public Map<String, String> getEnvs() {
    return myData.getEnvs();
  }

  /**
   * @param envs Map instance containing user-defined environment variables
   *             (iteration order should be reliable user-specified, like {@link LinkedHashMap} or {@link ImmutableMap})
   */
  public void setEnvs(@Nonnull Map<String, String> envs) {
    setData(EnvironmentVariablesData.create(envs, myData.isPassParentEnvs()));
  }

  @Nonnull
  public EnvironmentVariablesData getData() {
    return myData;
  }

  public void setData(@Nonnull EnvironmentVariablesData data) {
    EnvironmentVariablesData oldData = myData;
    myData = data;
    myTextBox.setValue(stringifyEnvs(data.getEnvs()));
    if (!oldData.equals(data)) {
      fireStateChanged();
    }
  }

  @Nonnull
  protected String stringifyEnvs(@Nonnull EnvironmentVariablesData evd) {
    if (evd.getEnvs().isEmpty()) {
      return "";
    }
    StringBuilder buf = new StringBuilder();
    for (Map.Entry<String, String> entry : evd.getEnvs().entrySet()) {
      if (buf.length() > 0) {
        buf.append(";");
      }
      buf.append(StringUtil.escapeChar(entry.getKey(), ';')).append("=").append(StringUtil.escapeChar(entry.getValue(), ';'));
    }
    return buf.toString();
  }

  @Nonnull
  private static String stringifyEnvs(@Nonnull Map<String, String> envs) {
    if (envs.isEmpty()) {
      return "";
    }
    StringBuilder buf = new StringBuilder();
    for (Map.Entry<String, String> entry : envs.entrySet()) {
      if (buf.length() > 0) {
        buf.append(";");
      }
      buf.append(entry.getKey()).append("=").append(entry.getValue());
    }
    return buf.toString();
  }

  public boolean isPassParentEnvs() {
    return myData.isPassParentEnvs();
  }

  public void setPassParentEnvs(boolean passParentEnvs) {
    setData(EnvironmentVariablesData.create(myData.getEnvs(), passParentEnvs));
  }

  @Override
  public void addChangeListener(ChangeListener changeListener) {
    myListeners.addListener(changeListener);
  }

  @Override
  public void removeChangeListener(ChangeListener changeListener) {
    myListeners.removeListener(changeListener);
  }

  private void fireStateChanged() {
    myListeners.getMulticaster().stateChanged(new ChangeEvent(this));
  }

  public static void showParentEnvironmentDialog(@Nonnull Component parent) {
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

  private class MyEnvironmentVariablesDialog extends DialogWrapper {
    private final EnvVariablesTable myEnvVariablesTable;
    private final JCheckBox myUseDefaultCb = new JCheckBox(ExecutionBundle.message("env.vars.checkbox.title"));
    private final JPanel myWholePanel = new JPanel(new BorderLayout());

    protected MyEnvironmentVariablesDialog() {
      super(TargetAWT.to(getComponent()), true);
      myEnvVariablesTable = new EnvVariablesTable();
      myEnvVariablesTable.setValues(convertToVariables(myData.getEnvs(), false));

      myUseDefaultCb.setSelected(isPassParentEnvs());
      myWholePanel.add(myEnvVariablesTable.getComponent(), BorderLayout.CENTER);
      JPanel useDefaultPanel = new JPanel(new BorderLayout());
      useDefaultPanel.add(myUseDefaultCb, BorderLayout.CENTER);
      HyperlinkLabel showLink = new HyperlinkLabel(ExecutionBundle.message("env.vars.show.system"));
      useDefaultPanel.add(showLink, BorderLayout.EAST);
      showLink.addHyperlinkListener(new HyperlinkListener() {
        @Override
        public void hyperlinkUpdate(HyperlinkEvent e) {
          if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            showParentEnvironmentDialog(MyEnvironmentVariablesDialog.this.getWindow());
          }
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
}
