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

import com.intellij.openapi.Disposable;
import com.intellij.openapi.roots.ui.configuration.classpath.ClasspathPanel;
import com.intellij.openapi.roots.ui.configuration.projectRoot.StructureConfigurableContext;
import consulo.roots.ModuleRootLayer;
import org.jetbrains.annotations.NotNull;

/**
 * @author VISTALL
 * @since 27.09.14
 */
public class ModuleDependencyTabFactory implements AddModuleDependencyTabFactory {
  @Override
  public boolean isAvailable(@NotNull ModuleRootLayer layer) {
    return true;
  }

  @NotNull
  @Override
  public AddModuleDependencyTabContext createTabContext(@NotNull Disposable parent,
                                                        @NotNull ClasspathPanel panel,
                                                        @NotNull StructureConfigurableContext context) {
    return new ModuleDependencyTabContext(panel, context);
  }
}
