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
package com.intellij.openapi.projectRoots;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Consumer;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import org.jdom.Element;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.util.Collection;
import java.util.Collections;

public abstract class SdkType implements SdkTypeId {
  public static final ExtensionPointName<SdkType> EP_NAME = ExtensionPointName.create("com.intellij.sdkType");

  private final String myId;

  public SdkType(@Nonnull String id) {
    myId = id;
  }

  /**
   * @return paths for select in file chooser. Selected path will exists on file system
   */
  @Nonnull
  public Collection<String> suggestHomePaths() {
    return Collections.emptyList();
  }

  public boolean canCreatePredefinedSdks() {
    return false;
  }

  /**
   * If a path selected in the file chooser is not a valid SDK home path, returns an adjusted version of the path that is again
   * checked for validity.
   *
   * @param homePath the path selected in the file chooser.
   * @return the path to be used as the SDK home.
   */

  public String adjustSelectedSdkHome(String homePath) {
    return homePath;
  }

  public abstract boolean isValidSdkHome(String path);


  @Override
  @Nullable
  public final String getVersionString(Sdk sdk) {
    return getVersionString(sdk.getHomePath());
  }

  @Nullable
  public abstract String getVersionString(String sdkHome);

  public abstract String suggestSdkName(String currentSdkName, String sdkHome);

  public void setupSdkPaths(@Nonnull Sdk sdk) {
    SdkModificator sdkModificator = sdk.getSdkModificator();
    setupSdkPaths(sdkModificator);
    sdkModificator.commitChanges();
  }

  public void setupSdkPaths(@Nonnull SdkModificator sdkModificator) {
  }

  /**
   * @return Configurable object for the sdk's additional data or null if not applicable
   */
  @Nullable
  public AdditionalDataConfigurable createAdditionalDataConfigurable(SdkModel sdkModel, SdkModificator sdkModificator) {
    return null;
  }

  @Override
  public void saveAdditionalData(SdkAdditionalData additionalData, Element additional) {

  }

  @Override
  @Nullable
  public SdkAdditionalData loadAdditionalData(Sdk currentSdk, Element additional) {
    return null;
  }

  @Override
  public final String getName() {
    return myId;
  }

  @Nonnull
  @Override
  public final String getId() {
    return myId;
  }

  @Nonnull
  public abstract String getPresentableName();

  @Nonnull
  public abstract Image getIcon();

  @Nullable
  public Image getGroupIcon() {
    return ImageEffects.transparent(getIcon(), 0.5f);
  }

  @Nonnull
  public String getHelpTopic() {
    return "bundle";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof SdkType)) return false;

    final SdkType sdkType = (SdkType)o;

    return myId.equals(sdkType.myId);
  }

  @Override
  public int hashCode() {
    return myId.hashCode();
  }

  @Override
  public String toString() {
    return getId();
  }

  @Nonnull
  public FileChooserDescriptor getHomeChooserDescriptor() {
    final FileChooserDescriptor descriptor = new FileChooserDescriptor(false, true, false, false, false, false) {
      @Override
      public void validateSelectedFiles(VirtualFile[] files) throws Exception {
        if (files.length != 0) {
          final String selectedPath = files[0].getPath();
          boolean valid = isValidSdkHome(selectedPath);
          if (!valid) {
            valid = isValidSdkHome(adjustSelectedSdkHome(selectedPath));
            if (!valid) {
              String message = files[0].isDirectory()
                               ? ProjectBundle.message("sdk.configure.home.invalid.error", getPresentableName())
                               : ProjectBundle.message("sdk.configure.home.file.invalid.error", getPresentableName());
              throw new Exception(message);
            }
          }
        }
      }
    };
    descriptor.setTitle(ProjectBundle.message("sdk.configure.home.title", getPresentableName()));
    return descriptor;
  }

  @Nullable
  public String getDefaultDocumentationUrl(final @Nonnull Sdk sdk) {
    return null;
  }

  public boolean isRootTypeApplicable(final OrderRootType type) {
    return false;
  }

  /**
   * If this method return true, user can add Sdk via configuration. If false - required canCreatePredefinedSdks() = true,
   * and user can copy sdk with custom name
   * @return true if user add is supported
   */
  public boolean supportsUserAdd() {
    return true;
  }

  /**
   * If this method returns true, instead of showing the standard file path chooser when a new SDK of the type is created,
   * the {@link #showCustomCreateUI} method is called.
   *
   * @return true if the custom create UI is supported, false otherwise.
   * @since 12.0
   */
  public boolean supportsCustomCreateUI() {
    return false;
  }

  /**
   * Shows the custom SDK create UI. The returned SDK needs to have the correct name and home path; the framework will call
   * setupSdkPaths() on the returned SDK.
   *
   * @param sdkModel           the list of SDKs currently displayed in the configuration dialog.
   * @param parentComponent    the parent component for showing the dialog.
   * @param sdkCreatedCallback the callback to which the created SDK is passed.
   * @since 12.0
   */
  public void showCustomCreateUI(SdkModel sdkModel, JComponent parentComponent, @RequiredUIAccess Consumer<Sdk> sdkCreatedCallback) {
  }

  /**
   * Checks if the home directory of the specified SDK is valid. By default, checks that the directory points to a valid local
   * path. Can be overridden for remote SDKs.
   *
   * @param sdk the SDK to validate the path for.
   * @return true if the home path is valid, false otherwise.
   * @since 12.1
   */
  public boolean sdkHasValidPath(@Nonnull Sdk sdk) {
    VirtualFile homeDir = sdk.getHomeDirectory();
    return homeDir != null && homeDir.isValid();
  }

  public String sdkPath(VirtualFile homePath) {
    return homePath.getPath();
  }
}
