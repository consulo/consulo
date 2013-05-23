/*
 * Copyright 2013 Consulo.org
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
package org.jetbrains.idea.devkit.util;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.containers.HashSet;
import com.intellij.util.descriptors.ConfigFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.devkit.build.PluginBuildConfiguration;
import org.jetbrains.idea.devkit.build.PluginBuildUtil;
import org.jetbrains.idea.devkit.module.extension.PluginModuleExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author VISTALL
 * @since 13:48/23.05.13
 */
public class PluginModuleUtil {
  public static Module[] getAllPluginModules(final Project project) {
    List<Module> modules = new ArrayList<Module>();
    Module[] allModules = ModuleManager.getInstance(project).getModules();
    for (Module module : allModules) {
      if (ModuleUtil.getExtension(module, PluginModuleExtension.class) != null) {
        modules.add(module);
      }
    }
    return modules.toArray(new Module[modules.size()]);
  }

  @Nullable
  public static XmlFile getPluginXml(Module module) {
    if (module == null) return null;
    if (ModuleUtil.getExtension(module, PluginModuleExtension.class) == null) {
      return null;
    }

    final PluginBuildConfiguration buildConfiguration = PluginBuildConfiguration.getInstance(module);
    if (buildConfiguration == null) return null;
    final ConfigFile configFile = buildConfiguration.getPluginXmlConfigFile();
    return configFile != null ? configFile.getXmlFile() : null;
  }

  public static boolean isPluginModuleOrDependency(@NotNull Module module) {
    if (ModuleUtil.getExtension(module, PluginModuleExtension.class) != null) {
      return true;
    }

    return getCandidateModules(module).size() > 0;
  }

  public static List<Module> getCandidateModules(Module module) {
    final ModuleRootManager manager = ModuleRootManager.getInstance(module);

    final Module[] modules = ModuleManager.getInstance(module.getProject()).getModules();
    final List<Module> candidates = new ArrayList<Module>(modules.length);
    final Set<Module> deps = new HashSet<Module>(modules.length);
    for (Module m : modules) {
      if (ModuleUtil.getExtension(module, PluginModuleExtension.class) != null) {
        deps.clear();
        PluginBuildUtil.getDependencies(m, deps);

        if (deps.contains(module) && getPluginXml(m) != null) {
          candidates.add(m);
        }
      }
    }
    return candidates;
  }

}
