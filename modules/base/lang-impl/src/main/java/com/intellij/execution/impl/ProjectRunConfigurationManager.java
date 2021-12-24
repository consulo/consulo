/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.execution.impl;

import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.UnknownRunConfiguration;
import com.intellij.openapi.components.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import consulo.application.AccessRule;
import consulo.project.startup.StartupActivity;
import consulo.ui.UIAccess;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jdom.Element;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * User: anna
 * Date: 28-Mar-2006
 */
@State(name = "ProjectRunConfigurationManager", storages = {
        @Storage(file = StoragePathMacros.PROJECT_CONFIG_DIR + "/runConfigurations/", stateSplitter = ProjectRunConfigurationManager.RunConfigurationStateSplitter.class)})
@Singleton
public class ProjectRunConfigurationManager implements PersistentStateComponent<Element> {
  public static class MyStartupActivity implements StartupActivity.DumbAware {

    @Override
    public void runActivity(@Nonnull Project project, @Nonnull UIAccess uiAccess) {
      ProjectRunConfigurationManager manager = project.getInstance(ProjectRunConfigurationManager.class);

      // just initialize it project run manager (load shared runs)

      RunManager runManager = RunManager.getInstance(project);
      RunnerAndConfigurationSettings selectedConfiguration = runManager.getSelectedConfiguration();
      if (selectedConfiguration == null) {
        List<RunnerAndConfigurationSettings> settings = runManager.getAllSettings();
        if (!settings.isEmpty()) {
          runManager.setSelectedConfiguration(settings.get(0));
        }
      }
    }
  }

  private final RunManagerImpl myManager;
  private List<Element> myUnloadedElements;

  @Inject
  public ProjectRunConfigurationManager(@Nonnull RunManager manager) {
    myManager = (RunManagerImpl)manager;
  }

  @Override
  public Element getState() {
    Element state = new Element("state");
    for (RunnerAndConfigurationSettings configuration : myManager.getStableConfigurations(true)) {
      myManager.addConfigurationElement(state, configuration);
    }
    if (!ContainerUtil.isEmpty(myUnloadedElements)) {
      for (Element unloadedElement : myUnloadedElements) {
        state.addContent(unloadedElement.clone());
      }
    }
    return state;
  }

  @Override
  public void loadState(Element state) {
    if (myUnloadedElements != null) {
      myUnloadedElements.clear();
    }

    Set<String> existing = new HashSet<String>();
    for (Iterator<Element> iterator = state.getChildren().iterator(); iterator.hasNext(); ) {
      Element child = iterator.next();
      RunnerAndConfigurationSettings configuration = myManager.loadConfiguration(child, true);
      if (configuration != null) {
        existing.add(configuration.getUniqueID());
      }
      else if (child.getName().equals(RunManagerImpl.CONFIGURATION)) {
        if (myUnloadedElements == null) {
          myUnloadedElements = new SmartList<>();
        }
        iterator.remove();
        myUnloadedElements.add(child);
      }
    }

    myManager.removeNotExistingSharedConfigurations(existing);
    if (myManager.getSelectedConfiguration() == null) {
      final List<RunConfiguration> allConfigurations = myManager.getAllConfigurationsList();
      for (final RunConfiguration configuration : allConfigurations) {
        final RunnerAndConfigurationSettings settings = myManager.getSettings(allConfigurations.get(0));
        if (!(configuration instanceof UnknownRunConfiguration)) {
          AccessRule.read(() -> myManager.setSelectedConfiguration(settings));
          break;
        }
      }
    }

    // IDEA-60004: configs may never be sorted before write, so call it manually after shared configs read
    myManager.setOrdered(false);
    myManager.getSortedConfigurations();
  }

  static class RunConfigurationStateSplitter extends StateSplitterEx {
    @Override
    public List<Pair<Element, String>> splitState(@Nonnull Element state) {
      return splitState(state, RunManagerImpl.NAME_ATTR);
    }
  }
}
