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
package consulo.execution.test.sm;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.execution.configuration.ConfigurationType;
import consulo.execution.configuration.ConfigurationTypeUtil;
import consulo.execution.test.sm.action.AbstractImportTestsAction;
import consulo.project.Project;
import consulo.ui.image.Image;
import consulo.util.xml.serializer.annotation.Attribute;
import consulo.util.xml.serializer.annotation.MapAnnotation;
import consulo.util.xml.serializer.annotation.Property;
import consulo.util.xml.serializer.annotation.Tag;
import jakarta.inject.Singleton;

import jakarta.annotation.Nullable;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

@Singleton
@State(name = "TestHistory", storages = {@Storage(file = StoragePathMacros.WORKSPACE_FILE)})
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
public class TestHistoryConfiguration implements PersistentStateComponent<TestHistoryConfiguration.State> {

  public static class State {

    private Map<String, ConfigurationBean> myHistoryElements = new LinkedHashMap<String, ConfigurationBean>();

    @Property(surroundWithTag = false)
    @MapAnnotation(surroundKeyWithTag = false, surroundWithTag = false, surroundValueWithTag = false, entryTagName = "history-entry", keyAttributeName = "file")
    public Map<String, ConfigurationBean> getHistoryElements() {
      return myHistoryElements;
    }

    public void setHistoryElements(final Map<String, ConfigurationBean> elements) {
      myHistoryElements = elements;
    }
  }

  @Tag("configuration")
  public static class ConfigurationBean {

    @Attribute("name")
    public String name;
    @Attribute("configurationId")
    public String configurationId;
  }

  private State myState = new State();

  public static TestHistoryConfiguration getInstance(Project project) {
    return project.getInstance(TestHistoryConfiguration.class);
  }

  @Override
  public State getState() {
    return myState;
  }

  @Override
  public void loadState(State state) {
    myState = state;
  }

  public Collection<String> getFiles() {
    return myState.getHistoryElements().keySet();
  }

  public String getConfigurationName(String file) {
    final ConfigurationBean bean = myState.getHistoryElements().get(file);
    return bean != null ? bean.name : null;
  }

  @Nullable
  public Image getIcon(String file) {
    final ConfigurationBean bean = myState.getHistoryElements().get(file);
    if (bean != null) {
      ConfigurationType type = ConfigurationTypeUtil.findConfigurationType(bean.configurationId);
      if (type != null) return type.getIcon();
    }
    return null;
  }

  public void registerHistoryItem(String file, String configName, String configId) {
    final ConfigurationBean bean = new ConfigurationBean();
    bean.name = configName;
    bean.configurationId = configId;
    final Map<String, ConfigurationBean> historyElements = myState.getHistoryElements();
    historyElements.put(file, bean);
    if (historyElements.size() > AbstractImportTestsAction.getHistorySize()) {
      final String first = historyElements.keySet().iterator().next();
      historyElements.remove(first);
    }
  }
}
