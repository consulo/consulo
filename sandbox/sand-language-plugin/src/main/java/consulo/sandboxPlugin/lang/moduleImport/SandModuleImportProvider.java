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

import consulo.application.AllIcons;
import consulo.application.WriteAction;
import consulo.module.ModifiableModuleModel;
import consulo.module.Module;
import consulo.project.Project;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.module.content.ModuleRootManager;
import consulo.ide.impl.idea.openapi.vfs.VfsUtil;
import consulo.annotation.access.RequiredReadAction;
import consulo.ide.impl.moduleImport.ModuleImportContext;
import consulo.ide.impl.moduleImport.ModuleImportProvider;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2019-08-26
 */
public class SandModuleImportProvider implements ModuleImportProvider<ModuleImportContext> {
  @Nonnull
  @Override
  public String getName() {
    return "sand";
  }

  @Nonnull
  @Override
  public Image getIcon() {
    return AllIcons.Nodes.Static;
  }

  @Override
  public boolean canImport(@Nonnull File fileOrDirectory) {
    return new File(fileOrDirectory, "sand.txt").exists();
  }

  @Override
  public boolean isOnlyForNewImport() {
    return false;
  }

  @RequiredReadAction
  @Override
  public void process(@Nonnull ModuleImportContext context, @Nonnull Project project, @Nonnull ModifiableModuleModel model, @Nonnull Consumer<Module> newModuleConsumer) {
    String path = context.getPath();

    Module module = model.newModule(context.getName(), path);

    ModifiableRootModel modifiableModel = ModuleRootManager.getInstance(module).getModifiableModel();
    modifiableModel.addContentEntry(VfsUtil.pathToUrl(path));
    WriteAction.runAndWait(modifiableModel::commit);

    newModuleConsumer.accept(module);
  }
}
