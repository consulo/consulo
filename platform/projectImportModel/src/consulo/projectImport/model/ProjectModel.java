/*
 * Copyright 2013 must-be.org
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
package consulo.projectImport.model;

import consulo.projectImport.model.module.ModuleModel;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author VISTALL
 * @since 17:15/19.06.13
 */
public class ProjectModel extends NamedModelContainer{
  public ProjectModel(String name) {
    super(name);
  }

  @NotNull
  public ProjectLibraryTableModel getLibraryTable() {
    return findChildOrCreate(ProjectLibraryTableModel.class);
  }

  @NotNull
  public ModuleTableModel getModuleTable() {
    return findChildOrCreate(ModuleTableModel.class);
  }

  @NotNull
  public List<ModuleModel> getModules() {
    return getModuleTable().getModules();
  }
}
