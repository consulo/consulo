/*
 * Copyright 2013-2020 consulo.io
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
package consulo.ide.impl.fileChooser;

import consulo.annotation.component.ComponentProfiles;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.application.macro.PathMacros;
import consulo.component.ComponentManager;
import consulo.disposer.Disposable;
import consulo.fileChooser.*;
import consulo.fileChooser.provider.FileChooseDialogProvider;
import consulo.fileChooser.provider.FileOperateDialogProvider;
import consulo.fileChooser.provider.FileSaveDialogProvider;
import consulo.project.Project;
import consulo.project.ui.wm.WindowManager;
import consulo.ui.TextBox;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.ObjectUtil;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2020-05-30
 */
@Singleton
@ServiceImpl(profiles = ComponentProfiles.UNIFIED)
public class FileChooserFactoryImpl extends FileChooserFactory {
  @Nonnull
  @Override
  public FileChooserDialog createFileChooser(@Nonnull FileChooserDescriptor descriptor, @Nullable ComponentManager project, @Nullable Component parent) {
    return findProvider(descriptor, FileOperateDialogSettings::getFileChooseDialogId, FileChooseDialogProvider.class).createFileChooser(descriptor, project, parent);
  }

  @Nonnull
  @Override
  public PathChooserDialog createPathChooser(@Nonnull FileChooserDescriptor descriptor, @Nullable ComponentManager project, @Nullable Component parent) {
    Component parentComponent = parent == null ? TargetAWT.to(WindowManager.getInstance().suggestParentWindow((Project)project)) : parent;
    return findProvider(descriptor, FileOperateDialogSettings::getFileChooseDialogId, FileChooseDialogProvider.class).createPathChooser(descriptor, project, parentComponent);
  }

  @Nonnull
  @Override
  public FileSaverDialog createSaveFileDialog(@Nonnull FileSaverDescriptor descriptor, @Nullable ComponentManager project) {
    return findProvider(descriptor, FileOperateDialogSettings::getFileSaveDialogId, FileSaveDialogProvider.class).createSaveFileDialog(descriptor, project, null);
  }

  @Nonnull
  @Override
  public FileSaverDialog createSaveFileDialog(@Nonnull FileSaverDescriptor descriptor, @Nonnull Component parent) {
    return findProvider(descriptor, FileOperateDialogSettings::getFileSaveDialogId, FileSaveDialogProvider.class).createSaveFileDialog(descriptor, null, parent);
  }

  @Override
  public void installFileCompletion(@Nonnull TextBox textBox, @Nonnull FileChooserDescriptor descriptor, boolean showHidden, @Nullable Disposable parent) {
  }

  public static Map<String, String> getMacroMap() {
    final PathMacros macros = PathMacros.getInstance();
    final Set<String> allNames = macros.getAllMacroNames();
    final Map<String, String> map = new HashMap<>(allNames.size());
    for (String eachMacroName : allNames) {
      map.put("$" + eachMacroName + "$", macros.getValue(eachMacroName));
    }

    return map;
  }

  @Nonnull
  private static <T extends FileOperateDialogProvider> T findProvider(@Nonnull FileChooserDescriptor fileChooserDescriptor,
                                                                      @Nonnull Function<FileOperateDialogSettings, String> idFunc,
                                                                      @Nonnull Class<T> ep) {
    List<T> extensions = Application.get().getExtensionPoint(ep).getExtensionList();

    String forceOperateDialogProviderId = fileChooserDescriptor.getForceOperateDialogProviderId();
    if (forceOperateDialogProviderId != null) {
      for (T extension : extensions) {
        if (forceOperateDialogProviderId.equals(extension.getId()) && extension.isAvailable()) {
          return extension;
        }
      }

      throw new IllegalArgumentException("Unknown file operate id");
    }

    FileOperateDialogSettings settings = FileOperateDialogSettings.getInstance();

    String targetId = idFunc.apply(settings);

    if (targetId != null) {
      for (T extension : extensions) {
        if (targetId.equals(extension.getId()) && extension.isAvailable()) {
          return extension;
        }
      }
    }

    return ObjectUtil.notNull(ContainerUtil.find(extensions, t -> FileOperateDialogProvider.APPLICATION_ID.equals(t.getId())));
  }
}
