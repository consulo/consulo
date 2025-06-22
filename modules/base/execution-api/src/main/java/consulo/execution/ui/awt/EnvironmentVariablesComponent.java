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
package consulo.execution.ui.awt;

import consulo.execution.configuration.EnvironmentVariablesData;
import consulo.execution.localize.ExecutionLocalize;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.UserActivityProviderComponent;
import consulo.ui.ex.awt.LabeledComponent;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import jakarta.annotation.Nonnull;
import org.jdom.Element;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import java.util.Map;
import java.util.Objects;

/**
 * @author anna
 * @since 2007-05-14
 */
public class EnvironmentVariablesComponent extends LabeledComponent<JComponent> implements UserActivityProviderComponent {
  private static final String ENVS = "envs";
  public static final String ENV = "env";
  public static final String NAME = "name";
  public static final String VALUE = "value";
  private static final String OPTION = "option";
  private static final String ENV_VARIABLES = "ENV_VARIABLES";

  private final EnvironmentVariablesTextFieldWithBrowseButton myEnvVars;

  @RequiredUIAccess
  public EnvironmentVariablesComponent() {
    super();
    myEnvVars = new EnvironmentVariablesTextFieldWithBrowseButton();
    setComponent((JComponent)TargetAWT.to(myEnvVars.getComponent()));
    setText(ExecutionLocalize.environmentVariablesComponentTitle().get());
  }

  public void setEnvs(@Nonnull Map<String, String> envs) {
    myEnvVars.setEnvs(envs);
  }

  @Nonnull
  public Map<String, String> getEnvs() {
    return myEnvVars.getEnvs();
  }

  public boolean isPassParentEnvs() {
    return myEnvVars.isPassParentEnvs();
  }

  public void setPassParentEnvs(final boolean passParentEnvs) {
    myEnvVars.setPassParentEnvs(passParentEnvs);
  }

  @Nonnull
  public EnvironmentVariablesData getEnvData() {
    return myEnvVars.getData();
  }

  public void setEnvData(@Nonnull EnvironmentVariablesData envData) {
    myEnvVars.setData(envData);
  }

  /**
   * Consider using {@link EnvironmentVariablesData#readExternal(Element)} instead for simplicity and better performance.
   */
  public static void readExternal(Element element, Map<String, String> envs) {
    final Element envsElement = element.getChild(ENVS);
    if (envsElement != null) {
      for (Element envElement : envsElement.getChildren(ENV)) {
        final String envName = envElement.getAttributeValue(NAME);
        final String envValue = envElement.getAttributeValue(VALUE);
        if (envName != null && envValue != null) {
          envs.put(envName, envValue);
        }
      }
    }
    else {
      for (Element o : element.getChildren(OPTION)) {
        if (Objects.equals(o.getAttributeValue(NAME), ENV_VARIABLES)) {
          splitVars(envs, o.getAttributeValue(VALUE));
          break;
        }
      }
    }
  }

  private static void splitVars(final Map<String, String> envs, final String val) {
    if (val != null) {
      final String[] envVars = val.split(";");
      for (String envVar : envVars) {
        final int idx = envVar.indexOf('=');
        if (idx > -1) {
          envs.put(envVar.substring(0, idx), idx < envVar.length() - 1 ? envVar.substring(idx + 1) : "");
        }
      }
    }
  }

  /**
   * Consider using {@link EnvironmentVariablesData#writeExternal(Element)} instead for simplicity and better performance.
   */
  public static void writeExternal(@Nonnull Element element, @Nonnull Map<String, String> envs) {
    final Element envsElement = new Element(ENVS);
    for (String envName : envs.keySet()) {
      final Element envElement = new Element(ENV);
      envElement.setAttribute(NAME, envName);
      envElement.setAttribute(VALUE, envs.get(envName));
      envsElement.addContent(envElement);
    }
    element.addContent(envsElement);
  }

  @Override
  public void addChangeListener(final ChangeListener changeListener) {
    myEnvVars.addChangeListener(changeListener);
  }

  @Override
  public void removeChangeListener(final ChangeListener changeListener) {
    myEnvVars.removeChangeListener(changeListener);
  }
}
