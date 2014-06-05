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
package org.mustbe.consulo.ide.impl;

import com.intellij.openapi.application.Result;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.module.ModifiableModuleModel;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mustbe.consulo.roots.impl.ExcludedContentFolderTypeProvider;

/**
 * @author VISTALL
 * @since 05.06.14
 */
public abstract class NewProjectOrModuleDialog extends DialogWrapper {
  protected NewProjectOrModuleDialog(@Nullable Project project, boolean canBeParent) {
    super(project, canBeParent);
  }

  @NotNull
  public abstract String getLocationText();

  @Nullable
  public abstract String getNameText();

  @NotNull
  public Module doCreate(@NotNull final Project project, @NotNull final VirtualFile baseDir) {
    return doCreate(ModuleManager.getInstance(project).getModifiableModel(), baseDir);
  }

  @NotNull
  public Module doCreate(@NotNull final ModifiableModuleModel modifiableModel, @NotNull final VirtualFile baseDir) {
    return new WriteAction<Module>() {
      @Override
      protected void run(Result<Module> result) throws Throwable {
        result.setResult(doCreateImpl(modifiableModel, baseDir));
      }
    }.execute().getResultObject();
  }

  @NotNull
  private Module doCreateImpl(@NotNull final ModifiableModuleModel modifiableModel, @NotNull final VirtualFile baseDir) {
    String name = StringUtil.notNullize(getNameText(), baseDir.getName());

    Module newModule = modifiableModel.newModule(name, baseDir.getPath());

    ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(newModule);
    ModifiableRootModel modifiableModelForModule = moduleRootManager.getModifiableModel();
    ContentEntry contentEntry = modifiableModelForModule.addContentEntry(baseDir);

    if (!isModuleCreation()) {
      contentEntry.addFolder(contentEntry.getUrl() + "/" + Project.DIRECTORY_STORE_FOLDER, ExcludedContentFolderTypeProvider.getInstance());
    }

    postSetupModule(modifiableModelForModule);

    modifiableModelForModule.commit();

    modifiableModel.commit();

    baseDir.refresh(true, true);
    return newModule;
  }

  protected boolean isModuleCreation() {
    return false;
  }

  protected void postSetupModule(@NotNull ModifiableRootModel modifiableRootModel) {

  }
}
