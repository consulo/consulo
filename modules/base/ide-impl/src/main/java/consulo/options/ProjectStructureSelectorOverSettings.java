/*
 * Copyright 2013-2021 consulo.io
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
package consulo.options;

import consulo.ide.setting.ProjectStructureSelector;
import consulo.module.Module;
import consulo.configurable.SearchableConfigurable;
import consulo.configurable.UnnamedConfigurable;
import com.intellij.openapi.options.ex.ConfigurableWrapper;
import com.intellij.openapi.options.ex.Settings;
import consulo.project.ProjectBundle;
import consulo.content.bundle.Sdk;
import consulo.module.content.layer.orderEntry.LibraryOrderEntry;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.content.library.Library;
import com.intellij.openapi.roots.ui.configuration.ClasspathEditor;
import com.intellij.openapi.roots.ui.configuration.ModuleEditor;
import com.intellij.openapi.roots.ui.configuration.ProjectConfigurable;
import com.intellij.openapi.roots.ui.configuration.artifacts.ArtifactsStructureConfigurable;
import com.intellij.openapi.roots.ui.configuration.projectRoot.ModuleConfigurable;
import com.intellij.openapi.roots.ui.configuration.projectRoot.ModuleStructureConfigurable;
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectLibrariesConfigurable;
import com.intellij.openapi.roots.ui.configuration.projectRoot.SdkListConfigurable;
import consulo.ide.setting.ui.MasterDetailsComponent;
import consulo.compiler.artifact.Artifact;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.concurrent.AsyncResult;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.BiConsumer;

/**
 * @author VISTALL
 * @since 20/04/2021
 */
public class ProjectStructureSelectorOverSettings implements ProjectStructureSelector {
  private final Settings mySettings;

  public ProjectStructureSelectorOverSettings(Settings settings) {
    mySettings = settings;
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public AsyncResult<Void> select(@Nullable Artifact artifact, boolean requestFocus) {
    return selectAsync(ArtifactsStructureConfigurable.ID, ArtifactsStructureConfigurable.class, (artifactsStructureConfigurable, runnable) -> {
      artifactsStructureConfigurable.selectNodeInTree(artifact);
      runnable.run();
    });
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public AsyncResult<Void> select(@Nonnull Sdk sdk, boolean requestFocus) {
    return selectAsync(SdkListConfigurable.ID, SdkListConfigurable.class, (sdkListConfigurable, runnable) -> {
      sdkListConfigurable.selectNodeInTree(sdk);
      runnable.run();
    });
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public AsyncResult<Void> select(@Nullable String moduleToSelect, @Nullable String tabId, boolean requestFocus) {
    return selectAsync(ModuleStructureConfigurable.ID, ModuleStructureConfigurable.class, (moduleStructureConfigurable, runnable) -> {
      // just select Modules
      if (moduleToSelect == null) {
        runnable.run();
        return;
      }
      moduleStructureConfigurable.selectNodeInTree(moduleToSelect).doWhenDone((node) -> {
        ModuleEditor moduleEditor = ((ModuleConfigurable)((MasterDetailsComponent.MyNode)node).getConfigurable()).getModuleEditor();

        if (tabId == null) {
          moduleEditor.selectEditor(ProjectBundle.message("module.paths.title"));
        }
        else {
          moduleEditor.selectEditor(tabId);
        }

        runnable.run();
      });
    });
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public AsyncResult<Void> select(@Nonnull LibraryOrderEntry libraryOrderEntry, boolean requestFocus) {
    Library library = libraryOrderEntry.getLibrary();
    if (library == null) {
      return AsyncResult.rejected();
    }
    return selectProjectOrGlobalLibrary(library, requestFocus);
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public AsyncResult<Void> selectOrderEntry(@Nonnull Module module, @Nullable OrderEntry orderEntry) {
    return selectAsync(ModuleStructureConfigurable.ID, ModuleStructureConfigurable.class, (moduleStructureConfigurable, runnable) -> {
      moduleStructureConfigurable.selectNodeInTree(module).doWhenDone((node) -> {
        ModuleEditor moduleEditor = ((ModuleConfigurable)((MasterDetailsComponent.MyNode)node).getConfigurable()).getModuleEditor();

        moduleEditor.selectEditor(ProjectBundle.message("module.dependencies.title"));

        ClasspathEditor editor = (ClasspathEditor)moduleEditor.getEditor(ProjectBundle.message("module.dependencies.title"));

        if (orderEntry != null) {
          editor.selectOrderEntry(orderEntry);
        }
        
        runnable.run();
      });
    });
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public AsyncResult<Void> selectProjectOrGlobalLibrary(@Nonnull Library library, boolean requestFocus) {
    return selectAsync(ProjectLibrariesConfigurable.ID, ProjectLibrariesConfigurable.class, (projectLibrariesConfigurable, runnable) -> {
      projectLibrariesConfigurable.selectNodeInTree(library);
      runnable.run();
    });
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public AsyncResult<Void> selectProjectGeneralSettings(boolean requestFocus) {
    return selectAsync(ProjectConfigurable.ID, ProjectConfigurable.class, (projectConfigurable, runnable) -> {
      runnable.run();
    });
  }

  @RequiredUIAccess
  @Nonnull
  private <T extends UnnamedConfigurable> AsyncResult<Void> selectAsync(String id, Class<T> cls, BiConsumer<T, Runnable> consumer) {
    AsyncResult<Void> result = AsyncResult.undefined();

    UIAccess.current().give(() -> {
      SearchableConfigurable configurable = mySettings.findConfigurableById(id);
      assert configurable != null;

      T config = ConfigurableWrapper.cast(configurable, cls);

      mySettings.select(configurable).doWhenDone(() -> consumer.accept(config, result::setDone));
    });

    return result;
  }
}
