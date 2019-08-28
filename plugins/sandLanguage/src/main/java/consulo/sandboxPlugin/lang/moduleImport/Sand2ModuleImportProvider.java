/*
 * Copyright 2013-2019 consulo.io
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
package consulo.sandboxPlugin.lang.moduleImport;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.module.ModifiableModuleModel;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ui.configuration.ModulesProvider;
import com.intellij.packaging.artifacts.ModifiableArtifactModel;
import consulo.moduleImport.ModuleImportContext;
import consulo.moduleImport.ModuleImportProvider;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.Collections;
import java.util.List;

/**
 * @author VISTALL
 * @since 2019-08-26
 */
public class Sand2ModuleImportProvider implements ModuleImportProvider<ModuleImportContext> {
  @Nonnull
  @Override
  public String getName() {
    return "sand2";
  }

  @Nonnull
  @Override
  public Image getIcon() {
    return AllIcons.Nodes.ClassInitializer;
  }

  @Override
  public boolean canImport(@Nonnull File fileOrDirectory) {
    return new File(fileOrDirectory, "sand2.txt").exists();
  }

  @Override
  public boolean isOnlyForNewImport() {
    return false;
  }

  @Nonnull
  @Override
  public List<Module> commit(@Nonnull ModuleImportContext context,
                             @Nonnull Project project,
                             @Nullable ModifiableModuleModel model,
                             @Nonnull ModulesProvider modulesProvider,
                             @Nullable ModifiableArtifactModel artifactModel) {
    return Collections.emptyList();
  }
}
