/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package com.intellij.openapi.roots.ui;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ContentFolder;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.libraries.Library;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import consulo.annotation.DeprecationInfo;

public abstract class OrderEntryAppearanceService {
  public static OrderEntryAppearanceService getInstance() {
    return ServiceManager.getService(OrderEntryAppearanceService.class);
  }

  @Nonnull
  @Deprecated
  @DeprecationInfo("Use #forOrderEntry(@ OrderEntry)")
  public CellAppearanceEx forOrderEntry(@Deprecated Project project, @Nonnull OrderEntry orderEntry, @Deprecated boolean selected) {
    return forOrderEntry(orderEntry);
  }

  @Nonnull
  public abstract CellAppearanceEx forOrderEntry(@Nonnull OrderEntry orderEntry);

  @Nonnull
  public abstract CellAppearanceEx forLibrary(Project project, @Nonnull Library library, boolean hasInvalidRoots);

  @Nonnull
  public abstract CellAppearanceEx forSdk(@Nullable Sdk jdk, boolean isInComboBox, boolean selected, boolean showVersion);

  @Nonnull
  public abstract CellAppearanceEx forContentFolder(@Nonnull ContentFolder folder);

  @Nonnull
  public abstract CellAppearanceEx forModule(@Nonnull Module module);
}
