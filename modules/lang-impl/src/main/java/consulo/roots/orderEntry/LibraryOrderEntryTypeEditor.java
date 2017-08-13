/*
 * Copyright 2013-2016 consulo.io
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
package consulo.roots.orderEntry;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.impl.LibraryOrderEntryImpl;
import com.intellij.openapi.roots.impl.libraries.LibraryEx;
import com.intellij.openapi.roots.libraries.Library;
import consulo.roots.types.BinariesOrderRootType;
import com.intellij.openapi.roots.ui.CellAppearanceEx;
import com.intellij.openapi.roots.ui.FileAppearanceService;
import com.intellij.openapi.roots.ui.OrderEntryAppearanceService;
import com.intellij.openapi.roots.ui.configuration.ProjectStructureConfigurable;
import consulo.roots.ui.configuration.ProjectStructureDialog;
import com.intellij.openapi.roots.ui.configuration.classpath.ClasspathTableItem;
import com.intellij.openapi.roots.ui.configuration.classpath.LibraryClasspathTableItem;
import com.intellij.openapi.roots.ui.configuration.projectRoot.StructureConfigurableContext;
import com.intellij.util.Consumer;
import org.jetbrains.annotations.NotNull;

/**
 * @author VISTALL
 * @since 06-Jun-16
 */
public class LibraryOrderEntryTypeEditor implements OrderEntryTypeEditor<LibraryOrderEntryImpl> {
  @Override
  public void navigate(@NotNull final LibraryOrderEntryImpl orderEntry) {
    Project project = orderEntry.getModuleRootLayer().getProject();
    ProjectStructureDialog.show(project, new Consumer<ProjectStructureConfigurable>() {
      @Override
      public void consume(ProjectStructureConfigurable config) {
        config.select(orderEntry, true);
      }
    });
  }

  @NotNull
  @Override
  public CellAppearanceEx getCellAppearance(@NotNull LibraryOrderEntryImpl orderEntry) {
    if (!orderEntry.isValid()) { //library can be removed
      return FileAppearanceService.getInstance().forInvalidUrl(orderEntry.getPresentableName());
    }
    Library library = orderEntry.getLibrary();
    assert library != null : orderEntry;
    return OrderEntryAppearanceService.getInstance().forLibrary(orderEntry.getModuleRootLayer().getProject(), library,
                                                                !((LibraryEx)library).getInvalidRootUrls(BinariesOrderRootType.getInstance()).isEmpty());
  }

  @NotNull
  @Override
  public ClasspathTableItem<LibraryOrderEntryImpl> createTableItem(@NotNull LibraryOrderEntryImpl orderEntry, @NotNull StructureConfigurableContext context) {
    return new LibraryClasspathTableItem<LibraryOrderEntryImpl>(orderEntry, context);
  }
}
