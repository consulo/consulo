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
package consulo.externalService.statistic;

import consulo.application.ApplicationManager;
import consulo.util.collection.ArrayUtil;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FeatureDescriptor {
  @Nonnull
  private String myId;
  private String myGroupId;
  @Nonnull
  private String myTipFileName;
  @Nonnull
  private String myDisplayName;
  private int myDaysBeforeFirstShowUp;
  private int myDaysBetweenSuccesiveShowUps;
  private Set<String> myDependencies;
  private int myMinUsageCount;

  private int myUsageCount;
  private long myLastTimeShown;
  private long myLastTimeUsed;
  private int myShownCount;
  private ProductivityFeaturesProvider myProvider;

  private static final String ATTRIBUTE_COUNT = "count";
  private static final String ATTRIBUTE_LAST_SHOWN = "last-shown";
  private static final String ATTRIBUTE_LAST_USED = "last-used";
  private static final String ATTRIBUTE_SHOWN_COUNT = "shown-count";
  private static final String ATTRIBUTE_ID = "id";
  private static final String ATTRIBUTE_TIP_FILE = "tip-file";
  private static final String ATTRIBUTE_FIRST_SHOW = "first-show";
  private static final String ATTRIBUTE_SUCCESSIVE_SHOW = "successive-show";
  private static final String ATTRIBUTE_MIN_USAGE_COUNT = "min-usage-count";
  private static final String ELEMENT_DEPENDENCY = "dependency";

  public FeatureDescriptor(GroupDescriptor group) {
    myGroupId = group.getId();
  }

  public FeatureDescriptor(final String id) {
    myId = id;
  }

  public FeatureDescriptor(String id, @NonNls String tipFileName, String displayName) {
    myId = id;
    myTipFileName = tipFileName;
    myDisplayName = displayName;
  }

  public FeatureDescriptor(@NonNls String id,
                           @NonNls String groupId,
                           @NonNls String tipFileName,
                           String displayName,
                           int daysBeforeFirstShowUp,
                           int daysBetweenSuccessiveShowUps,
                           Set<String> dependencies,
                           int minUsageCount,
                           ProductivityFeaturesProvider provider) {
    myId = id;
    myGroupId = groupId;
    myTipFileName = tipFileName;
    myDisplayName = displayName;
    myDaysBeforeFirstShowUp = daysBeforeFirstShowUp;
    myDaysBetweenSuccesiveShowUps = daysBetweenSuccessiveShowUps;
    myDependencies = dependencies;
    myMinUsageCount = minUsageCount;
    myProvider = provider;
  }

  public void readExternal(Element element) {
    myId = element.getAttributeValue(ATTRIBUTE_ID);
    myTipFileName = element.getAttributeValue(ATTRIBUTE_TIP_FILE);
    myDisplayName = FeatureStatisticsBundle.message(myId);
    myDaysBeforeFirstShowUp = Integer.parseInt(element.getAttributeValue(ATTRIBUTE_FIRST_SHOW));
    myDaysBetweenSuccesiveShowUps = Integer.parseInt(element.getAttributeValue(ATTRIBUTE_SUCCESSIVE_SHOW));
    String minUsageCount = element.getAttributeValue(ATTRIBUTE_MIN_USAGE_COUNT);
    myMinUsageCount = minUsageCount == null ? 1 : Integer.parseInt(minUsageCount);
    List dependencies = element.getChildren(ELEMENT_DEPENDENCY);
    if (dependencies != null && !dependencies.isEmpty()) {
      myDependencies = new HashSet<String>();
      for (Object dependency : dependencies) {
        Element dependencyElement = (Element)dependency;
        myDependencies.add(dependencyElement.getAttributeValue(ATTRIBUTE_ID));
      }
    }
  }

  @Nonnull
  public String getId() {
    return myId;
  }

  public String getGroupId() {
    return myGroupId;
  }

  @Nonnull
  public String getTipFileName() {
    return myTipFileName;
  }

  @Nonnull
  public String getDisplayName() {
    return myDisplayName;
  }

  public int getUsageCount() {
    return myUsageCount;
  }

  public Class<? extends ProductivityFeaturesProvider> getProvider() {
    if (myProvider == null) {
      return null;
    }
    return myProvider.getClass();
  }

  public void triggerUsed() {
    long current = System.currentTimeMillis();
    long delta = myUsageCount > 0 ? current - Math.max(myLastTimeUsed, ApplicationManager.getApplication().getStartTime()) : 0;
    myLastTimeUsed = current;
    myUsageCount++;
  }

  public boolean isUnused() {
    return myUsageCount < myMinUsageCount;
  }

  @Override
  public String toString() {
    StringBuilder buffer = new StringBuilder();

    buffer.append("id = [");
    buffer.append(myId);
    buffer.append("], displayName = [");
    buffer.append(myDisplayName);
    buffer.append("], groupId = [");
    buffer.append(myGroupId);
    buffer.append("], usageCount = [");
    buffer.append(myUsageCount);
    buffer.append("]");

    return buffer.toString();
  }

  public int getDaysBeforeFirstShowUp() {
    return myDaysBeforeFirstShowUp;
  }

  public int getDaysBetweenSuccesiveShowUps() {
    return myDaysBetweenSuccesiveShowUps;
  }

  public int getMinUsageCount() {
    return myMinUsageCount;
  }

  public long getLastTimeShown() {
    return myLastTimeShown;
  }

  public String[] getDependencyFeatures() {
    if (myDependencies == null) return ArrayUtil.EMPTY_STRING_ARRAY;
    return ArrayUtil.toStringArray(myDependencies);
  }

  public void triggerShown() {
    myLastTimeShown = System.currentTimeMillis();
    myShownCount++;
  }

  public long getLastTimeUsed() {
    return myLastTimeUsed;
  }

  public int getShownCount() {
    return myShownCount;
  }

  public void copyStatistics(FeatureDescriptor statistics) {
    myUsageCount = statistics.getUsageCount();
    myLastTimeShown = statistics.getLastTimeShown();
    myLastTimeUsed = statistics.getLastTimeUsed();
    myShownCount = statistics.getShownCount();
  }

  public void readStatistics(Element element) {
    String count = element.getAttributeValue(ATTRIBUTE_COUNT);
    String lastShown = element.getAttributeValue(ATTRIBUTE_LAST_SHOWN);
    String lastUsed = element.getAttributeValue(ATTRIBUTE_LAST_USED);
    String shownCount = element.getAttributeValue(ATTRIBUTE_SHOWN_COUNT);

    myUsageCount = count == null ? 0 : Integer.parseInt(count);
    myLastTimeShown = lastShown == null ? 0 : Long.parseLong(lastShown);
    myLastTimeUsed = lastUsed == null ? 0 : Long.parseLong(lastUsed);
    myShownCount = shownCount == null ? 0 : Integer.parseInt(shownCount);
  }

  public void writeStatistics(Element element) {
    element.setAttribute(ATTRIBUTE_COUNT, String.valueOf(getUsageCount()));
    element.setAttribute(ATTRIBUTE_LAST_SHOWN, String.valueOf(getLastTimeShown()));
    element.setAttribute(ATTRIBUTE_LAST_USED, String.valueOf(getLastTimeUsed()));
    element.setAttribute(ATTRIBUTE_SHOWN_COUNT, String.valueOf(getShownCount()));
  }
}