/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package com.intellij.openapi.fileChooser.impl;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathMacros;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.fileChooser.*;
import com.intellij.openapi.fileChooser.ex.FileTextFieldImpl;
import com.intellij.openapi.fileChooser.ex.LocalFsFinder;
import com.intellij.openapi.project.Project;
import com.intellij.util.ObjectUtil;
import com.intellij.util.containers.ContainerUtil;
import consulo.ui.fileOperateDialog.FileChooseDialogProvider;
import consulo.ui.fileOperateDialog.FileOperateDialogProvider;
import consulo.fileChooser.FileOperateDialogSettings;
import consulo.ui.fileOperateDialog.FileSaveDialogProvider;
import gnu.trove.THashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Singleton;
import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

@Singleton
public class FileChooserFactoryImpl extends FileChooserFactory {
  @Nonnull
  @Override
  public FileChooserDialog createFileChooser(@Nonnull FileChooserDescriptor descriptor, @Nullable Project project, @Nullable Component parent) {
    return findProvider(descriptor, FileOperateDialogSettings::getFileChooseDialogId, FileChooseDialogProvider.EP_NAME).createFileChooser(descriptor, project, parent);
  }

  @Nonnull
  @Override
  public PathChooserDialog createPathChooser(@Nonnull FileChooserDescriptor descriptor, @Nullable Project project, @Nullable Component parent) {
    return findProvider(descriptor, FileOperateDialogSettings::getFileChooseDialogId, FileChooseDialogProvider.EP_NAME).createPathChooser(descriptor, project, parent);
  }

  @Nonnull
  @Override
  public FileTextField createFileTextField(@Nonnull final FileChooserDescriptor descriptor, boolean showHidden, @Nullable Disposable parent) {
    return new FileTextFieldImpl.Vfs(new JTextField(), getMacroMap(), parent, new LocalFsFinder.FileChooserFilter(descriptor, showHidden));
  }

  @Override
  public void installFileCompletion(@Nonnull JTextField field, @Nonnull FileChooserDescriptor descriptor, boolean showHidden, @Nullable Disposable parent) {
    if (!ApplicationManager.getApplication().isUnitTestMode() && !ApplicationManager.getApplication().isHeadlessEnvironment()) {
      new FileTextFieldImpl.Vfs(field, getMacroMap(), parent, new LocalFsFinder.FileChooserFilter(descriptor, showHidden));
    }
  }

  public static Map<String, String> getMacroMap() {
    final PathMacros macros = PathMacros.getInstance();
    final Set<String> allNames = macros.getAllMacroNames();
    final Map<String, String> map = new THashMap<>(allNames.size());
    for (String eachMacroName : allNames) {
      map.put("$" + eachMacroName + "$", macros.getValue(eachMacroName));
    }

    return map;
  }

  @Nonnull
  @Override
  public FileSaverDialog createSaveFileDialog(@Nonnull FileSaverDescriptor descriptor, @Nullable Project project) {
    return findProvider(descriptor, FileOperateDialogSettings::getFileSaveDialogId, FileSaveDialogProvider.EP_NAME).createSaveFileDialog(descriptor, project, null);
  }

  @Nonnull
  @Override
  public FileSaverDialog createSaveFileDialog(@Nonnull FileSaverDescriptor descriptor, @Nonnull Component parent) {
    return findProvider(descriptor, FileOperateDialogSettings::getFileSaveDialogId, FileSaveDialogProvider.EP_NAME).createSaveFileDialog(descriptor, null, parent);
  }

  @Nonnull
  private static <T extends FileOperateDialogProvider> T findProvider(@Nonnull FileChooserDescriptor fileChooserDescriptor,
                                                                      @Nonnull Function<FileOperateDialogSettings, String> idFunc,
                                                                      @Nonnull ExtensionPointName<T> ep) {
    List<T> extensions = ep.getExtensionList();

    String forceOperateDialogProviderId = fileChooserDescriptor.getForceOperateDialogProviderId();
    if (forceOperateDialogProviderId != null) {
      for (T extension : extensions) {
        if (forceOperateDialogProviderId.equals(extension.getId()) && extension.isAvaliable()) {
          return extension;
        }
      }

      throw new IllegalArgumentException("Unknown file operate id");
    }

    FileOperateDialogSettings settings = FileOperateDialogSettings.getInstance();

    String targetId = idFunc.apply(settings);

    if (targetId != null) {
      for (T extension : extensions) {
        if (targetId.equals(extension.getId()) && extension.isAvaliable()) {
          return extension;
        }
      }
    }

    return ObjectUtil.notNull(ContainerUtil.find(extensions, t -> FileOperateDialogProvider.APPLICATION_ID.equals(t.getId())));
  }
}
