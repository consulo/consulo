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
package consulo.ide.impl.roots.ui.configuration.classpath;

import consulo.ide.setting.module.AddModuleDependencyContext;
import consulo.module.Module;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.ide.setting.module.ClasspathPanel;
import consulo.util.lang.StringUtil;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.module.content.layer.ModifiableModuleRootLayer;
import consulo.ide.setting.module.LibrariesConfigurator;
import consulo.ide.setting.module.ModulesConfigurator;

import jakarta.annotation.Nonnull;
import java.util.*;

/**
 * @author VISTALL
 * @since 27.09.14
 */
public class ModuleDependencyContext extends AddModuleDependencyContext<List<Module>> {
  private final List<Module> myNotAddedModules;

  public ModuleDependencyContext(ClasspathPanel panel, ModulesConfigurator modulesConfigurator, LibrariesConfigurator librariesConfigurator) {
    super(panel, modulesConfigurator, librariesConfigurator);
    myNotAddedModules = calcNotAddedModules();
  }

  private List<Module> calcNotAddedModules() {
    final ModifiableRootModel rootModel = myClasspathPanel.getRootModel();
    Set<Module> addedModules = new HashSet<>(Arrays.asList(rootModel.getModuleDependencies(true)));
    addedModules.add(rootModel.getModule());

    final Module[] modules = myClasspathPanel.getModuleConfigurationState().getModulesConfigurator().getModules();
    final List<Module> elements = new ArrayList<>();
    for (final Module module : modules) {
      if (!addedModules.contains(module)) {
        elements.add(module);
      }
    }
    ContainerUtil.sort(elements, (o1, o2) -> StringUtil.compare(o1.getName(), o2.getName(), false));
    return elements;
  }

  public List<Module> getNotAddedModules() {
    return myNotAddedModules;
  }

  @Override
  public boolean isEmpty() {
    return myNotAddedModules.isEmpty();
  }

  @Nonnull
  @Override
  public List<OrderEntry> createOrderEntries(@Nonnull ModifiableModuleRootLayer layer, @Nonnull List<Module> value) {
    List<OrderEntry> orderEntries = new ArrayList<>();
    for (Module selectedValue : value) {
      orderEntries.add(layer.addModuleOrderEntry(selectedValue));
    }
    return orderEntries;
  }
}
