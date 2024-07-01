/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

/**
 * @author cdr
 */
package consulo.project.ui.view.tree;

import consulo.annotation.access.RequiredReadAction;
import consulo.dataContext.DataContext;
import consulo.language.editor.LangDataKeys;
import consulo.module.ModifiableModuleModel;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.project.Project;
import consulo.util.collection.ArrayUtil;
import consulo.util.dataholder.Key;

import jakarta.annotation.Nonnull;
import java.util.*;

public class ModuleGroup {
  public static final Key<ModuleGroup[]> ARRAY_DATA_KEY = Key.create("moduleGroup.array");

  private final String[] myGroupPath;

  public ModuleGroup(@Nonnull String[] groupPath) {
    myGroupPath = groupPath;
  }

  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ModuleGroup)) return false;

    final ModuleGroup moduleGroup = (ModuleGroup)o;

    return Arrays.equals(myGroupPath, moduleGroup.myGroupPath);
  }

  public int hashCode() {
    return myGroupPath[myGroupPath.length-1].hashCode();
  }

  public String[] getGroupPath() {
    return myGroupPath;
  }

  @Nonnull
  @RequiredReadAction
  public Collection<Module> modulesInGroup(Project project, boolean recursively) {
    final Module[] allModules = ModuleManager.getInstance(project).getModules();
    List<Module> result = new ArrayList<>();
    for (final Module module : allModules) {
      String[] group = ModuleManager.getInstance(project).getModuleGroupPath(module);
      if (group == null) continue;
      if (Arrays.equals(myGroupPath, group) || (recursively && isChild(myGroupPath, group))) {
        result.add(module);
      }
    }
    return result;
  }

  @RequiredReadAction
  public Collection<ModuleGroup> childGroups(Project project) {
    return childGroups(null, project);
  }

  @RequiredReadAction
  public Collection<ModuleGroup> childGroups(DataContext dataContext) {
    return childGroups(dataContext.getData(LangDataKeys.MODIFIABLE_MODULE_MODEL), dataContext.getData(Project.KEY));
  }

  @RequiredReadAction
  public Collection<ModuleGroup> childGroups(ModifiableModuleModel model, Project project) {
    final Module[] allModules;
    if ( model != null ) {
      allModules = model.getModules();
    } else {
      allModules = ModuleManager.getInstance(project).getModules();
    }

    Set<ModuleGroup> result = new HashSet<>();
    for (Module module : allModules) {
      String[] group;
      if ( model != null ) {
        group = model.getModuleGroupPath(module);
      } else {
        group = ModuleManager.getInstance(project).getModuleGroupPath(module);
      }
      if (group == null) continue;
      final String[] directChild = directChild(myGroupPath, group);
      if (directChild != null) {
        result.add(new ModuleGroup(directChild));
      }
    }

    return result;
  }

  private static boolean isChild(final String[] parent, final String[] descendant) {
    if (parent.length >= descendant.length) return false;
    for (int i = 0; i < parent.length; i++) {
      String group = parent[i];
      if (!group.equals(descendant[i])) return false;
    }
    return true;
  }

  private static String[] directChild(final String[] parent, final String[] descendant) {
    if (!isChild(parent, descendant)) return null;
    return ArrayUtil.append(parent, descendant[parent.length]);
  }

  public String presentableText() {
    return "'" + myGroupPath[myGroupPath.length - 1] + "'";
  }

  public String toString() {
    return myGroupPath[myGroupPath.length - 1];
  }
}
