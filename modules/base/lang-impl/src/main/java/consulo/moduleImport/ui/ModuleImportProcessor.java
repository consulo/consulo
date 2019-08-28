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
package consulo.moduleImport.ui;

import com.intellij.ide.IdeBundle;
import com.intellij.ide.actions.OpenProjectFileChooserDescriptor;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.openapi.util.AsyncResult;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.ContainerUtil;
import consulo.awt.TargetAWT;
import consulo.moduleImport.ModuleImportProvider;
import consulo.moduleImport.ModuleImportProviders;
import consulo.ui.Alerts;
import consulo.ui.RequiredUIAccess;
import consulo.ui.fileChooser.FileChooser;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.io.File;
import java.util.Collections;
import java.util.List;

/**
 * @author VISTALL
 * @since 2019-08-26
 */
public class ModuleImportProcessor {
  private static final String LAST_IMPORTED_LOCATION = "last.imported.location";

  @RequiredUIAccess
  public static AsyncResult<List<Module>> create(@Nonnull Project project, boolean isImport) {
    FileChooserDescriptor descriptor = new FileChooserDescriptor(true, true, true, true, false, false) {
      FileChooserDescriptor myDelegate = new OpenProjectFileChooserDescriptor(true);

      @Override
      public Image getIcon(VirtualFile file) {
        for (ModuleImportProvider importProvider : ModuleImportProviders.getExtensions(true)) {
          if (importProvider.canImport(VfsUtilCore.virtualToIoFile(file))) {
            return importProvider.getIcon();
          }
        }
        Image icon = myDelegate.getIcon(file);
        return icon == null ? super.getIcon(file) : icon;
      }
    };
    descriptor.setHideIgnored(false);
    descriptor.setTitle("Select File or Directory to Import");
    String description = getFileChooserDescription(isImport);
    descriptor.setDescription(description);

    VirtualFile toSelect = null;
    String lastLocation = PropertiesComponent.getInstance().getValue(LAST_IMPORTED_LOCATION);
    if (lastLocation != null) {
      toSelect = LocalFileSystem.getInstance().refreshAndFindFileByPath(lastLocation);
    }

    AsyncResult<VirtualFile> fileChooseAsync = FileChooser.chooseFile(descriptor, project, toSelect);
    fileChooseAsync.doWhenDone((f) -> {
      PropertiesComponent.getInstance().setValue(LAST_IMPORTED_LOCATION, f.getPath());

      showImportChooser(project, f, isImport, AsyncResult.undefined());
    });
    fileChooseAsync.doWhenRejected(() -> {
      // todo
    });

    return AsyncResult.resolved(Collections.emptyList());
  }

  @RequiredUIAccess
  private static void showImportChooser(Project project, VirtualFile file, boolean isImport, AsyncResult<ModuleImportResult> result) {
    List<ModuleImportProvider> providers = ModuleImportProviders.getExtensions(isImport);

    File ioFile = VfsUtilCore.virtualToIoFile(file);
    List<ModuleImportProvider> avaliableProviders = ContainerUtil.filter(providers, provider -> provider.canImport(ioFile));
    if (avaliableProviders.isEmpty()) {
      Alerts.okError("Cannot import anything from '" + FileUtil.toSystemDependentName(file.getPath()) + "'").show();
      result.setRejected();
      return;
    }

    if(avaliableProviders.size() == 1) {
      showImportWizard(project, file, avaliableProviders.get(0));
    }
    else {
      JBPopupFactory.getInstance().createListPopup(new BaseListPopupStep<ModuleImportProvider>("Import Target", avaliableProviders) {
        @Nullable
        @Override
        public Icon getIconFor(ModuleImportProvider value) {
          return TargetAWT.to(value.getIcon());
        }

        @Nonnull
        @Override
        public String getTextFor(ModuleImportProvider value) {
          return value.getName();
        }

        @Override
        public PopupStep onChosen(ModuleImportProvider selectedValue, boolean finalChoice) {
          return doFinalStep(() -> showImportWizard(project, file, selectedValue));
        }
      }).showInFocusCenter();
    }
  }

  @RequiredUIAccess
  private static void showImportWizard(Project project, VirtualFile targetFile, ModuleImportProvider moduleImportProvider) {
    ModuleImportDialog dialog = new ModuleImportDialog<>(project, targetFile, moduleImportProvider);

    dialog.showAsync();
  }

  private static String getFileChooserDescription(boolean isImport) {
    List<ModuleImportProvider> providers = ModuleImportProviders.getExtensions(isImport);
    return IdeBundle.message("import.project.chooser.header", StringUtil.join(providers, ModuleImportProvider::getFileSample, ", <br>"));
  }
}
