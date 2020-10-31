/*
 * Copyright 2013-2017 consulo.io
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
package consulo.moduleImport;

import com.intellij.ide.IdeBundle;
import com.intellij.ide.impl.ProjectUtil;
import com.intellij.ide.impl.util.NewOrImportModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.platform.DefaultProjectOpenProcessor;
import com.intellij.projectImport.ProjectOpenProcessor;
import com.intellij.util.ThreeState;
import com.intellij.util.containers.ContainerUtil;
import consulo.moduleImport.ui.ModuleImportProcessor;
import consulo.platform.base.localize.IdeLocalize;
import consulo.ui.Alert;
import consulo.ui.UIAccess;
import consulo.ui.image.Image;
import consulo.util.concurrent.AsyncResult;
import consulo.util.lang.Pair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 31-Jan-17
 */
public class ImportProjectOpenProcessor extends ProjectOpenProcessor {
  private final List<ModuleImportProvider> myProviders;

  public ImportProjectOpenProcessor() {
    myProviders = ModuleImportProviders.getExtensions(false);
  }

  @Nonnull
  @Override
  public String getFileSample() {
    throw new IllegalArgumentException("should never called");
  }

  @Override
  public void collectFileSamples(@Nonnull Consumer<String> fileSamples) {
    for (ModuleImportProvider provider : myProviders) {
      fileSamples.accept(provider.getFileSample());
    }
  }

  @Nullable
  @Override
  public Image getIcon(@Nonnull VirtualFile file) {
    File ioFile = VfsUtil.virtualToIoFile(file);
    for (ModuleImportProvider provider : myProviders) {
      if (provider.canImport(ioFile)) {
        return provider.getIcon();
      }
    }
    return null;
  }

  @Override
  public boolean canOpenProject(@Nonnull File file) {
    for (ModuleImportProvider provider : myProviders) {
      if (provider.canImport(file)) {
        return true;
      }
    }

    return false;
  }

  @Nonnull
  @Override
  public AsyncResult<Project> doOpenProjectAsync(@Nonnull VirtualFile virtualFile, @Nonnull UIAccess uiAccess) {
    File ioPath = VfsUtil.virtualToIoFile(virtualFile);

    List<ModuleImportProvider> targetProviders = ContainerUtil.filter(myProviders, moduleImportProvider -> moduleImportProvider.canImport(ioPath));

    if (targetProviders.size() == 0) {
      throw new IllegalArgumentException("must be not empty");
    }

    String expectedProjectPath = ModuleImportProvider.getDefaultPath(virtualFile);

    AsyncResult<ThreeState> askDialogResult = AsyncResult.undefined();
    if (!uiAccess.isHeadless() && DefaultProjectOpenProcessor.getInstance().canOpenProject(new File(expectedProjectPath))) {
      Alert<ThreeState> alert = Alert.create();
      alert.title(IdeLocalize.titleOpenProject());
      alert.text(IdeBundle.message("project.import.open.existing", "an existing project", FileUtil.toSystemDependentName(ioPath.getPath()), virtualFile.getName()));
      alert.asQuestion();

      alert.button(IdeBundle.message("project.import.open.existing.openExisting"), ThreeState.YES);
      alert.asDefaultButton();

      alert.button(IdeBundle.message("project.import.open.existing.reimport"), ThreeState.NO);

      alert.button(Alert.CANCEL, ThreeState.UNSURE);
      alert.asExitButton();

      uiAccess.give(() -> alert.showAsync().notify(askDialogResult));
    }
    else {
      askDialogResult.setDone(ThreeState.NO);
    }

    AsyncResult<Project> projectResult = AsyncResult.undefined();

    askDialogResult.doWhenDone(threeState -> {
      switch (threeState) {
        case YES:
          ProjectManager.getInstance().openProjectAsync(virtualFile, uiAccess).notify(projectResult);
          break;
        case NO:
          uiAccess.give(() -> {
            AsyncResult<Pair<ModuleImportContext, ModuleImportProvider<ModuleImportContext>>> result = AsyncResult.undefined();
            ModuleImportProcessor.showImportChooser(null, virtualFile, targetProviders, result);

            result.doWhenDone(pair -> {
              ModuleImportContext context = pair.getFirst();

              ModuleImportProvider<ModuleImportContext> provider = pair.getSecond();
              AsyncResult<Project> importProjectAsync = NewOrImportModuleUtil.importProject(context, provider);
              importProjectAsync.doWhenDone((newProject) -> {
                ProjectUtil.updateLastProjectLocation(expectedProjectPath);

                ProjectManager.getInstance().openProjectAsync(newProject, uiAccess).notify(projectResult);
              });

              importProjectAsync.doWhenRejected((Runnable)projectResult::setRejected);
            });

            result.doWhenRejected((pair, error) -> {
              pair.getFirst().dispose();

              projectResult.setRejected();
            });
          });
          break;
        case UNSURE:
          projectResult.setRejected();
          break;
      }
    });

    return projectResult;
  }
}