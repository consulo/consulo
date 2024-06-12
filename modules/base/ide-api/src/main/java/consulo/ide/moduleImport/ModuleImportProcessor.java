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
package consulo.ide.moduleImport;

import consulo.application.ApplicationPropertiesComponent;
import consulo.fileChooser.FileChooser;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.localize.LocalizeValue;
import consulo.platform.base.localize.IdeLocalize;
import consulo.project.Project;
import consulo.ui.Alerts;
import consulo.ui.ComboBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.DialogBuilder;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.image.Image;
import consulo.ui.layout.LabeledLayout;
import consulo.util.collection.ContainerUtil;
import consulo.util.concurrent.AsyncResult;
import consulo.util.io.FileUtil;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.io.File;
import java.util.List;

/**
 * @author VISTALL
 * @since 2019-08-26
 */
public class ModuleImportProcessor {
  private static final String LAST_IMPORTED_LOCATION = "last.imported.location";

  /**
   * Will execute module importing. Will show popup for selecting import providers if more that one, and then show import wizard
   *
   * @param project - null mean its new project creation
   * @return
   */
  @RequiredUIAccess
  public static <C extends ModuleImportContext> AsyncResult<Pair<C, ModuleImportProvider<C>>> showFileChooser(@Nullable Project project, @Nullable FileChooserDescriptor chooserDescriptor) {
    boolean isModuleImport = project != null;

    FileChooserDescriptor descriptor = ObjectUtil.notNull(chooserDescriptor, createAllImportDescriptor(isModuleImport));

    VirtualFile toSelect = null;
    String lastLocation = ApplicationPropertiesComponent.getInstance().getValue(LAST_IMPORTED_LOCATION);
    if (lastLocation != null) {
      toSelect = LocalFileSystem.getInstance().refreshAndFindFileByPath(lastLocation);
    }

    AsyncResult<Pair<C, ModuleImportProvider<C>>> result = AsyncResult.undefined();

    AsyncResult<VirtualFile> fileChooseAsync = FileChooser.chooseFile(descriptor, project, toSelect);
    fileChooseAsync.doWhenDone((f) -> {
      ApplicationPropertiesComponent.getInstance().setValue(LAST_IMPORTED_LOCATION, f.getPath());

      showImportChooser(project, f, AsyncResult.undefined());
    });

    fileChooseAsync.doWhenRejected((Runnable)result::setRejected);

    return result;
  }

  @Nonnull
  private static FileChooserDescriptor createAllImportDescriptor(boolean isModuleImport) {
    FileChooserDescriptor descriptor = new FileChooserDescriptor(true, true, true, true, false, false) {
      @Override
      public Image getIcon(VirtualFile file) {
        for (ModuleImportProvider importProvider : ModuleImportProviders.getExtensions(isModuleImport)) {
          if (importProvider.canImport(VirtualFileUtil.virtualToIoFile(file))) {
            return importProvider.getIcon();
          }
        }
        return super.getIcon(file);
      }
    };
    descriptor.setHideIgnored(false);
    descriptor.setTitle("Select File or Directory to Import");
    String description = getFileChooserDescription(isModuleImport);
    descriptor.setDescription(description);
    return descriptor;
  }

  @RequiredUIAccess
  public static <C extends ModuleImportContext> void showImportChooser(
    @Nullable Project project,
    VirtualFile file,
    @Nonnull AsyncResult<Pair<C, ModuleImportProvider<C>>> result
  ) {
    boolean isModuleImport = project != null;

    List<ModuleImportProvider> providers = ModuleImportProviders.getExtensions(isModuleImport);

    File ioFile = VirtualFileUtil.virtualToIoFile(file);
    List<ModuleImportProvider> avaliableProviders = ContainerUtil.filter(providers, provider -> provider.canImport(ioFile));
    if (avaliableProviders.isEmpty()) {
      Alerts.okError("Cannot import anything from '" + FileUtil.toSystemDependentName(file.getPath()) + "'").showAsync();
      result.setRejected();
      return;
    }

    showImportChooser(project, file, providers, result);
  }

  @RequiredUIAccess
  @SuppressWarnings("unchecked")
  public static <C extends ModuleImportContext> void showImportChooser(
    @Nullable Project project,
    @Nonnull VirtualFile file,
    @Nonnull List<ModuleImportProvider> providers,
    @Nonnull AsyncResult<Pair<C, ModuleImportProvider<C>>> result
  ) {
    if (providers.size() == 1) {
      showImportWizard(project, file, providers.get(0), result);
    }
    else {
      AsyncResult<ModuleImportProvider> importResult = showImportTarget(providers);
      importResult.doWhenDone((r) -> showImportWizard(project, file, r, result));
    }
  }

  @RequiredUIAccess
  private static AsyncResult<ModuleImportProvider> showImportTarget(@Nonnull List<ModuleImportProvider> providers) {
    ComboBox<ModuleImportProvider> box = ComboBox.create(providers);
    box.setRender((render, index, item) -> {
      assert item != null;
      render.withIcon(item.getIcon());
      render.append(item.getName());
    });
    box.setValueByIndex(0);

    LabeledLayout layout = LabeledLayout.create(LocalizeValue.localizeTODO("Select import target"), box);

    DialogBuilder builder = new DialogBuilder();
    builder.setTitle("Import Target");
    builder.setCenterPanel((JComponent)TargetAWT.to(layout));

    AsyncResult<ModuleImportProvider> result = AsyncResult.undefined();

    AsyncResult<Void> showResult = builder.showAsync();
    showResult.doWhenDone(() -> result.setDone(box.getValue()));
    showResult.doWhenRejected((Runnable)result::setRejected);
    return result;
  }

  @RequiredUIAccess
  private static <C extends ModuleImportContext> void showImportWizard(
    @Nullable Project project,
    @Nonnull VirtualFile targetFile,
    @Nonnull ModuleImportProvider<C> moduleImportProvider,
    @Nonnull AsyncResult<Pair<C, ModuleImportProvider<C>>> result
  ) {
    ModuleImportDialog<C> dialog = new ModuleImportDialog<>(project, targetFile, moduleImportProvider);

    AsyncResult<Void> showAsync = dialog.showAsync();

    showAsync.doWhenDone(() -> result.setDone(Pair.create(dialog.getContext(), moduleImportProvider)));
    showAsync.doWhenRejected(() -> result.setRejected(Pair.create(dialog.getContext(), moduleImportProvider)));
  }

  private static String getFileChooserDescription(boolean isImport) {
    List<ModuleImportProvider> providers = ModuleImportProviders.getExtensions(isImport);
    return IdeLocalize.importProjectChooserHeader(
      StringUtil.join(providers, ModuleImportProvider::getFileSample, ", <br>")
    ).get();
  }
}
