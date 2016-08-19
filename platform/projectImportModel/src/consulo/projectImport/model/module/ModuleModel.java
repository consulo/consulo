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
package consulo.projectImport.model.module;

import consulo.projectImport.model.NamedModelContainer;
import org.jetbrains.annotations.NotNull;

/**
 * @author VISTALL
 * @since 17:17/19.06.13
 */
public class ModuleModel extends NamedModelContainer {
  private final String myDir;

  public ModuleModel(@NotNull String name, @NotNull String dir) {
    super(name);
    myDir = dir;
  }

  @NotNull
  public String getDir() {
    return myDir;
  }
}
