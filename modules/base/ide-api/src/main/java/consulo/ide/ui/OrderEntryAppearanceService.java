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
package consulo.ide.ui;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.content.bundle.Sdk;
import consulo.content.library.Library;
import consulo.module.Module;
import consulo.module.content.layer.ContentFolder;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.project.Project;
import consulo.ui.ex.ColoredTextContainer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Consumer;

@ServiceAPI(ComponentScope.APPLICATION)
public abstract class OrderEntryAppearanceService {
  public static OrderEntryAppearanceService getInstance() {
    return Application.get().getInstance(OrderEntryAppearanceService.class);
  }

  @Nonnull
  public abstract Consumer<ColoredTextContainer> getRenderForOrderEntry(@Nonnull OrderEntry orderEntry);

  @Nonnull
  public Consumer<ColoredTextContainer> getRenderForModule(@Nonnull Module module) {
    return it -> forModule(module).customize(it);
  }

  @Nonnull
  public Consumer<ColoredTextContainer> getRenderForLibrary(Project project, @Nonnull Library library, boolean hasInvalidRoots) {
    return it -> forLibrary(project, library, hasInvalidRoots).customize(it);
  }

  @Nonnull
  public abstract CellAppearanceEx forLibrary(Project project, @Nonnull Library library, boolean hasInvalidRoots);

  @Nonnull
  public abstract CellAppearanceEx forSdk(@Nullable Sdk jdk, boolean isInComboBox, boolean selected, boolean showVersion);

  @Nonnull
  public abstract CellAppearanceEx forContentFolder(@Nonnull ContentFolder folder);

  @Nonnull
  public abstract CellAppearanceEx forModule(@Nonnull Module module);
}
