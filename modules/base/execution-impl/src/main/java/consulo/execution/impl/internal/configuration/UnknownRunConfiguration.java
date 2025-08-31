/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package consulo.execution.impl.internal.configuration;

import consulo.execution.RuntimeConfigurationException;
import consulo.execution.WithoutOwnBeforeRunSteps;
import consulo.execution.configuration.*;
import consulo.process.ExecutionException;
import consulo.execution.executor.Executor;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.execution.runner.ProgramRunner;
import consulo.configurable.ConfigurationException;
import consulo.execution.configuration.ui.SettingsEditor;
import consulo.project.Project;
import consulo.util.xml.serializer.InvalidDataException;
import consulo.util.xml.serializer.WriteExternalException;
import consulo.ui.image.Image;
import org.jdom.Attribute;
import org.jdom.Element;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author spleaner
 */
public class UnknownRunConfiguration implements RunConfiguration, WithoutOwnBeforeRunSteps {
  private final ConfigurationFactory myFactory;
  private Element myStoredElement;
  private String myName;
  private final Project myProject;

  private static final AtomicInteger myUniqueName = new AtomicInteger(1);
  private boolean myDoNotStore;

  public UnknownRunConfiguration(@Nonnull ConfigurationFactory factory, @Nonnull Project project) {
    myFactory = factory;
    myProject = project;
  }

  public void setDoNotStore(boolean b) {
    myDoNotStore = b;
  }

  @Override
  @Nullable
  public Image getIcon() {
    return null;
  }

  public boolean isDoNotStore() {
    return myDoNotStore;
  }

  @Override
  public ConfigurationFactory getFactory() {
    return myFactory;
  }

  @Override
  public void setName(String name) {
    myName = name;
  }

  @Nonnull
  @Override
  public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
    return new UnknownSettingsEditor();
  }

  @Override
  public Project getProject() {
    return myProject;
  }

  @Override
  @Nonnull
  public ConfigurationType getType() {
    return UnknownConfigurationType.INSTANCE;
  }

  @Override
  public ConfigurationPerRunnerSettings createRunnerSettings(ConfigurationInfoProvider provider) {
    return null;
  }

  @Override
  public SettingsEditor<ConfigurationPerRunnerSettings> getRunnerSettingsEditor(ProgramRunner runner) {
    return null;
  }

  @Override
  public RunConfiguration clone() {
    try {
      UnknownRunConfiguration cloned = (UnknownRunConfiguration) super.clone();
      return cloned;
    } catch (CloneNotSupportedException e) {
      return null;
    }
  }


  @Override
  public int getUniqueID() {
    return System.identityHashCode(this);
  }

  @Override
  public RunProfileState getState(@Nonnull Executor executor, @Nonnull ExecutionEnvironment env) throws ExecutionException {
    String factoryName = "";
    if (myStoredElement != null) {
      factoryName = myStoredElement.getAttributeValue("type");
    }
    throw new ExecutionException("Unknown run configuration type" + (factoryName.isEmpty() ? "" : " " + factoryName));
  }

  @Override
  public String getName() {
    if (myName == null) {
      myName = String.format("Unknown%s", myUniqueName.getAndAdd(1));
    }

    return myName;
  }

  @Override
  public void checkConfiguration() throws RuntimeConfigurationException {
    throw new RuntimeConfigurationException("Broken configuration due to unavailable plugin or invalid configuration data.");
  }

  @Override
  public void readExternal(Element element) throws InvalidDataException {
    myStoredElement = (Element) element.clone();
  }

  @Override
  public void writeExternal(Element element) throws WriteExternalException {
    if (myStoredElement != null) {
      List attributeList = myStoredElement.getAttributes();
      for (Object anAttributeList : attributeList) {
        Attribute a = (Attribute) anAttributeList;
        element.setAttribute(a.getName(), a.getValue());
      }

      List list = myStoredElement.getChildren();
      for (Object child : list) {
        Element c = (Element) child;
        element.addContent((Element) c.clone());
      }
    }
  }

  private static class UnknownSettingsEditor extends SettingsEditor<UnknownRunConfiguration> {
    private final JPanel myPanel;

    private UnknownSettingsEditor() {
      myPanel = new JPanel();
      myPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 50, 0));

      myPanel.add(new JLabel("This configuration cannot be edited", JLabel.CENTER));
    }

    @Override
    protected void resetEditorFrom(UnknownRunConfiguration s) {
    }

    @Override
    protected void applyEditorTo(UnknownRunConfiguration s) throws ConfigurationException {
    }

    @Override
    @Nonnull
    protected JComponent createEditor() {
      return myPanel;
    }
  }
}
