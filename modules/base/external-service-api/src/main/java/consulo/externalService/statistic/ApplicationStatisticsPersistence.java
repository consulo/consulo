package consulo.externalService.statistic;

import consulo.project.Project;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public abstract class ApplicationStatisticsPersistence {
  private final Map<String, Map<String, Set<UsageDescriptor>>> myApplicationData = new HashMap<>();

  public ApplicationStatisticsPersistence() {
  }

  public void persistUsages(String groupDescriptor, Project project, Set<UsageDescriptor> usageDescriptors) {
      if (!myApplicationData.containsKey(groupDescriptor)) {
          myApplicationData.put(groupDescriptor, new HashMap<>());
      }
      myApplicationData.get(groupDescriptor).put(project.getName(), usageDescriptors);
  }

  
  public Map<String, Set<UsageDescriptor>> getApplicationData(String groupDescriptor) {
      if (!myApplicationData.containsKey(groupDescriptor)) {
          myApplicationData.put(groupDescriptor, new HashMap<>());
      }
      return myApplicationData.get(groupDescriptor);
  }

  
  public Map<String, Map<String, Set<UsageDescriptor>>> getApplicationData() {
      return myApplicationData;
  }
}
