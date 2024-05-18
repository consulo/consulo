// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.service;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.project.Project;
import consulo.util.concurrent.Promise;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

@ServiceAPI(ComponentScope.PROJECT)
public interface ServiceViewManager {
  static ServiceViewManager getInstance(Project project) {
    return project.getInstance(ServiceViewManager.class);
  }

  @Nonnull
  Promise<Void> select(@Nonnull Object service, @Nonnull Class<?> contributorClass, boolean activate, boolean focus);

  @Nonnull
  Promise<Void> expand(@Nonnull Object service, @Nonnull Class<?> contributorClass);

  @Nonnull
  Promise<Void> extract(@Nonnull Object service, @Nonnull Class<?> contributorClass);

  @Nullable
  String getToolWindowId(@Nonnull Class<?> contributorClass);
}
