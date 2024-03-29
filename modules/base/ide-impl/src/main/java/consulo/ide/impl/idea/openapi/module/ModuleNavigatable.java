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
package consulo.ide.impl.idea.openapi.module;

import consulo.project.ui.view.internal.ProjectSettingsService;
import consulo.navigation.Navigatable;
import consulo.module.Module;

import jakarta.annotation.Nonnull;

/**
 * @author Konstantin Bulenkov
 */
public class ModuleNavigatable implements Navigatable {
  private final Module module;

  public ModuleNavigatable(@Nonnull Module module) {
    this.module = module;
  }

  @Override
  public void navigate(boolean requestFocus) {
    ProjectSettingsService.getInstance(module.getProject()).openContentEntriesSettings(module);
  }

  @Override
  public boolean canNavigate() {
    return !module.isDisposed();
  }

  @Override
  public boolean canNavigateToSource() {
    return false;
  }
}
