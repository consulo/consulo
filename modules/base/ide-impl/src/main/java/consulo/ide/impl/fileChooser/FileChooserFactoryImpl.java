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
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.jspecify.annotations.Nullable;
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
  private final Application myApplication;
  @Inject
  public FileChooserFactoryImpl(Application application) {
    myApplication = application;
  }

  
  @Override
  public FileChooserBuilder newBuilder(FileChooserDescriptor descriptor) {
    return new FileChooserBuilderImpl(
      myApplication,
      descriptor,
      it -> findProvider(it, FileOperateDialogSettings::getFileChooseDialogId, FileChooseDialogProvider.class)
    );
  }

  
  @Override
  public FileSaverBuilder newSaverBuilder(FileSaverDescriptor descriptor) {
    return new FileSaverBuilderImpl(
      descriptor,
      it -> findProvider(it, FileOperateDialogSettings::getFileSaveDialogId, FileSaveDialogProvider.class)
    );
  }

  @Override
  public void installFileCompletion(TextBox textBox, FileChooserDescriptor descriptor, boolean showHidden, @Nullable Disposable parent) {
  }

  public static Map<String, String> getMacroMap() {
    PathMacros macros = PathMacros.getInstance();
    Set<String> allNames = macros.getAllMacroNames();
    Map<String, String> map = new HashMap<>(allNames.size());
    for (String eachMacroName : allNames) {
      map.put("$" + eachMacroName + "$", macros.getValue(eachMacroName));
    }

    return map;
  }

  
  private static <T extends FileOperateDialogProvider> T findProvider(FileChooserDescriptor fileChooserDescriptor,
                                                                      Function<FileOperateDialogSettings, String> idFunc,
                                                                      Class<T> ep) {
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
