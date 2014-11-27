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
package com.intellij.openapi.roots.ui.configuration.classpath.dependencyTab;

import com.intellij.openapi.roots.ModifiableModuleRootLayer;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.ui.configuration.classpath.ClasspathPanel;
import com.intellij.openapi.roots.ui.configuration.classpath.ClasspathTableItem;
import com.intellij.openapi.roots.ui.configuration.projectRoot.StructureConfigurableContext;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author VISTALL
 * @since 27.09.14
 */
public abstract class AddModuleDependencyTabContext {
  protected final ClasspathPanel myClasspathPanel;
  protected final StructureConfigurableContext myContext;

  protected AddModuleDependencyTabContext(ClasspathPanel classpathPanel, StructureConfigurableContext context) {
    myClasspathPanel = classpathPanel;
    myContext = context;
  }

  @NotNull
  public abstract String getTabName();

  public boolean isEmpty() {
    return false;
  }

  public final void processAddOrderEntries(DialogWrapper dialogWrapper) {
    ModifiableModuleRootLayer currentLayer = (ModifiableModuleRootLayer)myClasspathPanel.getRootModel().getCurrentLayer();

    List<OrderEntry> orderEntries = createOrderEntries(currentLayer, dialogWrapper);
    if(orderEntries.isEmpty()) {
      return;
    }

    List<ClasspathTableItem<?>> items = new ArrayList<ClasspathTableItem<?>>(orderEntries.size());
    for (OrderEntry orderEntry : orderEntries) {
      ClasspathTableItem<?> item = ClasspathTableItem.createItem(orderEntry, myContext);
      items.add(item);
    }
    myClasspathPanel.addItems(items);
  }

  public List<OrderEntry> createOrderEntries(@NotNull ModifiableModuleRootLayer layer, DialogWrapper dialogWrapper) {
    return Collections.emptyList();
  }

  @NotNull
  public abstract JComponent getComponent();
}
