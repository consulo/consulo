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
package com.intellij.openapi.roots.libraries;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.LibraryOrderEntry;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.ui.configuration.ProjectSettingsService;
import com.intellij.pom.Navigatable;
import org.jetbrains.annotations.NotNull;

/**
 * @author Konstantin Bulenkov
 */
public class LibraryNavigatable implements Navigatable {
  private final Module module;
  private OrderEntry element;

  public LibraryNavigatable(@NotNull Library library, @NotNull Module module) {
    this.module = module;
    for (OrderEntry entry : ModuleRootManager.getInstance(module).getOrderEntries()) {
      if (entry instanceof LibraryOrderEntry) {
        if (((LibraryOrderEntry)entry).getLibrary() == library) {
          element = entry;
        }
      }
    }
  }

  @Override
  public void navigate(boolean requestFocus) {
    ProjectSettingsService.getInstance(module.getProject()).openLibraryOrSdkSettings(element);
  }

  @Override
  public boolean canNavigate() {
    return !module.isDisposed() && element != null;
  }

  @Override
  public boolean canNavigateToSource() {
    return false;
  }
}
