/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

package consulo.ide.impl.idea.openapi.projectRoots.impl;

import consulo.application.ApplicationManager;
import consulo.content.bundle.Sdk;
import consulo.content.bundle.SdkTable;
import consulo.content.bundle.SdkType;
import consulo.content.bundle.SdkUtil;
import consulo.fileChooser.FileChooser;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.project.ProjectBundle;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.function.Consumer;

/**
 * @author yole
 */
public class SdkConfigurationUtil {
  public static final String PREDEFINED_PREFIX = " (predefined)";

  private SdkConfigurationUtil() {
  }

  private static FileChooserDescriptor createCompositeDescriptor(final SdkType... sdkTypes) {
    FileChooserDescriptor descriptor0 = sdkTypes[0].getHomeChooserDescriptor();
    FileChooserDescriptor descriptor =
      new FileChooserDescriptor(descriptor0.isChooseFiles(), descriptor0.isChooseFolders(), descriptor0.isChooseJars(),
                                descriptor0.isChooseJarsAsFiles(), descriptor0.isChooseJarContents(), descriptor0.isChooseMultiple()) {

        @Override
        public void validateSelectedFiles(final VirtualFile[] files) throws Exception {
          if (files.length > 0) {
            for (SdkType type : sdkTypes) {
              if (type.isValidSdkHome(files[0].getPath())) {
                return;
              }
            }
          }
          String message = files.length > 0 && files[0].isDirectory()
                           ? ProjectBundle.message("sdk.configure.home.invalid.error", sdkTypes[0].getPresentableName())
                           : ProjectBundle.message("sdk.configure.home.file.invalid.error", sdkTypes[0].getPresentableName());
          throw new Exception(message);
        }
      };
    descriptor.setTitle(descriptor0.getTitle());
    return descriptor;
  }

  @RequiredUIAccess
  public static void addSdk(@Nonnull final Sdk sdk) {
    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      @Override
      public void run() {
        SdkTable.getInstance().addSdk(sdk);
      }
    });
  }

  @RequiredUIAccess
  public static void removeSdk(final Sdk sdk) {
    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      @Override
      public void run() {
        SdkTable.getInstance().removeSdk(sdk);
      }
    });
  }

  @Deprecated
  public static String createUniqueSdkName(SdkType type, String home, final Sdk[] sdks) {
    return SdkUtil.createUniqueSdkName(type, home, sdks);
  }

  @Deprecated
  public static String createUniqueSdkName(final String suggestedName, final Sdk[] sdks) {
    return SdkUtil.createUniqueSdkName(suggestedName, sdks);
  }

  @RequiredUIAccess
  public static void selectSdkHome(final SdkType sdkType, @Nonnull @RequiredUIAccess final Consumer<String> consumer) {
    final FileChooserDescriptor descriptor = sdkType.getHomeChooserDescriptor();
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      Sdk sdk = SdkTable.getInstance().findMostRecentSdkOfType(sdkType);
      if (sdk == null) throw new RuntimeException("No SDK of type " + sdkType + " found");
      consumer.accept(sdk.getHomePath());
      return;
    }

    FileChooser.chooseFiles(descriptor, null, getSuggestedSdkPath(sdkType)).doWhenDone(virtualFiles -> {
      final String path = virtualFiles[0].getPath();
      if (sdkType.isValidSdkHome(path)) {
        consumer.accept(path);
        return;
      }

      final String adjustedPath = sdkType.adjustSelectedSdkHome(path);
      if (sdkType.isValidSdkHome(adjustedPath)) {
        consumer.accept(adjustedPath);
      }
    });
  }

  @Nullable
  public static VirtualFile getSuggestedSdkPath(SdkType sdkType) {
    Collection<String> paths = sdkType.suggestHomePaths();
    if(paths.isEmpty()) {
      return null;
    }

    for (String path : paths) {
      VirtualFile maybeSdkHomePath = LocalFileSystem.getInstance().findFileByPath(path);
      if(maybeSdkHomePath != null) {
        return maybeSdkHomePath;
      }
    }
    return null;
  }
}
