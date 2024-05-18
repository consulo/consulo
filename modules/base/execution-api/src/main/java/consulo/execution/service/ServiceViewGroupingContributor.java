// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.service;

import jakarta.annotation.Nonnull;

import java.util.List;

public interface ServiceViewGroupingContributor<T, G> extends ServiceViewContributor<T> {
  @Nonnull
  List<G> getGroups(@Nonnull T service);

  @Nonnull
  ServiceViewDescriptor getGroupDescriptor(@Nonnull G group);
}
