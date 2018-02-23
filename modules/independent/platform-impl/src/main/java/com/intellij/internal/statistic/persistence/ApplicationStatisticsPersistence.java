package com.intellij.internal.statistic.persistence;

import com.intellij.internal.statistic.beans.GroupDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.internal.statistic.beans.UsageDescriptor;
import com.intellij.util.containers.HashMap;
import javax.annotation.Nonnull;

import java.util.Map;
import java.util.Set;

public abstract class ApplicationStatisticsPersistence {
  private final Map<GroupDescriptor, Map<String, Set<UsageDescriptor>>> myApplicationData = new HashMap<GroupDescriptor, Map<String, Set<UsageDescriptor>>>();

  public ApplicationStatisticsPersistence() {
  }

  public void persistUsages(@Nonnull GroupDescriptor groupDescriptor, @Nonnull Project project, @Nonnull Set<UsageDescriptor> usageDescriptors) {
      if (!myApplicationData.containsKey(groupDescriptor)) {
          myApplicationData.put(groupDescriptor, new HashMap<String, Set<UsageDescriptor>>());
      }
      myApplicationData.get(groupDescriptor).put(project.getName(), usageDescriptors);
  }

  @Nonnull
  public Map<String, Set<UsageDescriptor>> getApplicationData(@Nonnull GroupDescriptor groupDescriptor) {
      if (!myApplicationData.containsKey(groupDescriptor)) {
          myApplicationData.put(groupDescriptor, new HashMap<String, Set<UsageDescriptor>>());
      }
      return myApplicationData.get(groupDescriptor);
  }

  @Nonnull
  public Map<GroupDescriptor, Map<String, Set<UsageDescriptor>>> getApplicationData() {
      return myApplicationData;
  }

}
