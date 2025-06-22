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
package consulo.execution.impl.internal.configuration;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.AccessRule;
import consulo.component.persist.*;
import consulo.execution.RunManager;
import consulo.execution.RunnerAndConfigurationSettings;
import consulo.execution.configuration.RunConfiguration;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.SmartList;
import consulo.util.lang.Pair;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jdom.Element;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * @author anna
 * @since 2006-03-28
 */
@State(name = "ProjectRunConfigurationManager", storages = {
        @Storage(file = StoragePathMacros.PROJECT_CONFIG_DIR + "/runConfigurations/", stateSplitter = ProjectRunConfigurationManager.RunConfigurationStateSplitter.class)})
@Singleton
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
public class ProjectRunConfigurationManager implements PersistentStateComponent<Element> {

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

    Set<String> existing = new HashSet<>();
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
