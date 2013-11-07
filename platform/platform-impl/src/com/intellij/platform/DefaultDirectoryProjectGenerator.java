package com.intellij.platform;

import com.intellij.openapi.application.Result;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.module.ModifiableModuleModel;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ContentFolderType;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author VISTALL
 * @since 14:30/30.09.13
 */
public class DefaultDirectoryProjectGenerator implements DirectoryProjectGenerator<Object> {
  @Nls
  @NotNull
  @Override
  public String getName() {
    return "Empty project";
  }

  @Nullable
  @Override
  public Object showGenerationSettings(VirtualFile baseDir) throws ProcessCanceledException {
    return null;
  }

  @Override
  public void generateProject(@NotNull final Project project,
                              @NotNull final VirtualFile baseDir,
                              @Nullable Object settings,
                              @Nullable Module module) {
    new WriteAction<Object>() {

      @Override
      protected void run(Result<Object> result) throws Throwable {
        ModuleManager moduleManager = ModuleManager.getInstance(project);

        ModifiableModuleModel modifiableModel = moduleManager.getModifiableModel();
        Module newModule = modifiableModel.newModule(baseDir.getName(), baseDir.getPath());

        ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(newModule);
        ModifiableRootModel modifiableModelForModule = moduleRootManager.getModifiableModel();
        ContentEntry contentEntry = modifiableModelForModule.addContentEntry(baseDir);
        contentEntry.addFolder(baseDir.getUrl() + "/" + Project.DIRECTORY_STORE_FOLDER, ContentFolderType.EXCLUDED);
        modifiableModelForModule.commit();

        modifiableModel.commit();
      }
    }.execute();
  }

  @Nullable
  @Override
  public String validate(@NotNull String baseDirPath) {
    return null;
  }
}
