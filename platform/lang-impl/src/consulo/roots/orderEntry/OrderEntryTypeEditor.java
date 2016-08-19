/*
 * Copyright 2013-2016 must-be.org
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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.extensions.KeyedFactoryEPBean;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.ui.CellAppearanceEx;
import com.intellij.openapi.roots.ui.configuration.ProjectStructureConfigurable;
import com.intellij.openapi.roots.ui.configuration.classpath.ClasspathTableItem;
import com.intellij.openapi.roots.ui.configuration.projectRoot.StructureConfigurableContext;
import com.intellij.openapi.roots.ui.util.SimpleTextCellAppearance;
import com.intellij.openapi.util.KeyedExtensionFactory;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.Consumer;
import consulo.roots.ui.configuration.ProjectStructureDialog;
import org.jetbrains.annotations.NotNull;

/**
 * @author VISTALL
 * @since 06-Jun-16
 */
public abstract interface OrderEntryTypeEditor<T extends OrderEntry> {
  ExtensionPointName<KeyedFactoryEPBean> EP_NAME = ExtensionPointName.create("consulo.orderEntryTypeEditor");

  KeyedExtensionFactory<OrderEntryTypeEditor, OrderEntryType> FACTORY =
          new KeyedExtensionFactory<OrderEntryTypeEditor, OrderEntryType>(OrderEntryTypeEditor.class, EP_NAME,
                                                                          ApplicationManager.getApplication().getPicoContainer()) {
            @Override
            public OrderEntryTypeEditor getByKey(@NotNull OrderEntryType key) {
              // special hack for unknown order entry type
              if (key instanceof UnknownOrderEntryType) {
                return new UnknownOrderEntryTypeEditor();
              }
              return super.getByKey(key);
            }

            @Override
            public String getKey(@NotNull final OrderEntryType key) {
              return key.getId();
            }
          };

  @NotNull
  default CellAppearanceEx getCellAppearance(@NotNull T orderEntry) {
    return new SimpleTextCellAppearance(orderEntry.getPresentableName(), null, SimpleTextAttributes.REGULAR_ATTRIBUTES);
  }

  @NotNull
  default ClasspathTableItem<T> createTableItem(@NotNull T orderEntry, @NotNull StructureConfigurableContext context) {
    return new ClasspathTableItem<T>(orderEntry);
  }

  default void navigate(@NotNull final T orderEntry) {
    Project project = orderEntry.getOwnerModule().getProject();
    final ProjectStructureConfigurable config = ProjectStructureConfigurable.getInstance(project);
    ProjectStructureDialog.show(project, new Consumer<ProjectStructureConfigurable>() {
      @Override
      public void consume(ProjectStructureConfigurable configurable) {
        config.selectOrderEntry(orderEntry.getOwnerModule(), orderEntry);
      }
    });
  }
}
