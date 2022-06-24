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
import consulo.annotation.component.Service;
import consulo.annotation.component.ServiceImpl;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import jakarta.inject.Singleton;
import org.jdom.Element;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;

/**
 * User: anna
 */
@Singleton
@Service(ComponentScope.PROJECT)
@ServiceImpl
@State(name = "UnknownFeatures", storages = @Storage(StoragePathMacros.WORKSPACE_FILE))
public class UnknownFeaturesCollector implements PersistentStateComponent<Element> {
  private static final String FEATURE_ID = "featureType";
  private static final String IMPLEMENTATION_NAME = "implementationName";

  private final Set<UnknownExtension> myUnknownExtensions = new HashSet<>();
  private final Set<UnknownExtension> myIgnoredUnknownExtensions = new HashSet<>();

  public static UnknownFeaturesCollector getInstance(Project project) {
    return project.getInstance(UnknownFeaturesCollector.class);
  }

  public void registerUnknownFeature(Class extensionClass, String implementationName) {
    final UnknownExtension feature = new UnknownExtension(extensionClass.getName(), implementationName);
    if (!isIgnored(feature)) {
      myUnknownExtensions.add(feature);
    }
  }

  public boolean isIgnored(UnknownExtension feature) {
    return myIgnoredUnknownExtensions.contains(feature);
  }

  public void ignoreFeature(UnknownExtension feature) {
    myIgnoredUnknownExtensions.add(feature);
  }

  public Set<UnknownExtension> getUnknownExtensions() {
    return myUnknownExtensions;
  }

  @Nullable
  @Override
  public Element getState() {
    if (myIgnoredUnknownExtensions.isEmpty()) return null;

    final Element ignored = new Element("ignored");
    for (UnknownExtension feature : myIgnoredUnknownExtensions) {
      final Element option = new Element("option");
      option.setAttribute(FEATURE_ID, feature.getExtensionKey());
      option.setAttribute(IMPLEMENTATION_NAME, feature.getValue());
      ignored.addContent(option);
    }
    return ignored;
  }

  @Override
  public void loadState(Element state) {
    myIgnoredUnknownExtensions.clear();
    for (Element element : state.getChildren()) {
      myIgnoredUnknownExtensions.add(new UnknownExtension(element.getAttributeValue(FEATURE_ID), element.getAttributeValue(IMPLEMENTATION_NAME)));
    }
  }
}
