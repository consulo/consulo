/*
 * Copyright 2013-2023 consulo.io
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
package consulo.content.bundle;

import consulo.fileChooser.FileChooserDescriptor;
import consulo.localize.LocalizeValue;
import consulo.platform.Platform;
import consulo.project.localize.ProjectLocalize;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.nio.file.Path;
import java.util.Collection;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 25/04/2023
 */
public abstract class BundleType extends SdkType {
  public BundleType(@Nonnull String id) {
    super(id);
  }

  public abstract void collectHomePaths(@Nonnull Platform platform, @Nonnull Consumer<Path> pathConsumer);

  public boolean canCreatePredefinedSdks(@Nonnull Platform platform) {
    return false;
  }

  public boolean isValidSdkHome(@Nonnull Platform platform, @Nonnull Path path) {
    return getVersionString(platform, path) != null;
  }

  @Nonnull
  public String suggestSdkName(@Nonnull Platform platform, @Nullable String currentSdkName, @Nonnull Path path) {
    return getPresentableName() + " " + getVersionString(platform, path);
  }

  @Nullable
  public abstract String getVersionString(@Nonnull Platform platform, @Nonnull Path path);

  /**
   * If a path selected in the file chooser is not a valid SDK home path, returns an adjusted version of the path that is again
   * checked for validity.
   *
   * @param homePath the path selected in the file chooser.
   * @return the path to be used as the SDK home.
   */
  @Nonnull
  public Path adjustSelectedSdkHome(@Nonnull Platform platform, Path homePath) {
    return homePath;
  }

  @Nonnull
  public FileChooserDescriptor getHomeChooserDescriptor(@Nonnull Platform platform) {
    final FileChooserDescriptor descriptor = new FileChooserDescriptor(false, true, false, false, false, false) {
      @Override
      public void validateSelectedFiles(VirtualFile[] files) throws Exception {
        if (files.length != 0) {
          final Path selectedPath = files[0].toNioPath();
          boolean valid = isValidSdkHome(platform, selectedPath);
          if (!valid) {
            valid = isValidSdkHome(platform, adjustSelectedSdkHome(platform, selectedPath));
            if (!valid) {
              LocalizeValue message = files[0].isDirectory()
                ? ProjectLocalize.sdkConfigureHomeInvalidError(getPresentableName())
                : ProjectLocalize.sdkConfigureHomeFileInvalidError(getPresentableName());
              throw new Exception(message.get());
            }
          }
        }
      }
    };
    descriptor.withTitleValue(ProjectLocalize.sdkConfigureHomeTitle(getPresentableName()));
    return descriptor;
  }

  // region obsolete stuff

  @Nonnull
  @Override
  @Deprecated
  public final FileChooserDescriptor getHomeChooserDescriptor() {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public final String suggestSdkName(String currentSdkName, String sdkHome) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public final String adjustSelectedSdkHome(String homePath) {
    throw new UnsupportedOperationException();
  }

  @Nullable
  @Override
  @Deprecated
  public final String getVersionString(String sdkHome) {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  @Override
  @Deprecated
  public final Collection<String> suggestHomePaths() {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public final boolean isValidSdkHome(String path) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public final boolean canCreatePredefinedSdks() {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public final String sdkPath(VirtualFile homePath) {
    throw new UnsupportedOperationException();
  }
  // endregion
}
