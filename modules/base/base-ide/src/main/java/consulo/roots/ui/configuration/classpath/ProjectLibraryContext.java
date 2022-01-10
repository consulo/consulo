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
package consulo.roots.ui.configuration.classpath;

import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.roots.DependencyScope;
import com.intellij.openapi.roots.LibraryDependencyScopeSuggester;
import com.intellij.openapi.roots.LibraryOrderEntry;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.ui.configuration.LibraryTableModifiableModelProvider;
import com.intellij.openapi.roots.ui.configuration.classpath.ClasspathPanel;
import com.intellij.openapi.roots.ui.configuration.libraries.LibraryEditingUtil;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.containers.ContainerUtil;
import consulo.roots.ModifiableModuleRootLayer;
import consulo.roots.ui.configuration.LibrariesConfigurator;
import consulo.roots.ui.configuration.ModulesConfigurator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author VISTALL
 * @since 28.09.14
 */
public class ProjectLibraryContext extends AddModuleDependencyContext<List<Library>> {
  private List<Library> myItems;

  public ProjectLibraryContext(final ClasspathPanel classpathPanel, ModulesConfigurator modulesConfigurator, LibrariesConfigurator librariesConfigurator) {
    super(classpathPanel, modulesConfigurator, librariesConfigurator);

    LibraryTableModifiableModelProvider projectLibrariesProvider = librariesConfigurator.getProjectLibrariesProvider();
    Library[] libraries = projectLibrariesProvider.getModifiableModel().getLibraries();
    final Condition<Library> condition = LibraryEditingUtil.getNotAddedLibrariesCondition(myClasspathPanel.getRootModel());

    myItems = ContainerUtil.filter(libraries, condition);
    ContainerUtil.sort(myItems, (o1, o2) -> StringUtil.compare(o1.getName(), o2.getName(), false));
  }

  @Nonnull
  @Override
  public List<OrderEntry> createOrderEntries(@Nonnull ModifiableModuleRootLayer layer, @Nonnull List<Library> value) {
    List<OrderEntry> entries = new ArrayList<>();

    for (Library item : value) {
      final OrderEntry[] orderEntries = layer.getOrderEntries();
      for (OrderEntry orderEntry : orderEntries) {
        if (orderEntry instanceof LibraryOrderEntry) {
          final LibraryOrderEntry libraryOrderEntry = (LibraryOrderEntry)orderEntry;
          if (item.equals(libraryOrderEntry.getLibrary())) {
            continue; // already added
          }

          String name = item.getName();
          if (name != null && name.equals(libraryOrderEntry.getLibraryName())) {
            if (orderEntry.isValid()) {
              Messages.showErrorDialog(ProjectBundle.message("classpath.message.library.already.added", item.getName()), ProjectBundle.message("classpath.title.adding.dependency"));
              return Collections.emptyList();
            }
            else {
              layer.removeOrderEntry(orderEntry);
            }
          }
        }
      }
      final LibraryOrderEntry orderEntry = layer.addLibraryEntry(item);
      DependencyScope defaultScope = getDefaultScope(item);
      if (defaultScope != null) {
        orderEntry.setScope(defaultScope);
      }
      entries.add(orderEntry);
    }

    return entries;
  }

  @Nullable
  private static DependencyScope getDefaultScope(Library item) {
    for (LibraryDependencyScopeSuggester suggester : LibraryDependencyScopeSuggester.EP_NAME.getExtensionList()) {
      DependencyScope scope = suggester.getDefaultDependencyScope(item);
      if (scope != null) {
        return scope;
      }
    }
    return null;
  }

  @Override
  public boolean isEmpty() {
    return myItems.isEmpty();
  }

  public List<Library> getItems() {
    return myItems;
  }
}
