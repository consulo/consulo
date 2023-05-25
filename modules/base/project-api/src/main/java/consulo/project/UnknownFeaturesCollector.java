/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.project;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.container.plugin.PluginExtensionPreview;
import consulo.container.plugin.PluginId;
import jakarta.inject.Singleton;
import org.jdom.Element;

import jakarta.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;

/**
 * User: anna
 */
@Singleton
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
@State(name = "UnknownFeatures", storages = @Storage(StoragePathMacros.WORKSPACE_FILE))
public class UnknownFeaturesCollector implements PersistentStateComponent<Element> {
  private static final String API_PLUGIN_ID = "apiPluginId";
  private static final String API_CLASS_NAME = "apiClassName";
  private static final String IMPL_ID = "implId";

  private final Set<PluginExtensionPreview> myUnknownExtensions = new HashSet<>();
  private final Set<PluginExtensionPreview> myIgnoredUnknownExtensions = new HashSet<>();

  public static UnknownFeaturesCollector getInstance(Project project) {
    return project.getInstance(UnknownFeaturesCollector.class);
  }

  public void registerUnknownFeature(Class extensionClass, String implementationName) {
    final PluginExtensionPreview extensionPreview = new PluginExtensionPreview(extensionClass, implementationName);
    if (!isIgnored(extensionPreview)) {
      myUnknownExtensions.add(extensionPreview);
    }
  }

  public boolean isIgnored(PluginExtensionPreview feature) {
    return myIgnoredUnknownExtensions.contains(feature);
  }

  public void ignoreFeature(PluginExtensionPreview feature) {
    myIgnoredUnknownExtensions.add(feature);
  }

  public Set<PluginExtensionPreview> getUnknownExtensions() {
    return myUnknownExtensions;
  }

  @Nullable
  @Override
  public Element getState() {
    if (myIgnoredUnknownExtensions.isEmpty()) return null;

    final Element ignored = new Element("ignoreExtensions");
    for (PluginExtensionPreview feature : myIgnoredUnknownExtensions) {
      final Element option = new Element("ignoreExtensions");
      option.setAttribute(API_CLASS_NAME, feature.getApiClassName());
      option.setAttribute(API_PLUGIN_ID, feature.getApiPluginId().getIdString());
      option.setAttribute(IMPL_ID, feature.getImplId());
      ignored.addContent(option);
    }
    return ignored;
  }

  @Override
  public void loadState(Element state) {
    myIgnoredUnknownExtensions.clear();
    for (Element element : state.getChildren()) {
      String apiPluginId = element.getAttributeValue(API_PLUGIN_ID);
      String apiClassName = element.getAttributeValue(API_CLASS_NAME);
      String implId = element.getAttributeValue(IMPL_ID);
      myIgnoredUnknownExtensions.add(new PluginExtensionPreview(PluginId.getId(apiPluginId), apiClassName, implId));
    }
  }
}
