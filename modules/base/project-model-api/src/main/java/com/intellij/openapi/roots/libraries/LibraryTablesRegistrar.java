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
package com.intellij.openapi.roots.libraries;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import consulo.annotation.DeprecationInfo;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

@Deprecated(forRemoval = true)
@DeprecationInfo("Just use ProjectLibraryTable")
public abstract class LibraryTablesRegistrar {
  @NonNls
  public static final String PROJECT_LEVEL = "project";

  @DeprecationInfo("Global libraries cant be configured via UI, and replaced by Bundles")
  @Deprecated
  @NonNls
  public static final String APPLICATION_LEVEL = "application";

  public static LibraryTablesRegistrar getInstance() {
    return ServiceManager.getService(LibraryTablesRegistrar.class);
  }

  @Nonnull
  public abstract LibraryTable getLibraryTable();

  @Nonnull
  public abstract LibraryTable getLibraryTable(@Nonnull Project project);

  @Nullable
  public abstract LibraryTable getLibraryTableByLevel(String level, @Nonnull Project project);

  @Nonnull
  public List<LibraryTable> getCustomLibraryTables() {
    return List.of();
  }
}