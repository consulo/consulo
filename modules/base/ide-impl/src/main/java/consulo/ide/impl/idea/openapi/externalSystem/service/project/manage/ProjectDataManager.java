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
package consulo.ide.impl.idea.openapi.externalSystem.service.project.manage;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.externalSystem.model.DataNode;
import consulo.externalSystem.model.Key;
import consulo.externalSystem.service.project.manage.ProjectDataService;
import consulo.externalSystem.util.ExternalSystemApiUtil;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.util.collection.Stack;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import jakarta.annotation.Nonnull;
import java.util.*;

/**
 * Aggregates all {@link ProjectDataService registered data services} and provides entry points for project data management.
 *
 * @author Denis Zhdanov
 * @since 4/16/13 11:38 AM
 */
@Singleton
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
public class ProjectDataManager {
  private static final Logger LOG = Logger.getInstance(ProjectDataManager.class);

  private static final ExtensionPointCacheKey<ProjectDataService, Map<Key<?>, List<ProjectDataService<?, ?>>>> CACHE_KEY =
    ExtensionPointCacheKey.create("ProjectDataService", walker -> {
      Map<Key<?>, List<ProjectDataService<?, ?>>> result = new HashMap<>();
      walker.walk(service -> {
        List<ProjectDataService<?, ?>> services = result.get(service.getTargetDataKey());
        if (services == null) {
          result.put(service.getTargetDataKey(), services = new ArrayList<>());
        }
        services.add(service);
      });

      for (List<ProjectDataService<?, ?>> services : result.values()) {
        ExternalSystemApiUtil.orderAwareSort(services);
      }

      return result;
    });

  private final Application myApplication;

  @Inject
  public ProjectDataManager(Application application) {
    myApplication = application;
  }

  @Nonnull
  private Map<Key<?>, List<ProjectDataService<?, ?>>> getServices() {
    return myApplication.getExtensionPoint(ProjectDataService.class).getOrBuildCache(CACHE_KEY);
  }

  @SuppressWarnings("unchecked")
  public <T> void importData(@Nonnull Collection<DataNode<?>> nodes, @Nonnull Project project, boolean synchronous) {
    Map<Key<?>, List<DataNode<?>>> grouped = ExternalSystemApiUtil.group(nodes);
    for (Map.Entry<Key<?>, List<DataNode<?>>> entry : grouped.entrySet()) {
      // Simple class cast makes ide happy but compiler fails.
      Collection<DataNode<T>> dummy = new ArrayList<>();
      for (DataNode<?> node : entry.getValue()) {
        dummy.add((DataNode<T>)node);
      }
      importData((Key<T>)entry.getKey(), dummy, project, synchronous);
    }
  }

  @SuppressWarnings("unchecked")
  public <T> void importData(@Nonnull Key<T> key, @Nonnull Collection<DataNode<T>> nodes, @Nonnull Project project, boolean synchronous) {
    ensureTheDataIsReadyToUse(nodes);
    List<ProjectDataService<?, ?>> services = getServices().get(key);
    if (services == null) {
      LOG.warn(String.format("Can't import data nodes '%s'. Reason: no service is registered for key %s. Available services for %s",
                             nodes,
                             key, getServices().keySet()));
    }
    else {
      for (ProjectDataService<?, ?> service : services) {
        ((ProjectDataService<T, ?>)service).importData(nodes, project, synchronous);
      }
    }

    Collection<DataNode<?>> children = new ArrayList<>();
    for (DataNode<T> node : nodes) {
      children.addAll(node.getChildren());
    }
    importData(children, project, synchronous);
  }

  @SuppressWarnings("unchecked")
  private <T> void ensureTheDataIsReadyToUse(@Nonnull Collection<DataNode<T>> nodes) {
    Map<Key<?>, List<ProjectDataService<?, ?>>> servicesByKey = getServices();
    Stack<DataNode<T>> toProcess = new Stack<>(nodes);
    while (!toProcess.isEmpty()) {
      DataNode<T> node = toProcess.pop();
      List<ProjectDataService<?, ?>> services = servicesByKey.get(node.getKey());
      if (services != null) {
        for (ProjectDataService<?, ?> service : services) {
          node.prepareData(service.getClass().getClassLoader());
        }
      }

      for (DataNode<?> dataNode : node.getChildren()) {
        toProcess.push((DataNode<T>)dataNode);
      }
    }
  }

  @SuppressWarnings("unchecked")
  public <T> void removeData(@Nonnull Key<?> key, @Nonnull Collection<T> toRemove, @Nonnull Project project, boolean synchronous) {
    List<ProjectDataService<?, ?>> services = getServices().get(key);
    for (ProjectDataService<?, ?> service : services) {
      ((ProjectDataService<?, T>)service).removeData(toRemove, project, synchronous);
    }
  }
}
