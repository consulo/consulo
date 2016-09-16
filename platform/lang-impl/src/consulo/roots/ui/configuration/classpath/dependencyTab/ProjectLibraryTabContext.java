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
package consulo.roots.ui.configuration.classpath.dependencyTab;

import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.roots.DependencyScope;
import com.intellij.openapi.roots.LibraryDependencyScopeSuggester;
import com.intellij.openapi.roots.LibraryOrderEntry;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.ui.CellAppearanceEx;
import com.intellij.openapi.roots.ui.OrderEntryAppearanceService;
import com.intellij.openapi.roots.ui.configuration.classpath.ClasspathPanel;
import com.intellij.openapi.roots.ui.configuration.libraries.LibraryEditingUtil;
import com.intellij.openapi.roots.ui.configuration.projectRoot.StructureConfigurableContext;
import com.intellij.openapi.roots.ui.configuration.projectRoot.StructureLibraryTableModifiableModelProvider;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.ColoredListCellRendererWrapper;
import com.intellij.ui.ListSpeedSearch;
import com.intellij.ui.components.JBList;
import com.intellij.util.containers.ContainerUtil;
import consulo.roots.ModifiableModuleRootLayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * @author VISTALL
 * @since 28.09.14
 */
public class ProjectLibraryTabContext extends AddModuleDependencyTabContext {
  private JBList myLibraryList;
  private List<Library> myItems;

  public ProjectLibraryTabContext(final ClasspathPanel classpathPanel, StructureConfigurableContext context) {
    super(classpathPanel, context);

    StructureLibraryTableModifiableModelProvider projectLibrariesProvider = context.getProjectLibrariesProvider();
    Library[] libraries = projectLibrariesProvider.getModifiableModel().getLibraries();
    final Condition<Library> condition = LibraryEditingUtil.getNotAddedLibrariesCondition(myClasspathPanel.getRootModel());

    myItems = ContainerUtil.filter(libraries, condition);
    ContainerUtil.sort(myItems, new Comparator<Library>() {
      @Override
      public int compare(Library o1, Library o2) {
        return StringUtil.compare(o1.getName(), o2.getName(), false);
      }
    });

    myLibraryList = new JBList(myItems);
    myLibraryList.setCellRenderer(new ColoredListCellRendererWrapper<Library>() {
      @Override
      protected void doCustomize(JList list, Library value, int index, boolean selected, boolean hasFocus) {
        final CellAppearanceEx appearance = OrderEntryAppearanceService.getInstance().forLibrary(classpathPanel.getProject(), value, false);

        appearance.customize(this);
      }
    });
    new ListSpeedSearch(myLibraryList);
  }

  @Override
  public List<OrderEntry> createOrderEntries(@NotNull ModifiableModuleRootLayer layer, DialogWrapper dialogWrapper) {
    Object[] selectedValues = myLibraryList.getSelectedValues();

    List<OrderEntry> entries = new ArrayList<OrderEntry>(selectedValues.length);

    for (Object selectedValue : selectedValues) {
      Library item = (Library) selectedValue;

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
              Messages.showErrorDialog(ProjectBundle.message("classpath.message.library.already.added", item.getName()),
                                       ProjectBundle.message("classpath.title.adding.dependency"));
              return null;
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
    for (LibraryDependencyScopeSuggester suggester : LibraryDependencyScopeSuggester.EP_NAME.getExtensions()) {
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

  @NotNull
  @Override
  public String getTabName() {
    return "Library";
  }

  @NotNull
  @Override
  public JComponent getComponent() {
    return myLibraryList;
  }
}
