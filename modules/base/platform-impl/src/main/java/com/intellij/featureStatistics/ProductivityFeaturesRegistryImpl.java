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
package com.intellij.featureStatistics;

import com.intellij.openapi.util.JDOMUtil;
import consulo.logging.Logger;
import jakarta.inject.Singleton;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jetbrains.annotations.TestOnly;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Singleton
public class ProductivityFeaturesRegistryImpl extends ProductivityFeaturesRegistry {
  private static final Logger LOG = Logger.getInstance(ProductivityFeaturesRegistryImpl.class);

  private final Map<String, FeatureDescriptor> myFeatures = new HashMap<>();
  private final Map<String, GroupDescriptor> myGroups = new HashMap<>();

  private boolean myAdditionalFeaturesLoaded = false;

  public static final String WELCOME = "features.welcome";

  private static final String TAG_GROUP = "group";
  private static final String TAG_FEATURE = "feature";
  private static final String TODO_HTML_MARKER = "todo.html";

  public ProductivityFeaturesRegistryImpl() {
    reloadFromXml();
  }

  private void reloadFromXml() {
    try {
      readFromXml(ProductivityFeaturesRegistryImpl.class.getResource("/ProductivityFeaturesRegistry.xml"));
    }
    catch (Exception e) {
      LOG.error(e);
    }
  }

  private void readFromXml(URL url) throws JDOMException, IOException {
    Element root = JDOMUtil.load(url);
    readGroups(root);
  }

  private void lazyLoadFromPluginsFeaturesProviders() {
    if (myAdditionalFeaturesLoaded) return;
    loadFeaturesFromProviders(ProductivityFeaturesProvider.EP_NAME.getExtensionList());
    myAdditionalFeaturesLoaded = true;
  }

  private void loadFeaturesFromProviders(List<ProductivityFeaturesProvider> providers) {
    for (ProductivityFeaturesProvider provider : providers) {
      final GroupDescriptor[] groupDescriptors = provider.getGroupDescriptors();
      if (groupDescriptors != null) {
        for (GroupDescriptor groupDescriptor : groupDescriptors) {
          myGroups.put(groupDescriptor.getId(), groupDescriptor);
        }
      }
      final FeatureDescriptor[] featureDescriptors = provider.getFeatureDescriptors();
      if (featureDescriptors != null) {
        for (FeatureDescriptor featureDescriptor : featureDescriptors) {
          final FeatureDescriptor featureLoadedStatistics = myFeatures.get(featureDescriptor.getId());
          if (featureLoadedStatistics != null) {
            featureDescriptor.copyStatistics(featureLoadedStatistics);
          }
          myFeatures.put(featureDescriptor.getId(), featureDescriptor);
        }
      }
    }
  }

  private void readGroups(Element element) {
    List groups = element.getChildren(TAG_GROUP);
    for (Object group : groups) {
      Element groupElement = (Element)group;
      readGroup(groupElement);
    }
  }

  private void readGroup(Element groupElement) {
    GroupDescriptor groupDescriptor = new GroupDescriptor();
    groupDescriptor.readExternal(groupElement);
    String groupId = groupDescriptor.getId();
    myGroups.put(groupId, groupDescriptor);
    readFeatures(groupElement, groupDescriptor);
  }

  private void readFeatures(Element groupElement, GroupDescriptor groupDescriptor) {
    List features = groupElement.getChildren(TAG_FEATURE);
    for (Object feature : features) {
      Element featureElement = (Element)feature;
      FeatureDescriptor featureDescriptor = new FeatureDescriptor(groupDescriptor);
      featureDescriptor.readExternal(featureElement);
      if (!TODO_HTML_MARKER.equals(featureDescriptor.getTipFileName())) {
        myFeatures.put(featureDescriptor.getId(), featureDescriptor);
      }
    }
  }

  @Override
  @Nonnull
  public Set<String> getFeatureIds() {
    lazyLoadFromPluginsFeaturesProviders();
    return myFeatures.keySet();
  }

  @Override
  public FeatureDescriptor getFeatureDescriptor(@Nonnull String id) {
    lazyLoadFromPluginsFeaturesProviders();
    return getFeatureDescriptorEx(id);
  }

  public FeatureDescriptor getFeatureDescriptorEx(@Nonnull String id) {
    if (WELCOME.equals(id)) {
      return new FeatureDescriptor(WELCOME, "AdaptiveWelcome.html", FeatureStatisticsBundle.message("feature.statistics.welcome.tip.name"));
    }
    return myFeatures.get(id);
  }

  @Override
  public GroupDescriptor getGroupDescriptor(@Nonnull String id) {
    lazyLoadFromPluginsFeaturesProviders();
    return myGroups.get(id);
  }

  @Override
  public String toString() {
    return super.toString() + "; myAdditionalFeaturesLoaded=" + myAdditionalFeaturesLoaded;
  }

  @TestOnly
  public void prepareForTest() {
    myAdditionalFeaturesLoaded = false;
    myFeatures.clear();
    myGroups.clear();
    reloadFromXml();
  }
}
