package com.intellij.internal.statistic.persistence;

import com.intellij.internal.statistic.beans.UsageDescriptor;
import com.intellij.openapi.project.Project;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public abstract class ApplicationStatisticsPersistence {
  private final Map<String, Map<String, Set<UsageDescriptor>>> myApplicationData = new HashMap<>();

  public ApplicationStatisticsPersistence() {
  }

  public void persistUsages(@Nonnull String groupDescriptor, @Nonnull Project project, @Nonnull Set<UsageDescriptor> usageDescriptors) {
      if (!myApplicationData.containsKey(groupDescriptor)) {
          myApplicationData.put(groupDescriptor, new HashMap<>());
      }
      myApplicationData.get(groupDescriptor).put(project.getName(), usageDescriptors);
  }

  @Nonnull
  public Map<String, Set<UsageDescriptor>> getApplicationData(@Nonnull String groupDescriptor) {
      if (!myApplicationData.containsKey(groupDescriptor)) {
          myApplicationData.put(groupDescriptor, new HashMap<>());
      }
      return myApplicationData.get(groupDescriptor);
  }

  @Nonnull
  public Map<String, Map<String, Set<UsageDescriptor>>> getApplicationData() {
      return myApplicationData;
  }
}
