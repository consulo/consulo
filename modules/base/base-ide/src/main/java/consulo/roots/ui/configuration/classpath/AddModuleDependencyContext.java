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

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.ui.configuration.classpath.ClasspathPanel;
import com.intellij.openapi.roots.ui.configuration.classpath.ClasspathTableItem;
import consulo.roots.ModifiableModuleRootLayer;
import consulo.roots.ui.configuration.LibrariesConfigurator;
import consulo.roots.ui.configuration.ModulesConfigurator;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 27.09.14
 */
public abstract class AddModuleDependencyContext<T> {
  protected final ClasspathPanel myClasspathPanel;
  protected final ModulesConfigurator myModulesConfigurator;
  protected final LibrariesConfigurator myLibrariesConfigurator;

  protected AddModuleDependencyContext(ClasspathPanel classpathPanel, ModulesConfigurator modulesConfigurator, LibrariesConfigurator librariesConfigurator) {
    myClasspathPanel = classpathPanel;
    myModulesConfigurator = modulesConfigurator;
    myLibrariesConfigurator = librariesConfigurator;
  }

  public boolean isEmpty() {
    return false;
  }

  @Nonnull
  public ClasspathPanel getClasspathPanel() {
    return myClasspathPanel;
  }

  @Nonnull
  public ModulesConfigurator getModulesConfigurator() {
    return myModulesConfigurator;
  }

  @Nonnull
  public LibrariesConfigurator getLibrariesConfigurator() {
    return myLibrariesConfigurator;
  }

  @Nonnull
  public Project getProject() {
    return myClasspathPanel.getProject();
  }

  public final void processAddOrderEntries(@Nonnull T value) {
    ModifiableModuleRootLayer currentLayer = (ModifiableModuleRootLayer)myClasspathPanel.getRootModel().getCurrentLayer();

    List<OrderEntry> orderEntries = createOrderEntries(currentLayer, value);
    if(orderEntries.isEmpty()) {
      return;
    }

    List<ClasspathTableItem<?>> items = new ArrayList<>(orderEntries.size());
    for (OrderEntry orderEntry : orderEntries) {
      ClasspathTableItem<?> item = ClasspathTableItem.createItem(orderEntry, myClasspathPanel.getProject(), myModulesConfigurator, myLibrariesConfigurator);
      items.add(item);
    }
    myClasspathPanel.addItems(items);
  }

  @Nonnull
  public abstract List<OrderEntry> createOrderEntries(@Nonnull ModifiableModuleRootLayer layer, @Nonnull T value);
}
