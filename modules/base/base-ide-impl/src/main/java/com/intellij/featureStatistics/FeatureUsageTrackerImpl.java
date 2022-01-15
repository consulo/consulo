/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

import com.intellij.openapi.application.PermanentInstallationID;
import com.intellij.openapi.components.*;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializer;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import javax.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.Set;

@Singleton
@State(name = "FeatureUsageStatistics", storages = @Storage(value = "feature.usage.statistics.xml", roamingType = RoamingType.DISABLED))
public class FeatureUsageTrackerImpl extends FeatureUsageTracker implements PersistentStateComponent<Element> {
  private static final int HOUR = 1000 * 60 * 60;
  private static final long DAY = HOUR * 24;
  private CompletionStatistics myCompletionStats = new CompletionStatistics();
  private CumulativeStatistics myFixesStats = new CumulativeStatistics();
  private boolean HAVE_BEEN_SHOWN = false;

  private final ProductivityFeaturesRegistry myRegistry;

  private static final String FEATURE_TAG = "feature";
  private static final String ATT_ID = "id";
  private static final String COMPLETION_STATS_TAG = "completionStatsTag";
  private static final String FIXES_STATS_TAG = "fixesStatsTag";
  private static final String ATT_HAVE_BEEN_SHOWN = "have-been-shown";

  @Inject
  public FeatureUsageTrackerImpl(ProductivityFeaturesRegistry productivityFeaturesRegistry) {
    myRegistry = productivityFeaturesRegistry;
  }

  @Override
  public boolean isToBeShown(String featureId, Project project) {
    return isToBeShown(featureId, project, DAY);
  }

  private boolean isToBeShown(String featureId, Project project, final long timeUnit) {
    ProductivityFeaturesRegistry registry = ProductivityFeaturesRegistry.getInstance();
    FeatureDescriptor descriptor = registry.getFeatureDescriptor(featureId);
    if (descriptor == null || !descriptor.isUnused()) return false;

    String[] dependencyFeatures = descriptor.getDependencyFeatures();
    boolean locked = dependencyFeatures.length > 0;
    for (int i = 0; locked && i < dependencyFeatures.length; i++) {
      if (!registry.getFeatureDescriptor(dependencyFeatures[i]).isUnused()) {
        locked = false;
      }
    }
    if (locked) return false;

    long current = System.currentTimeMillis();
    long succesive_interval = descriptor.getDaysBetweenSuccesiveShowUps() * timeUnit + descriptor.getShownCount() * 2;
    long firstShowUpInterval = descriptor.getDaysBeforeFirstShowUp() * timeUnit;
    long lastTimeUsed = descriptor.getLastTimeUsed();
    long lastTimeShown = descriptor.getLastTimeShown();
    return lastTimeShown == 0 && firstShowUpInterval + getFirstRunTime() < current ||
           lastTimeShown > 0 && current - lastTimeShown > succesive_interval && current - lastTimeUsed > succesive_interval;
  }

  @Override
  public boolean isToBeAdvertisedInLookup(@NonNls String featureId, Project project) {
    FeatureDescriptor descriptor = ProductivityFeaturesRegistry.getInstance().getFeatureDescriptor(featureId);
    if (descriptor != null && System.currentTimeMillis() - descriptor.getLastTimeUsed() > 10 * DAY) {
      return true;
    }
    
    return isToBeShown(featureId, project, HOUR);
  }

  @Nonnull
  public CompletionStatistics getCompletionStatistics() {
    return myCompletionStats;
  }

  public CumulativeStatistics getFixesStats() {
    return myFixesStats;
  }

  public long getFirstRunTime() {
    return PermanentInstallationID.date();
  }

  @Override
  public void loadState(final Element element) {
    List featuresList = element.getChildren(FEATURE_TAG);
    for (Object aFeaturesList : featuresList) {
      Element featureElement = (Element)aFeaturesList;
      FeatureDescriptor descriptor =
        ((ProductivityFeaturesRegistryImpl)myRegistry).getFeatureDescriptorEx(featureElement.getAttributeValue(ATT_ID));
      if (descriptor != null) {
        descriptor.readStatistics(featureElement);
      }
    }

    Element stats = element.getChild(COMPLETION_STATS_TAG);
    if (stats != null) {
      myCompletionStats = XmlSerializer.deserialize(stats, CompletionStatistics.class);
    }

    Element fStats = element.getChild(FIXES_STATS_TAG);
    if (fStats != null) {
      myFixesStats = XmlSerializer.deserialize(fStats, CumulativeStatistics.class);
    }

    HAVE_BEEN_SHOWN = Boolean.valueOf(element.getAttributeValue(ATT_HAVE_BEEN_SHOWN));
  }

  @Override
  public Element getState() {
    Element element = new Element("state");
    ProductivityFeaturesRegistry registry = ProductivityFeaturesRegistry.getInstance();
    Set<String> ids = registry.getFeatureIds();
    for (String id: ids) {
      Element featureElement = new Element(FEATURE_TAG);
      featureElement.setAttribute(ATT_ID, id);
      FeatureDescriptor descriptor = registry.getFeatureDescriptor(id);
      descriptor.writeStatistics(featureElement);
      element.addContent(featureElement);
    }

    Element statsTag = new Element(COMPLETION_STATS_TAG);
    XmlSerializer.serializeInto(myCompletionStats, statsTag);
    element.addContent(statsTag);

    Element fstatsTag = new Element(FIXES_STATS_TAG);
    XmlSerializer.serializeInto(myFixesStats, fstatsTag);
    element.addContent(fstatsTag);

    element.setAttribute(ATT_HAVE_BEEN_SHOWN, String.valueOf(HAVE_BEEN_SHOWN));

    return element;
  }

  @Override
  public void triggerFeatureUsed(String featureId) {
    ProductivityFeaturesRegistry registry = ProductivityFeaturesRegistry.getInstance();
    FeatureDescriptor descriptor = registry.getFeatureDescriptor(featureId);
    if (descriptor == null) {
     // TODO: LOG.error("Feature '" + featureId +"' must be registered prior triggerFeatureUsed() is called");
    }
    else {
      descriptor.triggerUsed();
    }
  }

  @Override
  public void triggerFeatureShown(String featureId) {
    FeatureDescriptor descriptor = ProductivityFeaturesRegistry.getInstance().getFeatureDescriptor(featureId);
    if (descriptor != null) {
      descriptor.triggerShown();
    }
  }

}
