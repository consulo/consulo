/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.openapi.roots.impl.libraries;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar;
import consulo.disposer.Disposable;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;

@Singleton
public class LibraryTablesRegistrarImpl extends LibraryTablesRegistrar implements Disposable {
  @Override
  @Nonnull
  public LibraryTable getLibraryTable() {
    return ApplicationLibraryTable.getApplicationTable();
  }

  @Override
  @Nonnull
  public LibraryTable getLibraryTable(@Nonnull Project project) {
    return ProjectLibraryTable.getInstance(project);
  }

  @Override
  public LibraryTable getLibraryTableByLevel(String level, @Nonnull Project project) {
    if (LibraryTablesRegistrar.PROJECT_LEVEL.equals(level)) return getLibraryTable(project);
    if (LibraryTablesRegistrar.APPLICATION_LEVEL.equals(level)) return getLibraryTable();
    return null;
  }

  @Override
  public void dispose() {
  }
}