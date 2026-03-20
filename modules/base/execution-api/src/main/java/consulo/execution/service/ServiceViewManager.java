// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.service;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.project.Project;
import consulo.util.concurrent.Promise;
import org.jspecify.annotations.Nullable;

@ServiceAPI(ComponentScope.PROJECT)
public interface ServiceViewManager {
  static ServiceViewManager getInstance(Project project) {
    return project.getInstance(ServiceViewManager.class);
  }

  
  Promise<Void> select(Object service, Class<?> contributorClass, boolean activate, boolean focus);

  
  Promise<Void> expand(Object service, Class<?> contributorClass);

  
  Promise<Void> extract(Object service, Class<?> contributorClass);

  @Nullable String getToolWindowId(Class<?> contributorClass);
}
