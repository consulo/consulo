/*
 * Copyright 2013-2014 must-be.org
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
package org.mustbe.consulo.roots.impl;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.ui.configuration.ProjectStructureConfigurable;
import com.intellij.openapi.roots.ui.configuration.ProjectStructureDialog;
import com.intellij.openapi.roots.ui.configuration.classpath.ClasspathTableItem;
import com.intellij.openapi.roots.ui.configuration.projectRoot.StructureConfigurableContext;
import com.intellij.util.Consumer;
import org.jetbrains.annotations.NotNull;
import org.mustbe.consulo.roots.OrderEntryTypeProvider;

/**
 * @author VISTALL
 * @since 21.08.14
 */
public abstract class OrderEntryTypeProviderEx<T extends OrderEntry> implements OrderEntryTypeProvider<T> {
  @NotNull
  public ClasspathTableItem<T> createTableItem(@NotNull T orderEntry, @NotNull StructureConfigurableContext context) {
    return new ClasspathTableItem<T>(orderEntry);
  }

  @Override
  public void navigate(@NotNull final T orderEntry) {
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
